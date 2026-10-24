package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.BlastType
import com.example.model.GoalItem
import com.example.model.RewardItem
import com.example.model.UserRewardStats
import com.example.ui.components.CreateGoalDialog
import com.example.ui.components.CreateRewardDialog
import com.example.ui.components.OneUiHeader
import com.example.ui.components.UserProgressCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.OneUiAccent
import com.example.ui.theme.OneUiError
import com.example.ui.theme.OneUiPrimary
import com.example.ui.theme.OneUiPrimaryLight
import com.example.ui.theme.OneUiSecondary
import com.example.ui.theme.OneUiSuccess
import com.example.ui.theme.OneUiWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun GoalsRewardsScreen(
    userStats: UserRewardStats,
    goals: List<GoalItem>,
    rewards: List<RewardItem>,
    celebrationMessage: String?,
    onClearCelebration: () -> Unit,
    onRedeemReward: (String) -> Unit,
    onCreateGoal: (title: String, description: String, targetCount: Int, category: String, rewardPoints: Int) -> Unit,
    onCreateCustomReward: (title: String, description: String, costPoints: Int, iconCategory: String) -> Unit,
    onDeleteGoal: (String) -> Unit,
    modifier: Modifier = Modifier,
    onTriggerCelebrationBlast: ((BlastType) -> Unit)? = null
) {
    var showCreateGoalDialog by remember { mutableStateOf(false) }
    var showCreateRewardDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
    ) {
        OneUiHeader(
            title = "Goals & Rewards",
            subtitle = "Earn XP and unlock real-life rewards for completing events and tasks",
            isSyncing = false,
            isGoogleConnected = true,
            onSyncClick = {}
        )

        Spacer(modifier = Modifier.height(18.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Floating Celebration Banner
            AnimatedVisibility(
                visible = !celebrationMessage.isNullOrBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                celebrationMessage?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .testTag("celebration_banner"),
                        color = Color(0xFF1E1E1E),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = msg,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(
                                onClick = onClearCelebration,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 1. User Progress Card with XP bar and level-up indicators
            UserProgressCard(
                userStats = userStats,
                onNavigateToRewards = {},
                onTriggerStreakBlast = { onTriggerCelebrationBlast?.invoke(BlastType.STREAK_GOAL) }
            )

            // 2. Interactive Milestone & Streak Particle Blast Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(22.dp))
                    .testTag("celebration_blast_panel"),
                color = DarkSurface,
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF222222))
                                    .border(1.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Celebratory Blast Animation",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Explosive particle animation upon milestones & streaks",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onTriggerCelebrationBlast?.invoke(BlastType.STREAK_GOAL) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_trigger_streak_blast"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF222222),
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF555555))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Streak Blast", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { onTriggerCelebrationBlast?.invoke(BlastType.LEVEL_UP) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_trigger_level_blast"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF191919),
                                contentColor = Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Level Blast", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 2. Active Goals Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Goals (${goals.count { !it.isCompleted }})",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedButton(
                        onClick = { showCreateGoalDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("add_goal_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Add Goal", fontSize = 13.sp)
                    }
                }

                if (goals.isEmpty()) {
                    Text(
                        text = "No active goals yet. Create one to earn massive bonus XP on milestone completions!",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                } else {
                    goals.forEach { goal ->
                        GoalCard(
                            goal = goal,
                            onDelete = { onDeleteGoal(goal.id) }
                        )
                    }
                }
            }

            // 3. Rewards Store & Redemption Center
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rewards Store",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedButton(
                        onClick = { showCreateRewardDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("add_custom_reward_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Add Reward", fontSize = 13.sp)
                    }
                }

                Text(
                    text = "Spend your completion points on real-life perks and treats:",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                rewards.forEach { reward ->
                    RewardCard(
                        reward = reward,
                        availablePoints = userStats.availablePoints,
                        onRedeem = { onRedeemReward(reward.id) }
                    )
                }
            }

            // 4. Milestone Badges Showcase
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Unlocked Achievements",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AchievementBadge(
                        title = "First Victory",
                        description = "1st task done",
                        isUnlocked = userStats.tasksCompletedCount >= 1,
                        modifier = Modifier.weight(1f)
                    )
                    AchievementBadge(
                        title = "Streak Pro",
                        description = "3+ day streak",
                        isUnlocked = userStats.currentStreakDays >= 3,
                        modifier = Modifier.weight(1f)
                    )
                    AchievementBadge(
                        title = "Task Master",
                        description = "5+ tasks done",
                        isUnlocked = userStats.tasksCompletedCount >= 5,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showCreateGoalDialog) {
        CreateGoalDialog(
            onDismiss = { showCreateGoalDialog = false },
            onSave = onCreateGoal
        )
    }

    if (showCreateRewardDialog) {
        CreateRewardDialog(
            onDismiss = { showCreateRewardDialog = false },
            onSave = onCreateCustomReward
        )
    }
}

