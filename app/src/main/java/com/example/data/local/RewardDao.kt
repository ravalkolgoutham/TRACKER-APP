package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RewardDao {

    @Query("SELECT * FROM rewards ORDER BY isRedeemed ASC, costPoints ASC")
    fun getAllRewardsFlow(): Flow<List<RewardEntity>>

    @Query("SELECT * FROM rewards")
    suspend fun getAllRewardsList(): List<RewardEntity>

    @Query("SELECT * FROM rewards WHERE id = :id LIMIT 1")
    suspend fun getRewardById(id: String): RewardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reward: RewardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rewards: List<RewardEntity>)

    @Update
    suspend fun update(reward: RewardEntity)

    @Query("DELETE FROM rewards WHERE id = :id")
    suspend fun delete(id: String)
}
