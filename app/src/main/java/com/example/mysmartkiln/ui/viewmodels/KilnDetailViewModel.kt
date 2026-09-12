package com.example.mysmartkiln.ui.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager
import com.example.mysmartkiln.data.local.KilnApInfoDto
import com.example.mysmartkiln.data.local.KilnDataSource
import com.example.mysmartkiln.data.local.KilnHttpClient
import com.example.mysmartkiln.data.local.KilnProgramUploader
import com.example.mysmartkiln.data.local.KilnStatusDto
import com.example.mysmartkiln.data.repository.KilnRepository
import com.example.mysmartkiln.data.repository.ProgramRepository
import com.example.mysmartkiln.utils.KilnNetworkConnector
import com.example.mysmartkiln.utils.ProgramStatusUtils
import com.example.mysmartkiln.utils.UserIdUtils
import com.example.mysmartkiln.utils.WifiUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class KilnStatus(
    val isConnected: Boolean = false,
    /** Tras perder Firebase mientras horneaba: el ESP puede estar reiniciando. */
    val isReconnecting: Boolean = false,
    val currentTemperature: Float = 0f,
    val targetTemperature: Float = 0f,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val espStatus: String = "",
    val currentStage: Int = 0,
    val totalStages: Int = 0,
    val elapsedTime: String = "0h 00m",
    val remainingTime: String = "0h 00m",
    val totalTime: String = "0h 00m",
    val rampRate: Float = 0f,
    val maintenanceTime: String = "0m",
    val elapsedMinutes: Int = 0
)

data class KilnProgram(
    val name: String = "",
    val startDate: String = "",
    val startTime: String = "",
    val duration: String = "",
    val remaining: String = "",
    val elapsed: String = "",
    val stage: String = "",
    val ramp: String = "",
    val maintenance: String = "",
    val stages: List<ProgramStage> = emptyList()
)

data class ProgramStage(
    val stageNumber: Int,
    val targetTemp: Float,
    val rampRate: Float,
    val maintenanceMinutes: Int,
    val durationMinutes: Int,
    val isCompleted: Boolean = false
)

/** Muestra de temperatura real para la curva (tiempo transcurrido desde el inicio). */
data class TemperatureSample(
    val elapsedMin: Float,
    val temperatureC: Float
)

/** Diálogo «espere X segundos» tras soltar el AP del horno. */
data class ApReconfigWaitState(
    val remainingSeconds: Int,
    val actionDescription: String
)

enum class LocalKilnAction(val description: String) {
    START("iniciar la horneada"),
    PAUSE("pausar el programa"),
    STOP("detener el programa"),
    RESUME("reanudar la horneada"),
    UPDATE_PROGRAM("actualizar el programa");

    companion object {
        fun fromCommand(cmd: String): LocalKilnAction = when (cmd) {
            "START" -> START
            "PAUSE" -> PAUSE
            "STOP" -> STOP
            "RESUME" -> RESUME
            else -> START
        }
    }
}

