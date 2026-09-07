package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mysmartkiln.R
import com.example.mysmartkiln.data.auth.AuthViewModel
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.components.ScrollGradient
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.LocalThemeController
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme
import com.example.mysmartkiln.ui.theme.ThemeController
import com.example.mysmartkiln.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    onNavigateToKilns: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onCreateProgram: () -> Unit = {},
    onNavigateToKilnsWithScan: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: AuthViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // Estados para controlar el menú "+"
    var isMenuOpen by remember { mutableStateOf(false) }
    val kilnColors = LocalKilnColors.current
    val themeController = LocalThemeController.current
    val themeMode by themeController.mode.collectAsState()
    
    // Estados para los dropdowns
    var selectedLanguage by remember { mutableStateOf("Español") }
    var selectedUnits by remember { mutableStateOf("Métricas") }
    val selectedTheme = if (themeMode == ThemeMode.ESP) "Oscuro" else "Claro"
    
    // Estados para mostrar/ocultar dropdowns
    var showLanguageDropdown by remember { mutableStateOf(false) }
    var showUnitsDropdown by remember { mutableStateOf(false) }
    var showThemeDropdown by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = kilnColors.screenBackground)
    ) {
        // Contenido principal con gradiente
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
            Spacer(modifier = Modifier.height(80.dp)) // Espacio desde el top
            
            // Título "Detalles de la cuenta" (fuera de la tarjeta)
            Text(
                text = "Detalles de la cuenta",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = kilnColors.title,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Tarjeta de detalles de la cuenta
            AccountDetailsCard()
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Campo de idioma
            LanguageSelector(
                selectedLanguage = selectedLanguage,
                onLanguageSelected = { selectedLanguage = it },
                showDropdown = showLanguageDropdown,
                onDropdownToggle = { showLanguageDropdown = !showLanguageDropdown }
            )
            
            // Espaciado dinámico basado en si el dropdown de idioma está abierto
            Spacer(modifier = Modifier.height(if (showLanguageDropdown) 80.dp else 16.dp))
            
            // Campo de unidades
            UnitsSelector(
                selectedUnits = selectedUnits,
                onUnitsSelected = { selectedUnits = it },
                showDropdown = showUnitsDropdown,
                onDropdownToggle = { showUnitsDropdown = !showUnitsDropdown }
            )
            
            // Espaciado dinámico basado en si el dropdown de unidades está abierto
            Spacer(modifier = Modifier.height(if (showUnitsDropdown) 80.dp else 16.dp))
            
            // Campo de tema
            ThemeSelector(
                selectedTheme = selectedTheme,
                onThemeSelected = { label ->
                    themeController.setMode(
                        if (label == "Oscuro") ThemeMode.ESP else ThemeMode.CLASSIC
                    )
                },
                showDropdown = showThemeDropdown,
                onDropdownToggle = { showThemeDropdown = !showThemeDropdown }
            )
            
            // Espaciado dinámico basado en si el dropdown de tema está abierto
            Spacer(modifier = Modifier.height(if (showThemeDropdown) 80.dp else 0.dp))
            
            // Spacer para empujar el botón hacia abajo
            Spacer(modifier = Modifier.weight(1f))
            
            // Botón de cerrar sesión a 180dp del final
            LogoutButton(onLogout = {
                viewModel.logout()
                onLogout()
            })
            
            Spacer(modifier = Modifier.height(180.dp))
            }
            
            // Gradiente en la parte inferior
            ScrollGradient(
                height = 40.dp,
                backgroundColor = kilnColors.screenBackground,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Overlay del menú de agregar - SE SUPERPONE cuando isMenuOpen = true
        if (isMenuOpen) {
            AddMenuScreen(
                onNewKilnClick = {
                    isMenuOpen = false
                    onNavigateToKilnsWithScan() // Navegar a Hornos e iniciar escaneo
                },
                onNewProgramClick = {
                    isMenuOpen = false
                    onCreateProgram() // Navegar a crear programa directamente
                },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f) // Por encima del contenido pero debajo de la navegación
            )
        }
        
        // Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomStart)
                .fillMaxWidth()
                .requiredHeight(height = 200.dp)
                .zIndex(2f) // Por encima del overlay del menú
        ) {
            // Navigation Bar Background - Full width with precise curved top
            Box(
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 0.dp, y = 60.dp)
                    .fillMaxWidth()
                    .requiredHeight(height = 141.dp)
                    .background(
                        color = kilnColors.navBar,
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
                onActivityClick = onNavigateToActivity,
                onSettingsClick = { /* Already on Settings screen */ },
                onPlusClick = { isMenuOpen = !isMenuOpen }, // Toggle menu
                isKilnsActive = false,
                isProgramsActive = false,
                isActivityActive = false,
                isSettingsActive = true,
                showFab = true, // Mostrar el botón FAB
                isMenuOpen = isMenuOpen, // Pass state to ResponsiveBottomNavigation
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

// Componente para la tarjeta de detalles de la cuenta
@Composable
private fun AccountDetailsCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Pablo Raimondo",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = AppColors.Primary600,
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp // Incrementado en 1sp desde bodyLarge (16sp)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "praimondo1970@hotmail.com",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = AppColors.Primary600
                )
            )
        }
    }
}

