package com.example.mysmartkiln.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Texto humano para “hace cuánto se perdió la telemetría del ESP”.
 */
object LinkLostFormat {

    /** Misma ventana que el detalle del horno (badge DESCONECTADO). */
    const val STALE_MS = 30_000L

    fun formatSince(sinceMs: Long, nowMs: Long = System.currentTimeMillis()): String {
        if (sinceMs <= 0L) return ""
        val elapsedMs = (nowMs - sinceMs).coerceAtLeast(0L)
        val sec = elapsedMs / 1000L
        return when {
            sec < 60 -> "hace un momento"
            sec < 3600 -> {
                val m = sec / 60
                if (m == 1L) "hace 1 minuto" else "hace $m minutos"
            }
            sec < 86_400 -> {
                val h = sec / 3600
                if (h == 1L) "hace 1 hora" else "hace $h horas"
            }
            sec < 86_400 * 7 -> {
                val d = sec / 86_400
                if (d == 1L) "hace 1 día" else "hace $d días"
            }
            else -> {
                val fmt = SimpleDateFormat("d/M/yyyy", Locale.getDefault())
                "desde ${fmt.format(Date(sinceMs))}"
            }
        }
    }
}
