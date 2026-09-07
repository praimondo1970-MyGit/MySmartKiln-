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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
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
    showArrow: Boolean = true, // Parámetro para controlar si se muestra el botón ">"
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    var offsetX by remember { mutableStateOf(0f) }
    
    // Sincronizar el estado local con el estado externo
    LaunchedEffect(isOpen) {
        // No hacer nada, solo observar cambios
    }
    
    val swipeOffset by animateFloatAsState(
        targetValue = if (isOpen) -160f else 0f, // Desplazamiento de 160dp para mostrar ambos botones
        animationSpec = tween(durationMillis = 200),
        label = "swipe"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(93.dp)
    ) {
        // Swipe Actions (Background)
        ProgramSwipeActions(
            onDelete = onDelete,
            onDuplicate = onDuplicate,
            modifier = Modifier
                .align(Alignment.CenterEnd) // Alinear a la derecha
                .requiredHeight(93.dp)
        )
        
        // Main Card Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(93.dp)
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
                .then(
                    if (!showArrow) {
                        // Si no hay botón, toda la tarjeta es clickeable
                        Modifier.clickable { 
                            android.util.Log.d("ProgramCard", "🎯 ProgramCard clicked - calling onCardClick")
                            onCardClick() 
                        }
                    } else {
                        Modifier
                    }
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            offsetX = 0f
                        },
                        onDragEnd = {
                            // Auto-close if not swiped enough to the left
                            if (offsetX > -10f) {
                                onSwipeStateChange(false)
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
                            if (isOpen && dragAmount > 0) {
                                // Regresar a posición original si se desliza hacia la derecha
                                onSwipeStateChange(false)
                            }
                            // Deslizamiento normal hacia la izquierda
                            else if (newOffset <= 0f && newOffset >= -160f) {
                                val shouldOpen = newOffset < -10f
                                if (shouldOpen != isOpen) {
                                    onSwipeStateChange(shouldOpen)
                                }
                            }
                        }
                    )
                }
                .padding(16.dp)
        ) {
        // Program name and arrow button (same layout as StageCard)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .offset(y = (-4).dp), // Mover levemente más arriba
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Program name
            Text(
                text = program.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = kilnColors.title,
                    fontWeight = FontWeight.Bold, // Cambiar de Medium a Bold
                    fontSize = 16.sp
                )
            )
            
            // Arrow button (same as StageCard) - Opcional
            if (showArrow) {
                Box(
                    modifier = Modifier
                        .requiredSize(24.dp)
                        .clip(CircleShape)
                        .background(color = kilnColors.accent)
                        .clickable { 
                            android.util.Log.d("ProgramCard", "🎯 ProgramCard arrow clicked - calling onCardClick")
                            onCardClick() 
                        },
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
        
        // Parameters row (horizontal layout like before)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Temperature parameter (horizontal layout)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Temperature icon
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
                
                // Temperature text
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
            
            // Duration parameter (horizontal layout)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Duration icon
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
                
                // Duration text
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
        } // Cerrar el Box del contenido principal de la tarjeta
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
            style = MaterialTheme.typography.bodyLarge.copy(
                color = kilnColors.body,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            ),
            modifier = Modifier.padding(top = 6.dp)
        )
        
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = kilnColors.body,
                fontSize = 12.sp
            ),
            modifier = Modifier.padding(top = 6.dp)
        )
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
                    name = "Programa especial para cups",
                    maxTemperature = "850",
                    duration = "6h 30min",
                    stageCount = 5
                ),
                onDelete = { /* Delete action */ },
                onDuplicate = { /* Duplicate action */ }
            )
            
            ProgramCard(
                program = ProgramCardData(
                    id = "2",
                    name = "Esmalte Gres",
                    maxTemperature = "1150",
                    duration = "8h 15min",
                    stageCount = 7
                ),
                onDelete = { /* Delete action */ },
                onDuplicate = { /* Duplicate action */ }
            )
        }
    }
}
