package com.example.mysmartkiln.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta cruda del display ESP (display_ui / display_lvgl).
 * No usar directamente en pantallas: preferir [KilnColors] semánticos.
 */
object EspBrand {
    val Black = Color(0xFF000000)
    /** Cards: un poco más claras que #161616 para separarlas del fondo. */
    val Card = Color(0xFF2A2A2A)
    val CardBorder = Color(0xFF555555)
    val Divider = Color(0xFF4A4A4A)
    val DarkGray = Color(0xFF404040)
    val TrackGray = Color(0xFF333333)
    val MidGray = Color(0xFF888888)
    val LightGray = Color(0xFFB0B0B0)
    val White = Color(0xFFFFFFFF)

    /** Naranja marca (antes Material Deep Orange #FF5722, muy rojizo en pantalla). */
    val Orange = Color(0xFFFF9800)
    val OrangeDim = Color(0xFFF57C00)
    val OrangeLight = Color(0xFFFFB74D)
    val OrangeReddish = Color(0xFFE65100)

    val Green = Color(0xFF8BC34A)
    val Yellow = Color(0xFFFFCC00)
    val Red = Color(0xFFFF4444)
}
