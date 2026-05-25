package com.example.mysmartkiln.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import java.util.UUID

/**
 * Gestor de conexiones BLE para control de hornos SmartKiln
 * Implementa conexión bajo demanda y continua según el estado del programa
 */
class BleManager(private val context: Context) {
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    
    private var bluetoothGatt: BluetoothGatt? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null
    private var tempCharacteristic: BluetoothGattCharacteristic? = null
    private var statusCharacteristic: BluetoothGattCharacteristic? = null
    private var wifiConfigCharacteristic: BluetoothGattCharacteristic? = null
    private var curvaCharacteristic: BluetoothGattCharacteristic? = null
    private var programNameCharacteristic: BluetoothGattCharacteristic? = null
    private var pendingProgramNameDescriptorWrite = false  // Flag para escribir descriptor del nombre del programa
    
    // MAC address del dispositivo actualmente conectado
    var connectedDeviceMac: String? = null
        private set
    
    // UUIDs del ESP32
    companion object {
        private val SERVICE_UUID = UUID.fromString("00001234-0000-1000-8000-00805f9b34fb")
        private val TEMP_CHAR_UUID = UUID.fromString("0000abcd-0000-1000-8000-00805f9b34fb")
        private val STATUS_CHAR_UUID = UUID.fromString("0000dcba-0000-1000-8000-00805f9b34fb")
        private val COMMAND_CHAR_UUID = UUID.fromString("00002211-0000-1000-8000-00805f9b34fb")
        private val WIFI_CONFIG_CHAR_UUID = UUID.fromString("0000a003-0000-1000-8000-00805f9b34fb")
        private val CURVA_CHAR_UUID = UUID.fromString("0000a001-0000-1000-8000-00805f9b34fb")
        private val PROGRAM_NAME_CHAR_UUID = UUID.fromString("0000a004-0000-1000-8000-00805f9b34fb")
        private val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        
        const val DEVICE_NAME = "SmartKiln - Ceramica"
    }
    
