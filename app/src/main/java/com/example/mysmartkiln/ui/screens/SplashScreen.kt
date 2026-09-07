package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.Logoicon
import com.example.mysmartkiln.ui.components.TypePrimarySizeDefaultStateActiveIconNewValue
import com.example.mysmartkiln.ui.components.TypeTerciarySizeDefaultStateActiveIconNewValue
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme
import com.example.mysmartkiln.ui.theme.RalewayFontFamily
import kotlinx.coroutines.delay

/**
 * New Splash Screen for MySmartKiln app based on the provided design code
 */

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(false) }

    // Simulate loading time
    LaunchedEffect(Unit) {
        delay(2000) // 2 seconds
        showContent = true
    }

    if (!showContent) {
        // Loading state
        LoadingContent()
    } else {
        // Main content
        GetStarted(
            onGetStarted = onNavigateToMain,
            onSignIn = onNavigateToLogin
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .requiredWidth(width = 390.dp)
            .requiredHeight(height = 844.dp)
            .background(color = LocalKilnColors.current.brandSurface)
    ) {

        // Logo
        Logoicon(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 154.dp,
                    end = 147.dp,
                    top = 213.dp,
                    bottom = 505.2037887573242.dp
                )
        )

        // Loading indicator
        CircularProgressIndicator(
            color = LocalKilnColors.current.onAccent,
            strokeWidth = 3.dp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 50.dp)
        )

        // Loading text
        Text(
            text = "Cargando...",
            style = MaterialTheme.typography.bodyLarge,
            color = LocalKilnColors.current.onAccent.copy(alpha = 0.8f),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 80.dp)
        )

        // Home indicator
        HomeIndicatorWhite(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-34).dp)
        )
    }
}

@Composable
fun GetStarted(
    onGetStarted: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .requiredWidth(width = 390.dp)
            .requiredHeight(height = 844.dp)
            .background(color = LocalKilnColors.current.brandSurface)
    ) {

        // Logo
        Logoicon(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 154.dp,
                    end = 147.dp,
                    top = 213.dp,
                    bottom = 505.2037887573242.dp
                )
        )

        // App name
        Text(
            text = "SmartKiln",
            color = LocalKilnColors.current.onAccent,
            style = TextStyle(
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = RalewayFontFamily
            ),
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 99.dp, y = 365.dp)
        )

        // Description
        Text(
            text = "Conecta tu horno a la nube y descubre una nueva forma de controlar, analizar y perfeccionar cada proceso de cocción, desde cualquier lugar.",
            color = LocalKilnColors.current.onAccent,
            textAlign = TextAlign.Center,
            lineHeight = 1.39.em,
            style = TextStyle(
                fontSize = 18.sp,
                fontFamily = RalewayFontFamily
            ),
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 56.dp, y = 452.dp)
                .requiredWidth(width = 285.dp)
        )

        // Get Started Button
        TypePrimarySizeDefaultStateActiveIconNewValue(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 28.dp, y = 636.dp),
            onClick = onGetStarted
        )

        // Sign In Button
        TypeTerciarySizeDefaultStateActiveIconNewValue(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 27.dp, y = 705.dp),
            onClick = onSignIn
        )

        // Home indicator
        HomeIndicatorWhite(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 0.dp, y = 810.dp)
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun GetStartedPreview() {
    MySmartKilnTheme {
        GetStarted(
            onGetStarted = { },
            onSignIn = { }
        )
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun SplashScreenLoadingPreview() {
    MySmartKilnTheme {
        LoadingContent()
    }
}