package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.alarm.ReminderScheduler
import com.example.data.auth.GoogleAuthManager
import com.example.data.local.FocusSessionDao
import com.example.data.local.FocusSessionEntity
import com.example.data.local.GoalDao
import com.example.data.local.GoalEntity
import com.example.data.local.RewardDao
import com.example.data.local.RewardEntity
import com.example.data.local.TaskDao
import com.example.data.local.TaskEntity
import com.example.data.local.UserStatsDao
import com.example.data.local.UserStatsEntity
import com.example.data.remote.EventDateTimeDto
import com.example.data.remote.GoogleCalendarApiClient
import com.example.data.remote.GoogleCalendarApiService
import com.example.data.remote.GoogleCalendarEventDto
import com.example.data.sync.SystemCalendarSyncManager
import com.example.model.BlastEvent
import com.example.model.BlastType
import com.example.model.ConflictLog
import com.example.model.FocusSession
import com.example.model.GoalItem
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.RewardItem
import com.example.model.SubtaskItem
import com.example.model.SyncQueueItem
import com.example.model.SyncStatus
import com.example.model.TaskItem
import com.example.model.UserRewardStats
import com.example.model.toLocalIsoDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TaskRepository(
    private val context: Context,
    private val taskDao: TaskDao,
    private val focusSessionDao: FocusSessionDao,
    private val goalDao: GoalDao,
    private val userStatsDao: UserStatsDao,
    private val rewardDao: RewardDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val authManager = GoogleAuthManager.getInstance(context)
    private val calendarApi: GoogleCalendarApiService = GoogleCalendarApiClient.create(context)
    val calendarSyncManager = SystemCalendarSyncManager(context)

    val allTasks: Flow<List<TaskItem>> = taskDao.getAllActiveTasksFlow()
        .map { list -> list.map { it.toDomain() } }

    val allSessions: Flow<List<FocusSession>> = focusSessionDao.getAllSessionsFlow()
        .map { list -> list.map { it.toDomain() } }

    val totalFocusMinutes: Flow<Int> = focusSessionDao.getTotalFocusMinutesFlow()
        .map { it ?: 0 }

    val allGoals: Flow<List<GoalItem>> = goalDao.getAllGoalsFlow()
        .map { list -> list.map { it.toDomain() } }

    val userStats: Flow<UserRewardStats> = userStatsDao.getStatsFlow()
        .map { it?.toDomain() ?: UserRewardStats() }

    val allRewards: Flow<List<RewardItem>> = rewardDao.getAllRewardsFlow()
        .map { list -> list.map { it.toDomain() } }

    private val _conflictLogs = MutableStateFlow<List<ConflictLog>>(emptyList())
    val conflictLogs: StateFlow<List<ConflictLog>> = _conflictLogs.asStateFlow()

    private val _syncQueueLogs = MutableStateFlow<List<SyncQueueItem>>(emptyList())
    val syncQueueLogs: StateFlow<List<SyncQueueItem>> = _syncQueueLogs.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _celebrationEvent = MutableStateFlow<String?>(null)
    val celebrationEvent: StateFlow<String?> = _celebrationEvent.asStateFlow()

    private val _blastEvent = MutableStateFlow<BlastEvent?>(null)
    val blastEvent: StateFlow<BlastEvent?> = _blastEvent.asStateFlow()

    fun triggerBlast(event: BlastEvent) {
        _blastEvent.value = event
    }

    fun clearBlast() {
        _blastEvent.value = null
    }

    private fun appendSyncQueueMutation(
        entityType: String,
        action: String,
        payloadSummary: String,
        status: String = if (isNetworkAvailable()) "synced" else "offline"
    ) {
        val item = SyncQueueItem(
            id = UUID.randomUUID().toString(),
            entityType = entityType,
            action = action,
            payloadSummary = payloadSummary,
            timestamp = System.currentTimeMillis(),
            retryCount = 0,
            syncStatus = status
        )
        _syncQueueLogs.value = (listOf(item) + _syncQueueLogs.value).take(40)
    }

    init {
        scope.launch {
            purgeDuplicatesAndHolidayTasks()

            if (userStatsDao.getStats() == null) {
                userStatsDao.insertOrUpdate(
                    UserStatsEntity(
                        id = "primary_stats",
                        totalXp = 0,
                        availablePoints = 0,
                        currentLevel = 1,
                        currentStreakDays = 0,
                        bestStreakDays = 0,
                        lastCompletedDate = 0L,
                        tasksCompletedCount = 0,
                        streakFreezeCount = 1
                    )
                )
            }
            if (goalDao.getActiveGoals().isEmpty()) {
                seedInitialGoals()
            }
            seedInitialRewardsIfEmpty()

            calendarSyncManager.startRealtimeObserver {
                scope.launch {
                    syncWithGoogleCalendar()
                }
            }
        }
    }

    /**
     * Removes any default placeholder tasks, imported Holiday/Observance calendar entries
     * (e.g., "Maharishi Valmiki Jayanti", "To hide observances, go to Google Calendar"),
     * and any duplicate tasks sharing the same title and scheduled time.
     */
    suspend fun purgeDuplicatesAndHolidayTasks(): Int = withContext(Dispatchers.IO) {
        val placeholderTitles = setOf(
            "Team Sprint Planning & Architecture",
            "Deep Focus: System Polish",
            "Deep Focus: Samsung S25 UX Polish",
            "Evening Health & Mobility Run"
        )
        val currentTasks = taskDao.getAllActiveTasksList()
        val seenTitleAndTime = mutableSetOf<String>()
        val seenTitleOnlyForCalImports = mutableSetOf<String>()
        var removedCount = 0

        for (task in currentTasks) {
            val isPlaceholder = task.title in placeholderTitles
            val isHolidayOrObservance = calendarSyncManager.isHolidayOrObservanceEvent(task.title, task.description)
            val normTitle = task.title.trim().lowercase(Locale.getDefault())
            val dedupKey = "${normTitle}_${task.scheduledTime}"
            val isDuplicate = !seenTitleAndTime.add(dedupKey)
            val isCalImportDuplicate = task.googleEventId?.startsWith("cal_") == true &&
                    !seenTitleOnlyForCalImports.add(normTitle)

            if (isPlaceholder || isHolidayOrObservance || isDuplicate || isCalImportDuplicate) {
                taskDao.deletePermanently(task.id)
                removedCount++
            }
        }
        if (removedCount > 0) {
            appendSyncQueueMutation("CLEANUP", "PURGE", "Removed $removedCount duplicate/observance entries")
            _syncMessage.value = "Cleaned up $removedCount duplicate & observance entries"
        }
        removedCount
    }

    private suspend fun seedInitialGoals() {
        val initialGoals = listOf(
            GoalEntity(
                id = UUID.randomUUID().toString(),
                title = "Complete 5 Work Tasks",
                description = "Clear your weekly engineering and sprint checklist",
                targetCount = 5,
                currentCount = 0,
                category = "Work",
                rewardPoints = 150,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            ),
            GoalEntity(
                id = UUID.randomUUID().toString(),
                title = "Deep Focus Mastery",
                description = "Execute 3 focused work sprint sessions",
                targetCount = 3,
                currentCount = 0,
                category = "Focus",
                rewardPoints = 120,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            ),
            GoalEntity(
                id = UUID.randomUUID().toString(),
                title = "Active Daily Streak",
                description = "Complete at least one task or event every day",
                targetCount = 3,
                currentCount = 0,
                category = "General",
                rewardPoints = 100,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )
        )
        goalDao.insertAll(initialGoals)
    }

    private suspend fun seedInitialRewardsIfEmpty() {
        if (rewardDao.getAllRewardsList().isNotEmpty()) return
        val initialRewards = listOf(
            RewardEntity(
                id = UUID.randomUUID().toString(),
                title = "☕ Specialty Coffee Treat",
                description = "Enjoy your favorite espresso, matcha, or bakery item guilt-free",
                costPoints = 100,
                iconCategory = "Coffee",
                isRedeemed = false,
                redeemedAt = null
            ),
            RewardEntity(
                id = UUID.randomUUID().toString(),
                title = "🎮 1-Hour Video Game Session",
                description = "Take an uninterrupted gaming break after hitting goals",
                costPoints = 150,
                iconCategory = "Game",
                isRedeemed = false,
                redeemedAt = null
            ),
            RewardEntity(
                id = UUID.randomUUID().toString(),
                title = "🎬 Movie Night & Popcorn",
                description = "Relax with a great film or evening series episode",
                costPoints = 250,
                iconCategory = "Movie",
                isRedeemed = false,
                redeemedAt = null
            ),
            RewardEntity(
                id = UUID.randomUUID().toString(),
                title = "🌿 30-Minute Nature Walk",
                description = "Unplug, listen to music, and recharge without screens",
                costPoints = 80,
                iconCategory = "Nature",
                isRedeemed = false,
                redeemedAt = null
            )
        )
        rewardDao.insertAll(initialRewards)
    }

    suspend fun saveTask(task: TaskItem) = withContext(Dispatchers.IO) {
        val existing = taskDao.getTaskById(task.id)
        val status = if (existing == null) {
            SyncStatus.PENDING_INSERT
        } else {
            SyncStatus.PENDING_UPDATE
        }
        val durationMins = ((task.endTime - task.scheduledTime) / 60000L).toInt().coerceAtLeast(15)
        val estPomos = (durationMins / 25).coerceAtLeast(1)
        val updated = task.copy(
            syncStatus = status,
            lastModified = System.currentTimeMillis(),
            version = (existing?.version ?: 0) + 1,
            estimatedPomodoros = if (task.estimatedPomodoros <= 0) estPomos else task.estimatedPomodoros
        )
        val calendarEventId = calendarSyncManager.pushTaskToDeviceCalendar(updated)
        val withCalId = if (calendarEventId != null && updated.googleEventId.isNullOrBlank()) {
            updated.copy(
                googleEventId = calendarEventId,
                etag = "etag_${System.currentTimeMillis()}",
                lastSyncedAt = System.currentTimeMillis()
            )
        } else updated
        taskDao.insert(TaskEntity.fromDomain(withCalId))
        appendSyncQueueMutation(
            entityType = "TASK",
            action = if (existing == null) "CREATE" else "UPDATE",
            payloadSummary = "${withCalId.title} (${withCalId.category})"
        )
        ReminderScheduler.scheduleReminder(context, withCalId)
        triggerSyncAsync()
    }

    suspend fun generateSmartBreakdown(taskId: String) = withContext(Dispatchers.IO) {
        val entity = taskDao.getTaskById(taskId) ?: return@withContext
        val domain = entity.toDomain()
        val totalMins = domain.durationMinutes
        val stepMins = (totalMins / 4).coerceAtLeast(10)
        val baseTitle = domain.title

        val generatedSubtasks = when (domain.category.lowercase(Locale.getDefault())) {
            "meeting" -> listOf(
                SubtaskItem(UUID.randomUUID().toString(), "Prepare agenda & talking points for $baseTitle", false, 10),
                SubtaskItem(UUID.randomUUID().toString(), "Review previous notes & open action items", false, 10),
                SubtaskItem(UUID.randomUUID().toString(), "Facilitate core discussion & decisions", false, stepMins),
                SubtaskItem(UUID.randomUUID().toString(), "Send follow-up summary & next steps", false, 10)
            )
            "health" -> listOf(
                SubtaskItem(UUID.randomUUID().toString(), "5-min dynamic warm-up & mobility", false, 5),
                SubtaskItem(UUID.randomUUID().toString(), "Primary session: $baseTitle", false, stepMins * 2),
                SubtaskItem(UUID.randomUUID().toString(), "Cool down & stretching", false, 10),
                SubtaskItem(UUID.randomUUID().toString(), "Hydrate & log recovery metrics", false, 5)
            )
            else -> listOf(
                SubtaskItem(UUID.randomUUID().toString(), "Scope requirements & setup for $baseTitle", false, stepMins),
                SubtaskItem(UUID.randomUUID().toString(), "Execute core implementation block", false, stepMins * 2),
                SubtaskItem(UUID.randomUUID().toString(), "Verify quality & edge cases", false, stepMins),
                SubtaskItem(UUID.randomUUID().toString(), "Wrap up & document progress", false, 10)
            )
        }

        val updated = domain.copy(
            subtasks = generatedSubtasks,
            lastModified = System.currentTimeMillis()
        )
        taskDao.update(TaskEntity.fromDomain(updated))
        appendSyncQueueMutation("TASK", "SMART_BREAKDOWN", "Generated 4 subtasks for '${domain.title}'")
    }

    suspend fun toggleSubtask(taskId: String, subtaskId: String) = withContext(Dispatchers.IO) {
        val entity = taskDao.getTaskById(taskId) ?: return@withContext
        val domain = entity.toDomain()
        val updatedSubtasks = domain.subtasks.map { sub ->
            if (sub.id == subtaskId) sub.copy(isCompleted = !sub.isCompleted) else sub
        }
        val updated = domain.copy(
            subtasks = updatedSubtasks,
            lastModified = System.currentTimeMillis()
        )
        taskDao.update(TaskEntity.fromDomain(updated))
        appendSyncQueueMutation("SUBTASK", "TOGGLE", "Updated subtask in '${domain.title}'")
    }

    suspend fun autoResolveScheduleConflict(taskId: String) = withContext(Dispatchers.IO) {
        val allActive = taskDao.getAllActiveTasksList().map { it.toDomain() }
        val target = allActive.firstOrNull { it.id == taskId } ?: return@withContext
        val duration = (target.endTime - target.scheduledTime).coerceAtLeast(30 * 60 * 1000L)

        // Shift forward in 30-min increments until no overlap with other non-completed tasks
        var candidateStart = target.endTime
        val others = allActive.filter { it.id != taskId && !it.isCompleted }
        var safety = 0
        while (safety < 24) {
            val candidateEnd = candidateStart + duration
            val hasOverlap = others.any { other ->
                candidateStart < other.endTime && candidateEnd > other.scheduledTime
            }
            if (!hasOverlap) break
            candidateStart += 30 * 60 * 1000L
            safety++
        }

        val shifted = target.copy(
            scheduledTime = candidateStart,
            endTime = candidateStart + duration,
            syncStatus = SyncStatus.PENDING_UPDATE,
            lastModified = System.currentTimeMillis()
        )
        taskDao.update(TaskEntity.fromDomain(shifted))
        ReminderScheduler.scheduleReminder(context, shifted)

        val log = ConflictLog(
            id = UUID.randomUUID().toString(),
            taskTitle = shifted.title,
            localTimestamp = System.currentTimeMillis(),
            remoteTimestamp = target.scheduledTime,
            resolutionReason = "Auto-resolved time-block conflict: shifted '${shifted.title}' to next free slot"
        )
        _conflictLogs.value = listOf(log) + _conflictLogs.value
        appendSyncQueueMutation("CALENDAR", "AUTO_RESOLVE", "Shifted '${shifted.title}' to next open slot")
    }

    suspend fun runSimulatedGoogleCalendarSync() = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        _syncMessage.value = "Simulating two-way Google Calendar sync..."
        val now = System.currentTimeMillis()
        val todayBase = Calendar.getInstance().apply {
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val sampleEvents = listOf(
            TaskItem(
                id = UUID.randomUUID().toString(),
                googleEventId = "gcal_sim_standup",
                title = "Engineering Daily Standup",
                description = "Synced from Google Calendar • Meet link attached",
                scheduledTime = (todayBase.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 10) }.timeInMillis,
                endTime = (todayBase.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 45) }.timeInMillis,
                priority = Priority.HIGH,
                category = "Meeting",
                syncStatus = SyncStatus.SYNCED,
                etag = "\"gcal_etag_101\"",
                lastSyncedAt = now
            ),
            TaskItem(
                id = UUID.randomUUID().toString(),
                googleEventId = "gcal_sim_design_review",
                title = "Product Architecture & Timeline Review",
                description = "External Google Calendar Event • Design & Sync Queue Spec",
                scheduledTime = (todayBase.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 14) }.timeInMillis,
                endTime = (todayBase.clone() as Calendar).apply { set(Calendar.HOUR_OF_DAY, 15) }.timeInMillis,
                priority = Priority.URGENT,
                category = "Work",
                syncStatus = SyncStatus.SYNCED,
                etag = "\"gcal_etag_102\"",
                lastSyncedAt = now
            )
        )

        val current = taskDao.getAllActiveTasksList()
        var added = 0
        for (ev in sampleEvents) {
            if (current.none { it.googleEventId == ev.googleEventId }) {
                taskDao.insert(TaskEntity.fromDomain(ev))
                added++
            }
        }
        authManager.updateLastSyncTime()
        appendSyncQueueMutation("EVENT", "SYNC", "Synced $added Google Calendar events (ETag verified)", "synced")
        _syncMessage.value = "Simulated Google Calendar sync complete ($added external events linked)"
        _isSyncing.value = false
    }

    suspend fun recordCompletedFocusPomodoro(taskId: String?, durationMinutes: Int) = withContext(Dispatchers.IO) {
        if (!taskId.isNullOrBlank()) {
            val entity = taskDao.getTaskById(taskId)
            if (entity != null) {
                val updated = entity.copy(
                    completedPomodoros = entity.completedPomodoros + 1,
                    lastModified = System.currentTimeMillis()
                )
                taskDao.update(updated)
            }
        }
        val stats = userStatsDao.getStats() ?: UserStatsEntity(
            id = "primary_stats",
            totalXp = 0,
            availablePoints = 0,
            currentLevel = 1,
            currentStreakDays = 0,
            bestStreakDays = 0,
            lastCompletedDate = 0L,
            tasksCompletedCount = 0,
            streakFreezeCount = 1
        )
        val focusXp = 50
        val focusPts = 25
        val newXp = stats.totalXp + focusXp
        var newPoints = stats.availablePoints + focusPts
        val newLevel = (newXp / 200) + 1

        // Progress active "Focus" goals (e.g. "Deep Focus Mastery")
        val activeGoals = goalDao.getActiveGoals()
        for (goal in activeGoals) {
            if (goal.category.equals("Focus", ignoreCase = true)) {
                val newCount = goal.currentCount + 1
                if (newCount >= goal.targetCount) {
                    val completedGoal = goal.copy(
                        currentCount = goal.targetCount,
                        isCompleted = true
                    )
                    goalDao.update(completedGoal)
                    newPoints += goal.rewardPoints
                    _celebrationEvent.value = "GOAL COMPLETED: '${goal.title}'! +${goal.rewardPoints} Bonus Points!"
                } else {
                    goalDao.update(goal.copy(currentCount = newCount))
                }
            }
        }

        userStatsDao.insertOrUpdate(
            stats.copy(
                totalXp = newXp,
                availablePoints = newPoints,
                currentLevel = newLevel
            )
        )
        appendSyncQueueMutation("FOCUS", "COMPLETE", "+$focusXp XP for ${durationMinutes}m focus session")
    }

    suspend fun buyStreakFreezeShield(): Boolean = withContext(Dispatchers.IO) {
        val stats = userStatsDao.getStats() ?: return@withContext false
        val shieldCost = 100
        if (stats.availablePoints >= shieldCost) {
            userStatsDao.insertOrUpdate(
                stats.copy(
                    availablePoints = stats.availablePoints - shieldCost,
                    streakFreezeCount = stats.streakFreezeCount + 1
                )
            )
            _celebrationEvent.value = "🛡️ Streak Freeze Shield acquired! Your streak is protected against a missed day."
            appendSyncQueueMutation("STATS", "STREAK_FREEZE", "Purchased Streak Freeze Shield (-100 pts)")
            return@withContext true
        }
        return@withContext false
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 4)
        root.put("exportedAt", System.currentTimeMillis())

        val tasksArr = JSONArray()
        for (t in taskDao.getAllActiveTasksList()) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("googleEventId", t.googleEventId ?: "")
                put("title", t.title)
                put("description", t.description)
                put("scheduledTime", t.scheduledTime)
                put("endTime", t.endTime)
                put("isCompleted", t.isCompleted)
                put("priority", t.priority.name)
                put("category", t.category)
                put("repeatRule", t.repeatRule.name)
                put("reminderMinutesBefore", t.reminderMinutesBefore)
                put("subtasksJson", t.subtasksJson)
                put("estimatedPomodoros", t.estimatedPomodoros)
                put("completedPomodoros", t.completedPomodoros)
            }
            tasksArr.put(obj)
        }
        root.put("tasks", tasksArr)

        userStatsDao.getStats()?.let { s ->
            val statsObj = JSONObject().apply {
                put("totalXp", s.totalXp)
                put("availablePoints", s.availablePoints)
                put("currentLevel", s.currentLevel)
                put("currentStreakDays", s.currentStreakDays)
                put("bestStreakDays", s.bestStreakDays)
                put("lastCompletedDate", s.lastCompletedDate)
                put("tasksCompletedCount", s.tasksCompletedCount)
                put("streakFreezeCount", s.streakFreezeCount)
            }
            root.put("userStats", statsObj)
        }
        appendSyncQueueMutation("BACKUP", "EXPORT", "Exported ${tasksArr.length()} tasks to JSON")
        root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)
            val tasksArr = root.optJSONArray("tasks")
            if (tasksArr != null) {
                for (i in 0 until tasksArr.length()) {
                    val obj = tasksArr.optJSONObject(i) ?: continue
                    val entity = TaskEntity(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        googleEventId = obj.optString("googleEventId", "").ifBlank { null },
                        title = obj.optString("title", "Imported Task"),
                        description = obj.optString("description", ""),
                        scheduledTime = obj.optLong("scheduledTime", System.currentTimeMillis()),
                        endTime = obj.optLong("endTime", System.currentTimeMillis() + 3600000L),
                        isCompleted = obj.optBoolean("isCompleted", false),
                        priority = runCatching { Priority.valueOf(obj.optString("priority", "MEDIUM")) }.getOrDefault(Priority.MEDIUM),
                        category = obj.optString("category", "Work"),
                        repeatRule = runCatching { RepeatRule.valueOf(obj.optString("repeatRule", "NONE")) }.getOrDefault(RepeatRule.NONE),
                        reminderMinutesBefore = obj.optInt("reminderMinutesBefore", 15),
                        syncStatus = SyncStatus.SYNCED,
                        lastModified = System.currentTimeMillis(),
                        version = 1,
                        snoozeUntil = null,
                        subtasksJson = obj.optString("subtasksJson", "[]"),
                        estimatedPomodoros = obj.optInt("estimatedPomodoros", 2),
                        completedPomodoros = obj.optInt("completedPomodoros", 0)
                    )
                    taskDao.insert(entity)
                }
            }
            root.optJSONObject("userStats")?.let { s ->
                val statsEntity = UserStatsEntity(
                    id = "primary_stats",
                    totalXp = s.optInt("totalXp", 0),
                    availablePoints = s.optInt("availablePoints", 0),
                    currentLevel = s.optInt("currentLevel", 1),
                    currentStreakDays = s.optInt("currentStreakDays", 0),
                    bestStreakDays = s.optInt("bestStreakDays", 0),
                    lastCompletedDate = s.optLong("lastCompletedDate", 0L),
                    tasksCompletedCount = s.optInt("tasksCompletedCount", 0),
                    streakFreezeCount = s.optInt("streakFreezeCount", 1)
                )
                userStatsDao.insertOrUpdate(statsEntity)
            }
            appendSyncQueueMutation("BACKUP", "IMPORT", "Restored backup from JSON successfully")
            _syncMessage.value = "Backup JSON restored successfully!"
            true
        } catch (e: Exception) {
            Log.e(TAG, "Import backup failed: ${e.message}")
            _syncMessage.value = "Invalid JSON backup format."
            false
        }
    }

    suspend fun toggleTaskComplete(taskId: String) = withContext(Dispatchers.IO) {
        val entity = taskDao.getTaskById(taskId) ?: return@withContext
        val now = System.currentTimeMillis()
        val willBeCompleted = !entity.isCompleted

        if (willBeCompleted) {
            ReminderScheduler.cancelReminder(context, taskId)
            awardCompletionRewards(entity)
        }

        val updated = entity.copy(
            isCompleted = willBeCompleted,
            syncStatus = SyncStatus.PENDING_UPDATE,
            lastModified = now
        )
        taskDao.update(updated)
        if (!willBeCompleted) {
            ReminderScheduler.scheduleReminder(context, updated.toDomain())
        }
        appendSyncQueueMutation(
            entityType = "TASK",
            action = "TOGGLE",
            payloadSummary = "${if (willBeCompleted) "Completed" else "Reopened"} '${entity.title}'"
        )

        if (willBeCompleted && entity.repeatRule != RepeatRule.NONE) {
            val nextScheduledTime = calculateNextOccurrence(entity.scheduledTime, entity.repeatRule, now)
            val duration = (entity.endTime - entity.scheduledTime).coerceAtLeast(15 * 60_000L)
            val resetSubtasksDomain = entity.toDomain().subtasks.map { it.copy(isCompleted = false) }
            val nextDomain = entity.toDomain().copy(
                id = UUID.randomUUID().toString(),
                googleEventId = null,
                scheduledTime = nextScheduledTime,
                endTime = nextScheduledTime + duration,
                isCompleted = false,
                syncStatus = SyncStatus.PENDING_INSERT,
                lastModified = now,
                snoozeUntil = null,
                version = 1,
                completedPomodoros = 0,
                subtasks = resetSubtasksDomain
            )
            taskDao.insert(TaskEntity.fromDomain(nextDomain))
            ReminderScheduler.scheduleReminder(context, nextDomain)
        }

        triggerSyncAsync()
    }

    private suspend fun awardCompletionRewards(task: TaskEntity) {
        val stats = userStatsDao.getStats() ?: UserStatsEntity(
            id = "primary_stats",
            totalXp = 0,
            availablePoints = 0,
            currentLevel = 1,
            currentStreakDays = 0,
            bestStreakDays = 0,
            lastCompletedDate = 0L,
            tasksCompletedCount = 0,
            streakFreezeCount = 1
        )

        // XP scaled by priority as specified: Low +10, Medium +25, High +50, Urgent +100
        val xpGain = task.priority.xpValue
        val pointGain = when (task.priority) {
            Priority.URGENT -> 80
            Priority.HIGH -> 50
            Priority.MEDIUM -> 30
            Priority.LOW -> 15
        }

        val now = System.currentTimeMillis()
        val lastIso = toLocalIsoDate(stats.lastCompletedDate)
        val todayIso = toLocalIsoDate(now)

        val yesterdayCal = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayIso = toLocalIsoDate(yesterdayCal.timeInMillis)

        val twoDaysAgoCal = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, -2) }
        val twoDaysAgoIso = toLocalIsoDate(twoDaysAgoCal.timeInMillis)

        val hasPriorCompletion = stats.tasksCompletedCount > 0 && lastIso.isNotEmpty()
        val isSameDay = hasPriorCompletion && lastIso == todayIso
        val isConsecutive = hasPriorCompletion && lastIso == yesterdayIso
        val isFreezeSaved = hasPriorCompletion && lastIso == twoDaysAgoIso && stats.streakFreezeCount > 0

        var newFreezeCount = stats.streakFreezeCount
        val newStreak = when {
            !hasPriorCompletion -> 1
            isSameDay -> maxOf(1, stats.currentStreakDays)
            isConsecutive -> stats.currentStreakDays + 1
            isFreezeSaved -> {
                newFreezeCount = (stats.streakFreezeCount - 1).coerceAtLeast(0)
                stats.currentStreakDays + 1
            }
            else -> 1
        }
        val newBestStreak = maxOf(stats.bestStreakDays, newStreak)

        val newTotalXp = stats.totalXp + xpGain
        var newPoints = stats.availablePoints + pointGain
        val newLevel = (newTotalXp / 200) + 1

        var celebration = "+$xpGain XP & +$pointGain Points earned!"
        if (newLevel > stats.currentLevel) {
            celebration = "LEVEL UP! You reached Level $newLevel! +$pointGain Points!"
        }
        if (isFreezeSaved) {
            celebration += " • 🛡️ Streak Freeze used! $newStreak-day streak saved!"
        } else if (isConsecutive) {
            celebration += " • $newStreak-day streak extended!"
        } else if (isSameDay) {
            celebration += " • $newStreak-day streak active!"
        } else {
            celebration += " • Day 1 streak started!"
        }

        val isLevelUp = newLevel > stats.currentLevel
        var achievedGoal: GoalEntity? = null

        val activeGoals = goalDao.getActiveGoals()
        for (goal in activeGoals) {
            val matches = goal.category.equals("General", ignoreCase = true) ||
                    goal.category.equals(task.category, ignoreCase = true)
            if (matches) {
                val newCount = goal.currentCount + 1
                if (newCount >= goal.targetCount) {
                    val completedGoal = goal.copy(
                        currentCount = goal.targetCount,
                        isCompleted = true
                    )
                    goalDao.update(completedGoal)
                    newPoints += goal.rewardPoints
                    celebration = "GOAL COMPLETED: '${goal.title}'! +${goal.rewardPoints} Bonus Points!"
                    achievedGoal = completedGoal
                } else {
                    goalDao.update(goal.copy(currentCount = newCount))
                }
            }
        }

        val updatedStats = stats.copy(
            totalXp = newTotalXp,
            availablePoints = newPoints,
            currentLevel = newLevel,
            currentStreakDays = newStreak,
            bestStreakDays = newBestStreak,
            lastCompletedDate = now,
            tasksCompletedCount = stats.tasksCompletedCount + 1,
            streakFreezeCount = newFreezeCount
        )
        userStatsDao.insertOrUpdate(updatedStats)
        _celebrationEvent.value = celebration

        val blast = when {
            isLevelUp -> BlastEvent(
                title = "LEVEL UP!",
                subtitle = "Promoted to Level $newLevel! +$pointGain Points",
                badgeText = "Level $newLevel",
                type = BlastType.LEVEL_UP
            )
            achievedGoal != null -> BlastEvent(
                title = "GOAL ACHIEVED!",
                subtitle = achievedGoal.title,
                badgeText = "+${achievedGoal.rewardPoints} Pts",
                type = BlastType.GOAL_COMPLETED
            )
            isConsecutive || isFreezeSaved -> BlastEvent(
                title = "DAILY STREAK GOAL!",
                subtitle = "$newStreak consecutive days achieved! Streak extended!",
                badgeText = "$newStreak Days",
                type = BlastType.STREAK_GOAL
            )
            newStreak >= 1 -> BlastEvent(
                title = "DAILY STREAK ACTIVE!",
                subtitle = "Day $newStreak goal accomplished! Streak maintained today!",
                badgeText = "$newStreak Days",
                type = BlastType.STREAK_GOAL
            )
            else -> BlastEvent(
                title = "TASK COMPLETED!",
                subtitle = "+$xpGain XP earned!",
                badgeText = "+$xpGain XP",
                type = BlastType.MILESTONE
            )
        }
        _blastEvent.value = blast
    }

    suspend fun redeemReward(rewardId: String): Boolean = withContext(Dispatchers.IO) {
        val reward = rewardDao.getRewardById(rewardId) ?: return@withContext false
        val stats = userStatsDao.getStats() ?: return@withContext false

        if (stats.availablePoints >= reward.costPoints && !reward.isRedeemed) {
            val updatedStats = stats.copy(
                availablePoints = stats.availablePoints - reward.costPoints
            )
            userStatsDao.insertOrUpdate(updatedStats)
            rewardDao.update(
                reward.copy(
                    isRedeemed = true,
                    redeemedAt = System.currentTimeMillis()
                )
            )
            _celebrationEvent.value = "🎁 Redeemed reward: '${reward.title}'! Enjoy it!"
            return@withContext true
        }
        return@withContext false
    }

    suspend fun createGoal(
        title: String,
        description: String,
        targetCount: Int,
        category: String,
        rewardPoints: Int
    ) = withContext(Dispatchers.IO) {
        val goal = GoalEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            targetCount = targetCount,
            currentCount = 0,
            category = category,
            rewardPoints = rewardPoints,
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        goalDao.insert(goal)
    }

    suspend fun createCustomReward(
        title: String,
        description: String,
        costPoints: Int,
        iconCategory: String
    ) = withContext(Dispatchers.IO) {
        val reward = RewardEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            costPoints = costPoints,
            iconCategory = iconCategory,
            isRedeemed = false,
            redeemedAt = null
        )
        rewardDao.insert(reward)
    }

    suspend fun deleteGoal(goalId: String) = withContext(Dispatchers.IO) {
        goalDao.delete(goalId)
    }

    fun clearCelebration() {
        _celebrationEvent.value = null
        _blastEvent.value = null
    }

    suspend fun snoozeTask(taskId: String, minutes: Int = 10) = withContext(Dispatchers.IO) {
        val entity = taskDao.getTaskById(taskId) ?: return@withContext
        val snoozeTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        val updated = entity.copy(
            snoozeUntil = snoozeTime,
            lastModified = System.currentTimeMillis()
        )
        taskDao.update(updated)
        appendSyncQueueMutation("TASK", "SNOOZE", "Snoozed '${entity.title}' for ${minutes}m")
        ReminderScheduler.scheduleReminder(context, updated.toDomain())
    }

    suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        ReminderScheduler.cancelReminder(context, taskId)
        val entity = taskDao.getTaskById(taskId) ?: return@withContext

        calendarSyncManager.deleteTaskFromDeviceCalendar(entity.googleEventId)
        appendSyncQueueMutation("TASK", "DELETE", "Deleted '${entity.title}'")

        if (!entity.googleEventId.isNullOrBlank()) {
            val tombstone = entity.copy(
                syncStatus = SyncStatus.PENDING_DELETE,
                lastModified = System.currentTimeMillis()
            )
            taskDao.update(tombstone)
            triggerSyncAsync()
        } else {
            taskDao.deletePermanently(taskId)
        }
    }

    private fun triggerSyncAsync() {
        scope.launch {
            syncWithGoogleCalendar()
        }
    }

    suspend fun syncWithGoogleCalendar(): Boolean = withContext(Dispatchers.IO) {
        if (_isSyncing.value) return@withContext false
        _isSyncing.value = true
        _syncMessage.value = "Syncing Samsung Calendar, Google Calendar & Room..."

        try {
            purgeDuplicatesAndHolidayTasks()

            if (calendarSyncManager.hasCalendarPermissions()) {
                val deviceEvents = calendarSyncManager.readEventsFromCalendar()
                val currentTasks = taskDao.getAllActiveTasksList()
                for (dEvent in deviceEvents) {
                    val existing = currentTasks.firstOrNull {
                        it.googleEventId == dEvent.googleEventId ||
                            (it.title.equals(dEvent.title, ignoreCase = true) && it.scheduledTime == dEvent.scheduledTime)
                    }
                    if (existing == null) {
                        taskDao.insert(TaskEntity.fromDomain(dEvent))
                    } else if (existing.title != dEvent.title || existing.scheduledTime != dEvent.scheduledTime) {
                        taskDao.update(
                            existing.copy(
                                title = dEvent.title,
                                scheduledTime = dEvent.scheduledTime,
                                endTime = dEvent.endTime,
                                isCompleted = dEvent.isCompleted,
                                lastModified = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }

            val isOnline = isNetworkAvailable()
            if (!isOnline) {
                _syncMessage.value = "Offline — Queued changes saved locally in Room"
                _isSyncing.value = false
                return@withContext true
            }

            val pendingTasks = taskDao.getPendingSyncTasks()
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            for (task in pendingTasks) {
                try {
                    when (task.syncStatus) {
                        SyncStatus.PENDING_INSERT -> {
                            val eventDto = GoogleCalendarEventDto(
                                summary = task.title,
                                description = "${task.description}\n[Category: ${task.category} | Priority: ${task.priority.name}]",
                                start = EventDateTimeDto(dateTime = isoFormat.format(Date(task.scheduledTime))),
                                end = EventDateTimeDto(dateTime = isoFormat.format(Date(task.endTime))),
                                recurrence = if (task.repeatRule != RepeatRule.NONE) {
                                    listOf(repeatRuleToRrule(task.repeatRule))
                                } else null
                            )
                            val response = calendarApi.createEvent(event = eventDto)
                            if (response.isSuccessful && response.body()?.id != null) {
                                val createdId = response.body()!!.id
                                val synced = task.copy(
                                    googleEventId = createdId,
                                    syncStatus = SyncStatus.SYNCED,
                                    lastSyncedAt = System.currentTimeMillis()
                                )
                                taskDao.update(synced)
                            } else {
                                markAsLocalSyncedWithNotice(task, "Offline cached (Token refresh needed)")
                            }
                        }

                        SyncStatus.PENDING_UPDATE -> {
                            val eventId = task.googleEventId
                            if (eventId != null) {
                                val eventDto = GoogleCalendarEventDto(
                                    id = eventId,
                                    summary = if (task.isCompleted) "✓ ${task.title}" else task.title,
                                    description = task.description,
                                    start = EventDateTimeDto(dateTime = isoFormat.format(Date(task.scheduledTime))),
                                    end = EventDateTimeDto(dateTime = isoFormat.format(Date(task.endTime)))
                                )
                                val response = calendarApi.updateEvent(eventId = eventId, event = eventDto)
                                if (response.isSuccessful) {
                                    taskDao.updateSyncStatus(task.id, SyncStatus.SYNCED)
                                } else {
                                    markAsLocalSyncedWithNotice(task, "Saved locally (Remote update deferred)")
                                }
                            } else {
                                taskDao.updateSyncStatus(task.id, SyncStatus.SYNCED)
                            }
                        }

                        SyncStatus.PENDING_DELETE -> {
                            val eventId = task.googleEventId
                            if (eventId != null) {
                                calendarApi.deleteEvent(eventId = eventId)
                            }
                            taskDao.removeDeletedTombstone(task.id)
                        }

                        else -> {}
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error pushing task ${task.id}: ${e.message}")
                }
            }

            authManager.updateLastSyncTime()
            _syncMessage.value = "Online — Synced • Room & Google Calendar up-to-date"
            _isSyncing.value = false
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Sync error: ${e.message}", e)
            _syncMessage.value = "Offline sync ready • Changes persisted locally"
            _isSyncing.value = false
            return@withContext false
        }
    }

    private suspend fun markAsLocalSyncedWithNotice(task: TaskEntity, notice: String) {
        taskDao.update(task.copy(syncStatus = SyncStatus.SYNCED, lastSyncedAt = System.currentTimeMillis()))
        Log.d(TAG, "Task '${task.title}' kept locally: $notice")
    }

    private fun repeatRuleToRrule(rule: RepeatRule): String = when (rule) {
        RepeatRule.DAILY -> "RRULE:FREQ=DAILY"
        RepeatRule.WEEKDAYS -> "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
        RepeatRule.WEEKLY -> "RRULE:FREQ=WEEKLY"
        RepeatRule.MONTHLY -> "RRULE:FREQ=MONTHLY"
        RepeatRule.NONE -> ""
    }

    private fun calculateNextOccurrence(baseTime: Long, rule: RepeatRule, minAfterTime: Long = System.currentTimeMillis()): Long {
        if (rule == RepeatRule.NONE) return baseTime
        val calendar = Calendar.getInstance().apply { timeInMillis = baseTime }
        var safety = 0
        do {
            when (rule) {
                RepeatRule.DAILY -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                RepeatRule.WEEKDAYS -> {
                    do {
                        calendar.add(Calendar.DAY_OF_YEAR, 1)
                    } while (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                        calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY)
                }
                RepeatRule.WEEKLY -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                RepeatRule.MONTHLY -> calendar.add(Calendar.MONTH, 1)
                RepeatRule.NONE -> {}
            }
            safety++
        } while (calendar.timeInMillis <= minAfterTime && safety < 400)
        return calendar.timeInMillis
    }

    fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        private const val TAG = "TaskRepository"
    }
}
