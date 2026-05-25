package com.example.mysmartkiln.ble

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.example.mysmartkiln.data.repository.ProgramRepository
import com.example.mysmartkiln.data.local.entities.ProgramEntity
import com.example.mysmartkiln.data.local.entities.StageEntity
import com.example.mysmartkiln.data.local.database.BakingDatabase
import com.example.mysmartkiln.data.local.dao.ProgramDao
import com.example.mysmartkiln.data.local.dao.StageDao

/**
 * ViewModel para gestionar conexiones BLE con lógica híbrida:
 * - Conexión bajo demanda en modo IDLE
 * - Conexión continua durante programa activo
 */
class BleViewModel(application: Application) : AndroidViewModel(application) {
    
    private val bleManager = BleManager(application.applicationContext)
    
    // Exponer BleManager para inyección en HybridConnectionManager
    fun getBleManager(): BleManager = bleManager
    
    // Inicializar base de datos y repositorio
    private val database = BakingDatabase.getDatabase(application.applicationContext)
    private val programDao = database.programDao()
    private val stageDao = database.stageDao()
    private val programRepository = ProgramRepository(programDao, stageDao)
    
    // Estados del BLE Manager
    val isConnected = bleManager.isConnected
    val temperature = bleManager.temperature
    val status = bleManager.status
    val programName = bleManager.programName
    val scannedDevices = bleManager.scannedDevices
    val isScanning = bleManager.isScanning
    val connectionState = bleManager.connectionState
    val lastError = bleManager.lastError
    val connectedDeviceMac: String? get() = bleManager.connectedDeviceMac
    
    // Modo de conexión actual
    private val _connectionMode = MutableStateFlow<ConnectionMode>(ConnectionMode.OnDemand)
    val connectionMode: StateFlow<ConnectionMode> = _connectionMode.asStateFlow()
    
    // MAC address del horno actual
    private val _currentKilnMac = MutableStateFlow<String?>(null)
    val currentKilnMac: StateFlow<String?> = _currentKilnMac.asStateFlow()
    
    // Control de reconexión
    private var reconnectionJob: kotlinx.coroutines.Job? = null
    private val _reconnectionAttempts = MutableStateFlow(0)
    val reconnectionAttempts: StateFlow<Int> = _reconnectionAttempts.asStateFlow()
    
    /**
     * Escanea dispositivos BLE cercanos
     */
    fun scanDevices(durationMs: Long = 5000) {
        viewModelScope.launch {
            bleManager.startScan(durationMs)
        }
    }
    
    /**
     * Conecta a un dispositivo y guarda su MAC
     */
    fun connectToDevice(macAddress: String) {
        viewModelScope.launch {
            _currentKilnMac.value = macAddress
            val success = bleManager.connectByMac(macAddress)
            
            if (!success) {
                _currentKilnMac.value = null
            } else {
                // Esperar a que se establezca la conexión
                delay(2000)
            }
        }
    }
    
    /**
     * Envía un comando al ESP32
     * En modo OnDemand: conecta → envía → desconecta
     * En modo Continuous: solo envía
     */
    fun sendCommand(command: String, autoDisconnect: Boolean = false) {
        viewModelScope.launch {
            val macAddress = _currentKilnMac.value
            
            if (macAddress == null) {
                bleManager.clearError()
                return@launch
            }
            
            // Si no está conectado y es OnDemand, conectar primero
            if (!isConnected.value && _connectionMode.value is ConnectionMode.OnDemand) {
                val connected = bleManager.connectByMac(macAddress)
                if (!connected) {
                    return@launch
                }
                delay(2000)  // Esperar conexión
            }
            
            // Enviar comando
            val success = bleManager.sendCommand(command)
            
            // Desconectar si es necesario
            if (autoDisconnect && _connectionMode.value is ConnectionMode.OnDemand) {
                delay(500)  // Esperar respuesta
                bleManager.disconnect()
            }
        }
    }
    
