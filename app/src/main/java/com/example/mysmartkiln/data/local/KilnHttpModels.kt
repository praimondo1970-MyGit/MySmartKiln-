package com.example.mysmartkiln.data.local

object KilnApiDefaults {
    const val BASE_URL = "http://192.168.4.1"
    const val SMART_KILN_SSID_PREFIX = "SmartKiln"
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
    val error: String? = null
)

data class KilnInfoDto(
    val name: String = "",
    val model: String = "",
    val volume: String = "",
    val userId: String = "",
    val macAddress: String = ""
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
    val message: String? = null
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
