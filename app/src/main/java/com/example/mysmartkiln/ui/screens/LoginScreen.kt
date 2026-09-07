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
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.mysmartkiln.ui.components.ForgotPasswordDialog
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme
import com.example.mysmartkiln.ui.theme.RalewayFontFamily

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {
},
    onRegisterClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    viewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val authState by viewModel.authState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    
    // Cargar email y contraseña guardados si "Recordarme" está activado
    LaunchedEffect(Unit) {
        viewModel.getSavedEmail()?.let { savedEmail ->
            email = savedEmail
            rememberMe = true
        }
        viewModel.getSavedPassword()?.let { savedPassword ->
            password = savedPassword
        }
    }
    
    // Manejar estado de autenticación
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                onLoginSuccess()
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
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (email.isNotEmpty()) 53.dp else 52.dp),
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
                    lineHeight = if (email.isNotEmpty()) 18.sp else 20.sp
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
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (password.isNotEmpty()) 53.dp else 52.dp),
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
                    lineHeight = if (password.isNotEmpty()) 18.sp else 20.sp
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

            Spacer(modifier = Modifier.height(16.dp))

            // Remember me and Forgot password
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = LocalKilnColors.current.onAccent,
                            uncheckedColor = LocalKilnColors.current.onAccent,
                            checkmarkColor = LocalKilnColors.current.brandSurface
                        )
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Recordarme",
                        color = LocalKilnColors.current.onAccent,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontFamily = RalewayFontFamily
                        )
                    )
                }

                Text(
                    text = "Olvidaste tu contraseña",
                    color = LocalKilnColors.current.onAccent,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontFamily = RalewayFontFamily,
                        textDecoration = TextDecoration.Underline
                    ),
                    modifier = Modifier.clickable { showForgotPasswordDialog = true }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Login button
            Button(
                onClick = {
                    if (email.isNotBlank() && password.isNotBlank()) {
                        viewModel.login(email, password, rememberMe)
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
                        text = "Iniciar Sesión",
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

            // Register text
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(
                        color = LocalKilnColors.current.onAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = RalewayFontFamily
                    )) { append("No tenes una cuenta? ") }
                    withStyle(style = SpanStyle(
                        color = LocalKilnColors.current.onAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = RalewayFontFamily,
                        textDecoration = TextDecoration.Underline
                    )) { append("Registrate") }
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.clickable { onRegisterClick() }
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
        
        // Diálogo de "Olvidaste tu contraseña"
        if (showForgotPasswordDialog) {
            ForgotPasswordDialog(
                onDismiss = { showForgotPasswordDialog = false },
                onConfirm = { resetEmail ->
                    viewModel.resetPassword(resetEmail)
                    showForgotPasswordDialog = false
                },
                isLoading = isLoading
            )
        }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun LoginScreenPreview() {
    MySmartKilnTheme {
        LoginScreen()
    }
}