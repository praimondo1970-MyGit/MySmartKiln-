package com.example.mysmartkiln.data.model

/**
 * Registro canónico en `users/{uid}/activity/{id}`.
 * Sirve para hornos (KILN) y DataLogger (DATALOGGER).
 */
data class ActivityRecord(
    val id: String,
    val type: String,
    val deviceMac: String,
    val deviceName: String = "",
    val name: String,
    val comments: String = "",
    val startedAtMs: Long,
    val endedAtMs: Long,
    val durationMin: Int = 0,
    val maxTemperatureC: Int = 0,
    val sampleCount: Int = 0,
    val complete: Boolean = true,
    /** true mientras la cocción/grabación sigue en curso en el ESP. */
    val active: Boolean = false,
    val source: String = "APP",
    /** Id de curva en recordings/ (DataLogger); null en cocciones de horno v1. */
    val recordingId: String? = null
) {
    val isDataLogger: Boolean get() = type.equals("DATALOGGER", ignoreCase = true)

    fun toFirebaseMap(): Map<String, Any?> = buildMap {
        put("id", id)
        put("type", type.uppercase())
        put("deviceMac", deviceMac)
        put("deviceName", deviceName)
        put("name", name)
        put("comments", comments)
        put("startedAtMs", startedAtMs)
        put("endedAtMs", endedAtMs)
        put("durationMin", durationMin)
        put("maxTemperatureC", maxTemperatureC)
        put("sampleCount", sampleCount)
        put("complete", complete)
        put("active", active)
        put("source", source)
        if (!recordingId.isNullOrBlank()) put("recordingId", recordingId)
    }

    companion object {
        fun fromFirebase(map: Map<String, Any?>, fallbackId: String = ""): ActivityRecord? {
            val id = (map["id"] as? String)?.takeIf { it.isNotBlank() }
                ?: fallbackId.takeIf { it.isNotBlank() }
                ?: return null
            val type = (map["type"] as? String)?.trim()?.uppercase().orEmpty()
                .ifBlank { "KILN" }
            val deviceMac = (map["deviceMac"] as? String)?.trim().orEmpty()
            val name = (map["name"] as? String)?.trim().orEmpty()
                .ifBlank { (map["programName"] as? String)?.trim().orEmpty() }
            if (name.isBlank() && deviceMac.isBlank()) return null
            val started = (map["startedAtMs"] as? Number)?.toLong() ?: 0L
            val ended = (map["endedAtMs"] as? Number)?.toLong()
                ?: (map["timestamp"] as? Number)?.toLong()
                ?: started
            return ActivityRecord(
                id = id,
                type = type,
                deviceMac = deviceMac,
                deviceName = (map["deviceName"] as? String)?.trim().orEmpty(),
                name = name.ifBlank { if (type == "DATALOGGER") "Grabación" else "Cocción" },
                comments = (map["comments"] as? String).orEmpty(),
                startedAtMs = started,
                endedAtMs = ended,
                durationMin = (map["durationMin"] as? Number)?.toInt() ?: 0,
                maxTemperatureC = (map["maxTemperatureC"] as? Number)?.toInt()
                    ?: (map["maxTemperature"] as? Number)?.toInt()
                    ?: 0,
                sampleCount = (map["sampleCount"] as? Number)?.toInt() ?: 0,
                complete = map["complete"] as? Boolean ?: true,
                active = map["active"] as? Boolean ?: false,
                source = (map["source"] as? String)?.trim().orEmpty().ifBlank { "APP" },
                recordingId = (map["recordingId"] as? String)?.takeIf { it.isNotBlank() }
                    ?: id.takeIf { type == "DATALOGGER" || (map["sampleCount"] as? Number)?.toInt()?.let { it > 0 } == true }
            )
        }
    }
}
