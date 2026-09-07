package com.example.mysmartkiln.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors

/**
 * El ESP apaga su SoftAP ~12–14 s tras una sesión HTTP local (perfil B).
 * Si el usuario intenta operar antes, explicamos la espera en lugar de
 * mostrar «Conéctate al horno» o un spinner que falla.
 */
@Composable
fun KilnApReconfigWaitDialog(
    remainingSeconds: Int,
    actionDescription: String,
    onDismiss: () -> Unit
) {
    val secondsLabel = if (remainingSeconds == 1) "1 segundo" else "$remainingSeconds segundos"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Horno reconfigurando la red",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = LocalKilnColors.current.title
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "El horno está reconfigurando la red. Esperá $secondsLabel para $actionDescription.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = LocalKilnColors.current.title,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 24.sp
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Después podrás conectarte de nuevo al horno e intentar la acción.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF475569),
                        lineHeight = 22.sp
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Entendido",
                    color = LocalKilnColors.current.body,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
