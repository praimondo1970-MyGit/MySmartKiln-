package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.viewmodels.TemperatureSample
import com.example.mysmartkiln.utils.KilnCurveBuilder

@Composable
fun KilnTemperatureChart(
    chartModel: KilnCurveBuilder.ChartModel,
    realSamples: List<TemperatureSample>,
    currentElapsedMin: Float,
    currentTemperature: Float,
    showProgress: Boolean,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    val mutedColor = kilnColors.muted
    val onAccentColor = kilnColors.onAccent
    val gridColor = kilnColors.cardBorder.copy(alpha = 0.45f)
    val theoreticalColor = kilnColors.accent
    val realCurveColor = kilnColors.title
    val totalMin = chartModel.totalMinutes.coerceAtLeast(1f)
    val minTemp = chartModel.minTemp
    val maxTemp = chartModel.maxTemp
    val midTemp = (minTemp + maxTemp) / 2f
    val midTime = totalMin / 2f

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .width(36.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = KilnCurveBuilder.formatAxisTemp(maxTemp),
                    color = mutedColor,
                    fontSize = 10.sp
                )
                Text(
                    text = KilnCurveBuilder.formatAxisTemp(midTemp),
                    color = mutedColor,
                    fontSize = 10.sp
                )
                Text(
                    text = KilnCurveBuilder.formatAxisTemp(minTemp),
                    color = mutedColor,
                    fontSize = 10.sp
                )
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = 4.dp, end = 4.dp)
            ) {
                val padL = 8f
                val padR = 8f
                val padT = 8f
                val padB = 8f
                val plotW = size.width - padL - padR
                val plotH = size.height - padT - padB

                fun mapX(timeMin: Float): Float =
                    padL + (timeMin / totalMin).coerceIn(0f, 1f) * plotW

                fun mapY(tempC: Float): Float {
                    val range = (maxTemp - minTemp).coerceAtLeast(1f)
                    val norm = ((tempC - minTemp) / range).coerceIn(0f, 1f)
                    return padT + plotH * (1f - norm)
                }

                // Grid
                                for (i in 0..4) {
                    val y = padT + plotH * i / 4f
                    drawLine(gridColor, Offset(padL, y), Offset(padL + plotW, y), strokeWidth = 1f)
                }
                for (i in 0..4) {
                    val x = padL + plotW * i / 4f
                    drawLine(gridColor, Offset(x, padT), Offset(x, padT + plotH), strokeWidth = 1f)
                }

                // Theoretical program curve (orange)
                drawPolyline(
                    points = chartModel.theoretical,
                    mapX = ::mapX,
                    mapY = ::mapY,
                    color = theoreticalColor,
                    strokeWidth = 3f
                )

                // Real temperature trail (light)
                val realPoints = buildRealPoints(
                    samples = realSamples,
                    currentElapsedMin = currentElapsedMin,
                    currentTemperature = currentTemperature,
                    showProgress = showProgress
                )
                if (realPoints.size >= 2) {
                    drawPolyline(
                        points = realPoints,
                        mapX = ::mapX,
                        mapY = ::mapY,
                        color = realCurveColor,
                        strokeWidth = 2.5f
                    )
                } else if (realPoints.size == 1 && showProgress) {
                    drawCircle(
                        color = realCurveColor,
                        radius = 6f,
                        center = Offset(mapX(realPoints[0].timeMin), mapY(realPoints[0].tempC))
                    )
                }

                // Progress marker (“ahora”)
                if (showProgress && currentElapsedMin >= 0f) {
                    val progressX = mapX(currentElapsedMin.coerceAtMost(totalMin))
                    drawLine(
                        color = mutedColor.copy(alpha = 0.55f),
                        start = Offset(progressX, padT),
                        end = Offset(progressX, padT + plotH),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                    if (currentTemperature > 0f) {
                        drawCircle(
                            color = theoreticalColor,
                            radius = 8f,
                            center = Offset(progressX, mapY(currentTemperature))
                        )
                        drawCircle(
                            color = realCurveColor,
                            radius = 5f,
                            center = Offset(progressX, mapY(currentTemperature))
                        )
                        drawCircle(
                            color = onAccentColor,
                            radius = 2.5f,
                            center = Offset(progressX, mapY(currentTemperature))
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp, top = 4.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0",
                color = mutedColor,
                fontSize = 10.sp
            )
            Text(
                text = KilnCurveBuilder.formatAxisMinutes(midTime),
                color = mutedColor,
                fontSize = 10.sp
            )
            Text(
                text = KilnCurveBuilder.formatAxisMinutes(totalMin),
                color = mutedColor,
                fontSize = 10.sp
            )
        }
    }
}

private fun buildRealPoints(
    samples: List<TemperatureSample>,
    currentElapsedMin: Float,
    currentTemperature: Float,
    showProgress: Boolean
): List<KilnCurveBuilder.Point> {
    val fromSamples = samples.map { KilnCurveBuilder.Point(it.elapsedMin, it.temperatureC) }
    if (!showProgress || currentTemperature <= 0f) return fromSamples

    val last = fromSamples.lastOrNull()
    val current = KilnCurveBuilder.Point(currentElapsedMin, currentTemperature)
    return when {
        last == null -> listOf(current)
        kotlin.math.abs(last.timeMin - current.timeMin) < 0.05f &&
            kotlin.math.abs(last.tempC - current.tempC) < 0.5f -> fromSamples
        else -> fromSamples + current
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPolyline(
    points: List<KilnCurveBuilder.Point>,
    mapX: (Float) -> Float,
    mapY: (Float) -> Float,
    color: Color,
    strokeWidth: Float
) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(mapX(points.first().timeMin), mapY(points.first().tempC))
        for (i in 1 until points.size) {
            lineTo(mapX(points[i].timeMin), mapY(points[i].tempC))
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}
