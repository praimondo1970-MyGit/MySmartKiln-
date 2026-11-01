package com.example.mysmartkiln.data.local

import android.content.Context
import android.util.Log
import com.example.mysmartkiln.data.repository.KilnRepository
import com.example.mysmartkiln.utils.DeviceKind
import com.example.mysmartkiln.utils.KilnNetworkConnector
import com.example.mysmartkiln.utils.WifiUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Une SoftAP en segundo plano al pasar a modo LOCAL (sobrevive salir de Ajustes).
 * Un SoftAP a la vez: prioriza el primer horno registrado.
 */
object KilnLocalLinkBootstrap {
    private const val TAG = "KilnLocalLinkBootstrap"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()
    private var job: Job? = null

    /**
     * Local→Online: retener SoftAP del teléfono hasta que kiln-info
     * (`offlineOnly=false`) responda OK. Evita soltar el AP y tener que
     * re-pedir el diálogo SoftAP (fallos intermitentes de ACTUALIZAR).
     * No afecta comandos START/PAUSE/etc. (siguen su propio release).
     */
    @Volatile
    private var holdSoftApForOnlinePush = false

    /**
     * Arranca (o reusa) conexión SoftAP sticky + kiln-info offlineOnly.
     * No muestra diálogos Compose; Android puede pedir WiFi/permiso una vez.
     */
    fun startLocalMode(
        context: Context,
        kilnRepository: KilnRepository,
        kilnHttpClient: KilnHttpClient,
        userId: String,
        pushKilnInfo: Boolean = true
    ) {
        val app = context.applicationContext
        holdSoftApForOnlinePush = false
        job?.cancel()
        job = scope.launch {
            mutex.withLock {
                runCatching {
                    ensureLocalLink(
                        context = app,
                        kilnRepository = kilnRepository,
                        kilnHttpClient = kilnHttpClient,
                        userId = userId,
                        pushKilnInfo = pushKilnInfo
                    )
                }.onFailure {
                    Log.w(TAG, "ensureLocalLink: ${it.message}")
                }
            }
        }
    }

    /**
     * Al elegir Online en Ajustes: cancela bootstrap Local y **retiene** SoftAP
     * hasta [completeOnlineHandoff] (tras kiln-info OK).
     */
    fun armOnlineHandoffKeepSoftAp() {
        job?.cancel()
        job = null
        holdSoftApForOnlinePush = true
        Log.d(TAG, "Online handoff: SoftAP retenido hasta kiln-info OK")
    }

    fun isOnlineHandoffPending(): Boolean = holdSoftApForOnlinePush

    /**
     * Tras kiln-info Online OK: soltar SoftAP del teléfono para que el ESP
     * pueda completar STA/Firebase y el móvil vuelva a WiFi de casa.
     */
    fun completeOnlineHandoff(context: Context) {
        holdSoftApForOnlinePush = false
        job?.cancel()
        job = null
        scope.launch {
            KilnNetworkConnector.release(
                context.applicationContext,
                markHandoffCooldown = true
            )
            Log.d(TAG, "Online handoff completo — SoftAP liberado")
        }
    }

    /**
     * Modo ONLINE en lista / residual: suelta SoftAP salvo handoff pendiente
     * (Local→Online aún sin kiln-info OK).
     */
    fun onOnlineMode(context: Context) {
        job?.cancel()
        job = null
        if (holdSoftApForOnlinePush) {
            Log.d(TAG, "onOnlineMode: SoftAP retenido (kiln-info Online pendiente)")
            return
        }
        scope.launch {
            KilnNetworkConnector.release(context.applicationContext, markHandoffCooldown = false)
        }
    }

    /**
     * Espera a que termine el bootstrap SoftAP en curso (p. ej. antes de
     * ACTUALIZAR HORNO) para no pelear por WifiNetworkSpecifier.
     */
    suspend fun awaitIdle() {
        job?.join()
    }

    /** Cancela bootstrap pendiente (p. ej. al pasar a ONLINE y empujar kiln-info). */
    fun cancelPending() {
        job?.cancel()
        job = null
    }

