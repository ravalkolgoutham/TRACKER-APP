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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.DarkBorder
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
    modifier: Modifier = Modifier
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
        Priority.LOW -> OneUiSecondary
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
                    SwipeToDismissBoxValue.StartToEnd -> OneUiSuccess.copy(alpha = 0.9f)
                    SwipeToDismissBoxValue.EndToStart -> OneUiWarning.copy(alpha = 0.9f)
                    else -> Color.Transparent
                },
                label = "swipe_color"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(color)
                    .padding(horizontal = 26.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Complete task",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Mark Complete",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Snooze 10m",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Default.Snooze,
                            contentDescription = "Snooze task",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .border(
                    1.dp,
                    if (task.isCompleted) DarkBorder.copy(alpha = 0.4f) else DarkBorder,
                    RoundedCornerShape(22.dp)
                )
                .clickable { onClick() },
            colors = CardDefaults.cardColors(
                containerColor = if (task.isCompleted) DarkSurfaceElevated.copy(alpha = 0.5f) else DarkSurfaceElevated
            ),
            shape = RoundedCornerShape(22.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Subtle category/priority vertical accent indicator
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (task.isCompleted) TextTertiary else priorityColor)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Header Row: Checkbox, Title, and Focus Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checkbox with 48dp minimum interactive touch area
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
                                    tint = TextSecondary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = task.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (task.isCompleted) TextTertiary else TextPrimary,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        // Focus Button Shortcut
                        if (!task.isCompleted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = onStartFocus,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(OneUiPrimary.copy(alpha = 0.15f))
                                    .testTag("focus_button_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start Focus timer for task",
                                    tint = OneUiPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Optional Description with generous spacing
                    if (task.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = task.description,
                            fontSize = 13.5.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(start = 50.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Meta Chips Row with generous spacing
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 50.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                        val formatted = "${dateFormat.format(Date(task.scheduledTime))}, ${timeFormat.format(Date(task.scheduledTime))}"

                        MetaChip(
                            icon = Icons.Default.AccessTime,
                            text = formatted,
                            color = OneUiSecondary
                        )

                        MetaChip(
                            icon = Icons.Default.Alarm,
                            text = "15m alert",
                            color = OneUiPrimaryLight
                        )

                        MetaChip(
                            text = task.priority.label,
                            color = priorityColor
                        )

                        if (task.repeatRule != RepeatRule.NONE) {
                            MetaChip(
                                icon = Icons.Default.Repeat,
                                text = task.repeatRule.label,
                                color = OneUiSecondary
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
            .background(color.copy(alpha = 0.12f))
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
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}
