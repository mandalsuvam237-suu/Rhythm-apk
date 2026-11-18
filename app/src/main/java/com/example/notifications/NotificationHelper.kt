package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object NotificationHelper {
    const val CHANNEL_SCHEDULE = "levelup_schedule_alerts"
    const val CHANNEL_FOCUS = "levelup_focus_timer"
    const val CHANNEL_REVIEW = "levelup_daily_review"

    const val EXTRA_ACTIVITY_ID = "extra_activity_id"
    const val EXTRA_DATE_KEY = "extra_date_key"
    const val EXTRA_NAV_TARGET = "extra_nav_target"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val schedule = NotificationChannel(
            CHANNEL_SCHEDULE,
            context.getString(R.string.channel_schedule_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_schedule_desc)
        }

        val focus = NotificationChannel(
            CHANNEL_FOCUS,
            context.getString(R.string.channel_focus_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_focus_desc)
        }

        val review = NotificationChannel(
            CHANNEL_REVIEW,
            context.getString(R.string.channel_review_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.channel_review_desc)
        }

        manager.createNotificationChannels(listOf(schedule, focus, review))
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun postNotification(
        context: Context,
        notificationId: Int,
        channelId: String,
        title: String,
        body: String,
        activityId: String? = null,
        dateKey: String? = null,
        navTarget: String? = null
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            activityId?.let { putExtra(EXTRA_ACTIVITY_ID, it) }
            dateKey?.let { putExtra(EXTRA_DATE_KEY, it) }
            navTarget?.let { putExtra(EXTRA_NAV_TARGET, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        }
    }
}
