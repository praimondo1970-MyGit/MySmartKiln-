package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.viewmodels.KilnDetailViewModel
import com.example.mysmartkiln.utils.KilnCurveBuilder

@Composable
fun KilnCurveScreen(
    kilnMacAddress: String,
    viewModel: KilnDetailViewModel
) {
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Text(
                text = "Temperatura y programa",
                color = AppColors.Primary700,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            if (kilnProgram.name.isNotBlank()) {
                Text(
                    text = kilnProgram.name,
                    color = AppColors.Primary400,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                LegendItem(color = AppColors.Primary700, label = "Programa")
                LegendItem(color = Color(0xFFFF6F00), label = "Real")
            }

            Spacer(modifier = Modifier.height(8.dp))

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
                            color = AppColors.Primary600,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Carga un programa para ver la curva teórica",
                            color = AppColors.Primary400,
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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Actual: ${kilnStatus.currentTemperature.toInt()}°C",
                        color = Color(0xFFFF6F00),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (isActiveRun) {
                        Text(
                            text = "Transcurrido: ${KilnCurveBuilder.formatAxisMinutes(currentElapsedMin)}",
                            color = AppColors.Primary600,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = "Duración: ${KilnCurveBuilder.formatAxisMinutes(chartModel.totalMinutes)}",
                        color = AppColors.Primary400,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
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
            color = AppColors.Primary700,
            fontSize = 13.sp
        )
    }
}
