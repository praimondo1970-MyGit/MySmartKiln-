package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel
import com.example.mysmartkiln.utils.KilnCurveBuilder

@Composable
fun KilnCurveScreen(
    kilnMacAddress: String,
    viewModel: KilnDetailViewModel
) {
    val kilnColors = LocalKilnColors.current

    val kilnStatus by viewModel.kilnStatus.collectAsState()
    val kilnProgram by viewModel.kilnProgram.collectAsState()
    val temperatureHistory by viewModel.temperatureHistory.collectAsState()

    LaunchedEffect(kilnMacAddress) {
        viewModel.loadKilnData(kilnMacAddress)
    }

    LaunchedEffect(kilnMacAddress, kilnProgram.name) {
        viewModel.ensureProgramStagesLoaded()
    }

    val chartModel = remember(kilnProgram.stages) {
        KilnCurveBuilder.buildTheoretical(kilnProgram.stages)
    }

    val isActiveRun = kilnStatus.isRunning || kilnStatus.isPaused ||
        kilnStatus.espStatus in setOf("CALENTANDO", "PAUSADO", "INTERRUMPIDO")

    val currentElapsedMin = maxOf(
        kilnStatus.elapsedMinutes.toFloat(),
        temperatureHistory.lastOrNull()?.elapsedMin ?: 0f
    )

    // Título fuera del gris; marco solo para leyenda + gráfico + textos inferiores.
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        val fabClearance = 112.dp
        val layoutHeight = (maxHeight - fabClearance).coerceAtLeast(280.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(layoutHeight)
                .padding(horizontal = 2.dp)
        ) {
            Text(
                text = "Temperatura y programa",
                color = kilnColors.title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )

            if (kilnProgram.name.isNotBlank()) {
                Text(
                    text = kilnProgram.name,
                    color = kilnColors.muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = kilnColors.cardBackground),
                border = BorderStroke(1.dp, kilnColors.cardBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        LegendItem(color = kilnColors.title, label = "Programa")
                        LegendItem(color = kilnColors.accent, label = "Real")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (kilnProgram.stages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Sin programa cargado",
                                    color = kilnColors.body,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Carga un programa para ver la curva teórica",
                                    color = kilnColors.muted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    } else {
                        KilnTemperatureChart(
                            chartModel = chartModel,
                            realSamples = temperatureHistory,
                            currentElapsedMin = currentElapsedMin,
                            currentTemperature = kilnStatus.currentTemperature,
                            showProgress = isActiveRun,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Actual: ${kilnStatus.currentTemperature.toInt()}°C",
                                color = kilnColors.accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (isActiveRun) {
                                Text(
                                    text = "Transcurrido: ${KilnCurveBuilder.formatAxisMinutes(currentElapsedMin)}",
                                    color = kilnColors.body,
                                    fontSize = 12.sp
                                )
                            }
                            Text(
                                text = "Duración: ${KilnCurveBuilder.formatAxisMinutes(chartModel.totalMinutes)}",
                                color = kilnColors.muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    val kilnColors = LocalKilnColors.current

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(3.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = kilnColors.title,
            fontSize = 13.sp
        )
    }
}
