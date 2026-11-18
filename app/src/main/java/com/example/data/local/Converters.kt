package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.ActivityPriority
import com.example.data.model.ActivityStatus

class Converters {
    @TypeConverter
    fun fromStatus(value: ActivityStatus): String = value.name

    @TypeConverter
    fun toStatus(value: String): ActivityStatus =
        runCatching { ActivityStatus.valueOf(value) }.getOrDefault(ActivityStatus.PLANNED)

    @TypeConverter
    fun fromPriority(value: ActivityPriority): String = value.name

    @TypeConverter
    fun toPriority(value: String): ActivityPriority =
        runCatching { ActivityPriority.valueOf(value) }.getOrDefault(ActivityPriority.MEDIUM)
}
