package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun StageLabels(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(32.dp)
            .padding(horizontal = 25.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Temperature Label
        Text(
            text = "Temperatura",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = LocalKilnColors.current.title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp) // Espacio extra para el texto largo
        )
        
        // Speed Label
        Text(
            text = "Velocidad",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = LocalKilnColors.current.title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            )
        )
        
        // Maintenance Label
        Text(
            text = "Meseta",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = LocalKilnColors.current.title,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(horizontal = 8.dp) // Espacio extra para balance visual
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StageLabelsPreview() {
    MySmartKilnTheme {
        StageLabels()
    }
}
















