    /**
     * Envía credenciales WiFi al ESP32
     */
    fun sendWifiConfig(wifiConfigJson: String) {
        viewModelScope.launch {
            val macAddress = _currentKilnMac.value
            
            if (macAddress == null) {
                bleManager.clearError()
                return@launch
            }
            
            // Si no está conectado, conectar primero
            if (!isConnected.value) {
                val connected = bleManager.connectByMac(macAddress)
                if (!connected) {
                    return@launch
                }
                delay(2000)  // Esperar conexión
            }
            
            // Enviar credenciales WiFi (el delay ya se maneja desde ConfigureKilnScreen)
            bleManager.sendWifiConfig(wifiConfigJson)
        }
    }
    
    /**
     * Lee temperatura y luego envía configuración WiFi
     * @param macAddress MAC address del horno
     * @param wifiConfigJson Configuración WiFi en JSON
     * @return Temperatura leída antes de enviar WiFi
     */
    fun readTemperatureAndSendWifi(macAddress: String, wifiConfigJson: String) {
        viewModelScope.launch {
            try {
                val temperature = bleManager.readTemperatureAndSendWifi(macAddress, wifiConfigJson)
                android.util.Log.d("BleViewModel", "🌡️ Temperatura leída durante configuración: $temperature°C")
            } catch (e: Exception) {
                android.util.Log.e("BleViewModel", "❌ Error en lectura + WiFi: ${e.message}")
            }
        }
    }
    
    /**
     * Inicia modo de conexión continua con reconexión automática
     * Se usa durante ejecución de programas
     */
    fun startContinuousMode(macAddress: String) {
        viewModelScope.launch {
            _connectionMode.value = ConnectionMode.Continuous
            _currentKilnMac.value = macAddress
            
            // Conectar si no está conectado
            if (!isConnected.value) {
                bleManager.connectByMac(macAddress)
                delay(2000)
            }
            
            // Iniciar reconexión automática
            startAutoReconnection(macAddress)
        }
    }
    
    /**
     * Detiene modo continuo y desconecta
     */
    fun stopContinuousMode() {
        viewModelScope.launch {
            _connectionMode.value = ConnectionMode.OnDemand
            stopAutoReconnection()
            bleManager.disconnect()
            _currentKilnMac.value = null
        }
    }
    
    /**
     * Inicia reconexión automática en caso de pérdida de conexión
     */
    private fun startAutoReconnection(macAddress: String) {
        reconnectionJob?.cancel()
        reconnectionJob = viewModelScope.launch {
            while (_connectionMode.value is ConnectionMode.Continuous) {
                
                // Si no está conectado, intentar reconectar
                if (!isConnected.value) {
                    _reconnectionAttempts.value++
                    
                    if (_reconnectionAttempts.value <= 10) {
                        bleManager.connectByMac(macAddress)
                        delay(5000)  // Reintentar cada 5 segundos
                    } else {
                        // Después de 10 intentos (50s), mostrar error
                        stopAutoReconnection()
                        break
                    }
                } else {
                    // Está conectado, resetear intentos
                    _reconnectionAttempts.value = 0
                    delay(1000)  // Verificar cada segundo
                }
            }
        }
    }
    
    /**
     * Detiene la reconexión automática
     */
    private fun stopAutoReconnection() {
        reconnectionJob?.cancel()
        reconnectionJob = null
        _reconnectionAttempts.value = 0
    }
    
    /**
     * Desconecta manualmente
     */
    fun disconnect() {
        viewModelScope.launch {
            stopAutoReconnection()
            bleManager.disconnect()
        }
    }
    
    /**
     * Limpia el error
     */
    fun clearError() {
        bleManager.clearError()
    }
    
    override fun onCleared() {
        super.onCleared()
        stopAutoReconnection()
        bleManager.disconnect()
    }
    
    /**
     * Limpia recursos BLE cuando se elimina un horno
     */
    fun cleanupBleResources() {
        bleManager.disconnect()
        bleManager.stopScan()
        android.util.Log.d("BleViewModel", "🧹 Recursos BLE limpiados completamente")
    }
    
    /**
     * Reinicia completamente el sistema BLE (para casos extremos)
     */
    fun resetBleSystem() {
        bleManager.disconnect()
        bleManager.stopScan()
        bleManager.clearError()
        android.util.Log.d("BleViewModel", "🔄 Sistema BLE reiniciado completamente")
    }
    
