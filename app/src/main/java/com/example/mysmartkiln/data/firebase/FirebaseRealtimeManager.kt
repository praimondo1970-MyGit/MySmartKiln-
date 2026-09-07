package com.example.mysmartkiln.data.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Gestor centralizado para operaciones con Firebase Realtime Database
 */
class FirebaseRealtimeManager {
    
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val reference: DatabaseReference = database.reference
    
    companion object {
        private const val TAG = "FirebaseRealtimeManager"
        private const val USERS_PATH = "users"
        private const val PROGRAMS_PATH = "programs"
        private const val KILNS_PATH = "kilns"
        private const val ACTIVITY_PATH = "activity"
        private const val KILNS_REALTIME_PATH = "kilns_realtime"
        private const val REALTIME_DATA_PATH = "realtimeData"
        private const val PROFILE_STATE_PATH = "profileState"
        private const val DEVICE_INFO_PATH = "deviceInfo"
        /** Evita que get() cuelgue minutos si el socket RTDB quedó zombie. */
        const val REALTIME_GET_TIMEOUT_MS = 3_000L
    }
    
    /**
     * Obtiene una referencia a un path específico
     */
    private fun getReference(path: String): DatabaseReference {
        return reference.child(path)
    }
    
    /**
     * Obtiene una referencia del usuario actual
     */
    fun getUserReference(userId: String): DatabaseReference {
        return getReference("$USERS_PATH/$userId")
    }
    
    // ==================== PROGRAMAS ====================
    
