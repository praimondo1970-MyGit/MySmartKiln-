package com.example.mysmartkiln.data.local

import com.example.mysmartkiln.data.local.entities.StageEntity

object KilnProfileJsonBuilder {

    /**
     * JSON para POST /api/profile (sin prefijo LOAD_PROFILE:).
     */
    fun build(programName: String, stages: List<StageEntity>, programUpdatedAt: Long): String {
        val sorted = stages.sortedBy { it.stageNumber }
        val segParts = sorted.joinToString(",") { stage ->
            val temp = stage.temperature.toFloatOrNull()?.toInt() ?: 0
            val rampa = stage.speed.toFloatOrNull()?.toInt() ?: 0
            val remojo = stage.maintenance.toIntOrNull() ?: 0
            """{"temp":$temp,"rampa":$rampa,"remojo":$remojo}"""
        }
        val escapedName = programName.replace("\\", "\\\\").replace("\"", "\\\"")
        return """{"nombre":"$escapedName","numSeg":${sorted.size},"programUpdatedAt":$programUpdatedAt,"seg":[$segParts]}"""
    }
}
