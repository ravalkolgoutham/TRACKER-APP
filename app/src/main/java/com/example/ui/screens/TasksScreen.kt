package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.ViewTimeline
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.SyncQueueItem
import com.example.model.SyncStatus
import com.example.model.TaskItem
import com.example.model.TaskViewMode
import com.example.model.UserRewardStats
import com.example.ui.NaturalLanguageTaskParser
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

@OptIn(ExperimentalLayoutApi::class)
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
        priority: Priority,
        category: String,
        repeatRule: RepeatRule,
        reminderMinutes: Int
    ) -> Unit,
    userStats: UserRewardStats,
    onNavigateToRewards: () -> Unit,
    modifier: Modifier = Modifier,
    onTriggerStreakBlast: (() -> Unit)? = null,
    syncQueueLogs: List<SyncQueueItem> = emptyList(),
    onQuickAddNaturalLanguage: ((String) -> Unit)? = null,
    onSmartBreakdown: ((String) -> Unit)? = null,
    onToggleSubtask: ((String, String) -> Unit)? = null,
    onAutoResolveConflict: ((String) -> Unit)? = null,
    onStartFocusWithDuration: ((taskId: String, title: String, durationMinutes: Int) -> Unit)? = null
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var taskToSnooze by remember { mutableStateOf<TaskItem?>(null) }
    var taskToDetail by remember { mutableStateOf<TaskItem?>(null) }
    var showGestureTip by remember { mutableStateOf(true) }
    var completedExpanded by remember { mutableStateOf(false) }
    var quickCommandText by remember { mutableStateOf("") }
    var selectedViewMode by remember { mutableStateOf(TaskViewMode.LIST) }

    val categories = listOf("All", "Work", "Personal", "Focus", "Health", "Meeting", "Finance")

    val pendingTasks = tasks.filter { !it.isCompleted }
    val completedTasks = tasks.filter { it.isCompleted }
    val totalCount = tasks.size
    val completedCount = completedTasks.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount.toFloat() else 0f
    val pendingQueueCount = tasks.count { it.syncStatus != SyncStatus.SYNCED }

    // Detect overlapping time blocks among active tasks
    val conflictingTaskIds = remember(pendingTasks) {
        val ids = mutableSetOf<String>()
        for (i in pendingTasks.indices) {
            for (j in i + 1 until pendingTasks.size) {
                val a = pendingTasks[i]
                val b = pendingTasks[j]
                if (a.scheduledTime < b.endTime && b.scheduledTime < a.endTime) {
                    ids.add(a.id)
                    ids.add(b.id)
                }
            }
        }
        ids
    }

    val liveNlpPreview = remember(quickCommandText) {
        if (quickCommandText.isBlank()) null else NaturalLanguageTaskParser.parse(quickCommandText)
    }

    com.example.ui.theme.AeroGlassBackdrop(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp),
            floatingActionButton = {
                com.example.ui.theme.AeroSquircleButton(
                    onClick = { showAddTaskDialog = true },
                    isActive = true,
                    cornerRadius = 18.dp,
                    modifier = Modifier
                        .padding(bottom = 16.dp, end = 8.dp)
                        .size(58.dp)
                        .testTag("add_task_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add new task or event",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Signature Top Header with Live Network & Sync Log Drawer
                OneUiHeader(
                    title = "Today's Schedule",
                    subtitle = "${pendingTasks.size} active tasks • Real-time Calendar Sync",
                    isSyncing = isSyncing,
                    isGoogleConnected = isGoogleConnected,
                    onSyncClick = onSyncClick,
                    pendingQueueCount = pendingQueueCount,
                    syncQueueLogs = syncQueueLogs
                )

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. 3D Stacked Frosted Glass Hero Showcase (inspired by reference middle screen)
                    item {
                        val activePrimary = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        val activeSecondary = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            // Lower 3D stacked glass layer 2
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.86f)
                                    .padding(top = 22.dp)
                                    .height(185.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(activeSecondary.copy(alpha = 0.16f))
                                    .border(1.dp, activePrimary.copy(alpha = 0.28f), RoundedCornerShape(26.dp))
                            )
                            // Middle 3D stacked glass layer 1
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.93f)
                                    .padding(top = 11.dp)
                                    .height(190.dp)
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(com.example.ui.theme.glassSurfaceBrush(0.75f))
                                    .border(1.dp, activePrimary.copy(alpha = 0.42f), RoundedCornerShape(26.dp))
                            )
                            // Main Top Frosted Glass Hero Card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(26.dp))
                                    .background(com.example.ui.theme.glassSurfaceBrush(1.05f))
                                    .border(1.dp, com.example.ui.theme.glassSpecularBorderBrush(activePrimary), RoundedCornerShape(26.dp))
                                    .clickable { showAddTaskDialog = true }
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(112.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    colors = listOf(
                                                        activeSecondary.copy(alpha = 0.28f),
                                                        DarkSurfaceElevated.copy(alpha = 0.85f),
                                                        activePrimary.copy(alpha = 0.25f)
                                                    )
                                                )
                                            )
                                            .border(
                                                1.dp,
                                                com.example.ui.theme.glassSpecularBorderBrush(activePrimary),
                                                RoundedCornerShape(18.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_hero_calendar_sync),
                                            contentDescription = "Calendar and task sync illustration",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(112.dp)
                                                .clip(RoundedCornerShape(18.dp)),
                                            contentScale = ContentScale.FillBounds
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Daily Velocity & Sync",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TextPrimary
                                            )
                                            Text(
                                                text = "$completedCount of $totalCount completed • Tap to quick-schedule",
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }

                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (progress >= 1f) OneUiSuccess else activePrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = if (progress >= 1f) OneUiSuccess else activePrimary,
                                        trackColor = DarkSurface
                                    )
                                }
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

                // 3. Natural Language Quick-Add Command Bar
                item {
                    val activePrimary = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(com.example.ui.theme.glassSurfaceBrush(0.95f))
                            .border(1.dp, com.example.ui.theme.glassSpecularBorderBrush(activePrimary), RoundedCornerShape(22.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = activePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Natural Language Command Bar",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activePrimary
                                    )
                                }
                                Text(
                                    text = "Try: tomorrow at 5pm for 45m !high #work",
                                    fontSize = 10.5.sp,
                                    color = TextTertiary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val submitQuickCommand = {
                                if (quickCommandText.isNotBlank()) {
                                    if (onQuickAddNaturalLanguage != null) {
                                        onQuickAddNaturalLanguage(quickCommandText)
                                    } else {
                                        val parsed = NaturalLanguageTaskParser.parse(quickCommandText)
                                        onSaveTask(
                                            parsed.cleanTitle,
                                            "Quick-Added via Command Bar",
                                            parsed.scheduledTime,
                                            parsed.scheduledTime + parsed.durationMinutes * 60_000L,
                                            parsed.priority,
                                            parsed.category,
                                            RepeatRule.NONE,
                                            15
                                        )
                                    }
                                    quickCommandText = ""
                                }
                            }

                            OutlinedTextField(
                                value = quickCommandText,
                                onValueChange = { quickCommandText = it },
                                placeholder = {
                                    Text(
                                        "e.g. Submit architecture review tomorrow at 5pm for 45m !urgent #work",
                                        fontSize = 13.sp,
                                        color = TextTertiary
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = submitQuickCommand,
                                        modifier = Modifier.testTag("quick_command_submit")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Quick Add Task",
                                            tint = if (quickCommandText.isNotBlank()) OneUiPrimary else TextTertiary
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = { submitQuickCommand() }
                                ),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quick_command_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = OneUiSecondary,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = DarkSurfaceElevated,
                                    unfocusedContainerColor = DarkSurfaceElevated
                                )
                            )

                            AnimatedVisibility(visible = liveNlpPreview != null) {
                                liveNlpPreview?.let { preview ->
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = OneUiPrimary.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = "Title: ${preview.cleanTitle}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = OneUiPrimaryLight,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            preview.detectedTokens.forEach { token ->
                                                Surface(
                                                    color = OneUiSecondary.copy(alpha = 0.18f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = token,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = OneUiSecondary,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Search, View Mode Switcher & Filter Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Search Bar + View Switcher Row
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

                        // View Mode Switcher: List | Kanban Board | Timeline
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TaskViewMode.values().forEach { mode ->
                                val isSelected = selectedViewMode == mode
                                val icon = when (mode) {
                                    TaskViewMode.LIST -> Icons.Default.ViewList
                                    TaskViewMode.KANBAN -> Icons.Default.ViewKanban
                                    TaskViewMode.TIMELINE -> Icons.Default.ViewTimeline
                                }
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { selectedViewMode = mode }
                                        .testTag("view_mode_${mode.name.lowercase()}"),
                                    color = if (isSelected) OneUiPrimary.copy(alpha = 0.22f) else DarkSurfaceElevated,
                                    border = BorderStroke(1.dp, if (isSelected) OneUiPrimary else DarkBorder),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = mode.label,
                                            tint = if (isSelected) OneUiPrimaryLight else TextSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = mode.label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        // Filter Squircle Pills Row
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (filter in TaskFilter.values()) {
                                val isSelected = selectedFilter == filter
                                com.example.ui.theme.AeroSquircleButton(
                                    onClick = { onFilterSelected(filter) },
                                    isActive = isSelected,
                                    cornerRadius = 14.dp,
                                    modifier = Modifier.testTag("filter_chip_${filter.name}")
                                ) {
                                    Text(
                                        text = filter.title,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                                    )
                                }
                            }
                        }

                        // 3D Glass Category Squircles Row (matching reference middle screen "All" + category squircles)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (cat in categories) {
                                val isSelected = selectedCategory == cat
                                com.example.ui.theme.AeroSquircleButton(
                                    onClick = { onCategorySelected(cat) },
                                    isActive = isSelected,
                                    cornerRadius = 14.dp
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
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
                                        text = "Swipe Right to complete • Swipe Left to snooze • Tap ⚡ Breakdown for AI subtasks",
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

                // 5. Render Tasks by Selected View Mode
                when (selectedViewMode) {
                    TaskViewMode.LIST -> {
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
                                    onClick = { taskToDetail = task },
                                    hasConflict = conflictingTaskIds.contains(task.id),
                                    onAutoResolveConflict = onAutoResolveConflict?.let { { it(task.id) } },
                                    onSmartBreakdown = onSmartBreakdown?.let { { it(task.id) } },
                                    onToggleSubtask = onToggleSubtask?.let { { subId -> it(task.id, subId) } }
                                )
                            }
                        }

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
                                        Text(
                                            text = "Completed (${completedTasks.size})",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = OneUiSuccess
                                        )
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
                                        onClick = { taskToDetail = task },
                                        onToggleSubtask = onToggleSubtask?.let { { subId -> it(task.id, subId) } }
                                    )
                                }
                            }
                        }
                    }

                    TaskViewMode.KANBAN -> {
                        val todoColumn = pendingTasks.filter { it.priority == Priority.LOW || it.priority == Priority.MEDIUM }
                        val urgentColumn = pendingTasks.filter { it.priority == Priority.HIGH || it.priority == Priority.URGENT }

                        item {
                            KanbanSectionHeader("🔥 High & Urgent Focus", urgentColumn.size, OneUiWarning)
                        }
                        items(urgentColumn, key = { "kanban_urg_${it.id}" }) { task ->
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

                        item {
                            KanbanSectionHeader("📋 Scheduled To-Do", todoColumn.size, OneUiSecondary)
                        }
                        items(todoColumn, key = { "kanban_todo_${it.id}" }) { task ->
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

                        item {
                            KanbanSectionHeader("✓ Done", completedTasks.size, OneUiSuccess)
                        }
                        items(completedTasks, key = { "kanban_done_${it.id}" }) { task ->
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

                    TaskViewMode.TIMELINE -> {
                        val chronological = tasks.sortedBy { it.scheduledTime }
                        val timeFmt = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
                        items(chronological, key = { "timeline_${it.id}" }) { task ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (conflictingTaskIds.contains(task.id)) OneUiError else OneUiSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${timeFmt.format(Date(task.scheduledTime))} (${task.durationMinutes}m block)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (conflictingTaskIds.contains(task.id)) OneUiError else OneUiSecondary
                                    )
                                }
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

                // 6. Empty State Illustration
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
                                    text = "No pending tasks in this category.\nUse the Natural Language Command Bar above or tap '+' below to schedule an event with 3D wheel time picking & Google Calendar sync.",
                                    fontSize = 13.5.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
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

    // Detail Bottom Sheet (Interactive Glass Inspector Pop-up)
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

@Composable
private fun KanbanSectionHeader(title: String, count: Int, accentColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Surface(
                color = accentColor.copy(alpha = 0.25f),
                shape = CircleShape
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
}
