package com.example.data.local

import com.example.data.model.AchievementEntity
import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.ActivityPriority
import com.example.data.model.ActivityStatus
import com.example.data.model.DailyPerformanceEntity
import com.example.data.model.FocusPlacePreset
import com.example.data.model.HabitEntity
import com.example.data.model.LeaderboardEntryEntity
import com.example.data.model.NotificationPreferenceEntity
import com.example.data.model.ScheduleTemplateEntity
import com.example.data.model.ScheduledActivityEntity
import com.example.data.model.TemplateActivityEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.XpTransactionEntity
import com.example.domain.DateTimeUtils
import com.example.domain.ScheduleEngine
import java.util.UUID

object SeedData {

    fun defaultCategories(): List<ActivityCategoryEntity> = listOf(
        ActivityCategoryEntity("cat_sleep", "Sleep", "bedtime", "#6366F1", isDefault = true, sortOrder = 0),
        ActivityCategoryEntity("cat_breakfast", "Breakfast", "restaurant", "#F59E0B", isDefault = true, sortOrder = 1),
        ActivityCategoryEntity("cat_studying", "Studying", "menu_book", "#3B82F6", isDefault = true, sortOrder = 2),
        ActivityCategoryEntity("cat_deep_work", "Deep Work", "bolt", "#FF6B4A", isDefault = true, sortOrder = 3),
        ActivityCategoryEntity("cat_exercise", "Exercise", "fitness_center", "#10B981", isDefault = true, sortOrder = 4),
        ActivityCategoryEntity("cat_reading", "Reading", "auto_stories", "#8B5CF6", isDefault = true, sortOrder = 5),
        ActivityCategoryEntity("cat_lunch", "Lunch", "lunch_dining", "#F97316", isDefault = true, sortOrder = 6),
        ActivityCategoryEntity("cat_break", "Break", "coffee", "#14B8A6", isDefault = true, sortOrder = 7),
        ActivityCategoryEntity("cat_personal", "Personal Work", "laptop_mac", "#EC4899", isDefault = true, sortOrder = 8),
        ActivityCategoryEntity("cat_family", "Family", "favorite", "#EF4444", isDefault = true, sortOrder = 9),
        ActivityCategoryEntity("cat_commuting", "Commuting", "directions_car", "#64748B", isDefault = true, sortOrder = 10),
        ActivityCategoryEntity("cat_other", "Other", "category", "#06B6D4", isDefault = true, sortOrder = 11)
    )

    fun defaultTemplates(): List<ScheduleTemplateEntity> = listOf(
        ScheduleTemplateEntity(
            id = "tpl_morning",
            name = "Morning Routine",
            description = "Energizing start with hydration, movement, breakfast, and top focus block.",
            accentColorHex = "#F59E0B",
            isDefault = true
        ),
        ScheduleTemplateEntity(
            id = "tpl_focus",
            name = "Focus Day",
            description = "High-leverage deep work blocks protected with mindful breaks.",
            accentColorHex = "#FF6B4A",
            isDefault = true
        ),
        ScheduleTemplateEntity(
            id = "tpl_study",
            name = "Study Day",
            description = "Structured spaced-repetition study blocks, active recall, and reading.",
            accentColorHex = "#3B82F6",
            isDefault = true
        ),
        ScheduleTemplateEntity(
            id = "tpl_ideal",
            name = "Ideal Week",
            description = "Balanced full-day blueprint from dawn routine through evening wind-down.",
            accentColorHex = "#10B981",
            isDefault = true
        ),
        ScheduleTemplateEntity(
            id = "tpl_rest",
            name = "Rest and Recharge",
            description = "Gentle recovery rhythm with reading, light walks, and quality family time.",
            accentColorHex = "#14B8A6",
            isDefault = true
        ),
        ScheduleTemplateEntity(
            id = "tpl_weekend",
            name = "Weekend Routine",
            description = "Slow brunch, creative personal projects, fitness, and social evening.",
            accentColorHex = "#8B5CF6",
            isDefault = true
        )
    )

