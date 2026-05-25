package com.example.mysmartkiln.data.local

import android.content.Context
import android.util.Log
import com.example.mysmartkiln.utils.KilnWifiNetworkBinder
import com.example.mysmartkiln.utils.WifiUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object KilnConnectionVerifier {

    private const val TAG = "KilnConnectionVerifier"

    suspend fun verify(
        context: Context,
        httpClient: KilnHttpClient? = null
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val client = httpClient ?: KilnHttpClient(appContext)
        val boundKilnWifi = WifiUtils.isOnSmartKilnNetwork(appContext) &&
            KilnWifiNetworkBinder.bindToWifi(appContext)
        try {
            verifyOnBoundNetwork(appContext, client)
        } finally {
            if (boundKilnWifi) {
                KilnWifiNetworkBinder.unbind(appContext)
            }
        }
    }

    private suspend fun verifyOnBoundNetwork(
        context: Context,
        client: KilnHttpClient
    ): Result<Pair<String, String>> {
        if (!client.ping()) {
            Log.d(TAG, "ping falló (WiFi SmartKiln=${WifiUtils.isOnSmartKilnNetwork(context)})")
            return if (WifiUtils.isOnSmartKilnNetwork(context)) {
                Result.failure(KilnVerifyException.PingFailed)
            } else {
                Result.failure(KilnVerifyException.NotOnKilnWifi)
            }
        }
        val info = client.getInfo().getOrElse {
            Log.w(TAG, "getInfo falló: ${it.message}")
            return Result.failure(it)
        }
        val mac = info.macAddress.replace(":", "").uppercase()
        if (mac.isBlank()) {
            return Result.failure(KilnVerifyException.InvalidMac)
        }
        val name = info.name.ifBlank { "Mi Horno" }
        return Result.success(mac to name)
    }
}

sealed class KilnVerifyException : Exception() {
    data object NotOnKilnWifi : KilnVerifyException()
    data object PingFailed : KilnVerifyException()
    data object InvalidMac : KilnVerifyException()
}
