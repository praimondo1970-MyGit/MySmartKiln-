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
import com.example.mysmartkiln.ui.theme.LocalKilnColors

/**
 * Diálogo que aparece cuando el usuario intenta operar el horno (Iniciar /
 * Pausa / Stop / Cargar programa) desde la vista remota (Firebase). Le indica
 * que debe conectarse al WiFi del horno y le abre los ajustes de WiFi.
 */
@Composable
fun KilnOperateOfflineDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Conéctate al horno",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = LocalKilnColors.current.title
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "No se pudo conectar al WiFi del horno automáticamente. Hazlo manual:",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = LocalKilnColors.current.title,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "1. Pulsa «Abrir WiFi» y elige la red SmartKiln-XXXX.\n" +
                        "2. Contraseña: últimos 8 caracteres del MAC del horno.\n" +
                        "3. Vuelve a la app e intenta el comando otra vez.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF475569),
                        lineHeight = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tras enviar el comando el horno cerrará su WiFi y volverás solo a tu red de casa.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B)
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                    onDismiss()
                }
            ) {
                Text(
                    text = "Abrir WiFi",
                    color = LocalKilnColors.current.body,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = Color(0xFF64748B)
                )
            }
        }
    )
}
