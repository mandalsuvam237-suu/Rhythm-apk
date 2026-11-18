package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Upcoming
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SecurityPreferences
import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.ActivityPriority
import com.example.data.model.ActivityStatus
import com.example.data.model.ScheduledActivityEntity
import com.example.data.model.ScheduledActivityUi
import com.example.data.model.UserProfileEntity
import com.example.domain.DateTimeUtils
import com.example.domain.OverlapConflict
import com.example.domain.PerformanceBreakdown
import com.example.domain.ScheduleEngine
import com.example.ui.components.ActivityEditorSheet
import com.example.ui.components.PerformanceScoreRing
import com.example.ui.components.TimelineActivityCard
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.TimelineFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    selectedDateKey: String,
    scheduleItems: List<ScheduledActivityUi>,
    weekActivityCounts: Map<String, Int>,
    timelineFilter: TimelineFilter,
    categories: List<ActivityCategoryEntity>,
    performance: PerformanceBreakdown,
    userProfile: UserProfileEntity?,
    highlightedActivityId: String?,
    compactCards: Boolean,
    tickerEpochMs: Long,
    onSelectDate: (String) -> Unit,
    onShiftDate: (Int) -> Unit,
    onJumpToToday: () -> Unit,
    onSelectDateUtcMillis: (Long) -> Unit,
    onSetTimelineFilter: (TimelineFilter) -> Unit,
    onCheckOverlap: suspend (String?, String, Int, Int) -> List<OverlapConflict>,
    onFindNextSlot: suspend (String, Int, Int) -> Int,
    onSaveActivity: (
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
        locationName: String,
        latitude: Double?,
        longitude: Double?
    ) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onSetStatus: (String, ActivityStatus) -> Unit,
    onSwapActivities: (String, String) -> Unit,
    onStartFocus: (ScheduledActivityEntity) -> Unit,
    onRescheduleUnfinished: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToCoach: () -> Unit,
    onNavigateToPlacesMap: () -> Unit,
    modifier: Modifier = Modifier,
    securityPrefs: SecurityPreferences = SecurityPreferences(),
    unlockedSensitiveCategoryIds: Set<String> = emptySet(),
    onRequestUnlockCategory: (categoryId: String, categoryName: String, onUnlocked: () -> Unit) -> Unit = { _, _, onUnlocked -> onUnlocked() },
    onLockSensitiveNow: () -> Unit = {},
    onNavigateToWeeklyPerformance: () -> Unit = {},
    onNavigateToSmartSuggestions: () -> Unit = {}
) {
    fun isCategoryLocked(categoryId: String): Boolean {
        return securityPrefs.lockSensitiveCategories &&
            categoryId in securityPrefs.sensitiveCategoryIds &&
            categoryId !in unlockedSensitiveCategoryIds
    }
    var showEditorFor by remember { mutableStateOf<ScheduledActivityEntity?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val todayKey = DateTimeUtils.todayKey()
    val prevKey = DateTimeUtils.shiftDateKey(selectedDateKey, -1)
    val nowMinute = remember(tickerEpochMs) { DateTimeUtils.currentMinuteOfDay() }

    val activeNowItem = remember(scheduleItems, selectedDateKey, nowMinute) {
        if (selectedDateKey != todayKey) null
        else scheduleItems.firstOrNull {
            ScheduleEngine.isActivityActiveNow(it.activity, todayKey, prevKey, nowMinute)
        }
    }

    val upcomingItem = remember(scheduleItems, selectedDateKey, nowMinute, activeNowItem) {
        scheduleItems.firstOrNull {
            !it.isCarriedOverFromPrevDay &&
                it.activity.id != activeNowItem?.activity?.id &&
                (it.activity.status == ActivityStatus.PLANNED || it.activity.status == ActivityStatus.IN_PROGRESS) &&
                (selectedDateKey != todayKey || it.activity.startMinute >= nowMinute)
        }
    }

    val unfinishedCount = remember(scheduleItems) {
        scheduleItems.count {
            !it.isCarriedOverFromPrevDay &&
                (it.activity.status == ActivityStatus.PLANNED || it.activity.status == ActivityStatus.IN_PROGRESS)
        }
    }

    val filteredTimelineItems = remember(scheduleItems, timelineFilter) {
        when (timelineFilter) {
            TimelineFilter.ALL -> scheduleItems
            TimelineFilter.REMAINING -> scheduleItems.filter {
                it.activity.status == ActivityStatus.PLANNED || it.activity.status == ActivityStatus.IN_PROGRESS
            }
            TimelineFilter.COMPLETED -> scheduleItems.filter {
                it.activity.status == ActivityStatus.COMPLETED
            }
        }
    }

    // Index in filteredTimelineItems after which the current local time marker should sit
    val nowMarkerInsertIndex = remember(filteredTimelineItems, selectedDateKey, todayKey, nowMinute, activeNowItem) {
        if (selectedDateKey != todayKey || activeNowItem != null || filteredTimelineItems.isEmpty()) {
            -1
        } else {
            val firstLater = filteredTimelineItems.indexOfFirst { item ->
                val effectiveStart = if (item.isCarriedOverFromPrevDay) 0 else item.activity.startMinute
                effectiveStart > nowMinute
            }
            if (firstLater == -1) filteredTimelineItems.size else firstLater
        }
    }

    val weekStripKeys = remember(selectedDateKey) {
        (-3..3).map { DateTimeUtils.shiftDateKey(selectedDateKey, it) }
    }

    val sameDayItems = remember(scheduleItems) {
        scheduleItems.filter { !it.isCarriedOverFromPrevDay }
    }

    val totalPlannedMinutes = remember(sameDayItems) {
        sameDayItems.filter { it.activity.status != ActivityStatus.SKIPPED }
            .sumOf { it.activity.durationMinutes }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("daily_timeline_lazy_column"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Header: Clickable Date Picker + Level/Streak + Today Shortcut
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDatePicker = true }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .testTag("date_picker_trigger")
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = DateTimeUtils.formatDisplayDate(selectedDateKey),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("selected_date_header")
                                )
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = "Choose date",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                if (userProfile?.isDemoMode == true) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "DEMO",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = when (selectedDateKey) {
                                    todayKey -> "Today's Timeline · ${DateTimeUtils.formatDuration(totalPlannedMinutes)} planned"
                                    DateTimeUtils.shiftDateKey(todayKey, 1) -> "Tomorrow's Plan · ${DateTimeUtils.formatDuration(totalPlannedMinutes)} planned"
                                    DateTimeUtils.shiftDateKey(todayKey, -1) -> "Yesterday's Log · ${DateTimeUtils.formatDuration(totalPlannedMinutes)} planned"
                                    else -> "Daily Schedule · ${DateTimeUtils.formatDuration(totalPlannedMinutes)} planned"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (selectedDateKey != todayKey) {
                            FilledTonalButton(
                                onClick = onJumpToToday,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("jump_to_today_btn")
                            ) {
                                Text("Today", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${userProfile?.currentStreak ?: 0}d · Lv.${userProfile?.level ?: 1}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Interactive 7-Day Date Navigation Strip
            item(key = "date_strip") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onShiftDate(-1) },
                        modifier = Modifier.testTag("prev_day_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous day")
                    }
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(weekStripKeys, key = { it }) { dayKey ->
                            val isSelected = dayKey == selectedDateKey
                            val isToday = dayKey == todayKey
                            val hasActivities = (weekActivityCounts[dayKey] ?: 0) > 0
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                    else -> MaterialTheme.colorScheme.surface
                                },
                                tonalElevation = if (isSelected) 4.dp else 1.dp,
                                modifier = Modifier
                                    .width(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectDate(dayKey) }
                                    .testTag("date_chip_$dayKey")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = DateTimeUtils.formatShortDay(dayKey).take(2),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = DateTimeUtils.formatDayNumber(dayKey),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    !hasActivities -> Color.Transparent
                                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                                    else -> MaterialTheme.colorScheme.primary
                                                }
                                            )
                                    )
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = { onShiftDate(1) },
                        modifier = Modifier.testTag("next_day_btn")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next day")
                    }
                }
            }

            // Daily Performance & Completion Dashboard Card
            item(key = "performance_card") {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PerformanceScoreRing(score = performance.score)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Completion: ${performance.completionPercent}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (performance.completionPercent / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(999.dp))
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${performance.completedCount}/${performance.totalCount} blocks done · ${performance.onTimePercent}% on time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Live Current Activity Spotlight or Upcoming Banner
            if (activeNowItem != null) {
                item(key = "active_now_spotlight") {
                    val act = activeNowItem.activity
                    val accent = parseHexColor(activeNowItem.category.colorHex)
                    val endMinute = if (activeNowItem.isCarriedOverFromPrevDay) act.endMinute - 1440 else act.endMinute
                    val minsLeft = (endMinute - nowMinute).coerceAtLeast(1)
                    val isSpotlightLocked = isCategoryLocked(act.categoryId)

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.06f))
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(shape = RoundedCornerShape(999.dp), color = accent) {
                                        Text(
                                            text = "ACTIVE NOW · ${DateTimeUtils.formatDuration(minsLeft)} LEFT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = DateTimeUtils.formatTimeRange(act.startMinute, act.durationMinutes),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = if (isSpotlightLocked) "Protected ${activeNowItem.category.name} Note" else act.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                if (isSpotlightLocked) {
                                    Button(
                                        onClick = {
                                            onRequestUnlockCategory(act.categoryId, activeNowItem.category.name) {}
                                        },
                                        modifier = Modifier.testTag("hero_unlock_btn")
                                    ) {
                                        Icon(Icons.Rounded.Lock, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Unlock ${activeNowItem.category.name}")
                                    }
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(
                                            onClick = { onStartFocus(act) },
                                            modifier = Modifier.testTag("hero_start_focus_btn")
                                        ) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Focus Timer")
                                        }
                                        FilledTonalButton(
                                            onClick = { onSetStatus(act.id, ActivityStatus.COMPLETED) }
                                        ) {
                                            Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Complete")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (upcomingItem != null) {
                item(key = "upcoming_spotlight") {
                    val isUpcomingLocked = isCategoryLocked(upcomingItem.activity.categoryId)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isUpcomingLocked) Icons.Rounded.Lock else Icons.Rounded.Upcoming,
                                contentDescription = if (isUpcomingLocked) "Protected" else "Upcoming",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Up Next · ${DateTimeUtils.formatMinuteOfDay(upcomingItem.activity.startMinute)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = if (isUpcomingLocked) "Protected ${upcomingItem.category.name} Note" else upcomingItem.activity.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (isUpcomingLocked) {
                                TextButton(
                                    onClick = {
                                        onRequestUnlockCategory(upcomingItem.activity.categoryId, upcomingItem.category.name) {}
                                    },
                                    modifier = Modifier.testTag("upcoming_unlock_btn")
                                ) {
                                    Text("Unlock")
                                }
                            } else {
                                TextButton(onClick = { onStartFocus(upcomingItem.activity) }) {
                                    Text("Focus")
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions + Timeline Filter Chips
            item(key = "quick_actions_and_filters") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onNavigateToCoach,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("coach_entry_btn")
                        ) {
                            Icon(Icons.Rounded.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AI Coach", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        FilledTonalButton(
                            onClick = onNavigateToPlacesMap,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("places_map_entry_btn")
                        ) {
                            Icon(Icons.Rounded.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Places Map", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToTemplates,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Templates")
                        }
                        AnimatedVisibility(visible = unfinishedCount > 0, modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = onRescheduleUnfinished,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reschedule_unfinished_btn")
                            ) {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Carry Over ($unfinishedCount)")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onNavigateToWeeklyPerformance,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("weekly_performance_entry_btn")
                        ) {
                            Icon(Icons.Rounded.AutoGraph, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Weekly Chart", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        FilledTonalButton(
                            onClick = onNavigateToSmartSuggestions,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("smart_suggestions_entry_btn")
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Smart Plan", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }

                    if (securityPrefs.lockSensitiveCategories && unlockedSensitiveCategoryIds.isNotEmpty()) {
                        FilterChip(
                            selected = true,
                            onClick = onLockSensitiveNow,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.LockOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text("Sensitive Unlocked · Re-lock") },
                            modifier = Modifier.testTag("relock_sensitive_btn")
                        )
                    }

                    if (scheduleItems.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TimelineFilter.entries.forEach { filter ->
                                FilterChip(
                                    selected = timelineFilter == filter,
                                    onClick = { onSetTimelineFilter(filter) },
                                    label = { Text(filter.label) },
                                    modifier = Modifier.testTag("timeline_filter_${filter.name}")
                                )
                            }
                        }
                    }
                }
            }

            // Empty State or Visual Scrollable Timeline
            if (filteredTimelineItems.isEmpty()) {
                item(key = "empty_state") {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_empty_timeline),
                                contentDescription = "Empty schedule illustration",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )
                            Text(
                                text = if (scheduleItems.isEmpty()) "Your timeline is wide open" else "No matching activities",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (scheduleItems.isEmpty()) {
                                    "Add your first activity block or apply a curated routine template to get started."
                                } else {
                                    "Try switching the timeline filter to view all scheduled activities."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (scheduleItems.isEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(onClick = {
                                        showEditorFor = null
                                        isCreatingNew = true
                                    }) {
                                        Icon(Icons.Rounded.Add, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Add Activity")
                                    }
                                    OutlinedButton(onClick = onNavigateToTemplates) {
                                        Text("Browse Templates")
                                    }
                                }
                            } else {
                                OutlinedButton(onClick = { onSetTimelineFilter(TimelineFilter.ALL) }) {
                                    Text("Show All")
                                }
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = filteredTimelineItems,
                    key = { _, ui -> "${ui.isCarriedOverFromPrevDay}_${ui.activity.id}" }
                ) { index, ui ->
                    if (index == nowMarkerInsertIndex) {
                        CurrentTimeDivider(nowMinute = nowMinute)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    val act = ui.activity
                    val sameDayIndex = sameDayItems.indexOfFirst { it.activity.id == act.id }
                    val isNow = activeNowItem?.activity?.id == act.id &&
                        activeNowItem.isCarriedOverFromPrevDay == ui.isCarriedOverFromPrevDay
                    val accent = parseHexColor(ui.category.colorHex)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Timeline Rail (Hour label + Vertical Connector + Node Dot)
                        TimelineRailColumn(
                            timeLabel = if (ui.isCarriedOverFromPrevDay) "00:00" else DateTimeUtils.formatMinuteOfDay(act.startMinute),
                            accentColor = accent,
                            isCompleted = act.status == ActivityStatus.COMPLETED,
                            isSkipped = act.status == ActivityStatus.SKIPPED,
                            isActiveNow = isNow,
                            isLast = index == filteredTimelineItems.lastIndex
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        val isCardLocked = isCategoryLocked(act.categoryId)
                        TimelineActivityCard(
                            item = ui,
                            isActiveNow = isNow,
                            isHighlighted = highlightedActivityId == act.id,
                            nowMinuteOfDay = nowMinute,
                            compactMode = compactCards,
                            canMoveUp = !ui.isCarriedOverFromPrevDay && sameDayIndex > 0,
                            canMoveDown = !ui.isCarriedOverFromPrevDay && sameDayIndex in 0 until sameDayItems.lastIndex,
                            onToggleComplete = {
                                val nextStatus = if (act.status == ActivityStatus.COMPLETED) {
                                    ActivityStatus.PLANNED
                                } else {
                                    ActivityStatus.COMPLETED
                                }
                                onSetStatus(act.id, nextStatus)
                            },
                            onStartFocus = { onStartFocus(act) },
                            onEdit = {
                                if (!ui.isCarriedOverFromPrevDay) {
                                    showEditorFor = act
                                    isCreatingNew = false
                                }
                            },
                            onSkip = {
                                val nextStatus = if (act.status == ActivityStatus.SKIPPED) {
                                    ActivityStatus.PLANNED
                                } else {
                                    ActivityStatus.SKIPPED
                                }
                                onSetStatus(act.id, nextStatus)
                            },
                            onMoveUp = {
                                if (sameDayIndex > 0) {
                                    onSwapActivities(act.id, sameDayItems[sameDayIndex - 1].activity.id)
                                }
                            },
                            onMoveDown = {
                                if (sameDayIndex in 0 until sameDayItems.lastIndex) {
                                    onSwapActivities(act.id, sameDayItems[sameDayIndex + 1].activity.id)
                                }
                            },
                            onDelete = { onDeleteActivity(act.id) },
                            modifier = Modifier.weight(1f),
                            isLockedSensitive = isCardLocked,
                            onUnlockSensitive = {
                                onRequestUnlockCategory(act.categoryId, ui.category.name) {}
                            }
                        )
                    }
                }

                if (nowMarkerInsertIndex == filteredTimelineItems.size) {
                    item(key = "now_marker_end") {
                        CurrentTimeDivider(nowMinute = nowMinute)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                showEditorFor = null
                isCreatingNew = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("quick_add_activity_fab")
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Quick Add Activity")
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(selectedDateKey) {
            DateTimeUtils.toEpochMillis(selectedDateKey, 12 * 60)
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onSelectDateUtcMillis(it) }
                        showDatePicker = false
                    }
                ) { Text("Select") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (isCreatingNew || showEditorFor != null) {
        ActivityEditorSheet(
            existingActivity = showEditorFor,
            dateKey = selectedDateKey,
            categories = categories,
            onCheckOverlap = onCheckOverlap,
            onFindNextSlot = onFindNextSlot,
            onDismiss = {
                isCreatingNew = false
                showEditorFor = null
            },
            onSave = onSaveActivity
        )
    }
}

@Composable
private fun TimelineRailColumn(
    timeLabel: String,
    accentColor: Color,
    isCompleted: Boolean,
    isSkipped: Boolean,
    isActiveNow: Boolean,
    isLast: Boolean
) {
    val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    Column(
        modifier = Modifier
            .width(50.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActiveNow) FontWeight.Bold else FontWeight.Medium,
            color = if (isActiveNow) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(if (isActiveNow) 14.dp else 11.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isActiveNow || isCompleted -> accentColor
                        isSkipped -> MaterialTheme.colorScheme.outline
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
                .then(
                    if (!isActiveNow && !isCompleted && !isSkipped) {
                        Modifier.border(2.dp, accentColor, CircleShape)
                    } else {
                        Modifier
                    }
                )
        )
        if (!isLast) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f, fill = true)
                    .padding(top = 4.dp)
                    .background(lineColor)
            )
        }
    }
}

@Composable
private fun CurrentTimeDivider(nowMinute: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag("current_time_divider"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Text(
                text = "NOW ${DateTimeUtils.formatMinuteOfDay(nowMinute)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        )
    }
}
