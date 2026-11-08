package com.example.mysmartkiln

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.mysmartkiln.ui.screens.SignInScreen
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme
import com.example.mysmartkiln.ui.theme.ThemePreferences

class SignInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MySmartKilnTheme(themeMode = ThemePreferences.read(this)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SignInScreen(
                        onSignInSuccess = {
                            // Navegar a MainActivity después del registro exitoso
                            navigateToMain()
                        },
                        onLoginClick = {
                            // Navegar a la pantalla de login
                            navigateToLogin()
                        }
                    )
                }
            }
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
