package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.AppColors

@Composable
fun MaintenanceInput(
    modifier: Modifier = Modifier,
    value: String = "",
    onValueChange: (String) -> Unit = {},
    onClick: () -> Unit = {},
    isActive: Boolean = false
) {
    Column(
        modifier = modifier
    ) {
        // Label
        Text(
            text = "Meseta",
            color = AppColors.color_Primary_700,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        // Input Field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color = Color.White)
                .border(
                    border = BorderStroke(1.dp, AppColors.Primary100),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onClick() }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(
                    color = AppColors.color_Primary_700,
                    fontSize = 16.sp,
                    textAlign = TextAlign.End
                ),
                cursorBrush = if (isActive) SolidColor(AppColors.color_Primary_700) else SolidColor(Color.Transparent),
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                enabled = isActive
            )
            
            // Unit indicator
            if (value.isEmpty()) {
                Text(
                    text = "Min",
                    color = AppColors.color_Primary_700,
                    fontSize = 16.sp,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Preview(widthDp = 100, heightDp = 80)
@Composable
private fun MaintenanceInputPreview() {
    MaintenanceInput()
}
