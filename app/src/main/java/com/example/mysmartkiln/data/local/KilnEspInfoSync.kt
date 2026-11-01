package com.example.mysmartkiln.data.local

import android.content.Context
import android.util.Log
import com.example.mysmartkiln.utils.KilnNetworkConnector
import com.example.mysmartkiln.utils.WifiUtils
import kotlinx.coroutines.delay

/**
 * Envía kiln-info al ESP: ping directo o auto-conexión SoftAP.
 *
 * Fallos intermitentes típicos: bootstrap SoftAP en paralelo, DHCP lento,
 * o [KilnNetworkConnector.connect] que hacía release() aunque ya estuviéramos
 * en el AP. Aquí reintentamos HTTP y reutilizamos SoftAP activo.
 */
object KilnEspInfoSync {
    private const val TAG = "KilnEspInfoSync"

    sealed class UploadOutcome {
        data object Success : UploadOutcome()
        data class Failure(val reason: String) : UploadOutcome()
    }

    suspend fun upload(
        context: Context,
        kilnMacNoColons: String,
        kilnHttpClient: KilnHttpClient,
        kilnInfoJson: String,
        deviceType: SoftApDeviceType = SoftApDeviceType.KILN,
        onDetail: (String) -> Unit = {}
    ): Boolean = uploadDetailed(
        context, kilnMacNoColons, kilnHttpClient, kilnInfoJson, deviceType, onDetail
    ) is UploadOutcome.Success

    suspend fun uploadDetailed(
        context: Context,
        kilnMacNoColons: String,
        kilnHttpClient: KilnHttpClient,
        kilnInfoJson: String,
        deviceType: SoftApDeviceType = SoftApDeviceType.KILN,
        onDetail: (String) -> Unit = {},
        /** false en modo LOCAL: SoftAP sticky para lista/detalle. */
        releaseAfter: Boolean = true
    ): UploadOutcome {
        suspend fun tryUploadOnce(
            connectTimeoutMs: Int,
            readTimeoutMs: Int
        ): UploadOutcome {
            onDetail("Enviando configuración al dispositivo…")
            if (!kilnHttpClient.ping(connectTimeoutMs, readTimeoutMs)) {
                return UploadOutcome.Failure("Sin respuesta HTTP en SoftAP (ping)")
            }
            val result = kilnHttpClient.uploadKilnInfo(kilnInfoJson)
            return result.fold(
                onSuccess = {
                    Log.d(TAG, "kiln-info enviado por HTTP")
                    UploadOutcome.Success
                },
                onFailure = {
                    Log.w(TAG, "kiln-info HTTP: ${it.message}")
                    UploadOutcome.Failure(it.message ?: "HTTP kiln-info falló")
                }
            )
        }

        suspend fun tryUploadWithRetries(
            attempts: Int,
            connectTimeoutMs: Int = 2500,
            readTimeoutMs: Int = 2500,
            gapMs: Long = 350L
        ): UploadOutcome {
            var last: UploadOutcome = UploadOutcome.Failure("Sin respuesta HTTP en SoftAP (ping)")
            repeat(attempts) { i ->
                last = tryUploadOnce(connectTimeoutMs, readTimeoutMs)
                if (last is UploadOutcome.Success) return last
                if (i < attempts - 1) delay(gapMs * (i + 1))
            }
            return last
        }

        var shouldMarkCooldown = false
        try {
            onDetail("Comprobando conexión con el dispositivo…")
            val direct = tryUploadWithRetries(attempts = 3, connectTimeoutMs = 2000, readTimeoutMs = 2000)
            if (direct is UploadOutcome.Success) {
                shouldMarkCooldown = true
                return direct
            }

            if (!KilnNetworkConnector.isSupported) {
                return UploadOutcome.Failure(
                    "Este Android no soporta conexión automática al SoftAP"
                )
            }
            if (!KilnNetworkConnector.hasRequiredPermissions(context)) {
                return UploadOutcome.Failure(
                    "Falta permiso de WiFi cercano / ubicación"
                )
            }

            val ssid = KilnNetworkConnector.ssidFromMac(kilnMacNoColons, deviceType)
                ?: return UploadOutcome.Failure("MAC inválida para SoftAP")
            val pass = KilnNetworkConnector.passphraseFromMac(kilnMacNoColons)
                ?: return UploadOutcome.Failure("No se pudo derivar la clave SoftAP")

            val currentSsid = WifiUtils.getCurrentWifiSSID(context)
            val alreadyOnTarget =
                KilnNetworkConnector.activeNetwork != null ||
                    (!currentSsid.isNullOrBlank() &&
                        currentSsid.equals(ssid, ignoreCase = true))

            if (alreadyOnTarget) {
                onDetail("Reintentando en SoftAP ya conectado…")
                Log.d(TAG, "SoftAP ya asociado ($ssid) — reintento HTTP sin reconnect")
                delay(800)
                val reused = tryUploadWithRetries(attempts = 5)
                if (reused is UploadOutcome.Success) {
                    shouldMarkCooldown = true
                    return reused
                }
            }

            onDetail("Conectando a $ssid…")
            Log.d(TAG, "Auto-conexión a $ssid para kiln-info")
            if (KilnNetworkConnector.connect(context, ssid, pass) == null) {
                return UploadOutcome.Failure(
                    "No se unió a $ssid (cancelado o timeout del diálogo de Android)"
                )
            }
            shouldMarkCooldown = true
            // DHCP / binder de red: varios intentos (mismo patrón que bootstrap LOCAL).
            delay(1500)
            return tryUploadWithRetries(attempts = 6, gapMs = 400L)
        } finally {
            if (releaseAfter) {
                delay(450)
                val softApStillUp = try {
                    kilnHttpClient.ping(connectTimeoutMs = 1200, readTimeoutMs = 1200)
                } catch (_: Exception) {
                    false
                }
                KilnNetworkConnector.release(
                    context,
                    markHandoffCooldown = shouldMarkCooldown && !softApStillUp
                )
            }
        }
    }
}
