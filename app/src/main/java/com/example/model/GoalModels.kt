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
    val totalXp: Int = 0,
    val availablePoints: Int = 0,
    val currentLevel: Int = 1,
    val currentStreakDays: Int = 0,
    val bestStreakDays: Int = 0,
    val lastCompletedDate: Long = 0L,
    val tasksCompletedCount: Int = 0
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

    val isStreakMaintainedToday: Boolean
        get() {
            if (tasksCompletedCount == 0 || lastCompletedDate <= 0L) return false
            val lastCal = java.util.Calendar.getInstance().apply { timeInMillis = lastCompletedDate }
            val nowCal = java.util.Calendar.getInstance()
            return lastCal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR) &&
                    lastCal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR)
        }

    val isStreakActive: Boolean
        get() {
            if (tasksCompletedCount == 0 || lastCompletedDate <= 0L) return false
            val lastCal = java.util.Calendar.getInstance().apply { timeInMillis = lastCompletedDate }
            val nowCal = java.util.Calendar.getInstance()
            val sameDay = lastCal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR) &&
                    lastCal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR)
            val yesterday = if (lastCal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR)) {
                lastCal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR) - 1
            } else if (nowCal.get(java.util.Calendar.YEAR) == lastCal.get(java.util.Calendar.YEAR) + 1) {
                nowCal.get(java.util.Calendar.DAY_OF_YEAR) == 1 &&
                        lastCal.get(java.util.Calendar.DAY_OF_YEAR) == lastCal.getActualMaximum(java.util.Calendar.DAY_OF_YEAR)
            } else {
                false
            }
            return sameDay || yesterday
        }

    val displayStreakDays: Int
        get() = if (isStreakActive) maxOf(1, currentStreakDays) else 0
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

enum class BlastType {
    STREAK_GOAL,
    LEVEL_UP,
    GOAL_COMPLETED,
    MILESTONE
}

data class BlastEvent(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val subtitle: String,
    val badgeText: String = "",
    val type: BlastType = BlastType.MILESTONE
)

