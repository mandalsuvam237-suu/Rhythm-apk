package com.example

import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.ActivityPriority
import com.example.data.model.ActivityStatus
import com.example.data.model.DailyPerformanceEntity
import com.example.data.model.ScheduledActivityEntity
import com.example.data.model.TimerSessionEntity
import com.example.domain.DateTimeUtils
import com.example.domain.ScheduleEngine
import com.example.domain.SmartSuggestionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone

class ScheduleEngineTest {

    private fun sampleActivity(
        id: String,
        dateKey: String = "2026-11-16",
        startMinute: Int,
        durationMinutes: Int,
        status: ActivityStatus = ActivityStatus.PLANNED,
        priority: ActivityPriority = ActivityPriority.MEDIUM,
        categoryId: String = "cat_deep_work",
        actualSpentSeconds: Long = 0L
    ) = ScheduledActivityEntity(
        id = id,
        userId = "u1",
        dateKey = dateKey,
        title = "Block $id",
        categoryId = categoryId,
        startMinute = startMinute,
        durationMinutes = durationMinutes,
        status = status,
        priority = priority,
        actualSpentSeconds = actualSpentSeconds
    )

    @Test
    fun overlapDetection_catchesSameDayAndMidnightOverflow() {
        val existingToday = listOf(sampleActivity("a1", "2026-11-16", startMinute = 9 * 60, durationMinutes = 60))
        val sleepYesterday = listOf(sampleActivity("sleep", "2026-11-15", startMinute = 23 * 60, durationMinutes = 480)) // 23:00 -> 07:00 (+1d)

        // Same day overlap 09:30..10:30 overlaps 09:00..10:00
        val sameDayConflicts = ScheduleEngine.findOverlaps(
            candidateId = null,
            candidateDateKey = "2026-11-16",
            candidateStartMinute = 9 * 60 + 30,
            candidateDurationMinutes = 60,
            sameDayActivities = existingToday,
            prevDayActivities = sleepYesterday
        )
        assertEquals(1, sameDayConflicts.size)
        assertEquals("a1", sameDayConflicts[0].conflictingActivity.id)

        // Early morning 06:30..07:15 overlaps yesterday's 23:00..07:00 sleep
        val midnightOverflowConflicts = ScheduleEngine.findOverlaps(
            candidateId = null,
            candidateDateKey = "2026-11-16",
            candidateStartMinute = 6 * 60 + 30,
            candidateDurationMinutes = 45,
            sameDayActivities = existingToday,
            prevDayActivities = sleepYesterday
        )
        assertEquals(1, midnightOverflowConflicts.size)
        assertEquals("sleep", midnightOverflowConflicts[0].conflictingActivity.id)

        // Free slot at 07:00..08:00 has zero conflicts
        val freeConflicts = ScheduleEngine.findOverlaps(
            candidateId = null,
            candidateDateKey = "2026-11-16",
            candidateStartMinute = 7 * 60,
            candidateDurationMinutes = 60,
            sameDayActivities = existingToday,
            prevDayActivities = sleepYesterday
        )
        assertTrue(freeConflicts.isEmpty())
    }

    @Test
    fun activityActiveNow_respectsMidnightCarryOver() {
        val sleepYesterday = sampleActivity("sleep", "2026-11-15", startMinute = 23 * 60, durationMinutes = 480)
        assertTrue(ScheduleEngine.isActivityActiveNow(sleepYesterday, "2026-11-16", "2026-11-15", 2 * 60))
        assertFalse(ScheduleEngine.isActivityActiveNow(sleepYesterday, "2026-11-16", "2026-11-15", 8 * 60))
    }

    @Test
    fun timestampTimer_survivesAppClosureAndRecordsOvertime() {
        val t0 = 1_700_000_000_000L
        val session = TimerSessionEntity(
            id = "s1",
            activityId = null,
            dateKey = "2026-11-16",
            title = "Deep Work",
            categoryId = "cat_deep_work",
            plannedSeconds = 1500L, // 25m
            accumulatedSeconds = 300L, // 5m before closing app
            lastResumedEpochMs = t0,
            isRunning = true
        )
        // Reopened 25 minutes later -> total 30m (1800s), overtime = 5m (300s)
        val reopenedAt = t0 + 25 * 60 * 1000L
        assertEquals(1800L, ScheduleEngine.computeElapsedSeconds(session, reopenedAt))
        assertEquals(300L, ScheduleEngine.computeOvertimeSeconds(session, reopenedAt))
    }

