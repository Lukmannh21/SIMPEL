package com.mbkm.telgo

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.transition.Explode
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore

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

    // Adapters
    private val previewEventsAdapter = EventsAdapter()
    private val allEventsAdapter = EventsAdapter()

    private var isBottomSheetVisible = false
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

        // Apply animation to cards
        val cardView = findViewById<View>(R.id.cardView)
        val animation = AnimationUtils.loadAnimation(this, R.anim.card_animation)
        cardView.startAnimation(animation)

        // Set listener for bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.selectedItemId = R.id.navigation_services

        // Set up RecyclerViews
        setupRecyclerViews()

        // Add button listeners
        setupButtonListeners()

        // Setup calendar with decorators
        setupCalendar()

        // Load event dates for calendar
        loadEventDatesForCalendar()

        // Load preview events
        loadPreviewEvents()
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
            val animation = AnimationUtils.loadAnimation(this, R.anim.button_animation)
            it.startAnimation(animation)
            it.postDelayed({
                val intent = Intent(this, UploadProjectActivity::class.java)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }, 200)
        }

        btnLastHistory.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.button_animation)
            it.startAnimation(animation)
            it.postDelayed({
                val intent = Intent(this, LastUpdateActivity::class.java)
                startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
            }, 200)
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