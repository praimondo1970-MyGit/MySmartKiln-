package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun Group20(
    modifier: Modifier = Modifier,
    temperature: String = "",
    speed: String = "",
    maintenance: String = "",
    onTemperatureChange: (String) -> Unit = {},
    onSpeedChange: (String) -> Unit = {},
    onMaintenanceChange: (String) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(height = 106.dp) // Aumentar altura para incluir labels
    ) {
        // Labels row - textos separados arriba
        StageLabels()
        
        // Input fields row - campos perfectamente alineados
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp)
                .padding(top = 4.dp), // Reducir de 8dp a 4dp para acercar campos a textos
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Temperature Field
            StageInputField(
                label = "", // Ya no necesita label
                unit = "°C",
                value = temperature,
                onValueChange = onTemperatureChange,
                textOffset = 5.dp // Offset del texto dentro del campo
            )
            
            // Speed Field
            StageInputField(
                label = "", // Ya no necesita label
                unit = "°C/min",
                value = speed,
                onValueChange = onSpeedChange,
                textOffset = (-4).dp // Offset del texto dentro del campo
            )
            
            // Maintenance Field
            StageInputField(
                label = "", // Ya no necesita label
                unit = "m",
                value = maintenance,
                onValueChange = onMaintenanceChange,
                textOffset = 1.dp // Offset del texto dentro del campo
            )
        }
    }
}

@Preview(widthDp = 322, heightDp = 106)
@Composable
private fun Group20Preview() {
    MySmartKilnTheme {
        Group20(
            temperature = "850",
            speed = "2",
            maintenance = "30"
        )
    }
 }