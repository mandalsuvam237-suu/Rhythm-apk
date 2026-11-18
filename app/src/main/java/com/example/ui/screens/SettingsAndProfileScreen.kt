package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.local.SecurityPreferences
import com.example.data.local.VisualPreferences
import com.example.data.model.ActivityCategoryEntity
import com.example.data.model.FontChoice
import com.example.data.model.NotificationPreferenceEntity
import com.example.data.model.ThemePalette
import com.example.data.model.UserProfileEntity
import com.example.domain.DateTimeUtils
import com.example.notifications.NotificationHelper
import com.example.security.BiometricAuthHelper
import com.example.security.BiometricCapability
import com.example.ui.components.SetBackupPasscodeDialog
import com.example.ui.theme.previewSwatchesForPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAndProfileScreen(
    visualPreferences: VisualPreferences,
    userProfile: UserProfileEntity?,
    notificationPref: NotificationPreferenceEntity?,
    onSelectTheme: (ThemePalette) -> Unit,
    onSelectFont: (FontChoice) -> Unit,
    onSetCompactCards: (Boolean) -> Unit,
    onSaveNotificationPref: (NotificationPreferenceEntity) -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onRegisterOrLogin: (String, String, String) -> Unit,
    onRecoverPassword: (String, String) -> Unit,
    onLogout: () -> Unit,
    onToggleDemoMode: (Boolean) -> Unit,
    onToggleLeaderboardOptIn: (Boolean) -> Unit,
    onSyncNow: () -> Unit,
    onExportDataJson: suspend () -> String,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier,
    securityPreferences: SecurityPreferences = SecurityPreferences(),
    categories: List<ActivityCategoryEntity> = emptyList(),
    onSetLockOnStartup: (Boolean) -> Unit = {},
    onSetLockSensitiveCategories: (Boolean) -> Unit = {},
    onToggleSensitiveCategory: (String) -> Unit = {},
    onSetBackupPasscode: (String) -> Unit = {},
    onLockNow: () -> Unit = {},
    onVerifyBiometricsNow: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pref = notificationPref ?: NotificationPreferenceEntity()
    val biometricCapability = remember { BiometricAuthHelper.checkCapability(context) }

    var hasNotifPerm by remember {
        mutableStateOf(NotificationHelper.hasNotificationPermission(context))
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotifPerm = granted
    }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showPasscodeSetupDialog by remember { mutableStateOf(false) }
    var exportedPreview by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_lazy_column"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Customize & Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // Theme & Font Customization
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Theme Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemePalette.entries.forEach { palette ->
                            val selected = visualPreferences.themePalette == palette
                            val swatches = previewSwatchesForPalette(palette)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onSelectTheme(palette) }
                                    .testTag("theme_chip_${palette.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                                        swatches.forEach { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(14.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                        }
                                    }
                                    Text(palette.displayName, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }

                    Text("Typography", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FontChoice.entries.forEach { font ->
                            FilterChip(
                                selected = visualPreferences.fontChoice == font,
                                onClick = { onSelectFont(font) },
                                label = { Text(font.displayName) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Compact Timeline Cards", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Fit more activities on screen", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = visualPreferences.compactCards,
                            onCheckedChange = onSetCompactCards
                        )
                    }
                }
            }
        }

        // Notifications
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Notifications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(onClick = {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }) {
                                Text("Allow")
                            }
                        }
                    }

                    PrefToggleRow("Before Activity Starts", pref.notifyBeforeEnabled) {
                        onSaveNotificationPref(pref.copy(notifyBeforeEnabled = it))
                    }
                    if (pref.notifyBeforeEnabled) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(5, 10, 15, 30).forEach { mins ->
                                FilterChip(
                                    selected = pref.minutesBefore == mins,
                                    onClick = { onSaveNotificationPref(pref.copy(minutesBefore = mins)) },
                                    label = { Text("${mins}m before") }
                                )
                            }
                        }
                    }
                    PrefToggleRow("When Activity Begins", pref.notifyOnStart) {
                        onSaveNotificationPref(pref.copy(notifyOnStart = it))
                    }
                    PrefToggleRow("Overdue Activity Alert", pref.notifyOnOverdue) {
                        onSaveNotificationPref(pref.copy(notifyOnOverdue = it))
                    }
                    PrefToggleRow("Focus Timer Completion", pref.notifyOnFocusEnd) {
                        onSaveNotificationPref(pref.copy(notifyOnFocusEnd = it))
                    }
                    PrefToggleRow("Daily Review Reminder", pref.dailyReviewEnabled) {
                        onSaveNotificationPref(pref.copy(dailyReviewEnabled = it))
                    }
                    if (pref.dailyReviewEnabled) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Review time: ${DateTimeUtils.formatMinuteOfDay(pref.dailyReviewMinute)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row {
                                TextButton(onClick = {
                                    onSaveNotificationPref(pref.copy(dailyReviewMinute = (pref.dailyReviewMinute - 30).coerceAtLeast(18 * 60)))
                                }) { Text("-30m") }
                                TextButton(onClick = {
                                    onSaveNotificationPref(pref.copy(dailyReviewMinute = (pref.dailyReviewMinute + 30).coerceAtMost(23 * 60 + 30)))
                                }) { Text("+30m") }
                            }
                        }
                    }
                }
            }
        }

        // Security & Biometrics
        item(key = "security_biometrics") {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("security_biometrics_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Security & Biometrics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (biometricCapability == BiometricCapability.READY) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Fingerprint,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(biometricCapability.label, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = biometricCapability.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (biometricCapability != BiometricCapability.READY) {
                                TextButton(onClick = {
                                    runCatching { context.startActivity(BiometricAuthHelper.buildEnrollIntent()) }
                                }) { Text("Enroll") }
                            } else {
                                TextButton(onClick = onVerifyBiometricsNow) { Text("Test") }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Lock Rhythm on Startup", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Require fingerprint, face unlock, or passcode on launch", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = securityPreferences.lockOnStartup,
                            onCheckedChange = onSetLockOnStartup,
                            modifier = Modifier.testTag("lock_on_startup_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Protect Sensitive Categories", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text("Mask private category notes until verified", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = securityPreferences.lockSensitiveCategories,
                            onCheckedChange = onSetLockSensitiveCategories,
                            modifier = Modifier.testTag("lock_sensitive_categories_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showPasscodeSetupDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("set_backup_passcode_btn")
                        ) {
                            Icon(Icons.Rounded.Pin, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (securityPreferences.hasBackupPasscode) "Update Passcode" else "Backup Passcode")
                        }
                        OutlinedButton(
                            onClick = onLockNow,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lock_now_btn")
                        ) {
                            Icon(Icons.Rounded.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lock Now")
                        }
                    }
                }
            }
        }

        if (securityPreferences.lockSensitiveCategories && categories.isNotEmpty()) {
            item(key = "security_protected_categories") {
                var showCategoryPicker by remember { mutableStateOf(false) }
                val protectedNames = remember(categories, securityPreferences.sensitiveCategoryIds) {
                    categories.filter { it.id in securityPreferences.sensitiveCategoryIds }.map { it.name }
                }
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showCategoryPicker = !showCategoryPicker }
                                .testTag("toggle_protected_categories_btn"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Protected Categories (${protectedNames.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (protectedNames.isEmpty()) "None selected" else protectedNames.joinToString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (showCategoryPicker) "Hide" else "Edit",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (showCategoryPicker) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                categories.forEach { cat ->
                                    val selected = cat.id in securityPreferences.sensitiveCategoryIds
                                    FilterChip(
                                        selected = selected,
                                        onClick = { onToggleSensitiveCategory(cat.id) },
                                        label = { Text(cat.name) },
                                        leadingIcon = if (selected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Lock,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.testTag("sensitive_cat_chip_${cat.id}")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Profile, Account, Cloud Sync & Data Export
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Account & Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userProfile?.avatarInitials ?: "LU",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = userProfile?.displayName ?: "Guest",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (userProfile?.isLoggedIn == true) userProfile.email else "Signed out (Local mode)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(onClick = { showProfileDialog = true }) {
                                Text("Edit")
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showAuthDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (userProfile?.isLoggedIn == true) "Account / Password" else "Sign In / Register")
                        }
                        if (userProfile?.isLoggedIn == true) {
                            TextButton(onClick = onLogout) {
                                Text("Sign Out")
                            }
                        }
                    }

                    // Demo Mode Toggle
                    PrefToggleRow(
                        label = "Demo Mode (Sample History)",
                        checked = userProfile?.isDemoMode == true,
                        onCheckedChange = onToggleDemoMode
                    )

                    PrefToggleRow(
                        label = "Show me on Leaderboard",
                        checked = userProfile?.leaderboardOptIn ?: true,
                        onCheckedChange = onToggleLeaderboardOptIn
                    )

                    // Local-first backup/sync status
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Offline-First SQLite Storage", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    text = if (visualPreferences.lastSyncEpochMs > 0L) {
                                        "Last snapshot: ${DateTimeUtils.formatTimestampShort(visualPreferences.lastSyncEpochMs)}"
                                    } else {
                                        "All schedules, stats & timers saved locally."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = onSyncNow) {
                                Icon(Icons.Rounded.Sync, contentDescription = "Verify local snapshot")
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                scope.launch {
                                    val json = onExportDataJson()
                                    exportedPreview = json
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/json"
                                        putExtra(Intent.EXTRA_SUBJECT, "Rhythm Backup")
                                        putExtra(Intent.EXTRA_TEXT, json)
                                    }
                                    runCatching {
                                        context.startActivity(Intent.createChooser(shareIntent, "Export Rhythm Data"))
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON")
                        }
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset Data")
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        var name by remember { mutableStateOf(userProfile?.displayName ?: "") }
        var email by remember { mutableStateOf(userProfile?.email ?: "") }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display Name") })
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) onUpdateProfile(name, email)
                    showProfileDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAuthDialog) {
        var mode by remember { mutableIntStateOf(0) } // 0 = Register/Login, 1 = Recover
        var name by remember { mutableStateOf(userProfile?.displayName ?: "") }
        var email by remember { mutableStateOf(userProfile?.email ?: "") }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text(if (mode == 0) "Local Account Login / Register" else "Recover Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (mode == 0) {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    }
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") })
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(if (mode == 0) "Password" else "New Password") },
                        visualTransformation = PasswordVisualTransformation()
                    )
                    TextButton(onClick = { mode = if (mode == 0) 1 else 0 }) {
                        Text(if (mode == 0) "Forgot password? Recover" else "Back to Login / Register")
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (mode == 0) onRegisterOrLogin(name, email, password)
                    else onRecoverPassword(email, password)
                    showAuthDialog = false
                }) { Text(if (mode == 0) "Confirm" else "Update Password") }
            },
            dismissButton = { TextButton(onClick = { showAuthDialog = false }) { Text("Cancel") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account & Reset Data?") },
            text = { Text("This permanently deletes all local schedules, focus logs, XP, and custom templates.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAccount()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Everything") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }

    exportedPreview?.let { json ->
        AlertDialog(
            onDismissRequest = { exportedPreview = null },
            title = { Text("Exported JSON Snapshot") },
            text = {
                Text(
                    text = json.take(650) + if (json.length > 650) "\n..." else "",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            confirmButton = {
                Button(onClick = { exportedPreview = null }) { Text("Done") }
            }
        )
    }

    if (showPasscodeSetupDialog) {
        SetBackupPasscodeDialog(
            hasExistingPasscode = securityPreferences.hasBackupPasscode,
            onSavePasscode = onSetBackupPasscode,
            onClearPasscode = { onSetBackupPasscode("") },
            onDismiss = { showPasscodeSetupDialog = false }
        )
    }
}

@Composable
private fun PrefToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
