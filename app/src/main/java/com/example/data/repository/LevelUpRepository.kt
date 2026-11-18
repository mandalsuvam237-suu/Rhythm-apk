package com.example.data.repository

import android.content.Context
import com.example.data.local.LevelUpDao
import com.example.data.local.LevelUpDatabase
import com.example.data.local.PreferencesStore
import com.example.data.local.SecurityPreferences
import com.example.data.local.SeedData
import com.example.data.local.VisualPreferences
import com.example.data.model.AchievementEntity
import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.ActivityPriority
import com.example.data.model.ActivityStatus
import com.example.data.model.DailyPerformanceEntity
import com.example.data.model.FontChoice
import com.example.data.model.HabitEntity
import com.example.data.model.LeaderboardEntryEntity
import com.example.data.model.NotificationPreferenceEntity
import com.example.data.model.ScheduleTemplateEntity
import com.example.data.model.ScheduledActivityEntity
import com.example.data.model.ScheduledActivityUi
import com.example.data.model.TemplateActivityEntity
import com.example.data.model.ThemePalette
import com.example.data.model.TimerSessionEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.XpTransactionEntity
import com.example.domain.DateTimeUtils
import com.example.domain.OverlapConflict
import com.example.domain.ScheduleEngine
import com.example.domain.SmartSchedulePlan
import com.example.domain.SmartSuggestedBlock
import com.example.domain.SmartSuggestionEngine
import com.example.domain.StatsSummary
import com.example.domain.WeeklyPerformanceSummary
import com.example.notifications.NotificationScheduler
import com.example.widget.LevelUpWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.UUID

data class TemplateApplyResult(
    val addedCount: Int,
    val shiftedCount: Int,
    val skippedCount: Int,
    val message: String
)

