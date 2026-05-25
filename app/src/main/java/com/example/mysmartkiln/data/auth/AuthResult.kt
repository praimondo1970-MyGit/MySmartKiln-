package com.example.mysmartkiln.data.auth

/**
 * Clase sellada para representar el estado de autenticación
 */
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val userId: String, val email: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

/**
 * Clase para representar el resultado de operaciones de autenticación
 */
data class AuthResult(
    val isSuccess: Boolean,
    val message: String? = null,
    val userId: String? = null,
    val email: String? = null
)














































