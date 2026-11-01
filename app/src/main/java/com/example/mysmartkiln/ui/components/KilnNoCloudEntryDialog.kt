package com.example.mysmartkiln.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.DialogBrand
import com.example.mysmartkiln.ui.theme.LocalKilnColors

/**
 * Aviso al abrir un horno sin telemetría Firebase fresca (chip SIN NUBE).
 * Ofrece control por SoftAP local o entrar solo a mirar.
 */
@Composable
fun KilnNoCloudEntryDialog(
    kilnName: String,
    onConnectLocal: () -> Unit,
    onEnterAnyway: () -> Unit,
    onDismiss: () -> Unit
) {
    val kilnColors = LocalKilnColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = kilnColors.cardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Sin conexión a la nube",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = kilnColors.title
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "«$kilnName» no está publicando datos en internet " +
                        "(sin WiFi usable o sin Firebase reciente).",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = kilnColors.body,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Podés controlarlo cerca uniéndote a su red local SoftAP " +
                        "(SmartKiln-…). Si solo querés abrir la pantalla, elegí «Entrar igual».",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = kilnColors.muted,
                        lineHeight = 22.sp
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConnectLocal) {
                Text(
                    text = "Conectar en red local",
                    color = DialogBrand.primaryButton(),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = onEnterAnyway) {
                    Text(
                        text = "Entrar igual",
                        color = kilnColors.body,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Cancelar",
                        color = kilnColors.muted
                    )
                }
            }
        }
    )
}
