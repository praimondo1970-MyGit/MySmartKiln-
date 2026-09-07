package com.example.mysmartkiln.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel
import com.example.mysmartkiln.ui.viewmodels.ProgramStage
import com.example.mysmartkiln.utils.KilnNetworkConnector

@Composable
fun KilnStatusScreen(
    kilnMacAddress: String,
    viewModel: KilnDetailViewModel,
    onLoadProgram: () -> Unit = {},
    onConnectToKiln: () -> Unit = {}
) {
    val context = LocalContext.current
    val kilnStatus by viewModel.kilnStatus.collectAsState()
    val kilnProgram by viewModel.kilnProgram.collectAsState()
    val isUploadingProgram by viewModel.isUploadingProgram.collectAsState()
    val isConnectingToAp by viewModel.isConnectingToAp.collectAsState()
    val shouldShowManualWifiDialog by viewModel.shouldShowManualWifiDialog.collectAsState()
    val kilnColors = LocalKilnColors.current

    // Pedimos el permiso runtime que necesita WifiNetworkSpecifier
    // (NEARBY_WIFI_DEVICES en Android 13+, ACCESS_FINE_LOCATION antes). Sin
    // este permiso, requestNetwork() lanza SecurityException y no podemos
    // hacer la auto-conexión al AP del horno.
    val nearbyWifiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Resultado se consulta vía hasRequiredPermissions cuando haga falta */ }

    LaunchedEffect(kilnMacAddress) {
        if (kilnProgram.name.isEmpty()) {
            viewModel.loadKilnData(kilnMacAddress)
        }
        viewModel.ensureProgramStagesLoaded()
        if (KilnNetworkConnector.isSupported &&
            !KilnNetworkConnector.hasRequiredPermissions(context)
        ) {
            nearbyWifiPermissionLauncher.launch(
                KilnNetworkConnector.requiredRuntimePermission()
            )
        }
    }

    if (shouldShowManualWifiDialog) {
        KilnOperateOfflineDialog(onDismiss = { viewModel.clearManualWifiDialogRequest() })
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = kilnColors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = if (kilnColors.isEsp) 0.dp else 4.dp),
        border = if (kilnColors.isEsp) {
            androidx.compose.foundation.BorderStroke(1.dp, kilnColors.cardBorder)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Program Name and Time Row - only show if program is loaded
            if (kilnProgram.name.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-5).dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side - Program Name
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = kilnProgram.name,
                            color = kilnColors.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    // Separator line
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(kilnColors.cardBorder)
                    )

                    // Right side - Total Duration
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.icon_time),
                                contentDescription = "Time",
                                tint = kilnColors.body,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = kilnStatus.totalTime,
                                color = kilnColors.body,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "Duración",
                            color = kilnColors.muted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Temperature Display with optional circular progress
            Box(
                modifier = Modifier.size(235.dp),
                contentAlignment = Alignment.Center
            ) {
                // Circular progress arcs showing stages (only if program is loaded)
                if (kilnProgram.name.isNotEmpty() && kilnProgram.stages.isNotEmpty()) {
                    ProgressArcs(
                        stages = kilnProgram.stages,
                        elapsedMinutes = kilnStatus.elapsedMinutes
                    )
                }
                
                // Temperature display in the center (always visible)
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Normal,
                                color = kilnColors.body
                            )
                        ) {
                            append("${kilnStatus.currentTemperature.toInt()}")
                        }
                        withStyle(
                            style = SpanStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Normal,
                                color = kilnColors.body,
                                baselineShift = BaselineShift(0.6f)
                            )
                        ) {
                            append("°C")
                        }
                    },
                    modifier = Modifier.offset(y = (-10).dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Push content to make space for bottom elements
            Spacer(modifier = Modifier.weight(1f))
            
            // Max Temperature (left) and Remaining Time (right) - only show if program is loaded
            if (kilnProgram.name.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side - Max Temperature
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.etapa_temp),
                                contentDescription = "Max Temperature",
                                tint = kilnColors.body,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${kilnStatus.targetTemperature.toInt()}°C",
                                color = kilnColors.body,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                        Text(
                            text = "Temp max",
                            color = kilnColors.muted,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 32.dp) // Alineado con la temperatura (24dp ícono + 8dp spacer)
                        )
                    }
                    
                    // Right side - Remaining Time
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.icon_time),
                                contentDescription = "Remaining",
                                tint = kilnColors.body,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = kilnStatus.remainingTime,
                                color = kilnColors.body,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "Pendiente",
                            color = kilnColors.muted,
                            fontSize = 13.sp
                        )
                    }
                }
                
                // Spacer between Max Temp/Remaining and Buttons
                // 10dp offset adicional
                Spacer(modifier = Modifier.height(10.dp))
            }

            val hasProgram = kilnProgram.name.isNotEmpty()
            val espStatus = kilnStatus.espStatus

            // Mostramos los botones SIEMPRE en su posición habitual. Si el
            // teléfono no está en la red WiFi del horno, el clic abre el
            // diálogo guía «Conéctate al horno» en vez de mandar el comando.
            // Solo se desactivan visualmente mientras se sube un perfil.
            val buttonsClickable = !isUploadingProgram && !isConnectingToAp
            // Stop solo tiene sentido si hay cocción activa o pausada; si ya
            // está detenido/idle no debe intentar conectar ni mandar STOP.
            val canStop = kilnStatus.isRunning || kilnStatus.isPaused ||
                espStatus in setOf("CALENTANDO", "PAUSADO")

            // El ViewModel intenta primero conectarse al AP del horno por
            // WifiNetworkSpecifier (Android 10+). Si Android no lo permite,
            // recién ahí marca shouldShowManualWifiDialog y mostramos el
            // diálogo con instrucciones manuales (Settings).
            fun guardedAction(action: () -> Unit): () -> Unit = action

            if (isUploadingProgram) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            when {
                !hasProgram -> {
                    Button(
                        onClick = guardedAction(onLoadProgram),
                        enabled = buttonsClickable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = kilnColors.accent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cargar Programa",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                espStatus == "INTERRUMPIDO" -> {
                    Text(
                        text = "Programa interrumpido. Usá el aviso para reanudar o cancelar.",
                        color = kilnColors.muted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                    )
                }
                espStatus == "FINALIZADO" -> {
                    Button(
                        onClick = guardedAction { viewModel.stopKiln() },
                        enabled = buttonsClickable && canStop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            // Mockup Fase A: Stop = negro/gris oscuro; Iniciar = naranja
                            containerColor = if (kilnColors.isEsp) Color.Black else kilnColors.accent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_stop),
                            contentDescription = "Stop",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stop",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when {
                            kilnStatus.isPaused -> {
                                Button(
                                    onClick = guardedAction { viewModel.playKiln() },
                                    enabled = buttonsClickable,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = kilnColors.fab
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.icon_play),
                                        contentDescription = "Iniciar",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Iniciar",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            espStatus == "CALENTANDO" -> {
                                Button(
                                    onClick = guardedAction { viewModel.pauseKiln() },
                                    enabled = buttonsClickable,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = kilnColors.fab
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.icon_pause),
                                        contentDescription = "Pausa",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Pausa",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            else -> {
                                Button(
                                    onClick = guardedAction { viewModel.playKiln() },
                                    enabled = buttonsClickable,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = kilnColors.fab
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.icon_play),
                                        contentDescription = "Iniciar",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Iniciar",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = guardedAction { viewModel.stopKiln() },
                            enabled = buttonsClickable && canStop,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                // Mockup Fase A: Stop = negro; Iniciar/Pausa = naranja (fab)
                                containerColor = if (kilnColors.isEsp) Color.Black else kilnColors.accent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.icon_stop),
                                contentDescription = "Stop",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Stop",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Program Progress Bar
 * Displays horizontal progress bar divided into segments representing each stage
 */
@Composable
fun ProgramProgressBar(
    stages: List<ProgramStage>,
    elapsedMinutes: Int,
    modifier: Modifier = Modifier
) {
    if (stages.isEmpty()) return
    val kilnColors = LocalKilnColors.current

    val totalMinutes = stages.sumOf { it.durationMinutes.coerceAtLeast(0) }
    if (totalMinutes <= 0) return

    // Calculate how much time has elapsed in each stage
    var remainingElapsed = elapsedMinutes

    Row(
        modifier = modifier.height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        stages.forEach { stage ->
            val duration = stage.durationMinutes.coerceAtLeast(0)
            val stageWeight = duration.toFloat() / totalMinutes.toFloat()

            val stageProgress = when {
                duration <= 0 || remainingElapsed <= 0 -> 0f
                remainingElapsed >= duration -> {
                    remainingElapsed -= duration
                    1f
                }
                else -> {
                    val progress = remainingElapsed.toFloat() / duration.toFloat()
                    remainingElapsed = 0
                    progress
                }
            }

            Box(
                modifier = Modifier
                    .weight(stageWeight.coerceAtLeast(0.0001f))
                    .fillMaxHeight()
            ) {
                // Background (gray)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = Color.LightGray,
                            shape = RoundedCornerShape(4.dp)
                        )
                )
                
                // Progress (blue)
                if (stageProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(stageProgress)
                            .background(
                                color = kilnColors.body,
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun TemperatureGauge(
    currentTemperature: Float,
    targetTemperature: Float,
    programStages: List<com.example.mysmartkiln.ui.viewmodels.ProgramStage> = emptyList(),
    elapsedMinutes: Int = 0
) {
    val kilnColors = LocalKilnColors.current
    Box(
        modifier = Modifier.size(210.dp),
        contentAlignment = Alignment.Center
    ) {
        // Temperature gauge background (simplified)
        Box(
            modifier = Modifier
                .size(210.dp)
                .background(
                    Color.LightGray.copy(alpha = 0.2f),
                    CircleShape
                )
        )
        
        // Arcos de progreso por etapas (desde 135° hasta 405°, 270° total)
        ProgressArcs(stages = programStages, elapsedMinutes = elapsedMinutes)

        // Temperature display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = buildAnnotatedString {
                    // Número de temperatura en 42sp
                    withStyle(
                        style = SpanStyle(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Normal,
                            color = kilnColors.body
                        )
                    ) {
                        append("${currentTemperature.toInt()}")
                    }
                    // Símbolo °C en 21sp (50% de 42sp)
                    withStyle(
                        style = SpanStyle(
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Normal,
                            color = kilnColors.body
                        )
                    ) {
                        append("°C")
                    }
                }
            )
        }
    }
}

@Composable
fun ProgressArcs(
    stages: List<ProgramStage> = emptyList(),
    elapsedMinutes: Int = 0
) {
    if (stages.isEmpty()) return

    val kilnColors = LocalKilnColors.current
    val stageDurations = stages.map { it.durationMinutes.coerceAtLeast(0) }
    val totalMinutes = stageDurations.sum()
    if (totalMinutes <= 0) return

    val totalArcDegrees = 270f
    val separatorDegrees = 3f
    val numSeparators = (stages.size - 1).coerceAtLeast(0)
    val availableDegrees = totalArcDegrees - (separatorDegrees * numSeparators)

    Canvas(modifier = Modifier.size(235.dp)) {
        val strokeWidth = 8.dp.toPx()
        val strokeStyle = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
        // Inset para que el stroke no se recorte en el borde del Canvas.
        val arcInset = strokeWidth / 2f
        val arcSize = Size(
            width = size.minDimension - strokeWidth,
            height = size.minDimension - strokeWidth
        )
        val arcTopLeft = Offset(arcInset, arcInset)
        val arcRadius = arcSize.minDimension / 2f

        var currentAngle = 135f
        var accumulatedMinutes = 0

        stages.forEachIndexed { index, stage ->
            val duration = stageDurations[index]
            val stageProportion = duration.toFloat() / totalMinutes.toFloat()
            val stageSweepAngle = availableDegrees * stageProportion

            val stageStartMinute = accumulatedMinutes
            val stageEndMinute = accumulatedMinutes + duration

            val completedInThisStage = when {
                duration <= 0 -> 0
                elapsedMinutes >= stageEndMinute -> duration
                elapsedMinutes > stageStartMinute -> elapsedMinutes - stageStartMinute
                else -> 0
            }

            val completedSweepAngle = if (duration > 0) {
                stageSweepAngle * (completedInThisStage.toFloat() / duration.toFloat())
            } else {
                0f
            }

            if (stageSweepAngle > 0f) {
                drawArc(
                    color = kilnColors.track,
                    startAngle = currentAngle,
                    sweepAngle = stageSweepAngle,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = strokeStyle
                )
            }

            if (completedSweepAngle > 0f) {
                drawArc(
                    color = kilnColors.progress,
                    startAngle = currentAngle,
                    sweepAngle = completedSweepAngle,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = strokeStyle
                )
            }

            if (index < stages.size - 1) {
                val separatorAngle = currentAngle + stageSweepAngle + (separatorDegrees / 2f)
                val separatorRadians = Math.toRadians(separatorAngle.toDouble())
                val startRadius = arcRadius - 8.dp.toPx()
                val endRadius = arcRadius + 8.dp.toPx()

                drawLine(
                    color = kilnColors.cardBackground,
                    start = Offset(
                        x = center.x + (startRadius * Math.cos(separatorRadians)).toFloat(),
                        y = center.y + (startRadius * Math.sin(separatorRadians)).toFloat()
                    ),
                    end = Offset(
                        x = center.x + (endRadius * Math.cos(separatorRadians)).toFloat(),
                        y = center.y + (endRadius * Math.sin(separatorRadians)).toFloat()
                    ),
                    strokeWidth = 2.dp.toPx()
                )
            }

            currentAngle += stageSweepAngle + separatorDegrees
            accumulatedMinutes += duration
        }
    }
}

/**
 * Convierte un String de tiempo en formato "Xh YYmin" a minutos totales
 * Ejemplos: "1h 30min" -> 90, "0h 15min" -> 15, "2h 00min" -> 120
 */
private fun parseTimeToMinutes(timeString: String): Int {
    var totalMinutes = 0
    
    // Extraer horas (ejemplo: "1h 30min" -> "1")
    val hoursMatch = Regex("""(\d+)h""").find(timeString)
    if (hoursMatch != null) {
        totalMinutes += hoursMatch.groupValues[1].toInt() * 60
    }
    
    // Extraer minutos (ejemplo: "1h 30min" -> "30")
    val minutesMatch = Regex("""(\d+)min""").find(timeString)
    if (minutesMatch != null) {
        totalMinutes += minutesMatch.groupValues[1].toInt()
    }
    
    return totalMinutes
}

