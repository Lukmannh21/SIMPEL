package com.mbkm.telgo

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var coordinatorLayout: CoordinatorLayout
    private lateinit var switchTocNotifications: SwitchMaterial
    private lateinit var switchPlanOaNotifications: SwitchMaterial
    private lateinit var switchEventReminders: SwitchMaterial
    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var btnClearNotificationHistory: MaterialButton
    private lateinit var btnRequestBatteryOptimization: MaterialButton
    private lateinit var statusText: TextView
    private lateinit var statusIcon: ImageView
    private lateinit var statusCard: MaterialCardView
    private lateinit var tocNotificationLayout: LinearLayout
    private lateinit var planOaNotificationLayout: LinearLayout
    private lateinit var eventReminderLayout: LinearLayout
    private lateinit var generalSettingsCard: MaterialCardView
    private lateinit var optimizationCard: MaterialCardView

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "NotificationSettings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        // Initialize UI components
        toolbar = findViewById(R.id.toolbar)
        collapsingToolbar = findViewById(R.id.collapsingToolbar)
        coordinatorLayout = findViewById(R.id.coordinatorLayout)
        switchTocNotifications = findViewById(R.id.switchTocNotifications)
        switchPlanOaNotifications = findViewById(R.id.switchPlanOaNotifications)
        switchEventReminders = findViewById(R.id.switchEventReminders)
        progressBar = findViewById(R.id.progressBar)
        btnClearNotificationHistory = findViewById(R.id.btnClearNotificationHistory)
        btnRequestBatteryOptimization = findViewById(R.id.btnRequestBatteryOptimization)
        statusText = findViewById(R.id.statusText)
        statusIcon = findViewById(R.id.statusIcon)
        statusCard = findViewById(R.id.statusCard)
        tocNotificationLayout = findViewById(R.id.tocNotificationLayout)
        planOaNotificationLayout = findViewById(R.id.planOaNotificationLayout)
        eventReminderLayout = findViewById(R.id.eventReminderLayout)
        generalSettingsCard = findViewById(R.id.generalSettingsCard)
        optimizationCard = findViewById(R.id.optimizationCard)

        // Set up toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = ""

        // Apply entrance animations
        applyEntranceAnimations()

        // Load user preferences
        loadUserPreferences()

        // Set up touch interactions for layouts
        setupTouchInteractions()

        // Set up switch listeners with animations
        setupSwitchListeners()

        // Set up clear history button
        setupClearHistoryButton()

        // Set up battery optimization button
        setupBatteryOptimizationButton()

        // Setup AppBar offset change listener for parallax effect
        setupAppBarOffsetChangeListener()
    }

    private fun setupAppBarOffsetChangeListener() {
        val appBarLayout = findViewById<AppBarLayout>(R.id.appBarLayout)
        appBarLayout.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            val scrollRange = appBarLayout.totalScrollRange
            val percentage = Math.abs(verticalOffset).toFloat() / scrollRange.toFloat()

            // Fade title in as user scrolls
            val titleText = findViewById<TextView>(R.id.titleText)
            titleText.alpha = 1 - percentage
        })
    }

    private fun applyEntranceAnimations() {
        // Card staggered animation
        val cards = arrayOf(statusCard, generalSettingsCard, optimizationCard)
        val staggerDelay = 150L

        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.translationY = 100f
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(index * staggerDelay)
                .setDuration(500)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
        }

        // Button animation
        btnClearNotificationHistory.alpha = 0f
        btnClearNotificationHistory.postDelayed({
            btnClearNotificationHistory.alpha = 1f
            val animation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
            animation.duration = 500
            btnClearNotificationHistory.startAnimation(animation)
        }, cards.size * staggerDelay)
    }

    private fun setupTouchInteractions() {
        // Set click listeners for option rows
        tocNotificationLayout.setOnClickListener {
            switchTocNotifications.toggle()
        }

        planOaNotificationLayout.setOnClickListener {
            switchPlanOaNotifications.toggle()
        }

        eventReminderLayout.setOnClickListener {
            switchEventReminders.toggle()
        }
    }

    private fun setupSwitchListeners() {
        switchTocNotifications.setOnCheckedChangeListener { _, isChecked ->
            animateSwitch(switchTocNotifications)
            updateNotificationStatus()
            updateNotificationPreference("toc_enabled", isChecked)
        }

        switchPlanOaNotifications.setOnCheckedChangeListener { _, isChecked ->
            animateSwitch(switchPlanOaNotifications)
            updateNotificationStatus()
            updateNotificationPreference("plan_oa_enabled", isChecked)
        }

        switchEventReminders.setOnCheckedChangeListener { _, isChecked ->
            animateSwitch(switchEventReminders)
            updateNotificationStatus()
            updateNotificationPreference("event_reminders_enabled", isChecked)
        }
    }

    private fun updateNotificationStatus() {
        val anyEnabled = switchTocNotifications.isChecked ||
                switchPlanOaNotifications.isChecked ||
                switchEventReminders.isChecked

        if (anyEnabled) {
            statusText.text = "Notifications are enabled"
            statusText.setTextColor(ContextCompat.getColor(this, R.color.green_success))
            statusIcon.setImageResource(R.drawable.ic_notifications_active)
            statusIcon.setColorFilter(ContextCompat.getColor(this, R.color.green_success))
        } else {
            statusText.text = "All notifications are disabled"
            statusText.setTextColor(Color.parseColor("#F44336"))
            statusIcon.setImageResource(R.drawable.ic_notifications_off)
            statusIcon.setColorFilter(Color.parseColor("#F44336"))
        }

        // Animate the status card
        val pulse = ObjectAnimator.ofFloat(statusCard, "scaleX", 1f, 1.05f, 1f)
        pulse.duration = 300
        pulse.repeatCount = 0
        pulse.interpolator = AccelerateDecelerateInterpolator()
        pulse.start()

        val pulseY = ObjectAnimator.ofFloat(statusCard, "scaleY", 1f, 1.05f, 1f)
        pulseY.duration = 300
        pulseY.repeatCount = 0
        pulseY.interpolator = AccelerateDecelerateInterpolator()
        pulseY.start()
    }

    private fun animateSwitch(switch: SwitchMaterial) {
        val scaleX = ObjectAnimator.ofFloat(switch, "scaleX", 1f, 0.8f, 1f)
        scaleX.duration = 200
        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleX.start()

        val scaleY = ObjectAnimator.ofFloat(switch, "scaleY", 1f, 0.8f, 1f)
        scaleY.duration = 200
        scaleY.interpolator = AccelerateDecelerateInterpolator()
        scaleY.start()
    }

    private fun setupClearHistoryButton() {
        btnClearNotificationHistory.setOnClickListener {
            animateButton(btnClearNotificationHistory)
            clearNotificationHistory()
        }
    }

    private fun setupBatteryOptimizationButton() {
        btnRequestBatteryOptimization.setOnClickListener {
            animateButton(btnRequestBatteryOptimization)
            if (NotificationManager.shouldRequestBatteryOptimization(this)) {
                NotificationManager.requestBatteryOptimizationExemption(this)
            } else {
                showSnackbar("App is already optimized for background operations")
            }
        }
    }

    private fun animateButton(button: MaterialButton) {
        val scaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f, 1f)
        scaleX.duration = 200
        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleX.start()

        val scaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f, 1f)
        scaleY.duration = 200
        scaleY.interpolator = AccelerateDecelerateInterpolator()
        scaleY.start()
    }

    private fun loadUserPreferences() {
        val currentUser = auth.currentUser ?: return

        showProgressWithAnimation()

        firestore.collection("user_preferences")
            .document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                hideProgressWithAnimation()

                if (document.exists()) {
                    // Load notification toggle states
                    val tocEnabled = document.getBoolean("toc_enabled") ?: true
                    val planOaEnabled = document.getBoolean("plan_oa_enabled") ?: true
                    val eventRemindersEnabled = document.getBoolean("event_reminders_enabled") ?: true

                    switchTocNotifications.isChecked = tocEnabled
                    switchPlanOaNotifications.isChecked = planOaEnabled
                    switchEventReminders.isChecked = eventRemindersEnabled

                    updateNotificationStatus()
                } else {
                    // Default values for new users
                    switchTocNotifications.isChecked = true
                    switchPlanOaNotifications.isChecked = true
                    switchEventReminders.isChecked = true

                    updateNotificationStatus()

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
                hideProgressWithAnimation()
                showSnackbar("Error loading settings: ${e.message}")
            }
    }

    private fun showProgressWithAnimation() {
        progressBar.alpha = 0f
        progressBar.visibility = View.VISIBLE
        progressBar.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun hideProgressWithAnimation() {
        progressBar.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                progressBar.visibility = View.GONE
            }
            .start()
    }

    private fun updateNotificationPreference(preferenceKey: String, isEnabled: Boolean) {
        val currentUser = auth.currentUser ?: return

        firestore.collection("user_preferences")
            .document(currentUser.uid)
            .update(preferenceKey, isEnabled)
            .addOnSuccessListener {
                // Update was successful
                showSnackbar("Preferences updated successfully")
            }
            .addOnFailureListener { e ->
                showSnackbar("Failed to update preference")

                // Roll back the UI
                when (preferenceKey) {
                    "toc_enabled" -> switchTocNotifications.isChecked = !isEnabled
                    "plan_oa_enabled" -> switchPlanOaNotifications.isChecked = !isEnabled
                    "event_reminders_enabled" -> switchEventReminders.isChecked = !isEnabled
                }
                updateNotificationStatus()
            }
    }

    private fun clearNotificationHistory() {
        val currentUser = auth.currentUser ?: return

        val animation = AnimationUtils.loadAnimation(this, R.anim.pulse)
        btnClearNotificationHistory.startAnimation(animation)

        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Clear Notification History")
            .setMessage("This will permanently delete all your past notifications. Continue?")
            .setPositiveButton("Clear All") { _, _ ->
                showProgressWithAnimation()

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
                            hideProgressWithAnimation()
                            showSnackbar("No notifications to clear")
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

                                        hideProgressWithAnimation()

                                        // Show success animation and message
                                        val successAnim = AnimationUtils.loadAnimation(this, R.anim.success_animation)
                                        btnClearNotificationHistory.startAnimation(successAnim)

                                        showSnackbar("Notification history cleared")

                                        // Cancel any pending notifications
                                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                                        notificationManager.cancelAll()
                                    }
                                    .addOnFailureListener { e ->
                                        hideProgressWithAnimation()
                                        Log.e(TAG, "Failed to update clear time", e)
                                        showSnackbar("Notifications cleared")
                                    }
                            }
                            .addOnFailureListener { e ->
                                hideProgressWithAnimation()
                                showSnackbar("Failed to clear history: ${e.message}")
                            }
                    }
                    .addOnFailureListener { e ->
                        hideProgressWithAnimation()
                        showSnackbar("Failed to clear history: ${e.message}")
                    }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}