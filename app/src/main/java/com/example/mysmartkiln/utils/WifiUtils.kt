package com.example.mysmartkiln.utils

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.mysmartkiln.data.local.KilnApiDefaults

object WifiUtils {

    fun isOnSmartKilnNetwork(context: Context): Boolean {
        val ssid = getCurrentWifiSSID(context) ?: return false
        return isSmartKilnSsid(ssid)
    }

    fun isSmartKilnSsid(ssid: String): Boolean {
        return ssid.startsWith(KilnApiDefaults.SMART_KILN_SSID_PREFIX, ignoreCase = true)
    }

    fun isConnectedToWifi(context: Context): Boolean {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                @Suppress("DEPRECATION")
                return networkInfo?.type == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected
            }
        } catch (e: Exception) {
            android.util.Log.e("WifiUtils", "Error verificando WiFi: ${e.message}")
        }
        return false
    }

    /**
     * Obtiene el SSID de la red WiFi activa.
     * En Android 10+ el SSID puede ser null sin permiso de ubicación; se usan varias vías.
     */
    fun getCurrentWifiSSID(context: Context): String? {
        if (!isConnectedToWifi(context)) return null

        try {
            parseSsid(transportInfoSsid(context))?.let { return it }
            parseSsid(legacyConnectionSsid(context))?.let { return it }
        } catch (e: Exception) {
            android.util.Log.e("WifiUtils", "Error obteniendo SSID: ${e.message}")
        }
        return null
    }

    private fun transportInfoSsid(context: Context): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return null
        val network = connectivityManager.activeNetwork ?: return null
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return null
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null

        val transportInfo = caps.transportInfo
        if (transportInfo is WifiInfo) {
            return wifiInfoToSsid(transportInfo)
        }
        return null
    }

    @Suppress("DEPRECATION")
    private fun legacyConnectionSsid(context: Context): String? {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                ?: return null
        return wifiInfoToSsid(wifiManager.connectionInfo)
    }

    private fun wifiInfoToSsid(wifiInfo: WifiInfo?): String? {
        if (wifiInfo == null) return null
        @Suppress("DEPRECATION")
        return wifiInfo.ssid
    }

    private fun parseSsid(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.removeSurrounding("\"").trim()
        if (cleaned.isEmpty() ||
            cleaned.equals("<unknown ssid>", ignoreCase = true) ||
            cleaned.equals("unknown ssid", ignoreCase = true) ||
            cleaned == "0x"
        ) {
            return null
        }
        return cleaned
    }

    fun hasLocationPermissionForSsid(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @Suppress("DEPRECATION")
    fun getAvailableNetworks(context: Context): List<String> {
        try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager?.isWifiEnabled == true) {
                wifiManager.startScan()
                return wifiManager.scanResults
                    .filter { !it.SSID.isNullOrBlank() }
                    .sortedByDescending { it.level }
                    .map { it.SSID }
                    .distinct()
            }
        } catch (e: Exception) {
            android.util.Log.e("WifiUtils", "Error escaneando redes: ${e.message}")
        }
        return emptyList()
    }
}
