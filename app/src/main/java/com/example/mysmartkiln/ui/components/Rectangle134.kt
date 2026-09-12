package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun Rectangle134(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val kilnColors = LocalKilnColors.current
    if (kilnColors.isEsp) {
        val shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        Box(
            modifier = modifier
                .fillMaxWidth()
                .requiredHeight(348.dp)
                .clip(shape)
                .background(kilnColors.cardBackground, shape)
                .border(1.dp, kilnColors.cardBorder, shape)
        ) {
            // Handle estilo sheet ESP
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(kilnColors.muted.copy(alpha = 0.55f))
            )
            content()
        }
    } else {
        val shape = RoundedCornerShape(
            topStart = 60.dp,
            topEnd = 60.dp,
            bottomStart = 0.dp,
            bottomEnd = 0.dp
        )
        Box(
            modifier = modifier
                .fillMaxWidth()
                .requiredHeight(348.dp)
                .shadow(elevation = 15.dp, shape = shape)
                .background(color = kilnColors.cardBackground, shape = shape)
        ) {
            content()
        }
    }
}

@Preview(widthDp = 390, heightDp = 348)
@Composable
private fun Rectangle134Preview() {
    MySmartKilnTheme {
        Rectangle134()
    }
}
