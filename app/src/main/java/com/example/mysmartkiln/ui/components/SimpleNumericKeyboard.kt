package com.example.mysmartkiln.ui.components

import com.example.mysmartkiln.ui.theme.LocalKilnColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun SimpleNumericKeyboard(
    modifier: Modifier = Modifier,
    onKeyPress: (String) -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .background(color = Color(0xffd1d5db))
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Fila 1: 1, 2, 3
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                NumericKey("1", "", onKeyPress)
                NumericKey("2", "ABC", onKeyPress)
                NumericKey("3", "DEF", onKeyPress)
            }
            
            // Fila 2: 4, 5, 6
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                NumericKey("4", "GHI", onKeyPress)
                NumericKey("5", "JKL", onKeyPress)
                NumericKey("6", "MNO", onKeyPress)
            }
            
            // Fila 3: 7, 8, 9
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                NumericKey("7", "PQRS", onKeyPress)
                NumericKey("8", "TUV", onKeyPress)
                NumericKey("9", "WXYZ", onKeyPress)
            }
            
            // Fila 4: espacio vacío, 0, Delete
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Espacio vacío a la izquierda
                Box(modifier = Modifier.size(100.dp))
                
                // Tecla 0 centrada
                NumericKey("0", "", onKeyPress)
                
                // Botón Delete
                DeleteKey(onKeyPress)
            }
        }
    }
}

@Composable
private fun NumericKey(
    number: String,
    letters: String,
    onKeyPress: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(40.dp)
            .background(
                color = LocalKilnColors.current.onAccent,
                shape = RoundedCornerShape(5.dp)
            )
            .clip(RoundedCornerShape(5.dp))
            .clickable { onKeyPress(number) },
        contentAlignment = Alignment.Center
    ) {
        // Solo mostrar el número, sin letras
        Text(
            text = number,
            color = Color.Black,
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DeleteKey(
    onKeyPress: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(40.dp)
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(5.dp)
            )
            .clip(RoundedCornerShape(5.dp))
            .clickable { onKeyPress("delete") },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⌫",
            color = Color.Black,
            fontSize = 24.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(widthDp = 388, heightDp = 300)
@Composable
private fun SimpleNumericKeyboardPreview() {
    MySmartKilnTheme {
        SimpleNumericKeyboard()
    }
}
