package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private lateinit var tvFullName: TextView
    private lateinit var tvNIK: TextView
    private lateinit var tvCompanyName: TextView
    private lateinit var tvUnit: TextView
    private lateinit var tvPosition: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var btnEditProfile: Button
    private lateinit var btnNotificationSettings: Button
    private lateinit var btnLogout: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var cardPersonalInfo: CardView
    private lateinit var cardAccountDetails: CardView
    private lateinit var cardNotificationSettings: CardView
    private lateinit var ivProfileImage: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Apply activity transition animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        tvFullName = findViewById(R.id.tvFullName)
        tvNIK = findViewById(R.id.tvNIK)
        tvCompanyName = findViewById(R.id.tvCompanyName)
        tvUnit = findViewById(R.id.tvUnit)
        tvPosition = findViewById(R.id.tvPosition)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnNotificationSettings = findViewById(R.id.btnNotificationSettings)
        btnLogout = findViewById(R.id.btnLogout)
        bottomNavigationView = findViewById(R.id.bottomNavigation)
        cardPersonalInfo = findViewById(R.id.cardPersonalInfo)
        cardAccountDetails = findViewById(R.id.cardAccountDetails)
        cardNotificationSettings = findViewById(R.id.cardNotificationSettings)
        ivProfileImage = findViewById(R.id.ivProfileImage)

        // Setup bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.selectedItemId = R.id.navigation_account

        // Setup button click listeners
        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnNotificationSettings.setOnClickListener {
            val intent = Intent(this, NotificationSettingsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        btnLogout.setOnClickListener {
            // Sign out from Firebase
            auth.signOut()
            // Navigate to login screen
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }

        // Add card click animations
        setupCardAnimations()

        // Load user data
        loadUserData()
    }

    private fun setupCardAnimations() {
        cardPersonalInfo.setOnClickListener {
            it.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()
        }

        cardAccountDetails.setOnClickListener {
            it.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
            }.start()
        }

        cardNotificationSettings.setOnClickListener {
            it.animate().scaleX(0.96f).scaleY(0.96f).setDuration(100).withEndAction {
                it.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                val intent = Intent(this, NotificationSettingsActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }.start()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning from EditProfileActivity
        loadUserData()

        // Ensure the correct navigation item is selected
        bottomNavigationView.selectedItemId = R.id.navigation_account
    }

    override fun finish() {
        super.finish()
        // Apply exit animation
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Set email from auth
            tvEmail.text = currentUser.email

            // Get additional user data from Firestore
            val userId = currentUser.uid
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Set user data from Firestore with animation
                        animateDataLoading {
                            val fullName = document.getString("fullName") ?: ""
                            val nik = document.getString("nik") ?: ""
                            val companyName = document.getString("companyName") ?: ""
                            val unit = document.getString("unit") ?: ""
                            val position = document.getString("position") ?: ""
                            val phone = document.getString("phone") ?: ""

                            tvFullName.text = if (fullName.isNotEmpty()) fullName else "Not set"
                            tvNIK.text = if (nik.isNotEmpty()) nik else "Not set"
                            tvCompanyName.text = if (companyName.isNotEmpty()) companyName else "Not set"
                            tvUnit.text = if (unit.isNotEmpty()) unit else "Not set"
                            tvPosition.text = if (position.isNotEmpty()) position else "Not set"
                            tvPhone.text = if (phone.isNotEmpty()) phone else "Not set"
                        }
                    } else {
                        // No document found
                        setDefaultValues()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                    setDefaultValues()
                }
        } else {
            // User not logged in, redirect to login
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun animateDataLoading(onComplete: () -> Unit) {
        // Fade out current data
        val views = listOf(tvFullName, tvNIK, tvCompanyName, tvUnit, tvPosition, tvPhone)
        var completedAnimations = 0

        for (view in views) {
            view.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    // Update the data
                    onComplete.invoke()

                    // Fade in with new data
                    view.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .withEndAction {
                            completedAnimations++
                            if (completedAnimations == views.size) {
                                // All animations complete
                            }
                        }
                        .start()
                }
                .start()
        }
    }

    private fun setDefaultValues() {
        tvFullName.text = "Not set"
        tvNIK.text = "Not set"
        tvCompanyName.text = "Not set"
        tvUnit.text = "Not set"
        tvPosition.text = "Not set"
        tvPhone.text = "Not set"
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_home -> {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                return true
            }
            R.id.navigation_services -> {
                val intent = Intent(this, ServicesActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return true
            }
            R.id.navigation_history -> {
                val intent = Intent(this, LastUpdateActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return true
            }
            R.id.navigation_account -> {
                // We're already in ProfileActivity, no need to start a new activity
                return true
            }
        }
        return false
    }
}