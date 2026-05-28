package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.utils.KilnEspStatusLabels

@Composable
fun KilnEspStatusBadge(
    espStatus: String,
    modifier: Modifier = Modifier
) {
    val label = KilnEspStatusLabels.label(espStatus)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = KilnEspStatusLabels.chipBackground(espStatus)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(KilnEspStatusLabels.dotColor(espStatus), CircleShape)
            )
            Text(
                text = label,
                color = KilnEspStatusLabels.chipContent(espStatus),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}
