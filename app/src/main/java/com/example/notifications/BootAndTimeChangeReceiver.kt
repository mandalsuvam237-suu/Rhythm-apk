package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.LevelUpDatabase
import com.example.data.local.SeedData
import com.example.domain.DateTimeUtils
import com.example.widget.LevelUpWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootAndTimeChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = LevelUpDatabase.getInstance(context).dao()
                val scheduler = NotificationScheduler(context)
                val pref = dao.getNotificationPreferenceOnce() ?: SeedData.defaultNotificationPref()
                val today = DateTimeUtils.todayKey()
                val tomorrow = DateTimeUtils.shiftDateKey(today, 1)

                val upcoming = dao.getActivitiesInRangeOnce(today, tomorrow)
                upcoming.forEach { scheduler.scheduleForActivity(it, pref) }

                dao.getActiveTimerOnce()?.let { session ->
                    scheduler.scheduleFocusEnd(session, pref)
                }

                scheduler.scheduleDailyReview(pref)
                LevelUpWidgetProvider.requestUpdate(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
