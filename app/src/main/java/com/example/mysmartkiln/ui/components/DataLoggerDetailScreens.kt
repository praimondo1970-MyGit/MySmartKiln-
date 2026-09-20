package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.data.local.KilnDataSource
import com.example.mysmartkiln.ui.theme.DialogBrand
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel
import com.example.mysmartkiln.ui.viewmodels.TemperatureSample
import com.example.mysmartkiln.utils.KilnCurveBuilder
import kotlin.math.roundToInt

/**
 * Detalle DataLogger — Estado: herradura + GRABAR / GUARDAR.
 */
@Composable
fun DataLoggerStatusScreen(
    kilnMacAddress: String,
    viewModel: KilnDetailViewModel
) {
    val kilnColors = LocalKilnColors.current
    val kilnStatus by viewModel.kilnStatus.collectAsState()
    val activeRecording by viewModel.activeRecording.collectAsState()
    val recordingMessage by viewModel.recordingMessage.collectAsState()
    var showStartDialog by remember { mutableStateOf(false) }

    // loadKilnData lo hace KilnDetailScreen; acá no re-cargar (evita carreras).

    val temp = kilnStatus.currentTemperature
    val scaleMax = (temp * 1.15f).coerceAtLeast(400f).coerceAtMost(1300f)
        .let { base ->
            val step = 250f
            ((base / step).toInt() + 1) * step
        }
    // Sesión local, ESP en GRABANDO, o isRunning del datalogger
    val isRecording = activeRecording?.isActive == true ||
        kilnStatus.espStatus.equals("GRABANDO", ignoreCase = true) ||
        kilnStatus.isRunning

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        val fabClearance = 112.dp
        val layoutHeight = (maxHeight - fabClearance).coerceAtLeast(320.dp)

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
                    currentTempC = temp,
                    scaleMaxC = scaleMax,
                    modifier = Modifier.fillMaxSize()
                )
                StatusGaugeLabels(
                    currentTempC = temp,
                    ascentRate = null,
                    showAscentRate = false
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            DataLoggerRecordButtons(
                isRecording = isRecording,
                onRecord = { showStartDialog = true },
                onSave = { viewModel.stopLoggerRecording() },
                modifier = Modifier.offset(y = (-10).dp)
            )

            if (!recordingMessage.isNullOrBlank()) {
                Text(
                    text = recordingMessage.orEmpty(),
                    color = kilnColors.muted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    if (showStartDialog) {
        DataLoggerStartRecordingDialog(
            onDismiss = { showStartDialog = false },
            onConfirm = { name, comments ->
                showStartDialog = false
                viewModel.startLoggerRecording(name, comments)
            }
        )
    }
}

@Composable
private fun DataLoggerRecordButtons(
    isRecording: Boolean,
    onRecord: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    val recordColor = Color(0xFFFF7811)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onRecord,
            enabled = !isRecording,
            modifier = Modifier
                .weight(1.15f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = recordColor,
                disabledContainerColor = kilnColors.track,
                disabledContentColor = kilnColors.muted
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        if (isRecording) kilnColors.muted else Color.White,
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "GRABAR",
                color = if (isRecording) kilnColors.muted else kilnColors.onFab,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
        Button(
            onClick = onSave,
            enabled = isRecording,
            modifier = Modifier
                .weight(0.85f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                disabledContainerColor = kilnColors.cardBorder,
                disabledContentColor = kilnColors.muted
            ),
            border = BorderStroke(
                1.dp,
                if (isRecording) {
                    if (kilnColors.isDark) Color.White.copy(alpha = 0.35f)
                    else kilnColors.title.copy(alpha = 0.25f)
                } else {
                    kilnColors.cardBorder
                }
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "GUARDAR",
                color = if (isRecording) kilnColors.onAccent else kilnColors.muted,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun DataLoggerStartRecordingDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, comments: String) -> Unit
) {
    val kilnColors = LocalKilnColors.current
    var name by remember { mutableStateOf("") }
    var comments by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nueva grabación",
                fontWeight = FontWeight.Bold,
                color = kilnColors.title
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Nombre con el que guardarás esta horneada / sesión.",
                    color = kilnColors.muted,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = kilnColors.accent,
                        cursorColor = kilnColors.accent,
                        focusedLabelColor = kilnColors.accent
                    )
                )
                OutlinedTextField(
                    value = comments,
                    onValueChange = { comments = it },
                    label = { Text("Comentarios (opcional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = kilnColors.accent,
                        cursorColor = kilnColors.accent,
                        focusedLabelColor = kilnColors.accent
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, comments) },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("Comenzar", color = kilnColors.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = kilnColors.muted)
            }
        },
        containerColor = kilnColors.cardBackground
    )
}

/**
 * Detalle DataLogger — Datos: muestras en vivo + CSV del dispositivo.
 */
