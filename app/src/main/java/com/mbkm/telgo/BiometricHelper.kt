package com.mbkm.telgo

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

class BiometricHelper(private val context: Context) {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // Check if the device supports biometric authentication
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED,
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED,
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED,
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> false
            else -> false
        }
    }

    // Check if fingerprint login is enabled for the current user
    fun isFingerprintEnabled(): Boolean {
        val preferences = context.getSharedPreferences("TelGoPrefs", Context.MODE_PRIVATE)
        val currentUserEmail = preferences.getString("userEmail", "") ?: ""

        if (currentUserEmail.isBlank()) return false

        val fingerprintPrefs = context.getSharedPreferences("FingerprintPrefs", Context.MODE_PRIVATE)
        return fingerprintPrefs.getBoolean("fingerprint_enabled_$currentUserEmail", false)
    }

    // Enable or disable fingerprint login for the current user
    fun setFingerprintEnabled(enabled: Boolean) {
        val preferences = context.getSharedPreferences("TelGoPrefs", Context.MODE_PRIVATE)
        val currentUserEmail = preferences.getString("userEmail", "") ?: ""

        if (currentUserEmail.isBlank()) return

        val fingerprintPrefs = context.getSharedPreferences("FingerprintPrefs", Context.MODE_PRIVATE)
        fingerprintPrefs.edit().putBoolean("fingerprint_enabled_$currentUserEmail", enabled).apply()

        // Store credentials securely if enabled (use Android KeyStore in production)
        if (enabled) {
            val userPassword = preferences.getString("securePassword", "") ?: ""
            fingerprintPrefs.edit().putString("password_$currentUserEmail", userPassword).apply()
        } else {
            fingerprintPrefs.edit().remove("password_$currentUserEmail").apply()
        }
    }

    // Get stored password for fingerprint authentication
    fun getStoredPassword(email: String): String? {
        val fingerprintPrefs = context.getSharedPreferences("FingerprintPrefs", Context.MODE_PRIVATE)
        return fingerprintPrefs.getString("password_$email", null)
    }

    // Check if fingerprint is enabled for a specific email (for login screen)
    fun isFingerprintEnabledForEmail(email: String): Boolean {
        val fingerprintPrefs = context.getSharedPreferences("FingerprintPrefs", Context.MODE_PRIVATE)
        return fingerprintPrefs.getBoolean("fingerprint_enabled_$email", false)
    }

    // Create and show biometric prompt for authentication
    fun showBiometricPrompt(
        activity: FragmentActivity,
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (Int, String) -> Unit,
        onFailed: () -> Unit
    ) {
        executor = ContextCompat.getMainExecutor(activity)

        biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess(result)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errorCode, errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login with Fingerprint")
            .setSubtitle("Place your finger on the sensor to verify your identity")
            .setNegativeButtonText("Cancel")
            .setConfirmationRequired(false)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // Store the email and encrypted password after successful login
    fun storeCredentialsAfterLogin(email: String, password: String) {
        val preferences = context.getSharedPreferences("TelGoPrefs", Context.MODE_PRIVATE)
        preferences.edit().putString("userEmail", email).apply()
        preferences.edit().putString("securePassword", password).apply()
    }
}