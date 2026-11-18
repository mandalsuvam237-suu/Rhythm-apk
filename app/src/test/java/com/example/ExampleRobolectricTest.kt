package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.PreferencesStore
import com.example.data.local.SecurityPreferences
import com.example.domain.DateTimeUtils
import com.example.security.BiometricAuthHelper
import com.example.security.BiometricCapability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetSecurityBefore() {
        BiometricAuthHelper.testCapabilityOverride = BiometricCapability.NONE_ENROLLED
        resetSecurityPrefs()
    }

    @After
    fun resetSecurityAfter() {
        BiometricAuthHelper.testCapabilityOverride = null
        resetSecurityPrefs()
    }

    private fun resetSecurityPrefs() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = PreferencesStore(context)
        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(3_000L) {
                store.setLockOnStartup(false)
                store.setLockSensitiveCategories(true)
                store.setSensitiveCategoryIds(SecurityPreferences.DEFAULT_SENSITIVE_CATEGORY_IDS)
                store.setBackupPasscodeHash("")
            }
        }
    }

    @Test
    fun readStringFromContext_returnsRhythm() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Rhythm", appName)
    }

    @Test
    fun mainActivity_displaysNavigationAndQuickAddFab() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("daily_timeline_lazy_column").assertIsDisplayed()
        composeRule.onNodeWithTag("quick_add_activity_fab").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_today").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_focus").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_templates").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_stats").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_settings").assertIsDisplayed()
    }

    @Test
    fun dateNavigation_shiftsToNextDayAndJumpsBackToToday() {
        composeRule.waitForIdle()
        val today = DateTimeUtils.todayKey()
        val tomorrow = DateTimeUtils.shiftDateKey(today, 1)

        composeRule.onNodeWithTag("selected_date_header", useUnmergedTree = true)
            .assertTextEquals(DateTimeUtils.formatDisplayDate(today))

        composeRule.onNodeWithTag("next_day_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("selected_date_header", useUnmergedTree = true)
            .assertTextEquals(DateTimeUtils.formatDisplayDate(tomorrow))
        composeRule.onNodeWithTag("jump_to_today_btn").assertIsDisplayed()

        composeRule.onNodeWithTag("jump_to_today_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("selected_date_header", useUnmergedTree = true)
            .assertTextEquals(DateTimeUtils.formatDisplayDate(today))
    }

    @Test
    fun quickAddActivity_rendersTimelineCardInLazyColumn() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("quick_add_activity_fab").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("activity_title_input").performTextInput("Design Review")
        composeRule.onNodeWithTag("save_activity_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Design Review", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithTag("daily_timeline_lazy_column")
            .performScrollToNode(hasTestTag("timeline_filter_ALL"))
        composeRule.onNodeWithTag("timeline_filter_ALL").assertIsDisplayed()

        composeRule.onNodeWithTag("daily_timeline_lazy_column")
            .performScrollToNode(hasText("Design Review"))
        composeRule.onAllNodesWithText("Design Review", useUnmergedTree = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun aiCoachScreen_displaysFirebaseNoticeAndRespondsInMultiTurnChat() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("daily_timeline_lazy_column")
            .performScrollToNode(hasTestTag("coach_entry_btn"))
        composeRule.onNodeWithTag("coach_entry_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("ai_firebase_notice_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("search_grounding_switch").assertIsDisplayed()

        composeRule.onNodeWithTag("coach_prompt_input").performTextInput("Plan a study block")
        composeRule.onNodeWithTag("coach_send_btn").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Sample Response", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("coach_chat_lazy_column")
            .performScrollToNode(hasText("Sample Response", substring = true))
        composeRule.onAllNodesWithText("Sample Response", substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun placesMapScreen_displaysNoticeAndFocusPins() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("daily_timeline_lazy_column")
            .performScrollToNode(hasTestTag("places_map_entry_btn"))
        composeRule.onNodeWithTag("places_map_entry_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("maps_api_key_notice").assertIsDisplayed()
        composeRule.onNodeWithTag("places_map_container").assertIsDisplayed()
        composeRule.onNodeWithTag("places_lazy_column")
            .performScrollToNode(hasTestTag("schedule_here_btn"))
        composeRule.onNodeWithTag("schedule_here_btn").assertIsDisplayed()
    }

    @Test
    fun sensitiveCategoryNote_isMaskedUntilUnlocked_andStartupLockWorksWithBackupPasscode() {
        composeRule.waitForIdle()

        // Move 3 days ahead for a clean timeline free of overlap with prior test entries
        repeat(3) {
            composeRule.onNodeWithTag("next_day_btn").performClick()
            composeRule.waitForIdle()
        }

        // 1. Add a sensitive activity under "Personal Work" (cat_personal, protected by default)
        composeRule.onNodeWithTag("quick_add_activity_fab").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("activity_title_input").performTextInput("Private Budget Notes")
        composeRule.onNodeWithTag("editor_cat_cat_personal")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithTag("save_activity_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // Verify sensitive note title is masked behind biometric lock on timeline
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Protected Personal Work Note", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("daily_timeline_lazy_column")
            .performScrollToNode(hasText("Protected Personal Work Note", substring = true))
        composeRule.onAllNodesWithText("Protected Personal Work Note", substring = true)
            .onFirst()
            .assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithText("Private Budget Notes", substring = true)
                .fetchSemanticsNodes()
                .size
        )

        // 2. Navigate to Profile -> Security & Biometrics, set backup passcode & enable startup lock
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_lazy_column")
            .performScrollToNode(hasTestTag("toggle_protected_categories_btn"))

        composeRule.onNodeWithTag("set_backup_passcode_btn").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithTag("new_passcode_input").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("new_passcode_input").performTextInput("2580")
        composeRule.onNodeWithTag("confirm_passcode_input").performTextInput("2580")
        composeRule.onNodeWithTag("save_passcode_btn").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(
                hasTestTag("set_backup_passcode_btn") and hasText("Update Passcode", substring = true)
            ).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("lock_on_startup_switch").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(
                hasTestTag("lock_on_startup_switch") and isOn()
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // 3. Trigger immediate startup lock and verify StartupBiometricLockOverlay + backup passcode unlock
        composeRule.onNodeWithTag("lock_now_btn").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithTag("startup_lock_overlay").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("startup_lock_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("unlock_backup_passcode_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("backup_passcode_input").performTextInput("2580")
        composeRule.onNodeWithTag("verify_passcode_confirm_btn").performClick()
        composeRule.waitForIdle()

        assertEquals(
            0,
            composeRule.onAllNodesWithTag("startup_lock_overlay").fetchSemanticsNodes().size
        )

        // 4. Unlock sensitive Personal Work card on timeline via backup passcode
        composeRule.onNodeWithTag("nav_today").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("daily_timeline_lazy_column")
            .performScrollToNode(hasTestTag("smart_suggestions_entry_btn"))
        composeRule.onNodeWithTag("upcoming_unlock_btn")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithTag("backup_passcode_input").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("backup_passcode_input").performTextInput("2580")
        composeRule.onNodeWithTag("verify_passcode_confirm_btn").performClick()
        composeRule.waitForIdle()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Private Budget Notes", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onAllNodesWithText("Private Budget Notes", substring = true)
            .onFirst()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("daily_timeline_lazy_column")
            .performScrollToNode(hasTestTag("relock_sensitive_btn"))
        composeRule.onNodeWithTag("relock_sensitive_btn").assertIsDisplayed()
    }

    @Test
    fun weeklyPerformanceScreen_displays7DayChartAndSwitchesModes() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("daily_timeline_lazy_column")
            .performScrollToNode(hasTestTag("weekly_performance_entry_btn"))
        composeRule.onNodeWithTag("weekly_performance_entry_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("weekly_performance_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("weekly_performance_lazy_column")
            .performScrollToNode(hasTestTag("weekly_chart_card"))
        composeRule.onNodeWithTag("weekly_chart_tooltip").assertIsDisplayed()
        composeRule.onNodeWithTag("weekly_chart_mode_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("weekly_chart_mode_area").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("weekly_chart_day_0").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("weekly_chart_tooltip").assertIsDisplayed()
    }

    @Test
    fun smartSuggestionsScreen_displaysRoomAnalysisAndAppliesSchedule() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("daily_timeline_lazy_column")
            .performScrollToNode(hasTestTag("smart_suggestions_entry_btn"))
        composeRule.onNodeWithTag("smart_suggestions_entry_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("smart_suggestions_screen").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("smart_score_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("smart_score_card").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(
                hasTestTag("apply_smart_suggestions_btn") and isEnabled()
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("apply_smart_suggestions_btn")
            .assertIsDisplayed()
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("daily_timeline_lazy_column")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithTag("daily_timeline_lazy_column").assertIsDisplayed()
    }
}
