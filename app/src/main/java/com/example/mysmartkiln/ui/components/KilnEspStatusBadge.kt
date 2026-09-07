package com.example.mysmartkiln.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.utils.KilnEspStatusLabels

@Composable
fun KilnEspStatusBadge(
    espStatus: String,
    modifier: Modifier = Modifier
) {
    val normalized = KilnEspStatusLabels.normalize(espStatus)
    val label = KilnEspStatusLabels.label(espStatus)
    val isPaused = normalized == "PAUSADO"

    val infiniteTransition = rememberInfiniteTransition(label = "paused_blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "paused_alpha"
    )
    val contentAlpha = if (isPaused) blinkAlpha else 1f

    Surface(
        modifier = modifier.alpha(contentAlpha),
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
