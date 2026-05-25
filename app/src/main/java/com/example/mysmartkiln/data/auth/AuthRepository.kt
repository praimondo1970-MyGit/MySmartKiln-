package com.example.mysmartkiln.data.auth

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/**
 * Repositorio que gestiona la lógica de autenticación
 */
class AuthRepository(context: Context) {
    
    private val authManager = FirebaseAuthManager()
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "MySmartKilnPrefs", 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_SAVED_EMAIL = "saved_email"
        private const val KEY_SAVED_PASSWORD = "saved_password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    /**
     * Inicia sesión con email y contraseña
     */
    suspend fun login(
        email: String, 
        password: String,
        rememberMe: Boolean = false
    ): AuthResult {
        return try {
            val result = authManager.signInWithEmail(email, password)
            
            if (result.isSuccess) {
                val user = result.getOrNull()
                
                // Guardar preferencias de "Recordarme"
                saveRememberMePreferences(rememberMe, email, password)
                
                AuthResult(
                    isSuccess = true,
                    message = "Inicio de sesión exitoso",
                    userId = user?.uid,
                    email = user?.email
                )
            } else {
                val exception = result.exceptionOrNull()
                AuthResult(
                    isSuccess = false,
                    message = getErrorMessage(exception)
                )
            }
        } catch (e: Exception) {
            AuthResult(
                isSuccess = false,
                message = getErrorMessage(e)
            )
        }
    }
    
    /**
     * Registra un nuevo usuario
     */
    suspend fun register(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String? = null
    ): AuthResult {
        // Validaciones
        if (email.isBlank()) {
            return AuthResult(false, "El correo electrónico es requerido")
        }
        
        if (!isValidEmail(email)) {
            return AuthResult(false, "El correo electrónico no es válido")
        }
        
        if (password.isBlank()) {
            return AuthResult(false, "La contraseña es requerida")
        }
        
        if (password.length < 6) {
            return AuthResult(false, "La contraseña debe tener al menos 6 caracteres")
        }
        
        if (password != confirmPassword) {
            return AuthResult(false, "Las contraseñas no coinciden")
        }
        
        return try {
            val result = authManager.signUpWithEmail(email, password, displayName)
            
            if (result.isSuccess) {
                val user = result.getOrNull()
                AuthResult(
                    isSuccess = true,
                    message = "Registro exitoso",
                    userId = user?.uid,
                    email = user?.email
                )
            } else {
                val exception = result.exceptionOrNull()
                AuthResult(
                    isSuccess = false,
                    message = getErrorMessage(exception)
                )
            }
        } catch (e: Exception) {
            AuthResult(
                isSuccess = false,
                message = getErrorMessage(e)
            )
        }
    }
    
    /**
     * Envía email para restablecer contraseña
     */
    suspend fun resetPassword(email: String): AuthResult {
        if (email.isBlank()) {
            return AuthResult(false, "El correo electrónico es requerido")
        }
        
        if (!isValidEmail(email)) {
            return AuthResult(false, "El correo electrónico no es válido")
        }
        
        return try {
            val result = authManager.sendPasswordResetEmail(email)
            
            if (result.isSuccess) {
                AuthResult(
                    isSuccess = true,
                    message = "Correo de restablecimiento enviado. Revisa tu bandeja de entrada."
                )
            } else {
                val exception = result.exceptionOrNull()
                AuthResult(
                    isSuccess = false,
                    message = getErrorMessage(exception)
                )
            }
        } catch (e: Exception) {
            AuthResult(
                isSuccess = false,
                message = getErrorMessage(e)
            )
        }
    }
    
    /**
     * Cierra sesión
     */
    fun logout() {
        authManager.signOut()
        prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply()
    }
    
    /**
     * Verifica si el usuario está autenticado
     */
    fun isUserLoggedIn(): Boolean {
        return authManager.isUserLoggedIn
    }
    
    /**
     * Obtiene el email del usuario actual
     */
    fun getCurrentUserEmail(): String? {
        return authManager.getCurrentUserEmail()
    }
    
    /**
     * Obtiene el ID del usuario actual
     */
    fun getCurrentUserId(): String? {
        return authManager.getCurrentUserId()
    }
    
    /**
     * Verifica si "Recordarme" está activado
     */
    fun isRememberMeEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMEMBER_ME, false)
    }
    
    /**
     * Obtiene el email guardado
     */
    fun getSavedEmail(): String? {
        return prefs.getString(KEY_SAVED_EMAIL, null)
    }
    
    /**
     * Guarda las preferencias de "Recordarme"
     */
    private fun saveRememberMePreferences(rememberMe: Boolean, email: String, password: String = "") {
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            putString(KEY_SAVED_EMAIL, if (rememberMe) email else null)
            putString(KEY_SAVED_PASSWORD, if (rememberMe) password else null)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }
    
    /**
     * Obtiene la contraseña guardada
     */
    fun getSavedPassword(): String? {
        return prefs.getString(KEY_SAVED_PASSWORD, null)
    }
    
    /**
     * Valida formato de email
     */
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Convierte excepciones de Firebase en mensajes legibles
     */
    private fun getErrorMessage(exception: Throwable?): String {
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> "Credenciales inválidas. Verifica tu correo y contraseña."
            is FirebaseAuthInvalidUserException -> "No existe una cuenta con este correo electrónico."
            is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con este correo electrónico."
            is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil. Usa al menos 6 caracteres."
            else -> exception?.message ?: "Error desconocido. Intenta nuevamente."
        }
    }
}


