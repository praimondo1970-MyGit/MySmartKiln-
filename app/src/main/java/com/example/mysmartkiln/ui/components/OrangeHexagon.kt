package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

/**
 * Componente que muestra un hexágono usando una imagen PNG
 */
@Composable
fun OrangeHexagon(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 44.dp,
    color: Color = AppColors.Primary300
) {
    Image(
        painter = painterResource(id = R.drawable.poligono_iconos),
        contentDescription = null,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(color) // Aplica el color al hexágono
    )
}

@Preview(widthDp = 44, heightDp = 44)
@Composable
private fun OrangeHexagonPreview() {
    MySmartKilnTheme {
        OrangeHexagon()
    }
}
