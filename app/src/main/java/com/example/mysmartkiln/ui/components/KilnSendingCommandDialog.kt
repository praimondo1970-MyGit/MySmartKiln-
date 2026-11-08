package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mysmartkiln.ui.theme.DialogBrand
import com.example.mysmartkiln.ui.theme.LocalKilnColors

/**
 * Diálogo modal con un círculo girando que se muestra mientras la app se
 * conecta al AP del horno y envía un comando (START / PAUSE / STOP / programa / configuración).
 */
@Composable
fun KilnSendingCommandDialog(
    phase: String,
    detail: String? = null
) {
    val kilnColors = LocalKilnColors.current
    Dialog(
        onDismissRequest = { /* no-op: bloqueamos el cierre */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = kilnColors.cardBackground,
            tonalElevation = 0.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, kilnColors.cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(72.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = DialogBrand.primaryButton(),
                        strokeWidth = 5.dp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = phase,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = kilnColors.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )

                if (!detail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = kilnColors.muted,
                            fontSize = 13.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
