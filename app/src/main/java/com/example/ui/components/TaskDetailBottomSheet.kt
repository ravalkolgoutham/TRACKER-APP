package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TaskItem
import com.example.ui.theme.AeroSquircleButton
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
import com.example.ui.theme.glassSpecularBorderBrush
import com.example.ui.theme.glassSurfaceBrush
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailBottomSheet(
    task: TaskItem,
    onDismiss: () -> Unit,
    onToggleComplete: () -> Unit,
    onSnooze: () -> Unit,
    onStartFocus: (durationMinutes: Int) -> Unit,
    onDelete: () -> Unit,
    onSmartBreakdown: (() -> Unit)? = null,
    onToggleSubtask: ((String) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // 0 = Description / Overview, 1 = Specification & Subtasks (matching the reference UI tabs)
    var activeTab by remember { mutableIntStateOf(0) }
    var customFocusPomodoros by remember { mutableIntStateOf(task.estimatedPomodoros.coerceAtLeast(1)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp)
                .testTag("task_detail_bottom_sheet")
        ) {
            // Reference-style Top Bar: Glowing `<` Squircle Button + Uppercase Title + Sync Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AeroSquircleButton(
                    onClick = onDismiss,
                    isActive = true,
                    cornerRadius = 12.dp,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = task.category.uppercase(Locale.getDefault()) + " — " + task.priority.label.uppercase(Locale.getDefault()),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    letterSpacing = 0.8.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(OneUiSecondary.copy(alpha = 0.15f))
                        .border(1.dp, OneUiSecondary.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = null,
                        tint = OneUiSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (task.googleEventId != null) "Calendar" else "Local",
                        color = OneUiSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Segmented Glass Tabs: "Description" | "Specification" (inspired directly by reference image left screen)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                listOf("Description" to 0, "Specification" to 1).forEach { (label, idx) ->
                    val isSelected = activeTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) DarkSurfaceElevated else DarkSurface.copy(alpha = 0.6f)
                            )
                            .border(
                                width = 1.dp,
                                brush = if (isSelected) glassSpecularBorderBrush(OneUiSecondary) else Brush.linearGradient(
                                    listOf(DarkBorder.copy(alpha = 0.4f), DarkBorder.copy(alpha = 0.2f))
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { activeTab = idx }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) OneUiSecondary else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Title
            Text(
                text = task.title,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (activeTab == 0) {
                // Description & Interactive Controls Tab
                Text(
                    text = task.description.ifBlank {
                        "Scheduled in your unified timeline with automatic Google & Samsung Calendar synchronization and exact 15-minute lead-time alarm notifications."
                    },
                    fontSize = 13.5.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive Pomodoro Target Stepper Row (matching the reference cart screen `+ 1 -` squircle steppers)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(glassSurfaceBrush())
                        .border(1.dp, glassSpecularBorderBrush(), RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "🍅 Pomodoro Sprint Target",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${task.completedPomodoros} completed • ${customFocusPomodoros * 25} mins total focus",
                                fontSize = 11.5.sp,
                                color = OneUiSecondary
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AeroSquircleButton(
                                onClick = { customFocusPomodoros = (customFocusPomodoros + 1).coerceAtMost(12) },
                                isActive = true,
                                cornerRadius = 8.dp,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase Pomodoros",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = customFocusPomodoros.toString(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )

                            AeroSquircleButton(
                                onClick = { customFocusPomodoros = (customFocusPomodoros - 1).coerceAtLeast(1) },
                                isActive = false,
                                cornerRadius = 8.dp,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease Pomodoros",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Interactive Action Pills: Snooze, Complete/Reopen, Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AeroSquircleButton(
                        onClick = {
                            onToggleComplete()
                            onDismiss()
                        },
                        isActive = false,
                        cornerRadius = 14.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("detail_complete_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = OneUiSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (task.isCompleted) "Reopen" else "Complete",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUiSuccess
                            )
                        }
                    }

                    AeroSquircleButton(
                        onClick = {
                            onSnooze()
                            onDismiss()
                        },
                        isActive = false,
                        cornerRadius = 14.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("detail_snooze_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = null,
                                tint = OneUiWarning,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Snooze",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUiWarning
                            )
                        }
                    }

                    AeroSquircleButton(
                        onClick = {
                            onDelete()
                            onDismiss()
                        },
                        isActive = false,
                        cornerRadius = 14.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("detail_delete_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = OneUiError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Delete",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = OneUiError
                            )
                        }
                    }
                }
            } else {
                // Specification & Smart Subtasks Tab
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(glassSurfaceBrush())
                        .border(1.dp, glassSpecularBorderBrush(), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val sdf = SimpleDateFormat("EEEE, MMM d, yyyy • h:mm a", Locale.getDefault())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = OneUiSecondary, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = sdf.format(Date(task.scheduledTime)), fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Alarm, contentDescription = null, tint = OneUiPrimaryLight, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Exact Alarm: ${task.reminderMinutesBefore}m lead time (${task.durationMinutes}m block)", fontSize = 12.5.sp, color = TextSecondary)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Repeat, contentDescription = null, tint = OneUiSecondary, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Recurrence: ${task.repeatRule.label}", fontSize = 12.5.sp, color = TextSecondary)
                        }

                        if (task.subtasks.isEmpty() && onSmartBreakdown != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            AeroSquircleButton(
                                onClick = onSmartBreakdown,
                                isActive = true,
                                cornerRadius = 12.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate Smart Subtask Breakdown", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        } else if (task.subtasks.isNotEmpty()) {
                            LinearProgressIndicator(
                                progress = { task.subtaskProgressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = OneUiSuccess,
                                trackColor = DarkBorder
                            )
                            task.subtasks.forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleSubtask?.invoke(sub.id) }
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (sub.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (sub.isCompleted) OneUiSuccess else OneUiSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sub.title,
                                        fontSize = 12.5.sp,
                                        color = if (sub.isCompleted) TextTertiary else TextPrimary,
                                        textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(text = "${sub.estimatedMinutes}m", fontSize = 11.sp, color = TextTertiary)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reference-style Bottom Glass Bar: Reward Value on Left + Glowing Gradient Action Pill on Right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(glassSurfaceBrush(0.95f))
                    .border(1.dp, glassSpecularBorderBrush(OneUiSecondary), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Completion Reward",
                            fontSize = 11.sp,
                            color = TextTertiary
                        )
                        Text(
                            text = "+${task.priority.xpValue} XP",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = OneUiSecondary
                        )
                    }

                    AeroSquircleButton(
                        onClick = {
                            onStartFocus(customFocusPomodoros * 25)
                            onDismiss()
                        },
                        isActive = true,
                        cornerRadius = 16.dp,
                        modifier = Modifier
                            .height(48.dp)
                            .width(165.dp)
                            .testTag("detail_focus_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Start Focus",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}
