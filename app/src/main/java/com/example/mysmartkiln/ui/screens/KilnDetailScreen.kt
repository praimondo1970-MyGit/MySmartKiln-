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
import com.example.mysmartkiln.ui.components.KilnStatusScreen
import com.example.mysmartkiln.ui.components.KilnProgramScreen
import com.example.mysmartkiln.ui.components.KilnCurveScreen
import com.example.mysmartkiln.ui.components.KilnSendingCommandDialog
import com.example.mysmartkiln.ui.theme.AppColors
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
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(kilnMacAddress) {
        viewModel.loadKilnData(kilnMacAddress)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshConnection()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Primary100)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp)
        ) {
            // Título del horno con estado de conexión - misma posición que "Programas" y "Mis Hornos"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 70.dp, bottom = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nombre del horno
                Text(
                    text = kilnName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color(0xFF1E3A8A),
                        fontWeight = FontWeight.Bold
                    )
                )
                
                val statusLabel = when (dataSource) {
                    KilnDataSource.LOCAL_AP -> "Local"
                    KilnDataSource.FIREBASE -> "Remoto"
                    KilnDataSource.DISCONNECTED -> if (kilnStatus.currentTemperature > 0f) "Local" else "—"
                }
                val statusColor = when (dataSource) {
                    KilnDataSource.LOCAL_AP -> Color(0xFF10B981)
                    KilnDataSource.FIREBASE -> if (kilnStatus.isConnected) Color(0xFF10B981) else Color(0xFF94A3B8)
                    KilnDataSource.DISCONNECTED ->
                        if (kilnStatus.currentTemperature > 0f) Color(0xFF10B981) else Color(0xFF94A3B8)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF1E3A8A),
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
            }

            // Tab Navigation con indicador manual
            Column {
                // Tabs sin indicador
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .height(27.dp), // Solo para los textos
                    containerColor = AppColors.Primary100,
                    contentColor = AppColors.Primary700,
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
                                    color = if (pagerState.currentPage == index) AppColors.Primary700 else AppColors.Primary400,
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
                                            color = AppColors.Primary700,
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
            // Navigation Bar Background - Full width with precise curved top
            Box(
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 0.dp, y = 60.dp)
                    .fillMaxWidth()
                    .requiredHeight(height = 141.dp)
                    .background(
                        color = Color(0xFF003D99),
                        shape = object : androidx.compose.ui.graphics.Shape {
                            override fun createOutline(
                                size: androidx.compose.ui.geometry.Size,
                                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                                density: androidx.compose.ui.unit.Density
                            ): androidx.compose.ui.graphics.Outline {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    val width = size.width
                                    val height = size.height

                                    // Convert dp to pixels for precise positioning
                                    val densityValue = density.density

                                    // Start from left edge at y=34 (relative to top of shape)
                                    val startY = 34f * densityValue
                                    val curveStartY = startY

                                    // Move to start position (left edge, y=34)
                                    moveTo(0f, curveStartY)

                                    // Left side straight line (from 0,34 to 147,34)
                                    lineTo(width * (147f / 390f), curveStartY)

                                    // First curve: C158.046,34 166.586,43.8745 173.422,52.5508
                                    cubicTo(
                                        width * (158.046f / 390f), curveStartY,
                                        width * (166.586f / 390f), curveStartY + (9.8745f * densityValue),
                                        width * (173.422f / 390f), curveStartY + (18.5508f * densityValue)
                                    )

                                    // Second curve: C178.915,59.5232 187.435,64 197,64
                                    cubicTo(
                                        width * (178.915f / 390f), curveStartY + (25.5232f * densityValue),
                                        width * (187.435f / 390f), curveStartY + (30f * densityValue),
                                        width * (197f / 390f), curveStartY + (30f * densityValue)
                                    )

                                    // Third curve: C206.565,64 215.085,59.5232 220.578,52.5508
                                    cubicTo(
                                        width * (206.565f / 390f), curveStartY + (30f * densityValue),
                                        width * (215.085f / 390f), curveStartY + (25.5232f * densityValue),
                                        width * (220.578f / 390f), curveStartY + (18.5508f * densityValue)
                                    )

                                    // Fourth curve: C227.414,43.8745 235.954,34 247,34
                                    cubicTo(
                                        width * (227.414f / 390f), curveStartY + (9.8745f * densityValue),
                                        width * (235.954f / 390f), curveStartY,
                                        width * (247f / 390f), curveStartY
                                    )

                                    // Right side straight line (from 247,34 to right edge, y=34)
                                    lineTo(width, curveStartY)

                                    // Continue to right edge at y=54
                                    lineTo(width, curveStartY + (20f * densityValue))

                                    // Bottom right corner
                                    lineTo(width, height)

                                    // Bottom edge
                                    lineTo(0f, height)

                                    // Left side back to start
                                    lineTo(0f, curveStartY + (20f * densityValue))

                                    // Close the path back to start
                                    close()
                                }
                                return androidx.compose.ui.graphics.Outline.Generic(path)
                            }
                        }
                    )
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
    }
}
