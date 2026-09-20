package com.example.mysmartkiln.utils

/**
 * Estado de enlace remoto mostrado en la tarjeta de lista (WiFi / Firebase).
 */
enum class HybridConnectionState {
    WIFI_ONLINE,
    WIFI_NO_INTERNET,
    WIFI_FIREBASE_DOWN,
    OFFLINE
}
