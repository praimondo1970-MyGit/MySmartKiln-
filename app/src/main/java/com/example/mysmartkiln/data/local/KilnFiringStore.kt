package com.example.mysmartkiln.data.local

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager
import com.example.mysmartkiln.data.model.ActivityRecord
import com.example.mysmartkiln.data.work.SyncWorker
import com.example.mysmartkiln.ui.viewmodels.ProgramStage
import com.example.mysmartkiln.ui.viewmodels.TemperatureSample
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Persistencia local de cocciones de horno (curva medida + etapas del programa).
 * Archivo: kiln_firings.json
 *
 * [syncedToCloud]=false → pendiente de subir a Firebase (reintento transparente).
 */
data class KilnFiringSession(
    val id: String,
    val deviceMac: String,
    val programName: String,
    val deviceName: String = "",
    val startedAtMs: Long,
    val endedAtMs: Long,
    val samples: List<TemperatureSample> = emptyList(),
    val stages: List<ProgramStage> = emptyList(),
    /** true = cocción llegó a FINALIZADO; false = STOP / cancelada / corte. */
    val complete: Boolean = true,
    val syncedToCloud: Boolean = false
)

class KilnFiringStore(context: Context) {

    private val file = File(context.applicationContext.filesDir, "kiln_firings.json")

