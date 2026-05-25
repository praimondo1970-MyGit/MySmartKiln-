package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun CrearUnPrograma(modifier: Modifier = Modifier) {
    Text(
        text = "Crear un programa",
        color = AppColors.color_Primary_700,
        lineHeight = 1.09.em,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Bold
        ),
        modifier = modifier
                .requiredWidth(width = 316.dp))
 }

@Preview(widthDp = 316, heightDp = 24)
@Composable
private fun CrearUnProgramaPreview() {
    MySmartKilnTheme {
        CrearUnPrograma(Modifier)
    }
 }
