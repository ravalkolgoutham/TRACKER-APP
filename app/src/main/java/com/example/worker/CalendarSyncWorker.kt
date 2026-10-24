package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.SyncTaskApp

class CalendarSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting background Google Calendar sync via WorkManager...")
        return try {
            val repository = SyncTaskApp.instance.repository
            val success = repository.syncWithGoogleCalendar()
            if (success) {
                Log.d(TAG, "WorkManager sync finished successfully.")
                Result.success()
            } else {
                Log.d(TAG, "WorkManager sync offline or deferred, will retry.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "WorkManager sync exception: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "CalendarSyncWorker"
    }
}
