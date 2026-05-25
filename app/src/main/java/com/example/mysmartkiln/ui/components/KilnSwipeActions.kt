package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
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
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun KilnSwipeActions(
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .requiredWidth(width = 160.dp) // Ancho de acciones de 160dp para coincidir con el desplazamiento
            .requiredHeight(height = 235.dp) // Misma altura que la tarjeta del horno
    ) {
        // Botón de Borrar (azul) - alineado a la derecha con offset
        Box(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .offset(x = (-83).dp) // Offset ajustado para mostrar ambos botones claramente (64dp editar + 8dp espacio + 5dp margen + 6dp)
                .requiredSize(size = 64.dp)
                .clip(shape = RoundedCornerShape(12.dp))
                .background(color = AppColors.Primary400) // Mismo color que Group41
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
                    color = AppColors.White,
                    lineHeight = 1.em,
                    style = TextStyle(fontSize = 12.sp),
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .offset(x = 6.dp, y = 2.dp)
                        .requiredWidth(width = 36.dp)
                        .requiredHeight(height = 23.dp)
                )
            }
        }
        
        // Botón de Editar (naranja) - alineado al margen derecho
        Box(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .offset(x = (-8).dp) // 8dp desde el margen derecho para mejor visibilidad
                .requiredSize(size = 64.dp)
                .clip(shape = RoundedCornerShape(12.dp))
                .background(color = AppColors.color_Terciary_800) // Mismo color que el botón de duplicar
                .clickable { onEdit() }
        ) {
            Box(
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .offset(y = 4.dp)
                    .requiredWidth(width = 36.dp)
                    .requiredHeight(height = 48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_edit),
                    contentDescription = "Edit",
                    modifier = Modifier
                        .align(alignment = Alignment.TopCenter)
                        .requiredWidth(width = 25.dp)
                        .requiredHeight(height = 24.dp),
                    tint = AppColors.White
                )
                Text(
                    text = "editar",
                    color = AppColors.White,
                    lineHeight = 1.em,
                    style = TextStyle(fontSize = 12.sp),
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
fun KilnSwipeActionsPreview() {
    MySmartKilnTheme {
        Box(
            modifier = Modifier
                .requiredWidth(160.dp)
                .requiredHeight(195.dp)
                .background(Color.LightGray)
        ) {
            KilnSwipeActions()
        }
    }
}

