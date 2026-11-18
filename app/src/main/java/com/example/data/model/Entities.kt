package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ActivityStatus {
    PLANNED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
}

enum class ActivityPriority(val weight: Int, val label: String) {
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High")
}

enum class ThemePalette(val displayName: String) {
    LIGHT("Light"),
    DARK("Dark"),
    PASTEL_PINK("Pastel Pink"),
    BLUE("Ocean Blue"),
    GREEN("Emerald Green"),
    ORANGE("Sunset Orange"),
    PURPLE("Royal Purple")
}

enum class FontChoice(val displayName: String) {
    PLUS_JAKARTA("Plus Jakarta Sans"),
    SPACE_GROTESK("Space Grotesk"),
    PLAYFAIR("Playfair Display"),
    JETBRAINS_MONO("JetBrains Mono")
}

enum class StatsRange(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    YEARLY("Yearly"),
    CUSTOM("Custom")
}

enum class LeaderboardPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

@Entity(tableName = "users")
data class UserProfileEntity(
    @PrimaryKey val uid: String,
    val displayName: String,
    val email: String,
    val avatarInitials: String,
    val passwordHash: String = "",
    val totalXp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalFocusMinutes: Int = 0,
    val leaderboardOptIn: Boolean = true,
    val isLoggedIn: Boolean = true,
    val isDemoMode: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activities")
data class ActivityCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "daily_schedules",
    foreignKeys = [
        ForeignKey(
            entity = ActivityCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["dateKey"]),
        Index(value = ["categoryId"]),
        Index(value = ["userId", "dateKey"])
    ]
)
data class ScheduledActivityEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val dateKey: String,
    val title: String,
    val notes: String = "",
    val categoryId: String,
    val startMinute: Int,
    val durationMinutes: Int,
    val status: ActivityStatus = ActivityStatus.PLANNED,
    val priority: ActivityPriority = ActivityPriority.MEDIUM,
    val isRepeating: Boolean = false,
    val repeatDaysMask: Int = 0,
    val reminderBeforeMinutes: Int = 10,
    val actualSpentSeconds: Long = 0L,
    val completedAt: Long? = null,
    val sortOrder: Int = 0,
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val endMinute: Int get() = startMinute + durationMinutes
    val crossesMidnight: Boolean get() = endMinute > 1440
    val hasLocation: Boolean get() = latitude != null && longitude != null
}

@Entity(tableName = "schedule_templates")
data class ScheduleTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val accentColorHex: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "template_activities",
    foreignKeys = [
        ForeignKey(
            entity = ScheduleTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["templateId"])]
)
data class TemplateActivityEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val title: String,
    val categoryId: String,
    val startMinute: Int,
    val durationMinutes: Int,
    val priority: ActivityPriority = ActivityPriority.MEDIUM,
    val notes: String = "",
    val sortOrder: Int = 0
)

@Entity(
    tableName = "timer_sessions",
    indices = [Index(value = ["activityId"]), Index(value = ["dateKey"])]
)
data class TimerSessionEntity(
    @PrimaryKey val id: String,
    val activityId: String?,
    val dateKey: String,
    val title: String,
    val categoryId: String,
    val plannedSeconds: Long,
    val accumulatedSeconds: Long = 0L,
    val lastResumedEpochMs: Long? = null,
    val isRunning: Boolean = false,
    val isFinished: Boolean = false,
    val overtimeSeconds: Long = 0L,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null
)

@Entity(tableName = "daily_performance")
data class DailyPerformanceEntity(
    @PrimaryKey val dateKey: String,
    val totalPlannedCount: Int,
    val completedCount: Int,
    val onTimeCount: Int,
    val highPriorityTotal: Int,
    val highPriorityCompleted: Int,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val score: Int,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val categoryId: String,
    val targetDaysPerWeek: Int = 5,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastCompletedDateKey: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val code: String,
    val title: String,
    val description: String,
    val iconKey: String,
    val xpReward: Int,
    val unlockedAt: Long? = null
)

@Entity(
    tableName = "xp_transactions",
    indices = [Index(value = ["dateKey"]), Index(value = ["userId"])]
)
data class XpTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val amount: Int,
    val reason: String,
    val sourceType: String,
    val dateKey: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "leaderboard_entries")
data class LeaderboardEntryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val displayName: String,
    val avatarInitials: String,
    val dailyXp: Int,
    val weeklyXp: Int,
    val monthlyXp: Int,
    val level: Int,
    val streak: Int,
    val isCurrentUser: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "notification_preferences")
data class NotificationPreferenceEntity(
    @PrimaryKey val id: String = "default",
    val notifyBeforeEnabled: Boolean = true,
    val minutesBefore: Int = 10,
    val notifyOnStart: Boolean = true,
    val notifyOnOverdue: Boolean = true,
    val notifyOnFocusEnd: Boolean = true,
    val dailyReviewEnabled: Boolean = true,
    val dailyReviewMinute: Int = 21 * 60,
    val updatedAt: Long = System.currentTimeMillis()
)

data class ScheduledActivityUi(
    val activity: ScheduledActivityEntity,
    val category: ActivityCategoryEntity,
    val isCarriedOverFromPrevDay: Boolean = false
)

data class FocusPlacePreset(
    val id: String,
    val name: String,
    val subtitle: String,
    val suggestedCategoryId: String,
    val latitude: Double,
    val longitude: Double
)
