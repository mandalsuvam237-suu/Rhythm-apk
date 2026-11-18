package com.example.data.ai

import kotlinx.coroutines.delay
import java.util.UUID

enum class AiModelTier(
    val modelId: String,
    val displayName: String,
    val taskFocus: String,
    val supportsSearchGrounding: Boolean
) {
    CARBON(
        modelId = "gemini-4-carbon",
        displayName = "Gemini 4 Carbon",
        taskFocus = "Deep reasoning & complex planning",
        supportsSearchGrounding = true
    ),
    ARGON(
        modelId = "gemini-4-argon",
        displayName = "Gemini 4 Argon",
        taskFocus = "Complex interactive tasks",
        supportsSearchGrounding = true
    ),
    HYDROGEN(
        modelId = "gemini-4-hydrogen",
        displayName = "Gemini 4 Hydrogen",
        taskFocus = "General tasks & Search Grounding",
        supportsSearchGrounding = true
    ),
    FLASH_LITE(
        modelId = "gemini-3.1-flash-lite",
        displayName = "Gemini 3.1 Flash-Lite",
        taskFocus = "Fast lightweight replies",
        supportsSearchGrounding = false
    )
}

enum class AiRolePreset(
    val label: String,
    val systemInstruction: String
) {
    PRODUCTIVITY_COACH(
        label = "Coach",
        systemInstruction = "You are Rhythm Coach, a concise, practical time-blocking assistant. Suggest realistic focus intervals, recovery breaks, and priority sequencing tailored to the user's schedule."
    ),
    DEEP_STRATEGIST(
        label = "Strategist",
        systemInstruction = "You are a deep-reasoning productivity architect. Break ambiguous goals into concrete milestones, highlight bottlenecks, and balance cognitive load across the day."
    ),
    STUDY_TUTOR(
        label = "Study Tutor",
        systemInstruction = "You are an evidence-based study coach specializing in active recall, spaced repetition, and structured Pomodoro study blocks."
    ),
    LOCAL_GUIDE(
        label = "Places Guide",
        systemInstruction = "You help organize location-aware routines, commute buffers, and ideal environments for deep work, studying, or outdoor movement."
    )
}

data class GroundingSource(
    val title: String,
    val url: String,
    val snippet: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isFromUser: Boolean,
    val text: String,
    val modelUsed: AiModelTier = AiModelTier.HYDROGEN,
    val isSearchGrounded: Boolean = false,
    val groundingSources: List<GroundingSource> = emptyList(),
    val isSamplePlaceholder: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

data class AiReplyResult(
    val text: String,
    val modelUsed: AiModelTier,
    val isSearchGrounded: Boolean,
    val groundingSources: List<GroundingSource>,
    val isSamplePlaceholder: Boolean
)

/**
 * Abstraction for Rhythm's multi-turn AI coach and Google Search Grounding.
 * Can be backed by Firebase AI Logic when Firebase is enabled, or a clearly labeled
 * sample placeholder when Firebase setup is declined.
 */
interface AiCoachRepository {
    fun isLiveFirebaseConnected(): Boolean

    suspend fun generateReply(
        history: List<ChatMessage>,
        userPrompt: String,
        systemInstruction: String,
        model: AiModelTier,
        useSearchGrounding: Boolean,
        scheduleContextSummary: String
    ): AiReplyResult
}

/**
 * Placeholder implementation used when Firebase setup was declined by the user.
 * Clearly labels every response as a sample placeholder rather than calling an unprovisioned
 * backend or embedding client-side Gemini API keys.
 */
class PlaceholderAiCoachRepository : AiCoachRepository {

    override fun isLiveFirebaseConnected(): Boolean = false

    override suspend fun generateReply(
        history: List<ChatMessage>,
        userPrompt: String,
        systemInstruction: String,
        model: AiModelTier,
        useSearchGrounding: Boolean,
        scheduleContextSummary: String
    ): AiReplyResult {
        delay(40L)

        val effectiveGrounding = useSearchGrounding && model.supportsSearchGrounding
        val turnCount = history.count { it.isFromUser } + 1
        val lower = userPrompt.lowercase()

        val focusTopic = when {
            "study" in lower || "exam" in lower -> "Study Sprint Plan"
            "map" in lower || "place" in lower || "cafe" in lower || "library" in lower -> "Location & Commute Routine"
            "break" in lower || "tired" in lower || "sleep" in lower -> "Recovery & Energy Pacing"
            "current" in lower || "latest" in lower || "today" in lower || "news" in lower || "hours" in lower -> "Live Grounded Lookup"
            else -> "Schedule Optimization"
        }

        val sampleSources = if (effectiveGrounding) {
            listOf(
                GroundingSource(
                    title = "Sample Search Source · Ultradian Focus Cycles",
                    url = "https://example.com/sample/focus-research",
                    snippet = "Placeholder search citation: 90-minute deep work sprints paired with 15–20 minute movement breaks."
                ),
                GroundingSource(
                    title = "Sample Search Source · Local Study Venue Tips",
                    url = "https://example.com/sample/study-venues",
                    snippet = "Placeholder search citation: verify opening hours and quiet-zone availability before commuting."
                )
            )
        } else {
            emptyList()
        }

        val roleHint = systemInstruction.trim().take(72).let {
            if (systemInstruction.length > 72) "$it..." else it
        }

        val reply = buildString {
            appendLine("[Sample Response — Firebase AI Logic not connected]")
            appendLine()
            appendLine("Topic: $focusTopic (Turn #$turnCount via ${model.displayName} [${model.modelId}])")
            appendLine("Role instruction: \"$roleHint\"")
            if (scheduleContextSummary.isNotBlank()) {
                appendLine("Context used: $scheduleContextSummary")
            }
            appendLine()
            when {
                effectiveGrounding -> {
                    appendLine("• Google Search Grounding is toggled ON (${model.modelId}). When connected to Firebase AI Logic, this query uses Tool.googleSearch() to fetch live web facts and cite verified sources below.")
                    appendLine("• Recommended Rhythm step for \"${userPrompt.take(60)}\": block a 45–60 min focused window, add a 15 min buffer if traveling, and attach the destination in Places Map.")
                }
                model == AiModelTier.CARBON -> {
                    appendLine("• Deep Reasoning preview (${model.modelId}):")
                    appendLine("  1. Protect your highest-energy 90m window for high-priority tasks.")
                    appendLine("  2. Group commute or location-based errands back-to-back to avoid context switching.")
                    appendLine("  3. Review completion ratio in Insights before adding late blocks.")
                }
                model == AiModelTier.FLASH_LITE -> {
                    appendLine("• Fast summary (${model.modelId}): Split \"${userPrompt.take(45)}\" into one 25m Focus Timer block + a 5m break, then check off for streak XP.")
                }
                else -> {
                    appendLine("• Interactive coach plan (${model.modelId}):")
                    appendLine("  1. Start with the next upcoming activity in your Today timeline.")
                    appendLine("  2. Use Focus Timer to track actual vs. planned minutes accurately.")
                    appendLine("  3. Pin your workspace on the Places Map if you're working away from home.")
                }
            }
            appendLine()
            append("Enable Firebase AI Logic to replace these sample responses with live Gemini answers.")
        }

        return AiReplyResult(
            text = reply,
            modelUsed = model,
            isSearchGrounded = effectiveGrounding,
            groundingSources = sampleSources,
            isSamplePlaceholder = true
        )
    }
}
