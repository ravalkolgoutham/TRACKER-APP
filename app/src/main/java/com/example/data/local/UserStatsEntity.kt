package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.UserRewardStats

@Entity(tableName = "user_stats")
data class UserStatsEntity(
    @PrimaryKey val id: String = "primary_stats",
    val totalXp: Int,
    val availablePoints: Int,
    val currentLevel: Int,
    val currentStreakDays: Int,
    val lastCompletedDate: Long,
    val tasksCompletedCount: Int
) {
    fun toDomain(): UserRewardStats = UserRewardStats(
        id = id,
        totalXp = totalXp,
        availablePoints = availablePoints,
        currentLevel = currentLevel,
        currentStreakDays = currentStreakDays,
        lastCompletedDate = lastCompletedDate,
        tasksCompletedCount = tasksCompletedCount
    )

    companion object {
        fun fromDomain(stats: UserRewardStats): UserStatsEntity = UserStatsEntity(
            id = stats.id,
            totalXp = stats.totalXp,
            availablePoints = stats.availablePoints,
            currentLevel = stats.currentLevel,
            currentStreakDays = stats.currentStreakDays,
            lastCompletedDate = stats.lastCompletedDate,
            tasksCompletedCount = stats.tasksCompletedCount
        )
    }
}
