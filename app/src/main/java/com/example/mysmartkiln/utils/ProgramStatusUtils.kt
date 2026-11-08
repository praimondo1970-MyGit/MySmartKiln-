package com.example.mysmartkiln.utils

import android.util.Log

/**
 * Utilidades para determinar el estado de ejecución de un programa
 */
object ProgramStatusUtils {
    
    private const val TAG = "ProgramStatusUtils"
    private const val DATA_TIMEOUT_MS = 5 * 60 * 1000L // 5 minutos
    
    /**
     * Enum para representar el estado de ejecución del programa
     */
    enum class ProgramRunningState {
        CONFIRMED_RUNNING,      // Confirmado por Firebase reciente
        ESTIMATED_RUNNING,      // Estimado basado en cálculos
        CONFIRMED_STOPPED,      // Confirmado que está detenido
        ESTIMATED_STOPPED,      // Estimado que debería estar detenido
        UNKNOWN                 // No se puede determinar
    }
    
    /**
     * Determina el estado del programa:
     * 1. Estado desde Firebase (reciente)
     * 2. Estimación basada en cálculos de tiempo
     */
    fun determineProgramState(
        firebaseData: Map<String, Any?>?,
        programTotalDurationMinutes: Int? = null,
        isDataLogger: Boolean = false
    ): ProgramRunningState {
        // DataLogger no usa timing de cocción del horno
        if (isDataLogger ||
            (firebaseData?.get("type") as? String)?.equals("DATALOGGER", true) == true
        ) {
            val status = (firebaseData?.get("status") as? String)?.uppercase()
            return when (status) {
                "GRABANDO" -> ProgramRunningState.CONFIRMED_RUNNING
                "IDLE", "FINALIZADO", "DETENIDO" -> ProgramRunningState.CONFIRMED_STOPPED
                else -> ProgramRunningState.UNKNOWN
            }
        }
        
        // 1. PRIORIDAD: Estado desde Firebase (reciente, <30 segundos)
        if (firebaseData != null) {
            val timestamp = firebaseData["timestamp"] as? Long ?: 0L
            val currentTime = System.currentTimeMillis()
            val timeSinceUpdate = if (timestamp > 0) (currentTime - timestamp) else Long.MAX_VALUE
            val isRecent = timeSinceUpdate < 30000  // 30 segundos
            
            if (isRecent) {
                val status = firebaseData["status"] as? String
                val programName = firebaseData["programName"] as? String
                
                when (status) {
                    "running", "CALENTANDO" -> {
                        if (programName != null && programName.isNotEmpty()) {
                            Log.d(TAG, "✅ Estado CONFIRMED_RUNNING desde Firebase (reciente)")
                            return ProgramRunningState.CONFIRMED_RUNNING
                        }
                    }
                    "paused", "PAUSADO" -> {
                        // Pausado = no corriendo activamente
                        Log.d(TAG, "⏸️ Estado CONFIRMED_STOPPED desde Firebase (pausado)")
                        return ProgramRunningState.CONFIRMED_STOPPED
                    }
                    "INTERRUMPIDO" -> {
                        Log.d(TAG, "⚠️ Estado CONFIRMED_STOPPED desde Firebase (interrumpido)")
                        return ProgramRunningState.CONFIRMED_STOPPED
                    }
                    "stopped", "DETENIDO", "finished", "FINALIZADO", "idle", "IDLE" -> {
                        Log.d(TAG, "🛑 Estado CONFIRMED_STOPPED desde Firebase ($status)")
                        return ProgramRunningState.CONFIRMED_STOPPED
                    }
                }
            }
        }
        
        // 2. Estimación basada en cálculos de tiempo
        if (firebaseData != null) {
            try {
                // Convertir programStartTime de diferentes tipos posibles desde Firebase
                val programStartTimeValue = firebaseData["programStartTime"]
                val programStartTime: Long? = when {
                    programStartTimeValue == null -> null
                    programStartTimeValue is Long -> programStartTimeValue
                    programStartTimeValue is Number -> programStartTimeValue.toLong()
                    else -> null
                }
                
                // Convertir programTotalDuration
                val programTotalDurationValue = firebaseData["programTotalDuration"]
                val programTotalDuration: Int? = when {
                    programTotalDurationValue == null -> programTotalDurationMinutes
                    programTotalDurationValue is Int -> programTotalDurationValue
                    programTotalDurationValue is Number -> programTotalDurationValue.toInt()
                    else -> programTotalDurationMinutes
                }
                
                val lastStatus = firebaseData["status"] as? String
                
                // Convertir programPauseTime
                val programPauseTimeValue = firebaseData["programPauseTime"]
                val programPauseTime: Long? = when {
                    programPauseTimeValue == null -> null
                    programPauseTimeValue is Long -> programPauseTimeValue
                    programPauseTimeValue is Number -> programPauseTimeValue.toLong()
                    else -> null
                }
                
                val timestamp = firebaseData["timestamp"] as? Long ?: 0L
                
                // Si los datos son muy antiguos, no confiar en la estimación
                val timeSinceUpdate = if (timestamp > 0) (System.currentTimeMillis() - timestamp) else Long.MAX_VALUE
                if (timeSinceUpdate > DATA_TIMEOUT_MS) {
                    Log.d(TAG, "⏱️ Datos muy antiguos (${timeSinceUpdate}ms), no estimar")
                    return ProgramRunningState.UNKNOWN
                }
                
                if (programStartTime != null && programStartTime > 0 && 
                    programTotalDuration != null && programTotalDuration > 0 && 
                    (lastStatus == "running" || lastStatus == "CALENTANDO")) {
                    
                    // programStartTime viene en segundos desde Firebase (Unix timestamp)
                    val currentTimeSeconds = System.currentTimeMillis() / 1000
                    var elapsedMinutes = (currentTimeSeconds - programStartTime) / 60
                    
                    // Si estaba pausado, ajustar el tiempo transcurrido
                    if (programPauseTime != null && programPauseTime > 0 && 
                        (lastStatus == "paused" || lastStatus == "PAUSADO")) {
                        // Tiempo transcurrido = tiempo hasta la pausa
                        elapsedMinutes = (programPauseTime - programStartTime) / 60
                        Log.d(TAG, "⏸️ Programa pausado, tiempo transcurrido: $elapsedMinutes minutos")
                        return ProgramRunningState.CONFIRMED_STOPPED
                    }
                    
                    Log.d(TAG, "⏱️ Estimación: $elapsedMinutes minutos / $programTotalDuration minutos")
                    
                    return if (elapsedMinutes < programTotalDuration) {
                        Log.d(TAG, "✅ Estado ESTIMATED_RUNNING (tiempo no expirado)")
                        ProgramRunningState.ESTIMATED_RUNNING
                    } else {
                        Log.d(TAG, "🛑 Estado ESTIMATED_STOPPED (tiempo expirado)")
                        ProgramRunningState.ESTIMATED_STOPPED
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en estimación de tiempo: ${e.message}", e)
                // Continuar sin estimación si hay error
            }
        }
        
        // 4. FALLBACK: No se puede determinar
        Log.d(TAG, "❓ Estado UNKNOWN (datos insuficientes)")
        return ProgramRunningState.UNKNOWN
    }
    
    /**
     * Calcula la duración total de un programa en minutos desde sus segmentos
     */
    fun calculateTotalDuration(
        stages: List<ProgramStageInfo>,
        initialTemperature: Float = 0f
    ): Int {
        var totalMinutes = 0
        var previousTemp = initialTemperature
        
        stages.forEach { stage ->
            val tempIncrement = stage.targetTemp - previousTemp
            
            // Calcular tiempo de rampa (en minutos)
            val rampMinutes = if (stage.rampRate > 0 && tempIncrement > 0) {
                (tempIncrement / stage.rampRate).toInt()
            } else {
                0
            }
            
            // Sumar tiempo de mantenimiento
            totalMinutes += rampMinutes + stage.maintenanceMinutes
            
            // Actualizar temperatura anterior para el próximo segmento
            previousTemp = stage.targetTemp
        }
        
        return totalMinutes
    }
    
    /**
     * Datos de un segmento del programa para cálculo de duración
     */
    data class ProgramStageInfo(
        val targetTemp: Float,
        val rampRate: Float,
        val maintenanceMinutes: Int
    )
}

