package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var logoImageView: ImageView
    private lateinit var watermarkTextView: TextView
    private lateinit var pulseView: View
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Initialize notification system early - added for background notifications
        setupNotificationsSystem()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        logoImageView = findViewById(R.id.splash_logo)
        watermarkTextView = findViewById(R.id.watermark_text)
        pulseView = findViewById(R.id.logo_background)

        // Apply animations
        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_animation)
        val watermarkAnimation = AnimationUtils.loadAnimation(this, R.anim.watermark_animation)
        val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_animation)

        logoImageView.startAnimation(logoAnimation)
        watermarkTextView.startAnimation(watermarkAnimation)
        pulseView.startAnimation(pulseAnimation)

        // Delay for displaying splash and then directing to login
        Handler(Looper.getMainLooper()).postDelayed({
            // Always log user out and redirect to login screen
            forceLoginScreen()
        }, 2500) // Extended to 2.5 seconds to enjoy the animations
    }

    private fun setupNotificationsSystem() {
        try {
            // 1. Initialize notification channels and basic setup
            NotificationManager.initialize(this)

            // 2. Set up aggressive background notification checks
            NotificationManager.setupAggressiveBackgroundChecks(this)

            // 3. Trigger an immediate check to ensure notifications are working
            NotificationManager.checkNotificationsNow(this)

            Log.d(TAG, "Notification system initialized successfully with aggressive scheduling")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up notifications: ${e.message}", e)
        }
    }

    private fun forceLoginScreen() {
        // Always sign out the user if there's an existing session
        if (auth.currentUser != null) {
            auth.signOut()

            // Clear user data from SharedPreferences
            val preferences = getSharedPreferences("TelGoPrefs", MODE_PRIVATE)
            preferences.edit().apply {
                remove("userRole")
                remove("userStatus")
                remove("lastUserInfoUpdate")
                apply()
            }

            Log.d(TAG, "User signed out to enforce fresh login")
        }

        // Direct to LoginActivity
        startActivity(Intent(this, LoginActivity::class.java))

        // Apply exit animation
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}