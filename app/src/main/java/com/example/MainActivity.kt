package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.SyncTaskViewModel
import com.example.ui.components.ParticleBlastOverlay
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.FocusScreen
import com.example.ui.screens.GoalsRewardsScreen
import com.example.ui.screens.SyncSettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.OneUiPrimary
import com.example.ui.theme.SyncTaskTheme
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

enum class AppScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TASKS("Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircleOutline),
    CALENDAR("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
    FOCUS("Focus", Icons.Filled.HourglassTop, Icons.Outlined.HourglassTop),
    GOALS("Rewards", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents),
    SYNC("Sync", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: SyncTaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SyncTaskTheme(darkTheme = true) {
                // Request Notification Permission on Android 13+
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val calGranted = (permissions[Manifest.permission.READ_CALENDAR] ?: false) &&
                            (permissions[Manifest.permission.WRITE_CALENDAR] ?: false)
                    if (calGranted) {
                        viewModel.triggerSync()
                    }
                }

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.READ_CALENDAR)
                    }
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.WRITE_CALENDAR)
                    }
                    if (permissionsToRequest.isNotEmpty()) {
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                }

                SyncTaskAppRoot(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SyncTaskAppRoot(viewModel: SyncTaskViewModel) {
    var currentScreen by remember { mutableStateOf(AppScreen.TASKS) }

    // Intercept back button when not on Tasks home screen
    BackHandler(enabled = currentScreen != AppScreen.TASKS) {
        currentScreen = AppScreen.TASKS
    }

    // Collect UI state
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val allTasks by viewModel.tasks.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isGoogleConnected by viewModel.isAuthConnected.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()
    val authEmail by viewModel.authEmail.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val conflictLogs by viewModel.conflictLogs.collectAsStateWithLifecycle()

    val timerStatus by viewModel.timerStatus.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingFocusSeconds.collectAsStateWithLifecycle()
    val totalSeconds by viewModel.totalFocusSeconds.collectAsStateWithLifecycle()
    val currentFocusTaskTitle by viewModel.currentFocusTaskTitle.collectAsStateWithLifecycle()
    val totalFocusMinutes by viewModel.totalFocusMinutes.collectAsStateWithLifecycle()
    val focusSessions by viewModel.focusSessions.collectAsStateWithLifecycle()

    // Goals & Rewards
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val userStats by viewModel.userStats.collectAsStateWithLifecycle()
    val rewards by viewModel.rewards.collectAsStateWithLifecycle()
    val celebrationMessage by viewModel.celebrationMessage.collectAsStateWithLifecycle()
    val blastEvent by viewModel.blastEvent.collectAsStateWithLifecycle()

    // Multi-Window & Adaptive Container: Checks available width
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideDisplay = maxWidth >= 600.dp

        if (isWideDisplay) {
            // Tablet, Desktop, or Wide Multi-Window: Side Navigation Rail
            Row(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
                NavigationRail(
                    containerColor = DarkSurface,
                    contentColor = TextSecondary,
                    modifier = Modifier.testTag("tablet_navigation_rail")
                ) {
                    AppScreen.values().forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title, fontSize = 11.sp) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                indicatorColor = OneUiPrimary.copy(alpha = 0.35f),
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            ),
                            modifier = Modifier.testTag("nav_rail_${screen.name.lowercase()}")
                        )
                    }
                }

                Crossfade(targetState = currentScreen, label = "screen_crossfade") { screen ->
                    when (screen) {
                        AppScreen.TASKS -> TasksScreen(
                            tasks = filteredTasks,
                            selectedCategory = selectedCategory,
                            selectedFilter = selectedFilter,
                            searchQuery = searchQuery,
                            isSyncing = isSyncing,
                            isGoogleConnected = isGoogleConnected,
                            onCategorySelected = viewModel::setSelectedCategory,
                            onFilterSelected = viewModel::setSelectedFilter,
                            onSearchChanged = viewModel::setSearchQuery,
                            onToggleComplete = viewModel::toggleComplete,
                            onSnooze = viewModel::snoozeTask,
                            onDelete = viewModel::deleteTask,
                            onStartFocus = { id, title ->
                                viewModel.startFocusSession(25, id, title)
                                currentScreen = AppScreen.FOCUS
                            },
                            onSyncClick = viewModel::triggerSync,
                            onSaveTask = { title, desc, start, end, prio, cat, rule, reminder ->
                                viewModel.saveTask(null, title, desc, start, end, prio, cat, rule, reminder)
                            },
                            userStats = userStats,
                            onNavigateToRewards = { currentScreen = AppScreen.GOALS },
                            onTriggerStreakBlast = { viewModel.triggerTestBlast(com.example.model.BlastType.STREAK_GOAL) }
                        )
                        AppScreen.CALENDAR -> CalendarScreen(
                            tasks = allTasks,
                            isSyncing = isSyncing,
                            isGoogleConnected = isGoogleConnected,
                            onSyncClick = viewModel::triggerSync,
                            onToggleComplete = viewModel::toggleComplete,
                            onSnooze = viewModel::snoozeTask,
                            onDelete = viewModel::deleteTask,
                            onStartFocus = { id, title ->
                                viewModel.startFocusSession(25, id, title)
                                currentScreen = AppScreen.FOCUS
                            }
                        )
                        AppScreen.FOCUS -> FocusScreen(
                            timerStatus = timerStatus,
                            remainingSeconds = remainingSeconds,
                            totalSeconds = totalSeconds,
                            currentTaskTitle = currentFocusTaskTitle,
                            totalFocusMinutes = totalFocusMinutes,
                            focusSessions = focusSessions,
                            onStartTimer = { mins, title -> viewModel.startFocusSession(mins, taskTitle = title) },
                            onPauseTimer = viewModel::pauseFocusSession,
                            onResumeTimer = viewModel::resumeFocusSession,
                            onStopTimer = viewModel::stopFocusSession
                        )
                        AppScreen.GOALS -> GoalsRewardsScreen(
                            userStats = userStats,
                            goals = goals,
                            rewards = rewards,
                            celebrationMessage = celebrationMessage,
                            onClearCelebration = viewModel::clearCelebration,
                            onRedeemReward = viewModel::redeemReward,
                            onCreateGoal = viewModel::createGoal,
                            onCreateCustomReward = viewModel::createCustomReward,
                            onDeleteGoal = viewModel::deleteGoal,
                            onTriggerCelebrationBlast = viewModel::triggerTestBlast
                        )
                        AppScreen.SYNC -> SyncSettingsScreen(
                            authEmail = authEmail,
                            isGoogleConnected = isGoogleConnected,
                            isSyncing = isSyncing,
                            syncMessage = syncMessage,
                            lastSyncTime = lastSyncTime,
                            conflictLogs = conflictLogs,
                            onToggleAccountConnection = viewModel::toggleAccountConnection,
                            onTriggerSync = viewModel::triggerSync
                        )
                    }
                }
            }
        } else {
            // Handheld / Compact Portrait Layout: Bottom Navigation Bar
            Scaffold(
                containerColor = DarkBackground,
                bottomBar = {
                    NavigationBar(
                        containerColor = DarkSurface,
                        contentColor = TextSecondary,
                        modifier = Modifier.testTag("bottom_navigation_bar"),
                        tonalElevation = 8.dp
                    ) {
                        AppScreen.values().forEach { screen ->
                            val isSelected = currentScreen == screen
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { currentScreen = screen },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = { Text(screen.title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = OneUiPrimary.copy(alpha = 0.35f),
                                    unselectedIconColor = TextTertiary,
                                    unselectedTextColor = TextTertiary
                                ),
                                modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Crossfade(
                    targetState = currentScreen,
                    modifier = Modifier.padding(paddingValues),
                    label = "screen_crossfade"
                ) { screen ->
                    when (screen) {
                        AppScreen.TASKS -> TasksScreen(
                            tasks = filteredTasks,
                            selectedCategory = selectedCategory,
                            selectedFilter = selectedFilter,
                            searchQuery = searchQuery,
                            isSyncing = isSyncing,
                            isGoogleConnected = isGoogleConnected,
                            onCategorySelected = viewModel::setSelectedCategory,
                            onFilterSelected = viewModel::setSelectedFilter,
                            onSearchChanged = viewModel::setSearchQuery,
                            onToggleComplete = viewModel::toggleComplete,
                            onSnooze = viewModel::snoozeTask,
                            onDelete = viewModel::deleteTask,
                            onStartFocus = { id, title ->
                                viewModel.startFocusSession(25, id, title)
                                currentScreen = AppScreen.FOCUS
                            },
                            onSyncClick = viewModel::triggerSync,
                            onSaveTask = { title, desc, start, end, prio, cat, rule, reminder ->
                                viewModel.saveTask(null, title, desc, start, end, prio, cat, rule, reminder)
                            },
                            userStats = userStats,
                            onNavigateToRewards = { currentScreen = AppScreen.GOALS },
                            onTriggerStreakBlast = { viewModel.triggerTestBlast(com.example.model.BlastType.STREAK_GOAL) }
                        )
                        AppScreen.CALENDAR -> CalendarScreen(
                            tasks = allTasks,
                            isSyncing = isSyncing,
                            isGoogleConnected = isGoogleConnected,
                            onSyncClick = viewModel::triggerSync,
                            onToggleComplete = viewModel::toggleComplete,
                            onSnooze = viewModel::snoozeTask,
                            onDelete = viewModel::deleteTask,
                            onStartFocus = { id, title ->
                                viewModel.startFocusSession(25, id, title)
                                currentScreen = AppScreen.FOCUS
                            }
                        )
                        AppScreen.FOCUS -> FocusScreen(
                            timerStatus = timerStatus,
                            remainingSeconds = remainingSeconds,
                            totalSeconds = totalSeconds,
                            currentTaskTitle = currentFocusTaskTitle,
                            totalFocusMinutes = totalFocusMinutes,
                            focusSessions = focusSessions,
                            onStartTimer = { mins, title -> viewModel.startFocusSession(mins, taskTitle = title) },
                            onPauseTimer = viewModel::pauseFocusSession,
                            onResumeTimer = viewModel::resumeFocusSession,
                            onStopTimer = viewModel::stopFocusSession
                        )
                        AppScreen.GOALS -> GoalsRewardsScreen(
                            userStats = userStats,
                            goals = goals,
                            rewards = rewards,
                            celebrationMessage = celebrationMessage,
                            onClearCelebration = viewModel::clearCelebration,
                            onRedeemReward = viewModel::redeemReward,
                            onCreateGoal = viewModel::createGoal,
                            onCreateCustomReward = viewModel::createCustomReward,
                            onDeleteGoal = viewModel::deleteGoal,
                            onTriggerCelebrationBlast = viewModel::triggerTestBlast
                        )
                        AppScreen.SYNC -> SyncSettingsScreen(
                            authEmail = authEmail,
                            isGoogleConnected = isGoogleConnected,
                            isSyncing = isSyncing,
                            syncMessage = syncMessage,
                            lastSyncTime = lastSyncTime,
                            conflictLogs = conflictLogs,
                            onToggleAccountConnection = viewModel::toggleAccountConnection,
                            onTriggerSync = viewModel::triggerSync
                        )
                    }
                }
            }
        }

        // 3. Celebratory Particle Blast Overlay
        ParticleBlastOverlay(
            blastEvent = blastEvent,
            onDismiss = viewModel::clearBlast
        )
    }
}
