package com.example.mysmartkiln.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
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
import com.example.mysmartkiln.ui.components.DataLoggerStatusScreen
import com.example.mysmartkiln.ui.components.DataLoggerDataScreen
import com.example.mysmartkiln.ui.components.DataLoggerCurveScreen
import com.example.mysmartkiln.ui.components.KilnCommandProgressBanner
import com.example.mysmartkiln.ui.components.KilnInterruptedDialog
import com.example.mysmartkiln.ui.components.KilnApReconfigWaitDialog
import com.example.mysmartkiln.ui.components.KilnOperateOfflineDialog
import com.example.mysmartkiln.ui.components.KilnEspStatusBadge
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.DialogBrand
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.data.local.KilnDataSource
import com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel
import com.example.mysmartkiln.utils.AppOperationMode
import com.example.mysmartkiln.utils.KilnNetworkConnector
import com.example.mysmartkiln.utils.KilnOfflineSession
import com.example.mysmartkiln.utils.LocalOperationModeController
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KilnDetailScreen(
    navController: NavController,
    kilnName: String,
    kilnMacAddress: String,
    preferLocal: Boolean = false,
    viewModel: KilnDetailViewModel
) {
    val isDataLogger by viewModel.isDataLogger.collectAsState()
    val tabTitles = if (isDataLogger) {
        listOf("Estado", "Datos", "Curva")
    } else {
        listOf("Estado", "Programa", "Curva")
    }
    val pagerState = rememberPagerState(pageCount = { tabTitles.size })
    val scope = rememberCoroutineScope()
    var isMenuOpen by remember { mutableStateOf(false) }
    val kilnStatus by viewModel.kilnStatus.collectAsState()
    val dataSource by viewModel.dataSource.collectAsState()
    val preferLocalMode by viewModel.preferLocalMode.collectAsState()
    val operationMode by LocalOperationModeController.current.mode.collectAsState()
    val isSendingCommand by viewModel.isSendingCommand.collectAsState()
    val isConnectingToAp by viewModel.isConnectingToAp.collectAsState()
    val isPreparingLocalLink by viewModel.isPreparingLocalLink.collectAsState()
    val sendingPhase by viewModel.sendingCommandPhase.collectAsState()
    val sendingDetail by viewModel.sendingCommandDetail.collectAsState()
    val showInterruptedDialog by viewModel.showInterruptedDialog.collectAsState()
    val apReconfigWaitDialog by viewModel.apReconfigWaitDialog.collectAsState()
    val localActionError by viewModel.localActionError.collectAsState()
    val shouldShowManualWifiDialog by viewModel.shouldShowManualWifiDialog.collectAsState()
    val needsNearbyWifiPermission by viewModel.needsNearbyWifiPermission.collectAsState()
    val kilnProgram by viewModel.kilnProgram.collectAsState()
    val kilnColors = LocalKilnColors.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val nearbyWifiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onNearbyWifiPermissionResult(granted)
    }

    LaunchedEffect(needsNearbyWifiPermission) {
        if (!needsNearbyWifiPermission) return@LaunchedEffect
        if (!KilnNetworkConnector.isSupported) {
            viewModel.onNearbyWifiPermissionResult(false)
            return@LaunchedEffect
        }
        if (KilnNetworkConnector.hasRequiredPermissions(context)) {
            viewModel.onNearbyWifiPermissionResult(true)
            return@LaunchedEffect
        }
        nearbyWifiPermissionLauncher.launch(
            KilnNetworkConnector.requiredRuntimePermission()
        )
    }

    LaunchedEffect(kilnMacAddress, preferLocal) {
        // Pedir permiso pronto si el usuario eligió SoftAP desde la lista.
        if (preferLocal &&
            KilnNetworkConnector.isSupported &&
            !KilnNetworkConnector.hasRequiredPermissions(context)
        ) {
            nearbyWifiPermissionLauncher.launch(
                KilnNetworkConnector.requiredRuntimePermission()
            )
        }
        viewModel.loadKilnData(kilnMacAddress, preferLocal = preferLocal)
    }

    // Ajustes Online/Local: reflejar política al instante (chip Local/Remoto).
    LaunchedEffect(operationMode, kilnMacAddress, isDataLogger) {
        if (isDataLogger || kilnMacAddress.isBlank()) return@LaunchedEffect
        val prefer =
            operationMode == AppOperationMode.LOCAL ||
                KilnOfflineSession.prefersLocal(kilnMacAddress)
        viewModel.onAppOperationModeChanged(preferLocal = prefer)
    }

    // Etapas para el círculo de duración: Firebase/HTTP suelen traer solo el
    // nombre; las etapas viven en Room. Sin esto, Status no dibuja arcos hasta
    // visitar la pestaña Programa (que sí llama ensureProgramStagesLoaded).
    LaunchedEffect(kilnMacAddress, kilnProgram.name, isDataLogger) {
        if (!isDataLogger && kilnProgram.name.isNotBlank()) {
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
                .padding(vertical = 4.dp)
        ) {
            // Título + ID a la izquierda; conexión + estado a la derecha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 56.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = kilnName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = kilnColors.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        maxLines = 1
                    )
                    if (kilnColors.isEsp) {
                        Text(
                            text = "ID: ${kilnMacAddress.takeLast(8).uppercase()}",
                            color = kilnColors.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                val awaitingLocalLink = operationMode == AppOperationMode.LOCAL &&
                    dataSource != KilnDataSource.LOCAL_AP &&
                    (isConnectingToAp || isPreparingLocalLink || !kilnStatus.isConnected)
                // Label = modo Ajustes; color = enlace vivo.
                val statusLabel = when {
                    operationMode == AppOperationMode.LOCAL &&
                        dataSource == KilnDataSource.LOCAL_AP -> "Local"
                    operationMode == AppOperationMode.LOCAL &&
                        awaitingLocalLink &&
                        (isConnectingToAp || isPreparingLocalLink) -> "Conectando…"
                    operationMode == AppOperationMode.LOCAL -> "Local"
                    dataSource == KilnDataSource.FIREBASE && kilnStatus.isConnected -> "Remoto"
                    dataSource == KilnDataSource.FIREBASE &&
                        kilnStatus.isReconnecting &&
                        kilnStatus.lastLiveTelemetryAtMs > 0L -> "Reconectando"
                    dataSource == KilnDataSource.FIREBASE -> "Remoto"
                    else -> "Remoto"
                }
                val statusColor = when {
                    operationMode == AppOperationMode.LOCAL &&
                        dataSource == KilnDataSource.LOCAL_AP -> Color(0xFF10B981)
                    operationMode == AppOperationMode.LOCAL &&
                        awaitingLocalLink &&
                        (isConnectingToAp || isPreparingLocalLink) -> Color(0xFFF59E0B)
                    operationMode == AppOperationMode.LOCAL -> Color(0xFF94A3B8)
                    dataSource == KilnDataSource.FIREBASE && kilnStatus.isConnected ->
                        Color(0xFF10B981)
                    dataSource == KilnDataSource.FIREBASE &&
                        kilnStatus.isReconnecting &&
                        kilnStatus.lastLiveTelemetryAtMs > 0L -> Color(0xFFF59E0B)
                    else -> Color(0xFF94A3B8)
                }
                val loggerConnected = dataSource != KilnDataSource.DISCONNECTED &&
                    (kilnStatus.isConnected || kilnStatus.currentTemperature > 0f)
                val badgeStatus = when {
                    showInterruptedDialog -> "INTERRUMPIDO"
                    // SoftAP: el HTTP local manda (aunque el flag isConnected
                    // de Firebase parpadee en el handoff).
                    dataSource == KilnDataSource.LOCAL_AP ->
                        kilnStatus.espStatus.ifBlank { "IDLE" }
                    // Preferencia SoftAP sin enlace: no Horneando de Firebase viejo.
                    awaitingLocalLink -> "DESCONECTADO"
                    // Pausa / horneando / interrumpido: no tapar con DESCONECTADO
                    // durante el cambio SoftAP → WiFi casa (mismo bounce que el botón).
                    kilnStatus.isPaused ||
                        kilnStatus.espStatus.equals("PAUSADO", ignoreCase = true) ->
                        "PAUSADO"
                    kilnStatus.espStatus.equals("CALENTANDO", ignoreCase = true) ||
                        kilnStatus.espStatus.equals("INTERRUMPIDO", ignoreCase = true) ||
                        kilnStatus.espStatus.equals("RECONNECTING", ignoreCase = true) ||
                        kilnStatus.espStatus.equals("FINALIZADO", ignoreCase = true) ->
                        kilnStatus.espStatus
                    !kilnStatus.isConnected &&
                        kilnStatus.lastKnownEspStatus.equals("PAUSADO", ignoreCase = true) ->
                        "PAUSADO"
                    !kilnStatus.isConnected &&
                        kilnStatus.lastKnownEspStatus.uppercase() in setOf(
                            "CALENTANDO", "INTERRUMPIDO", "FINALIZADO"
                        ) ->
                        kilnStatus.lastKnownEspStatus
                    !kilnStatus.isConnected -> "DESCONECTADO"
                    else -> kilnStatus.espStatus
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                fontSize = 12.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(statusColor, CircleShape)
                        )
                    }
                    if (isDataLogger) {
                        DataLoggerConnectionBadge(connected = loggerConnected)
                    } else {
                        KilnEspStatusBadge(espStatus = badgeStatus)
                    }
                }
            }

            // Tab Navigation con indicador manual
            Column {
                TabRow(
                    selectedTabIndex = pagerState.currentPage.coerceIn(0, tabTitles.lastIndex),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .height(26.dp),
                    containerColor = kilnColors.screenBackground,
                    contentColor = kilnColors.accent,
                    divider = {},
                    indicator = {}
                ) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = if (kilnColors.isEsp) title.uppercase() else title,
                                    color = if (pagerState.currentPage == index) {
                                        kilnColors.accent
                                    } else {
                                        kilnColors.body
                                    },
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(2.dp)
                ) {
                    repeat(tabTitles.size) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == pagerState.currentPage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.4f)
                                        .height(2.dp)
                                        .background(
                                            color = kilnColors.accent,
                                            shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.Top
            ) { page ->
                if (isDataLogger) {
                    when (page) {
                        0 -> DataLoggerStatusScreen(
                            kilnMacAddress = kilnMacAddress,
                            viewModel = viewModel
                        )
                        1 -> DataLoggerDataScreen(
                            kilnMacAddress = kilnMacAddress,
                            viewModel = viewModel
                        )
                        else -> DataLoggerCurveScreen(
                            kilnMacAddress = kilnMacAddress,
                            viewModel = viewModel
                        )
                    }
                } else {
                    when (page) {
                        0 -> KilnStatusScreen(
                            kilnMacAddress = kilnMacAddress,
                            viewModel = viewModel,
                            onLoadProgram = {
                                navController.navigate("select_program/$kilnMacAddress/$kilnName")
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
            }
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

        // SoftAP al entrar (SIN NUBE) o tras comando: instrucciones WiFi manual.
        // No mostrar mientras SoftAP/permiso están en curso (evita doble diálogo).
        if (shouldShowManualWifiDialog &&
            !isSendingCommand &&
            !isConnectingToAp &&
            !isPreparingLocalLink &&
            !needsNearbyWifiPermission
        ) {
            KilnOperateOfflineDialog(onDismiss = { viewModel.clearManualWifiDialogRequest() })
        }

        // Un solo feedback (banner, sin Dialog): SoftAP + envío HTTP.
        // El Dialog de Compose tapa el WiFi nativo; el banner no.
        if (isPreparingLocalLink || isSendingCommand) {
            KilnCommandProgressBanner(
                phase = sendingPhase.ifBlank { "Trabajando con el horno…" },
                detail = sendingDetail.ifBlank { null },
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(8f)
            )
        }

        apReconfigWaitDialog?.let { waitState ->
            KilnApReconfigWaitDialog(
                remainingSeconds = waitState.remainingSeconds,
                actionDescription = waitState.actionDescription,
                onDismiss = { viewModel.clearApReconfigWaitDialog() }
            )
        }

        localActionError?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.clearLocalActionError() },
                containerColor = kilnColors.cardBackground,
                shape = RoundedCornerShape(16.dp),
                title = {
                    Text(
                        text = "No se actualizó el horno",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = kilnColors.title
                        )
                    )
                },
                text = {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kilnColors.body,
                            lineHeight = 22.sp
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearLocalActionError() }) {
                        Text(
                            text = "Entendido",
                            color = DialogBrand.primaryButton(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        // Diálogo "Programa interrumpido": no mostrarlo mientras se pide SoftAP
        // (el Dialog de Compose tapa el diálogo nativo de Android para SmartKiln-…).
        if (!isDataLogger &&
            showInterruptedDialog &&
            !isSendingCommand &&
            !isConnectingToAp &&
            !isPreparingLocalLink
        ) {
            KilnInterruptedDialog(
                programName = kilnProgram.name,
                onResume = { viewModel.resumeKiln() },
                onCancel = { viewModel.cancelInterruptedProgram() }
            )
        }
    }
}

@Composable
private fun DataLoggerConnectionBadge(connected: Boolean) {
    val kilnColors = LocalKilnColors.current
    val bg = if (connected) {
        kilnColors.success.copy(alpha = 0.22f)
    } else {
        kilnColors.track
    }
    val fg = if (connected) kilnColors.success else kilnColors.muted
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(fg, CircleShape)
            )
            Text(
                text = if (connected) "CONECTADO" else "SIN CONEXIÓN",
                color = fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 13.sp
            )
        }
    }
}
