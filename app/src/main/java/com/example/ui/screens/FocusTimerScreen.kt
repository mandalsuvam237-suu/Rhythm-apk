package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.ActivityStatus
import com.example.data.model.ScheduledActivityEntity
import com.example.data.model.ScheduledActivityUi
import com.example.data.model.TimerSessionEntity
import com.example.domain.DateTimeUtils
import com.example.domain.ScheduleEngine
import com.example.ui.components.CategoryVisuals
import com.example.ui.theme.parseHexColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FocusTimerScreen(
    activeTimer: TimerSessionEntity?,
    recentSessions: List<TimerSessionEntity>,
    todayActivities: List<ScheduledActivityUi>,
    categories: List<ActivityCategoryEntity>,
    tickerEpochMs: Long,
    onStartActivityTimer: (ScheduledActivityEntity) -> Unit,
    onStartQuickTimer: (String, String, Int) -> Unit,
    onPauseTimer: (String) -> Unit,
    onResumeTimer: (String) -> Unit,
    onStopTimer: (String, Boolean) -> Unit,
    onResetTimer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showStopDialog by remember { mutableStateOf(false) }
    var quickTitle by remember { mutableStateOf("Deep Focus Sprint") }
    var quickCategoryId by remember { mutableStateOf(categories.firstOrNull { it.id == "cat_deep_work" }?.id ?: categories.firstOrNull()?.id ?: "cat_deep_work") }
    var quickMinutes by remember { mutableIntStateOf(25) }

    val elapsedSeconds = remember(activeTimer, tickerEpochMs) {
        activeTimer?.let { ScheduleEngine.computeElapsedSeconds(it, tickerEpochMs) } ?: 0L
    }
    val overtimeSeconds = remember(activeTimer, elapsedSeconds) {
        activeTimer?.let { (elapsedSeconds - it.plannedSeconds).coerceAtLeast(0L) } ?: 0L
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Focus Mode",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Timestamp-backed timer survives backgrounding & restarts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (activeTimer != null) {
            item {
                val plannedSec = activeTimer.plannedSeconds.coerceAtLeast(1L)
                val progress = (elapsedSeconds.toFloat() / plannedSec.toFloat()).coerceIn(0f, 1f)
                val isOvertime = overtimeSeconds > 0L
                val remaining = (plannedSec - elapsedSeconds).coerceAtLeast(0L)
                val ringColor = if (isOvertime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                val trackColor = MaterialTheme.colorScheme.surfaceVariant

                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isOvertime) MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                            else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = when {
                                    isOvertime -> "OVERTIME +${DateTimeUtils.formatSecondsAsClock(overtimeSeconds)}"
                                    activeTimer.isRunning -> "IN FOCUS"
                                    else -> "PAUSED"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOvertime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }

                        Text(
                            text = activeTimer.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier.size(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val stroke = 14.dp.toPx()
                                drawArc(
                                    color = trackColor,
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = ringColor,
                                    startAngle = -90f,
                                    sweepAngle = if (isOvertime) 360f else progress * 360f,
                                    useCenter = false,
                                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (isOvertime) {
                                        "+${DateTimeUtils.formatSecondsAsClock(overtimeSeconds)}"
                                    } else {
                                        DateTimeUtils.formatSecondsAsClock(remaining)
                                    },
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOvertime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("timer_clock_readout")
                                )
                                Text(
                                    text = "Elapsed ${DateTimeUtils.formatSecondsAsClock(elapsedSeconds)} / ${DateTimeUtils.formatSecondsAsClock(plannedSec)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (activeTimer.isRunning) {
                                Button(
                                    onClick = { onPauseTimer(activeTimer.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("timer_pause_btn")
                                ) {
                                    Icon(Icons.Rounded.Pause, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pause")
                                }
                            } else {
                                Button(
                                    onClick = { onResumeTimer(activeTimer.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("timer_resume_btn")
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Resume")
                                }
                            }

                            OutlinedButton(
                                onClick = { showStopDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("timer_stop_btn")
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop")
                            }
                        }

                        TextButton(
                            onClick = { showResetDialog = true },
                            modifier = Modifier.testTag("timer_reset_btn")
                        ) {
                            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Timer")
                        }
                    }
                }
            }
        } else {
            // Quick Focus Launcher
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start Quick Focus Session",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = quickTitle,
                            onValueChange = { quickTitle = it },
                            label = { Text("Focus Goal") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(15, 25, 45, 60, 90).forEach { mins ->
                                FilterChip(
                                    selected = quickMinutes == mins,
                                    onClick = { quickMinutes = mins },
                                    label = { Text("${mins}m") }
                                )
                            }
                        }

                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.take(6).forEach { cat ->
                                FilterChip(
                                    selected = quickCategoryId == cat.id,
                                    onClick = { quickCategoryId = cat.id },
                                    label = { Text(cat.name) }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (quickTitle.isNotBlank()) {
                                    onStartQuickTimer(quickTitle, quickCategoryId, quickMinutes)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("start_quick_focus_btn")
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Start ${quickMinutes}m Focus")
                        }
                    }
                }
            }

            // Today's Activities ready for focus
            val readyActivities = todayActivities.filter {
                !it.isCarriedOverFromPrevDay && it.activity.status != ActivityStatus.COMPLETED && it.activity.status != ActivityStatus.SKIPPED
            }
            if (readyActivities.isNotEmpty()) {
                item {
                    Text(
                        text = "Launch From Today's Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(readyActivities, key = { it.activity.id }) { ui ->
                    val accent = parseHexColor(ui.category.colorHex)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onStartActivityTimer(ui.activity) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = CategoryVisuals.iconFor(ui.category.iconKey),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(ui.activity.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${DateTimeUtils.formatTimeRange(ui.activity.startMinute, ui.activity.durationMinutes)} · ${DateTimeUtils.formatDuration(ui.activity.durationMinutes)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Start", tint = accent)
                        }
                    }
                }
            }
        }

        // Session History
        val finished = recentSessions.filter { it.isFinished }
        if (finished.isNotEmpty()) {
            item {
                Text(
                    text = "Recent Focus Sessions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(finished.take(8), key = { it.id }) { sess ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sess.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${sess.dateKey} · Actual ${DateTimeUtils.formatSecondsAsClock(sess.accumulatedSeconds)} (Planned ${DateTimeUtils.formatSecondsAsClock(sess.plannedSeconds)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (sess.overtimeSeconds > 0) {
                            Text(
                                text = "+${DateTimeUtils.formatSecondsAsClock(sess.overtimeSeconds)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }

    if (showResetDialog && activeTimer != null) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Focus Timer?") },
            text = { Text("This discards the current elapsed session without logging time.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        onResetTimer(activeTimer.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showStopDialog && activeTimer != null) {
        AlertDialog(
            onDismissRequest = { showStopDialog = false },
            title = { Text("Finish Focus Session") },
            text = { Text("Save ${DateTimeUtils.formatSecondsAsClock(elapsedSeconds)} of tracked time. Mark linked activity completed?") },
            confirmButton = {
                Button(onClick = {
                    showStopDialog = false
                    onStopTimer(activeTimer.id, true)
                }) {
                    Text("Save & Complete")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        showStopDialog = false
                        onStopTimer(activeTimer.id, false)
                    }) {
                        Text("Save Only")
                    }
                    TextButton(onClick = { showStopDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}
