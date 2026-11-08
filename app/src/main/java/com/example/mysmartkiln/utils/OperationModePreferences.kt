package com.example.mysmartkiln.utils

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Modo de operación global de la app (Ajustes), no por horno.
 * LOCAL → priorizar SoftAP en todos los hornos.
 * ONLINE → Firebase cuando haya nube; SoftAP solo si hace falta.
 */
enum class AppOperationMode {
    ONLINE,
    LOCAL
}

object OperationModePreferences {
    private const val PREFS = "mysmartkiln_operation"
    private const val KEY_MODE = "operation_mode"

    fun read(context: Context): AppOperationMode {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, AppOperationMode.ONLINE.name)
        return runCatching { AppOperationMode.valueOf(raw ?: AppOperationMode.ONLINE.name) }
            .getOrDefault(AppOperationMode.ONLINE)
    }

    fun write(context: Context, mode: AppOperationMode) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }
}

class OperationModeController(context: Context) {
    private val appContext = context.applicationContext
    private val _mode = MutableStateFlow(OperationModePreferences.read(appContext))
    val mode: StateFlow<AppOperationMode> = _mode.asStateFlow()

    fun setMode(mode: AppOperationMode) {
        OperationModePreferences.write(appContext, mode)
        _mode.value = mode
    }

    fun isLocal(): Boolean = _mode.value == AppOperationMode.LOCAL
}

val LocalOperationModeController = staticCompositionLocalOf<OperationModeController> {
    error("LocalOperationModeController no provisto")
}
