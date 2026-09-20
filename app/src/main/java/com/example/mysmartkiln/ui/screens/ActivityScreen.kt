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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.mysmartkiln.ui.components.ActivityLoggerCurveScreen
import com.example.mysmartkiln.ui.components.EspBrandHeader
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.OrangeHexagon
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.components.BottomNavBarBackground
import com.example.mysmartkiln.ui.components.SearchBar
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme
import com.example.mysmartkiln.ui.viewmodels.ActivityUiItem
import com.example.mysmartkiln.ui.viewmodels.ActivityViewModel
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel

// UI de tarjeta (compatible con layout existente)
data class ActivityItem(
    val id: String,
    val kilnBrand: String,
    val kilnVolume: String,
    val kilnName: String,
    val executionDate: String,
    val programName: String,
    val maxTemperature: String,
    val duration: String,
    val timestamp: Long = System.currentTimeMillis(),
    val outcomeLabel: String? = null
)

private fun ActivityUiItem.toActivityItem() = ActivityItem(
    id = id,
    kilnBrand = kilnBrand,
    kilnVolume = kilnVolume,
    kilnName = kilnName,
    executionDate = executionDate,
    programName = programName,
    maxTemperature = maxTemperature,
    duration = duration,
    timestamp = timestamp,
    outcomeLabel = outcomeLabel
)

