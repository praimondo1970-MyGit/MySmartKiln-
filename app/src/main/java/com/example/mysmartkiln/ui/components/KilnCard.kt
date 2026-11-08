package com.example.mysmartkiln.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.awaitCancellation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.data.local.KilnTelemetryCache
import com.example.mysmartkiln.data.local.SoftApDeviceType
import com.example.mysmartkiln.data.local.entity.KilnEntity
import com.example.mysmartkiln.data.model.DeviceRealtimeSnapshot
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme
import com.example.mysmartkiln.utils.AppOperationMode
import com.example.mysmartkiln.utils.KilnConnectionState
import com.example.mysmartkiln.utils.UserIdUtils
import com.example.mysmartkiln.utils.ConnectivityManager
import com.example.mysmartkiln.utils.DeviceKind
import com.example.mysmartkiln.utils.KilnNetworkConnector
import com.example.mysmartkiln.utils.KilnOfflineSession
import com.example.mysmartkiln.utils.LinkLostFormat
import com.example.mysmartkiln.utils.LocalOperationModeController
import com.example.mysmartkiln.utils.ProgramStatusUtils
import com.example.mysmartkiln.utils.WifiUtils

/** Timeout sin updates del ESP → DESCONECTADO en la tarjeta (alineado al detalle). */
private const val ESP_CARD_TELEMETRY_STALE_MS = LinkLostFormat.STALE_MS

private val PROCESS_CHIP_STATUSES = setOf(
    "CALENTANDO", "PAUSADO", "INTERRUMPIDO", "FINALIZADO", "GRABANDO", "RUNNING", "PAUSED"
)

private enum class KilnListChipKind {
    BAKING,
    PAUSED,
    INTERRUPTED,
    FINISHED,
    CONNECTED,
    OFFLINE
}

/** Canal de enlace del horno (independiente del chip de proceso). */
private enum class KilnLinkMode {
    CHECKING,
    LOCAL,
    REMOTO
}

