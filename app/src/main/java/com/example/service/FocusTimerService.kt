package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.SyncTaskApp
import com.example.data.local.AppDatabase
import com.example.data.local.FocusSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class FocusTimerService : Service() {

    enum class TimerStatus {
        IDLE, RUNNING, PAUSED, COMPLETED
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var timerJob: Job? = null
    private var endTimestamp: Long = 0L
    private var pausedRemainingMillis: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 25)
                val taskId = intent.getStringExtra(EXTRA_TASK_ID)
                val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE)
                val tag = intent.getStringExtra(EXTRA_TAG) ?: "Deep Work"
                startTimer(durationMinutes, taskId, taskTitle, tag)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
        }
        return START_NOT_STICKY
    }

    private fun startTimer(
        durationMinutes: Int,
        taskId: String?,
        taskTitle: String?,
        tag: String
    ) {
        timerJob?.cancel()
        _currentTaskId.value = taskId
        _currentTaskTitle.value = taskTitle ?: tag
        _totalSeconds.value = durationMinutes * 60
        _remainingSeconds.value = durationMinutes * 60
        _currentTag.value = tag
        _timerStatus.value = TimerStatus.RUNNING

        val durationMillis = durationMinutes * 60 * 1000L
        endTimestamp = System.currentTimeMillis() + durationMillis

        startForeground(NOTIFICATION_ID, buildNotification())

        timerJob = serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val remainingMillis = (endTimestamp - now).coerceAtLeast(0L)
                val secondsLeft = (remainingMillis / 1000).toInt()
                _remainingSeconds.value = secondsLeft

                updateNotification()

                if (secondsLeft <= 0) {
                    onTimerComplete()
                    break
                }
                delay(1000)
            }
        }
    }

    private fun pauseTimer() {
        if (_timerStatus.value == TimerStatus.RUNNING) {
            timerJob?.cancel()
            val now = System.currentTimeMillis()
            pausedRemainingMillis = (endTimestamp - now).coerceAtLeast(0L)
            _remainingSeconds.value = (pausedRemainingMillis / 1000).toInt()
            _timerStatus.value = TimerStatus.PAUSED
            updateNotification()
        }
    }

    private fun resumeTimer() {
        if (_timerStatus.value == TimerStatus.PAUSED) {
            endTimestamp = System.currentTimeMillis() + pausedRemainingMillis
            _timerStatus.value = TimerStatus.RUNNING

            timerJob = serviceScope.launch {
                while (isActive) {
                    val now = System.currentTimeMillis()
                    val remainingMillis = (endTimestamp - now).coerceAtLeast(0L)
                    val secondsLeft = (remainingMillis / 1000).toInt()
                    _remainingSeconds.value = secondsLeft

                    updateNotification()

                    if (secondsLeft <= 0) {
                        onTimerComplete()
                        break
                    }
                    delay(1000)
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _timerStatus.value = TimerStatus.IDLE
        _remainingSeconds.value = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onTimerComplete() {
        _timerStatus.value = TimerStatus.COMPLETED
        _remainingSeconds.value = 0

        // Persist session to Room Database
        val taskId = _currentTaskId.value
        val taskTitle = _currentTaskTitle.value
        val durationMins = _totalSeconds.value / 60
        val tag = _currentTag.value

        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.focusSessionDao().insertSession(
                    FocusSessionEntity(
                        id = UUID.randomUUID().toString(),
                        taskId = taskId,
                        taskTitle = taskTitle,
                        durationMinutes = durationMins,
                        completedAt = System.currentTimeMillis(),
                        tag = tag
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error saving focus session: ${e.message}")
            }
        }

        // Show completion notification
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val completionNotification = NotificationCompat.Builder(this, SyncTaskApp.CHANNEL_FOCUS_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("🎉 Focus Session Completed!")
            .setContentText("Great work! You finished $durationMins mins of '$taskTitle'.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID + 1, completionNotification)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): android.app.Notification {
        val title = "🎯 Focus: ${_currentTaskTitle.value}"
        val remaining = _remainingSeconds.value
        val mins = remaining / 60
        val secs = remaining % 60
        val timeString = String.format("%02d:%02d", mins, secs)
        val statusText = if (_timerStatus.value == TimerStatus.PAUSED) "Paused" else "Running"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, SyncTaskApp.CHANNEL_FOCUS_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText("$timeString remaining • $statusText")
            .setProgress(_totalSeconds.value, _totalSeconds.value - remaining, false)
            .setOngoing(_timerStatus.value == TimerStatus.RUNNING)
            .setContentIntent(openAppPendingIntent)

        // Action Buttons
        if (_timerStatus.value == TimerStatus.RUNNING) {
            val pauseIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_PAUSE }
            val pausePending = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", pausePending)
        } else if (_timerStatus.value == TimerStatus.PAUSED) {
            val resumeIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_RESUME }
            val resumePending = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            builder.addAction(android.R.drawable.ic_media_play, "Resume", resumePending)
        }

        val stopIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 3, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "End", stopPending)

        return builder.build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
    }

    companion object {
        private const val TAG = "FocusTimerService"
        private const val NOTIFICATION_ID = 2001

        const val ACTION_START = "com.example.synctask.FOCUS_START"
        const val ACTION_PAUSE = "com.example.synctask.FOCUS_PAUSE"
        const val ACTION_RESUME = "com.example.synctask.FOCUS_RESUME"
        const val ACTION_STOP = "com.example.synctask.FOCUS_STOP"

        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TAG = "extra_tag"

        // Observable StateFlows for UI binding
        private val _timerStatus = MutableStateFlow(TimerStatus.IDLE)
        val timerStatus: StateFlow<TimerStatus> = _timerStatus.asStateFlow()

        private val _remainingSeconds = MutableStateFlow(25 * 60)
        val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

        private val _totalSeconds = MutableStateFlow(25 * 60)
        val totalSeconds: StateFlow<Int> = _totalSeconds.asStateFlow()

        private val _currentTaskId = MutableStateFlow<String?>(null)
        val currentTaskId: StateFlow<String?> = _currentTaskId.asStateFlow()

        private val _currentTaskTitle = MutableStateFlow("Deep Work")
        val currentTaskTitle: StateFlow<String> = _currentTaskTitle.asStateFlow()

        private val _currentTag = MutableStateFlow("General")
        val currentTag: StateFlow<String> = _currentTag.asStateFlow()

        fun start(context: Context, minutes: Int, taskId: String? = null, taskTitle: String? = null, tag: String = "Deep Work") {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_MINUTES, minutes)
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TITLE, taskTitle)
                putExtra(EXTRA_TAG, tag)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply { action = ACTION_PAUSE }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply { action = ACTION_RESUME }
            context.startService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }
    }
}
