package com.example.mysmartkiln.utils

import com.example.mysmartkiln.data.local.entity.KilnEntity

/**
 * Distingue DataLogger vs horno.
 * Preferir [KilnEntity.deviceType]; si falta, heurística (volume + manufacturer).
 * En Firebase, [deviceInfo.type] es la fuente canónica: "KILN" | "DATALOGGER".
 */
object DeviceKind {

    fun isDataLogger(kiln: KilnEntity): Boolean {
        val stored = kiln.deviceType.trim().uppercase()
        if (stored == "DATALOGGER") return true
        if (stored == "KILN") return false
        return isDataLoggerHeuristic(kiln)
    }

    /** Fallback para filas Room sin [deviceType] (pre Fase 2). */
    private fun isDataLoggerHeuristic(kiln: KilnEntity): Boolean {
        val vol = kiln.volume.trim()
        val volumeLooksEmpty = vol.isEmpty() || vol == "0" || vol == "0.0"
        if (!volumeLooksEmpty) return false
        val m = kiln.manufacturer.trim().uppercase()
        return m == "K" || m == "S" ||
            m == "TYPE K" || m == "TYPE S" ||
            m == "TIPO K" || m == "TIPO S" ||
            m == "DATALOGGER"
    }

    fun firebaseType(kiln: KilnEntity): String =
        if (isDataLogger(kiln)) "DATALOGGER" else "KILN"

    /** Tipo de termocupla para DataLogger (K/S), o null. */
    fun thermocoupleOrNull(kiln: KilnEntity): String? {
        if (!isDataLogger(kiln)) return null
        val m = kiln.manufacturer.trim().uppercase()
        return when {
            m == "S" || m.contains("S") && !m.contains("K") -> "S"
            m == "K" || m.contains("K") -> "K"
            else -> null
        }
    }
}
