package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
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
fun NewKilnCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val kilnColors = LocalKilnColors.current
    if (kilnColors.isEsp) {
        EspAddActionCard(
            title = "Nuevo Horno",
            onClick = onClick,
            modifier = modifier
        ) {
            Icon(
                painter = painterResource(id = R.drawable.icon_flame),
                contentDescription = "Icono horno",
                tint = kilnColors.accent,
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        ClassicNewKilnCard(modifier = modifier, onClick = onClick)
    }
}

@Composable
private fun ClassicNewKilnCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val kilnColors = LocalKilnColors.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(83.dp)
            .clip(shape)
            .background(color = kilnColors.cardBackground)
            .clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = R.drawable.bg_new_item_card),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(83.dp),
            contentScale = ContentScale.FillBounds
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .size(44.dp)
        ) {
            OrangeHexagon(modifier = Modifier.size(44.dp), color = AppColors.Primary300)
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.nav_hornos),
                contentDescription = "Icono horno",
                tint = kilnColors.onAccent,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
            )
        }
        Text(
            text = "Nuevo Horno",
            style = TextStyle(
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = kilnColors.muted
            ),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 80.dp)
        )
    }
}

@Preview(widthDp = 350, heightDp = 83)
@Composable
private fun NewKilnCardPreview() {
    MySmartKilnTheme {
        NewKilnCard()
    }
}
