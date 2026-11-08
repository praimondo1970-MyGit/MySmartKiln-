package com.example.mysmartkiln.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Colores de botones primarios de diálogo / auth coherentes con Estado (INICIAR).
 */
object DialogBrand {
    val PrimaryEsp = Color(0xFFFF7811)

    @Composable
    fun primaryButton(): Color {
        val colors = LocalKilnColors.current
        return if (colors.isEsp) PrimaryEsp else colors.accent
    }
}
