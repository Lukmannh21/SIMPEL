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

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
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

        // Delay for checking user login status
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserLoggedIn()
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

    private fun checkUserLoggedIn() {
        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, go to ServicesActivity
            val intent = Intent(this, ServicesActivity::class.java)
            startActivity(intent)

            // Request battery optimization exemption if the user is logged in
            if (NotificationManager.shouldRequestBatteryOptimization(this)) {
                try {
                    NotificationManager.requestBatteryOptimizationExemption(this)
                } catch (e: Exception) {
                    Log.e(TAG, "Error requesting battery optimization: ${e.message}", e)
                    // Continue without stopping the flow if this fails
                }
            }
        } else {
            // No user is signed in, go to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }

        // Apply exit animation
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }
}