package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.AppColors
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
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalKilnColors.current.brandSurface,
        title = {
            Text(
                text = "Restablecer Contraseña",
                color = LocalKilnColors.current.onAccent,
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
                    color = LocalKilnColors.current.onAccent,
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
                            color = LocalKilnColors.current.onAccent,
                            style = TextStyle(
                                fontSize = 14.sp,
                                fontFamily = RalewayFontFamily
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = LocalKilnColors.current.onAccent,
                        unfocusedTextColor = LocalKilnColors.current.onAccent,
                        focusedLabelColor = LocalKilnColors.current.onAccent,
                        unfocusedLabelColor = LocalKilnColors.current.onAccent,
                        focusedIndicatorColor = LocalKilnColors.current.onAccent,
                        unfocusedIndicatorColor = LocalKilnColors.current.onAccent,
                        cursorColor = LocalKilnColors.current.onAccent,
                        focusedContainerColor = LocalKilnColors.current.brandSurface,
                        unfocusedContainerColor = LocalKilnColors.current.brandSurface
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
                    containerColor = LocalKilnColors.current.accent
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = LocalKilnColors.current.onAccent,
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Text(
                        text = "Enviar",
                        color = LocalKilnColors.current.onAccent,
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
                    color = LocalKilnColors.current.onAccent,
                    style = TextStyle(
                        fontFamily = RalewayFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    )
}














































