package com.example.model

enum class Priority(val label: String, val level: Int, val xpValue: Int) {
    LOW("Low", 1, 10),
    MEDIUM("Medium", 2, 25),
    HIGH("High", 3, 50),
    URGENT("Urgent", 4, 100)
}

enum class RepeatRule(val label: String) {
    NONE("Does not repeat"),
    DAILY("Every day"),
    WEEKDAYS("Weekdays (Mon-Fri)"),
    WEEKLY("Every week"),
    MONTHLY("Every month")
}

enum class SyncStatus {
    SYNCED,
    PENDING_INSERT,
    PENDING_UPDATE,
    PENDING_DELETE,
    CONFLICT_RESOLVED
}

enum class TaskViewMode(val label: String) {
    LIST("List"),
    KANBAN("Kanban"),
    TIMELINE("Timeline")
}

data class SubtaskItem(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false,
    val estimatedMinutes: Int = 15
)

data class SyncQueueItem(
    val id: String,
    val entityType: String, // "TASK", "EVENT", "FOCUS", "STATS"
    val action: String,     // "CREATE", "UPDATE", "TOGGLE", "DELETE", "SYNC"
    val payloadSummary: String,
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val syncStatus: String = "synced" // "synced", "pending", "conflict", "offline"
)

data class TaskItem(
    val id: String,
    val googleEventId: String? = null,
    val title: String,
    val description: String = "",
    val scheduledTime: Long, // Epoch millis
    val endTime: Long = scheduledTime + (60 * 60 * 1000), // Default 1 hr
    val isCompleted: Boolean = false,
    val priority: Priority = Priority.MEDIUM,
    val category: String = "Work",
    val repeatRule: RepeatRule = RepeatRule.NONE,
    val reminderMinutesBefore: Int = 15,
    val syncStatus: SyncStatus = SyncStatus.PENDING_INSERT,
    val lastModified: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val snoozeUntil: Long? = null,
    val etag: String? = null,
    val lastSyncedAt: Long? = null,
    val subtasks: List<SubtaskItem> = emptyList(),
    val estimatedPomodoros: Int = 2,
    val completedPomodoros: Int = 0
) {
    val durationMinutes: Int
        get() = ((endTime - scheduledTime) / 60000L).toInt().coerceAtLeast(15)

    val isExternalCalendarEvent: Boolean
        get() = !googleEventId.isNullOrBlank()

    val subtaskProgressFraction: Float
        get() = if (subtasks.isNotEmpty()) {
            subtasks.count { it.isCompleted }.toFloat() / subtasks.size.toFloat()
        } else 0f
}

data class ConflictLog(
    val id: String,
    val taskTitle: String,
    val localTimestamp: Long,
    val remoteTimestamp: Long,
    val resolutionReason: String,
    val resolvedAt: Long = System.currentTimeMillis()
)

data class FocusSession(
    val id: String,
    val taskId: String?,
    val taskTitle: String?,
    val durationMinutes: Int,
    val completedAt: Long = System.currentTimeMillis(),
    val tag: String = "General"
)
