package com.example.mysmartkiln.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mysmartkiln.data.local.dao.ProgramDao
import com.example.mysmartkiln.data.local.dao.StageDao
import com.example.mysmartkiln.data.local.entities.ProgramEntity
import com.example.mysmartkiln.data.local.entities.StageEntity

/**
 * Base de datos principal de la aplicación MySmartKiln.
 * 
 * Esta clase es el punto de acceso central a la base de datos Room.
 * Coordina todas las entidades y DAOs.
 * 
 * @Database = Marca esta clase como base de datos Room
 * entities = Lista de todas las tablas (entidades)
 * version = Número de versión de la BD (incrementa cuando cambies el esquema)
 * exportSchema = false para simplificar (en producción se recomienda true)
 */
@Database(
    entities = [
        ProgramEntity::class,  // Tabla "programs"
        StageEntity::class     // Tabla "stages"
    ],
    version = 1,
    exportSchema = false
)
abstract class BakingDatabase : RoomDatabase() {
    
    /**
     * Proporciona acceso al DAO de programas.
     * Room genera automáticamente la implementación.
     */
    abstract fun programDao(): ProgramDao
    
    /**
     * Proporciona acceso al DAO de etapas.
     * Room genera automáticamente la implementación.
     */
    abstract fun stageDao(): StageDao
    
    companion object {
        /**
         * Instancia única de la base de datos (Singleton pattern).
         * 
         * @Volatile = Asegura que el valor sea visible para todos los threads
         * INSTANCE = La única instancia de la base de datos en toda la app
         */
        @Volatile
        private var INSTANCE: BakingDatabase? = null
        
        /**
         * Obtiene la instancia de la base de datos.
         * Si no existe, la crea. Si ya existe, la reutiliza.
         * 
         * Patrón Singleton: Solo una instancia en toda la app
         * synchronized = Solo un thread puede crear la BD a la vez
         * 
         * @param context Contexto de Android (necesario para crear la BD)
         * @return Instancia única de BakingDatabase
         * 
         * Ejemplo de uso:
         * val database = BakingDatabase.getDatabase(context)
         * val programDao = database.programDao()
         */
        fun getDatabase(context: Context): BakingDatabase {
            // Si INSTANCE no es null, retornarla
            // Si es null, entrar al bloque synchronized
            return INSTANCE ?: synchronized(this) {
                // Double-check: verificar de nuevo dentro del bloque
                val instance = INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,  // Usar contexto de aplicación (no Activity)
                    BakingDatabase::class.java,
                    "baking_database"  // Nombre del archivo de BD en el dispositivo
                )
                    // En caso de cambios en el esquema sin migración,
                    // destruir la BD y crearla de nuevo (solo para desarrollo)
                    .fallbackToDestructiveMigration()
                    .build()
                
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Destruye la instancia de la base de datos.
         * Útil para testing o para forzar recreación.
         * 
         * NO usar en producción normalmente.
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
























































