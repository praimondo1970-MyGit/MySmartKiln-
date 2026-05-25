package com.example.mysmartkiln.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.mysmartkiln.data.local.entities.ProgramEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object) para operaciones con Programas.
 * 
 * Define todas las operaciones que puedes hacer con la tabla "programs":
 * - Obtener todos los programas
 * - Obtener un programa por ID
 * - Insertar un programa nuevo
 * - Actualizar un programa existente
 * - Eliminar un programa
 * 
 * Flow<> = Observable: La UI se actualiza automáticamente cuando cambian los datos
 * suspend = Función asíncrona: No bloquea la UI mientras se ejecuta
 */
@Dao
interface ProgramDao {
    
    /**
     * Obtiene todos los programas ordenados por fecha de actualización (más recientes primero).
     * 
     * Flow<List<>> = Lista que se actualiza automáticamente
     * La UI observa este Flow y se refresca cuando hay cambios
     * 
     * Ejemplo de uso:
     * programDao.getAllPrograms().collect { programs ->
     *     // programs contiene la lista actualizada
     * }
     */
    @Query("SELECT * FROM programs ORDER BY updatedAt DESC")
    fun getAllPrograms(): Flow<List<ProgramEntity>>
    
    /**
     * Obtiene un programa específico por su ID.
     * 
     * suspend = Función asíncrona, debes llamarla desde una coroutine
     * Retorna null si no existe el programa
     * 
     * Ejemplo de uso:
     * val program = programDao.getProgramById("prog_001")
     */
    @Query("SELECT * FROM programs WHERE id = :programId")
    suspend fun getProgramById(programId: String): ProgramEntity?
    
    /**
     * Inserta un nuevo programa en la base de datos.
     * 
     * OnConflictStrategy.REPLACE = Si el ID ya existe, reemplaza el registro
     * (útil para actualizar también)
     * 
     * Ejemplo de uso:
     * val newProgram = ProgramEntity(
     *     id = UUID.randomUUID().toString(),
     *     name = "Mi Programa"
     * )
     * programDao.insertProgram(newProgram)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: ProgramEntity)
    
    /**
     * Actualiza un programa existente.
     * Room identifica el programa por su @PrimaryKey (id)
     * 
     * Ejemplo de uso:
     * val updated = existingProgram.copy(
     *     name = "Nuevo Nombre",
     *     updatedAt = System.currentTimeMillis()
     * )
     * programDao.updateProgram(updated)
     */
    @Update
    suspend fun updateProgram(program: ProgramEntity)
    
    /**
     * Elimina un programa de la base de datos.
     * 
     * IMPORTANTE: Gracias al ForeignKey CASCADE, al eliminar un programa
     * se eliminan automáticamente todas sus etapas.
     * 
     * Ejemplo de uso:
     * programDao.deleteProgram(program)
     */
    @Delete
    suspend fun deleteProgram(program: ProgramEntity)
    
    /**
     * Elimina un programa por su ID (alternativa).
     * 
     * Ejemplo de uso:
     * programDao.deleteProgramById("prog_001")
     */
    @Query("DELETE FROM programs WHERE id = :programId")
    suspend fun deleteProgramById(programId: String)
    
    /**
     * Elimina todos los programas de la base de datos.
     * 
     * IMPORTANTE: Gracias al ForeignKey CASCADE, al eliminar todos los programas
     * se eliminan automáticamente todas sus etapas.
     * 
     * Ejemplo de uso:
     * programDao.deleteAllPrograms()
     */
    @Query("DELETE FROM programs")
    suspend fun deleteAllPrograms()
}









