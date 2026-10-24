package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.RewardItem

@Entity(tableName = "rewards")
data class RewardEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val costPoints: Int,
    val iconCategory: String,
    val isRedeemed: Boolean,
    val redeemedAt: Long?
) {
    fun toDomain(): RewardItem = RewardItem(
        id = id,
        title = title,
        description = description,
        costPoints = costPoints,
        iconCategory = iconCategory,
        isRedeemed = isRedeemed,
        redeemedAt = redeemedAt
    )

    companion object {
        fun fromDomain(reward: RewardItem): RewardEntity = RewardEntity(
            id = reward.id,
            title = reward.title,
            description = reward.description,
            costPoints = reward.costPoints,
            iconCategory = reward.iconCategory,
            isRedeemed = reward.isRedeemed,
            redeemedAt = reward.redeemedAt
        )
    }
}
