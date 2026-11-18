package com.example.domain

import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.ActivityPriority
import com.example.data.model.ActivityStatus
import com.example.data.model.DailyPerformanceEntity
import com.example.data.model.ScheduledActivityEntity
import com.example.data.model.TimerSessionEntity
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class OverlapConflict(
    val conflictingActivity: ScheduledActivityEntity,
    val overlapStartMinute: Int,
    val overlapEndMinute: Int
)

data class PerformanceBreakdown(
    val score: Int,
    val completionPoints: Int,
    val onTimePoints: Int,
    val priorityPoints: Int,
    val consistencyPoints: Int,
    val completionPercent: Int,
    val onTimePercent: Int,
    val totalCount: Int,
    val completedCount: Int
)

data class CategoryUsageStat(
    val category: ActivityCategoryEntity,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val completedCount: Int,
    val totalCount: Int
)

data class TimeBucketStat(
    val label: String,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val score: Int
)

data class StatsSummary(
    val totalPlannedMinutes: Int,
    val totalActualMinutes: Int,
    val completedCount: Int,
    val incompleteCount: Int,
    val onTimePercent: Int,
    val averageScore: Int,
    val categoryStats: List<CategoryUsageStat>,
    val bucketStats: List<TimeBucketStat>
)

data class WeeklyDayCompletionStat(
    val dateKey: String,
    val dayLabel: String,
    val dateLabel: String,
    val totalActivities: Int,
    val completedActivities: Int,
    val completionRatePercent: Int,
    val performanceScore: Int,
    val plannedMinutes: Int,
    val actualMinutes: Int
)

data class WeeklyPerformanceSummary(
    val startDateKey: String,
    val endDateKey: String,
    val days: List<WeeklyDayCompletionStat>,
    val averageCompletionRatePercent: Int,
    val totalCompleted: Int,
    val totalPlanned: Int,
    val bestDay: WeeklyDayCompletionStat?,
    val deltaVsPreviousWeekPercent: Int
)

object ScheduleEngine {

    /**
     * Checks if a candidate activity [candidateDateKey, candidateStart, candidateDuration] overlaps
     * with any existing activity on the same day, previous day (overflowing past midnight),
     * or next day (if candidate crosses midnight).
     */
    fun findOverlaps(
        candidateId: String?,
        candidateDateKey: String,
        candidateStartMinute: Int,
        candidateDurationMinutes: Int,
        sameDayActivities: List<ScheduledActivityEntity>,
        prevDayActivities: List<ScheduledActivityEntity> = emptyList(),
        nextDayActivities: List<ScheduledActivityEntity> = emptyList()
    ): List<OverlapConflict> {
        val candStart = candidateStartMinute
        val candEnd = candidateStartMinute + candidateDurationMinutes
        val conflicts = mutableListOf<OverlapConflict>()

        // 1. Same day comparisons
        for (existing in sameDayActivities) {
            if (existing.id == candidateId || existing.status == ActivityStatus.SKIPPED) continue
            val exStart = existing.startMinute
            val exEnd = existing.startMinute + existing.durationMinutes
            if (candStart < exEnd && exStart < candEnd) {
                conflicts.add(
                    OverlapConflict(
                        conflictingActivity = existing,
                        overlapStartMinute = maxOf(candStart, exStart),
                        overlapEndMinute = minOf(candEnd, exEnd)
                    )
                )
            }
        }

        // 2. Previous day activities crossing midnight into candidateDateKey (offset by -1440)
        for (prev in prevDayActivities) {
            if (prev.id == candidateId || prev.status == ActivityStatus.SKIPPED) continue
            if (prev.endMinute <= 1440) continue
            val overflowStart = 0
            val overflowEnd = prev.endMinute - 1440
            if (candStart < overflowEnd && overflowStart < candEnd) {
                conflicts.add(
                    OverlapConflict(
                        conflictingActivity = prev,
                        overlapStartMinute = maxOf(candStart, overflowStart),
                        overlapEndMinute = minOf(candEnd, overflowEnd)
                    )
                )
            }
        }

        // 3. Candidate crosses midnight into nextDayActivities (candEnd > 1440)
        if (candEnd > 1440) {
            val candOverflowEnd = candEnd - 1440
            for (next in nextDayActivities) {
                if (next.id == candidateId || next.status == ActivityStatus.SKIPPED) continue
                val exStart = next.startMinute
                val exEnd = next.endMinute
                if (0 < exEnd && exStart < candOverflowEnd) {
                    conflicts.add(
                        OverlapConflict(
                            conflictingActivity = next,
                            overlapStartMinute = exStart + 1440,
                            overlapEndMinute = minOf(candEnd, exEnd + 1440)
                        )
                    )
                }
            }
        }

        return conflicts
    }