@Composable
fun KilnCard(
    kiln: KilnEntity,
    /** [telemetryFresh] = true si el ESP publica en Firebase dentro del umbral STALE_MS. */
    onClick: (telemetryFresh: Boolean) -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onLoadProgramClick: () -> Unit = {},
    connectionState: KilnConnectionState = KilnConnectionState.OFFLINE,
    currentUserId: String? = null,
    programName: String? = null,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    var isSwiped by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    
    val context = LocalContext.current
    // Seed: solo preview (temp/nombre). Nunca marcar “conectado” hasta Firebase/SoftAP.
    val seed = remember(kiln.macAddress) { KilnTelemetryCache.get(kiln.macAddress) }
    
    // Estado para la temperatura en tiempo real y conexión
    var firebaseTemperature by remember(kiln.macAddress) {
        mutableStateOf(seed?.temperature)
    }
    var firebaseProgramName by remember(kiln.macAddress) {
        mutableStateOf(seed?.programName)
    }
    var isWifiConnected by remember(kiln.macAddress) { mutableStateOf(false) }
    var hasProgram by remember { mutableStateOf(false) }
    var programRunningState by remember {
        mutableStateOf(ProgramStatusUtils.ProgramRunningState.UNKNOWN)
    }
    var firebaseStatus by remember(kiln.macAddress) {
        mutableStateOf(seed?.status?.takeIf { it.isNotBlank() })
    }
    var lastOnlineTimestamp by remember(kiln.macAddress) { mutableStateOf(0L) }
    var temperature by remember { mutableStateOf<Float?>(null) }
    var hasReceivedFirebaseSnapshot by remember(kiln.macAddress) { mutableStateOf(false) }
    
    // Estado para el timer de verificación de conexión
    var lastDataReceived by remember(kiln.macAddress) {
        mutableStateOf(System.currentTimeMillis())
    }
    var connectionCheckTicker by remember { mutableStateOf(0L) }
    val isDataLogger = remember(kiln) { DeviceKind.isDataLogger(kiln) }
    val operationMode by LocalOperationModeController.current.mode.collectAsState()
    
    // ConnectivityManager para validar conectividad real
    val connectivityManager = remember { ConnectivityManager(context) }
    val isConnectedToInternet by connectivityManager.isConnectedToInternet.collectAsState()
    val isFirebaseReachable by connectivityManager.isFirebaseReachable.collectAsState()
    
    // Arranque pesimista: desconectado hasta que RTDB/conectividad digan lo contrario.
    var updatedConnectionState by remember {
        mutableStateOf(KilnConnectionState.OFFLINE)
    }
    
    // SINCRONIZAR updatedConnectionState cuando connectionState cambia externamente
    LaunchedEffect(connectionState) {
        android.util.Log.d("KilnCard", "🔄 ConnectionState cambió externamente: $connectionState")
        updatedConnectionState = connectionState
    }
    
    // Temperatura desde Firebase / caché de telemetría
    LaunchedEffect(firebaseTemperature) {
        temperature = firebaseTemperature
        android.util.Log.d("KilnCard", "🔄 Temperatura actualizada: $temperature")
    }
    
    val isConnected = when (updatedConnectionState) {
        KilnConnectionState.WIFI_ONLINE ->
            isWifiConnected && isConnectedToInternet && isFirebaseReachable
        KilnConnectionState.WIFI_NO_INTERNET,
        KilnConnectionState.WIFI_FIREBASE_DOWN,
        KilnConnectionState.OFFLINE -> false
    }
    
    android.util.Log.d("KilnCard", "🔍 DEBUG CONNECTION STATE:")
    android.util.Log.d("KilnCard", "  - connectionState: $connectionState")
    android.util.Log.d("KilnCard", "  - updatedConnectionState: $updatedConnectionState")
    android.util.Log.d("KilnCard", "  - isWifiConnected: $isWifiConnected")
    android.util.Log.d("KilnCard", "  - isConnectedToInternet: $isConnectedToInternet")
    android.util.Log.d("KilnCard", "  - isFirebaseReachable: $isFirebaseReachable")
    android.util.Log.d("KilnCard", "  - isConnected: $isConnected")
    
    val connectionType = when (updatedConnectionState) {
        KilnConnectionState.WIFI_ONLINE,
        KilnConnectionState.WIFI_NO_INTERNET,
        KilnConnectionState.WIFI_FIREBASE_DOWN -> "wifi"
        KilnConnectionState.OFFLINE -> "offline"
    }
    
    val currentFirebaseProgramName = firebaseProgramName
    val finalProgramName = when {
        currentFirebaseProgramName != null && currentFirebaseProgramName.isNotEmpty() -> {
            android.util.Log.d("KilnCard", "📋 Usando nombre del programa desde Firebase: '$currentFirebaseProgramName'")
            currentFirebaseProgramName
        }
        programName != null && programName.isNotEmpty() -> programName
        else -> {
            android.util.Log.d("KilnCard", "📋 No hay nombre del programa disponible")
            null
        }
    }
    
    LaunchedEffect(updatedConnectionState) {
        android.util.Log.d("KilnCard", "🌐 Iniciando monitoreo de conectividad")
        connectivityManager.startConnectivityMonitoring()
        awaitCancellation()
    }
    
    LaunchedEffect(isConnectedToInternet, isFirebaseReachable) {
        updatedConnectionState = when {
            !isConnectedToInternet -> KilnConnectionState.WIFI_NO_INTERNET
            !isFirebaseReachable -> KilnConnectionState.WIFI_FIREBASE_DOWN
            else -> KilnConnectionState.WIFI_ONLINE
        }
        android.util.Log.d(
            "KilnCard",
            "🔄 ConnectionState actualizado por conectividad: $updatedConnectionState " +
                "(Internet: $isConnectedToInternet, Firebase: $isFirebaseReachable)"
        )
    }
    
    // Cleanup al desmontar el componente
    DisposableEffect(Unit) {
        onDispose {
            connectivityManager.stopConnectivityMonitoring()
        }
    }
    
    // Timer: recomponer y marcar desconectado por timestamp ESP (no solo por
    // “último evento Firebase”, que puede re-dispararse con datos viejos).
    // Tick al cambiar modo (Local↔Online) para refrescar SSID SoftAP al toque.
    // En LOCAL: intervalo corto para detectar SoftAP; en ONLINE también al inicio.
    LaunchedEffect(
        kiln.macAddress,
        operationMode,
        kiln.offlineOnly
    ) {
        val preferLocalTick =
            !isDataLogger && (
                operationMode == AppOperationMode.LOCAL ||
                    kiln.offlineOnly ||
                    KilnOfflineSession.prefersLocal(kiln.macAddress)
            )
        connectionCheckTicker = System.currentTimeMillis()
        val intervalMs = if (preferLocalTick) 2_000L else 5_000L
        while (isActive) {
            delay(intervalMs)
            val now = System.currentTimeMillis()
            connectionCheckTicker = now
            val espTs = lastOnlineTimestamp
            isWifiConnected = espTs > 0L && (now - espTs) < LinkLostFormat.STALE_MS
            if (!isWifiConnected && espTs > 0L) {
                android.util.Log.w(
                    "KilnCard",
                    "Telemetría stale (${now - espTs}ms) — chip SIN NUBE"
                )
            }
        }
    }
    
    // Observar temperatura y estado de conexión desde Firebase.
    // get() inmediato + listener: una sola lectura de RealtimeData basta para
    // decidir REMOTO vivo (timestamp ESP < STALE); no esperar solo al WebSocket.
    LaunchedEffect(kiln.macAddress, currentUserId) {
        val firebaseManager = com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager()

        val userIdToUse = kiln.userId.takeIf {
            it.isNotBlank() && it != "test_user_123"
        } ?: currentUserId.orEmpty().takeIf {
            it.isNotBlank() && it != "test_user_123"
        }
        if (userIdToUse.isNullOrBlank()) {
            android.util.Log.w("KilnCard", "Sin userId válido — no se observa Firebase")
            isWifiConnected = false
            return@LaunchedEffect
        }

        fun applyRealtime(data: Map<String, Any?>?) {
            lastDataReceived = System.currentTimeMillis()
            hasReceivedFirebaseSnapshot = true
            if (data == null) {
                firebaseTemperature = null
                firebaseProgramName = null
                isWifiConnected = false
                return
            }
            val snap = DeviceRealtimeSnapshot.parse(
                raw = data,
                expectedIsDataLogger = DeviceKind.isDataLogger(kiln)
            )
            if (snap.mismatchesExpected(DeviceKind.isDataLogger(kiln))) {
                android.util.Log.w(
                    "KilnCard",
                    "realtime type=${snap.type} ≠ dispositivo — ignorando"
                )
                return
            }
            KilnTelemetryCache.putFromFirebase(
                mac = kiln.macAddress,
                status = snap.status,
                temperature = snap.temperature,
                timestampMs = snap.timestampMs,
                programName = snap.label.takeIf { it.isNotEmpty() }
            )
            val cached = KilnTelemetryCache.get(kiln.macAddress)
            firebaseTemperature = cached?.temperature ?: snap.temperature
            val timestamp = cached?.timestampMs?.takeIf { it > 0L } ?: snap.timestampMs
            if (timestamp > 0L) {
                lastOnlineTimestamp = timestamp
            }
            val currentTimeMs = System.currentTimeMillis()
            isWifiConnected = lastOnlineTimestamp > 0L &&
                (currentTimeMs - lastOnlineTimestamp) < LinkLostFormat.STALE_MS
            connectionCheckTicker = currentTimeMs
            firebaseStatus = cached?.status?.takeIf { it.isNotBlank() }
                ?: snap.status.ifBlank { null }
            firebaseProgramName = cached?.programName
                ?: snap.label.takeIf { it.isNotEmpty() }
            try {
                programRunningState = ProgramStatusUtils.determineProgramState(
                    firebaseData = data,
                    programTotalDurationMinutes = null,
                    isDataLogger = DeviceKind.isDataLogger(kiln)
                )
                hasProgram = when (programRunningState) {
                    ProgramStatusUtils.ProgramRunningState.CONFIRMED_RUNNING,
                    ProgramStatusUtils.ProgramRunningState.ESTIMATED_RUNNING -> true
                    ProgramStatusUtils.ProgramRunningState.CONFIRMED_STOPPED,
                    ProgramStatusUtils.ProgramRunningState.ESTIMATED_STOPPED,
                    ProgramStatusUtils.ProgramRunningState.UNKNOWN -> false
                }
            } catch (e: Exception) {
                android.util.Log.e("KilnCard", "Error determineProgramState: ${e.message}")
                hasProgram = snap.status == "GRABANDO" ||
                    snap.status == "CALENTANDO" ||
                    snap.status == "PAUSADO"
                programRunningState = ProgramStatusUtils.ProgramRunningState.UNKNOWN
            }
            android.util.Log.d(
                "KilnCard",
                "temp=${firebaseTemperature} status=${snap.status} " +
                    "ageMs=${if (timestamp > 0L) currentTimeMs - timestamp else -1} " +
                    "live=$isWifiConnected"
            )
        }

        // Disparo paralelo: get() no espera a que el listener “calibre”.
        launch {
            firebaseManager.getRealtimeData(userIdToUse, kiln.macAddress)
                .onSuccess { applyRealtime(it) }
                .onFailure {
                    android.util.Log.w("KilnCard", "getRealtimeData: ${it.message}")
                }
        }

        try {
            firebaseManager.observeRealtimeData(userIdToUse, kiln.macAddress).collect { data ->
                applyRealtime(data)
            }
        } catch (e: Exception) {
            android.util.Log.e("KilnCard", "💥 ERROR en Firebase: ${e.message}")
            firebaseTemperature = null
            firebaseProgramName = null
            isWifiConnected = false
        }
    }
    
    val swipeOffset by animateFloatAsState(
        targetValue = if (isSwiped) -160f else 0f, // Desplazamiento de 160dp para mostrar ambos botones (editar + eliminar)
        animationSpec = tween(durationMillis = 200),
        label = "swipe"
    )

    val activityStatusText = when (firebaseStatus) {
        "running", "CALENTANDO" -> "Horneando"
        "paused", "PAUSADO" -> "Pausa"
        "stopped", "DETENIDO" -> "Detenido"
        "finished", "FINALIZADO" -> "Finalizado"
        "INTERRUMPIDO" -> "Interrumpido"
        "idle", "IDLE" -> "Inactivo"
        null -> "Inactivo"
        else -> "Inactivo"
    }
    // Telemetría fresca solo tras confirmación de esta sesión (Firebase o SoftAP).
    // Seed de caché NO cuenta como “conectado” al abrir la app.
    val nowMs = if (connectionCheckTicker > 0L) connectionCheckTicker else System.currentTimeMillis()
    val telemetryAgeMs = if (lastOnlineTimestamp > 0L) {
        nowMs - lastOnlineTimestamp
    } else {
        Long.MAX_VALUE
    }
    val localHoldKeepsChip = KilnTelemetryCache.get(kiln.macAddress)?.let { e ->
        e.fromLocalCommand &&
            nowMs - e.updatedAtWallMs < 12_000L &&
            e.status.trim().uppercase() in PROCESS_CHIP_STATUSES
    } == true
    val expectedSoftApSsid = remember(kiln.macAddress, isDataLogger) {
        KilnNetworkConnector.ssidFromMac(
            kiln.macAddress,
            if (isDataLogger) SoftApDeviceType.DATALOGGER else SoftApDeviceType.KILN
        )
    }
    val currentWifiSsid = remember(connectionCheckTicker) {
        WifiUtils.getCurrentWifiSSID(context)
    }
    val onThisDeviceSoftAp =
        !expectedSoftApSsid.isNullOrBlank() &&
            !currentWifiSsid.isNullOrBlank() &&
            currentWifiSsid.equals(expectedSoftApSsid, ignoreCase = true)
    val isLocalLink = onThisDeviceSoftAp || localHoldKeepsChip
    val firebaseLinkLive =
        hasReceivedFirebaseSnapshot &&
            lastOnlineTimestamp > 0L &&
            telemetryAgeMs < ESP_CARD_TELEMETRY_STALE_MS
    // SoftAP cuenta para temp/estado; el punto REMOTO solo mira Firebase.
    val isTelemetryFresh = isLocalLink || firebaseLinkLive
    // Label = política Ajustes (inmediato). No depende de SoftAP ni RTDB.
    val linkMode = when {
        isDataLogger -> null
        operationMode == AppOperationMode.LOCAL -> KilnLinkMode.LOCAL
        else -> KilnLinkMode.REMOTO
    }
    // LOCAL→verde: SoftAP vivo. REMOTO→verde: solo RTDB fresco (no SoftAP residual
    // tras salir de Local, que dejaba el punto verde “como si siguiera local”).
    val linkLive = when (linkMode) {
        KilnLinkMode.LOCAL -> isLocalLink
        KilnLinkMode.REMOTO -> firebaseLinkLive
        else -> false
    }
    // Chip de proceso / estado del horno (como antes).
    val listChipKind = when {
        !isTelemetryFresh -> KilnListChipKind.OFFLINE
        isDataLogger -> KilnListChipKind.CONNECTED
        activityStatusText == "Horneando" -> KilnListChipKind.BAKING
        activityStatusText == "Pausa" -> KilnListChipKind.PAUSED
        activityStatusText == "Interrumpido" -> KilnListChipKind.INTERRUPTED
        activityStatusText == "Finalizado" -> KilnListChipKind.FINISHED
        else -> KilnListChipKind.CONNECTED
    }
    // Tap: nube fresca O SoftAP local → no mostrar diálogo SIN NUBE.
    val cloudOrLocalOk = isTelemetryFresh || isLocalLink
    val temperatureLabel = if (temperature != null) "${temperature?.toInt()}°C" else "--°C"
    val cardHeight = if (kilnColors.isEsp) 96.dp else 235.dp
    val cardCorner = if (kilnColors.isEsp) 12.dp else 16.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(cardHeight)
    ) {
        KilnSwipeActions(
            onEdit = onEditClick,
            onDelete = onDeleteClick,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .requiredHeight(cardHeight)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(cardHeight)
                .offset(x = swipeOffset.dp)
                .shadow(
                    elevation = if (kilnColors.isEsp) 0.dp else 4.dp,
                    shape = RoundedCornerShape(cardCorner)
                )
                .background(
                    color = kilnColors.cardBackground,
                    shape = RoundedCornerShape(cardCorner)
                )
                .border(
                    width = 1.dp,
                    color = kilnColors.cardBorder,
                    shape = RoundedCornerShape(cardCorner)
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            offsetX = 0f
                        },
                        onDragEnd = {
                            if (offsetX > -10f) {
                                isSwiped = false
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            offsetX += dragAmount
                            val newOffset = swipeOffset + dragAmount

                            if (isSwiped && dragAmount > 0) {
                                isSwiped = false
                            } else if (newOffset <= 0f && newOffset >= -160f) {
                                isSwiped = newOffset < -10f
                            }
                        }
                    )
                }
                .clickable {
                    if (isSwiped) {
                        isSwiped = false
                    } else {
                        onClick(cloudOrLocalOk)
                    }
                }
                .padding(if (kilnColors.isEsp) 12.dp else 16.dp)
        ) {
            if (kilnColors.isEsp) {
                KilnCardEspContent(
                    kilnName = kiln.name,
                    kilnId = "ID: ${kiln.macAddress.takeLast(8).uppercase()}",
                    temperatureLabel = temperatureLabel,
                    chipKind = listChipKind,
                    isDataLogger = isDataLogger,
                    linkMode = linkMode,
                    linkLive = linkLive
                )
            } else {
                KilnCardClassicContent(
                    kilnName = kiln.name,
                    temperatureLabel = temperatureLabel,
                    connectionType = connectionType,
                    isConnected = isConnected,
                    updatedConnectionState = updatedConnectionState,
                    lastOnlineTimestamp = lastOnlineTimestamp,
                    finalProgramName = finalProgramName,
                    hasProgram = hasProgram,
                    activityStatusText = activityStatusText,
                    onLoadProgramClick = onLoadProgramClick
                )
            }
        }
    }
}

