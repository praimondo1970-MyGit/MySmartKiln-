package com.example.mysmartkiln.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta cruda del display ESP (display_ui / display_lvgl).
 * No usar directamente en pantallas: preferir [KilnColors] semánticos.
 *
 * Jerarquía de grises (mockup / captura):
 * - Fondo pantalla ≈ #121212
 * - Menú inferior ≈ #161616 (casi fundido al fondo)
 * - Tarjetas ≈ #1C1C1C (un escalón más claras)
 * - Chips / pills ≈ #363636
 * - Bordes muy sutiles ≈ #2C2C2C
 */
object EspBrand {
    val Black = Color(0xFF121212)
    /** Cards: un escalón más claras que el fondo, sin saltar a gris medio. */
    val Card = Color(0xFF1C1C1C)
    val CardBorder = Color(0xFF2C2C2C)
    val Divider = Color(0xFF2A2A2A)
    val DarkGray = Color(0xFF404040)
    /** Pills de estado (p.ej. Desconectado). */
    val TrackGray = Color(0xFF363636)
    val MidGray = Color(0xFF888888)
    val LightGray = Color(0xFFB0B0B0)
    val White = Color(0xFFFFFFFF)

    /** Naranja marca. */
    val Orange = Color(0xFFFF9800)
    val OrangeDim = Color(0xFFF57C00)
    val OrangeLight = Color(0xFFFFB74D)
    val OrangeReddish = Color(0xFFE65100)

    val Green = Color(0xFF8BC34A)
    val Yellow = Color(0xFFFFCC00)
    val Red = Color(0xFFFF4444)

    val NavBar = Color(0xFF161616)

    /**
     * Espejo luminoso de la misma jerarquía (misma lógica de escalones, invertida).
     * Misma marca naranja / semáforos; no cambia el modo Oscuro.
     */
    object Light {
        /** Fondo: near-white (espejo de #121212). */
        val Screen = Color(0xFFF5F5F5)
        /** Nav: un peldaño respecto al fondo (espejo de #161616). */
        val NavBar = Color(0xFFEBEBEB)
        /** Cards: más claras que el fondo (espejo de card > screen). */
        val Card = Color(0xFFFFFFFF)
        val CardBorder = Color(0xFFE0E0E0)
        val Divider = Color(0xFFE6E6E6)
        val TrackGray = Color(0xFFD6D6D6)
        val MidGray = Color(0xFF757575)
        val BodyGray = Color(0xFF5C5C5C)
        val Ink = Color(0xFF121212)
        /** Separadores de panel (espejo de #5C5C5C en oscuro). */
        val PanelLine = Color(0xFFD0D0D0)
    }
}
