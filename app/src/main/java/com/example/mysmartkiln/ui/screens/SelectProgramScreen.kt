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
import com.example.mysmartkiln.ui.components.BottomNavBarBackground
import com.example.mysmartkiln.ui.components.SearchBar
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
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
            .background(color = LocalKilnColors.current.screenBackground)
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
                    tint = LocalKilnColors.current.accent
                )
            }
            
            // Title centered
            Text(
                text = "Seleccionar Programa",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = LocalKilnColors.current.title,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = "Buscar programas...",
            onSearchClick = { /* Solo funciona como filtro */ },
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
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
                
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
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
                    tint = LocalKilnColors.current.accent
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
                    color = LocalKilnColors.current.title,
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Subtitle
            Text(
                text = "Primero debes crear un programa\ndesde la pantalla de Programas",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = LocalKilnColors.current.title
                ),
                textAlign = TextAlign.Center
            )
        }
        
        // Bottom spacing for navigation
        Spacer(modifier = Modifier.height(240.dp))
    }
}

