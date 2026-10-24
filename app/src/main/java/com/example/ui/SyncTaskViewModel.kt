package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SyncTaskApp
import com.example.data.auth.GoogleAuthManager
import com.example.model.ConflictLog
import com.example.model.FocusSession
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.TaskItem
import com.example.service.FocusTimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TaskFilter(val title: String) {
    TODAY("Today"),
    UPCOMING("Upcoming"),
    HIGH_PRIORITY("High Priority"),
    COMPLETED("Completed"),
    ALL("All Tasks")
}

class SyncTaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as SyncTaskApp).repository
    private val authManager = GoogleAuthManager.getInstance(application)

    val tasks: StateFlow<List<TaskItem>> = repository.allTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusSessions: StateFlow<List<FocusSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFocusMinutes: StateFlow<Int> = repository.totalFocusMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val conflictLogs: StateFlow<List<ConflictLog>> = repository.conflictLogs
    val isSyncing: StateFlow<Boolean> = repository.isSyncing
    val syncMessage: StateFlow<String?> = repository.syncMessage

    // Goals & Rewards System
    val goals: StateFlow<List<com.example.model.GoalItem>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStats: StateFlow<com.example.model.UserRewardStats> = repository.userStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.model.UserRewardStats())

    val rewards: StateFlow<List<com.example.model.RewardItem>> = repository.allRewards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val celebrationMessage: StateFlow<String?> = repository.celebrationEvent

    val authEmail: StateFlow<String> = authManager.userEmail
    val isAuthConnected: StateFlow<Boolean> = authManager.isConnected
    val lastSyncTime: StateFlow<Long> = authManager.lastSyncTime

    // Focus Timer State delegated from Foreground Service
    val timerStatus: StateFlow<FocusTimerService.TimerStatus> = FocusTimerService.timerStatus
    val remainingFocusSeconds: StateFlow<Int> = FocusTimerService.remainingSeconds
    val totalFocusSeconds: StateFlow<Int> = FocusTimerService.totalSeconds
    val currentFocusTaskTitle: StateFlow<String> = FocusTimerService.currentTaskTitle

    // Filter & Search states
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaskFilter.TODAY)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Combined filtered tasks
    val filteredTasks: StateFlow<List<TaskItem>> = combine(
        tasks,
        _selectedCategory,
        _selectedFilter,
        _searchQuery
    ) { allTasks, category, filter, query ->
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        val endOfToday = calendar.timeInMillis

        allTasks.filter { task ->
            // Category filter
            val matchesCategory = category == "All" || task.category.equals(category, ignoreCase = true)

            // Status filter
            val matchesFilter = when (filter) {
                TaskFilter.TODAY -> !task.isCompleted && task.scheduledTime <= endOfToday
                TaskFilter.UPCOMING -> !task.isCompleted && task.scheduledTime > endOfToday
                TaskFilter.HIGH_PRIORITY -> !task.isCompleted && (task.priority == Priority.HIGH || task.priority == Priority.URGENT)
                TaskFilter.COMPLETED -> task.isCompleted
                TaskFilter.ALL -> true
            }

            // Search query
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true) ||
                    task.category.contains(query, ignoreCase = true)

            matchesCategory && matchesFilter && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun setSelectedFilter(filter: TaskFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun saveTask(
        id: String?,
        title: String,
        description: String,
        scheduledTime: Long,
        endTime: Long,
        priority: Priority,
        category: String,
        repeatRule: RepeatRule,
        reminderMinutes: Int = 15
    ) {
        viewModelScope.launch {
            val task = TaskItem(
                id = id ?: java.util.UUID.randomUUID().toString(),
                title = title.trim(),
                description = description.trim(),
                scheduledTime = scheduledTime,
                endTime = endTime,
                priority = priority,
                category = category,
                repeatRule = repeatRule,
                reminderMinutesBefore = reminderMinutes
            )
            repository.saveTask(task)
        }
    }

    fun toggleComplete(taskId: String) {
        viewModelScope.launch {
            repository.toggleTaskComplete(taskId)
        }
    }

    fun snoozeTask(taskId: String, minutes: Int = 10) {
        viewModelScope.launch {
            repository.snoozeTask(taskId, minutes)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            repository.syncWithGoogleCalendar()
        }
    }

    fun toggleAccountConnection() {
        val current = isAuthConnected.value
        authManager.setConnected(!current)
        if (!current) {
            triggerSync()
        }
    }

    // Focus Timer Actions
    fun startFocusSession(minutes: Int, taskId: String? = null, taskTitle: String? = null, tag: String = "Deep Work") {
        FocusTimerService.start(getApplication(), minutes, taskId, taskTitle, tag)
    }

    fun pauseFocusSession() {
        FocusTimerService.pause(getApplication())
    }

    fun resumeFocusSession() {
        FocusTimerService.resume(getApplication())
    }

    fun stopFocusSession() {
        FocusTimerService.stop(getApplication())
    }

    // Goals & Rewards Actions
    fun redeemReward(rewardId: String) {
        viewModelScope.launch {
            repository.redeemReward(rewardId)
        }
    }

    fun createGoal(title: String, description: String, targetCount: Int, category: String, rewardPoints: Int) {
        viewModelScope.launch {
            repository.createGoal(title, description, targetCount, category, rewardPoints)
        }
    }

    fun createCustomReward(title: String, description: String, costPoints: Int, iconCategory: String) {
        viewModelScope.launch {
            repository.createCustomReward(title, description, costPoints, iconCategory)
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
        }
    }

    fun clearCelebration() {
        repository.clearCelebration()
    }
}
