package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.security.BiometricAuthHelper
import com.example.security.BiometricCapability

@Composable
fun StartupBiometricLockOverlay(
    hasBackupPasscode: Boolean,
    authErrorMessage: String?,
    onRequestBiometricUnlock: () -> Unit,
    onVerifyBackupPasscode: (String) -> Boolean,
    onPasscodeUnlocked: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    onRecoveryUnlockWithoutEnrollment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val capability = remember { BiometricAuthHelper.checkCapability(context) }
    var showPasscodeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (capability == BiometricCapability.READY) {
            onRequestBiometricUnlock()
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag("startup_lock_overlay")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (capability == BiometricCapability.READY) Icons.Rounded.Fingerprint else Icons.Rounded.Lock,
                            contentDescription = "Rhythm locked",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Text(
                        text = "Rhythm is Locked",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = when (capability) {
                            BiometricCapability.READY -> "Verify with fingerprint, face unlock, or device credential to view your schedules and notes."
                            else -> capability.detail
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (!authErrorMessage.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = authErrorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (capability == BiometricCapability.READY) {
                        Button(
                            onClick = onRequestBiometricUnlock,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("unlock_biometric_btn")
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Unlock with Biometrics")
                        }
                    }

                    if (hasBackupPasscode) {
                        OutlinedButton(
                            onClick = { showPasscodeDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("unlock_backup_passcode_btn")
                        ) {
                            Icon(Icons.Rounded.Pin, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Backup Passcode")
                        }
                    }

                    if (capability != BiometricCapability.READY) {
                        OutlinedButton(
                            onClick = onOpenSecuritySettings,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Settings, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enroll in Device Settings")
                        }

                        if (!hasBackupPasscode) {
                            TextButton(
                                onClick = onRecoveryUnlockWithoutEnrollment,
                                modifier = Modifier.testTag("recovery_unlock_btn")
                            ) {
                                Text("Continue Without Enrolled Biometrics")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPasscodeDialog) {
        BackupPasscodeVerifyDialog(
            title = "Unlock Rhythm",
            subtitle = "Enter your backup passcode.",
            onVerify = onVerifyBackupPasscode,
            onVerified = {
                showPasscodeDialog = false
                onPasscodeUnlocked()
            },
            onDismiss = { showPasscodeDialog = false }
        )
    }
}

@Composable
fun BackupPasscodeVerifyDialog(
    title: String,
    subtitle: String,
    onVerify: (String) -> Boolean,
    onVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = passcode,
                    onValueChange = {
                        passcode = it
                        errorMsg = null
                    },
                    label = { Text("Backup Passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = errorMsg != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_passcode_input")
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (onVerify(passcode)) {
                        onVerified()
                    } else {
                        errorMsg = "Incorrect passcode. Please try again."
                    }
                },
                modifier = Modifier.testTag("verify_passcode_confirm_btn")
            ) { Text("Unlock") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SetBackupPasscodeDialog(
    hasExistingPasscode: Boolean,
    onSavePasscode: (String) -> Unit,
    onClearPasscode: () -> Unit,
    onDismiss: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (hasExistingPasscode) "Update Backup Passcode" else "Set Backup Passcode") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Used if biometric sensors are unavailable or when authenticating sensitive categories without device biometrics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = passcode,
                    onValueChange = {
                        passcode = it
                        errorMsg = null
                    },
                    label = { Text("Enter Passcode (4+ digits)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_passcode_input")
                )
                OutlinedTextField(
                    value = confirmPasscode,
                    onValueChange = {
                        confirmPasscode = it
                        errorMsg = null
                    },
                    label = { Text("Confirm Passcode") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_passcode_input")
                )
                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = passcode.trim()
                    when {
                        clean.length < 4 -> errorMsg = "Passcode must be at least 4 characters."
                        clean != confirmPasscode.trim() -> errorMsg = "Passcodes do not match."
                        else -> {
                            onSavePasscode(clean)
                            onDismiss()
                        }
                    }
                },
                modifier = Modifier.testTag("save_passcode_btn")
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (hasExistingPasscode) {
                    TextButton(onClick = {
                        onClearPasscode()
                        onDismiss()
                    }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
