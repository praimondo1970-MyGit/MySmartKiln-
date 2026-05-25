package com.example.mysmartkiln.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "realtime_data")
data class RealtimeDataEntity(
    @PrimaryKey val macAddress: String,
    val temperature: Float,
    val status: String,
    val timestamp: Long,
    val isPendingSync: Boolean = false
)












































