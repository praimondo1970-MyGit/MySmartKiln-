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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Icon
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

data class StageData(
    val stageNumber: Int,
    val temperature: String,
    val speed: String,
    val maintenance: String
)

@Composable
fun StageCard(
    stageData: StageData,
    onStageClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    var isSwiped by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    
    val swipeOffset by animateFloatAsState(
        targetValue = if (isSwiped) -80f else 0f, // Desplazamiento de 80dp (unificado con otras tarjetas)
        animationSpec = tween(durationMillis = 200),
        label = "swipe"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(120.dp)
    ) {
        // Delete button (background) - usando lógica de ActivityCard
        Box(
            modifier = Modifier
                .width(80.dp)
                .requiredHeight(120.dp)
                .align(Alignment.CenterEnd)
        ) {
            Group41(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { onDeleteClick() }
            )
        }
        
        // Main card (foreground)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(120.dp)
                .offset(x = swipeOffset.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp)
                )
                .background(
                    color = kilnColors.cardBackground,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = kilnColors.cardBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                       .pointerInput(Unit) {
                           detectHorizontalDragGestures(
                               onDragStart = {
                                   offsetX = 0f
                               },
                               onDragEnd = {
                                   // Auto-close if not swiped enough to the left
                                   if (offsetX > -10f) {
                                       isSwiped = false
                                   }
                                   offsetX = 0f
                               },
                               onDragCancel = {
                                   offsetX = 0f
                               },
                               onHorizontalDrag = { change, dragAmount ->
                                   offsetX += dragAmount
                                   val newOffset = swipeOffset + dragAmount

                                   // Si está deslizada hacia la izquierda y se desliza hacia la derecha
                                   if (isSwiped && dragAmount > 0) {
                                       // Regresar a posición original si se desliza hacia la derecha
                                       isSwiped = false
                                   }
                                   // Deslizamiento normal hacia la izquierda
                                   else if (newOffset <= 0f && newOffset >= -80f) {
                                       isSwiped = newOffset < -10f
                                   }
                               }
                           )
                       }
                .clickable { 
                    if (isSwiped) {
                        isSwiped = false
                    } else {
                        onStageClick() 
                    }
                }
                .padding(8.dp)
        ) {
            // Stage title and arrow button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stage title
                Text(
                    text = "Etapa ${stageData.stageNumber}",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                        color = kilnColors.title,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                )
                
                // Arrow button
                Box(
                    modifier = Modifier
                        .requiredSize(24.dp)
                        .clip(CircleShape)
                        .background(color = kilnColors.accent)
                        .clickable { onStageClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ">",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                            color = kilnColors.onAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.offset(y = (-1).dp)
                    )
                }
            }
            
            // Parameters row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Temperature
                ParameterColumn(
                    icon = R.drawable.etapa_temp,
                    value = "${stageData.temperature}°C",
                    label = "Temperatura"
                )
                
                // Speed
                ParameterColumn(
                    icon = R.drawable.etapa_vel,
                    value = "${stageData.speed}°C/Min",
                    label = "Velocidad"
                )
                
                // Maintenance
                ParameterColumn(
                    icon = R.drawable.etapa_mes,
                    value = "${stageData.maintenance} min",
                    label = "Meseta"
                )
            }
        }
    }
}

@Composable
private fun ParameterColumn(
    icon: Int,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    Column(
        modifier = modifier.requiredWidth(100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Box(
            modifier = Modifier
                .requiredSize(35.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color = kilnColors.accent.copy(alpha = 0.15f))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = kilnColors.accent,
                modifier = Modifier.requiredSize(24.dp)
            )
        }
        
        // Value
        Text(
            text = value,
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(
                color = kilnColors.body,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(top = 6.dp)
        )
        
        // Label
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                color = kilnColors.body,
                fontSize = 12.sp
            ),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** Vista de etapa sin edición (p. ej. pestaña Programa del horno). */
@Composable
fun ReadOnlyStageCard(
    stageData: StageData,
    isCurrentStage: Boolean = false,
    isCompleted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current

    val backgroundColor = when {
        isCurrentStage -> kilnColors.cardBackground
        isCompleted -> kilnColors.cardBackground
        else -> kilnColors.cardBackground
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(120.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
            .then(
                if (isCurrentStage) {
                    Modifier.border(2.dp, kilnColors.accent, RoundedCornerShape(12.dp))
                } else {
                    Modifier.border(1.dp, kilnColors.cardBorder, RoundedCornerShape(12.dp))
                }
            )
            .background(color = backgroundColor, shape = RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Etapa ${stageData.stageNumber}",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                    color = kilnColors.title,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
            )
            if (isCurrentStage) {
                Text(
                    text = "En curso",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                        color = kilnColors.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            } else if (isCompleted) {
                Text(
                    text = "Completada",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(
                        color = kilnColors.success,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ParameterColumn(
                icon = R.drawable.etapa_temp,
                value = "${stageData.temperature}°C",
                label = "Temperatura"
            )
            ParameterColumn(
                icon = R.drawable.etapa_vel,
                value = "${stageData.speed}°C/Min",
                label = "Velocidad"
            )
            ParameterColumn(
                icon = R.drawable.etapa_mes,
                value = "${stageData.maintenance} min",
                label = "Meseta"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StageCardPreview() {
    MySmartKilnTheme {
        StageCard(
            stageData = StageData(
                stageNumber = 1,
                temperature = "850",
                speed = "2",
                maintenance = "30"
            ),
            onStageClick = {},
            onDeleteClick = {}
        )
    }
}