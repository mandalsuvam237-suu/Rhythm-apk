package com.example

import com.example.data.ai.AiModelTier
import com.example.data.ai.AiRolePreset
import com.example.data.ai.ChatMessage
import com.example.data.ai.PlaceholderAiCoachRepository
import com.example.data.local.SecurityPreferences
import com.example.data.repository.DataExportHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Local unit tests for Rhythm domain & AI placeholder behavior.
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun securityPreferences_defaultsAndPasscodeHashing_workCorrectly() {
        val defaults = SecurityPreferences()
        assertFalse(defaults.lockOnStartup)
        assertTrue(defaults.lockSensitiveCategories)
        assertTrue("cat_personal" in defaults.sensitiveCategoryIds)
        assertTrue("cat_family" in defaults.sensitiveCategoryIds)
        assertFalse("cat_sleep" in defaults.sensitiveCategoryIds)
        assertFalse(defaults.hasBackupPasscode)

        val hash1 = DataExportHelper.sha256("2580")
        val hash2 = DataExportHelper.sha256("2580")
        val wrongHash = DataExportHelper.sha256("9999")
        assertEquals(hash1, hash2)
        assertNotEquals(hash1, wrongHash)
        assertTrue(defaults.copy(backupPasscodeHash = hash1).hasBackupPasscode)
    }

    @Test
    fun placeholderAiCoach_returnsLabeledMultiTurnAndSearchGroundedResponse() = runBlocking {
        val repo = PlaceholderAiCoachRepository()
        assertFalse(repo.isLiveFirebaseConnected())

        val firstTurn = ChatMessage(isFromUser = true, text = "Hello")
        val reply = repo.generateReply(
            history = listOf(firstTurn),
            userPrompt = "Find latest focus music study techniques",
            systemInstruction = AiRolePreset.STUDY_TUTOR.systemInstruction,
            model = AiModelTier.HYDROGEN,
            useSearchGrounding = true,
            scheduleContextSummary = "Today: 3 activities"
        )

        assertTrue(reply.isSamplePlaceholder)
        assertTrue(reply.isSearchGrounded)
        assertTrue(reply.text.contains("Sample Response"))
        assertTrue(reply.text.contains("Turn #2"))
        assertTrue(reply.text.contains("gemini-4-hydrogen"))
        assertTrue(reply.groundingSources.isNotEmpty())
    }
}