    fun defaultTemplateActivities(): List<TemplateActivityEntity> = listOf(
        // Morning Routine
        TemplateActivityEntity("ta_m1", "tpl_morning", "Morning Stretch & Mobility", "cat_exercise", 7 * 60, 30, ActivityPriority.MEDIUM, "Light warmup & hydration", 0),
        TemplateActivityEntity("ta_m2", "tpl_morning", "Nourishing Breakfast", "cat_breakfast", 7 * 60 + 30, 30, ActivityPriority.LOW, "Protein + coffee/tea", 1),
        TemplateActivityEntity("ta_m3", "tpl_morning", "Morning Reading", "cat_reading", 8 * 60, 30, ActivityPriority.MEDIUM, "20 pages non-fiction", 2),
        TemplateActivityEntity("ta_m4", "tpl_morning", "Priority Deep Work Sprint", "cat_deep_work", 8 * 60 + 30, 90, ActivityPriority.HIGH, "No notifications", 3),

        // Focus Day
        TemplateActivityEntity("ta_f1", "tpl_focus", "Deep Work Block I", "cat_deep_work", 9 * 60, 120, ActivityPriority.HIGH, "Ship core milestone", 0),
        TemplateActivityEntity("ta_f2", "tpl_focus", "Mindful Walk & Coffee", "cat_break", 11 * 60, 20, ActivityPriority.LOW, "Step away from screens", 1),
        TemplateActivityEntity("ta_f3", "tpl_focus", "Midday Lunch & Reset", "cat_lunch", 12 * 60 + 30, 45, ActivityPriority.MEDIUM, "", 2),
        TemplateActivityEntity("ta_f4", "tpl_focus", "Deep Work Block II", "cat_deep_work", 13 * 60 + 30, 90, ActivityPriority.HIGH, "Review & iterate", 3),
        TemplateActivityEntity("ta_f5", "tpl_focus", "Admin & Personal Wrap-up", "cat_personal", 16 * 60, 45, ActivityPriority.MEDIUM, "Inbox zero", 4),

        // Study Day
        TemplateActivityEntity("ta_s1", "tpl_study", "Active Recall Session", "cat_studying", 8 * 60 + 30, 90, ActivityPriority.HIGH, "Core concepts & notes", 0),
        TemplateActivityEntity("ta_s2", "tpl_study", "Short Brain Break", "cat_break", 10 * 60, 15, ActivityPriority.LOW, "Hydrate & stretch", 1),
        TemplateActivityEntity("ta_s3", "tpl_study", "Problem Solving Practice", "cat_studying", 10 * 60 + 15, 90, ActivityPriority.HIGH, "Timed drills", 2),
        TemplateActivityEntity("ta_s4", "tpl_study", "Lunch Break", "cat_lunch", 12 * 60, 45, ActivityPriority.LOW, "", 3),
        TemplateActivityEntity("ta_s5", "tpl_study", "Literature & Summary Reading", "cat_reading", 14 * 60, 60, ActivityPriority.MEDIUM, "Synthesize key takeaways", 4),

        // Ideal Week
        TemplateActivityEntity("ta_i1", "tpl_ideal", "Morning Workout", "cat_exercise", 6 * 60 + 45, 45, ActivityPriority.HIGH, "Cardio + strength", 0),
        TemplateActivityEntity("ta_i2", "tpl_ideal", "Breakfast & Daily Plan", "cat_breakfast", 7 * 60 + 30, 30, ActivityPriority.MEDIUM, "Set top 3 outcomes", 1),
        TemplateActivityEntity("ta_i3", "tpl_ideal", "Commute & Audio Learning", "cat_commuting", 8 * 60 + 15, 30, ActivityPriority.LOW, "", 2),
        TemplateActivityEntity("ta_i4", "tpl_ideal", "Core Deep Work", "cat_deep_work", 9 * 60, 120, ActivityPriority.HIGH, "", 3),
        TemplateActivityEntity("ta_i5", "tpl_ideal", "Lunch & Recharge", "cat_lunch", 12 * 60 + 30, 45, ActivityPriority.LOW, "", 4),
        TemplateActivityEntity("ta_i6", "tpl_ideal", "Family Dinner & Unwind", "cat_family", 18 * 60 + 30, 75, ActivityPriority.MEDIUM, "Screen-free evening", 5),
        TemplateActivityEntity("ta_i7", "tpl_ideal", "Night Sleep", "cat_sleep", 23 * 60, 450, ActivityPriority.HIGH, "7.5h restorative sleep", 6),

        // Rest and Recharge
        TemplateActivityEntity("ta_r1", "tpl_rest", "Slow Breakfast", "cat_breakfast", 9 * 60, 45, ActivityPriority.LOW, "No rush", 0),
        TemplateActivityEntity("ta_r2", "tpl_rest", "Leisure Reading", "cat_reading", 10 * 60 + 30, 60, ActivityPriority.MEDIUM, "Fiction or biography", 1),
        TemplateActivityEntity("ta_r3", "tpl_rest", "Nature Walk", "cat_exercise", 15 * 60, 45, ActivityPriority.MEDIUM, "Sunlight & fresh air", 2),
        TemplateActivityEntity("ta_r4", "tpl_rest", "Family Evening", "cat_family", 18 * 60, 90, ActivityPriority.MEDIUM, "", 3),

        // Weekend Routine
        TemplateActivityEntity("ta_w1", "tpl_weekend", "Weekend Brunch", "cat_breakfast", 9 * 60 + 30, 45, ActivityPriority.LOW, "", 0),
        TemplateActivityEntity("ta_w2", "tpl_weekend", "Creative Personal Project", "cat_personal", 11 * 60, 90, ActivityPriority.MEDIUM, "Passion build", 1),
        TemplateActivityEntity("ta_w3", "tpl_weekend", "Outdoor Run / Gym", "cat_exercise", 16 * 60, 60, ActivityPriority.HIGH, "", 2),
        TemplateActivityEntity("ta_w4", "tpl_weekend", "Friends & Family Time", "cat_family", 19 * 60, 120, ActivityPriority.MEDIUM, "", 3)
    )

