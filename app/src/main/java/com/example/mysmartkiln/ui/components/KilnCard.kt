package com.example.mysmartkiln.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import com.example.mysmartkiln.data.local.entity.KilnEntity
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme
import com.example.mysmartkiln.ble.HybridConnectionState
import com.example.mysmartkiln.utils.UserIdUtils
import com.example.mysmartkiln.utils.ConnectivityManager
import com.example.mysmartkiln.utils.ProgramStatusUtils

@Composable
fun KilnCard(
    kiln: KilnEntity,
    onClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onLoadProgramClick: () -> Unit = {}, // Para navegar a selección de programa
    bleTemperature: Float? = null,  // Temperatura desde BLE (tiene prioridad)
    isBleConnected: Boolean = false,  // Si está conectado por BLE
    bleStatus: String? = null,  // Estado del programa desde BLE ("CALENTANDO", "PAUSADO", etc.)
    connectionState: HybridConnectionState = HybridConnectionState.OFFLINE,  // Estado híbrido de conexión
    currentUserId: String? = null,  // UID del usuario actual para Firebase
    programName: String? = null,  // Nombre del programa cargado en el ESP32
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    var isSwiped by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    
    val context = LocalContext.current
    
    // Estado para la temperatura en tiempo real y conexión
    var firebaseTemperature by remember { mutableStateOf<Float?>(null) }
    var firebaseProgramName by remember { mutableStateOf<String?>(null) } // Nombre del programa desde Firebase
    var isWifiConnected by remember { mutableStateOf(false) }
    var hasProgram by remember { mutableStateOf(false) }
    var programRunningState by remember { mutableStateOf(ProgramStatusUtils.ProgramRunningState.UNKNOWN) }
    var firebaseStatus by remember { mutableStateOf<String?>(null) } // Estado del horno desde Firebase
    var lastOnlineTimestamp by remember { mutableStateOf(0L) }
    var temperature by remember { mutableStateOf<Float?>(null) } // Temperatura final calculada
    
    // Estado para el timer de verificación de conexión
    var lastDataReceived by remember { mutableStateOf(System.currentTimeMillis()) }
    var connectionCheckTicker by remember { mutableStateOf(0L) }
    
    // ConnectivityManager para validar conectividad real
    val connectivityManager = remember { ConnectivityManager(context) }
    val isConnectedToInternet by connectivityManager.isConnectedToInternet.collectAsState()
    val isFirebaseReachable by connectivityManager.isFirebaseReachable.collectAsState()
    
    // Estado para el connectionState actualizado basado en conectividad
    var updatedConnectionState by remember { mutableStateOf(connectionState) }
    
    // SINCRONIZAR updatedConnectionState cuando connectionState cambia externamente
    LaunchedEffect(connectionState) {
        android.util.Log.d("KilnCard", "🔄 ConnectionState cambió externamente: $connectionState")
        updatedConnectionState = connectionState
    }
    
    // Cache local de última temperatura BLE válida para evitar caídas temporales
    var cachedBleTemperature by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(bleTemperature) {
        if (bleTemperature != null) {
            cachedBleTemperature = bleTemperature
        }
    }
    
    // Actualizar temperatura final en tiempo real cuando cambien los valores
    LaunchedEffect(bleTemperature, cachedBleTemperature, firebaseTemperature) {
        // Guardar valores locales para evitar problemas de smart cast
        val currentBle = bleTemperature
        val currentCached = cachedBleTemperature
        val currentFirebase = firebaseTemperature
        
        // Prioridad: BLE actual > BLE cacheado > Firebase (0°C es válido cuando el horno está frío)
        temperature = when {
            currentBle != null -> currentBle
            currentCached != null -> currentCached
            currentFirebase != null -> currentFirebase
            else -> null
        }
        android.util.Log.d("KilnCard", "🔄 Temperatura actualizada: $temperature (BLE: $currentBle, Cache: $currentCached, Firebase: $currentFirebase)")
    }
    
    // Calcular estado de conexión basado en el estado híbrido y conectividad real
    val isConnected = when (updatedConnectionState) {
        HybridConnectionState.BLE_CONNECTED -> true
        HybridConnectionState.WIFI_ONLINE -> isWifiConnected && isConnectedToInternet && isFirebaseReachable
        HybridConnectionState.WIFI_NO_INTERNET -> false
        HybridConnectionState.WIFI_FIREBASE_DOWN -> false
        HybridConnectionState.OFFLINE -> false
    }
    
    // Debug logs para entender el problema
    android.util.Log.d("KilnCard", "🔍 DEBUG CONNECTION STATE:")
    android.util.Log.d("KilnCard", "  - connectionState: $connectionState")
    android.util.Log.d("KilnCard", "  - updatedConnectionState: $updatedConnectionState")
    android.util.Log.d("KilnCard", "  - isWifiConnected: $isWifiConnected")
    android.util.Log.d("KilnCard", "  - isConnectedToInternet: $isConnectedToInternet")
    android.util.Log.d("KilnCard", "  - isFirebaseReachable: $isFirebaseReachable")
    android.util.Log.d("KilnCard", "  - isConnected: $isConnected")
    
    // Determinar el tipo de conexión para mostrar el icono correcto
    val connectionType = when (updatedConnectionState) {
        HybridConnectionState.BLE_CONNECTED -> "ble"
        HybridConnectionState.WIFI_ONLINE -> "wifi"
        HybridConnectionState.WIFI_NO_INTERNET -> "wifi"
        HybridConnectionState.WIFI_FIREBASE_DOWN -> "wifi"
        HybridConnectionState.OFFLINE -> "offline"
    }
    
    // Determinar el nombre del programa a mostrar: priorizar BLE si hay conexión BT, sino usar Firebase
    // Guardar en variable local para evitar problemas con smart cast en propiedades delegadas
    val currentFirebaseProgramName = firebaseProgramName
    val finalProgramName = when {
        connectionType == "ble" && isConnected && programName != null && programName.isNotEmpty() -> {
            android.util.Log.d("KilnCard", "📋 Usando nombre del programa desde BLE: '$programName'")
            programName
        }
        currentFirebaseProgramName != null && currentFirebaseProgramName.isNotEmpty() -> {
            android.util.Log.d("KilnCard", "📋 Usando nombre del programa desde Firebase: '$currentFirebaseProgramName'")
            currentFirebaseProgramName
        }
        else -> {
            android.util.Log.d("KilnCard", "📋 No hay nombre de programa disponible")
            null
        }
    }
    
    // Iniciar monitoreo de conectividad cuando no hay BT
    LaunchedEffect(updatedConnectionState) {
        if (updatedConnectionState != HybridConnectionState.BLE_CONNECTED) {
            android.util.Log.d("KilnCard", "🌐 Iniciando monitoreo de conectividad (no hay BT)")
            connectivityManager.startConnectivityMonitoring()
        } else {
            android.util.Log.d("KilnCard", "🔵 Deteniendo monitoreo de conectividad (BT activo)")
            connectivityManager.stopConnectivityMonitoring()
        }
        
        // Cleanup cuando se cancela el LaunchedEffect
        awaitCancellation()
    }
    
    // Actualizar connectionState basado en conectividad real (solo para modo WiFi)
    LaunchedEffect(isConnectedToInternet, isFirebaseReachable) {
        // Solo actualizar si NO estamos en modo BLE
        if (updatedConnectionState != HybridConnectionState.BLE_CONNECTED) {
            updatedConnectionState = when {
                !isConnectedToInternet -> HybridConnectionState.WIFI_NO_INTERNET
                !isFirebaseReachable -> HybridConnectionState.WIFI_FIREBASE_DOWN
                else -> HybridConnectionState.WIFI_ONLINE
            }
            android.util.Log.d("KilnCard", "🔄 ConnectionState actualizado por conectividad: $updatedConnectionState (Internet: $isConnectedToInternet, Firebase: $isFirebaseReachable)")
        }
    }
    
    // Cleanup al desmontar el componente
    DisposableEffect(Unit) {
        onDispose {
            connectivityManager.stopConnectivityMonitoring()
        }
    }
    
    // Timer separado para verificar desconexión activamente
    LaunchedEffect(connectionCheckTicker) {
        val connectionCheckInterval = 5000L // Verificar cada 5 segundos
        val FIREBASE_TIMEOUT_MS = 15000L // 15 segundos sin datos = desconectado
        
        while (isActive) {
            delay(connectionCheckInterval)
            val currentTime = System.currentTimeMillis()
            val timeSinceLastData = currentTime - lastDataReceived
            
            android.util.Log.d("KilnCard", "⏰ Verificación de conexión: ${timeSinceLastData}ms desde último dato")
            
            // Si han pasado más de 15 segundos sin datos, marcar como desconectado
            if (timeSinceLastData > FIREBASE_TIMEOUT_MS) {
                android.util.Log.w("KilnCard", "⚠️ Sin datos por ${timeSinceLastData}ms - marcando como desconectado")
                android.util.Log.d("KilnCard", "🔄 Actualizando isWifiConnected: false")
                android.util.Log.d("KilnCard", "🔄 Preservando lastOnlineTimestamp: $lastOnlineTimestamp")
                isWifiConnected = false
                // NO resetear lastOnlineTimestamp - mantener el último valor válido
                android.util.Log.d("KilnCard", "✅ Variables actualizadas - isWifiConnected: $isWifiConnected, lastOnlineTimestamp: $lastOnlineTimestamp")
            }
            
            // Forzar recomposición
            connectionCheckTicker = System.currentTimeMillis()
        }
    }
    
    // Observar temperatura y estado de conexión desde Firebase
    LaunchedEffect(kiln.macAddress, currentUserId) {
        val firebaseManager = com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager()
        
        // Ruta donde el ESP publica: la del horno al darlo de alta, no la sesión si difieren
        val userIdToUse = kiln.userId.takeIf { it.isNotBlank() }
            ?: currentUserId.orEmpty().ifBlank { "test_user_123" }
        android.util.Log.d("KilnCard", "🔍 Observando temperatura - userId: $userIdToUse, macAddress: ${kiln.macAddress}")
        android.util.Log.d("KilnCard", "📍 Ruta Firebase: users/$userIdToUse/kilns/${kiln.macAddress}/realtimeData")
        
        try {
            firebaseManager.observeRealtimeData(userIdToUse, kiln.macAddress).collect { data ->
                // Actualizar timestamp de último dato recibido
                lastDataReceived = System.currentTimeMillis()
                android.util.Log.d("KilnCard", "📡 Datos recibidos - actualizando timestamp de conexión")
                android.util.Log.d("KilnCard", "📦 Datos recibidos de Firebase: $data")
                android.util.Log.d("KilnCard", "🔍 Tipo de datos: ${data?.javaClass?.simpleName}")
                android.util.Log.d("KilnCard", "🔍 ¿Es null? ${data == null}")
            
            if (data == null) {
                android.util.Log.e("KilnCard", "❌ Data es NULL - No se encontraron datos en Firebase")
                firebaseTemperature = null
                firebaseProgramName = null
                isWifiConnected = false
                return@collect
            }
            
            android.util.Log.d("KilnCard", "🔑 Keys en data: ${data.keys}")
            android.util.Log.d("KilnCard", "🔍 Buscando 'temperature' en data...")
            
            val tempValue = data["temperature"]
            android.util.Log.d("KilnCard", "📊 Valor de temperature: $tempValue (Tipo: ${tempValue?.javaClass?.simpleName})")
            
            firebaseTemperature = when (tempValue) {
                is Number -> {
                    android.util.Log.d("KilnCard", "✅ Temperatura encontrada: $tempValue")
                    tempValue.toFloat()
                }
                is String -> {
                    try {
                        val parsed = tempValue.toFloat()
                        android.util.Log.d("KilnCard", "✅ Temperatura parseada de String: $parsed")
                        parsed
                    } catch (e: Exception) {
                        android.util.Log.e("KilnCard", "❌ Error parseando String a Float: ${e.message}")
                        null
                    }
                }
                else -> {
                    android.util.Log.w("KilnCard", "⚠️ Tipo de dato no soportado: ${tempValue?.javaClass?.simpleName}")
                    null
                }
            }
            
                       // Verificar estado de conexión basado en timestamp
                       val timestamp = com.example.mysmartkiln.utils.UserIdUtils
                           .parseFirebaseTimestamp(data["timestamp"])
                       lastOnlineTimestamp = timestamp
                       val currentTimeMs = System.currentTimeMillis()
                       val CONNECTION_TIMEOUT_MS = 30000L // 30 segundos
                       
                       android.util.Log.d("KilnCard", "🕐 Timestamp Firebase: $timestamp")
                       android.util.Log.d("KilnCard", "🕐 Timestamp actual: $currentTimeMs")
                       android.util.Log.d("KilnCard", "🕐 Diferencia: ${currentTimeMs - timestamp} ms")
                       android.util.Log.d("KilnCard", "🕐 Timeout: $CONNECTION_TIMEOUT_MS ms")
                       
                       // Verificar si los datos son recientes (no cacheados)
                       val isRecentData = if (timestamp > 0) {
                           val timeDiff = currentTimeMs - timestamp
                           val isRecent = timeDiff < CONNECTION_TIMEOUT_MS
                           android.util.Log.d("KilnCard", "🔌 ¿Datos recientes? $isRecent (diff: ${timeDiff}ms)")
                           isRecent
                       } else {
                           android.util.Log.d("KilnCard", "🔌 Timestamp inválido, marcando como desconectado")
                           false
                       }
                       
                       // Solo marcar como conectado si los datos son recientes
                       isWifiConnected = isRecentData
            
            // Leer estado del horno desde Firebase
            val statusFromFirebase = data["status"] as? String
            firebaseStatus = statusFromFirebase
            
            // Determinar estado del programa usando lógica híbrida
            // bleStatus se pasa como parámetro desde KilnsScreen
            try {
                programRunningState = ProgramStatusUtils.determineProgramState(
                    hasBleConnection = isBleConnected,
                    bleStatus = if (isBleConnected) bleStatus else null,
                    bleProgramName = if (isBleConnected) programName else null,
                    firebaseData = data,
                    programTotalDurationMinutes = null // Se puede calcular desde la app si tenemos los segmentos
                )
                
                // Actualizar hasProgram basado en el estado calculado
                hasProgram = when (programRunningState) {
                    ProgramStatusUtils.ProgramRunningState.CONFIRMED_RUNNING,
                    ProgramStatusUtils.ProgramRunningState.ESTIMATED_RUNNING -> true
                    ProgramStatusUtils.ProgramRunningState.CONFIRMED_STOPPED,
                    ProgramStatusUtils.ProgramRunningState.ESTIMATED_STOPPED,
                    ProgramStatusUtils.ProgramRunningState.UNKNOWN -> false
                }
            } catch (e: Exception) {
                android.util.Log.e("KilnCard", "❌ Error en determineProgramState: ${e.message}")
                android.util.Log.e("KilnCard", "❌ Stack trace: ${e.stackTraceToString()}")
                // En caso de error, usar lógica simple basada en status de Firebase
                val status = data["status"] as? String
                hasProgram = status == "running" || status == "paused"
                programRunningState = ProgramStatusUtils.ProgramRunningState.UNKNOWN
            }
            
            // Leer nombre del programa desde Firebase
            val programNameValue = data["programName"]
            firebaseProgramName = when (programNameValue) {
                is String -> {
                    val name = programNameValue.trim()
                    android.util.Log.d("KilnCard", "📋 Nombre del programa desde Firebase: '$name'")
                    if (name.isNotEmpty()) name else null
                }
                else -> {
                    android.util.Log.d("KilnCard", "📋 No hay nombre de programa en Firebase (tipo: ${programNameValue?.javaClass?.simpleName})")
                    null
                }
            }
            
            // Calcular temperatura final se hace en un LaunchedEffect separado
            // para actualizar inmediatamente cuando cambia BLE
            android.util.Log.d("KilnCard", "🔍 bleTemperature: $bleTemperature")
            android.util.Log.d("KilnCard", "🔍 firebaseTemperature: $firebaseTemperature")
            android.util.Log.d("KilnCard", "🔍 cachedBleTemperature: $cachedBleTemperature")
            android.util.Log.d("KilnCard", "🔌 Estado de conexión: ${if (isBleConnected) "Conectado" else "Desconectado"}")
            android.util.Log.d("KilnCard", "📋 Estado del programa: $programRunningState")
            android.util.Log.d("KilnCard", "📋 Tiene programa: $hasProgram")
            android.util.Log.d("KilnCard", "📋 firebaseProgramName: $firebaseProgramName")
        }
        } catch (e: Exception) {
            android.util.Log.e("KilnCard", "💥 ERROR en Firebase: ${e.message}")
            android.util.Log.e("KilnCard", "💥 Stack trace: ${e.stackTraceToString()}")
            firebaseTemperature = null
            firebaseProgramName = null
            isWifiConnected = false
        } finally {
            // El timer de verificación se maneja en un LaunchedEffect separado
        }
    }
    
    val swipeOffset by animateFloatAsState(
        targetValue = if (isSwiped) -160f else 0f, // Desplazamiento de 160dp para mostrar ambos botones (editar + eliminar)
        animationSpec = tween(durationMillis = 200),
        label = "swipe"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .requiredHeight(235.dp)
    ) {
        // Swipe Actions (background) - usando patrón de ProgramCard
        KilnSwipeActions(
            onEdit = onEditClick,
            onDelete = onDeleteClick,
            modifier = Modifier
                .align(Alignment.CenterEnd) // Alinear a la derecha
                .requiredHeight(235.dp)
        )
        
        // Main card (foreground)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(235.dp)
                .offset(x = swipeOffset.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    color = kilnColors.cardBackground,
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 1.dp,
                    color = kilnColors.cardBorder,
                    shape = RoundedCornerShape(16.dp)
                )
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            offsetX = 0f
                        },
                        onDragEnd = {
                            // Auto-close if not swiped enough to the left
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

                            // Si está deslizada hacia la izquierda y se desliza hacia la derecha
                            if (isSwiped && dragAmount > 0) {
                                // Regresar a posición original si se desliza hacia la derecha
                                isSwiped = false
                            }
                            // Deslizamiento normal hacia la izquierda
                            else if (newOffset <= 0f && newOffset >= -160f) {
                                isSwiped = newOffset < -10f
                            }
                        }
                    )
                }
                .clickable { 
                    if (isSwiped) {
                        isSwiped = false
                    } else {
                        onClick() 
                    }
                }
                .padding(16.dp)
        ) {
            // Información del horno
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Nombre del horno (centrado)
                Text(
                    text = kiln.name,
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
                
                // Icono de temperatura debajo del nombre
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono de temperatura
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
                    
                    // Temperatura en tiempo real
                    Text(
                        text = if (temperature != null) "${temperature?.toInt()}°C" else "--°C",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kilnColors.body,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    )
                }
                
                // Icono dinámico: Bluetooth (BLE) > WiFi > Reloj (desconectado)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono con prioridad: Bluetooth > WiFi > Reloj
                    Box(
                        modifier = Modifier
                            .requiredSize(35.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color = kilnColors.accent.copy(alpha = 0.15f))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                id = when {
                                    // Solo mostrar Bluetooth cuando BLE está realmente conectado
                                    connectionType == "ble" && isConnected -> R.drawable.icon_bluetooth
                                    // Todos los demás casos usan WiFi
                                    else -> R.drawable.icon_wifi
                                }
                            ),
                            contentDescription = when {
                                connectionType == "ble" && isConnected -> "Connected via Bluetooth"
                                else -> "WiFi connection status"
                            },
                            tint = kilnColors.accent,
                            modifier = Modifier.requiredSize(24.dp)
                        )
                    }
                    
                    // Texto dinámico según el estado de conexión
                    val timeSinceLastOnline = calculateTimeSinceLastOnline(lastOnlineTimestamp)
                    
                    // Log para debugging de UI
                    android.util.Log.d("KilnCard", "🎨 UI Update - connectionType: $connectionType, isConnected: $isConnected, lastOnlineTimestamp: $lastOnlineTimestamp")
                    
                    val displayText = when {
                        // BLE conectado: solo "Conectado"
                        connectionType == "ble" && isConnected -> "Conectado"
                        // WiFi conectado: "Conectado"
                        connectionType == "wifi" && isConnected -> "Conectado"
                        // WiFi sin internet: "Sin internet"
                        updatedConnectionState == HybridConnectionState.WIFI_NO_INTERNET -> "Sin internet"
                        // WiFi Firebase down: "Sin conexion a la nube"
                        updatedConnectionState == HybridConnectionState.WIFI_FIREBASE_DOWN -> "Sin conexion a la nube"
                        // WiFi desconectado pero con timestamp: "Visto hace xxx"
                        connectionType == "wifi" && !isConnected && lastOnlineTimestamp > 0 -> "Visto hace $timeSinceLastOnline"
                        // WiFi desconectado sin timestamp: "Desconectado"
                        connectionType == "wifi" && !isConnected -> "Desconectado"
                        // BLE desconectado: "Desconectado"
                        connectionType == "ble" && !isConnected -> "Desconectado"
                        // Fallback: "Desconectado"
                        else -> "Desconectado"
                    }
                    
                    android.util.Log.d("KilnCard", "🎨 Texto final: $displayText")
                    
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = kilnColors.body,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    )
                }
                
                // Icono de programa debajo de los iconos anteriores
                // El espaciado de 12.dp del Column asegura la misma distancia entre filas
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        android.util.Log.d("KilnCard", "🔥 Program icon clicked for kiln: ${kiln.name}")
                        onLoadProgramClick()
                    }
                ) {
                    // Icono de programa
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
                    
                    // Nombre del programa cargado o texto por defecto
                    // Usar finalProgramName que viene de BLE (si hay conexión) o Firebase (si no hay BT)
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
                
                // Icono de estado de actividad (llama) debajo del icono de programa
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono de llama
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
                    
                    // Texto del estado de actividad
                    val activityStatusText = when {
                        // Prioridad 1: Estado desde BLE (más confiable)
                        isBleConnected && bleStatus != null -> {
                            when (bleStatus) {
                                "CALENTANDO" -> "Horneando"
                                "PAUSADO" -> "Pausa"
                                "DETENIDO" -> "Detenido"
                                "FINALIZADO" -> "Finalizado"
                                "INTERRUMPIDO" -> "Interrumpido"
                                "IDLE" -> "Inactivo"
                                else -> "Inactivo"
                            }
                        }
                        // Prioridad 2: Estado desde Firebase
                        firebaseStatus != null -> {
                            when (firebaseStatus) {
                                "running", "CALENTANDO" -> "Horneando"
                                "paused", "PAUSADO" -> "Pausa"
                                "stopped", "DETENIDO" -> "Detenido"
                                "finished", "FINALIZADO" -> "Finalizado"
                                "INTERRUMPIDO" -> "Interrumpido"
                                "idle", "IDLE" -> "Inactivo"
                                else -> "Inactivo"
                            }
                        }
                        // Fallback: Sin estado conocido
                        else -> "Inactivo"
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
            "${minutes}min"
        }
        elapsedSeconds < 86400 -> {
            val hours = elapsedSeconds / 3600
            val minutes = (elapsedSeconds % 3600) / 60
            if (minutes > 0) "${hours}h ${minutes}min" else "${hours}h"
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
    MySmartKilnTheme {
        KilnCard(
            kiln = KilnEntity(
                macAddress = "A4CF128856E8",
                bleMacAddress = "A4:CF:12:88:56:E8",
                name = "MiHorno",
                manufacturer = "AFRA",
                volume = "25",
                userId = "test_user_123"
            )
        )
    }
}
