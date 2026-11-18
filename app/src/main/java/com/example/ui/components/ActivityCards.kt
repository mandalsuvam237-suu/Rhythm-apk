package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ActivityPriority
import com.example.data.model.ActivityStatus
import com.example.data.model.ScheduledActivityUi
import com.example.domain.DateTimeUtils
import com.example.ui.theme.parseHexColor

@Composable
fun TimelineActivityCard(
    item: ScheduledActivityUi,
    isActiveNow: Boolean,
    isHighlighted: Boolean,
    nowMinuteOfDay: Int,
    compactMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleComplete: () -> Unit,
    onStartFocus: () -> Unit,
    onEdit: () -> Unit,
    onSkip: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    isLockedSensitive: Boolean = false,
    onUnlockSensitive: () -> Unit = {}
) {
    val act = item.activity
    val cat = item.category
    val accent = parseHexColor(cat.colorHex)
    val isCompleted = act.status == ActivityStatus.COMPLETED
    val isSkipped = act.status == ActivityStatus.SKIPPED
    var menuExpanded by remember { mutableStateOf(false) }

    val borderColor by animateColorAsState(
        targetValue = when {
            isActiveNow -> accent
            isHighlighted -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        },
        label = "card_border"
    )

    val containerColor = when {
        isActiveNow -> accent.copy(alpha = 0.11f)
        isSkipped -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.surface
    }

    val remainingLabel = remember(act.id, act.startMinute, act.durationMinutes, act.status, nowMinuteOfDay, item.isCarriedOverFromPrevDay) {
        when {
            isCompleted -> "Completed"
            isSkipped -> "Skipped"
            isActiveNow -> {
                val effectiveEnd = if (item.isCarriedOverFromPrevDay) act.endMinute - 1440 else act.endMinute
                val left = (effectiveEnd - nowMinuteOfDay).coerceAtLeast(1)
                "${DateTimeUtils.formatDuration(left)} left"
            }
            act.startMinute > nowMinuteOfDay && act.dateKey == DateTimeUtils.todayKey() -> {
                val until = act.startMinute - nowMinuteOfDay
                "In ${DateTimeUtils.formatDuration(until)}"
            }
            else -> DateTimeUtils.formatDuration(act.durationMinutes)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("activity_card_${act.id}")
            .alpha(if (isSkipped) 0.65f else 1f)
            .clickable(onClick = if (isLockedSensitive) onUnlockSensitive else onEdit),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(if (isActiveNow || isHighlighted) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActiveNow) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = if (compactMode) 10.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color accent stripe + Category Icon
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (compactMode) 42.dp else 54.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLockedSensitive) Icons.Rounded.Lock else CategoryVisuals.iconFor(cat.iconKey),
                    contentDescription = if (isLockedSensitive) "Protected ${cat.name}" else cat.name,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (item.isCarriedOverFromPrevDay) {
                            "00:00 – ${DateTimeUtils.formatMinuteOfDay(act.endMinute)} (Cont.)"
                        } else {
                            DateTimeUtils.formatTimeRange(act.startMinute, act.durationMinutes)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActiveNow) accent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isActiveNow) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = accent
                        ) {
                            Text(
                                text = "NOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (!isLockedSensitive && act.isRepeating) {
                        Icon(
                            imageVector = Icons.Rounded.Repeat,
                            contentDescription = "Repeating",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (isLockedSensitive) "Protected ${cat.name} Note" else act.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (!isLockedSensitive && (isCompleted || isSkipped)) TextDecoration.LineThrough else TextDecoration.None,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = if (isLockedSensitive) Modifier.testTag("locked_activity_title_${act.id}") else Modifier
                )

                if (isLockedSensitive) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Biometric unlock required",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (!compactMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${cat.name} · $remainingLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (act.priority == ActivityPriority.HIGH) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "HIGH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (act.locationName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Place,
                                contentDescription = "Location",
                                tint = accent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = act.locationName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (isLockedSensitive) {
                FilledTonalButton(
                    onClick = onUnlockSensitive,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("unlock_activity_btn_${act.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Unlock ${cat.name}",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Unlock")
                }
            } else if (!item.isCarriedOverFromPrevDay) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isCompleted && !isSkipped) {
                        IconButton(
                            onClick = onStartFocus,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("focus_activity_btn_${act.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Start Focus Timer",
                                tint = accent
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) accent else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable(onClick = onToggleComplete)
                            .testTag("complete_activity_btn_${act.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Rounded.Check else Icons.Rounded.Check,
                            contentDescription = if (isCompleted) "Mark planned" else "Mark completed",
                            tint = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            modifier = Modifier
                                .padding(8.dp)
                                .size(18.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("activity_menu_btn_${act.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                }
                            )
                            if (canMoveUp) {
                                DropdownMenuItem(
                                    text = { Text("Swap Earlier") },
                                    leadingIcon = { Icon(Icons.Rounded.ArrowUpward, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onMoveUp()
                                    }
                                )
                            }
                            if (canMoveDown) {
                                DropdownMenuItem(
                                    text = { Text("Swap Later") },
                                    leadingIcon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onMoveDown()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (isSkipped) "Restore to Planned" else "Skip Activity") },
                                leadingIcon = {
                                    Icon(
                                        if (isSkipped) Icons.Rounded.Undo else Icons.Rounded.FastForward,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSkip()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
