package com.example.mysmartkiln.data.local

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class LoggerRecordingSample(
    val elapsedMin: Float,
    val temperatureC: Float,
    val epochMs: Long = System.currentTimeMillis()
)

data class LoggerRecordingSession(
    val id: String,
    val deviceMac: String,
    val name: String,
    val comments: String,
    val startedAtMs: Long,
    val endedAtMs: Long? = null,
    val samples: List<LoggerRecordingSample> = emptyList()
) {
    val isActive: Boolean get() = endedAtMs == null
}

/**
 * Persistencia local simple de grabaciones del DataLogger (JSON en filesDir).
 * Evita migración Room por ahora; luego se puede mover a Room.
 */
class LoggerRecordingStore(context: Context) {

    private val file = File(context.applicationContext.filesDir, "logger_recordings.json")

    @Synchronized
    fun loadAll(): List<LoggerRecordingSession> {
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
    fun getById(sessionId: String): LoggerRecordingSession? =
        loadAll().firstOrNull { it.id == sessionId }

    @Synchronized
    fun upsert(session: LoggerRecordingSession) {
        val all = loadAll().toMutableList()
        val idx = all.indexOfFirst { it.id == session.id }
        if (idx >= 0) all[idx] = session else all.add(0, session)
        saveAll(all)
    }

    @Synchronized
    fun appendSample(sessionId: String, sample: LoggerRecordingSample) {
        val all = loadAll().toMutableList()
        val idx = all.indexOfFirst { it.id == sessionId }
        if (idx < 0) return
        val cur = all[idx]
        all[idx] = cur.copy(samples = cur.samples + sample)
        saveAll(all)
    }

    @Synchronized
    fun delete(sessionId: String): Boolean {
        val all = loadAll().toMutableList()
        val removed = all.removeAll { it.id == sessionId }
        if (removed) saveAll(all)
        return removed
    }

    private fun saveAll(sessions: List<LoggerRecordingSession>) {
        val arr = JSONArray()
        sessions.forEach { s ->
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("deviceMac", s.deviceMac)
                    put("name", s.name)
                    put("comments", s.comments)
                    put("startedAtMs", s.startedAtMs)
                    if (s.endedAtMs != null) put("endedAtMs", s.endedAtMs)
                    put(
                        "samples",
                        JSONArray().also { ja ->
                            s.samples.forEach { sample ->
                                ja.put(
                                    JSONObject().apply {
                                        put("elapsedMin", sample.elapsedMin.toDouble())
                                        put("temperatureC", sample.temperatureC.toDouble())
                                        put("epochMs", sample.epochMs)
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

    private fun parseSession(obj: JSONObject): LoggerRecordingSession? {
        return try {
            val samplesArr = obj.optJSONArray("samples") ?: JSONArray()
            val samples = buildList {
                for (i in 0 until samplesArr.length()) {
                    val s = samplesArr.getJSONObject(i)
                    add(
                        LoggerRecordingSample(
                            elapsedMin = s.getDouble("elapsedMin").toFloat(),
                            temperatureC = s.getDouble("temperatureC").toFloat(),
                            epochMs = s.optLong("epochMs", 0L)
                        )
                    )
                }
            }
            LoggerRecordingSession(
                id = obj.getString("id"),
                deviceMac = obj.getString("deviceMac"),
                name = obj.getString("name"),
                comments = obj.optString("comments", ""),
                startedAtMs = obj.getLong("startedAtMs"),
                endedAtMs = if (obj.has("endedAtMs")) obj.getLong("endedAtMs") else null,
                samples = samples
            )
        } catch (_: Exception) {
            null
        }
    }
}
