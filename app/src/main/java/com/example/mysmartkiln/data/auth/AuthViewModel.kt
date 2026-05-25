package com.example.mysmartkiln.data.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.mysmartkiln.utils.UserIdUtils

/**
 * ViewModel para gestionar el estado de autenticación en la UI
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = AuthRepository(application)
    
    // Estado de autenticación
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // Mensaje de error o éxito
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    
    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    /**
     * Inicia sesión con email y contraseña
     */
    fun login(email: String, password: String, rememberMe: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading
            
            val result = repository.login(email, password, rememberMe)
            
            _isLoading.value = false
            
            if (result.isSuccess) {
                _authState.value = AuthState.Success(
                    userId = result.userId ?: "",
                    email = result.email ?: ""
                )
                _message.value = result.message
            } else {
                _authState.value = AuthState.Error(result.message ?: "Error al iniciar sesión")
                _message.value = result.message
            }
        }
    }
    
    /**
     * Registra un nuevo usuario
     */
    fun register(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = AuthState.Loading
            
            val result = repository.register(email, password, confirmPassword, displayName)
            
            _isLoading.value = false
            
            if (result.isSuccess) {
                _authState.value = AuthState.Success(
                    userId = result.userId ?: "",
                    email = result.email ?: ""
                )
                _message.value = result.message
            } else {
                _authState.value = AuthState.Error(result.message ?: "Error al registrar")
                _message.value = result.message
            }
        }
    }
    
    /**
     * Envía email para restablecer contraseña
     */
    fun resetPassword(email: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = repository.resetPassword(email)
            
            _isLoading.value = false
            _message.value = result.message
            
            if (!result.isSuccess) {
                _authState.value = AuthState.Error(result.message ?: "Error al enviar correo")
            }
        }
    }
    
    /**
     * Cierra sesión
     */
    fun logout() {
        repository.logout()
        _authState.value = AuthState.Idle
        _message.value = null
    }
    
    /**
     * Verifica si el usuario está autenticado
     */
    fun checkAuthStatus() {
        if (repository.isUserLoggedIn()) {
            val email = repository.getCurrentUserEmail()
            val userId = repository.getCurrentUserId()
            _authState.value = AuthState.Success(
                userId = userId ?: "",
                email = email ?: ""
            )
        } else {
            _authState.value = AuthState.Idle
        }
    }
    
    /**
     * Obtiene el email guardado (si "Recordarme" está activado)
     */
    fun getSavedEmail(): String? {
        return if (repository.isRememberMeEnabled()) {
            repository.getSavedEmail()
        } else {
            null
        }
    }
    
    /**
     * Obtiene la contraseña guardada (si "Recordarme" está activado)
     */
    fun getSavedPassword(): String? {
        return if (repository.isRememberMeEnabled()) {
            repository.getSavedPassword()
        } else {
            null
        }
    }
    
    /**
     * Limpia el mensaje
     */
    fun clearMessage() {
        _message.value = null
    }
    
    /**
     * Reinicia el estado a Idle
     */
    fun resetState() {
        _authState.value = AuthState.Idle
        _message.value = null
    }
    
    /**
     * Obtiene el UID normalizado del usuario actual para Firebase
     */
    fun getCurrentNormalizedUserId(): String {
        val email = repository.getCurrentUserEmail()
        return UserIdUtils.getCurrentUserId(email)
    }
    
    /**
     * Obtiene el email del usuario actual
     */
    fun getCurrentUserEmail(): String? {
        return repository.getCurrentUserEmail()
    }
}


