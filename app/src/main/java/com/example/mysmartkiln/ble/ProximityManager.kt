package com.example.mysmartkiln.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager para detectar proximidad de hornos BLE
 * Detecta cuando el usuario está cerca (3-4 metros) de un horno
 */
class ProximityManager(private val context: Context) {
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    private val _isNearKiln = MutableStateFlow(false)
    val isNearKiln: StateFlow<Boolean> = _isNearKiln.asStateFlow()
    
    private val _nearbyKilns = MutableStateFlow<List<String>>(emptyList())
    val nearbyKilns: StateFlow<List<String>> = _nearbyKilns.asStateFlow()
    
    private var registeredKilns = listOf<String>()
    
    private var scanJob: Job? = null
    private val scanTimeout = 2000L // 2 segundos para detección rápida
    private val proximityThreshold = -65 // RSSI threshold para ~2-3 metros (más estricto)
    private var isBleConnected = false // Estado de conexión BLE para optimizar escaneo
    private var consecutiveErrors = 0 // Contador de errores consecutivos
    private val maxConsecutiveErrors = 3 // Máximo de errores antes de pausa larga
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi
            val deviceName = device.name ?: "Unknown"
            
            Log.d("ProximityManager", "🔍 Dispositivo encontrado: $deviceName (${device.address}) - RSSI: $rssi")
            
            // Resetear contador de errores en escaneo exitoso
            consecutiveErrors = 0
            
            // Normalizar MAC address del dispositivo detectado (quitar dos puntos y convertir a mayúsculas)
            val detectedMacNormalized = device.address.replace(":", "").uppercase()
            
            // Normalizar MACs registrados para comparación
            val normalizedRegisteredKilns = registeredKilns.map { it.replace(":", "").uppercase() }
            
