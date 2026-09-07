package com.example.mysmartkiln.ui.components

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.data.local.KilnApiDefaults
import com.example.mysmartkiln.utils.WifiUtils
import kotlinx.coroutines.delay

@Composable
fun WifiScanDialog(
    onNetworkSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var networks by remember { mutableStateOf<List<String>>(emptyList()) }
    var isScanning by remember { mutableStateOf(true) }
    
    // Escanear redes WiFi al abrir el diálogo
    LaunchedEffect(Unit) {
        delay(500) // Pequeño delay para iniciar el escaneo
        networks = WifiUtils.getAvailableNetworks(context)
            .filter { ssid ->
                !ssid.startsWith(KilnApiDefaults.SMART_KILN_SSID_PREFIX, ignoreCase = true)
            }
        isScanning = false
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = LocalKilnColors.current.cardBackground
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Título
                Text(
                    text = "Red con acceso a internet",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = LocalKilnColors.current.title,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Lista de redes o loading
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    when {
                        isScanning -> {
                            // Indicador de carga
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = LocalKilnColors.current.body,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Escaneando redes WiFi...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = LocalKilnColors.current.title
                                    )
                                )
                            }
                        }
                        
                        networks.isEmpty() -> {
                            // Sin redes encontradas
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.icon_search),
                                    contentDescription = "No hay redes",
                                    tint = LocalKilnColors.current.title,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No se encontraron redes WiFi",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = LocalKilnColors.current.title,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Asegúrate de tener el WiFi activado",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF666666),
                                        textAlign = TextAlign.Center
                                    )
                                )
                            }
                        }
                        
                        else -> {
                            // Lista de redes
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(networks) { ssid ->
                                    NetworkItem(
                                        ssid = ssid,
                                        onClick = {
                                            onNetworkSelected(ssid)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Botón Cancelar
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE5E7EB),
                        contentColor = LocalKilnColors.current.title
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Cancelar",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkItem(
    ssid: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF3F4F6),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.icon_wifi),
                    contentDescription = "WiFi",
                    tint = LocalKilnColors.current.title,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = ssid,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = LocalKilnColors.current.title,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            Icon(
                painter = painterResource(R.drawable.icon_arrow_right),
                contentDescription = "Seleccionar",
                tint = LocalKilnColors.current.title,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}












































