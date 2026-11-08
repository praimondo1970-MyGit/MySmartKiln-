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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** Scheme Material para Claro (UI ESP luminosa). */
private val ClassicLightColorScheme = lightColorScheme(
    primary = EspBrand.Orange,
    onPrimary = EspBrand.White,
    primaryContainer = EspBrand.OrangeLight,
    onPrimaryContainer = EspBrand.Light.Ink,
    secondary = EspBrand.OrangeDim,
    onSecondary = EspBrand.White,
    secondaryContainer = EspBrand.Light.TrackGray,
    onSecondaryContainer = EspBrand.Light.Ink,
    tertiary = EspBrand.Green,
    onTertiary = EspBrand.Light.Ink,
    tertiaryContainer = EspBrand.Light.Divider,
    onTertiaryContainer = EspBrand.Light.Ink,
    background = EspBrand.Light.Screen,
    onBackground = EspBrand.Light.Ink,
    surface = EspBrand.Light.Card,
    onSurface = EspBrand.Light.Ink,
    surfaceVariant = EspBrand.Light.TrackGray,
    onSurfaceVariant = EspBrand.Light.BodyGray,
    error = EspBrand.Red,
    onError = EspBrand.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = EspBrand.Light.Ink,
    outline = EspBrand.Light.CardBorder,
    outlineVariant = EspBrand.Light.Divider,
    scrim = EspBrand.Black.copy(alpha = 0.45f)
)

/** Scheme Material alineado al display ESP (Oscuro). Sin cambios de diseño. */
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
    val useEspDark = themeMode == ThemeMode.ESP

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useEspDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useEspDark -> EspDarkColorScheme
        else -> ClassicLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = kilnColors.screenBackground.toArgb()
            window.navigationBarColor = kilnColors.navBar.toArgb()
            val insets = WindowCompat.getInsetsController(window, view)
            // Iconos oscuros sobre Claro; claros sobre Oscuro
            insets.isAppearanceLightStatusBars = !useEspDark
            insets.isAppearanceLightNavigationBars = !useEspDark
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
