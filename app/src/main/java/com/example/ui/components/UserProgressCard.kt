package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRewardStats
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.OneUiPrimary
import com.example.ui.theme.OneUiPrimaryLight
import com.example.ui.theme.OneUiSecondary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun UserProgressCard(
    userStats: UserRewardStats,
    onNavigateToRewards: () -> Unit,
    modifier: Modifier = Modifier,
    onTriggerStreakBlast: (() -> Unit)? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = userStats.levelProgressFraction,
        animationSpec = tween(durationMillis = 800),
        label = "xp_progress"
    )

    val currentInLevel = (userStats.totalXp - userStats.xpForCurrentLevel).coerceAtLeast(0)
    val neededInLevel = (userStats.xpForNextLevel - userStats.xpForCurrentLevel).coerceAtLeast(1)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
            .clickable { onNavigateToRewards() }
            .testTag("user_progress_card"),
        color = DarkSurface,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 1. Top Section: Level Crest Badge, Rank Title & Prominent Consecutive Day Streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Level Crest + User Title
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(OneUiPrimary, OneUiSecondary)
                                )
                            )
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "LVL",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUiSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "${userStats.currentLevel}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userStats.levelTitle,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Rank Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(OneUiPrimary.copy(alpha = 0.18f))
                                    .border(1.dp, OneUiPrimary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Rank ${userStats.currentLevel}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUiPrimaryLight
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${userStats.totalXp} XP • ${userStats.availablePoints} pts",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Consecutive Daily Streak Pill
                val streakDays = userStats.displayStreakDays
                val isMaintained = userStats.isStreakMaintainedToday
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isMaintained) Color(0xFFF59E0B).copy(alpha = 0.18f) 
                                else DarkSurfaceElevated
                            )
                            .border(
                                1.dp, 
                                if (isMaintained) Color(0xFFF59E0B).copy(alpha = 0.6f) else DarkBorder, 
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { onTriggerStreakBlast?.invoke() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak Flame",
                            tint = if (isMaintained) Color(0xFFF59E0B) else TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$streakDays Days",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMaintained) Color(0xFFFCD34D) else TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = if (isMaintained) "Maintained today ✓" else "Pending today",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isMaintained) Color(0xFF10B981) else TextTertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 2. XP Visual Progress Bar with Level-Up Indicators
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Level ${userStats.currentLevel} Progress",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "$currentInLevel / $neededInLevel XP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUiSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Vibrant Multi-Gradient Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkBorderSubtle, RoundedCornerShape(5.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        OneUiPrimary,
                                        OneUiSecondary,
                                        Color(0xFF818CF8)
                                    )
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Milestone checkpoints (25%, 50%, 75%, Level Up)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    VibrantMilestonePip(label = "25%", isReached = animatedProgress >= 0.25f)
                    VibrantMilestonePip(label = "50%", isReached = animatedProgress >= 0.50f)
                    VibrantMilestonePip(label = "75%", isReached = animatedProgress >= 0.75f)
                    VibrantMilestonePip(label = "Lv. ${userStats.currentLevel + 1} ⭐", isReached = animatedProgress >= 1f)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Dedicated Daily Streak Tracking Feature (Consecutive Day Visualizer)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkBorderSubtle, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Daily Streak Tracking",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Text(
                        text = "Best: ${userStats.bestStreakDays}d record",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFCD34D)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7-Day Consecutive Streak Days Grid
                StreakWeekTracker(
                    currentStreakDays = userStats.displayStreakDays,
                    isMaintainedToday = userStats.isStreakMaintainedToday
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Informational Context & Instructions
                val streakGuidance = when {
                    userStats.isStreakMaintainedToday ->
                        "Streak safe! You completed tasks today on consecutive days. Complete tomorrow to hit ${userStats.currentStreakDays + 1} days!"
                    userStats.isStreakActive ->
                        "Your ${userStats.currentStreakDays}-day streak is on the line! Complete any task or event today to maintain consecutive day progress."
                    else ->
                        "Complete any task today to start your consecutive day streak and level up faster."
                }

                Text(
                    text = streakGuidance,
                    fontSize = 11.5.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Bottom Milestone Highlights Row: Tasks count & View Rewards Action
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderSubtle)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${userStats.tasksCompletedCount} Event Milestones Completed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigateToRewards() }
                    ) {
                        Text(
                            text = "Rewards",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = OneUiPrimaryLight
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Go to rewards",
                            tint = OneUiPrimaryLight,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakWeekTracker(
    currentStreakDays: Int,
    isMaintainedToday: Boolean
) {
    // Generate 7 consecutive days ending today
    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val now = Calendar.getInstance()

    val daysList = (6 downTo 0).map { offset ->
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -offset)
        }
        val isToday = offset == 0
        // Determine if this day is part of the active streak
        val isStreakDay = if (isToday) {
            isMaintainedToday
        } else {
            // Day offset within current streak
            val streakStartOffset = if (isMaintainedToday) currentStreakDays - 1 else currentStreakDays
            offset <= streakStartOffset && currentStreakDays > 0
        }

        Triple(dayFormat.format(cal.time).take(2).uppercase(Locale.getDefault()), isToday, isStreakDay)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        daysList.forEach { (label, isToday, isStreakDay) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) Color.White else TextTertiary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isStreakDay -> Color(0xFF10B981)
                                isToday -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                else -> DarkSurface
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = when {
                                isStreakDay -> Color(0xFF10B981)
                                isToday -> Color(0xFFF59E0B)
                                else -> DarkBorderSubtle
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isStreakDay -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Day Completed",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        isToday -> {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Pending Today",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF475569))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VibrantMilestonePip(label: String, isReached: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isReached) OneUiSecondary else Color(0xFF334155))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isReached) FontWeight.Bold else FontWeight.Normal,
            color = if (isReached) OneUiSecondary else Color(0xFF64748B)
        )
    }
}
