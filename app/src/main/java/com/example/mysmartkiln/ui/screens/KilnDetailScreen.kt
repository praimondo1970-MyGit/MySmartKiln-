package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.screens.AddMenuScreen
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.components.BottomNavBarBackground
import com.example.mysmartkiln.ui.components.KilnStatusScreen
import com.example.mysmartkiln.ui.components.KilnProgramScreen
import com.example.mysmartkiln.ui.components.KilnCurveScreen
import com.example.mysmartkiln.ui.components.KilnSendingCommandDialog
import com.example.mysmartkiln.ui.components.KilnInterruptedDialog
import com.example.mysmartkiln.ui.components.KilnApReconfigWaitDialog
import com.example.mysmartkiln.ui.components.KilnEspStatusBadge
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.data.local.KilnDataSource
import com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KilnDetailScreen(
    navController: NavController,
    kilnName: String,
    kilnMacAddress: String,
    viewModel: KilnDetailViewModel
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    var isMenuOpen by remember { mutableStateOf(false) }
    val kilnStatus by viewModel.kilnStatus.collectAsState()
    val dataSource by viewModel.dataSource.collectAsState()
    val isSendingCommand by viewModel.isSendingCommand.collectAsState()
    val sendingPhase by viewModel.sendingCommandPhase.collectAsState()
    val sendingDetail by viewModel.sendingCommandDetail.collectAsState()
    val showInterruptedDialog by viewModel.showInterruptedDialog.collectAsState()
    val apReconfigWaitDialog by viewModel.apReconfigWaitDialog.collectAsState()
    val kilnProgram by viewModel.kilnProgram.collectAsState()
    val kilnColors = LocalKilnColors.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(kilnMacAddress) {
        viewModel.loadKilnData(kilnMacAddress)
    }

    // Etapas para el círculo de duración: Firebase/HTTP suelen traer solo el
    // nombre; las etapas viven en Room. Sin esto, Status no dibuja arcos hasta
    // visitar la pestaña Programa (que sí llama ensureProgramStagesLoaded).
    LaunchedEffect(kilnMacAddress, kilnProgram.name) {
        if (kilnProgram.name.isNotBlank()) {
            viewModel.ensureProgramStagesLoaded()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val handle = navController.currentBackStackEntry?.savedStateHandle
                if (handle?.get<Boolean>("await_program_reload") == true) {
                    handle["await_program_reload"] = false
                    viewModel.reloadCurrentProgramAfterEdit()
                }
                viewModel.refreshConnection()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(kilnColors.screenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp)
        ) {
            // Título a la izquierda; conexión + estado operativo apilados a la derecha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 70.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = kilnName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = kilnColors.title,
                        fontWeight = FontWeight.Bold
                    )
                )

                val statusLabel = when (dataSource) {
                    KilnDataSource.LOCAL_AP -> "Local"
                    KilnDataSource.FIREBASE -> "Remoto"
                    KilnDataSource.DISCONNECTED ->
                        if (kilnStatus.currentTemperature > 0f) "Local" else "—"
                }
                val statusColor = when (dataSource) {
                    KilnDataSource.LOCAL_AP -> Color(0xFF10B981)
                    KilnDataSource.FIREBASE -> when {
                        kilnStatus.isConnected -> Color(0xFF10B981)
                        kilnStatus.isReconnecting -> Color(0xFFF59E0B)
                        else -> Color(0xFF94A3B8)
                    }
                    KilnDataSource.DISCONNECTED ->
                        if (kilnStatus.currentTemperature > 0f) Color(0xFF10B981) else Color(0xFF94A3B8)
                }
                val badgeStatus = when {
                    showInterruptedDialog -> "INTERRUMPIDO"
                    kilnStatus.isReconnecting -> "RECONNECTING"
                    else -> kilnStatus.espStatus
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = kilnColors.title,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(statusColor, CircleShape)
                        )
                    }
                    KilnEspStatusBadge(espStatus = badgeStatus)
                }
            }

            // Tab Navigation con indicador manual
            Column {
                // Tabs sin indicador
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .height(27.dp), // Solo para los textos
                    containerColor = kilnColors.screenBackground,
                    contentColor = kilnColors.accent,
                    divider = {}, // Sin divisor
                    indicator = {} // Sin indicador automático
                ) {
                    listOf("Estado", "Programa", "Curva").forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    color = if (pagerState.currentPage == index) kilnColors.accent else kilnColors.muted,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                            }
                        )
                    }
                }
                
                // Indicador manual debajo de los tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .height(3.dp)
                ) {
                    // Tres espacios para los tres tabs
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Mostrar indicador solo en el tab seleccionado
                            if (index == pagerState.currentPage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f) // 50% del ancho del tab
                                        .height(3.dp)
                                        .background(
                                            color = kilnColors.accent,
                                            shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // Spacer para posicionar las Cards justo debajo de los tabs
            Spacer(modifier = Modifier.height(10.dp))
            
            // Content with Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            ) { page ->
                when (page) {
                    0 -> KilnStatusScreen(
                        kilnMacAddress = kilnMacAddress,
                        viewModel = viewModel,
                        onLoadProgram = {
                            navController.navigate("select_program/$kilnMacAddress/$kilnName")
                        },
                        onConnectToKiln = {
                            navController.navigate("connect_kiln/$kilnMacAddress/$kilnName")
                        }
                    )
                    1 -> KilnProgramScreen(
                        kilnMacAddress = kilnMacAddress,
                        viewModel = viewModel,
                        onChangeProgram = {
                            navController.navigate("select_program/$kilnMacAddress/$kilnName")
                        },
                        onEditCurrentProgram = { programId ->
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("await_program_reload", true)
                            navController.navigate(
                                com.example.mysmartkiln.navigation.Screen.CreateProgram
                                    .createRoute(programId, "edit")
                            )
                        }
                    )
                    2 -> KilnCurveScreen(
                        kilnMacAddress = kilnMacAddress,
                        viewModel = viewModel
                    )
                }
            }
            
            // Spacer para el resto del espacio
            Spacer(modifier = Modifier.weight(1f))
        }

        // Overlay del menú de agregar - SE SUPERPONE cuando isMenuOpen = true
        if (isMenuOpen) {
            AddMenuScreen(
                onNewKilnClick = {
                    isMenuOpen = false
                    navController.navigate("kilns?startScanning=true")
                },
                onNewProgramClick = {
                    isMenuOpen = false
                    navController.navigate("create_program/new/create")
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f) // Por encima del contenido pero debajo de la navegación
            )
        }

        // Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomStart)
                .fillMaxWidth()
                .requiredHeight(height = 200.dp)
                .zIndex(2f) // Por encima del overlay del menú
        ) {
            // Navigation Bar Background
            BottomNavBarBackground(
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 0.dp, y = 60.dp)
            )

            // Responsive Bottom Navigation Bar
            ResponsiveBottomNavigation(
                onKilnsClick = { navController.navigate("kilns") },
                onProgramsClick = { navController.navigate("programs") },
                onActivityClick = { navController.navigate("activity") },
                onSettingsClick = { navController.navigate("settings") },
                onPlusClick = { isMenuOpen = !isMenuOpen }, // Toggle menu
                isKilnsActive = false,
                isProgramsActive = false,
                isActivityActive = false,
                isSettingsActive = false,
                showFab = true, // Mostrar el botón FAB
                isMenuOpen = isMenuOpen, // Pass state to ResponsiveBottomNavigation
                modifier = Modifier.align(alignment = Alignment.BottomStart)
            )
        }

        // Home Indicator
        HomeIndicatorWhite(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 0.dp, y = 810.dp)
        )

        // Diálogo modal con círculo girando: conectando + enviando comando.
        if (isSendingCommand) {
            KilnSendingCommandDialog(
                phase = sendingPhase.ifBlank { "Enviando comando…" },
                detail = sendingDetail.ifBlank { null }
            )
        }

        apReconfigWaitDialog?.let { waitState ->
            KilnApReconfigWaitDialog(
                remainingSeconds = waitState.remainingSeconds,
                actionDescription = waitState.actionDescription,
                onDismiss = { viewModel.clearApReconfigWaitDialog() }
            )
        }

        // Diálogo "Programa interrumpido": el ESP detectó al boot que estaba
        // calentando antes del corte. Si está abierto el modal de envío de
        // comando, lo dejamos arriba (estamos justo respondiendo al usuario).
        if (showInterruptedDialog && !isSendingCommand) {
            KilnInterruptedDialog(
                programName = kilnProgram.name,
                onResume = { viewModel.resumeKiln() },
                onCancel = { viewModel.cancelInterruptedProgram() }
            )
        }
    }
}
