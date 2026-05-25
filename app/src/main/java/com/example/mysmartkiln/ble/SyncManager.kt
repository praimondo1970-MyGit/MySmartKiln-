package com.example.mysmartkiln.ble

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Manager para sincronización diferida de datos
 * Maneja la cola de datos pendientes cuando se está en modo BLE
 */
class SyncManager {
    
    private val pendingSyncQueue = ConcurrentLinkedQueue<PendingSyncData>()
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()
    
    private var syncJob: Job? = null
    
    /**
     * Agrega datos a la cola de sincronización
     */
    fun queueForSync(priority: SyncPriority, data: Any) {
        val pendingData = PendingSyncData(priority, data)
        pendingSyncQueue.offer(pendingData)
        _pendingCount.value = pendingSyncQueue.size
        
        Log.d("SyncManager", "📝 Datos agregados a cola de sincronización: $priority (Total: ${pendingSyncQueue.size})")
    }
    
    /**
     * Inicia la sincronización de datos pendientes
     */
    fun startSync() {
        if (_isSyncing.value) {
            Log.d("SyncManager", "🔄 Sincronización ya en progreso...")
            return
        }
        
        if (pendingSyncQueue.isEmpty()) {
            Log.d("SyncManager", "✅ No hay datos pendientes para sincronizar")
            return
        }
        
        Log.d("SyncManager", "🚀 Iniciando sincronización de ${pendingSyncQueue.size} elementos...")
        
        _isSyncing.value = true
        
        syncJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ordenar por prioridad
                val sortedData = pendingSyncQueue.toList().sortedBy { it.priority.ordinal }
                
                // Procesar por prioridades
                for (priority in SyncPriority.values()) {
                    val dataForPriority = sortedData.filter { it.priority == priority }
                    
                    if (dataForPriority.isNotEmpty()) {
                        Log.d("SyncManager", "📊 Sincronizando ${dataForPriority.size} elementos de prioridad: $priority")
                        
                        // Simular sincronización (aquí iría la lógica real de Firebase)
                        for (data in dataForPriority) {
                            syncData(data)
                            pendingSyncQueue.remove(data)
                            _pendingCount.value = pendingSyncQueue.size
                            
                            // Pequeña pausa entre elementos
                            delay(100)
                        }
                    }
                }
                
                Log.d("SyncManager", "✅ Sincronización completada")
                
            } catch (e: Exception) {
                Log.e("SyncManager", "❌ Error en sincronización: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    /**
     * Simula la sincronización de un elemento de datos
     */
    private suspend fun syncData(data: PendingSyncData) {
        // Aquí iría la lógica real de sincronización con Firebase
        Log.d("SyncManager", "📤 Sincronizando: ${data.priority} - ${data.data}")
        
        // Simular tiempo de sincronización
        delay(200)
    }
    
    /**
     * Detiene la sincronización
     */
    fun stopSync() {
        Log.d("SyncManager", "🛑 Deteniendo sincronización...")
        
        syncJob?.cancel()
        syncJob = null
        _isSyncing.value = false
    }
    
    /**
     * Limpia la cola de sincronización
     */
    fun clearQueue() {
        Log.d("SyncManager", "🗑️ Limpiando cola de sincronización...")
        
        pendingSyncQueue.clear()
        _pendingCount.value = 0
    }
    
    /**
     * Obtiene el número de elementos pendientes
     */
    fun getPendingCount(): Int {
        return pendingSyncQueue.size
    }
    
    /**
     * Verifica si hay datos pendientes
     */
    fun hasPendingData(): Boolean {
        return pendingSyncQueue.isNotEmpty()
    }
}






































