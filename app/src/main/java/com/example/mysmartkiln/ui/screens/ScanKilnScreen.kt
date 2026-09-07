package com.example.mysmartkiln.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.ble.BleViewModel
import com.example.mysmartkiln.ble.ConnectionState
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Pantalla para escanear y conectar a hornos BLE
 * Modo OnDemand: conecta solo para configuración
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScanKilnScreen(
    viewModel: BleViewModel,
    onKilnRegistered: (String) -> Unit = {
},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val lastError by viewModel.lastError.collectAsState()
    
    var showConfigDialog by remember { mutableStateOf(false) }
    var selectedMac by remember { mutableStateOf<String?>(null) }
    
    // Permisos BLE
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        listOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }
    
    val permissionsState = rememberMultiplePermissionsState(permissions)
    
    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
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
            // Header
            Text(
                text = "Buscar Hornos",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = LocalKilnColors.current.title
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botón de escaneo
            Button(
                onClick = { 
                    if (permissionsState.allPermissionsGranted) {
                        viewModel.scanDevices()
                    } else {
                        permissionsState.launchMultiplePermissionRequest()
                    }
                },
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalKilnColors.current.accent
                )
            ) {
                Text(
                    if (!permissionsState.allPermissionsGranted) "Activar Permisos"
                    else if (isScanning) "Escaneando..."
                    else "Buscar Hornos"
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Mensaje de error
            lastError?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Text(
                        text = error,
                        color = Color(0xFFC62828),
                        modifier = Modifier.padding(16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Lista de dispositivos
            if (isScanning) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LocalKilnColors.current.body)
                }
            } else if (scannedDevices.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = LocalKilnColors.current.cardBackground
                    )
                ) {
                    Text(
                        text = "No se encontraron hornos.\nPresiona 'Buscar Hornos' para escanear.",
                        modifier = Modifier.padding(16.dp),
                        color = LocalKilnColors.current.body
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scannedDevices) { device ->
                        KilnDeviceCard(
                            deviceName = device.name ?: "Desconocido",
                            macAddress = device.address,
                            onClick = {
                                selectedMac = device.address
                                showConfigDialog = true
                            }
                        )
                    }
                }
            }
        }
        
        // Diálogo de configuración
        if (showConfigDialog && selectedMac != null) {
            ConfigureKilnDialog(
                macAddress = selectedMac!!,
                viewModel = viewModel,
                onDismiss = { showConfigDialog = false },
                onSuccess = { mac ->
                    showConfigDialog = false
                    onKilnRegistered(mac)
                }
            )
        }
    }
}

@Composable
private fun KilnDeviceCard(
    deviceName: String,
    macAddress: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = LocalKilnColors.current.cardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = LocalKilnColors.current.title
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "MAC: $macAddress",
                style = MaterialTheme.typography.bodySmall,
                color = LocalKilnColors.current.body
            )
        }
    }
}

@Composable
private fun ConfigureKilnDialog(
    macAddress: String,
    viewModel: BleViewModel,
    onDismiss: () -> Unit,
    onSuccess: (String) -> Unit
) {
    var kilnName by remember { mutableStateOf("") }
    var kilnModel by remember { mutableStateOf("SmartKiln Pro") }
    var kilnVolume by remember { mutableStateOf("50") }
    var isLoading by remember { mutableStateOf(false) }
    
    val connectionState by viewModel.connectionState.collectAsState()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Horno") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = kilnName,
                    onValueChange = { kilnName = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = kilnModel,
                    onValueChange = { kilnModel = it },
                    label = { Text("Modelo") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = kilnVolume,
                    onValueChange = { kilnVolume = it },
                    label = { Text("Volumen (L)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isLoading) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                
                when (connectionState) {
                    is ConnectionState.Connecting -> {
                        Text("Conectando...", color = LocalKilnColors.current.body)
                    }
                    is ConnectionState.Connected -> {
                        Text("✅ Enviando configuración...", color = Color(0xFF4CAF50))
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // TODO: Implementar envío de KILN_INFO
                    isLoading = true
                    // Por ahora solo simula
                    onSuccess(macAddress)
                },
                enabled = !isLoading && kilnName.isNotBlank()
            ) {
                Text("Registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

