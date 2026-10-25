package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.SyncQueueItem
import com.example.model.SyncStatus
import com.example.model.TaskItem
import com.example.ui.components.OneUiHeader
import com.example.ui.components.SnoozeOptionsBottomSheet
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskDetailBottomSheet
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.OneUiError
import com.example.ui.theme.OneUiPrimary
import com.example.ui.theme.OneUiPrimaryLight
import com.example.ui.theme.OneUiSecondary
import com.example.ui.theme.OneUiSuccess
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    tasks: List<TaskItem>,
    isSyncing: Boolean,
    isGoogleConnected: Boolean,
    onSyncClick: () -> Unit,
    onToggleComplete: (String) -> Unit,
    onSnooze: (String, Int) -> Unit,
    onDelete: (String) -> Unit,
    onStartFocus: (taskId: String, title: String) -> Unit,
    modifier: Modifier = Modifier,
    syncQueueLogs: List<SyncQueueItem> = emptyList(),
    onAutoResolveConflict: ((String) -> Unit)? = null,
    onRunSimulatedSync: (() -> Unit)? = null,
    onSmartBreakdown: ((String) -> Unit)? = null,
    onToggleSubtask: ((String, String) -> Unit)? = null,
    onStartFocusWithDuration: ((taskId: String, title: String, durationMinutes: Int) -> Unit)? = null
) {
    val dates = remember {
        (0..13).map { offset ->
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
            }
            cal.time
        }
    }

    var selectedDate by remember { mutableStateOf(dates[0]) }
    var showHourlyTimelineGrid by remember { mutableStateOf(true) }
    var taskToSnooze by remember { mutableStateOf<TaskItem?>(null) }
    var taskToDetail by remember { mutableStateOf<TaskItem?>(null) }

    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dayNumFormat = SimpleDateFormat("d", Locale.getDefault())
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    val dateYearDayFormat = SimpleDateFormat("yyyy-DDD", Locale.getDefault())
    val selectedDayKey = dateYearDayFormat.format(selectedDate)

    val dayTasks = tasks.filter { task ->
        val taskDayKey = dateYearDayFormat.format(Date(task.scheduledTime))
        taskDayKey == selectedDayKey
    }.sortedBy { it.scheduledTime }

    // Detect overlapping time blocks on the selected date
    val conflictingTaskIds = remember(dayTasks) {
        val active = dayTasks.filter { !it.isCompleted }
        val ids = mutableSetOf<String>()
        for (i in active.indices) {
            for (j in i + 1 until active.size) {
                val a = active[i]
                val b = active[j]
                if (a.scheduledTime < b.endTime && b.scheduledTime < a.endTime) {
                    ids.add(a.id)
                    ids.add(b.id)
                }
            }
        }
        ids
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        OneUiHeader(
            title = "Calendar Schedule",
            subtitle = "${tasks.count { it.googleEventId != null }} events synced • Two-Way Google & Samsung Calendar",
            isSyncing = isSyncing,
            isGoogleConnected = isGoogleConnected,
            onSyncClick = onSyncClick,
            pendingQueueCount = tasks.count { it.syncStatus != SyncStatus.SYNCED },
            syncQueueLogs = syncQueueLogs
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Month & Year + Simulated Two-Way Sync Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthYearFormat.format(selectedDate),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onRunSimulatedSync != null) {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onRunSimulatedSync() }
                            .testTag("calendar_simulated_sync_btn"),
                        color = OneUiSecondary.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, OneUiSecondary.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = "Simulated Google Calendar Sync",
                                tint = OneUiSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Sim Google Sync",
                                fontSize = 11.5.sp,
                                color = OneUiSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = OneUiSuccess,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "2-Way Sync",
                        fontSize = 11.5.sp,
                        color = OneUiSuccess,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Date Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            dates.forEach { date ->
                val isSelected = dateYearDayFormat.format(date) == selectedDayKey
                val dayKey = dateYearDayFormat.format(date)
                val hasEvents = tasks.any { dateYearDayFormat.format(Date(it.scheduledTime)) == dayKey }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            1.dp,
                            if (isSelected) OneUiPrimary else DarkBorder,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedDate = date }
                        .testTag("date_chip_${dayNumFormat.format(date)}"),
                    color = if (isSelected) OneUiPrimary else DarkSurfaceElevated,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 15.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = dayFormat.format(date).uppercase(Locale.getDefault()),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White.copy(alpha = 0.9f) else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dayNumFormat.format(date),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasEvents) {
                                        if (isSelected) Color(0xFFFFD700) else OneUiSecondary
                                    } else Color.Transparent
                                )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Selected Day Agenda Header + Unified Timeline Grid Toggle
        val selectedHeaderFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = selectedHeaderFormat.format(selectedDate),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${dayTasks.size} scheduled • ${conflictingTaskIds.size} conflicts",
                    fontSize = 12.sp,
                    color = if (conflictingTaskIds.isNotEmpty()) OneUiError else OneUiSecondary
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showHourlyTimelineGrid = !showHourlyTimelineGrid }
                    .testTag("toggle_calendar_timeline_view"),
                color = DarkSurfaceElevated,
                border = BorderStroke(1.dp, OneUiPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showHourlyTimelineGrid) Icons.Default.ViewTimeline else Icons.Default.ViewAgenda,
                        contentDescription = "Toggle Timeline Grid",
                        tint = OneUiPrimaryLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showHourlyTimelineGrid) "Timeline (6AM–11PM)" else "Agenda List",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = OneUiPrimaryLight
                    )
                }
            }
        }

        // Conflict Detection Alert Banner
        if (conflictingTaskIds.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                color = OneUiError.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, OneUiError.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = OneUiError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${conflictingTaskIds.size} overlapping events detected",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = OneUiError
                        )
                    }
                    val firstConflictId = conflictingTaskIds.firstOrNull()
                    if (firstConflictId != null && onAutoResolveConflict != null) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onAutoResolveConflict(firstConflictId) }
                                .testTag("calendar_auto_resolve_btn"),
                            color = OneUiError,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "Auto-Resolve Slot →",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!showHourlyTimelineGrid) {
            if (dayTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_empty_tasks_illustration),
                            contentDescription = null,
                            modifier = Modifier.size(130.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No events scheduled for this day",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Sim Google Sync' above to pull external Google Calendar events or add a task with a specific time.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = dayTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = { onToggleComplete(task.id) },
                            onSnooze = { taskToSnooze = task },
                            onDelete = { onDelete(task.id) },
                            onStartFocus = { onStartFocus(task.id, task.title) },
                            onClick = { taskToDetail = task },
                            hasConflict = conflictingTaskIds.contains(task.id),
                            onAutoResolveConflict = onAutoResolveConflict?.let { { it(task.id) } },
                            onSmartBreakdown = onSmartBreakdown?.let { { it(task.id) } },
                            onToggleSubtask = onToggleSubtask?.let { { subId -> it(task.id, subId) } }
                        )
                    }
                }
            }
        } else {
            // Unified Day Timeline Grid (6 AM to 11 PM)
            val hours = remember { (6..23).toList() }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .testTag("unified_day_timeline_grid"),
                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(hours, key = { "hour_slot_$it" }) { hour24 ->
                    val hourTasks = dayTasks.filter { task ->
                        val cal = Calendar.getInstance().apply { timeInMillis = task.scheduledTime }
                        cal.get(Calendar.HOUR_OF_DAY) == hour24
                    }
                    val hourLabel = when {
                        hour24 == 0 -> "12 AM"
                        hour24 < 12 -> "$hour24 AM"
                        hour24 == 12 -> "12 PM"
                        else -> "${hour24 - 12} PM"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = hourLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hourTasks.isNotEmpty()) OneUiSecondary else TextTertiary,
                            modifier = Modifier
                                .width(58.dp)
                                .padding(top = 10.dp)
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (hourTasks.isNotEmpty()) DarkSurface else DarkSurface.copy(alpha = 0.45f))
                                .border(
                                    1.dp,
                                    if (hourTasks.any { conflictingTaskIds.contains(it.id) }) OneUiError
                                    else if (hourTasks.isNotEmpty()) OneUiPrimary.copy(alpha = 0.4f)
                                    else DarkBorder.copy(alpha = 0.5f),
                                    RoundedCornerShape(16.dp)
                                )
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (hourTasks.isEmpty()) {
                                Text(
                                    text = "Free slot — Available for Deep Work",
                                    fontSize = 11.5.sp,
                                    color = TextTertiary
                                )
                            } else {
                                hourTasks.forEach { task ->
                                    TaskCard(
                                        task = task,
                                        onToggleComplete = { onToggleComplete(task.id) },
                                        onSnooze = { taskToSnooze = task },
                                        onDelete = { onDelete(task.id) },
                                        onStartFocus = { onStartFocus(task.id, task.title) },
                                        onClick = { taskToDetail = task },
                                        hasConflict = conflictingTaskIds.contains(task.id),
                                        onAutoResolveConflict = onAutoResolveConflict?.let { { it(task.id) } },
                                        onSmartBreakdown = onSmartBreakdown?.let { { it(task.id) } },
                                        onToggleSubtask = onToggleSubtask?.let { { subId -> it(task.id, subId) } }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    taskToSnooze?.let { task ->
        SnoozeOptionsBottomSheet(
            task = task,
            onDismiss = { taskToSnooze = null },
            onSnoozeSelected = { minutes ->
                onSnooze(task.id, minutes)
                taskToSnooze = null
            }
        )
    }

    taskToDetail?.let { currentDetail ->
        val liveTask = tasks.find { it.id == currentDetail.id } ?: currentDetail
        TaskDetailBottomSheet(
            task = liveTask,
            onDismiss = { taskToDetail = null },
            onToggleComplete = { onToggleComplete(liveTask.id) },
            onSnooze = { taskToSnooze = liveTask },
            onStartFocus = { mins ->
                if (onStartFocusWithDuration != null) {
                    onStartFocusWithDuration(liveTask.id, liveTask.title, mins)
                } else {
                    onStartFocus(liveTask.id, liveTask.title)
                }
            },
            onDelete = { onDelete(liveTask.id) },
            onSmartBreakdown = onSmartBreakdown?.let { { it(liveTask.id) } },
            onToggleSubtask = onToggleSubtask?.let { { subId -> it(liveTask.id, subId) } }
        )
    }
}
