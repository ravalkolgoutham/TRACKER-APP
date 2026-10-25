package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.GoalEntity
import com.example.data.local.RewardEntity
import com.example.data.local.TaskEntity
import com.example.data.local.UserStatsEntity
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.SyncStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SyncTask", appName)
    }

    @Test
    fun `insert and retrieve task from Room database`() = runBlocking {
        val taskId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val task = TaskEntity(
            id = taskId,
            googleEventId = "g_event_123",
            title = "Test S25 Multitasking Task",
            description = "Verify Room offline sync and gestures",
            scheduledTime = now + 3600000,
            endTime = now + 7200000,
            isCompleted = false,
            priority = Priority.URGENT,
            category = "Work",
            repeatRule = RepeatRule.DAILY,
            reminderMinutesBefore = 15,
            syncStatus = SyncStatus.SYNCED,
            lastModified = now,
            version = 1,
            snoozeUntil = null
        )

        db.taskDao().insert(task)

        val retrieved = db.taskDao().getTaskById(taskId)
        assertNotNull(retrieved)
        assertEquals("Test S25 Multitasking Task", retrieved?.title)
        assertEquals(Priority.URGENT, retrieved?.priority)
        assertEquals(15, retrieved?.reminderMinutesBefore)
    }

    @Test
    fun `query upcoming incomplete tasks for alarm restoration`() = runBlocking {
        val now = System.currentTimeMillis()
        val upcoming = TaskEntity(
            id = "upcoming_1",
            googleEventId = null,
            title = "Upcoming Meeting",
            description = "",
            scheduledTime = now + 1000000,
            endTime = now + 2000000,
            isCompleted = false,
            priority = Priority.HIGH,
            category = "Meeting",
            repeatRule = RepeatRule.NONE,
            reminderMinutesBefore = 15,
            syncStatus = SyncStatus.PENDING_INSERT,
            lastModified = now,
            version = 1,
            snoozeUntil = null
        )
        val completed = upcoming.copy(id = "completed_1", isCompleted = true)

        db.taskDao().insert(upcoming)
        db.taskDao().insert(completed)

        val tasksToRestore = db.taskDao().getUpcomingIncompleteTasks(now)
        assertEquals(1, tasksToRestore.size)
        assertEquals("Upcoming Meeting", tasksToRestore[0].title)
    }

    @Test
    fun `test goals and rewards persistence`() = runBlocking {
        val goal = GoalEntity(
            id = "goal_1",
            title = "Complete 5 Sprint Tasks",
            description = "Test description",
            targetCount = 5,
            currentCount = 3,
            category = "Work",
            rewardPoints = 150,
            isCompleted = false,
            createdAt = System.currentTimeMillis()
        )
        db.goalDao().insert(goal)

        val activeGoals = db.goalDao().getActiveGoals()
        assertEquals(1, activeGoals.size)
        assertEquals("Complete 5 Sprint Tasks", activeGoals[0].title)

        val stats = UserStatsEntity(
            id = "primary_stats",
            totalXp = 300,
            availablePoints = 250,
            currentLevel = 2,
            currentStreakDays = 4,
            lastCompletedDate = System.currentTimeMillis(),
            tasksCompletedCount = 6
        )
        db.userStatsDao().insertOrUpdate(stats)

        val retrievedStats = db.userStatsDao().getStats()
        assertNotNull(retrievedStats)
        assertEquals(300, retrievedStats?.totalXp)
        assertEquals(250, retrievedStats?.availablePoints)

        val reward = RewardEntity(
            id = "reward_1",
            title = "Coffee Treat",
            description = "Espresso",
            costPoints = 100,
            iconCategory = "Coffee",
            isRedeemed = false,
            redeemedAt = null
        )
        db.rewardDao().insert(reward)

        val retrievedReward = db.rewardDao().getRewardById("reward_1")
        assertNotNull(retrievedReward)
        assertEquals("Coffee Treat", retrievedReward?.title)
    }

    @Test
    fun `test blast event data model and types`() {
        val streakBlast = com.example.model.BlastEvent(
            title = "DAILY STREAK GOAL!",
            subtitle = "7 consecutive days achieved! Streak extended!",
            badgeText = "7 Days",
            type = com.example.model.BlastType.STREAK_GOAL
        )
        assertEquals("DAILY STREAK GOAL!", streakBlast.title)
        assertEquals(com.example.model.BlastType.STREAK_GOAL, streakBlast.type)
        assertEquals("7 Days", streakBlast.badgeText)

        val levelBlast = com.example.model.BlastEvent(
            title = "LEVEL UP!",
            subtitle = "You reached Level 3!",
            badgeText = "Level 3",
            type = com.example.model.BlastType.LEVEL_UP
        )
        assertEquals(com.example.model.BlastType.LEVEL_UP, levelBlast.type)
    }

    @Test
    fun `query upcoming incomplete tasks restores snoozed tasks even when original scheduledTime is in the past`() = runBlocking {
        val now = System.currentTimeMillis()
        val pastScheduledButSnoozedFuture = TaskEntity(
            id = "snoozed_1",
            googleEventId = null,
            title = "Snoozed Deep Work Sprint",
            description = "Should be restored on boot",
            scheduledTime = now - 600_000L, // 10m ago
            endTime = now + 1_200_000L,
            isCompleted = false,
            priority = Priority.URGENT,
            category = "Focus",
            repeatRule = RepeatRule.NONE,
            reminderMinutesBefore = 15,
            syncStatus = SyncStatus.SYNCED,
            lastModified = now,
            version = 1,
            snoozeUntil = now + 600_000L // 10m in future
        )

        db.taskDao().insert(pastScheduledButSnoozedFuture)

        val tasksToRestore = db.taskDao().getUpcomingIncompleteTasks(now)
        assertEquals(1, tasksToRestore.size)
        assertEquals("Snoozed Deep Work Sprint", tasksToRestore[0].title)
    }

    @Test
    fun `natural language parser extracts priority category and duration`() {
        val parsed = com.example.ui.NaturalLanguageTaskParser.parse(
            "Prepare Q4 Architecture Deck tomorrow 3pm #Work !urgent 45m"
        )
        assertEquals("Prepare Q4 Architecture Deck", parsed.cleanTitle)
        assertEquals(Priority.URGENT, parsed.priority)
        assertEquals("Work", parsed.category)
        assertEquals(45, parsed.durationMinutes)
        assertTrue(parsed.scheduledTime > System.currentTimeMillis())
    }

    @Test
    fun `subtask serialization and domain conversion round-trip`() {
        val subtasks = listOf(
            com.example.model.SubtaskItem("s1", "Outline architecture", true),
            com.example.model.SubtaskItem("s2", "Benchmark Room queries", false)
        )
        val encoded = TaskEntity.encodeSubtasks(subtasks)
        val decoded = TaskEntity.parseSubtasks(encoded)
        assertEquals(2, decoded.size)
        assertEquals("Outline architecture", decoded[0].title)
        assertEquals(true, decoded[0].isCompleted)
        assertEquals("Benchmark Room queries", decoded[1].title)
        assertEquals(false, decoded[1].isCompleted)
    }
}
