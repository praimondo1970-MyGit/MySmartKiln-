package com.example.mysmartkiln.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mysmartkiln.data.repository.ProgramRepository
import com.example.mysmartkiln.ui.components.StageData
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de Crear Programa.
 * 
 * Maneja:
 * - Estado de la UI (nombre del programa, etapas)
 * - Lógica de negocio (validaciones, guardar)
 * - Comunicación con el Repository
 * 
 * Ventajas del ViewModel:
 * - Sobrevive a rotaciones de pantalla
 * - Separa lógica de UI
 * - Maneja coroutines de forma segura
 */
class CreateProgramViewModel(
    private val repository: ProgramRepository
) : ViewModel() {
    
    // ============================================
    // ESTADO DE LA UI
    // ============================================
    
    /**
     * ID del programa original cuando estamos en modo edición.
     * Null si estamos creando un programa nuevo.
     */
    var originalProgramId by mutableStateOf<String?>(null)
        private set
    
    /**
     * Indica si estamos en modo edición (true) o creación (false).
     */
    var isEditMode by mutableStateOf(false)
        private set
    
    /**
     * Nombre del programa que el usuario está creando.
     */
    var programName by mutableStateOf("")
        private set
    
    /**
     * Lista de etapas del programa.
     */
    var stages by mutableStateOf<List<StageData>>(emptyList())
        private set
    
    /**
     * Indica si se está guardando el programa.
     */
    var isSaving by mutableStateOf(false)
        private set
    
    /**
     * Mensaje de éxito al guardar.
     */
    var successMessage by mutableStateOf<String?>(null)
        private set
    
    /**
     * Mensaje de error si algo falla.
     */
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    /**
     * Indica si el programa se guardó exitosamente (para navegar).
     */
    var programSavedSuccessfully by mutableStateOf(false)
        private set
    
    // ============================================
    // FUNCIONES PÚBLICAS (llamadas desde la UI)
    // ============================================
    
    /**
     * Actualiza el nombre del programa.
     * 
     * @param name Nuevo nombre
     */
    fun updateProgramName(name: String) {
        programName = name
    }
    
    /**
     * Agrega una nueva etapa a la lista.
     * 
     * @param stage Datos de la etapa
     */
    fun addStage(stage: StageData) {
        stages = stages + stage
    }
    
    /**
     * Actualiza una etapa existente.
     * 
     * @param index Índice de la etapa a actualizar
     * @param stage Nuevos datos de la etapa
     */
    fun updateStage(index: Int, stage: StageData) {
        if (index in stages.indices) {
            val updatedStages = stages.toMutableList()
            updatedStages[index] = stage
            stages = updatedStages
        }
    }
    
    /**
     * Elimina una etapa de la lista.
     * 
     * @param index Índice de la etapa a eliminar
     */
    fun deleteStage(index: Int) {
        if (index in stages.indices) {
            val updatedStages = stages.toMutableList()
            updatedStages.removeAt(index)
            // Renumerar las etapas
            stages = updatedStages.mapIndexed { newIndex, stage ->
                stage.copy(stageNumber = newIndex + 1)
            }
        }
    }
    
    /**
     * Guarda el programa completo en la base de datos.
     * 
     * Validaciones:
     * - Nombre no vacío
     * - Al menos una etapa
     * - Temperatura y velocidad obligatorias
     * 
     * Si estamos en modo edición, actualiza el programa existente.
     * Si estamos creando uno nuevo, crea un nuevo registro.
     */
    fun saveProgram() {
        // Limpiar mensajes previos
        successMessage = null
        errorMessage = null
        
        // Validar nombre
        if (programName.isBlank()) {
            errorMessage = "El nombre del programa no puede estar vacío"
            return
        }
        
        // Validar que haya al menos una etapa
        if (stages.isEmpty()) {
            errorMessage = "Debes agregar al menos una etapa"
            return
        }
        
        // Validar campos obligatorios de cada etapa
        stages.forEachIndexed { index, stage ->
            if (stage.temperature.isBlank()) {
                errorMessage = "La temperatura de la etapa ${index + 1} es obligatoria"
                return
            }
            if (stage.speed.isBlank()) {
                errorMessage = "La velocidad de la etapa ${index + 1} es obligatoria"
                return
            }
        }
        
        // Guardar en base de datos
        isSaving = true
        viewModelScope.launch {
            try {
                if (isEditMode && originalProgramId != null) {
                    // MODO EDICIÓN: Actualizar programa existente
                    repository.updateProgramName(originalProgramId!!, programName)
                    repository.updateProgramStages(originalProgramId!!, stages)
                    
                    successMessage = "Programa '$programName' actualizado exitosamente"
                } else {
                    // MODO CREACIÓN: Crear nuevo programa
                    val programId = repository.saveProgram(programName, stages)
                    
                    successMessage = "Programa '$programName' guardado exitosamente"
                }
                
                // Éxito
                isSaving = false
                programSavedSuccessfully = true  // Marcar como guardado para navegar
                
                // Limpiar formulario después de guardar
                clearForm()
                
            } catch (e: Exception) {
                // Error
                errorMessage = "Error al guardar: ${e.message}"
                isSaving = false
            }
        }
    }
    
    /**
     * Limpia el formulario después de guardar.
     */
    private fun clearForm() {
        programName = ""
        stages = emptyList()
        originalProgramId = null
        isEditMode = false
    }
    
    /**
     * Limpia los mensajes de éxito/error.
     */
    fun clearMessages() {
        successMessage = null
        errorMessage = null
    }
    
    /**
     * Resetea el flag de guardado exitoso.
     */
    fun resetSaveState() {
        programSavedSuccessfully = false
    }
    
    /**
     * Carga las etapas de un programa existente para duplicación.
     * El nombre se deja vacío para que el usuario lo complete.
     * 
     * @param programId ID del programa a duplicar
     */
    fun loadProgramForDuplication(programId: String) {
        isEditMode = false
        originalProgramId = null
        loadProgram(programId, includeNameForDuplication = false)
    }
    
    /**
     * Carga un programa existente para edición.
     * Carga tanto el nombre como las etapas.
     * 
     * @param programId ID del programa a editar
     */
    fun loadProgramForEditing(programId: String) {
        isEditMode = true
        originalProgramId = programId
        loadProgram(programId, includeNameForDuplication = true)
    }
    
    /**
     * Carga un programa desde la base de datos.
     * 
     * @param programId ID del programa
     * @param includeNameForDuplication Si es true, carga el nombre; si es false, lo deja vacío
     */
    private fun loadProgram(programId: String, includeNameForDuplication: Boolean) {
        viewModelScope.launch {
            try {
                // Obtener el programa original
                val originalProgram = repository.getProgramById(programId)
                if (originalProgram != null) {
                    // Obtener las etapas del programa original
                    repository.getStagesByProgram(programId).collect { stageEntities ->
                        // Convertir a StageData
                        val stageDataList = stageEntities.map { stage ->
                            StageData(
                                stageNumber = stage.stageNumber,
                                temperature = stage.temperature,
                                speed = stage.speed,
                                maintenance = stage.maintenance
                            )
                        }
                        
                        // Cargar las etapas
                        stages = stageDataList
                        
                        // Cargar el nombre solo si es para edición
                        programName = if (includeNameForDuplication) {
                            originalProgram.name
                        } else {
                            "" // Nombre vacío para duplicación
                        }
                        
                        val mode = if (includeNameForDuplication) "edición" else "duplicación"
                        println("DEBUG: Programa cargado para $mode: ${stageDataList.size} etapas, nombre='$programName'")
                        return@collect // Solo la primera emisión
                    }
                } else {
                    errorMessage = "No se pudo encontrar el programa original"
                }
            } catch (e: Exception) {
                println("DEBUG: Error al cargar programa: ${e.message}")
                errorMessage = "Error al cargar el programa: ${e.localizedMessage}"
            }
        }
    }
}








