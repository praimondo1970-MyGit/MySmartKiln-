package com.example.mysmartkiln.data.model

import com.example.mysmartkiln.utils.UserIdUtils

/**
 * Snapshot tipado de `users/.../kilns/{mac}/realtimeData`.
 *
 * Campos comunes: temperature, status, timestamp, type, programName (label).
 * Solo horno: programStartTime, programTotalDuration, programPauseTime.
 * Solo DataLogger: sessionId, logCount.
 */
data class DeviceRealtimeSnapshot(
    val type: String,
    val temperature: Float,
    val status: String,
    val timestampMs: Long,
    /** Nombre de programa (horno) o de sesión (logger). */
    val label: String,
    val programStartTimeSec: Long? = null,
    val programTotalDurationMin: Int? = null,
    val programPauseTimeSec: Long? = null,
    /** Minutos de cocción reportados por el ESP (fuente de verdad remota). */
    val elapsedMinutes: Int? = null,
    val remainingMinutes: Int? = null,
    val sessionId: String? = null,
    val logCount: Int? = null
) {
    val isDataLogger: Boolean
        get() = type.equals("DATALOGGER", ignoreCase = true)

    val isKiln: Boolean
        get() = type.equals("KILN", ignoreCase = true) ||
            (type.isBlank() && !isDataLogger)

    /** True si el ESP publicó un type que no coincide con el dispositivo abierto. */
    fun mismatchesExpected(expectedIsDataLogger: Boolean): Boolean {
        if (type.isBlank()) return false
        return isDataLogger != expectedIsDataLogger
    }

    companion object {
        fun parse(
            raw: Map<String, Any?>,
            expectedIsDataLogger: Boolean? = null
        ): DeviceRealtimeSnapshot {
            val typeRaw = (raw["type"] as? String)?.trim()?.uppercase().orEmpty()
            val resolvedType = when {
                typeRaw == "DATALOGGER" || typeRaw == "KILN" -> typeRaw
                expectedIsDataLogger == true -> "DATALOGGER"
                expectedIsDataLogger == false -> "KILN"
                else -> ""
            }
            val isLogger = resolvedType == "DATALOGGER" ||
                (resolvedType.isEmpty() && expectedIsDataLogger == true)

            val temperature = when (val v = raw["temperature"]) {
                is Number -> v.toFloat()
                is String -> v.toFloatOrNull() ?: 0f
                else -> 0f
            }
            val status = (raw["status"] as? String)?.trim()?.uppercase().orEmpty()
            val timestampMs = UserIdUtils.parseFirebaseTimestamp(raw["timestamp"])
            val label = (raw["programName"] as? String)?.trim().orEmpty()

            return if (isLogger) {
                DeviceRealtimeSnapshot(
                    type = resolvedType.ifBlank { "DATALOGGER" },
                    temperature = temperature,
                    status = status,
                    timestampMs = timestampMs,
                    label = label,
                    sessionId = raw["sessionId"]?.toString()?.takeIf { it.isNotBlank() },
                    logCount = (raw["logCount"] as? Number)?.toInt()
                )
            } else {
                DeviceRealtimeSnapshot(
                    type = resolvedType.ifBlank { "KILN" },
                    temperature = temperature,
                    status = status,
                    timestampMs = timestampMs,
                    label = label,
                    programStartTimeSec = (raw["programStartTime"] as? Number)?.toLong()
                        ?.takeIf { it > 0 },
                    programTotalDurationMin = (raw["programTotalDuration"] as? Number)?.toInt()
                        ?.takeIf { it > 0 },
                    programPauseTimeSec = (raw["programPauseTime"] as? Number)?.toLong()
                        ?.takeIf { it > 0 },
                    elapsedMinutes = (raw["elapsedMinutes"] as? Number)?.toInt()
                        ?.takeIf { it >= 0 },
                    remainingMinutes = (raw["remainingMinutes"] as? Number)?.toInt()
                        ?.takeIf { it >= 0 }
                )
            }
        }
    }
}
