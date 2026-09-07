package com.example.mysmartkiln.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mysmartkiln.ui.viewmodels.CreateProgramViewModel
import com.example.mysmartkiln.ui.viewmodels.ViewModelFactory
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.mysmartkiln.R
import androidx.compose.ui.zIndex
import com.example.mysmartkiln.ui.components.AddStageButton
import com.example.mysmartkiln.ui.components.AgregarButton
import com.example.mysmartkiln.ui.components.AgregarEtapa
import com.example.mysmartkiln.ui.components.CircularTerciary
import com.example.mysmartkiln.ui.components.CrearPrograma
import com.example.mysmartkiln.ui.components.Etapas
import com.example.mysmartkiln.ui.components.Group20
import com.example.mysmartkiln.ui.components.HomeIndicatorWhite
import com.example.mysmartkiln.ui.components.Plusbtn
import com.example.mysmartkiln.ui.components.ProgramNameInput
import com.example.mysmartkiln.ui.components.Rectangle134
import com.example.mysmartkiln.ui.components.ResponsiveBottomNavigation
import com.example.mysmartkiln.ui.components.BottomNavBarBackground
import com.example.mysmartkiln.ui.components.ScrollGradient
import com.example.mysmartkiln.ui.components.StageCard
import com.example.mysmartkiln.ui.components.StageData
import com.example.mysmartkiln.ui.theme.AppColors
import com.example.mysmartkiln.ui.theme.LocalKilnColors
import com.example.mysmartkiln.ui.theme.MySmartKilnTheme

