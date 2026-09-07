package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun Rectangle134(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth() // Adapts to screen width
            .requiredHeight(height = 348.dp) // Height from SVG viewBox
            .shadow(
                elevation = 15.dp,
                shape = RoundedCornerShape(
                    topStart = 60.dp,  // 60dp radius for top corners
                    topEnd = 60.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
            .background(
                color = LocalKilnColors.current.screenBackground, // Changed to Primary100
                shape = RoundedCornerShape(
                    topStart = 60.dp,
                    topEnd = 60.dp,
                    bottomStart = 0.dp,
                    bottomEnd = 0.dp
                )
            )
    ) {
        content()
    }
}

@Preview(widthDp = 390, heightDp = 348)
@Composable
private fun Rectangle134Preview() {
    MySmartKilnTheme {
        Rectangle134(Modifier)
    }
}
