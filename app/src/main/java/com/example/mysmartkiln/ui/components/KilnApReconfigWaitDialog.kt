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
 * El ESP apaga su SoftAP ~8–10 s tras una sesión HTTP local (perfil C).
 */
@Composable
fun KilnApReconfigWaitDialog(
    remainingSeconds: Int,
    actionDescription: String,
    onDismiss: () -> Unit
) {
    val kilnColors = LocalKilnColors.current
    val secondsLabel = if (remainingSeconds == 1) "1 segundo" else "$remainingSeconds segundos"

    AlertDialog(
        // Solo se cierra con «Entendido» (no tap fuera ni atrás).
        onDismissRequest = {},
        containerColor = kilnColors.cardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Horno reconfigurando la red",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = kilnColors.title
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "El horno está reconfigurando la red. Esperá $secondsLabel para $actionDescription.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = kilnColors.body,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Después podrás conectarte de nuevo al horno e intentar la acción.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = kilnColors.muted,
                        lineHeight = 22.sp
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Entendido",
                    color = DialogBrand.primaryButton(),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
