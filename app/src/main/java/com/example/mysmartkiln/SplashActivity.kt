package com.example.mysmartkiln

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.mysmartkiln.data.auth.AuthRepository
import com.example.mysmartkiln.data.auth.FirebaseAuthManager
import com.example.mysmartkiln.ui.screens.AfterSplashDestination
import com.example.mysmartkiln.ui.screens.SplashScreen
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme
import com.example.mysmartkiln.ui.theme.ThemePreferences

class SplashActivity : ComponentActivity() {

    private val authManager = FirebaseAuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAuthenticationStatus()
    }

    private fun checkAuthenticationStatus() {
        val authRepository = AuthRepository(applicationContext)
        val afterLoading = when {
            authManager.isUserLoggedIn -> AfterSplashDestination.Main
            authRepository.isRememberMeEnabled() -> AfterSplashDestination.Login
            else -> AfterSplashDestination.GetStarted
        }

        setContent {
            MySmartKilnTheme(themeMode = ThemePreferences.read(this)) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SplashScreen(
                        afterLoading = afterLoading,
                        // Sesión activa: siempre Main (no re-ramificar a Login)
                        onNavigateToMain = { navigateToMain() },
                        onNavigateToLogin = { navigateToLogin() },
                        // Get Started → Comenzar: sin sesión va a Login
                        onGetStarted = {
                            if (authManager.isUserLoggedIn) {
                                navigateToMain()
                            } else {
                                navigateToLogin()
                            }
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
