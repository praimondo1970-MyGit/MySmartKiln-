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
import com.example.mysmartkiln.ui.components.BottomNavBarBackground
import com.example.mysmartkiln.ui.components.ScrollGradient
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors

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
            BottomNavBarBackground(
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 0.dp, y = 60.dp)
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

