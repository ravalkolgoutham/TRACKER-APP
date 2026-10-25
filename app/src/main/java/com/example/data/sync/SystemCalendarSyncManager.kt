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
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "CalendarSyncManager"

data class CalendarAccountInfo(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String,
    val isSamsung: Boolean,
    val isGoogle: Boolean,
    val isPrimary: Boolean,
    val isWritable: Boolean
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
                    delay(1200)
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
     * Finds active user-writable Samsung & Google Calendars on device,
     * excluding read-only Holiday, Observance, Birthday, and Contact calendars.
     */
    fun getAvailableCalendars(): List<CalendarAccountInfo> {
        if (!hasCalendarPermissions()) return emptyList()

        val list = mutableListOf<CalendarAccountInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
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
                val accessIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx) ?: "Default Calendar"
                    val accName = cursor.getString(accNameIdx) ?: ""
                    val accType = cursor.getString(accTypeIdx) ?: ""
                    val isPrimary = if (primaryIdx != -1) cursor.getInt(primaryIdx) == 1 else false
                    val accessLevel = if (accessIdx != -1) cursor.getInt(accessIdx) else 0

                    val lowerName = name.lowercase(Locale.getDefault())
                    val lowerAcc = accName.lowercase(Locale.getDefault())
                    val isHolidayOrSystemFeed = lowerName.contains("holiday") ||
                            lowerName.contains("observance") ||
                            lowerName.contains("birthday") ||
                            lowerName.contains("contact") ||
                            lowerAcc.contains("holiday") ||
                            lowerAcc.contains("#contacts") ||
                            lowerAcc.contains("group.v.calendar.google.com")

                    if (isHolidayOrSystemFeed) {
                        continue
                    }

                    val isWritable = accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
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
                            isPrimary = isPrimary,
                            isWritable = isWritable
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading calendars", e)
        }
        return list
    }

    private fun getPrimaryCalendarId(): Long? {
        val calendars = getAvailableCalendars().filter { it.isWritable }
        if (calendars.isEmpty()) return null
        return calendars.firstOrNull { it.isPrimary }?.id
            ?: calendars.firstOrNull { it.isGoogle }?.id
            ?: calendars.firstOrNull { it.isSamsung }?.id
            ?: calendars.firstOrNull()?.id
    }

    /**
     * Helper to identify Holiday / Observance / auto-generated system calendar events
     * so they never pollute the user's personal task list.
     */
    fun isHolidayOrObservanceEvent(title: String, description: String): Boolean {
        val lowerDesc = description.lowercase(Locale.getDefault())
        val lowerTitle = title.lowercase(Locale.getDefault())
        return lowerDesc.contains("to hide observances") ||
                lowerDesc.contains("observance") ||
                lowerDesc.contains("public holiday") ||
                lowerDesc.contains("regional holiday") ||
                lowerDesc.contains("government holiday") ||
                lowerTitle.contains("jayanti") && lowerDesc.contains("google calendar")
    }

    /**
     * Reads user events from primary writable calendars only, excluding all
     * Holiday/Observance feeds and deduplicating by (title, scheduledTime).
     */
    fun readEventsFromCalendar(daysPast: Int = 7, daysFuture: Int = 30): List<TaskItem> {
        if (!hasCalendarPermissions()) return emptyList()

        val writableCalendarIds = getAvailableCalendars()
            .filter { it.isWritable }
            .map { it.id }
            .toSet()
        if (writableCalendarIds.isEmpty()) return emptyList()

        val results = mutableListOf<TaskItem>()
        val seenKeys = mutableSetOf<String>()

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysPast)
        val startMillis = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, daysPast + daysFuture)
        val endMillis = cal.timeInMillis

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.CALENDAR_ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY
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
                val calIdIdx = cursor.getColumnIndex(CalendarContract.Events.CALENDAR_ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val descIdx = cursor.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val endIdx = cursor.getColumnIndex(CalendarContract.Events.DTEND)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)

                while (cursor.moveToNext()) {
                    val calId = if (calIdIdx != -1) cursor.getLong(calIdIdx) else -1L
                    if (calId !in writableCalendarIds) continue

                    val rawId = cursor.getLong(idIdx)
                    val rawTitle = cursor.getString(titleIdx)?.trim() ?: continue
                    if (rawTitle.isBlank()) continue

                    val desc = cursor.getString(descIdx) ?: ""
                    val isAllDay = if (allDayIdx != -1) cursor.getInt(allDayIdx) == 1 else false

                    // Skip Holiday / Observance events or all-day public holidays not created by SyncTask
                    if (isHolidayOrObservanceEvent(rawTitle, desc)) continue
                    if (isAllDay && !desc.contains("[Category:")) continue

                    val start = cursor.getLong(startIdx)
                    val end = if (endIdx != -1 && !cursor.isNull(endIdx)) {
                        cursor.getLong(endIdx)
                    } else {
                        start + 3600000L
                    }

                    val isDone = rawTitle.startsWith("✓ ") || desc.contains("[Status: Completed]")
                    val cleanTitle = rawTitle.removePrefix("✓ ").trim()

                    // Deduplicate by normalized title + start time
                    val dedupKey = "${cleanTitle.lowercase(Locale.getDefault())}_$start"
                    if (!seenKeys.add(dedupKey)) continue

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
                val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventIdNum)
                val updatedRows = contentResolver.update(uri, values, null, null)
                if (updatedRows > 0) {
                    return task.googleEventId
                }
            }

            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val newId = uri?.lastPathSegment
            if (newId != null) {
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
