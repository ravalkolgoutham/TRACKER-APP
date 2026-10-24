package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.TaskItem
import com.example.receiver.ReminderReceiver

object ReminderScheduler {

    private const val TAG = "ReminderScheduler"

    fun scheduleReminder(context: Context, task: TaskItem) {
        if (task.isCompleted) return

        val reminderOffset = task.reminderMinutesBefore * 60 * 1000L
        val triggerAtMillis = task.snoozeUntil ?: (task.scheduledTime - reminderOffset)

        // Don't schedule if trigger time is in the past
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.d(TAG, "Trigger time is in the past for task: ${task.title}, skipping alarm.")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_TASK_ID, task.id)
            putExtra(ReminderReceiver.EXTRA_TASK_TITLE, task.title)
            putExtra(ReminderReceiver.EXTRA_TASK_DESCRIPTION, task.description)
            putExtra(ReminderReceiver.EXTRA_SCHEDULED_TIME, task.scheduledTime)
            putExtra(ReminderReceiver.EXTRA_PRIORITY, task.priority.name)
        }

        val requestCode = task.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled reminder for '${task.title}' at $triggerAtMillis (15m before event)")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule exact alarm: ${e.message}")
        }
    }

    fun cancelReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
        }
        val requestCode = taskId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled reminder for task ID: $taskId")
        }
    }
}
