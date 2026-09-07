package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun Icons(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
                .requiredWidth(width = 1280.dp)
                .requiredHeight(height = 2321.dp)
                .clip(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(color = LocalKilnColors.current.cardBackground)
        ) {
        Box(
            modifier = Modifier
                        .requiredWidth(width = 1280.dp)
                        .requiredHeight(height = 132.dp)
            ) {
            Box(
                modifier = Modifier
                                .requiredWidth(width = 1280.dp)
                                .requiredHeight(height = 132.dp)
                                .clip(shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                                .background(color = Color(0xff222222)))
            Text(
                text = "Icons",
                color = LocalKilnColors.current.onAccent,
                style = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Light),
                modifier = Modifier
                                .align(alignment = Alignment.TopStart)
                                .offset(x = 55.dp,
                                                y = 47.dp))
            }
        // Content would continue here with all the icon definitions...
        // For brevity, I'm showing the structure
    }
}

@Preview(widthDp = 1280, heightDp = 2321)
@Composable
private fun IconsPreview() {
    Icons(Modifier)
}

























































