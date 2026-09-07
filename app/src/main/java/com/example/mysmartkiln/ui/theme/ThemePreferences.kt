package com.example.mysmartkiln.ui.theme

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferencia de look: Clásico (azul claro) vs Horno/ESP (oscuro naranja).
 */
object ThemePreferences {
    private const val PREFS = "mysmartkiln_theme"
    private const val KEY_MODE = "theme_mode"

    fun read(context: Context): ThemeMode {
        val raw = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_MODE, ThemeMode.CLASSIC.name)
        return runCatching { ThemeMode.valueOf(raw ?: ThemeMode.CLASSIC.name) }
            .getOrDefault(ThemeMode.CLASSIC)
    }

    fun write(context: Context, mode: ThemeMode) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }
}

class ThemeController(context: Context) {
    private val appContext = context.applicationContext
    private val _mode = MutableStateFlow(ThemePreferences.read(appContext))
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    fun setMode(mode: ThemeMode) {
        ThemePreferences.write(appContext, mode)
        _mode.value = mode
    }
}

val LocalThemeController = staticCompositionLocalOf<ThemeController> {
    error("LocalThemeController no provisto")
}