    /**
     * Finds the earliest conflict-free start minute on candidateDateKey at or after preferredStart.
     */
    fun findNextAvailableSlot(
        preferredStartMinute: Int,
        durationMinutes: Int,
        sameDayActivities: List<ScheduledActivityEntity>,
        prevDayActivities: List<ScheduledActivityEntity>,
        nextDayActivities: List<ScheduledActivityEntity>
    ): Int {
        var candidate = preferredStartMinute.coerceIn(0, 1435)
        var step = 0
        while (step < 96) {
            val overlaps = findOverlaps(
                candidateId = null,
                candidateDateKey = "",
                candidateStartMinute = candidate,
                candidateDurationMinutes = durationMinutes,
                sameDayActivities = sameDayActivities,
                prevDayActivities = prevDayActivities,
                nextDayActivities = nextDayActivities
            )
            if (overlaps.isEmpty()) return candidate
            val maxEnd = overlaps.maxOf { it.overlapEndMinute }
            candidate = ((maxEnd + 4) / 5) * 5
            if (candidate >= 1440) {
                candidate = 0
            }
            step++
        }
        return preferredStartMinute
    }

    /**
     * Checks if an activity is active at [nowMinute] on [todayKey] (including yesterday's midnight-crossing items).
     */
    fun isActivityActiveNow(
        activity: ScheduledActivityEntity,
        todayKey: String,
        prevDayKey: String,
        nowMinute: Int
    ): Boolean {
        if (activity.status == ActivityStatus.COMPLETED || activity.status == ActivityStatus.SKIPPED) {
            return false
        }
        return when (activity.dateKey) {
            todayKey -> nowMinute in activity.startMinute until activity.endMinute
            prevDayKey -> activity.endMinute > 1440 && nowMinute in 0 until (activity.endMinute - 1440)
            else -> false
        }
    }

    /**
     * Transparent 0-100 Productivity Score:
     * - 40 pts: Task Completion ratio
     * - 25 pts: On-Time Completion ratio
     * - 20 pts: Priority Task Completion ratio (defaults to proportional completion if none are HIGH)
     * - 15 pts: Consistency / Streak Bonus (3 pts per streak day up to 15, forgiving on missed days)
     */
    fun calculatePerformance(
        activities: List<ScheduledActivityEntity>,
        streakDays: Int
    ): PerformanceBreakdown {
        val activeItems = activities.filter { it.status != ActivityStatus.SKIPPED }
        if (activeItems.isEmpty()) {
            val consistencyPts = min(15, streakDays * 3)
            return PerformanceBreakdown(
                score = 0,
                completionPoints = 0,
                onTimePoints = 0,
                priorityPoints = 0,
                consistencyPoints = consistencyPts,
                completionPercent = 0,
                onTimePercent = 0,
                totalCount = 0,
                completedCount = 0
            )
        }

        val total = activeItems.size
        val completed = activeItems.filter { it.status == ActivityStatus.COMPLETED }
        val completedCount = completed.size

        val completionRatio = completedCount.toDouble() / total
        val completionPts = (completionRatio * 40.0).roundToInt()

        val onTimeCount = completed.count { act ->
            val completedAt = act.completedAt
            if (completedAt == null) {
                true
            } else {
                val scheduledEndMs = DateTimeUtils.toEpochMillis(act.dateKey, act.endMinute)
                completedAt <= scheduledEndMs + 15 * 60_000L
            }
        }
        val onTimeRatio = if (completedCount == 0) 0.0 else onTimeCount.toDouble() / completedCount
        val onTimePts = (onTimeRatio * 25.0).roundToInt()

        val highPriorityItems = activeItems.filter { it.priority == ActivityPriority.HIGH }
        val priorityRatio = if (highPriorityItems.isEmpty()) {
            completionRatio
        } else {
            highPriorityItems.count { it.status == ActivityStatus.COMPLETED }.toDouble() / highPriorityItems.size
        }
        val priorityPts = (priorityRatio * 20.0).roundToInt()

        val consistencyPts = if (completedCount > 0) {
            min(15, maxOf(3, streakDays * 3))
        } else {
            0
        }

        val totalScore = (completionPts + onTimePts + priorityPts + consistencyPts).coerceIn(0, 100)
        return PerformanceBreakdown(
            score = totalScore,
            completionPoints = completionPts,
            onTimePoints = onTimePts,
            priorityPoints = priorityPts,
            consistencyPoints = consistencyPts,
            completionPercent = (completionRatio * 100.0).roundToInt(),
            onTimePercent = (onTimeRatio * 100.0).roundToInt(),
            totalCount = total,
            completedCount = completedCount
        )
    }

