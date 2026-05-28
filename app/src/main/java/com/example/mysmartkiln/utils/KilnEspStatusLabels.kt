package com.example.mysmartkiln.utils

import androidx.compose.ui.graphics.Color

/**
 * Etiquetas en español para los estados que publica el ESP en Firebase/HTTP.
 */
object KilnEspStatusLabels {

    fun normalize(raw: String): String = raw.trim().uppercase()

    fun label(raw: String): String = when (normalize(raw)) {
        "CALENTANDO" -> "Horneando"
        "PAUSADO" -> "Pausado"
        "DETENIDO" -> "Detenido"
        "FINALIZADO" -> "Finalizado"
        "INTERRUMPIDO" -> "Interrumpido"
        "RECONNECTING" -> "Reconectando…"
        "IDLE", "" -> "Inactivo"
        else -> if (raw.isBlank()) "Sin estado" else raw
    }

    fun chipBackground(raw: String): Color = when (normalize(raw)) {
        "CALENTANDO" -> Color(0xFFFFEDD5)
        "PAUSADO" -> Color(0xFFFEF3C7)
        "INTERRUMPIDO" -> Color(0xFFFEE2E2)
        "RECONNECTING" -> Color(0xFFFEF3C7)
        "FINALIZADO" -> Color(0xFFD1FAE5)
        "DETENIDO" -> Color(0xFFF1F5F9)
        else -> Color(0xFFF1F5F9)
    }

    fun chipContent(raw: String): Color = when (normalize(raw)) {
        "CALENTANDO" -> Color(0xFFC2410C)
        "PAUSADO" -> Color(0xFFB45309)
        "INTERRUMPIDO" -> Color(0xFFB91C1C)
        "RECONNECTING" -> Color(0xFFB45309)
        "FINALIZADO" -> Color(0xFF047857)
        "DETENIDO" -> Color(0xFF475569)
        else -> Color(0xFF64748B)
    }

    fun dotColor(raw: String): Color = chipContent(raw)
}