@Composable
fun ActivityScreen(
    onNavigateToKilns: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onCreateProgram: () -> Unit = {},
    onNavigateToKilnsWithScan: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ActivityViewModel = viewModel()
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<ActivityUiItem?>(null) }
    val activitiesRaw by viewModel.activities.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val curveDetail by viewModel.curveDetail.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    if (curveDetail != null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(color = LocalKilnColors.current.screenBackground)
        ) {
            ActivityLoggerCurveScreen(
                detail = curveDetail!!,
                onBack = { viewModel.closeCurveDetail() },
                modifier = Modifier.fillMaxSize()
            )

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

            Box(
                modifier = Modifier
                    .align(alignment = Alignment.BottomStart)
                    .fillMaxWidth()
                    .requiredHeight(height = 200.dp)
                    .zIndex(2f)
            ) {
                BottomNavBarBackground(
                    modifier = Modifier
                        .align(alignment = Alignment.TopStart)
                        .offset(x = 0.dp, y = 60.dp)
                )
                ResponsiveBottomNavigation(
                    onKilnsClick = onNavigateToKilns,
                    onProgramsClick = onNavigateToPrograms,
                    onActivityClick = { viewModel.closeCurveDetail() },
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
        }
        return
    }

    val filtered = remember(activitiesRaw, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) activitiesRaw
        else activitiesRaw.filter {
            it.programName.contains(q, ignoreCase = true) ||
                it.kilnName.contains(q, ignoreCase = true) ||
                it.kilnBrand.contains(q, ignoreCase = true)
        }
    }
    val grouped = remember(filtered) {
        filtered.groupBy { it.monthLabel }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = LocalKilnColors.current.screenBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 10.dp)
        ) {
            val kilnColors = LocalKilnColors.current

            if (kilnColors.isEsp) {
                EspBrandHeader(
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 48.dp,
                        bottom = 8.dp
                    )
                )
                Text(
                    text = "Actividad",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = kilnColors.title,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(
                        start = 24.dp,
                        end = 24.dp,
                        top = 8.dp,
                        bottom = 16.dp
                    )
                )
            } else {
                Text(
                    text = "Actividad",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = kilnColors.title,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 70.dp, bottom = 16.dp)
                )
            }
            
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Buscar...",
                onSearchClick = { },
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
            )

            when {
                isLoading && filtered.isEmpty() -> {
                    Text(
                        text = "Cargando…",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = LocalKilnColors.current.title
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(24.dp)
                    )
                }
                error != null && filtered.isEmpty() -> {
                    Text(
                        text = error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = LocalKilnColors.current.title
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(24.dp)
                    )
                }
                filtered.isEmpty() -> {
                    Text(
                        text = "Todavía no hay actividad.\nLas grabaciones del DataLogger y las cocciones finalizadas aparecerán acá.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = LocalKilnColors.current.title
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(24.dp)
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 29.dp, end = 24.dp)
                    ) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            grouped.forEach { (month, monthItems) ->
                                item(key = "header_$month") {
                                    Text(
                                        text = month,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = LocalKilnColors.current.title,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                                    )
                                }
                                items(monthItems, key = { it.id }) { ui ->
                                    ActivityCard(
                                        activity = ui.toActivityItem(),
                                        onClick = {
                                            if (ui.canOpenCurve) {
                                                viewModel.openActivity(ui)
                                            }
                                        },
                                        onDelete = { pendingDelete = ui }
                                    )
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        
            Spacer(
                modifier = Modifier.height(if (kilnColors.isEsp) 110.dp else 160.dp)
            )
        }

        pendingDelete?.let { target ->
            DeleteActivityConfirmDialog(
                activityName = target.programName.ifBlank { "esta actividad" },
                hasCloudCurve = target.isDataLogger || !target.recordingId.isNullOrBlank(),
                onDismiss = { pendingDelete = null },
                onConfirm = {
                    pendingDelete = null
                    viewModel.deleteActivity(target)
                }
            )
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
    onClick: () -> Unit = {},
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
                .clickable(enabled = !isOpen) { onClick() }
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
                
                // Segunda línea: programa + chip Finalizado / Interrumpida
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activity.programName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = LocalKilnColors.current.body,
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    activity.outcomeLabel?.let { label ->
                        val interrupted = label.equals("Interrumpida", ignoreCase = true)
                        val kilnColors = LocalKilnColors.current
                        val bg = if (interrupted) {
                            kilnColors.danger.copy(alpha = 0.18f)
                        } else {
                            kilnColors.success.copy(alpha = 0.18f)
                        }
                        val fg = if (interrupted) kilnColors.danger else kilnColors.success
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = label.uppercase(),
                                color = fg,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                lineHeight = 12.sp
                            )
                        }
                    }
                }
                
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
    val kilnColors = LocalKilnColors.current
    Box(
        modifier = modifier
            .width(80.dp)
            .requiredHeight(height = 130.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        val shape = RoundedCornerShape(12.dp)
        if (kilnColors.isEsp) {
            val tint = kilnColors.title.copy(alpha = 0.72f)
            Column(
                modifier = Modifier
                    .requiredSize(width = 62.dp, height = 72.dp)
                    .border(1.dp, tint, shape)
                    .clip(shape)
                    .clickable(onClick = onDelete),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.typedeletesize16colorblack),
                    contentDescription = "BORRAR",
                    tint = tint,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "BORRAR",
                    color = tint,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .requiredSize(64.dp)
                    .clip(shape)
                    .background(AppColors.Primary400)
                    .clickable(onClick = onDelete),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.typedeletesize16colorblack),
                    contentDescription = "borrar",
                    tint = AppColors.White,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "borrar",
                    color = AppColors.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun DeleteActivityConfirmDialog(
    activityName: String,
    hasCloudCurve: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val kilnColors = LocalKilnColors.current
    val body = if (hasCloudCurve) {
        "Vas a eliminar «$activityName».\n\n" +
            "También se borrará de la nube (Firebase) y de este teléfono, " +
            "incluida la curva de temperatura. Esta acción no se puede deshacer."
    } else {
        "Vas a eliminar «$activityName».\n\n" +
            "También se borrará de la nube. Esta acción no se puede deshacer."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "¿Eliminar actividad?",
                fontWeight = FontWeight.Bold,
                color = kilnColors.title
            )
        },
        text = {
            Text(
                text = body,
                color = kilnColors.muted,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Eliminar", color = AppColors.Primary400, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = kilnColors.muted)
            }
        }
    )
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
                    duration = "6h 30m"
                ),
                onDelete = { /* Delete action */ }
            )
        }
    }
}