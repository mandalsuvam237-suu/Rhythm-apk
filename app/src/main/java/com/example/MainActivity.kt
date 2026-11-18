package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.AutoAwesomeMosaic
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.notifications.NotificationHelper
import com.example.security.BiometricAuthHelper
import com.example.security.BiometricCapability
import com.example.ui.components.BackupPasscodeVerifyDialog
import com.example.ui.components.StartupBiometricLockOverlay
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PlacesMapScreen
import com.example.ui.screens.SettingsAndProfileScreen
import com.example.ui.screens.SmartSuggestionsScreen
import com.example.ui.screens.StatsAndRankScreen
import com.example.ui.screens.TemplatesAndLibraryScreen
import com.example.ui.screens.WeeklyPerformanceScreen
import com.example.ui.theme.LevelUpTheme
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

enum class AppDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    TODAY("today", "Today", Icons.Rounded.CalendarToday, Icons.Outlined.CalendarToday),
    FOCUS("focus", "Focus", Icons.Rounded.Timer, Icons.Outlined.Timer),
    TEMPLATES("templates", "Library", Icons.Rounded.AutoAwesomeMosaic, Icons.Outlined.AutoAwesomeMosaic),
    STATS("stats", "Insights", Icons.Rounded.BarChart, Icons.Outlined.BarChart),
    SETTINGS("settings", "Profile", Icons.Rounded.Settings, Icons.Outlined.Settings)
}

private const val ROUTE_COACH = "coach"
private const val ROUTE_PLACES = "places"
private const val ROUTE_WEEKLY_PERFORMANCE = "weekly_performance"
private const val ROUTE_SMART_SUGGESTIONS = "smart_suggestions"

private data class PendingPasscodeAuth(
    val title: String,
    val subtitle: String,
    val onSuccess: () -> Unit
)

class MainActivity : FragmentActivity() {

