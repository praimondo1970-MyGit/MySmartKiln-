package com.example.mysmartkiln.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.OrangeHexagon
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.components.BottomNavBarBackground
import com.example.mysmartkiln.ui.components.ScrollGradient
import com.example.mysmartkiln.ui.components.SearchBar
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

// Estructura de datos para las actividades de horneado
data class ActivityItem(
    val id: String,
    val kilnBrand: String,        // "AFRA"
    val kilnVolume: String,       // "45Lt" 
    val kilnName: String,         // "AFRA 45Lt" (combinado)
    val executionDate: String,    // "26/09/2025"
    val programName: String,      // "Esmalte de Gres"
    val maxTemperature: String,   // "1120"
    val duration: String,         // "6h 30min"
    val timestamp: Long = System.currentTimeMillis() // Para ordenamiento
)

@Composable
fun ActivityScreen(
    onNavigateToKilns: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onCreateProgram: () -> Unit = {},
    onNavigateToKilnsWithScan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Estado para controlar el menú "+"
    var isMenuOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Datos de ejemplo basados en la imagen
    val activities = listOf(
        ActivityItem(
            id = "1",
            kilnBrand = "AFRA",
            kilnVolume = "45Lt",
            kilnName = "AFRA 45Lt",
            executionDate = "26/09/2025",
            programName = "Esmalte de Gres",
            maxTemperature = "1120",
            duration = "6h 30min"
        ),
        ActivityItem(
            id = "2",
            kilnBrand = "SIMSIC",
            kilnVolume = "E40",
            kilnName = "SIMSIC E40",
            executionDate = "24/09/2025",
            programName = "Biscoho para baja temperatura",
            maxTemperature = "800",
            duration = "10h 30min"
        ),
        ActivityItem(
            id = "3",
            kilnBrand = "BAIRES",
            kilnVolume = "256",
            kilnName = "BAIRES 256",
            executionDate = "15/09/2025",
            programName = "Gres media temperatura",
            maxTemperature = "1080",
            duration = "6h 30min"
        )
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = LocalKilnColors.current.screenBackground)
    ) {
        // Contenido principal
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp)
        ) {
            // Título
            Text(
                text = "Actividad",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = LocalKilnColors.current.title,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 70.dp, bottom = 16.dp)
            )
            
            // SearchBar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearchClick = { /* Solo funciona como filtro */ },
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
            )
            
            // Separador de fecha
            Text(
                text = "Septiembre 2025",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = LocalKilnColors.current.title,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
            )
            
            // Lista de actividades con gradiente
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 29.dp, end = 24.dp)
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(activities) { activity ->
                        ActivityCard(
                            activity = activity,
                            onDelete = { /* TODO: Implementar eliminación */ }
                        )
                    }
                    
                    // Espacio adicional al final para el gradiente
                    item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
            
            // Gradiente en la parte inferior
            ScrollGradient(
                height = 40.dp,
                backgroundColor = LocalKilnColors.current.screenBackground,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Bottom spacing for navigation
        Spacer(modifier = Modifier.height(160.dp))
        }
        
        // Overlay del menú de agregar
        if (isMenuOpen) {
            AddMenuScreen(
                onNewKilnClick = {
                    isMenuOpen = false
                    onNavigateToKilnsWithScan()
                },
                onNewProgramClick = {
                    isMenuOpen = false
                    onCreateProgram()
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            )
        }
        
        // Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomStart)
                .fillMaxWidth()
                .requiredHeight(height = 200.dp)
                .zIndex(2f)
        ) {
            // Navigation Bar Background
            BottomNavBarBackground(
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 0.dp, y = 60.dp)
            )
            
            // Responsive Bottom Navigation Bar
            ResponsiveBottomNavigation(
                onKilnsClick = onNavigateToKilns,
                onProgramsClick = onNavigateToPrograms,
                onActivityClick = { /* Ya estamos en Activity */ },
                onSettingsClick = onNavigateToSettings,
                onPlusClick = { isMenuOpen = !isMenuOpen },
                isKilnsActive = false,
                isProgramsActive = false,
                isActivityActive = true,
                isSettingsActive = false,
                showFab = true,
                isMenuOpen = isMenuOpen,
                modifier = Modifier.align(alignment = Alignment.BottomStart)
            )
        }
        
        // Home Indicator
        HomeIndicatorWhite(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 0.dp, y = 810.dp)
        )
    }
}

