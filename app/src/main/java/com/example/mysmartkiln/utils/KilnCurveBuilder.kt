package com.example.mysmartkiln.utils

import com.example.mysmartkiln.ui.viewmodels.ProgramStage

/**
 * Construye la curva teórica tiempo–temperatura a partir de las etapas del programa.
 */
object KilnCurveBuilder {

    const val DEFAULT_START_TEMP_C = 25f

    data class Point(
        val timeMin: Float,
        val tempC: Float
    )

    data class ChartModel(
        val theoretical: List<Point>,
        val totalMinutes: Float,
        val minTemp: Float,
        val maxTemp: Float
    )

    fun buildTheoretical(
        stages: List<ProgramStage>,
        startTempC: Float = DEFAULT_START_TEMP_C
    ): ChartModel {
        if (stages.isEmpty()) {
            return ChartModel(
                theoretical = emptyList(),
                totalMinutes = 0f,
                minTemp = startTempC,
                maxTemp = startTempC + 50f
            )
        }

        val sorted = stages.sortedBy { it.stageNumber }
        val points = mutableListOf(Point(0f, startTempC))
        var timeMin = 0f
        var currentTemp = startTempC

        for (stage in sorted) {
            val rampMin = (stage.durationMinutes - stage.maintenanceMinutes)
                .coerceAtLeast(0)
                .toFloat()
            val holdMin = stage.maintenanceMinutes.toFloat()
            val target = stage.targetTemp

            if (rampMin > 0f) {
                timeMin += rampMin
                points.add(Point(timeMin, target))
                currentTemp = target
            } else if (kotlin.math.abs(target - currentTemp) > 0.5f) {
                points.add(Point(timeMin, target))
                currentTemp = target
            }

            if (holdMin > 0f) {
                timeMin += holdMin
                points.add(Point(timeMin, currentTemp))
            }
        }

        val temps = points.map { it.tempC }
        val minT = ((temps.minOrNull() ?: startTempC) - 30f).coerceAtLeast(0f)
        val maxT = (temps.maxOrNull() ?: startTempC) + 30f
        val total = timeMin.coerceAtLeast(1f)

        return ChartModel(
            theoretical = points,
            totalMinutes = total,
            minTemp = minT,
            maxTemp = maxT.coerceAtLeast(minT + 50f)
        )
    }

    fun formatAxisMinutes(minutes: Float): String {
        val total = minutes.toInt()
        val hours = total / 60
        val mins = total % 60
        return if (hours > 0) "${hours}h${String.format("%02d", mins)}"
        else "${mins}m"
    }

    fun formatAxisTemp(tempC: Float): String = "${tempC.toInt()}°"
}