class LevelUpRepository(
    private val context: Context,
    private val dao: LevelUpDao = LevelUpDatabase.getInstance(context).dao(),
    private val prefsStore: PreferencesStore = PreferencesStore(context),
    private val notificationScheduler: NotificationScheduler = NotificationScheduler(context)
) {

    val visualPreferences: Flow<VisualPreferences> = prefsStore.visualPreferences
    val securityPreferences: Flow<SecurityPreferences> = prefsStore.securityPreferences
    val userProfile: Flow<UserProfileEntity?> = dao.observeUser()
    val categories: Flow<List<ActivityCategoryEntity>> = dao.observeCategories()
    val templates: Flow<List<ScheduleTemplateEntity>> = dao.observeTemplates()
    val templateActivities: Flow<List<TemplateActivityEntity>> = dao.observeAllTemplateActivities()
    val activeTimer: Flow<TimerSessionEntity?> = dao.observeActiveTimer()
    val recentTimerSessions: Flow<List<TimerSessionEntity>> = dao.observeRecentTimerSessions()
    val habits: Flow<List<HabitEntity>> = dao.observeHabits()
    val achievements: Flow<List<AchievementEntity>> = dao.observeAchievements()
    val xpTransactions: Flow<List<XpTransactionEntity>> = dao.observeXpTransactions()
    val leaderboard: Flow<List<LeaderboardEntryEntity>> = dao.observeLeaderboard()
    val notificationPreference: Flow<NotificationPreferenceEntity?> = dao.observeNotificationPreference()

    suspend fun ensureInitialized() {
        if (dao.getCategoriesOnce().isEmpty()) {
            dao.upsertCategories(SeedData.defaultCategories())
        }
        if (dao.getTemplatesOnce().isEmpty()) {
            dao.upsertTemplates(SeedData.defaultTemplates())
            dao.upsertTemplateActivities(SeedData.defaultTemplateActivities())
        }
        if (dao.getAchievementsOnce().isEmpty()) {
            dao.upsertAchievements(SeedData.defaultAchievements())
        }
        if (dao.getNotificationPreferenceOnce() == null) {
            dao.upsertNotificationPreference(SeedData.defaultNotificationPref())
        }
        if (dao.getUser() == null) {
            val user = SeedData.defaultUserProfile()
            dao.upsertUser(user)
            refreshLeaderboardInternal(user)
        }
        prefsStore.markSeeded()
        val notifPref = dao.getNotificationPreferenceOnce() ?: SeedData.defaultNotificationPref()
        notificationScheduler.scheduleDailyReview(notifPref)
    }

    fun observeScheduleForDate(dateKey: String): Flow<List<ScheduledActivityUi>> {
        val prevKey = DateTimeUtils.shiftDateKey(dateKey, -1)
        return combine(
            dao.observeActivitiesForDate(dateKey),
            dao.observeActivitiesForDate(prevKey),
            dao.observeCategories()
        ) { dayList, prevList, catList ->
            val catMap = catList.associateBy { it.id }
            val fallbackCat = catList.firstOrNull()
                ?: ActivityCategoryEntity("cat_other", "Other", "category", "#06B6D4", true, 11)

            val carriedOver = prevList
                .filter { it.endMinute > 1440 }
                .map { act ->
                    ScheduledActivityUi(
                        activity = act,
                        category = catMap[act.categoryId] ?: fallbackCat,
                        isCarriedOverFromPrevDay = true
                    )
                }

            val currentDay = dayList.map { act ->
                ScheduledActivityUi(
                    activity = act,
                    category = catMap[act.categoryId] ?: fallbackCat,
                    isCarriedOverFromPrevDay = false
                )
            }
            carriedOver + currentDay
        }
    }

    fun observeDayCountsInRange(startKey: String, endKey: String): Flow<Map<String, Int>> {
        return dao.observeActivitiesInRange(startKey, endKey).let { flow ->
            kotlinx.coroutines.flow.flow {
                flow.collect { list ->
                    emit(
                        list.filter { it.status != ActivityStatus.SKIPPED }
                            .groupingBy { it.dateKey }
                            .eachCount()
                    )
                }
            }
        }
    }

    fun observePerformanceForDate(dateKey: String): Flow<DailyPerformanceEntity?> =
        dao.observePerformanceForDate(dateKey)

    fun observeStatsForRange(startKey: String, endKey: String): Flow<StatsSummary> {
        val rangeKeys = DateTimeUtils.dateRangeKeys(startKey, endKey)
        return combine(
            dao.observeActivitiesInRange(startKey, endKey),
            dao.observeCategories(),
            dao.observePerformancesInRange(startKey, endKey)
        ) { acts, cats, perfs ->
            ScheduleEngine.aggregateStats(acts, cats, perfs, rangeKeys)
        }
    }

    fun observeWeeklyPerformance(anchorDateKey: String): Flow<WeeklyPerformanceSummary> {
        val prevStart = DateTimeUtils.shiftDateKey(anchorDateKey, -13)
        return combine(
            dao.observeActivitiesInRange(prevStart, anchorDateKey),
            dao.observePerformancesInRange(prevStart, anchorDateKey)
        ) { acts, perfs ->
            ScheduleEngine.calculateWeeklyPerformance(acts, perfs, anchorDateKey)
        }
    }

    suspend fun findOverlaps(
        candidateId: String?,
        dateKey: String,
        startMinute: Int,
        durationMinutes: Int
    ): List<OverlapConflict> {
        val prevKey = DateTimeUtils.shiftDateKey(dateKey, -1)
        val nextKey = DateTimeUtils.shiftDateKey(dateKey, 1)
        return ScheduleEngine.findOverlaps(
            candidateId = candidateId,
            candidateDateKey = dateKey,
            candidateStartMinute = startMinute,
            candidateDurationMinutes = durationMinutes,
            sameDayActivities = dao.getActivitiesForDateOnce(dateKey),
            prevDayActivities = dao.getActivitiesForDateOnce(prevKey),
            nextDayActivities = dao.getActivitiesForDateOnce(nextKey)
        )
    }

    suspend fun findNextAvailableSlot(dateKey: String, preferredStart: Int, durationMinutes: Int): Int {
        val prevKey = DateTimeUtils.shiftDateKey(dateKey, -1)
        val nextKey = DateTimeUtils.shiftDateKey(dateKey, 1)
        return ScheduleEngine.findNextAvailableSlot(
            preferredStartMinute = preferredStart,
            durationMinutes = durationMinutes,
            sameDayActivities = dao.getActivitiesForDateOnce(dateKey),
            prevDayActivities = dao.getActivitiesForDateOnce(prevKey),
            nextDayActivities = dao.getActivitiesForDateOnce(nextKey)
        )
    }

    suspend fun saveActivity(
        existingId: String?,
        dateKey: String,
        title: String,
        notes: String,
        categoryId: String,
        startMinute: Int,
        durationMinutes: Int,
        priority: ActivityPriority,
        isRepeating: Boolean,
        repeatDaysMask: Int,
        reminderBeforeMinutes: Int,
        locationName: String = "",
        latitude: Double? = null,
        longitude: Double? = null
    ): ScheduledActivityEntity {
        val user = dao.getUser() ?: SeedData.defaultUserProfile()
        val existing = existingId?.let { dao.getScheduledActivityById(it) }
        val now = System.currentTimeMillis()
        val item = ScheduledActivityEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            userId = user.uid,
            dateKey = dateKey,
            title = title.trim(),
            notes = notes.trim(),
            categoryId = categoryId,
            startMinute = startMinute.coerceIn(0, 1439),
            durationMinutes = durationMinutes.coerceIn(5, 720),
            status = existing?.status ?: ActivityStatus.PLANNED,
            priority = priority,
            isRepeating = isRepeating,
            repeatDaysMask = repeatDaysMask,
            reminderBeforeMinutes = reminderBeforeMinutes,
            actualSpentSeconds = existing?.actualSpentSeconds ?: 0L,
            completedAt = existing?.completedAt,
            sortOrder = existing?.sortOrder ?: startMinute,
            locationName = locationName.trim(),
            latitude = latitude,
            longitude = longitude,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        dao.upsertActivity(item)

        // Propagate repeating instances for upcoming 6 days if newly created or updated with mask
        if (isRepeating && repeatDaysMask > 0 && existing == null) {
            for (dayOffset in 1..6) {
                val futureDate = DateTimeUtils.shiftDateKey(dateKey, dayOffset)
                if (DateTimeUtils.matchesWeekdayMask(futureDate, repeatDaysMask)) {
                    val conflicts = findOverlaps(null, futureDate, item.startMinute, item.durationMinutes)
                    if (conflicts.isEmpty()) {
                        val copy = item.copy(
                            id = UUID.randomUUID().toString(),
                            dateKey = futureDate,
                            status = ActivityStatus.PLANNED,
                            actualSpentSeconds = 0L,
                            completedAt = null
                        )
                        dao.upsertActivity(copy)
                        scheduleNotificationsFor(copy)
                    }
                }
            }
        }

        scheduleNotificationsFor(item)
        recalculatePerformance(dateKey)
        LevelUpWidgetProvider.requestUpdate(context)
        return item
    }

    suspend fun deleteActivity(activityId: String) {
        val existing = dao.getScheduledActivityById(activityId) ?: return
        notificationScheduler.cancelForActivity(activityId)
        dao.deleteActivityById(activityId)
        recalculatePerformance(existing.dateKey)
        LevelUpWidgetProvider.requestUpdate(context)
    }

    suspend fun moveActivityTime(activityId: String, newStartMinute: Int) {
        val existing = dao.getScheduledActivityById(activityId) ?: return
        val updated = existing.copy(
            startMinute = newStartMinute.coerceIn(0, 1439),
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertActivity(updated)
        scheduleNotificationsFor(updated)
        recalculatePerformance(updated.dateKey)
        LevelUpWidgetProvider.requestUpdate(context)
    }

    suspend fun swapActivityTimes(firstId: String, secondId: String) {
        val a = dao.getScheduledActivityById(firstId) ?: return
        val b = dao.getScheduledActivityById(secondId) ?: return
        val now = System.currentTimeMillis()
        val aUpdated = a.copy(startMinute = b.startMinute, sortOrder = b.sortOrder, updatedAt = now)
        val bUpdated = b.copy(startMinute = a.startMinute, sortOrder = a.sortOrder, updatedAt = now)
        dao.upsertActivities(listOf(aUpdated, bUpdated))
        scheduleNotificationsFor(aUpdated)
        scheduleNotificationsFor(bUpdated)
        LevelUpWidgetProvider.requestUpdate(context)
    }

    suspend fun setActivityStatus(activityId: String, newStatus: ActivityStatus) {
        val existing = dao.getScheduledActivityById(activityId) ?: return
        if (existing.status == newStatus) return
        val now = System.currentTimeMillis()
        val wasCompleted = existing.status == ActivityStatus.COMPLETED
        val updated = existing.copy(
            status = newStatus,
            completedAt = if (newStatus == ActivityStatus.COMPLETED) now else null,
            actualSpentSeconds = if (newStatus == ActivityStatus.COMPLETED && existing.actualSpentSeconds == 0L) {
                existing.durationMinutes * 60L
            } else {
                existing.actualSpentSeconds
            },
            updatedAt = now
        )
        dao.upsertActivity(updated)

        if (newStatus == ActivityStatus.COMPLETED || newStatus == ActivityStatus.SKIPPED) {
            notificationScheduler.cancelForActivity(activityId)
        } else {
            scheduleNotificationsFor(updated)
        }

        if (!wasCompleted && newStatus == ActivityStatus.COMPLETED) {
            val xpEarned = ScheduleEngine.calculateXpForCompletion(updated)
            awardXp(
                amount = xpEarned,
                reason = "Completed ${updated.title}",
                sourceType = "ACTIVITY",
                dateKey = updated.dateKey
            )
        }

        recalculatePerformance(updated.dateKey)
        evaluateAchievements(updated.dateKey)
        LevelUpWidgetProvider.requestUpdate(context)
    }

    suspend fun rescheduleUnfinishedActivities(fromDateKey: String, toDateKey: String): Int {
        val pending = dao.getActivitiesForDateOnce(fromDateKey)
            .filter { it.status == ActivityStatus.PLANNED || it.status == ActivityStatus.IN_PROGRESS }
        if (pending.isEmpty()) return 0

        var movedCount = 0
        for (item in pending) {
            val slot = findNextAvailableSlot(toDateKey, item.startMinute, item.durationMinutes)
            val updated = item.copy(
                dateKey = toDateKey,
                startMinute = slot,
                status = ActivityStatus.PLANNED,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsertActivity(updated)
            scheduleNotificationsFor(updated)
            movedCount++
        }
        recalculatePerformance(fromDateKey)
        recalculatePerformance(toDateKey)
        LevelUpWidgetProvider.requestUpdate(context)
        return movedCount
    }

    // Templates
    suspend fun saveTemplate(
        existingId: String?,
        name: String,
        description: String,
        accentColorHex: String,
        activities: List<TemplateActivityEntity>
    ) {
        val now = System.currentTimeMillis()
        val tplId = existingId ?: "tpl_${UUID.randomUUID()}"
        val template = ScheduleTemplateEntity(
            id = tplId,
            name = name.trim(),
            description = description.trim(),
            accentColorHex = accentColorHex,
            isDefault = false,
            createdAt = now,
            updatedAt = now
        )
        dao.upsertTemplate(template)
        dao.deleteTemplateActivitiesFor(tplId)
        val normalized = activities.mapIndexed { idx, act ->
            act.copy(
                id = if (act.id.isBlank()) UUID.randomUUID().toString() else act.id,
                templateId = tplId,
                sortOrder = idx
            )
        }
        dao.upsertTemplateActivities(normalized)
        unlockAchievement("ARCHITECT")
    }

    suspend fun duplicateTemplate(templateId: String) {
        val templates = dao.getTemplatesOnce()
        val original = templates.firstOrNull { it.id == templateId } ?: return
        val items = dao.getTemplateActivitiesOnce(templateId)
        val newId = "tpl_${UUID.randomUUID()}"
        val now = System.currentTimeMillis()
        dao.upsertTemplate(
            original.copy(
                id = newId,
                name = "${original.name} (Copy)",
                isDefault = false,
                createdAt = now,
                updatedAt = now
            )
        )
        dao.upsertTemplateActivities(
            items.map {
                it.copy(
                    id = UUID.randomUUID().toString(),
                    templateId = newId
                )
            }
        )
    }

    suspend fun deleteTemplate(templateId: String) {
        dao.deleteTemplateActivitiesFor(templateId)
        dao.deleteTemplateById(templateId)
    }

    /**
     * Non-destructive template application: preserves all existing activities.
     * If a template item overlaps existing items, automatically shifts to the next conflict-free slot,
     * or skips if no slot exists.
     */
    suspend fun applyTemplateToDate(templateId: String, dateKey: String): TemplateApplyResult {
        val templateItems = dao.getTemplateActivitiesOnce(templateId)
        if (templateItems.isEmpty()) {
            return TemplateApplyResult(0, 0, 0, "Template has no activities.")
        }
        val user = dao.getUser() ?: SeedData.defaultUserProfile()
        var added = 0
        var shifted = 0
        var skipped = 0

        for (tpl in templateItems) {
            val overlaps = findOverlaps(null, dateKey, tpl.startMinute, tpl.durationMinutes)
            val chosenStart = if (overlaps.isEmpty()) {
                tpl.startMinute
            } else {
                val candidate = findNextAvailableSlot(dateKey, tpl.startMinute, tpl.durationMinutes)
                val check = findOverlaps(null, dateKey, candidate, tpl.durationMinutes)
                if (check.isEmpty()) {
                    shifted++
                    candidate
                } else {
                    skipped++
                    continue
                }
            }
            val newAct = ScheduledActivityEntity(
                id = UUID.randomUUID().toString(),
                userId = user.uid,
                dateKey = dateKey,
                title = tpl.title,
                notes = tpl.notes,
                categoryId = tpl.categoryId,
                startMinute = chosenStart,
                durationMinutes = tpl.durationMinutes,
                status = ActivityStatus.PLANNED,
                priority = tpl.priority,
                sortOrder = tpl.sortOrder
            )
            dao.upsertActivity(newAct)
            scheduleNotificationsFor(newAct)
            added++
        }

        recalculatePerformance(dateKey)
        unlockAchievement("ARCHITECT")
        LevelUpWidgetProvider.requestUpdate(context)

        val msg = buildString {
            append("Added $added block")
            if (added != 1) append("s")
            if (shifted > 0) append(" ($shifted shifted to avoid overlap)")
            if (skipped > 0) append(", $skipped skipped (day full)")
        }
        return TemplateApplyResult(added, shifted, skipped, msg)
    }

    /**
     * Queries completed tasks and historical daily scores from Room to generate
     * a conflict-free suggested schedule for [targetDateKey].
     */
    suspend fun generateSmartSchedulePlan(
        targetDateKey: String,
        excludedCategoryIds: Set<String> = emptySet()
    ): SmartSchedulePlan {
        val allActivities = dao.getAllScheduledActivitiesOnce()
        val categories = dao.getCategoriesOnce()
        val performances = dao.getAllPerformancesOnce()
        val user = dao.getUser()
        val targetExisting = dao.getActivitiesForDateOnce(targetDateKey)
        val prevExisting = dao.getActivitiesForDateOnce(DateTimeUtils.shiftDateKey(targetDateKey, -1))
        val nextExisting = dao.getActivitiesForDateOnce(DateTimeUtils.shiftDateKey(targetDateKey, 1))

        return SmartSuggestionEngine.generatePlan(
            targetDateKey = targetDateKey,
            allActivities = allActivities,
            categories = categories,
            performances = performances,
            targetDayExisting = targetExisting,
            prevDayActivities = prevExisting,
            nextDayActivities = nextExisting,
            streakDays = user?.currentStreak ?: 0,
            excludedCategoryIds = excludedCategoryIds
        )
    }

    /**
     * Persists user-accepted smart schedule suggestions to Room non-destructively
     * (re-checking overlaps and shifting into the next free slot if needed).
     */
    suspend fun applySmartSuggestions(
        targetDateKey: String,
        blocks: List<SmartSuggestedBlock>
    ): Int {
        if (blocks.isEmpty()) return 0
        val user = dao.getUser() ?: SeedData.defaultUserProfile()
        var addedCount = 0
        val now = System.currentTimeMillis()

        for ((index, block) in blocks.withIndex()) {
            val overlaps = findOverlaps(null, targetDateKey, block.startMinute, block.durationMinutes)
            val chosenStart = if (overlaps.isEmpty()) {
                block.startMinute
            } else {
                val candidate = findNextAvailableSlot(targetDateKey, block.startMinute, block.durationMinutes)
                val check = findOverlaps(null, targetDateKey, candidate, block.durationMinutes)
                if (check.isEmpty()) candidate else continue
            }

            val entity = ScheduledActivityEntity(
                id = UUID.randomUUID().toString(),
                userId = user.uid,
                dateKey = targetDateKey,
                title = block.title,
                notes = block.notes,
                categoryId = block.categoryId,
                startMinute = chosenStart,
                durationMinutes = block.durationMinutes,
                status = ActivityStatus.PLANNED,
                priority = block.priority,
                sortOrder = chosenStart + index,
                locationName = block.locationName,
                latitude = block.latitude,
                longitude = block.longitude,
                createdAt = now,
                updatedAt = now
            )
            dao.upsertActivity(entity)
            scheduleNotificationsFor(entity)
            addedCount++
        }

        if (addedCount > 0) {
            recalculatePerformance(targetDateKey)
            unlockAchievement("ARCHITECT")
            LevelUpWidgetProvider.requestUpdate(context)
        }
        return addedCount
    }

    // Categories & Habits
    suspend fun saveCategory(existingId: String?, name: String, iconKey: String, colorHex: String) {
        val existing = existingId?.let { id -> dao.getCategoriesOnce().firstOrNull { it.id == id } }
        val category = ActivityCategoryEntity(
            id = existing?.id ?: "cat_${UUID.randomUUID()}",
            name = name.trim(),
            iconKey = iconKey,
            colorHex = colorHex,
            isDefault = existing?.isDefault ?: false,
            sortOrder = existing?.sortOrder ?: 50
        )
        dao.upsertCategory(category)
    }

    suspend fun deleteCustomCategory(categoryId: String) {
        dao.deleteCustomCategory(categoryId)
    }

    suspend fun saveHabit(title: String, categoryId: String, targetDaysPerWeek: Int) {
        val habit = HabitEntity(
            id = "hab_${UUID.randomUUID()}",
            title = title.trim(),
            categoryId = categoryId,
            targetDaysPerWeek = targetDaysPerWeek.coerceIn(1, 7)
        )
        dao.upsertHabit(habit)
    }

    suspend fun toggleHabitCompletedToday(habit: HabitEntity) {
        val today = DateTimeUtils.todayKey()
        if (habit.lastCompletedDateKey == today) return
        val yesterday = DateTimeUtils.shiftDateKey(today, -1)
        val newStreak = if (habit.lastCompletedDateKey == yesterday) habit.currentStreak + 1 else 1
        val updated = habit.copy(
            currentStreak = newStreak,
            bestStreak = maxOf(habit.bestStreak, newStreak),
            lastCompletedDateKey = today
        )
        dao.upsertHabit(updated)
        awardXp(25, "Habit: ${habit.title}", "HABIT", today)
    }

    suspend fun deleteHabit(id: String) = dao.deleteHabitById(id)

    // Timestamp-based Focus Timer
    suspend fun startTimer(
        activityId: String?,
        title: String,
        categoryId: String,
        plannedMinutes: Int,
        dateKey: String = DateTimeUtils.todayKey()
    ): TimerSessionEntity {
        val active = dao.getActiveTimerOnce()
        if (active != null && !active.isFinished) {
            dao.upsertTimerSession(
                active.copy(
                    isRunning = false,
                    isFinished = true,
                    accumulatedSeconds = ScheduleEngine.computeElapsedSeconds(active, System.currentTimeMillis()),
                    endedAt = System.currentTimeMillis()
                )
            )
            notificationScheduler.cancelFocusEnd(active.id)
        }
        val now = System.currentTimeMillis()
        val session = TimerSessionEntity(
            id = UUID.randomUUID().toString(),
            activityId = activityId,
            dateKey = dateKey,
            title = title,
            categoryId = categoryId,
            plannedSeconds = plannedMinutes.coerceAtLeast(1) * 60L,
            accumulatedSeconds = 0L,
            lastResumedEpochMs = now,
            isRunning = true,
            isFinished = false,
            startedAt = now
        )
        dao.upsertTimerSession(session)

        if (activityId != null) {
            dao.getScheduledActivityById(activityId)?.let { act ->
                if (act.status == ActivityStatus.PLANNED) {
                    dao.upsertActivity(act.copy(status = ActivityStatus.IN_PROGRESS, updatedAt = now))
                }
            }
        }

        val pref = dao.getNotificationPreferenceOnce() ?: SeedData.defaultNotificationPref()
        notificationScheduler.scheduleFocusEnd(session, pref)
        LevelUpWidgetProvider.requestUpdate(context)
        return session
    }

    suspend fun pauseTimer(sessionId: String) {
        val session = dao.getTimerById(sessionId) ?: return
        if (!session.isRunning) return
        val now = System.currentTimeMillis()
        val elapsed = ScheduleEngine.computeElapsedSeconds(session, now)
        val updated = session.copy(
            accumulatedSeconds = elapsed,
            lastResumedEpochMs = null,
            isRunning = false,
            overtimeSeconds = (elapsed - session.plannedSeconds).coerceAtLeast(0L)
        )
        dao.upsertTimerSession(updated)
        notificationScheduler.cancelFocusEnd(sessionId)
    }

    suspend fun resumeTimer(sessionId: String) {
        val session = dao.getTimerById(sessionId) ?: return
        if (session.isRunning || session.isFinished) return
        val now = System.currentTimeMillis()
        val updated = session.copy(
            lastResumedEpochMs = now,
            isRunning = true
        )
        dao.upsertTimerSession(updated)
        val pref = dao.getNotificationPreferenceOnce() ?: SeedData.defaultNotificationPref()
        notificationScheduler.scheduleFocusEnd(updated, pref)
    }

    suspend fun stopTimer(sessionId: String, markActivityCompleted: Boolean) {
        val session = dao.getTimerById(sessionId) ?: return
        val now = System.currentTimeMillis()
        val elapsed = ScheduleEngine.computeElapsedSeconds(session, now)
        val overtime = (elapsed - session.plannedSeconds).coerceAtLeast(0L)
        val finished = session.copy(
            accumulatedSeconds = elapsed,
            lastResumedEpochMs = null,
            isRunning = false,
            isFinished = true,
            overtimeSeconds = overtime,
            endedAt = now
        )
        dao.upsertTimerSession(finished)
        notificationScheduler.cancelFocusEnd(sessionId)

        val trackedMinutes = (elapsed / 60L).toInt().coerceAtLeast(1)
        val user = dao.getUser()
        if (user != null) {
            dao.upsertUser(user.copy(totalFocusMinutes = user.totalFocusMinutes + trackedMinutes, updatedAt = now))
        }

        if (session.activityId != null) {
            val act = dao.getScheduledActivityById(session.activityId)
            if (act != null) {
                val newTotalSpent = act.actualSpentSeconds + elapsed
                dao.upsertActivity(
                    act.copy(
                        actualSpentSeconds = newTotalSpent,
                        status = if (markActivityCompleted) ActivityStatus.COMPLETED else act.status,
                        completedAt = if (markActivityCompleted) now else act.completedAt,
                        updatedAt = now
                    )
                )
                if (markActivityCompleted && act.status != ActivityStatus.COMPLETED) {
                    val xp = ScheduleEngine.calculateXpForCompletion(act) + (trackedMinutes / 10) * 5
                    awardXp(xp, "Focused on ${act.title}", "TIMER", act.dateKey)
                } else {
                    val focusXp = (trackedMinutes * 2).coerceIn(5, 60)
                    awardXp(focusXp, "Focus session: ${session.title}", "TIMER", session.dateKey)
                }
                recalculatePerformance(act.dateKey)
            }
        } else {
            val focusXp = (trackedMinutes * 2).coerceIn(5, 60)
            awardXp(focusXp, "Focus session: ${session.title}", "TIMER", session.dateKey)
        }
        evaluateAchievements(session.dateKey)
        LevelUpWidgetProvider.requestUpdate(context)
    }

    suspend fun resetTimer(sessionId: String) {
        val session = dao.getTimerById(sessionId) ?: return
        notificationScheduler.cancelFocusEnd(sessionId)
        dao.upsertTimerSession(
            session.copy(
                accumulatedSeconds = 0L,
                lastResumedEpochMs = null,
                isRunning = false,
                isFinished = true,
                overtimeSeconds = 0L,
                endedAt = System.currentTimeMillis()
            )
        )
    }

    // Visual, Security & Notification preferences
    suspend fun setThemePalette(palette: ThemePalette) = prefsStore.setThemePalette(palette)
    suspend fun setFontChoice(font: FontChoice) = prefsStore.setFontChoice(font)
    suspend fun setCompactCards(compact: Boolean) = prefsStore.setCompactCards(compact)
    suspend fun setLockOnStartup(enabled: Boolean) = prefsStore.setLockOnStartup(enabled)
    suspend fun setLockSensitiveCategories(enabled: Boolean) = prefsStore.setLockSensitiveCategories(enabled)
    suspend fun setSensitiveCategoryIds(ids: Set<String>) = prefsStore.setSensitiveCategoryIds(ids)
    suspend fun toggleSensitiveCategory(categoryId: String) = prefsStore.toggleSensitiveCategory(categoryId)
    suspend fun setCategorySensitive(categoryId: String, sensitive: Boolean) {
        val current = prefsStore.securityPreferences.first().sensitiveCategoryIds
        val updated = if (sensitive) current + categoryId else current - categoryId
        prefsStore.setSensitiveCategoryIds(updated)
    }

    suspend fun setBackupPasscode(passcode: String) {
        val hash = if (passcode.isBlank()) "" else DataExportHelper.sha256(passcode.trim())
        prefsStore.setBackupPasscodeHash(hash)
    }

    fun verifyBackupPasscode(passcode: String, storedHash: String): Boolean {
        if (storedHash.isBlank()) return false
        return DataExportHelper.sha256(passcode.trim()) == storedHash
    }

    suspend fun saveNotificationPreference(pref: NotificationPreferenceEntity) {
        dao.upsertNotificationPreference(pref.copy(updatedAt = System.currentTimeMillis()))
        val today = DateTimeUtils.todayKey()
        val tomorrow = DateTimeUtils.shiftDateKey(today, 1)
        dao.getActivitiesInRangeOnce(today, tomorrow).forEach {
            notificationScheduler.scheduleForActivity(it, pref)
        }
        notificationScheduler.scheduleDailyReview(pref)
    }

    // Account, Privacy, Demo Mode, Export & Reset
    suspend fun updateProfile(displayName: String, email: String) {
        val user = dao.getUser() ?: SeedData.defaultUserProfile()
        val initials = displayName.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "LU" }
        val updated = user.copy(
            displayName = displayName.trim(),
            email = email.trim(),
            avatarInitials = initials,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUser(updated)
        refreshLeaderboardInternal(updated)
    }

    suspend fun registerOrSignIn(displayName: String, email: String, password: String): Result<UserProfileEntity> {
        if (email.isBlank() || !email.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid email address."))
        }
        if (password.length < 4) {
            return Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
        }
        val current = dao.getUser() ?: SeedData.defaultUserProfile()
        val hash = DataExportHelper.sha256(password)
        val cleanName = displayName.trim().ifBlank { current.displayName }
        val initials = cleanName.split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }.ifBlank { "LU" }
        val updated = current.copy(
            displayName = cleanName,
            email = email.trim(),
            avatarInitials = initials,
            passwordHash = hash,
            isLoggedIn = true,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUser(updated)
        refreshLeaderboardInternal(updated)
        return Result.success(updated)
    }

    suspend fun recoverPassword(email: String, newPassword: String): Result<Unit> {
        val current = dao.getUser() ?: return Result.failure(IllegalStateException("No account found."))
        if (!current.email.equals(email.trim(), ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Email does not match the registered profile."))
        }
        if (newPassword.length < 4) {
            return Result.failure(IllegalArgumentException("New password must be at least 4 characters."))
        }
        dao.upsertUser(current.copy(passwordHash = DataExportHelper.sha256(newPassword), updatedAt = System.currentTimeMillis()))
        return Result.success(Unit)
    }

    suspend fun setLoggedIn(loggedIn: Boolean) {
        val current = dao.getUser() ?: return
        dao.upsertUser(current.copy(isLoggedIn = loggedIn, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setLeaderboardOptIn(optIn: Boolean) {
        val current = dao.getUser() ?: return
        val updated = current.copy(leaderboardOptIn = optIn, updatedAt = System.currentTimeMillis())
        dao.upsertUser(updated)
        refreshLeaderboardInternal(updated)
    }

    suspend fun setDemoMode(enabled: Boolean) {
        val current = dao.getUser() ?: SeedData.defaultUserProfile()
        val today = DateTimeUtils.todayKey()
        dao.clearAllScheduledActivities()
        dao.clearPerformances()
        dao.clearHabits()
        dao.clearXpTransactions()

        if (enabled) {
            val (demoActs, demoPerfs) = SeedData.buildDemoActivities(current.uid, today)
            dao.upsertActivities(demoActs)
            demoPerfs.forEach { dao.upsertPerformance(it) }
            SeedData.buildDemoHabits().forEach { dao.upsertHabit(it) }
            val demoXp = SeedData.buildDemoXpTransactions(current.uid, today)
            demoXp.forEach { dao.insertXpTransaction(it) }
            val totalXp = demoXp.sumOf { it.amount }
            val updated = current.copy(
                isDemoMode = true,
                totalXp = totalXp,
                level = ScheduleEngine.levelForXp(totalXp),
                currentStreak = 6,
                longestStreak = 9,
                totalFocusMinutes = 240,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsertUser(updated)
            refreshLeaderboardInternal(updated)
        } else {
            val resetUser = current.copy(
                isDemoMode = false,
                totalXp = 0,
                level = 1,
                currentStreak = 0,
                longestStreak = 0,
                totalFocusMinutes = 0,
                updatedAt = System.currentTimeMillis()
            )
            dao.upsertUser(resetUser)
            refreshLeaderboardInternal(resetUser)
        }
        val pref = dao.getNotificationPreferenceOnce() ?: SeedData.defaultNotificationPref()
        dao.getActivitiesForDateOnce(today).forEach { notificationScheduler.scheduleForActivity(it, pref) }
        LevelUpWidgetProvider.requestUpdate(context)
    }

    suspend fun syncSnapshotLocally(): Long {
        val now = System.currentTimeMillis()
        prefsStore.setLastSyncEpochMs(now)
        val user = dao.getUser()
        if (user != null) {
            refreshLeaderboardInternal(user)
        }
        return now
    }

    suspend fun exportAllDataJson(): String {
        return DataExportHelper.buildExportJson(
            user = dao.getUser(),
            categories = dao.getCategoriesOnce(),
            activities = dao.getAllScheduledActivitiesOnce(),
            templates = dao.getTemplatesOnce(),
            performances = dao.getAllPerformancesOnce(),
            habits = dao.getHabitsOnce(),
            achievements = dao.getAchievementsOnce(),
            xpTransactions = dao.getAllXpTransactionsOnce()
        )
    }

    suspend fun deleteAccountAndAllData() {
        dao.getAllScheduledActivitiesOnce().forEach { notificationScheduler.cancelForActivity(it.id) }
        dao.clearAllScheduledActivities()
        dao.clearTimerSessions()
        dao.clearPerformances()
        dao.clearHabits()
        dao.clearXpTransactions()
        dao.clearLeaderboard()
        dao.upsertAchievements(SeedData.defaultAchievements())
        val fresh = SeedData.defaultUserProfile()
        dao.upsertUser(fresh)
        refreshLeaderboardInternal(fresh)
        LevelUpWidgetProvider.requestUpdate(context)
    }

    private suspend fun scheduleNotificationsFor(activity: ScheduledActivityEntity) {
        val pref = dao.getNotificationPreferenceOnce() ?: SeedData.defaultNotificationPref()
        notificationScheduler.scheduleForActivity(activity, pref)
    }

    private suspend fun recalculatePerformance(dateKey: String) {
        val list = dao.getActivitiesForDateOnce(dateKey)
        val user = dao.getUser()
        val breakdown = ScheduleEngine.calculatePerformance(list, user?.currentStreak ?: 0)
        val highTotal = list.count { it.status != ActivityStatus.SKIPPED && it.priority == ActivityPriority.HIGH }
        val highDone = list.count { it.status == ActivityStatus.COMPLETED && it.priority == ActivityPriority.HIGH }
        val plannedMins = list.filter { it.status != ActivityStatus.SKIPPED }.sumOf { it.durationMinutes }
        val actualMins = list.sumOf { (it.actualSpentSeconds / 60L).toInt() }

        dao.upsertPerformance(
            DailyPerformanceEntity(
                dateKey = dateKey,
                totalPlannedCount = breakdown.totalCount,
                completedCount = breakdown.completedCount,
                onTimeCount = (breakdown.completedCount * breakdown.onTimePercent) / 100,
                highPriorityTotal = highTotal,
                highPriorityCompleted = highDone,
                plannedMinutes = plannedMins,
                actualMinutes = actualMins,
                score = breakdown.score
            )
        )
        updateUserStreakFromPerformances()
    }

    private suspend fun updateUserStreakFromPerformances() {
        val user = dao.getUser() ?: return
        val today = DateTimeUtils.todayKey()
        var streak = 0
        for (offset in 0 downTo -30) {
            val key = DateTimeUtils.shiftDateKey(today, offset)
            val perf = dao.getPerformanceForDateOnce(key)
            if (perf != null && perf.completedCount > 0) {
                streak++
            } else if (offset < 0) {
                break
            }
        }
        val updated = user.copy(
            currentStreak = streak,
            longestStreak = maxOf(user.longestStreak, streak),
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUser(updated)
        refreshLeaderboardInternal(updated)
    }

    private suspend fun awardXp(amount: Int, reason: String, sourceType: String, dateKey: String) {
        val user = dao.getUser() ?: return
        dao.insertXpTransaction(
            XpTransactionEntity(
                id = UUID.randomUUID().toString(),
                userId = user.uid,
                amount = amount,
                reason = reason,
                sourceType = sourceType,
                dateKey = dateKey
            )
        )
        val newXp = (user.totalXp + amount).coerceAtLeast(0)
        val newLevel = ScheduleEngine.levelForXp(newXp)
        val updated = user.copy(
            totalXp = newXp,
            level = newLevel,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertUser(updated)
        refreshLeaderboardInternal(updated)
    }

    private suspend fun evaluateAchievements(dateKey: String) {
        val allActs = dao.getAllScheduledActivitiesOnce()
        val completedCount = allActs.count { it.status == ActivityStatus.COMPLETED }
        if (completedCount >= 1) unlockAchievement("FIRST_STEP")
        if (completedCount >= 5) unlockAchievement("EARLY_BIRD")

        val user = dao.getUser()
        if (user != null) {
            if (user.totalFocusMinutes >= 60) unlockAchievement("FOCUS_HOUR")
            if (user.currentStreak >= 3) unlockAchievement("STREAK_3")
            if (user.currentStreak >= 7) unlockAchievement("STREAK_7")
            if (user.level >= 5) unlockAchievement("LEVEL_5")
        }

        val perf = dao.getPerformanceForDateOnce(dateKey)
        if (perf != null && perf.score >= 90) {
            unlockAchievement("PERFECT_DAY")
        }
    }

    private suspend fun unlockAchievement(code: String) {
        val list = dao.getAchievementsOnce()
        val target = list.firstOrNull { it.code == code && it.unlockedAt == null } ?: return
        dao.updateAchievement(target.copy(unlockedAt = System.currentTimeMillis()))
        awardXp(target.xpReward, "Badge unlocked: ${target.title}", "ACHIEVEMENT", DateTimeUtils.todayKey())
    }

    private suspend fun refreshLeaderboardInternal(user: UserProfileEntity) {
        val today = DateTimeUtils.todayKey()
        val weekStart = DateTimeUtils.shiftDateKey(today, -6)
        val monthStart = DateTimeUtils.shiftDateKey(today, -29)

        val todayXp = dao.getXpTransactionsInRangeOnce(today, today).sumOf { it.amount }
        val weekXp = dao.getXpTransactionsInRangeOnce(weekStart, today).sumOf { it.amount }
        val monthXp = dao.getXpTransactionsInRangeOnce(monthStart, today).sumOf { it.amount }

        dao.clearLeaderboard()
        dao.upsertLeaderboardEntries(SeedData.defaultLeaderboardPeers(user, todayXp, weekXp, monthXp))
    }
}
