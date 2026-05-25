package com.example.mysmartkiln.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.*
import com.example.mysmartkiln.data.local.database.AppDatabase
import com.example.mysmartkiln.data.local.entity.KilnEntity
import com.example.mysmartkiln.data.local.entity.PendingSyncEntity
import com.example.mysmartkiln.data.local.entity.RealtimeDataEntity
import com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager
import com.example.mysmartkiln.data.work.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class KilnRepository(
    private val context: Context,
    private val firebaseManager: FirebaseRealtimeManager
) {
    
    private val database = AppDatabase.getDatabase(context)
    private val kilnDao = database.kilnDao()
    private val realtimeDataDao = database.realtimeDataDao()
    private val pendingSyncDao = database.pendingSyncDao()
    private val workManager = WorkManager.getInstance(context)
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ========== KILN OPERATIONS ==========

    /**
     * Guarda en Room de inmediato. Firebase se sincroniza en segundo plano (no bloquea la UI).
     */
    suspend fun saveKiln(kiln: KilnEntity) {
        withContext(Dispatchers.IO) {
            kilnDao.insertKiln(kiln)
        }
        syncScope.launch {
            syncKilnToFirebaseInBackground(kiln)
        }
    }

    /** Tareas HTTP/Firebase que no deben bloquear la pantalla de guardado. */
    fun runInBackground(block: suspend () -> Unit) {
        syncScope.launch { block() }
    }

    private suspend fun syncKilnToFirebaseInBackground(kiln: KilnEntity) {
        if (!isNetworkAvailable()) {
            kilnDao.markAsPendingSync(kiln.macAddress)
            scheduleSyncWork()
            return
        }
        try {
            syncKilnToFirebase(kiln)
            kilnDao.markAsSynced(kiln.macAddress, System.currentTimeMillis())
        } catch (e: Exception) {
            kilnDao.markAsPendingSync(kiln.macAddress)
            scheduleSyncWork()
        }
    }
    
    fun getAllKilns(): Flow<List<KilnEntity>> {
        return kilnDao.getAllKilns()
    }
    
    fun getKilnsByUserId(userId: String): Flow<List<KilnEntity>> {
        return kilnDao.getKilnsByUserId(userId)
    }
    
    suspend fun getKilnByMacAddress(macAddress: String): KilnEntity? {
        return kilnDao.getKilnByMacAddress(macAddress)
    }
    
    suspend fun deleteKiln(macAddress: String) {
        // Eliminar de la base de datos local
        val kiln = kilnDao.getKilnByMacAddress(macAddress)
        if (kiln != null) {
            kilnDao.deleteKiln(kiln)
            
            // Intentar eliminar de Firebase si hay conexión
            if (isNetworkAvailable()) {
                try {
                    deleteKilnFromFirebase(kiln)
                } catch (e: Exception) {
                    // Si falla, podríamos agregar una entrada de sincronización pendiente
                    // Por ahora solo log
                }
            }
        }
    }
    
    // ========== REALTIME DATA OPERATIONS ==========
    
    suspend fun saveRealtimeData(data: RealtimeDataEntity) {
        // Guardar localmente
        realtimeDataDao.insertRealtimeData(data)
        
        // Intentar sincronizar inmediatamente si hay conexión
        if (isNetworkAvailable()) {
            try {
                syncRealtimeDataToFirebase(data)
                realtimeDataDao.markAsSynced(data.macAddress)
            } catch (e: Exception) {
                // Si falla, marcar como pendiente de sincronización
                realtimeDataDao.markAsPendingSync(data.macAddress)
                scheduleSyncWork()
            }
        } else {
            // Sin conexión, marcar como pendiente
            realtimeDataDao.markAsPendingSync(data.macAddress)
            scheduleSyncWork()
        }
    }
    
    fun getLatestRealtimeData(macAddress: String): Flow<RealtimeDataEntity?> {
        return realtimeDataDao.getLatestRealtimeDataFlow(macAddress)
    }
    
    suspend fun getLatestRealtimeDataSync(macAddress: String): RealtimeDataEntity? {
        return realtimeDataDao.getLatestRealtimeData(macAddress)
    }
    
    // ========== SYNC OPERATIONS ==========
    
    suspend fun syncPendingData() {
        if (!isNetworkAvailable()) return
        
        // Sincronizar hornos pendientes
        val pendingKilns = kilnDao.getPendingSyncKilns()
        for (kiln in pendingKilns) {
            try {
                syncKilnToFirebase(kiln)
                kilnDao.markAsSynced(kiln.macAddress, System.currentTimeMillis())
            } catch (e: Exception) {
                // Log error but continue with other items
                e.printStackTrace()
            }
        }
        
        // Sincronizar datos de tiempo real pendientes
        val pendingRealtimeData = realtimeDataDao.getPendingSyncRealtimeData()
        for (data in pendingRealtimeData) {
            try {
                syncRealtimeDataToFirebase(data)
                realtimeDataDao.markAsSynced(data.macAddress)
            } catch (e: Exception) {
                // Log error but continue with other items
                e.printStackTrace()
            }
        }
        
        // Limpiar datos antiguos (más de 7 días)
        val cutoffTimestamp = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        realtimeDataDao.deleteOldData(cutoffTimestamp)
    }
    
    private suspend fun syncKilnToFirebase(kiln: KilnEntity) {
        val deviceInfoPath = "users/${kiln.userId}/kilns/${kiln.macAddress}/deviceInfo"
        firebaseManager.saveData(
            path = deviceInfoPath,
            data = mapOf(
                "name" to kiln.name,
                "model" to kiln.manufacturer,
                "volume" to kiln.volume,
                "macAddress" to kiln.macAddress
            )
        )
    }
    
    private suspend fun syncRealtimeDataToFirebase(data: RealtimeDataEntity) {
        val realtimeDataPath = "users/test_user_123/kilns/${data.macAddress}/realtimeData"
        firebaseManager.saveData(
            path = realtimeDataPath,
            data = mapOf(
                "temperature" to data.temperature,
                "status" to data.status,
                "timestamp" to data.timestamp
            )
        )
    }
    
    private suspend fun deleteKilnFromFirebase(kiln: KilnEntity) {
        val kilnPath = "users/${kiln.userId}/kilns/${kiln.macAddress}"
        firebaseManager.deleteData(kilnPath)
    }
    
    // ========== WORK MANAGER ==========
    
    private fun scheduleSyncWork() {
        val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()
        
        workManager.enqueueUniqueWork(
            "sync_data",
            ExistingWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
    
    // ========== NETWORK UTILITIES ==========
    
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