@Composable
fun CreateProgramScreen(
    programIdToEdit: String? = null,
    isEditMode: Boolean = false,
    onNavigateBack: () -> Unit = {},
    onNavigateToAddStage: () -> Unit = {},
    onNavigateToKilns: () -> Unit = {},
    onNavigateToPrograms: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToKilnsWithScan: () -> Unit = {}, // Nuevo parámetro para navegar con escaneo
    modifier: Modifier = Modifier
) {
    // Obtener el ViewModel con el Factory
    val context = LocalContext.current
    val viewModel: CreateProgramViewModel = viewModel(
        factory = ViewModelFactory(context)
    )
    
    // Cargar programa si está en modo editar o duplicar
    LaunchedEffect(programIdToEdit, isEditMode) {
        programIdToEdit?.let { programId ->
            if (isEditMode) {
                // Modo editar: cargar nombre y etapas
                viewModel.loadProgramForEditing(programId)
            } else {
                // Modo duplicar: cargar solo etapas, nombre vacío
                viewModel.loadProgramForDuplication(programId)
            }
        }
    }
    
    // Volver a la pantalla anterior tras guardar (detalle del horno o lista de programas)
    LaunchedEffect(viewModel.programSavedSuccessfully) {
        if (viewModel.programSavedSuccessfully) {
            viewModel.resetSaveState()
            onNavigateBack()
        }
    }
    
    // Estado local de la UI (no relacionado con persistencia)
    var showAddStageOverlay by remember { mutableStateOf(false) }
    
    // Estado para controlar el menú "+"
    var isMenuOpen by remember { mutableStateOf(false) }
    
    // Usar el estado del ViewModel
    val programName = viewModel.programName
    val stages = viewModel.stages
    
    // Snackbar para mostrar mensajes
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Mostrar mensajes de éxito/error
    LaunchedEffect(viewModel.successMessage, viewModel.errorMessage) {
        viewModel.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
        viewModel.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessages()
        }
    }

    // Estado para los campos del modal
    var modalTemperature by remember { mutableStateOf("") }
    var modalSpeed by remember { mutableStateOf("") }
    var modalMaintenance by remember { mutableStateOf("") }
    
    // Estado para edición
    var isEditingStage by remember { mutableStateOf(false) }
    var editingStageIndex by remember { mutableStateOf(-1) }
    
    // Detectar cuando el teclado está visible para aplicar offset adicional
    val density = LocalDensity.current
    val keyboardHeight = WindowInsets.ime.getBottom(density)
    val isKeyboardVisible = keyboardHeight > 0

    // Offset adicional cuando el teclado está visible (puedes ajustar este valor)
    val additionalOffset = if (isKeyboardVisible) 60.dp else 0.dp

    // LazyListState para mejorar la detección de gestos
    val lazyListState = rememberLazyListState()
    
    // Función para resetear el modal
    fun resetModal() {
        modalTemperature = ""
        modalSpeed = ""
        modalMaintenance = ""
        isEditingStage = false
        editingStageIndex = -1
    }
    
    // Función para abrir modal para crear etapa nueva
    fun openCreateStageModal() {
        resetModal()
        showAddStageOverlay = true
    }
    
    // Función para cerrar el modal
    fun closeModal() {
        showAddStageOverlay = false
        resetModal()
    }
    
    // Función para validar y agregar/editar etapa
    fun addStage() {
        // Validar campos obligatorios
        if (modalTemperature.isBlank() || modalSpeed.isBlank()) {
            // TODO: Mostrar mensaje de error
            return
        }

        if (isEditingStage && editingStageIndex >= 0) {
            // Editar etapa existente usando ViewModel
            val updatedStage = StageData(
                stageNumber = editingStageIndex + 1,
                temperature = modalTemperature,
                speed = modalSpeed,
                maintenance = modalMaintenance.ifBlank { "0" }
            )
            viewModel.updateStage(editingStageIndex, updatedStage)
        } else {
            // Crear nueva etapa usando ViewModel
            val newStage = StageData(
                stageNumber = stages.size + 1,
                temperature = modalTemperature,
                speed = modalSpeed,
                maintenance = modalMaintenance.ifBlank { "0" }
            )
            viewModel.addStage(newStage)
        }

        // Cerrar modal y limpiar campos
        closeModal()
    }
    
    // Función para editar etapa
    fun editStage(stageIndex: Int) {
        val stage = stages[stageIndex]
        modalTemperature = stage.temperature
        modalSpeed = stage.speed
        modalMaintenance = stage.maintenance
        isEditingStage = true
        editingStageIndex = stageIndex
        showAddStageOverlay = true
    }

    // Función para eliminar etapa
    fun deleteStage(stageIndex: Int) {
        viewModel.deleteStage(stageIndex)
    }
    
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(color = LocalKilnColors.current.screenBackground)
    ) {
        // Scrollable content con gradiente
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 160.dp) // 160dp from bottom
        ) {
            LazyColumn(
                state = lazyListState, // Usar el estado para mejor detección de gestos
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 29.dp,
                    end = 24.dp,
                    top = 0.dp, // Sin padding superior - el contenido empieza desde arriba
                    bottom = 40.dp // Espacio para el gradiente
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Title según el modo
            item {
                CrearPrograma(
                    text = when {
                        programIdToEdit != null && isEditMode -> "Modificar programa"
                        programIdToEdit != null && !isEditMode -> "Duplicar programa"
                        else -> "Crear programa"
                    },
                    modifier = Modifier.padding(top = 70.dp) // Padding superior reducido a 70dp
                )
            }
            
            // Program Name Input
            item {
                ProgramNameInput(
                    value = programName,
                    onValueChange = { viewModel.updateProgramName(it) }
                )
            }
            
            // Etapas title and Add Stage Button row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Etapas()
                    AddStageButton(
                        onClick = {
                            openCreateStageModal()
                        }
                    )
                }
            }
            
            // Stages List
            if (stages.isNotEmpty()) {
                itemsIndexed(stages) { index, stage ->
                    StageCard(
                        stageData = stage,
                        onStageClick = { editStage(index) },
                        onDeleteClick = { deleteStage(index) }
                    )
                }
            }
            
            // Create Button - appears when there's at least one stage
            if (stages.isNotEmpty()) {
                item {
                           androidx.compose.material3.Button(
                               onClick = { viewModel.saveProgram() },
                               enabled = !viewModel.isSaving, // Deshabilitar mientras guarda
                               modifier = Modifier
                                   .fillMaxWidth()
                                   .height(48.dp) // Same height as "Comenzar" button
                                   .padding(top = 5.dp), // Padding superior reducido a 5dp
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = LocalKilnColors.current.accent,
                            contentColor = LocalKilnColors.current.onAccent,
                            disabledContainerColor = LocalKilnColors.current.cardBorder,
                            disabledContentColor = LocalKilnColors.current.muted
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = if (viewModel.isSaving) {
                                "Guardando..."
                            } else if (programIdToEdit != null) {
                                "Guardar"  // Tanto para editar como duplicar
                            } else {
                                "Crear"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
            
            // Gradiente en la parte inferior
            ScrollGradient(
                height = 40.dp,
                backgroundColor = LocalKilnColors.current.screenBackground,
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
                    // Ya estamos en crear programa, solo cerramos el menú
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
        
        // Add Stage Overlay - por encima de la barra de navegación/FAB
        AnimatedVisibility(
            visible = showAddStageOverlay,
            modifier = Modifier.zIndex(3f),
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(durationMillis = 300)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 300)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .background(
                        color = LocalKilnColors.current.navBar.copy(alpha = 0.9f)
                    )
            ) {
                // Rectangle134 component positioned at bottom with additional offset when keyboard is visible
                Rectangle134(
                    modifier = Modifier
                        .align(alignment = Alignment.BottomCenter)
                        .offset(y = additionalOffset)
                ) {
                           // Agregar/Editar etapa title positioned at top-left of the modal
                           AgregarEtapa(
                               text = if (isEditingStage) "Editar etapa" else "Agregar etapa",
                               modifier = Modifier
                                   .align(alignment = Alignment.TopStart)
                                   .padding(start = 30.dp, top = 25.dp) // Moved 5dp to the right (25dp + 5dp = 30dp)
                           )
                    
                    // CircularTerciary close button positioned in the modal
                    CircularTerciary(
                        onClick = { closeModal() },
                        modifier = Modifier
                            .align(alignment = Alignment.TopEnd)
                            .padding(end = 25.dp, top = 38.dp) // Moved 2dp left (27dp - 2dp = 25dp)
                    )
                    
                    // Group20 component with input fields positioned below the title
                    Group20(
                        modifier = Modifier
                            .align(alignment = Alignment.TopStart)
                            .padding(top = 100.dp), // Ya no necesita padding horizontal
                        temperature = modalTemperature,
                        speed = modalSpeed,
                        maintenance = modalMaintenance,
                        onTemperatureChange = { modalTemperature = it },
                        onSpeedChange = { modalSpeed = it },
                        onMaintenanceChange = { modalMaintenance = it }
                    )
                    
                    // Agregar button positioned at 200dp from top of the modal
                    AgregarButton(
                        onClick = { addStage() },
                        modifier = Modifier
                            .align(alignment = Alignment.TopStart)
                            .padding(start = 25.dp, end = 25.dp, top = 240.dp) // Aumentar de 200dp a 240dp
                    )
                }
            }
        }
        
        // Snackbar para mostrar mensajes de éxito/error
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 220.dp) // Por encima de la barra de navegación
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateProgramScreenPreview() {
    MySmartKilnTheme {
        CreateProgramScreen()
    }
}