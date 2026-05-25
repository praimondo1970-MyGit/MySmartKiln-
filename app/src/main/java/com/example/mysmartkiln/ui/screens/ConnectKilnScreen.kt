package com.example.mysmartkiln.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.data.local.KilnDataSource
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel
import com.example.mysmartkiln.utils.WifiUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectKilnScreen(
    kilnName: String,
    viewModel: KilnDetailViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dataSource by viewModel.dataSource.collectAsState()
    val apInfo by viewModel.apInfo.collectAsState()
    val isVerifying by viewModel.isVerifyingConnection.collectAsState()
    val connectionMessage by viewModel.connectionMessage.collectAsState()
    val currentSsid = remember { mutableStateOf(WifiUtils.getCurrentWifiSSID(context)) }

    LaunchedEffect(Unit) {
        viewModel.refreshApInfo()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Primary100)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color(0xFF1E3A8A)
                )
            }
            Text(
                text = "Conectar a $kilnName",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color(0xFF1E3A8A),
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "1. Abre Ajustes WiFi del teléfono",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Conéctate a la red del horno (empieza por SmartKiln-). La contraseña son los últimos 8 caracteres del MAC.",
                    color = Color(0xFF475569),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Abrir ajustes WiFi")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "2. Datos de la red (desde el horno)",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (apInfo != null) {
                    InfoRow("SSID", apInfo!!.ssid)
                    InfoRow("Contraseña", apInfo!!.password)
                    InfoRow("URL", apInfo!!.url)
                } else {
                    Text(
                        text = "Conéctate al WiFi del horno y pulsa «Verificar conexión» para leer SSID y contraseña.",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { viewModel.refreshApInfo() }) {
                    Text("Actualizar datos AP")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val ssidNow = WifiUtils.getCurrentWifiSSID(context)
        currentSsid.value = ssidNow

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "3. Verificar",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A),
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow("WiFi actual", ssidNow ?: "No conectado")
                val connected = dataSource == KilnDataSource.LOCAL_AP
                Text(
                    text = if (connected) "Conectado al horno (HTTP local)" else "Sin conexión HTTP al horno",
                    color = if (connected) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                if (connectionMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = connectionMessage, color = Color(0xFF64748B), fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        currentSsid.value = WifiUtils.getCurrentWifiSSID(context)
                        viewModel.tryConnectLocal()
                    },
                    enabled = !isVerifying,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Primary700)
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Verificar conexión", color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF64748B), fontSize = 14.sp)
        Text(
            text = value,
            color = Color(0xFF1E3A8A),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
    }
}
