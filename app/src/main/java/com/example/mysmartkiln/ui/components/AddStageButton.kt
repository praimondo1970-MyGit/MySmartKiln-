package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun AddStageButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .requiredSize(size = 48.dp)
    ) {
        Box(
            modifier = Modifier
                .requiredSize(size = 48.dp)
        ) {
            // Fondo circular naranja
            Box(
                modifier = Modifier
                    .requiredSize(size = 48.dp)
                    .clip(shape = CircleShape)
                    .background(color = AppColors.color_Terciary_800)
            )
            
            // Icono plus SVG
            Image(
                painter = painterResource(id = R.drawable.plusicon),
                contentDescription = "add-stage-icon",
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .requiredSize(size = 20.dp)
            )
        }
    }
}

@Preview(widthDp = 48, heightDp = 48)
@Composable
private fun AddStageButtonPreview() {
    MySmartKilnTheme {
        AddStageButton(Modifier)
    }
}