    fun defaultAchievements(): List<AchievementEntity> = listOf(
        AchievementEntity("ach_first_step", "FIRST_STEP", "First Block Done", "Complete your first scheduled activity.", "check_circle", 50, null),
        AchievementEntity("ach_early_bird", "EARLY_BIRD", "On-Time Pro", "Complete 5 activities within their scheduled window.", "schedule", 100, null),
        AchievementEntity("ach_focus_60", "FOCUS_HOUR", "Deep Diver", "Log 60 minutes in Focus Timer mode.", "timer", 120, null),
        AchievementEntity("ach_streak_3", "STREAK_3", "Momentum Builder", "Reach a 3-day productivity streak.", "local_fire_department", 150, null),
        AchievementEntity("ach_streak_7", "STREAK_7", "Week Warrior", "Maintain a 7-day productivity streak.", "whatshot", 300, null),
        AchievementEntity("ach_perfect_day", "PERFECT_DAY", "Peak Score 90+", "Achieve a daily performance score of 90 or higher.", "emoji_events", 250, null),
        AchievementEntity("ach_template_user", "ARCHITECT", "Routine Architect", "Apply or create a schedule template.", "auto_awesome", 80, null),
        AchievementEntity("ach_level_5", "LEVEL_5", "Level 5 Climber", "Reach Level 5 by earning 1,600+ XP.", "trending_up", 400, null)
    )

    fun defaultUserProfile(): UserProfileEntity = UserProfileEntity(
        uid = "local_user_1",
        displayName = "Alex Mercer",
        email = "alex@levelup.local",
        avatarInitials = "AM",
        passwordHash = "",
        totalXp = 0,
        level = 1,
        currentStreak = 0,
        longestStreak = 0,
        totalFocusMinutes = 0,
        leaderboardOptIn = true,
        isLoggedIn = true,
        isDemoMode = false
    )

