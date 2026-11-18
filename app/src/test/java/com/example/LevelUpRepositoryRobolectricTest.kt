package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LevelUpDatabase
import com.example.data.local.PreferencesStore
import com.example.data.model.ActivityPriority
import com.example.data.model.ActivityStatus
import com.example.data.repository.DataExportHelper
import com.example.data.repository.LevelUpRepository
import com.example.domain.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LevelUpRepositoryRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: LevelUpDatabase
    private lateinit var repository: LevelUpRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LevelUpDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = LevelUpRepository(
            context = context,
            dao = db.dao(),
            prefsStore = PreferencesStore(context)
        )
        runBlocking {
            repository.ensureInitialized()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            repository.setLockOnStartup(false)
            repository.setLockSensitiveCategories(true)
            repository.setSensitiveCategoryIds(com.example.data.local.SecurityPreferences.DEFAULT_SENSITIVE_CATEGORY_IDS)
            repository.setBackupPasscode("")
        }
        db.close()
    }

    @Test
    fun createEditCompleteDeleteActivity_andUpdateXpAndLeaderboard() = runBlocking {
        val today = DateTimeUtils.todayKey()
        val created = repository.saveActivity(
            existingId = null,
            dateKey = today,
            title = "Morning Deep Work",
            notes = "Ship core module",
            categoryId = "cat_deep_work",
            startMinute = 9 * 60,
            durationMinutes = 60,
            priority = ActivityPriority.HIGH,
            isRepeating = false,
            repeatDaysMask = 0,
            reminderBeforeMinutes = 10
        )

        var list = repository.observeScheduleForDate(today).first()
        assertEquals(1, list.size)
        assertEquals("Morning Deep Work", list[0].activity.title)

        // Edit
        repository.saveActivity(
            existingId = created.id,
            dateKey = today,
            title = "Morning Deep Work v2",
            notes = "Updated",
            categoryId = "cat_deep_work",
            startMinute = 9 * 60,
            durationMinutes = 90,
            priority = ActivityPriority.HIGH,
            isRepeating = false,
            repeatDaysMask = 0,
            reminderBeforeMinutes = 10
        )
        list = repository.observeScheduleForDate(today).first()
        assertEquals("Morning Deep Work v2", list[0].activity.title)
        assertEquals(90, list[0].activity.durationMinutes)

        // Complete -> earns XP & unlocks FIRST_STEP achievement
        repository.setActivityStatus(created.id, ActivityStatus.COMPLETED)
        val user = repository.userProfile.first()
        assertNotNull(user)
        assertTrue((user?.totalXp ?: 0) > 0)

        // Delete
        repository.deleteActivity(created.id)
        list = repository.observeScheduleForDate(today).first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun applyTemplate_isNonDestructiveWhenConflictsExist() = runBlocking {
        val today = DateTimeUtils.todayKey()
        repository.saveActivity(
            existingId = null,
            dateKey = today,
            title = "Existing Morning Call",
            notes = "",
            categoryId = "cat_personal",
            startMinute = 9 * 60,
            durationMinutes = 60,
            priority = ActivityPriority.MEDIUM,
            isRepeating = false,
            repeatDaysMask = 0,
            reminderBeforeMinutes = 5
        )

        val result = repository.applyTemplateToDate("tpl_focus", today)
        assertTrue(result.addedCount > 0)
        val after = repository.observeScheduleForDate(today).first()
        assertTrue(after.any { it.activity.title == "Existing Morning Call" })
    }

    @Test
    fun timerPauseResumeStop_persistsTimeAndLogsToActivity() = runBlocking {
        val today = DateTimeUtils.todayKey()
        val act = repository.saveActivity(
            existingId = null,
            dateKey = today,
            title = "Algorithm Study",
            notes = "",
            categoryId = "cat_studying",
            startMinute = 14 * 60,
            durationMinutes = 45,
            priority = ActivityPriority.MEDIUM,
            isRepeating = false,
            repeatDaysMask = 0,
            reminderBeforeMinutes = 10
        )

        val session = repository.startTimer(act.id, act.title, act.categoryId, 45, today)
        assertTrue(session.isRunning)

        repository.pauseTimer(session.id)
        val paused = db.dao().getTimerById(session.id)
        assertNotNull(paused)
        assertFalse(paused!!.isRunning)

        repository.resumeTimer(session.id)
        val resumed = db.dao().getTimerById(session.id)
        assertTrue(resumed!!.isRunning)

        repository.stopTimer(session.id, markActivityCompleted = true)
        val completedAct = db.dao().getScheduledActivityById(act.id)
        assertEquals(ActivityStatus.COMPLETED, completedAct?.status)
    }

    @Test
    fun demoModeAndExportAndAccountReset_workCleanly() = runBlocking {
        repository.setDemoMode(true)
        var user = repository.userProfile.first()
        assertTrue(user?.isDemoMode == true)
        assertTrue((user?.totalXp ?: 0) > 0)

        val json = repository.exportAllDataJson()
        assertTrue(json.contains("Rhythm"))
        assertTrue(json.contains("scheduledActivities"))

        repository.deleteAccountAndAllData()
        user = repository.userProfile.first()
        assertFalse(user?.isDemoMode == true)
        assertEquals(0, user?.totalXp)
    }

    @Test
    fun saveActivityWithLocation_persistsAndExportsLocationFields() = runBlocking {
        val today = DateTimeUtils.todayKey()
        val created = repository.saveActivity(
            existingId = null,
            dateKey = today,
            title = "Library Study Block",
            notes = "Bring headphones",
            categoryId = "cat_studying",
            startMinute = 10 * 60,
            durationMinutes = 60,
            priority = ActivityPriority.HIGH,
            isRepeating = false,
            repeatDaysMask = 0,
            reminderBeforeMinutes = 10,
            locationName = "Central Public Library",
            latitude = 37.7793,
            longitude = -122.4159
        )

        val fetched = db.dao().getScheduledActivityById(created.id)
        assertNotNull(fetched)
        assertTrue(fetched!!.hasLocation)
        assertEquals("Central Public Library", fetched.locationName)
        assertEquals(37.7793, fetched.latitude ?: 0.0, 0.0001)

        val json = repository.exportAllDataJson()
        assertTrue(json.contains("Central Public Library"))
    }

    @Test
    fun securityPreferences_persistLockFlagsSensitiveCategoriesAndPasscodeHash() = runBlocking {
        repository.setLockOnStartup(true)
        repository.setLockSensitiveCategories(true)
        repository.setSensitiveCategoryIds(setOf("cat_personal", "cat_deep_work"))
        repository.setBackupPasscode("2468")

        val sec = repository.securityPreferences.first()
        assertTrue(sec.lockOnStartup)
        assertTrue(sec.lockSensitiveCategories)
        assertEquals(setOf("cat_personal", "cat_deep_work"), sec.sensitiveCategoryIds)
        assertTrue(sec.hasBackupPasscode)
        assertEquals(DataExportHelper.sha256("2468"), sec.backupPasscodeHash)
    }

    @Test
    fun smartSuggestions_readsRoomCompletedTasks_andAppliesNonDestructively() = runBlocking {
        val today = DateTimeUtils.todayKey()
        val tomorrow = DateTimeUtils.shiftDateKey(today, 1)

        val act = repository.saveActivity(
            existingId = null,
            dateKey = today,
            title = "Kotlin Architecture Review",
            notes = "Room test",
            categoryId = "cat_deep_work",
            startMinute = 9 * 60,
            durationMinutes = 60,
            priority = ActivityPriority.HIGH,
            isRepeating = false,
            repeatDaysMask = 0,
            reminderBeforeMinutes = 10
        )
        repository.setActivityStatus(act.id, ActivityStatus.COMPLETED)

        val plan = repository.generateSmartSchedulePlan(tomorrow)
        assertTrue(plan.analyzedCompletedCount >= 1)
        assertTrue(plan.suggestions.isNotEmpty())
        assertTrue(plan.suggestedProjectedScore >= plan.baselineProjectedScore)

        val added = repository.applySmartSuggestions(tomorrow, plan.suggestions)
        assertEquals(plan.suggestions.size, added)

        val tomorrowSchedule = repository.observeScheduleForDate(tomorrow).first()
        assertEquals(added, tomorrowSchedule.size)
    }
}