    /**
     * Guarda un programa en Firebase
     */
    suspend fun saveProgram(userId: String, programId: String, programData: Map<String, Any?>): Result<Unit> {
        return try {
            getUserReference(userId)
                .child(PROGRAMS_PATH)
                .child(programId)
                .setValue(programData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene todos los programas del usuario
     */
    suspend fun getPrograms(userId: String): Result<List<Map<String, Any?>>> {
        return try {
            val snapshot = getUserReference(userId)
                .child(PROGRAMS_PATH)
                .get()
                .await()
            
            val programs = mutableListOf<Map<String, Any?>>()
            snapshot.children.forEach { child ->
                @Suppress("UNCHECKED_CAST")
                val program = child.value as? Map<String, Any?>
                program?.let { programs.add(it) }
            }
            Result.success(programs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene un programa específico
     */
    suspend fun getProgram(userId: String, programId: String): Result<Map<String, Any?>?> {
        return try {
            val snapshot = getUserReference(userId)
                .child(PROGRAMS_PATH)
                .child(programId)
                .get()
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val program = snapshot.value as? Map<String, Any?>
            Result.success(program)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Elimina un programa
     */
    suspend fun deleteProgram(userId: String, programId: String): Result<Unit> {
        return try {
            getUserReference(userId)
                .child(PROGRAMS_PATH)
                .child(programId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== HORNOS ====================
    
    /**
     * Guarda un horno en Firebase
     */
    suspend fun saveKiln(userId: String, kilnId: String, kilnData: Map<String, Any?>): Result<Unit> {
        return try {
            getUserReference(userId)
                .child(KILNS_PATH)
                .child(kilnId)
                .setValue(kilnData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene todos los hornos del usuario
     */
    suspend fun getKilns(userId: String): Result<List<Map<String, Any?>>> {
        return try {
            val snapshot = getUserReference(userId)
                .child(KILNS_PATH)
                .get()
                .await()
            
            val kilns = mutableListOf<Map<String, Any?>>()
            snapshot.children.forEach { child ->
                @Suppress("UNCHECKED_CAST")
                val kiln = child.value as? Map<String, Any?>
                kiln?.let { kilns.add(it) }
            }
            Result.success(kilns)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Elimina un horno
     */
    suspend fun deleteKiln(userId: String, kilnId: String): Result<Unit> {
        return try {
            getUserReference(userId)
                .child(KILNS_PATH)
                .child(kilnId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== ACTIVIDAD ====================
    
    /**
     * Guarda un registro de actividad (cocción)
     */
    suspend fun saveActivity(userId: String, activityId: String, activityData: Map<String, Any?>): Result<Unit> {
        return try {
            getUserReference(userId)
                .child(ACTIVITY_PATH)
                .child(activityId)
                .setValue(activityData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene todo el historial de actividad
     */
    suspend fun getActivities(userId: String): Result<List<Map<String, Any?>>> {
        return try {
            val snapshot = getUserReference(userId)
                .child(ACTIVITY_PATH)
                .get()
                .await()
            
            val activities = mutableListOf<Map<String, Any?>>()
            snapshot.children.forEach { child ->
                @Suppress("UNCHECKED_CAST")
                val activity = child.value as? Map<String, Any?>
                activity?.let { activities.add(it) }
            }
            Result.success(activities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Elimina un registro de actividad
     */
    suspend fun deleteActivity(userId: String, activityId: String): Result<Unit> {
        return try {
            getUserReference(userId)
                .child(ACTIVITY_PATH)
                .child(activityId)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== DATOS EN TIEMPO REAL ====================

    /**
     * Emite true/false según el estado del WebSocket de RTDB (.info/connected).
     * Útil para detectar conexiones zombie sin esperar al poll de datos.
     */
    fun observeConnectionState(): Flow<Boolean> = callbackFlow {
        val connectedRef = database.getReference(".info/connected")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) == true
                android.util.Log.d(TAG, "RTDB .info/connected = $connected")
                trySend(connected)
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e(TAG, "RTDB connection listener cancelled: ${error.message}")
                close(error.toException())
            }
        }
        connectedRef.addValueEventListener(listener)
        awaitClose {
            connectedRef.removeEventListener(listener)
        }
    }

    /**
     * Fuerza al SDK a cerrar y reabrir el socket RTDB.
     * Llamar tras detectar .info/connected=false o datos obsoletos prolongados.
     */
    fun reconnectFirebase() {
        android.util.Log.w(TAG, "RTDB reconnect: goOffline → goOnline")
        database.goOffline()
        database.goOnline()
    }
    
    /**
     * Observa los datos en tiempo real de un horno (temperatura, estado, timestamp)
     * Path: /users/{userId}/kilns/{macAddress}/realtimeData
     * 
     * @param userId ID del usuario
     * @param macAddress Dirección MAC del horno
     * @return Flow que emite mapas con los datos en tiempo real
     */
    fun observeRealtimeData(userId: String, macAddress: String): Flow<Map<String, Any?>?> = callbackFlow {
        val path = "$USERS_PATH/$userId/$KILNS_PATH/$macAddress/$REALTIME_DATA_PATH"
        android.util.Log.d("FirebaseRealtimeManager", "🔍 observeRealtimeData iniciado")
        android.util.Log.d("FirebaseRealtimeManager", "📍 Ruta Firebase: $path")
        android.util.Log.d("FirebaseRealtimeManager", "👤 userId: $userId")
        android.util.Log.d("FirebaseRealtimeManager", "📱 macAddress: $macAddress")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                android.util.Log.d("FirebaseRealtimeManager", "📦 onDataChange llamado")
                android.util.Log.d("FirebaseRealtimeManager", "🔍 snapshot.exists(): ${snapshot.exists()}")
                android.util.Log.d("FirebaseRealtimeManager", "🔍 snapshot.value: ${snapshot.value}")
                
                @Suppress("UNCHECKED_CAST")
                val data = snapshot.value as? Map<String, Any?>
                android.util.Log.d("FirebaseRealtimeManager", "📊 data extraído: $data")
                android.util.Log.d("FirebaseRealtimeManager", "📊 data tipo: ${data?.javaClass?.simpleName}")
                
                trySend(data)
                android.util.Log.d("FirebaseRealtimeManager", "✅ Datos enviados al Flow")
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("FirebaseRealtimeManager", "❌ onCancelled: ${error.message}")
                android.util.Log.e("FirebaseRealtimeManager", "❌ Error code: ${error.code}")
                close(error.toException())
            }
        }
        
        val ref = getUserReference(userId)
            .child(KILNS_PATH)
            .child(macAddress)
            .child(REALTIME_DATA_PATH)
        
        android.util.Log.d("FirebaseRealtimeManager", "🔗 Registrando listener en referencia: ${ref.path}")
        ref.addValueEventListener(listener)
        android.util.Log.d("FirebaseRealtimeManager", "✅ Listener registrado exitosamente")
        
        awaitClose {
            android.util.Log.d("FirebaseRealtimeManager", "🔄 Removiendo listener")
            ref.removeEventListener(listener)
        }
    }

    /**
     * Perfil cargado en el ESP (persiste en IDLE; distinto de realtimeData/programName).
     * Path: /users/{userId}/kilns/{macAddress}/profileState
     */
    fun observeProfileState(userId: String, macAddress: String): Flow<Map<String, Any?>?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                @Suppress("UNCHECKED_CAST")
                val data = snapshot.value as? Map<String, Any?>
                trySend(data)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        val ref = getUserReference(userId)
            .child(KILNS_PATH)
            .child(macAddress)
            .child(PROFILE_STATE_PATH)

        ref.addValueEventListener(listener)
        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    suspend fun getProfileState(userId: String, macAddress: String): Result<Map<String, Any?>?> {
        return try {
            val snapshot = withTimeout(REALTIME_GET_TIMEOUT_MS) {
                getUserReference(userId)
                    .child(KILNS_PATH)
                    .child(macAddress)
                    .child(PROFILE_STATE_PATH)
                    .get()
                    .await()
            }
            @Suppress("UNCHECKED_CAST")
            val data = snapshot.value as? Map<String, Any?>
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene los datos en tiempo real de un horno (una sola vez)
     * Path: /users/{userId}/kilns/{macAddress}/realtimeData
     * 
     * @param userId ID del usuario
     * @param macAddress Dirección MAC del horno
     * @return Result con los datos en tiempo real
     */
    suspend fun getRealtimeData(userId: String, macAddress: String): Result<Map<String, Any?>?> {
        return try {
            val snapshot = withTimeout(REALTIME_GET_TIMEOUT_MS) {
                getUserReference(userId)
                    .child(KILNS_PATH)
                    .child(macAddress)
                    .child(REALTIME_DATA_PATH)
                    .get()
                    .await()
            }
            
            @Suppress("UNCHECKED_CAST")
            val data = snapshot.value as? Map<String, Any?>
            Result.success(data)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "getRealtimeData timeout/error: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene la información del dispositivo (deviceInfo)
     * Path: /users/{userId}/kilns/{macAddress}/deviceInfo
     * 
     * @param userId ID del usuario
     * @param macAddress Dirección MAC del horno
     * @return Result con la información del dispositivo
     */
    suspend fun getDeviceInfo(userId: String, macAddress: String): Result<Map<String, Any?>?> {
        return try {
            val snapshot = getUserReference(userId)
                .child(KILNS_PATH)
                .child(macAddress)
                .child(DEVICE_INFO_PATH)
                .get()
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val data = snapshot.value as? Map<String, Any?>
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Escucha cambios en tiempo real de la temperatura de un horno
     * @return Flow que emite mapas con temperatura y timestamp
     */
    @Deprecated("Use observeRealtimeData instead", ReplaceWith("observeRealtimeData(userId, kilnId)"))
    fun observeKilnRealtime(kilnId: String): Flow<Map<String, Any?>?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                @Suppress("UNCHECKED_CAST")
                val data = snapshot.value as? Map<String, Any?>
                trySend(data)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        val ref = getReference("$KILNS_REALTIME_PATH/$kilnId")
        ref.addValueEventListener(listener)
        
        awaitClose {
            ref.removeEventListener(listener)
        }
    }
    
    /**
     * Actualiza datos en tiempo real del horno (temperatura, timestamp)
     */
    @Deprecated("Use the ESP32 to update realtimeData", ReplaceWith(""))
    suspend fun updateKilnRealtime(kilnId: String, temperature: Double, timestamp: Long): Result<Unit> {
        return try {
            val data = mapOf(
                "temperature" to temperature,
                "timestamp" to timestamp
            )
            getReference("$KILNS_REALTIME_PATH/$kilnId")
                .setValue(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== UTILIDADES ====================
    
    /**
     * Guarda datos genéricos en un path específico
     */
    suspend fun saveData(path: String, data: Map<String, Any?>): Result<Unit> {
        return try {
            getReference(path)
                .setValue(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Obtiene datos genéricos de un path específico
     */
    suspend fun getData(path: String): Result<Map<String, Any?>?> {
        return try {
            val snapshot = getReference(path)
                .get()
                .await()
            
            @Suppress("UNCHECKED_CAST")
            val data = snapshot.value as? Map<String, Any?>
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Elimina datos de un path específico
     */
    suspend fun deleteData(path: String): Result<Unit> {
        return try {
            getReference(path)
                .removeValue()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