// Componente selector de idioma
@Composable
private fun LanguageSelector(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    showDropdown: Boolean,
    onDropdownToggle: () -> Unit
) {
    val languages = listOf("Español", "Inglés")
    
    Column {
        Text(
            text = "Idioma",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = AppColors.Primary800,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box {
            // Campo principal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, AppColors.Primary100, RoundedCornerShape(12.dp))
                    .clickable { onDropdownToggle() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedLanguage,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = AppColors.Primary600
                        )
                    )
                    
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = "Dropdown",
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (showDropdown) 180f else 0f),
                        tint = AppColors.Primary600
                    )
                }
            }
            
            // Dropdown options
            if (showDropdown) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, AppColors.Primary100, RoundedCornerShape(12.dp))
                        .zIndex(10f) // Aumentar zIndex para que aparezca por encima de todos los elementos
                ) {
                    Column {
                        languages.forEach { language ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLanguageSelected(language)
                                        onDropdownToggle()
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = language,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = AppColors.Primary600
                                    )
                                )
                                
                                // Tilde para opción activa
                                if (language == selectedLanguage) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Seleccionado",
                                        tint = AppColors.Primary600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (language != languages.last()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(AppColors.Primary100)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Componente selector de unidades
@Composable
private fun UnitsSelector(
    selectedUnits: String,
    onUnitsSelected: (String) -> Unit,
    showDropdown: Boolean,
    onDropdownToggle: () -> Unit
) {
    val units = listOf("Métricas", "Imperial")
    
    Column {
        Text(
            text = "Unidades",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = AppColors.Primary800,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box {
            // Campo principal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, AppColors.Primary100, RoundedCornerShape(12.dp))
                    .clickable { onDropdownToggle() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedUnits,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = AppColors.Primary600
                        )
                    )
                    
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = "Dropdown",
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (showDropdown) 180f else 0f),
                        tint = AppColors.Primary600
                    )
                }
            }
            
            // Dropdown options
            if (showDropdown) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, AppColors.Primary100, RoundedCornerShape(12.dp))
                        .zIndex(10f) // Aumentar zIndex para que aparezca por encima de todos los elementos
                ) {
                    Column {
                        units.forEach { unit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onUnitsSelected(unit)
                                        onDropdownToggle()
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = unit,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = AppColors.Primary600
                                    )
                                )
                                
                                // Tilde para opción activa
                                if (unit == selectedUnits) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Seleccionado",
                                        tint = AppColors.Primary600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (unit != units.last()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(AppColors.Primary100)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Componente selector de tema
@Composable
private fun ThemeSelector(
    selectedTheme: String,
    onThemeSelected: (String) -> Unit,
    showDropdown: Boolean,
    onDropdownToggle: () -> Unit
) {
    val themes = listOf("Claro", "Oscuro")
    val kilnColors = LocalKilnColors.current

    Column {
        Text(
            text = "Tema",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = kilnColors.title,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Box {
            // Campo principal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(kilnColors.cardBackground)
                    .border(1.dp, kilnColors.cardBorder, RoundedCornerShape(12.dp))
                    .clickable { onDropdownToggle() }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedTheme,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = kilnColors.body
                        )
                    )
                    
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = "Dropdown",
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (showDropdown) 180f else 0f),
                        tint = kilnColors.body
                    )
                }
            }
            
            // Dropdown options
            if (showDropdown) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(kilnColors.cardBackground)
                        .border(1.dp, kilnColors.cardBorder, RoundedCornerShape(12.dp))
                        .zIndex(10f) // Aumentar zIndex para que aparezca por encima de todos los elementos
                ) {
                    Column {
                        themes.forEach { theme ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onThemeSelected(theme)
                                        onDropdownToggle()
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = theme,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = kilnColors.body
                                    )
                                )
                                
                                // Tilde para opción activa
                                if (theme == selectedTheme) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Seleccionado",
                                        tint = kilnColors.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (theme != themes.last()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(kilnColors.cardBorder)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Componente botón de cerrar sesión
@Composable
private fun LogoutButton(onLogout: () -> Unit) {
    val kilnColors = LocalKilnColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(kilnColors.accent)
            .clickable { onLogout() }
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = "Cerrar sesión",
            style = MaterialTheme.typography.titleMedium.copy(
                color = kilnColors.onAccent,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val controller = remember { ThemeController(context) }
    CompositionLocalProvider(LocalThemeController provides controller) {
        MySmartKilnTheme(themeMode = ThemeMode.CLASSIC) {
            SettingsScreen()
        }
    }
}