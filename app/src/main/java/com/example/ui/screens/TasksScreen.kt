package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.TaskItem
import com.example.ui.TaskFilter
import com.example.ui.components.OneUiHeader
import com.example.ui.components.QuickAddTaskDialog
import com.example.ui.components.SnoozeOptionsBottomSheet
import com.example.ui.components.TaskCard
import com.example.ui.components.TaskDetailBottomSheet
import com.example.ui.components.UserProgressCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.OneUiPrimary
import com.example.ui.theme.OneUiPrimaryLight
import com.example.ui.theme.OneUiSecondary
import com.example.ui.theme.OneUiSuccess
import com.example.ui.theme.OneUiWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun TasksScreen(
    tasks: List<TaskItem>,
    selectedCategory: String,
    selectedFilter: TaskFilter,
    searchQuery: String,
    isSyncing: Boolean,
    isGoogleConnected: Boolean,
    onCategorySelected: (String) -> Unit,
    onFilterSelected: (TaskFilter) -> Unit,
    onSearchChanged: (String) -> Unit,
    onToggleComplete: (String) -> Unit,
    onSnooze: (String, Int) -> Unit,
    onDelete: (String) -> Unit,
    onStartFocus: (taskId: String, title: String) -> Unit,
    onSyncClick: () -> Unit,
    onSaveTask: (
        title: String,
        description: String,
        scheduledTime: Long,
        endTime: Long,
        priority: com.example.model.Priority,
        category: String,
        repeatRule: com.example.model.RepeatRule,
        reminderMinutes: Int
    ) -> Unit,
    userStats: com.example.model.UserRewardStats,
    onNavigateToRewards: () -> Unit,
    modifier: Modifier = Modifier,
    onTriggerStreakBlast: (() -> Unit)? = null
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToSnooze by remember { mutableStateOf<TaskItem?>(null) }
    var taskToDetail by remember { mutableStateOf<TaskItem?>(null) }
    var showGestureTip by remember { mutableStateOf(true) }
    var completedExpanded by remember { mutableStateOf(false) }

    val categories = listOf("All", "Work", "Personal", "Focus", "Health", "Meeting")

    val pendingTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }
    val totalCount = tasks.size
    val completedCount = completedTasks.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0.dp),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = OneUiPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 16.dp, end = 8.dp)
                    .size(60.dp)
                    .testTag("add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new task or event",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Signature Top Header
            OneUiHeader(
                title = "Today's Schedule",
                subtitle = "${pendingTasks.size} active tasks • Real-time Calendar Sync",
                isSyncing = isSyncing,
                isGoogleConnected = isGoogleConnected,
                onSyncClick = onSyncClick
            )

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Visual Hero Banner & Daily Progress Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp)),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Hero Vector Art
                            Image(
                                painter = painterResource(id = R.drawable.ic_hero_calendar_sync),
                                contentDescription = "Calendar and task sync illustration",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(115.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.FillWidth
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Daily Progress Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Daily Completion",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "$completedCount of $totalCount tasks completed (${(progress * 100).toInt()}%)",
                                        fontSize = 12.5.sp,
                                        color = TextSecondary
                                    )
                                }

                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (progress >= 1f) OneUiSuccess else OneUiSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Progress Bar
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = if (progress >= 1f) OneUiSuccess else OneUiPrimary,
                                trackColor = DarkSurfaceElevated
                            )
                        }
                    }
                }

                // 2. User XP Progress & Level Progression Card
                item {
                    UserProgressCard(
                        userStats = userStats,
                        onNavigateToRewards = onNavigateToRewards,
                        onTriggerStreakBlast = onTriggerStreakBlast
                    )
                }

                // 3. Search & Filter Section with generous spacing
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchChanged,
                            placeholder = { Text("Search tasks, tags, or meetings...", fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchChanged("") }) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_tasks_input"),
                            shape = RoundedCornerShape(18.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = OneUiPrimary,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurfaceElevated
                            )
                        )

                        // Filter Chips Row
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (filter in TaskFilter.values()) {
                                val isSelected = selectedFilter == filter
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onFilterSelected(filter) }
                                        .testTag("filter_chip_${filter.name}"),
                                    color = if (isSelected) OneUiPrimary else DarkSurfaceElevated,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = filter.title,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        // Category Chips Row
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (cat in categories) {
                                val isSelected = selectedCategory == cat
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onCategorySelected(cat) },
                                    color = if (isSelected) OneUiSecondary.copy(alpha = 0.2f) else DarkSurface,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) OneUiSecondary else TextTertiary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Intuitive Gesture Hint Banner
                        if (showGestureTip) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = OneUiPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Swipe,
                                        contentDescription = null,
                                        tint = OneUiPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Swipe Right to complete • Swipe Left to snooze • Tap for details",
                                        fontSize = 11.5.sp,
                                        color = OneUiPrimaryLight,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { showGestureTip = false },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Dismiss tip",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Pending / Active Tasks Section
                if (pendingTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Pending Tasks (${pendingTasks.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(
                        items = pendingTasks,
                        key = { it.id }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = { onToggleComplete(task.id) },
                            onSnooze = { taskToSnooze = task },
                            onDelete = { onDelete(task.id) },
                            onStartFocus = { onStartFocus(task.id, task.title) },
                            onClick = { taskToDetail = task }
                        )
                    }
                }

                // 4. Completed Tasks Section (Collapsible)
                if (completedTasks.isNotEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { completedExpanded = !completedExpanded },
                            color = DarkSurfaceElevated,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Completed (${completedTasks.size})",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = OneUiSuccess
                                    )
                                }
                                Icon(
                                    imageVector = if (completedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = TextSecondary
                                )
                            }
                        }
                    }

                    if (completedExpanded) {
                        items(
                            items = completedTasks,
                            key = { it.id }
                        ) { task ->
                            TaskCard(
                                task = task,
                                onToggleComplete = { onToggleComplete(task.id) },
                                onSnooze = { taskToSnooze = task },
                                onDelete = { onDelete(task.id) },
                                onStartFocus = { onStartFocus(task.id, task.title) },
                                onClick = { taskToDetail = task }
                            )
                        }
                    }
                }

                // 5. Empty State Illustration
                if (pendingTasks.isEmpty() && completedTasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_empty_tasks_illustration),
                                    contentDescription = "Empty state celebration illustration",
                                    modifier = Modifier.size(160.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "All Caught Up!",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "No pending tasks in this category.\nTap the '+' button below to schedule an event with 15m lead reminders & Google Calendar sync.",
                                    fontSize = 13.5.sp,
                                    color = TextSecondary,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Add Dialog
    if (showAddTaskDialog) {
        QuickAddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onSave = onSaveTask
        )
    }

    // Snooze Bottom Sheet
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

    // Detail Bottom Sheet
    taskToDetail?.let { task ->
        TaskDetailBottomSheet(
            task = task,
            onDismiss = { taskToDetail = null },
            onToggleComplete = { onToggleComplete(task.id) },
            onSnooze = { taskToSnooze = task },
            onStartFocus = { onStartFocus(task.id, task.title) },
            onDelete = { onDelete(task.id) }
        )
    }
}
