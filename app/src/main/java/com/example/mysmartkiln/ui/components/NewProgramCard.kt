package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
fun NewProgramCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val kilnColors = LocalKilnColors.current
    if (kilnColors.isEsp) {
        EspAddActionCard(
            title = "Nuevo Programa",
            onClick = onClick,
            modifier = modifier
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.nav_programas),
                contentDescription = "Icono programa",
                tint = kilnColors.accent,
                modifier = Modifier.size(24.dp)
            )
        }
    } else {
        ClassicNewProgramCard(modifier = modifier, onClick = onClick)
    }
}

@Composable
private fun ClassicNewProgramCard(
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
            painter = painterResource(id = R.drawable.bg_new_item_card2),
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
                imageVector = ImageVector.vectorResource(id = R.drawable.nav_programas),
                contentDescription = "Icono programa",
                tint = kilnColors.onAccent,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
            )
        }
        Text(
            text = "Nuevo Programa",
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

@Composable
internal fun EspAddActionCard(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val kilnColors = LocalKilnColors.current
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(kilnColors.cardBackground, shape)
            .border(1.dp, kilnColors.cardBorder, shape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(kilnColors.track, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }
        Text(
            text = title,
            style = TextStyle(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = kilnColors.title
            )
        )
    }
}

@Preview(widthDp = 350, heightDp = 83)
@Composable
private fun NewProgramCardPreview() {
    MySmartKilnTheme {
        NewProgramCard()
    }
}
