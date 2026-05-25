package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun Group41(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
                .requiredSize(size = 64.dp)
        ) {
        Box(
            modifier = Modifier
                        .requiredSize(size = 64.dp)
                        .clip(shape = RoundedCornerShape(12.dp))
                        .background(color = AppColors.Primary400))
        Box(
            modifier = Modifier
                        .align(alignment = Alignment.Center) // Centrar horizontalmente el contenedor interno
                        .offset(y = 4.dp) // Mover todo el contenido más abajo
                        .requiredWidth(width = 36.dp)
                        .requiredHeight(height = 48.dp)
            ) {
            Icon(
                painter = painterResource(id = R.drawable.typedeletesize16colorblack),
                contentDescription = "Icon",
                modifier = Modifier
                                .align(alignment = Alignment.TopCenter) // Centrar horizontalmente el icono
                                .requiredWidth(width = 25.dp)
                                .requiredHeight(height = 24.dp))
            Text(
                text = "borrar",
                color = AppColors.White,
                lineHeight = 1.em,
                style = TextStyle(
                                fontSize = 10.sp),
                modifier = Modifier
                                .align(alignment = Alignment.BottomCenter) // Centrado horizontal, texto abajo
                                .offset(x = 6.dp, y = 2.dp) // Mover texto 6dp a la derecha y 2dp más abajo
                                .requiredWidth(width = 36.dp)
                                .requiredHeight(height = 23.dp))
            }
        }
 }

@Preview(widthDp = 64, heightDp = 64)
@Composable
private fun Group41Preview() {
    MySmartKilnTheme {
        Group41(Modifier)
    }
 }








