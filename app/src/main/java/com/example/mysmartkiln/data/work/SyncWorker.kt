package com.example.mysmartkiln.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mysmartkiln.data.firebase.FirebaseRealtimeManager
import com.example.mysmartkiln.data.local.KilnFiringStore
import com.example.mysmartkiln.data.local.KilnFiringCloudSync
import com.example.mysmartkiln.data.repository.KilnRepository
import com.example.mysmartkiln.utils.UserIdUtils
import com.google.firebase.auth.FirebaseAuth

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val firebaseManager = FirebaseRealtimeManager()
    private val kilnRepository = KilnRepository(applicationContext, firebaseManager)

    override suspend fun doWork(): Result {
        return try {
            kilnRepository.syncPendingData()
            val email = FirebaseAuth.getInstance().currentUser?.email
            val uid = UserIdUtils.getCurrentUserId(email).takeIf {
                it.isNotBlank() && it != "anonymous_user" && it != "test_user_123"
            }
            KilnFiringCloudSync.syncPending(applicationContext, uid, firebaseManager)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
