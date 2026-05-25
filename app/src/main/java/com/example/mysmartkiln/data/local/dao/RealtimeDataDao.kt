package com.example.mysmartkiln.data.local.dao

import androidx.room.*
import com.example.mysmartkiln.data.local.entity.RealtimeDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RealtimeDataDao {
    
    @Query("SELECT * FROM realtime_data WHERE macAddress = :macAddress ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestRealtimeData(macAddress: String): RealtimeDataEntity?
    
    @Query("SELECT * FROM realtime_data WHERE macAddress = :macAddress ORDER BY timestamp DESC LIMIT 1")
    fun getLatestRealtimeDataFlow(macAddress: String): Flow<RealtimeDataEntity?>
    
    @Query("SELECT * FROM realtime_data WHERE isPendingSync = 1")
    suspend fun getPendingSyncRealtimeData(): List<RealtimeDataEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRealtimeData(data: RealtimeDataEntity)
    
    @Update
    suspend fun updateRealtimeData(data: RealtimeDataEntity)
    
    @Query("UPDATE realtime_data SET isPendingSync = 0 WHERE macAddress = :macAddress")
    suspend fun markAsSynced(macAddress: String)
    
    @Query("UPDATE realtime_data SET isPendingSync = 1 WHERE macAddress = :macAddress")
    suspend fun markAsPendingSync(macAddress: String)
    
    @Query("DELETE FROM realtime_data WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOldData(cutoffTimestamp: Long)
}












































