package com.example.mysmartkiln.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun BackIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.ArrowBack,
        contentDescription = "Back Icon",
        tint = LocalKilnColors.current.accent,
        modifier = modifier
    )
}

@Preview(widthDp = 24, heightDp = 24)
@Composable
private fun BackIconPreview() {
    MySmartKilnTheme {
        BackIcon(Modifier)
    }
}
