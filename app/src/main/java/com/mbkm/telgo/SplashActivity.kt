package com.mbkm.telgo

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()

        // Remove any problematic window insets code here
        // Just keep the basic functionality for the splash screen

        // Delay for a short time to show splash screen
        Handler(Looper.getMainLooper()).postDelayed({
            checkUserLoggedIn()
        }, 1500) // 1.5 seconds delay
    }

    private fun checkUserLoggedIn() {
        // Check if user is signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // User is signed in, go to HomeActivity
            startActivity(Intent(this, ServicesActivity::class.java))
        } else {
            // No user is signed in, go to LoginActivity
            startActivity(Intent(this, LoginActivity::class.java))
        }
        finish()
    }
}