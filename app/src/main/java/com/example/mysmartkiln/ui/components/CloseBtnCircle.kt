package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

/**
 * Botón circular de cerrar (X) con fondo azul
 * Se muestra cuando el menú flotante está abierto
 */
@Composable
fun CloseBtnCircle(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {
}
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
            // Fondo circular azul
            Box(
                modifier = Modifier
                    .requiredSize(size = 53.dp)
                    .clip(shape = CircleShape)
                    .background(color = LocalKilnColors.current.muted) // Azul claro
            )
            
            // Icono X (cerrar)
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.close_icon),
                contentDescription = "close-icon",
                tint = LocalKilnColors.current.onAccent,
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .requiredSize(size = 26.dp)
            )
        }
    }
}

@Preview(widthDp = 53, heightDp = 53)
@Composable
private fun CloseBtnCirclePreview() {
    MySmartKilnTheme {
        CloseBtnCircle(Modifier)
    }
}

