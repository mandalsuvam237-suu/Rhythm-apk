package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.LevelUpApplication
import com.example.data.ai.AiCoachRepository
import com.example.data.ai.AiModelTier
import com.example.data.ai.AiReplyResult
import com.example.data.ai.AiRolePreset
import com.example.data.ai.ChatMessage
import com.example.data.ai.PlaceholderAiCoachRepository
import com.example.data.local.SecurityPreferences
import com.example.data.local.VisualPreferences
import com.example.data.model.AchievementEntity
import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.ActivityPriority
import com.example.data.model.ActivityStatus
import com.example.data.model.DailyPerformanceEntity
import com.example.data.model.FontChoice
import com.example.data.model.HabitEntity
import com.example.data.model.LeaderboardEntryEntity
import com.example.data.model.LeaderboardPeriod
import com.example.data.model.NotificationPreferenceEntity
import com.example.data.model.ScheduleTemplateEntity
import com.example.data.model.ScheduledActivityEntity
import com.example.data.model.ScheduledActivityUi
import com.example.data.model.StatsRange
import com.example.data.model.TemplateActivityEntity
import com.example.data.model.ThemePalette
import com.example.data.model.TimerSessionEntity
import com.example.data.model.UserProfileEntity
import com.example.data.model.XpTransactionEntity
import com.example.domain.DateTimeUtils
import com.example.domain.OverlapConflict
import com.example.domain.PerformanceBreakdown
import com.example.domain.ScheduleEngine
import com.example.domain.SmartSchedulePlan
import com.example.domain.StatsSummary
import com.example.domain.WeeklyPerformanceSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone

