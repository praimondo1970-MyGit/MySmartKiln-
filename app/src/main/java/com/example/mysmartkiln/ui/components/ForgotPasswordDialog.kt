package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.DialogBrand
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.RalewayFontFamily

@Composable
fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    val kilnColors = LocalKilnColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = kilnColors.cardBackground,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Restablecer Contraseña",
                color = kilnColors.title,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = RalewayFontFamily
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Ingresa tu correo electrónico y te enviaremos un enlace para restablecer tu contraseña.",
                    color = kilnColors.body,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = RalewayFontFamily
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = {
                        Text(
                            text = "Correo electrónico",
                            color = kilnColors.muted,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontFamily = RalewayFontFamily
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = kilnColors.title,
                        unfocusedTextColor = kilnColors.title,
                        focusedLabelColor = kilnColors.muted,
                        unfocusedLabelColor = kilnColors.muted,
                        focusedIndicatorColor = DialogBrand.primaryButton(),
                        unfocusedIndicatorColor = kilnColors.cardBorder,
                        cursorColor = DialogBrand.primaryButton(),
                        focusedContainerColor = kilnColors.cardBackground,
                        unfocusedContainerColor = kilnColors.cardBackground
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (email.isNotBlank()) {
                        onConfirm(email)
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DialogBrand.primaryButton(),
                    contentColor = kilnColors.onAccent
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = kilnColors.onAccent,
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Text(
                        text = "Enviar",
                        color = kilnColors.onAccent,
                        style = TextStyle(
                            fontFamily = RalewayFontFamily,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text(
                    text = "Cancelar",
                    color = kilnColors.muted,
                    style = TextStyle(
                        fontFamily = RalewayFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    )
}
