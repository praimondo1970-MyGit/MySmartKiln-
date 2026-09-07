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
    val isEsp: Boolean
) {
    companion object {
        val Classic = KilnColors(
            screenBackground = AppColors.Primary100,
            cardBackground = AppColors.White,
            cardBorder = AppColors.Primary200,
            title = Color(0xFF1E3A8A),
            body = AppColors.Primary700,
            muted = AppColors.Primary400,
            accent = AppColors.Primary700,
            accentMuted = AppColors.Primary600,
            onAccent = AppColors.White,
            navBar = AppColors.Primary900,
            fab = AppColors.Tertiary900,
            onFab = AppColors.White,
            track = Color.DarkGray.copy(alpha = 0.4f),
            progress = AppColors.Primary700,
            success = AppColors.Success600,
            warning = AppColors.Warning700,
            danger = AppColors.Error600,
            isEsp = false
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
            navBar = Color(0xFF1F1F1F),
            fab = EspBrand.Orange,
            onFab = EspBrand.White,
            track = EspBrand.TrackGray,
            progress = EspBrand.Orange,
            success = EspBrand.Green,
            warning = EspBrand.Yellow,
            danger = EspBrand.Red,
            isEsp = true
        )

        fun forMode(mode: ThemeMode): KilnColors = when (mode) {
            ThemeMode.CLASSIC -> Classic
            ThemeMode.ESP -> Esp
        }
    }
}

val LocalKilnColors = staticCompositionLocalOf { KilnColors.Classic }