    /**
     * @return true si SoftAP responde (ping) para el horno objetivo.
     */
    suspend fun ensureLocalLink(
        context: Context,
        kilnRepository: KilnRepository,
        kilnHttpClient: KilnHttpClient,
        userId: String,
        pushKilnInfo: Boolean = true
    ): Boolean {
        val all = kilnRepository.getAllKilns().first()
        val candidates = all.filter { kiln ->
            userId.isBlank() ||
                kiln.userId.isBlank() ||
                kiln.userId.equals(userId, ignoreCase = true)
        }.ifEmpty { all }
        val kiln = candidates.firstOrNull { !DeviceKind.isDataLogger(it) }
        if (kiln == null) {
            Log.w(TAG, "Sin horno para SoftAP LOCAL")
            return false
        }

        if (kilnHttpClient.ping()) {
            Log.d(TAG, "SoftAP ya responde (${kiln.macAddress})")
            if (pushKilnInfo) {
                pushOfflineOnly(context, kilnHttpClient, kiln, releaseAfter = false)
            }
            return true
        }

        if (!KilnNetworkConnector.isSupported) {
            Log.w(TAG, "WifiNetworkSpecifier no soportado")
            return false
        }
        if (!KilnNetworkConnector.hasRequiredPermissions(context)) {
            Log.w(TAG, "Sin permiso nearby/location — SoftAP diferido")
            return false
        }

        val ssid = KilnNetworkConnector.ssidFromMac(kiln.macAddress, SoftApDeviceType.KILN)
            ?: return false
        val pass = KilnNetworkConnector.passphraseFromMac(kiln.macAddress) ?: return false

        // Ya asociados al SSID correcto (p. ej. sesión previa).
        val currentSsid = WifiUtils.getCurrentWifiSSID(context)
        if (!currentSsid.isNullOrBlank() &&
            currentSsid.equals(ssid, ignoreCase = true) &&
            kilnHttpClient.ping()
        ) {
            if (pushKilnInfo) {
                pushOfflineOnly(context, kilnHttpClient, kiln, releaseAfter = false)
            }
            return true
        }

        Log.d(TAG, "Conectando SoftAP sticky → $ssid")
        val network = KilnNetworkConnector.connect(context, ssid, pass, timeoutMs = 12_000L)
        if (network == null) {
            Log.w(TAG, "SoftAP no unido ($ssid)")
            return false
        }
        delay(400)
        var ok = false
        repeat(4) {
            if (kilnHttpClient.ping()) {
                ok = true
                return@repeat
            }
            delay(250)
        }
        if (!ok) {
            Log.w(TAG, "Red SoftAP sin HTTP — se mantiene bind por reintento")
            return false
        }
        if (pushKilnInfo) {
            pushOfflineOnly(context, kilnHttpClient, kiln, releaseAfter = false)
        }
        Log.d(TAG, "SoftAP LOCAL OK sticky (${kiln.macAddress})")
        return true
    }

    private suspend fun pushOfflineOnly(
        context: Context,
        kilnHttpClient: KilnHttpClient,
        kiln: com.example.mysmartkiln.data.local.entity.KilnEntity,
        releaseAfter: Boolean
    ) {
        val updated = kiln.copy(offlineOnly = true, isPendingSync = true)
        val json = KilnInfoJsonBuilder.build(
            name = updated.name,
            model = updated.manufacturer,
            volume = updated.volume,
            userId = updated.userId,
            type = DeviceKind.firebaseType(updated),
            offlineOnly = true,
            thermocoupleType = DeviceKind.thermocoupleOrNull(updated)
        )
        KilnEspInfoSync.uploadDetailed(
            context = context,
            kilnMacNoColons = updated.macAddress,
            kilnHttpClient = kilnHttpClient,
            kilnInfoJson = json,
            deviceType = SoftApDeviceType.KILN,
            releaseAfter = releaseAfter
        )
    }
}
