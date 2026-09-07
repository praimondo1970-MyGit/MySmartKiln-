package com.example.mysmartkiln.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
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
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mysmartkiln.ui.components.AppPrimaryButton
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.Plusbtn
import com.example.mysmartkiln.ui.components.ProgramCard
import com.example.mysmartkiln.ui.components.ProgramCardData
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.components.ScrollGradient
import com.example.mysmartkiln.ui.components.SearchBar
import com.example.mysmartkiln.ui.viewmodels.ProgramsListViewModel
import com.example.mysmartkiln.ui.viewmodels.ViewModelFactory
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun ProgramsScreen(
    onNavigateToKilns: () -> Unit = {
},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onCreateProgram: () -> Unit = {},
    onEditProgram: (String) -> Unit = {},
    onDuplicateProgram: (String) -> Unit = {},
    onNavigateToKilnsWithScan: () -> Unit = {}, // Nuevo parámetro para navegar con escaneo
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: ProgramsListViewModel = viewModel(factory = ViewModelFactory(context))
    
    val programs by viewModel.filteredPrograms.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasPrograms by viewModel.hasPrograms.collectAsState()
    val searchQuery = viewModel.searchQuery
    
    // Estado para controlar el menú "+"
    var isMenuOpen by remember { mutableStateOf(false) }
    
    // Debug logs
    println("DEBUG ProgramsScreen: hasPrograms = $hasPrograms")
    println("DEBUG ProgramsScreen: programs count = ${programs.size}")
    println("DEBUG ProgramsScreen: isLoading = $isLoading")
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = LocalKilnColors.current.screenBackground)
    ) {
        // Contenido principal - SIEMPRE VISIBLE
        if (hasPrograms) {
            // Show programs list
            ProgramsListContent(
                programs = programs,
                searchQuery = searchQuery,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onProgramClick = onEditProgram,  // Botón ">" - editar con nombre
                onCreateProgram = onCreateProgram,
                onDeleteProgram = viewModel::deleteProgram,
                onDuplicateProgram = onDuplicateProgram,  // Botón "duplicar" - sin nombre
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Show empty state
            EmptyProgramsContent(
                onCreateProgram = onCreateProgram,
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
                       color = LocalKilnColors.current.navBar,
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
            onProgramsClick = { /* Already on Programs screen */ },
                onActivityClick = onNavigateToActivity,
            onSettingsClick = onNavigateToSettings,
            onPlusClick = { isMenuOpen = !isMenuOpen }, // Toggle menu
            isKilnsActive = false,
            isProgramsActive = true,
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
private fun ProgramsListContent(
    programs: List<ProgramCardData>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onProgramClick: (String) -> Unit,
    onCreateProgram: () -> Unit,
    onDeleteProgram: (String) -> Unit,
    onDuplicateProgram: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Estado para rastrear qué tarjeta está abierta
    var openCardId by remember { mutableStateOf<String?>(null) }
    
        Column(
        modifier = modifier.padding(vertical = 10.dp) // Remover padding horizontal para permitir swipe
    ) {
        // Title - Same position as "Crear Programa"
        Text(
            text = "Programas",
            style = MaterialTheme.typography.titleLarge.copy(
                color = LocalKilnColors.current.title,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 70.dp, bottom = 16.dp)
        )
        
        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearchClick = { /* Solo funciona como filtro, no navega */ },
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp)
        )
        
        // Programs list con gradiente
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
                        isOpen = openCardId == program.id,
                        onSwipeStateChange = { isOpen ->
                            openCardId = if (isOpen) program.id else null
                        },
                        onCardClick = { onProgramClick(program.id) },
                        onDelete = { onDeleteProgram(program.id) },
                        onDuplicate = { onDuplicateProgram(program.id) }
                    )
                }
                
                // Espacio adicional al final para el gradiente
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
            
            // Gradiente en la parte inferior
            ScrollGradient(
                height = 40.dp,
                backgroundColor = LocalKilnColors.current.screenBackground,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Bottom spacing for navigation
        Spacer(modifier = Modifier.height(160.dp))
    }
}

@Composable
private fun EmptyProgramsContent(
    onCreateProgram: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Content Group
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Empty State Illustration
                Image(
                    painter = painterResource(id = R.drawable.img_sin_hornos),
                contentDescription = "Sin programas guardados",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(bottom = 32.dp)
                    .offset(y = 110.dp)
                )
                
            // Title
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                    .height(60.dp)
                    .offset(y = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hay programas guardados",
                        style = MaterialTheme.typography.titleLarge.copy(
                        color = LocalKilnColors.current.title,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )
                }
                
            // Subtitle
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                    .height(80.dp)
                    .offset(y = 119.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Cuando estés listo para comenzar",
                            style = MaterialTheme.typography.bodyLarge.copy(
                            color = LocalKilnColors.current.title
                            ),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "presiona comenzar para crear",
                            style = MaterialTheme.typography.bodyLarge.copy(
                            color = LocalKilnColors.current.title
                            ),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "tu primer programa de cocción",
                            style = MaterialTheme.typography.bodyLarge.copy(
                            color = LocalKilnColors.current.title
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
        // Bottom Content - Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Start Button
                androidx.compose.material3.Button(
                onClick = onCreateProgram,
                    enabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                    .height(48.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = LocalKilnColors.current.accent,
                        contentColor = Color.White
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Comenzar",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                // Spacer to push button up from bottom navigation
                Spacer(modifier = Modifier.height(240.dp))
            }
    }
}

@Preview(widthDp = 390, heightDp = 844)
@Composable
private fun ProgramsScreenPreview() {
    MySmartKilnTheme {
        ProgramsScreen()
    }
}
