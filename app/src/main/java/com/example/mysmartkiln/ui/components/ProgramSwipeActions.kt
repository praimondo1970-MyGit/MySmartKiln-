package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun ProgramSwipeActions(
    onDelete: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    Box(modifier = modifier.requiredWidth(140.dp)) {
        if (kilnColors.isEsp) {
            IconSwipeFrame(
                icon = R.drawable.typedeletesize16colorblack,
                label = "BORRAR",
                tint = kilnColors.title.copy(alpha = 0.72f),
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-72).dp)
            )
            IconSwipeFrame(
                icon = R.drawable.typeduplicate,
                label = "DUPLICAR",
                tint = kilnColors.accent.copy(alpha = 0.72f),
                onClick = onDuplicate,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-6).dp)
            )
        } else {
            ClassicProgramSwipeButton(
                label = "borrar",
                icon = R.drawable.typedeletesize16colorblack,
                background = AppColors.Primary400,
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-74).dp)
            )
            ClassicProgramSwipeButton(
                label = "duplicar",
                icon = R.drawable.typeduplicate,
                background = AppColors.color_Terciary_800,
                onClick = onDuplicate,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-4).dp)
            )
        }
    }
}

@Composable
private fun IconSwipeFrame(
    icon: Int,
    label: String,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = modifier
            .requiredSize(width = 62.dp, height = 56.dp)
            .border(1.dp, tint, shape)
            .clip(shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = tint,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun ClassicProgramSwipeButton(
    label: String,
    icon: Int,
    background: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .requiredSize(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = label,
            tint = AppColors.White,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = AppColors.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProgramSwipeActionsPreview() {
    MySmartKilnTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(100.dp)
                .background(Color.DarkGray)
        ) {
            ProgramSwipeActions(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .requiredHeight(100.dp)
            )
        }
    }
}
