package com.example.mysmartkiln.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kilns")
data class KilnEntity(
    @PrimaryKey val macAddress: String,  // MAC sin ":" para Firebase (ej: A4CF128856EA)
    val bleMacAddress: String,  // MAC con ":" para BLE (ej: A4:CF:12:88:56:EA)
    val name: String,
    val manufacturer: String,
    val volume: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncTimestamp: Long = 0L,
    val isPendingSync: Boolean = true
)