    private var pendingIntentRef: Intent? = null
    private var viewModelRef: MainViewModel? = null
    private var isAuthenticatingBiometric: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingIntentRef = intent
        setContent {
            val viewModel: MainViewModel = viewModel()
            viewModelRef = viewModel
            LaunchedEffect(Unit) {
                handleDeepLink(intent, viewModel)
            }
            LevelUpApp(
                viewModel = viewModel,
                onBiometricPromptStateChanged = { active ->
                    isAuthenticatingBiometric = active
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntentRef = intent
        viewModelRef?.let { handleDeepLink(intent, it) }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && !isAuthenticatingBiometric) {
            viewModelRef?.onAppBackgrounded()
        }
    }

    private fun handleDeepLink(intent: Intent?, viewModel: MainViewModel) {
        if (intent == null) return
        val dateKey = intent.getStringExtra(NotificationHelper.EXTRA_DATE_KEY)
        val activityId = intent.getStringExtra(NotificationHelper.EXTRA_ACTIVITY_ID)
        if (dateKey != null || activityId != null) {
            viewModel.focusActivity(activityId, dateKey)
        }
    }
}

@Composable
fun LevelUpApp(
    viewModel: MainViewModel,
    onBiometricPromptStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val visualPrefs by viewModel.visualPreferences.collectAsStateWithLifecycle()
    val securityPrefs by viewModel.securityPreferences.collectAsStateWithLifecycle()
    val appUnlockedThisSession by viewModel.appUnlockedThisSession.collectAsStateWithLifecycle()
    val unlockedSensitiveCategoryIds by viewModel.unlockedSensitiveCategoryIds.collectAsStateWithLifecycle()

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val selectedDateKey by viewModel.selectedDateKey.collectAsStateWithLifecycle()
    val scheduleItems by viewModel.scheduleItems.collectAsStateWithLifecycle()
    val weekActivityCounts by viewModel.weekActivityCounts.collectAsStateWithLifecycle()
    val timelineFilter by viewModel.timelineFilter.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val performance by viewModel.selectedDayPerformance.collectAsStateWithLifecycle()
    val yesterdayPerformance by viewModel.yesterdayPerformance.collectAsStateWithLifecycle()
    val highlightedId by viewModel.highlightedActivityId.collectAsStateWithLifecycle()
    val tickerEpochMs by viewModel.tickerEpochMs.collectAsStateWithLifecycle()

    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val templateActivities by viewModel.templateActivities.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()

    val activeTimer by viewModel.activeTimer.collectAsStateWithLifecycle()
    val recentTimerSessions by viewModel.recentTimerSessions.collectAsStateWithLifecycle()

    val statsRange by viewModel.statsRange.collectAsStateWithLifecycle()
    val customStart by viewModel.customStartKey.collectAsStateWithLifecycle()
    val customEnd by viewModel.customEndKey.collectAsStateWithLifecycle()
    val statsSummary by viewModel.statsSummary.collectAsStateWithLifecycle()
    val weeklyPerformance by viewModel.weeklyPerformance.collectAsStateWithLifecycle()
    val leaderboardPeriod by viewModel.leaderboardPeriod.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val xpTransactions by viewModel.xpTransactions.collectAsStateWithLifecycle()
    val notificationPref by viewModel.notificationPreference.collectAsStateWithLifecycle()

    val coachMessages by viewModel.coachMessages.collectAsStateWithLifecycle()
    val coachLoading by viewModel.coachLoading.collectAsStateWithLifecycle()
    val selectedAiModel by viewModel.selectedAiModel.collectAsStateWithLifecycle()
    val selectedAiRole by viewModel.selectedAiRole.collectAsStateWithLifecycle()
    val customSystemInstruction by viewModel.customSystemInstruction.collectAsStateWithLifecycle()
    val searchGroundingEnabled by viewModel.searchGroundingEnabled.collectAsStateWithLifecycle()
    val includeScheduleContext by viewModel.includeScheduleContext.collectAsStateWithLifecycle()

    val smartPlan by viewModel.smartPlan.collectAsStateWithLifecycle()
    val smartPlanLoading by viewModel.smartPlanLoading.collectAsStateWithLifecycle()
    val smartAiSummary by viewModel.smartAiSummary.collectAsStateWithLifecycle()
    val selectedSuggestionIds by viewModel.selectedSuggestionIds.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppDestination.TODAY.route
    val showTopLevelNav = currentRoute != ROUTE_COACH &&
        currentRoute != ROUTE_PLACES &&
        currentRoute != ROUTE_WEEKLY_PERFORMANCE &&
        currentRoute != ROUTE_SMART_SUGGESTIONS
    val snackbarHostState = remember { SnackbarHostState() }

    var startupAuthError by remember { mutableStateOf<String?>(null) }
    var pendingPasscodeAuth by remember { mutableStateOf<PendingPasscodeAuth?>(null) }

    fun launchBiometricOrFallback(
        title: String,
        subtitle: String,
        isStartup: Boolean = false,
        onSuccess: () -> Unit
    ) {
        val capability = BiometricAuthHelper.checkCapability(context)
        val fragmentActivity = BiometricAuthHelper.findFragmentActivity(context)
        if (capability == BiometricCapability.READY && fragmentActivity != null) {
            onBiometricPromptStateChanged(true)
            BiometricAuthHelper.authenticate(
                activity = fragmentActivity,
                title = title,
                subtitle = subtitle,
                negativeButtonText = if (securityPrefs.hasBackupPasscode) "Use Backup Passcode" else null,
                onSuccess = {
                    onBiometricPromptStateChanged(false)
                    startupAuthError = null
                    onSuccess()
                },
                onError = { errorCode, errMsg ->
                    onBiometricPromptStateChanged(false)
                    val shouldFallbackToPasscode = securityPrefs.hasBackupPasscode && (
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                            errorCode == BiometricPrompt.ERROR_HW_UNAVAILABLE ||
                            errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT
                        )
                    if (shouldFallbackToPasscode) {
                        pendingPasscodeAuth = PendingPasscodeAuth(title, subtitle, onSuccess)
                    } else if (isStartup) {
                        startupAuthError = errMsg
                    } else if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_CANCELED
                    ) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Authentication cancelled: $errMsg")
                        }
                    }
                }
            )
        } else if (securityPrefs.hasBackupPasscode) {
            pendingPasscodeAuth = PendingPasscodeAuth(title, subtitle, onSuccess)
        } else if (!isStartup) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("No device biometrics or passcode configured — temporarily unlocked.")
            }
            onSuccess()
        } else {
            startupAuthError = capability.detail
        }
    }

    val requestUnlockCategory: (String, String, () -> Unit) -> Unit = { categoryId, categoryName, onUnlocked ->
        launchBiometricOrFallback(
            title = "Unlock $categoryName",
            subtitle = "Verify your identity to view or edit protected notes.",
            isStartup = false,
            onSuccess = {
                viewModel.unlockSensitiveCategory(categoryId)
                onUnlocked()
            }
        )
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isExpandedWidth = LocalConfiguration.current.screenWidthDp >= 720

    LevelUpTheme(
        themePalette = visualPrefs.themePalette,
        fontChoice = visualPrefs.fontChoice
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (!isExpandedWidth && showTopLevelNav) {
                        NavigationBar {
                            AppDestination.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(AppDestination.TODAY.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isExpandedWidth && showTopLevelNav) {
                        NavigationRail {
                            AppDestination.entries.forEach { dest ->
                                val selected = currentRoute == dest.route
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(dest.route) {
                                            popUpTo(AppDestination.TODAY.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = { Text(dest.label) },
                                    modifier = Modifier.testTag("nav_${dest.route}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = AppDestination.TODAY.route
                        ) {
                            composable(AppDestination.TODAY.route) {
                                HomeScreen(
                                    selectedDateKey = selectedDateKey,
                                    scheduleItems = scheduleItems,
                                    weekActivityCounts = weekActivityCounts,
                                    timelineFilter = timelineFilter,
                                    categories = categories,
                                    performance = performance,
                                    userProfile = userProfile,
                                    highlightedActivityId = highlightedId,
                                    compactCards = visualPrefs.compactCards,
                                    tickerEpochMs = tickerEpochMs,
                                    onSelectDate = viewModel::selectDate,
                                    onShiftDate = viewModel::shiftSelectedDate,
                                    onJumpToToday = viewModel::jumpToToday,
                                    onSelectDateUtcMillis = viewModel::selectDateFromUtcMillis,
                                    onSetTimelineFilter = viewModel::setTimelineFilter,
                                    onCheckOverlap = viewModel::checkOverlaps,
                                    onFindNextSlot = viewModel::findNextAvailableSlot,
                                    onSaveActivity = viewModel::saveActivity,
                                    onDeleteActivity = viewModel::deleteActivity,
                                    onSetStatus = viewModel::setActivityStatus,
                                    onSwapActivities = viewModel::swapActivityTimes,
                                    onStartFocus = { activity ->
                                        viewModel.startFocusForActivity(activity)
                                        navController.navigate(AppDestination.FOCUS.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onRescheduleUnfinished = viewModel::rescheduleUnfinishedToTomorrow,
                                    onNavigateToTemplates = {
                                        navController.navigate(AppDestination.TEMPLATES.route) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onNavigateToCoach = {
                                        navController.navigate(ROUTE_COACH) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onNavigateToPlacesMap = {
                                        navController.navigate(ROUTE_PLACES) {
                                            launchSingleTop = true
                                        }
                                    },
                                    securityPrefs = securityPrefs,
                                    unlockedSensitiveCategoryIds = unlockedSensitiveCategoryIds,
                                    onRequestUnlockCategory = requestUnlockCategory,
                                    onLockSensitiveNow = viewModel::lockSensitiveCategoriesNow,
                                    onNavigateToWeeklyPerformance = {
                                        navController.navigate(ROUTE_WEEKLY_PERFORMANCE) {
                                            launchSingleTop = true
                                        }
                                    },
                                    onNavigateToSmartSuggestions = {
                                        navController.navigate(ROUTE_SMART_SUGGESTIONS) {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable(AppDestination.FOCUS.route) {
                                FocusTimerScreen(
                                    activeTimer = activeTimer,
                                    recentSessions = recentTimerSessions,
                                    todayActivities = scheduleItems,
                                    categories = categories,
                                    tickerEpochMs = tickerEpochMs,
                                    onStartActivityTimer = viewModel::startFocusForActivity,
                                    onStartQuickTimer = viewModel::startQuickFocus,
                                    onPauseTimer = viewModel::pauseTimer,
                                    onResumeTimer = viewModel::resumeTimer,
                                    onStopTimer = viewModel::stopTimer,
                                    onResetTimer = viewModel::resetTimer
                                )
                            }

                            composable(AppDestination.TEMPLATES.route) {
                                TemplatesAndLibraryScreen(
                                    selectedDateKey = selectedDateKey,
                                    templates = templates,
                                    templateActivities = templateActivities,
                                    categories = categories,
                                    habits = habits,
                                    onApplyTemplateToDate = viewModel::applyTemplateToDate,
                                    onDuplicateTemplate = viewModel::duplicateTemplate,
                                    onDeleteTemplate = viewModel::deleteTemplate,
                                    onSaveTemplate = viewModel::saveTemplate,
                                    onSaveCategory = viewModel::saveCategory,
                                    onDeleteCategory = viewModel::deleteCustomCategory,
                                    onSaveHabit = viewModel::saveHabit,
                                    onToggleHabitToday = viewModel::toggleHabitCompletedToday,
                                    onDeleteHabit = viewModel::deleteHabit,
                                    securityPrefs = securityPrefs,
                                    unlockedSensitiveCategoryIds = unlockedSensitiveCategoryIds,
                                    onSetCategorySensitive = viewModel::setCategorySensitive,
                                    onRequestUnlockCategory = requestUnlockCategory
                                )
                            }

                            composable(AppDestination.STATS.route) {
                                StatsAndRankScreen(
                                    selectedDateKey = selectedDateKey,
                                    statsRange = statsRange,
                                    customStartKey = customStart,
                                    customEndKey = customEnd,
                                    statsSummary = statsSummary,
                                    todayPerformance = performance,
                                    yesterdayPerformance = yesterdayPerformance,
                                    userProfile = userProfile,
                                    leaderboardPeriod = leaderboardPeriod,
                                    leaderboard = leaderboard,
                                    achievements = achievements,
                                    xpTransactions = xpTransactions,
                                    onShiftDate = viewModel::shiftSelectedDate,
                                    onSelectStatsRange = viewModel::setStatsRange,
                                    onSetCustomRange = viewModel::setCustomDateRange,
                                    onSelectLeaderboardPeriod = viewModel::setLeaderboardPeriod,
                                    onToggleLeaderboardOptIn = viewModel::setLeaderboardOptIn,
                                    onNavigateToWeeklyPerformance = {
                                        navController.navigate(ROUTE_WEEKLY_PERFORMANCE) {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            }

                            composable(AppDestination.SETTINGS.route) {
                                SettingsAndProfileScreen(
                                    visualPreferences = visualPrefs,
                                    userProfile = userProfile,
                                    notificationPref = notificationPref,
                                    onSelectTheme = viewModel::setThemePalette,
                                    onSelectFont = viewModel::setFontChoice,
                                    onSetCompactCards = viewModel::setCompactCards,
                                    onSaveNotificationPref = viewModel::saveNotificationPreference,
                                    onUpdateProfile = viewModel::updateProfile,
                                    onRegisterOrLogin = viewModel::registerOrLogin,
                                    onRecoverPassword = viewModel::recoverPassword,
                                    onLogout = viewModel::logout,
                                    onToggleDemoMode = viewModel::setDemoMode,
                                    onToggleLeaderboardOptIn = viewModel::setLeaderboardOptIn,
                                    onSyncNow = viewModel::syncNow,
                                    onExportDataJson = viewModel::exportAllDataJson,
                                    onDeleteAccount = viewModel::deleteAccountAndReset,
                                    securityPreferences = securityPrefs,
                                    categories = categories,
                                    onSetLockOnStartup = viewModel::setLockOnStartup,
                                    onSetLockSensitiveCategories = viewModel::setLockSensitiveCategories,
                                    onToggleSensitiveCategory = viewModel::toggleSensitiveCategory,
                                    onSetBackupPasscode = viewModel::setBackupPasscode,
                                    onLockNow = {
                                        if (securityPrefs.lockOnStartup) {
                                            viewModel.lockAllProtectedContent()
                                        } else {
                                            viewModel.lockSensitiveCategoriesNow()
                                        }
                                    },
                                    onVerifyBiometricsNow = {
                                        launchBiometricOrFallback(
                                            title = "Verify Biometrics",
                                            subtitle = "Confirm fingerprint, face unlock, or screen lock works on this device.",
                                            isStartup = false,
                                            onSuccess = {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("Biometric verification succeeded")
                                                }
                                            }
                                        )
                                    }
                                )
                            }

                            composable(ROUTE_COACH) {
                                AiCoachScreen(
                                    messages = coachMessages,
                                    isLoading = coachLoading,
                                    isLiveFirebaseConnected = viewModel.isLiveAiConnected,
                                    selectedModel = selectedAiModel,
                                    selectedRole = selectedAiRole,
                                    customSystemInstruction = customSystemInstruction,
                                    searchGroundingEnabled = searchGroundingEnabled,
                                    includeScheduleContext = includeScheduleContext,
                                    onSelectModel = viewModel::selectAiModel,
                                    onSelectRole = viewModel::selectAiRole,
                                    onUpdateSystemInstruction = viewModel::updateCustomSystemInstruction,
                                    onToggleSearchGrounding = viewModel::setSearchGroundingEnabled,
                                    onToggleIncludeScheduleContext = viewModel::setIncludeScheduleContext,
                                    onSendMessage = viewModel::sendCoachMessage,
                                    onClearChat = viewModel::clearCoachChat,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(ROUTE_PLACES) {
                                PlacesMapScreen(
                                    selectedDateKey = selectedDateKey,
                                    scheduleItems = scheduleItems,
                                    categories = categories,
                                    onCheckOverlap = viewModel::checkOverlaps,
                                    onFindNextSlot = viewModel::findNextAvailableSlot,
                                    onSaveActivity = viewModel::saveActivity,
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(ROUTE_WEEKLY_PERFORMANCE) {
                                WeeklyPerformanceScreen(
                                    selectedDateKey = selectedDateKey,
                                    weeklySummary = weeklyPerformance,
                                    onShiftWeek = viewModel::shiftSelectedDate,
                                    onJumpToCurrentWeek = viewModel::jumpToToday,
                                    onOpenDayTimeline = { dateKey ->
                                        viewModel.selectDate(dateKey)
                                        navController.navigate(AppDestination.TODAY.route) {
                                            popUpTo(AppDestination.TODAY.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }

                            composable(ROUTE_SMART_SUGGESTIONS) {
                                SmartSuggestionsScreen(
                                    selectedDateKey = selectedDateKey,
                                    plan = smartPlan,
                                    isLoading = smartPlanLoading,
                                    aiSummary = smartAiSummary,
                                    isLiveFirebaseConnected = viewModel.isLiveAiConnected,
                                    selectedIds = selectedSuggestionIds,
                                    onGeneratePlan = viewModel::generateSmartSuggestions,
                                    onToggleSelection = viewModel::toggleSuggestionSelection,
                                    onApplySelected = {
                                        viewModel.applySelectedSmartSuggestions { _ ->
                                            navController.popBackStack()
                                        }
                                    },
                                    onNavigateBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }

            if (securityPrefs.lockOnStartup && !appUnlockedThisSession) {
                StartupBiometricLockOverlay(
                    hasBackupPasscode = securityPrefs.hasBackupPasscode,
                    authErrorMessage = startupAuthError,
                    onRequestBiometricUnlock = {
                        launchBiometricOrFallback(
                            title = "Unlock Rhythm",
                            subtitle = "Authenticate to access your daily schedule and notes.",
                            isStartup = true,
                            onSuccess = { viewModel.markAppUnlocked() }
                        )
                    },
                    onVerifyBackupPasscode = viewModel::verifyBackupPasscode,
                    onPasscodeUnlocked = {
                        startupAuthError = null
                        viewModel.markAppUnlocked()
                    },
                    onOpenSecuritySettings = {
                        runCatching { context.startActivity(BiometricAuthHelper.buildEnrollIntent()) }
                    },
                    onRecoveryUnlockWithoutEnrollment = {
                        startupAuthError = null
                        viewModel.markAppUnlocked()
                    }
                )
            }

            pendingPasscodeAuth?.let { auth ->
                BackupPasscodeVerifyDialog(
                    title = auth.title,
                    subtitle = auth.subtitle,
                    onVerify = viewModel::verifyBackupPasscode,
                    onVerified = {
                        pendingPasscodeAuth = null
                        auth.onSuccess()
                    },
                    onDismiss = { pendingPasscodeAuth = null }
                )
            }
        }
    }
}

