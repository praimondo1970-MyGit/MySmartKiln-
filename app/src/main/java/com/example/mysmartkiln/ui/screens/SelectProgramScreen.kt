package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.ProgramCard
import com.example.mysmartkiln.ui.components.ProgramCardData
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.components.ScrollGradient
import com.example.mysmartkiln.ui.components.SearchBar
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.viewmodels.ProgramsListViewModel
import com.example.mysmartkiln.ui.viewmodels.ViewModelFactory

@Composable
fun SelectProgramScreen(
    onProgramSelected: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onNavigateToKilns: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onCreateProgram: () -> Unit = {},
    onNavigateToKilnsWithScan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ProgramsListViewModel = viewModel(factory = ViewModelFactory(context))
    
    val programs by viewModel.filteredPrograms.collectAsState()
    val hasPrograms by viewModel.hasPrograms.collectAsState()
    val searchQuery = viewModel.searchQuery
    
    // Estado para controlar el menú "+"
    var isMenuOpen by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColors.Primary100)
    ) {
        // Contenido principal - SIEMPRE VISIBLE
        if (hasPrograms) {
            // Show programs list
            SelectProgramListContent(
                programs = programs,
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onProgramSelected = onProgramSelected,
                onNavigateBack = onNavigateBack,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Show empty state
            EmptySelectProgramContent(
                onNavigateBack = onNavigateBack,
                modifier = Modifier.fillMaxSize()
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
                    onCreateProgram()
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
                onActivityClick = onNavigateToActivity,
                onSettingsClick = onNavigateToSettings,
                onPlusClick = { isMenuOpen = !isMenuOpen }, // Toggle menu
                isKilnsActive = false,
                isProgramsActive = false,
                isActivityActive = false,
                isSettingsActive = false,
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

@Composable
private fun SelectProgramListContent(
    programs: List<ProgramCardData>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onProgramSelected: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 10.dp)
    ) {
        // Top Bar with back button and title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 24.dp, top = 60.dp, bottom = 16.dp)
        ) {
            // Back button
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = AppColors.Primary700
                )
            }
            
            // Title centered
            Text(
                text = "Seleccionar Programa",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color(0xFF1E3A8A),
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearchClick = { /* Solo funciona como filtro */ },
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        )
        
        // Programs list with gradient
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 29.dp, end = 24.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(programs) { program ->
                    ProgramCard(
                        program = program,
                        isOpen = false, // No swipe actions in selection mode
                        onSwipeStateChange = { },
                        onCardClick = { onProgramSelected(program.id) }, // Select on click
                        onDelete = { }, // No delete in selection mode
                        onDuplicate = { }, // No duplicate in selection mode
                        showArrow = false // No mostrar botón ">" en modo selección
                    )
                }
                
                // Extra space at the end for gradient
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
            
            // Gradient at the bottom
            ScrollGradient(
                height = 40.dp,
                backgroundColor = AppColors.Primary100,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Bottom spacing for navigation
        Spacer(modifier = Modifier.height(160.dp))
    }
}

@Composable
private fun EmptySelectProgramContent(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Bar with back button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, top = 60.dp, bottom = 16.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = AppColors.Primary700
                )
            }
        }
        
        // Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            // Empty State Illustration
            Image(
                painter = painterResource(id = R.drawable.img_sin_hornos),
                contentDescription = "Sin programas guardados",
                modifier = Modifier
                    .size(200.dp)
                    .padding(bottom = 32.dp)
            )
            
            // Title
            Text(
                text = "No hay programas guardados",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = Color(0xFF1E3A8A),
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Subtitle
            Text(
                text = "Primero debes crear un programa\ndesde la pantalla de Programas",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = Color(0xFF1E3A8A)
                ),
                textAlign = TextAlign.Center
            )
        }
        
        // Bottom spacing for navigation
        Spacer(modifier = Modifier.height(240.dp))
    }
}

