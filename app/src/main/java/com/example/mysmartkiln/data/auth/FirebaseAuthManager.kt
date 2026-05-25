package com.example.mysmartkiln.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

/**
 * Clase que encapsula todas las operaciones de Firebase Authentication
 */
class FirebaseAuthManager {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    /**
     * Obtiene el usuario actual autenticado
     */
    val currentUser: FirebaseUser?
        get() = auth.currentUser
    
    /**
     * Verifica si hay un usuario autenticado
     */
    val isUserLoggedIn: Boolean
        get() = currentUser != null
    
    /**
     * Inicia sesión con email y contraseña
     * @return Result con el FirebaseUser o un error
     */
    suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Error al iniciar sesión: Usuario nulo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Registra un nuevo usuario con email y contraseña
     * @return Result con el FirebaseUser o un error
     */
    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            
            if (user != null) {
                // Actualizar el nombre de usuario si se proporcionó
                displayName?.let {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(it)
                        .build()
                    user.updateProfile(profileUpdates).await()
                }
                Result.success(user)
            } else {
                Result.failure(Exception("Error al registrar usuario: Usuario nulo"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Envía un email para restablecer la contraseña
     * @return Result indicando éxito o error
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Cierra la sesión del usuario actual
     */
    fun signOut() {
        auth.signOut()
    }
    
    /**
     * Obtiene el email del usuario actual
     */
    fun getCurrentUserEmail(): String? {
        return currentUser?.email
    }
    
    /**
     * Obtiene el UID del usuario actual
     */
    fun getCurrentUserId(): String? {
        return currentUser?.uid
    }
    
    /**
     * Obtiene el nombre del usuario actual
     */
    fun getCurrentUserDisplayName(): String? {
        return currentUser?.displayName
    }
    
    /**
     * Verifica si el email del usuario está verificado
     */
    fun isEmailVerified(): Boolean {
        return currentUser?.isEmailVerified ?: false
    }
    
    /**
     * Envía un email de verificación al usuario actual
     */
    suspend fun sendEmailVerification(): Result<Unit> {
        return try {
            currentUser?.sendEmailVerification()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}














































