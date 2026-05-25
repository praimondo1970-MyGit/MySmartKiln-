package com.example.mysmartkiln.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mysmartkiln.data.local.database.BakingDatabase
import com.example.mysmartkiln.data.repository.ProgramRepository

/**
 * Factory para crear ViewModels con dependencias.
 * 
 * Room y Repository necesitan ser inyectados en el ViewModel,
 * pero ViewModels no pueden tener constructores con parámetros
 * directamente. Esta Factory resuelve ese problema.
 * 
 * @param context Contexto de Android para crear la base de datos
 */
class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Crear la base de datos
        val database = BakingDatabase.getDatabase(context)
        
        // Crear el repository con los DAOs
        val repository = ProgramRepository(
            programDao = database.programDao(),
            stageDao = database.stageDao()
        )
        
        // Crear el ViewModel correspondiente
        return when {
            modelClass.isAssignableFrom(CreateProgramViewModel::class.java) -> {
                CreateProgramViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ProgramsListViewModel::class.java) -> {
                ProgramsListViewModel(repository) as T
            }
            modelClass.isAssignableFrom(KilnDetailViewModel::class.java) -> {
                KilnDetailViewModel(repository, context.applicationContext) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}