@Composable
private fun KilnCardEspContent(
    kilnName: String,
    kilnId: String,
    temperatureLabel: String,
    chipKind: KilnListChipKind,
    isDataLogger: Boolean,
    linkMode: KilnLinkMode? = null,
    linkLive: Boolean = false
) {
    val kilnColors = LocalKilnColors.current
    // Horno: llama. Otros dispositivos (DataLogger, etc.): icono de capas (ex-Programas).
    val leadingIcon = if (isDataLogger) {
        R.drawable.icon_programs
    } else {
        R.drawable.icon_flame
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(id = leadingIcon),
            contentDescription = null,
            tint = kilnColors.accent,
            modifier = Modifier.size(36.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = kilnName,
                    color = kilnColors.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = kilnId,
                    color = kilnColors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_thermometer),
                        contentDescription = null,
                        tint = kilnColors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = temperatureLabel,
                        color = kilnColors.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        maxLines = 1
                    )
                }
                // DataLogger: chip de estado aquí (posición original).
                // Horno: el chip va en la columna derecha alineada al chevron.
                if (isDataLogger || linkMode == null) {
                    KilnListStatusChip(
                        kind = chipKind,
                        isDataLogger = isDataLogger,
                        modifier = Modifier.offset(x = 10.dp)
                    )
                }
            }
        }

            if (linkMode != null) {
                // Horno: ambas burbujas con el mismo borde derecho, justo antes del `>`.
                Box(
                    modifier = Modifier.fillMaxHeight()
                ) {
                    KilnLinkModeIndicator(
                        mode = linkMode,
                        linkLive = linkLive,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                    KilnListStatusChip(
                        kind = chipKind,
                        isDataLogger = false,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            Icon(
                painter = painterResource(id = R.drawable.icon_arrow_right),
                contentDescription = null,
                tint = kilnColors.muted,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.icon_arrow_right),
                contentDescription = null,
                tint = kilnColors.muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun KilnListStatusChip(
    kind: KilnListChipKind,
    isDataLogger: Boolean = false,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    val transition = rememberInfiniteTransition(label = "pauseBlink")
    val blinkAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pauseAlpha"
    )
    val pauseAlpha = if (kind == KilnListChipKind.PAUSED) blinkAlpha else 1f

    val style = when (kind) {
        KilnListChipKind.BAKING -> ChipStyle(
            bg = kilnColors.accent,
            fg = kilnColors.onAccent,
            label = "HORNEANDO",
            showFlame = true
        )
        KilnListChipKind.PAUSED -> ChipStyle(
            bg = kilnColors.warning.copy(alpha = 0.22f),
            fg = kilnColors.warning,
            label = "PAUSA",
            showFlame = false
        )
        KilnListChipKind.INTERRUPTED -> ChipStyle(
            bg = kilnColors.danger.copy(alpha = 0.22f),
            fg = kilnColors.danger,
            label = "INTERRUMPIDO",
            showFlame = false
        )
        KilnListChipKind.FINISHED -> ChipStyle(
            bg = kilnColors.success.copy(alpha = 0.22f),
            fg = kilnColors.success,
            label = "FINALIZADO",
            showFlame = false
        )
        KilnListChipKind.CONNECTED -> if (isDataLogger) {
            ChipStyle(
                bg = kilnColors.success.copy(alpha = 0.22f),
                fg = kilnColors.success,
                label = "CONECTADO",
                showFlame = false
            )
        } else {
            ChipStyle(
                bg = kilnColors.muted.copy(alpha = 0.22f),
                fg = kilnColors.title,
                label = "INACTIVO",
                showFlame = false
            )
        }
        KilnListChipKind.OFFLINE -> ChipStyle(
            bg = kilnColors.muted.copy(alpha = 0.22f),
            fg = kilnColors.muted,
            label = if (isDataLogger) "DESCONECTADO" else "SIN NUBE",
            showFlame = false,
            showWifi = true
        )
    }

    Box(
        modifier = modifier
            .alpha(pauseAlpha)
            .clip(RoundedCornerShape(18.dp))
            .background(style.bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            when {
                style.showFlame -> {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_flame),
                        contentDescription = null,
                        tint = style.fg,
                        modifier = Modifier.size(12.dp)
                    )
                }
                style.showWifi -> {
                    Icon(
                        painter = painterResource(id = R.drawable.icon_wifi),
                        contentDescription = null,
                        tint = style.fg,
                        modifier = Modifier.size(12.dp)
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(style.fg, CircleShape)
                    )
                }
            }
            Text(
                text = style.label,
                color = style.fg,
                fontWeight = if (kind == KilnListChipKind.BAKING) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * Canal vigente (LOCAL / REMOTO). Círculo verde solo si el enlace está vivo;
 * si Remoto quedó sin publishes (router off) → gris + chip SIN NUBE abajo.
 */
@Composable
private fun KilnLinkModeIndicator(
    mode: KilnLinkMode,
    linkLive: Boolean,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    val activeGreen = Color(0xFF10B981)
    val dotColor = if (linkLive) activeGreen else kilnColors.muted
    val labelColor = if (linkLive) kilnColors.title else kilnColors.muted

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        when (mode) {
            KilnLinkMode.CHECKING -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = kilnColors.accent
                )
                Text(
                    text = "…",
                    color = kilnColors.muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
            KilnLinkMode.LOCAL,
            KilnLinkMode.REMOTO -> {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(dotColor, CircleShape)
                )
                Text(
                    text = if (mode == KilnLinkMode.LOCAL) "LOCAL" else "REMOTO",
                    color = labelColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

private data class ChipStyle(
    val bg: Color,
    val fg: Color,
    val label: String,
    val showFlame: Boolean,
    val showWifi: Boolean = false
)

@Composable
private fun KilnCardClassicContent(
    kilnName: String,
    temperatureLabel: String,
    connectionType: String,
    isConnected: Boolean,
    updatedConnectionState: KilnConnectionState,
    lastOnlineTimestamp: Long,
    finalProgramName: String?,
    hasProgram: Boolean,
    activityStatusText: String,
    onLoadProgramClick: () -> Unit
) {
    val kilnColors = LocalKilnColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = kilnName,
            style = MaterialTheme.typography.titleMedium.copy(
                color = kilnColors.title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(35.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = kilnColors.accent.copy(alpha = 0.15f))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.etapa_temp),
                    contentDescription = "Temperature",
                    tint = kilnColors.accent,
                    modifier = Modifier.requiredSize(24.dp)
                )
            }

            Text(
                text = temperatureLabel,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = kilnColors.body,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(35.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = kilnColors.accent.copy(alpha = 0.15f))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_wifi),
                    contentDescription = "WiFi connection status",
                    tint = kilnColors.accent,
                    modifier = Modifier.requiredSize(24.dp)
                )
            }

            val timeSinceLastOnline = calculateTimeSinceLastOnline(lastOnlineTimestamp)
            val displayText = when {
                connectionType == "wifi" && isConnected -> "Conectado"
                updatedConnectionState == KilnConnectionState.WIFI_NO_INTERNET -> "Sin internet"
                updatedConnectionState == KilnConnectionState.WIFI_FIREBASE_DOWN -> "Sin nube"
                connectionType == "wifi" && !isConnected && lastOnlineTimestamp > 0 ->
                    "Visto hace $timeSinceLastOnline"
                else -> "Desconectado"
            }

            Text(
                text = displayText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = kilnColors.body,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable {
                onLoadProgramClick()
            }
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(35.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = kilnColors.accent.copy(alpha = 0.15f))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_programs),
                    contentDescription = "Program",
                    tint = kilnColors.accent,
                    modifier = Modifier.requiredSize(24.dp)
                )
            }

            val programDisplayText = if (finalProgramName != null && finalProgramName.isNotEmpty()) {
                finalProgramName
            } else if (hasProgram) {
                "Programa activo"
            } else {
                "Sin programa"
            }

            Text(
                text = programDisplayText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = kilnColors.body,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .requiredSize(35.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color = kilnColors.accent.copy(alpha = 0.15f))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_flame),
                    contentDescription = "Activity Status",
                    tint = kilnColors.accent,
                    modifier = Modifier.requiredSize(24.dp)
                )
            }

            Text(
                text = activityStatusText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = kilnColors.body,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


/**
 * Calcula el tiempo transcurrido desde la última conexión
 * @param timestamp Timestamp en milisegundos de la última conexión
 * @return String formateado con el tiempo transcurrido
 */
private fun calculateTimeSinceLastOnline(timestamp: Long): String {
    if (timestamp == 0L) return "N/A"
    
    val currentTimeMs = System.currentTimeMillis()
    val elapsedMs = currentTimeMs - timestamp
    
    // Convertir a segundos
    val elapsedSeconds = elapsedMs / 1000
    
    return when {
        elapsedSeconds < 60 -> "${elapsedSeconds}s"
        elapsedSeconds < 3600 -> {
            val minutes = elapsedSeconds / 60
            "${minutes}m"
        }
        elapsedSeconds < 86400 -> {
            val hours = elapsedSeconds / 3600
            val minutes = (elapsedSeconds % 3600) / 60
            if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
        }
        else -> {
            val days = elapsedSeconds / 86400
            "${days}d"
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KilnCardPreview() {
    val context = LocalContext.current
    val opController = remember {
        com.example.mysmartkiln.utils.OperationModeController(context)
    }
    MySmartKilnTheme {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalOperationModeController provides opController
        ) {
            KilnCard(
                kiln = KilnEntity(
                    macAddress = "A4CF128856E8",
                    bleMacAddress = "A4:CF:12:88:56:E8",
                    name = "MiHorno",
                    manufacturer = "AFRA",
                    volume = "25",
                    userId = "preview_user",
                    deviceType = "KILN"
                )
            )
        }
    }
}
