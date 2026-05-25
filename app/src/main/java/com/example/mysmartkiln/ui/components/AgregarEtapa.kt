package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun AgregarEtapa(
    text: String = "Agregar etapa",
    modifier: Modifier = Modifier
) {
    // Title - Fixed height container to maintain position
    Box(
        modifier = modifier
            .padding(bottom = 16.dp)
            .height(60.dp), // Fixed height to accommodate text
        contentAlignment = Alignment.CenterStart // Align to left instead of center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(
                color = Color(0xFF1E3A8A), // Dark blue - same as CrearPrograma
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Start // Left align instead of center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AgregarEtapaPreview() {
    MySmartKilnTheme {
        AgregarEtapa()
    }
}