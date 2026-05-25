package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mysmartkiln.data.viewmodel.RealtimeDataViewModel
import com.example.mysmartkiln.ui.components.RealtimeDataCard
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.theme.AppColors

/**
 * Pantalla de monitoreo en tiempo real del horno
 * Muestra temperatura, estado y conexión en tiempo real desde Firebase
 */
@Composable
fun MonitoringScreen(
    macAddress: String,
    onNavigateToKilns: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: RealtimeDataViewModel = viewModel()
) {
    // Observar datos en tiempo real
    val realtimeData by viewModel.realtimeData.collectAsState()
    val isDeviceConnected by viewModel.isDeviceConnected.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    // Iniciar observación cuando se carga la pantalla
    LaunchedEffect(macAddress) {
        viewModel.startObserving(macAddress)
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColors.Primary100)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp) // Espacio para la navegación
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Primary700)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Monitoreo del Horno",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            // Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    // Tarjeta de datos en tiempo real
                    RealtimeDataCard(
                        realtimeData = realtimeData,
                        isConnected = isDeviceConnected,
                        isLoading = isLoading
                    )
                }
                
                // Mensaje de error si existe
                errorMessage?.let { error ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .background(
                                    color = Color(0xFFFFEBEE),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = error,
                                color = Color(0xFFC62828),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Información adicional
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Información de Conexión",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.Primary800,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        InfoRow(
                            label = "Dirección MAC",
                            value = macAddress
                        )
                        
                        realtimeData?.let { data ->
                            InfoRow(
                                label = "Última actualización",
                                value = formatTimestamp(data.timestamp)
                            )
                            
                            InfoRow(
                                label = "Tiempo transcurrido",
                                value = getTimeElapsed(data.timestamp)
                            )
                        }
                    }
                }
            }
        }
        
        // Bottom Navigation
        ResponsiveBottomNavigation(
            onKilnsClick = onNavigateToKilns,
            onProgramsClick = onNavigateToPrograms,
            onActivityClick = onNavigateToActivity,
            onSettingsClick = onNavigateToSettings,
            onPlusClick = {},
            isKilnsActive = false,
            isProgramsActive = false,
            isActivityActive = false,
            isSettingsActive = false,
            showFab = false,
            isMenuOpen = false,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.Primary600
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = AppColors.Primary800
        )
    }
}

/**
 * Formatea el timestamp Unix a formato legible
 */
private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Nunca"
    
    val date = java.util.Date(timestamp * 1000) // Convertir a milisegundos
    val formatter = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
    return formatter.format(date)
}

/**
 * Calcula el tiempo transcurrido desde el timestamp
 */
private fun getTimeElapsed(timestamp: Long): String {
    if (timestamp == 0L) return "N/A"
    
    val currentTime = System.currentTimeMillis() / 1000
    val elapsed = currentTime - timestamp
    
    return when {
        elapsed < 60 -> "$elapsed segundos"
        elapsed < 3600 -> "${elapsed / 60} minutos"
        elapsed < 86400 -> "${elapsed / 3600} horas"
        else -> "${elapsed / 86400} días"
    }
}













