class KilnDetailViewModel(
    private val programRepository: ProgramRepository,
    private val kilnRepository: KilnRepository,
    private val appContext: Context
) : ViewModel() {

    private val _kilnStatus = MutableStateFlow(KilnStatus())
    val kilnStatus: StateFlow<KilnStatus> = _kilnStatus.asStateFlow()

    private val _kilnProgram = MutableStateFlow(KilnProgram())
    val kilnProgram: StateFlow<KilnProgram> = _kilnProgram.asStateFlow()

    private val _temperatureHistory = MutableStateFlow<List<TemperatureSample>>(emptyList())
    val temperatureHistory: StateFlow<List<TemperatureSample>> = _temperatureHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _dataSource = MutableStateFlow(KilnDataSource.DISCONNECTED)
    val dataSource: StateFlow<KilnDataSource> = _dataSource.asStateFlow()

    private val _apInfo = MutableStateFlow<KilnApInfoDto?>(null)
    val apInfo: StateFlow<KilnApInfoDto?> = _apInfo.asStateFlow()

    private val _isVerifyingConnection = MutableStateFlow(false)
    val isVerifyingConnection: StateFlow<Boolean> = _isVerifyingConnection.asStateFlow()

    private val _connectionMessage = MutableStateFlow("")
    val connectionMessage: StateFlow<String> = _connectionMessage.asStateFlow()

    /** HTTP al horno respondiendo (control Iniciar/Pausa/Stop y envío de perfil). */
    private val _canControlLocally = MutableStateFlow(false)
    val canControlLocally: StateFlow<Boolean> = _canControlLocally.asStateFlow()

    private val _isUploadingProgram = MutableStateFlow(false)
    val isUploadingProgram: StateFlow<Boolean> = _isUploadingProgram.asStateFlow()

    /** Mientras Android está abriendo el diálogo nativo "¿Usar SmartKiln-XXXX?". */
    private val _isConnectingToAp = MutableStateFlow(false)
    val isConnectingToAp: StateFlow<Boolean> = _isConnectingToAp.asStateFlow()

    /**
     * Flag que cubre TODO el ciclo de envío de un comando local
     * (Iniciar / Pausa / Stop / cargar programa): conexión al AP, hand-off TLS,
     * envío HTTP y respuesta. Lo usa la UI para mostrar el diálogo modal con
     * el círculo girando ("Enviando comando al horno…").
     */
    private val _isSendingCommand = MutableStateFlow(false)
    val isSendingCommand: StateFlow<Boolean> = _isSendingCommand.asStateFlow()

    /** Texto principal que se muestra dentro del diálogo modal. */
    private val _sendingCommandPhase = MutableStateFlow("")
    val sendingCommandPhase: StateFlow<String> = _sendingCommandPhase.asStateFlow()

    /** Texto secundario (línea pequeña) del diálogo modal. */
    private val _sendingCommandDetail = MutableStateFlow("")
    val sendingCommandDetail: StateFlow<String> = _sendingCommandDetail.asStateFlow()

    /**
     * Cuando ensureLocalConnection() no logra auto-conectar (API <29 o el
     * usuario rechazó), exponemos esta señal para que la pantalla muestre el
     * diálogo manual con instrucciones de Settings.
     */
    private val _shouldShowManualWifiDialog = MutableStateFlow(false)
    val shouldShowManualWifiDialog: StateFlow<Boolean> = _shouldShowManualWifiDialog.asStateFlow()

    private val _apReconfigWaitDialog = MutableStateFlow<ApReconfigWaitState?>(null)
    val apReconfigWaitDialog: StateFlow<ApReconfigWaitState?> = _apReconfigWaitDialog.asStateFlow()

    /**
     * Verdadero cuando detectamos que el horno reporta status="INTERRUMPIDO".
     * El firmware lo deja así si el ESP se reseteó mientras estaba CALENTANDO
     * o PAUSADO. La UI muestra un diálogo "Reanudar / Cancelar" no
     * descartable para que el usuario decida qué hacer.
     */
    private val _showInterruptedDialog = MutableStateFlow(false)
    val showInterruptedDialog: StateFlow<Boolean> = _showInterruptedDialog.asStateFlow()

    private var pendingProgramUploadId: String? = null

    private val firebaseManager = FirebaseRealtimeManager()
    private val kilnHttpClient = KilnHttpClient(appContext)
    private var currentMacAddress: String = ""
    private var firebaseJob: Job? = null
    private var firebaseSyncJob: Job? = null
    private var firebaseConnectionJob: Job? = null
    private var firebaseProfileJob: Job? = null
    private var firebaseWatchdogJob: Job? = null
    private var localPollJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
  /** userId de la ruta Firebase donde el ESP publica (el guardado en el horno). */
    private var kilnFirebaseUserId: String? = null
    private var firebaseListenerUserId: String? = null

    /** Tras corte de luz / pérdida de RTDB mientras había cocción activa. */
    private var awaitingKilnReconnect = false
    private var reconnectDeadlineMs = 0L

    /** Inicio real de la cocción (reloj del teléfono) para fecha/hora en la pestaña Programa. */
    private var programStartedAtWallMs: Long? = null
    private var pausedElapsedMin: Int? = null

    /** ID Room del programa actualmente cargado en la vista de detalle. */
    private val _loadedProgramId = MutableStateFlow<String?>(null)
    val loadedProgramId: StateFlow<String?> = _loadedProgramId.asStateFlow()

    /**
     * Tras elegir un programa en la app, el ESP/Firebase pueden seguir reportando
     * el perfil anterior unos segundos. Mientras tanto, no pisar nombre ni tiempos locales.
     */
    private var localProgramSelectedAtMs = 0L

    /** updatedAt del programa la última vez que se subió / sincronizó con el ESP. */
    private var lastSyncedProgramUpdatedAt = 0L

    private var lastFirebaseHealMs = 0L
    private var firebaseHealInProgress = false
    private var firebaseRtdbWasConnected = true
    /** Último snapshot con timestamp ESP fresco (< CONNECTION_TIMEOUT_MS). */
    private var lastFirebaseLiveDataMs = 0L

    private fun authFirebaseUserId(): String {
        val email = FirebaseAuth.getInstance().currentUser?.email
        return if (email != null) UserIdUtils.getCurrentUserId(email) else "test_user_123"
    }

    private fun resolveFirebaseUserId(): String? {
        val fromKiln = kilnFirebaseUserId?.takeIf { it.isNotBlank() }
        return fromKiln ?: authFirebaseUserId().takeIf { it.isNotBlank() }
    }

    companion object {
        private const val TAG = "KilnDetailViewModel"
        private const val CONNECTION_TIMEOUT_MS = 15_000L
        private const val LOCAL_POLL_INTERVAL_MS = 1_500L
        // Perfil B (equilibrado). Revertir perfil A: PROBE=6, HANDOFF_FAILURES=2, COOLDOWN=0
        private const val LOCAL_PROBE_ATTEMPTS = 3
        private const val LOCAL_PROBE_DELAY_MS = 350L
        /** Handoff a Firebase tras N pings fallidos (perfil A: 2). */
        private const val LOCAL_HANDOFF_PING_FAILURES = 1
        /**
         * Tras soltar el AP del horno: ventana en la que el ESP apaga/restaura
         * SoftAP (~12–14 s perfil B). Bloquea connect() y muestra aviso al usuario.
         */
        private const val KILN_AP_RECONFIG_WAIT_MS = 15_000L
        /** Poll RTDB en modo remoto por si el listener no dispara (p. ej. tras reinicio del ESP). */
        private const val FIREBASE_STATUS_POLL_MS = 5_000L
        private const val FIREBASE_FAST_POLL_MS = 2_000L
        /** Poll agresivo mientras Remoto está gris (paso 3). */
        private const val FIREBASE_DISCONNECTED_POLL_MS = 1_000L
        /** Sin datos frescos del ESP → forzar heal (paso 5). */
        private const val FIREBASE_STALE_DATA_MS = 15_000L
        private const val FIREBASE_WATCHDOG_INTERVAL_MS = 5_000L
        /** Mínimo entre heals forzados (evita tormenta si .info/connected parpadea). */
        private const val FIREBASE_HEAL_DEBOUNCE_MS = 5_000L
        /** Breve pausa tras goOffline/goOnline antes de reenganchar el listener. */
        private const val FIREBASE_RECONNECT_SETTLE_MS = 400L
        /** Tiempo para que el ESP vuelva a WiFi + Firebase tras un corte. */
        private const val RECONNECT_GRACE_MS = 120_000L
        private val ACTIVE_KILN_STATUSES = setOf(
            "CALENTANDO", "PAUSADO", "FINALIZADO"
        )
        private val RECONNECT_TRIGGER_STATUSES = setOf(
            "CALENTANDO", "PAUSADO", "INTERRUMPIDO"
        )
    }

    override fun onCleared() {
        stopMonitoring()
        KilnNetworkConnector.release(appContext)
        super.onCleared()
    }

    fun loadKilnData(macAddress: String) {
        if (currentMacAddress != macAddress) {
            stopMonitoring()
            _dataSource.value = KilnDataSource.DISCONNECTED
        }
        currentMacAddress = macAddress
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val kiln = kilnRepository.getKilnByMacAddress(macAddress)
                kilnFirebaseUserId = kiln?.userId?.takeIf { it.isNotBlank() }
                val authUid = authFirebaseUserId()
                if (kilnFirebaseUserId != null && kilnFirebaseUserId != authUid) {
                    Log.w(
                        TAG,
                        "Firebase userId del horno ($kilnFirebaseUserId) ≠ sesión ($authUid). " +
                            "Escuchando la ruta del horno (donde publica el ESP)."
                    )
                }
                checkRunningProgram()
                syncFirebaseStatusOnce()
                syncProfileStateOnce()
                // Tras sync, ya puede haber programName sin stages → arcos del Status.
                ensureProgramStagesLoaded()
                detectAndStartMonitoring()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading kiln data: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Re-evalúa WiFi del horno al volver a la pantalla (p. ej. tras conectar en Ajustes). */
    fun refreshConnection() {
        viewModelScope.launch {
            syncFirebaseStatusOnce()
            syncProfileStateOnce()
            detectAndStartMonitoring()
            if (_dataSource.value == KilnDataSource.FIREBASE && !_kilnStatus.value.isConnected) {
                healFirebaseConnection("resume-refresh")
            }
        }
    }

    fun clearConnectionMessage() {
        _connectionMessage.value = ""
    }

    fun tryConnectLocal() {
        viewModelScope.launch {
            _isVerifyingConnection.value = true
            _connectionMessage.value = ""
            try {
                if (kilnHttpClient.ping()) {
                    startLocalMonitoring()
                    kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
                    refreshApInfo()
                    _canControlLocally.value = true
                    _connectionMessage.value = "Conexión local establecida."
                } else {
                    _canControlLocally.value = false
                    _connectionMessage.value =
                        "No se pudo contactar al horno en 192.168.4.1. ¿Estás en su WiFi?"
                    _dataSource.value = KilnDataSource.DISCONNECTED
                }
            } finally {
                _isVerifyingConnection.value = false
            }
        }
    }

    fun refreshApInfo() {
        viewModelScope.launch {
            if (!kilnHttpClient.ping()) return@launch
            kilnHttpClient.getApInfo()
                .onSuccess { _apInfo.value = it }
                .onFailure { Log.w(TAG, "getApInfo: ${it.message}") }
        }
    }

    private suspend fun detectAndStartMonitoring() {
        // Mantener el listener Firebase enganchado siempre que conozcamos el horno.
        // Es idempotente: si ya está activo no se reinicia. Esto evita que la
        // pantalla de detalle quede "congelada" mientras el AP del horno se cae
        // y el teléfono vuelve al WiFi de casa.
        if (currentMacAddress.isNotEmpty()) {
            startFirebaseMonitoring(currentMacAddress)
        }
        tryEstablishLocalFirst()
    }

    /**
     * Intenta HTTP local antes que Firebase para evitar parpadeo "Sin señal" al entrar en el AP.
     */
    private suspend fun tryEstablishLocalFirst(): Boolean {
        repeat(LOCAL_PROBE_ATTEMPTS) {
            if (kilnHttpClient.ping()) {
                startLocalMonitoring()
                kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
                refreshApInfo()
                return true
            }
            delay(LOCAL_PROBE_DELAY_MS)
        }
        return false
    }

    private fun stopMonitoring() {
        firebaseJob?.cancel()
        firebaseJob = null
        firebaseListenerUserId = null
        firebaseConnectionJob?.cancel()
        firebaseConnectionJob = null
        firebaseProfileJob?.cancel()
        firebaseProfileJob = null
        firebaseWatchdogJob?.cancel()
        firebaseWatchdogJob = null
        stopNetworkMonitoring()
        firebaseSyncJob?.cancel()
        firebaseSyncJob = null
        localPollJob?.cancel()
        localPollJob = null
        awaitingKilnReconnect = false
        reconnectDeadlineMs = 0L
        programStartedAtWallMs = null
        pausedElapsedMin = null
        _loadedProgramId.value = null
        lastSyncedProgramUpdatedAt = 0L
        lastFirebaseLiveDataMs = 0L
        clearTemperatureHistory()
    }

    private fun clearTemperatureHistory() {
        _temperatureHistory.value = emptyList()
    }

    private fun recordTemperatureSample(temperatureC: Float) {
        if (temperatureC <= 0f) return
        val status = _kilnStatus.value
        val active = status.isRunning || status.isPaused ||
            status.espStatus in ACTIVE_KILN_STATUSES
        if (!active) return

        val elapsedMin = when {
            status.espStatus == "PAUSADO" || status.espStatus == "INTERRUMPIDO" ->
                (pausedElapsedMin ?: status.elapsedMinutes).toFloat()
            programStartedAtWallMs != null ->
                ((System.currentTimeMillis() - programStartedAtWallMs!!) / 60_000f)
                    .coerceAtLeast(0f)
            status.elapsedMinutes > 0 -> status.elapsedMinutes.toFloat()
            else -> return
        }

        val history = _temperatureHistory.value
        val last = history.lastOrNull()
        val shouldAdd = last == null ||
            kotlin.math.abs(last.elapsedMin - elapsedMin) >= 0.2f ||
            kotlin.math.abs(last.temperatureC - temperatureC) >= 1.5f

        if (shouldAdd) {
            _temperatureHistory.value = (history + TemperatureSample(elapsedMin, temperatureC))
                .takeLast(400)
        }
    }

    private fun ensureProgramStartWallClock() {
        if (programStartedAtWallMs == null) {
            programStartedAtWallMs = System.currentTimeMillis()
        }
    }

    private fun clearProgramStartWallClock() {
        programStartedAtWallMs = null
        pausedElapsedMin = null
        clearTemperatureHistory()
    }

    private fun formatStartDate(wallMs: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(wallMs))

    private fun formatStartTime(wallMs: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(wallMs))

    private fun computeTotalDurationMin(
        dto: KilnStatusDto? = null,
        firebaseData: Map<String, Any?>? = null
    ): Int {
        dto?.programDurationMin?.takeIf { it > 0 }?.let { return it }
        (firebaseData?.get("programTotalDuration") as? Number)?.toInt()
            ?.takeIf { it > 0 }?.let { return it }
        val fromStages = _kilnProgram.value.stages.sumOf { it.durationMinutes }
        if (fromStages > 0) return fromStages
        return _kilnStatus.value.totalTime
            .let { parseFormattedDurationMinutes(it) }
    }

    private fun parseFormattedDurationMinutes(formatted: String): Int {
        val match = Regex("""(\d+)h\s*(\d+)(?:min|m)\b""").find(formatted.trim()) ?: return 0
        val hours = match.groupValues[1].toIntOrNull() ?: 0
        val mins = match.groupValues[2].toIntOrNull() ?: 0
        return hours * 60 + mins
    }

    /**
     * El horno o Firebase aún no reflejan el programa recién elegido en Room.
     */
    private fun shouldPreferLocalProgramMetadata(
        remoteProgramName: String,
        statusForUi: String
    ): Boolean {
        val localName = _kilnProgram.value.name.trim()
        if (localName.isEmpty() || _kilnProgram.value.stages.isEmpty()) {
            return false
        }
        if (statusForUi in ACTIVE_KILN_STATUSES) {
            return false
        }
        val remote = remoteProgramName.trim()
        if (remote.isNotEmpty() && !remote.equals(localName, ignoreCase = true)) {
            return true
        }
        return localProgramSelectedAtMs > 0L &&
            System.currentTimeMillis() - localProgramSelectedAtMs < 45_000L
    }

    private fun noteRemoteProgramNameSeen(remoteProgramName: String) {
        val remote = remoteProgramName.trim()
        val local = _kilnProgram.value.name.trim()
        if (remote.isNotEmpty() && remote.equals(local, ignoreCase = true)) {
            localProgramSelectedAtMs = 0L
        }
    }

    /** Refresca nombre y tiempos desde las etapas cargadas en Room (sin ESP). */
    private fun refreshProgramDisplayFromLocalStages() {
        val stages = _kilnProgram.value.stages
        if (stages.isEmpty()) return
        val totalMinutes = stages.sumOf { it.durationMinutes }
        val maxTemp = stages.maxOfOrNull { it.targetTemp }
            ?: _kilnStatus.value.targetTemperature
        syncKilnProgramFields(
            programName = _kilnProgram.value.name,
            durationMin = totalMinutes,
            elapsedMin = 0,
            remainingMin = totalMinutes,
            currentStage = 0,
            totalStages = stages.size
        )
        _kilnStatus.value = _kilnStatus.value.copy(
            targetTemperature = maxTemp,
            isRunning = false,
            isPaused = false
        )
    }

    private fun syncKilnProgramFields(
        programName: String? = null,
        durationMin: Int,
        elapsedMin: Int,
        remainingMin: Int,
        currentStage: Int,
        totalStages: Int
    ) {
        val durationStr = formatMinutes(durationMin.coerceAtLeast(0))
        val elapsedStr = formatMinutes(elapsedMin.coerceAtLeast(0))
        val remainingStr = formatMinutes(remainingMin.coerceAtLeast(0))
        val stageLabel = when {
            totalStages > 0 -> "${currentStage.coerceAtLeast(0)}/$totalStages"
            else -> _kilnProgram.value.stage
        }
        val startMs = programStartedAtWallMs
        val startDate = startMs?.let { formatStartDate(it) }.orEmpty()
        val startTime = startMs?.let { formatStartTime(it) }.orEmpty()

        _kilnProgram.value = _kilnProgram.value.copy(
            name = programName?.takeIf { it.isNotEmpty() } ?: _kilnProgram.value.name,
            startDate = startDate,
            startTime = startTime,
            duration = durationStr,
            elapsed = elapsedStr,
            remaining = remainingStr,
            stage = stageLabel
        )
        _kilnStatus.value = _kilnStatus.value.copy(
            totalTime = durationStr,
            elapsedTime = elapsedStr,
            remainingTime = remainingStr,
            elapsedMinutes = elapsedMin.coerceAtLeast(0),
            currentStage = currentStage.coerceAtLeast(0),
            totalStages = totalStages.coerceAtLeast(_kilnStatus.value.totalStages)
        )
    }

    private fun computeElapsedFromFirebaseEsp(
        data: Map<String, Any?>,
        statusForUi: String
    ): Int? {
        val startSec = (data["programStartTime"] as? Number)?.toLong()?.takeIf { it > 0 }
            ?: return null
        return when (statusForUi) {
            "PAUSADO", "INTERRUMPIDO" -> {
                val pauseSec = (data["programPauseTime"] as? Number)?.toLong()?.takeIf { it > 0 }
                if (pauseSec != null && pauseSec >= startSec) {
                    ((pauseSec - startSec) / 60).toInt()
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private fun applyProgramTimingFromFirebase(
        data: Map<String, Any?>,
        statusForUi: String,
        programName: String,
        isConnected: Boolean
    ) {
        if (!isConnected) return
        if (shouldPreferLocalProgramMetadata(programName, statusForUi)) {
            return
        }
        val name = programName.ifEmpty { _kilnProgram.value.name }
        if (name.isEmpty()) return
        noteRemoteProgramNameSeen(programName)

        when (statusForUi) {
            "IDLE", "DETENIDO", "FINALIZADO" -> clearProgramStartWallClock()
            "CALENTANDO" -> {
                if (programStartedAtWallMs == null) {
                    if (pausedElapsedMin != null) {
                        programStartedAtWallMs =
                            System.currentTimeMillis() - pausedElapsedMin!! * 60_000L
                        pausedElapsedMin = null
                    } else {
                        ensureProgramStartWallClock()
                    }
                }
            }
            "PAUSADO", "INTERRUMPIDO" -> {
                if (pausedElapsedMin == null && programStartedAtWallMs != null) {
                    pausedElapsedMin = ((System.currentTimeMillis() - programStartedAtWallMs!!) /
                        60_000L).toInt().coerceAtLeast(0)
                }
            }
        }

        val totalDur = computeTotalDurationMin(firebaseData = data)
        if (totalDur <= 0 && _kilnProgram.value.stages.isEmpty()) return

        val active = statusForUi in setOf("CALENTANDO", "PAUSADO", "INTERRUMPIDO")
        val elapsedFromEsp = computeElapsedFromFirebaseEsp(data, statusForUi)
        val elapsedMin = when {
            elapsedFromEsp != null -> elapsedFromEsp
            statusForUi == "PAUSADO" || statusForUi == "INTERRUMPIDO" ->
                pausedElapsedMin ?: _kilnStatus.value.elapsedMinutes
            active && programStartedAtWallMs != null ->
                ((System.currentTimeMillis() - programStartedAtWallMs!!) / 60_000L)
                    .toInt().coerceAtLeast(0)
            else -> 0
        }
        val remainingMin = if (totalDur > 0) {
            (totalDur - elapsedMin).coerceAtLeast(0)
        } else {
            0
        }
        val totalStages = _kilnStatus.value.totalStages
            .coerceAtLeast(_kilnProgram.value.stages.size)
            .coerceAtLeast(if (data["programName"] != null) 1 else 0)
        val currentStage = _kilnStatus.value.currentStage.coerceAtLeast(
            if (active && totalStages > 0) 1 else 0
        )

        syncKilnProgramFields(
            programName = name,
            durationMin = totalDur,
            elapsedMin = elapsedMin,
            remainingMin = remainingMin,
            currentStage = currentStage,
            totalStages = totalStages
        )
    }

    private fun updateProgramWallClockForStatus(status: String, elapsedFromDeviceMin: Int) {
        when (status) {
            "CALENTANDO" -> {
                if (programStartedAtWallMs == null) {
                    if (pausedElapsedMin != null) {
                        programStartedAtWallMs =
                            System.currentTimeMillis() - pausedElapsedMin!! * 60_000L
                        pausedElapsedMin = null
                    } else if (elapsedFromDeviceMin > 0) {
                        programStartedAtWallMs =
                            System.currentTimeMillis() - elapsedFromDeviceMin * 60_000L
                    } else {
                        ensureProgramStartWallClock()
                    }
                }
            }
            "PAUSADO", "INTERRUMPIDO" -> {
                pausedElapsedMin = elapsedFromDeviceMin.coerceAtLeast(0)
            }
            "IDLE", "DETENIDO", "FINALIZADO" -> clearProgramStartWallClock()
        }
    }

    private fun updateReconnectGrace(latestStatus: String, isFirebaseLive: Boolean) {
        val now = System.currentTimeMillis()
        if (isFirebaseLive) {
            awaitingKilnReconnect = false
            reconnectDeadlineMs = 0L
            return
        }
        val prev = _kilnStatus.value
        val wasActive = prev.isRunning ||
            prev.espStatus in RECONNECT_TRIGGER_STATUSES ||
            latestStatus in RECONNECT_TRIGGER_STATUSES ||
            _kilnProgram.value.name.isNotEmpty() && prev.espStatus == "CALENTANDO"
        if (wasActive && !awaitingKilnReconnect) {
            awaitingKilnReconnect = true
            reconnectDeadlineMs = now + RECONNECT_GRACE_MS
            Log.d(TAG, "Sin datos RTDB recientes — esperando reconexión del horno")
        }
        if (awaitingKilnReconnect && now >= reconnectDeadlineMs) {
            awaitingKilnReconnect = false
            Log.d(TAG, "Ventana de reconexión finalizada")
        }
    }

    private fun inReconnectGrace(): Boolean {
        return awaitingKilnReconnect && System.currentTimeMillis() < reconnectDeadlineMs
    }

    private fun resolveStatusForUi(statusRaw: String, isFirebaseLive: Boolean): String {
        val inGrace = inReconnectGrace()
        return when {
            isFirebaseLive -> statusRaw
            inGrace -> when (statusRaw) {
                "INTERRUMPIDO", "CALENTANDO", "PAUSADO" -> statusRaw
                "IDLE", "" -> {
                    val prev = _kilnStatus.value.espStatus
                    if (prev in RECONNECT_TRIGGER_STATUSES) prev else "RECONNECTING"
                }
                else -> statusRaw
            }
            statusRaw == "INTERRUMPIDO" -> "IDLE"
            statusRaw in setOf("CALENTANDO", "PAUSADO") -> "IDLE"
            else -> statusRaw.ifEmpty { "IDLE" }
        }
    }

    private suspend fun syncFirebaseStatusOnce() {
        val mac = currentMacAddress.ifEmpty { return }
        val uid = resolveFirebaseUserId() ?: return
        firebaseManager.getRealtimeData(uid, mac)
            .onSuccess { applyFirebaseSnapshot(it) }
            .onFailure {
                Log.w(TAG, "syncFirebaseStatusOnce: ${it.message}")
                healFirebaseConnection("poll-failed")
            }
    }

    private suspend fun syncProfileStateOnce() {
        val mac = currentMacAddress.ifEmpty { return }
        val uid = resolveFirebaseUserId() ?: return
        firebaseManager.getProfileState(uid, mac)
            .onSuccess { applyProfileStateSnapshot(it) }
            .onFailure { Log.w(TAG, "syncProfileStateOnce: ${it.message}") }
    }

    private fun startFirebaseStatusPolling() {
        firebaseSyncJob?.cancel()
        firebaseSyncJob = viewModelScope.launch {
            while (isActive && currentMacAddress.isNotEmpty()) {
                if (_dataSource.value != KilnDataSource.LOCAL_AP) {
                    syncFirebaseStatusOnce()
                }
                val pollMs = when {
                    awaitingKilnReconnect -> FIREBASE_FAST_POLL_MS
                    !_kilnStatus.value.isConnected -> FIREBASE_DISCONNECTED_POLL_MS
                    else -> FIREBASE_STATUS_POLL_MS
                }
                delay(pollMs)
            }
        }
    }

    private fun startLocalMonitoring() {
        // OJO: NO cancelamos firebaseJob. Lo dejamos corriendo para que cuando
        // el horno cierre su AP (tras START/PAUSE/STOP) la UI ya tenga datos
        // frescos de Firebase sin esperar a que termine este bucle.
        if (currentMacAddress.isNotEmpty()) {
            startFirebaseMonitoring(currentMacAddress)
        }
        localPollJob?.cancel()
        _dataSource.value = KilnDataSource.LOCAL_AP

        localPollJob = viewModelScope.launch {
            var consecutivePingFailures = 0
            while (isActive) {
                val kilnReachable = kilnHttpClient.ping()

                if (!kilnReachable) {
                    consecutivePingFailures++
                    _canControlLocally.value = false

                    // Perfil B: handoff al primer ping fallido aunque el SSID
                    // siga reportando SmartKiln-* (perfil A: >= 2 fallos).
                    val shouldHandOff =
                        consecutivePingFailures >= LOCAL_HANDOFF_PING_FAILURES
                    if (shouldHandOff) {
                        if (_dataSource.value == KilnDataSource.LOCAL_AP) {
                            _dataSource.value = KilnDataSource.FIREBASE
                        }
                        // Soltamos la red pedida por WifiNetworkSpecifier para
                        // que Android vuelva solo a la WiFi de casa. Si nunca
                        // se pidió (API <29) es no-op.
                        KilnNetworkConnector.release(appContext)
                        // Firebase ya está enganchado: rompemos el bucle.
                        break
                    }
                    delay(LOCAL_POLL_INTERVAL_MS)
                    continue
                }

                consecutivePingFailures = 0
                if (_dataSource.value != KilnDataSource.LOCAL_AP) {
                    _dataSource.value = KilnDataSource.LOCAL_AP
                }

                kilnHttpClient.getStatus()
                    .onSuccess {
                        applyLocalStatus(it)
                        _canControlLocally.value = true
                    }
                    .onFailure {
                        Log.w(TAG, "Poll status failed: ${it.message}")
                        _canControlLocally.value = false
                        _kilnStatus.value = _kilnStatus.value.copy(isConnected = false)
                    }
                delay(LOCAL_POLL_INTERVAL_MS)
            }
        }
    }

    private fun startFirebaseMonitoring(macAddress: String) {
        val uid = resolveFirebaseUserId() ?: return
        // Idempotente solo si el listener ya escucha la misma ruta Firebase.
        if (firebaseJob?.isActive == true && firebaseListenerUserId == uid) {
            if (firebaseSyncJob?.isActive != true) startFirebaseStatusPolling()
            if (firebaseConnectionJob?.isActive != true) startFirebaseConnectionMonitoring()
            if (firebaseProfileJob?.isActive != true) startFirebaseProfileMonitoring(macAddress)
            if (firebaseWatchdogJob?.isActive != true) startFirebaseWatchdog()
            if (networkCallback == null) startNetworkMonitoring()
            return
        }

        firebaseJob?.cancel()
        firebaseListenerUserId = uid

        if (_dataSource.value == KilnDataSource.DISCONNECTED) {
            _dataSource.value = KilnDataSource.FIREBASE
        }

        Log.d(TAG, "Firebase listen: users/$uid/kilns/$macAddress/realtimeData")

        startFirebaseConnectionMonitoring()
        startFirebaseProfileMonitoring(macAddress)
        startFirebaseStatusPolling()
        startFirebaseWatchdog()
        startNetworkMonitoring()

        firebaseJob = viewModelScope.launch {
            try {
                firebaseManager.observeRealtimeData(uid, macAddress).collect { data ->
                    applyFirebaseSnapshot(data)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing realtime data: ${e.message}", e)
            }
        }
    }

    /** Paso 2: detecta .info/connected=false y fuerza heal + reenganche del listener. */
    private fun startFirebaseConnectionMonitoring() {
        if (firebaseConnectionJob?.isActive == true) return
        firebaseConnectionJob = viewModelScope.launch {
            try {
                firebaseManager.observeConnectionState().collect { connected ->
                    Log.d(TAG, "Firebase RTDB socket connected=$connected")
                    if (!connected) {
                        if (firebaseRtdbWasConnected) {
                            Log.w(TAG, "RTDB socket lost — scheduling heal")
                        }
                        firebaseRtdbWasConnected = false
                        healFirebaseConnection("rtdb-disconnected")
                    } else {
                        if (!firebaseRtdbWasConnected) {
                            Log.i(TAG, "RTDB socket restored")
                            syncFirebaseStatusOnce()
                        }
                        firebaseRtdbWasConnected = true
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Firebase connection monitor ended: ${e.message}")
            }
        }
    }

    private fun startFirebaseProfileMonitoring(macAddress: String) {
        val uid = resolveFirebaseUserId() ?: return
        if (firebaseProfileJob?.isActive == true) return
        firebaseProfileJob = viewModelScope.launch {
            try {
                firebaseManager.observeProfileState(uid, macAddress).collect { data ->
                    applyProfileStateSnapshot(data)
                }
            } catch (e: Exception) {
                Log.w(TAG, "profileState listener ended: ${e.message}")
            }
        }
    }

    /**
     * Confirma por RTDB que el ESP ya tiene el perfil (profileState), sin HTTP local.
     */
    private fun applyProfileStateSnapshot(data: Map<String, Any?>?) {
        if (data == null) return
        val remoteName = (data["programName"] as? String)?.trim().orEmpty()
        val remoteUpdatedAt = UserIdUtils.parseFirebaseTimestamp(data["programUpdatedAt"])
        if (remoteName.isEmpty() || remoteUpdatedAt <= 0L) return

        viewModelScope.launch {
            val program = resolveProgramForProfileSync(remoteName) ?: return@launch
            if (!remoteName.equals(program.name, ignoreCase = true)) return@launch
            if (remoteUpdatedAt >= program.updatedAt) {
                markProgramSyncedWithKiln(program.id, program.updatedAt)
            }
        }
    }

    private suspend fun resolveProgramForProfileSync(programName: String) =
        _loadedProgramId.value?.let { programRepository.getProgramById(it) }
            ?: _kilnProgram.value.name.trim().takeIf { it.isNotEmpty() }
                ?.let { localName ->
                    programRepository.allPrograms.first()
                        .find { it.name.equals(localName, ignoreCase = true) }
                }
            ?: programRepository.allPrograms.first()
                .find { it.name.equals(programName, ignoreCase = true) }

    private fun markProgramSyncedWithKiln(programId: String, updatedAt: Long) {
        if (lastSyncedProgramUpdatedAt >= updatedAt && pendingProgramUploadId == null) {
            return
        }
        lastSyncedProgramUpdatedAt = updatedAt
        _loadedProgramId.value = programId
        pendingProgramUploadId = null
        Log.d(TAG, "profileState: horno sincronizado con programa $programId (updatedAt=$updatedAt)")
    }

    /** Paso 4: reacciona cuando Android recupera internet validada. */
    private fun startNetworkMonitoring() {
        if (networkCallback != null) return
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.i(TAG, "Network available — healing Firebase")
                viewModelScope.launch {
                    healFirebaseConnection("network-available")
                }
            }

            override fun onLost(network: Network) {
                Log.w(TAG, "Network lost")
            }
        }
        cm.registerNetworkCallback(request, callback)
        networkCallback = callback
    }

    private fun stopNetworkMonitoring() {
        networkCallback?.let { callback ->
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            runCatching { cm.unregisterNetworkCallback(callback) }
        }
        networkCallback = null
    }

    /** Paso 5: si el socket parece vivo pero los datos del ESP llevan >15s, forzar heal. */
    private fun startFirebaseWatchdog() {
        if (firebaseWatchdogJob?.isActive == true) return
        firebaseWatchdogJob = viewModelScope.launch {
            while (isActive && currentMacAddress.isNotEmpty()) {
                delay(FIREBASE_WATCHDOG_INTERVAL_MS)
                if (_dataSource.value == KilnDataSource.LOCAL_AP) continue

                val lastLive = lastFirebaseLiveDataMs
                if (lastLive == 0L) continue

                val staleMs = System.currentTimeMillis() - lastLive
                if (staleMs >= FIREBASE_STALE_DATA_MS) {
                    Log.w(TAG, "Stale Firebase data (${staleMs}ms) — scheduling heal")
                    healFirebaseConnection("stale-data")
                }
            }
        }
    }

    /**
     * Cierra y reabre el socket RTDB, reengancha el listener y pide un snapshot fresco.
     */
    private suspend fun healFirebaseConnection(reason: String) {
        val mac = currentMacAddress
        if (mac.isEmpty()) return

        val now = System.currentTimeMillis()
        if (firebaseHealInProgress) {
            Log.d(TAG, "Heal skipped ($reason): already in progress")
            return
        }
        if (now - lastFirebaseHealMs < FIREBASE_HEAL_DEBOUNCE_MS) {
            Log.d(TAG, "Heal skipped ($reason): debounce")
            return
        }

        firebaseHealInProgress = true
        lastFirebaseHealMs = now
        try {
            Log.w(TAG, "Healing Firebase RTDB ($reason)")

            firebaseJob?.cancel()
            firebaseJob = null
            firebaseListenerUserId = null

            firebaseManager.reconnectFirebase()
            delay(FIREBASE_RECONNECT_SETTLE_MS)

            startFirebaseMonitoring(mac)
            syncFirebaseStatusOnce()
        } catch (e: Exception) {
            Log.e(TAG, "Heal failed ($reason): ${e.message}", e)
        } finally {
            firebaseHealInProgress = false
        }
    }

    private fun applyFirebaseSnapshot(data: Map<String, Any?>?) {
        // Si el HTTP local está respondiendo, esa es la verdad: no dejamos que
        // Firebase pise programName/status/etapa. Solo el bucle local actualiza
        // esos campos cuando _canControlLocally = true.
        val isLocalAuthoritative = _canControlLocally.value &&
            _dataSource.value == KilnDataSource.LOCAL_AP

        if (data == null) {
            if (!isLocalAuthoritative) {
                updateReconnectGrace(_kilnStatus.value.espStatus, isFirebaseLive = false)
                val grace = inReconnectGrace()
                _kilnStatus.value = _kilnStatus.value.copy(
                    isConnected = false,
                    isReconnecting = grace,
                    currentTemperature = if (grace) _kilnStatus.value.currentTemperature else 0f
                )
            }
            return
        }

        val temperature = when (val v = data["temperature"]) {
            is Number -> v.toFloat()
            is String -> v.toFloatOrNull() ?: 0f
            else -> 0f
        }
        val timestamp = UserIdUtils.parseFirebaseTimestamp(data["timestamp"])
        val statusRaw = (data["status"] as? String)?.trim()?.uppercase().orEmpty()
        val programName = (data["programName"] as? String)?.trim().orEmpty()

        val currentTimeMs = System.currentTimeMillis()
        val isConnected = timestamp > 0 &&
            (currentTimeMs - timestamp) < CONNECTION_TIMEOUT_MS
        if (isConnected) {
            lastFirebaseLiveDataMs = currentTimeMs
        }

        updateReconnectGrace(statusRaw, isConnected)
        val statusForUi = resolveStatusForUi(statusRaw, isConnected)
        val isReconnecting = inReconnectGrace() && !isConnected

        val programRunningState = ProgramStatusUtils.determineProgramState(
            hasBleConnection = false,
            bleStatus = null,
            bleProgramName = null,
            firebaseData = data,
            programTotalDurationMinutes = _kilnProgram.value.stages
                .takeIf { it.isNotEmpty() }
                ?.sumOf { it.durationMinutes }
        )

        val shouldUpdateRunning = if (!isConnected) {
            false
        } else when (programRunningState) {
            ProgramStatusUtils.ProgramRunningState.CONFIRMED_RUNNING,
            ProgramStatusUtils.ProgramRunningState.ESTIMATED_RUNNING -> true
            ProgramStatusUtils.ProgramRunningState.CONFIRMED_STOPPED,
            ProgramStatusUtils.ProgramRunningState.ESTIMATED_STOPPED -> false
            else -> programName.isNotEmpty() && statusForUi in ACTIVE_KILN_STATUSES
        }
        val isPaused = statusForUi == "PAUSADO"

        // Diálogo solo con INTERRUMPIDO reciente confirmado por el ESP en Firebase.
        _showInterruptedDialog.value =
            (statusForUi == "INTERRUMPIDO" && isConnected)

        if (isLocalAuthoritative) {
            // Temperatura remota + estado (p. ej. INTERRUMPIDO tras reinicio del ESP
            // mientras el teléfono aún cree que está en sesión local).
            _kilnStatus.value = _kilnStatus.value.copy(
                isConnected = isConnected,
                isReconnecting = isReconnecting,
                currentTemperature = temperature,
                espStatus = statusForUi.ifEmpty { _kilnStatus.value.espStatus }
            )
            recordTemperatureSample(temperature)
            return
        }

        if (!shouldPreferLocalProgramMetadata(programName, statusForUi)) {
            if (isConnected && programName.isNotEmpty() &&
                programName != _kilnProgram.value.name
            ) {
                _kilnProgram.value = _kilnProgram.value.copy(name = programName)
            }
            noteRemoteProgramNameSeen(programName)
            if (_kilnProgram.value.name.isNotEmpty() &&
                _kilnProgram.value.stages.isEmpty()
            ) {
                ensureProgramStagesLoaded()
            }
        }

        _kilnStatus.value = _kilnStatus.value.copy(
            isConnected = isConnected,
            isReconnecting = isReconnecting,
            currentTemperature = temperature,
            isRunning = shouldUpdateRunning,
            isPaused = isPaused,
            espStatus = statusForUi
        )

        applyProgramTimingFromFirebase(
            data = data,
            statusForUi = statusForUi,
            programName = programName,
            isConnected = isConnected
        )
        recordTemperatureSample(temperature)
    }

    private fun applyLocalStatus(dto: KilnStatusDto) {
        if (dto.error != null) {
            Log.w(TAG, "Status error from kiln: ${dto.error}")
            return
        }
        val status = dto.status.trim().uppercase()
        val programName = dto.programName.trim()
        val hasActiveProgram = programName.isNotEmpty() || dto.profileLoaded
        val isPaused = status == "PAUSADO"
        val isActive = hasActiveProgram && status in ACTIVE_KILN_STATUSES

        _showInterruptedDialog.value = (status == "INTERRUMPIDO")

        val preferLocalProgram = shouldPreferLocalProgramMetadata(programName, status)

        if (!preferLocalProgram) {
            if (programName.isNotEmpty() && programName != _kilnProgram.value.name) {
                _kilnProgram.value = _kilnProgram.value.copy(name = programName)
            }
            noteRemoteProgramNameSeen(programName)
            if (_kilnProgram.value.name.isNotEmpty() &&
                _kilnProgram.value.stages.isEmpty()
            ) {
                ensureProgramStagesLoaded()
            }
        }

        awaitingKilnReconnect = false
        reconnectDeadlineMs = 0L
        _kilnStatus.value = _kilnStatus.value.copy(
            isConnected = true,
            isReconnecting = false,
            currentTemperature = dto.temperature,
            espStatus = status,
            isPaused = isPaused,
            isRunning = isActive,
            totalStages = if (preferLocalProgram) {
                _kilnProgram.value.stages.size.coerceAtLeast(dto.numStages)
            } else {
                dto.numStages
            }
        )

        if (preferLocalProgram) {
            refreshProgramDisplayFromLocalStages()
            recordTemperatureSample(dto.temperature)
            return
        }

        val durationMin = computeTotalDurationMin(dto = dto)
        updateProgramWallClockForStatus(status, dto.elapsedMinutes.coerceAtLeast(0))

        if (durationMin > 0 || programName.isNotEmpty() || dto.profileLoaded) {
            val elapsedMin = when {
                status in setOf("PAUSADO", "INTERRUMPIDO") ->
                    dto.elapsedMinutes.coerceAtLeast(0)
                status == "CALENTANDO" && dto.elapsedMinutes > 0 ->
                    dto.elapsedMinutes
                status == "CALENTANDO" && programStartedAtWallMs != null ->
                    ((System.currentTimeMillis() - programStartedAtWallMs!!) / 60_000L)
                        .toInt().coerceAtLeast(0)
                else -> 0
            }
            val remainingMin = when {
                dto.remainingMinutes > 0 -> dto.remainingMinutes
                durationMin > 0 -> (durationMin - elapsedMin).coerceAtLeast(0)
                else -> 0
            }
            val totalStages = dto.numStages
                .coerceAtLeast(_kilnProgram.value.stages.size)
            val currentStage = when {
                dto.currentStage > 0 -> dto.currentStage
                status in ACTIVE_KILN_STATUSES && totalStages > 0 -> 1
                else -> 0
            }
            syncKilnProgramFields(
                programName = programName.takeIf { it.isNotEmpty() },
                durationMin = durationMin,
                elapsedMin = elapsedMin,
                remainingMin = remainingMin,
                currentStage = currentStage,
                totalStages = totalStages
            )
        }
        recordTemperatureSample(dto.temperature)
    }

    private fun formatMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return "${hours}h ${String.format("%02d", mins)}m"
    }

    private suspend fun ensureLocalConnection(): Boolean {
        if (_canControlLocally.value) return true
        if (_dataSource.value == KilnDataSource.LOCAL_AP && kilnHttpClient.ping()) {
            kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
            _canControlLocally.value = true
            return true
        }
        if (kilnHttpClient.ping()) {
            startLocalMonitoring()
            refreshApInfo()
            kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
            _canControlLocally.value = true
            return true
        }

        // Auto-conexión vía WifiNetworkSpecifier (Android 10+). Android muestra
        // un diálogo nativo encima de la app la primera vez (después recuerda
        // la aprobación). NO sobrescribimos _sendingCommandPhase para que el
        // título del diálogo modal del comando (Iniciando/Pausando/...) se
        // mantenga estable durante toda la operación.
        if (KilnNetworkConnector.isSupported &&
            currentMacAddress.isNotEmpty() &&
            !KilnNetworkConnector.isInPostHandoffCooldown(KILN_AP_RECONFIG_WAIT_MS)
        ) {
            val ssid = KilnNetworkConnector.ssidFromMac(currentMacAddress)
            val pass = KilnNetworkConnector.passphraseFromMac(currentMacAddress)
            if (ssid != null && pass != null) {
                _isConnectingToAp.value = true
                _sendingCommandDetail.value = "Conectando al WiFi del horno…"
                try {
                    val network = KilnNetworkConnector.connect(appContext, ssid, pass)
                    if (network != null) {
                        // El AP recién aceptó la asociación; dejamos margen para
                        // que el ESP termine de enrutar antes del primer ping.
                        delay(500)
                        if (kilnHttpClient.ping()) {
                            startLocalMonitoring()
                            refreshApInfo()
                            kilnHttpClient.getStatus()
                                .onSuccess { applyLocalStatus(it) }
                            _canControlLocally.value = true
                            return true
                        }
                        Log.w(TAG, "Network del horno disponible pero ping falló")
                        KilnNetworkConnector.release(appContext)
                    }
                } finally {
                    _isConnectingToAp.value = false
                }
            }
        }

        _canControlLocally.value = false
        // Durante la ventana de reconfiguración del AP, no confundir con WiFi manual.
        if (remainingApReconfigSeconds() != null) {
            return false
        }
        // Solo pedimos abrir Settings si el atajo automático no es viable
        // (API <29 o el usuario no aprobó el diálogo del sistema).
        _shouldShowManualWifiDialog.value = !KilnNetworkConnector.isSupported ||
            !WifiUtils.isOnSmartKilnNetwork(appContext)
        return false
    }

    fun clearManualWifiDialogRequest() {
        _shouldShowManualWifiDialog.value = false
    }

    fun clearApReconfigWaitDialog() {
        _apReconfigWaitDialog.value = null
    }

    private fun remainingApReconfigSeconds(): Int? {
        if (KilnNetworkConnector.lastReleaseAtMs <= 0L) return null
        val elapsed = System.currentTimeMillis() - KilnNetworkConnector.lastReleaseAtMs
        if (elapsed >= KILN_AP_RECONFIG_WAIT_MS) return null
        val remainingMs = KILN_AP_RECONFIG_WAIT_MS - elapsed
        return kotlin.math.ceil(remainingMs / 1000.0).toInt().coerceAtLeast(1)
    }

    /** True si hay que esperar (y ya se publicó el diálogo). */
    private suspend fun blockIfApReconfiguring(action: LocalKilnAction): Boolean {
        // Fuera de la ventana de 15 s no hace falta ping: evita ~1,5 s de espera
        // antes del modal cuando el AP ya está disponible pero el teléfono
        // sigue en la WiFi de casa.
        val remaining = remainingApReconfigSeconds() ?: return false
        if (kilnHttpClient.ping()) return false
        _apReconfigWaitDialog.value = ApReconfigWaitState(remaining, action.description)
        return true
    }

    /**
     * Tras transferir datos al ESP por HTTP local: suelta WifiNetworkSpecifier
     * para que Android vuelva a la WiFi de casa, y pasa la UI a Firebase.
     * El ESP apaga SoftAP en paralelo (~15 s perfil B) por si el usuario entró a mano.
     */
    private fun handOffToHomeWifiAfterLocalSession(reason: String) {
        localPollJob?.cancel()
        localPollJob = null
        _canControlLocally.value = false
        if (_dataSource.value == KilnDataSource.LOCAL_AP) {
            _dataSource.value = KilnDataSource.FIREBASE
        }
        KilnNetworkConnector.release(appContext)
        if (currentMacAddress.isNotEmpty()) {
            startFirebaseMonitoring(currentMacAddress)
        }
        Log.d(TAG, "Hand-off tras sesión local ($reason) → Firebase / WiFi casa")
    }

    private suspend fun sendLocalCommand(cmd: String): Boolean {
        if (blockIfApReconfiguring(LocalKilnAction.fromCommand(cmd))) {
            return false
        }

        // El diálogo modal con el círculo girando se muestra durante TODO el
        // proceso (conexión al AP + envío HTTP + lectura de status). El título
        // (phase) se queda fijo durante toda la operación; solo el subtítulo
        // (detail) cambia para informar de la sub-fase.
        val title = when (cmd) {
            "START" -> "Iniciando horno…"
            "RESUME" -> "Reanudando horno…"
            "PAUSE" -> "Pausando horno…"
            "STOP" -> "Deteniendo horno…"
            else -> "Enviando comando…"
        }
        _sendingCommandPhase.value = title
        _sendingCommandDetail.value = "Conectando al horno…"
        _isSendingCommand.value = true
        try {
            if (!ensureLocalConnection()) return false

            _sendingCommandDetail.value = "Enviando comando…"

            return kilnHttpClient.sendCommand(cmd)
                .onSuccess {
                    Log.d(TAG, "Command $cmd OK")
                    // Update optimista: anticipamos el estado del horno antes
                    // de que llegue la confirmación del HTTP/Firebase, así el
                    // botón en pantalla cambia en cuanto cierra el diálogo.
                    applyOptimisticCommandState(cmd)
                    delay(400)
                    kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
                    handOffToHomeWifiAfterLocalSession(cmd)
                }
                .onFailure { Log.e(TAG, "Command $cmd failed: ${it.message}") }
                .isSuccess
        } finally {
            _isSendingCommand.value = false
            _sendingCommandPhase.value = ""
            _sendingCommandDetail.value = ""
        }
    }

    /**
     * Refleja el efecto esperado del comando en _kilnStatus al instante, sin
     * esperar la confirmación del horno. Esto evita que el botón muestre el
     * estado anterior durante los segundos que tarda el handoff a Firebase.
     * Si la confirmación real difiere, applyLocalStatus / Firebase la
     * sobrescriben en la próxima actualización.
     */
    private fun applyOptimisticCommandState(cmd: String) {
        var resumeElapsedMin = 0
        when (cmd) {
            "START" -> {
                clearTemperatureHistory()
                ensureProgramStartWallClock()
                pausedElapsedMin = null
            }
            "RESUME" -> {
                resumeElapsedMin = pausedElapsedMin ?: _kilnStatus.value.elapsedMinutes
                programStartedAtWallMs =
                    System.currentTimeMillis() - resumeElapsedMin * 60_000L
                pausedElapsedMin = null
            }
            "PAUSE" -> {
                pausedElapsedMin = programStartedAtWallMs?.let {
                    ((System.currentTimeMillis() - it) / 60_000L).toInt().coerceAtLeast(0)
                } ?: _kilnStatus.value.elapsedMinutes
            }
            "STOP" -> clearProgramStartWallClock()
        }

        _kilnStatus.value = when (cmd) {
            "START", "RESUME" -> _kilnStatus.value.copy(
                isRunning = true,
                isPaused = false,
                espStatus = "CALENTANDO"
            )
            "PAUSE" -> _kilnStatus.value.copy(
                isRunning = true,
                isPaused = true,
                espStatus = "PAUSADO"
            )
            "STOP" -> _kilnStatus.value.copy(
                isRunning = false,
                isPaused = false,
                espStatus = "IDLE"
            )
            else -> _kilnStatus.value
        }

        val totalDur = computeTotalDurationMin()
        if (totalDur > 0 && cmd != "STOP") {
            val elapsed = when (cmd) {
                "PAUSE" -> pausedElapsedMin ?: 0
                "RESUME" -> resumeElapsedMin
                else -> 0
            }
            syncKilnProgramFields(
                durationMin = totalDur,
                elapsedMin = elapsed,
                remainingMin = (totalDur - elapsed).coerceAtLeast(0),
                currentStage = _kilnStatus.value.currentStage.coerceAtLeast(1),
                totalStages = _kilnStatus.value.totalStages
                    .coerceAtLeast(_kilnProgram.value.stages.size)
            )
        }
    }

    fun stopKiln() {
        val status = _kilnStatus.value
        val canStop = status.isRunning || status.isPaused ||
            status.espStatus in setOf("CALENTANDO", "PAUSADO")
        if (!canStop) {
            Log.d(TAG, "stopKiln ignorado: horno ya detenido (${status.espStatus})")
            return
        }
        viewModelScope.launch {
            sendLocalCommand("STOP")
        }
    }

    fun playKiln() {
        viewModelScope.launch {
            sendLocalCommand("START")
        }
    }

    fun pauseKiln() {
        viewModelScope.launch {
            sendLocalCommand("PAUSE")
        }
    }

    /**
     * Acción "Reanudar" del diálogo de interrupción: el ESP arranca el
     * programa desde el segmento/fase guardados en NVS antes del corte.
     */
    fun resumeKiln() {
        viewModelScope.launch {
            sendLocalCommand("RESUME")
            _showInterruptedDialog.value = false
        }
    }

    /**
     * Acción "Cancelar" del diálogo de interrupción: limpia el run_state
     * en el ESP (manda STOP) y el horno vuelve a IDLE.
     */
    fun cancelInterruptedProgram() {
        viewModelScope.launch {
            sendLocalCommand("STOP")
            _showInterruptedDialog.value = false
        }
    }

    fun modifyProgram() {
        Log.d(TAG, "Modify program — seleccionar otro programa")
    }

    /** Recarga etapas desde Room si solo tenemos el nombre (p. ej. al volver a la pantalla). */
    fun ensureProgramStagesLoaded() {
        viewModelScope.launch {
            if (_kilnProgram.value.stages.isNotEmpty()) return@launch
            try {
                val byId = _loadedProgramId.value
                if (byId != null && loadProgramStagesIntoState(byId, resetSchedule = false)) {
                    return@launch
                }
                val name = _kilnProgram.value.name.trim()
                if (name.isEmpty()) return@launch
                val match = programRepository.allPrograms.first()
                    .find { it.name.equals(name, ignoreCase = true) }
                if (match != null) {
                    loadProgramStagesIntoState(match.id, resetSchedule = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "ensureProgramStagesLoaded: ${e.message}", e)
            }
        }
    }

    /**
     * ID del programa actual para editarlo. Resuelve por ID cargado o por nombre en Room.
     */
    suspend fun resolveCurrentProgramIdForEdit(): String? {
        _loadedProgramId.value?.let { return it }
        val name = _kilnProgram.value.name.trim()
        if (name.isEmpty()) return null
        return programRepository.allPrograms.first()
            .find { it.name.equals(name, ignoreCase = true) }
            ?.id
            ?.also { _loadedProgramId.value = it }
    }

    /**
     * Tras editar el programa en CreateProgramScreen, refresca etapas locales
     * y vuelve a subir el perfil al horno (también si está corriendo).
     * Solo actúa si Room tiene un updatedAt más reciente que la última sync.
     */
    fun reloadCurrentProgramAfterEdit() {
        val programId = _loadedProgramId.value ?: return
        viewModelScope.launch {
            try {
                val program = programRepository.getProgramById(programId) ?: return@launch
                if (program.updatedAt <= lastSyncedProgramUpdatedAt &&
                    _kilnProgram.value.stages.isNotEmpty()
                ) {
                    return@launch
                }

                val wasRunning = _kilnStatus.value.isRunning ||
                    _kilnStatus.value.isPaused ||
                    _kilnStatus.value.espStatus in ACTIVE_KILN_STATUSES

                loadProgramStagesIntoState(programId, resetSchedule = !wasRunning)
                lastSyncedProgramUpdatedAt = program.updatedAt
                uploadProfileIfLocal(programId, program.name)
                Log.d(
                    TAG,
                    "Programa refresado tras edición: ${program.name} (running=$wasRunning)"
                )
            } catch (e: Exception) {
                Log.e(TAG, "reloadCurrentProgramAfterEdit: ${e.message}", e)
            }
        }
    }

    private suspend fun buildProgramStagesFromRoom(programId: String): List<ProgramStage>? {
        val stageEntities = programRepository.getStagesByProgram(programId).first()
        if (stageEntities.isEmpty()) return null

        var previousTemp = 0f
        return stageEntities.map { stageEntity ->
            val temperature = stageEntity.temperature.toFloatOrNull() ?: 0f
            val speed = stageEntity.speed.toFloatOrNull() ?: 0f
            val maintenance = stageEntity.maintenance.toIntOrNull() ?: 0
            val tempIncrement = (temperature - previousTemp).coerceAtLeast(0f)
            val rampMinutes = if (speed > 0) (tempIncrement / speed).toInt().coerceAtLeast(0) else 0
            val totalDuration = (rampMinutes + maintenance).coerceAtLeast(0)
            previousTemp = temperature
            ProgramStage(
                stageNumber = stageEntity.stageNumber,
                targetTemp = temperature,
                rampRate = speed,
                maintenanceMinutes = maintenance.coerceAtLeast(0),
                durationMinutes = totalDuration,
                isCompleted = false
            )
        }
    }

    private suspend fun loadProgramStagesIntoState(
        programId: String,
        resetSchedule: Boolean
    ): Boolean {
        val programEntity = programRepository.getProgramById(programId) ?: return false
        val programStages = buildProgramStagesFromRoom(programId) ?: return false
        _loadedProgramId.value = programId

        val totalMinutes = programStages.sumOf { it.durationMinutes }
        val maxTemp = programStages.maxOfOrNull { it.targetTemp } ?: 0f

        if (resetSchedule) {
            clearTemperatureHistory()
            clearProgramStartWallClock()
            localProgramSelectedAtMs = System.currentTimeMillis()
            _kilnProgram.value = KilnProgram(
                name = programEntity.name,
                duration = formatMinutes(totalMinutes),
                remaining = formatMinutes(totalMinutes),
                elapsed = "0h 00m",
                stage = "0/${programStages.size}",
                stages = programStages
            )
            syncKilnProgramFields(
                programName = programEntity.name,
                durationMin = totalMinutes,
                elapsedMin = 0,
                remainingMin = totalMinutes,
                currentStage = 0,
                totalStages = programStages.size
            )
            _kilnStatus.value = _kilnStatus.value.copy(
                isRunning = false,
                isPaused = false,
                targetTemperature = maxTemp
            )
        } else {
            val elapsed = _kilnStatus.value.elapsedMinutes.coerceAtLeast(0)
            _kilnProgram.value = _kilnProgram.value.copy(
                name = programEntity.name,
                stages = programStages
            )
            syncKilnProgramFields(
                programName = programEntity.name,
                durationMin = totalMinutes,
                elapsedMin = elapsed,
                remainingMin = (totalMinutes - elapsed).coerceAtLeast(0),
                currentStage = _kilnStatus.value.currentStage,
                totalStages = programStages.size
            )
            _kilnStatus.value = _kilnStatus.value.copy(
                targetTemperature = maxTemp
            )
        }
        return true
    }

    fun loadSelectedProgram(programId: String) {
        Log.d(TAG, "loadSelectedProgram: $programId")
        localProgramSelectedAtMs = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                if (!loadProgramStagesIntoState(programId, resetSchedule = true)) {
                    Log.e(TAG, "Programa no encontrado o sin etapas: $programId")
                    return@launch
                }
                refreshProgramDisplayFromLocalStages()
                val programEntity = programRepository.getProgramById(programId)!!
                uploadProfileIfLocal(programId, programEntity.name)
                Log.d(TAG, "Program loaded: ${programEntity.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading selected program: ${e.message}", e)
            }
        }
    }

    private fun uploadProfileIfLocal(programId: String, programName: String) {
        pendingProgramUploadId = programId
        viewModelScope.launch {
            uploadProgramWithProgress(programId, programName)
        }
    }

    /**
     * Sube el perfil al horno mostrando el mismo diálogo modal con círculo
     * girando que usan los comandos. Si no estamos en la red del horno,
     * usa la auto-conexión vía WifiNetworkSpecifier (igual que sendLocalCommand).
     * Si la auto-conexión falla, deja el programa como pendingProgramUploadId
     * para que refreshConnection() lo reintente más tarde.
     */
    private suspend fun uploadProgramWithProgress(programId: String, programName: String) {
        if (blockIfApReconfiguring(LocalKilnAction.UPDATE_PROGRAM)) {
            Log.w(TAG, "Upload pospuesto: horno reconfigurando red")
            return
        }

        _sendingCommandPhase.value = "Cargando «$programName»…"
        _sendingCommandDetail.value = "Conectando al horno…"
        _isSendingCommand.value = true
        _isUploadingProgram.value = true
        try {
            if (!ensureLocalConnection()) {
                // ensureLocalConnection ya gestionó el manualWifiDialog si hizo falta.
                Log.w(TAG, "Upload pospuesto: sin conexión local")
                return
            }

            _sendingCommandDetail.value = "Enviando programa al horno…"

            KilnProgramUploader.uploadProgram(
                appContext,
                programRepository,
                programId,
                kilnHttpClient
            )
                .onSuccess {
                    pendingProgramUploadId = null
                    _canControlLocally.value = true
                    noteRemoteProgramNameSeen(programName)
                    programRepository.getProgramById(programId)?.let {
                        lastSyncedProgramUpdatedAt = it.updatedAt
                        markProgramSyncedWithKiln(programId, it.updatedAt)
                    }
                    kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
                    handOffToHomeWifiAfterLocalSession("profile")
                    Log.d(TAG, "Profile uploaded for $programId ($programName)")
                }
                .onFailure {
                    Log.e(TAG, "Profile upload failed for $programId: ${it.message}")
                }
        } finally {
            _isUploadingProgram.value = false
            _isSendingCommand.value = false
            _sendingCommandPhase.value = ""
            _sendingCommandDetail.value = ""
        }
    }

    private suspend fun retryPendingProgramUpload() {
        val programId = pendingProgramUploadId ?: return
        // Reintento silencioso: solo si ya estamos en el WiFi del horno. No
        // disparamos diálogos invasivos en cada ON_RESUME o refresh; el
        // diálogo modal de progreso solo aparece cuando el usuario
        // explícitamente carga el programa.
        if (!WifiUtils.isOnSmartKilnNetwork(appContext)) return
        val program = programRepository.getProgramById(programId) ?: return
        uploadProgramWithProgress(programId, program.name)
    }

    fun checkRunningProgram() {
        viewModelScope.launch {
            if (_kilnStatus.value.isRunning && _kilnProgram.value.name.isNotEmpty()) {
                Log.d(TAG, "Programa ya corriendo: ${_kilnProgram.value.name}")
            }
        }
    }
}
