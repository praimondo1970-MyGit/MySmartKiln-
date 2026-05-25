package com.example.mysmartkiln.utils

/**
 * Utilidades para manejo de User IDs en Firebase
 */
object UserIdUtils {
    
    /**
     * Normaliza un email o UID para ser válido en rutas de Firebase
     * Reemplaza caracteres no válidos (@ y .) con _
     * 
     * @param emailOrUid Email o UID a normalizar
     * @return UID normalizado válido para Firebase
     */
    fun normalizeUserId(emailOrUid: String): String {
        return emailOrUid
            .replace("@", "_")
            .replace(".", "_")
            .replace(" ", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("/", "_")
    }
    
    /**
     * Verifica si un UID es válido para rutas de Firebase
     * 
     * @param uid UID a verificar
     * @return true si es válido, false si no
     */
    fun isValidFirebaseUid(uid: String): Boolean {
        return uid.matches(Regex("^[a-zA-Z0-9_-]+$"))
    }
    
    /**
     * Obtiene el UID normalizado del usuario actual
     * Si no hay usuario autenticado, retorna un UID por defecto
     * 
     * @param currentUserEmail Email del usuario actual (puede ser null)
     * @return UID normalizado para usar en Firebase
     */
    fun getCurrentUserId(currentUserEmail: String?): String {
        return if (currentUserEmail != null) {
            normalizeUserId(currentUserEmail)
        } else {
            "anonymous_user"
        }
    }
}





































