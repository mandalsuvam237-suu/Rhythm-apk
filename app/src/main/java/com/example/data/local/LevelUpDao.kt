package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelUpDao {
    // User Profile
    @Query("SELECT * FROM users LIMIT 1")
    fun observeUser(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUser(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUser(user: UserProfileEntity)

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    // Activity Categories
    @Query("SELECT * FROM activities ORDER BY sortOrder ASC, name ASC")
    fun observeCategories(): Flow<List<ActivityCategoryEntity>>

    @Query("SELECT * FROM activities ORDER BY sortOrder ASC, name ASC")
    suspend fun getCategoriesOnce(): List<ActivityCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<ActivityCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: ActivityCategoryEntity)

    @Query("DELETE FROM activities WHERE id = :id AND isDefault = 0")
    suspend fun deleteCustomCategory(id: String)

    // Scheduled Activities
    @Query("SELECT * FROM daily_schedules WHERE dateKey = :dateKey ORDER BY startMinute ASC, sortOrder ASC")
    fun observeActivitiesForDate(dateKey: String): Flow<List<ScheduledActivityEntity>>

    @Query("SELECT * FROM daily_schedules WHERE dateKey = :dateKey ORDER BY startMinute ASC, sortOrder ASC")
    suspend fun getActivitiesForDateOnce(dateKey: String): List<ScheduledActivityEntity>

    @Query("SELECT * FROM daily_schedules WHERE dateKey BETWEEN :startKey AND :endKey ORDER BY dateKey ASC, startMinute ASC")
    fun observeActivitiesInRange(startKey: String, endKey: String): Flow<List<ScheduledActivityEntity>>

    @Query("SELECT * FROM daily_schedules WHERE dateKey BETWEEN :startKey AND :endKey ORDER BY dateKey ASC, startMinute ASC")
    suspend fun getActivitiesInRangeOnce(startKey: String, endKey: String): List<ScheduledActivityEntity>

    @Query("SELECT * FROM daily_schedules ORDER BY dateKey ASC, startMinute ASC")
    suspend fun getAllScheduledActivitiesOnce(): List<ScheduledActivityEntity>

    @Query("SELECT * FROM daily_schedules WHERE id = :id")
    suspend fun getScheduledActivityById(id: String): ScheduledActivityEntity?

    @Query("SELECT * FROM daily_schedules WHERE isRepeating = 1 GROUP BY title, startMinute")
    suspend fun getRepeatingTemplatesOnce(): List<ScheduledActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivity(activity: ScheduledActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivities(activities: List<ScheduledActivityEntity>)

    @Query("DELETE FROM daily_schedules WHERE id = :id")
    suspend fun deleteActivityById(id: String)

    @Query("DELETE FROM daily_schedules WHERE userId = :userId")
    suspend fun deleteActivitiesForUser(userId: String)

    @Query("DELETE FROM daily_schedules")
    suspend fun clearAllScheduledActivities()

    // Templates
    @Query("SELECT * FROM schedule_templates ORDER BY isDefault DESC, name ASC")
    fun observeTemplates(): Flow<List<ScheduleTemplateEntity>>

    @Query("SELECT * FROM schedule_templates ORDER BY isDefault DESC, name ASC")
    suspend fun getTemplatesOnce(): List<ScheduleTemplateEntity>

    @Query("SELECT * FROM template_activities ORDER BY startMinute ASC, sortOrder ASC")
    fun observeAllTemplateActivities(): Flow<List<TemplateActivityEntity>>

    @Query("SELECT * FROM template_activities WHERE templateId = :templateId ORDER BY startMinute ASC, sortOrder ASC")
    suspend fun getTemplateActivitiesOnce(templateId: String): List<TemplateActivityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplate(template: ScheduleTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplates(templates: List<ScheduleTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTemplateActivities(items: List<TemplateActivityEntity>)

    @Query("DELETE FROM template_activities WHERE templateId = :templateId")
    suspend fun deleteTemplateActivitiesFor(templateId: String)

    @Query("DELETE FROM schedule_templates WHERE id = :templateId")
    suspend fun deleteTemplateById(templateId: String)

    // Timer Sessions
    @Query("SELECT * FROM timer_sessions WHERE isFinished = 0 ORDER BY startedAt DESC LIMIT 1")
    fun observeActiveTimer(): Flow<TimerSessionEntity?>

    @Query("SELECT * FROM timer_sessions WHERE isFinished = 0 ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveTimerOnce(): TimerSessionEntity?

    @Query("SELECT * FROM timer_sessions WHERE id = :id")
    suspend fun getTimerById(id: String): TimerSessionEntity?

    @Query("SELECT * FROM timer_sessions ORDER BY startedAt DESC LIMIT 30")
    fun observeRecentTimerSessions(): Flow<List<TimerSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTimerSession(session: TimerSessionEntity)

    @Query("DELETE FROM timer_sessions")
    suspend fun clearTimerSessions()

    // Daily Performance
    @Query("SELECT * FROM daily_performance WHERE dateKey BETWEEN :startKey AND :endKey ORDER BY dateKey ASC")
    fun observePerformancesInRange(startKey: String, endKey: String): Flow<List<DailyPerformanceEntity>>

    @Query("SELECT * FROM daily_performance WHERE dateKey = :dateKey")
    fun observePerformanceForDate(dateKey: String): Flow<DailyPerformanceEntity?>

    @Query("SELECT * FROM daily_performance WHERE dateKey = :dateKey")
    suspend fun getPerformanceForDateOnce(dateKey: String): DailyPerformanceEntity?

    @Query("SELECT * FROM daily_performance ORDER BY dateKey ASC")
    suspend fun getAllPerformancesOnce(): List<DailyPerformanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPerformance(perf: DailyPerformanceEntity)

    @Query("DELETE FROM daily_performance")
    suspend fun clearPerformances()

    // Habits
    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    suspend fun getHabitsOnce(): List<HabitEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: String)

    @Query("DELETE FROM habits")
    suspend fun clearHabits()

    // Achievements
    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC, xpReward ASC")
    fun observeAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAchievementsOnce(): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAchievements(items: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(item: AchievementEntity)

    // XP Transactions
    @Query("SELECT * FROM xp_transactions ORDER BY createdAt DESC LIMIT 100")
    fun observeXpTransactions(): Flow<List<XpTransactionEntity>>

    @Query("SELECT * FROM xp_transactions WHERE dateKey BETWEEN :startKey AND :endKey")
    suspend fun getXpTransactionsInRangeOnce(startKey: String, endKey: String): List<XpTransactionEntity>

    @Query("SELECT * FROM xp_transactions")
    suspend fun getAllXpTransactionsOnce(): List<XpTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertXpTransaction(tx: XpTransactionEntity)

    @Query("DELETE FROM xp_transactions")
    suspend fun clearXpTransactions()

    // Leaderboard
    @Query("SELECT * FROM leaderboard_entries ORDER BY weeklyXp DESC")
    fun observeLeaderboard(): Flow<List<LeaderboardEntryEntity>>

    @Query("SELECT * FROM leaderboard_entries")
    suspend fun getLeaderboardOnce(): List<LeaderboardEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeaderboardEntries(entries: List<LeaderboardEntryEntity>)

    @Query("DELETE FROM leaderboard_entries")
    suspend fun clearLeaderboard()

    // Notification Preferences
    @Query("SELECT * FROM notification_preferences WHERE id = 'default'")
    fun observeNotificationPreference(): Flow<NotificationPreferenceEntity?>

    @Query("SELECT * FROM notification_preferences WHERE id = 'default'")
    suspend fun getNotificationPreferenceOnce(): NotificationPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotificationPreference(pref: NotificationPreferenceEntity)
}
