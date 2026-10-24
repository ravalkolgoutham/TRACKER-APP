package com.example.model

data class GoalItem(
    val id: String,
    val title: String,
    val description: String = "",
    val targetCount: Int = 5,
    val currentCount: Int = 0,
    val category: String = "General",
    val rewardPoints: Int = 150,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressFraction: Float
        get() = if (targetCount > 0) (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f) else 0f
}

data class UserRewardStats(
    val id: String = "primary_stats",
    val totalXp: Int = 240,
    val availablePoints: Int = 180,
    val currentLevel: Int = 2,
    val currentStreakDays: Int = 3,
    val lastCompletedDate: Long = System.currentTimeMillis(),
    val tasksCompletedCount: Int = 7
) {
    val levelTitle: String
        get() = when (currentLevel) {
            1 -> "Novice Organizer"
            2 -> "Focused Strategist"
            3 -> "Deep Work Ace"
            4 -> "Execution Maestro"
            else -> "Productivity Titan"
        }

    val xpForCurrentLevel: Int
        get() = (currentLevel - 1) * 200

    val xpForNextLevel: Int
        get() = currentLevel * 200

    val levelProgressFraction: Float
        get() {
            val needed = xpForNextLevel - xpForCurrentLevel
            if (needed <= 0) return 1f
            val currentInLevel = (totalXp - xpForCurrentLevel).coerceAtLeast(0)
            return (currentInLevel.toFloat() / needed.toFloat()).coerceIn(0f, 1f)
        }
}

data class RewardItem(
    val id: String,
    val title: String,
    val description: String = "",
    val costPoints: Int,
    val iconCategory: String = "Gift",
    val isRedeemed: Boolean = false,
    val redeemedAt: Long? = null
)
