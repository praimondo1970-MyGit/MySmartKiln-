package com.example.mysmartkiln.ble

/**
 * Estados de conexión del horno para el sistema híbrido
 */
enum class HybridConnectionState {
    BLE_CONNECTED,      // "Conectado" + icono Bluetooth
    WIFI_ONLINE,        // "Conectado" + icono WiFi (internet + Firebase OK)
    WIFI_NO_INTERNET,   // "Sin internet" + icono WiFi
    WIFI_FIREBASE_DOWN, // "Sin nube" + icono WiFi
    OFFLINE             // "Desconectado" + icono offline
}

/**
 * Prioridades de sincronización
 */
enum class SyncPriority {
    REALTIME_DATA,  // Temperatura, estado del horno
    PROGRAMS,       // Programas guardados
    OTHER_UPDATES   // Configuraciones, logs, etc.
}

/**
 * Datos para sincronización diferida
 */
data class PendingSyncData(
    val priority: SyncPriority,
    val data: Any,
    val timestamp: Long = System.currentTimeMillis()
)












