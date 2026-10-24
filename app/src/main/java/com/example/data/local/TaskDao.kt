package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE syncStatus != 'PENDING_DELETE' ORDER BY isCompleted ASC, scheduledTime ASC")
    fun getAllActiveTasksFlow(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE syncStatus != 'PENDING_DELETE' ORDER BY isCompleted ASC, scheduledTime ASC")
    suspend fun getAllActiveTasksList(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE googleEventId = :googleEventId LIMIT 1")
    suspend fun getTaskByGoogleEventId(googleEventId: String): TaskEntity?

    @Query("SELECT * FROM tasks WHERE syncStatus IN ('PENDING_INSERT', 'PENDING_UPDATE', 'PENDING_DELETE')")
    suspend fun getPendingSyncTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 AND syncStatus != 'PENDING_DELETE' AND scheduledTime > :currentTime")
    suspend fun getUpcomingIncompleteTasks(currentTime: Long): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deletePermanently(id: String)

    @Query("UPDATE tasks SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("DELETE FROM tasks WHERE syncStatus = 'PENDING_DELETE' AND id = :id")
    suspend fun removeDeletedTombstone(id: String)
}
