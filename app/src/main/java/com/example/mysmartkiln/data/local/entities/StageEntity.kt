package com.example.mysmartkiln.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que representa una Etapa de un Programa en la base de datos.
 * 
 * Esta tabla almacena cada etapa individual de un programa de horneado:
 * - Temperatura objetivo
 * - Velocidad de rampa (°C/min)
 * - Tiempo de meseta (minutos)
 * 
 * Relación con ProgramEntity:
 * - Cada etapa pertenece a UN programa (Foreign Key)
 * - Si se elimina un programa, se eliminan sus etapas automáticamente (CASCADE)
 * - Un programa puede tener MUCHAS etapas
 */
@Entity(
    tableName = "stages",
    foreignKeys = [
        ForeignKey(
            entity = ProgramEntity::class,
            parentColumns = ["id"],           // Columna del programa (padre)
            childColumns = ["programId"],     // Columna de la etapa (hijo)
            onDelete = ForeignKey.CASCADE     // Si eliminas programa → elimina etapas
        )
    ],
    indices = [
        Index(value = ["programId"])  // Índice para búsquedas rápidas por programa
    ]
)
data class StageEntity(
    @PrimaryKey
    val id: String,  // UUID único de la etapa
    
    val programId: String,  // ID del programa al que pertenece (Foreign Key)
    
    val stageNumber: Int,  // Número de etapa (1, 2, 3, ...)
    
    val temperature: String,  // Temperatura objetivo (ej: "850")
    
    val speed: String,  // Velocidad de rampa en °C/min (ej: "2")
    
    val maintenance: String  // Tiempo de meseta en minutos (ej: "30")
)
























































