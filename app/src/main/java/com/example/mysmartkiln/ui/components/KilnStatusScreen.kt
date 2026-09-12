package com.example.mysmartkiln.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel
import com.example.mysmartkiln.ui.viewmodels.TemperatureSample
import com.example.mysmartkiln.utils.KilnNetworkConnector
import kotlin.math.roundToInt

/**
 * Pestaña Estado — Fase B: layout cercano al mockup ESP
 * (herradura por temperatura, velocidad real, tiles + avance de ciclo).
 */
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
    val temperatureHistory by viewModel.temperatureHistory.collectAsState()
    val isUploadingProgram by viewModel.isUploadingProgram.collectAsState()
    val isConnectingToAp by viewModel.isConnectingToAp.collectAsState()
    val shouldShowManualWifiDialog by viewModel.shouldShowManualWifiDialog.collectAsState()
    val kilnColors = LocalKilnColors.current

    val nearbyWifiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

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

    val hasProgram = kilnProgram.name.isNotEmpty()
    val espStatus = kilnStatus.espStatus
    val buttonsClickable = !isUploadingProgram && !isConnectingToAp
    val canStop = kilnStatus.isRunning || kilnStatus.isPaused ||
        espStatus in setOf("CALENTANDO", "PAUSADO")

    val totalMinutes = programTotalMinutes(
        stagesSum = kilnProgram.stages.sumOf { it.durationMinutes },
        formattedTotal = kilnStatus.totalTime
    )
    val cycleProgress = if (totalMinutes > 0) {
        (kilnStatus.elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val ascentRate = rememberAscentRateCPerMin(temperatureHistory)
    val scaleMax = tempScaleMax(kilnStatus.targetTemperature)

    val stageLabel = when {
        kilnStatus.totalStages > 0 && kilnStatus.currentStage > 0 ->
            "${kilnStatus.currentStage}/${kilnStatus.totalStages}"
        kilnProgram.stage.isNotBlank() && kilnProgram.stage != "0/0" -> kilnProgram.stage
        kilnProgram.stages.isNotEmpty() -> "0/${kilnProgram.stages.size}"
        else -> "—"
    }

    // ESP layout model:
    // 1) El pager llega detrás del nav/FAB → hay que RESTAR ese clearance.
    // 2) fillMaxHeight+scroll+paddingBottom creaba “hueco muerto” bajo botones
    //    y apilaba herradura/tarjeta/botones arriba.
    // 3) weight sin restar FAB empujaba botones dentro del cradle.
    // Solución: Column de altura (maxHeight - fabClearance) + Spacers weight iguales.
    if (kilnColors.isEsp) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            val fabClearance = 112.dp
            val layoutHeight = (maxHeight - fabClearance).coerceAtLeast(320.dp)
            val canDistribute = layoutHeight >= 420.dp

            if (canDistribute) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(layoutHeight)
                        .padding(horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(15.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = layoutHeight * 0.40f + 10.dp)
                            .padding(horizontal = 35.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EspTemperatureHorseshoe(
                            currentTempC = kilnStatus.currentTemperature,
                            scaleMaxC = scaleMax,
                            modifier = Modifier.fillMaxSize()
                        )
                        StatusGaugeLabels(
                            currentTempC = kilnStatus.currentTemperature,
                            ascentRate = ascentRate
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.45f))

                    if (hasProgram) {
                        StatusSummaryCard(
                            programName = kilnProgram.name,
                            duration = kilnStatus.totalTime.ifBlank { "—" },
                            objective = "${kilnStatus.targetTemperature.toInt()}°C",
                            remaining = kilnStatus.remainingTime.ifBlank { "—" },
                            stage = stageLabel,
                            cycleProgress = cycleProgress,
                            modifier = Modifier.offset(y = (-10).dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1.55f))

                    if (isUploadingProgram) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            color = kilnColors.accent,
                            trackColor = kilnColors.track
                        )
                    }

                    StatusActionButtons(
                        hasProgram = hasProgram,
                        espStatus = espStatus,
                        isPaused = kilnStatus.isPaused,
                        buttonsClickable = buttonsClickable,
                        canStop = canStop,
                        onLoadProgram = onLoadProgram,
                        onPlay = { viewModel.playKiln() },
                        onPause = { viewModel.pauseKiln() },
                        onStop = { viewModel.stopKiln() },
                        modifier = Modifier.offset(y = (-50).dp)
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 2.dp)
                        .padding(bottom = fabClearance),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EspTemperatureHorseshoe(
                            currentTempC = kilnStatus.currentTemperature,
                            scaleMaxC = scaleMax,
                            modifier = Modifier.fillMaxWidth()
                        )
                        StatusGaugeLabels(
                            currentTempC = kilnStatus.currentTemperature,
                            ascentRate = ascentRate
                        )
                    }
                    Spacer(modifier = Modifier.height(0.dp))
                    if (hasProgram) {
                        StatusSummaryCard(
                            programName = kilnProgram.name,
                            duration = kilnStatus.totalTime.ifBlank { "—" },
                            objective = "${kilnStatus.targetTemperature.toInt()}°C",
                            remaining = kilnStatus.remainingTime.ifBlank { "—" },
                            stage = stageLabel,
                            cycleProgress = cycleProgress
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    if (isUploadingProgram) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            color = kilnColors.accent,
                            trackColor = kilnColors.track
                        )
                    }
                    StatusActionButtons(
                        hasProgram = hasProgram,
                        espStatus = espStatus,
                        isPaused = kilnStatus.isPaused,
                        buttonsClickable = buttonsClickable,
                        canStop = canStop,
                        onLoadProgram = onLoadProgram,
                        onPlay = { viewModel.playKiln() },
                        onPause = { viewModel.pauseKiln() },
                        onStop = { viewModel.stopKiln() },
                        modifier = Modifier.offset(y = (-50).dp)
                    )
                }
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = kilnColors.cardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            border = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    EspTemperatureHorseshoe(
                        currentTempC = kilnStatus.currentTemperature,
                        scaleMaxC = scaleMax,
                        modifier = Modifier.fillMaxWidth()
                    )
                    StatusGaugeLabels(
                        currentTempC = kilnStatus.currentTemperature,
                        ascentRate = ascentRate
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (hasProgram) {
                    StatusSummaryCard(
                        programName = kilnProgram.name,
                        duration = kilnStatus.totalTime.ifBlank { "—" },
                        objective = "${kilnStatus.targetTemperature.toInt()}°C",
                        remaining = kilnStatus.remainingTime.ifBlank { "—" },
                        stage = stageLabel,
                        cycleProgress = cycleProgress
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (isUploadingProgram) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        color = kilnColors.accent,
                        trackColor = kilnColors.track
                    )
                }

                StatusActionButtons(
                    hasProgram = hasProgram,
                    espStatus = espStatus,
                    isPaused = kilnStatus.isPaused,
                    buttonsClickable = buttonsClickable,
                    canStop = canStop,
                    onLoadProgram = onLoadProgram,
                    onPlay = { viewModel.playKiln() },
                    onPause = { viewModel.pauseKiln() },
                    onStop = { viewModel.stopKiln() }
                )
            }
        }
    }
}

