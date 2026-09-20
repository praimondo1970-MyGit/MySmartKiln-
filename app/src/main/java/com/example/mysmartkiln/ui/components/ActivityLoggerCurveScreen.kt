package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.viewmodels.ActivityCurveDetail
import com.example.mysmartkiln.utils.CsvShareUtils
import com.example.mysmartkiln.utils.KilnCurveBuilder
import kotlin.math.roundToInt

/**
 * Curva histórica DataLogger — mismo lenguaje visual que Curva / Actividad.
 * Deja espacio inferior para la barra con +; encima va «Exportar datos».
 */
@Composable
fun ActivityLoggerCurveScreen(
    detail: ActivityCurveDetail,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    val context = LocalContext.current
    val samples = detail.samples
    val stages = detail.stages
    val chartModel = remember(samples, stages, detail.isKiln) {
        if (detail.isKiln && stages.isNotEmpty()) {
            KilnCurveBuilder.buildHistorical(stages = stages, samples = samples)
        } else {
            KilnCurveBuilder.buildFromSamples(
                samples = samples,
                fallbackTempC = samples.lastOrNull()?.temperatureC ?: 25f
            )
        }
    }
    val last = samples.lastOrNull()
    val maxTemp = samples.maxOfOrNull { it.temperatureC }?.roundToInt()
    val canExport = samples.isNotEmpty() && !detail.isLoading
    val emptyLabel = if (detail.isKiln) "Cocción" else "Grabación"

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(kilnColors.screenBackground)
    ) {
        val fabClearance = if (kilnColors.isEsp) 110.dp else 160.dp
        val contentHeight = (maxHeight - fabClearance).coerceAtLeast(280.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight)
                .padding(horizontal = 16.dp)
        ) {
            if (kilnColors.isEsp) {
                EspBrandHeader(
                    modifier = Modifier.padding(top = 36.dp, bottom = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = kilnColors.accent
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Curva de temperatura",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = kilnColors.title,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = detail.title.ifBlank { emptyLabel },
                        color = kilnColors.muted,
                        fontSize = 13.sp
                    )
                    Text(
                        text = detail.subtitle,
                        color = kilnColors.muted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when {
                detail.isLoading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = kilnColors.accent)
                    }
                }
                detail.error != null && samples.isEmpty() && stages.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = detail.error,
                            color = kilnColors.muted,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
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
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (detail.isKiln && stages.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 16.dp, height = 3.dp)
                                                .background(kilnColors.accent, RoundedCornerShape(2.dp))
                                        )
                                        Text(
                                            text = "Programa",
                                            color = kilnColors.muted,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(kilnColors.title, CircleShape)
                                    )
                                    Text(
                                        text = if (detail.isKiln) "Horno" else "Medida",
                                        color = kilnColors.muted,
                                        fontSize = 12.sp
                                    )
                                }
                                if (detail.fromLocal) {
                                    Text(
                                        text = "· local",
                                        color = kilnColors.muted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            KilnTemperatureChart(
                                chartModel = chartModel,
                                realSamples = samples,
                                currentElapsedMin = last?.elapsedMin ?: 0f,
                                currentTemperature = last?.temperatureC ?: 0f,
                                showProgress = false,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(text = "Máx.", color = kilnColors.muted, fontSize = 11.sp)
                                    Text(
                                        text = maxTemp?.let { "$it °C" } ?: "—",
                                        color = kilnColors.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Muestras",
                                        color = kilnColors.muted,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${samples.size}",
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

            Spacer(modifier = Modifier.height(12.dp))

            AppPrimaryButton(
                text = "Exportar datos",
                onClick = {
                    CsvShareUtils.shareTemperatureCsv(
                        context = context,
                        fileNameBase = detail.title.ifBlank { "curva_datalogger" },
                        samples = samples
                    )
                },
                enabled = canExport,
                fullWidth = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
