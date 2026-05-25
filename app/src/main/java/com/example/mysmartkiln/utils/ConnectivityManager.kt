package com.example.mysmartkiln.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manager para validar conectividad a internet y servicios específicos
 */
class ConnectivityManager(private val context: Context) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _isConnectedToInternet = MutableStateFlow(false)
    val isConnectedToInternet: StateFlow<Boolean> = _isConnectedToInternet.asStateFlow()
    
    private val _isFirebaseReachable = MutableStateFlow(false)
    val isFirebaseReachable: StateFlow<Boolean> = _isFirebaseReachable.asStateFlow()
    
    private var connectivityJob: Job? = null
    private val connectivityCheckInterval = 5000L // 5 segundos
    
    /**
     * Inicia el monitoreo de conectividad
     */
    fun startConnectivityMonitoring() {
        Log.d("ConnectivityManager", "🚀 Iniciando monitoreo de conectividad...")
        
        connectivityJob?.cancel()
        connectivityJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // Verificar conectividad básica
                    val hasInternet = checkInternetConnectivity()
                    _isConnectedToInternet.value = hasInternet
                    
                    // Si hay internet, verificar Firebase
                    if (hasInternet) {
                        val firebaseReachable = checkFirebaseConnectivity()
                        _isFirebaseReachable.value = firebaseReachable
                        
                        Log.d("ConnectivityManager", "🌐 Internet: $hasInternet, Firebase: $firebaseReachable")
                    } else {
                        _isFirebaseReachable.value = false
                        Log.d("ConnectivityManager", "🌐 Sin internet")
                    }
                    
                } catch (e: Exception) {
                    Log.e("ConnectivityManager", "❌ Error verificando conectividad: ${e.message}")
                    _isConnectedToInternet.value = false
                    _isFirebaseReachable.value = false
                }
                
                delay(connectivityCheckInterval)
            }
        }
    }
    
    /**
     * Detiene el monitoreo de conectividad
     */
    fun stopConnectivityMonitoring() {
        Log.d("ConnectivityManager", "🛑 Deteniendo monitoreo de conectividad...")
        connectivityJob?.cancel()
    }
    
    /**
     * Verifica conectividad básica a internet
     */
    private fun checkInternetConnectivity(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo?.isConnectedOrConnecting == true
            }
        } catch (e: Exception) {
            Log.e("ConnectivityManager", "❌ Error verificando conectividad básica: ${e.message}")
            false
        }
    }
    
    /**
     * Verifica conectividad específica a Firebase
     */
    private suspend fun checkFirebaseConnectivity(): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                // Intentar conectar a Firebase Realtime Database específicamente
                val url = URL("https://mysmartkiln-default-rtdb.firebaseio.com/.json")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 3000 // 3 segundos timeout
                connection.readTimeout = 3000
                
                val responseCode = connection.responseCode
                connection.disconnect()
                
                // Códigos 200-299 y 400-499 indican que Firebase está disponible
                // (400-499 pueden ser errores de autenticación, pero el servicio está activo)
                responseCode in 200..499
            }
        } catch (e: Exception) {
            Log.e("ConnectivityManager", "❌ Error verificando Firebase: ${e.message}")
            false
        }
    }
    
    /**
     * Verificación inmediata de conectividad (sin esperar al timer)
     */
    suspend fun checkConnectivityNow(): Pair<Boolean, Boolean> {
        val hasInternet = checkInternetConnectivity()
        val firebaseReachable = if (hasInternet) {
            checkFirebaseConnectivity()
        } else {
            false
        }
        
        Log.d("ConnectivityManager", "🔍 Verificación inmediata - Internet: $hasInternet, Firebase: $firebaseReachable")
        return Pair(hasInternet, firebaseReachable)
    }
}
