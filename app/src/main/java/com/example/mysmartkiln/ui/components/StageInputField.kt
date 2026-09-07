package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun StageInputField(
    label: String,
    unit: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    textOffset: androidx.compose.ui.unit.Dp = 0.dp
) {
    // Solo el campo de entrada, sin label
    Box(
        modifier = modifier
            .requiredWidth(100.dp) // Reducir ancho de 105dp a 100dp
            .clip(shape = RoundedCornerShape(12.dp))
            .background(color = LocalKilnColors.current.cardBackground)
            .border(
                border = BorderStroke(1.dp, LocalKilnColors.current.screenBackground),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = LocalKilnColors.current.title, // Dark blue text
                    fontSize = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End // Alineación a la derecha
                ),
                cursorBrush = SolidColor(LocalKilnColors.current.title),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number // Set numeric keyboard
                ),
                decorationBox = { innerTextField ->
                    // Calcular el espacio para las unidades según el tipo
                    val unitSpace = when(unit) {
                        "°C" -> 18.dp      // Espacio para temperatura: ancho de "°C" + 1dp
                        "°C/Min" -> 41.dp  // Espacio para velocidad: ancho de "°C/Min" + 1dp
                        else -> 24.dp      // Espacio para meseta: ancho de "Min" + 1dp
                    }
                    
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Text input area with padding for units
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = unitSpace)
                                .padding(4.dp)
                                .offset(x = textOffset) // Aplicar offset al área de texto
                        ) {
                            innerTextField()
                        }
                        // Unit text positioned at the right aligned with input text
                        Box(
                            modifier = Modifier
                                .align(androidx.compose.ui.Alignment.CenterEnd)
                                .padding(top = 4.dp, bottom = 4.dp), // Mismo padding vertical que el texto
                            contentAlignment = androidx.compose.ui.Alignment.CenterStart
                        ) {
                            Text(
                                text = unit,
                                style = TextStyle(
                                    color = LocalKilnColors.current.body,
                                    fontSize = 13.sp
                                ),
                                modifier = Modifier.padding(start = 1.dp)
                            )
                        }
                    }
                }
            )
    }
}

@Preview(showBackground = true)
@Composable
fun StageInputFieldPreview() {
    MySmartKilnTheme {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .background(color = LocalKilnColors.current.screenBackground)
        ) {
            StageInputField(
                label = "Temperatura",
                unit = "°C",
                value = "",
                onValueChange = {}
            )
        }
    }
}
