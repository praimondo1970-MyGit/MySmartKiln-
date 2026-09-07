package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun AgregarButton(
    onClick: () -> Unit = {
},
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp), // Same height as Comenzar button
        colors = ButtonDefaults.buttonColors(
            containerColor = LocalKilnColors.current.accent, // Same color as Comenzar button
            contentColor = Color.White,
            disabledContainerColor = LocalKilnColors.current.cardBorder,
            disabledContentColor = LocalKilnColors.current.muted
        ),
        shape = RoundedCornerShape(16.dp) // Same shape as Comenzar button
    ) {
        Text(
            text = "Agregar",
            style = MaterialTheme.typography.titleMedium // Same style as Comenzar button
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AgregarButtonPreview() {
    MySmartKilnTheme {
        AgregarButton()
    }
}

























