    @Synchronized
    fun loadAll(): List<KilnFiringSession> {
        if (!file.exists()) return emptyList()
        return try {
            val root = JSONArray(file.readText())
            buildList {
                for (i in 0 until root.length()) {
                    parseSession(root.getJSONObject(i))?.let { add(it) }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun getById(firingId: String): KilnFiringSession? =
        loadAll().firstOrNull { it.id == firingId }

    @Synchronized
    fun pendingCloudSync(): List<KilnFiringSession> =
        loadAll().filter { !it.syncedToCloud }

    @Synchronized
    fun upsert(session: KilnFiringSession) {
        val all = loadAll().toMutableList()
        val idx = all.indexOfFirst { it.id == session.id }
        if (idx >= 0) all[idx] = session else all.add(0, session)
        saveAll(all)
    }

    @Synchronized
    fun markSynced(firingId: String) {
        val all = loadAll().toMutableList()
        val idx = all.indexOfFirst { it.id == firingId }
        if (idx < 0) return
        all[idx] = all[idx].copy(syncedToCloud = true)
        saveAll(all)
    }

    @Synchronized
    fun delete(firingId: String): Boolean {
        val all = loadAll().toMutableList()
        val removed = all.removeAll { it.id == firingId }
        if (removed) saveAll(all)
        return removed
    }

    private fun saveAll(sessions: List<KilnFiringSession>) {
        val arr = JSONArray()
        sessions.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("deviceMac", s.deviceMac)
                    put("programName", s.programName)
                    put("deviceName", s.deviceName)
                    put("startedAtMs", s.startedAtMs)
                    put("endedAtMs", s.endedAtMs)
                    put("complete", s.complete)
                    put("syncedToCloud", s.syncedToCloud)
                    put(
                        "samples",
                        JSONArray().also { ja ->
                            s.samples.forEach { sample ->
                                ja.put(
                                    JSONObject().apply {
                                        put("elapsedMin", sample.elapsedMin.toDouble())
                                        put("temperatureC", sample.temperatureC.toDouble())
                                    }
                                )
                            }
                        }
                    )
                    put(
                        "stages",
                        JSONArray().also { ja ->
                            s.stages.forEach { stage ->
                                ja.put(
                                    JSONObject().apply {
                                        put("stageNumber", stage.stageNumber)
                                        put("targetTemp", stage.targetTemp.toDouble())
                                        put("rampRate", stage.rampRate.toDouble())
                                        put("maintenanceMinutes", stage.maintenanceMinutes)
                                        put("durationMinutes", stage.durationMinutes)
                                    }
                                )
                            }
                        }
                    )
                }
            )
        }
        file.writeText(arr.toString())
    }

    private fun parseSession(obj: JSONObject): KilnFiringSession? {
        return try {
            val samplesArr = obj.optJSONArray("samples") ?: JSONArray()
            val samples = buildList {
                for (i in 0 until samplesArr.length()) {
                    val s = samplesArr.getJSONObject(i)
                    add(
                        TemperatureSample(
                            elapsedMin = s.getDouble("elapsedMin").toFloat(),
                            temperatureC = s.getDouble("temperatureC").toFloat()
                        )
                    )
                }
            }
            val stagesArr = obj.optJSONArray("stages") ?: JSONArray()
            val stages = buildList {
                for (i in 0 until stagesArr.length()) {
                    val st = stagesArr.getJSONObject(i)
                    add(
                        ProgramStage(
                            stageNumber = st.optInt("stageNumber", i + 1),
                            targetTemp = st.getDouble("targetTemp").toFloat(),
                            rampRate = st.optDouble("rampRate", 0.0).toFloat(),
                            maintenanceMinutes = st.optInt("maintenanceMinutes", 0),
                            durationMinutes = st.optInt("durationMinutes", 0)
                        )
                    )
                }
            }
            KilnFiringSession(
                id = obj.getString("id"),
                deviceMac = obj.getString("deviceMac"),
                programName = obj.optString("programName", "Cocción"),
                deviceName = obj.optString("deviceName", ""),
                startedAtMs = obj.getLong("startedAtMs"),
                endedAtMs = obj.getLong("endedAtMs"),
                samples = samples,
                stages = stages,
                complete = obj.optBoolean("complete", true),
                syncedToCloud = obj.optBoolean("syncedToCloud", false)
            )
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Sube cocciones locales pendientes a Firebase (recordings + activity).
 * Transparente: sin UI; se dispara al archivar, al abrir Actividad y vía WorkManager.
 */
object KilnFiringCloudSync {

    private const val TAG = "KilnFiringCloudSync"
    private const val WORK_NAME = "sync_kiln_firings"

    suspend fun uploadSession(
        userId: String,
        session: KilnFiringSession,
        firebase: FirebaseRealtimeManager = FirebaseRealtimeManager()
    ): Boolean {
        val maxTemp = session.samples.maxOfOrNull { it.temperatureC }?.toInt() ?: 0
        val durationMin = session.samples.maxOfOrNull { it.elapsedMin }?.toInt()
            ?: ((session.endedAtMs - session.startedAtMs) / 60_000L).toInt().coerceAtLeast(0)
        val sampleMaps = session.samples.map { s ->
            mapOf(
                "elapsedMin" to s.elapsedMin,
                "temperatureC" to s.temperatureC
            )
        }
        val stageMaps = session.stages.map { st ->
            mapOf(
                "stageNumber" to st.stageNumber,
                "targetTemp" to st.targetTemp,
                "rampRate" to st.rampRate,
                "maintenanceMinutes" to st.maintenanceMinutes,
                "durationMinutes" to st.durationMinutes
            )
        }
        val rec = firebase.saveKilnFiringRecording(
            userId = userId,
            mac = session.deviceMac,
            recordingId = session.id,
            meta = mapOf(
                "type" to "KILN",
                "name" to session.programName,
                "deviceMac" to session.deviceMac,
                "startedAtMs" to session.startedAtMs,
                "endedAtMs" to session.endedAtMs,
                "sampleCount" to session.samples.size,
                "active" to false,
                "ended" to true,
                "complete" to session.complete
            ),
            samples = sampleMaps,
            stages = stageMaps
        )
        if (rec.isFailure) {
            Log.e(TAG, "recordings falló ${session.id}: ${rec.exceptionOrNull()?.message}")
            return false
        }
        val act = firebase.upsertActivity(
            userId = userId,
            record = ActivityRecord(
                id = session.id,
                type = "KILN",
                deviceMac = session.deviceMac,
                deviceName = session.deviceName,
                name = session.programName,
                comments = "",
                startedAtMs = session.startedAtMs,
                endedAtMs = session.endedAtMs,
                durationMin = durationMin,
                maxTemperatureC = maxTemp,
                sampleCount = session.samples.size,
                complete = session.complete,
                source = "APP",
                recordingId = session.id
            )
        )
        if (act.isFailure) {
            Log.e(TAG, "activity falló ${session.id}: ${act.exceptionOrNull()?.message}")
            return false
        }
        return true
    }

    /** Sube todas las pendientes; marca syncedToCloud al OK. */
    suspend fun syncPending(
        context: Context,
        userId: String?,
        firebase: FirebaseRealtimeManager = FirebaseRealtimeManager()
    ): Int {
        if (userId.isNullOrBlank()) return 0
        val store = KilnFiringStore(context)
        val pending = store.pendingCloudSync()
        if (pending.isEmpty()) return 0
        var ok = 0
        for (session in pending) {
            if (uploadSession(userId, session, firebase)) {
                store.markSynced(session.id)
                ok++
                Log.d(TAG, "Sincronizada ${session.id}")
            }
        }
        if (ok < pending.size) {
            scheduleRetry(context)
        }
        return ok
    }

    fun scheduleRetry(context: Context) {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
        Log.d(TAG, "WorkManager programado para reintentar firings")
    }
}
