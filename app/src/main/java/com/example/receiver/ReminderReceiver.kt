package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.SyncTaskApp
import com.example.data.local.AppDatabase
import com.example.model.RepeatRule
import com.example.model.SyncStatus
import com.example.model.TaskItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "onReceive action: $action")

        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return

        when (action) {
            ACTION_REMINDER -> {
                val taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Upcoming Task"
                val description = intent.getStringExtra(EXTRA_TASK_DESCRIPTION) ?: ""
                val scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, System.currentTimeMillis())
                val priority = intent.getStringExtra(EXTRA_PRIORITY) ?: "MEDIUM"

                showReminderNotification(context, taskId, taskTitle, description, scheduledTime, priority)
            }
            ACTION_COMPLETE_TASK -> {
                handleCompleteTask(context, taskId)
            }
            ACTION_SNOOZE_TASK -> {
                handleSnoozeTask(context, taskId)
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        taskId: String,
        taskTitle: String,
        description: String,
        scheduledTime: Long,
        priority: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(scheduledTime))

        // Open App Intent
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark Complete Action
        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_COMPLETE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + "_complete").hashCode(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 10m Action
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_TASK
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (taskId + "_snooze").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, SyncTaskApp.CHANNEL_REMINDERS_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ Reminder: $taskTitle")
            .setContentText("Starts in 15 mins at $formattedTime • Priority: $priority")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Task: $taskTitle\nTime: $formattedTime\nPriority: $priority" +
                        if (description.isNotBlank()) "\nNotes: $description" else ""
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Complete", completePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 10m", snoozePendingIntent)
            .build()

        notificationManager.notify(taskId.hashCode(), notification)
    }

    private fun handleCompleteTask(context: Context, taskId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.hashCode())

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val taskEntity = db.taskDao().getTaskById(taskId)
            if (taskEntity != null) {
                val updatedTask = taskEntity.copy(
                    isCompleted = true,
                    syncStatus = SyncStatus.PENDING_UPDATE,
                    lastModified = System.currentTimeMillis()
                )
                db.taskDao().update(updatedTask)

                // Reschedule next occurrence if recurring
                if (taskEntity.repeatRule != RepeatRule.NONE) {
                    val nextScheduledTime = calculateNextOccurrence(taskEntity.scheduledTime, taskEntity.repeatRule)
                    val nextDuration = taskEntity.endTime - taskEntity.scheduledTime
                    val nextTask = taskEntity.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        googleEventId = null,
                        scheduledTime = nextScheduledTime,
                        endTime = nextScheduledTime + nextDuration,
                        isCompleted = false,
                        syncStatus = SyncStatus.PENDING_INSERT,
                        lastModified = System.currentTimeMillis(),
                        snoozeUntil = null
                    )
                    db.taskDao().insert(nextTask)
                    com.example.alarm.ReminderScheduler.scheduleReminder(context, nextTask.toDomain())
                }
            }
        }
    }

    private fun handleSnoozeTask(context: Context, taskId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.hashCode())

        val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000L) // 10 minutes

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val taskEntity = db.taskDao().getTaskById(taskId)
            if (taskEntity != null) {
                val updated = taskEntity.copy(
                    snoozeUntil = snoozeTime,
                    lastModified = System.currentTimeMillis()
                )
                db.taskDao().update(updated)
                com.example.alarm.ReminderScheduler.scheduleReminder(context, updated.toDomain())
            }
        }
    }

    private fun calculateNextOccurrence(baseTime: Long, rule: RepeatRule): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = baseTime }
        when (rule) {
            RepeatRule.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            RepeatRule.WEEKDAYS -> {
                do {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                } while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                    calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
            }
            RepeatRule.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            RepeatRule.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            RepeatRule.NONE -> {}
        }
        return calendar.timeInMillis
    }

    companion object {
        const val TAG = "ReminderReceiver"
        const val ACTION_REMINDER = "com.example.synctask.ACTION_REMINDER"
        const val ACTION_COMPLETE_TASK = "com.example.synctask.ACTION_COMPLETE_TASK"
        const val ACTION_SNOOZE_TASK = "com.example.synctask.ACTION_SNOOZE_TASK"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_TASK_DESCRIPTION = "extra_task_description"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_PRIORITY = "extra_priority"
    }
}
