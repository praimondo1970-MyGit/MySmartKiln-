package com.example.mysmartkiln.data.local

import android.content.Context
import android.util.Log
import com.example.mysmartkiln.data.local.entity.KilnEntity
import com.example.mysmartkiln.data.repository.KilnRepository
import com.example.mysmartkiln.utils.DeviceKind
import com.example.mysmartkiln.utils.KilnOfflineSession
import kotlinx.coroutines.flow.first

/**
 * Propaga modo Online/Local de Ajustes → Room + ESP (`offlineOnly` en kiln-info).
 *
 * LOCAL  → offlineOnly=true  (SoftAP sticky, sin STA/Firebase)
 * ONLINE → offlineOnly=false (recupera STA con WiFi NVS del ESP)
 *
 * [applyAppFlags] actualiza Room/sesión al instante (UI lista/detalle).
 * [applyToUserKilns] re-aplica flags y empuja kiln-info por SoftAP.
 */
object OperationModeEspSync {
    private const val TAG = "OperationModeEspSync"

    data class Result(
        val total: Int,
        val synced: Int,
        val syncedNames: List<String> = emptyList(),
        val failedNames: List<String> = emptyList(),
        val lastFailureReason: String? = null
    )

    /**
     * Política inmediata: Room `offlineOnly` + [KilnOfflineSession].
     * No habla con el ESP — la UI puede reflejar Local/Remoto al instante.
     * @return cantidad de filas actualizadas en Room
     */
    suspend fun applyAppFlags(
        kilnRepository: KilnRepository,
        userId: String,
        offlineOnly: Boolean
    ): Int {
        val candidates = resolveCandidates(kilnRepository, userId)
        if (candidates.isEmpty()) {
            Log.w(TAG, "applyAppFlags: sin hornos en Room (userId=$userId)")
            return 0
        }
        for (kiln in candidates) {
            val updated = kiln.copy(offlineOnly = offlineOnly, isPendingSync = true)
            kilnRepository.saveKiln(updated)
            if (DeviceKind.isDataLogger(kiln)) continue
            if (offlineOnly) {
                KilnOfflineSession.markPreferLocal(kiln.macAddress)
            } else {
                KilnOfflineSession.clearPreferLocal(kiln.macAddress)
            }
        }
        if (!offlineOnly) {
            // Por si quedaron MACs sin fila Room.
            KilnOfflineSession.clearAllPreferLocal()
        }
        Log.d(TAG, "applyAppFlags offlineOnly=$offlineOnly → ${candidates.size} fila(s) Room")
        return candidates.size
    }

    suspend fun applyToUserKilns(
        context: Context,
        kilnRepository: KilnRepository,
        kilnHttpClient: KilnHttpClient,
        userId: String,
        offlineOnly: Boolean,
        onDetail: (String) -> Unit = {}
    ): Result {
        applyAppFlags(kilnRepository, userId, offlineOnly)

        val softApTargets = resolveCandidates(kilnRepository, userId)
            .filterNot { DeviceKind.isDataLogger(it) }

        if (softApTargets.isEmpty()) {
            Log.w(TAG, "Solo DataLogger(s) o sin hornos — nada que actualizar por SoftAP")
            return Result(
                total = 0,
                synced = 0,
                syncedNames = emptyList(),
                failedNames = emptyList(),
                lastFailureReason = "No hay hornos registrados para actualizar"
            )
        }

        Log.d(
            TAG,
            "Propagando offlineOnly=$offlineOnly a ${softApTargets.size} horno(s) " +
                "(userId=$userId)"
        )

        var synced = 0
        val syncedNames = mutableListOf<String>()
        val failed = mutableListOf<String>()
        var lastReason: String? = null

        for (kiln in softApTargets) {
            val updated = kiln.copy(offlineOnly = offlineOnly, isPendingSync = true)
            val label = kiln.name.ifBlank { kiln.macAddress }
            onDetail("Actualizando $label…")
            // No soltar SoftAP aquí: LOCAL lo necesita sticky; ONLINE lo libera
            // solo tras Success (completeOnlineHandoff) para reintentar si falla.
            val outcome = pushKilnInfo(
                context = context,
                kilnHttpClient = kilnHttpClient,
                kiln = updated,
                onDetail = onDetail,
                releaseAfter = false
            )
            when (outcome) {
                is KilnEspInfoSync.UploadOutcome.Success -> {
                    synced++
                    syncedNames += label
                    Log.d(TAG, "ESP OK ${kiln.macAddress} offlineOnly=$offlineOnly")
                    // Un SoftAP a la vez: en LOCAL nos quedamos con el primero OK.
                    if (offlineOnly) break
                }
                is KilnEspInfoSync.UploadOutcome.Failure -> {
                    failed += label
                    lastReason = outcome.reason
                    Log.w(TAG, "ESP no alcanzó ${kiln.macAddress}: ${outcome.reason}")
                }
            }
        }

        if (!offlineOnly && synced > 0) {
            // ESP ya tiene offlineOnly=0 y arma release SoftAP; el teléfono
            // debe soltar el AP para que el STA/Firebase avance.
            KilnLocalLinkBootstrap.completeOnlineHandoff(context)
        }

        return Result(
            total = softApTargets.size,
            synced = synced,
            syncedNames = syncedNames,
            failedNames = failed,
            lastFailureReason = lastReason
        )
    }

    private suspend fun resolveCandidates(
        kilnRepository: KilnRepository,
        userId: String
    ): List<KilnEntity> {
        val all = kilnRepository.getAllKilns().first()
        return all.filter { kiln ->
            userId.isBlank() ||
                kiln.userId.isBlank() ||
                kiln.userId.equals(userId, ignoreCase = true)
        }.ifEmpty { all }
    }

    private suspend fun pushKilnInfo(
        context: Context,
        kilnHttpClient: KilnHttpClient,
        kiln: KilnEntity,
        onDetail: (String) -> Unit,
        releaseAfter: Boolean = true
    ): KilnEspInfoSync.UploadOutcome {
        val isLogger = DeviceKind.isDataLogger(kiln)
        val type = DeviceKind.firebaseType(kiln)
        val modelForEsp = if (isLogger) "DATALOGGER" else kiln.manufacturer
        val json = KilnInfoJsonBuilder.build(
            name = kiln.name,
            model = modelForEsp,
            volume = kiln.volume,
            userId = kiln.userId,
            type = type,
            offlineOnly = kiln.offlineOnly,
            thermocoupleType = DeviceKind.thermocoupleOrNull(kiln)
        )
        Log.d(TAG, "kiln-info body: $json")
        return KilnEspInfoSync.uploadDetailed(
            context = context,
            kilnMacNoColons = kiln.macAddress,
            kilnHttpClient = kilnHttpClient,
            kilnInfoJson = json,
            deviceType = if (isLogger) SoftApDeviceType.DATALOGGER else SoftApDeviceType.KILN,
            onDetail = onDetail,
            releaseAfter = releaseAfter
        )
    }
}
