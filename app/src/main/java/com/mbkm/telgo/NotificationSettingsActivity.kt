package com.mbkm.telgo

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var switchTocNotifications: SwitchCompat
    private lateinit var switchPlanOaNotifications: SwitchCompat
    private lateinit var switchEventReminders: SwitchCompat
    private lateinit var progressBar: ProgressBar
    private lateinit var btnClearNotificationHistory: Button
    private lateinit var btnRequestBatteryOptimization: Button

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "NotificationSettings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar)
        switchTocNotifications = findViewById(R.id.switchTocNotifications)
        switchPlanOaNotifications = findViewById(R.id.switchPlanOaNotifications)
        switchEventReminders = findViewById(R.id.switchEventReminders)
        progressBar = findViewById(R.id.progressBar)
        btnClearNotificationHistory = findViewById(R.id.btnClearNotificationHistory)
        btnRequestBatteryOptimization = findViewById(R.id.btnRequestBatteryOptimization)

        // Set up toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Notification Settings"

        // Load user preferences
        loadUserPreferences()

        // Set up switch listeners
        switchTocNotifications.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationPreference("toc_enabled", isChecked)
        }

        switchPlanOaNotifications.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationPreference("plan_oa_enabled", isChecked)
        }

        switchEventReminders.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationPreference("event_reminders_enabled", isChecked)
        }

        // Set up clear history button
        btnClearNotificationHistory.setOnClickListener {
            clearNotificationHistory()
        }

        // Set up battery optimization button
        btnRequestBatteryOptimization.setOnClickListener {
            if (NotificationManager.shouldRequestBatteryOptimization(this)) {
                NotificationManager.requestBatteryOptimizationExemption(this)
            } else {
                Toast.makeText(this, "App is already optimized for background operations", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserPreferences() {
        val currentUser = auth.currentUser ?: return

        progressBar.visibility = View.VISIBLE

        firestore.collection("user_preferences")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE

                if (document.exists()) {
                    // Load notification toggle states
                    val tocEnabled = document.getBoolean("toc_enabled") ?: true
                    val planOaEnabled = document.getBoolean("plan_oa_enabled") ?: true
                    val eventRemindersEnabled = document.getBoolean("event_reminders_enabled") ?: true

                    switchTocNotifications.isChecked = tocEnabled
                    switchPlanOaNotifications.isChecked = planOaEnabled
                    switchEventReminders.isChecked = eventRemindersEnabled

                } else {
                    // Default values for new users
                    switchTocNotifications.isChecked = true
                    switchPlanOaNotifications.isChecked = true
                    switchEventReminders.isChecked = true

                    // Create default preferences document
                    val defaultPrefs = hashMapOf(
                        "toc_enabled" to true,
                        "plan_oa_enabled" to true,
                        "event_reminders_enabled" to true
                    )

                    firestore.collection("user_preferences")
                        .document(currentUser.uid)
                        .set(defaultPrefs)
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateNotificationPreference(preferenceKey: String, isEnabled: Boolean) {
        val currentUser = auth.currentUser ?: return

        firestore.collection("user_preferences")
            .document(currentUser.uid)
            .update(preferenceKey, isEnabled)
            .addOnSuccessListener {
                // Update was successful
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update preference: ${e.message}", Toast.LENGTH_SHORT).show()
                // Roll back the UI
                when (preferenceKey) {
                    "toc_enabled" -> switchTocNotifications.isChecked = !isEnabled
                    "plan_oa_enabled" -> switchPlanOaNotifications.isChecked = !isEnabled
                    "event_reminders_enabled" -> switchEventReminders.isChecked = !isEnabled
                }
            }
    }

    private fun clearNotificationHistory() {
        val currentUser = auth.currentUser ?: return

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Clear Notification History")
            .setMessage("This will permanently delete all your past notifications. Continue?")
            .setPositiveButton("Clear All") { _, _ ->
                progressBar.visibility = View.VISIBLE

                // Delete all notifications for this user
                firestore.collection("notifications")
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .addOnSuccessListener { documents ->
                        val batch = firestore.batch()

                        for (doc in documents) {
                            batch.delete(doc.reference)
                        }

                        if (documents.isEmpty) {
                            // No notifications found
                            progressBar.visibility = View.GONE
                            Toast.makeText(this, "No notifications to clear", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        batch.commit()
                            .addOnSuccessListener {
                                // Record the clear operation
                                firestore.collection("user_preferences")
                                    .document(currentUser.uid)
                                    .set(
                                        mapOf(
                                            "last_notification_clear_time" to System.currentTimeMillis(),
                                            "notifications_cleared" to true
                                        ),
                                        SetOptions.merge()
                                    )
                                    .addOnSuccessListener {
                                        // Also clear local notification data
                                        NotificationManager.clearNotificationData(this)

                                        progressBar.visibility = View.GONE
                                        Toast.makeText(this, "Notification history cleared", Toast.LENGTH_SHORT).show()

                                        // Cancel any pending notifications
                                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                                        notificationManager.cancelAll()
                                    }
                                    .addOnFailureListener { e ->
                                        progressBar.visibility = View.GONE
                                        Log.e(TAG, "Failed to update clear time", e)
                                        Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener { e ->
                                progressBar.visibility = View.GONE
                                Toast.makeText(this, "Failed to clear history: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, "Failed to clear history: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}