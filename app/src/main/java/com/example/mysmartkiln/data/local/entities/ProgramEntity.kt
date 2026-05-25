package com.example.mysmartkiln.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa un Programa de Horneado en la base de datos.
 * 
 * Esta tabla almacena la información básica de cada programa:
 * - ID único
 * - Nombre del programa
 * - Timestamps de creación y actualización
 * 
 * Las etapas del programa se almacenan en una tabla separada (StageEntity)
 * con una relación 1-a-muchos.
 */
@Entity(tableName = "programs")
data class ProgramEntity(
    @PrimaryKey
    val id: String,  // UUID generado al crear el programa
    
    val name: String,  // Nombre del programa (ej: "Esmalte para Gres")
    
    val createdAt: Long = System.currentTimeMillis(),  // Timestamp de creación
    
    val updatedAt: Long = System.currentTimeMillis()   // Timestamp de última modificación
)
























