@Composable
fun DataLoggerDataScreen(
    kilnMacAddress: String,
    viewModel: KilnDetailViewModel
) {
    val kilnColors = LocalKilnColors.current
    val temperatureHistory by viewModel.temperatureHistory.collectAsState()
    val csv by viewModel.deviceHistoryCsv.collectAsState()
    val loading by viewModel.deviceHistoryLoading.collectAsState()
    val error by viewModel.deviceHistoryError.collectAsState()
    val dataSource by viewModel.dataSource.collectAsState()

    LaunchedEffect(kilnMacAddress) {
        viewModel.loadKilnData(kilnMacAddress)
    }

    val csvRows = remember(csv) { parseHistoryCsvPreview(csv) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = "Historial",
            color = kilnColors.title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            text = "Muestras en vivo en la app y registro del dispositivo (CSV).",
            color = kilnColors.muted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Button(
            onClick = { viewModel.fetchDeviceHistoryCsv() },
            enabled = !loading && dataSource == KilnDataSource.LOCAL_AP,
            colors = ButtonDefaults.buttonColors(
                containerColor = DialogBrand.primaryButton(),
                contentColor = kilnColors.onAccent,
                disabledContainerColor = kilnColors.track,
                disabledContentColor = kilnColors.muted
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = kilnColors.onAccent,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(10.dp))
            }
            Text(
                text = if (dataSource == KilnDataSource.LOCAL_AP) {
                    "Leer historial del dispositivo"
                } else {
                    "Conectate al SoftAP para leer CSV"
                },
                fontWeight = FontWeight.SemiBold
            )
        }

        if (error != null) {
            Text(
                text = error ?: "",
                color = kilnColors.danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "En vivo (${temperatureHistory.size})",
            color = kilnColors.title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (temperatureHistory.isEmpty() && csvRows.isEmpty()) {
                item {
                    Text(
                        text = "Todavía no hay muestras. Con el dispositivo conectado se irán agregando.",
                        color = kilnColors.muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }

            items(temperatureHistory.asReversed().take(80)) { sample ->
                HistorySampleRow(sample)
            }

            if (csvRows.isNotEmpty()) {
                item {
                    Text(
                        text = "Desde dispositivo (${csvRows.size})",
                        color = kilnColors.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
                items(csvRows.take(100)) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(kilnColors.cardBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, kilnColors.cardBorder, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = row.first, color = kilnColors.muted, fontSize = 12.sp)
                        Text(
                            text = "${row.second} °C",
                            color = kilnColors.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(120.dp)) }
        }
    }
}

@Composable
private fun HistorySampleRow(sample: TemperatureSample) {
    val kilnColors = LocalKilnColors.current
    val mins = sample.elapsedMin.roundToInt()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(kilnColors.cardBackground, RoundedCornerShape(8.dp))
            .border(1.dp, kilnColors.cardBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "+${mins} min",
            color = kilnColors.muted,
            fontSize = 12.sp
        )
        Text(
            text = String.format("%.1f °C", sample.temperatureC),
            color = kilnColors.title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

private fun parseHistoryCsvPreview(csv: String?): List<Pair<String, String>> {
    if (csv.isNullOrBlank()) return emptyList()
    return csv.lineSequence()
        .drop(1)
        .mapNotNull { line ->
            val parts = line.split(';', ',', '\t')
            if (parts.size < 2) return@mapNotNull null
            val epoch = parts[0].trim()
            val temp = parts[1].trim()
            if (epoch.isEmpty() || temp.isEmpty()) null else epoch to temp
        }
        .toList()
        .asReversed()
}

/**
 * Detalle DataLogger — Curva solo con temperatura medida (sin programa teórico).
 */
@Composable
fun DataLoggerCurveScreen(
    kilnMacAddress: String,
    viewModel: KilnDetailViewModel
) {
    val kilnColors = LocalKilnColors.current
    val kilnStatus by viewModel.kilnStatus.collectAsState()
    val temperatureHistory by viewModel.temperatureHistory.collectAsState()

    LaunchedEffect(kilnMacAddress) {
        viewModel.loadKilnData(kilnMacAddress)
    }

    val chartModel = remember(temperatureHistory, kilnStatus.currentTemperature) {
        KilnCurveBuilder.buildFromSamples(
            samples = temperatureHistory,
            fallbackTempC = kilnStatus.currentTemperature.takeIf { it > 0f } ?: 25f
        )
    }
    val currentElapsedMin = temperatureHistory.lastOrNull()?.elapsedMin ?: 0f
    val currentTemp = kilnStatus.currentTemperature

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        val layoutHeight = (maxHeight - 122.dp).coerceAtLeast(280.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(layoutHeight)
                .padding(horizontal = 2.dp)
        ) {
            Text(
                text = "Curva de temperatura",
                color = kilnColors.title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Registro medido por el dispositivo",
                color = kilnColors.muted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(kilnColors.cardBackground, RoundedCornerShape(14.dp))
                    .border(1.dp, kilnColors.cardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(kilnColors.title, CircleShape)
                    )
                    Text(
                        text = "Medida",
                        color = kilnColors.muted,
                        fontSize = 12.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                if (temperatureHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sin muestras aún.\nConectá el dispositivo para ver la curva.",
                            color = kilnColors.muted,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    KilnTemperatureChart(
                        chartModel = chartModel,
                        realSamples = temperatureHistory,
                        currentElapsedMin = currentElapsedMin,
                        currentTemperature = currentTemp,
                        showProgress = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Actual", color = kilnColors.muted, fontSize = 11.sp)
                        Text(
                            text = if (currentTemp > 0f) "${currentTemp.roundToInt()} °C" else "—",
                            color = kilnColors.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Muestras", color = kilnColors.muted, fontSize = 11.sp)
                        Text(
                            text = "${temperatureHistory.size}",
                            color = kilnColors.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