    fun defaultLeaderboardPeers(currentUser: UserProfileEntity, todayDailyXp: Int, weeklyXp: Int, monthlyXp: Int): List<LeaderboardEntryEntity> {
        val entries = mutableListOf(
            LeaderboardEntryEntity("lb_peer_1", "peer_maya", "Maya Lin", "ML", 215, 1180, 4420, 7, 12, false),
            LeaderboardEntryEntity("lb_peer_2", "peer_liam", "Liam O'Connor", "LO", 180, 990, 3810, 6, 8, false),
            LeaderboardEntryEntity("lb_peer_3", "peer_priya", "Priya Nair", "PN", 145, 860, 3150, 5, 6, false),
            LeaderboardEntryEntity("lb_peer_4", "peer_kenji", "Kenji Sato", "KS", 95, 620, 2290, 4, 4, false),
            LeaderboardEntryEntity("lb_peer_5", "peer_sofia", "Sofia Vega", "SV", 65, 410, 1520, 3, 2, false)
        )
        if (currentUser.leaderboardOptIn) {
            entries.add(
                LeaderboardEntryEntity(
                    id = "lb_current_user",
                    userId = currentUser.uid,
                    displayName = "${currentUser.displayName} (You)",
                    avatarInitials = currentUser.avatarInitials,
                    dailyXp = todayDailyXp,
                    weeklyXp = weeklyXp,
                    monthlyXp = monthlyXp,
                    level = currentUser.level,
                    streak = currentUser.currentStreak,
                    isCurrentUser = true
                )
            )
        }
        return entries
    }

