package com.example.mysmartkiln.data.firebase

import com.example.mysmartkiln.data.model.ActivityRecord
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
     * Guarda un horno en Firebase.
     * ¡Cuidado! [setValue] en la raíz del MAC borra realtimeData/recordings.
     * Preferir [updateDeviceInfo].
     */
    @Deprecated("No escribir la raíz kilns/{mac}; usa updateDeviceInfo")
    suspend fun saveKiln(userId: String, kilnId: String, kilnData: Map<String, Any?>): Result<Unit> {
        return try {
            getUserReference(userId)
                .child(KILNS_PATH)
                .child(kilnId)
                .child(DEVICE_INFO_PATH)
                .updateChildren(kilnData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lista metadata de dispositivos sin bajar recordings/samples.
     * Requiere las MACs conocidas (p. ej. desde Room): RTDB no permite "solo keys"
     * desde el SDK cliente.
     */
    suspend fun getDeviceInfosForMacs(
        userId: String,
        macAddresses: List<String>
    ): Result<List<Pair<String, Map<String, Any?>>>> {
        return try {
            val out = mutableListOf<Pair<String, Map<String, Any?>>>()
            for (mac in macAddresses) {
                if (mac.isBlank()) continue
                val info = getDeviceInfo(userId, mac).getOrNull() ?: continue
                out.add(mac to info)
            }
            Result.success(out)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * @deprecated Descarga TODO el árbol bajo kilns/ (incluye recordings).
     * Usar [getDeviceInfosForMacs] con MACs de Room.
     */
    @Deprecated(
        message = "Descarga recordings/samples enteros. Usar getDeviceInfosForMacs.",
        replaceWith = ReplaceWith("getDeviceInfosForMacs(userId, macAddresses)")
    )
    suspend fun getKilns(userId: String): Result<List<Map<String, Any?>>> {
        // No hacer get() en kilns/: devolver vacío a propósito.
        android.util.Log.w(
            "FirebaseRealtimeManager",
            "getKilns($userId) bloqueado — usá getDeviceInfosForMacs"
        )
        return Result.success(emptyList())
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
     * Upsert canónico en activity/{id}.
     * Campos: type, deviceMac, deviceName, name, startedAtMs, endedAtMs, …
     */
    suspend fun upsertActivity(userId: String, record: ActivityRecord): Result<Unit> {
        return try {
            getUserReference(userId)
                .child(ACTIVITY_PATH)
                .child(record.id)
                .updateChildren(record.toFirebaseMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** @deprecated Preferir [upsertActivity] con [ActivityRecord]. */
    suspend fun saveActivity(userId: String, activityId: String, activityData: Map<String, Any?>): Result<Unit> {
        return try {
            getUserReference(userId)
                .child(ACTIVITY_PATH)
                .child(activityId)
                .updateChildren(activityData)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Historial de actividad del usuario (más reciente primero).
     */
    suspend fun getActivities(userId: String): Result<List<ActivityRecord>> {
        return try {
            val snapshot = getUserReference(userId)
                .child(ACTIVITY_PATH)
                .get()
                .await()

            val activities = snapshot.children.mapNotNull { child ->
                @Suppress("UNCHECKED_CAST")
                val map = child.value as? Map<String, Any?> ?: return@mapNotNull null
                ActivityRecord.fromFirebase(map, fallbackId = child.key.orEmpty())
            }.sortedByDescending { it.endedAtMs.takeIf { t -> t > 0 } ?: it.startedAtMs }
            Result.success(activities)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== DATALOGGER RECORDINGS ====================

    private fun recordingRef(userId: String, mac: String, recordingId: String): DatabaseReference =
        getUserReference(userId)
            .child(KILNS_PATH)
            .child(mac)
            .child("recordings")
            .child(recordingId)

    suspend fun saveLoggerRecordingMeta(
        userId: String,
        mac: String,
        recordingId: String,
        meta: Map<String, Any?>
    ): Result<Unit> {
        return try {
            recordingRef(userId, mac, recordingId).child("meta").setValue(meta).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun appendLoggerRecordingSample(
        userId: String,
        mac: String,
        recordingId: String,
        sample: Map<String, Any?>
    ): Result<Unit> {
        return try {
            recordingRef(userId, mac, recordingId).child("samples").push().setValue(sample).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun finishLoggerRecording(
        userId: String,
        mac: String,
        recordingId: String,
        endedAtMs: Long,
        sampleCount: Int
    ): Result<Unit> {
        return try {
            recordingRef(userId, mac, recordingId).child("meta").updateChildren(
                mapOf(
                    "endedAtMs" to endedAtMs,
                    "sampleCount" to sampleCount,
                    "active" to false,
                    "ended" to true
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Borra meta + samples de una grabación DataLogger. */
    suspend fun deleteLoggerRecording(
        userId: String,
        mac: String,
        recordingId: String
    ): Result<Unit> {
        if (recordingId.isBlank() || mac.isBlank()) {
            return Result.success(Unit)
        }
        return try {
            val cleanMac = mac.replace(":", "").replace("-", "").uppercase()
            recordingRef(userId, cleanMac, recordingId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Guarda una cocción de horno (meta + samples + stages) bajo
     * `kilns/{mac}/recordings/{id}` (mismo árbol que DataLogger).
     */
    suspend fun saveKilnFiringRecording(
        userId: String,
        mac: String,
        recordingId: String,
        meta: Map<String, Any?>,
        samples: List<Map<String, Any?>>,
        stages: List<Map<String, Any?>>
    ): Result<Unit> {
        return try {
            val cleanMac = mac.replace(":", "").replace("-", "").uppercase()
            val samplesMap = samples.mapIndexed { i, s -> i.toString() to s }.toMap()
            val stagesMap = stages.mapIndexed { i, s -> i.toString() to s }.toMap()
            recordingRef(userId, cleanMac, recordingId).setValue(
                mapOf(
                    "meta" to meta,
                    "samples" to samplesMap,
                    "stages" to stagesMap
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lee meta + samples (+ stages si hay) de recordings/{id}.
     */
    data class RecordingPayload(
        val meta: Map<String, Any?>,
        val samples: List<Map<String, Any?>>,
        val stages: List<Map<String, Any?>>
    )

    suspend fun getRecordingPayload(
        userId: String,
        mac: String,
        recordingId: String
    ): Result<RecordingPayload> {
        return try {
            val cleanMac = mac.replace(":", "").replace("-", "").uppercase()
            val snap = recordingRef(userId, cleanMac, recordingId).get().await()
            @Suppress("UNCHECKED_CAST")
            val meta = (snap.child("meta").value as? Map<String, Any?>) ?: emptyMap()
            val samples = parseIndexedMaps(snap.child("samples"))
            val stages = parseIndexedMaps(snap.child("stages"))
            Result.success(RecordingPayload(meta, samples, stages))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseIndexedMaps(
        node: com.google.firebase.database.DataSnapshot
    ): List<Map<String, Any?>> {
        val indexed = mutableListOf<Pair<Int, Map<String, Any?>>>()
        node.children.forEach { child ->
            @Suppress("UNCHECKED_CAST")
            val map = child.value as? Map<String, Any?> ?: return@forEach
            val idx = child.key?.toIntOrNull() ?: indexed.size
            indexed.add(idx to map)
        }
        indexed.sortBy { it.first }
        return indexed.map { it.second }
    }

    /**
     * Lee meta + samples indexados de una grabación DataLogger.
     * @return Pair(meta, samples ordenados por índice)
     */
    suspend fun getLoggerRecording(
        userId: String,
        mac: String,
        recordingId: String
    ): Result<Pair<Map<String, Any?>, List<Map<String, Any?>>>> {
        return try {
            val snap = recordingRef(userId, mac, recordingId).get().await()
            @Suppress("UNCHECKED_CAST")
            val meta = (snap.child("meta").value as? Map<String, Any?>) ?: emptyMap()
            val samplesNode = snap.child("samples")
            val indexed = mutableListOf<Pair<Int, Map<String, Any?>>>()
            samplesNode.children.forEach { child ->
                @Suppress("UNCHECKED_CAST")
                val map = child.value as? Map<String, Any?> ?: return@forEach
                val idx = child.key?.toIntOrNull() ?: indexed.size
                indexed.add(idx to map)
            }
            indexed.sortBy { it.first }
            Result.success(meta to indexed.map { it.second })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Evalúa si la grabación en Firebase está cerrada y completa
     * (`ended`/`active=false` y sampleCount == cantidad de samples).
     */
    data class LoggerRecordingAssessment(
        val complete: Boolean,
        val ended: Boolean,
        val active: Boolean,
        val metaSampleCount: Int,
        val cloudSampleCount: Int,
        val samples: List<Map<String, Any?>>
    )

    suspend fun assessLoggerRecordingCompleteness(
        userId: String,
        mac: String,
        recordingId: String
    ): Result<LoggerRecordingAssessment> {
        return getLoggerRecording(userId, mac, recordingId).map { (meta, samples) ->
            val active = meta["active"] as? Boolean ?: false
            val ended = (meta["ended"] as? Boolean == true) || !active
            val metaCount = (meta["sampleCount"] as? Number)?.toInt()
                ?: samples.size
            val cloudCount = samples.size
            val complete = ended && !active && cloudCount > 0 && cloudCount >= metaCount
            LoggerRecordingAssessment(
                complete = complete,
                ended = ended,
                active = active,
                metaSampleCount = metaCount,
                cloudSampleCount = cloudCount,
                samples = samples
            )
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
        val mac = macAddress.replace(":", "").replace("-", "").uppercase()
        val path = "$USERS_PATH/$userId/$KILNS_PATH/$mac/$REALTIME_DATA_PATH"
        android.util.Log.d("FirebaseRealtimeManager", "🔍 observeRealtimeData iniciado")
        android.util.Log.d("FirebaseRealtimeManager", "📍 Ruta Firebase: $path")
        android.util.Log.d("FirebaseRealtimeManager", "👤 userId: $userId")
        android.util.Log.d("FirebaseRealtimeManager", "📱 macAddress: $mac")
        
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
            .child(mac)
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
            val mac = macAddress.replace(":", "").replace("-", "").uppercase()
            val snapshot = withTimeout(REALTIME_GET_TIMEOUT_MS) {
                getUserReference(userId)
                    .child(KILNS_PATH)
                    .child(mac)
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

    /** Actualiza campos sin borrar el resto del nodo. */
    suspend fun updateData(path: String, data: Map<String, Any?>): Result<Unit> {
        return try {
            getReference(path)
                .updateChildren(data)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upsert canónico de deviceInfo (nunca setValue en la raíz del MAC).
     * Campos: type, name, model, macAddress, volume?, thermocouple?
     */
    suspend fun upsertDeviceInfo(
        userId: String,
        macAddress: String,
        name: String,
        model: String,
        type: String,
        volume: String? = null,
        thermocouple: String? = null
    ): Result<Unit> {
        val data = mutableMapOf<String, Any?>(
            "name" to name,
            "model" to model,
            "macAddress" to macAddress,
            "type" to type.ifBlank { "KILN" }.uppercase()
        )
        if (!volume.isNullOrBlank()) {
            data["volume"] = volume
        }
        if (!thermocouple.isNullOrBlank()) {
            data["thermocouple"] = thermocouple.uppercase()
        }
        return updateData(
            path = "$USERS_PATH/$userId/$KILNS_PATH/$macAddress/$DEVICE_INFO_PATH",
            data = data
        )
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


