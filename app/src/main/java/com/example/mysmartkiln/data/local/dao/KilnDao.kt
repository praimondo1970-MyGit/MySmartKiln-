package com.example.mysmartkiln.data.local.dao

import androidx.room.*
import com.example.mysmartkiln.data.local.entity.KilnEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KilnDao {
    
    @Query("SELECT * FROM kilns")
    fun getAllKilns(): Flow<List<KilnEntity>>
    
    @Query("SELECT * FROM kilns WHERE userId = :userId")
    fun getKilnsByUserId(userId: String): Flow<List<KilnEntity>>
    
    @Query("SELECT * FROM kilns WHERE macAddress = :macAddress")
    suspend fun getKilnByMacAddress(macAddress: String): KilnEntity?
    
    @Query("SELECT * FROM kilns WHERE isPendingSync = 1")
    suspend fun getPendingSyncKilns(): List<KilnEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKiln(kiln: KilnEntity)
    
    @Update
    suspend fun updateKiln(kiln: KilnEntity)
    
    @Delete
    suspend fun deleteKiln(kiln: KilnEntity)
    
    @Query("UPDATE kilns SET isPendingSync = 0, lastSyncTimestamp = :timestamp WHERE macAddress = :macAddress")
    suspend fun markAsSynced(macAddress: String, timestamp: Long)
    
    @Query("UPDATE kilns SET isPendingSync = 1 WHERE macAddress = :macAddress")
    suspend fun markAsPendingSync(macAddress: String)
}












