    /**
     * Envía un programa al horno conectado
     */
    suspend fun sendProgramToKiln(programId: String, kilnMacAddress: String) {
        android.util.Log.d("BleViewModel", "🎯 sendProgramToKiln llamado")
        android.util.Log.d("BleViewModel", "  - programId: $programId")
        android.util.Log.d("BleViewModel", "  - kilnMacAddress: $kilnMacAddress")
        android.util.Log.d("BleViewModel", "  - isConnected: ${isConnected.value}")
        
        if (!isConnected.value) {
            android.util.Log.e("BleViewModel", "❌ No hay conexión BLE activa")
            // Establecer error para que AppNavigation lo detecte
            bleManager.setError("No hay conexión BLE activa con el horno")
            return
        }
        
        try {
            // Obtener datos del programa desde la base de datos local
            val program = programRepository.getProgramById(programId)
            if (program == null) {
                android.util.Log.e("BleViewModel", "❌ Programa no encontrado: $programId")
                return
            }
            
            // Obtener las etapas del programa (Flow -> tomar solo el primer valor)
            val stages = programRepository.getStagesByProgram(programId)
            val stagesList = stages.first() // Obtener el primer valor del Flow (suspending)
            
            if (stagesList.isEmpty()) {
                android.util.Log.e("BleViewModel", "❌ El programa no tiene etapas: $programId")
                return
            }
            
            // Convertir a ProgramStage para crear el JSON
            val programStages = stagesList.sortedBy { it.stageNumber }.map { stage ->
                ProgramStage(
                    temperature = stage.temperature.toFloatOrNull() ?: 0f,
                    rampRate = stage.speed.toFloatOrNull() ?: 0f,
                    holdTime = stage.maintenance.toIntOrNull() ?: 0
                )
            }
            
            val programJson = createProgramJson(programId, program.name, programStages)
            android.util.Log.d("BleViewModel", "📋 Enviando programa JSON: $programJson")
            
            val success = bleManager.sendProgram(programJson)
            if (success) {
                android.util.Log.d("BleViewModel", "✅ Programa '${program.name}' enviado exitosamente")
            } else {
                android.util.Log.e("BleViewModel", "❌ Error al enviar programa")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("BleViewModel", "💥 Error al obtener programa: ${e.message}")
        }
    }
    
    /**
     * Crea el JSON del programa en el formato esperado por el ESP32
     */
    private fun createProgramJson(programId: String, programName: String, stages: List<ProgramStage>): String {
        val jsonBuilder = StringBuilder()
        jsonBuilder.append("LOAD_PROFILE:{")
        jsonBuilder.append("\"nombre\":\"$programName\",")
        jsonBuilder.append("\"numSeg\":${stages.size},") // Campo requerido por el ESP32
        jsonBuilder.append("\"seg\":[")
        
        stages.forEachIndexed { index, stage ->
            if (index > 0) jsonBuilder.append(",")
            jsonBuilder.append("{")
            jsonBuilder.append("\"temp\":${stage.temperature.toInt()},")
            jsonBuilder.append("\"rampa\":${stage.rampRate.toInt()},")
            jsonBuilder.append("\"remojo\":${stage.holdTime}")
            jsonBuilder.append("}")
        }
        
        jsonBuilder.append("]}")
        return jsonBuilder.toString()
    }
}

/**
 * Datos de una etapa del programa
 */
data class ProgramStage(
    val temperature: Float,  // Temperatura objetivo en °C
    val rampRate: Float,     // Velocidad de calentamiento en °C/min
    val holdTime: Int        // Tiempo de remojo en minutos
)

/**
 * Modos de conexión BLE
 */
sealed class ConnectionMode {
    /**
     * Conexión bajo demanda: conecta solo cuando necesita enviar comando
     */
    object OnDemand : ConnectionMode()
    
    /**
     * Conexión continua: mantiene BLE conectado con reconexión automática
     */
    object Continuous : ConnectionMode()
}

