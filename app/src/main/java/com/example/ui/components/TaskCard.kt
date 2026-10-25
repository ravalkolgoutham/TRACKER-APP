package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.SyncStatus
import com.example.model.TaskItem
import com.example.ui.theme.CategoryFocus
import com.example.ui.theme.CategoryHealth
import com.example.ui.theme.CategoryMeeting
import com.example.ui.theme.CategoryPersonal
import com.example.ui.theme.CategoryWork
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.OneUiError
import com.example.ui.theme.OneUiPrimary
import com.example.ui.theme.OneUiPrimaryLight
import com.example.ui.theme.OneUiSecondary
import com.example.ui.theme.OneUiSuccess
import com.example.ui.theme.OneUiWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskCard(
    task: TaskItem,
    onToggleComplete: () -> Unit,
    onSnooze: () -> Unit,
    onDelete: () -> Unit,
    onStartFocus: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasConflict: Boolean = false,
    onAutoResolveConflict: (() -> Unit)? = null,
    onSmartBreakdown: (() -> Unit)? = null,
    onToggleSubtask: ((String) -> Unit)? = null
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onToggleComplete()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onSnooze()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    val priorityColor = when (task.priority) {
        Priority.URGENT -> OneUiError
        Priority.HIGH -> OneUiWarning
        Priority.MEDIUM -> OneUiPrimary
        Priority.LOW -> OneUiSuccess
    }

    val categoryColor = when (task.category) {
        "Work" -> CategoryWork
        "Personal" -> CategoryPersonal
        "Focus" -> CategoryFocus
        "Health" -> CategoryHealth
        "Meeting" -> CategoryMeeting
        else -> OneUiPrimary
    }

    val borderColor = when {
        hasConflict -> OneUiError
        task.isExternalCalendarEvent -> OneUiSecondary.copy(alpha = 0.65f)
        task.isCompleted -> DarkBorder.copy(alpha = 0.4f)
        else -> DarkBorder
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_card_${task.id}"),
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> OneUiSuccess.copy(alpha = 0.25f)
                    SwipeToDismissBoxValue.EndToStart -> OneUiWarning.copy(alpha = 0.25f)
                    else -> Color.Transparent
                },
                label = "swipe_color"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(color)
                    .border(1.dp, DarkBorder, RoundedCornerShape(22.dp))
                    .padding(horizontal = 26.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Complete task",
                            tint = OneUiSuccess,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Mark Complete (+${task.priority.xpValue} XP)",
                            color = OneUiSuccess,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Snooze 10m",
                            color = OneUiWarning,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "Snooze task",
                            tint = OneUiWarning,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (task.isCompleted) {
                        com.example.ui.theme.glassSurfaceBrush(0.6f)
                    } else {
                        com.example.ui.theme.glassSurfaceBrush(1f)
                    }
                )
                .border(
                    width = 1.dp,
                    brush = if (hasConflict) {
                        androidx.compose.ui.graphics.Brush.linearGradient(listOf(OneUiError, OneUiWarning))
                    } else {
                        com.example.ui.theme.glassSpecularBorderBrush(categoryColor)
                    },
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 15.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Vibrant priority/category vertical accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (task.isCompleted) TextTertiary else priorityColor)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Header Row: Checkbox, Title, and Focus Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { onToggleComplete() }
                                .testTag("task_checkbox_${task.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (task.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Task completed",
                                    tint = OneUiSuccess,
                                    modifier = Modifier.size(26.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Mark task complete",
                                    tint = priorityColor,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (task.isCompleted) TextTertiary else TextPrimary,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!task.isCompleted) {
                            // Smart AI Subtask Breakdown Button
                            if (onSmartBreakdown != null && task.subtasks.isEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(OneUiWarning.copy(alpha = 0.16f))
                                        .border(1.dp, OneUiWarning.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                        .clickable { onSmartBreakdown() }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("smart_breakdown_button_${task.id}")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Smart Breakdown",
                                            tint = OneUiWarning,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Breakdown",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OneUiWarning
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))
                            com.example.ui.theme.AeroSquircleButton(
                                onClick = onStartFocus,
                                isActive = true,
                                cornerRadius = 11.dp,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("focus_button_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Focus timer for task",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = task.description,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(start = 48.dp)
                        )
                    }

                    // Scheduling Conflict Alert + 1-Click Auto-Resolve Action
                    if (hasConflict && !task.isCompleted) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(OneUiError.copy(alpha = 0.14f))
                                .border(1.dp, OneUiError.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = OneUiError,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Overlapping time block",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = OneUiError
                                )
                            }
                            if (onAutoResolveConflict != null) {
                                Text(
                                    text = "Auto-Resolve →",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(OneUiError)
                                        .clickable { onAutoResolveConflict() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .testTag("auto_resolve_conflict_${task.id}")
                                )
                            }
                        }
                    }

                    // Nested Subtasks Progress & Interactive Checklist
                    if (task.subtasks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val doneSubs = task.subtasks.count { it.isCompleted }
                        val totalSubs = task.subtasks.size
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurface)
                                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚡ Smart Subtasks ($doneSubs/$totalSubs completed)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUiSecondary
                                )
                                Text(
                                    text = "${(task.subtaskProgressFraction * 100).toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = OneUiSuccess
                                )
                            }
                            LinearProgressIndicator(
                                progress = { task.subtaskProgressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = OneUiSuccess,
                                trackColor = DarkBorder
                            )
                            task.subtasks.forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleSubtask?.invoke(sub.id) }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (sub.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (sub.isCompleted) OneUiSuccess else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sub.title,
                                        fontSize = 12.sp,
                                        color = if (sub.isCompleted) TextTertiary else TextPrimary,
                                        textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${sub.estimatedMinutes}m",
                                        fontSize = 10.5.sp,
                                        color = TextTertiary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Meta Chips Row
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 48.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                        val formatted = "${dateFormat.format(Date(task.scheduledTime))}, ${timeFormat.format(Date(task.scheduledTime))} (${task.durationMinutes}m)"

                        MetaChip(
                            icon = Icons.Default.AccessTime,
                            text = formatted,
                            color = OneUiSecondary
                        )

                        MetaChip(
                            text = "#${task.category}",
                            color = categoryColor
                        )

                        MetaChip(
                            text = "${task.priority.label} (+${task.priority.xpValue} XP)",
                            color = priorityColor
                        )

                        // Pomodoro Actual vs Estimated Badge
                        MetaChip(
                            text = "🍅 ${task.completedPomodoros}/${task.estimatedPomodoros}",
                            color = OneUiWarning
                        )

                        if (task.isExternalCalendarEvent) {
                            MetaChip(
                                icon = Icons.Default.CalendarMonth,
                                text = "Google Calendar",
                                color = OneUiSecondary
                            )
                        }

                        if (task.repeatRule != RepeatRule.NONE) {
                            MetaChip(
                                icon = Icons.Default.Repeat,
                                text = task.repeatRule.label,
                                color = OneUiPrimaryLight
                            )
                        }

                        when (task.syncStatus) {
                            SyncStatus.SYNCED -> {
                                MetaChip(
                                    icon = Icons.Default.Cloud,
                                    text = "Synced",
                                    color = OneUiSuccess
                                )
                            }
                            SyncStatus.CONFLICT_RESOLVED -> {
                                MetaChip(
                                    icon = Icons.Default.Cloud,
                                    text = "Reconciled",
                                    color = OneUiSecondary
                                )
                            }
                            SyncStatus.PENDING_INSERT, SyncStatus.PENDING_UPDATE -> {
                                MetaChip(
                                    icon = Icons.Default.Cloud,
                                    text = "Queued",
                                    color = OneUiWarning
                                )
                            }
                            SyncStatus.PENDING_DELETE -> {}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetaChip(
    text: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}
