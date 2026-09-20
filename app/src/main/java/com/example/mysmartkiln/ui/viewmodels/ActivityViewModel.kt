package com.example.mysmartkiln.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mysmartkiln.data.auth.AuthRepository
import com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager
import com.example.mysmartkiln.data.local.LoggerRecordingSample
import com.example.mysmartkiln.data.local.LoggerRecordingSession
import com.example.mysmartkiln.data.local.LoggerRecordingStore
import com.example.mysmartkiln.data.local.KilnFiringSession
import com.example.mysmartkiln.data.local.KilnFiringStore
import com.example.mysmartkiln.data.local.KilnFiringCloudSync
import com.example.mysmartkiln.data.local.database.AppDatabase
import com.example.mysmartkiln.data.model.ActivityRecord
import com.example.mysmartkiln.utils.UserIdUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActivityUiItem(
    val id: String,
    val type: String,
    val deviceMac: String,
    val recordingId: String?,
    val kilnName: String,
    val kilnBrand: String,
    val kilnVolume: String,
    val executionDate: String,
    val programName: String,
    val maxTemperature: String,
    val duration: String,
    val timestamp: Long,
    val monthLabel: String,
    /** true = FINALIZADO normal; false = STOP / cancelada / incompleta. */
    val complete: Boolean = true
) {
    val isDataLogger: Boolean
        get() = type.equals("DATALOGGER", ignoreCase = true)

    val isKiln: Boolean
        get() = type.equals("KILN", ignoreCase = true)

    val canOpenCurve: Boolean
        get() = deviceMac.isNotBlank() && !recordingId.isNullOrBlank()

    /** Etiqueta de resultado para la tarjeta (solo hornos). */
    val outcomeLabel: String?
        get() = when {
            !isKiln -> null
            complete -> "Finalizado"
            else -> "Interrumpida"
        }
}

data class ActivityCurveDetail(
    val title: String,
    val subtitle: String,
    val samples: List<TemperatureSample> = emptyList(),
    val stages: List<ProgramStage> = emptyList(),
    val isKiln: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    /** true si se mostró desde el JSON local sin bajar de Firebase. */
    val fromLocal: Boolean = false
)

