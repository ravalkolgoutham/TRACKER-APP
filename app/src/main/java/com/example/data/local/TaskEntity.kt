package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.SubtaskItem
import com.example.model.SyncStatus
import com.example.model.TaskItem
import org.json.JSONArray
import org.json.JSONObject

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
    val snoozeUntil: Long?,
    val etag: String? = null,
    val lastSyncedAt: Long? = null,
    val subtasksJson: String = "[]",
    val estimatedPomodoros: Int = 2,
    val completedPomodoros: Int = 0
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
        snoozeUntil = snoozeUntil,
        etag = etag,
        lastSyncedAt = lastSyncedAt,
        subtasks = parseSubtasks(subtasksJson),
        estimatedPomodoros = estimatedPomodoros,
        completedPomodoros = completedPomodoros
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
            snoozeUntil = task.snoozeUntil,
            etag = task.etag,
            lastSyncedAt = task.lastSyncedAt,
            subtasksJson = encodeSubtasks(task.subtasks),
            estimatedPomodoros = task.estimatedPomodoros,
            completedPomodoros = task.completedPomodoros
        )

        fun parseSubtasks(json: String): List<SubtaskItem> {
            if (json.isBlank() || json == "[]") return emptyList()
            return try {
                val arr = JSONArray(json)
                (0 until arr.length()).mapNotNull { idx ->
                    val obj = arr.optJSONObject(idx) ?: return@mapNotNull null
                    SubtaskItem(
                        id = obj.optString("id", "sub_$idx"),
                        title = obj.optString("title", ""),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        estimatedMinutes = obj.optInt("estimatedMinutes", 15)
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }

        fun encodeSubtasks(subtasks: List<SubtaskItem>): String {
            if (subtasks.isEmpty()) return "[]"
            val arr = JSONArray()
            for (s in subtasks) {
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("title", s.title)
                    put("isCompleted", s.isCompleted)
                    put("estimatedMinutes", s.estimatedMinutes)
                }
                arr.put(obj)
            }
            return arr.toString()
        }
    }
}
