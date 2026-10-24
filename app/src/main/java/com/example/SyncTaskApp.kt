package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.AppDatabase
import com.example.data.repository.TaskRepository
import com.example.worker.CalendarSyncWorker
import java.util.concurrent.TimeUnit

class SyncTaskApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize local Room Database
        database = AppDatabase.getDatabase(this)
        repository = TaskRepository(
            this,
            database.taskDao(),
            database.focusSessionDao(),
            database.goalDao(),
            database.userStatsDao(),
            database.rewardDao()
        )

        // Setup notification channels
        createNotificationChannels()

        // Setup WorkManager periodic background sync
        setupBackgroundSync()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 1. Reminders Channel (High importance, vibration, sound)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Task & Event Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies 15 minutes before scheduled tasks and events"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                setSound(defaultSoundUri, audioAttributes)
            }

            // 2. Focus Timer Channel (Foreground Service)
            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS_ID,
                "Focus Session Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active countdown and controls for your current focus session"
                setShowBadge(false)
            }

            // 3. Background Sync Channel
            val syncChannel = NotificationChannel(
                CHANNEL_SYNC_ID,
                "Google Calendar Sync",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Status of background synchronization and conflict resolution"
            }

            notificationManager.createNotificationChannels(
                listOf(reminderChannel, focusChannel, syncChannel)
            )
        }
    }

    private fun setupBackgroundSync() {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
                1, TimeUnit.HOURS,
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "GoogleCalendarSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )
        } catch (e: Throwable) {
            android.util.Log.w("SyncTaskApp", "WorkManager initialization skipped: ${e.message}")
        }
    }

    companion object {
        const val CHANNEL_REMINDERS_ID = "channel_reminders"
        const val CHANNEL_FOCUS_ID = "channel_focus"
        const val CHANNEL_SYNC_ID = "channel_sync"

        lateinit var instance: SyncTaskApp
            private set
    }
}
