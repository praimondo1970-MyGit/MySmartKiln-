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
import com.example.mysmartkiln.data.local.KilnTelemetryCache
import com.example.mysmartkiln.data.repository.KilnRepository
import com.example.mysmartkiln.data.repository.ProgramRepository
import com.example.mysmartkiln.data.local.SoftApDeviceType
import com.example.mysmartkiln.utils.DeviceKind
import com.example.mysmartkiln.utils.KilnNetworkConnector
import com.example.mysmartkiln.utils.ProgramStatusUtils
import com.example.mysmartkiln.utils.UserIdUtils
import com.example.mysmartkiln.utils.WifiUtils
import com.example.mysmartkiln.data.local.LoggerRecordingStore
import com.example.mysmartkiln.data.local.LoggerRecordingSession
import com.example.mysmartkiln.data.local.LoggerRecordingSample
import com.example.mysmartkiln.data.local.KilnFiringStore
import com.example.mysmartkiln.data.local.KilnFiringSession
import com.example.mysmartkiln.data.local.KilnFiringCloudSync
import com.example.mysmartkiln.data.model.DeviceRealtimeSnapshot
import java.util.UUID
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
    /**
     * Último estado de proceso visto con telemetría fresca (CALENTANDO, PAUSADO, …).
     * Se conserva al perder enlace para mostrarlo en el detalle de Estado.
     */
    val lastKnownEspStatus: String = "",
    /** Reloj del teléfono en el último dato vivo (para “desconectado hace…”). */
    val lastLiveTelemetryAtMs: Long = 0L,
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
    UPDATE_PROGRAM("actualizar el programa"),
    LOGGER_START("iniciar la grabación"),
    LOGGER_SAVE("guardar la grabación");

    companion object {
        fun fromCommand(cmd: String): LocalKilnAction = when (cmd) {
            "START" -> START
            "PAUSE" -> PAUSE
            "STOP" -> STOP
            "RESUME" -> RESUME
            "START_LOG" -> LOGGER_START
            "STOP_LOG" -> LOGGER_SAVE
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

    private val _isDataLogger = MutableStateFlow(false)
    val isDataLogger: StateFlow<Boolean> = _isDataLogger.asStateFlow()

    private val _deviceHistoryCsv = MutableStateFlow<String?>(null)
    val deviceHistoryCsv: StateFlow<String?> = _deviceHistoryCsv.asStateFlow()

    private val _deviceHistoryLoading = MutableStateFlow(false)
    val deviceHistoryLoading: StateFlow<Boolean> = _deviceHistoryLoading.asStateFlow()

    private val _deviceHistoryError = MutableStateFlow<String?>(null)
    val deviceHistoryError: StateFlow<String?> = _deviceHistoryError.asStateFlow()

    /** Reloj de sesión para muestras del DataLogger (sin programa de horno). */
    private var loggerSessionStartedAtMs: Long? = null

    private val recordingStore = LoggerRecordingStore(appContext)
    private val kilnFiringStore = KilnFiringStore(appContext)
    private val _activeRecording = MutableStateFlow<LoggerRecordingSession?>(null)
    val activeRecording: StateFlow<LoggerRecordingSession?> = _activeRecording.asStateFlow()
    private val _recordingMessage = MutableStateFlow<String?>(null)
    val recordingMessage: StateFlow<String?> = _recordingMessage.asStateFlow()

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

    /** Mientras Android está abriendo el diálogo nativo "¿Usar SmartKiln-XXXXXXXX?". */
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

    /** Error de acción local (p. ej. falló la carga de programa al ESP). */
    private val _localActionError = MutableStateFlow<String?>(null)
    val localActionError: StateFlow<String?> = _localActionError.asStateFlow()

    /**
     * Verdadero cuando detectamos que el horno reporta status="INTERRUMPIDO".
     * El firmware lo deja así si el ESP se reseteó mientras estaba CALENTANDO
     * o PAUSADO. La UI muestra un diálogo "Reanudar / Cancelar" no
     * descartable para que el usuario decida qué hacer.
     */
    private val _showInterruptedDialog = MutableStateFlow(false)
    val showInterruptedDialog: StateFlow<Boolean> = _showInterruptedDialog.asStateFlow()

    /**
     * El usuario ya eligió Reanudar/Cancelar en este episodio de interrupción.
     * Evita que un snapshot Firebase viejo con INTERRUMPIDO reabra el diálogo
     * tras un RESUME exitoso. Se limpia al salir de INTERRUMPIDO.
     */
    private var interruptedDialogAcked = false

    /**
     * Tras START/PAUSE/STOP/RESUME por SoftAP, Firebase puede seguir unos segundos
     * con el status anterior. Mientras tanto no dejamos que RTDB pise el estado local.
     */
    private var heldCommandStatus: String? = null
    private var heldCommandUntilMs: Long = 0L

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

    private fun authFirebaseUserId(): String? {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return null
        return UserIdUtils.getCurrentUserId(email).takeIf { it.isNotBlank() && it != "test_user_123" }
    }

    private fun resolveFirebaseUserId(): String? {
        val fromKiln = kilnFirebaseUserId?.takeIf {
            it.isNotBlank() && it != "test_user_123"
        }
        return fromKiln ?: authFirebaseUserId()
    }

    companion object {
        private const val TAG = "KilnDetailViewModel"
        /** Sin timestamp fresco en realtimeData → desconectado para la UI. */
        private const val CONNECTION_TIMEOUT_MS = 30_000L
        private const val LOCAL_POLL_INTERVAL_MS = 1_500L
        // Perfil B (equilibrado). Revertir perfil A: PROBE=6, HANDOFF_FAILURES=2, COOLDOWN=0
        private const val LOCAL_PROBE_ATTEMPTS = 3
        private const val LOCAL_PROBE_DELAY_MS = 350L
        /** Handoff a Firebase tras N pings fallidos (perfil A: 2). */
        private const val LOCAL_HANDOFF_PING_FAILURES = 1
        /**
         * Tras soltar el AP del horno: ventana en la que el ESP apaga/restaura
         * SoftAP (perfil C: gracia~0,3 s + off~8 s). Bloquea connect() y aviso UI.
         */
        private const val KILN_AP_RECONFIG_WAIT_MS = 10_000L
        /** Poll RTDB en modo remoto por si el listener no dispara (p. ej. tras reinicio del ESP). */
        private const val FIREBASE_STATUS_POLL_MS = 5_000L
        private const val FIREBASE_FAST_POLL_MS = 2_000L
        /** Poll agresivo mientras Remoto está gris (paso 3). */
        private const val FIREBASE_DISCONNECTED_POLL_MS = 1_000L
        /** Sin datos frescos del ESP → forzar heal (paso 5). */
        private const val FIREBASE_STALE_DATA_MS = 30_000L
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
        private val PROCESS_STATUSES = setOf(
            "CALENTANDO", "PAUSADO", "INTERRUMPIDO", "FINALIZADO",
            "IDLE", "DETENIDO", "GRABANDO"
        )
    }

    override fun onCleared() {
        stopMonitoring()
        KilnNetworkConnector.release(appContext)
        super.onCleared()
    }

    fun loadKilnData(macAddress: String) {
        val macNorm = normalizeMac(macAddress)
        if (normalizeMac(currentMacAddress) != macNorm) {
            stopMonitoring()
            _dataSource.value = KilnDataSource.DISCONNECTED
            _activeRecording.value = null
            _recordingMessage.value = null
            interruptedDialogAcked = false
            _showInterruptedDialog.value = false
            clearCommandStatusHold()
        }
        currentMacAddress = macAddress
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val kiln = kilnRepository.getKilnByMacAddress(macAddress)
                _isDataLogger.value = kiln?.let { DeviceKind.isDataLogger(it) } == true
                if (_isDataLogger.value && loggerSessionStartedAtMs == null) {
                    loggerSessionStartedAtMs = System.currentTimeMillis()
                }
                // Restaurar grabación activa local (sobrevive recomposición / handoff)
                if (_isDataLogger.value && _activeRecording.value?.isActive != true) {
                    restoreActiveRecordingFromStore(macNorm)
                }
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
                ensureProgramStagesLoaded()
                detectAndStartMonitoring()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading kiln data: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun restoreActiveRecordingFromStore(mac: String) {
        val active = recordingStore.loadAll()
            .firstOrNull { normalizeMac(it.deviceMac) == mac && it.isActive }
            ?: return
        _activeRecording.value = active
        loggerSessionStartedAtMs = active.startedAtMs
        Log.d(TAG, "Restaurada grabación activa ${active.id} (${active.name})")
    }

    /**
     * Si el ESP reporta GRABANDO pero la UI perdió la sesión (app reiniciada /
     * confirmación HTTP fallida), recrea un stub activo para habilitar GUARDAR.
     */
    private fun ensureActiveRecordingFromTelemetry(
        status: String,
        sessionId: String?,
        programName: String?
    ) {
        if (!_isDataLogger.value) return
        val grabbing = status.equals("GRABANDO", ignoreCase = true)
        if (!grabbing) return
        if (_activeRecording.value?.isActive == true) return

        val mac = normalizeMac(currentMacAddress)
        restoreActiveRecordingFromStore(mac)
        if (_activeRecording.value?.isActive == true) return

        // Buscar por sessionId aunque se haya marcado ended por error
        val sid = sessionId?.takeIf { it.isNotBlank() }
        if (sid != null) {
            val byId = recordingStore.loadAll().firstOrNull { it.id == sid }
            if (byId != null) {
                val revived = byId.copy(endedAtMs = null)
                _activeRecording.value = revived
                recordingStore.upsert(revived)
                loggerSessionStartedAtMs = revived.startedAtMs
                Log.d(TAG, "Reactivada grabación $sid desde store (ESP GRABANDO)")
                return
            }
        }

        val stub = LoggerRecordingSession(
            id = sid ?: java.util.UUID.randomUUID().toString().replace("-", "").take(16),
            deviceMac = mac,
            name = programName?.takeIf { it.isNotBlank() } ?: "Sesión en curso",
            comments = "",
            startedAtMs = System.currentTimeMillis(),
            samples = emptyList()
        )
        _activeRecording.value = stub
        recordingStore.upsert(stub)
        loggerSessionStartedAtMs = stub.startedAtMs
        Log.d(TAG, "Stub de grabación activa ${stub.id} (ESP GRABANDO)")
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
                if (isLocalHttpForCurrentDevice()) {
                    startLocalMonitoring()
                    kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
                    refreshApInfo()
                    _canControlLocally.value = true
                    _connectionMessage.value = "Conexión local establecida."
                } else {
                    _canControlLocally.value = false
                    _connectionMessage.value =
                        "No se pudo contactar al dispositivo correcto en 192.168.4.1. " +
                            "¿Estás en su WiFi SoftAP (y no en el de otro equipo)?"
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
            if (isLocalHttpForCurrentDevice()) {
                startLocalMonitoring()
                kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
                refreshApInfo()
                return true
            }
            delay(LOCAL_PROBE_DELAY_MS)
        }
        return false
    }

    /**
     * SoftAP de horno y DataLogger comparten 192.168.4.1.
     * Solo aceptamos HTTP local si /api/info reporta la MAC del dispositivo abierto.
     */
    private suspend fun isLocalHttpForCurrentDevice(): Boolean {
        if (!kilnHttpClient.ping()) return false
        val expected = normalizeMac(currentMacAddress)
        if (expected.length < 8) return false
        val info = kilnHttpClient.getInfo().getOrNull()
        if (info != null) {
            val remote = normalizeMac(info.macAddress)
            if (remote.isNotBlank()) {
                if (remote != expected) {
                    Log.w(
                        TAG,
                        "SoftAP MAC $remote ≠ dispositivo abierto $expected — ignorando (¿otro SoftAP?)"
                    )
                    return false
                }
                return true
            }
        }
        // Firmware viejo sin mac en /api/info: exigir que el SSID SoftAP coincida.
        val ssid = WifiUtils.getCurrentWifiSSID(appContext) ?: return false
        val expectedSsid = KilnNetworkConnector.ssidFromMac(
            expected,
            if (_isDataLogger.value) SoftApDeviceType.DATALOGGER else SoftApDeviceType.KILN
        )
        val ok = expectedSsid != null && ssid.equals(expectedSsid, ignoreCase = true)
        if (!ok) {
            Log.w(TAG, "SSID actual '$ssid' ≠ esperado '$expectedSsid' — no se usa HTTP local")
        }
        return ok
    }

    private fun normalizeMac(mac: String): String =
        mac.replace(":", "").replace("-", "").uppercase()

    private fun statusMatchesCurrentDevice(dto: KilnStatusDto): Boolean {
        val t = dto.type.trim().uppercase()
        if (t.isBlank()) return true
        val loggerStatus = t == "DATALOGGER"
        return loggerStatus == _isDataLogger.value
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
        if (_isDataLogger.value) {
            loggerSessionStartedAtMs = System.currentTimeMillis()
        }
    }

    private fun recordTemperatureSample(temperatureC: Float) {
        if (temperatureC <= 0f) return
        val status = _kilnStatus.value

        if (_isDataLogger.value) {
            val start = loggerSessionStartedAtMs ?: System.currentTimeMillis().also {
                loggerSessionStartedAtMs = it
            }
            val elapsedMin = ((System.currentTimeMillis() - start) / 60_000f).coerceAtLeast(0f)
            val history = _temperatureHistory.value
            val last = history.lastOrNull()
            val shouldAdd = last == null ||
                kotlin.math.abs(last.elapsedMin - elapsedMin) >= 0.2f ||
                kotlin.math.abs(last.temperatureC - temperatureC) >= 1.5f
            if (shouldAdd) {
                _temperatureHistory.value = (history + TemperatureSample(elapsedMin, temperatureC))
                    .takeLast(400)
                // La curva persistida la escribe el ESP (Firebase/flash).
                // Acá solo espejo en memoria de la sesión activa para UI local.
                mirrorActiveRecordingSample(elapsedMin, temperatureC)
            }
            return
        }

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

    fun startLoggerRecording(name: String, comments: String) {
        if (!_isDataLogger.value) return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _recordingMessage.value = "Ingresá un nombre para la grabación"
            return
        }
        val mac = normalizeMac(currentMacAddress)
        if (mac.isBlank()) return
        if (_activeRecording.value?.isActive == true) {
            _recordingMessage.value = "Ya hay una grabación en curso"
            return
        }

        val sessionId = UUID.randomUUID().toString().replace("-", "").take(16)
        val session = LoggerRecordingSession(
            id = sessionId,
            deviceMac = mac,
            name = trimmed,
            comments = comments.trim(),
            startedAtMs = System.currentTimeMillis(),
            samples = emptyList()
        )

        viewModelScope.launch {
            // Igual que el horno: comando solo por SoftAP (cerca del ESP).
            if (blockIfApReconfiguring(LocalKilnAction.LOGGER_START)) {
                _recordingMessage.value = "Esperá unos segundos y reintentá GRABAR."
                return@launch
            }

            _sendingCommandPhase.value = "Iniciando grabación…"
            _sendingCommandDetail.value = "Conectando al DataLogger…"
            _isSendingCommand.value = true
            _recordingMessage.value = null
            try {
                if (!ensureLocalConnection()) {
                    _recordingMessage.value =
                        "Acercate al DataLogger y conectate a su SoftAP (SmartLink-…) para grabar."
                    return@launch
                }

                // Optimista: marcar UI en "grabando" antes del HTTP.
                // Si el ESP suelta SoftAP al responder, el socket puede fallar
                // aunque START_LOG ya se haya ejecutado.
                activateLoggerSession(session)

                _sendingCommandDetail.value = "Enviando GRABAR…"
                val cmd = kilnHttpClient.sendLoggerCommand(
                    cmd = "START_LOG",
                    name = trimmed,
                    comments = comments.trim(),
                    sessionId = sessionId
                )
                val resp = cmd.getOrNull()
                val httpOk = cmd.isSuccess && resp?.ok == true
                if (httpOk) {
                    val idFromEsp = resp?.sessionId?.takeIf { it.isNotBlank() } ?: sessionId
                    if (idFromEsp != session.id) {
                        activateLoggerSession(session.copy(id = idFromEsp))
                    }
                } else if (resp?.ok == false && resp.error == "already_logging") {
                    // Ya había sesión en el ESP: mantener UI activa
                    _recordingMessage.value = "El DataLogger ya estaba grabando."
                } else {
                    // Posible corte por handoff SoftAP: confirmar por /api/session o Firebase
                    val confirmed = confirmLoggerStarted(sessionId)
                    if (!confirmed) {
                        // No cerrar la sesión local: el ESP pudo haber arrancado igual.
                        // GUARDAR debe seguir disponible; el usuario puede sincronizar por SoftAP.
                        _recordingMessage.value =
                            "Sin confirmación HTTP del ESP. Si está grabando, usá GUARDAR cerca del SoftAP."
                    } else {
                        _recordingMessage.value =
                            "Grabando (el SoftAP se liberó; sesión confirmada)."
                    }
                }

                handOffToHomeWifiAfterLocalSession("START_LOG")
            } finally {
                _isSendingCommand.value = false
                _sendingCommandPhase.value = ""
                _sendingCommandDetail.value = ""
            }
        }
    }

    /** Confirma que el ESP quedó en GRABANDO tras un START con respuesta HTTP dudosa. */
    private suspend fun confirmLoggerStarted(expectedSessionId: String): Boolean {
        kilnHttpClient.getLoggerSession().getOrNull()?.let { s ->
            if (s.active || s.sessionId == expectedSessionId) return true
        }
        val uid = resolveFirebaseUserId() ?: return false
        val mac = normalizeMac(currentMacAddress)
        repeat(8) {
            kotlinx.coroutines.delay(1500)
            val live = firebaseManager.getRealtimeData(uid, mac).getOrNull() ?: return@repeat
            val status = live["status"]?.toString().orEmpty()
            val sid = live["sessionId"]?.toString().orEmpty()
            if (status.equals("GRABANDO", ignoreCase = true) ||
                sid.equals(expectedSessionId, ignoreCase = true)
            ) {
                return true
            }
        }
        return false
    }

    private fun activateLoggerSession(started: LoggerRecordingSession) {
        loggerSessionStartedAtMs = started.startedAtMs
        _temperatureHistory.value = emptyList()
        loggerSessionStartedAtMs = started.startedAtMs
        _activeRecording.value = started
        recordingStore.upsert(started)
        _recordingMessage.value =
            "Grabando. El DataLogger liberó el SoftAP; la app vuelve a WiFi de casa / Firebase."

        viewModelScope.launch {
            val uid = resolveFirebaseUserId() ?: return@launch
            firebaseManager.saveLoggerRecordingMeta(
                userId = uid,
                mac = started.deviceMac,
                recordingId = started.id,
                meta = mapOf(
                    "name" to started.name,
                    "comments" to started.comments,
                    "startedAtMs" to started.startedAtMs,
                    "active" to true,
                    "ended" to false,
                    "deviceMac" to started.deviceMac,
                    "type" to "DATALOGGER",
                    "intervalSec" to 180
                )
            )
        }
    }

    /** GUARDAR: SoftAP (STOP+CSV) preferido; Firebase solo si ya está completa. */
    fun stopLoggerRecording() {
        // Si la UI se perdió pero el ESP sigue GRABANDO, intentar recuperar sesión
        var session = _activeRecording.value
        if (session == null || !session.isActive) {
            val mac = normalizeMac(currentMacAddress)
            session = recordingStore.loadAll()
                .firstOrNull { normalizeMac(it.deviceMac) == mac && it.isActive }
            if (session != null) {
                _activeRecording.value = session
            }
        }
        val active = session ?: return
        if (!active.isActive) return

        viewModelScope.launch {
            if (blockIfApReconfiguring(LocalKilnAction.LOGGER_SAVE)) {
                _recordingMessage.value = "Esperá unos segundos y reintentá GUARDAR."
                return@launch
            }

            _sendingCommandPhase.value = "Guardando grabación…"
            _sendingCommandDetail.value = "Buscando datos…"
            _isSendingCommand.value = true
            _recordingMessage.value = null
            val uid = resolveFirebaseUserId()
            val mac = active.deviceMac
            try {
                // 1) SoftAP: STOP + CSV (fuente de verdad)
                _sendingCommandDetail.value = "Conectando al DataLogger…"
                if (tryLocalConnectionQuiet()) {
                    saveLoggerViaSoftAp(active, uid, mac)
                    return@launch
                }

                // 2) Sin SoftAP: solo si Firebase ya tiene la sesión cerrada y completa
                if (uid == null) {
                    _recordingMessage.value =
                        "Acercate al DataLogger (SmartLink-…) para GUARDAR. Sin SoftAP ni sesión Firebase."
                    return@launch
                }

                _sendingCommandDetail.value =
                    "Verificando si Firebase ya tiene la curva completa…"
                val assessment = firebaseManager
                    .assessLoggerRecordingCompleteness(uid, mac, active.id)
                    .getOrNull()

                if (assessment?.complete == true) {
                    val samples = assessment.samples.mapNotNull { mapToLoggerSample(it) }
                    finalizeLoggerSave(
                        session = active,
                        samples = samples,
                        uid = uid,
                        sourceLabel = "Firebase"
                    )
                    return@launch
                }

                val metaN = assessment?.metaSampleCount ?: 0
                val cloudN = assessment?.cloudSampleCount ?: 0
                val why = when {
                    assessment == null ->
                        "No se pudo leer la grabación en Firebase."
                    assessment.active || !assessment.ended ->
                        "La medición sigue activa en el DataLogger (o aún no cerró en la nube)."
                    cloudN == 0 ->
                        "Firebase no tiene muestras todavía."
                    cloudN < metaN ->
                        "Firebase incompleto: $cloudN / $metaN muestras."
                    else ->
                        "Los datos en Firebase no están completos."
                }

                // Último intento: SoftAP con diálogo (igual que el horno)
                _sendingCommandDetail.value = "Se necesita SoftAP para cerrar la sesión…"
                if (ensureLocalConnection()) {
                    saveLoggerViaSoftAp(active, uid, mac)
                    return@launch
                }

                _recordingMessage.value =
                    "$why Acercate al SoftAP (SmartLink-…) para GUARDAR."
            } finally {
                _isSendingCommand.value = false
                _sendingCommandPhase.value = ""
                _sendingCommandDetail.value = ""
            }
        }
    }

    /** STOP_LOG + history.csv + hand-off SoftAP + archivar. */
    private suspend fun saveLoggerViaSoftAp(
        session: LoggerRecordingSession,
        uid: String?,
        mac: String
    ) {
        _sendingCommandDetail.value = "Cerrando sesión en el ESP…"
        val localStop = kilnHttpClient.sendLoggerCommand("STOP_LOG")
        if (localStop.getOrNull()?.ok != true) {
            val stillActive = kilnHttpClient.getLoggerSession().getOrNull()?.active == true
            if (stillActive) {
                _recordingMessage.value =
                    "No se pudo GUARDAR en el ESP: ${localStop.exceptionOrNull()?.message ?: localStop.getOrNull()?.error ?: "error"}"
                return
            }
        }

        _sendingCommandDetail.value = "Descargando curva del ESP…"
        var samples = kilnHttpClient.getHistoryCsv()
            .getOrNull()
            ?.let { parseLoggerHistoryCsv(it) }
            .orEmpty()

        // Liberar SoftAP ya: archivar a Firebase no necesita AP
        handOffToHomeWifiAfterLocalSession("STOP_LOG")

        if (samples.isEmpty() && uid != null) {
            _sendingCommandDetail.value = "Completando desde Firebase…"
            samples = loadLoggerSamplesFromFirebase(uid, mac, session.id)
        }
        if (samples.isEmpty()) {
            samples = _activeRecording.value?.samples.orEmpty()
        }

        finalizeLoggerSave(
            session = session,
            samples = samples,
            uid = uid,
            sourceLabel = "ESP"
        )
    }

    /**
     * Intenta SoftAP sin abrir el diálogo de WiFi manual si falla
     * (así podemos caer a Firebase completo).
     */
    private suspend fun tryLocalConnectionQuiet(): Boolean {
        if (_canControlLocally.value) return true
        if (_dataSource.value == KilnDataSource.LOCAL_AP && isLocalHttpForCurrentDevice()) {
            kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
            _canControlLocally.value = true
            return true
        }
        if (isLocalHttpForCurrentDevice()) {
            startLocalMonitoring()
            refreshApInfo()
            kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
            _canControlLocally.value = true
            return true
        }
        if (KilnNetworkConnector.isSupported &&
            currentMacAddress.isNotEmpty() &&
            !KilnNetworkConnector.isInPostHandoffCooldown(KILN_AP_RECONFIG_WAIT_MS)
        ) {
            val deviceType = if (_isDataLogger.value) {
                SoftApDeviceType.DATALOGGER
            } else {
                SoftApDeviceType.KILN
            }
            val ssid = KilnNetworkConnector.ssidFromMac(currentMacAddress, deviceType)
            val pass = KilnNetworkConnector.passphraseFromMac(currentMacAddress)
            if (ssid != null && pass != null) {
                _isConnectingToAp.value = true
                _sendingCommandDetail.value = "Conectando al WiFi del dispositivo…"
                try {
                    val network = KilnNetworkConnector.connect(appContext, ssid, pass)
                    if (network != null) {
                        delay(500)
                        if (isLocalHttpForCurrentDevice()) {
                            startLocalMonitoring()
                            refreshApInfo()
                            kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
                            _canControlLocally.value = true
                            return true
                        }
                        KilnNetworkConnector.release(appContext)
                    }
                } finally {
                    _isConnectingToAp.value = false
                }
            }
        }
        return false
    }

    private suspend fun loadLoggerSamplesFromFirebase(
        uid: String,
        mac: String,
        recordingId: String
    ): List<LoggerRecordingSample> {
        return firebaseManager.getLoggerRecording(uid, mac, recordingId)
            .getOrNull()
            ?.second
            ?.mapNotNull { mapToLoggerSample(it) }
            .orEmpty()
    }

    private fun mapToLoggerSample(map: Map<String, Any?>): LoggerRecordingSample? {
        val elapsed = (map["elapsedMin"] as? Number)?.toFloat() ?: return null
        val temp = (map["temperatureC"] as? Number)?.toFloat() ?: return null
        val epoch = (map["epochMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
        return LoggerRecordingSample(elapsed, temp, epoch)
    }

    private suspend fun finalizeLoggerSave(
        session: LoggerRecordingSession,
        samples: List<LoggerRecordingSample>,
        uid: String?,
        sourceLabel: String
    ) {
        val ended = System.currentTimeMillis()
        val finished = session.copy(endedAtMs = ended, samples = samples)
        _activeRecording.value = null
        recordingStore.upsert(finished)

        if (uid != null) {
            firebaseManager.finishLoggerRecording(
                userId = uid,
                mac = session.deviceMac,
                recordingId = session.id,
                endedAtMs = ended,
                sampleCount = samples.size
            )
            val maxTemp = samples.maxOfOrNull { it.temperatureC }?.toInt() ?: 0
            val durationMin = samples.maxOfOrNull { it.elapsedMin }?.toInt() ?: 0
            val deviceName = kilnRepository.getKilnByMacAddress(session.deviceMac)?.name
                ?: session.name
            firebaseManager.upsertActivity(
                userId = uid,
                record = com.example.mysmartkiln.data.model.ActivityRecord(
                    id = session.id,
                    type = "DATALOGGER",
                    deviceMac = session.deviceMac,
                    deviceName = deviceName,
                    name = session.name,
                    comments = session.comments,
                    startedAtMs = session.startedAtMs,
                    endedAtMs = ended,
                    durationMin = durationMin,
                    maxTemperatureC = maxTemp,
                    sampleCount = samples.size,
                    complete = samples.isNotEmpty(),
                    source = sourceLabel,
                    recordingId = session.id
                )
            )
        }

        _recordingMessage.value = if (samples.isNotEmpty()) {
            "Guardado ($sourceLabel): ${finished.name} (${samples.size} muestras)"
        } else {
            "Sesión cerrada pero sin muestras ($sourceLabel)."
        }
    }

    fun clearRecordingMessage() {
        _recordingMessage.value = null
    }

    /** Solo espejo UI/local; no escribe Firebase (el ESP es la fuente). */
    private fun mirrorActiveRecordingSample(elapsedMin: Float, temperatureC: Float) {
        val session = _activeRecording.value ?: return
        if (!session.isActive) return
        val sample = LoggerRecordingSample(
            elapsedMin = elapsedMin,
            temperatureC = temperatureC
        )
        val updated = session.copy(samples = session.samples + sample)
        _activeRecording.value = updated
        recordingStore.appendSample(session.id, sample)
    }

    private fun parseLoggerHistoryCsv(csv: String): List<LoggerRecordingSample> {
        return csv.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("elapsed") && !it.startsWith("epoch") }
            .mapNotNull { line ->
                val p = line.split(';', ',', '\t')
                if (p.size < 2) return@mapNotNull null
                val elapsed = p[0].toFloatOrNull() ?: return@mapNotNull null
                val temp = p[1].toFloatOrNull() ?: return@mapNotNull null
                val epoch = when {
                    p.size >= 4 -> (p[3].toLongOrNull() ?: 0L) * 1000L
                    else -> 0L
                }
                LoggerRecordingSample(elapsedMin = elapsed, temperatureC = temp, epochMs = epoch)
            }
            .toList()
    }

    /** Descarga historial CSV del SoftAP (`/api/history.csv`) si hay enlace local. */
    fun fetchDeviceHistoryCsv() {
        viewModelScope.launch {
            _deviceHistoryLoading.value = true
            _deviceHistoryError.value = null
            try {
                kilnHttpClient.getHistoryCsv()
                    .onSuccess { csv ->
                        _deviceHistoryCsv.value = csv
                    }
                    .onFailure { err ->
                        _deviceHistoryError.value =
                            err.message ?: "No se pudo leer el historial del dispositivo"
                    }
            } finally {
                _deviceHistoryLoading.value = false
            }
        }
    }

    /** Evita duplicar activity al recibir FINALIZADO varias veces. */
    private var lastPublishedKilnActivityKey: String? = null

    /**
     * Archiva resumen + curva (samples + etapas) en local y Firebase.
     * [completedNormally]=true → FINALIZADO; false → STOP / cancelar interrupción.
     * Debe llamarse antes de clearProgramStartWallClock().
     *
     * @param firebaseStartMs inicio desde RTDB (`programStartTime`), si el wall-clock local se perdió.
     */
    private fun maybeArchiveKilnFiring(
        statusForUi: String,
        completedNormally: Boolean = true,
        firebaseStartMs: Long? = null
    ) {
        if (_isDataLogger.value) return
        val finishedOk = statusForUi.equals("FINALIZADO", ignoreCase = true)
        val stopped = statusForUi.equals("IDLE", ignoreCase = true) ||
            statusForUi.equals("DETENIDO", ignoreCase = true)
        if (!finishedOk && !(stopped && !completedNormally)) return
        // STOP sin haber tenido cocción activa: no archivar vacío.
        if (!completedNormally &&
            programStartedAtWallMs == null &&
            _temperatureHistory.value.isEmpty()
        ) {
            Log.d(TAG, "Archive KILN STOP omitido: sin sesión activa")
            return
        }
        val mac = normalizeMac(currentMacAddress)
        if (mac.isBlank()) {
            Log.w(TAG, "Archive KILN omitido: MAC vacío")
            return
        }

        val ended = System.currentTimeMillis()
        val history = _temperatureHistory.value.toList()
        val stages = _kilnProgram.value.stages.toList()
        val startMs = programStartedAtWallMs
            ?: firebaseStartMs?.takeIf { it > 0 }
            ?: history.maxOfOrNull { it.elapsedMin }?.let { elapsed ->
                (ended - (elapsed * 60_000f).toLong()).coerceAtMost(ended)
            }
            ?: ended

        val key = "${mac}_$startMs"
        if (lastPublishedKilnActivityKey == key) {
            Log.d(TAG, "Archive KILN ya publicado: $key")
            return
        }
        lastPublishedKilnActivityKey = key

        val programName = _kilnProgram.value.name.ifBlank { "Cocción" }
        val activityId = key

        viewModelScope.launch {
            val kiln = kilnRepository.getKilnByMacAddress(mac)
            val localSession = KilnFiringSession(
                id = activityId,
                deviceMac = mac,
                programName = programName,
                deviceName = kiln?.name.orEmpty(),
                startedAtMs = startMs,
                endedAtMs = ended,
                samples = history,
                stages = stages,
                complete = completedNormally,
                syncedToCloud = false
            )
            kilnFiringStore.upsert(localSession)
            Log.d(
                TAG,
                "Archive KILN local: $activityId complete=$completedNormally " +
                    "samples=${history.size} stages=${stages.size}"
            )

            val uid = resolveFirebaseUserId()
            if (uid == null) {
                Log.w(TAG, "Archive KILN: sin userId — queda local; se reintentará")
                KilnFiringCloudSync.scheduleRetry(appContext)
                return@launch
            }
            val uploaded = KilnFiringCloudSync.uploadSession(uid, localSession, firebaseManager)
            if (uploaded) {
                kilnFiringStore.markSynced(activityId)
                Log.d(TAG, "Activity KILN respaldo app→Firebase: $activityId")
            } else {
                Log.w(TAG, "Archive KILN cloud falló — reintento diferido")
                KilnFiringCloudSync.scheduleRetry(appContext)
            }
        }
    }

    private fun firebaseProgramStartMs(data: Map<String, Any?>): Long? {
        val raw = (data["programStartTime"] as? Number)?.toLong()?.takeIf { it > 0 }
            ?: return null
        // Solo epoch Unix real (~2020+). Históricamente el ESP publicaba millis()/1000
        // (uptime), que la app interpretaba como epoch → % del ciclo = 100%.
        val asSec = if (raw < 10_000_000_000L) raw else raw / 1000L
        if (asSec < 1_600_000_000L) return null
        return UserIdUtils.firebaseTimestampMs(raw)
    }

    /**
     * Elapsed reportado por el ESP en RTDB (misma lógica que display / SoftAP).
     * Preferir esto a reconstruir con wall-clock.
     */
    private fun firebaseElapsedMinutes(data: Map<String, Any?>): Int? {
        return (data["elapsedMinutes"] as? Number)?.toInt()?.takeIf { it >= 0 }
    }

    private fun firebaseRemainingMinutes(data: Map<String, Any?>): Int? {
        return (data["remainingMinutes"] as? Number)?.toInt()?.takeIf { it >= 0 }
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
        // Archivar FINALIZADO antes de cualquier return temprano (nombre vacío, preferLocal, etc.).
        if (statusForUi.equals("FINALIZADO", ignoreCase = true)) {
            maybeArchiveKilnFiring(statusForUi, firebaseStartMs = firebaseProgramStartMs(data))
        }

        if (!isConnected) {
            if (statusForUi.equals("FINALIZADO", ignoreCase = true) ||
                statusForUi.equals("IDLE", ignoreCase = true) ||
                statusForUi.equals("DETENIDO", ignoreCase = true)
            ) {
                clearProgramStartWallClock()
            }
            return
        }
        if (shouldPreferLocalProgramMetadata(programName, statusForUi)) {
            if (statusForUi.equals("FINALIZADO", ignoreCase = true) ||
                statusForUi.equals("IDLE", ignoreCase = true) ||
                statusForUi.equals("DETENIDO", ignoreCase = true)
            ) {
                clearProgramStartWallClock()
            }
            return
        }
        val name = programName.ifEmpty { _kilnProgram.value.name }
        if (name.isEmpty()) {
            if (statusForUi.equals("FINALIZADO", ignoreCase = true) ||
                statusForUi.equals("IDLE", ignoreCase = true) ||
                statusForUi.equals("DETENIDO", ignoreCase = true)
            ) {
                clearProgramStartWallClock()
            }
            return
        }
        noteRemoteProgramNameSeen(programName)

        when (statusForUi) {
            "IDLE", "DETENIDO", "FINALIZADO" -> {
                clearProgramStartWallClock()
            }
            "CALENTANDO" -> {
                if (programStartedAtWallMs == null) {
                    val espElapsed = firebaseElapsedMinutes(data)
                    val fbStart = firebaseProgramStartMs(data)
                    when {
                        pausedElapsedMin != null -> {
                            programStartedAtWallMs =
                                System.currentTimeMillis() - pausedElapsedMin!! * 60_000L
                            pausedElapsedMin = null
                        }
                        espElapsed != null -> {
                            // Alinear reloj local al elapsed del ESP (entre updates RTDB).
                            programStartedAtWallMs =
                                System.currentTimeMillis() - espElapsed * 60_000L
                        }
                        fbStart != null -> programStartedAtWallMs = fbStart
                        else -> ensureProgramStartWallClock()
                    }
                } else {
                    // Re-sincronizar si el ESP publica elapsed (evita drift al volver a ESTADO).
                    firebaseElapsedMinutes(data)?.let { espElapsed ->
                        programStartedAtWallMs =
                            System.currentTimeMillis() - espElapsed * 60_000L
                    }
                }
            }
            "PAUSADO", "INTERRUMPIDO" -> {
                val espElapsed = firebaseElapsedMinutes(data)
                if (espElapsed != null) {
                    pausedElapsedMin = espElapsed
                } else if (pausedElapsedMin == null && programStartedAtWallMs != null) {
                    pausedElapsedMin = ((System.currentTimeMillis() - programStartedAtWallMs!!) /
                        60_000L).toInt().coerceAtLeast(0)
                }
            }
        }

        val totalDur = computeTotalDurationMin(firebaseData = data)
        if (totalDur <= 0 && _kilnProgram.value.stages.isEmpty()) return

        val active = statusForUi in setOf("CALENTANDO", "PAUSADO", "INTERRUMPIDO")
        val elapsedFromEspPub = firebaseElapsedMinutes(data)
        val elapsedFromPauseFields = computeElapsedFromFirebaseEsp(data, statusForUi)
        val elapsedMin = when {
            // 1) ESP publica elapsedMinutes en cada telemetría → fuente de verdad
            elapsedFromEspPub != null && active -> elapsedFromEspPub
            elapsedFromEspPub != null &&
                (statusForUi == "PAUSADO" || statusForUi == "INTERRUMPIDO") ->
                elapsedFromEspPub
            elapsedFromPauseFields != null -> elapsedFromPauseFields
            statusForUi == "PAUSADO" || statusForUi == "INTERRUMPIDO" ->
                pausedElapsedMin ?: _kilnStatus.value.elapsedMinutes
            active && programStartedAtWallMs != null ->
                ((System.currentTimeMillis() - programStartedAtWallMs!!) / 60_000L)
                    .toInt().coerceAtLeast(0)
            else -> 0
        }
        val remainingFromEsp = firebaseRemainingMinutes(data)
        val remainingMin = when {
            remainingFromEsp != null && active -> remainingFromEsp
            totalDur > 0 -> (totalDur - elapsedMin).coerceAtLeast(0)
            else -> 0
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
            "IDLE", "DETENIDO", "FINALIZADO" -> {
                maybeArchiveKilnFiring(status)
                clearProgramStartWallClock()
            }
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
            // No bajar GRABANDO a IDLE: la UI de GUARDAR depende de este estado.
            statusRaw == "GRABANDO" -> "GRABANDO"
            // PAUSADO es decisión del usuario: conservarlo aunque el enlace
            // Firebase esté momentáneamente caído tras SoftAP/handoff.
            statusRaw == "PAUSADO" -> "PAUSADO"
            // CALENTANDO sin telemetría fresca no se muestra como activo.
            statusRaw == "CALENTANDO" -> "IDLE"
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
            var pollsSinceIdentityCheck = 0
            while (isActive) {
                val kilnReachable = kilnHttpClient.ping()

                if (!kilnReachable) {
                    consecutivePingFailures++
                    _canControlLocally.value = false

                    // Perfil B: handoff al primer ping fallido aunque el SSID
                    // siga reportando SoftAP del dispositivo (perfil A: >= 2 fallos).
                    val shouldHandOff =
                        consecutivePingFailures >= LOCAL_HANDOFF_PING_FAILURES &&
                            !_isSendingCommand.value &&
                            !_isUploadingProgram.value
                    if (shouldHandOff) {
                        if (_dataSource.value == KilnDataSource.LOCAL_AP) {
                            _dataSource.value = KilnDataSource.FIREBASE
                        }
                        // Soltamos la red pedida por WifiNetworkSpecifier para
                        // que Android vuelva solo a la WiFi de casa. Si nunca
                        // se pidió (API <29) es no-op.
                        KilnNetworkConnector.release(appContext, markHandoffCooldown = true)
                        // Firebase ya está enganchado: rompemos el bucle.
                        break
                    }
                    delay(LOCAL_POLL_INTERVAL_MS)
                    continue
                }

                consecutivePingFailures = 0
                pollsSinceIdentityCheck++
                if (pollsSinceIdentityCheck >= 8) {
                    pollsSinceIdentityCheck = 0
                    if (!isLocalHttpForCurrentDevice()) {
                        Log.w(TAG, "HTTP local dejó de coincidir con este dispositivo — handoff")
                        if (_dataSource.value == KilnDataSource.LOCAL_AP) {
                            _dataSource.value = KilnDataSource.FIREBASE
                        }
                        KilnNetworkConnector.release(appContext, markHandoffCooldown = true)
                        _canControlLocally.value = false
                        break
                    }
                }

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

    /** Paso 5: si el socket parece vivo pero los datos del ESP llevan >30s, forzar heal. */
    private fun startFirebaseWatchdog() {
        if (firebaseWatchdogJob?.isActive == true) return
        firebaseWatchdogJob = viewModelScope.launch {
            while (isActive && currentMacAddress.isNotEmpty()) {
                delay(FIREBASE_WATCHDOG_INTERVAL_MS)
                if (_dataSource.value == KilnDataSource.LOCAL_AP) continue

                val lastLive = lastFirebaseLiveDataMs
                if (lastLive == 0L) continue

                val staleMs = System.currentTimeMillis() - lastLive
                if (staleMs >= CONNECTION_TIMEOUT_MS && _kilnStatus.value.isConnected) {
                    updateReconnectGrace(_kilnStatus.value.espStatus, isFirebaseLive = false)
                    val grace = inReconnectGrace()
                    val liveAt = _kilnStatus.value.lastLiveTelemetryAtMs.let { prev ->
                        if (prev > 0L) prev else lastLive
                    }
                    _kilnStatus.value = _kilnStatus.value.copy(
                        isConnected = false,
                        isReconnecting = grace,
                        isRunning = false,
                        lastLiveTelemetryAtMs = liveAt
                    )
                    Log.w(TAG, "Telemetría stale (${staleMs}ms) — UI desconectada")
                }
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

        val snap = DeviceRealtimeSnapshot.parse(
            raw = data,
            expectedIsDataLogger = _isDataLogger.value
        )
        if (snap.mismatchesExpected(_isDataLogger.value)) {
            Log.w(
                TAG,
                "realtimeData.type=${snap.type} no corresponde al dispositivo " +
                    "(isDataLogger=${_isDataLogger.value}) — ignorando snapshot"
            )
            return
        }

        val temperature = snap.temperature
        val timestamp = snap.timestampMs
        val statusRaw = snap.status
        val programName = snap.label

        val currentTimeMs = System.currentTimeMillis()
        // Solo timestamp fresco del ESP cuenta como enlace vivo (no status/temp viejos).
        val isConnected =
            timestamp > 0L && (currentTimeMs - timestamp) < CONNECTION_TIMEOUT_MS
        if (isConnected) {
            lastFirebaseLiveDataMs = currentTimeMs
        }

        updateReconnectGrace(statusRaw, isConnected)
        var statusForUi = resolveStatusForUi(statusRaw, isConnected)
        statusForUi = applyCommandStatusHold(statusForUi)
        val isReconnecting = inReconnectGrace() && !isConnected
        val lastKnown = when {
            isConnected && statusRaw.uppercase() in PROCESS_STATUSES -> statusRaw.uppercase()
            isConnected && statusForUi.isNotBlank() -> statusForUi.uppercase()
            else -> _kilnStatus.value.lastKnownEspStatus
        }
        val liveAtMs = if (isConnected) {
            currentTimeMs
        } else {
            _kilnStatus.value.lastLiveTelemetryAtMs.let { prev ->
                if (prev > 0L) prev else lastFirebaseLiveDataMs
            }
        }

        val programRunningState = if (_isDataLogger.value) {
            ProgramStatusUtils.ProgramRunningState.UNKNOWN
        } else {
            ProgramStatusUtils.determineProgramState(
                firebaseData = data,
                programTotalDurationMinutes = _kilnProgram.value.stages
                    .takeIf { it.isNotEmpty() }
                    ?.sumOf { it.durationMinutes },
                isDataLogger = false
            )
        }

        val shouldUpdateRunning = if (_isDataLogger.value) {
            statusForUi.equals("GRABANDO", ignoreCase = true)
        } else if (!isConnected) {
            false
        } else when (programRunningState) {
            ProgramStatusUtils.ProgramRunningState.CONFIRMED_RUNNING,
            ProgramStatusUtils.ProgramRunningState.ESTIMATED_RUNNING -> true
            ProgramStatusUtils.ProgramRunningState.CONFIRMED_STOPPED,
            ProgramStatusUtils.ProgramRunningState.ESTIMATED_STOPPED -> false
            else -> programName.isNotEmpty() && statusForUi in ACTIVE_KILN_STATUSES
        }
        val isPaused = statusForUi == "PAUSADO"

        // Diálogo de interrupción: ver [updateInterruptedDialog].
        updateInterruptedDialog(statusForUi, liveLink = isConnected)

        if (isLocalAuthoritative) {
            // SoftAP/HTTP es la fuente de verdad: no pisar status ni flags de
            // proceso con un snapshot RTDB (puede ir atrasado varios segundos).
            _kilnStatus.value = _kilnStatus.value.copy(
                isConnected = isConnected,
                isReconnecting = isReconnecting,
                currentTemperature = temperature,
                lastKnownEspStatus = lastKnown.ifEmpty { _kilnStatus.value.lastKnownEspStatus },
                lastLiveTelemetryAtMs = liveAtMs
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
            espStatus = statusForUi,
            lastKnownEspStatus = lastKnown.ifEmpty { _kilnStatus.value.lastKnownEspStatus },
            lastLiveTelemetryAtMs = liveAtMs
        )
        if (currentMacAddress.isNotEmpty() && statusForUi.isNotBlank()) {
            KilnTelemetryCache.putFromFirebase(
                mac = currentMacAddress,
                status = statusForUi,
                temperature = temperature,
                timestampMs = timestamp,
                programName = programName.takeIf { it.isNotBlank() }
            )
        }

        if (_isDataLogger.value &&
            statusForUi.equals("GRABANDO", ignoreCase = true)
        ) {
            ensureActiveRecordingFromTelemetry(
                status = statusForUi,
                sessionId = snap.sessionId,
                programName = programName
            )
        }

        // Timing de cocción solo para hornos (ignora campos de logger)
        if (!_isDataLogger.value) {
            applyProgramTimingFromFirebase(
                data = data,
                statusForUi = statusForUi,
                programName = programName,
                isConnected = isConnected
            )
        }
        recordTemperatureSample(temperature)
    }

    private fun applyLocalStatus(dto: KilnStatusDto) {
        if (dto.error != null) {
            Log.w(TAG, "Status error from kiln: ${dto.error}")
            return
        }
        if (!statusMatchesCurrentDevice(dto)) {
            Log.w(
                TAG,
                "Status type=${dto.type} no corresponde al dispositivo abierto " +
                    "(isDataLogger=${_isDataLogger.value}) — ignorando"
            )
            return
        }
        val status = dto.status.trim().uppercase()
        val programName = dto.programName.trim()
        val hasActiveProgram = programName.isNotEmpty() || dto.profileLoaded
        val isPaused = status == "PAUSADO"
        val isActive = if (_isDataLogger.value) {
            status == "GRABANDO" || dto.logging
        } else {
            hasActiveProgram && status in ACTIVE_KILN_STATUSES
        }

        updateInterruptedDialog(status, liveLink = true)

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
        lastFirebaseLiveDataMs = System.currentTimeMillis()
        // SoftAP es fuente de verdad: si coincide el hold, lo liberamos.
        if (status.isNotBlank()) {
            applyCommandStatusHold(status)
        }
        _kilnStatus.value = _kilnStatus.value.copy(
            isConnected = true,
            isReconnecting = false,
            currentTemperature = dto.temperature,
            espStatus = status,
            lastKnownEspStatus = if (status in PROCESS_STATUSES) {
                status
            } else {
                _kilnStatus.value.lastKnownEspStatus
            },
            lastLiveTelemetryAtMs = lastFirebaseLiveDataMs,
            isPaused = isPaused,
            isRunning = isActive,
            totalStages = if (preferLocalProgram) {
                _kilnProgram.value.stages.size.coerceAtLeast(dto.numStages)
            } else {
                dto.numStages
            }
        )
        if (currentMacAddress.isNotEmpty() && status.isNotBlank()) {
            KilnTelemetryCache.putFromLocal(
                mac = currentMacAddress,
                status = status,
                temperature = dto.temperature,
                programName = dto.programName.takeIf { it.isNotBlank() }
            )
        }

        if (_isDataLogger.value &&
            (status == "GRABANDO" || dto.logging)
        ) {
            ensureActiveRecordingFromTelemetry(
                status = status,
                sessionId = dto.sessionId.takeIf { it.isNotBlank() },
                programName = programName
            )
        }

        if (preferLocalProgram) {
            // Igual archivar si el SoftAP ya reporta FINALIZADO (antes se salía sin hacerlo).
            if (status.equals("FINALIZADO", ignoreCase = true)) {
                maybeArchiveKilnFiring(status)
                clearProgramStartWallClock()
            }
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
        if (_dataSource.value == KilnDataSource.LOCAL_AP && isLocalHttpForCurrentDevice()) {
            kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
            _canControlLocally.value = true
            return true
        }
        if (isLocalHttpForCurrentDevice()) {
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
            val deviceType = if (_isDataLogger.value) {
                SoftApDeviceType.DATALOGGER
            } else {
                SoftApDeviceType.KILN
            }
            val ssid = KilnNetworkConnector.ssidFromMac(currentMacAddress, deviceType)
            val pass = KilnNetworkConnector.passphraseFromMac(currentMacAddress)
            if (ssid != null && pass != null) {
                _isConnectingToAp.value = true
                _sendingCommandDetail.value = "Conectando al WiFi del dispositivo…"
                try {
                    val network = KilnNetworkConnector.connect(appContext, ssid, pass)
                    if (network != null) {
                        // El AP recién aceptó la asociación; dejamos margen para
                        // que el ESP termine de enrutar antes del primer ping.
                        delay(500)
                        if (isLocalHttpForCurrentDevice()) {
                            startLocalMonitoring()
                            refreshApInfo()
                            kilnHttpClient.getStatus()
                                .onSuccess { applyLocalStatus(it) }
                            _canControlLocally.value = true
                            return true
                        }
                        Log.w(TAG, "Network disponible pero no es este dispositivo / ping falló")
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

    fun clearLocalActionError() {
        _localActionError.value = null
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
     * El ESP apaga SoftAP en paralelo (~8–10 s perfil C) por si el usuario entró a mano.
     */
    private fun handOffToHomeWifiAfterLocalSession(reason: String) {
        localPollJob?.cancel()
        localPollJob = null
        _canControlLocally.value = false
        if (_dataSource.value == KilnDataSource.LOCAL_AP) {
            _dataSource.value = KilnDataSource.FIREBASE
        }
        KilnNetworkConnector.release(appContext, markHandoffCooldown = true)
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
                    holdCommandStatusAfterLocalSuccess(cmd)
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
            "STOP" -> {
                // Antes de borrar el wall-clock: archivar como Interrumpida.
                maybeArchiveKilnFiring("IDLE", completedNormally = false)
                clearProgramStartWallClock()
            }
        }

        _kilnStatus.value = when (cmd) {
            "START", "RESUME" -> _kilnStatus.value.copy(
                isRunning = true,
                isPaused = false,
                espStatus = "CALENTANDO",
                lastKnownEspStatus = "CALENTANDO"
            )
            "PAUSE" -> _kilnStatus.value.copy(
                isRunning = true,
                isPaused = true,
                espStatus = "PAUSADO",
                lastKnownEspStatus = "PAUSADO"
            )
            "STOP" -> _kilnStatus.value.copy(
                isRunning = false,
                isPaused = false,
                espStatus = "IDLE"
            )
            else -> _kilnStatus.value
        }
        if (cmd in setOf("START", "RESUME", "PAUSE", "STOP") && currentMacAddress.isNotEmpty()) {
            val status = when (cmd) {
                "PAUSE" -> "PAUSADO"
                "STOP" -> "IDLE"
                else -> "CALENTANDO"
            }
            KilnTelemetryCache.putFromLocal(
                mac = currentMacAddress,
                status = status,
                temperature = _kilnStatus.value.currentTemperature
            )
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
            val paused = _kilnStatus.value.isPaused ||
                _kilnStatus.value.espStatus.equals("PAUSADO", ignoreCase = true)
            sendLocalCommand(if (paused) "RESUME" else "START")
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
            // Ocultar mientras se envía; el latch solo si RESUME OK (si falla,
            // el próximo snapshot INTERRUMPIDO vuelve a mostrar el diálogo).
            _showInterruptedDialog.value = false
            val ok = sendLocalCommand("RESUME")
            interruptedDialogAcked = ok
            if (!ok) {
                Log.w(TAG, "RESUME falló — se podrá reabrir el diálogo de interrupción")
            }
        }
    }

    /**
     * Acción "Cancelar" del diálogo de interrupción: limpia el run_state
     * en el ESP (manda STOP) y el horno vuelve a IDLE.
     */
    fun cancelInterruptedProgram() {
        viewModelScope.launch {
            _showInterruptedDialog.value = false
            val ok = sendLocalCommand("STOP")
            interruptedDialogAcked = ok
            if (!ok) {
                Log.w(TAG, "STOP (cancel interrupción) falló — se podrá reabrir el diálogo")
            }
        }
    }

    /**
     * Muestra el diálogo solo ante INTERRUMPIDO con enlace vivo, y no lo
     * reabre si el usuario ya decidió Reanudar/Cancelar en este episodio.
     */
    private fun updateInterruptedDialog(status: String, liveLink: Boolean) {
        if (_isDataLogger.value) {
            _showInterruptedDialog.value = false
            return
        }
        val interrupted = status.equals("INTERRUMPIDO", ignoreCase = true)
        if (!interrupted) {
            interruptedDialogAcked = false
            _showInterruptedDialog.value = false
            return
        }
        if (interruptedDialogAcked || !liveLink) {
            _showInterruptedDialog.value = false
            return
        }
        _showInterruptedDialog.value = true
    }

    private fun holdCommandStatusAfterLocalSuccess(cmd: String) {
        val status = when (cmd) {
            "PAUSE" -> "PAUSADO"
            "START", "RESUME" -> "CALENTANDO"
            "STOP" -> "IDLE"
            else -> return
        }
        heldCommandStatus = status
        // Cubre handoff SoftAP + publish Firebase (perfil C ~8–10 s) con margen.
        heldCommandUntilMs = System.currentTimeMillis() + 12_000L
        Log.d(TAG, "Hold status local '$status' hasta que Firebase coincida")
    }

    private fun clearCommandStatusHold() {
        heldCommandStatus = null
        heldCommandUntilMs = 0L
    }

    /**
     * Evita que un snapshot RTDB viejo (p. ej. CALENTANDO tras PAUSE) pise la UI
     * hasta que el ESP publique el nuevo status o expire el hold.
     */
    private fun applyCommandStatusHold(statusForUi: String): String {
        val held = heldCommandStatus ?: return statusForUi
        if (System.currentTimeMillis() >= heldCommandUntilMs) {
            clearCommandStatusHold()
            return statusForUi
        }
        val remote = statusForUi.trim().uppercase()
        if (remote == held) {
            clearCommandStatusHold()
            return statusForUi
        }
        Log.d(TAG, "Hold activo: UI='$held' (Firebase aún '$remote')")
        return held
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
                _localActionError.value =
                    "No se pudo conectar al horno para cargar «$programName». Acercate al SoftAP e intentá de nuevo."
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
                    _sendingCommandDetail.value = "Confirmando en el horno…"
                    val status = kilnHttpClient.getStatus().getOrNull()
                    val remoteName = status?.programName?.trim().orEmpty()
                    if (!programNamesMatch(programName, remoteName)) {
                        Log.e(
                            TAG,
                            "Profile POST ok pero status.programName='$remoteName' ≠ '$programName'"
                        )
                        _localActionError.value =
                            "El horno no confirmó el programa «$programName». " +
                                "En el display sigue el anterior. Reintentá cerca del SoftAP."
                        return@onSuccess
                    }
                    pendingProgramUploadId = null
                    _canControlLocally.value = true
                    noteRemoteProgramNameSeen(programName)
                    programRepository.getProgramById(programId)?.let { prog ->
                        lastSyncedProgramUpdatedAt = prog.updatedAt
                        markProgramSyncedWithKiln(programId, prog.updatedAt)
                    }
                    status?.let { applyLocalStatus(it) }
                    handOffToHomeWifiAfterLocalSession("profile")
                    Log.d(TAG, "Profile uploaded for $programId ($programName)")
                }
                .onFailure {
                    Log.e(TAG, "Profile upload failed for $programId: ${it.message}")
                    _localActionError.value =
                        "No se pudo cargar «$programName» en el horno: ${it.message ?: "error"}"
                }
        } finally {
            _isUploadingProgram.value = false
            _isSendingCommand.value = false
            _sendingCommandPhase.value = ""
            _sendingCommandDetail.value = ""
        }
    }

    /** El ESP guarda el nombre en un buffer de ~31 caracteres. */
    private fun programNamesMatch(local: String, remote: String): Boolean {
        val a = local.trim()
        val b = remote.trim()
        if (b.isEmpty()) return false
        if (a.equals(b, ignoreCase = true)) return true
        val max = 31
        return a.take(max).equals(b.take(max), ignoreCase = true)
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
