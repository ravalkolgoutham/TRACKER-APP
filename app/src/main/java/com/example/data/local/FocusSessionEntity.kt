package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.FocusSession

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val taskId: String?,
    val taskTitle: String?,
    val durationMinutes: Int,
    val completedAt: Long,
    val tag: String
) {
    fun toDomain(): FocusSession = FocusSession(
        id = id,
        taskId = taskId,
        taskTitle = taskTitle,
        durationMinutes = durationMinutes,
        completedAt = completedAt,
        tag = tag
    )

    companion object {
        fun fromDomain(session: FocusSession): FocusSessionEntity = FocusSessionEntity(
            id = session.id,
            taskId = session.taskId,
            taskTitle = session.taskTitle,
            durationMinutes = session.durationMinutes,
            completedAt = session.completedAt,
            tag = session.tag
        )
    }
}
