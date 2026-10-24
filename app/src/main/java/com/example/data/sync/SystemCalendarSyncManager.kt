package com.example.data.sync

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.SyncStatus
import com.example.model.TaskItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID

private const val TAG = "CalendarSyncManager"

data class CalendarAccountInfo(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String,
    val isSamsung: Boolean,
    val isGoogle: Boolean,
    val isPrimary: Boolean
)

class SystemCalendarSyncManager(private val context: Context) {

    private val contentResolver = context.contentResolver
    private var contentObserver: ContentObserver? = null
    private var debounceJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun hasCalendarPermissions(): Boolean {
        val readGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val writeGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        return readGranted && writeGranted
    }

    /**
     * Start observing CalendarContract.Events in real-time.
     * Triggers automatically whenever Samsung Calendar or Google Calendar alters events.
     */
    fun startRealtimeObserver(onCalendarChanged: () -> Unit) {
        if (!hasCalendarPermissions()) {
            Log.d(TAG, "Cannot start calendar observer: permissions not granted")
            return
        }

        stopRealtimeObserver()

        contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                super.onChange(selfChange, uri)
                Log.d(TAG, "Real-time calendar change detected on $uri")
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(800) // Debounce rapid consecutive modifications
                    onCalendarChanged()
                }
            }
        }

        try {
            contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                contentObserver!!
            )
            Log.d(TAG, "Real-time CalendarContract observer registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering calendar observer", e)
        }
    }

    fun stopRealtimeObserver() {
        contentObserver?.let {
            try {
                contentResolver.unregisterContentObserver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering observer", e)
            }
        }
        contentObserver = null
    }

    /**
     * Finds active Samsung & Google Calendars on device.
     */
    fun getAvailableCalendars(): List<CalendarAccountInfo> {
        if (!hasCalendarPermissions()) return emptyList()

        val list = mutableListOf<CalendarAccountInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY
        )

        try {
            contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accNameIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val accTypeIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)
                val primaryIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx) ?: "Default Calendar"
                    val accName = cursor.getString(accNameIdx) ?: ""
                    val accType = cursor.getString(accTypeIdx) ?: ""
                    val isPrimary = if (primaryIdx != -1) cursor.getInt(primaryIdx) == 1 else false

                    val isSamsung = accType.contains("sec", ignoreCase = true) ||
                            accType.contains("samsung", ignoreCase = true) ||
                            accName.contains("samsung", ignoreCase = true)
                    val isGoogle = accType.equals("com.google", ignoreCase = true)

                    list.add(
                        CalendarAccountInfo(
                            id = id,
                            name = name,
                            accountName = accName,
                            accountType = accType,
                            isSamsung = isSamsung,
                            isGoogle = isGoogle,
                            isPrimary = isPrimary
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading calendars", e)
        }
        return list
    }

    /**
     * Resolves the best primary writable calendar ID (Samsung or Google).
     */
    private fun getPrimaryCalendarId(): Long? {
        val calendars = getAvailableCalendars()
        if (calendars.isEmpty()) return null
        return calendars.firstOrNull { it.isPrimary }?.id
            ?: calendars.firstOrNull { it.isGoogle }?.id
            ?: calendars.firstOrNull { it.isSamsung }?.id
            ?: calendars.firstOrNull()?.id
    }

    /**
     * Reads events across device calendars (Samsung & Google) for synchronization into Room.
     */
    fun readEventsFromCalendar(daysPast: Int = 30, daysFuture: Int = 90): List<TaskItem> {
        if (!hasCalendarPermissions()) return emptyList()

        val results = mutableListOf<TaskItem>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysPast)
        val startMillis = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, daysPast + daysFuture)
        val endMillis = cal.timeInMillis

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.STATUS
        )

        val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?) AND (${CalendarContract.Events.DELETED} != 1)"
        val selectionArgs = arrayOf(startMillis.toString(), endMillis.toString())

        try {
            contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val descIdx = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIdx = cursor.getColumnIndex(CalendarContract.Events.DTEND)

                while (cursor.moveToNext()) {
                    val rawId = cursor.getLong(idIdx)
                    val rawTitle = cursor.getString(titleIdx) ?: "Untitled Event"
                    val desc = cursor.getString(descIdx) ?: ""
                    val start = cursor.getLong(startIdx)
                    val end = if (endIdx != -1 && !cursor.isNull(endIdx)) {
                        cursor.getLong(endIdx)
                    } else {
                        start + 3600000L
                    }

                    val isDone = rawTitle.startsWith("✓ ") || desc.contains("[Status: Completed]")
                    val cleanTitle = rawTitle.removePrefix("✓ ").trim()

                    results.add(
                        TaskItem(
                            id = UUID.randomUUID().toString(),
                            googleEventId = "cal_$rawId",
                            title = cleanTitle,
                            description = desc,
                            scheduledTime = start,
                            endTime = end,
                            isCompleted = isDone,
                            priority = Priority.MEDIUM,
                            category = if (desc.contains("Focus")) "Focus" else "Work",
                            repeatRule = RepeatRule.NONE,
                            reminderMinutesBefore = 15,
                            syncStatus = SyncStatus.SYNCED,
                            lastModified = System.currentTimeMillis()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading events from calendar", e)
        }
        return results
    }

    /**
     * Push a task created or modified in the app to Samsung/Google calendar.
     */
    fun pushTaskToDeviceCalendar(task: TaskItem): String? {
        if (!hasCalendarPermissions()) return null
        val calendarId = getPrimaryCalendarId() ?: return null

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, if (task.isCompleted) "✓ ${task.title}" else task.title)
            put(CalendarContract.Events.DESCRIPTION, "${task.description}\n[Category: ${task.category} | Priority: ${task.priority.name}]")
            put(CalendarContract.Events.DTSTART, task.scheduledTime)
            put(CalendarContract.Events.DTEND, task.endTime)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }

        try {
            val eventIdNum = task.googleEventId?.removePrefix("cal_")?.toLongOrNull()
            if (eventIdNum != null) {
                // Update existing
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventIdNum)
                val updatedRows = contentResolver.update(uri, values, null, null)
                if (updatedRows > 0) {
                    return task.googleEventId
                }
            }

            // Insert new event
            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val newId = uri?.lastPathSegment
            if (newId != null) {
                // Add default 15-minute reminder in calendar
                addCalendarReminder(newId.toLong(), task.reminderMinutesBefore)
                return "cal_$newId"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing task to calendar", e)
        }
        return null
    }

    private fun addCalendarReminder(eventId: Long, minutesBefore: Int) {
        try {
            val values = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                put(CalendarContract.Reminders.MINUTES, minutesBefore)
            }
            contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding reminder to event $eventId", e)
        }
    }

    fun deleteTaskFromDeviceCalendar(googleEventId: String?): Boolean {
        if (!hasCalendarPermissions() || googleEventId == null) return false
        val eventIdNum = googleEventId.removePrefix("cal_").toLongOrNull() ?: return false
        try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventIdNum)
            val deleted = contentResolver.delete(uri, null, null)
            return deleted > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting calendar event $eventIdNum", e)
        }
        return false
    }
}
