package com.example.mysmartkiln.data.local

object KilnApiDefaults {
    const val BASE_URL = "http://192.168.4.1"
    /** SoftAP horno (y legacy corto SmartKiln-XXXX). */
    const val SMART_KILN_SSID_PREFIX = "SmartKiln"
    /** SoftAP DataLogger (firmware futuro). */
    const val SMART_LINK_SSID_PREFIX = "SmartLink"
}

/** Tipo de dispositivo inferido del prefijo SoftAP. */
enum class SoftApDeviceType {
    KILN,
    DATALOGGER,
    UNKNOWN
}

data class KilnStatusDto(
    val temperature: Float = 0f,
    val status: String = "",
    val programName: String = "",
    val numStages: Int = 0,
    val profileLoaded: Boolean = false,
    val programDurationMin: Int = 0,
    val elapsedMinutes: Int = 0,
    val remainingMinutes: Int = 0,
    val currentStage: Int = 0,
    val error: String? = null,
    /** "KILN" | "DATALOGGER" si el firmware lo publica. */
    val type: String = "",
    val logCount: Int = 0,
    val sessionId: String = "",
    val logging: Boolean = false,
    val intervalSec: Int = 0
)

/** Meta de sesión DataLogger (`GET /api/session`). */
data class LoggerSessionDto(
    val sessionId: String = "",
    val name: String = "",
    val comments: String = "",
    val active: Boolean = false,
    val ended: Boolean = false,
    val startedAtMs: Double = 0.0,
    val intervalSec: Int = 180,
    val sampleCount: Int = 0,
    val lastElapsedMin: Float = 0f,
    val lastTempC: Float = 0f,
    val interrupted: Boolean = false
)

data class KilnInfoDto(
    val name: String = "",
    val model: String = "",
    val volume: String = "",
    val userId: String = "",
    val macAddress: String = "",
    /** "KILN" | "DATALOGGER" (firmware SoftAP /api/info). */
    val type: String = ""
)

data class KilnApInfoDto(
    val ssid: String = "",
    val password: String = "",
    val url: String = "",
    val apiStatus: String = ""
)

data class KilnCommandResponseDto(
    val ok: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val cmd: String? = null,
    val status: String? = null,
    val sessionId: String? = null,
    val sampleCount: Int? = null
)

data class KilnProfileResponseDto(
    val ok: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

data class KilnInfoUploadResponseDto(
    val ok: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

enum class KilnDataSource {
    LOCAL_AP,
    FIREBASE,
    DISCONNECTED
}
