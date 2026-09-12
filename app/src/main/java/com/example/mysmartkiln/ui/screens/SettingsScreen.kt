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
import com.example.mysmartkiln.ui.components.BottomNavBarBackground
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
            // Navigation Bar Background
            BottomNavBarBackground(
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 0.dp, y = 60.dp)
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
            .background(LocalKilnColors.current.cardBackground)
            .padding(20.dp)
    ) {
        Column {
            Text(
                text = "Pablo Raimondo",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = LocalKilnColors.current.body,
                    fontWeight = FontWeight.Medium,
                    fontSize = 17.sp // Incrementado en 1sp desde bodyLarge (16sp)
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "praimondo1970@hotmail.com",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = LocalKilnColors.current.body
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
                color = LocalKilnColors.current.title,
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
                    .background(LocalKilnColors.current.cardBackground)
                    .border(1.dp, LocalKilnColors.current.cardBorder, RoundedCornerShape(12.dp))
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
                            color = LocalKilnColors.current.body
                        )
                    )
                    
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = "Dropdown",
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (showDropdown) 180f else 0f),
                        tint = LocalKilnColors.current.body
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
                        .background(LocalKilnColors.current.cardBackground)
                        .border(1.dp, LocalKilnColors.current.cardBorder, RoundedCornerShape(12.dp))
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
                                        color = LocalKilnColors.current.body
                                    )
                                )
                                
                                // Tilde para opción activa
                                if (language == selectedLanguage) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Seleccionado",
                                        tint = LocalKilnColors.current.body,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (language != languages.last()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(LocalKilnColors.current.accent.copy(alpha = 0.15f))
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
                color = LocalKilnColors.current.title,
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
                    .background(LocalKilnColors.current.cardBackground)
                    .border(1.dp, LocalKilnColors.current.cardBorder, RoundedCornerShape(12.dp))
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
                            color = LocalKilnColors.current.body
                        )
                    )
                    
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_down),
                        contentDescription = "Dropdown",
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(if (showDropdown) 180f else 0f),
                        tint = LocalKilnColors.current.body
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
                        .background(LocalKilnColors.current.cardBackground)
                        .border(1.dp, LocalKilnColors.current.cardBorder, RoundedCornerShape(12.dp))
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
                                        color = LocalKilnColors.current.body
                                    )
                                )
                                
                                // Tilde para opción activa
                                if (unit == selectedUnits) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = "Seleccionado",
                                        tint = LocalKilnColors.current.body,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            if (unit != units.last()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(LocalKilnColors.current.accent.copy(alpha = 0.15f))
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