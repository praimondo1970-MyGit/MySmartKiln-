package com.example.mysmartkiln.data.local

import org.json.JSONObject

object KilnInfoJsonBuilder {

    /**
     * JSON para POST /api/kiln-info (mismo esquema que KILN_INFO por BLE, sin prefijo).
     * [type] = "KILN" | "DATALOGGER" — se refleja en Firebase deviceInfo.type
     */
    fun build(
        name: String,
        model: String,
        volume: String,
        userId: String,
        type: String = "KILN",
        offlineOnly: Boolean = false,
        homeWifiSsid: String? = null,
        homeWifiPassword: String? = null,
        thermocoupleType: String? = null
    ): String {
        val doc = JSONObject().apply {
            put("name", name)
            put("model", model)
            put("type", type.ifBlank { "KILN" }.uppercase())
            val volumeNum = volume.toFloatOrNull()
            if (volumeNum != null) {
                put("volume", volumeNum)
            } else {
                put("volume", volume)
            }
            put("userId", userId)
            put("offlineOnly", offlineOnly)
            if (!thermocoupleType.isNullOrBlank()) {
                put("thermocouple", thermocoupleType.uppercase())
            }
            if (!offlineOnly &&
                !homeWifiSsid.isNullOrBlank() &&
                !homeWifiPassword.isNullOrBlank()
            ) {
                put(
                    "wifi",
                    JSONObject().apply {
                        put("ssid", homeWifiSsid)
                        put("password", homeWifiPassword)
                    }
                )
            }
        }
        return doc.toString()
    }
}