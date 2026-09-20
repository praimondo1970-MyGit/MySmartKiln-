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
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.utils.KilnEspStatusLabels

/**
 * Badge de estado del horno (detalle). Misma semántica/visual que el chip de la lista.
 */
@Composable
fun KilnEspStatusBadge(
    espStatus: String,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    val normalized = KilnEspStatusLabels.normalize(espStatus)
    val label = KilnEspStatusLabels.label(espStatus).uppercase()
    val isPaused = normalized == "PAUSADO" || normalized == "PAUSED" || normalized == "RECONNECTING"
    val isBaking = normalized == "CALENTANDO" || normalized == "RUNNING"
    val isInterrupted = normalized == "INTERRUMPIDO"
    val isFinished = normalized == "FINALIZADO" || normalized == "FINISHED"
    val isDisconnected = normalized == "DESCONECTADO" ||
        normalized == "DISCONNECTED" ||
        normalized == "OFFLINE"

    val infiniteTransition = rememberInfiniteTransition(label = "paused_blink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "paused_alpha"
    )
    val contentAlpha = if (isPaused) blinkAlpha else 1f

    val bg: Color
    val fg: Color
    when {
        isBaking -> {
            bg = kilnColors.accent
            fg = kilnColors.onAccent
        }
        isPaused -> {
            bg = kilnColors.warning.copy(alpha = 0.22f)
            fg = kilnColors.warning
        }
        isInterrupted -> {
            bg = kilnColors.danger.copy(alpha = 0.22f)
            fg = kilnColors.danger
        }
        isFinished -> {
            bg = kilnColors.success.copy(alpha = 0.22f)
            fg = kilnColors.success
        }
        isDisconnected -> {
            bg = kilnColors.muted.copy(alpha = 0.22f)
            fg = kilnColors.muted
        }
        else -> {
            // INACTIVO / Detenido — mismo criterio que la lista
            bg = kilnColors.muted.copy(alpha = 0.22f)
            fg = kilnColors.title
        }
    }

    Surface(
        modifier = modifier.alpha(contentAlpha),
        shape = RoundedCornerShape(18.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            when {
                isBaking -> {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_flame),
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(12.dp)
                    )
                }
                isDisconnected -> {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_wifi),
                        contentDescription = null,
                        tint = fg,
                        modifier = Modifier.size(12.dp)
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(fg, CircleShape)
                    )
                }
            }
            Text(
                text = label,
                color = fg,
                fontWeight = if (isBaking) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 13.sp
            )
        }
    }
}
