package com.example.mysmartkiln.ui.components

import com.example.mysmartkiln.ui.theme.LocalKilnColors

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun SystemKeyboardsIPhoneLightNumbers(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
                .requiredWidth(width = 388.dp)
                .requiredHeight(height = 301.dp)
                .background(color = LocalKilnColors.current.navBar)
        ) {
        Box(
            modifier = Modifier
                        .fillMaxSize()
            ) {
            Box(
                modifier = Modifier
                                .fillMaxSize()
                                .background(color = LocalKilnColors.current.cardBorder))
            }
        Box(
            modifier = Modifier
                        .align(alignment = Alignment.TopStart)
                        .offset(x = 6.21.dp,
                                    y = 6.21.dp)
                        .requiredWidth(width = 376.dp)
                        .requiredHeight(height = 214.dp)
            ) {
            Box(
                modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 260.7359848022461.dp,
                                                end = -6.208000183105469.dp,
                                                top = 6.206185340881348.dp,
                                                bottom = 160.3264455795288.dp)
                ) {
                Box(
                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = LocalKilnColors.current.cardBackground)
                                        .clip(shape = RoundedCornerShape(5.dp)))
                Text(
                    text = "3",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 25.sp,
                                        letterSpacing = 0.29.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                Text(
                    text = "DEF",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                }
            Box(
                modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 133.4720001220703.dp,
                                                end = 121.05598449707031.dp,
                                                top = 6.206185340881348.dp,
                                                bottom = 160.3264455795288.dp)
                ) {
                Box(
                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = LocalKilnColors.current.cardBackground)
                                        .clip(shape = RoundedCornerShape(5.dp)))
                Text(
                    text = "2",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 25.sp,
                                        letterSpacing = 0.29.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                Text(
                    text = "ABC",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                }
            Box(
                modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 6.208000183105469.dp,
                                                end = 248.31998443603516.dp,
                                                top = 6.206185340881348.dp,
                                                bottom = 160.3264455795288.dp)
                ) {
                Box(
                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = LocalKilnColors.current.cardBackground)
                                        .clip(shape = RoundedCornerShape(5.dp)))
                Text(
                    text = "1",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 25.sp,
                                        letterSpacing = 0.29.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                Text(
                    text = " ",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                }
            Box(
                modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 260.7359848022461.dp,
                                                end = -6.208000183105469.dp,
                                                top = 61.027493476867676.dp,
                                                bottom = 104.470778465271.dp)
                ) {
                Box(
                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = LocalKilnColors.current.cardBackground)
                                        .clip(shape = RoundedCornerShape(5.dp)))
                Text(
                    text = "6",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 25.sp,
                                        letterSpacing = 0.29.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                Text(
                    text = "MNO",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                }
            Box(
                modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 133.4720001220703.dp,
                                                end = 121.05598449707031.dp,
                                                top = 61.027493476867676.dp,
                                                bottom = 104.470778465271.dp)
                ) {
                Box(
                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = LocalKilnColors.current.cardBackground)
                                        .clip(shape = RoundedCornerShape(5.dp)))
                Text(
                    text = "5",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 25.sp,
                                        letterSpacing = 0.29.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                Text(
                    text = "JKL",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 25.sp,
                                        letterSpacing = 0.29.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                Text(
                    text = "JKL",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                }
            Box(
                modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 6.208000183105469.dp,
                                                end = 248.31998443603516.dp,
                                                top = 61.027493476867676.dp,
                                                bottom = 104.470778465271.dp)
                ) {
                Box(
                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = LocalKilnColors.current.cardBackground)
                                        .clip(shape = RoundedCornerShape(5.dp)))
                Text(
                    text = "4",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 25.sp,
                                        letterSpacing = 0.29.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                Text(
                    text = "GHI",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                }
            Box(
                modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 260.7359848022461.dp,
                                                end = -6.208000183105469.dp,
                                                top = 116.88316440582275.dp,
                                                bottom = 48.615111351013184.dp)
                ) {
                Box(
                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = LocalKilnColors.current.cardBackground)
                                        .clip(shape = RoundedCornerShape(5.dp)))
                Text(
                    text = "9",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 25.sp,
                                        letterSpacing = 0.29.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                Text(
                    text = "WXYZ",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                }
            Box(
                modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 133.4720001220703.dp,
                                                end = 121.05598449707031.dp,
                                                top = 116.88316440582275.dp,
                                                bottom = 48.615111351013184.dp)
                ) {
                Box(
                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = LocalKilnColors.current.cardBackground)
                                        .clip(shape = RoundedCornerShape(5.dp)))
                Text(
                    text = "8",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 25.sp,
                                        letterSpacing = 0.29.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                Text(
                    text = "TUV",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                }
            Box(
                modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 6.208000183105469.dp,
                                                end = 248.31998443603516.dp,
                                                top = 116.88316440582275.dp,
                                                bottom = 48.615111351013184.dp)
                ) {
                Box(
                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = LocalKilnColors.current.cardBackground)
                                        .clip(shape = RoundedCornerShape(5.dp)))
                Text(
                    text = "7",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 25.sp,
                                        letterSpacing = 0.29.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                Text(
                    text = "PQRS",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                }
            Box(
                modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 133.4720001220703.dp,
                                                end = 121.05598449707031.dp,
                                                top = 172.73882389068604.dp,
                                                bottom = -6.206185340881348.dp)
                ) {
                Box(
                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color = LocalKilnColors.current.cardBackground)
                                        .clip(shape = RoundedCornerShape(5.dp)))
                Text(
                    text = "0",
                    color = LocalKilnColors.current.title,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                                        fontSize = 25.sp,
                                        letterSpacing = 0.29.sp),
                    modifier = Modifier
                                        .fillMaxSize())
                }
            Box(
                modifier = Modifier
                                .align(alignment = Alignment.TopStart)
                                .offset(x = 305.23.dp,
                                                y = 179.98.dp)
                                .requiredWidth(width = 25.dp)
                                .requiredHeight(height = 19.dp)
                                .background(color = LocalKilnColors.current.muted)
                                .clip(shape = RoundedCornerShape(3.dp)))
            }
        }
 }

@Preview(widthDp = 388, heightDp = 301)
@Composable
private fun SystemKeyboardsIPhoneLightNumbersPreview() {
    MySmartKilnTheme {
        SystemKeyboardsIPhoneLightNumbers(Modifier)
    }
 }
