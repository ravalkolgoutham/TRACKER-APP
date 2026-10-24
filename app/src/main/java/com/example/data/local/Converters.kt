package com.example.data.local

import androidx.room.TypeConverter
import com.example.model.Priority
import com.example.model.RepeatRule
import com.example.model.SyncStatus

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String = priority.name

    @TypeConverter
    fun toPriority(value: String): Priority = runCatching { Priority.valueOf(value) }.getOrDefault(Priority.MEDIUM)

    @TypeConverter
    fun fromRepeatRule(rule: RepeatRule): String = rule.name

    @TypeConverter
    fun toRepeatRule(value: String): RepeatRule = runCatching { RepeatRule.valueOf(value) }.getOrDefault(RepeatRule.NONE)

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String = status.name

    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus = runCatching { SyncStatus.valueOf(value) }.getOrDefault(SyncStatus.SYNCED)
}