// Componente para las tarjetas de actividad basado en ProgramCard
@Composable
private fun ActivityCard(
    activity: ActivityItem,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isOpen by remember { mutableStateOf(false) }
    
    val swipeOffset by animateFloatAsState(
        targetValue = if (isOpen) -80f else 0f, // Desplazamiento de 80dp hacia la izquierda (solo delete)
        animationSpec = tween(durationMillis = 200),
        label = "swipe"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(130.dp)
    ) {
        // Swipe Actions (Background) - Solo eliminar
        ActivitySwipeActions(
            onDelete = onDelete,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .requiredHeight(130.dp)
        )
        
        // Main Card Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(130.dp)
                .offset(x = swipeOffset.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(12.dp)
                )
                .background(
                    color = LocalKilnColors.current.cardBackground,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = LocalKilnColors.current.cardBorder,
                    shape = RoundedCornerShape(12.dp)
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            offsetX = 0f
                        },
                        onDragEnd = {
                            if (offsetX > -10f) {
                                isOpen = false
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            offsetX += dragAmount
                            val newOffset = swipeOffset + dragAmount

                            if (isOpen && dragAmount > 0) {
                                isOpen = false
                            }
                            else if (newOffset <= 0f && newOffset >= -80f) {
                                val shouldOpen = newOffset < -10f
                                if (shouldOpen != isOpen) {
                                    isOpen = shouldOpen
                                }
                            }
                        }
                    )
                }
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Primera línea: Nombre del horno (izquierda) y fecha (derecha)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activity.kilnName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = LocalKilnColors.current.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                    
                    Text(
                        text = activity.executionDate,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = LocalKilnColors.current.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
                
                // Segunda línea: Nombre del programa
                Text(
                    text = activity.programName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = LocalKilnColors.current.body,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Tercera línea: Detalles (temperatura y duración)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Detalle de temperatura
                    ActivityDetailItem(
                        icon = R.drawable.etapa_temp,
                        value = "${activity.maxTemperature}°C",
                        label = "Temp max."
                    )
                    
                    // Detalle de duración
                    ActivityDetailItem(
                        icon = R.drawable.etapa_mes,
                        value = activity.duration,
                        label = "Duracion"
                    )
                }
            }
        }
    }
}

// Componente para los detalles individuales (temperatura/duración) - igual que ProgramCard
@Composable
private fun ActivityDetailItem(
    icon: Int,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        // Icono con fondo azul claro (igual que ProgramCard)
        Box(
            modifier = Modifier
                .requiredSize(35.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(color = LocalKilnColors.current.screenBackground)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = LocalKilnColors.current.body,
                modifier = Modifier.requiredSize(24.dp)
            )
        }
        
        // Texto del valor y etiqueta (igual que ProgramCard)
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = LocalKilnColors.current.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = LocalKilnColors.current.accent,
                    fontSize = 12.sp
                )
            )
        }
    }
}

// Componente para las acciones de swipe (solo eliminar)
@Composable
private fun ActivitySwipeActions(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(80.dp)
            .requiredHeight(height = 130.dp)
    ) {
        // Botón de Borrar (azul) - mismo estilo que ProgramSwipeActions
        Box(
            modifier = Modifier
                .align(alignment = Alignment.CenterEnd)
                .requiredSize(size = 64.dp)
                .clip(shape = RoundedCornerShape(12.dp))
                .background(color = LocalKilnColors.current.fab) // Mismo color que ProgramSwipeActions
                .clickable { onDelete() }
        ) {
            Box(
                modifier = Modifier
                    .align(alignment = Alignment.Center)
                    .offset(y = 4.dp)
                    .requiredWidth(width = 36.dp)
                    .requiredHeight(height = 48.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.typedeletesize16colorblack),
                    contentDescription = "Delete",
                    modifier = Modifier
                        .align(alignment = Alignment.TopCenter)
                        .requiredWidth(width = 25.dp)
                        .requiredHeight(height = 24.dp)
                )
                Text(
                    text = "borrar",
                    color = LocalKilnColors.current.onAccent,
                    lineHeight = 1.em,
                    style = TextStyle(fontSize = 10.sp),
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .offset(x = 6.dp, y = 2.dp)
                        .requiredWidth(width = 36.dp)
                        .requiredHeight(height = 23.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ActivityScreenPreview() {
    MySmartKilnTheme {
        ActivityScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun ActivityCardPreview() {
    MySmartKilnTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActivityCard(
                activity = ActivityItem(
                    id = "1",
                    kilnBrand = "AFRA",
                    kilnVolume = "45Lt",
                    kilnName = "AFRA 45Lt",
                    executionDate = "26/09/2025",
                    programName = "Esmalte de Gres",
                    maxTemperature = "1120",
                    duration = "6h 30min"
                ),
                onDelete = { /* Delete action */ }
            )
        }
    }
}