class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val firebaseManager = FirebaseRealtimeManager()
    private val authRepository = AuthRepository(application)
    private val recordingStore = LoggerRecordingStore(application)
    private val kilnFiringStore = KilnFiringStore(application)
    private val kilnDao = AppDatabase.getDatabase(application).kilnDao()

    private val _activities = MutableStateFlow<List<ActivityUiItem>>(emptyList())
    val activities: StateFlow<List<ActivityUiItem>> = _activities.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _curveDetail = MutableStateFlow<ActivityCurveDetail?>(null)
    val curveDetail: StateFlow<ActivityCurveDetail?> = _curveDetail.asStateFlow()

    private val dayFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val monthFmt = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            val uid = currentUserId()

            // Sube cocciones locales pendientes (sin UI).
            withContext(Dispatchers.IO) {
                KilnFiringCloudSync.syncPending(
                    context = getApplication(),
                    userId = uid,
                    firebase = firebaseManager
                )
            }

            val nameByMac = withContext(Dispatchers.IO) {
                kilnDao.getAllKilns().first().associate { kiln ->
                    normalizeMac(kiln.macAddress) to kiln.name.trim()
                }.filterValues { it.isNotBlank() }
            }

            val localFallback = withContext(Dispatchers.IO) {
                val loggerItems = recordingStore.loadAll()
                    .filter { it.endedAtMs != null && it.samples.isNotEmpty() }
                    .map { it.toActivityUiItem(nameByMac) }
                val kilnItems = kilnFiringStore.loadAll()
                    .filter { it.samples.isNotEmpty() || it.stages.isNotEmpty() }
                    .map { it.toActivityUiItem(nameByMac) }
                (loggerItems + kilnItems).sortedByDescending { it.timestamp }
            }

            if (uid == null) {
                _activities.value = localFallback
                _error.value = if (localFallback.isEmpty()) {
                    "Iniciá sesión para ver la actividad"
                } else {
                    null
                }
                _isLoading.value = false
                return@launch
            }

            firebaseManager.getActivities(uid)
                .onSuccess { list ->
                    // Ocultar cocciones aún activas (ESP escribe activity al START).
                    val ended = list.filter { !it.active }
                    val fromFb = ended.map { it.toUiItem(nameByMac) }
                    val fbIds = fromFb.map { it.id }.toSet()
                    val merged = fromFb + localFallback.filter { it.id !in fbIds }
                    _activities.value = merged.sortedByDescending { it.timestamp }
                }
                .onFailure { e ->
                    _activities.value = localFallback
                    _error.value = if (localFallback.isEmpty()) {
                        e.message ?: "No se pudo cargar la actividad"
                    } else {
                        null
                    }
                }
            _isLoading.value = false
        }
    }

    /**
     * Elimina el resumen de Actividad, la curva en Firebase (`recordings/`)
     * y la copia local. Irreversible.
     */
    fun deleteActivity(item: ActivityUiItem) {
        viewModelScope.launch {
            val uid = currentUserId()
            val recordingId = item.recordingId?.takeIf { it.isNotBlank() } ?: item.id
            withContext(Dispatchers.IO) {
                recordingStore.delete(recordingId)
                kilnFiringStore.delete(recordingId)
                if (uid != null) {
                    firebaseManager.deleteActivity(uid, item.id)
                    if (item.deviceMac.isNotBlank() &&
                        (item.isDataLogger || item.isKiln || !item.recordingId.isNullOrBlank())
                    ) {
                        firebaseManager.deleteLoggerRecording(
                            userId = uid,
                            mac = item.deviceMac,
                            recordingId = recordingId
                        )
                    }
                }
            }
            _activities.value = _activities.value.filterNot { it.id == item.id }
            if (_curveDetail.value != null) closeCurveDetail()
        }
    }

    fun openActivity(item: ActivityUiItem) {
        if (!item.canOpenCurve) return
        val recordingId = item.recordingId ?: return
        val title = item.programName
        val subtitle = "${item.kilnName} · ${item.executionDate}"
        val isKiln = item.isKiln
        _curveDetail.value = ActivityCurveDetail(
            title = title,
            subtitle = subtitle,
            isKiln = isKiln,
            isLoading = true
        )
        viewModelScope.launch {
            if (isKiln) {
                openKilnCurve(item, recordingId, title, subtitle)
            } else {
                openLoggerCurve(item, recordingId, title, subtitle)
            }
        }
    }

    private suspend fun openKilnCurve(
        item: ActivityUiItem,
        recordingId: String,
        title: String,
        subtitle: String
    ) {
        val local = withContext(Dispatchers.IO) {
            kilnFiringStore.getById(recordingId)
        }
        if (local != null && (local.samples.isNotEmpty() || local.stages.isNotEmpty())) {
            _curveDetail.value = ActivityCurveDetail(
                title = title,
                subtitle = subtitle,
                samples = local.samples,
                stages = local.stages,
                isKiln = true,
                isLoading = false,
                fromLocal = true
            )
            return
        }

        val uid = currentUserId()
        if (uid == null) {
            _curveDetail.value = ActivityCurveDetail(
                title = title,
                subtitle = subtitle,
                isKiln = true,
                isLoading = false,
                error = "Sin datos locales ni sesión para bajar de Firebase."
            )
            return
        }

        firebaseManager.getRecordingPayload(uid, item.deviceMac, recordingId)
            .onSuccess { payload ->
                val samples = payload.samples.mapNotNull { map ->
                    val elapsed = (map["elapsedMin"] as? Number)?.toFloat() ?: return@mapNotNull null
                    val temp = (map["temperatureC"] as? Number)?.toFloat() ?: return@mapNotNull null
                    TemperatureSample(elapsed, temp)
                }
                val stages = payload.stages.mapIndexedNotNull { index, map ->
                    val target = (map["targetTemp"] as? Number)?.toFloat() ?: return@mapIndexedNotNull null
                    ProgramStage(
                        stageNumber = (map["stageNumber"] as? Number)?.toInt() ?: (index + 1),
                        targetTemp = target,
                        rampRate = (map["rampRate"] as? Number)?.toFloat() ?: 0f,
                        maintenanceMinutes = (map["maintenanceMinutes"] as? Number)?.toInt() ?: 0,
                        durationMinutes = (map["durationMinutes"] as? Number)?.toInt() ?: 0
                    )
                }
                if (samples.isNotEmpty() || stages.isNotEmpty()) {
                    val started = (payload.meta["startedAtMs"] as? Number)?.toLong()
                        ?: item.timestamp
                    val ended = (payload.meta["endedAtMs"] as? Number)?.toLong()
                        ?: System.currentTimeMillis()
                    val name = (payload.meta["name"] as? String)?.takeIf { it.isNotBlank() }
                        ?: item.programName
                    withContext(Dispatchers.IO) {
                        kilnFiringStore.upsert(
                            KilnFiringSession(
                                id = recordingId,
                                deviceMac = item.deviceMac,
                                programName = name,
                                startedAtMs = started,
                                endedAtMs = ended,
                                samples = samples,
                                stages = stages
                            )
                        )
                    }
                }
                _curveDetail.value = ActivityCurveDetail(
                    title = title,
                    subtitle = subtitle,
                    samples = samples,
                    stages = stages,
                    isKiln = true,
                    isLoading = false,
                    fromLocal = false,
                    error = if (samples.isEmpty() && stages.isEmpty()) {
                        "Esta cocción no tiene curva guardada."
                    } else {
                        null
                    }
                )
            }
            .onFailure { e ->
                _curveDetail.value = ActivityCurveDetail(
                    title = title,
                    subtitle = subtitle,
                    isKiln = true,
                    isLoading = false,
                    error = e.message ?: "No se pudo cargar la curva"
                )
            }
    }

    private suspend fun openLoggerCurve(
        item: ActivityUiItem,
        recordingId: String,
        title: String,
        subtitle: String
    ) {
        val local = withContext(Dispatchers.IO) {
            recordingStore.getById(recordingId)
        }
        if (local != null && local.samples.isNotEmpty()) {
            _curveDetail.value = ActivityCurveDetail(
                title = title,
                subtitle = subtitle,
                samples = local.samples.map {
                    TemperatureSample(it.elapsedMin, it.temperatureC)
                },
                isKiln = false,
                isLoading = false,
                fromLocal = true
            )
            return
        }

        val uid = currentUserId()
        if (uid == null) {
            _curveDetail.value = ActivityCurveDetail(
                title = title,
                subtitle = subtitle,
                isLoading = false,
                error = "Sin datos locales ni sesión para bajar de Firebase."
            )
            return
        }

        firebaseManager.getLoggerRecording(uid, item.deviceMac, recordingId)
            .onSuccess { (meta, rawSamples) ->
                val loggerSamples = rawSamples.mapNotNull { map ->
                    val elapsed = (map["elapsedMin"] as? Number)?.toFloat()
                        ?: return@mapNotNull null
                    val temp = (map["temperatureC"] as? Number)?.toFloat()
                        ?: return@mapNotNull null
                    val epoch = (map["epochMs"] as? Number)?.toLong() ?: 0L
                    LoggerRecordingSample(elapsed, temp, epoch)
                }

                if (loggerSamples.isNotEmpty()) {
                    val started = (meta["startedAtMs"] as? Number)?.toLong()
                        ?: item.timestamp
                    val ended = (meta["endedAtMs"] as? Number)?.toLong()
                        ?: System.currentTimeMillis()
                    val name = (meta["name"] as? String)?.takeIf { it.isNotBlank() }
                        ?: item.programName
                    val comments = (meta["comments"] as? String).orEmpty()
                    withContext(Dispatchers.IO) {
                        recordingStore.upsert(
                            LoggerRecordingSession(
                                id = recordingId,
                                deviceMac = item.deviceMac,
                                name = name,
                                comments = comments,
                                startedAtMs = started,
                                endedAtMs = ended,
                                samples = loggerSamples
                            )
                        )
                    }
                }

                _curveDetail.value = ActivityCurveDetail(
                    title = title,
                    subtitle = subtitle,
                    samples = loggerSamples.map {
                        TemperatureSample(it.elapsedMin, it.temperatureC)
                    },
                    isKiln = false,
                    isLoading = false,
                    fromLocal = false,
                    error = if (loggerSamples.isEmpty()) {
                        "Esta grabación no tiene muestras."
                    } else {
                        null
                    }
                )
            }
            .onFailure { e ->
                _curveDetail.value = ActivityCurveDetail(
                    title = title,
                    subtitle = subtitle,
                    isLoading = false,
                    error = e.message ?: "No se pudo cargar la curva"
                )
            }
    }

    fun closeCurveDetail() {
        _curveDetail.value = null
    }

    private fun currentUserId(): String? {
        val email = authRepository.getCurrentUserEmail()
        val uid = UserIdUtils.getCurrentUserId(email)
        return uid.takeIf {
            it.isNotBlank() && it != "anonymous_user" && it != "test_user_123"
        }
    }

    private fun normalizeMac(mac: String): String =
        mac.replace(":", "").replace("-", "").uppercase()

    /** Prefiere el nombre guardado en la app; evita mostrar MAC crudo. */
    private fun resolveDeviceLabel(
        deviceMac: String,
        deviceNameFromCloud: String,
        nameByMac: Map<String, String>,
        fallback: String
    ): String {
        val clean = normalizeMac(deviceMac)
        nameByMac[clean]?.takeIf { it.isNotBlank() }?.let { return it }
        val fromCloud = deviceNameFromCloud.trim()
        if (fromCloud.isNotBlank() && !looksLikeMac(fromCloud)) return fromCloud
        return fallback
    }

    private fun looksLikeMac(value: String): Boolean {
        val clean = value.replace(":", "").replace("-", "").replace("…", "").trim()
        return clean.length in 6..16 && clean.all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }
    }

    private fun LoggerRecordingSession.toActivityUiItem(
        nameByMac: Map<String, String>
    ): ActivityUiItem {
        val whenMs = endedAtMs ?: startedAtMs
        val date = Date(whenMs)
        val maxTemp = samples.maxOfOrNull { it.temperatureC }?.toInt() ?: 0
        val durationMin = samples.maxOfOrNull { it.elapsedMin }?.toInt() ?: 0
        val deviceLabel = resolveDeviceLabel(
            deviceMac = deviceMac,
            deviceNameFromCloud = "",
            nameByMac = nameByMac,
            fallback = "DataLogger"
        )
        return ActivityUiItem(
            id = id,
            type = "DATALOGGER",
            deviceMac = deviceMac,
            recordingId = id,
            kilnName = deviceLabel,
            kilnBrand = "DataLogger",
            kilnVolume = if (samples.isNotEmpty()) "${samples.size} muestras" else "",
            executionDate = dayFmt.format(date),
            programName = name.ifBlank { "Grabación" },
            maxTemperature = if (maxTemp > 0) maxTemp.toString() else "—",
            duration = formatDuration(durationMin),
            timestamp = whenMs,
            monthLabel = monthFmt.format(date).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString()
            },
            complete = true
        )
    }

    private fun KilnFiringSession.toActivityUiItem(
        nameByMac: Map<String, String>
    ): ActivityUiItem {
        val date = Date(endedAtMs.takeIf { it > 0 } ?: startedAtMs)
        val maxTemp = samples.maxOfOrNull { it.temperatureC }?.toInt() ?: 0
        val durationMin = samples.maxOfOrNull { it.elapsedMin }?.toInt()
            ?: ((endedAtMs - startedAtMs) / 60_000L).toInt().coerceAtLeast(0)
        val deviceLabel = resolveDeviceLabel(
            deviceMac = deviceMac,
            deviceNameFromCloud = deviceName,
            nameByMac = nameByMac,
            fallback = "Horno"
        )
        return ActivityUiItem(
            id = id,
            type = "KILN",
            deviceMac = deviceMac,
            recordingId = id,
            kilnName = deviceLabel,
            kilnBrand = "Horno",
            kilnVolume = if (samples.isNotEmpty()) "${samples.size} muestras" else "",
            executionDate = dayFmt.format(date),
            programName = programName.ifBlank { "Cocción" },
            maxTemperature = if (maxTemp > 0) maxTemp.toString() else "—",
            duration = formatDuration(durationMin),
            timestamp = endedAtMs.takeIf { it > 0 } ?: startedAtMs,
            monthLabel = monthFmt.format(date).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString()
            },
            complete = complete
        )
    }

    private fun ActivityRecord.toUiItem(nameByMac: Map<String, String>): ActivityUiItem {
        val whenMs = endedAtMs.takeIf { it > 0 } ?: startedAtMs
        val date = if (whenMs > 0) Date(whenMs) else Date()
        val typeLabel = if (isDataLogger) "DataLogger" else "Horno"
        val displayDevice = resolveDeviceLabel(
            deviceMac = deviceMac,
            deviceNameFromCloud = deviceName,
            nameByMac = nameByMac,
            fallback = typeLabel
        )
        return ActivityUiItem(
            id = id,
            type = type,
            deviceMac = deviceMac,
            recordingId = recordingId
                ?: id.takeIf { isDataLogger || sampleCount > 0 },
            kilnName = displayDevice,
            kilnBrand = typeLabel,
            kilnVolume = when {
                sampleCount > 0 -> "$sampleCount muestras"
                else -> ""
            },
            executionDate = dayFmt.format(date),
            programName = name,
            maxTemperature = if (maxTemperatureC > 0) maxTemperatureC.toString() else "—",
            duration = formatDuration(durationMin),
            timestamp = whenMs,
            monthLabel = monthFmt.format(date).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString()
            },
            complete = complete
        )
    }

    private fun formatDuration(minutes: Int): String {
        if (minutes <= 0) return "—"
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h <= 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${String.format("%02d", m)}m"
        }
    }
}
