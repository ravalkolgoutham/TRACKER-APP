package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.GoalItem

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val targetCount: Int,
    val currentCount: Int,
    val category: String,
    val rewardPoints: Int,
    val isCompleted: Boolean,
    val createdAt: Long
) {
    fun toDomain(): GoalItem = GoalItem(
        id = id,
        title = title,
        description = description,
        targetCount = targetCount,
        currentCount = currentCount,
        category = category,
        rewardPoints = rewardPoints,
        isCompleted = isCompleted,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(goal: GoalItem): GoalEntity = GoalEntity(
            id = goal.id,
            title = goal.title,
            description = goal.description,
            targetCount = goal.targetCount,
            currentCount = goal.currentCount,
            category = goal.category,
            rewardPoints = goal.rewardPoints,
            isCompleted = goal.isCompleted,
            createdAt = goal.createdAt
        )
    }
}
