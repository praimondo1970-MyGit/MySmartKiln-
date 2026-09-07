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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Scheme Material para modo Clásico (app actual). */
private val ClassicLightColorScheme = lightColorScheme(
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
    background = AppColors.Primary100,
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

/** Scheme Material alineado al display ESP. */
private val EspDarkColorScheme = darkColorScheme(
    primary = EspBrand.Orange,
    onPrimary = EspBrand.White,
    primaryContainer = EspBrand.OrangeDim,
    onPrimaryContainer = EspBrand.White,
    secondary = EspBrand.OrangeLight,
    onSecondary = EspBrand.Black,
    secondaryContainer = EspBrand.Card,
    onSecondaryContainer = EspBrand.LightGray,
    tertiary = EspBrand.Green,
    onTertiary = EspBrand.Black,
    tertiaryContainer = EspBrand.DarkGray,
    onTertiaryContainer = EspBrand.White,
    background = EspBrand.Black,
    onBackground = EspBrand.White,
    surface = EspBrand.Card,
    onSurface = EspBrand.White,
    surfaceVariant = EspBrand.DarkGray,
    onSurfaceVariant = EspBrand.LightGray,
    error = EspBrand.Red,
    onError = EspBrand.White,
    errorContainer = EspBrand.OrangeReddish,
    onErrorContainer = EspBrand.White,
    outline = EspBrand.CardBorder,
    outlineVariant = EspBrand.Divider,
    scrim = EspBrand.Black
)

@Composable
fun MySmartKilnTheme(
    themeMode: ThemeMode = ThemeMode.CLASSIC,
    // Compat: si alguien pasa darkTheme sin mode, se ignora a favor de themeMode
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val kilnColors = KilnColors.forMode(themeMode)
    val useEsp = themeMode == ThemeMode.ESP

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useEsp) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useEsp -> EspDarkColorScheme
        else -> ClassicLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = kilnColors.screenBackground.toArgb()
            window.navigationBarColor = kilnColors.navBar.toArgb()
            val insets = WindowCompat.getInsetsController(window, view)
            // Iconos claros sobre fondo oscuro ESP; oscuros sobre fondo claro clásico
            insets.isAppearanceLightStatusBars = !useEsp
            insets.isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(LocalKilnColors provides kilnColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
