package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.ui.theme.AppShadows

/**
 * Utility functions for applying shadows to components
 */

/**
 * Apply shadow to a surface with different shadow levels
 */
@Composable
fun ShadowedSurface(
    modifier: Modifier = Modifier,
    shadowLevel: ShadowLevel = ShadowLevel.Medium,
    shape: Shape = RectangleShape,
    color: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.shadow(
            elevation = when (shadowLevel) {
                ShadowLevel.None -> 0.dp
                ShadowLevel.XS -> AppShadows.ShadowXS
                ShadowLevel.Small -> AppShadows.ShadowSM
                ShadowLevel.Medium -> AppShadows.ShadowMD
                ShadowLevel.Large -> AppShadows.ShadowLG
                ShadowLevel.XL -> AppShadows.ShadowXL
            },
            shape = shape
        ),
        shape = shape,
        color = color
    ) {
        content()
    }
}

/**
 * Apply shadow to a card with different shadow levels
 */
@Composable
fun ShadowedCard(
    modifier: Modifier = Modifier,
    shadowLevel: ShadowLevel = ShadowLevel.Medium,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    colors: androidx.compose.material3.CardColors = CardDefaults.cardColors(),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = shape,
        colors = colors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = when (shadowLevel) {
                ShadowLevel.None -> 0.dp
                ShadowLevel.XS -> AppShadows.ShadowXS
                ShadowLevel.Small -> AppShadows.ShadowSM
                ShadowLevel.Medium -> AppShadows.ShadowMD
                ShadowLevel.Large -> AppShadows.ShadowLG
                ShadowLevel.XL -> AppShadows.ShadowXL
            }
        )
    ) {
        content()
    }
}

/**
 * Apply shadow to an icon
 */
@Composable
fun ShadowedIcon(
    modifier: Modifier = Modifier,
    shadowLevel: ShadowLevel = ShadowLevel.Small,
    shape: Shape = androidx.compose.foundation.shape.CircleShape,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.shadow(
            elevation = when (shadowLevel) {
                ShadowLevel.None -> 0.dp
                ShadowLevel.XS -> AppShadows.IconsShadow
                ShadowLevel.Small -> AppShadows.IconsShadow
                ShadowLevel.Medium -> AppShadows.ShadowSM
                ShadowLevel.Large -> AppShadows.ShadowMD
                ShadowLevel.XL -> AppShadows.ShadowLG
            },
            shape = shape
        ),
        shape = shape,
        color = Color.Transparent
    ) {
        content()
    }
}

/**
 * Shadow levels available in the design system
 */
enum class ShadowLevel {
    None,
    XS,
    Small,
    Medium,
    Large,
    XL
}

/**
 * Extension function to apply shadow to any composable
 */
fun Modifier.appShadow(
    shadowLevel: ShadowLevel = ShadowLevel.Medium,
    shape: Shape = RectangleShape
): Modifier {
    return this.shadow(
        elevation = when (shadowLevel) {
            ShadowLevel.None -> 0.dp
            ShadowLevel.XS -> AppShadows.ShadowXS
            ShadowLevel.Small -> AppShadows.ShadowSM
            ShadowLevel.Medium -> AppShadows.ShadowMD
            ShadowLevel.Large -> AppShadows.ShadowLG
            ShadowLevel.XL -> AppShadows.ShadowXL
        },
        shape = shape
    )
}