    @Test
    fun performanceAndStats_calculateAccuratelyWithoutFakeNumbers() {
        val cats = listOf(
            ActivityCategoryEntity("cat_deep_work", "Deep Work", "bolt", "#FF6B4A", true, 0),
            ActivityCategoryEntity("cat_reading", "Reading", "auto_stories", "#8B5CF6", true, 1)
        )
        val acts = listOf(
            sampleActivity("1", "2026-11-16", 9 * 60, 60, ActivityStatus.COMPLETED, ActivityPriority.HIGH, "cat_deep_work", 3600L),
            sampleActivity("2", "2026-11-16", 11 * 60, 30, ActivityStatus.PLANNED, ActivityPriority.LOW, "cat_reading", 0L)
        )
        val breakdown = ScheduleEngine.calculatePerformance(acts, streakDays = 2)
        assertEquals(50, breakdown.completionPercent)
        assertTrue(breakdown.score in 50..80)

        val stats = ScheduleEngine.aggregateStats(
            activities = acts,
            categories = cats,
            performances = listOf(DailyPerformanceEntity("2026-11-16", 2, 1, 1, 1, 1, 90, 60, breakdown.score)),
            dateRangeKeys = listOf("2026-11-16")
        )
        assertEquals(90, stats.totalPlannedMinutes)
        assertEquals(60, stats.totalActualMinutes)
        assertEquals(1, stats.completedCount)
        assertEquals(1, stats.incompleteCount)
    }

    @Test
    fun timezoneShift_adjustsEpochMillisAppropriately() {
        val laMs = DateTimeUtils.toEpochMillis("2026-11-16", 9 * 60, TimeZone.getTimeZone("America/Los_Angeles"))
        val tokyoMs = DateTimeUtils.toEpochMillis("2026-11-16", 9 * 60, TimeZone.getTimeZone("Asia/Tokyo"))
        // Tokyo 09:00 occurs 17 hours earlier than LA 09:00 in November (UTC+9 vs UTC-8)
        assertEquals(17 * 3600_000L, laMs - tokyoMs)
    }

    @Test
    fun weeklyPerformance_calculatesLast7DaysCompletionRatesAndDelta() {
        val acts = listOf(
            // Current 7 days (2026-11-10 .. 2026-11-16)
            sampleActivity("c1", "2026-11-10", 9 * 60, 60, ActivityStatus.COMPLETED),
            sampleActivity("c2", "2026-11-10", 11 * 60, 30, ActivityStatus.PLANNED), // 1/2 = 50%
            sampleActivity("c3", "2026-11-14", 9 * 60, 60, ActivityStatus.COMPLETED),
            sampleActivity("c4", "2026-11-14", 10 * 60, 30, ActivityStatus.COMPLETED), // 2/2 = 100%
            sampleActivity("c5", "2026-11-16", 9 * 60, 45, ActivityStatus.COMPLETED),
            sampleActivity("c6", "2026-11-16", 12 * 60, 30, ActivityStatus.SKIPPED), // skipped ignored -> 1/1 = 100%
            // Prior 7 days (2026-11-03 .. 2026-11-09): 1 completed out of 2 = 50%
            sampleActivity("p1", "2026-11-08", 9 * 60, 60, ActivityStatus.COMPLETED),
            sampleActivity("p2", "2026-11-09", 9 * 60, 60, ActivityStatus.PLANNED)
        )

        val summary = ScheduleEngine.calculateWeeklyPerformance(
            activities = acts,
            performances = emptyList(),
            anchorDateKey = "2026-11-16"
        )

        assertEquals(7, summary.days.size)
        assertEquals("2026-11-10", summary.startDateKey)
        assertEquals("2026-11-16", summary.endDateKey)
        assertEquals(50, summary.days.first { it.dateKey == "2026-11-10" }.completionRatePercent)
        assertEquals(100, summary.days.first { it.dateKey == "2026-11-14" }.completionRatePercent)
        assertEquals(100, summary.days.first { it.dateKey == "2026-11-16" }.completionRatePercent)
        assertEquals(0, summary.days.first { it.dateKey == "2026-11-12" }.completionRatePercent)
        // Current week: 4 completed / 5 non-skipped = 80%; prior week: 1/2 = 50% -> +30%
        assertEquals(4, summary.totalCompleted)
        assertEquals(5, summary.totalPlanned)
        assertEquals(80, summary.averageCompletionRatePercent)
        assertEquals(30, summary.deltaVsPreviousWeekPercent)
        assertEquals("2026-11-14", summary.bestDay?.dateKey)
    }