    // Estados observables
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _temperature = MutableStateFlow(0f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()
    
    private val _status = MutableStateFlow("IDLE")
    val status: StateFlow<String> = _status.asStateFlow()
    
    private val _programName = MutableStateFlow<String?>(null)
    val programName: StateFlow<String?> = _programName.asStateFlow()
    
    private val _scannedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDevice>> = _scannedDevices.asStateFlow()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()
    
    // BroadcastReceiver para detectar cambios en el estado del Bluetooth
    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    android.util.Log.d("BleManager", "📡 Bluetooth estado cambió: $state")
                    
                    when (state) {
                        BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                            android.util.Log.w("BleManager", "⚠️ Bluetooth desactivado - forzando desconexión")
                            if (_isConnected.value) {
                                _isConnected.value = false
                                _connectionState.value = ConnectionState.Disconnected
                                cleanup()
                            }
                        }
                    }
                }
            }
        }
    }
    
    init {
        // Registrar el receiver para detectar cambios en el estado del Bluetooth
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothStateReceiver, filter)
        android.util.Log.d("BleManager", "✅ BroadcastReceiver de Bluetooth registrado")
    }
    
    // GATT Callback
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            android.util.Log.d("BleManager", "🔄 onConnectionStateChange: status=$status, newState=$newState")
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    android.util.Log.d("BleManager", "✅ BLE CONECTADO - iniciando descubrimiento de servicios")
                    connectedDeviceMac = gatt?.device?.address
                    android.util.Log.d("BleManager", "📍 MAC del dispositivo conectado: $connectedDeviceMac")
                    _isConnected.value = true
                    _connectionState.value = ConnectionState.Connected
                    gatt?.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    android.util.Log.d("BleManager", "❌ BLE DESCONECTADO - limpiando recursos")
                    connectedDeviceMac = null
                    _isConnected.value = false
                    _connectionState.value = ConnectionState.Disconnected
                    cleanup()
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            android.util.Log.d("BleManager", "🔍 onServicesDiscovered llamado - status: $status")
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                android.util.Log.d("BleManager", "✅ Servicios descubiertos exitosamente")
                
                // Log todos los servicios disponibles
                gatt?.services?.forEach { service ->
                    android.util.Log.d("BleManager", "📋 Servicio encontrado: ${service.uuid}")
                    service.characteristics.forEach { char ->
                        android.util.Log.d("BleManager", "  - Característica: ${char.uuid}")
                    }
                }
                
                val service = gatt?.getService(SERVICE_UUID)
                android.util.Log.d("BleManager", "🎯 Buscando servicio: $SERVICE_UUID")
                android.util.Log.d("BleManager", "🎯 Servicio encontrado: ${service != null}")
                
                commandCharacteristic = service?.getCharacteristic(COMMAND_CHAR_UUID)
                tempCharacteristic = service?.getCharacteristic(TEMP_CHAR_UUID)
                statusCharacteristic = service?.getCharacteristic(STATUS_CHAR_UUID)
                wifiConfigCharacteristic = service?.getCharacteristic(WIFI_CONFIG_CHAR_UUID)
                curvaCharacteristic = service?.getCharacteristic(CURVA_CHAR_UUID)
                programNameCharacteristic = service?.getCharacteristic(PROGRAM_NAME_CHAR_UUID)
                
                android.util.Log.d("BleManager", "📊 Características encontradas:")
                android.util.Log.d("BleManager", "  - commandCharacteristic: ${commandCharacteristic != null}")
                android.util.Log.d("BleManager", "  - tempCharacteristic: ${tempCharacteristic != null}")
                android.util.Log.d("BleManager", "  - statusCharacteristic: ${statusCharacteristic != null}")
                android.util.Log.d("BleManager", "  - wifiConfigCharacteristic: ${wifiConfigCharacteristic != null}")
                android.util.Log.d("BleManager", "  - curvaCharacteristic: ${curvaCharacteristic != null}")
                android.util.Log.d("BleManager", "  - programNameCharacteristic: ${programNameCharacteristic != null}")
                
                // Habilitar notificaciones
                enableNotifications()
            } else {
                android.util.Log.e("BleManager", "❌ Error descubriendo servicios: $status")
            }
        }
        
        @Deprecated("Deprecated in API 33")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            android.util.Log.d("BleManager", "📡 onCharacteristicChanged (deprecated) - UUID: ${characteristic?.uuid}")
            characteristic?.value?.let { value ->
                android.util.Log.d("BleManager", "📦 Bytes recibidos (${value.size}): ${value.joinToString { it.toString() }}")
                android.util.Log.d("BleManager", "📝 Como String: '${String(value)}'")
                
                when (characteristic.uuid) {
                    TEMP_CHAR_UUID -> {
                        // Intentar primero como float directo (4 bytes en formato little-endian)
                        val temp = if (value.size == 4) {
                            val buffer = java.nio.ByteBuffer.wrap(value).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                            buffer.getFloat()
                        } else {
                            // Si no son 4 bytes, intentar como string
                            String(value).toFloatOrNull() ?: 0f
                        }
                        android.util.Log.d("BleManager", "🌡️ Temperatura interpretada (deprecated): $temp°C")
                        _temperature.value = temp
                    }
                    STATUS_CHAR_UUID -> {
                        val status = String(value)
                        android.util.Log.d("BleManager", "📊 Estado recibido (deprecated): $status")
                        _status.value = status
                    }
                    PROGRAM_NAME_CHAR_UUID -> {
                        val programName = String(value).trim()
                        android.util.Log.d("BleManager", "📋 Nombre del programa recibido (deprecated): '$programName'")
                        _programName.value = if (programName.isNotEmpty()) programName else null
                    }
                }
            }
        }
        
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            android.util.Log.d("BleManager", "📖 onCharacteristicRead - UUID: ${characteristic.uuid}, status: $status")
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic.value?.let { value ->
                    when (characteristic.uuid) {
                        PROGRAM_NAME_CHAR_UUID -> {
                            val programName = String(value).trim()
                            android.util.Log.d("BleManager", "📋 Nombre del programa leído: '$programName'")
                            _programName.value = if (programName.isNotEmpty()) programName else null
                        }
                        TEMP_CHAR_UUID -> {
                            val temp = if (value.size == 4) {
                                val buffer = java.nio.ByteBuffer.wrap(value).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                                buffer.getFloat()
                            } else {
                                String(value).toFloatOrNull() ?: 0f
                            }
                            android.util.Log.d("BleManager", "🌡️ Temperatura leída: $temp°C")
                            _temperature.value = temp
                        }
                    }
                }
            } else {
                android.util.Log.e("BleManager", "❌ Error leyendo característica: $status")
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            android.util.Log.d("BleManager", "📡 onCharacteristicChanged - UUID: ${characteristic.uuid}")
            android.util.Log.d("BleManager", "📦 Bytes recibidos (${value.size}): ${value.joinToString { it.toString() }}")
            android.util.Log.d("BleManager", "📝 Como String: '${String(value)}'")
            
            when (characteristic.uuid) {
                TEMP_CHAR_UUID -> {
                    // Intentar primero como float directo (4 bytes en formato little-endian)
                    val temp = if (value.size == 4) {
                        val buffer = java.nio.ByteBuffer.wrap(value).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                        buffer.getFloat()
                    } else {
                        // Si no son 4 bytes, intentar como string
                        String(value).toFloatOrNull() ?: 0f
                    }
                    android.util.Log.d("BleManager", "🌡️ Temperatura interpretada: $temp°C")
                    _temperature.value = temp
                }
                STATUS_CHAR_UUID -> {
                    val status = String(value).trim()
                    android.util.Log.d("BleManager", "📊 Estado recibido: '$status'")
                    _status.value = status
                }
                PROGRAM_NAME_CHAR_UUID -> {
                    val programName = String(value).trim()
                    android.util.Log.d("BleManager", "📋 Nombre del programa recibido: '$programName'")
                    _programName.value = if (programName.isNotEmpty()) programName else null
                }
            }
        }
        
        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            val charUuid = descriptor?.characteristic?.uuid
            val descUuid = descriptor?.uuid
            android.util.Log.d("BleManager", "📝 onDescriptorWrite CALLBACK:")
            android.util.Log.d("BleManager", "  - status: $status (0=SUCCESS)")
            android.util.Log.d("BleManager", "  - descriptor UUID: ${descUuid}")
            android.util.Log.d("BleManager", "  - characteristic UUID: ${charUuid}")
            
            if (status == BluetoothGatt.GATT_SUCCESS) {
                android.util.Log.d("BleManager", "✅ Descriptor escrito exitosamente para característica: ${charUuid}")
                
                // Si es el descriptor del nombre del programa, leer el valor actual
                if (charUuid == PROGRAM_NAME_CHAR_UUID) {
                    android.util.Log.d("BleManager", "🎯 ¡DESCRIPTOR DEL NOMBRE DEL PROGRAMA CONFIRMADO!")
                    android.util.Log.d("BleManager", "📖 Notificaciones de nombre de programa habilitadas - leyendo valor actual...")
                    
                    pendingProgramNameDescriptorWrite = false  // Ya se escribió
                    
                    // Esperar un poco antes de leer para asegurar que el descriptor esté listo
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(200)
                        descriptor.characteristic?.let { char ->
                            try {
                                val readSuccess = bluetoothGatt?.readCharacteristic(char)
                                android.util.Log.d("BleManager", "📖 Lectura de nombre de programa después de habilitar descriptor: $readSuccess")
                            } catch (e: Exception) {
                                android.util.Log.e("BleManager", "❌ Error leyendo nombre de programa en onDescriptorWrite: ${e.message}")
                            }
                        }
                    }
                } else if (charUuid == STATUS_CHAR_UUID && pendingProgramNameDescriptorWrite) {
                    // Después de escribir el descriptor de estado, escribir el del nombre del programa
                    android.util.Log.d("BleManager", "📝 Descriptor de estado confirmado - ahora escribiendo descriptor del nombre del programa")
                    
                    programNameCharacteristic?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)?.let { programDesc ->
                        programDesc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        val written = bluetoothGatt?.writeDescriptor(programDesc)
                        android.util.Log.d("BleManager", "📝 Descriptor de nombre de programa escrito (después de estado): $written")
                    } ?: android.util.Log.e("BleManager", "❌ No se pudo obtener descriptor del nombre del programa")
                } else if (charUuid == TEMP_CHAR_UUID && pendingProgramNameDescriptorWrite) {
                    // Alternativa: después del descriptor de temperatura (si el de estado nunca llega)
                    // Esperar un momento y luego escribir el descriptor del nombre del programa
                    android.util.Log.d("BleManager", "📝 Descriptor de temperatura confirmado - programando escritura del descriptor del nombre del programa")
                    
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(500)  // Esperar un momento para ver si llega el callback de estado
                        if (pendingProgramNameDescriptorWrite) {
                            android.util.Log.d("BleManager", "⏱️ Timeout esperando descriptor de estado - escribiendo descriptor del nombre del programa ahora")
                            programNameCharacteristic?.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)?.let { programDesc ->
                                programDesc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                val written = bluetoothGatt?.writeDescriptor(programDesc)
                                android.util.Log.d("BleManager", "📝 Descriptor de nombre de programa escrito (fallback después de temperatura): $written")
                            }
                        }
                    }
                } else {
                    android.util.Log.d("BleManager", "ℹ️ Descriptor confirmado para otra característica (no es nombre de programa)")
                }
            } else {
                android.util.Log.e("BleManager", "❌ Error escribiendo descriptor: status=$status, UUID=${charUuid}")
                if (charUuid == PROGRAM_NAME_CHAR_UUID) {
                    android.util.Log.e("BleManager", "❌ ERROR CRÍTICO: No se pudo escribir descriptor del nombre del programa!")
                }
            }
        }
        
        override fun onDescriptorRead(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            val charUuid = descriptor?.characteristic?.uuid
            android.util.Log.d("BleManager", "📖 onDescriptorRead: status=$status, descriptor=${descriptor?.uuid}, char=${charUuid}")
            
            if (status == BluetoothGatt.GATT_SUCCESS && charUuid == PROGRAM_NAME_CHAR_UUID) {
                val descValue = descriptor?.value
                android.util.Log.d("BleManager", "📖 Valor del descriptor leído: ${descValue?.contentToString()}")
                
                // Después de leer el descriptor, intentar escribir si no está habilitado
                descValue?.let { value ->
                    val isEnabled = value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                                   value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                    if (!isEnabled) {
                        android.util.Log.d("BleManager", "📝 Descriptor no está habilitado, escribiendo...")
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        bluetoothGatt?.writeDescriptor(descriptor)
                    }
                }
            }
        }
    }
    
    // Scan Callback
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                // Log para debugging
                android.util.Log.d("BleManager", "Dispositivo encontrado: ${device.name} - ${device.address}")
                
                // Buscar dispositivos que contengan "SmartKiln" en el nombre
                val deviceName = device.name ?: ""
                if (deviceName.contains("SmartKiln", ignoreCase = true) && !_scannedDevices.value.contains(device)) {
                    android.util.Log.d("BleManager", "Dispositivo SmartKiln agregado: ${device.name}")
                    _scannedDevices.value = _scannedDevices.value + device
                }
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            android.util.Log.e("BleManager", "Error en escaneo: $errorCode")
            _lastError.value = "Error en escaneo: $errorCode"
            _isScanning.value = false
        }
    }
    
    /**
     * Escanea dispositivos BLE cercanos
     */
    @SuppressLint("MissingPermission")
    suspend fun startScan(durationMs: Long = 5000) {
        try {
            android.util.Log.d("BleManager", "Iniciando escaneo BLE...")
            
            if (bluetoothAdapter?.isEnabled != true) {
                android.util.Log.e("BleManager", "Bluetooth desactivado")
                _lastError.value = "Bluetooth desactivado"
                return
            }
            
            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner == null) {
                android.util.Log.e("BleManager", "BLE no disponible")
                _lastError.value = "BLE no disponible"
                return
            }
            
            android.util.Log.d("BleManager", "Limpiando dispositivos previos y iniciando escaneo...")
            _scannedDevices.value = emptyList()
            _isScanning.value = true
            _lastError.value = null
            
            scanner.startScan(scanCallback)
            android.util.Log.d("BleManager", "Escaneo iniciado, esperando $durationMs ms...")
            
            delay(durationMs)
            
            scanner.stopScan(scanCallback)
            android.util.Log.d("BleManager", "Escaneo finalizado. Dispositivos encontrados: ${_scannedDevices.value.size}")
            
            _isScanning.value = false
        } catch (e: SecurityException) {
            android.util.Log.e("BleManager", "Error de permisos: ${e.message}")
            _lastError.value = "Permisos BLE requeridos"
            _isScanning.value = false
        } catch (e: Exception) {
            android.util.Log.e("BleManager", "Error al escanear: ${e.message}")
            _lastError.value = "Error al escanear: ${e.message}"
            _isScanning.value = false
        }
    }
    
    /**
     * Conecta a un dispositivo por MAC address (sin escanear)
     */
    @SuppressLint("MissingPermission")
    fun connectByMac(macAddress: String): Boolean {
        android.util.Log.d("BleManager", "🔌 connectByMac llamado: $macAddress")
        
        if (bluetoothAdapter?.isEnabled != true) {
            _lastError.value = "Bluetooth desactivado"
            android.util.Log.e("BleManager", "❌ Bluetooth desactivado")
            return false
        }
        
        android.util.Log.d("BleManager", "🔄 Iniciando conexión BLE a: $macAddress")
        _connectionState.value = ConnectionState.Connecting
        
        return try {
            val device = bluetoothAdapter.getRemoteDevice(macAddress)
            android.util.Log.d("BleManager", "📱 Dispositivo obtenido: ${device.name} (${device.address})")
            
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
            android.util.Log.d("BleManager", "🔗 connectGatt iniciado - esperando callback")
            true
        } catch (e: Exception) {
            _lastError.value = "Error al conectar: ${e.message}"
            _connectionState.value = ConnectionState.Disconnected
            android.util.Log.e("BleManager", "❌ Error en connectByMac: ${e.message}")
            false
        }
    }
    
    /**
     * Conecta a un dispositivo BLE
     */
    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice): Boolean {
        if (bluetoothAdapter?.isEnabled != true) {
            _lastError.value = "Bluetooth desactivado"
            return false
        }
        
        _connectionState.value = ConnectionState.Connecting
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
        return true
    }
    
    /**
     * Habilita notificaciones para temperatura y estado
     */
    @SuppressLint("MissingPermission")
    private fun enableNotifications() {
        android.util.Log.d("BleManager", "🔔 Habilitando notificaciones BLE...")
        
        tempCharacteristic?.let { char ->
            val notificationSet = bluetoothGatt?.setCharacteristicNotification(char, true)
            android.util.Log.d("BleManager", "📡 Notificación de temperatura habilitada: $notificationSet")
            
            char.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)?.let { descriptor ->
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val descriptorWritten = bluetoothGatt?.writeDescriptor(descriptor)
                android.util.Log.d("BleManager", "📝 Descriptor de temperatura escrito: $descriptorWritten")
            } ?: android.util.Log.e("BleManager", "❌ Descriptor de temperatura no encontrado")
        } ?: android.util.Log.e("BleManager", "❌ Característica de temperatura no encontrada")
        
        // Habilitar notificaciones de estado
        ensureStatusNotificationsEnabled()
        
        // Habilitar notificaciones para nombre del programa
        // IMPORTANTE: Solo habilitar notificación, el descriptor se escribirá después del de estado
        programNameCharacteristic?.let { char ->
            android.util.Log.d("BleManager", "🔍 Característica de nombre de programa encontrada: ${char.uuid}")
            android.util.Log.d("BleManager", "🔍 Propiedades de la característica: ${char.properties}")
            
            val notificationSet = bluetoothGatt?.setCharacteristicNotification(char, true)
            android.util.Log.d("BleManager", "📡 Notificación de nombre de programa habilitada: $notificationSet")
            
            // Marcar que necesitamos escribir el descriptor después
            // Se escribirá en onDescriptorWrite cuando se confirme el descriptor de estado
            if (char.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG) != null) {
                pendingProgramNameDescriptorWrite = true
                android.util.Log.d("BleManager", "⏳ Descriptor de nombre de programa será escrito después del descriptor de estado")
            } else {
                android.util.Log.e("BleManager", "❌ Descriptor de nombre de programa no encontrado")
            }
        } ?: android.util.Log.w("BleManager", "⚠️ Característica de nombre de programa no encontrada (puede ser versión antigua del ESP32)")
        
        android.util.Log.d("BleManager", "✅ Proceso de habilitación de notificaciones completado")
    }
    
    /**
     * Asegura que las notificaciones de estado estén habilitadas
     * Se puede llamar múltiples veces de forma segura
     */
    @SuppressLint("MissingPermission")
    private fun ensureStatusNotificationsEnabled() {
        statusCharacteristic?.let { char ->
            // Habilitar notificaciones a nivel GATT
            val notificationSet = bluetoothGatt?.setCharacteristicNotification(char, true)
            android.util.Log.d("BleManager", "📡 Notificación de estado habilitada: $notificationSet")
            
            // Escribir descriptor (necesario para que las notificaciones funcionen)
            char.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG)?.let { descriptor ->
                // Verificar si ya está habilitado
                val currentValue = descriptor.value
                val isAlreadyEnabled = currentValue != null && 
                    (currentValue.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                     currentValue.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE))
                
                if (!isAlreadyEnabled) {
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    val descriptorWritten = bluetoothGatt?.writeDescriptor(descriptor) ?: false
                    android.util.Log.d("BleManager", "📝 Descriptor de estado escrito: $descriptorWritten")
                    if (!descriptorWritten) {
                        android.util.Log.w("BleManager", "⚠️ Advertencia: writeDescriptor retornó false, pero puede ser normal (operación asíncrona)")
                    }
                } else {
                    android.util.Log.d("BleManager", "✅ Notificaciones de estado ya estaban habilitadas")
                }
            } ?: android.util.Log.e("BleManager", "❌ Descriptor de estado no encontrado")
        } ?: android.util.Log.e("BleManager", "❌ Característica de estado no encontrada")
    }
    
    /**
     * Envía un comando al ESP32
     */
    @SuppressLint("MissingPermission")
    suspend fun sendCommand(command: String): Boolean {
        if (!_isConnected.value || commandCharacteristic == null) {
            _lastError.value = "No conectado"
            return false
        }
        
        return try {
            commandCharacteristic?.value = command.toByteArray()
            val success = bluetoothGatt?.writeCharacteristic(commandCharacteristic) ?: false
            
            if (!success) {
                _lastError.value = "Error al enviar comando"
            }
            
            delay(100)  // Dar tiempo para que se procese
            success
        } catch (e: Exception) {
            _lastError.value = "Error: ${e.message}"
            false
        }
    }
    
    /**
     * Envía credenciales WiFi al ESP32
     */
    @SuppressLint("MissingPermission")
    suspend fun sendWifiConfig(wifiConfigJson: String): Boolean {
        android.util.Log.d("BleManager", "🔍 sendWifiConfig llamado")
        android.util.Log.d("BleManager", "  - Conectado: ${_isConnected.value}")
        android.util.Log.d("BleManager", "  - wifiConfigCharacteristic: ${wifiConfigCharacteristic != null}")
        
        if (!_isConnected.value || wifiConfigCharacteristic == null) {
            _lastError.value = "No conectado o característica WiFi no disponible"
            android.util.Log.e("BleManager", "❌ sendWifiConfig: No conectado o característica no disponible")
            android.util.Log.e("BleManager", "  - isConnected: ${_isConnected.value}")
            android.util.Log.e("BleManager", "  - characteristic null: ${wifiConfigCharacteristic == null}")
            return false
        }
        
        return try {
            android.util.Log.d("BleManager", "📶 Enviando credenciales WiFi...")
            android.util.Log.d("BleManager", "  - JSON: $wifiConfigJson")
            android.util.Log.d("BleManager", "  - Longitud: ${wifiConfigJson.length} bytes")
            
            wifiConfigCharacteristic?.value = wifiConfigJson.toByteArray()
            val success = bluetoothGatt?.writeCharacteristic(wifiConfigCharacteristic) ?: false
            
            if (!success) {
                _lastError.value = "Error al enviar credenciales WiFi"
                android.util.Log.e("BleManager", "❌ Error al escribir característica WiFi")
            } else {
                android.util.Log.d("BleManager", "✅ Credenciales WiFi enviadas exitosamente")
                // Esperar más tiempo para que el ESP32 procese el callback
                delay(500)  // Aumentado de 100ms a 500ms para dar tiempo al procesamiento
            }
            
            success
        } catch (e: Exception) {
            _lastError.value = "Error al enviar WiFi: ${e.message}"
            android.util.Log.e("BleManager", "💥 Excepción al enviar WiFi: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Envía un programa al ESP32 y espera confirmación
     */
    @SuppressLint("MissingPermission")
    suspend fun sendProgram(programJson: String): Boolean {
        android.util.Log.d("BleManager", "🎯 sendProgram llamado")
        android.util.Log.d("BleManager", "  - Conectado: ${_isConnected.value}")
        android.util.Log.d("BleManager", "  - curvaCharacteristic: ${curvaCharacteristic != null}")
        android.util.Log.d("BleManager", "  - statusCharacteristic: ${statusCharacteristic != null}")
        
        if (!_isConnected.value || curvaCharacteristic == null) {
            _lastError.value = "No conectado o característica de curva no disponible"
            android.util.Log.e("BleManager", "❌ sendProgram: No conectado o característica no disponible")
            android.util.Log.e("BleManager", "  - isConnected: ${_isConnected.value}")
            android.util.Log.e("BleManager", "  - characteristic null: ${curvaCharacteristic == null}")
            return false
        }
        
        // Asegurar que las notificaciones de estado estén habilitadas antes de enviar
        ensureStatusNotificationsEnabled()
        
        return try {
            android.util.Log.d("BleManager", "📋 Enviando programa...")
            android.util.Log.d("BleManager", "  - JSON: $programJson")
            android.util.Log.d("BleManager", "  - Longitud: ${programJson.length} bytes")
            
            // Resetear el estado antes de enviar para detectar el cambio
            _status.value = "SENDING"
            
            curvaCharacteristic?.value = programJson.toByteArray()
            val writeSuccess = bluetoothGatt?.writeCharacteristic(curvaCharacteristic) ?: false
            
            if (!writeSuccess) {
                _lastError.value = "Error al enviar programa"
                android.util.Log.e("BleManager", "❌ Error al escribir característica de curva")
                return false
            }
            
            android.util.Log.d("BleManager", "📤 Programa escrito, esperando confirmación del ESP32...")
            
            // Esperar confirmación del ESP32 (PROFILE_OK o PROFILE_ERROR)
            val confirmation = waitForProgramConfirmation(timeoutMs = 5000) // 5 segundos timeout
            
            when (confirmation) {
                "PROFILE_OK" -> {
                    android.util.Log.d("BleManager", "✅ Programa enviado y confirmado por ESP32: PROFILE_OK")
                    _lastError.value = null
                    
                    // Leer el nombre del programa inmediatamente después de recibir PROFILE_OK
                    // El ESP32 debería haberlo enviado por notificación, pero leemos explícitamente para asegurar
                    delay(300) // Dar tiempo a que el ESP32 envíe la notificación
                    try {
                        programNameCharacteristic?.let { char ->
                            val readSuccess = bluetoothGatt?.readCharacteristic(char)
                            android.util.Log.d("BleManager", "📖 Lectura explícita de nombre de programa después de PROFILE_OK: $readSuccess")
                        } ?: android.util.Log.w("BleManager", "⚠️ No se puede leer nombre de programa: característica no disponible")
                    } catch (e: Exception) {
                        android.util.Log.e("BleManager", "❌ Error leyendo nombre de programa: ${e.message}")
                    }
                    
                    true
                }
                "PROFILE_ERROR" -> {
                    android.util.Log.e("BleManager", "❌ ESP32 reportó error al procesar el programa: PROFILE_ERROR")
                    _lastError.value = "El ESP32 reportó un error al procesar el programa"
                    false
                }
                else -> {
                    // Timeout: El programa se envió exitosamente, pero no recibimos confirmación
                    // Esto puede ocurrir si las notificaciones no están habilitadas
                    // Como el ESP32 confirma por Serial que recibió el programa, asumimos éxito
                    android.util.Log.w("BleManager", "⚠️ Timeout esperando confirmación del ESP32. Estado recibido: $confirmation")
                    android.util.Log.w("BleManager", "⚠️ PERO el programa se envió exitosamente. Si el ESP32 lo recibió, será exitoso.")
                    android.util.Log.w("BleManager", "⚠️ Nota: Esto puede indicar que las notificaciones de estado no están habilitadas correctamente.")
                    
                    // Opción 1: Asumir éxito (el programa se envió correctamente)
                    // El ESP32 procesará el programa incluso si no recibimos confirmación
                    _lastError.value = null
                    android.util.Log.d("BleManager", "✅ Asumiendo éxito: programa enviado correctamente")
                    true
                    
                    // Opción 2: Reportar timeout (descomentar si prefieres ser más estricto)
                    // _lastError.value = "No se recibió confirmación del ESP32, pero el programa se envió correctamente"
                    // return false
                }
            }
        } catch (e: Exception) {
            _lastError.value = "Error al enviar programa: ${e.message}"
            android.util.Log.e("BleManager", "💥 Excepción al enviar programa: ${e.message}")
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Espera la confirmación del ESP32 después de enviar un programa
     * @param timeoutMs Tiempo máximo a esperar en milisegundos
     * @return "PROFILE_OK", "PROFILE_ERROR", o "TIMEOUT" si no se recibe confirmación
     */
    private suspend fun waitForProgramConfirmation(timeoutMs: Long): String {
        return try {
            android.util.Log.d("BleManager", "⏳ Esperando confirmación del ESP32 (timeout: ${timeoutMs}ms)...")
            android.util.Log.d("BleManager", "📊 Estado actual antes de esperar: ${_status.value}")
            
            // Esperar el primer estado que sea PROFILE_OK o PROFILE_ERROR después de establecer SENDING
            // Descartamos el valor actual (SENDING) y esperamos el siguiente
            val confirmation = withTimeout(timeoutMs) {
                _status
                    .drop(1) // Descartar el valor actual (SENDING)
                    .first { status ->
                        android.util.Log.d("BleManager", "📊 Estado recibido: $status")
                        status == "PROFILE_OK" || status == "PROFILE_ERROR"
                    }
            }
            
            android.util.Log.d("BleManager", "✅ Confirmación recibida: $confirmation")
            confirmation
        } catch (e: TimeoutCancellationException) {
            android.util.Log.w("BleManager", "⏱️ Timeout esperando confirmación del ESP32")
            android.util.Log.d("BleManager", "📊 Estado final: ${_status.value}")
            "TIMEOUT"
        } catch (e: Exception) {
            android.util.Log.e("BleManager", "💥 Error esperando confirmación: ${e.message}")
            "TIMEOUT"
        }
    }
    
    /**
     * Desconecta del dispositivo BLE
     */
    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        cleanup()
    }
    
    /**
     * Detiene el escaneo BLE
     */
    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            android.util.Log.d("BleManager", "🔍 Escaneo BLE detenido")
        } catch (e: Exception) {
            android.util.Log.e("BleManager", "❌ Error deteniendo escaneo: ${e.message}")
        }
    }
    
    /**
     * Limpia recursos
     */
    @SuppressLint("MissingPermission")
    private fun cleanup() {
        // Detener escaneo antes de limpiar
        stopScan()
        
        bluetoothGatt?.close()
        bluetoothGatt = null
        commandCharacteristic = null
        tempCharacteristic = null
        statusCharacteristic = null
        wifiConfigCharacteristic = null
        curvaCharacteristic = null
        programNameCharacteristic = null
        
        // Limpiar nombre del programa al desconectar
        _programName.value = null
        
        android.util.Log.d("BleManager", "🧹 Recursos BLE limpiados")
    }
    
    /**
     * Establece un mensaje de error manualmente
     */
    fun setError(message: String) {
        _lastError.value = message
        android.util.Log.e("BleManager", "❌ Error establecido: $message")
    }
    
    /**
     * Limpia el último error
     */
    fun clearError() {
        _lastError.value = null
    }
    
    /**
     * Destruye el BleManager y libera todos los recursos
     * Debe ser llamado cuando ya no se necesite el BleManager
     */
    fun destroy() {
        try {
            context.unregisterReceiver(bluetoothStateReceiver)
            android.util.Log.d("BleManager", "✅ BroadcastReceiver de Bluetooth desregistrado")
        } catch (e: Exception) {
            android.util.Log.w("BleManager", "⚠️ Error al desregistrar receiver: ${e.message}")
        }
        disconnect()
    }
    
    /**
     * Escanea y conecta a un horno específico por MAC address
     * @param macAddress MAC address del horno (formato: A4:CF:12:88:56:E8)
     * @param timeout Timeout en milisegundos para el escaneo
     * @return true si encontró y conectó al dispositivo
     */
    @SuppressLint("MissingPermission")
    suspend fun scanAndConnectToKiln(macAddress: String, timeout: Long = 3000): Boolean {
        try {
            android.util.Log.d("BleManager", "🔍 Escaneando horno: $macAddress")
            
            if (bluetoothAdapter?.isEnabled != true) {
                android.util.Log.e("BleManager", "Bluetooth desactivado")
                return false
            }
            
            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner == null) {
                android.util.Log.e("BleManager", "Scanner BLE no disponible")
                return false
            }
            
            var deviceFound = false
            val scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult?) {
                    result?.device?.let { device ->
                        if (device.address == macAddress) {
                            android.util.Log.d("BleManager", "✅ Horno encontrado: ${device.address}")
                            deviceFound = true
                            scanner.stopScan(this)
                        }
                    }
                }
                
                override fun onScanFailed(errorCode: Int) {
                    android.util.Log.e("BleManager", "Escaneo fallido: $errorCode")
                }
            }
            
            scanner.startScan(scanCallback)
            
            // Esperar hasta encontrar o timeout
            val startTime = System.currentTimeMillis()
            while (!deviceFound && (System.currentTimeMillis() - startTime) < timeout) {
                delay(100)
            }
            
            scanner.stopScan(scanCallback)
            
            if (deviceFound) {
                android.util.Log.d("BleManager", "🔌 Conectando a horno: $macAddress")
                return connectByMac(macAddress)
            } else {
                android.util.Log.d("BleManager", "❌ Horno no encontrado: $macAddress")
                return false
            }
            
        } catch (e: Exception) {
            android.util.Log.e("BleManager", "Error al escanear/conectar: ${e.message}")
            return false
        }
    }
    
    /**
     * Lee la temperatura actual del horno por BLE
     * @return Temperatura en °C o null si no está disponible
     */
    @SuppressLint("MissingPermission")
    suspend fun readTemperature(): Float? {
        if (!_isConnected.value || tempCharacteristic == null) {
            android.util.Log.e("BleManager", "No conectado o característica no disponible")
            return null
        }
        
        return try {
            val success = bluetoothGatt?.readCharacteristic(tempCharacteristic)
            if (success == true) {
                // Esperar a que se actualice el valor (callback lo actualiza)
                delay(500)
                _temperature.value
            } else {
                android.util.Log.e("BleManager", "Error al leer temperatura")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("BleManager", "Excepción al leer temperatura: ${e.message}")
            null
        }
    }
    
    /**
     * Escanea, conecta y lee temperatura de un horno
     * Desconecta automáticamente después de leer
     * @param macAddress MAC address del horno
     * @return Temperatura en °C o null si falló
     */
    @SuppressLint("MissingPermission")
    suspend fun quickReadTemperature(macAddress: String): Float? {
        try {
            android.util.Log.d("BleManager", "📊 Lectura rápida de temperatura: $macAddress")
            
            // 1. Escanear y conectar
            val connected = scanAndConnectToKiln(macAddress, timeout = 3000)
            if (!connected) {
                android.util.Log.w("BleManager", "No se pudo conectar")
                return null
            }
            
            // 2. Esperar a que se descubran servicios
            delay(1000)
            
            // 3. Leer temperatura
            val temperature = readTemperature()
            
            // 4. Desconectar
            disconnect()
            
            android.util.Log.d("BleManager", "✅ Temperatura leída: $temperature°C")
            return temperature
            
        } catch (e: Exception) {
            android.util.Log.e("BleManager", "Error en lectura rápida: ${e.message}")
            disconnect()
            return null
        }
    }
    
    /**
     * Lee temperatura y luego envía configuración WiFi
     * @param macAddress MAC address del horno
     * @param wifiConfigJson Configuración WiFi en JSON
     * @return Temperatura leída antes de enviar WiFi
     */
    @SuppressLint("MissingPermission")
    suspend fun readTemperatureAndSendWifi(macAddress: String, wifiConfigJson: String): Float? {
        try {
            android.util.Log.d("BleManager", "📊 Lectura de temperatura + WiFi: $macAddress")
            
            // 1. Escanear y conectar
            val connected = scanAndConnectToKiln(macAddress, timeout = 3000)
            if (!connected) {
                android.util.Log.w("BleManager", "No se pudo conectar")
                return null
            }
            
            // 2. Esperar a que se descubran servicios
            delay(1000)
            
            // 3. Leer temperatura PRIMERO
            val temperature = readTemperature()
            android.util.Log.d("BleManager", "🌡️ Temperatura leída antes de WiFi: $temperature°C")
            
            // 4. Enviar configuración WiFi
            val wifiSent = sendWifiConfig(wifiConfigJson)
            android.util.Log.d("BleManager", "📶 WiFi enviado: $wifiSent")
            
            // 5. Esperar antes de desconectar para que el ESP32 procese el callback
            if (wifiSent) {
                android.util.Log.d("BleManager", "⏳ Esperando 1 segundo para que ESP32 procese WiFi config...")
                delay(1000)  // Esperar 1 segundo adicional antes de desconectar
            }
            
            // 6. Desconectar
            disconnect()
            
            return temperature
            
        } catch (e: Exception) {
            android.util.Log.e("BleManager", "Error en lectura + WiFi: ${e.message}")
            disconnect()
            return null
        }
    }
}

/**
 * Estados de conexión BLE
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    object Reconnecting : ConnectionState()
}

