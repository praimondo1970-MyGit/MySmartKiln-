package com.example.mysmartkiln.ui.theme

import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.unit.dp

object AppShadows {
    // Shadow sizes from the project
    val IconsShadow = 10.dp
    val ShadowXS = 12.dp
    val ShadowSM = 12.dp
    val ShadowMD = 16.dp
    val ShadowLG = 20.dp
    val ShadowXL = 24.dp
    
    // Shadow effects with blur, offset and alpha
    val ShadowNone = Shadow(
        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0f),
        offset = androidx.compose.ui.geometry.Offset(0f, 0f),
        blurRadius = 0f
    )
    
    val ShadowXS_Effect = Shadow(
        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f),
        offset = androidx.compose.ui.geometry.Offset(0f, 2f),
        blurRadius = 4f
    )
    
    val ShadowSM_Effect = Shadow(
        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f),
        offset = androidx.compose.ui.geometry.Offset(0f, 2f),
        blurRadius = 6f
    )
    
    val ShadowMD_Effect = Shadow(
        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f),
        offset = androidx.compose.ui.geometry.Offset(0f, 4f),
        blurRadius = 8f
    )
    
    val ShadowLG_Effect = Shadow(
        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f),
        offset = androidx.compose.ui.geometry.Offset(0f, 6f),
        blurRadius = 12f
    )
    
    val ShadowXL_Effect = Shadow(
        color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.1f),
        offset = androidx.compose.ui.geometry.Offset(0f, 8f),
        blurRadius = 16f
    )
}
