package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AchievementEntity
import com.example.data.model.DailyPerformanceEntity
import com.example.data.model.LeaderboardEntryEntity
import com.example.data.model.LeaderboardPeriod
import com.example.data.model.StatsRange
import com.example.data.model.UserProfileEntity
import com.example.data.model.XpTransactionEntity
import com.example.domain.DateTimeUtils
import com.example.domain.PerformanceBreakdown
import com.example.domain.ScheduleEngine
import com.example.domain.StatsSummary
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.PlannedVsActualBarChart

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsAndRankScreen(
    selectedDateKey: String,
    statsRange: StatsRange,
    customStartKey: String,
    customEndKey: String,
    statsSummary: StatsSummary,
    todayPerformance: PerformanceBreakdown,
    yesterdayPerformance: DailyPerformanceEntity?,
    userProfile: UserProfileEntity?,
    leaderboardPeriod: LeaderboardPeriod,
    leaderboard: List<LeaderboardEntryEntity>,
    achievements: List<AchievementEntity>,
    xpTransactions: List<XpTransactionEntity>,
    onShiftDate: (Int) -> Unit,
    onSelectStatsRange: (StatsRange) -> Unit,
    onSetCustomRange: (String, String) -> Unit,
    onSelectLeaderboardPeriod: (LeaderboardPeriod) -> Unit,
    onToggleLeaderboardOptIn: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToWeeklyPerformance: () -> Unit = {}
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var showCustomRangeDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Insights & Rank",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Statistics") }
                SegmentedButton(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Level & Rank") }
            }
        }

        if (activeTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onShiftDate(-1) }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous date")
                        }
                        Text(
                            text = if (statsRange == StatsRange.CUSTOM) {
                                "${DateTimeUtils.formatMonthDay(customStartKey)} – ${DateTimeUtils.formatMonthDay(customEndKey)}"
                            } else {
                                "Anchor: ${DateTimeUtils.formatMonthDay(selectedDateKey)}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { onShiftDate(1) }) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next date")
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatsRange.entries.forEach { range ->
                            FilterChip(
                                selected = statsRange == range,
                                onClick = {
                                    if (range == StatsRange.CUSTOM) {
                                        showCustomRangeDialog = true
                                    } else {
                                        onSelectStatsRange(range)
                                    }
                                },
                                label = { Text(range.label) },
                                modifier = Modifier.testTag("stats_range_${range.name}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateToWeeklyPerformance,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_weekly_performance_btn")
                    ) {
                        Icon(Icons.Rounded.AutoGraph, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Weekly Performance (7-Day Completion)")
                    }
                }

                // KPI Grid
                item {
                    val prevScore = yesterdayPerformance?.score ?: 0
                    val delta = todayPerformance.score - prevScore
                    val deltaText = when {
                        yesterdayPerformance == null -> "Today"
                        delta > 0 -> "+$delta vs yesterday"
                        delta < 0 -> "$delta vs yesterday"
                        else -> "Even vs yesterday"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StatMetricCard(
                                title = "Planned vs Actual",
                                value = DateTimeUtils.formatDuration(statsSummary.totalActualMinutes),
                                subtitle = "of ${DateTimeUtils.formatDuration(statsSummary.totalPlannedMinutes)} planned",
                                modifier = Modifier.weight(1f)
                            )
                            StatMetricCard(
                                title = "Completion",
                                value = "${statsSummary.completedCount} done",
                                subtitle = "${statsSummary.incompleteCount} incomplete",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            StatMetricCard(
                                title = "On-Time Rate",
                                value = "${statsSummary.onTimePercent}%",
                                subtitle = "Avg score ${statsSummary.averageScore}/100",
                                modifier = Modifier.weight(1f)
                            )
                            StatMetricCard(
                                title = "Today's Score",
                                value = "${todayPerformance.score}/100",
                                subtitle = deltaText,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Donut Chart
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Category Time Distribution",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            CategoryDonutChart(stats = statsSummary.categoryStats)
                        }
                    }
                }

                // Bar Graph
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Planned vs. Actual Time",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            PlannedVsActualBarChart(buckets = statsSummary.bucketStats)
                        }
                    }
                }

                // Transparent Performance Score Breakdown
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Transparent Score Formula (0–100)",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Missed activities never permanently lower future scores—each day resets cleanly while streaks reward consistency.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ScoreFactorRow("Task Completion", todayPerformance.completionPoints, 40)
                            ScoreFactorRow("On-Time Completion", todayPerformance.onTimePoints, 25)
                            ScoreFactorRow("Priority Tasks", todayPerformance.priorityPoints, 20)
                            ScoreFactorRow("Consistency Bonus", todayPerformance.consistencyPoints, 15)
                        }
                    }
                }
            }
        } else {
            val user = userProfile
            val level = user?.level ?: 1
            val currentXp = user?.totalXp ?: 0
            val floorXp = ScheduleEngine.xpThresholdForLevel(level)
            val nextXp = ScheduleEngine.xpThresholdForLevel(level + 1)
            val levelProgress = if (nextXp > floorXp) {
                ((currentXp - floorXp).toFloat() / (nextXp - floorXp).toFloat()).coerceIn(0f, 1f)
            } else 0f

            val sortedLeaderboard = remember(leaderboard, leaderboardPeriod) {
                leaderboard.sortedByDescending { entry ->
                    when (leaderboardPeriod) {
                        LeaderboardPeriod.DAILY -> entry.dailyXp
                        LeaderboardPeriod.WEEKLY -> entry.weeklyXp
                        LeaderboardPeriod.MONTHLY -> entry.monthlyXp
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Hero Level Banner
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Image(
                                painter = painterResource(id = R.drawable.img_hero_banner),
                                contentDescription = "Rhythm banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(175.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Black.copy(alpha = 0.25f), Color.Black.copy(alpha = 0.78f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "LEVEL $level · ${user?.displayName ?: "Player"}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "$currentXp XP total · ${nextXp - currentXp} XP to Level ${level + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                LinearProgressIndicator(
                                    progress = { levelProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Text(
                                        text = "Streak: ${user?.currentStreak ?: 0}d (Best ${user?.longestStreak ?: 0}d)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Focus: ${user?.totalFocusMinutes ?: 0}m",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Leaderboard
                item {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Leaderboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Join", style = MaterialTheme.typography.labelMedium)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        checked = user?.leaderboardOptIn ?: true,
                                        onCheckedChange = onToggleLeaderboardOptIn
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                LeaderboardPeriod.entries.forEach { p ->
                                    FilterChip(
                                        selected = leaderboardPeriod == p,
                                        onClick = { onSelectLeaderboardPeriod(p) },
                                        label = { Text(p.label) }
                                    )
                                }
                            }
                            sortedLeaderboard.forEachIndexed { idx, entry ->
                                val xpVal = when (leaderboardPeriod) {
                                    LeaderboardPeriod.DAILY -> entry.dailyXp
                                    LeaderboardPeriod.WEEKLY -> entry.weeklyXp
                                    LeaderboardPeriod.MONTHLY -> entry.monthlyXp
                                }
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (entry.isCurrentUser) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "#${idx + 1}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.width(32.dp)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(entry.avatarInitials, style = MaterialTheme.typography.labelLarge)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(entry.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                            Text("Lv.${entry.level} · ${entry.streak}d streak", style = MaterialTheme.typography.labelMedium)
                                        }
                                        Text(
                                            text = "$xpVal XP",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Achievements
                item {
                    Text(
                        text = "Achievements (${achievements.count { it.unlockedAt != null }}/${achievements.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(achievements, key = { it.id }) { ach ->
                    val unlocked = ach.unlockedAt != null
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (unlocked) 1f else 0.7f)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (unlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = if (unlocked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ach.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(ach.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("+${ach.xpReward} XP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                if (xpTransactions.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent XP Ledger",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(xpTransactions.take(8), key = { it.id }) { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(tx.reason, style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("+${tx.amount} XP", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (showCustomRangeDialog) {
        var daysBack by remember { mutableIntStateOf(14) }
        AlertDialog(
            onDismissRequest = { showCustomRangeDialog = false },
            title = { Text("Custom Date Range") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select window ending on ${DateTimeUtils.formatMonthDay(selectedDateKey)}:")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 7, 14, 30, 90).forEach { d ->
                            OutlinedButton(onClick = { daysBack = d }) { Text("${d}d") }
                        }
                    }
                    Text(
                        text = "${DateTimeUtils.formatMonthDay(DateTimeUtils.shiftDateKey(selectedDateKey, -(daysBack - 1)))} – ${DateTimeUtils.formatMonthDay(selectedDateKey)}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val start = DateTimeUtils.shiftDateKey(selectedDateKey, -(daysBack - 1))
                    onSetCustomRange(start, selectedDateKey)
                    showCustomRangeDialog = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRangeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ScoreFactorRow(label: String, points: Int, maxPoints: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$points / $maxPoints pts", style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(
            progress = { (points.toFloat() / maxPoints.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
        )
    }
}
