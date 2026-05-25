package com.example.mysmartkiln.data.model

/**
 * Modelo de datos para un horno registrado
 * Contiene la información necesaria para conectarse por BLE
 */
data class RegisteredKiln(
    val macAddress: String,        // Identificador único BLE (formato: A4CF128856E8)
    val name: String,               // Nombre personalizado del horno
    val model: String,              // Modelo del horno
    val volume: Float,              // Volumen en litros
    val userId: String,             // ID del usuario propietario
    val registeredAt: Long = System.currentTimeMillis(),  // Timestamp de registro
    val lastConnected: Long = 0L    // Última vez que se conectó
) {
    /**
     * Path en Firebase para este horno
     */
    fun getFirebasePath(): String {
        return "/users/$userId/kilns/$macAddress"
    }
    
    /**
     * Path para realtimeData
     */
    fun getRealtimeDataPath(): String {
        return "${getFirebasePath()}/realtimeData"
    }
    
    /**
     * Path para deviceInfo
     */
    fun getDeviceInfoPath(): String {
        return "${getFirebasePath()}/deviceInfo"
    }
    
    /**
     * Formatea el nombre completo del horno para mostrar en UI
     */
    fun getDisplayName(): String {
        return "$name ($model)"
    }
    
    /**
     * Verifica si el horno fue conectado recientemente (últimas 24 horas)
     */
    fun wasRecentlyConnected(): Boolean {
        val dayInMillis = 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - lastConnected) < dayInMillis
    }
}













































