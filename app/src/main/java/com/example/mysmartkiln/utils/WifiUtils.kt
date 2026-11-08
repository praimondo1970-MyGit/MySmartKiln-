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
import com.example.mysmartkiln.data.local.SoftApDeviceType

/**
 * Descripción de un SoftAP de dispositivo detectado por el escáner WiFi.
 *
 * - [ssid]   `SmartKiln-XXXXXXXX` (horno) o `SmartLink-XXXXXXXX` (datalogger).
 *            Sufijo = últimos 8 hex del MAC (= passphrase). Legacy horno: `SmartKiln-XXXX`.
 * - [bssid]  MAC del SoftAP; últimos 8 hex = passphrase.
 * - [signalLevel] dBm de señal (más cercano a 0 = más fuerte).
 * - [deviceType] Inferido del prefijo SSID.
 */
data class KilnAccessPoint(
    val ssid: String,
    val bssid: String,
    val signalLevel: Int,
    val deviceType: SoftApDeviceType = SoftApDeviceType.UNKNOWN,
    /** True si el teléfono ya está asociado a este SoftAP (no solo en scan). */
    val alreadyConnected: Boolean = false
)

object WifiUtils {

    fun isOnSmartKilnNetwork(context: Context): Boolean {
        val ssid = getCurrentWifiSSID(context) ?: return false
        return isDeviceSoftApSsid(ssid)
    }

    /** SoftAP de dispositivo propio (horno o datalogger). */
    fun isDeviceSoftApSsid(ssid: String): Boolean {
        return deviceTypeFromSsid(ssid) != SoftApDeviceType.UNKNOWN
    }

    /** @deprecated Usar [isDeviceSoftApSsid]; se mantiene por compatibilidad. */
    fun isSmartKilnSsid(ssid: String): Boolean = isDeviceSoftApSsid(ssid)

    fun deviceTypeFromSsid(ssid: String): SoftApDeviceType {
        val cleaned = ssid.trim().removeSurrounding("\"")
        return when {
            cleaned.startsWith(KilnApiDefaults.SMART_LINK_SSID_PREFIX, ignoreCase = true) ->
                SoftApDeviceType.DATALOGGER
            cleaned.startsWith(KilnApiDefaults.SMART_KILN_SSID_PREFIX, ignoreCase = true) ->
                SoftApDeviceType.KILN
            else -> SoftApDeviceType.UNKNOWN
        }
    }

    fun isConnectedToWifi(context: Context): Boolean {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                    ?: return false

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // SoftAP sin internet: activeNetwork a veces es datos móviles.
                // Contamos cualquier red WiFi asociada.
                connectivityManager.allNetworks.forEach { network ->
                    val caps = connectivityManager.getNetworkCapabilities(network)
                    if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                        return true
                    }
                }
                return false
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
     * Obtiene el SSID de la WiFi asociada.
     * Prioriza SoftAP SmartKiln/SmartLink si hay varias (AP local + casa).
     */
    fun getCurrentWifiSSID(context: Context): String? {
        try {
            collectWifiInfos(context).forEach { info ->
                parseSsid(wifiInfoToSsid(info))?.let { ssid ->
                    if (isDeviceSoftApSsid(ssid)) return ssid
                }
            }
            collectWifiInfos(context).forEach { info ->
                parseSsid(wifiInfoToSsid(info))?.let { return it }
            }
        } catch (e: Exception) {
            android.util.Log.e("WifiUtils", "Error obteniendo SSID: ${e.message}")
        }
        return null
    }

    /**
     * SoftAP al que el teléfono ya está conectado (aunque no aparezca en scanResults).
     */
    fun getConnectedDeviceAccessPoint(context: Context): KilnAccessPoint? {
        return try {
            collectWifiInfos(context).forEach { info ->
                val ssid = parseSsid(wifiInfoToSsid(info)) ?: return@forEach
                if (!isDeviceSoftApSsid(ssid)) return@forEach
                @Suppress("DEPRECATION")
                val bssid = info.bssid?.takeIf { it.isNotBlank() && it != "02:00:00:00:00:00" }
                    ?: ""
                return KilnAccessPoint(
                    ssid = ssid,
                    bssid = bssid,
                    signalLevel = info.rssi,
                    deviceType = deviceTypeFromSsid(ssid),
                    alreadyConnected = true
                )
            }
            null
        } catch (e: Exception) {
            android.util.Log.e("WifiUtils", "Error leyendo SoftAP conectado: ${e.message}")
            null
        }
    }

    private fun collectWifiInfos(context: Context): List<WifiInfo> {
        val out = mutableListOf<WifiInfo>()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                cm?.allNetworks?.forEach { network ->
                    val caps = cm.getNetworkCapabilities(network) ?: return@forEach
                    if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return@forEach
                    val transportInfo = caps.transportInfo
                    if (transportInfo is WifiInfo) out.add(transportInfo)
                }
            }
            @Suppress("DEPRECATION")
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiManager?.connectionInfo?.let { out.add(it) }
        } catch (e: Exception) {
            android.util.Log.e("WifiUtils", "Error listando WifiInfo: ${e.message}")
        }
        return out.distinctBy { "${parseSsid(wifiInfoToSsid(it))}|${it.bssid}" }
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
     * SoftAP visibles: horno (`SmartKiln-`) o datalogger (`SmartLink-`),
     * más la red SoftAP ya conectada (Android a menudo no la incluye en el scan).
     */
    fun getSmartKilnNetworks(context: Context): List<String> {
        return getSmartKilnAccessPoints(context).map { it.ssid }.distinct()
    }

    /**
     * Detalle de SoftAP: SSID, BSSID, señal y [SoftApDeviceType] por prefijo.
     * Passphrase WPA2 = últimos 8 hex del BSSID/MAC (o del sufijo SSID de 8).
     */
    @Suppress("DEPRECATION")
    fun getSmartKilnAccessPoints(context: Context): List<KilnAccessPoint> {
        return try {
            val connected = getConnectedDeviceAccessPoint(context)
            val wifiManager =
                context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val scanned = if (wifiManager?.isWifiEnabled == true) {
                wifiManager.startScan()
                wifiManager.scanResults
                    .asSequence()
                    .filter { !it.SSID.isNullOrBlank() && isDeviceSoftApSsid(it.SSID) }
                    .filter { !it.BSSID.isNullOrBlank() }
                    .sortedByDescending { it.level }
                    .map {
                        KilnAccessPoint(
                            ssid = it.SSID,
                            bssid = it.BSSID,
                            signalLevel = it.level,
                            deviceType = deviceTypeFromSsid(it.SSID),
                            alreadyConnected = connected?.ssid.equals(it.SSID, ignoreCase = true)
                        )
                    }
                    .toList()
            } else {
                emptyList()
            }

            buildList {
                if (connected != null) add(connected)
                scanned.forEach { ap ->
                    val dup = any {
                        it.ssid.equals(ap.ssid, ignoreCase = true) ||
                            (it.bssid.isNotBlank() && it.bssid.equals(ap.bssid, ignoreCase = true))
                    }
                    if (!dup) add(ap)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("WifiUtils", "Error escaneando SoftAP dispositivo: ${e.message}")
            emptyList()
        }
    }
}
