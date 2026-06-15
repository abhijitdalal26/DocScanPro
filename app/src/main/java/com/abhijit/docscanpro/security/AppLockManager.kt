package com.abhijit.docscanpro.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Manages app-level and document-level locking.
 * Supports biometric (fingerprint/face) and PIN fallback.
 */
class AppLockManager(private val context: Context) {

    enum class BiometricAvailability {
        AVAILABLE,
        NO_HARDWARE,
        NOT_ENROLLED,
        UNAVAILABLE
    }

    fun checkBiometricAvailability(): BiometricAvailability {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    /**
     * Shows a biometric prompt. Call from a FragmentActivity (MainActivity in our case).
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Unlock DocScan Pro",
        subtitle: String = "Use biometric to unlock",
        onSuccess: () -> Unit,
        onFailed: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationFailed() {
                onFailed()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onError(errString.toString())
            }
        }

        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    companion object {
        // Session-level lock state (cleared on app kill)
        private var isUnlocked = false
        private var lastUnlockTime = 0L

        fun setUnlocked(unlocked: Boolean) {
            isUnlocked = unlocked
            if (unlocked) lastUnlockTime = System.currentTimeMillis()
        }

        fun isSessionUnlocked(timeoutMinutes: Int): Boolean {
            if (!isUnlocked) return false
            if (timeoutMinutes == 0) return true // 0 = never auto-lock
            val elapsed = System.currentTimeMillis() - lastUnlockTime
            return elapsed < timeoutMinutes * 60_000L
        }

        fun lock() {
            isUnlocked = false
            lastUnlockTime = 0L
        }
    }
}