    /**
     * Explicit Demo Mode dataset: ONLY populated when the user explicitly activates Demo Mode.
     */
    fun buildDemoActivities(userId: String, todayKey: String): Pair<List<ScheduledActivityEntity>, List<DailyPerformanceEntity>> {
        val nowMinute = DateTimeUtils.currentMinuteOfDay()
        val items = mutableListOf<ScheduledActivityEntity>()
        val perfs = mutableListOf<DailyPerformanceEntity>()

        for (offset in -6..0) {
            val dateKey = DateTimeUtils.shiftDateKey(todayKey, offset)
            val isToday = offset == 0
            val dayItems = listOf(
                Triple("Morning Strength & Stretch", "cat_exercise", 7 * 60 to 45),
                Triple("Healthy Breakfast", "cat_breakfast", 7 * 60 + 45 to 30),
                Triple("Deep Work: Core Project", "cat_deep_work", 9 * 60 to 120),
                Triple("Mindful Coffee Break", "cat_break", 11 * 60 to 20),
                Triple("Study & System Design", "cat_studying", 13 * 60 + 30 to 90),
                Triple("Evening Reading", "cat_reading", 19 * 60 to 45),
                Triple("Restorative Sleep", "cat_sleep", 23 * 60 to 450)
            ).mapIndexed { idx, (title, catId, slot) ->
                val (start, dur) = slot
                val status = when {
                    !isToday -> if ((idx + offset) % 6 == 0) ActivityStatus.SKIPPED else ActivityStatus.COMPLETED
                    start + dur <= nowMinute -> ActivityStatus.COMPLETED
                    nowMinute in start until (start + dur) -> ActivityStatus.IN_PROGRESS
                    else -> ActivityStatus.PLANNED
                }
                val endEpoch = DateTimeUtils.toEpochMillis(dateKey, start + dur)
                val demoPlace = when (catId) {
                    "cat_exercise" -> FocusPlacePreset("pl_park", "City Park Loop Track", "Outdoor fitness trail", "cat_exercise", 37.7694, -122.4862)
                    "cat_deep_work" -> FocusPlacePreset("pl_cowork", "Civic Co-Working Hub", "Quiet fiber workspace", "cat_deep_work", 37.7897, -122.4014)
                    "cat_studying" -> FocusPlacePreset("pl_library", "Central Public Library", "3rd floor quiet reading room", "cat_studying", 37.7793, -122.4159)
                    "cat_break" -> FocusPlacePreset("pl_cafe", "Botanical Roast Cafe", "Sunlight courtyard break", "cat_break", 37.7749, -122.4194)
                    else -> null
                }
                ScheduledActivityEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    dateKey = dateKey,
                    title = title,
                    notes = if (isToday) "Demo schedule block" else "Completed in demo history",
                    categoryId = catId,
                    startMinute = start,
                    durationMinutes = dur,
                    status = status,
                    priority = if (idx == 2 || idx == 0) ActivityPriority.HIGH else ActivityPriority.MEDIUM,
                    actualSpentSeconds = if (status == ActivityStatus.COMPLETED) dur * 60L else 0L,
                    completedAt = if (status == ActivityStatus.COMPLETED) endEpoch else null,
                    sortOrder = idx,
                    locationName = demoPlace?.name.orEmpty(),
                    latitude = demoPlace?.latitude,
                    longitude = demoPlace?.longitude
                )
            }
            items.addAll(dayItems)
            val breakdown = ScheduleEngine.calculatePerformance(dayItems, streakDays = (offset + 7).coerceAtLeast(1))
            perfs.add(
                DailyPerformanceEntity(
                    dateKey = dateKey,
                    totalPlannedCount = breakdown.totalCount,
                    completedCount = breakdown.completedCount,
                    onTimeCount = (breakdown.completedCount * breakdown.onTimePercent) / 100,
                    highPriorityTotal = dayItems.count { it.priority == ActivityPriority.HIGH },
                    highPriorityCompleted = dayItems.count { it.priority == ActivityPriority.HIGH && it.status == ActivityStatus.COMPLETED },
                    plannedMinutes = dayItems.sumOf { it.durationMinutes },
                    actualMinutes = dayItems.sumOf { (it.actualSpentSeconds / 60L).toInt() },
                    score = breakdown.score
                )
            )
        }
        return items to perfs
    }

    fun buildDemoHabits(): List<HabitEntity> = listOf(
        HabitEntity("hab_demo_1", "30m Morning Movement", "cat_exercise", 5, 4, 9, DateTimeUtils.todayKey()),
        HabitEntity("hab_demo_2", "20 Pages Deep Reading", "cat_reading", 7, 6, 12, DateTimeUtils.todayKey()),
        HabitEntity("hab_demo_3", "Zero-Distraction Deep Work", "cat_deep_work", 5, 3, 7, DateTimeUtils.todayKey())
    )

    fun buildDemoXpTransactions(userId: String, todayKey: String): List<XpTransactionEntity> = listOf(
        XpTransactionEntity(UUID.randomUUID().toString(), userId, 75, "Completed Deep Work: Core Project", "ACTIVITY", todayKey),
        XpTransactionEntity(UUID.randomUUID().toString(), userId, 55, "Completed Morning Strength & Stretch", "ACTIVITY", todayKey),
        XpTransactionEntity(UUID.randomUUID().toString(), userId, 45, "Focus Session Completed", "TIMER", DateTimeUtils.shiftDateKey(todayKey, -1)),
        XpTransactionEntity(UUID.randomUUID().toString(), userId, 120, "Weekly Consistency Milestone", "STREAK", DateTimeUtils.shiftDateKey(todayKey, -2))
    )

    fun defaultNotificationPref(): NotificationPreferenceEntity = NotificationPreferenceEntity()

    fun defaultFocusPlaces(): List<FocusPlacePreset> = listOf(
        FocusPlacePreset(
            id = "place_library",
            name = "Central Public Library",
            subtitle = "Silent study desks & research archives",
            suggestedCategoryId = "cat_studying",
            latitude = 37.7793,
            longitude = -122.4159
        ),
        FocusPlacePreset(
            id = "place_cowork",
            name = "Civic Co-Working Hub",
            subtitle = "Ergonomic deep work booths",
            suggestedCategoryId = "cat_deep_work",
            latitude = 37.7897,
            longitude = -122.4014
        ),
        FocusPlacePreset(
            id = "place_park",
            name = "City Park Loop Track",
            subtitle = "Running path & outdoor mobility",
            suggestedCategoryId = "cat_exercise",
            latitude = 37.7694,
            longitude = -122.4862
        ),
        FocusPlacePreset(
            id = "place_cafe",
            name = "Botanical Roast Cafe",
            subtitle = "Midday coffee & reading nook",
            suggestedCategoryId = "cat_reading",
            latitude = 37.7749,
            longitude = -122.4194
        ),
        FocusPlacePreset(
            id = "place_studio",
            name = "Home Focus Studio",
            subtitle = "Distraction-free desk setup",
            suggestedCategoryId = "cat_personal",
            latitude = 37.7612,
            longitude = -122.4350
        )
    )
}
