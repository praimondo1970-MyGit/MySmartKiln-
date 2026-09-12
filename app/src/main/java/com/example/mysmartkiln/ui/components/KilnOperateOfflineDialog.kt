package com.example.mysmartkiln.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.DialogBrand
import com.example.mysmartkiln.ui.theme.LocalKilnColors

/**
 * Diálogo cuando el usuario intenta operar el horno desde la vista remota (Firebase).
 */
@Composable
fun KilnOperateOfflineDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val kilnColors = LocalKilnColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = kilnColors.cardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Conéctate al horno",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = kilnColors.title
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "No se pudo conectar al WiFi del horno automáticamente. Hazlo manual:",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = kilnColors.body,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "1. Pulsa «Abrir WiFi» y elige la red SmartKiln-XXXX.\n" +
                        "2. Contraseña: últimos 8 caracteres del MAC del horno.\n" +
                        "3. Vuelve a la app e intenta el comando otra vez.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = kilnColors.muted,
                        lineHeight = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tras enviar el comando el horno cerrará su WiFi y volverás solo a tu red de casa.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = kilnColors.muted
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
                    color = DialogBrand.primaryButton(),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancelar",
                    color = kilnColors.muted
                )
            }
        }
    )
}
