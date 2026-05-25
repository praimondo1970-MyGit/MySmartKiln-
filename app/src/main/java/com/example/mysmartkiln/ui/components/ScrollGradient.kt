package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.ui.theme.AppColors

/**
 * Componente que agrega un gradiente al final de contenido scrolleable
 * @param height Altura del gradiente (default: 40dp)
 * @param backgroundColor Color de fondo del gradiente (default: Primary100)
 */
@Composable
fun ScrollGradient(
    height: androidx.compose.ui.unit.Dp = 40.dp,
    backgroundColor: Color = AppColors.Primary100,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor.copy(alpha = 0f), // Transparente en la parte superior
                        backgroundColor.copy(alpha = 1f)  // Opaco en la parte inferior
                    )
                )
            )
    )
}

/**
 * Componente que envuelve contenido scrolleable con gradiente
 * @param content Contenido scrolleable
 * @param gradientHeight Altura del gradiente (default: 40dp)
 * @param backgroundColor Color de fondo del gradiente (default: Primary100)
 */
@Composable
fun ScrollableWithGradient(
    content: @Composable () -> Unit,
    gradientHeight: androidx.compose.ui.unit.Dp = 40.dp,
    backgroundColor: Color = AppColors.Primary100,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Contenido principal
        content()
        
        // Gradiente superpuesto en la parte inferior
        ScrollGradient(
            height = gradientHeight,
            backgroundColor = backgroundColor,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}













































