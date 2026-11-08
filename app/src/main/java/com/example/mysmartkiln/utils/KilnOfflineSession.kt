package com.example.mysmartkiln.utils

/**
 * Recuerdo en memoria (sesión de app): hornos sin nube donde el usuario eligió
 * priorizar SoftAP hasta que vuelva telemetría Firebase fresca.
 */
object KilnOfflineSession {

    private val preferLocalMacs = mutableSetOf<String>()

    fun markPreferLocal(mac: String) {
        val key = normalize(mac)
        if (key.isNotEmpty()) preferLocalMacs.add(key)
    }

    fun clearPreferLocal(mac: String) {
        preferLocalMacs.remove(normalize(mac))
    }

    fun clearAllPreferLocal() {
        preferLocalMacs.clear()
    }

    fun prefersLocal(mac: String): Boolean =
        preferLocalMacs.contains(normalize(mac))

    private fun normalize(mac: String): String =
        mac.replace(":", "").replace("-", "").uppercase()
}
