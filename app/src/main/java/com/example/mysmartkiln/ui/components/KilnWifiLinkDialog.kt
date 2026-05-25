package com.example.mysmartkiln.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.AppColors

/**
 * Paso inicial: instrucciones para conectarse al WiFi AP del horno.
 * La búsqueda visual ocurre en KilnsScreen (overlay tipo escaneo BLE).
 */
@Composable
fun KilnWifiLinkDialog(
    onDismiss: () -> Unit,
    onOpenWifiSettings: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Conecta tu horno",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E3A8A)
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Conéctate al WiFi del horno para vincularlo.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFF1E3A8A),
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "1. Pulsa «Abrir WiFi» y elige la red SmartKiln-XXXX.\n" +
                        "2. Contraseña: últimos 8 caracteres del MAC del horno.\n" +
                        "3. Vuelve a esta app: buscaremos el horno automáticamente.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF475569),
                        lineHeight = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No necesitas internet; solo la red del horno.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Si acabas de guardar otro horno, espera ~10 s a que reaparezca la red SmartKiln.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOpenWifiSettings()
                    context.startActivity(
                        Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            ) {
                Text(
                    text = "Abrir WiFi",
                    color = AppColors.Primary700,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Ahora no",
                    color = Color(0xFF64748B)
                )
            }
        }
    )
}
