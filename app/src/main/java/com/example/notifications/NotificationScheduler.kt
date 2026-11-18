package com.example.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.ActivityStatus
import com.example.data.model.NotificationPreferenceEntity
import com.example.data.model.ScheduledActivityEntity
import com.example.data.model.TimerSessionEntity
import com.example.domain.DateTimeUtils
import java.util.Calendar

class NotificationScheduler(private val context: Context) {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    companion object {
        const val TYPE_BEFORE = "before"
        const val TYPE_START = "start"
        const val TYPE_OVERDUE = "overdue"
        const val TYPE_FOCUS_END = "focus_end"
        const val TYPE_DAILY_REVIEW = "daily_review"

        const val EXTRA_TYPE = "reminder_type"
        const val EXTRA_ACTIVITY_ID = "activity_id"
        const val EXTRA_DATE_KEY = "date_key"
        const val EXTRA_TITLE = "title"
    }

    fun scheduleForActivity(
        activity: ScheduledActivityEntity,
        pref: NotificationPreferenceEntity
    ) {
        cancelForActivity(activity.id)
        if (activity.status != ActivityStatus.PLANNED && activity.status != ActivityStatus.IN_PROGRESS) {
            return
        }
        val now = System.currentTimeMillis()

        val startMs = DateTimeUtils.toEpochMillis(activity.dateKey, activity.startMinute)
        val endMs = DateTimeUtils.toEpochMillis(activity.dateKey, activity.endMinute)

        val beforeMins = if (activity.reminderBeforeMinutes > 0) activity.reminderBeforeMinutes else pref.minutesBefore
        if (pref.notifyBeforeEnabled && beforeMins > 0) {
            val beforeMs = startMs - beforeMins * 60_000L
            if (beforeMs > now) {
                scheduleAlarm(
                    requestCode = requestCodeFor(activity.id, TYPE_BEFORE),
                    triggerAtMs = beforeMs,
                    type = TYPE_BEFORE,
                    activityId = activity.id,
                    dateKey = activity.dateKey,
                    title = activity.title
                )
            }
        }

        if (pref.notifyOnStart && startMs > now) {
            scheduleAlarm(
                requestCode = requestCodeFor(activity.id, TYPE_START),
                triggerAtMs = startMs,
                type = TYPE_START,
                activityId = activity.id,
                dateKey = activity.dateKey,
                title = activity.title
            )
        }

        if (pref.notifyOnOverdue) {
            val overdueMs = endMs + 5 * 60_000L
            if (overdueMs > now) {
                scheduleAlarm(
                    requestCode = requestCodeFor(activity.id, TYPE_OVERDUE),
                    triggerAtMs = overdueMs,
                    type = TYPE_OVERDUE,
                    activityId = activity.id,
                    dateKey = activity.dateKey,
                    title = activity.title
                )
            }
        }
    }

    fun cancelForActivity(activityId: String) {
        listOf(TYPE_BEFORE, TYPE_START, TYPE_OVERDUE).forEach { type ->
            cancelAlarm(requestCodeFor(activityId, type), type, activityId)
        }
    }

    fun scheduleFocusEnd(session: TimerSessionEntity, pref: NotificationPreferenceEntity) {
        cancelFocusEnd(session.id)
        if (!pref.notifyOnFocusEnd || !session.isRunning || session.isFinished) return
        val resumedAt = session.lastResumedEpochMs ?: return
        val remainingSeconds = (session.plannedSeconds - session.accumulatedSeconds).coerceAtLeast(0L)
        if (remainingSeconds <= 0L) return
        val triggerMs = resumedAt + remainingSeconds * 1000L
        if (triggerMs > System.currentTimeMillis()) {
            scheduleAlarm(
                requestCode = requestCodeFor(session.id, TYPE_FOCUS_END),
                triggerAtMs = triggerMs,
                type = TYPE_FOCUS_END,
                activityId = session.activityId ?: session.id,
                dateKey = session.dateKey,
                title = session.title
            )
        }
    }

    fun cancelFocusEnd(sessionId: String) {
        cancelAlarm(requestCodeFor(sessionId, TYPE_FOCUS_END), TYPE_FOCUS_END, sessionId)
    }

    fun scheduleDailyReview(pref: NotificationPreferenceEntity) {
        val code = requestCodeFor("daily_review", TYPE_DAILY_REVIEW)
        cancelAlarm(code, TYPE_DAILY_REVIEW, "daily_review")
        if (!pref.dailyReviewEnabled) return

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, pref.dailyReviewMinute / 60)
            set(Calendar.MINUTE, pref.dailyReviewMinute % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        scheduleAlarm(
            requestCode = code,
            triggerAtMs = cal.timeInMillis,
            type = TYPE_DAILY_REVIEW,
            activityId = "daily_review",
            dateKey = DateTimeUtils.todayKey(),
            title = "Daily Productivity Review"
        )
    }

    private fun scheduleAlarm(
        requestCode: Int,
        triggerAtMs: Long,
        type: String,
        activityId: String,
        dateKey: String,
        title: String
    ) {
        val am = alarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_ACTIVITY_ID, activityId)
            putExtra(EXTRA_DATE_KEY, dateKey)
            putExtra(EXTRA_TITLE, title)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            am.canScheduleExactAlarms()
        } else {
            true
        }
        if (canExact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pi)
        }
    }

    private fun cancelAlarm(requestCode: Int, type: String, activityId: String) {
        val am = alarmManager ?: return
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_ACTIVITY_ID, activityId)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun requestCodeFor(id: String, type: String): Int = (id + "_" + type).hashCode()
}
