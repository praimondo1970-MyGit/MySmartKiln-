package com.example.mysmartkiln.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.mysmartkiln.data.local.KilnApiDefaults
import com.example.mysmartkiln.data.local.SoftApDeviceType
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Pide a Android conectar al AP del horno usando WifiNetworkSpecifier (API 29+).
 *
 * Diferencia clave con el flujo legacy: el sistema operativo muestra un diálogo
 * propio sobre la app (sin abrir Settings), el usuario aprueba y al recibir
 * onAvailable() podemos enrutar HTTP por esa red sin tener que volver a la app
 * desde el switcher de tareas.
 *
 * El callback de red queda registrado mientras dure la sesión local; cuando el
 * ESP cierra su AP llamamos release() para que Android suelte la asociación y
 * vuelva solo a la WiFi de casa.
 */
object KilnNetworkConnector {

    private const val TAG = "KilnNetConnector"

    @Volatile
    private var activeCallback: ConnectivityManager.NetworkCallback? = null

    @Volatile
    var activeNetwork: Network? = null
        private set

    /** Tras release(): evita re-pedir el AP en el mismo ciclo de resume (perfil C). */
    @Volatile
    var lastReleaseAtMs: Long = 0L
        private set

    fun isInPostHandoffCooldown(cooldownMs: Long): Boolean {
        if (lastReleaseAtMs <= 0L) return false
        return System.currentTimeMillis() - lastReleaseAtMs < cooldownMs
    }

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    /**
     * Verifica si tenemos los permisos runtime necesarios para llamar a
     * requestNetwork(WifiNetworkSpecifier). Si esto devuelve false, hay que
     * pedirlos antes de llamar a connect() o el sistema lanzará SecurityException.
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        if (!isSupported) return false
        // Android 13+ usa NEARBY_WIFI_DEVICES en lugar de FINE_LOCATION.
        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
        return ContextCompat.checkSelfPermission(context, needed) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** Permiso runtime concreto que requiere esta API en el dispositivo actual. */
    fun requiredRuntimePermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.NEARBY_WIFI_DEVICES
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }

    /**
     * Devuelve la Network del AP si la conexión fue aceptada (o ya estaba en
     * curso). Null si no está soportado, falló el sistema o el usuario rechazó.
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(
        context: Context,
        ssid: String,
        passphrase: String,
        timeoutMs: Long = 30_000L
    ): Network? {
        if (!isSupported) {
            Log.w(TAG, "WifiNetworkSpecifier no disponible (<API 29)")
            return null
        }
        if (!hasRequiredPermissions(context)) {
            Log.w(TAG, "Falta permiso ${requiredRuntimePermission()} — connect() no puede correr")
            return null
        }

        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null

        // Si ya hay un request previo, lo soltamos. Cada conexión nueva debe
        // empezar limpia para que el diálogo del sistema vuelva a aparecer
        // si el usuario lo cerró.
        release(context)

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(passphrase)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        return suspendCancellableCoroutine { cont ->
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    Log.d(TAG, "AP horno disponible: $network")
                    activeNetwork = network
                    if (cont.isActive) cont.resume(network)
                }

                override fun onUnavailable() {
                    Log.w(TAG, "AP horno no disponible (timeout o rechazo)")
                    activeNetwork = null
                    if (cont.isActive) cont.resume(null)
                }

                override fun onLost(network: Network) {
                    Log.w(TAG, "AP horno perdido: $network")
                    if (activeNetwork == network) activeNetwork = null
                }
            }

            activeCallback = callback

            try {
                cm.requestNetwork(request, callback, timeoutMs.toInt())
            } catch (e: SecurityException) {
                Log.e(TAG, "Sin permisos para requestNetwork: ${e.message}")
                activeCallback = null
                if (cont.isActive) cont.resume(null)
                return@suspendCancellableCoroutine
            }

            cont.invokeOnCancellation {
                Log.d(TAG, "connect cancelado, liberando")
                release(context)
            }
        }
    }

    /**
     * Suelta el callback activo. Android vuelve solo a la red anterior (la WiFi
     * de casa con internet validada).
     *
     * @param markHandoffCooldown Solo true tras una sesión SoftAP real (ESP apaga
     * el AP ~8–10 s perfil C). Cleanup al abrir la lista / salir de pantalla no debe
     * bloquear «cambiar programa» ni otros comandos locales.
     */
    fun release(context: Context, markHandoffCooldown: Boolean = false) {
        // Crítico: si quedó bindProcessToNetwork al SoftAP (sin internet),
        // Firebase/RTDB no llegan y TODOS los dispositivos aparecen desconectados.
        KilnWifiNetworkBinder.unbind(context)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (markHandoffCooldown) {
                lastReleaseAtMs = System.currentTimeMillis()
            }
            return
        }
        val cm = context.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return
        activeCallback?.let { cb ->
            try {
                cm.unregisterNetworkCallback(cb)
                Log.d(TAG, "callback liberado, Android vuelve a red previa")
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "callback ya desregistrado: ${e.message}")
            }
        }
        activeCallback = null
        activeNetwork = null
        if (markHandoffCooldown) {
            lastReleaseAtMs = System.currentTimeMillis()
            Log.d(TAG, "handoff SoftAP: cooldown de reconfiguración iniciado")
        }
    }

    /**
     * SSID SoftAP del horno: `SmartKiln-` + últimos 8 hex (= passphrase).
     * DataLogger usará [ssidFromMac] con prefijo SmartLink en su propio helper.
     */
    fun ssidFromMac(macAddress: String): String? {
        val pass = passphraseFromMac(macAddress) ?: return null
        return "${KilnApiDefaults.SMART_KILN_SSID_PREFIX}-$pass"
    }

    fun ssidFromMac(macAddress: String, type: SoftApDeviceType): String? {
        val pass = passphraseFromMac(macAddress) ?: return null
        val prefix = when (type) {
            SoftApDeviceType.DATALOGGER -> KilnApiDefaults.SMART_LINK_SSID_PREFIX
            SoftApDeviceType.KILN, SoftApDeviceType.UNKNOWN ->
                KilnApiDefaults.SMART_KILN_SSID_PREFIX
        }
        return "$prefix-$pass"
    }

    /**
     * Passphrase SoftAP = últimos 8 hex del MAC (WPA2, mismo sufijo del SSID).
     */
    fun passphraseFromMac(macAddress: String): String? {
        val clean = macAddress.replace(":", "").replace("-", "").uppercase()
        if (clean.length < 8) return null
        return clean.takeLast(8)
    }

    /**
     * Variante desde BSSID del scan. En ESP32 el SoftAP suele ser STA+1
     * (ej. …C569 vs …C568 del SSID/pass), así que **no** es fuente primaria.
     */
    fun passphraseFromBssid(bssid: String): String? = passphraseFromMac(bssid)

    /**
     * Si el SSID es `SmartKiln-XXXXXXXX` / `SmartLink-XXXXXXXX` (8 hex),
     * la clave es el sufijo. Legacy `SmartKiln-XXXX` (4) → null (usar BSSID).
     */
    fun passphraseFromSsid(ssid: String): String? {
        val cleaned = ssid.trim().removeSurrounding("\"")
        val suffix = when {
            cleaned.startsWith("${KilnApiDefaults.SMART_LINK_SSID_PREFIX}-", ignoreCase = true) ->
                cleaned.substring(KilnApiDefaults.SMART_LINK_SSID_PREFIX.length + 1)
            cleaned.startsWith("${KilnApiDefaults.SMART_KILN_SSID_PREFIX}-", ignoreCase = true) ->
                cleaned.substring(KilnApiDefaults.SMART_KILN_SSID_PREFIX.length + 1)
            else -> return null
        }
        val hex = suffix.replace(":", "").replace("-", "").uppercase()
        // Solo 8 hex: coincide con WPA2 pass del firmware. Sufijo corto = legacy.
        if (hex.length == 8 && hex.all { it in '0'..'9' || it in 'A'..'F' }) return hex
        return null
    }

    /**
     * Preferir sufijo del SSID (misma fuente que el firmware).
     * BSSID solo si el SSID no trae los 8 hex (legacy / sin sufijo).
     */
    fun resolvePassphrase(accessPoint: KilnAccessPoint): String? {
        passphraseFromSsid(accessPoint.ssid)?.let { return it }
        return passphraseFromBssid(accessPoint.bssid)
    }
}