    @Test
    fun smartSuggestionEngine_analyzesCompletedTasks_avoidsOverlaps_andBoostsProjectedScore() {
        val cats = listOf(
            ActivityCategoryEntity("cat_deep_work", "Deep Work", "bolt", "#FF6B4A", true, 0),
            ActivityCategoryEntity("cat_break", "Break", "coffee", "#22C55E", true, 1),
            ActivityCategoryEntity("cat_reading", "Reading", "auto_stories", "#8B5CF6", true, 2),
            ActivityCategoryEntity("cat_personal", "Personal Work", "person", "#EC4899", true, 3)
        )
        val completedHistory = listOf(
            sampleActivity("h1", "2026-11-14", 9 * 60, 45, ActivityStatus.COMPLETED, ActivityPriority.HIGH, "cat_deep_work", 3600L).copy(title = "Architecture Sprint"),
            sampleActivity("h2", "2026-11-15", 9 * 60, 45, ActivityStatus.COMPLETED, ActivityPriority.HIGH, "cat_deep_work", 3600L).copy(title = "Architecture Sprint"),
            sampleActivity("h3", "2026-11-15", 17 * 60, 30, ActivityStatus.COMPLETED, ActivityPriority.MEDIUM, "cat_reading", 1800L).copy(title = "Evening Tech Reading"),
            sampleActivity("h_priv", "2026-11-15", 20 * 60, 30, ActivityStatus.COMPLETED, ActivityPriority.HIGH, "cat_personal", 1800L).copy(title = "Private Journal")
        )
        // Existing conflict on target day 2026-11-17 at 09:00..10:00
        val existingTomorrow = listOf(
            sampleActivity("ex1", "2026-11-17", 9 * 60, 60, ActivityStatus.PLANNED, ActivityPriority.MEDIUM, "cat_deep_work")
        )

        val plan = SmartSuggestionEngine.generatePlan(
            targetDateKey = "2026-11-17",
            allActivities = completedHistory,
            categories = cats,
            performances = emptyList(),
            targetDayExisting = existingTomorrow,
            prevDayActivities = emptyList(),
            nextDayActivities = emptyList(),
            streakDays = 3,
            excludedCategoryIds = setOf("cat_personal")
        )

        assertFalse(plan.isUsingStarterSignals)
        assertEquals(3, plan.analyzedCompletedCount)
        assertTrue(plan.suggestedProjectedScore > plan.baselineProjectedScore)
        assertTrue(plan.suggestions.any { it.title == "Architecture Sprint" && it.durationMinutes == 60 })
        assertTrue(plan.suggestions.none { it.title == "Private Journal" })
        // Must not overlap existing 09:00..10:00 block
        plan.suggestions.forEach { block ->
            val overlaps = ScheduleEngine.findOverlaps(
                candidateId = null,
                candidateDateKey = "2026-11-17",
                candidateStartMinute = block.startMinute,
                candidateDurationMinutes = block.durationMinutes,
                sameDayActivities = existingTomorrow
            )
            assertTrue("Suggested block ${block.title} overlapped existing block", overlaps.isEmpty())
        }
    }
}
