package com.example.mysmartkiln.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager
import com.example.mysmartkiln.data.repository.KilnRepository

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val firebaseManager = FirebaseRealtimeManager()
    private val kilnRepository = KilnRepository(applicationContext, firebaseManager)

    override suspend fun doWork(): Result {
        return try {
            kilnRepository.syncPendingData()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
