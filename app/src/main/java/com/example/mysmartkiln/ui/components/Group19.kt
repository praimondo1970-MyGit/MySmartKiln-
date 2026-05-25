package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun Group19(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
                .requiredWidth(width = 319.dp)
                .requiredHeight(height = 74.dp)
        ) {
        Box(
            modifier = Modifier
                        .requiredWidth(width = 319.dp)
                        .requiredHeight(height = 74.dp)
            ) {
            Text(
                text = "Nombre",
                color = AppColors.color_Primary_600,
                lineHeight = 1.em,
                style = TextStyle(
                                fontSize = 16.sp),
                modifier = Modifier
                                .align(alignment = Alignment.TopStart)
                                .offset(x = 1.dp,
                                                y = 0.dp))
            Box(
                modifier = Modifier
                                .align(alignment = Alignment.TopStart)
                                .offset(x = 0.dp,
                                                y = 22.dp)
                                .requiredWidth(width = 319.dp)
                                .requiredHeight(height = 52.dp)
                                .clip(shape = RoundedCornerShape(12.dp))
                                .background(color = AppColors.color_Primary_50)
                                .border(border = BorderStroke(1.dp, AppColors.Primary100),
                                                shape = RoundedCornerShape(12.dp))
                                .shadow(elevation = 15.dp,
                                                shape = RoundedCornerShape(12.dp)))
            }
        }
 }

@Preview(widthDp = 319, heightDp = 74)
@Composable
private fun Group19Preview() {
    MySmartKilnTheme {
        Group19(Modifier)
    }
 }