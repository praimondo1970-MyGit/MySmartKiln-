package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import com.example.mysmartkiln.R
import com.example.mysmartkiln.data.auth.AuthState
import com.example.mysmartkiln.data.auth.AuthViewModel
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme
import com.example.mysmartkiln.ui.theme.RalewayFontFamily

@Composable
fun SignInScreen(
    onSignInSuccess: () -> Unit = {
},
    onLoginClick: () -> Unit = {},
    viewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    val authState by viewModel.authState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    
    // Manejar estado de autenticación
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                onSignInSuccess()
            }
            is AuthState.Error -> {
                val errorMessage = (authState as AuthState.Error).message
                snackbarHostState.showSnackbar(errorMessage)
            }
            else -> {}
        }
    }
    
    // Mostrar mensajes
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = LocalKilnColors.current.brandSurface)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        // Snackbar para mensajes
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(120.dp))

            // SmartKiln Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.offset(x = (-20).dp)
            ) {
                // Logo icon
                Image(
                    painter = painterResource(id = R.drawable.logoicon),
                    contentDescription = "Logo Icon",
                    modifier = Modifier.size(60.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // SmartKiln text
                Text(
                    text = "SmartKiln",
                    color = LocalKilnColors.current.onAccent,
                    style = TextStyle(
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RalewayFontFamily
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = {
                    Text(
                        text = "Correo electrónico",
                        color = LocalKilnColors.current.onAccent,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = RalewayFontFamily
                        ),
                        modifier = Modifier.offset(y = (-1).dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(53.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                keyboardActions = KeyboardActions(
                    onDone = { /* Handle done action */ }
                ),
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
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = RalewayFontFamily,
                    baselineShift = androidx.compose.ui.text.style.BaselineShift(-0.15f)
                ),
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.mail),
                        contentDescription = "Email Icon",
                        tint = LocalKilnColors.current.onAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Spacer(modifier = Modifier.height(21.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = {
                    Text(
                        text = "Contraseña",
                        color = LocalKilnColors.current.onAccent,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = RalewayFontFamily
                        ),
                        modifier = Modifier.offset(y = (-1).dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(53.dp),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = RalewayFontFamily,
                    baselineShift = androidx.compose.ui.text.style.BaselineShift(-0.15f)
                ),
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.lock),
                        contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                        tint = LocalKilnColors.current.onAccent,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { passwordVisible = !passwordVisible }
                    )
                }
            )

            Spacer(modifier = Modifier.height(21.dp))

            // Confirm Password field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = {
                    Text(
                        text = "Confirmar contraseña",
                        color = LocalKilnColors.current.onAccent,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = RalewayFontFamily
                        ),
                        modifier = Modifier.offset(y = (-1).dp)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(53.dp),
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontFamily = RalewayFontFamily,
                    baselineShift = androidx.compose.ui.text.style.BaselineShift(-0.15f)
                ),
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.lock),
                        contentDescription = if (confirmPasswordVisible) "Ocultar confirmación" else "Mostrar confirmación",
                        tint = LocalKilnColors.current.onAccent,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { confirmPasswordVisible = !confirmPasswordVisible }
                    )
                }
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Sign In button
            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank()) {
                        viewModel.register(email, password, confirmPassword)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(49.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LocalKilnColors.current.accent
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = LocalKilnColors.current.onAccent,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "Registrarse",
                        color = LocalKilnColors.current.onAccent,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = RalewayFontFamily
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Login text
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(
                        color = LocalKilnColors.current.onAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = RalewayFontFamily
                    )) { append("¿Tenes una cuenta? ") }
                    withStyle(style = SpanStyle(
                        color = LocalKilnColors.current.onAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RalewayFontFamily,
                        textDecoration = TextDecoration.Underline
                    )) { append("Log in") }
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { onLoginClick() }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Home indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .padding(horizontal = 150.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            color = LocalKilnColors.current.onAccent,
                            shape = RoundedCornerShape(2.dp)
                        )
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SignInScreenPreview() {
    MySmartKilnTheme {
        SignInScreen()
    }
}
