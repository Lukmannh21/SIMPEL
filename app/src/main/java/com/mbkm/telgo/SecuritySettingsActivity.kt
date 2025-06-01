package com.mbkm.telgo

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.biometric.BiometricManager

class SecuritySettingsActivity : AppCompatActivity() {

    private lateinit var switchFingerprint: Switch
    private lateinit var tvFingerprintStatus: TextView
    private lateinit var btnSave: Button
    private lateinit var biometricHelper: BiometricHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_settings)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Security Settings"

        // Initialize components
        switchFingerprint = findViewById(R.id.switchFingerprint)
        tvFingerprintStatus = findViewById(R.id.tvFingerprintStatus)
        btnSave = findViewById(R.id.btnSave)
        biometricHelper = BiometricHelper(this)

        // Check if biometric is available
        if (biometricHelper.isBiometricAvailable()) {
            switchFingerprint.isEnabled = true
            tvFingerprintStatus.text = "Fingerprint authentication is available on this device"

            // Load user preference
            switchFingerprint.isChecked = biometricHelper.isFingerprintEnabled()
        } else {
            switchFingerprint.isEnabled = false
            tvFingerprintStatus.text = "Fingerprint authentication is not available on this device"
        }

        // Save button click listener
        btnSave.setOnClickListener {
            if (switchFingerprint.isChecked && switchFingerprint.isEnabled) {
                // Test biometric authentication before enabling
                biometricHelper.showBiometricPrompt(
                    this,
                    onSuccess = {
                        biometricHelper.setFingerprintEnabled(true)
                        Toast.makeText(this, "Fingerprint authentication enabled", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onError = { errorCode, message ->
                        Toast.makeText(this, "Biometric error: $message", Toast.LENGTH_SHORT).show()
                        switchFingerprint.isChecked = false
                    },
                    onFailed = {
                        Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                biometricHelper.setFingerprintEnabled(false)
                Toast.makeText(this, "Fingerprint authentication disabled", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}