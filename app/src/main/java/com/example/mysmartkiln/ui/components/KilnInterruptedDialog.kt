package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors

/**
 * Diálogo modal que aparece cuando el horno reporta status="INTERRUMPIDO".
 * Esto sucede cuando el ESP se reinició mientras estaba CALENTANDO o
 * PAUSADO (ej: corte de luz). El firmware persiste el segmento/fase en NVS
 * y al boot deja el horno en este estado especial esperando una decisión.
 *
 * El usuario tiene dos opciones:
 *  - Reanudar: el ESP retoma el programa desde el segmento/fase guardados.
 *  - Cancelar: el ESP limpia el run_state y vuelve a IDLE.
 *
 * El diálogo no se puede cerrar con back ni con tap fuera porque queremos
 * que el usuario tome una decisión consciente.
 */
@Composable
fun KilnInterruptedDialog(
    programName: String,
    onResume: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* no-op: bloqueamos el cierre */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = LocalKilnColors.current.onAccent,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Programa interrumpido",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = AppColors.Warning800,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                val description = if (programName.isNotBlank()) {
                    "El horno se reinició durante la cocción de «$programName». ¿Querés reanudar desde donde quedó o cancelar el programa?"
                } else {
                    "El horno se reinició durante una cocción en curso. ¿Querés reanudar desde donde quedó o cancelar el programa?"
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF334155),
                        fontSize = 14.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onResume,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LocalKilnColors.current.accent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Reanudar cocción",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.Error600
                    )
                ) {
                    Text(
                        text = "Cancelar programa",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
