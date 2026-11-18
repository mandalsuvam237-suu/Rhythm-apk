package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.LevelUpDatabase
import com.example.data.local.SeedData
import com.example.data.model.ActivityStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val type = intent.getStringExtra(NotificationScheduler.EXTRA_TYPE) ?: return pendingResult.finish()
        val activityId = intent.getStringExtra(NotificationScheduler.EXTRA_ACTIVITY_ID) ?: ""
        val dateKey = intent.getStringExtra(NotificationScheduler.EXTRA_DATE_KEY)
        val fallbackTitle = intent.getStringExtra(NotificationScheduler.EXTRA_TITLE) ?: "Activity"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = LevelUpDatabase.getInstance(context).dao()
                when (type) {
                    NotificationScheduler.TYPE_BEFORE,
                    NotificationScheduler.TYPE_START,
                    NotificationScheduler.TYPE_OVERDUE -> {
                        val activity = dao.getScheduledActivityById(activityId)
                        if (activity == null ||
                            activity.status == ActivityStatus.COMPLETED ||
                            activity.status == ActivityStatus.SKIPPED
                        ) {
                            return@launch
                        }
                        val (title, body) = when (type) {
                            NotificationScheduler.TYPE_BEFORE ->
                                "Starting soon: ${activity.title}" to "Your scheduled block begins shortly."
                            NotificationScheduler.TYPE_START ->
                                "Time to start: ${activity.title}" to "Tap to open your timeline or launch Focus Mode."
                            else ->
                                "Overdue check-in: ${activity.title}" to "Did you finish this block? Mark complete or reschedule."
                        }
                        NotificationHelper.postNotification(
                            context = context,
                            notificationId = (activity.id + "_" + type).hashCode(),
                            channelId = NotificationHelper.CHANNEL_SCHEDULE,
                            title = title,
                            body = body,
                            activityId = activity.id,
                            dateKey = activity.dateKey,
                            navTarget = "home"
                        )
                    }

                    NotificationScheduler.TYPE_FOCUS_END -> {
                        NotificationHelper.postNotification(
                            context = context,
                            notificationId = (activityId + "_" + type).hashCode(),
                            channelId = NotificationHelper.CHANNEL_FOCUS,
                            title = "Focus session target reached!",
                            body = "$fallbackTitle reached its planned duration. Stop or keep going in overtime.",
                            activityId = activityId,
                            dateKey = dateKey,
                            navTarget = "timer"
                        )
                    }

                    NotificationScheduler.TYPE_DAILY_REVIEW -> {
                        NotificationHelper.postNotification(
                            context = context,
                            notificationId = "daily_review".hashCode(),
                            channelId = NotificationHelper.CHANNEL_REVIEW,
                            title = "Daily Review Ready",
                            body = "Review today's score, complete remaining blocks, and lock in your streak!",
                            navTarget = "stats"
                        )
                        val pref = dao.getNotificationPreferenceOnce() ?: SeedData.defaultNotificationPref()
                        NotificationScheduler(context).scheduleDailyReview(pref)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
