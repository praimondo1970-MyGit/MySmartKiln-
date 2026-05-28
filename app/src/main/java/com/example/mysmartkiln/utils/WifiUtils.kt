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

/**
 * Descripción de un AP de horno detectado por el escáner WiFi.
 *
 * - [ssid]   `SmartKiln-XXXX` (últimos 4 hex del MAC del horno).
 * - [bssid]  MAC del SoftAP (lo expone el sistema operativo). Sus últimos 4
 *            bytes (8 caracteres hex) coinciden con los del MAC base del
 *            ESP32-S3, así que sirven para derivar la passphrase WPA2 del
 *            AP, que el firmware arma con esos mismos 8 hex (uppercase).
 * - [signalLevel] dBm de señal (más cercano a 0 = más fuerte).
 */
data class KilnAccessPoint(
    val ssid: String,
    val bssid: String,
    val signalLevel: Int
)

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

    /**
     * Devuelve los SSIDs visibles que arrancan con `SmartKiln-`, ordenados
     * por intensidad de señal. Útil cuando solo necesitamos el SSID (por ej.
     * mostrar nombres en una lista o filtrarlos en otro diálogo).
     *
     * Importante: requiere que el WiFi esté habilitado y, en Android 13+,
     * el permiso NEARBY_WIFI_DEVICES (en versiones anteriores
     * ACCESS_FINE_LOCATION) para que `scanResults` devuelva algo distinto a
     * lista vacía.
     */
    fun getSmartKilnNetworks(context: Context): List<String> {
        return getSmartKilnAccessPoints(context).map { it.ssid }.distinct()
    }

    /**
     * Variante detallada de [getSmartKilnNetworks] que también devuelve el
     * BSSID y la potencia de cada AP. La usa el flujo de alta para listar
     * los hornos detectados y derivar la passphrase WPA2 a partir del BSSID
     * (los 8 últimos caracteres hex del MAC), porque el SSID solo expone 4.
     */
    @Suppress("DEPRECATION")
    fun getSmartKilnAccessPoints(context: Context): List<KilnAccessPoint> {
        return try {
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager?.isWifiEnabled != true) return emptyList()
            wifiManager.startScan()
            wifiManager.scanResults
                .asSequence()
                .filter { !it.SSID.isNullOrBlank() && isSmartKilnSsid(it.SSID) }
                .filter { !it.BSSID.isNullOrBlank() }
                .sortedByDescending { it.level }
                .map { KilnAccessPoint(ssid = it.SSID, bssid = it.BSSID, signalLevel = it.level) }
                .distinctBy { it.bssid }
                .toList()
        } catch (e: Exception) {
            android.util.Log.e("WifiUtils", "Error escaneando hornos: ${e.message}")
            emptyList()
        }
    }
}
