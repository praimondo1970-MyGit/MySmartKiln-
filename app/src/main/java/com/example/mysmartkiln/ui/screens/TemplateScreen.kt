package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.Plusbtn
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun TemplateScreen(
    onNavigateToKilns: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColors.color_Primary_50)
    ) {
        // Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomStart)
                .fillMaxWidth()
                .requiredHeight(height = 200.dp)
        ) {
            // Floating Action Button (FAB)
            Plusbtn(
                modifier = Modifier
                    .align(alignment = Alignment.TopCenter)
                    .offset(x = 1.dp, y = 57.dp)
            )
            
            // Navigation Bar Background - Full width with precise curved top
            Box(
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 0.dp, y = 60.dp)
                    .fillMaxWidth()
                    .requiredHeight(height = 141.dp)
                    .background(
                       color = Color(0xFF003D99),
                        shape = object : androidx.compose.ui.graphics.Shape {
                            override fun createOutline(
                                size: androidx.compose.ui.geometry.Size,
                                layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                                density: androidx.compose.ui.unit.Density
                            ): androidx.compose.ui.graphics.Outline {
                                val path = androidx.compose.ui.graphics.Path().apply {
                                    val width = size.width
                                    val height = size.height
                                    
                                    // Convert dp to pixels for precise positioning
                                    val densityValue = density.density
                                    
                                    // Start from left edge at y=34 (relative to top of shape)
                                    val startY = 34f * densityValue
                                    val curveStartY = startY
                                    
                                    // Move to start position (left edge, y=34)
                                    moveTo(0f, curveStartY)
                                    
                                    // Left side straight line (from 0,34 to 147,34)
                                    lineTo(width * (147f/390f), curveStartY)
                                    
                                    // First curve: C158.046,34 166.586,43.8745 173.422,52.5508
                                    cubicTo(
                                        width * (158.046f/390f), curveStartY,
                                        width * (166.586f/390f), curveStartY + (9.8745f * densityValue),
                                        width * (173.422f/390f), curveStartY + (18.5508f * densityValue)
                                    )
                                    
                                    // Second curve: C178.915,59.5232 187.435,64 197,64
                                    cubicTo(
                                        width * (178.915f/390f), curveStartY + (25.5232f * densityValue),
                                        width * (187.435f/390f), curveStartY + (30f * densityValue),
                                        width * (197f/390f), curveStartY + (30f * densityValue)
                                    )
                                    
                                    // Third curve: C206.565,64 215.085,59.5232 220.578,52.5508
                                    cubicTo(
                                        width * (206.565f/390f), curveStartY + (30f * densityValue),
                                        width * (215.085f/390f), curveStartY + (25.5232f * densityValue),
                                        width * (220.578f/390f), curveStartY + (18.5508f * densityValue)
                                    )
                                    
                                    // Fourth curve: C227.414,43.8745 235.954,34 247,34
                                    cubicTo(
                                        width * (227.414f/390f), curveStartY + (9.8745f * densityValue),
                                        width * (235.954f/390f), curveStartY,
                                        width * (247f/390f), curveStartY
                                    )
                                    
                                    // Right side straight line (from 247,34 to right edge, y=34)
                                    lineTo(width, curveStartY)
                                    
                                    // Continue to right edge at y=54
                                    lineTo(width, curveStartY + (20f * densityValue))
                                    
                                    // Bottom right corner
                                    lineTo(width, height)
                                    
                                    // Bottom edge
                                    lineTo(0f, height)
                                    
                                    // Left side back to start
                                    lineTo(0f, curveStartY + (20f * densityValue))
                                    
                                    // Close the path back to start
                                    close()
                                }
                                return androidx.compose.ui.graphics.Outline.Generic(path)
                            }
                        }
                    )
            )
            
            // Responsive Bottom Navigation Bar
            ResponsiveBottomNavigation(
                onKilnsClick = onNavigateToKilns,
                onProgramsClick = onNavigateToPrograms,
                onActivityClick = { /* Ya estamos en Activity */ },
                onSettingsClick = onNavigateToSettings,
                onPlusClick = { /* TODO: Implementar lógica del botón + */ },
                isKilnsActive = false,
                isProgramsActive = false,
                isActivityActive = true,
                isSettingsActive = false,
                showFab = false,
                modifier = Modifier.align(alignment = Alignment.BottomStart)
            )
        }
        
        // Home Indicator
        HomeIndicatorWhite(
            modifier = Modifier
                .align(alignment = Alignment.TopStart)
                .offset(x = 0.dp, y = 810.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TemplateScreenPreview() {
    MySmartKilnTheme {
        TemplateScreen()
    }
}