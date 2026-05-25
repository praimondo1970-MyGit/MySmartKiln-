package com.example.mysmartkiln.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey val id: String,
    val type: String, // "kiln", "realtime_data"
    val data: String, // JSON data
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)












