enum class TimelineFilter(val label: String) {
    ALL("All"),
    REMAINING("Remaining"),
    COMPLETED("Done")
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as LevelUpApplication).repository
    private val aiCoachRepository: AiCoachRepository = PlaceholderAiCoachRepository()
    val isLiveAiConnected: Boolean get() = aiCoachRepository.isLiveFirebaseConnected()

    private val _selectedDateKey = MutableStateFlow(DateTimeUtils.todayKey())
    val selectedDateKey: StateFlow<String> = _selectedDateKey.asStateFlow()

    private val _highlightedActivityId = MutableStateFlow<String?>(null)
    val highlightedActivityId: StateFlow<String?> = _highlightedActivityId.asStateFlow()

    private val _timelineFilter = MutableStateFlow(TimelineFilter.ALL)
    val timelineFilter: StateFlow<TimelineFilter> = _timelineFilter.asStateFlow()

    private val _statsRange = MutableStateFlow(StatsRange.WEEKLY)
    val statsRange: StateFlow<StatsRange> = _statsRange.asStateFlow()

    private val _customStartKey = MutableStateFlow(DateTimeUtils.shiftDateKey(DateTimeUtils.todayKey(), -6))
    val customStartKey: StateFlow<String> = _customStartKey.asStateFlow()

    private val _customEndKey = MutableStateFlow(DateTimeUtils.todayKey())
    val customEndKey: StateFlow<String> = _customEndKey.asStateFlow()

    private val _leaderboardPeriod = MutableStateFlow(LeaderboardPeriod.WEEKLY)
    val leaderboardPeriod: StateFlow<LeaderboardPeriod> = _leaderboardPeriod.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val _coachMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val coachMessages: StateFlow<List<ChatMessage>> = _coachMessages.asStateFlow()

    private val _coachLoading = MutableStateFlow(false)
    val coachLoading: StateFlow<Boolean> = _coachLoading.asStateFlow()

    private val _selectedAiModel = MutableStateFlow(AiModelTier.HYDROGEN)
    val selectedAiModel: StateFlow<AiModelTier> = _selectedAiModel.asStateFlow()

    private val _selectedAiRole = MutableStateFlow(AiRolePreset.PRODUCTIVITY_COACH)
    val selectedAiRole: StateFlow<AiRolePreset> = _selectedAiRole.asStateFlow()

    private val _customSystemInstruction = MutableStateFlow(AiRolePreset.PRODUCTIVITY_COACH.systemInstruction)
    val customSystemInstruction: StateFlow<String> = _customSystemInstruction.asStateFlow()

    private val _searchGroundingEnabled = MutableStateFlow(true)
    val searchGroundingEnabled: StateFlow<Boolean> = _searchGroundingEnabled.asStateFlow()

    private val _includeScheduleContext = MutableStateFlow(true)
    val includeScheduleContext: StateFlow<Boolean> = _includeScheduleContext.asStateFlow()

    private val _appUnlockedThisSession = MutableStateFlow(false)
    val appUnlockedThisSession: StateFlow<Boolean> = _appUnlockedThisSession.asStateFlow()

    private val _unlockedSensitiveCategoryIds = MutableStateFlow<Set<String>>(emptySet())
    val unlockedSensitiveCategoryIds: StateFlow<Set<String>> = _unlockedSensitiveCategoryIds.asStateFlow()

    private val _smartPlan = MutableStateFlow<SmartSchedulePlan?>(null)
    val smartPlan: StateFlow<SmartSchedulePlan?> = _smartPlan.asStateFlow()

    private val _smartPlanLoading = MutableStateFlow(false)
    val smartPlanLoading: StateFlow<Boolean> = _smartPlanLoading.asStateFlow()

    private val _smartAiSummary = MutableStateFlow<AiReplyResult?>(null)
    val smartAiSummary: StateFlow<AiReplyResult?> = _smartAiSummary.asStateFlow()

    private val _selectedSuggestionIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSuggestionIds: StateFlow<Set<String>> = _selectedSuggestionIds.asStateFlow()

    /**
     * Emits current epoch millis every second so live "NOW" highlight & timestamp-backed timer stay crisp.
     */
    val tickerEpochMs: StateFlow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(1000L)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), System.currentTimeMillis())

    val visualPreferences: StateFlow<VisualPreferences> = repository.visualPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), VisualPreferences())

    val securityPreferences: StateFlow<SecurityPreferences> = repository.securityPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), SecurityPreferences())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val categories: StateFlow<List<ActivityCategoryEntity>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val templates: StateFlow<List<ScheduleTemplateEntity>> = repository.templates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val templateActivities: StateFlow<List<TemplateActivityEntity>> = repository.templateActivities
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeTimer: StateFlow<TimerSessionEntity?> = repository.activeTimer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val recentTimerSessions: StateFlow<List<TimerSessionEntity>> = repository.recentTimerSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val habits: StateFlow<List<HabitEntity>> = repository.habits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val achievements: StateFlow<List<AchievementEntity>> = repository.achievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val xpTransactions: StateFlow<List<XpTransactionEntity>> = repository.xpTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val leaderboard: StateFlow<List<LeaderboardEntryEntity>> = repository.leaderboard
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notificationPreference: StateFlow<NotificationPreferenceEntity?> = repository.notificationPreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val scheduleItems: StateFlow<List<ScheduledActivityUi>> = _selectedDateKey
        .flatMapLatest { dateKey -> repository.observeScheduleForDate(dateKey) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val weekActivityCounts: StateFlow<Map<String, Int>> = _selectedDateKey
        .flatMapLatest { anchor ->
            val start = DateTimeUtils.shiftDateKey(anchor, -3)
            val end = DateTimeUtils.shiftDateKey(anchor, 3)
            repository.observeDayCountsInRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val selectedDayPerformance: StateFlow<PerformanceBreakdown> = combine(
        scheduleItems,
        userProfile
    ) { items, user ->
        val dayActs = items.filter { !it.isCarriedOverFromPrevDay }.map { it.activity }
        ScheduleEngine.calculatePerformance(dayActs, user?.currentStreak ?: 0)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        PerformanceBreakdown(0, 0, 0, 0, 0, 0, 0, 0, 0)
    )

    val yesterdayPerformance: StateFlow<DailyPerformanceEntity?> = _selectedDateKey
        .flatMapLatest { key ->
            val prevKey = DateTimeUtils.shiftDateKey(key, -1)
            repository.observePerformanceForDate(prevKey)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val statsSummary: StateFlow<StatsSummary> = combine(
        _statsRange,
        _selectedDateKey,
        _customStartKey,
        _customEndKey
    ) { range, anchor, customStart, customEnd ->
        when (range) {
            StatsRange.DAILY -> anchor to anchor
            StatsRange.WEEKLY -> DateTimeUtils.shiftDateKey(anchor, -6) to anchor
            StatsRange.MONTHLY -> DateTimeUtils.shiftDateKey(anchor, -29) to anchor
            StatsRange.YEARLY -> DateTimeUtils.shiftDateKey(anchor, -364) to anchor
            StatsRange.CUSTOM -> {
                if (customStart <= customEnd) customStart to customEnd else customEnd to customStart
            }
        }
    }.flatMapLatest { (startKey, endKey) ->
        repository.observeStatsForRange(startKey, endKey)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        StatsSummary(0, 0, 0, 0, 0, 0, emptyList(), emptyList())
    )

    val weeklyPerformance: StateFlow<WeeklyPerformanceSummary> = _selectedDateKey
        .flatMapLatest { anchor -> repository.observeWeeklyPerformance(anchor) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            ScheduleEngine.calculateWeeklyPerformance(emptyList(), emptyList(), DateTimeUtils.todayKey())
        )

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    fun selectDate(dateKey: String) {
        _selectedDateKey.value = dateKey
    }

    fun shiftSelectedDate(days: Int) {
        _selectedDateKey.value = DateTimeUtils.shiftDateKey(_selectedDateKey.value, days)
    }

    fun jumpToToday() {
        _selectedDateKey.value = DateTimeUtils.todayKey()
    }

    fun selectDateFromUtcMillis(utcMillis: Long) {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = utcMillis
        }
        val localCal = Calendar.getInstance().apply {
            set(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH),
                0, 0, 0
            )
            set(Calendar.MILLISECOND, 0)
        }
        _selectedDateKey.value = DateTimeUtils.formatKey(localCal)
    }

    fun setTimelineFilter(filter: TimelineFilter) {
        _timelineFilter.value = filter
    }

    fun focusActivity(activityId: String?, dateKey: String? = null) {
        dateKey?.let { _selectedDateKey.value = it }
        _highlightedActivityId.value = activityId
    }

    fun setStatsRange(range: StatsRange) {
        _statsRange.value = range
    }

    fun setCustomDateRange(startKey: String, endKey: String) {
        _customStartKey.value = startKey
        _customEndKey.value = endKey
        _statsRange.value = StatsRange.CUSTOM
    }

    fun setLeaderboardPeriod(period: LeaderboardPeriod) {
        _leaderboardPeriod.value = period
    }

    suspend fun checkOverlaps(
        candidateId: String?,
        dateKey: String,
        startMinute: Int,
        durationMinutes: Int
    ): List<OverlapConflict> = repository.findOverlaps(candidateId, dateKey, startMinute, durationMinutes)

    suspend fun findNextAvailableSlot(dateKey: String, preferredStart: Int, durationMinutes: Int): Int =
        repository.findNextAvailableSlot(dateKey, preferredStart, durationMinutes)

    fun saveActivity(
        existingId: String?,
        dateKey: String,
        title: String,
        notes: String,
        categoryId: String,
        startMinute: Int,
        durationMinutes: Int,
        priority: ActivityPriority,
        isRepeating: Boolean,
        repeatDaysMask: Int,
        reminderBeforeMinutes: Int,
        locationName: String = "",
        latitude: Double? = null,
        longitude: Double? = null
    ) {
        viewModelScope.launch {
            repository.saveActivity(
                existingId, dateKey, title, notes, categoryId,
                startMinute, durationMinutes, priority, isRepeating,
                repeatDaysMask, reminderBeforeMinutes,
                locationName, latitude, longitude
            )
            _snackbarEvents.tryEmit(if (existingId == null) "Activity added" else "Activity updated")
        }
    }

    fun deleteActivity(activityId: String) {
        viewModelScope.launch {
            repository.deleteActivity(activityId)
            _snackbarEvents.tryEmit("Activity removed")
        }
    }

    fun setActivityStatus(activityId: String, status: ActivityStatus) {
        viewModelScope.launch {
            repository.setActivityStatus(activityId, status)
        }
    }

    fun swapActivityTimes(firstId: String, secondId: String) {
        viewModelScope.launch {
            repository.swapActivityTimes(firstId, secondId)
            _snackbarEvents.tryEmit("Activities rearranged")
        }
    }

    fun moveActivityTime(activityId: String, newStartMinute: Int) {
        viewModelScope.launch {
            repository.moveActivityTime(activityId, newStartMinute)
            _snackbarEvents.tryEmit("Activity rescheduled")
        }
    }

    fun rescheduleUnfinishedToTomorrow() {
        viewModelScope.launch {
            val fromKey = _selectedDateKey.value
            val toKey = DateTimeUtils.shiftDateKey(fromKey, 1)
            val moved = repository.rescheduleUnfinishedActivities(fromKey, toKey)
            _snackbarEvents.tryEmit(
                if (moved > 0) "Moved $moved unfinished block(s) to ${DateTimeUtils.formatMonthDay(toKey)}"
                else "No unfinished blocks to reschedule"
            )
        }
    }

    // Templates
    fun applyTemplateToSelectedDate(templateId: String) {
        viewModelScope.launch {
            val res = repository.applyTemplateToDate(templateId, _selectedDateKey.value)
            _snackbarEvents.tryEmit(res.message)
        }
    }

    fun applyTemplateToDate(templateId: String, targetDateKey: String) {
        viewModelScope.launch {
            val res = repository.applyTemplateToDate(templateId, targetDateKey)
            _snackbarEvents.tryEmit("${DateTimeUtils.formatMonthDay(targetDateKey)}: ${res.message}")
        }
    }

    fun saveTemplate(
        existingId: String?,
        name: String,
        description: String,
        accentColorHex: String,
        items: List<TemplateActivityEntity>
    ) {
        viewModelScope.launch {
            repository.saveTemplate(existingId, name, description, accentColorHex, items)
            _snackbarEvents.tryEmit("Template saved")
        }
    }

    fun duplicateTemplate(templateId: String) {
        viewModelScope.launch {
            repository.duplicateTemplate(templateId)
            _snackbarEvents.tryEmit("Template duplicated")
        }
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            repository.deleteTemplate(templateId)
            _snackbarEvents.tryEmit("Template deleted")
        }
    }

    // Categories & Habits
    fun saveCategory(existingId: String?, name: String, iconKey: String, colorHex: String) {
        viewModelScope.launch {
            repository.saveCategory(existingId, name, iconKey, colorHex)
            _snackbarEvents.tryEmit("Category saved")
        }
    }

    fun deleteCustomCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCustomCategory(categoryId)
            _snackbarEvents.tryEmit("Custom category deleted")
        }
    }

    fun saveHabit(title: String, categoryId: String, targetDaysPerWeek: Int) {
        viewModelScope.launch {
            repository.saveHabit(title, categoryId, targetDaysPerWeek)
            _snackbarEvents.tryEmit("Habit added")
        }
    }

    fun toggleHabitCompletedToday(habit: HabitEntity) {
        viewModelScope.launch {
            repository.toggleHabitCompletedToday(habit)
            _snackbarEvents.tryEmit("Habit checked in (+25 XP)")
        }
    }

    fun deleteHabit(id: String) {
        viewModelScope.launch {
            repository.deleteHabit(id)
        }
    }

    // Focus Timer
    fun startFocusForActivity(activity: ScheduledActivityEntity) {
        viewModelScope.launch {
            repository.startTimer(
                activityId = activity.id,
                title = activity.title,
                categoryId = activity.categoryId,
                plannedMinutes = activity.durationMinutes,
                dateKey = activity.dateKey
            )
            _snackbarEvents.tryEmit("Focus started: ${activity.title}")
        }
    }

    fun startQuickFocus(title: String, categoryId: String, plannedMinutes: Int) {
        viewModelScope.launch {
            repository.startTimer(null, title, categoryId, plannedMinutes)
            _snackbarEvents.tryEmit("Focus started")
        }
    }

    fun pauseTimer(sessionId: String) {
        viewModelScope.launch { repository.pauseTimer(sessionId) }
    }

    fun resumeTimer(sessionId: String) {
        viewModelScope.launch { repository.resumeTimer(sessionId) }
    }

    fun stopTimer(sessionId: String, markCompleted: Boolean) {
        viewModelScope.launch {
            repository.stopTimer(sessionId, markCompleted)
            _snackbarEvents.tryEmit("Focus session saved")
        }
    }

    fun resetTimer(sessionId: String) {
        viewModelScope.launch {
            repository.resetTimer(sessionId)
            _snackbarEvents.tryEmit("Timer reset")
        }
    }

    // Visual, Security & Notification Preferences
    fun setThemePalette(palette: ThemePalette) {
        viewModelScope.launch { repository.setThemePalette(palette) }
    }

    fun setFontChoice(font: FontChoice) {
        viewModelScope.launch { repository.setFontChoice(font) }
    }

    fun setCompactCards(compact: Boolean) {
        viewModelScope.launch { repository.setCompactCards(compact) }
    }

    fun setLockOnStartup(enabled: Boolean) {
        if (enabled) {
            // Keep active session unlocked while user finishes configuring settings
            _appUnlockedThisSession.value = true
        }
        viewModelScope.launch {
            repository.setLockOnStartup(enabled)
            if (enabled) {
                _snackbarEvents.tryEmit("Startup biometric lock enabled")
            } else {
                _snackbarEvents.tryEmit("Startup lock disabled")
            }
        }
    }

    fun setLockSensitiveCategories(enabled: Boolean) {
        viewModelScope.launch {
            repository.setLockSensitiveCategories(enabled)
            if (enabled) {
                _unlockedSensitiveCategoryIds.value = emptySet()
                _snackbarEvents.tryEmit("Sensitive category protection enabled")
            } else {
                _snackbarEvents.tryEmit("Sensitive category protection disabled")
            }
        }
    }

    fun toggleSensitiveCategory(categoryId: String) {
        viewModelScope.launch {
            repository.toggleSensitiveCategory(categoryId)
            _unlockedSensitiveCategoryIds.value = _unlockedSensitiveCategoryIds.value - categoryId
        }
    }

    fun setCategorySensitive(categoryId: String, sensitive: Boolean) {
        viewModelScope.launch {
            repository.setCategorySensitive(categoryId, sensitive)
            if (sensitive) {
                _unlockedSensitiveCategoryIds.value = _unlockedSensitiveCategoryIds.value - categoryId
                _snackbarEvents.tryEmit("Category protected with biometrics")
            } else {
                _snackbarEvents.tryEmit("Category protection removed")
            }
        }
    }

    fun setBackupPasscode(passcode: String) {
        viewModelScope.launch {
            repository.setBackupPasscode(passcode)
            _snackbarEvents.tryEmit(if (passcode.isBlank()) "Backup passcode cleared" else "Backup passcode saved")
        }
    }

    fun verifyBackupPasscode(passcode: String): Boolean =
        repository.verifyBackupPasscode(passcode, securityPreferences.value.backupPasscodeHash)

    fun markAppUnlocked() {
        _appUnlockedThisSession.value = true
    }

    fun onAppBackgrounded() {
        if (securityPreferences.value.lockOnStartup) {
            _appUnlockedThisSession.value = false
        }
        _unlockedSensitiveCategoryIds.value = emptySet()
    }

    fun lockAllProtectedContent() {
        _appUnlockedThisSession.value = false
        _unlockedSensitiveCategoryIds.value = emptySet()
    }

    fun unlockSensitiveCategory(categoryId: String) {
        _unlockedSensitiveCategoryIds.value = _unlockedSensitiveCategoryIds.value + categoryId
    }

    fun lockSensitiveCategoriesNow() {
        _unlockedSensitiveCategoryIds.value = emptySet()
        _snackbarEvents.tryEmit("Sensitive categories locked")
    }

    fun saveNotificationPreference(pref: NotificationPreferenceEntity) {
        viewModelScope.launch {
            repository.saveNotificationPreference(pref)
            _snackbarEvents.tryEmit("Notification preferences saved")
        }
    }

    // Account, Demo Mode, Export, Sync, Delete
    fun updateProfile(displayName: String, email: String) {
        viewModelScope.launch {
            repository.updateProfile(displayName, email)
            _snackbarEvents.tryEmit("Profile updated")
        }
    }

    fun registerOrLogin(displayName: String, email: String, password: String) {
        viewModelScope.launch {
            val result = repository.registerOrSignIn(displayName, email, password)
            _snackbarEvents.tryEmit(
                result.fold(
                    onSuccess = { "Signed in as ${it.displayName}" },
                    onFailure = { it.message ?: "Authentication error" }
                )
            )
        }
    }

    fun recoverPassword(email: String, newPassword: String) {
        viewModelScope.launch {
            val result = repository.recoverPassword(email, newPassword)
            _snackbarEvents.tryEmit(
                result.fold(
                    onSuccess = { "Password updated locally" },
                    onFailure = { it.message ?: "Recovery failed" }
                )
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.setLoggedIn(false)
            _snackbarEvents.tryEmit("Signed out")
        }
    }

    fun setLeaderboardOptIn(optIn: Boolean) {
        viewModelScope.launch {
            repository.setLeaderboardOptIn(optIn)
            _snackbarEvents.tryEmit(
                if (optIn) "Joined leaderboard" else "Hidden from leaderboard"
            )
        }
    }

    fun setDemoMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setDemoMode(enabled)
            _snackbarEvents.tryEmit(if (enabled) "Demo Mode enabled with sample history" else "Demo Mode cleared")
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            repository.syncSnapshotLocally()
            _snackbarEvents.tryEmit("Local database snapshot verified")
        }
    }

    suspend fun exportAllDataJson(): String = repository.exportAllDataJson()

    fun deleteAccountAndReset() {
        viewModelScope.launch {
            repository.deleteAccountAndAllData()
            _snackbarEvents.tryEmit("Account and all local data reset")
        }
    }

    // AI Coach & Google Search Grounding
    fun selectAiModel(model: AiModelTier) {
        _selectedAiModel.value = model
    }

    fun selectAiRole(role: AiRolePreset) {
        _selectedAiRole.value = role
        _customSystemInstruction.value = role.systemInstruction
    }

    fun updateCustomSystemInstruction(instruction: String) {
        _customSystemInstruction.value = instruction
    }

    fun setSearchGroundingEnabled(enabled: Boolean) {
        _searchGroundingEnabled.value = enabled
        if (enabled && !_selectedAiModel.value.supportsSearchGrounding) {
            _selectedAiModel.value = AiModelTier.HYDROGEN
        }
    }

    fun setIncludeScheduleContext(include: Boolean) {
        _includeScheduleContext.value = include
    }

    fun sendCoachMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isBlank() || _coachLoading.value) return
        val historySnapshot = _coachMessages.value
        val userMsg = ChatMessage(
            isFromUser = true,
            text = prompt,
            modelUsed = _selectedAiModel.value
        )
        _coachMessages.value = historySnapshot + userMsg
        _coachLoading.value = true

        viewModelScope.launch {
            try {
                val contextSummary = if (_includeScheduleContext.value) {
                    val sec = securityPreferences.value
                    val lockedCats = if (sec.lockSensitiveCategories) {
                        sec.sensitiveCategoryIds - _unlockedSensitiveCategoryIds.value
                    } else emptySet()
                    val items = scheduleItems.value
                        .filter { !it.isCarriedOverFromPrevDay && it.activity.categoryId !in lockedCats }
                    val perf = selectedDayPerformance.value
                    val dateLabel = DateTimeUtils.formatDisplayDate(_selectedDateKey.value)
                    val pinnedPlaces = items.mapNotNull { it.activity.locationName.takeIf(String::isNotBlank) }.distinct()
                    buildString {
                        append("$dateLabel: ${items.size} visible activities (${perf.completionPercent}% done, score ${perf.score}/100)")
                        if (pinnedPlaces.isNotEmpty()) {
                            append("; places: ${pinnedPlaces.joinToString()}")
                        }
                    }
                } else {
                    ""
                }

                val result = aiCoachRepository.generateReply(
                    history = historySnapshot,
                    userPrompt = prompt,
                    systemInstruction = _customSystemInstruction.value.ifBlank { _selectedAiRole.value.systemInstruction },
                    model = _selectedAiModel.value,
                    useSearchGrounding = _searchGroundingEnabled.value,
                    scheduleContextSummary = contextSummary
                )

                val assistantMsg = ChatMessage(
                    isFromUser = false,
                    text = result.text,
                    modelUsed = result.modelUsed,
                    isSearchGrounded = result.isSearchGrounded,
                    groundingSources = result.groundingSources,
                    isSamplePlaceholder = result.isSamplePlaceholder
                )
                _coachMessages.value = _coachMessages.value + assistantMsg
            } finally {
                _coachLoading.value = false
            }
        }
    }

    fun clearCoachChat() {
        _coachMessages.value = emptyList()
    }

    // Smart Schedule Suggestion Engine (Room completed tasks + AI Coach summary)
    fun generateSmartSuggestions(
        targetDateKey: String = DateTimeUtils.shiftDateKey(_selectedDateKey.value, 1)
    ) {
        if (_smartPlanLoading.value) return
        _smartPlanLoading.value = true
        viewModelScope.launch {
            try {
                val sec = securityPreferences.value
                val lockedCats = if (sec.lockSensitiveCategories) {
                    sec.sensitiveCategoryIds - _unlockedSensitiveCategoryIds.value
                } else emptySet()

                val plan = repository.generateSmartSchedulePlan(
                    targetDateKey = targetDateKey,
                    excludedCategoryIds = lockedCats
                )
                _smartPlan.value = plan
                _selectedSuggestionIds.value = plan.suggestions.map { it.id }.toSet()
                _smartPlanLoading.value = false

                val summaryPrompt = buildString {
                    append("Summarize how this ${plan.suggestions.size}-block upcoming schedule ")
                    append("(projected score ${plan.suggestedProjectedScore}/100, ")
                    append("${plan.analyzedCompletedCount} completed Room tasks analyzed, ")
                    append("peak hour ${DateTimeUtils.formatMinuteOfDay(plan.peakFocusHour * 60)}) ")
                    append("improves productivity score.")
                }
                val aiSummary = aiCoachRepository.generateReply(
                    history = emptyList(),
                    userPrompt = summaryPrompt,
                    systemInstruction = AiRolePreset.DEEP_STRATEGIST.systemInstruction,
                    model = _selectedAiModel.value,
                    useSearchGrounding = false,
                    scheduleContextSummary = plan.insights.joinToString(" | ")
                )
                _smartAiSummary.value = aiSummary
            } finally {
                _smartPlanLoading.value = false
            }
        }
    }

    fun toggleSuggestionSelection(suggestionId: String) {
        val current = _selectedSuggestionIds.value
        _selectedSuggestionIds.value = if (suggestionId in current) {
            current - suggestionId
        } else {
            current + suggestionId
        }
    }

    fun applySelectedSmartSuggestions(onAppliedToDate: (String) -> Unit = {}) {
        val plan = _smartPlan.value ?: return
        val chosen = plan.suggestions.filter { it.id in _selectedSuggestionIds.value }
        if (chosen.isEmpty()) {
            _snackbarEvents.tryEmit("Select at least one suggested block to apply")
            return
        }
        viewModelScope.launch {
            val added = repository.applySmartSuggestions(plan.targetDateKey, chosen)
            if (added > 0) {
                _selectedDateKey.value = plan.targetDateKey
                _snackbarEvents.tryEmit(
                    "Added $added smart-suggested block(s) to ${DateTimeUtils.formatMonthDay(plan.targetDateKey)}"
                )
                onAppliedToDate(plan.targetDateKey)
            } else {
                _snackbarEvents.tryEmit("Could not place blocks — day timeline is already full")
            }
        }
    }
}
