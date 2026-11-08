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
    onClick: () -> Unit = {},
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (kilnColors.isEsp) {
                Color(0xFFFF7811) // mismo naranja que INICIAR
            } else {
                kilnColors.accent
            },
            contentColor = kilnColors.onAccent,
            disabledContainerColor = kilnColors.cardBorder,
            disabledContentColor = kilnColors.muted
        ),
        shape = RoundedCornerShape(if (kilnColors.isEsp) 12.dp else 16.dp)
    ) {
        Text(
            text = "Agregar",
            style = MaterialTheme.typography.titleMedium
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

























