            // Verificar si es un horno registrado y está cerca
            if (normalizedRegisteredKilns.contains(detectedMacNormalized) && rssi >= proximityThreshold) {
                Log.d("ProximityManager", "🎯 Horno registrado cerca detectado: $deviceName (${device.address}, RSSI: $rssi)")
                
                val currentKilns = _nearbyKilns.value.toMutableList()
                // Usar MAC con formato original (con dos puntos) para mantener consistencia
                val macToAdd = device.address.uppercase()
                if (!currentKilns.contains(macToAdd)) {
                    currentKilns.add(macToAdd)
                    _nearbyKilns.value = currentKilns
                    Log.d("ProximityManager", "✅ Agregado a nearbyKilns: $macToAdd")
                }
                
                if (!_isNearKiln.value) {
                    _isNearKiln.value = true
                    Log.d("ProximityManager", "✅ Usuario cerca del horno - Activando modo BLE")
                }
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            val errorMessage = when (errorCode) {
                1 -> "SCAN_FAILED_ALREADY_STARTED"
                2 -> "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED"
                3 -> "SCAN_FAILED_INTERNAL_ERROR"
                4 -> "SCAN_FAILED_FEATURE_UNSUPPORTED"
                5 -> "SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES"
                else -> "UNKNOWN_ERROR"
            }
            Log.e("ProximityManager", "❌ Error en escaneo de proximidad: $errorCode ($errorMessage)")
            
            consecutiveErrors++
            
            // Si es error 2 (APPLICATION_REGISTRATION_FAILED), implementar backoff exponencial
            if (errorCode == 2) {
                val waitTime = when {
                    consecutiveErrors <= 3 -> 5000L // 5 segundos para primeros errores
                    consecutiveErrors <= 6 -> 15000L // 15 segundos para errores medios
                    else -> 30000L // 30 segundos para muchos errores
                }
                
                Log.w("ProximityManager", "⚠️ Error de registro de aplicación ($consecutiveErrors consecutivos) - esperando ${waitTime/1000} segundos")
                
                // Pausar el escaneo por el tiempo calculado
                CoroutineScope(Dispatchers.IO).launch {
                    delay(waitTime)
                }
            } else {
                consecutiveErrors = 0 // Resetear contador para otros errores
            }
        }
    }
    
    /**
     * Inicia la detección de proximidad
     */
    fun startProximityDetection() {
        if (bluetoothLeScanner == null) {
            Log.e("ProximityManager", "❌ BLE Scanner no disponible")
            return
        }
        
        // Verificar si Bluetooth está habilitado
        if (bluetoothAdapter?.isEnabled != true) {
            Log.e("ProximityManager", "❌ Bluetooth no está habilitado")
            return
        }
        
        Log.d("ProximityManager", "🚀 Iniciando detección de proximidad...")
        
        scanJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // Si hay conexión BLE activa, reducir frecuencia de escaneo
                    if (isBleConnected) {
                        Log.d("ProximityManager", "🔵 BLE conectado - reduciendo frecuencia de escaneo")
                        delay(5000) // 5 segundos entre escaneos cuando BLE está conectado
                        continue
                    }
                    
                    // Limpiar lista de hornos cercanos
                    _nearbyKilns.value = emptyList()
                    _isNearKiln.value = false
                    
                    // Iniciar escaneo
                    Log.d("ProximityManager", "🔍 Iniciando escaneo BLE...")
                    bluetoothLeScanner.startScan(scanCallback)
                    Log.d("ProximityManager", "✅ Escaneo BLE iniciado")
                    
                    // Esperar timeout
                    delay(scanTimeout)
                    
                    // Detener escaneo
                    bluetoothLeScanner.stopScan(scanCallback)
                    
                    // Si no hay hornos cerca, esperar más tiempo antes del siguiente escaneo
                    if (_nearbyKilns.value.isEmpty()) {
                        delay(4000) // 4 segundos entre escaneos si no hay hornos cerca
                    } else {
                        delay(2000) // 2 segundos si hay hornos cerca (menos agresivo)
                    }
                    
                } catch (e: Exception) {
                    Log.e("ProximityManager", "❌ Error en detección de proximidad: ${e.message}")
                    delay(5000) // Esperar 5 segundos en caso de error
                }
            }
        }
    }
    
    /**
     * Detiene la detección de proximidad
     */
    fun stopProximityDetection() {
        Log.d("ProximityManager", "🛑 Deteniendo detección de proximidad...")
        
        scanJob?.cancel()
        scanJob = null
        
        bluetoothLeScanner?.stopScan(scanCallback)
        
        _isNearKiln.value = false
        _nearbyKilns.value = emptyList()
    }
    
    /**
     * Verifica si un horno específico está cerca
     */
    fun isKilnNearby(macAddress: String): Boolean {
        return _nearbyKilns.value.contains(macAddress)
    }
    
    /**
     * Obtiene la lista de hornos cercanos
     */
    fun getNearbyKilns(): List<String> {
        return _nearbyKilns.value
    }
    
    /**
     * Configura los hornos registrados para el escaneo de proximidad
     */
    fun setRegisteredKilns(macAddresses: List<String>) {
        registeredKilns = macAddresses
        Log.d("ProximityManager", "🎯 Hornos registrados para proximidad: $macAddresses")
    }
    
    /**
     * Actualiza el estado de conexión BLE para optimizar el escaneo
     */
    fun setBleConnectionState(connected: Boolean) {
        isBleConnected = connected
        Log.d("ProximityManager", "🔵 Estado BLE actualizado: ${if (connected) "CONECTADO" else "DESCONECTADO"}")
    }
    
    /**
     * Detiene el escaneo BLE
     */
    fun stopScanning() {
        try {
            bluetoothLeScanner?.stopScan(scanCallback)
            Log.d("ProximityManager", "🔍 Escaneo BLE detenido")
        } catch (e: Exception) {
            Log.e("ProximityManager", "❌ Error deteniendo escaneo: ${e.message}")
        }
    }
}
