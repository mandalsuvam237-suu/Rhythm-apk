package com.example.data.repository

import com.example.data.model.AchievementEntity
import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.DailyPerformanceEntity
import com.example.data.model.HabitEntity
import com.example.data.model.ScheduleTemplateEntity
import com.example.data.model.ScheduledActivityEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.XpTransactionEntity
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

object DataExportHelper {

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun buildExportJson(
        user: UserProfileEntity?,
        categories: List<ActivityCategoryEntity>,
        activities: List<ScheduledActivityEntity>,
        templates: List<ScheduleTemplateEntity>,
        performances: List<DailyPerformanceEntity>,
        habits: List<HabitEntity>,
        achievements: List<AchievementEntity>,
        xpTransactions: List<XpTransactionEntity>
    ): String {
        val root = JSONObject()
        root.put("exportedAtEpochMs", System.currentTimeMillis())
        root.put("app", "Rhythm")

        user?.let { u ->
            root.put("user", JSONObject().apply {
                put("uid", u.uid)
                put("displayName", u.displayName)
                put("email", u.email)
                put("totalXp", u.totalXp)
                put("level", u.level)
                put("currentStreak", u.currentStreak)
                put("longestStreak", u.longestStreak)
                put("totalFocusMinutes", u.totalFocusMinutes)
                put("leaderboardOptIn", u.leaderboardOptIn)
            })
        }

        root.put("categories", JSONArray().apply {
            categories.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("iconKey", c.iconKey)
                    put("colorHex", c.colorHex)
                    put("isDefault", c.isDefault)
                })
            }
        })

        root.put("scheduledActivities", JSONArray().apply {
            activities.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id)
                    put("dateKey", a.dateKey)
                    put("title", a.title)
                    put("categoryId", a.categoryId)
                    put("startMinute", a.startMinute)
                    put("durationMinutes", a.durationMinutes)
                    put("status", a.status.name)
                    put("priority", a.priority.name)
                    put("actualSpentSeconds", a.actualSpentSeconds)
                    if (a.locationName.isNotBlank()) put("locationName", a.locationName)
                    a.latitude?.let { put("latitude", it) }
                    a.longitude?.let { put("longitude", it) }
                })
            }
        })

        root.put("templates", JSONArray().apply {
            templates.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("description", t.description)
                })
            }
        })

        root.put("dailyPerformances", JSONArray().apply {
            performances.forEach { p ->
                put(JSONObject().apply {
                    put("dateKey", p.dateKey)
                    put("score", p.score)
                    put("completedCount", p.completedCount)
                    put("totalPlannedCount", p.totalPlannedCount)
                })
            }
        })

        root.put("habits", JSONArray().apply {
            habits.forEach { h ->
                put(JSONObject().apply {
                    put("id", h.id)
                    put("title", h.title)
                    put("currentStreak", h.currentStreak)
                    put("bestStreak", h.bestStreak)
                })
            }
        })

        root.put("unlockedAchievements", JSONArray().apply {
            achievements.filter { it.unlockedAt != null }.forEach { ach ->
                put(JSONObject().apply {
                    put("code", ach.code)
                    put("title", ach.title)
                    put("unlockedAt", ach.unlockedAt)
                })
            }
        })

        root.put("xpTransactionsCount", xpTransactions.size)
        return root.toString(2)
    }
}
