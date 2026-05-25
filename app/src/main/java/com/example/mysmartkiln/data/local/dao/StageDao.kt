package com.example.mysmartkiln.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mysmartkiln.data.local.entities.StageEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para operaciones con Etapas.
 * 
 * Define todas las operaciones que puedes hacer con la tabla "stages":
 * - Obtener todas las etapas de un programa
 * - Insertar una etapa
 * - Insertar múltiples etapas a la vez
 * - Eliminar todas las etapas de un programa
 */
@Dao
interface StageDao {
    
    /**
     * Obtiene todas las etapas de un programa específico,
     * ordenadas por número de etapa (1, 2, 3...).
     * 
     * Flow<List<>> = Lista observable que se actualiza automáticamente
     * 
     * Ejemplo de uso:
     * stageDao.getStagesByProgram("prog_001").collect { stages ->
     *     // stages contiene todas las etapas del programa
     *     // ordenadas: Etapa 1, Etapa 2, Etapa 3...
     * }
     */
    @Query("SELECT * FROM stages WHERE programId = :programId ORDER BY stageNumber ASC")
    fun getStagesByProgram(programId: String): Flow<List<StageEntity>>
    
    /**
     * Inserta una nueva etapa en la base de datos.
     * 
     * OnConflictStrategy.REPLACE = Si el ID ya existe, reemplaza
     * 
     * Ejemplo de uso:
     * val newStage = StageEntity(
     *     id = UUID.randomUUID().toString(),
     *     programId = "prog_001",
     *     stageNumber = 1,
     *     temperature = "850",
     *     speed = "2",
     *     maintenance = "30"
     * )
     * stageDao.insertStage(newStage)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStage(stage: StageEntity)
    
    /**
     * Inserta múltiples etapas a la vez (más eficiente).
     * 
     * Útil al crear un programa completo con todas sus etapas.
     * 
     * Ejemplo de uso:
     * val stages = listOf(
     *     StageEntity(...),  // Etapa 1
     *     StageEntity(...),  // Etapa 2
     *     StageEntity(...)   // Etapa 3
     * )
     * stageDao.insertStages(stages)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStages(stages: List<StageEntity>)
    
    /**
     * Elimina todas las etapas de un programa específico.
     * 
     * Útil cuando quieres reemplazar todas las etapas de un programa.
     * 
     * NOTA: No es necesario llamar esto al eliminar un programa,
     * porque el CASCADE lo hace automáticamente.
     * 
     * Ejemplo de uso:
     * stageDao.deleteStagesByProgram("prog_001")
     */
    @Query("DELETE FROM stages WHERE programId = :programId")
    suspend fun deleteStagesByProgram(programId: String)
    
    /**
     * Cuenta cuántas etapas tiene un programa.
     * 
     * Útil para validaciones o mostrar información.
     * 
     * Ejemplo de uso:
     * val count = stageDao.getStageCount("prog_001")
     * // count = 3 (si el programa tiene 3 etapas)
     */
    @Query("SELECT COUNT(*) FROM stages WHERE programId = :programId")
    suspend fun getStageCount(programId: String): Int
}
























































