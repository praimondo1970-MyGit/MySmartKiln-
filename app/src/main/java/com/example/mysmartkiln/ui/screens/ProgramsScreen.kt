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
import com.example.mysmartkiln.ui.components.EspBrandHeader
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.Plusbtn
import com.example.mysmartkiln.ui.components.ProgramCard
import com.example.mysmartkiln.ui.components.ProgramCardData
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.components.BottomNavBarBackground
import com.example.mysmartkiln.ui.components.SearchBar
import com.example.mysmartkiln.ui.viewmodels.ProgramsListViewModel
import com.example.mysmartkiln.ui.viewmodels.ViewModelFactory
import com.example.mysmartkiln.R
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun ProgramsScreen(
    onNavigateToKilns: () -> Unit = {},
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
            // Navigation Bar Background
            BottomNavBarBackground(
                modifier = Modifier
                    .align(alignment = Alignment.TopStart)
                    .offset(x = 0.dp, y = 60.dp)
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
    val kilnColors = LocalKilnColors.current
    // Estado para rastrear qué tarjeta está abierta
    var openCardId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.padding(vertical = 10.dp) // Remover padding horizontal para permitir swipe
    ) {
        if (kilnColors.isEsp) {
            EspBrandHeader(
                modifier = Modifier.padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 48.dp,
                    bottom = 8.dp
                )
            )
            Text(
                text = "Programas",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = kilnColors.title,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 8.dp,
                    bottom = 16.dp
                )
            )
        } else {
            Text(
                text = "Programas",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = kilnColors.title,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 70.dp, bottom = 16.dp)
            )
        }
        
        // Search bar (mockup ESP: compacta, icono a la izquierda)
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = "Buscar programas...",
            onSearchClick = { /* Solo funciona como filtro, no navega */ },
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
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
                
                // Espacio final para poder desplazar la última tarjeta
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
        
        // Deja ~12–15dp entre el final del scroll y el botón + (ESP).
        Spacer(
            modifier = Modifier.height(if (kilnColors.isEsp) 110.dp else 160.dp)
        )
    }
}

@Composable
private fun EmptyProgramsContent(
    onCreateProgram: () -> Unit,
    modifier: Modifier = Modifier
) {
    val kilnColors = LocalKilnColors.current
    Column(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (kilnColors.isEsp) {
                EspBrandHeader(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, bottom = 8.dp)
                )
            }
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
                        contentColor = LocalKilnColors.current.onAccent
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
