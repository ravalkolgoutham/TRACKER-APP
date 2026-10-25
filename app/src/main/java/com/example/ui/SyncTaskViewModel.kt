package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SyncTaskApp
import com.example.data.auth.GoogleAuthManager
import com.example.model.BlastEvent
import com.example.model.BlastType
import com.example.model.ConflictLog
import com.example.model.FocusSession
import com.example.model.GoalItem
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.RewardItem
import com.example.model.SyncQueueItem
import com.example.model.TaskItem
import com.example.model.UserRewardStats
import com.example.service.FocusTimerService
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TaskFilter(val title: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    HIGH_PRIORITY("High Priority"),
    COMPLETED("Completed")
}

data class NaturalLanguagePreview(
    val cleanTitle: String,
    val priority: Priority,
    val category: String,
    val scheduledTime: Long,
    val durationMinutes: Int,
    val detectedTokens: List<String>
)

object NaturalLanguageTaskParser {
    fun parse(raw: String, nowMillis: Long = System.currentTimeMillis()): NaturalLanguagePreview {
        var working = raw
        val tokens = mutableListOf<String>()

        var priority = Priority.MEDIUM
        val prioMap = listOf(
            Regex("""(?i)(?:!urgent|\bp1)\b""") to (Priority.URGENT to "⚡ Urgent"),
            Regex("""(?i)(?:!high|\bp2)\b""") to (Priority.HIGH to "🔥 High"),
            Regex("""(?i)(?:!med|!medium|\bp3)\b""") to (Priority.MEDIUM to "Medium"),
            Regex("""(?i)(?:!low|\bp4)\b""") to (Priority.LOW to "Low")
        )
        for ((regex, pair) in prioMap) {
            if (regex.containsMatchIn(working)) {
                priority = pair.first
                tokens.add(pair.second)
                working = regex.replace(working, " ")
                break
            }
        }

        var category = "Work"
        val catMap = listOf(
            Regex("""(?i)#work\b""") to "Work",
            Regex("""(?i)#(study|focus)\b""") to "Focus",
            Regex("""(?i)#personal\b""") to "Personal",
            Regex("""(?i)#health\b""") to "Health",
            Regex("""(?i)#meeting\b""") to "Meeting"
        )
        for ((regex, catName) in catMap) {
            if (regex.containsMatchIn(working)) {
                category = catName
                tokens.add("#$catName")
                working = regex.replace(working, " ")
                break
            }
        }

        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis + 3600_000L
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val tomorrowRegex = Regex("""(?i)\btomorrow\b""")
        val todayRegex = Regex("""(?i)\btoday\b""")
        val nextMonRegex = Regex("""(?i)\bnext\s+monday\b""")
        when {
            tomorrowRegex.containsMatchIn(working) -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                tokens.add("📅 Tomorrow")
                working = tomorrowRegex.replace(working, " ")
            }
            nextMonRegex.containsMatchIn(working) -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
                tokens.add("📅 Next Mon")
                working = nextMonRegex.replace(working, " ")
            }
            todayRegex.containsMatchIn(working) -> {
                tokens.add("📅 Today")
                working = todayRegex.replace(working, " ")
            }
        }

        val timeRegex = Regex("""(?i)(?:\bat\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b|\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b)""")
        timeRegex.find(working)?.let { match ->
            val hrStr = match.groupValues[1].ifEmpty { match.groupValues[4] }
            val minStr = match.groupValues[2].ifEmpty { match.groupValues[5] }
            val ampmStr = match.groupValues[3].ifEmpty { match.groupValues[6] }
            val hrRaw = hrStr.toIntOrNull() ?: 9
            val minRaw = minStr.toIntOrNull() ?: 0
            val ampm = ampmStr.lowercase(Locale.getDefault())
            val hr24 = when {
                ampm == "pm" && hrRaw < 12 -> hrRaw + 12
                ampm == "am" && hrRaw == 12 -> 0
                else -> hrRaw.coerceIn(0, 23)
            }
            cal.set(Calendar.HOUR_OF_DAY, hr24)
            cal.set(Calendar.MINUTE, minRaw.coerceIn(0, 59))
            tokens.add("⏰ ${match.value.replaceFirst("at ", "", ignoreCase = true).trim()}")
            working = working.replaceRange(match.range, " ")
        }

        var durationMins = 60
        val durRegex = Regex("""(?i)(?:\bfor\s+)?\b(\d{1,3})\s*(m|min|mins|h|hr|hrs)\b""")
        durRegex.find(working)?.let { match ->
            val num = match.groupValues[1].toIntOrNull() ?: 60
            val unit = match.groupValues[2].lowercase(Locale.getDefault())
            durationMins = if (unit.startsWith("h")) num * 60 else num
            tokens.add("⏱ ${durationMins}m")
            working = working.replaceRange(match.range, " ")
        }

        val cleanedTitle = working.replace(Regex("""\s+"""), " ").trim()
        return NaturalLanguagePreview(
            cleanTitle = cleanedTitle.ifBlank { raw.trim() },
            priority = priority,
            category = category,
            scheduledTime = cal.timeInMillis,
            durationMinutes = durationMins,
            detectedTokens = tokens
        )
    }
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
    val syncQueueLogs: StateFlow<List<SyncQueueItem>> = repository.syncQueueLogs
    val isSyncing: StateFlow<Boolean> = repository.isSyncing
    val syncMessage: StateFlow<String?> = repository.syncMessage

    val goals: StateFlow<List<GoalItem>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userStats: StateFlow<UserRewardStats> = repository.userStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserRewardStats())

    val rewards: StateFlow<List<RewardItem>> = repository.allRewards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val celebrationMessage: StateFlow<String?> = repository.celebrationEvent
    val blastEvent: StateFlow<BlastEvent?> = repository.blastEvent

    val authEmail: StateFlow<String> = authManager.userEmail
    val isAuthConnected: StateFlow<Boolean> = authManager.isConnected
    val lastSyncTime: StateFlow<Long> = authManager.lastSyncTime

    val timerStatus: StateFlow<FocusTimerService.TimerStatus> = FocusTimerService.timerStatus
    val remainingFocusSeconds: StateFlow<Int> = FocusTimerService.remainingSeconds
    val totalFocusSeconds: StateFlow<Int> = FocusTimerService.totalSeconds
    val currentFocusTaskTitle: StateFlow<String> = FocusTimerService.currentTaskTitle

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedFilter: StateFlow<TaskFilter> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

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
            val matchesCategory = category == "All" || task.category.equals(category, ignoreCase = true)

            val matchesFilter = when (filter) {
                TaskFilter.TODAY -> !task.isCompleted && task.scheduledTime <= endOfToday
                TaskFilter.UPCOMING -> !task.isCompleted && task.scheduledTime > endOfToday
                TaskFilter.OVERDUE -> !task.isCompleted && task.endTime < now
                TaskFilter.HIGH_PRIORITY -> !task.isCompleted && (task.priority == Priority.HIGH || task.priority == Priority.URGENT)
                TaskFilter.COMPLETED -> task.isCompleted
                TaskFilter.ALL -> true
            }

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
                id = id ?: UUID.randomUUID().toString(),
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

    fun quickAddFromNaturalLanguage(rawInput: String) {
        if (rawInput.isBlank()) return
        val parsed = NaturalLanguageTaskParser.parse(rawInput)
        val durationMs = parsed.durationMinutes * 60 * 1000L
        saveTask(
            id = null,
            title = parsed.cleanTitle,
            description = "Quick-Added via Command Bar",
            scheduledTime = parsed.scheduledTime,
            endTime = parsed.scheduledTime + durationMs,
            priority = parsed.priority,
            category = parsed.category,
            repeatRule = RepeatRule.NONE,
            reminderMinutes = 15
        )
    }

    fun generateSmartBreakdown(taskId: String) {
        viewModelScope.launch {
            repository.generateSmartBreakdown(taskId)
        }
    }

    fun toggleSubtask(taskId: String, subtaskId: String) {
        viewModelScope.launch {
            repository.toggleSubtask(taskId, subtaskId)
        }
    }

    fun autoResolveConflict(taskId: String) {
        viewModelScope.launch {
            repository.autoResolveScheduleConflict(taskId)
        }
    }

    fun runSimulatedGoogleSync() {
        viewModelScope.launch {
            repository.runSimulatedGoogleCalendarSync()
        }
    }

    fun purgeDuplicatesAndHolidays() {
        viewModelScope.launch {
            repository.purgeDuplicatesAndHolidayTasks()
        }
    }

    fun buyStreakFreezeShield() {
        viewModelScope.launch {
            repository.buyStreakFreezeShield()
        }
    }

    fun exportBackupJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            onResult(json)
        }
    }

    fun importBackupJson(json: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.importBackupJson(json)
            onResult(ok)
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

    fun clearBlast() {
        repository.clearBlast()
    }

    fun triggerTestBlast(type: BlastType = BlastType.STREAK_GOAL) {
        val stats = userStats.value
        val event = when (type) {
            BlastType.STREAK_GOAL -> BlastEvent(
                title = "DAILY STREAK GOAL!",
                subtitle = "${stats.displayStreakDays}-day streak milestone accomplished!",
                badgeText = "${stats.displayStreakDays} Days",
                type = BlastType.STREAK_GOAL
            )
            BlastType.LEVEL_UP -> BlastEvent(
                title = "LEVEL UP!",
                subtitle = "Promoted to Level ${stats.currentLevel + 1}! Master rank unlocked!",
                badgeText = "Level ${stats.currentLevel + 1}",
                type = BlastType.LEVEL_UP
            )
            BlastType.GOAL_COMPLETED -> BlastEvent(
                title = "GOAL ACHIEVED!",
                subtitle = "Completed milestone target! +200 Bonus Points",
                badgeText = "+200 Pts",
                type = BlastType.GOAL_COMPLETED
            )
            BlastType.MILESTONE -> BlastEvent(
                title = "MILESTONE REACHED!",
                subtitle = "High efficiency streak unlocked! Keep it up!",
                badgeText = "Milestone",
                type = BlastType.MILESTONE
            )
        }
        repository.triggerBlast(event)
    }
}
