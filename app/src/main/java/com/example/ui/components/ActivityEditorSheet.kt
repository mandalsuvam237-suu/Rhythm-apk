package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SeedData
import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.ActivityPriority
import com.example.data.model.ScheduledActivityEntity
import com.example.domain.DateTimeUtils
import com.example.domain.OverlapConflict
import com.example.ui.theme.parseHexColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActivityEditorSheet(
    existingActivity: ScheduledActivityEntity?,
    dateKey: String,
    categories: List<ActivityCategoryEntity>,
    onCheckOverlap: suspend (String?, String, Int, Int) -> List<OverlapConflict>,
    onFindNextSlot: suspend (String, Int, Int) -> Int,
    onDismiss: () -> Unit,
    initialLocationName: String = "",
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    initialCategoryId: String? = null,
    onSave: (
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
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(existingActivity?.title ?: "") }
    var notes by remember { mutableStateOf(existingActivity?.notes ?: "") }
    var selectedCatId by remember {
        mutableStateOf(
            existingActivity?.categoryId
                ?: initialCategoryId
                ?: categories.firstOrNull()?.id
                ?: "cat_deep_work"
        )
    }
    var locationName by remember {
        mutableStateOf(existingActivity?.locationName?.takeIf { it.isNotBlank() } ?: initialLocationName)
    }
    var latitude by remember { mutableStateOf(existingActivity?.latitude ?: initialLatitude) }
    var longitude by remember { mutableStateOf(existingActivity?.longitude ?: initialLongitude) }
    var startMinute by remember {
        mutableIntStateOf(existingActivity?.startMinute ?: ((DateTimeUtils.currentMinuteOfDay() / 15 + 1) * 15).coerceAtMost(23 * 60))
    }
    var durationMinutes by remember { mutableIntStateOf(existingActivity?.durationMinutes ?: 60) }
    var priority by remember { mutableStateOf(existingActivity?.priority ?: ActivityPriority.MEDIUM) }
    var isRepeating by remember { mutableStateOf(existingActivity?.isRepeating ?: false) }
    var repeatMask by remember { mutableIntStateOf(existingActivity?.repeatDaysMask ?: 0b0011111) }
    var reminderBefore by remember { mutableIntStateOf(existingActivity?.reminderBeforeMinutes ?: 10) }

    var overlaps by remember { mutableStateOf<List<OverlapConflict>>(emptyList()) }
    var allowOverlapOverride by remember { mutableStateOf(false) }

    LaunchedEffect(startMinute, durationMinutes, dateKey) {
        overlaps = onCheckOverlap(existingActivity?.id, dateKey, startMinute, durationMinutes)
        allowOverlapOverride = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (existingActivity == null) "Schedule Activity" else "Edit Activity",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Activity Title") },
                placeholder = { Text("e.g., Deep Work Sprint") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_title_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            // Location / Focus Place Picker
            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text("Location (optional)") },
                placeholder = { Text("e.g., Central Library or pick a spot") },
                leadingIcon = { Icon(Icons.Rounded.Place, contentDescription = null) },
                trailingIcon = {
                    if (locationName.isNotBlank() || latitude != null) {
                        IconButton(onClick = {
                            locationName = ""
                            latitude = null
                            longitude = null
                        }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear location")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activity_location_input")
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SeedData.defaultFocusPlaces().forEach { place ->
                    val selected = locationName == place.name
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) {
                                locationName = ""
                                latitude = null
                                longitude = null
                            } else {
                                locationName = place.name
                                latitude = place.latitude
                                longitude = place.longitude
                            }
                        },
                        label = { Text(place.name) }
                    )
                }
            }

            // Category picker
            Text("Category", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { cat ->
                    val catColor = parseHexColor(cat.colorHex)
                    val selected = cat.id == selectedCatId
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) catColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .testTag("editor_cat_${cat.id}")
                            .then(
                                if (selected) Modifier.border(1.5.dp, catColor, RoundedCornerShape(14.dp))
                                else Modifier
                            )
                            .clickable { selectedCatId = cat.id }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = CategoryVisuals.iconFor(cat.iconKey),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = cat.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Start time & Duration controls
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Start Time", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = DateTimeUtils.formatMinuteOfDay(startMinute),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { startMinute = (startMinute - 15).coerceAtLeast(0) },
                                modifier = Modifier.testTag("start_minus_15")
                            ) {
                                Icon(Icons.Rounded.Remove, contentDescription = "-15m")
                            }
                            TextButton(onClick = { startMinute = (startMinute + 60) % 1440 }) {
                                Text("+1h")
                            }
                            IconButton(
                                onClick = { startMinute = (startMinute + 15).coerceAtMost(1435) },
                                modifier = Modifier.testTag("start_plus_15")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "+15m")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Duration", style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = DateTimeUtils.formatDuration(durationMinutes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { durationMinutes = (durationMinutes - 15).coerceAtLeast(5) }) {
                                Icon(Icons.Rounded.Remove, contentDescription = "-15m duration")
                            }
                            IconButton(onClick = { durationMinutes = (durationMinutes + 15).coerceAtMost(720) }) {
                                Icon(Icons.Rounded.Add, contentDescription = "+15m duration")
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 45, 60, 90, 120, 450).forEach { mins ->
                            FilterChip(
                                selected = durationMinutes == mins,
                                onClick = { durationMinutes = mins },
                                label = { Text(DateTimeUtils.formatDuration(mins)) }
                            )
                        }
                    }

                    Text(
                        text = "Scheduled window: ${DateTimeUtils.formatTimeRange(startMinute, durationMinutes)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (startMinute + durationMinutes > 1440) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Overlap conflict warning
            if (overlaps.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overlap_warning_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = "Conflict",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Schedule Conflict Detected",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = "Overlaps with: " + overlaps.joinToString { it.conflictingActivity.title },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        startMinute = onFindNextSlot(dateKey, startMinute, durationMinutes)
                                    }
                                },
                                modifier = Modifier.testTag("find_next_slot_button")
                            ) {
                                Text("Find Next Free Slot")
                            }
                            TextButton(onClick = { allowOverlapOverride = true }) {
                                Text("Keep Anyway")
                            }
                        }
                    }
                }
            }

            // Priority
            Text("Priority", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityPriority.entries.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(p.label) }
                    )
                }
            }

            // Reminder offset
            Text("Reminder Before Start", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "At start", 5 to "5m before", 10 to "10m before", 15 to "15m before", 30 to "30m before").forEach { (mins, label) ->
                    FilterChip(
                        selected = reminderBefore == mins,
                        onClick = { reminderBefore = mins },
                        label = { Text(label) }
                    )
                }
            }

            // Repeat schedule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Repeat on Selected Days", style = MaterialTheme.typography.labelLarge)
                    Text("Auto-populates upcoming week", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = isRepeating, onCheckedChange = { isRepeating = it })
            }

            if (isRepeating) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("M", "T", "W", "T", "F", "S", "S")
                    days.forEachIndexed { idx, letter ->
                        val bit = 1 shl idx
                        val active = (repeatMask and bit) != 0
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { repeatMask = repeatMask xor bit },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val canSave = title.isNotBlank() && (overlaps.isEmpty() || allowOverlapOverride)
            Button(
                onClick = {
                    onSave(
                        existingActivity?.id,
                        dateKey,
                        title,
                        notes,
                        selectedCatId,
                        startMinute,
                        durationMinutes,
                        priority,
                        isRepeating,
                        repeatMask,
                        reminderBefore,
                        locationName,
                        latitude,
                        longitude
                    )
                    onDismiss()
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_activity_button")
            ) {
                Text(if (existingActivity == null) "Add to Schedule" else "Save Changes")
            }
        }
    }
}
