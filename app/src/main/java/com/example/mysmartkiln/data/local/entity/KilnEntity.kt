package com.example.mysmartkiln.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kilns")
data class KilnEntity(
    @PrimaryKey val macAddress: String,  // MAC sin ":" para Firebase (ej: A4CF128856EA)
    val bleMacAddress: String,  // MAC con ":" (legado de Room; mismo dispositivo que macAddress)
    val name: String,
    val manufacturer: String,
    val volume: String,
    val userId: String,
    /**
     * "KILN" | "DATALOGGER". Vacío en filas antiguas → [DeviceKind] usa heurística.
     * Para DATALOGGER, [manufacturer] suele ser el tipo de termocupla (K/S).
     */
    val deviceType: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncTimestamp: Long = 0L,
    val isPendingSync: Boolean = true
)
