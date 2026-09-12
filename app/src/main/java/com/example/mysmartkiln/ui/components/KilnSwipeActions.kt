package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
fun KilnSwipeActions(
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    Box(modifier = modifier.requiredWidth(160.dp)) {
        if (kilnColors.isEsp) {
            val shape = RoundedCornerShape(12.dp)
            val deleteTint = kilnColors.title.copy(alpha = 0.72f)
            val editTint = kilnColors.accent.copy(alpha = 0.72f)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-78).dp)
                    .requiredSize(width = 62.dp, height = 56.dp)
                    .border(1.dp, deleteTint, shape)
                    .clip(shape)
                    .clickable(onClick = onDelete),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.typedeletesize16colorblack),
                    contentDescription = "borrar",
                    tint = deleteTint,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "BORRAR",
                    color = deleteTint,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-10).dp)
                    .requiredSize(width = 62.dp, height = 56.dp)
                    .border(1.dp, editTint, shape)
                    .clip(shape)
                    .clickable(onClick = onEdit),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_edit),
                    contentDescription = "editar",
                    tint = editTint,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "EDITAR",
                    color = editTint,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-83).dp)
                    .requiredSize(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.Primary400)
                    .clickable(onClick = onDelete),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.typedeletesize16colorblack),
                    contentDescription = "borrar",
                    tint = AppColors.White,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "borrar",
                    color = AppColors.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-8).dp)
                    .requiredSize(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppColors.color_Terciary_800)
                    .clickable(onClick = onEdit),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_edit),
                    contentDescription = "editar",
                    tint = AppColors.White,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "editar",
                    color = AppColors.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
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
                .requiredHeight(100.dp)
                .background(Color.DarkGray)
        ) {
            KilnSwipeActions(modifier = Modifier.requiredHeight(100.dp))
        }
    }
}
