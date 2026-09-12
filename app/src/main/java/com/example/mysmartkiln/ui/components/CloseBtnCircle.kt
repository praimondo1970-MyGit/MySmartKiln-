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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

/**
 * Botón circular de cerrar (X).
 * Se muestra cuando el menú flotante está abierto.
 */
@Composable
fun CloseBtnCircle(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val kilnColors = LocalKilnColors.current

    IconButton(
        onClick = onClick,
        modifier = modifier
            .requiredSize(size = 50.dp)
    ) {
        Box(
            modifier = Modifier
                .requiredSize(size = 50.dp)
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(size = 50.dp)
                    .clip(shape = CircleShape)
                    .background(color = kilnColors.muted)
            )

            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.close_icon),
                contentDescription = "close-icon",
                tint = kilnColors.onFab,
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .requiredSize(size = 24.dp)
            )
        }
    }
}

@Preview(widthDp = 50, heightDp = 50)
@Composable
private fun CloseBtnCirclePreview() {
    MySmartKilnTheme {
        CloseBtnCircle()
    }
}
