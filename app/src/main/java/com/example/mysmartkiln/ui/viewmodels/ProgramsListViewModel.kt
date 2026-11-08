package com.example.mysmartkiln.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mysmartkiln.data.local.entities.ProgramEntity
import com.example.mysmartkiln.data.local.entities.StageEntity
import com.example.mysmartkiln.data.repository.ProgramRepository
import com.example.mysmartkiln.ui.components.ProgramCardData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProgramsListViewModel(
    private val repository: ProgramRepository
) : ViewModel() {

    private val _programs = MutableStateFlow<List<ProgramCardData>>(emptyList())
    val programs: StateFlow<List<ProgramCardData>> = _programs.asStateFlow()

    private val _filteredPrograms = MutableStateFlow<List<ProgramCardData>>(emptyList())
    val filteredPrograms: StateFlow<List<ProgramCardData>> = _filteredPrograms.asStateFlow()

    private val _hasPrograms = MutableStateFlow(false)
    val hasPrograms: StateFlow<Boolean> = _hasPrograms.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        loadPrograms()
        // Debug: Verificar programas directamente
        debugCheckPrograms()
    }
    
    private fun debugCheckPrograms() {
        viewModelScope.launch {
            try {
                // Verificar directamente desde el repository
                repository.allPrograms.collect { programs ->
                    println("DEBUG DIRECT: Encontrados ${programs.size} programas en BD")
                    programs.forEach { program ->
                        println("DEBUG DIRECT: ${program.name} - ${program.id}")
                    }
                }
            } catch (e: Exception) {
                println("DEBUG DIRECT: Error verificando programas: ${e.message}")
            }
        }
    }

    fun loadPrograms() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                repository.allPrograms.collect { programEntities ->
                    // Debug log
                    println("DEBUG: Cargando ${programEntities.size} programas")
                    programEntities.forEach { program ->
                        println("DEBUG: Programa encontrado: ${program.name} (ID: ${program.id})")
                    }
                    
                    // Convertir cada programa de forma asíncrona
                    val programCardDataList = programEntities.map { program ->
                        try {
                            convertToProgramCardData(program)
                        } catch (e: Exception) {
                            println("DEBUG: Error al convertir programa ${program.name}: ${e.message}")
                            // Retornar datos por defecto en caso de error
                            ProgramCardData(
                                id = program.id,
                                name = program.name,
                                maxTemperature = "0",
                                duration = "0m",
                                stageCount = 0
                            )
                        }
                    }
                    _programs.value = programCardDataList
                    _hasPrograms.value = programCardDataList.isNotEmpty()
                    
                    println("DEBUG: hasPrograms = ${_hasPrograms.value}")
                    println("DEBUG: programs count = ${programCardDataList.size}")
                    
                    applySearchFilter(searchQuery)
                }
            } catch (e: Exception) {
                println("DEBUG: Error al cargar programas: ${e.message}")
                e.printStackTrace()
                errorMessage = "Error al cargar programas: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun convertToProgramCardData(program: ProgramEntity): ProgramCardData {
        // Obtener las etapas del programa usando first para obtener el primer valor del Flow
        val stages: List<StageEntity> = try {
            repository.getStagesByProgram(program.id).first()
        } catch (e: Exception) {
            println("DEBUG: Error al obtener etapas del programa ${program.name}: ${e.message}")
            emptyList()
        }
        
        println("DEBUG: Programa ${program.name} tiene ${stages.size} etapas")
        
        // Calcular temperatura máxima
        val maxTemp = stages.maxOfOrNull { stage ->
            stage.temperature.toIntOrNull() ?: 0
        } ?: 0
        
        // Calcular duración total
        var previousTemp = 0
        var totalMinutes = 0.0
        
        stages.sortedBy { it.stageNumber }.forEach { stage ->
            val currentTemp = stage.temperature.toIntOrNull() ?: 0
            val speed = stage.speed.toDoubleOrNull() ?: 1.0
            val maintenance = stage.maintenance.toIntOrNull() ?: 0
            
            // Tiempo de rampa: (temperatura objetivo - temperatura inicial) / velocidad
            val rampTime = if (speed > 0) {
                (currentTemp - previousTemp).toDouble() / speed
            } else {
                0.0
            }
            
            // Duración de esta etapa = tiempo de rampa + tiempo de mantenimiento
            totalMinutes += rampTime + maintenance
            
            // La temperatura final de esta etapa es la inicial de la siguiente
            previousTemp = currentTemp
        }
        
        println("DEBUG: Programa ${program.name} - Temp max: $maxTemp, Duración: ${formatDuration(totalMinutes.toInt())}")
        
        return ProgramCardData(
            id = program.id,
            name = program.name,
            maxTemperature = maxTemp.toString(),
            duration = formatDuration(totalMinutes.toInt()),
            stageCount = stages.size
        )
    }

    private fun formatDuration(totalMinutes: Int): String {
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            else -> "0m"
        }
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        applySearchFilter(query)
    }

    private fun applySearchFilter(query: String) {
        if (query.isBlank()) {
            _filteredPrograms.value = _programs.value
        } else {
            _filteredPrograms.value = _programs.value.filter { program ->
                program.name.contains(query, ignoreCase = true)
            }
        }
    }

    fun clearError() {
        errorMessage = null
    }

    // hasPrograms is now a StateFlow, so we don't need this function

    fun getFilteredProgramsCount(): Int {
        return _filteredPrograms.value.size
    }
    
    // Debug function to test database connection
    fun debugTestDatabase() {
        viewModelScope.launch {
            try {
                println("DEBUG TEST: Probando conexión a base de datos...")
                repository.allPrograms.collect { programs ->
                    println("DEBUG TEST: Conexión exitosa. Programas encontrados: ${programs.size}")
                    programs.forEach { program ->
                        println("DEBUG TEST: - ${program.name} (${program.id})")
                    }
                    return@collect // Solo la primera emisión
                }
            } catch (e: Exception) {
                println("DEBUG TEST: Error en conexión: ${e.message}")
            }
        }
    }

    fun deleteProgram(programId: String) {
        viewModelScope.launch {
            try {
                // Eliminar solo el programa seleccionado
                repository.deleteProgramById(programId)
                println("DEBUG: Programa $programId eliminado exitosamente")
                
                // Recargar la lista de programas
                loadPrograms()
            } catch (e: Exception) {
                println("DEBUG: Error al eliminar programa: ${e.message}")
                errorMessage = "Error al eliminar programa: ${e.localizedMessage}"
            }
        }
    }

    fun duplicateProgram(programId: String) {
        viewModelScope.launch {
            try {
                // Get the original program
                val originalProgram = repository.getProgramById(programId)
                if (originalProgram != null) {
                    // Get stages for the original program
                    repository.getStagesByProgram(programId).collect { stages ->
                        // Convert stages to StageData format
                        val stageDataList = stages.map { stage ->
                            com.example.mysmartkiln.ui.components.StageData(
                                stageNumber = stage.stageNumber,
                                temperature = stage.temperature,
                                speed = stage.speed,
                                maintenance = stage.maintenance
                            )
                        }
                        
                        // Create new program with duplicated name
                        val duplicatedName = "${originalProgram.name} (Copia)"
                        val newProgramId = repository.saveProgram(duplicatedName, stageDataList)
                        
                        println("DEBUG: Programa duplicado exitosamente con ID: $newProgramId")
                        return@collect // Solo la primera emisión
                    }
                } else {
                    errorMessage = "No se pudo encontrar el programa original"
                }
            } catch (e: Exception) {
                println("DEBUG: Error al duplicar programa: ${e.message}")
                errorMessage = "Error al duplicar programa: ${e.localizedMessage}"
            }
        }
    }
}
