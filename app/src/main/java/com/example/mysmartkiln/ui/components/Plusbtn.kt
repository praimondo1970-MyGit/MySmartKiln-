package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun Plusbtn(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .requiredSize(size = 53.dp)
    ) {
        Box(
            modifier = Modifier
                .requiredSize(size = 53.dp)
        ) {
            // Fondo circular naranja
            Box(
                modifier = Modifier
                    .requiredSize(size = 53.dp)
                    .clip(shape = CircleShape)
                    .background(color = AppColors.color_Terciary_800)
            )
            
            // Icono plus SVG
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.plusicon),
                contentDescription = "plus-icon",
                tint = Color.White,
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .requiredSize(size = 26.dp)
            )
        }
    }
}

@Preview(widthDp = 53, heightDp = 53)
@Composable
private fun PlusbtnPreview() {
    MySmartKilnTheme {
        Plusbtn(Modifier)
    }
}
