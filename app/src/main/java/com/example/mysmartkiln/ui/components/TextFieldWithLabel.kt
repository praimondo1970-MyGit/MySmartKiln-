package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun TextFieldWithLabel(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Label
        Text(
            text = label,
            color = LocalKilnColors.current.accent,
            style = TextStyle(fontSize = 16.sp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(bottom = 8.dp)
        )
        
        // Text Field Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .height(52.dp)
                .clip(shape = RoundedCornerShape(12.dp))
                .background(
                    color = if (enabled) LocalKilnColors.current.cardBackground else LocalKilnColors.current.cardBackground
                )
                .border(
                    border = BorderStroke(1.dp, LocalKilnColors.current.cardBorder),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            // Text Input
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.CenterStart),
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    color = if (enabled) LocalKilnColors.current.accent else LocalKilnColors.current.muted
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    imeAction = imeAction
                ),
                enabled = enabled,
                singleLine = true
            )
            
            // Placeholder
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    color = LocalKilnColors.current.muted, // Tono más bajo de gris
                    style = TextStyle(fontSize = 16.sp),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Preview(widthDp = 319, heightDp = 74)
@Composable
private fun TextFieldWithLabelPreview() {
    MySmartKilnTheme {
        var text by remember { mutableStateOf("") }
        TextFieldWithLabel(
            label = "Nombre",
            value = text,
            onValueChange = { text = it },
            placeholder = "Ingresa el nombre del programa"
        )
    }
}
