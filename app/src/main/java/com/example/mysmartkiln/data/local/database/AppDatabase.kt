package com.example.mysmartkiln.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.mysmartkiln.data.local.dao.KilnDao
import com.example.mysmartkiln.data.local.dao.PendingSyncDao
import com.example.mysmartkiln.data.local.dao.RealtimeDataDao
import com.example.mysmartkiln.data.local.entity.KilnEntity
import com.example.mysmartkiln.data.local.entity.PendingSyncEntity
import com.example.mysmartkiln.data.local.entity.RealtimeDataEntity

@Database(
    entities = [
        KilnEntity::class,
        RealtimeDataEntity::class,
        PendingSyncEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun kilnDao(): KilnDao
    abstract fun realtimeDataDao(): RealtimeDataDao
    abstract fun pendingSyncDao(): PendingSyncDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smartkiln_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
