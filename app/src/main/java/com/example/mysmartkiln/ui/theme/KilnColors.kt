package com.example.mysmartkiln.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    CLASSIC,
    ESP
}

/**
 * Colores semánticos de la app. Las pantallas deben usar esto
 * (vía [LocalKilnColors]) en lugar de hardcodear Primary100 / White.
 *
 * [isEsp] controla el lenguaje visual / layouts (misma UI ESP en Claro y Oscuro).
 * [isDark] distingue Oscuro vs luminoso para tokens que lo necesiten.
 */
data class KilnColors(
    val screenBackground: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val title: Color,
    val body: Color,
    val muted: Color,
    val accent: Color,
    val accentMuted: Color,
    val onAccent: Color,
    val navBar: Color,
    val fab: Color,
    val onFab: Color,
    val track: Color,
    val progress: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    /** Separadores internos de paneles (resumen de estado, etc.). */
    val panelLine: Color,
    val isEsp: Boolean,
    val isDark: Boolean
) {
    companion object {
        /**
         * Claro = misma UI ESP que Oscuro, paleta luminosa espejo.
         * isEsp=true para no bifurcar layouts al viejo “classic”.
         */
        val Classic = KilnColors(
            screenBackground = EspBrand.Light.Screen,
            cardBackground = EspBrand.Light.Card,
            cardBorder = EspBrand.Light.CardBorder,
            title = EspBrand.Light.Ink,
            body = EspBrand.Light.BodyGray,
            muted = EspBrand.Light.MidGray,
            accent = EspBrand.Orange,
            accentMuted = EspBrand.OrangeDim,
            onAccent = EspBrand.White,
            navBar = EspBrand.Light.NavBar,
            fab = EspBrand.Orange,
            onFab = EspBrand.White,
            track = EspBrand.Light.TrackGray,
            progress = EspBrand.Orange,
            success = EspBrand.Green,
            warning = EspBrand.OrangeDim,
            danger = EspBrand.Red,
            panelLine = EspBrand.Light.PanelLine,
            isEsp = true,
            isDark = false
        )

        val Esp = KilnColors(
            screenBackground = EspBrand.Black,
            cardBackground = EspBrand.Card,
            cardBorder = EspBrand.CardBorder,
            title = EspBrand.White,
            body = EspBrand.LightGray,
            muted = EspBrand.MidGray,
            accent = EspBrand.Orange,
            accentMuted = EspBrand.OrangeDim,
            onAccent = EspBrand.White,
            navBar = EspBrand.NavBar,
            fab = EspBrand.Orange,
            onFab = EspBrand.White,
            track = EspBrand.TrackGray,
            progress = EspBrand.Orange,
            success = EspBrand.Green,
            warning = EspBrand.Yellow,
            danger = EspBrand.Red,
            panelLine = Color(0xFF5C5C5C),
            isEsp = true,
            isDark = true
        )

        fun forMode(mode: ThemeMode): KilnColors = when (mode) {
            ThemeMode.CLASSIC -> Classic
            ThemeMode.ESP -> Esp
        }
    }
}

val LocalKilnColors = staticCompositionLocalOf { KilnColors.Classic }
