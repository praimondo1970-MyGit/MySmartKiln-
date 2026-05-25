package com.example.mysmartkiln.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.Primary300,
    onPrimary = AppColors.Neutral900,
    primaryContainer = AppColors.Primary700,
    onPrimaryContainer = AppColors.Primary100,
    
    secondary = AppColors.Secondary300,
    onSecondary = AppColors.Neutral900,
    secondaryContainer = AppColors.Secondary700,
    onSecondaryContainer = AppColors.Secondary100,
    
    tertiary = AppColors.Tertiary300,
    onTertiary = AppColors.Neutral900,
    tertiaryContainer = AppColors.Tertiary700,
    onTertiaryContainer = AppColors.Tertiary100,
    
    background = AppColors.Neutral900,
    onBackground = AppColors.Neutral100,
    
    surface = AppColors.Neutral800,
    onSurface = AppColors.Neutral100,
    surfaceVariant = AppColors.Neutral700,
    onSurfaceVariant = AppColors.Neutral200,
    
    error = AppColors.Error400,
    onError = AppColors.Neutral900,
    errorContainer = AppColors.Error800,
    onErrorContainer = AppColors.Error100,
    
    outline = AppColors.Neutral600,
    outlineVariant = AppColors.Neutral700,
    
    scrim = AppColors.Black
)

private val LightColorScheme = lightColorScheme(
    primary = MySmartKilnPrimary,
    onPrimary = MySmartKilnOnPrimary,
    primaryContainer = AppColors.Primary100,
    onPrimaryContainer = AppColors.Primary900,
    
    secondary = MySmartKilnSecondary,
    onSecondary = MySmartKilnOnSecondary,
    secondaryContainer = AppColors.Secondary100,
    onSecondaryContainer = AppColors.Secondary900,
    
    tertiary = MySmartKilnTertiary,
    onTertiary = MySmartKilnOnTertiary,
    tertiaryContainer = AppColors.Tertiary100,
    onTertiaryContainer = AppColors.Tertiary900,
    
    background = MySmartKilnBackground,
    onBackground = MySmartKilnOnBackground,
    
    surface = MySmartKilnSurface,
    onSurface = MySmartKilnOnSurface,
    surfaceVariant = MySmartKilnSurfaceVariant,
    onSurfaceVariant = MySmartKilnOnSurfaceVariant,
    
    error = MySmartKilnError,
    onError = AppColors.White,
    errorContainer = AppColors.Error100,
    onErrorContainer = AppColors.Error900,
    
    outline = AppColors.Neutral400,
    outlineVariant = AppColors.Neutral200,
    
    scrim = AppColors.Black
)

@Composable
fun MySmartKilnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to use custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            window.navigationBarColor = AppColors.Primary900.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}