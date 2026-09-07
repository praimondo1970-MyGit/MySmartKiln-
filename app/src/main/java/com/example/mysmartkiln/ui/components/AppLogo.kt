package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors

/**
 * Logo component for MySmartKiln app
 */

/**
 * Main app logo with text and icon
 */
@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: LogoSize = LogoSize.Medium,
    showText: Boolean = true,
    showIcon: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon) {
            LogoIcon(size = size)
        }
        
        if (showText) {
            LogoText(size = size)
        }
    }
}

/**
 * Logo icon component
 */
@Composable
fun LogoIcon(
    modifier: Modifier = Modifier,
    size: LogoSize = LogoSize.Medium
) {
    val iconSize = when (size) {
        LogoSize.Small -> 32.dp
        LogoSize.Medium -> 48.dp
        LogoSize.Large -> 64.dp
        LogoSize.XL -> 80.dp
    }
    
    Surface(
        modifier = modifier.size(iconSize),
        shape = CircleShape,
        color = LocalKilnColors.current.accent,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "K",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = when (size) {
                        LogoSize.Small -> 18.sp
                        LogoSize.Medium -> 24.sp
                        LogoSize.Large -> 32.sp
                        LogoSize.XL -> 40.sp
                    }
                ),
                color = LocalKilnColors.current.onAccent
            )
        }
    }
}

/**
 * Logo text component
 */
@Composable
fun LogoText(
    modifier: Modifier = Modifier,
    size: LogoSize = LogoSize.Medium
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MySmartKiln",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = when (size) {
                    LogoSize.Small -> 16.sp
                    LogoSize.Medium -> 20.sp
                    LogoSize.Large -> 24.sp
                    LogoSize.XL -> 28.sp
                }
            ),
            color = LocalKilnColors.current.accent
        )
        
        Text(
            text = "Control de Horno IA",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = when (size) {
                    LogoSize.Small -> 10.sp
                    LogoSize.Medium -> 12.sp
                    LogoSize.Large -> 14.sp
                    LogoSize.XL -> 16.sp
                }
            ),
            color = LocalKilnColors.current.muted
        )
    }
}

/**
 * Compact logo for headers and small spaces
 */
@Composable
fun CompactLogo(
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(24.dp),
            shape = CircleShape,
            color = LocalKilnColors.current.accent
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "K",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = LocalKilnColors.current.onAccent
                )
            }
        }
        
        if (showText) {
            Text(
                text = "MySmartKiln",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = LocalKilnColors.current.accent
            )
        }
    }
}

/**
 * Logo icon component using drawable resource
 */
@Composable
fun Logoicon(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logoicon),
        contentDescription = "logo-icon",
        modifier = modifier
            .requiredWidth(width = 89.dp)
            .requiredHeight(height = 126.dp)
    )
}

@Preview(widthDp = 89, heightDp = 126)
@Composable
private fun LogoiconPreview() {
    Logoicon(Modifier)
}

/**
 * Logo sizes available
 */
enum class LogoSize {
    Small,
    Medium,
    Large,
    XL
}
