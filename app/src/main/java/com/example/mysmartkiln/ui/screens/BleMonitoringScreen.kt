package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ble.BleViewModel
import com.example.mysmartkiln.ble.ConnectionMode
import com.example.mysmartkiln.ble.ConnectionState
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors

/**
 * Pantalla de monitoreo con BLE continuo durante programa activo
 * Incluye reconexión automática y fallback a Firebase
 */
@Composable
fun BleMonitoringScreen(
    macAddress: String,
    bleViewModel: BleViewModel,
    onProgramFinished: () -> Unit = {
},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val connectionState by bleViewModel.connectionState.collectAsState()
    val temperature by bleViewModel.temperature.collectAsState()
    val status by bleViewModel.status.collectAsState()
    val reconnectionAttempts by bleViewModel.reconnectionAttempts.collectAsState()
    val connectionMode by bleViewModel.connectionMode.collectAsState()
    
    // Iniciar modo continuo al entrar
    LaunchedEffect(macAddress) {
        bleViewModel.startContinuousMode(macAddress)
    }
    
    // Detener modo continuo al salir o cuando programa termina
    LaunchedEffect(status) {
        if (status == "FINALIZADO") {
            bleViewModel.stopContinuousMode()
            onProgramFinished()
        }
    }
    
    // Limpiar al salir
    DisposableEffect(Unit) {
        onDispose {
            bleViewModel.stopContinuousMode()
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(LocalKilnColors.current.screenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header con estado de conexión
            ConnectionStatusHeader(
                connectionState = connectionState,
                reconnectionAttempts = reconnectionAttempts,
                connectionMode = connectionMode
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Datos del horno en tiempo real
            RealtimeDataSection(
                temperature = temperature,
                status = status,
                isConnected = connectionState is ConnectionState.Connected
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Controles
            if (status !in listOf("FINALIZADO", "DETENIDO")) {
                ControlButtons(
                    onPause = { bleViewModel.sendCommand("PAUSE") },
                    onStop = { bleViewModel.sendCommand("STOP") },
                    enabled = connectionState is ConnectionState.Connected
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Información de conexión
            ConnectionInfoCard(
                macAddress = macAddress,
                connectionMode = connectionMode,
                reconnectionAttempts = reconnectionAttempts
            )
        }
    }
}

@Composable
private fun ConnectionStatusHeader(
    connectionState: ConnectionState,
    reconnectionAttempts: Int,
    connectionMode: ConnectionMode
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (connectionState) {
                is ConnectionState.Connected -> LocalKilnColors.current.success.copy(alpha = 0.18f)
                is ConnectionState.Connecting, is ConnectionState.Reconnecting -> LocalKilnColors.current.warning.copy(alpha = 0.18f)
                else -> LocalKilnColors.current.danger.copy(alpha = 0.18f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Indicador visual
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = when (connectionState) {
                                is ConnectionState.Connected -> LocalKilnColors.current.success
                                is ConnectionState.Connecting, is ConnectionState.Reconnecting -> LocalKilnColors.current.warning
                                else -> LocalKilnColors.current.danger
                            },
                            shape = CircleShape
                        )
                )
                
                Column {
                    Text(
                        text = when (connectionState) {
                            is ConnectionState.Connected -> "Conectado via BLE"
                            is ConnectionState.Connecting -> "Conectando..."
                            is ConnectionState.Reconnecting -> "Reconectando... ($reconnectionAttempts/10)"
                            else -> "Desconectado"
                        },
                        fontWeight = FontWeight.Bold,
                        color = LocalKilnColors.current.title
                    )
                    
                    if (connectionMode is ConnectionMode.Continuous) {
                        Text(
                            text = "Modo continuo activo",
                            style = MaterialTheme.typography.bodySmall,
                            color = LocalKilnColors.current.body,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            
            // Indicador de modo
            if (connectionState is ConnectionState.Connected) {
                Text(
                    text = "⚡ 1s",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalKilnColors.current.success,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RealtimeDataSection(
    temperature: Float,
    status: String,
    isConnected: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LocalKilnColors.current.cardBackground
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Temperatura
            Text(
                text = if (isConnected) "${temperature.toInt()}°C" else "--°C",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = if (isConnected) LocalKilnColors.current.title else LocalKilnColors.current.muted,
                fontSize = 64.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Estado
            Text(
                text = if (isConnected) status else "Desconocido",
                style = MaterialTheme.typography.titleMedium,
                color = if (isConnected) LocalKilnColors.current.accent else LocalKilnColors.current.muted
            )
        }
    }
}

@Composable
private fun ControlButtons(
    onPause: () -> Unit,
    onStop: () -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onPause,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalKilnColors.current.warning,
                contentColor = Color.Black
            )
        ) {
            Text("Pausar")
        }
        
        Button(
            onClick = onStop,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = LocalKilnColors.current.danger
            )
        ) {
            Text("Detener")
        }
    }
    
    if (!enabled) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "⚠️ Acércate al horno para usar los controles",
            style = MaterialTheme.typography.bodySmall,
            color = LocalKilnColors.current.danger,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ConnectionInfoCard(
    macAddress: String,
    connectionMode: ConnectionMode,
    reconnectionAttempts: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = LocalKilnColors.current.cardBackground
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Información de Conexión",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = LocalKilnColors.current.title
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            InfoRow("MAC Address", macAddress)
            InfoRow("Modo", when (connectionMode) {
                is ConnectionMode.Continuous -> "Continuo (programa activo)"
                is ConnectionMode.OnDemand -> "Bajo demanda"
            })
            
            if (reconnectionAttempts > 0) {
                InfoRow("Intentos de reconexión", "$reconnectionAttempts/10")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = LocalKilnColors.current.body
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = LocalKilnColors.current.title
        )
    }
}













































