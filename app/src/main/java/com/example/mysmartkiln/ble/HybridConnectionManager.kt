package com.example.mysmartkiln.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

/**
 * Manager principal para el sistema híbrido BLE/WiFi
 * Coordina entre detección de proximidad, estados de conexión y sincronización
 */
class HybridConnectionManager(
    private val context: Context,
    private val bleManager: BleManager? = null  // Permite inyectar una instancia existente
) {
    
    private val proximityManager = ProximityManager(context)
    private val syncManager = SyncManager()
    // Usar la instancia proporcionada o crear una nueva
    private val bleManagerInstance = bleManager ?: BleManager(context)
    
    // Usar bleManagerInstance en lugar de bleManager en todo el código
    
    // Callback para actualizar temperaturas BLE en la UI
    private var temperatureUpdateCallback: ((String, Float) -> Unit)? = null
    
    private val _connectionState = MutableStateFlow(HybridConnectionState.OFFLINE)
    val connectionState: StateFlow<HybridConnectionState> = _connectionState.asStateFlow()
    
    private val _currentMode = MutableStateFlow(HybridConnectionMode.WIFI)
    val currentMode: StateFlow<HybridConnectionMode> = _currentMode.asStateFlow()
    
    private var modeSwitchJob: Job? = null
    private var bleTemperatureJob: Job? = null
    private val modeSwitchTimeout = 8000L // 8 segundos timeout para volver a WiFi (sincronizado con ESP32)
    private var connectionRetryCount = 0
    private val maxRetries = 3 // Máximo 3 reintentos de conexión
    private var proximityManagerFailed = false // Flag para detectar fallos del ProximityManager
    private var fallbackScanJob: Job? = null // Job para escaneo de fallback
    private var registeredKilnsList = listOf<String>() // Lista de hornos registrados para fallback
    private var isStarted = false // Flag para evitar reiniciar si ya está iniciado
    private var lastProgramSentTime = 0L // Timestamp de cuando se envió el último programa
    private val PROGRAM_STABILIZATION_DELAY = 30000L // 30 segundos después de enviar programa, no desconectar automáticamente
    
    init {
        // Observar cambios en proximidad y actualizar estado
        CoroutineScope(Dispatchers.IO).launch {
            combine(
                proximityManager.isNearKiln,
                proximityManager.nearbyKilns
            ) { isNear, kilns ->
                handleProximityChange(isNear, kilns)
            }.collect { }
        }
        
        // Observar estado de conexión BLE para detectar conexiones y desconexiones
        CoroutineScope(Dispatchers.IO).launch {
            bleManagerInstance.isConnected.collect { isConnected ->
                Log.d("HybridManager", "🔍 Estado BLE cambió: $isConnected")
                
                if (isConnected) {
                    // Conexión BLE exitosa - actualizar estado
                    Log.d("HybridManager", "✅ BLE conectado exitosamente - actualizando estado")
                    _connectionState.value = HybridConnectionState.BLE_CONNECTED
                    _currentMode.value = HybridConnectionMode.BLE
                    proximityManager.setBleConnectionState(true)
                    
                    // Resetear contador de reintentos en conexión exitosa
                    connectionRetryCount = 0
                    
                    // Esperar a que los servicios estén descubiertos y notificaciones habilitadas
                    val notificationsReady = waitForBleNotifications(5000) // 5 segundos timeout
                    
                    if (notificationsReady) {
                        Log.d("HybridManager", "📡 Notificaciones BLE habilitadas")
                        // Iniciar observación de temperatura BLE
                        val mac = bleManagerInstance.connectedDeviceMac ?: ""
                        Log.d("HybridManager", "📍 Iniciando observación con MAC: $mac")
                        startBleTemperatureObservation(mac)
                    } else {
                        Log.e("HybridManager", "❌ Timeout esperando notificaciones BLE")
                        // Intentar de todas formas
                        val mac = bleManagerInstance.connectedDeviceMac ?: ""
                        Log.d("HybridManager", "📍 Iniciando observación con MAC: $mac")
                        startBleTemperatureObservation(mac)
                    }
                } else if (_currentMode.value == HybridConnectionMode.BLE) {
                    // Si estamos en modo BLE pero se perdió la conexión
                    Log.d("HybridManager", "❌ BLE desconectado - cambiando a modo WiFi")
                    switchToWifiMode()
                }
            }
        }
    }
    
    /**
     * Maneja cambios en la proximidad del usuario
     */
    private suspend fun handleProximityChange(isNear: Boolean, nearbyKilns: List<String>) {
        Log.d("HybridManager", "🔄 Cambio de proximidad: cerca=$isNear, hornos=${nearbyKilns.size}")
        Log.d("HybridManager", "🔍 Estado actual: modo=${_currentMode.value}, connectionState=${_connectionState.value}")
        
        when {
            isNear && _currentMode.value == HybridConnectionMode.WIFI -> {
                // Usuario se acercó al horno - cambiar a modo BLE
                Log.d("HybridManager", "🎯 Condición cumplida: cerca=true Y modo=WIFI")
                switchToBleMode(nearbyKilns)
            }
            isNear && _currentMode.value == HybridConnectionMode.BLE -> {
                // Ya está en modo BLE, asegurar que el estado sea correcto
                Log.d("HybridManager", "🎯 Ya en modo BLE, verificando estado...")
                
                // CRÍTICO: Cancelar cualquier timeout de desconexión programado
                // porque estamos cerca del horno
                modeSwitchJob?.cancel()
                Log.d("HybridManager", "🚫 Timeout de desconexión cancelado - estamos cerca del horno")
                
                if (_connectionState.value != HybridConnectionState.BLE_CONNECTED) {
                    Log.d("HybridManager", "🔧 Corrigiendo estado a BLE_CONNECTED")
                    _connectionState.value = HybridConnectionState.BLE_CONNECTED
                }
                
                // Asegurar que la observación de temperatura esté iniciada
                if (bleTemperatureJob == null || !bleTemperatureJob!!.isActive) {
                    val mac = bleManagerInstance.connectedDeviceMac ?: nearbyKilns.firstOrNull() ?: ""
                    if (mac.isNotEmpty() && bleManagerInstance.isConnected.value) {
                        Log.d("HybridManager", "🔧 Iniciando observación de temperatura (no estaba activa)")
                        startBleTemperatureObservation(mac)
                    }
                }
            }
            !isNear && _currentMode.value == HybridConnectionMode.BLE -> {
                // Usuario se alejó del horno - programar vuelta a WiFi
                // Pero esperar un momento para evitar desconexiones por fluctuaciones breves
                Log.d("HybridManager", "🎯 Condición cumplida: cerca=false Y modo=BLE")
                
                // CRÍTICO: Verificar si hay una conexión BLE activa confirmada
                // Si hay conexión activa, NO desconectar basándonos solo en detección de proximidad
                // porque el ProximityManager puede reportar temporalmente que no hay hornos cerca
                // durante los ciclos de escaneo, incluso cuando estamos conectados
                val hasActiveBleConnection = bleManagerInstance.isConnected.value
                val hasConnectedMac = bleManagerInstance.connectedDeviceMac != null && 
                                     bleManagerInstance.connectedDeviceMac!!.isNotEmpty()
                
                if (hasActiveBleConnection && hasConnectedMac) {
                    Log.d("HybridManager", "🔵 Conexión BLE activa confirmada (MAC: ${bleManagerInstance.connectedDeviceMac})")
                    Log.d("HybridManager", "🔵 Ignorando reporte de ProximityManager (puede ser temporal durante escaneo)")
                    Log.d("HybridManager", "🔵 Manteniendo conexión BLE activa")
                    return@handleProximityChange
                }
                
                // CRÍTICO: Si acabamos de enviar un programa, NO desconectar automáticamente
                // para evitar desconexiones durante la estabilización de la conexión
                val timeSinceProgramSent = System.currentTimeMillis() - lastProgramSentTime
                if (timeSinceProgramSent < PROGRAM_STABILIZATION_DELAY) {
                    Log.d("HybridManager", "⏸️ Programa enviado hace ${timeSinceProgramSent/1000} segundos - evitando desconexión automática (esperando ${PROGRAM_STABILIZATION_DELAY/1000}s)")
                    Log.d("HybridManager", "⏸️ Tiempo restante: ${(PROGRAM_STABILIZATION_DELAY - timeSinceProgramSent)/1000} segundos")
                    return@handleProximityChange
                }
                
                // Solo programar desconexión si realmente no hay hornos cerca
                // Esto evita desconexiones por fluctuaciones momentáneas
                if (nearbyKilns.isEmpty()) {
                    scheduleWifiReturn()
                } else {
                    Log.d("HybridManager", "⏸️ Hay hornos en la lista pero isNear=false - esperando confirmación antes de desconectar")
                    // Esperar 2 segundos adicionales antes de programar desconexión
                    // para dar tiempo a que la señal se estabilice
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(2000)
                        // Verificar nuevamente si realmente no hay hornos cerca
                        // Y también verificar que no acabamos de enviar un programa
                        // Y CRÍTICO: verificar si hay conexión BLE activa antes de desconectar
                        val timeSinceProgramSent2 = System.currentTimeMillis() - lastProgramSentTime
                        val stillHasActiveConnection = bleManagerInstance.isConnected.value && 
                                                      (bleManagerInstance.connectedDeviceMac != null && 
                                                       bleManagerInstance.connectedDeviceMac!!.isNotEmpty())
                        
                        if (!isNear && nearbyKilns.isEmpty() && 
                            timeSinceProgramSent2 >= PROGRAM_STABILIZATION_DELAY &&
                            !stillHasActiveConnection) {
                            scheduleWifiReturn()
                        } else {
                            if (stillHasActiveConnection) {
                                Log.d("HybridManager", "✅ Conexión BLE activa detectada - cancelando desconexión programada")
                            } else {
                                Log.d("HybridManager", "✅ Señal recuperada o programa reciente - cancelando desconexión programada")
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Cambia a modo BLE
     */
    private suspend fun switchToBleMode(nearbyKilns: List<String>) {
        Log.d("HybridManager", "🔵 Cambiando a modo BLE...")
        
        _currentMode.value = HybridConnectionMode.BLE
        // NO establecer BLE_CONNECTED hasta que la conexión se confirme
        
        // Conectar al primer horno cercano y suscribirse a notificaciones
        if (nearbyKilns.isNotEmpty()) {
            val kilnMac = nearbyKilns.first()
            Log.d("HybridManager", "🔌 Conectando a horno BLE: $kilnMac")
            
            try {
                // Iniciar conexión BLE (asíncrona)
                Log.d("HybridManager", "🔌 Llamando a bleManagerInstance.connectByMac($kilnMac)")
                val connectionStarted = bleManagerInstance.connectByMac(kilnMac)
                Log.d("HybridManager", "🔌 connectByMac retornó: $connectionStarted")
                
                if (connectionStarted) {
                    Log.d("HybridManager", "✅ Conexión BLE iniciada: $kilnMac - esperando confirmación...")
                    // NO actualizar estado aquí - el observer de bleManagerInstance.isConnected se encargará
                    // cuando la conexión real se establezca
                } else {
                    Log.e("HybridManager", "❌ No se pudo iniciar conexión BLE: $kilnMac")
                    // Mantener en modo WiFi si BLE falla
                    _connectionState.value = HybridConnectionState.WIFI_ONLINE
                    handleBleConnectionFailure(kilnMac)
                }
            } catch (e: Exception) {
                Log.e("HybridManager", "❌ Error conectando a horno BLE: ${e.message}")
                // Mantener en modo WiFi si BLE falla
                _connectionState.value = HybridConnectionState.WIFI_ONLINE
                handleBleConnectionFailure(kilnMac)
            }
        }
    }
    
    /**
     * Programa el retorno a modo WiFi
     */
    private fun scheduleWifiReturn() {
        Log.d("HybridManager", "⏰ Programando retorno a WiFi en ${modeSwitchTimeout/1000} segundos...")
        
        modeSwitchJob?.cancel()
        modeSwitchJob = CoroutineScope(Dispatchers.IO).launch {
            delay(modeSwitchTimeout)
            
            if (_currentMode.value == HybridConnectionMode.BLE) {
                Log.d("HybridManager", "🔄 Timeout alcanzado - volviendo a modo WiFi")
                switchToWifiMode()
            }
        }
    }
    
    /**
     * Cambia a modo WiFi
     */
    private suspend fun switchToWifiMode() {
        Log.d("HybridManager", "🟢 Cambiando a modo WiFi...")
        
        // Notificar al ProximityManager que BLE se desconectó
        proximityManager.setBleConnectionState(false)
        
        // Desconectar BLE
        bleManagerInstance.disconnect()
        Log.d("HybridManager", "🔌 Desconectado de BLE")
        
        _currentMode.value = HybridConnectionMode.WIFI
        _connectionState.value = HybridConnectionState.WIFI_ONLINE
        
        // Iniciar sincronización de datos pendientes
        if (syncManager.hasPendingData()) {
            Log.d("HybridManager", "📤 Sincronizando datos pendientes...")
            syncManager.startSync()
        }
    }
    
    /**
     * Inicia el sistema híbrido
     */
    fun start() {
        // SIEMPRE verificar primero si hay una conexión BLE activa antes de hacer cualquier cosa
        val hasBleConnection = bleManagerInstance.isConnected.value
        val connectedMac = bleManagerInstance.connectedDeviceMac
        
        // Verificar también el estado del BluetoothAdapter para detectar conexiones activas
        val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
        val bluetoothEnabled = bluetoothAdapter?.isEnabled == true
        
        Log.d("HybridManager", "🔍 start() llamado - isStarted=$isStarted, BLE conectado=$hasBleConnection, MAC=$connectedMac, BT habilitado=$bluetoothEnabled")
        Log.d("HybridManager", "🔍 Estado actual antes de start: modo=${_currentMode.value}, connectionState=${_connectionState.value}")
        
        // Si hay una conexión BLE activa, NO resetear el estado
        if (hasBleConnection && connectedMac != null && connectedMac.isNotEmpty()) {
            Log.d("HybridManager", "✅ Conexión BLE activa detectada (MAC: $connectedMac) - NO reseteando estado")
            // Si el sistema no estaba iniciado, iniciar solo el ProximityManager sin resetear estado
            if (!isStarted) {
                Log.d("HybridManager", "🔄 Iniciando solo ProximityManager (sin resetear estado BLE)")
                proximityManager.startProximityDetection()
                isStarted = true
            }
            // Si el estado actual es BLE_CONNECTED, mantenerlo
            if (_connectionState.value == HybridConnectionState.BLE_CONNECTED) {
                Log.d("HybridManager", "🔵 Manteniendo estado BLE_CONNECTED existente")
            }
            // NO resetear el estado si hay una conexión BLE activa
            return
        }
        
        // Si ya está iniciado pero no hay conexión BLE, verificar si debemos reiniciar
        if (isStarted) {
            Log.d("HybridManager", "🔄 Sistema ya iniciado pero sin conexión BLE - continuando...")
        }
        
        Log.d("HybridManager", "🚀 Iniciando sistema híbrido BLE/WiFi...")
        
        proximityManager.startProximityDetection()
        
        // Solo resetear estado a WiFi si no hay conexión BLE activa
        if (!hasBleConnection) {
            Log.d("HybridManager", "🔄 Reseteando estado a WiFi (no hay conexión BLE)")
            _connectionState.value = HybridConnectionState.WIFI_ONLINE
            _currentMode.value = HybridConnectionMode.WIFI
        } else {
            Log.d("HybridManager", "🔵 Manteniendo estado BLE: ${_connectionState.value}")
        }
        
        isStarted = true
        
        // Iniciar escaneo de fallback después de 30 segundos si ProximityManager falla
        CoroutineScope(Dispatchers.IO).launch {
            delay(30000) // Esperar 30 segundos
            if (!proximityManagerFailed) {
                Log.d("HybridManager", "🔍 Verificando estado del ProximityManager...")
                // Verificar si ProximityManager está funcionando
                delay(10000) // Esperar 10 segundos más
                if (!proximityManagerFailed) {
                    Log.w("HybridManager", "⚠️ ProximityManager puede estar fallando - iniciando escaneo de fallback")
                    startFallbackScanning()
                }
            }
        }
    }
    
    /**
     * Inicia escaneo de fallback usando BleManager directamente
     */
    private fun startFallbackScanning() {
        proximityManagerFailed = true
        Log.d("HybridManager", "🔄 Iniciando escaneo de fallback con BleManager...")
        
        fallbackScanJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // Escanear cada 10 segundos usando BleManager
                    Log.d("HybridManager", "🔍 Escaneo de fallback - buscando hornos registrados...")
                    Log.d("HybridManager", "📋 Hornos registrados para fallback: $registeredKilnsList")
                    
                    // Usar la lista guardada de hornos registrados
                    if (registeredKilnsList.isNotEmpty()) {
                        val kilnMac = registeredKilnsList.first()
                        Log.d("HybridManager", "🎯 Intentando conectar a horno de fallback: $kilnMac")
                        
                        // Intentar conectar directamente usando BleManager
                        val connected = bleManagerInstance.connectByMac(kilnMac)
                        Log.d("HybridManager", "🔌 Resultado de conexión: $connected")
                        
                        if (connected) {
                            Log.d("HybridManager", "✅ Conexión de fallback iniciada: $kilnMac - esperando confirmación...")
                            // NO actualizar estado aquí - el observer de bleManagerInstance.isConnected se encargará
                            // cuando la conexión real se establezca
                            break // Salir del loop de fallback
                        } else {
                            Log.w("HybridManager", "⚠️ Fallo en conexión de fallback: $kilnMac")
                        }
                    } else {
                        Log.w("HybridManager", "⚠️ No hay hornos registrados para fallback")
                    }
                    
                    delay(10000) // Esperar 10 segundos antes del siguiente intento
                } catch (e: Exception) {
                    Log.e("HybridManager", "❌ Error en escaneo de fallback: ${e.message}")
                    delay(15000) // Esperar más tiempo en caso de error
                }
            }
        }
    }
    
    /**
     * Detiene el sistema híbrido
     */
    fun stop() {
        Log.d("HybridManager", "🛑 Deteniendo sistema híbrido...")
        
        proximityManager.stopProximityDetection()
        syncManager.stopSync()
        modeSwitchJob?.cancel()
        bleTemperatureJob?.cancel()
        fallbackScanJob?.cancel()
        bleManagerInstance.disconnect()
        
        _connectionState.value = HybridConnectionState.OFFLINE
        _currentMode.value = HybridConnectionMode.WIFI
        proximityManagerFailed = false
    }
    
    /**
     * Agrega datos para sincronización diferida
     */
    fun queueDataForSync(priority: SyncPriority, data: Any) {
        if (_currentMode.value == HybridConnectionMode.BLE) {
            Log.d("HybridManager", "📝 Agregando datos a cola de sincronización: $priority")
            syncManager.queueForSync(priority, data)
        } else {
            Log.d("HybridManager", "📤 Sincronizando datos inmediatamente: $priority")
            // Sincronización inmediata en modo WiFi
        }
    }
    
    /**
     * Marca que se envió un programa al horno
     * Esto evita desconexiones automáticas por un tiempo para permitir que la conexión se estabilice
     */
    fun notifyProgramSent() {
        lastProgramSentTime = System.currentTimeMillis()
        Log.d("HybridManager", "📤 Programa enviado - marcado timestamp: $lastProgramSentTime")
        Log.d("HybridManager", "📤 Desconexiones automáticas deshabilitadas por ${PROGRAM_STABILIZATION_DELAY/1000} segundos")
    }
    
    /**
     * Obtiene el estado actual de conexión
     */
    fun getConnectionState(): HybridConnectionState {
        return _connectionState.value
    }
    
    /**
     * Obtiene el modo actual
     */
    fun getCurrentMode(): HybridConnectionMode {
        return _currentMode.value
    }
    
    /**
     * Configura el callback para actualizar temperaturas BLE
     */
    fun setTemperatureUpdateCallback(callback: (String, Float) -> Unit) {
        temperatureUpdateCallback = callback
    }
    
    /**
     * Actualiza la temperatura BLE y notifica a la UI
     */
    private fun updateTemperature(macAddress: String, temperature: Float) {
        Log.d("HybridManager", "📤 updateTemperature llamado: MAC=$macAddress, temp=$temperature°C")
        Log.d("HybridManager", "🔍 Callback configurado: ${temperatureUpdateCallback != null}")
        if (temperatureUpdateCallback != null) {
            try {
                temperatureUpdateCallback!!.invoke(macAddress, temperature)
                Log.d("HybridManager", "✅ Callback invocado exitosamente")
            } catch (e: Exception) {
                Log.e("HybridManager", "❌ Error invocando callback: ${e.message}")
                Log.e("HybridManager", "❌ Stack trace: ${e.stackTraceToString()}")
            }
        } else {
            Log.w("HybridManager", "⚠️ No hay callback configurado para actualizar temperatura")
        }
    }
    
    /**
     * Espera a que la conexión BLE se establezca
     */
    private suspend fun waitForBleConnection(timeoutMs: Long): Boolean {
        Log.d("HybridManager", "⏳ Esperando conexión BLE (timeout: ${timeoutMs}ms)...")
        
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (bleManagerInstance.isConnected.value) {
                Log.d("HybridManager", "✅ Conexión BLE confirmada")
                return true
            }
            delay(100) // Verificar cada 100ms
        }
        
        Log.e("HybridManager", "❌ Timeout esperando conexión BLE")
        return false
    }
    
    /**
     * Espera a que las notificaciones BLE estén habilitadas
     */
    private suspend fun waitForBleNotifications(timeoutMs: Long): Boolean {
        Log.d("HybridManager", "⏳ Esperando notificaciones BLE (timeout: ${timeoutMs}ms)...")
        
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            // Verificar si hay una temperatura válida (indica que las notificaciones funcionan)
            if (bleManagerInstance.temperature.value > 0) {
                Log.d("HybridManager", "✅ Notificaciones BLE confirmadas (temp: ${bleManagerInstance.temperature.value}°C)")
                return true
            }
            delay(200) // Verificar cada 200ms
        }
        
        Log.w("HybridManager", "⚠️ Timeout esperando notificaciones BLE - continuando de todas formas")
        return false
    }
    
    /**
     * Maneja fallos de conexión BLE con reintentos
     */
    private suspend fun handleBleConnectionFailure(kilnMac: String) {
        connectionRetryCount++
        
        if (connectionRetryCount < maxRetries) {
            Log.d("HybridManager", "🔄 Reintentando conexión BLE (intento $connectionRetryCount/$maxRetries): $kilnMac")
            delay(2000) // Esperar 2 segundos antes del reintento
            
            // Intentar reconectar
            switchToBleMode(listOf(kilnMac))
        } else {
            Log.e("HybridManager", "❌ Máximo de reintentos alcanzado - volviendo a WiFi")
            connectionRetryCount = 0 // Resetear contador
            _connectionState.value = HybridConnectionState.WIFI_ONLINE
            _currentMode.value = HybridConnectionMode.WIFI
        }
    }
    
    /**
     * Inicia la observación de temperatura BLE
     */
    private fun startBleTemperatureObservation(macAddress: String) {
        Log.d("HybridManager", "🌡️ Iniciando observación de temperatura BLE: $macAddress")
        Log.d("HybridManager", "🔍 Estado actual: modo=${_currentMode.value}, connectionState=${_connectionState.value}")
        Log.d("HybridManager", "🔍 BLE Manager conectado: ${bleManagerInstance.isConnected.value}")
        
        // Cancelar observación anterior si existe
        bleTemperatureJob?.cancel()
        bleTemperatureJob = null
        Log.d("HybridManager", "🧹 Job anterior cancelado")
        
        // Verificar que estamos en modo BLE
        if (_currentMode.value != HybridConnectionMode.BLE) {
            Log.w("HybridManager", "⚠️ No se puede iniciar observación BLE: no estamos en modo BLE (modo actual: ${_currentMode.value})")
            return
        }
        
        // Iniciar nueva observación
        bleTemperatureJob = CoroutineScope(Dispatchers.IO).launch {
            Log.d("HybridManager", "🔄 Job de observación de temperatura iniciado en hilo: ${Thread.currentThread().name}")
            
            try {
                Log.d("HybridManager", "👀 Comenzando a observar temperatura del BleManager...")
                Log.d("HybridManager", "📊 Estado inicial de temperatura: ${bleManagerInstance.temperature.value}°C")
                
                // Observar el StateFlow de temperatura del BleManager
                // Usar collectLatest para evitar acumulación de valores
                bleManagerInstance.temperature.collect { temperature ->
                    Log.d("HybridManager", "📊 Temperatura recibida del BleManager: $temperature°C")
                    
                    // Verificar que aún estamos en modo BLE
                    if (_currentMode.value != HybridConnectionMode.BLE) {
                        Log.d("HybridManager", "🛑 Ya no estamos en modo BLE, cancelando observación")
                        cancel()
                        return@collect
                    }
                    
                    // Actualizar UI con cualquier valor recibido (0°C es válido cuando el horno está frío)
                    Log.d("HybridManager", "🌡️ Temperatura BLE recibida: $temperature°C - actualizando...")
                    updateTemperature(macAddress, temperature)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                Log.d("HybridManager", "🛑 Job de observación de temperatura cancelado")
                throw e // Re-throw cancellation exceptions
            } catch (e: Exception) {
                Log.e("HybridManager", "❌ Error observando temperatura BLE: ${e.message}")
                Log.e("HybridManager", "❌ Stack trace: ${e.stackTraceToString()}")
                delay(1000) // Esperar antes de reintentar
                // El loop continuará si el job aún está activo
            }
            
            Log.d("HybridManager", "🛑 Job de observación de temperatura finalizado")
        }
        
        Log.d("HybridManager", "✅ Job de observación de temperatura iniciado correctamente")
    }
    
    /**
     * Configura los hornos registrados para el sistema híbrido
     */
    fun setRegisteredKilns(macAddresses: List<String>) {
        proximityManager.setRegisteredKilns(macAddresses)
        registeredKilnsList = macAddresses // Guardar para fallback
        Log.d("HybridManager", "🎯 Hornos configurados: $macAddresses")
    }
    
    /**
     * Verifica si hay datos pendientes de sincronización
     */
    fun hasPendingSyncData(): Boolean {
        return syncManager.hasPendingData()
    }
    
    /**
     * Obtiene el número de elementos pendientes
     */
    fun getPendingSyncCount(): Int {
        return syncManager.getPendingCount()
    }
    
    /**
     * Verifica si hay una conexión BLE activa
     * Nota: Esta función verifica múltiples fuentes para detectar conexiones activas,
     * incluso si esta instancia específica no tiene la conexión directamente
     */
    fun isBleConnected(): Boolean {
        // 1. Verificar el estado del BleManager interno
        val isConnected = bleManagerInstance.isConnected.value
        val hasConnectedMac = bleManagerInstance.connectedDeviceMac != null
        
        // 2. Verificar el estado del HybridConnectionManager
        val isBleState = _connectionState.value == HybridConnectionState.BLE_CONNECTED
        
        // 3. Verificar si el dispositivo está cerca (indicador fuerte de conexión posible)
        val isNear = proximityManager.isNearKiln.value
        val nearbyKilns = proximityManager.nearbyKilns.value
        val hasNearbyKiln = nearbyKilns.isNotEmpty()
        
        // 4. Verificar si el BluetoothAdapter está habilitado (requisito básico)
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter
        val bluetoothEnabled = bluetoothAdapter?.isEnabled == true
        
        // Resultado: múltiples indicadores de conexión activa
        // Si el estado es BLE_CONNECTED, definitivamente hay conexión
        // Si hay un MAC conectado en el BleManager, hay conexión
        // Si el dispositivo está cerca Y hay hornos registrados, es muy probable que haya conexión
        val result = isConnected || hasConnectedMac || isBleState || (isNear && hasNearbyKiln && bluetoothEnabled)
        
        Log.d("HybridManager", "🔍 isBleConnected():")
        Log.d("HybridManager", "  - isConnected=$isConnected")
        Log.d("HybridManager", "  - hasConnectedMac=$hasConnectedMac")
        Log.d("HybridManager", "  - isBleState=$isBleState")
        Log.d("HybridManager", "  - isNear=$isNear")
        Log.d("HybridManager", "  - hasNearbyKiln=$hasNearbyKiln (${nearbyKilns.size} hornos)")
        Log.d("HybridManager", "  - bluetoothEnabled=$bluetoothEnabled")
        Log.d("HybridManager", "  - result=$result")
        return result
    }
    
    /**
     * Obtiene el MAC del dispositivo BLE conectado
     */
    fun getConnectedBleMac(): String? {
        return bleManagerInstance.connectedDeviceMac
    }
    
    /**
     * Obtiene el estado de conexión del BleManager interno
     */
    fun getBleConnectionState(): Boolean {
        return bleManagerInstance.isConnected.value
    }
}

/**
 * Modos de conexión para el sistema híbrido
 */
enum class HybridConnectionMode {
    WIFI,
    BLE
}
