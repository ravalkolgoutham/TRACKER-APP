package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.alarm.ReminderScheduler
import com.example.data.auth.GoogleAuthManager
import com.example.data.local.FocusSessionDao
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
import com.example.model.SyncStatus
import com.example.model.TaskItem
import com.example.model.UserRewardStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

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

    // Reactive streams from local Room DB
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

    init {
        scope.launch {
            // Remove any preloaded or placeholder tasks to ensure 0 default tasks
            val placeholderTitles = listOf(
                "Team Sprint Planning & Architecture",
                "Deep Focus: System Polish",
                "Deep Focus: Samsung S25 UX Polish",
                "Evening Health & Mobility Run"
            )
            val currentTasks = taskDao.getAllActiveTasksList()
            for (task in currentTasks) {
                if (task.title in placeholderTitles) {
                    taskDao.deletePermanently(task.id)
                }
            }

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
                        tasksCompletedCount = 0
                    )
                )
            }
            if (goalDao.getActiveGoals().isEmpty()) {
                seedInitialGoals()
            }
            seedInitialRewardsIfEmpty()

            // Start real-time observer for Samsung Calendar & Google Calendar
            calendarSyncManager.startRealtimeObserver {
                scope.launch {
                    syncWithGoogleCalendar()
                }
            }
        }
    }

    private suspend fun seedInitialGoals() {
        val initialGoals = listOf(
            GoalEntity(
                id = UUID.randomUUID().toString(),
                title = "Complete 5 Work Tasks",
                description = "Clear your weekly engineering and sprint checklist",
                targetCount = 5,
                currentCount = 2,
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
                currentCount = 1,
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
                currentCount = 2,
                category = "General",
                rewardPoints = 100,
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )
        )
        goalDao.insertAll(initialGoals)
    }

    private suspend fun seedInitialRewardsIfEmpty() {
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
        val updated = task.copy(
            syncStatus = status,
            lastModified = System.currentTimeMillis(),
            version = (existing?.version ?: 0) + 1
        )
        val calendarEventId = calendarSyncManager.pushTaskToDeviceCalendar(updated)
        val withCalId = if (calendarEventId != null && updated.googleEventId.isNullOrBlank()) {
            updated.copy(googleEventId = calendarEventId)
        } else updated
        taskDao.insert(TaskEntity.fromDomain(withCalId))
        ReminderScheduler.scheduleReminder(context, withCalId)
        triggerSyncAsync()
    }

    suspend fun toggleTaskComplete(taskId: String) = withContext(Dispatchers.IO) {
        val entity = taskDao.getTaskById(taskId) ?: return@withContext
        val now = System.currentTimeMillis()
        val willBeCompleted = !entity.isCompleted

        if (willBeCompleted) {
            ReminderScheduler.cancelReminder(context, taskId)
            // Award Rewards & XP for completing task!
            awardCompletionRewards(entity)
        }

        val updated = entity.copy(
            isCompleted = willBeCompleted,
            syncStatus = SyncStatus.PENDING_UPDATE,
            lastModified = now
        )
        taskDao.update(updated)

        // If repeating task is completed, automatically instantiate next scheduled occurrence
        if (willBeCompleted && entity.repeatRule != RepeatRule.NONE) {
            val nextScheduledTime = calculateNextOccurrence(entity.scheduledTime, entity.repeatRule)
            val duration = entity.endTime - entity.scheduledTime
            val nextTask = entity.copy(
                id = UUID.randomUUID().toString(),
                googleEventId = null,
                scheduledTime = nextScheduledTime,
                endTime = nextScheduledTime + duration,
                isCompleted = false,
                syncStatus = SyncStatus.PENDING_INSERT,
                lastModified = now,
                snoozeUntil = null,
                version = 1
            )
            taskDao.insert(nextTask)
            ReminderScheduler.scheduleReminder(context, nextTask.toDomain())
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
            tasksCompletedCount = 0
        )

        val (xpGain, pointGain) = when (task.priority) {
            Priority.URGENT -> 100 to 80
            Priority.HIGH -> 75 to 60
            Priority.MEDIUM -> 50 to 40
            Priority.LOW -> 25 to 20
        }

        val now = System.currentTimeMillis()
        val lastCal = Calendar.getInstance().apply { timeInMillis = stats.lastCompletedDate }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val hasPriorCompletion = stats.tasksCompletedCount > 0 && stats.lastCompletedDate > 0L

        val isSameDay = hasPriorCompletion &&
                lastCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                lastCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        val isConsecutive = hasPriorCompletion && if (lastCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
            nowCal.get(Calendar.DAY_OF_YEAR) == lastCal.get(Calendar.DAY_OF_YEAR) + 1
        } else if (nowCal.get(Calendar.YEAR) == lastCal.get(Calendar.YEAR) + 1) {
            nowCal.get(Calendar.DAY_OF_YEAR) == 1 &&
                    lastCal.get(Calendar.DAY_OF_YEAR) == lastCal.getActualMaximum(Calendar.DAY_OF_YEAR)
        } else {
            false
        }

        val newStreak = when {
            !hasPriorCompletion -> 1
            isSameDay -> maxOf(1, stats.currentStreakDays)
            isConsecutive -> stats.currentStreakDays + 1
            else -> 1 // Streak broken (>1 day gap), restarts at 1
        }
        val newBestStreak = maxOf(stats.bestStreakDays, newStreak)

        val newTotalXp = stats.totalXp + xpGain
        var newPoints = stats.availablePoints + pointGain
        val newLevel = (newTotalXp / 200) + 1

        var celebration = "+$xpGain XP & +$pointGain Points earned!"
        if (newLevel > stats.currentLevel) {
            celebration = "LEVEL UP! You reached Level $newLevel! +$pointGain Points!"
        }
        if (isConsecutive) {
            celebration += " • $newStreak-day streak extended!"
        } else if (isSameDay) {
            celebration += " • $newStreak-day streak active!"
        } else {
            celebration += " • Day 1 streak started!"
        }

        val isLevelUp = newLevel > stats.currentLevel
        var achievedGoal: GoalEntity? = null

        // Check and advance Goals matching this task
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
            tasksCompletedCount = stats.tasksCompletedCount + 1
        )
        userStatsDao.insertOrUpdate(updatedStats)
        _celebrationEvent.value = celebration

        // Trigger particle blast celebration for milestone or streak goal
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
            isConsecutive -> BlastEvent(
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
        ReminderScheduler.scheduleReminder(context, updated.toDomain())
    }

    suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        ReminderScheduler.cancelReminder(context, taskId)
        val entity = taskDao.getTaskById(taskId) ?: return@withContext

        // Delete from device calendars (Samsung & Google)
        calendarSyncManager.deleteTaskFromDeviceCalendar(entity.googleEventId)

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
            // 1. Bidirectional Real-time Sync with Device Calendars (Samsung Calendar & Google Calendar)
            if (calendarSyncManager.hasCalendarPermissions()) {
                val deviceEvents = calendarSyncManager.readEventsFromCalendar()
                val currentTasks = taskDao.getAllActiveTasksList()
                for (dEvent in deviceEvents) {
                    val existing = currentTasks.firstOrNull { it.googleEventId == dEvent.googleEventId }
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
                _syncMessage.value = "Samsung Calendar synced. Cloud sync will resume on reconnect."
                Log.d(TAG, "Device is offline. Queuing remote sync for later.")
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
                                    syncStatus = SyncStatus.SYNCED
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

            try {
                val nowIso = isoFormat.format(Date(System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)))
                val response = calendarApi.getEvents(timeMin = nowIso, maxResults = 30)

                if (response.isSuccessful) {
                    val remoteEvents = response.body()?.items ?: emptyList()
                    val newConflictLogs = _conflictLogs.value.toMutableList()

                    for (event in remoteEvents) {
                        val eventId = event.id ?: continue
                        val localEntity = taskDao.getTaskByGoogleEventId(eventId)

                        val remoteStartTime = parseIsoTime(event.start.dateTime ?: event.start.date) ?: System.currentTimeMillis()
                        val remoteEndTime = parseIsoTime(event.end.dateTime ?: event.end.date) ?: (remoteStartTime + 3600000)
                        val remoteUpdated = parseIsoTime(event.updated) ?: 0L

                        if (localEntity == null) {
                            val newTask = TaskEntity(
                                id = UUID.randomUUID().toString(),
                                googleEventId = eventId,
                                title = event.summary ?: "Calendar Event",
                                description = event.description ?: "",
                                scheduledTime = remoteStartTime,
                                endTime = remoteEndTime,
                                isCompleted = event.summary?.startsWith("✓") == true,
                                priority = Priority.MEDIUM,
                                category = "Meeting",
                                repeatRule = RepeatRule.NONE,
                                reminderMinutesBefore = 15,
                                syncStatus = SyncStatus.SYNCED,
                                lastModified = remoteUpdated,
                                version = 1,
                                snoozeUntil = null
                            )
                            taskDao.insert(newTask)
                            ReminderScheduler.scheduleReminder(context, newTask.toDomain())
                        } else {
                            if (localEntity.syncStatus == SyncStatus.PENDING_UPDATE) {
                                if (localEntity.lastModified >= remoteUpdated) {
                                    val log = ConflictLog(
                                        id = UUID.randomUUID().toString(),
                                        taskTitle = localEntity.title,
                                        localTimestamp = localEntity.lastModified,
                                        remoteTimestamp = remoteUpdated,
                                        resolutionReason = "Local change preserved (Local edit was newer than Google Calendar update)"
                                    )
                                    newConflictLogs.add(0, log)
                                    val eventDto = GoogleCalendarEventDto(
                                        id = eventId,
                                        summary = localEntity.title,
                                        description = localEntity.description,
                                        start = EventDateTimeDto(dateTime = isoFormat.format(Date(localEntity.scheduledTime))),
                                        end = EventDateTimeDto(dateTime = isoFormat.format(Date(localEntity.endTime)))
                                    )
                                    calendarApi.updateEvent(eventId = eventId, event = eventDto)
                                    taskDao.update(localEntity.copy(syncStatus = SyncStatus.CONFLICT_RESOLVED))
                                } else {
                                    val log = ConflictLog(
                                        id = UUID.randomUUID().toString(),
                                        taskTitle = event.summary ?: localEntity.title,
                                        localTimestamp = localEntity.lastModified,
                                        remoteTimestamp = remoteUpdated,
                                        resolutionReason = "Google Calendar update applied (Remote update was newer than local edit)"
                                    )
                                    newConflictLogs.add(0, log)
                                    val resolved = localEntity.copy(
                                        title = event.summary ?: localEntity.title,
                                        description = event.description ?: localEntity.description,
                                        scheduledTime = remoteStartTime,
                                        endTime = remoteEndTime,
                                        syncStatus = SyncStatus.CONFLICT_RESOLVED,
                                        lastModified = remoteUpdated
                                    )
                                    taskDao.update(resolved)
                                    ReminderScheduler.scheduleReminder(context, resolved.toDomain())
                                }
                            }
                        }
                    }
                    _conflictLogs.value = newConflictLogs
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching remote events: ${e.message}")
            }

            authManager.updateLastSyncTime()
            _syncMessage.value = "Sync completed • Room & Google Calendar up-to-date"
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
        taskDao.update(task.copy(syncStatus = SyncStatus.SYNCED))
        Log.d(TAG, "Task '${task.title}' kept locally: $notice")
    }

    private fun repeatRuleToRrule(rule: RepeatRule): String = when (rule) {
        RepeatRule.DAILY -> "RRULE:FREQ=DAILY"
        RepeatRule.WEEKDAYS -> "RRULE:FREQ=WEEKLY;BYDAY=MO,TU,WE,TH,FR"
        RepeatRule.WEEKLY -> "RRULE:FREQ=WEEKLY"
        RepeatRule.MONTHLY -> "RRULE:FREQ=MONTHLY"
        RepeatRule.NONE -> ""
    }

    private fun parseIsoTime(isoString: String?): Long? {
        if (isoString.isNullOrBlank()) return null
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(isoString.substring(0, 19))?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateNextOccurrence(baseTime: Long, rule: RepeatRule): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = baseTime }
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
        return calendar.timeInMillis
    }

    private fun isNetworkAvailable(): Boolean {
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
