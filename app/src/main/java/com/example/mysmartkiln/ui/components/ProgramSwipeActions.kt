package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun ProgramSwipeActions(
    onDelete: () -> Unit = {
},
    onDuplicate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .requiredWidth(width = 130.dp) // Ancho de acciones de 130dp
            .requiredHeight(height = 93.dp) // Misma altura que la tarjeta
    ) {
        // Botón de Borrar (azul) - alineado a la derecha con offset
        Box(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .offset(x = (-77).dp) // Offset de -77dp desde la derecha (-8dp + -64dp (ancho duplicar) + -5dp)
                .requiredSize(size = 64.dp)
                .clip(shape = RoundedCornerShape(12.dp))
                .background(color = LocalKilnColors.current.muted) // Mismo color que Group41
                .clickable { onDelete() }
        ) {
            Box(
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .offset(y = 4.dp)
                    .requiredWidth(width = 36.dp)
                    .requiredHeight(height = 48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.typedeletesize16colorblack),
                    contentDescription = "Delete",
                    modifier = Modifier
                        .align(alignment = Alignment.TopCenter)
                        .requiredWidth(width = 25.dp)
                        .requiredHeight(height = 24.dp)
                )
                Text(
                    text = "borrar",
                    color = LocalKilnColors.current.onAccent,
                    lineHeight = 1.em,
                    style = TextStyle(fontSize = 10.sp),
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .offset(x = 6.dp, y = 2.dp)
                        .requiredWidth(width = 36.dp)
                        .requiredHeight(height = 23.dp)
                )
            }
        }
        
        // Botón de Duplicar (naranja) - alineado al margen derecho
        Box(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .offset(x = (-3).dp) // 3dp desde el margen derecho
                .requiredSize(size = 64.dp)
                .clip(shape = RoundedCornerShape(12.dp))
                .background(color = LocalKilnColors.current.fab)
                .clickable { onDuplicate() }
        ) {
            Box(
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .offset(y = 4.dp)
                    .requiredWidth(width = 36.dp)
                    .requiredHeight(height = 48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.typeduplicate),
                    contentDescription = "Duplicate",
                    modifier = Modifier
                        .align(alignment = Alignment.TopCenter)
                        .requiredWidth(width = 25.dp)
                        .requiredHeight(height = 24.dp),
                    tint = AppColors.White
                )
                Text(
                    text = "duplicar",
                    color = LocalKilnColors.current.onAccent,
                    lineHeight = 1.em,
                    style = TextStyle(fontSize = 10.sp),
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .offset(x = 2.dp, y = 2.dp)
                        .requiredWidth(width = 36.dp)
                        .requiredHeight(height = 23.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProgramSwipeActionsPreview() {
    MySmartKilnTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(93.dp)
                .background(Color.LightGray)
        ) {
            ProgramSwipeActions()
        }
    }
}