@Composable
private fun GoalCard(
    goal: GoalItem,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (goal.isCompleted) OneUiSuccess.copy(alpha = 0.5f) else DarkBorder
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(OneUiSecondary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = goal.category,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUiSecondary
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(OneUiPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "+${goal.rewardPoints} pts",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUiPrimaryLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = goal.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (goal.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = goal.description,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }

                if (goal.isCompleted) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(OneUiSuccess.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = OneUiSuccess, modifier = Modifier.size(22.dp))
                    }
                } else {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Goal", tint = TextTertiary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar & Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (goal.isCompleted) "Completed ✓" else "Progress",
                    fontSize = 12.sp,
                    color = if (goal.isCompleted) OneUiSuccess else TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${goal.currentCount} / ${goal.targetCount} events",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (goal.isCompleted) OneUiSuccess else OneUiSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { goal.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (goal.isCompleted) OneUiSuccess else OneUiSecondary,
                trackColor = DarkSurface
            )
        }
    }
}

@Composable
private fun RewardCard(
    reward: RewardItem,
    availablePoints: Int,
    onRedeem: () -> Unit
) {
    val canAfford = availablePoints >= reward.costPoints && !reward.isRedeemed

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (reward.isRedeemed) OneUiSuccess.copy(alpha = 0.4f) else DarkBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                val iconVector = when (reward.iconCategory.lowercase()) {
                    "coffee" -> Icons.Default.LocalCafe
                    "game" -> Icons.Default.SportsEsports
                    "movie" -> Icons.Default.Movie
                    "nature" -> Icons.Default.Park
                    else -> Icons.Default.EmojiEvents
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (reward.isRedeemed) OneUiSuccess.copy(alpha = 0.15f)
                            else OneUiPrimary.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (reward.isRedeemed) Icons.Default.Check else iconVector,
                        contentDescription = null,
                        tint = if (reward.isRedeemed) OneUiSuccess else OneUiPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = reward.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (reward.isRedeemed) TextSecondary else TextPrimary
                    )
                    if (reward.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = reward.description,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${reward.costPoints} points",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (reward.isRedeemed) OneUiSuccess else OneUiWarning
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (reward.isRedeemed) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(OneUiSuccess.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(text = "Claimed ✓", fontSize = 12.sp, color = OneUiSuccess, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onRedeem,
                    enabled = canAfford,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF1E1E1E),
                        disabledContentColor = TextTertiary
                    ),
                    modifier = Modifier.testTag("redeem_button_${reward.id}")
                ) {
                    Text(text = "Redeem", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    title: String,
    description: String,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)),
        color = if (isUnlocked) DarkSurfaceElevated else DarkSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isUnlocked) OneUiPrimary.copy(alpha = 0.4f) else DarkBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isUnlocked) OneUiPrimary.copy(alpha = 0.2f)
                        else DarkBorder.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Default.Stars else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) OneUiPrimary else TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) TextPrimary else TextTertiary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = description,
                fontSize = 10.5.sp,
                color = TextSecondary
            )
        }
    }
}
