package com.example.mysmartkiln.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors

/**
 * Componente de navegación inferior responsive que se adapta automáticamente
 * al ancho de la pantalla, distribuyendo los iconos a ambos lados del botón +
 */
@Composable
fun ResponsiveBottomNavigation(
    onKilnsClick: () -> Unit = {},
    onProgramsClick: () -> Unit = {},
    onActivityClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onPlusClick: () -> Unit = {},
    isKilnsActive: Boolean = true,
    isProgramsActive: Boolean = false,
    isActivityActive: Boolean = false,
    isSettingsActive: Boolean = false,
    showFab: Boolean = true,
    isMenuOpen: Boolean = false, // Nuevo parámetro para controlar si el menú está abierto
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp) // Reducido de 140dp a 120dp
    ) {
        // Background shape (curved bottom navigation)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp) // Reducido de 140dp a 120dp
                .clip(
                    GenericShape { size, _ ->
                        val width = size.width
                        val height = size.height
                        
                        // Start from left edge at y=34 (relative to top of shape)
                        val startY = 34f
                        val curveStartY = startY
                        
                        // Move to start position (left edge, y=34)
                        moveTo(0f, curveStartY)
                        
                        // Left side straight line (from 0,34 to 147,34)
                        lineTo(width * (147f/390f), curveStartY)
                        
                        // First curve: C158.046,34 166.586,43.8745 173.422,52.5508
                        cubicTo(
                            width * (158.046f/390f), curveStartY,
                            width * (166.586f/390f), curveStartY + 9.8745f,
                            width * (173.422f/390f), curveStartY + 18.5508f
                        )
                        
                        // Second curve: C178.915,59.5232 187.435,64 197,64
                        cubicTo(
                            width * (178.915f/390f), curveStartY + 25.5232f,
                            width * (187.435f/390f), curveStartY + 30f,
                            width * (197f/390f), curveStartY + 30f
                        )
                        
                        // Third curve: C206.565,64 215.085,59.5232 220.578,52.5508
                        cubicTo(
                            width * (206.565f/390f), curveStartY + 30f,
                            width * (215.085f/390f), curveStartY + 25.5232f,
                            width * (220.578f/390f), curveStartY + 18.5508f
                        )
                        
                        // Fourth curve: C227.414,43.8745 235.954,34 247,34
                        cubicTo(
                            width * (227.414f/390f), curveStartY + 9.8745f,
                            width * (235.954f/390f), curveStartY,
                            width * (247f/390f), curveStartY
                        )
                        
                        // Right side straight line (from 247,34 to right edge, y=34)
                        lineTo(width, curveStartY)
                        
                        // Continue to right edge at y=54
                        lineTo(width, curveStartY + 20f)
                        
                        // Bottom right corner
                        lineTo(width, height)
                        
                        // Bottom edge
                        lineTo(0f, height)
                        
                        // Left side back to start
                        lineTo(0f, curveStartY + 20f)
                        
                        // Close the path back to start
                        close()
                    }
                )
                .background(color = Color.Transparent) // Transparente
        )
        
        val kilnColors = LocalKilnColors.current

        if (kilnColors.isEsp) {
            // ESP: una sola fila; el + a la misma altura visual que los tabs (como mockup).
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavigationTab(
                    icon = R.drawable.nav_hornos,
                    text = "Hornos",
                    isActive = isKilnsActive,
                    onClick = onKilnsClick,
                    enabled = !isMenuOpen,
                    modifier = Modifier.weight(1f)
                )
                NavigationTab(
                    icon = R.drawable.nav_programas,
                    text = "Programas",
                    isActive = isProgramsActive,
                    onClick = onProgramsClick,
                    enabled = !isMenuOpen,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (showFab) {
                        if (isMenuOpen) {
                            CloseBtnCircle(
                                onClick = onPlusClick,
                                modifier = Modifier.offset(y = (-5).dp)
                            )
                        } else {
                            Plusbtn(
                                onClick = onPlusClick,
                                modifier = Modifier.offset(y = (-5).dp)
                            )
                        }
                    }
                }
                NavigationTab(
                    icon = R.drawable.nav_actividad,
                    text = "Actividad",
                    isActive = isActivityActive,
                    onClick = onActivityClick,
                    enabled = !isMenuOpen,
                    modifier = Modifier.weight(1f)
                )
                NavigationTab(
                    icon = R.drawable.nav_ajustes,
                    text = "Ajustes",
                    isActive = isSettingsActive,
                    onClick = onSettingsClick,
                    enabled = !isMenuOpen,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // Clásico: FAB elevado sobre el notch.
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationTab(
                    icon = R.drawable.nav_hornos,
                    text = "Hornos",
                    isActive = isKilnsActive,
                    onClick = onKilnsClick,
                    enabled = !isMenuOpen
                )
                NavigationTab(
                    icon = R.drawable.nav_programas,
                    text = "Programas",
                    isActive = isProgramsActive,
                    onClick = onProgramsClick,
                    enabled = !isMenuOpen
                )
            }

            if (showFab) {
                if (isMenuOpen) {
                    CloseBtnCircle(
                        onClick = onPlusClick,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(56.dp)
                            .offset(y = (-40).dp)
                    )
                } else {
                    Plusbtn(
                        onClick = onPlusClick,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .size(56.dp)
                            .offset(y = (-40).dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationTab(
                    icon = R.drawable.nav_actividad,
                    text = "Actividad",
                    isActive = isActivityActive,
                    onClick = onActivityClick,
                    enabled = !isMenuOpen
                )
                NavigationTab(
                    icon = R.drawable.nav_ajustes,
                    text = "Ajustes",
                    isActive = isSettingsActive,
                    onClick = onSettingsClick,
                    enabled = !isMenuOpen
                )
            }
        }
    }
}

/**
 * Componente individual para cada tab de navegación
 */
@Composable
private fun NavigationTab(
    icon: Int,
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true // Nuevo parámetro para habilitar/deshabilitar el botón
) {
    val kilnColors = LocalKilnColors.current
    val activeColor = if (kilnColors.isEsp) kilnColors.accent else kilnColors.onAccent
    val inactiveColor = if (kilnColors.isEsp) kilnColors.muted else kilnColors.onAccent.copy(alpha = 0.85f)
    val contentColor = if (isActive) activeColor else inactiveColor

    Column(
        modifier = modifier
            .clickable(enabled = enabled) { onClick() } // Solo responde a clicks si está habilitado
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Icon
        Icon(
            painter = painterResource(id = icon),
            contentDescription = "$text-icon",
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        
        // Text
        Text(
            text = text,
            color = contentColor,
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
            )
        )
        
        // Active indicator
        if (isActive) {
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .height(3.dp)
                    .background(
                        color = activeColor,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
