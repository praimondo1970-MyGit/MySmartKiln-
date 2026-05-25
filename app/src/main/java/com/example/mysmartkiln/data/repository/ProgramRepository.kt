package com.example.mysmartkiln.data.repository

import com.example.mysmartkiln.data.local.dao.ProgramDao
import com.example.mysmartkiln.data.local.dao.StageDao
import com.example.mysmartkiln.data.local.entities.ProgramEntity
import com.example.mysmartkiln.data.local.entities.StageEntity
import com.example.mysmartkiln.ui.components.StageData
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository (Repositorio) para Programas de Horneado.
 * 
 * Este patrón separa la lógica de datos de la UI:
 * - La UI no accede directamente a los DAOs
 * - El Repository maneja toda la lógica de datos
 * - Facilita testing y cambios futuros (ej: agregar Firebase)
 * 
 * Ventajas:
 * - Abstracción: La UI no necesita saber de Room, DAOs, etc.
 * - Reutilización: Múltiples ViewModels pueden usar el mismo Repository
 * - Testing: Fácil de mockear para tests
 * - Flexibilidad: Puedes agregar cache, red, etc. sin cambiar la UI
 */
class ProgramRepository(
    private val programDao: ProgramDao,
    private val stageDao: StageDao
) {
    
    /**
     * Observa todos los programas.
     * 
     * Flow = Stream de datos que se actualiza automáticamente
     * La UI se refresca cuando hay cambios en la base de datos
     * 
     * Ejemplo de uso en ViewModel:
     * val programs = repository.allPrograms.collectAsState(initial = emptyList())
     */
    val allPrograms: Flow<List<ProgramEntity>> = programDao.getAllPrograms()
    
    /**
     * Obtiene un programa específico por su ID.
     * 
     * @param programId ID del programa
     * @return ProgramEntity o null si no existe
     */
    suspend fun getProgramById(programId: String): ProgramEntity? {
        return programDao.getProgramById(programId)
    }
    
    /**
     * Obtiene todas las etapas de un programa.
     * 
     * @param programId ID del programa
     * @return Flow con la lista de etapas ordenadas
     */
    fun getStagesByProgram(programId: String): Flow<List<StageEntity>> {
        return stageDao.getStagesByProgram(programId)
    }
    
    /**
     * Guarda un programa completo con sus etapas.
     * 
     * Esta función:
     * 1. Genera un UUID único para el programa
     * 2. Crea el ProgramEntity
     * 3. Guarda el programa en la BD
     * 4. Convierte los StageData a StageEntity
     * 5. Guarda todas las etapas
     * 6. Retorna el ID del programa creado
     * 
     * @param name Nombre del programa
     * @param stages Lista de StageData de la UI
     * @return ID del programa creado
     * 
     * Ejemplo de uso:
     * val programId = repository.saveProgram("Esmalte Gres", stagesList)
     */
    suspend fun saveProgram(name: String, stages: List<StageData>): String {
        // 1. Generar ID único
        val programId = UUID.randomUUID().toString()
        
        // 2. Crear entity del programa
        val program = ProgramEntity(
            id = programId,
            name = name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        // 3. Guardar programa
        programDao.insertProgram(program)
        
        // 4. Convertir StageData a StageEntity
        val stageEntities = stages.mapIndexed { index, stage ->
            StageEntity(
                id = UUID.randomUUID().toString(),
                programId = programId,
                stageNumber = index + 1,  // 1, 2, 3...
                temperature = stage.temperature,
                speed = stage.speed,
                maintenance = stage.maintenance
            )
        }
        
        // 5. Guardar todas las etapas
        stageDao.insertStages(stageEntities)
        
        // 6. Retornar el ID
        return programId
    }
    
    /**
     * Actualiza un programa existente (solo el nombre por ahora).
     * 
     * @param programId ID del programa a actualizar
     * @param newName Nuevo nombre del programa
     */
    suspend fun updateProgramName(programId: String, newName: String) {
        val program = programDao.getProgramById(programId)
        program?.let {
            val updated = it.copy(
                name = newName,
                updatedAt = System.currentTimeMillis()
            )
            programDao.updateProgram(updated)
        }
    }
    
    /**
     * Actualiza las etapas de un programa.
     * 
     * Esta función:
     * 1. Elimina todas las etapas anteriores
     * 2. Guarda las nuevas etapas
     * 
     * @param programId ID del programa
     * @param stages Nuevas etapas
     */
    suspend fun updateProgramStages(programId: String, stages: List<StageData>) {
        // 1. Eliminar etapas anteriores
        stageDao.deleteStagesByProgram(programId)
        
        // 2. Crear nuevas etapas
        val stageEntities = stages.mapIndexed { index, stage ->
            StageEntity(
                id = UUID.randomUUID().toString(),
                programId = programId,
                stageNumber = index + 1,
                temperature = stage.temperature,
                speed = stage.speed,
                maintenance = stage.maintenance
            )
        }
        
        // 3. Guardar nuevas etapas
        stageDao.insertStages(stageEntities)
        
        // 4. Actualizar timestamp del programa
        val program = programDao.getProgramById(programId)
        program?.let {
            programDao.updateProgram(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }
    
    /**
     * Elimina un programa (y todas sus etapas automáticamente).
     * 
     * Gracias al CASCADE, al eliminar el programa se eliminan
     * automáticamente todas sus etapas.
     * 
     * @param program Programa a eliminar
     */
    suspend fun deleteProgram(program: ProgramEntity) {
        programDao.deleteProgram(program)
    }
    
    /**
     * Elimina un programa por su ID.
     * 
     * @param programId ID del programa a eliminar
     */
    suspend fun deleteProgramById(programId: String) {
        programDao.deleteProgramById(programId)
    }
    
    /**
     * Elimina todos los programas (y todas sus etapas automáticamente).
     * 
     * Gracias al CASCADE, al eliminar todos los programas se eliminan
     * automáticamente todas las etapas.
     */
    suspend fun deleteAllPrograms() {
        programDao.deleteAllPrograms()
    }
    
    /**
     * Cuenta cuántas etapas tiene un programa.
     * 
     * @param programId ID del programa
     * @return Número de etapas
     */
    suspend fun getStageCount(programId: String): Int {
        return stageDao.getStageCount(programId)
    }
}









