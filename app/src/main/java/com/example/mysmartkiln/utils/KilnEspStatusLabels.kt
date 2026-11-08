package com.example.mysmartkiln.utils

import androidx.compose.ui.graphics.Color

/**
 * Etiquetas en español para los estados que publica el ESP en Firebase/HTTP.
 * Alineadas con los chips de la lista de hornos (HORNEANDO / PAUSA / …).
 */
object KilnEspStatusLabels {

    fun normalize(raw: String): String = raw.trim().uppercase()

    fun label(raw: String): String = when (normalize(raw)) {
        "CALENTANDO", "RUNNING" -> "Horneando"
        "PAUSADO", "PAUSED", "RECONNECTING" -> "Pausa"
        "DETENIDO", "STOPPED" -> "Inactivo"
        "FINALIZADO", "FINISHED" -> "Finalizado"
        "INTERRUMPIDO" -> "Interrumpido"
        "DESCONECTADO", "DISCONNECTED", "OFFLINE" -> "Desconectado"
        "IDLE", "" -> "Inactivo"
        else -> if (raw.isBlank()) "Inactivo" else raw
    }

    fun chipBackground(raw: String): Color = when (normalize(raw)) {
        "CALENTANDO", "RUNNING" -> Color(0xFFFFEDD5)
        "PAUSADO", "PAUSED", "RECONNECTING" -> Color(0xFFFEF3C7)
        "INTERRUMPIDO" -> Color(0xFFFEE2E2)
        "FINALIZADO", "FINISHED" -> Color(0xFFD1FAE5)
        "DESCONECTADO", "DISCONNECTED", "OFFLINE" -> Color(0xFFE2E8F0)
        else -> Color(0xFFF1F5F9)
    }

    fun chipContent(raw: String): Color = when (normalize(raw)) {
        "CALENTANDO", "RUNNING" -> Color(0xFFC2410C)
        "PAUSADO", "PAUSED", "RECONNECTING" -> Color(0xFFB45309)
        "INTERRUMPIDO" -> Color(0xFFB91C1C)
        "FINALIZADO", "FINISHED" -> Color(0xFF047857)
        "DESCONECTADO", "DISCONNECTED", "OFFLINE" -> Color(0xFF64748B)
        else -> Color(0xFF64748B)
    }

    fun dotColor(raw: String): Color = chipContent(raw)
}
