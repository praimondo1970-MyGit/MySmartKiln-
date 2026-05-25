package com.example.mysmartkiln.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager
import com.example.mysmartkiln.data.model.RealtimeData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para gestionar los datos en tiempo real del horno
 * Maneja la conexión a Firebase y el estado de conexión del dispositivo
 */
class RealtimeDataViewModel : ViewModel() {
    
    private val firebaseManager = FirebaseRealtimeManager()
    private val auth = FirebaseAuth.getInstance()
    
    private val _realtimeData = MutableStateFlow<RealtimeData?>(null)
    val realtimeData: StateFlow<RealtimeData?> = _realtimeData.asStateFlow()
    
    private val _isDeviceConnected = MutableStateFlow(false)
    val isDeviceConnected: StateFlow<Boolean> = _isDeviceConnected.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    /**
     * Inicia la observación de los datos en tiempo real de un horno
     * @param macAddress Dirección MAC del horno a observar
     */
    fun startObserving(macAddress: String) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                firebaseManager.observeRealtimeData(userId, macAddress)
                    .collect { data ->
                        if (data != null) {
                            val realtimeData = RealtimeData(
                                temperature = (data["temperature"] as? Number)?.toFloat() ?: 0f,
                                status = data["status"] as? String ?: "",
                                timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                            )
                            
                            _realtimeData.value = realtimeData
                            _isDeviceConnected.value = realtimeData.isConnected()
                            _isLoading.value = false
                        } else {
                            _realtimeData.value = null
                            _isDeviceConnected.value = false
                            _isLoading.value = false
                        }
                    }
            } catch (e: Exception) {
                _errorMessage.value = "Error al conectar con Firebase: ${e.message}"
                _isLoading.value = false
                _isDeviceConnected.value = false
            }
        }
    }
    
    /**
     * Obtiene los datos en tiempo real una sola vez (sin observación continua)
     * @param macAddress Dirección MAC del horno
     */
    fun fetchRealtimeData(macAddress: String) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            val result = firebaseManager.getRealtimeData(userId, macAddress)
            
            result.onSuccess { data ->
                if (data != null) {
                    val realtimeData = RealtimeData(
                        temperature = (data["temperature"] as? Number)?.toFloat() ?: 0f,
                        status = data["status"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                    )
                    
                    _realtimeData.value = realtimeData
                    _isDeviceConnected.value = realtimeData.isConnected()
                } else {
                    _realtimeData.value = null
                    _isDeviceConnected.value = false
                }
                _isLoading.value = false
            }.onFailure { error ->
                _errorMessage.value = "Error al obtener datos: ${error.message}"
                _isLoading.value = false
                _isDeviceConnected.value = false
            }
        }
    }
    
    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Resetea el estado del ViewModel
     */
    fun reset() {
        _realtimeData.value = null
        _isDeviceConnected.value = false
        _isLoading.value = false
        _errorMessage.value = null
    }
}













































