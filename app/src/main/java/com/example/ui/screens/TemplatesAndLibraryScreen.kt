package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.SecurityPreferences
import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.ActivityPriority
import com.example.data.model.HabitEntity
import com.example.data.model.ScheduleTemplateEntity
import com.example.data.model.TemplateActivityEntity
import com.example.domain.DateTimeUtils
import com.example.ui.components.CategoryVisuals
import com.example.ui.theme.parseHexColor
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplatesAndLibraryScreen(
    selectedDateKey: String,
    templates: List<ScheduleTemplateEntity>,
    templateActivities: List<TemplateActivityEntity>,
    categories: List<ActivityCategoryEntity>,
    habits: List<HabitEntity>,
    onApplyTemplateToDate: (String, String) -> Unit,
    onDuplicateTemplate: (String) -> Unit,
    onDeleteTemplate: (String) -> Unit,
    onSaveTemplate: (String?, String, String, String, List<TemplateActivityEntity>) -> Unit,
    onSaveCategory: (String?, String, String, String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onSaveHabit: (String, String, Int) -> Unit,
    onToggleHabitToday: (HabitEntity) -> Unit,
    onDeleteHabit: (String) -> Unit,
    modifier: Modifier = Modifier,
    securityPrefs: SecurityPreferences = SecurityPreferences(),
    unlockedSensitiveCategoryIds: Set<String> = emptySet(),
    onSetCategorySensitive: (String, Boolean) -> Unit = { _, _ -> },
    onRequestUnlockCategory: (categoryId: String, categoryName: String, onUnlocked: () -> Unit) -> Unit = { _, _, onUnlocked -> onUnlocked() }
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var applyDialogTemplate by remember { mutableStateOf<ScheduleTemplateEntity?>(null) }
    var editingTemplate by remember { mutableStateOf<ScheduleTemplateEntity?>(null) }
    var creatingTemplate by remember { mutableStateOf(false) }

    var showCategoryDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<ActivityCategoryEntity?>(null) }
    var showHabitDialog by remember { mutableStateOf(false) }

    val activitiesByTemplate = remember(templateActivities) {
        templateActivities.groupBy { it.templateId }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Templates & Library",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Templates") }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Library & Habits") }
            }
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Button(
                        onClick = {
                            editingTemplate = null
                            creatingTemplate = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_template_btn")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Custom Template")
                    }
                }

                items(templates, key = { it.id }) { tpl ->
                    val items = activitiesByTemplate[tpl.id].orEmpty()
                    val accent = parseHexColor(tpl.accentColorHex)
                    val totalMins = items.sumOf { it.durationMinutes }

                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
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
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(accent)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = tpl.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = accent.copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "${items.size} blocks · ${DateTimeUtils.formatDuration(totalMins)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = accent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = tpl.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (items.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items.take(4).forEach { act ->
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "${DateTimeUtils.formatMinuteOfDay(act.startMinute)} ${act.title}",
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { applyDialogTemplate = tpl },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("apply_template_${tpl.id}")
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Apply")
                                }
                                IconButton(onClick = { onDuplicateTemplate(tpl.id) }) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Duplicate template")
                                }
                                IconButton(onClick = {
                                    editingTemplate = tpl
                                    creatingTemplate = true
                                }) {
                                    Icon(Icons.Rounded.Edit, contentDescription = "Edit template")
                                }
                                if (!tpl.isDefault) {
                                    IconButton(onClick = { onDeleteTemplate(tpl.id) }) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Delete template",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
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
                        Text("Activity Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = {
                                editingCategory = null
                                showCategoryDialog = true
                            },
                            modifier = Modifier.testTag("add_custom_category_btn")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Custom")
                        }
                    }
                }

                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        categories.forEach { cat ->
                            val color = parseHexColor(cat.colorHex)
                            val isSensitive = securityPrefs.lockSensitiveCategories && cat.id in securityPrefs.sensitiveCategoryIds
                            val isLocked = isSensitive && cat.id !in unlockedSensitiveCategoryIds
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .testTag("cat_chip_${cat.id}")
                                    .clickable {
                                        if (isLocked) {
                                            onRequestUnlockCategory(cat.id, cat.name) {
                                                editingCategory = cat
                                                showCategoryDialog = true
                                            }
                                        } else {
                                            editingCategory = cat
                                            showCategoryDialog = true
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(color.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = CategoryVisuals.iconFor(cat.iconKey),
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(cat.name, style = MaterialTheme.typography.labelLarge)
                                    if (isSensitive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = if (isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                                            contentDescription = if (isLocked) "Protected sensitive category" else "Sensitive category unlocked",
                                            tint = if (isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Daily Habits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        OutlinedButton(onClick = { showHabitDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Habit")
                        }
                    }
                }

                if (habits.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Add recurring daily habits to build consistency streaks alongside your timeline.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(habits, key = { it.id }) { habit ->
                        val todayKey = DateTimeUtils.todayKey()
                        val doneToday = habit.lastCompletedDateKey == todayKey
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(habit.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Streak: ${habit.currentStreak}d (Best ${habit.bestStreak}d) · Target ${habit.targetDaysPerWeek}x/wk",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = { onToggleHabitToday(habit) }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Check in habit",
                                        tint = if (doneToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteHabit(habit.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete habit", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Apply Template Non-Destructive Date Picker Dialog
    applyDialogTemplate?.let { tpl ->
        val today = DateTimeUtils.todayKey()
        val tomorrow = DateTimeUtils.shiftDateKey(today, 1)
        AlertDialog(
            onDismissRequest = { applyDialogTemplate = null },
            title = { Text("Apply \"${tpl.name}\"") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Existing activities on the chosen date will be preserved. Conflicting blocks automatically shift to the next free slot.")
                    Button(
                        onClick = {
                            onApplyTemplateToDate(tpl.id, selectedDateKey)
                            applyDialogTemplate = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Selected Date (${DateTimeUtils.formatMonthDay(selectedDateKey)})")
                    }
                    OutlinedButton(
                        onClick = {
                            onApplyTemplateToDate(tpl.id, today)
                            applyDialogTemplate = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Today (${DateTimeUtils.formatMonthDay(today)})")
                    }
                    OutlinedButton(
                        onClick = {
                            onApplyTemplateToDate(tpl.id, tomorrow)
                            applyDialogTemplate = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Tomorrow (${DateTimeUtils.formatMonthDay(tomorrow)})")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { applyDialogTemplate = null }) { Text("Cancel") }
            }
        )
    }

    // Create / Edit Template Dialog
    if (creatingTemplate) {
        val existingItems = editingTemplate?.let { activitiesByTemplate[it.id].orEmpty() }.orEmpty()
        TemplateEditorDialog(
            existing = editingTemplate,
            existingItems = existingItems,
            categories = categories,
            onDismiss = {
                creatingTemplate = false
                editingTemplate = null
            },
            onSave = { name, desc, colorHex, items ->
                onSaveTemplate(editingTemplate?.id, name, desc, colorHex, items)
                creatingTemplate = false
                editingTemplate = null
            }
        )
    }

    // Category Create / Edit Dialog
    if (showCategoryDialog) {
        val currentCat = editingCategory
        CategoryEditorDialog(
            existing = currentCat,
            initialSensitive = currentCat != null && currentCat.id in securityPrefs.sensitiveCategoryIds,
            onDismiss = { showCategoryDialog = false },
            onSave = { name, iconKey, colorHex, isSensitive ->
                onSaveCategory(currentCat?.id, name, iconKey, colorHex)
                if (currentCat != null) {
                    onSetCategorySensitive(currentCat.id, isSensitive)
                }
                showCategoryDialog = false
            },
            onDelete = if (currentCat != null && !currentCat.isDefault) {
                {
                    onDeleteCategory(currentCat.id)
                    showCategoryDialog = false
                }
            } else null
        )
    }

    // New Habit Dialog
    if (showHabitDialog) {
        var habitTitle by remember { mutableStateOf("") }
        var habitTarget by remember { mutableIntStateOf(5) }
        AlertDialog(
            onDismissRequest = { showHabitDialog = false },
            title = { Text("Create Habit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = habitTitle,
                        onValueChange = { habitTitle = it },
                        label = { Text("Habit Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Target days/week: $habitTarget")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(3, 5, 7).forEach { target ->
                            OutlinedButton(onClick = { habitTarget = target }) { Text("${target}x") }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (habitTitle.isNotBlank()) {
                            onSaveHabit(habitTitle, categories.firstOrNull()?.id ?: "cat_other", habitTarget)
                            showHabitDialog = false
                        }
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showHabitDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    existing: ActivityCategoryEntity?,
    initialSensitive: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var iconKey by remember { mutableStateOf(existing?.iconKey ?: "bolt") }
    var colorHex by remember { mutableStateOf(existing?.colorHex ?: CategoryVisuals.presetColors.first()) }
    var isSensitive by remember { mutableStateOf(initialSensitive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Custom Activity Category" else "Edit ${existing.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (existing != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sensitive Category (Biometric Lock)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text("Mask activity notes on timeline until unlocked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isSensitive,
                            onCheckedChange = { isSensitive = it }
                        )
                    }
                }
                Text("Color", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryVisuals.presetColors.forEach { hex ->
                        val c = parseHexColor(hex)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (hex == colorHex) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryVisuals.availableIcons.forEach { (key, vec) ->
                        val selected = key == iconKey
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) parseHexColor(colorHex).copy(alpha = 0.22f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { iconKey = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(vec, contentDescription = key, tint = if (selected) parseHexColor(colorHex) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name, iconKey, colorHex, isSensitive) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun TemplateEditorDialog(
    existing: ScheduleTemplateEntity?,
    existingItems: List<TemplateActivityEntity>,
    categories: List<ActivityCategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, List<TemplateActivityEntity>) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var colorHex by remember { mutableStateOf(existing?.accentColorHex ?: "#FF6B4A") }
    val items = remember {
        mutableStateListOf<TemplateActivityEntity>().apply {
            if (existingItems.isNotEmpty()) {
                addAll(existingItems)
            } else {
                add(
                    TemplateActivityEntity(
                        id = UUID.randomUUID().toString(),
                        templateId = "",
                        title = "Focus Sprint",
                        categoryId = categories.firstOrNull { it.id == "cat_deep_work" }?.id ?: "cat_deep_work",
                        startMinute = 9 * 60,
                        durationMinutes = 60,
                        priority = ActivityPriority.HIGH
                    )
                )
            }
        }
    }
    var newBlockTitle by remember { mutableStateOf("") }
    var newBlockStartHour by remember { mutableIntStateOf(10) }
    var newBlockDuration by remember { mutableIntStateOf(45) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Template" else "Edit Template") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Template Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Blocks (${items.size})", style = MaterialTheme.typography.labelLarge)
                items.forEachIndexed { idx, block ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${DateTimeUtils.formatMinuteOfDay(block.startMinute)} · ${block.title} (${block.durationMinutes}m)",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { items.removeAt(idx) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove block")
                        }
                    }
                }
                OutlinedTextField(
                    value = newBlockTitle,
                    onValueChange = { newBlockTitle = it },
                    label = { Text("Add Block Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { newBlockStartHour = (newBlockStartHour + 1) % 24 }) {
                        Text("Start ${String.format("%02d:00", newBlockStartHour)}")
                    }
                    TextButton(onClick = { newBlockDuration = if (newBlockDuration >= 90) 30 else newBlockDuration + 15 }) {
                        Text("${newBlockDuration}m")
                    }
                    OutlinedButton(onClick = {
                        if (newBlockTitle.isNotBlank()) {
                            items.add(
                                TemplateActivityEntity(
                                    id = UUID.randomUUID().toString(),
                                    templateId = existing?.id ?: "",
                                    title = newBlockTitle.trim(),
                                    categoryId = categories.firstOrNull()?.id ?: "cat_other",
                                    startMinute = newBlockStartHour * 60,
                                    durationMinutes = newBlockDuration
                                )
                            )
                            newBlockTitle = ""
                        }
                    }) {
                        Text("+ Add")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && items.isNotEmpty()) {
                        onSave(name, desc.ifBlank { "Custom schedule routine" }, colorHex, items.toList())
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
