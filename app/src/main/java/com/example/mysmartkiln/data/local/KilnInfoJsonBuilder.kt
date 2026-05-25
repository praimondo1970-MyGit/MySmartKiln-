package com.example.mysmartkiln.data.local

import org.json.JSONObject

object KilnInfoJsonBuilder {

    /**
     * JSON para POST /api/kiln-info (mismo esquema que KILN_INFO por BLE, sin prefijo).
     */
    fun build(
        name: String,
        model: String,
        volume: String,
        userId: String,
        homeWifiSsid: String? = null,
        homeWifiPassword: String? = null
    ): String {
        val doc = JSONObject().apply {
            put("name", name)
            put("model", model)
            val volumeNum = volume.toFloatOrNull()
            if (volumeNum != null) {
                put("volume", volumeNum)
            } else {
                put("volume", volume)
            }
            put("userId", userId)
            if (!homeWifiSsid.isNullOrBlank() && !homeWifiPassword.isNullOrBlank()) {
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