    fun calculateXpForCompletion(activity: ScheduledActivityEntity): Int {
        val base = when (activity.priority) {
            ActivityPriority.HIGH -> 50
            ActivityPriority.MEDIUM -> 35
            ActivityPriority.LOW -> 20
        }
        val durationBonus = (activity.durationMinutes / 15) * 5
        return (base + durationBonus).coerceIn(20, 150)
    }

    fun levelForXp(totalXp: Int): Int {
        if (totalXp <= 0) return 1
        return 1 + floor(sqrt(totalXp / 100.0)).toInt()
    }

    fun xpThresholdForLevel(level: Int): Int {
        val n = (level - 1).coerceAtLeast(0)
        return n * n * 100
    }

    fun computeElapsedSeconds(session: TimerSessionEntity, nowEpochMs: Long): Long {
        val runningDelta = if (session.isRunning && session.lastResumedEpochMs != null) {
            ((nowEpochMs - session.lastResumedEpochMs).coerceAtLeast(0L)) / 1000L
        } else {
            0L
        }
        return (session.accumulatedSeconds + runningDelta).coerceAtLeast(0L)
    }

    fun computeOvertimeSeconds(session: TimerSessionEntity, nowEpochMs: Long): Long {
        val elapsed = computeElapsedSeconds(session, nowEpochMs)
        return (elapsed - session.plannedSeconds).coerceAtLeast(0L)
    }

    fun aggregateStats(
        activities: List<ScheduledActivityEntity>,
        categories: List<ActivityCategoryEntity>,
        performances: List<DailyPerformanceEntity>,
        dateRangeKeys: List<String>
    ): StatsSummary {
        val catMap = categories.associateBy { it.id }
        val nonSkipped = activities.filter { it.status != ActivityStatus.SKIPPED }
        val completed = nonSkipped.filter { it.status == ActivityStatus.COMPLETED }
        val incomplete = nonSkipped.filter { it.status != ActivityStatus.COMPLETED }

        val totalPlannedMinutes = nonSkipped.sumOf { it.durationMinutes }
        val totalActualMinutes = nonSkipped.sumOf { act ->
            val trackedMins = (act.actualSpentSeconds / 60L).toInt()
            when {
                trackedMins > 0 -> trackedMins
                act.status == ActivityStatus.COMPLETED -> act.durationMinutes
                else -> 0
            }
        }

        val onTimeCount = completed.count { act ->
            val completedAt = act.completedAt ?: return@count true
            val scheduledEndMs = DateTimeUtils.toEpochMillis(act.dateKey, act.endMinute)
            completedAt <= scheduledEndMs + 15 * 60_000L
        }
        val onTimePct = if (completed.isEmpty()) 0 else ((onTimeCount.toDouble() / completed.size) * 100.0).roundToInt()

        val perfByDate = performances.associateBy { it.dateKey }
        val scoredDays = dateRangeKeys.mapNotNull { perfByDate[it]?.score }.filter { it > 0 }
        val avgScore = if (scoredDays.isEmpty()) 0 else scoredDays.average().roundToInt()

        val categoryStats = nonSkipped.groupBy { it.categoryId }.mapNotNull { (catId, list) ->
            val category = catMap[catId] ?: return@mapNotNull null
            val planned = list.sumOf { it.durationMinutes }
            val actual = list.sumOf { act ->
                val tracked = (act.actualSpentSeconds / 60L).toInt()
                when {
                    tracked > 0 -> tracked
                    act.status == ActivityStatus.COMPLETED -> act.durationMinutes
                    else -> 0
                }
            }
            CategoryUsageStat(
                category = category,
                plannedMinutes = planned,
                actualMinutes = actual,
                completedCount = list.count { it.status == ActivityStatus.COMPLETED },
                totalCount = list.size
            )
        }.sortedByDescending { maxOf(it.plannedMinutes, it.actualMinutes) }

        val groupedByDate = nonSkipped.groupBy { it.dateKey }
        val bucketKeys = if (dateRangeKeys.size <= 14) {
            dateRangeKeys
        } else {
            dateRangeKeys.takeLast(14)
        }
        val bucketStats = bucketKeys.map { key ->
            val dayList = groupedByDate[key].orEmpty()
            val planned = dayList.sumOf { it.durationMinutes }
            val actual = dayList.sumOf { act ->
                val tracked = (act.actualSpentSeconds / 60L).toInt()
                if (tracked > 0) tracked else if (act.status == ActivityStatus.COMPLETED) act.durationMinutes else 0
            }
            val label = if (dateRangeKeys.size <= 7) {
                DateTimeUtils.formatShortDay(key)
            } else {
                DateTimeUtils.formatDayNumber(key)
            }
            TimeBucketStat(
                label = label,
                plannedMinutes = planned,
                actualMinutes = actual,
                score = perfByDate[key]?.score ?: 0
            )
        }

        return StatsSummary(
            totalPlannedMinutes = totalPlannedMinutes,
            totalActualMinutes = totalActualMinutes,
            completedCount = completed.size,
            incompleteCount = incomplete.size,
            onTimePercent = onTimePct,
            averageScore = avgScore,
            categoryStats = categoryStats,
            bucketStats = bucketStats
        )
    }

