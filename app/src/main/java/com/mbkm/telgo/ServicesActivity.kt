package com.mbkm.telgo

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.transition.Explode
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class ServicesActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnWitelSearch: Button
    private lateinit var btnUploadProject: Button
    private lateinit var btnLastHistory: Button
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var allEventsRecycler: RecyclerView
    private lateinit var upcomingEventsPreviewRecycler: RecyclerView
    private lateinit var btnUpcomingEvents: View
    private lateinit var btnSeeAllEvents: TextView
    private lateinit var btnCloseEventsSheet: ImageButton
    private lateinit var eventsBottomSheetContainer: CoordinatorLayout
    private lateinit var eventsBottomSheet: ConstraintLayout
    private lateinit var eventsDialogScrim: View
    private lateinit var loadingEventsShimmer: LinearLayout
    private lateinit var emptyEventsView: LinearLayout
    private lateinit var upcomingEventsPreview: LinearLayout
    private lateinit var eventsProgressBar: View
    private lateinit var eventsEmptyState: LinearLayout
    private lateinit var eventsEmptyStateText: TextView
    private lateinit var eventsFilterChips: ChipGroup
    private lateinit var eventsSearchView: androidx.appcompat.widget.SearchView


    // Notification-related UI components
    private lateinit var btnNotifications: FloatingActionButton
    private lateinit var notificationBadge: BadgeDrawable
    private lateinit var inAppNotification: View
    private lateinit var tvNotificationTitle: TextView
    private lateinit var tvNotificationMessage: TextView
    private lateinit var tvNotificationTimestamp: TextView
    private lateinit var btnCloseNotification: ImageView
    private lateinit var notificationsDashboardContainer: CoordinatorLayout
    private lateinit var notificationsDashboard: ConstraintLayout
    private lateinit var notificationsDialogScrim: View
    private lateinit var btnCloseNotificationsDashboard: ImageButton
    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var notificationsEmptyState: LinearLayout
    private lateinit var notificationsFilterChips: ChipGroup

    private lateinit var btnClearAllNotifications: Button
    private lateinit var btnUploadForms: Button

    private var userRole: String = "user"
    private var userStatus: String = "unverified"

    // Adapters
    private val previewEventsAdapter = EventsAdapter()
    private val allEventsAdapter = EventsAdapter()
    private val notificationsAdapter by lazy {
        NotificationsAdapter(this) { notification ->
            // Handle notification click - could open detail view or mark as read
            markNotificationAsRead(notification.id)
        }
    }

    // Request code for notification permission
    private val NOTIFICATION_PERMISSION_REQUEST_CODE = 100

    // Permission launcher for Android 13+
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, initialize notifications
            NotificationManager.checkNotificationsNow(this)
        } else {
            // Permission denied, inform user
            Toast.makeText(
                this,
                "Notification permission denied. You won't receive event reminders.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private var isBottomSheetVisible = false
    private var isNotificationsDashboardVisible = false
    private val firestore = FirebaseFirestore.getInstance()

    // Store event dates for calendar decoration
    private val tocEventDates = HashSet<CalendarDay>()
    private val planOaEventDates = HashSet<CalendarDay>()

    // Store all events for filtering
    private val allEvents = mutableListOf<EventModel>()
    private val tocEvents = mutableListOf<EventModel>()
    private val planOaEvents = mutableListOf<EventModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable content transitions
        with(window) {
            requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)
            enterTransition = Explode()
            exitTransition = Explode()
        }

        setContentView(R.layout.activity_services)



        val preferences = getSharedPreferences("TelGoPrefs", MODE_PRIVATE)
        userRole = preferences.getString("userRole", "user") ?: "user"
        userStatus = preferences.getString("userStatus", "unverified") ?: "unverified"


        // Initialize UI components
        bottomNavigationView = findViewById(R.id.bottomNavigation)
        btnWitelSearch = findViewById(R.id.btnWitelSearch)
        btnUploadProject = findViewById(R.id.btnUploadProject)
        btnLastHistory = findViewById(R.id.btnLastHistory)
        calendarView = findViewById(R.id.miniCalendar)
        btnUpcomingEvents = findViewById(R.id.btnUpcomingEvents)

        // Initialize new UI components
        allEventsRecycler = findViewById(R.id.allEventsRecycler)
        upcomingEventsPreviewRecycler = findViewById(R.id.upcomingEventsPreviewRecycler)
        btnSeeAllEvents = findViewById(R.id.btnSeeAllEvents)
        btnCloseEventsSheet = findViewById(R.id.btnCloseEventsSheet)
        eventsBottomSheetContainer = findViewById(R.id.eventsBottomSheetContainer)
        eventsBottomSheet = findViewById(R.id.eventsBottomSheet)
        eventsDialogScrim = findViewById(R.id.eventsDialogScrim)
        loadingEventsShimmer = findViewById(R.id.loadingEventsShimmer)
        emptyEventsView = findViewById(R.id.emptyEventsView)
        upcomingEventsPreview = findViewById(R.id.upcomingEventsPreview)
        eventsProgressBar = findViewById(R.id.eventsProgressBar)
        eventsEmptyState = findViewById(R.id.eventsEmptyState)
        eventsEmptyStateText = findViewById(R.id.eventsEmptyStateText)
        eventsFilterChips = findViewById(R.id.eventsFilterChips)

        btnUploadForms = findViewById(R.id.btnUploadForms)

        // Initialize SearchView
        eventsSearchView = findViewById(R.id.eventsSearchView)

        // Setup SearchView listener
        eventsSearchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { allEventsAdapter.filter(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { allEventsAdapter.filter(it) }
                return true
            }
        })

        // Apply animation to cards
        val cardView = findViewById<View>(R.id.cardView)
        val animation = AnimationUtils.loadAnimation(this, R.anim.card_animation)
        cardView.startAnimation(animation)

        // Set listener for bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.selectedItemId = R.id.navigation_services

        // In initializeNotificationsUI() add:
        btnClearAllNotifications = findViewById(R.id.btnClearAllNotifications)
        btnClearAllNotifications.setOnClickListener {
            clearAllNotifications()
        }

        // Set up RecyclerViews
        setupRecyclerViews()

        // Add button listeners
        setupButtonListeners()

        // Initialize notification UI
        initializeNotificationsUI()

        // Setup calendar with decorators
        setupCalendar()

        // Load event dates for calendar
        loadEventDatesForCalendar()

        // Load preview events
        loadPreviewEvents()

        // Initialize notification system
        NotificationManager.initialize(this)

        // Check and request notification permissions
        checkAndRequestNotificationPermission()

        // Load notifications count for badge
        loadNotificationsCount()

        // Handle if app was opened from notification
        handleNotificationIntent(intent)
    }

    private fun initializeNotificationsUI() {
        // Initialize notification button and badge
        btnNotifications = findViewById(R.id.btnNotifications)
        notificationBadge = BadgeDrawable.create(this)

        // Initialize in-app notification card
        inAppNotification = findViewById(R.id.inAppNotification)
        tvNotificationTitle = findViewById(R.id.notificationTitle)
        tvNotificationMessage = findViewById(R.id.notificationMessage)
        tvNotificationTimestamp = findViewById(R.id.notificationTimestamp)
        btnCloseNotification = findViewById(R.id.btnCloseNotification)

        // Initialize notifications dashboard
        notificationsDashboardContainer = findViewById(R.id.notificationsDashboardContainer)
        notificationsDashboard = findViewById(R.id.notificationsDashboard)
        notificationsDialogScrim = findViewById(R.id.notificationsDialogScrim)
        btnCloseNotificationsDashboard = findViewById(R.id.btnCloseNotificationsDashboard)
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        notificationsEmptyState = findViewById(R.id.notificationsEmptyState)
        notificationsFilterChips = findViewById(R.id.notificationsFilterChips)

        // Configure notifications recycler view
        notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        notificationsRecyclerView.adapter = notificationsAdapter

        // Set up notification button click
        btnNotifications.setOnClickListener {
            showNotificationsDashboard()
        }

        // Set up close button for in-app notification
        btnCloseNotification.setOnClickListener {
            hideInAppNotification()
        }

        // Set up close button for notifications dashboard
        btnCloseNotificationsDashboard.setOnClickListener {
            hideNotificationsDashboard()
        }

        // Set up scrim click for notifications dashboard
        notificationsDialogScrim.setOnClickListener {
            hideNotificationsDashboard()
        }

        // Setup filter chips
        notificationsFilterChips.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                filterNotifications(checkedIds[0])
            }
        }
    }

    // Check and request notification permissions
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    NotificationManager.checkNotificationsNow(this)
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show an explanation to the user
                    showNotificationPermissionRationale()
                }
                else -> {
                    // No explanation needed, request the permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For Android 12 and below, notification permissions are granted at install time
            NotificationManager.checkNotificationsNow(this)
        }
    }

    // Show rationale for notification permission
    private fun showNotificationPermissionRationale() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Notification Permission Required")
            .setMessage("This app needs notification permission to send you reminders about upcoming TOC and Plan OA events.")
            .setPositiveButton("Grant") { _, _ ->
                // Request the permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    this,
                    "Notification permission denied. You won't receive event reminders.",
                    Toast.LENGTH_LONG
                ).show()
            }
            .show()
    }

    private fun handleNotificationIntent(intent: Intent?) {
        // Check if this activity was launched from a notification
        intent?.let {
            if (it.getBooleanExtra("SHOW_EVENTS", false)) {
                val eventType = it.getStringExtra("EVENT_TYPE") ?: ""
                val siteId = it.getStringExtra("SITE_ID") ?: ""

                // Show events based on intent data
                if (eventType.isNotEmpty()) {
                    showEventsBottomSheet()

                    // Select the appropriate filter chip
                    when (eventType) {
                        NotificationHelper.NOTIFICATION_TYPE_TOC -> {
                            eventsFilterChips.check(R.id.chipTocEvents)
                        }
                        NotificationHelper.NOTIFICATION_TYPE_PLAN_OA -> {
                            eventsFilterChips.check(R.id.chipPlanOaEvents)
                        }
                    }
                }
            }
        }
    }

    private fun loadNotificationsCount() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        firestore.collection("notifications")
            .whereEqualTo("userId", currentUser.uid)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { documents ->
                val unreadCount = documents.size()

                if (unreadCount > 0) {
                    try {
                        // Set notification icon to active version
                        btnNotifications.setImageResource(R.drawable.ic_notification)

                        // Change button color to make it more noticeable
                        btnNotifications.backgroundTintList = ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.red_telkomsel)
                        )

                        // Add a subtle animation to draw attention
                        btnNotifications.animate()
                            .scaleX(1.1f)
                            .scaleY(1.1f)
                            .setDuration(200)
                            .withEndAction {
                                btnNotifications.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(200)
                                    .start()
                            }
                            .start()

                        // Show the latest notification in-app
                        if (documents.documents.isNotEmpty()) {
                            val latestNotification = documents.documents[0]
                            showInAppNotification(
                                latestNotification.getString("title") ?: "",
                                latestNotification.getString("message") ?: "",
                                latestNotification.getLong("timestamp") ?: System.currentTimeMillis()
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("Badge", "Error showing badge: ${e.message}")
                        // Fallback - just change the icon
                        btnNotifications.setImageResource(R.drawable.ic_notifications)
                    }
                } else {
                    // No unread notifications - use normal icon
                    btnNotifications.setImageResource(R.drawable.ic_notifications)
                    btnNotifications.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.red_telkomsel_dark)
                    )
                }
            }
    }

    private fun showInAppNotification(title: String, message: String, timestamp: Long) {
        // Set notification content
        tvNotificationTitle.text = title
        tvNotificationMessage.text = message

        // Format timestamp
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        tvNotificationTimestamp.text = dateFormat.format(Date(timestamp))

        // Show notification with animation
        inAppNotification.alpha = 0f
        inAppNotification.visibility = View.VISIBLE
        inAppNotification.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Auto-hide after 5 seconds
        inAppNotification.postDelayed({
            if (inAppNotification.visibility == View.VISIBLE) {
                hideInAppNotification()
            }
        }, 5000)
    }

    private fun hideInAppNotification() {
        inAppNotification.animate()
            .alpha(0f)
            .translationY(-100f)
            .setDuration(200)
            .withEndAction {
                inAppNotification.visibility = View.GONE
            }
            .start()
    }

    private fun showNotificationsDashboard() {
        if (isNotificationsDashboardVisible) return

        // Show dashboard container
        notificationsDashboardContainer.visibility = View.VISIBLE

        // Configure dashboard to slide up
        val sheetAnimation = ObjectAnimator.ofFloat(notificationsDashboard, "translationY",
            notificationsDashboard.height.toFloat(), 0f)
        sheetAnimation.duration = 300
        sheetAnimation.interpolator = DecelerateInterpolator()

        // Configure scrim to fade in
        val scrimAnimation = ObjectAnimator.ofFloat(notificationsDialogScrim, "alpha", 0f, 1f)
        scrimAnimation.duration = 300

        // Play animations together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(sheetAnimation, scrimAnimation)
        animatorSet.start()

        isNotificationsDashboardVisible = true

        // Load notifications
        loadNotifications()
    }

    private fun hideNotificationsDashboard() {
        if (!isNotificationsDashboardVisible) return

        // Configure dashboard to slide down
        val sheetAnimation = ObjectAnimator.ofFloat(notificationsDashboard, "translationY",
            0f, notificationsDashboard.height.toFloat())
        sheetAnimation.duration = 250

        // Configure scrim to fade out
        val scrimAnimation = ObjectAnimator.ofFloat(notificationsDialogScrim, "alpha", 1f, 0f)
        scrimAnimation.duration = 250

        // Play animations together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(sheetAnimation, scrimAnimation)
        animatorSet.start()

        // Hide container after animation completes
        sheetAnimation.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                if (animation.animatedFraction >= 0.9) {
                    notificationsDashboardContainer.visibility = View.GONE
                    isNotificationsDashboardVisible = false
                }
            }
        })
    }

    private fun loadNotifications() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        firestore.collection("notifications")
            .whereEqualTo("userId", currentUser.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val notificationsList = documents.mapNotNull { doc ->
                    val id = doc.id
                    val title = doc.getString("title") ?: ""
                    val message = doc.getString("message") ?: ""
                    val eventType = doc.getString("eventType") ?: ""
                    val siteId = doc.getString("siteId") ?: ""
                    val witel = doc.getString("witel") ?: ""
                    val eventDate = doc.getString("eventDate") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val isRead = doc.getBoolean("isRead") ?: false
                    val daysBefore = doc.getLong("daysBefore")?.toInt()

                    NotificationModel(
                        id = id,
                        title = title,
                        message = message,
                        eventType = eventType,
                        siteId = siteId,
                        witel = witel,
                        eventDate = eventDate,
                        timestamp = timestamp,
                        isRead = isRead,
                        daysBefore = daysBefore
                    )
                }

                if (notificationsList.isEmpty()) {
                    notificationsEmptyState.visibility = View.VISIBLE
                    notificationsRecyclerView.visibility = View.GONE
                } else {
                    notificationsEmptyState.visibility = View.GONE
                    notificationsRecyclerView.visibility = View.VISIBLE
                    notificationsAdapter.setNotifications(notificationsList)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading notifications: ${e.message}", Toast.LENGTH_SHORT).show()
                notificationsEmptyState.visibility = View.VISIBLE
                notificationsRecyclerView.visibility = View.GONE
            }
    }

    private fun filterNotifications(chipId: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val query = when (chipId) {
            R.id.chipAllNotifications -> {
                firestore.collection("notifications")
                    .whereEqualTo("userId", currentUser.uid)
            }
            R.id.chipTocNotifications -> {
                firestore.collection("notifications")
                    .whereEqualTo("userId", currentUser.uid)
                    .whereEqualTo("eventType", NotificationHelper.NOTIFICATION_TYPE_TOC)
            }
            R.id.chipPlanOaNotifications -> {
                firestore.collection("notifications")
                    .whereEqualTo("userId", currentUser.uid)
                    .whereEqualTo("eventType", NotificationHelper.NOTIFICATION_TYPE_PLAN_OA)
            }
            R.id.chipTodayNotifications -> {
                // Get today's start and end timestamps
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val startOfDay = cal.timeInMillis

                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                val endOfDay = cal.timeInMillis

                firestore.collection("notifications")
                    .whereEqualTo("userId", currentUser.uid)
                    .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                    .whereLessThanOrEqualTo("timestamp", endOfDay)
            }
            else -> {
                firestore.collection("notifications")
                    .whereEqualTo("userId", currentUser.uid)
            }
        }

        query.orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val notificationsList = documents.mapNotNull { doc ->
                    val id = doc.id
                    val title = doc.getString("title") ?: ""
                    val message = doc.getString("message") ?: ""
                    val eventType = doc.getString("eventType") ?: ""
                    val siteId = doc.getString("siteId") ?: ""
                    val witel = doc.getString("witel") ?: ""
                    val eventDate = doc.getString("eventDate") ?: ""
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val isRead = doc.getBoolean("isRead") ?: false
                    val daysBefore = doc.getLong("daysBefore")?.toInt()

                    NotificationModel(
                        id = id,
                        title = title,
                        message = message,
                        eventType = eventType,
                        siteId = siteId,
                        witel = witel,
                        eventDate = eventDate,
                        timestamp = timestamp,
                        isRead = isRead,
                        daysBefore = daysBefore
                    )
                }

                if (notificationsList.isEmpty()) {
                    notificationsEmptyState.visibility = View.VISIBLE
                    notificationsRecyclerView.visibility = View.GONE
                } else {
                    notificationsEmptyState.visibility = View.GONE
                    notificationsRecyclerView.visibility = View.VISIBLE
                    notificationsAdapter.setNotifications(notificationsList)
                }
            }
    }

    // Updated clearAllNotifications method
    private fun clearAllNotifications() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        // Show confirmation dialog
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Clear All Notifications")
            .setMessage("Are you sure you want to remove all notifications?")
            .setPositiveButton("Clear All") { _, _ ->
                // Show loading state
                val loadingDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                    .setView(R.layout.dialog_loading)
                    .setCancelable(false)
                    .create()
                loadingDialog.show()

                // First, permanently delete all notifications
                firestore.collection("notifications")
                    .whereEqualTo("userId", currentUser.uid)
                    .get()
                    .addOnSuccessListener { documents ->
                        val batch = firestore.batch()

                        for (doc in documents) {
                            batch.delete(doc.reference)
                        }

                        // Commit the batch deletion
                        batch.commit()
                            .addOnSuccessListener {
                                // Record the clear operation timestamp
                                val clearTimestamp = System.currentTimeMillis()

                                firestore.collection("user_preferences")
                                    .document(currentUser.uid)
                                    .set(
                                        mapOf(
                                            "last_notification_clear_time" to clearTimestamp,
                                            "notifications_cleared" to true
                                        ),
                                        SetOptions.merge()
                                    )
                                    .addOnSuccessListener {
                                        Log.d("NotificationClear", "Recorded clear operation at $clearTimestamp")

                                        // Also clear any cached notification data
                                        NotificationManager.clearNotificationData(this)

                                        // Clear the adapter
                                        notificationsAdapter.setNotifications(emptyList())

                                        // Show empty state
                                        notificationsEmptyState.visibility = View.VISIBLE
                                        notificationsRecyclerView.visibility = View.GONE

                                        // Refresh badge count
                                        loadNotificationsCount()

                                        // Dismiss loading dialog
                                        loadingDialog.dismiss()

                                        // Show success message
                                        Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show()

                                        // Hide notification dashboard
                                        hideNotificationsDashboard()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("NotificationClear", "Failed to record clear time", e)
                                        loadingDialog.dismiss()
                                        Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show()
                                        hideNotificationsDashboard()
                                    }
                            }
                            .addOnFailureListener { e ->
                                loadingDialog.dismiss()
                                Toast.makeText(this, "Error clearing notifications: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        loadingDialog.dismiss()
                        Toast.makeText(this, "Error clearing notifications: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun markNotificationAsRead(notificationId: String) {
        firestore.collection("notifications").document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener {
                Log.d("Notification", "Successfully marked notification as read")

                // Remove notification from adapter
                val currentList = notificationsAdapter.getNotifications()
                val updatedList = currentList.filterNot { it.id == notificationId }
                notificationsAdapter.setNotifications(updatedList)

                // Refresh notifications count
                loadNotificationsCount()

                // Show empty state if no notifications left
                if (updatedList.isEmpty()) {
                    notificationsEmptyState.visibility = View.VISIBLE
                    notificationsRecyclerView.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                Log.e("Notification", "Failed to mark notification as read: ${e.message}")
                Toast.makeText(this, "Failed to remove notification", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupButtonListeners() {
        btnWitelSearch.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.button_animation)
            it.startAnimation(animation)
            it.postDelayed({
                val intent = Intent(this, WitelSearchActivity::class.java)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }, 200)
        }

        btnUploadProject.setOnClickListener {
            if (userStatus == "verified" || userRole == "admin") {
                val animation = AnimationUtils.loadAnimation(this, R.anim.button_animation)
                it.startAnimation(animation)
                it.postDelayed({
                    val intent = Intent(this, UploadProjectActivity::class.java)
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                }, 200)
            } else {
                // Show verification required dialog for unverified users
                showVerificationRequiredDialog("upload new projects")
            }
        }

        btnLastHistory.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.button_animation)
            it.startAnimation(animation)
            it.postDelayed({
                val intent = Intent(this, LastUpdateActivity::class.java)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }, 200)
        }

        btnUploadForms.setOnClickListener {
            if (userStatus == "verified" || userRole == "admin") {
                val animation = AnimationUtils.loadAnimation(this, R.anim.button_animation)
                it.startAnimation(animation)
                it.postDelayed({
                    val intent = Intent(this, UploadFormsMenuActivity::class.java)
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                }, 200)
            } else {
                // Show verification required dialog for unverified users
                showVerificationRequiredDialog("upload forms")
            }
        }



        btnUpcomingEvents.setOnClickListener {
            showEventsBottomSheet()
        }

        btnSeeAllEvents.setOnClickListener {
            showEventsBottomSheet()
        }

        btnCloseEventsSheet.setOnClickListener {
            hideEventsBottomSheet()
        }

        eventsDialogScrim.setOnClickListener {
            hideEventsBottomSheet()
        }

        // Setup chip filter listeners
        eventsFilterChips.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                filterEvents(checkedIds[0])
            }
        }
    }

    // Show verification required dialog when unverified users try to access restricted features
    private fun showVerificationRequiredDialog(feature: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verification Required")
            .setMessage("Your account needs to be verified by an administrator before you can $feature. This ensures data quality and security.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("View Profile") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }
            .show()
    }

    private fun setupRecyclerViews() {
        // Setup preview recycler
        upcomingEventsPreviewRecycler.layoutManager = LinearLayoutManager(this)
        upcomingEventsPreviewRecycler.adapter = previewEventsAdapter
        upcomingEventsPreviewRecycler.isNestedScrollingEnabled = false

        // Setup all events recycler
        allEventsRecycler.layoutManager = LinearLayoutManager(this)
        allEventsRecycler.adapter = allEventsAdapter
    }

    private fun setupCalendar() {
        // Add a decorator for today
        calendarView.addDecorator(TodayDecorator())

        // Set calendar properties
        calendarView.topbarVisible = true
        calendarView.showOtherDates = MaterialCalendarView.SHOW_ALL

        // Handle calendar date selection
        calendarView.setOnDateChangedListener(OnDateSelectedListener { _, date, _ ->
            val animation = AnimationUtils.loadAnimation(this, R.anim.calendar_selection_animation)
            calendarView.startAnimation(animation)
            showEventsForDate(date)
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val intent = when (item.itemId) {
            R.id.navigation_home -> {
                Intent(this, HomeActivity::class.java)
            }
            R.id.navigation_services -> {
                // We're already in ServicesActivity, no need to start a new activity
                return true
            }
            R.id.navigation_history -> {
                Intent(this, LastUpdateActivity::class.java)
            }
            R.id.navigation_account -> {
                Intent(this, ProfileActivity::class.java)
            }
            else -> return false
        }

        // Start activity with transition animation
        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        return true
    }

    // Override onNewIntent to handle notifications when app is already open
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        // Refresh user role and status
        val preferences = getSharedPreferences("TelGoPrefs", MODE_PRIVATE)
        userRole = preferences.getString("userRole", "user") ?: "user"
        userStatus = preferences.getString("userStatus", "unverified") ?: "unverified"


    }

    private fun loadEventDatesForCalendar() {
        // Clear previous dates
        tocEventDates.clear()
        planOaEventDates.clear()

        // Format for converting string dates to CalendarDay
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Load TOC events
        firestore.collection("projects")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    // Process TOC dates
                    val tocDate = document.getString("toc")
                    if (!tocDate.isNullOrEmpty()) {
                        try {
                            val date = dateFormat.parse(tocDate)
                            date?.let {
                                val calendar = Calendar.getInstance()
                                calendar.time = it
                                val calendarDay = CalendarDay.from(
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                )
                                tocEventDates.add(calendarDay)
                            }
                        } catch (e: Exception) {
                            // Handle date parsing error
                        }
                    }

                    // Process Plan OA dates
                    val planOaDate = document.getString("tglPlanOa")
                    if (!planOaDate.isNullOrEmpty()) {
                        try {
                            val date = dateFormat.parse(planOaDate)
                            date?.let {
                                val calendar = Calendar.getInstance()
                                calendar.time = it
                                val calendarDay = CalendarDay.from(
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                )
                                planOaEventDates.add(calendarDay)
                            }
                        } catch (e: Exception) {
                            // Handle date parsing error
                        }
                    }
                }

                // Add decorators for event dates
                calendarView.addDecorator(TocEventDecorator(tocEventDates))
                calendarView.addDecorator(PlanOaEventDecorator(planOaEventDates))
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading event dates: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEventsForDate(date: CalendarDay) {
        // Format tanggal yang dipilih menjadi string seperti "2025-06-27"
        val selectedDate = "${date.year}-${String.format("%02d", date.month + 1)}-${String.format("%02d", date.day)}"

        // Membuat format tanggal yang lebih mudah dibaca
        val displayDate = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDate)!!
        )

        // Query untuk mencocokkan tanggal di Firestore
        firestore.collection("projects")
            .whereEqualTo("toc", selectedDate)
            .get()
            .addOnSuccessListener { tocDocuments ->
                val tocEvents = tocDocuments.mapNotNull { document ->
                    EventModel(
                        name = "TOC",
                        date = document.getString("toc") ?: "",
                        siteId = document.getString("siteId") ?: "",
                        witel = document.getString("witel") ?: ""
                    )
                }

                firestore.collection("projects")
                    .whereEqualTo("tglPlanOa", selectedDate)
                    .get()
                    .addOnSuccessListener { planOaDocuments ->
                        val planOaEvents = planOaDocuments.mapNotNull { document ->
                            EventModel(
                                name = "Plan OA",
                                date = document.getString("tglPlanOa") ?: "",
                                siteId = document.getString("siteId") ?: "",
                                witel = document.getString("witel") ?: ""
                            )
                        }

                        // Gabungkan semua event dari kedua query
                        val allEvents = tocEvents + planOaEvents

                        // Tampilkan data dalam dialog
                        if (allEvents.isNotEmpty()) {
                            EventsDialogFragment.newInstance(allEvents, displayDate)
                                .show(supportFragmentManager, "EventsDialog")
                        } else {
                            // Animasi untuk toast
                            val toast = Toast.makeText(this, "No events found for this date", Toast.LENGTH_SHORT)
                            toast.view?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.toast_animation))
                            toast.show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error loading Plan OA events: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading TOC events: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPreviewEvents() {
        // Show loading state
        loadingEventsShimmer.visibility = View.VISIBLE
        upcomingEventsPreview.visibility = View.GONE
        emptyEventsView.visibility = View.GONE

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        firestore.collection("projects")
            .whereGreaterThanOrEqualTo("toc", today)
            .get()
            .addOnSuccessListener { tocDocuments ->
                val tocEvents = tocDocuments.mapNotNull { document ->
                    EventModel(
                        name = "TOC",
                        date = document.getString("toc") ?: "",
                        siteId = document.getString("siteId") ?: "",
                        witel = document.getString("witel") ?: ""
                    )
                }

                this.tocEvents.clear()
                this.tocEvents.addAll(tocEvents)

                firestore.collection("projects")
                    .whereGreaterThanOrEqualTo("tglPlanOa", today)
                    .get()
                    .addOnSuccessListener { planOaDocuments ->
                        val planOaEvents = planOaDocuments.mapNotNull { document ->
                            EventModel(
                                name = "Plan OA",
                                date = document.getString("tglPlanOa") ?: "",
                                siteId = document.getString("siteId") ?: "",
                                witel = document.getString("witel") ?: ""
                            )
                        }

                        this.planOaEvents.clear()
                        this.planOaEvents.addAll(planOaEvents)

                        // Combine all events
                        allEvents.clear()
                        allEvents.addAll(tocEvents)
                        allEvents.addAll(planOaEvents)

                        // Group events for preview
                        displayPreviewEvents()
                    }
                    .addOnFailureListener { e ->
                        // Hide loading, show error
                        loadingEventsShimmer.visibility = View.GONE
                        Toast.makeText(this, "Error loading events: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                // Hide loading, show error
                loadingEventsShimmer.visibility = View.GONE
                Toast.makeText(this, "Error loading events: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayPreviewEvents() {
        // Hide loading
        loadingEventsShimmer.visibility = View.GONE

        if (allEvents.isEmpty()) {
            // Show empty state
            emptyEventsView.visibility = View.VISIBLE
            upcomingEventsPreview.visibility = View.GONE
            return
        }

        // Show preview
        upcomingEventsPreview.visibility = View.VISIBLE

        // Group and limit events
        val sortedEvents = allEvents.sortedBy { it.date }

        // Group by date and then by event type
        val groupedEvents = sortedEvents
            .groupBy { it.date }
            .flatMap { dateGroup ->
                // For each date, take only unique site IDs
                dateGroup.value.distinctBy { it.siteId }
            }
            .take(3) // Show only 3 events in preview

        // Update adapter
        previewEventsAdapter.setEvents(groupedEvents)

        // Show/hide "See All" button based on event count
        btnSeeAllEvents.visibility = if (allEvents.size > 3) View.VISIBLE else View.GONE
    }

    private fun showEventsBottomSheet() {
        if (isBottomSheetVisible) return

        // Show bottom sheet container
        eventsBottomSheetContainer.visibility = View.VISIBLE

        // Configure bottom sheet to slide up
        val sheetAnimation = ObjectAnimator.ofFloat(eventsBottomSheet, "translationY",
            eventsBottomSheet.height.toFloat(), 0f)
        sheetAnimation.duration = 300
        sheetAnimation.interpolator = DecelerateInterpolator()

        // Configure scrim to fade in
        val scrimAnimation = ObjectAnimator.ofFloat(eventsDialogScrim, "alpha", 0f, 1f)
        scrimAnimation.duration = 300

        // Play animations together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(sheetAnimation, scrimAnimation)
        animatorSet.start()

        isBottomSheetVisible = true

        // Load all events in bottom sheet
        loadAllEvents()
    }

    private fun hideEventsBottomSheet() {
        if (!isBottomSheetVisible) return

        // Configure bottom sheet to slide down
        val sheetAnimation = ObjectAnimator.ofFloat(eventsBottomSheet, "translationY",
            0f, eventsBottomSheet.height.toFloat())
        sheetAnimation.duration = 250

        // Configure scrim to fade out
        val scrimAnimation = ObjectAnimator.ofFloat(eventsDialogScrim, "alpha", 1f, 0f)
        scrimAnimation.duration = 250

        // Play animations together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(sheetAnimation, scrimAnimation)
        animatorSet.start()

        // Hide container after animation completes
        sheetAnimation.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            override fun onAnimationUpdate(animation: ValueAnimator) {
                if (animation.animatedFraction >= 0.9) {
                    eventsBottomSheetContainer.visibility = View.GONE
                    isBottomSheetVisible = false
                }
            }
        })
    }

    private fun loadAllEvents() {
        // Show loading state
        eventsProgressBar.visibility = View.VISIBLE
        eventsEmptyState.visibility = View.GONE
        allEventsRecycler.visibility = View.GONE

        // We already have the events loaded in memory, so just display them
        if (allEvents.isNotEmpty()) {
            displayAllEvents(allEvents)
            return
        }

        // If events aren't loaded yet, load them
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        firestore.collection("projects")
            .whereGreaterThanOrEqualTo("toc", today)
            .get()
            .addOnSuccessListener { tocDocuments ->
                val tocEvents = tocDocuments.mapNotNull { document ->
                    EventModel(
                        name = "TOC",
                        date = document.getString("toc") ?: "",
                        siteId = document.getString("siteId") ?: "",
                        witel = document.getString("witel") ?: ""
                    )
                }

                this.tocEvents.clear()
                this.tocEvents.addAll(tocEvents)

                firestore.collection("projects")
                    .whereGreaterThanOrEqualTo("tglPlanOa", today)
                    .get()
                    .addOnSuccessListener { planOaDocuments ->
                        val planOaEvents = planOaDocuments.mapNotNull { document ->
                            EventModel(
                                name = "Plan OA",
                                date = document.getString("tglPlanOa") ?: "",
                                siteId = document.getString("siteId") ?: "",
                                witel = document.getString("witel") ?: ""
                            )
                        }

                        this.planOaEvents.clear()
                        this.planOaEvents.addAll(planOaEvents)

                        // Combine all events
                        allEvents.clear()
                        allEvents.addAll(tocEvents)
                        allEvents.addAll(planOaEvents)

                        // Display events
                        displayAllEvents(allEvents)
                    }
                    .addOnFailureListener { e ->
                        // Hide loading, show error
                        eventsProgressBar.visibility = View.GONE
                        Toast.makeText(this, "Error loading events: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                // Hide loading, show error
                eventsProgressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading events: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun displayAllEvents(events: List<EventModel>) {
        // Hide loading
        eventsProgressBar.visibility = View.GONE

        if (events.isEmpty()) {
            // Show empty state
            eventsEmptyState.visibility = View.VISIBLE
            allEventsRecycler.visibility = View.GONE
            return
        }

        // Show events
        allEventsRecycler.visibility = View.VISIBLE

        // Group events by date
        val sortedEvents = events.sortedBy { it.date }

        // Update adapter
        allEventsAdapter.setEvents(sortedEvents)

        // Animate items in
        val animation = AnimationUtils.loadAnimation(this, R.anim.item_animation_fall_down)
        allEventsRecycler.startAnimation(animation)
    }

    private fun filterEvents(chipId: Int) {
        when (chipId) {
            R.id.chipAllEvents -> {
                displayAllEvents(allEvents)
            }
            R.id.chipTocEvents -> {
                displayAllEvents(tocEvents)
            }
            R.id.chipPlanOaEvents -> {
                displayAllEvents(planOaEvents)
            }
            R.id.chipThisWeek -> {
                // Filter for events this week
                val calendar = Calendar.getInstance()
                val today = calendar.time

                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val weekStart = calendar.time

                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val weekEnd = calendar.time

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val weekStartStr = dateFormat.format(weekStart)
                val weekEndStr = dateFormat.format(weekEnd)

                val thisWeekEvents = allEvents.filter { event ->
                    val eventDate = dateFormat.parse(event.date)
                    eventDate != null && !eventDate.before(weekStart) && !eventDate.after(weekEnd)
                }

                if (thisWeekEvents.isEmpty()) {
                    eventsEmptyStateText.text = "No events this week"
                }

                displayAllEvents(thisWeekEvents)
            }
            R.id.chipThisMonth -> {
                // Filter for events this month
                val calendar = Calendar.getInstance()
                val month = calendar.get(Calendar.MONTH)
                val year = calendar.get(Calendar.YEAR)

                val thisMonthEvents = allEvents.filter { event ->
                    val eventDateStr = event.date
                    if (eventDateStr.isNotEmpty()) {
                        val dateParts = eventDateStr.split("-")
                        if (dateParts.size == 3) {
                            val eventYear = dateParts[0].toInt()
                            val eventMonth = dateParts[1].toInt() - 1 // Calendar months are 0-based

                            eventYear == year && eventMonth == month
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }

                if (thisMonthEvents.isEmpty()) {
                    eventsEmptyStateText.text = "No events this month"
                }

                displayAllEvents(thisMonthEvents)
            }
        }
    }

    // Calendar decorator for today's date
    private inner class TodayDecorator : DayViewDecorator {
        private val today = CalendarDay.today()

        override fun shouldDecorate(day: CalendarDay): Boolean {
            return day == today
        }

        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(8f, ContextCompat.getColor(this@ServicesActivity, R.color.today_color)))
            view.setBackgroundDrawable(ContextCompat.getDrawable(this@ServicesActivity, R.drawable.today_background)!!)
        }
    }

    // Calendar decorator for TOC events
    private inner class TocEventDecorator(private val dates: Collection<CalendarDay>) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean {
            return dates.contains(day)
        }

        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(6f, ContextCompat.getColor(this@ServicesActivity, R.color.toc_event_color)))
        }
    }

    // Calendar decorator for Plan OA events
    private inner class PlanOaEventDecorator(private val dates: Collection<CalendarDay>) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean {
            return dates.contains(day)
        }

        override fun decorate(view: DayViewFacade) {
            view.addSpan(DotSpan(6f, ContextCompat.getColor(this@ServicesActivity, R.color.plan_oa_color)))
        }
    }
}