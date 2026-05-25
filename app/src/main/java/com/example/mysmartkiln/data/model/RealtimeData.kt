package com.example.mysmartkiln.data.model

/**
 * Modelo de datos en tiempo real del horno
 * Contiene temperatura, estado y timestamp para detección de conexión
 */
data class RealtimeData(
    val temperature: Float = 0f,
    val status: String = "",
    val timestamp: Long = 0L  // Unix timestamp en segundos
) {
    /**
     * Verifica si el dispositivo está conectado basándose en el timestamp
     * Considera desconectado si no se actualiza en más de 30 segundos
     */
    fun isConnected(currentTimeSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        return (currentTimeSeconds - timestamp) < 30
    }
    
    /**
     * Retorna el estado del horno o "Desconocido" si está desconectado
     */
    fun getDisplayStatus(currentTimeSeconds: Long = System.currentTimeMillis() / 1000): String {
        return if (isConnected(currentTimeSeconds)) {
            status
        } else {
            "Desconocido"
        }
    }
    
    /**
     * Retorna la temperatura formateada con validación de conexión
     */
    fun getFormattedTemperature(currentTimeSeconds: Long = System.currentTimeMillis() / 1000): String {
        return if (isConnected(currentTimeSeconds)) {
            "${temperature.toInt()}°C"
        } else {
            "--°C"
        }
    }
}













































