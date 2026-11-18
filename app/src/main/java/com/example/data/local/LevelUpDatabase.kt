package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.AchievementEntity
import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.DailyPerformanceEntity
import com.example.data.model.HabitEntity
import com.example.data.model.LeaderboardEntryEntity
import com.example.data.model.NotificationPreferenceEntity
import com.example.data.model.ScheduleTemplateEntity
import com.example.data.model.ScheduledActivityEntity
import com.example.data.model.TemplateActivityEntity
import com.example.data.model.TimerSessionEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.XpTransactionEntity

@Database(
    entities = [
        UserProfileEntity::class,
        ActivityCategoryEntity::class,
        ScheduledActivityEntity::class,
        ScheduleTemplateEntity::class,
        TemplateActivityEntity::class,
        TimerSessionEntity::class,
        DailyPerformanceEntity::class,
        HabitEntity::class,
        AchievementEntity::class,
        XpTransactionEntity::class,
        LeaderboardEntryEntity::class,
        NotificationPreferenceEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LevelUpDatabase : RoomDatabase() {
    abstract fun dao(): LevelUpDao

    companion object {
        @Volatile
        private var INSTANCE: LevelUpDatabase? = null

        fun getInstance(context: Context): LevelUpDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LevelUpDatabase::class.java,
                    "level_up.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
