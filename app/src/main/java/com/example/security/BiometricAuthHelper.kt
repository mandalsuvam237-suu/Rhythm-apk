package com.example.security

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricCapability(val label: String, val detail: String) {
    READY(
        "Biometrics Ready",
        "Fingerprint, face unlock, or device credential is enrolled and ready."
    ),
    NONE_ENROLLED(
        "No Biometrics Enrolled",
        "No fingerprint, face unlock, or screen lock is configured on this device."
    ),
    UNAVAILABLE(
        "Biometrics Unavailable",
        "Biometric sensor hardware is temporarily unavailable."
    ),
    NO_HARDWARE(
        "No Biometric Sensor",
        "This device lacks biometric hardware. Use a backup passcode instead."
    ),
    SECURITY_UPDATE_REQUIRED(
        "Security Update Required",
        "A system security update is required before biometrics can be used."
    )
}

object BiometricAuthHelper {

    private const val BIOMETRIC_ONLY_AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK
    private const val DEVICE_OR_BIOMETRIC_AUTHENTICATORS =
        BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    @VisibleForTesting
    internal var testCapabilityOverride: BiometricCapability? = null

    fun checkCapability(context: Context): BiometricCapability {
        testCapabilityOverride?.let { return it }
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(DEVICE_OR_BIOMETRIC_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricCapability.READY
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricCapability.NONE_ENROLLED
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricCapability.UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricCapability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED ->
                BiometricCapability.SECURITY_UPDATE_REQUIRED
            else -> BiometricCapability.UNAVAILABLE
        }
    }

    tailrec fun findFragmentActivity(context: Context): FragmentActivity? = when (context) {
        is FragmentActivity -> context
        is ContextWrapper -> findFragmentActivity(context.baseContext)
        else -> null
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String? = null,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, message: String) -> Unit
    ) {
        val capability = checkCapability(activity)
        if (capability != BiometricCapability.READY) {
            onError(BiometricPrompt.ERROR_NO_BIOMETRICS, capability.detail)
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }
            }
        )

        // BiometricPrompt prohibits setNegativeButtonText when DEVICE_CREDENTIAL is included.
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setConfirmationRequired(false)

        if (!negativeButtonText.isNullOrBlank()) {
            builder
                .setAllowedAuthenticators(BIOMETRIC_ONLY_AUTHENTICATORS)
                .setNegativeButtonText(negativeButtonText)
        } else {
            builder.setAllowedAuthenticators(DEVICE_OR_BIOMETRIC_AUTHENTICATORS)
        }

        prompt.authenticate(builder.build())
    }

    fun buildEnrollIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, DEVICE_OR_BIOMETRIC_AUTHENTICATORS)
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
    }
}
