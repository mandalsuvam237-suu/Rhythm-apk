package com.example.domain

import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.ActivityPriority
import com.example.data.model.ActivityStatus
import com.example.data.model.DailyPerformanceEntity
import com.example.data.model.ScheduledActivityEntity
import kotlin.math.roundToInt

data class SmartSuggestedBlock(
    val id: String,
    val title: String,
    val notes: String,
    val categoryId: String,
    val categoryName: String,
    val categoryColorHex: String,
    val startMinute: Int,
    val durationMinutes: Int,
    val priority: ActivityPriority,
    val rationale: String,
    val scoreDriver: String,
    val historicalMatchCount: Int,
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class SmartSchedulePlan(
    val targetDateKey: String,
    val analyzedCompletedCount: Int,
    val analyzedHistoryDays: Int,
    val peakFocusHour: Int,
    val onTimeRatePercent: Int,
    val baselineProjectedScore: Int,
    val suggestedProjectedScore: Int,
    val insights: List<String>,
    val suggestions: List<SmartSuggestedBlock>,
    val isUsingStarterSignals: Boolean
)

/**
 * Analyzes completed tasks and performance history in Room to synthesize a conflict-free
 * upcoming-day schedule optimized for Rhythm's 0–100 productivity scoring formula.
 */
object SmartSuggestionEngine {

    private data class HabitCandidate(
        val title: String,
        val categoryId: String,
        val avgStartMinute: Int,
        val calibratedDurationMinutes: Int,
        val completionCount: Int,
        val highPriorityCount: Int,
        val locationName: String,
        val latitude: Double?,
        val longitude: Double?
    )

    fun generatePlan(
        targetDateKey: String,
        allActivities: List<ScheduledActivityEntity>,
        categories: List<ActivityCategoryEntity>,
        performances: List<DailyPerformanceEntity>,
        targetDayExisting: List<ScheduledActivityEntity>,
        prevDayActivities: List<ScheduledActivityEntity>,
        nextDayActivities: List<ScheduledActivityEntity>,
        streakDays: Int,
        excludedCategoryIds: Set<String> = emptySet()
    ): SmartSchedulePlan {
        val catMap = categories.associateBy { it.id }
        val fallbackCat = categories.firstOrNull { it.id == "cat_deep_work" }
            ?: categories.firstOrNull()
            ?: ActivityCategoryEntity("cat_deep_work", "Deep Work", "bolt", "#FF6B4A", true, 3)

        val visibleCompleted = allActivities.filter {
            it.status == ActivityStatus.COMPLETED && it.categoryId !in excludedCategoryIds
        }

        val analyzedHistoryDays = visibleCompleted.map { it.dateKey }.distinct().size
        val onTimeCompleted = visibleCompleted.count { act ->
            val completedAt = act.completedAt ?: return@count true
            val scheduledEndMs = DateTimeUtils.toEpochMillis(act.dateKey, act.endMinute)
            completedAt <= scheduledEndMs + 15 * 60_000L
        }
        val onTimeRatePercent = if (visibleCompleted.isEmpty()) {
            0
        } else {
            ((onTimeCompleted.toDouble() / visibleCompleted.size.toDouble()) * 100.0)
                .roundToInt()
                .coerceIn(0, 100)
        }

        // Find peak completion hour across completed tasks (default 09:00 if no completed history)
        val peakFocusHour = if (visibleCompleted.isEmpty()) {
            9
        } else {
            visibleCompleted
                .groupBy { (it.startMinute / 60).coerceIn(6, 21) }
                .maxByOrNull { (_, group) ->
                    val onTimeScore = group.count { act ->
                        val completedAt = act.completedAt ?: return@count true
                        completedAt <= DateTimeUtils.toEpochMillis(act.dateKey, act.endMinute) + 15 * 60_000L
                    }
                    group.size * 2 + onTimeScore
                }?.key ?: 9
        }

        // Build candidate habits from completed tasks in Room
        val candidates = visibleCompleted
            .groupBy { "${it.categoryId}::${it.title.trim().lowercase()}" }
            .values
            .map { group ->
                val latest = group.maxByOrNull { it.completedAt ?: it.updatedAt } ?: group.first()
                val avgMinutes = group.map { act ->
                    val trackedMins = (act.actualSpentSeconds / 60L).toInt()
                    if (trackedMins > 0) trackedMins else act.durationMinutes
                }.average()
                val roundedDuration = (((avgMinutes + 2.5) / 5.0).toInt() * 5).coerceIn(15, 120)
                val avgStart = group.map { it.startMinute }.average().roundToInt()
                val located = group.lastOrNull { it.locationName.isNotBlank() }
                HabitCandidate(
                    title = latest.title.trim(),
                    categoryId = latest.categoryId,
                    avgStartMinute = (avgStart / 5) * 5,
                    calibratedDurationMinutes = roundedDuration,
                    completionCount = group.size,
                    highPriorityCount = group.count { it.priority == ActivityPriority.HIGH },
                    locationName = located?.locationName ?: "",
                    latitude = located?.latitude,
                    longitude = located?.longitude
                )
            }
            .sortedWith(
                compareByDescending<HabitCandidate> { it.completionCount }
                    .thenByDescending { it.highPriorityCount }
                    .thenByDescending { it.calibratedDurationMinutes }
            )

        val occupiedSameDay = targetDayExisting.toMutableList()
        val suggestions = mutableListOf<SmartSuggestedBlock>()

        fun allocateBlock(
            idSuffix: String,
            title: String,
            notes: String,
            categoryId: String,
            preferredStart: Int,
            durationMinutes: Int,
            priority: ActivityPriority,
            rationale: String,
            scoreDriver: String,
            matchCount: Int,
            locationName: String = "",
            latitude: Double? = null,
            longitude: Double? = null
        ) {
            val slot = ScheduleEngine.findNextAvailableSlot(
                preferredStartMinute = preferredStart.coerceIn(6 * 60, 22 * 60),
                durationMinutes = durationMinutes,
                sameDayActivities = occupiedSameDay,
                prevDayActivities = prevDayActivities,
                nextDayActivities = nextDayActivities
            )
            val overlapsAfterSearch = ScheduleEngine.findOverlaps(
                candidateId = null,
                candidateDateKey = targetDateKey,
                candidateStartMinute = slot,
                candidateDurationMinutes = durationMinutes,
                sameDayActivities = occupiedSameDay,
                prevDayActivities = prevDayActivities,
                nextDayActivities = nextDayActivities
            )
            if (overlapsAfterSearch.isNotEmpty()) return

            val cat = catMap[categoryId] ?: fallbackCat
            val suggested = SmartSuggestedBlock(
                id = "smart_${targetDateKey}_$idSuffix",
                title = title,
                notes = notes,
                categoryId = cat.id,
                categoryName = cat.name,
                categoryColorHex = cat.colorHex,
                startMinute = slot,
                durationMinutes = durationMinutes,
                priority = priority,
                rationale = rationale,
                scoreDriver = scoreDriver,
                historicalMatchCount = matchCount,
                locationName = locationName,
                latitude = latitude,
                longitude = longitude
            )
            suggestions += suggested
            occupiedSameDay += ScheduledActivityEntity(
                id = suggested.id,
                userId = "sim",
                dateKey = targetDateKey,
                title = suggested.title,
                categoryId = suggested.categoryId,
                startMinute = suggested.startMinute,
                durationMinutes = suggested.durationMinutes,
                priority = suggested.priority
            )
        }

        val isStarter = visibleCompleted.isEmpty()

        // 1. Primary HIGH-priority Peak Focus Block
        val primaryHabit = candidates.firstOrNull {
            it.categoryId !in setOf("cat_break", "cat_sleep", "cat_commuting")
        } ?: candidates.firstOrNull()

        if (primaryHabit != null) {
            val catName = catMap[primaryHabit.categoryId]?.name ?: "Focus"
            allocateBlock(
                idSuffix = "peak_focus",
                title = primaryHabit.title,
                notes = "Smart-scheduled during your peak completion window (${DateTimeUtils.formatMinuteOfDay(peakFocusHour * 60)}).",
                categoryId = primaryHabit.categoryId,
                preferredStart = peakFocusHour * 60,
                durationMinutes = primaryHabit.calibratedDurationMinutes,
                priority = ActivityPriority.HIGH,
                rationale = "Completed ${primaryHabit.completionCount}x in $catName · Calibrated to ${primaryHabit.calibratedDurationMinutes}m at peak hour ${DateTimeUtils.formatMinuteOfDay(peakFocusHour * 60)}",
                scoreDriver = "+20 Priority & +25 On-Time pts",
                matchCount = primaryHabit.completionCount,
                locationName = primaryHabit.locationName,
                latitude = primaryHabit.latitude,
                longitude = primaryHabit.longitude
            )
        } else {
            allocateBlock(
                idSuffix = "starter_focus",
                title = "Morning Deep Work Sprint",
                notes = "Starter high-priority block to anchor daily score (completion + priority + on-time).",
                categoryId = catMap["cat_deep_work"]?.id ?: fallbackCat.id,
                preferredStart = 9 * 60,
                durationMinutes = 60,
                priority = ActivityPriority.HIGH,
                rationale = "No completed Room history yet · Morning HIGH-priority block unlocks full Priority + On-Time scoring",
                scoreDriver = "+20 Priority & +25 On-Time pts",
                matchCount = 0
            )
        }

        // 2. Recovery Break right after primary block to prevent fatigue/overtime
        val breakPreferred = (suggestions.firstOrNull()?.let { it.startMinute + it.durationMinutes + 5 }) ?: (10 * 60 + 15)
        val breakHabit = candidates.firstOrNull { it.categoryId == "cat_break" }
        allocateBlock(
            idSuffix = "recovery_break",
            title = breakHabit?.title ?: "Recharge & Hydration Break",
            notes = "Short buffer after deep focus keeps remaining blocks on time.",
            categoryId = catMap["cat_break"]?.id ?: fallbackCat.id,
            preferredStart = breakPreferred,
            durationMinutes = breakHabit?.calibratedDurationMinutes?.coerceAtMost(25) ?: 15,
            priority = ActivityPriority.LOW,
            rationale = if (breakHabit != null) {
                "Matches your ${breakHabit.completionCount}x completed break routine to protect on-time pacing"
            } else {
                "15m post-sprint buffer preserves energy and protects afternoon On-Time completion"
            },
            scoreDriver = "Protects +25 On-Time pts",
            matchCount = breakHabit?.completionCount ?: 0
        )

        // 3. Secondary High-Value Task from distinct category in Room
        val secondaryHabit = candidates.firstOrNull { cand ->
            suggestions.none { it.title.equals(cand.title, ignoreCase = true) } &&
                cand.categoryId !in setOf("cat_sleep", "cat_break")
        }
        if (secondaryHabit != null) {
            val catName = catMap[secondaryHabit.categoryId]?.name ?: "Task"
            val afterBreak = suggestions.lastOrNull()?.let { it.startMinute + it.durationMinutes + 10 } ?: (11 * 60)
            val preferredSecond = maxOf(afterBreak, secondaryHabit.avgStartMinute)
            allocateBlock(
                idSuffix = "secondary_habit",
                title = secondaryHabit.title,
                notes = "Scheduled around your historical $catName completion pattern.",
                categoryId = secondaryHabit.categoryId,
                preferredStart = preferredSecond,
                durationMinutes = secondaryHabit.calibratedDurationMinutes,
                priority = ActivityPriority.HIGH,
                rationale = "Proven $catName task (${secondaryHabit.completionCount}x completed) · Avg ${secondaryHabit.calibratedDurationMinutes}m duration",
                scoreDriver = "+Completion & Priority pts",
                matchCount = secondaryHabit.completionCount,
                locationName = secondaryHabit.locationName,
                latitude = secondaryHabit.latitude,
                longitude = secondaryHabit.longitude
            )
        } else {
            val studCat = catMap["cat_studying"] ?: catMap["cat_exercise"] ?: fallbackCat
            val preferredSecond = suggestions.lastOrNull()?.let { it.startMinute + it.durationMinutes + 15 } ?: (13 * 60 + 30)
            allocateBlock(
                idSuffix = "secondary_skill",
                title = if (studCat.id == "cat_exercise") "Active Movement Session" else "Focused Skill & Reading Block",
                notes = "Balanced afternoon priority block for consistent daily XP.",
                categoryId = studCat.id,
                preferredStart = preferredSecond,
                durationMinutes = 45,
                priority = ActivityPriority.MEDIUM,
                rationale = "Balanced afternoon block boosts Completion Ratio across distinct categories",
                scoreDriver = "+Completion Ratio pts",
                matchCount = 0
            )
        }

        // 4. Consistency & Evening Review / Reading anchor
        val thirdHabit = candidates.firstOrNull { cand ->
            suggestions.none { it.title.equals(cand.title, ignoreCase = true) } &&
                cand.categoryId !in setOf("cat_sleep", "cat_break")
        }
        val wrapUpPreferred = maxOf(
            17 * 60,
            suggestions.lastOrNull()?.let { it.startMinute + it.durationMinutes + 30 } ?: (17 * 60)
        )
        if (thirdHabit != null) {
            val catName = catMap[thirdHabit.categoryId]?.name ?: "Focus"
            allocateBlock(
                idSuffix = "consistency_habit",
                title = thirdHabit.title,
                notes = "Locks in your daily streak and wraps up the upcoming schedule.",
                categoryId = thirdHabit.categoryId,
                preferredStart = wrapUpPreferred,
                durationMinutes = thirdHabit.calibratedDurationMinutes.coerceAtMost(60),
                priority = ActivityPriority.MEDIUM,
                rationale = "Completed ${thirdHabit.completionCount}x in $catName · Keeps multi-day streak consistency active",
                scoreDriver = "+15 Consistency Streak pts",
                matchCount = thirdHabit.completionCount,
                locationName = thirdHabit.locationName,
                latitude = thirdHabit.latitude,
                longitude = thirdHabit.longitude
            )
        } else {
            val wrapCat = catMap["cat_reading"] ?: catMap["cat_personal"]?.takeIf { it.id !in excludedCategoryIds } ?: fallbackCat
            allocateBlock(
                idSuffix = "daily_review",
                title = "Daily Wrap-Up & Reading",
                notes = "Quick end-of-day review to close open tasks and preserve streak bonus.",
                categoryId = wrapCat.id,
                preferredStart = wrapUpPreferred,
                durationMinutes = 30,
                priority = ActivityPriority.MEDIUM,
                rationale = "Short evening milestone ensures daily completion check-in for Streak Consistency",
                scoreDriver = "+15 Consistency Streak pts",
                matchCount = 0
            )
        }

        // Calculate baseline vs projected score when suggested blocks are completed on targetDateKey
        val baselineBreakdown = ScheduleEngine.calculatePerformance(
            activities = targetDayExisting,
            streakDays = streakDays
        )
        val simulatedCompletedSuggestions = suggestions.map { block ->
            ScheduledActivityEntity(
                id = block.id,
                userId = "sim",
                dateKey = targetDateKey,
                title = block.title,
                categoryId = block.categoryId,
                startMinute = block.startMinute,
                durationMinutes = block.durationMinutes,
                status = ActivityStatus.COMPLETED,
                priority = block.priority
            )
        }
        val combinedForProjection = targetDayExisting.map {
            if (it.status == ActivityStatus.PLANNED || it.status == ActivityStatus.IN_PROGRESS) {
                it.copy(status = ActivityStatus.COMPLETED)
            } else it
        } + simulatedCompletedSuggestions

        val projectedBreakdown = ScheduleEngine.calculatePerformance(
            activities = combinedForProjection,
            streakDays = maxOf(1, streakDays)
        )

        // Build 3 concise Room-derived analytical insights
        val insights = buildList {
            if (isStarter) {
                add("Room has 0 completed tasks yet — generated a starter high-priority schedule calibrated for Rhythm's 100-pt formula.")
            } else {
                add("Analyzed ${visibleCompleted.size} completed task(s) across $analyzedHistoryDays day(s) stored in Room.")
            }
            add("Peak productivity window: ${DateTimeUtils.formatMinuteOfDay(peakFocusHour * 60)} (${if (isStarter) "recommended morning window" else "$onTimeRatePercent% historical on-time rate"}). ")
            val recentPerfAvg = performances.takeLast(7).map { it.score }.filter { it > 0 }
            if (recentPerfAvg.isNotEmpty()) {
                add("Recent tracked avg score: ${recentPerfAvg.average().roundToInt()}/100 -> Completing this plan projects ${projectedBreakdown.score}/100.")
            } else {
                add("Includes HIGH-priority peak focus + recovery buffer + wrap-up block targeting ${projectedBreakdown.score}/100.")
            }
        }

        return SmartSchedulePlan(
            targetDateKey = targetDateKey,
            analyzedCompletedCount = visibleCompleted.size,
            analyzedHistoryDays = analyzedHistoryDays,
            peakFocusHour = peakFocusHour,
            onTimeRatePercent = onTimeRatePercent,
            baselineProjectedScore = baselineBreakdown.score,
            suggestedProjectedScore = projectedBreakdown.score,
            insights = insights.map { it.trim() },
            suggestions = suggestions.sortedBy { it.startMinute },
            isUsingStarterSignals = isStarter
        )
    }
}
