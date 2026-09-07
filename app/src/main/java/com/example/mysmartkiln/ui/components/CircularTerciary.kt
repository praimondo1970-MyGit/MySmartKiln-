package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun CircularTerciary(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .requiredSize(size = 34.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape = CircleShape)
                .background(color = LocalKilnColors.current.accent)
        )
        // Placeholder for icon - will be replaced when the actual icon is available
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Close",
            tint = LocalKilnColors.current.onAccent,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 9.dp,
                    end = 9.45714282989502.dp,
                    top = 8.999998092651367.dp,
                    bottom = 9.457144737243652.dp)
        )
    }
}

@Preview(widthDp = 34, heightDp = 34)
@Composable
private fun CircularTerciaryPreview() {
    MySmartKilnTheme {
        CircularTerciary(Modifier)
    }
 }