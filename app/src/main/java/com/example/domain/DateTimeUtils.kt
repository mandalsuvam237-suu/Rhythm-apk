package com.example.domain

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateTimeUtils {
    private val keyFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private val displayDayFormat: SimpleDateFormat
        get() = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

    private val shortDayFormat: SimpleDateFormat
        get() = SimpleDateFormat("EEE", Locale.getDefault())

    private val monthDayFormat: SimpleDateFormat
        get() = SimpleDateFormat("MMM d", Locale.getDefault())

    fun todayKey(timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = Calendar.getInstance(timeZone)
        return formatKey(cal)
    }

    fun formatKey(calendar: Calendar): String {
        val fmt = keyFormat.apply { timeZone = calendar.timeZone }
        return fmt.format(calendar.time)
    }

    fun parseKey(dateKey: String, timeZone: TimeZone = TimeZone.getDefault()): Calendar {
        val cal = Calendar.getInstance(timeZone)
        val fmt = keyFormat.apply { this.timeZone = timeZone }
        val parsed = runCatching { fmt.parse(dateKey) }.getOrNull()
        if (parsed != null) {
            cal.time = parsed
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal
    }

    fun shiftDateKey(dateKey: String, days: Int, timeZone: TimeZone = TimeZone.getDefault()): String {
        val cal = parseKey(dateKey, timeZone)
        cal.add(Calendar.DAY_OF_YEAR, days)
        return formatKey(cal)
    }

    fun formatDisplayDate(dateKey: String): String {
        val cal = parseKey(dateKey)
        return displayDayFormat.format(cal.time)
    }

    fun formatShortDay(dateKey: String): String {
        val cal = parseKey(dateKey)
        return shortDayFormat.format(cal.time)
    }

    fun formatDayNumber(dateKey: String): String {
        val cal = parseKey(dateKey)
        return cal.get(Calendar.DAY_OF_MONTH).toString()
    }

    fun formatMonthDay(dateKey: String): String {
        val cal = parseKey(dateKey)
        return monthDayFormat.format(cal.time)
    }

    fun currentMinuteOfDay(timeZone: TimeZone = TimeZone.getDefault()): Int {
        val cal = Calendar.getInstance(timeZone)
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun formatMinuteOfDay(minute: Int): String {
        val normalized = ((minute % 1440) + 1440) % 1440
        val h = normalized / 60
        val m = normalized % 60
        return String.format(Locale.US, "%02d:%02d", h, m)
    }

    fun formatTimeRange(startMinute: Int, durationMinutes: Int): String {
        val endMinute = startMinute + durationMinutes
        val crossesMidnight = endMinute > 1440
        val startStr = formatMinuteOfDay(startMinute)
        val endStr = formatMinuteOfDay(endMinute)
        return if (crossesMidnight) "$startStr – $endStr (+1d)" else "$startStr – $endStr"
    }

    fun formatDuration(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}m"
            h > 0 -> "${h}h"
            else -> "${m}m"
        }
    }

    fun formatSecondsAsClock(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val h = safe / 3600
        val m = (safe % 3600) / 60
        val s = safe % 60
        return if (h > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.US, "%02d:%02d", m, s)
        }
    }

    /**
     * Converts dateKey + minuteOffset (may exceed 1440 for next-day completion) to epoch millis
     * in the current local timezone.
     */
    fun toEpochMillis(
        dateKey: String,
        minuteOffset: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = parseKey(dateKey, timeZone)
        cal.add(Calendar.MINUTE, minuteOffset)
        return cal.timeInMillis
    }

    /**
     * Weekday index 0=Mon .. 6=Sun for bitmask checks.
     */
    fun weekdayIndex(dateKey: String): Int {
        val cal = parseKey(dateKey)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    fun matchesWeekdayMask(dateKey: String, mask: Int): Boolean {
        if (mask <= 0) return false
        val bit = 1 shl weekdayIndex(dateKey)
        return (mask and bit) != 0
    }

    fun dateRangeKeys(startDateKey: String, endDateKey: String): List<String> {
        val result = mutableListOf<String>()
        var cursor = startDateKey
        var guard = 0
        while (cursor <= endDateKey && guard < 400) {
            result.add(cursor)
            cursor = shiftDateKey(cursor, 1)
            guard++
        }
        return result
    }

    fun weekRangeKeys(anchorDateKey: String): List<String> {
        val idx = weekdayIndex(anchorDateKey)
        val monday = shiftDateKey(anchorDateKey, -idx)
        return (0..6).map { shiftDateKey(monday, it) }
    }

    fun monthRangeKeys(anchorDateKey: String): List<String> {
        val cal = parseKey(anchorDateKey)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return (0 until maxDay).map {
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_MONTH, it)
            formatKey(c)
        }
    }

    fun formatTimestampShort(epochMs: Long): String {
        val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        return fmt.format(Date(epochMs))
    }
}
