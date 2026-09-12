package com.example.mysmartkiln.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

data class ProgramCardData(
    val id: String,
    val name: String,
    val maxTemperature: String,
    val duration: String,
    val stageCount: Int
)

@Composable
fun ProgramCard(
    program: ProgramCardData,
    isOpen: Boolean = false,
    onSwipeStateChange: (Boolean) -> Unit = {},
    onCardClick: () -> Unit = {},
    onDelete: () -> Unit = {},
    onDuplicate: () -> Unit = {},
    showArrow: Boolean = true,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    var offsetX by remember { mutableStateOf(0f) }
    val cardHeight = if (kilnColors.isEsp) 100.dp else 93.dp
    val cardCorner = if (kilnColors.isEsp) 12.dp else 12.dp

    LaunchedEffect(isOpen) { }

    val swipeOffset by animateFloatAsState(
        targetValue = if (isOpen) -160f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "swipe"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(cardHeight)
    ) {
        ProgramSwipeActions(
            onDelete = onDelete,
            onDuplicate = onDuplicate,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .requiredHeight(cardHeight)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(cardHeight)
                .offset(x = swipeOffset.dp)
                .shadow(
                    elevation = if (kilnColors.isEsp) 0.dp else 4.dp,
                    shape = RoundedCornerShape(cardCorner)
                )
                .background(
                    color = kilnColors.cardBackground,
                    shape = RoundedCornerShape(cardCorner)
                )
                .border(
                    width = 1.dp,
                    color = kilnColors.cardBorder,
                    shape = RoundedCornerShape(cardCorner)
                )
                .clickable {
                    if (isOpen) {
                        onSwipeStateChange(false)
                    } else {
                        onCardClick()
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offsetX = 0f },
                        onDragEnd = {
                            if (offsetX > -10f) {
                                onSwipeStateChange(false)
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX += dragAmount
                            val newOffset = swipeOffset + dragAmount
                            if (isOpen && dragAmount > 0) {
                                onSwipeStateChange(false)
                            } else if (newOffset <= 0f && newOffset >= -160f) {
                                val shouldOpen = newOffset < -10f
                                if (shouldOpen != isOpen) {
                                    onSwipeStateChange(shouldOpen)
                                }
                            }
                        }
                    )
                }
                .padding(if (kilnColors.isEsp) 12.dp else 16.dp)
        ) {
            if (kilnColors.isEsp) {
                ProgramCardEspContent(
                    program = program,
                    showArrow = showArrow
                )
            } else {
                ProgramCardClassicContent(
                    program = program,
                    showArrow = showArrow,
                    onCardClick = onCardClick
                )
            }
        }
    }
}

@Composable
private fun ProgramCardEspContent(
    program: ProgramCardData,
    showArrow: Boolean
) {
    val kilnColors = LocalKilnColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.nav_programas),
            contentDescription = null,
            tint = kilnColors.accent,
            modifier = Modifier.size(36.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Nombre + chevron en la misma fila; las métricas usan todo el ancho hasta el borde derecho.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = program.name,
                    color = kilnColors.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (showArrow) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_arrow_right),
                        contentDescription = null,
                        tint = kilnColors.muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProgramMetricEsp(
                    icon = R.drawable.icon_thermometer,
                    value = "${program.maxTemperature}°C",
                    label = "Temp. máx.",
                    modifier = Modifier.weight(1f)
                )
                ProgramMetricEsp(
                    icon = R.drawable.icon_clock,
                    value = program.duration,
                    label = "Duración",
                    modifier = Modifier.weight(1f)
                )
                ProgramMetricEsp(
                    icon = R.drawable.icon_stage,
                    value = program.stageCount.toString(),
                    label = "Etapas",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ProgramMetricEsp(
    icon: Int,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = kilnColors.accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = value,
                color = kilnColors.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = label,
            color = kilnColors.muted,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProgramCardClassicContent(
    program: ProgramCardData,
    showArrow: Boolean,
    onCardClick: () -> Unit
) {
    val kilnColors = LocalKilnColors.current
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .offset(y = (-4).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = program.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = kilnColors.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            if (showArrow) {
                Box(
                    modifier = Modifier
                        .requiredSize(24.dp)
                        .clip(CircleShape)
                        .background(color = kilnColors.accent)
                        .clickable { onCardClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = kilnColors.onAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.offset(y = (-1).dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(35.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color = kilnColors.accent.copy(alpha = 0.15f))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.etapa_temp),
                        contentDescription = "Temperature",
                        tint = kilnColors.accent,
                        modifier = Modifier.requiredSize(24.dp)
                    )
                }
                Column {
                    Text(
                        text = "${program.maxTemperature}°C",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kilnColors.body,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "Temp max.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = kilnColors.body,
                            fontSize = 12.sp
                        )
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(35.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color = kilnColors.accent.copy(alpha = 0.15f))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.etapa_mes),
                        contentDescription = "Duration",
                        tint = kilnColors.accent,
                        modifier = Modifier.requiredSize(24.dp)
                    )
                }
                Column {
                    Text(
                        text = program.duration,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kilnColors.body,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    Text(
                        text = "Duracion",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = kilnColors.body,
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgramCardPreview() {
    MySmartKilnTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProgramCard(
                program = ProgramCardData(
                    id = "1",
                    name = "Esmalte Cone 6",
                    maxTemperature = "980",
                    duration = "1h 42m",
                    stageCount = 4
                )
            )
            ProgramCard(
                program = ProgramCardData(
                    id = "2",
                    name = "Gres Alta Temperatura",
                    maxTemperature = "1280",
                    duration = "8h 15m",
                    stageCount = 6
                )
            )
        }
    }
}