@Composable
private fun StatusGaugeLabels(
    currentTempC: Float,
    ascentRate: Float?
) {
    val kilnColors = LocalKilnColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(23.dp))
        Text(
            text = "TEMPERATURA ACTUAL",
            color = kilnColors.body,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${currentTempC.toInt()}",
                color = kilnColors.title,
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 84.sp
            )
            Text(
                text = "°C",
                color = kilnColors.title,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        if (kilnColors.isEsp) {
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(1.dp)
                    .background(kilnColors.cardBorder)
            )
            Spacer(modifier = Modifier.height(3.dp))
        }
        Text(
            text = "VELOCIDAD DE ASCENSO",
            color = kilnColors.body,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp
        )
        Text(
            text = formatAscentRate(ascentRate),
            color = if (ascentRate != null && ascentRate >= 0f) {
                kilnColors.success
            } else {
                kilnColors.muted
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun StatusSummaryCard(
    programName: String,
    duration: String,
    objective: String,
    remaining: String,
    stage: String,
    cycleProgress: Float,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    val panelColor = if (kilnColors.isEsp) kilnColors.cardBackground else kilnColors.screenBackground
    val separatorColor = if (kilnColors.isEsp) Color(0xFF5C5C5C) else kilnColors.cardBorder
    val stageValue = if (stage == "—" || stage.isBlank()) "—" else "ETAPA $stage"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(panelColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 7.5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_programs),
                    contentDescription = null,
                    tint = kilnColors.accent,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = programName,
                    color = kilnColors.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_duration),
                        contentDescription = null,
                        tint = kilnColors.title,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = duration,
                        color = kilnColors.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = "DURACIÓN",
                    color = kilnColors.body,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(separatorColor)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryTile(
                icon = R.drawable.icon_thermometer,
                value = objective,
                label = "OBJETIVO",
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(44.dp)
                    .background(separatorColor)
            )
            SummaryTile(
                icon = R.drawable.icon_stage,
                value = stageValue,
                label = "DEL CICLO",
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(44.dp)
                    .background(separatorColor)
            )
            SummaryTile(
                icon = R.drawable.icon_remaining,
                value = remaining,
                label = "TIEMPO RESTANTE",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "AVANCE DEL CICLO",
            color = kilnColors.body,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp
        )
        Spacer(modifier = Modifier.height(1.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-2).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
            ) {
                val y = size.height / 2f
                val track = kilnColors.track
                val accent = kilnColors.accent
                drawLine(
                    color = track,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round
                )
                if (cycleProgress > 0f) {
                    drawLine(
                        color = accent,
                        start = Offset(0f, y),
                        end = Offset(size.width * cycleProgress.coerceIn(0f, 1f), y),
                        strokeWidth = size.height,
                        cap = StrokeCap.Round
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${(cycleProgress * 100f).roundToInt()}%",
                color = kilnColors.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SummaryTile(
    icon: Int,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = kilnColors.accent,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(24.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(horizontalAlignment = Alignment.Start) {
            val tempParts = Regex("""^(\d+)(°C)$""").find(value)
            if (tempParts != null) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = tempParts.groupValues[1],
                        color = kilnColors.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.3.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tempParts.groupValues[2],
                        color = kilnColors.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            } else {
                Text(
                    text = value,
                    color = kilnColors.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = label,
                color = kilnColors.body,
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StatusActionButtons(
    hasProgram: Boolean,
    espStatus: String,
    isPaused: Boolean,
    buttonsClickable: Boolean,
    canStop: Boolean,
    onLoadProgram: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    Box(modifier = modifier.fillMaxWidth()) {
    when {
        !hasProgram -> {
            Button(
                onClick = onLoadProgram,
                enabled = buttonsClickable,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = kilnColors.accent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Cargar Programa",
                    color = kilnColors.onAccent,
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
                modifier = Modifier.fillMaxWidth()
            )
        }
        espStatus == "FINALIZADO" -> {
            Button(
                onClick = onStop,
                enabled = buttonsClickable && canStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (kilnColors.isEsp) Color.Black else kilnColors.accent
                ),
                border = if (kilnColors.isEsp) BorderStroke(1.dp, kilnColors.cardBorder) else null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_stop),
                    contentDescription = "Stop",
                    tint = kilnColors.onAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "STOP",
                    color = kilnColors.onAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val primaryLabel: String
                val primaryIcon: Int
                val primaryAction: () -> Unit
                when {
                    isPaused -> {
                        primaryLabel = "INICIAR"
                        primaryIcon = R.drawable.icon_play
                        primaryAction = onPlay
                    }
                    espStatus == "CALENTANDO" -> {
                        primaryLabel = "PAUSA"
                        primaryIcon = R.drawable.icon_pause
                        primaryAction = onPause
                    }
                    else -> {
                        primaryLabel = "INICIAR"
                        primaryIcon = R.drawable.icon_play
                        primaryAction = onPlay
                    }
                }
                val primaryButtonColor = if (kilnColors.isEsp) {
                    Color(0xFFFF7811) // punto medio entre #FF9800 y #FF5722
                } else {
                    kilnColors.fab
                }
                Button(
                    onClick = primaryAction,
                    enabled = buttonsClickable,
                    modifier = Modifier
                        .weight(1.15f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryButtonColor),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = primaryIcon),
                        contentDescription = primaryLabel,
                        tint = kilnColors.onFab,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = primaryLabel,
                        color = kilnColors.onFab,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Button(
                    onClick = onStop,
                    enabled = buttonsClickable && canStop,
                    modifier = Modifier
                        .weight(0.85f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (kilnColors.isEsp) Color.Black else kilnColors.accent,
                        disabledContainerColor = kilnColors.cardBorder,
                        disabledContentColor = kilnColors.muted
                    ),
                    border = if (kilnColors.isEsp) {
                        BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    } else {
                        null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_stop),
                        contentDescription = "Stop",
                        tint = kilnColors.onAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "STOP",
                        color = kilnColors.onAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
    }
}

/**
 * Herradura circular. Respeta maxHeight del padre (para layout con weight)
 * midiendo el diámetro por el menor entre ancho disponible y alto útil.
 */
@Composable
fun EspTemperatureHorseshoe(
    currentTempC: Float,
    scaleMaxC: Float,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    val trackColor = kilnColors.track
    val progressStart = kilnColors.accent
    val progressEnd = kilnColors.accentMuted
    val maxC = scaleMaxC.coerceAtLeast(1f)
    val progress = (currentTempC / maxC).coerceIn(0f, 1f)

    val startAngle = 145f
    val sweep = 250f
    // Relación alto-canvas / diámetro para cubrir puntas del arco.
    val arcHeightRatio = 0.78f

    BoxWithConstraints(modifier = modifier) {
        val boundedHeight = maxHeight.value.isFinite() && maxHeight < maxWidth * 3f
        val canvasHeight = if (boundedHeight) {
            maxHeight
        } else {
            maxWidth * 0.80f * arcHeightRatio
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight)
        ) {
            val strokeWidth = 17.dp.toPx()
            val strokeStyle = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val inset = strokeWidth / 2f
            val diameterByHeight = (size.height - inset) / arcHeightRatio
            val diameterByWidth = size.width - inset * 2f
            val diameter = (minOf(diameterByHeight, diameterByWidth) + 10.dp.toPx())
                .coerceAtMost(size.width - inset * 2f)
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = inset
            )
            val arcSize = Size(width = diameter, height = diameter)

            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = strokeStyle
            )
            if (progress > 0f) {
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(progressStart, progressEnd),
                        start = Offset(topLeft.x, topLeft.y + diameter * 0.5f),
                        end = Offset(topLeft.x + diameter, topLeft.y + diameter * 0.5f)
                    ),
                    startAngle = startAngle,
                    sweepAngle = sweep * progress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = strokeStyle
                )
            }
        }
    }
}

/** Compat: arcs por etapas (Programa/legacy). */
@Composable
fun ProgressArcs(
    stages: List<com.example.mysmartkiln.ui.viewmodels.ProgramStage> = emptyList(),
    elapsedMinutes: Int = 0
) {
    if (stages.isEmpty()) return
    val total = stages.sumOf { it.durationMinutes }.coerceAtLeast(1)
    val maxTemp = stages.maxOfOrNull { it.targetTemp } ?: 1000f
    // Visual de herradura por progreso temporal mapeado a temperatura de escala
    val syntheticTemp = maxTemp * (elapsedMinutes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    EspTemperatureHorseshoe(
        currentTempC = syntheticTemp,
        scaleMaxC = tempScaleMax(maxTemp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun rememberAscentRateCPerMin(samples: List<TemperatureSample>): Float? {
    return androidx.compose.runtime.remember(samples) {
        computeAscentRateCPerMin(samples)
    }
}

private fun programTotalMinutes(stagesSum: Int, formattedTotal: String): Int {
    if (stagesSum > 0) return stagesSum
    val match = Regex("""(\d+)h\s*(\d+)(?:min|m)""").find(formattedTotal.trim()) ?: return 0
    val hours = match.groupValues[1].toIntOrNull() ?: 0
    val mins = match.groupValues[2].toIntOrNull() ?: 0
    return hours * 60 + mins
}

private fun tempScaleMax(targetTemp: Float): Float {
    // Tope visual razonable: programas con target absurdo no inflan la herradura.
    val base = targetTemp.coerceIn(400f, 1300f)
    val step = 250f
    return ((base / step).toInt() + 1) * step
}

internal fun computeAscentRateCPerMin(
    samples: List<TemperatureSample>,
    windowMin: Float = 3f
): Float? {
    if (samples.size < 2) return null
    val last = samples.last()
    val window = samples.filter { it.elapsedMin >= last.elapsedMin - windowMin }
    val first = if (window.size >= 2) window.first() else samples.first()
    val dt = last.elapsedMin - first.elapsedMin
    if (dt < 0.15f) return null
    return (last.temperatureC - first.temperatureC) / dt
}

private fun formatAscentRate(rate: Float?): String {
    if (rate == null) return "—"
    val sign = if (rate >= 0f) "+" else ""
    return String.format("%s%.0f °C/min", sign, rate)
}
