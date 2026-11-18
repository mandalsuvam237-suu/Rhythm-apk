package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.LevelUpDatabase
import com.example.data.model.ActivityStatus
import com.example.domain.DateTimeUtils
import com.example.domain.ScheduleEngine
import com.example.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LevelUpWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appCtx = context.applicationContext
                val dao = LevelUpDatabase.getInstance(appCtx).dao()
                val todayKey = DateTimeUtils.todayKey()
                val prevKey = DateTimeUtils.shiftDateKey(todayKey, -1)
                val nowMin = DateTimeUtils.currentMinuteOfDay()

                val todayList = dao.getActivitiesForDateOnce(todayKey)
                val prevList = dao.getActivitiesForDateOnce(prevKey)
                val user = dao.getUser()
                val perf = ScheduleEngine.calculatePerformance(todayList, user?.currentStreak ?: 0)

                val active = (prevList + todayList).firstOrNull {
                    ScheduleEngine.isActivityActiveNow(it, todayKey, prevKey, nowMin)
                }
                val upcoming = todayList.filter {
                    it.status != ActivityStatus.COMPLETED &&
                        it.status != ActivityStatus.SKIPPED &&
                        it.id != active?.id &&
                        it.startMinute >= nowMin
                }.take(2)

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(appCtx.packageName, R.layout.widget_level_up)
                    views.setTextViewText(R.id.widget_date, DateTimeUtils.formatDisplayDate(todayKey))
                    views.setTextViewText(
                        R.id.widget_score_badge,
                        "${perf.completedCount}/${perf.totalCount} · ${perf.score} pts"
                    )

                    if (active != null) {
                        views.setTextViewText(R.id.widget_current_label, "NOW · ${DateTimeUtils.formatTimeRange(active.startMinute, active.durationMinutes)}")
                        views.setTextViewText(R.id.widget_current_title, active.title)
                        views.setOnClickPendingIntent(
                            R.id.widget_current_container,
                            buildOpenIntent(appCtx, widgetId * 10 + 1, active.id, active.dateKey)
                        )
                    } else {
                        views.setTextViewText(R.id.widget_current_label, "CURRENTLY FREE")
                        val nextUp = upcoming.firstOrNull()
                        views.setTextViewText(
                            R.id.widget_current_title,
                            nextUp?.let { "Next: ${it.title} at ${DateTimeUtils.formatMinuteOfDay(it.startMinute)}" }
                                ?: "Tap to plan your day"
                        )
                        views.setOnClickPendingIntent(
                            R.id.widget_current_container,
                            buildOpenIntent(appCtx, widgetId * 10 + 1, nextUp?.id, todayKey)
                        )
                    }

                    if (upcoming.isNotEmpty()) {
                        val first = upcoming[0]
                        views.setViewVisibility(R.id.widget_upcoming_1, View.VISIBLE)
                        views.setTextViewText(
                            R.id.widget_upcoming_1,
                            "• ${DateTimeUtils.formatMinuteOfDay(first.startMinute)}  ${first.title}"
                        )
                        views.setOnClickPendingIntent(
                            R.id.widget_upcoming_1,
                            buildOpenIntent(appCtx, widgetId * 10 + 2, first.id, first.dateKey)
                        )
                    } else {
                        views.setViewVisibility(R.id.widget_upcoming_1, View.GONE)
                    }

                    if (upcoming.size > 1) {
                        val second = upcoming[1]
                        views.setViewVisibility(R.id.widget_upcoming_2, View.VISIBLE)
                        views.setTextViewText(
                            R.id.widget_upcoming_2,
                            "• ${DateTimeUtils.formatMinuteOfDay(second.startMinute)}  ${second.title}"
                        )
                        views.setOnClickPendingIntent(
                            R.id.widget_upcoming_2,
                            buildOpenIntent(appCtx, widgetId * 10 + 3, second.id, second.dateKey)
                        )
                    } else {
                        views.setViewVisibility(R.id.widget_upcoming_2, View.GONE)
                    }

                    views.setOnClickPendingIntent(
                        R.id.widget_root,
                        buildOpenIntent(appCtx, widgetId * 10, active?.id ?: upcoming.firstOrNull()?.id, todayKey)
                    )

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildOpenIntent(
        context: Context,
        requestCode: Int,
        activityId: String?,
        dateKey: String
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationHelper.EXTRA_DATE_KEY, dateKey)
            if (activityId != null) {
                putExtra(NotificationHelper.EXTRA_ACTIVITY_ID, activityId)
            }
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun requestUpdate(context: Context) {
            val appCtx = context.applicationContext
            val manager = AppWidgetManager.getInstance(appCtx)
            val component = ComponentName(appCtx, LevelUpWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(appCtx, LevelUpWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                appCtx.sendBroadcast(intent)
            }
        }
    }
}
