package com.example.mysmartkiln.ui.screens

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.mysmartkiln.ble.BleViewModel
import com.example.mysmartkiln.ui.components.DeviceCard
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.components.ScrollGradient
import com.example.mysmartkiln.ui.theme.AppColors

/**
 * Pantalla que muestra los dispositivos BLE encontrados
 * Basada en el diseño de ActivityScreen
 */
@Composable
fun DevicesFoundScreen(
    bleViewModel: BleViewModel,
    onDeviceSelected: (String) -> Unit = {},
    onNavigateToKilns: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onCreateProgram: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scannedDevices by bleViewModel.scannedDevices.collectAsState()
    
    // Estado para controlar el menú "+"
    var isMenuOpen by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColors.Primary100)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp)
        ) {
            // Título "Dispositivos encontrados" - mismo estilo que "Programas"
            Text(
                text = "Dispositivos encontrados",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color(0xFF1E3A8A),
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 70.dp, bottom = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Lista de dispositivos con scroll gradient
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp)
                ) {
                    items(scannedDevices) { device ->
                        DeviceCard(
                            device = device,
                            onClick = { onDeviceSelected(device.address) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Espaciado final
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(160.dp))
                    }
                }
                
                // Gradiente de scroll
                ScrollGradient(
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
        
        // Overlay del menú de agregar
        if (isMenuOpen) {
            AddMenuScreen(
                onNewKilnClick = {
                    isMenuOpen = false
                    bleViewModel.scanDevices(5000)
                },
                onNewProgramClick = {
                    isMenuOpen = false
                    onCreateProgram()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            )
        }
        
        // Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomStart)
                .fillMaxWidth()
                .requiredHeight(height = 200.dp)
                .zIndex(2f)
        ) {
            // Navigation Bar Background
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
                                    val densityValue = density.density
                                    
                                    val startY = 34f * densityValue
                                    moveTo(0f, startY)
                                    lineTo(width * (147f/390f), startY)
                                    cubicTo(
                                        width * (158.046f/390f), startY,
                                        width * (166.586f/390f), startY + (9.8745f * densityValue),
                                        width * (173.422f/390f), startY + (18.5508f * densityValue)
                                    )
                                    cubicTo(
                                        width * (178.915f/390f), startY + (25.5232f * densityValue),
                                        width * (187.435f/390f), startY + (30f * densityValue),
                                        width * (197f/390f), startY + (30f * densityValue)
                                    )
                                    cubicTo(
                                        width * (206.565f/390f), startY + (30f * densityValue),
                                        width * (215.085f/390f), startY + (25.5232f * densityValue),
                                        width * (220.578f/390f), startY + (18.5508f * densityValue)
                                    )
                                    cubicTo(
                                        width * (227.414f/390f), startY + (9.8745f * densityValue),
                                        width * (235.954f/390f), startY,
                                        width * (247f/390f), startY
                                    )
                                    lineTo(width, startY)
                                    lineTo(width, startY + (20f * densityValue))
                                    lineTo(width, height)
                                    lineTo(0f, height)
                                    lineTo(0f, startY + (20f * densityValue))
                                    close()
                                }
                                return androidx.compose.ui.graphics.Outline.Generic(path)
                            }
                        }
                    )
            )
            
            // Responsive Bottom Navigation Bar
            ResponsiveBottomNavigation(
                onKilnsClick = onNavigateToKilns,
                onProgramsClick = onNavigateToPrograms,
                onActivityClick = onNavigateToActivity,
                onSettingsClick = onNavigateToSettings,
                onPlusClick = { isMenuOpen = !isMenuOpen },
                isKilnsActive = true,
                isProgramsActive = false,
                isActivityActive = false,
                isSettingsActive = false,
                showFab = true,
                isMenuOpen = isMenuOpen,
                modifier = Modifier.align(alignment = Alignment.BottomStart)
            )
        }
        
        // Home Indicator
        HomeIndicatorWhite(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 0.dp, y = 810.dp)
        )
    }
}

