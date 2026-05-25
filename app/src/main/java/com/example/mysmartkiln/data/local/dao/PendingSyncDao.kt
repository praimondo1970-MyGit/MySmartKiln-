package com.example.mysmartkiln.data.local.dao

import androidx.room.*
import com.example.mysmartkiln.data.local.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {
    
    @Query("SELECT * FROM pending_sync ORDER BY timestamp ASC")
    suspend fun getAllPendingSyncs(): List<PendingSyncEntity>
    
    @Query("SELECT * FROM pending_sync WHERE type = :type ORDER BY timestamp ASC")
    suspend fun getPendingSyncsByType(type: String): List<PendingSyncEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSync(pendingSync: PendingSyncEntity)
    
    @Delete
    suspend fun deletePendingSync(pendingSync: PendingSyncEntity)
    
    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deletePendingSyncById(id: String)
    
    @Query("UPDATE pending_sync SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: String)
    
    @Query("DELETE FROM pending_sync WHERE retryCount >= :maxRetries")
    suspend fun deleteFailedSyncs(maxRetries: Int = 3)
}












































