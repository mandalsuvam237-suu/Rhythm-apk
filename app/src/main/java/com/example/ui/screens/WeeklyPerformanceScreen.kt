package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.DateTimeUtils
import com.example.domain.WeeklyPerformanceSummary
import com.example.ui.components.PerformanceScoreRing
import com.example.ui.components.WeeklyChartStyle
import com.example.ui.components.WeeklyCompletionChart

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPerformanceScreen(
    selectedDateKey: String,
    weeklySummary: WeeklyPerformanceSummary,
    onShiftWeek: (Int) -> Unit,
    onJumpToCurrentWeek: () -> Unit,
    onOpenDayTimeline: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var chartStyle by remember { mutableStateOf(WeeklyChartStyle.BAR) }
    var selectedDayIndex by remember(weeklySummary.endDateKey) {
        mutableIntStateOf((weeklySummary.days.size - 1).coerceAtLeast(0))
    }
    val todayKey = DateTimeUtils.todayKey()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("weekly_performance_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Weekly Performance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${DateTimeUtils.formatMonthDay(weeklySummary.startDateKey)} – ${DateTimeUtils.formatMonthDay(weeklySummary.endDateKey)} (Last 7 Days)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("weekly_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (selectedDateKey != todayKey) {
                        TextButton(
                            onClick = onJumpToCurrentWeek,
                            modifier = Modifier.testTag("weekly_today_btn")
                        ) {
                            Text("This Week")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("weekly_performance_lazy_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 7-Day Window Selector
            item(key = "week_nav") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onShiftWeek(-7) },
                            modifier = Modifier.testTag("weekly_prev_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous 7 days")
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${DateTimeUtils.formatMonthDay(weeklySummary.startDateKey)} – ${DateTimeUtils.formatMonthDay(weeklySummary.endDateKey)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Rolling 7-Day Completion Window",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onShiftWeek(7) },
                            modifier = Modifier.testTag("weekly_next_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next 7 days")
                        }
                    }
                }
            }

            // Hero Summary Card
            item(key = "hero_summary") {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PerformanceScoreRing(
                            score = weeklySummary.averageCompletionRatePercent,
                            size = 84.dp,
                            strokeWidth = 8.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${weeklySummary.averageCompletionRatePercent}% Weekly Completion",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${weeklySummary.totalCompleted} of ${weeklySummary.totalPlanned} scheduled activities finished",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val delta = weeklySummary.deltaVsPreviousWeekPercent
                                val deltaColor = if (delta >= 0) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = deltaColor.copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (delta >= 0) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown,
                                            contentDescription = null,
                                            tint = deltaColor,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (delta > 0) "+$delta% vs prior 7d" else "$delta% vs prior 7d",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = deltaColor
                                        )
                                    }
                                }
                                weeklySummary.bestDay?.let { best ->
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.EmojiEvents,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Best: ${best.dayLabel} (${best.completionRatePercent}%)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Completion Rate Visualization Card (Compose Canvas Interactive Chart)
            item(key = "completion_chart_card") {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("weekly_chart_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoGraph,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Completion Rate (Last 7 Days)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Native Compose Canvas chart · Tap any day to inspect",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = chartStyle == WeeklyChartStyle.BAR,
                                onClick = { chartStyle = WeeklyChartStyle.BAR },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                                icon = { Icon(Icons.Rounded.BarChart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("weekly_chart_mode_bar")
                            ) {
                                Text("Bar Chart")
                            }
                            SegmentedButton(
                                selected = chartStyle == WeeklyChartStyle.AREA,
                                onClick = { chartStyle = WeeklyChartStyle.AREA },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                                icon = { Icon(Icons.Rounded.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.testTag("weekly_chart_mode_area")
                            ) {
                                Text("Trend Area")
                            }
                        }

                        WeeklyCompletionChart(
                            days = weeklySummary.days,
                            selectedIndex = selectedDayIndex,
                            onSelectIndex = { selectedDayIndex = it },
                            chartStyle = chartStyle
                        )
                    }
                }
            }

            // Day-by-Day Breakdown Header
            item(key = "breakdown_header") {
                Text(
                    text = "Day-by-Day Breakdown",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (weeklySummary.totalPlanned == 0) {
                item(key = "weekly_empty_tip") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "No activities scheduled in this 7-day window yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Add tasks to your Today timeline or enable Demo Mode in Profile to visualize a populated 7-day trend.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            itemsIndexed(weeklySummary.days, key = { _, day -> day.dateKey }) { idx, day ->
                val isSelected = idx == selectedDayIndex
                val accent = when {
                    day.totalActivities == 0 -> MaterialTheme.colorScheme.outline
                    day.completionRatePercent >= 80 -> MaterialTheme.colorScheme.secondary
                    day.completionRatePercent >= 50 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.tertiary
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { selectedDayIndex = idx }
                        .testTag("weekly_day_row_${day.dateKey}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${day.dayLabel}, ${day.dateLabel}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (day.dateKey == todayKey) {
                                        FilterChip(
                                            selected = true,
                                            onClick = { onOpenDayTimeline(day.dateKey) },
                                            label = { Text("Today") }
                                        )
                                    }
                                }
                                Text(
                                    text = if (day.totalActivities == 0) {
                                        "No blocks scheduled"
                                    } else {
                                        "${day.completedActivities}/${day.totalActivities} done · ${DateTimeUtils.formatDuration(day.actualMinutes)} logged · Score ${day.performanceScore}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = accent.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "${day.completionRatePercent}%",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                TextButton(onClick = { onOpenDayTimeline(day.dateKey) }) {
                                    Text("Open")
                                }
                            }
                        }

                        LinearProgressIndicator(
                            progress = { (day.completionRatePercent / 100f).coerceIn(0f, 1f) },
                            color = accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(999.dp))
                        )
                    }
                }
            }
        }
    }
}
