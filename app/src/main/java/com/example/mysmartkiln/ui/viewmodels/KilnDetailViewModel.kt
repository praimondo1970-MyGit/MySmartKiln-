package com.example.mysmartkiln.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager
import com.example.mysmartkiln.data.local.KilnApInfoDto
import com.example.mysmartkiln.data.local.KilnDataSource
import com.example.mysmartkiln.data.local.KilnHttpClient
import com.example.mysmartkiln.data.local.KilnProgramUploader
import com.example.mysmartkiln.data.local.KilnStatusDto
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

data class KilnStatus(
    val isConnected: Boolean = false,
    val currentTemperature: Float = 0f,
    val targetTemperature: Float = 0f,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val espStatus: String = "",
    val currentStage: Int = 0,
    val totalStages: Int = 0,
    val elapsedTime: String = "0h 00min",
    val remainingTime: String = "0h 00min",
    val totalTime: String = "0h 00min",
    val rampRate: Float = 0f,
    val maintenanceTime: String = "0min",
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

class KilnDetailViewModel(
    private val programRepository: ProgramRepository,
    private val appContext: Context
) : ViewModel() {

    private val _kilnStatus = MutableStateFlow(KilnStatus())
    val kilnStatus: StateFlow<KilnStatus> = _kilnStatus.asStateFlow()

    private val _kilnProgram = MutableStateFlow(KilnProgram())
    val kilnProgram: StateFlow<KilnProgram> = _kilnProgram.asStateFlow()

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

    private var pendingProgramUploadId: String? = null

    private val firebaseManager = FirebaseRealtimeManager()
    private val kilnHttpClient = KilnHttpClient(appContext)
    private var currentMacAddress: String = ""
    private var firebaseJob: Job? = null
    private var localPollJob: Job? = null

    private val userId: String
        get() {
            val email = FirebaseAuth.getInstance().currentUser?.email
            return if (email != null) UserIdUtils.getCurrentUserId(email) else "test_user_123"
        }

    companion object {
        private const val TAG = "KilnDetailViewModel"
        private const val CONNECTION_TIMEOUT_MS = 30_000L
        private const val LOCAL_POLL_INTERVAL_MS = 1_500L
        private const val LOCAL_PROBE_ATTEMPTS = 6
        private const val LOCAL_PROBE_DELAY_MS = 350L
        private val ACTIVE_KILN_STATUSES = setOf(
            "CALENTANDO", "PAUSADO", "FINALIZADO"
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
                checkRunningProgram()
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
            detectAndStartMonitoring()
            retryPendingProgramUpload()
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
        if (WifiUtils.isOnSmartKilnNetwork(appContext)) {
            _dataSource.value = KilnDataSource.LOCAL_AP
        }
        repeat(LOCAL_PROBE_ATTEMPTS) {
            if (kilnHttpClient.ping()) {
                startLocalMonitoring()
                kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
                refreshApInfo()
                return true
            }
            delay(LOCAL_PROBE_DELAY_MS)
        }
        if (WifiUtils.isOnSmartKilnNetwork(appContext)) {
            startLocalMonitoring()
            return true
        }
        if (_dataSource.value == KilnDataSource.LOCAL_AP) {
            _dataSource.value = KilnDataSource.DISCONNECTED
        }
        return false
    }

    private fun stopMonitoring() {
        firebaseJob?.cancel()
        firebaseJob = null
        localPollJob?.cancel()
        localPollJob = null
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
                val onKilnSsid = WifiUtils.isOnSmartKilnNetwork(appContext)

                if (!kilnReachable) {
                    consecutivePingFailures++
                    _canControlLocally.value = false

                    // Si el horno tiró su AP (tras START/PAUSE/STOP), bastan
                    // unos pocos pings fallidos para soltar la UI a Firebase
                    // aunque Android todavía reporte el SSID antiguo.
                    val shouldHandOff = !onKilnSsid || consecutivePingFailures >= 2
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
        // Idempotente: si ya hay un listener vivo (mismo MAC, no cancelado), no
        // re-engancha. Permite que el listener corra en paralelo al poll local.
        if (firebaseJob?.isActive == true) return

        if (_dataSource.value == KilnDataSource.DISCONNECTED) {
            _dataSource.value = KilnDataSource.FIREBASE
        }

        firebaseJob = viewModelScope.launch {
            try {
                firebaseManager.observeRealtimeData(userId, macAddress).collect { data ->
                    applyFirebaseSnapshot(data)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error observing realtime data: ${e.message}", e)
            }
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
                _kilnStatus.value = _kilnStatus.value.copy(
                    isConnected = false,
                    currentTemperature = 0f
                )
            }
            return
        }

        val temperature = when (val v = data["temperature"]) {
            is Number -> v.toFloat()
            is String -> v.toFloatOrNull() ?: 0f
            else -> 0f
        }
        val timestamp = when (val v = data["timestamp"]) {
            is Number -> v.toLong()
            is String -> v.toLongOrNull() ?: 0L
            else -> 0L
        }
        val statusRaw = (data["status"] as? String)?.trim()?.uppercase().orEmpty()
        val programName = (data["programName"] as? String)?.trim().orEmpty()

        val currentTimeMs = System.currentTimeMillis()
        val isConnected = timestamp > 0 &&
            (currentTimeMs - timestamp) < CONNECTION_TIMEOUT_MS

        val programRunningState = ProgramStatusUtils.determineProgramState(
            hasBleConnection = false,
            bleStatus = null,
            bleProgramName = null,
            firebaseData = data,
            programTotalDurationMinutes = _kilnProgram.value.stages
                .takeIf { it.isNotEmpty() }
                ?.sumOf { it.durationMinutes }
        )

        val shouldUpdateRunning = when (programRunningState) {
            ProgramStatusUtils.ProgramRunningState.CONFIRMED_RUNNING,
            ProgramStatusUtils.ProgramRunningState.ESTIMATED_RUNNING -> true
            ProgramStatusUtils.ProgramRunningState.CONFIRMED_STOPPED,
            ProgramStatusUtils.ProgramRunningState.ESTIMATED_STOPPED -> false
            else -> programName.isNotEmpty() && statusRaw in ACTIVE_KILN_STATUSES
        }
        val isPaused = statusRaw == "PAUSADO"

        if (isLocalAuthoritative) {
            // Solo refrescamos el indicador de "conectado" y la temperatura
            // remota, sin tocar programName/status que el local ya gobierna.
            return
        }

        if (programName.isNotEmpty() && programName != _kilnProgram.value.name) {
            _kilnProgram.value = _kilnProgram.value.copy(name = programName)
        }

        _kilnStatus.value = _kilnStatus.value.copy(
            isConnected = isConnected,
            currentTemperature = temperature,
            isRunning = shouldUpdateRunning,
            isPaused = isPaused,
            espStatus = statusRaw
        )
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

        if (programName.isNotEmpty() && programName != _kilnProgram.value.name) {
            _kilnProgram.value = _kilnProgram.value.copy(name = programName)
        }

        _kilnStatus.value = _kilnStatus.value.copy(
            isConnected = true,
            currentTemperature = dto.temperature,
            espStatus = status,
            isPaused = isPaused,
            isRunning = isActive,
            totalStages = dto.numStages
        )

        val durationMin = when {
            dto.programDurationMin > 0 -> dto.programDurationMin
            _kilnProgram.value.stages.isNotEmpty() ->
                _kilnProgram.value.stages.sumOf { it.durationMinutes }
            else -> 0
        }
        if (durationMin > 0) {
            val durationStr = formatMinutes(durationMin)
            val elapsedStr = formatMinutes(dto.elapsedMinutes.coerceAtLeast(0))
            val remainingStr = formatMinutes(
                if (dto.remainingMinutes > 0) dto.remainingMinutes
                else (durationMin - dto.elapsedMinutes).coerceAtLeast(0)
            )
            val stageLabel = if (dto.currentStage > 0 && dto.numStages > 0) {
                "${dto.currentStage}/${dto.numStages}"
            } else if (dto.numStages > 0) {
                "0/${dto.numStages}"
            } else {
                _kilnProgram.value.stage
            }
            _kilnStatus.value = _kilnStatus.value.copy(
                totalTime = durationStr,
                elapsedTime = elapsedStr,
                remainingTime = remainingStr,
                elapsedMinutes = dto.elapsedMinutes.coerceAtLeast(0),
                currentStage = dto.currentStage.coerceAtLeast(0),
                totalStages = dto.numStages.coerceAtLeast(_kilnStatus.value.totalStages)
            )
            _kilnProgram.value = _kilnProgram.value.copy(
                duration = durationStr,
                elapsed = elapsedStr,
                remaining = remainingStr,
                stage = stageLabel
            )
        }
    }

    private fun formatMinutes(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return "${hours}h ${String.format("%02d", mins)}min"
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
        if (KilnNetworkConnector.isSupported && currentMacAddress.isNotEmpty()) {
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
        // Solo pedimos abrir Settings si el atajo automático no es viable
        // (API <29 o el usuario no aprobó el diálogo del sistema).
        _shouldShowManualWifiDialog.value = !KilnNetworkConnector.isSupported ||
            !WifiUtils.isOnSmartKilnNetwork(appContext)
        return false
    }

    fun clearManualWifiDialogRequest() {
        _shouldShowManualWifiDialog.value = false
    }

    private suspend fun sendLocalCommand(cmd: String): Boolean {
        // El diálogo modal con el círculo girando se muestra durante TODO el
        // proceso (conexión al AP + envío HTTP + lectura de status). El título
        // (phase) se queda fijo durante toda la operación; solo el subtítulo
        // (detail) cambia para informar de la sub-fase.
        val title = when (cmd) {
            "START" -> "Iniciando horno…"
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
        _kilnStatus.value = when (cmd) {
            "START" -> _kilnStatus.value.copy(
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
                espStatus = "FINALIZADO"
            )
            else -> _kilnStatus.value
        }
    }

    fun stopKiln() {
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

    fun modifyProgram() {
        Log.d(TAG, "Modify program — seleccionar otro programa")
    }

    fun loadSelectedProgram(programId: String) {
        Log.d(TAG, "loadSelectedProgram: $programId")
        viewModelScope.launch {
            try {
                val programEntity = programRepository.getProgramById(programId) ?: run {
                    Log.e(TAG, "Programa no encontrado: $programId")
                    return@launch
                }
                val stageEntities = programRepository.getStagesByProgram(programId).first()
                if (stageEntities.isEmpty()) {
                    Log.e(TAG, "Programa sin etapas: $programId")
                    return@launch
                }

                var previousTemp = 0f
                val programStages = stageEntities.map { stageEntity ->
                    val temperature = stageEntity.temperature.toFloatOrNull() ?: 0f
                    val speed = stageEntity.speed.toFloatOrNull() ?: 0f
                    val maintenance = stageEntity.maintenance.toIntOrNull() ?: 0
                    val tempIncrement = temperature - previousTemp
                    val rampMinutes = if (speed > 0) (tempIncrement / speed).toInt() else 0
                    val totalDuration = rampMinutes + maintenance
                    previousTemp = temperature
                    ProgramStage(
                        stageNumber = stageEntity.stageNumber,
                        targetTemp = temperature,
                        rampRate = speed,
                        maintenanceMinutes = maintenance,
                        durationMinutes = totalDuration,
                        isCompleted = false
                    )
                }

                val totalMinutes = programStages.sumOf { it.durationMinutes }
                val totalHours = totalMinutes / 60
                val totalMins = totalMinutes % 60
                val totalDuration = "${totalHours}h ${String.format("%02d", totalMins)}min"
                val maxTemp = programStages.maxOfOrNull { it.targetTemp } ?: 0f

                _kilnProgram.value = KilnProgram(
                    name = programEntity.name,
                    duration = totalDuration,
                    remaining = totalDuration,
                    elapsed = "0h 00min",
                    stage = "0/${programStages.size}",
                    stages = programStages
                )

                _kilnStatus.value = _kilnStatus.value.copy(
                    isRunning = false,
                    targetTemperature = maxTemp,
                    totalStages = programStages.size,
                    currentStage = 0,
                    totalTime = totalDuration,
                    remainingTime = totalDuration,
                    elapsedTime = "0h 00min"
                )

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
                    kilnHttpClient.getStatus().onSuccess { applyLocalStatus(it) }
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