    /**
     * Computes day-by-day activity completion rates across the last 7 days ending on [anchorDateKey],
     * along with week-over-week completion rate delta compared to the prior 7-day window.
     */
    fun calculateWeeklyPerformance(
        activities: List<ScheduledActivityEntity>,
        performances: List<DailyPerformanceEntity>,
        anchorDateKey: String
    ): WeeklyPerformanceSummary {
        val currentStart = DateTimeUtils.shiftDateKey(anchorDateKey, -6)
        val currentKeys = DateTimeUtils.dateRangeKeys(currentStart, anchorDateKey)

        val prevStart = DateTimeUtils.shiftDateKey(anchorDateKey, -13)
        val prevEnd = DateTimeUtils.shiftDateKey(anchorDateKey, -7)
        val prevKeys = DateTimeUtils.dateRangeKeys(prevStart, prevEnd).toSet()

        val nonSkipped = activities.filter { it.status != ActivityStatus.SKIPPED }
        val actsByDate = nonSkipped.groupBy { it.dateKey }
        val perfByDate = performances.associateBy { it.dateKey }

        val days = currentKeys.map { key ->
            val dayActs = actsByDate[key].orEmpty()
            val total = dayActs.size
            val completed = dayActs.count { it.status == ActivityStatus.COMPLETED }
            val rate = if (total == 0) {
                0
            } else {
                ((completed.toDouble() / total.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
            }
            val score = perfByDate[key]?.score ?: if (total == 0) 0 else calculatePerformance(dayActs, 0).score
            val plannedMins = dayActs.sumOf { it.durationMinutes }
            val actualMins = dayActs.sumOf { act ->
                val tracked = (act.actualSpentSeconds / 60L).toInt()
                when {
                    tracked > 0 -> tracked
                    act.status == ActivityStatus.COMPLETED -> act.durationMinutes
                    else -> 0
                }
            }
            WeeklyDayCompletionStat(
                dateKey = key,
                dayLabel = DateTimeUtils.formatShortDay(key),
                dateLabel = DateTimeUtils.formatMonthDay(key),
                totalActivities = total,
                completedActivities = completed,
                completionRatePercent = rate,
                performanceScore = score,
                plannedMinutes = plannedMins,
                actualMinutes = actualMins
            )
        }

        val totalCompleted = days.sumOf { it.completedActivities }
        val totalPlanned = days.sumOf { it.totalActivities }
        val avgCompletionRate = if (totalPlanned == 0) {
            0
        } else {
            ((totalCompleted.toDouble() / totalPlanned.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        }

        val prevActs = nonSkipped.filter { it.dateKey in prevKeys }
        val prevAvgRate = if (prevActs.isEmpty()) {
            avgCompletionRate
        } else {
            val prevCompleted = prevActs.count { it.status == ActivityStatus.COMPLETED }
            ((prevCompleted.toDouble() / prevActs.size.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        }
        val delta = if (prevActs.isEmpty()) 0 else (avgCompletionRate - prevAvgRate)

        val bestDay = days
            .filter { it.totalActivities > 0 }
            .maxWithOrNull(
                compareBy<WeeklyDayCompletionStat> { it.completionRatePercent }
                    .thenBy { it.completedActivities }
                    .thenBy { it.performanceScore }
            )

        return WeeklyPerformanceSummary(
            startDateKey = currentStart,
            endDateKey = anchorDateKey,
            days = days,
            averageCompletionRatePercent = avgCompletionRate,
            totalCompleted = totalCompleted,
            totalPlanned = totalPlanned,
            bestDay = bestDay,
            deltaVsPreviousWeekPercent = delta
        )
    }
}
