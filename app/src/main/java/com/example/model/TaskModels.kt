package com.example.model

enum class Priority(val label: String, val level: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4)
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
    val reminderMinutesBefore: Int = 15, // Default 15 minutes as requested
    val syncStatus: SyncStatus = SyncStatus.PENDING_INSERT,
    val lastModified: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val snoozeUntil: Long? = null
)

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
