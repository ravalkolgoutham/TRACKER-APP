package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.SyncStatus
import com.example.model.TaskItem

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val googleEventId: String?,
    val title: String,
    val description: String,
    val scheduledTime: Long,
    val endTime: Long,
    val isCompleted: Boolean,
    val priority: Priority,
    val category: String,
    val repeatRule: RepeatRule,
    val reminderMinutesBefore: Int,
    val syncStatus: SyncStatus,
    val lastModified: Long,
    val version: Int,
    val snoozeUntil: Long?
) {
    fun toDomain(): TaskItem = TaskItem(
        id = id,
        googleEventId = googleEventId,
        title = title,
        description = description,
        scheduledTime = scheduledTime,
        endTime = endTime,
        isCompleted = isCompleted,
        priority = priority,
        category = category,
        repeatRule = repeatRule,
        reminderMinutesBefore = reminderMinutesBefore,
        syncStatus = syncStatus,
        lastModified = lastModified,
        version = version,
        snoozeUntil = snoozeUntil
    )

    companion object {
        fun fromDomain(task: TaskItem): TaskEntity = TaskEntity(
            id = task.id,
            googleEventId = task.googleEventId,
            title = task.title,
            description = task.description,
            scheduledTime = task.scheduledTime,
            endTime = task.endTime,
            isCompleted = task.isCompleted,
            priority = task.priority,
            category = task.category,
            repeatRule = task.repeatRule,
            reminderMinutesBefore = task.reminderMinutesBefore,
            syncStatus = task.syncStatus,
            lastModified = task.lastModified,
            version = task.version,
            snoozeUntil = task.snoozeUntil
        )
    }
}
