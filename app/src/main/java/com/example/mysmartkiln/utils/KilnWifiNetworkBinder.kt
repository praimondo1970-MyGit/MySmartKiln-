package com.example.mysmartkiln.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import java.net.HttpURLConnection
import java.net.URL

/**
 * Enruta HTTP local (192.168.4.1) por la WiFi del horno (sin internet validada).
 */
object KilnWifiNetworkBinder {

    private const val TAG = "KilnWifiNetworkBinder"

    /**
     * Red WiFi del horno: sin internet "validada" (típico del AP SmartKiln).
     * Evita enlazar por error a la WiFi de casa que sigue visible en el teléfono.
     */
    fun findKilnNetwork(context: Context): Network? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return null

        // Si KilnNetworkConnector pidió la red al sistema (WifiNetworkSpecifier
        // en API 29+), esa Network es la fuente de verdad: NO depende de
        // cm.activeNetwork ni del SSID que reporta WifiManager.
        KilnNetworkConnector.activeNetwork?.let { net ->
            cm.getNetworkCapabilities(net)?.let { caps ->
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.d(TAG, "Usando network del horno (specifier): $net")
                    return net
                }
            }
        }

        val onKilnSsid = WifiUtils.isOnSmartKilnNetwork(context)
        val active = cm.activeNetwork
        val activeCaps = active?.let { cm.getNetworkCapabilities(it) }
        if (onKilnSsid &&
            activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        ) {
            Log.d(TAG, "WiFi activa SmartKiln: $active")
            return active
        }

        var kilnCandidate: Network? = null
        var validatedWifi: Network? = null

        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue

            val isValidated =
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            if (!isValidated) {
                Log.d(TAG, "Red WiFi sin validar (candidata horno): $network")
                kilnCandidate = network
                if (onKilnSsid) return network
            } else {
                validatedWifi = network
            }
        }

        if (kilnCandidate != null) {
            return kilnCandidate
        }

        if (activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            Log.d(TAG, "Usando WiFi activa: $active")
            return active
        }

        if (validatedWifi != null) {
            Log.d(TAG, "Solo WiFi validada disponible: $validatedWifi")
            return validatedWifi
        }

        Log.w(TAG, "No hay red WiFi candidata para el horno")
        return null
    }

    fun openHttpConnection(context: Context, url: URL): HttpURLConnection {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            findKilnNetwork(context)?.let { network ->
                Log.d(TAG, "openConnection via red horno: $url")
                return network.openConnection(url) as HttpURLConnection
            }
        }
        Log.d(TAG, "openConnection por defecto: $url")
        return url.openConnection() as HttpURLConnection
    }

    /** Respaldo si openConnection por red falla. */
    fun bindToWifi(context: Context): Boolean {
        val network = findKilnNetwork(context) ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return false
        return cm.bindProcessToNetwork(network).also { ok ->
            if (ok) Log.d(TAG, "bindProcessToNetwork $network")
        }
    }

    fun unbind(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager ?: return
        cm.bindProcessToNetwork(null)
    }

    inline fun <T> withKilnNetwork(context: Context, block: () -> T): T {
        return block()
    }
}
