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
import com.example.mysmartkiln.data.auth.FirebaseAuthManager
import com.example.mysmartkiln.ui.screens.LoginScreen
import com.example.mysmartkiln.ui.screens.SplashScreen
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme
import com.example.mysmartkiln.ui.theme.ThemePreferences

class SplashActivity : ComponentActivity() {
    
    private val authManager = FirebaseAuthManager()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Verificar si el usuario está autenticado
        checkAuthenticationStatus()
    }
    
    private fun checkAuthenticationStatus() {
        setContent {
            MySmartKilnTheme(themeMode = ThemePreferences.read(this)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplashScreen(
                        onNavigateToMain = {
                            // Después de la animación del splash, verificar autenticación
                            if (authManager.isUserLoggedIn) {
                                navigateToMain()
                            } else {
                                navigateToLogin()
                            }
                        },
                        onNavigateToLogin = {
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
    
    private fun navigateToSignIn() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}

