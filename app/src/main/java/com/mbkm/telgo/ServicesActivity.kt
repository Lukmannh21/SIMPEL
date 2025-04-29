package com.mbkm.telgo

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.transition.Explode
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet

class ServicesActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnWitelSearch: Button
    private lateinit var btnUploadProject: Button
    private lateinit var btnLastHistory: Button
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var upcomingEventsRecycler: RecyclerView
    private lateinit var btnUpcomingEvents: Button

    private val eventsAdapter = EventsAdapter()
    private var isUpcomingEventsVisible = false
    private val firestore = FirebaseFirestore.getInstance()

    // Store event dates for calendar decoration
    private val tocEventDates = HashSet<CalendarDay>()
    private val planOaEventDates = HashSet<CalendarDay>()

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
        upcomingEventsRecycler = findViewById(R.id.upcomingEventsRecycler)
        btnUpcomingEvents = findViewById(R.id.btnUpcomingEvents)

        // Apply animation to cards
        val cardView = findViewById<View>(R.id.cardView)
        val animation = AnimationUtils.loadAnimation(this, R.anim.card_animation)
        cardView.startAnimation(animation)

        // Set listener for bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.selectedItemId = R.id.navigation_services

        // Set up RecyclerView
        upcomingEventsRecycler.layoutManager = LinearLayoutManager(this)
        upcomingEventsRecycler.adapter = eventsAdapter

        // Add button animation
        addButtonAnimation(btnWitelSearch)
        addButtonAnimation(btnUploadProject)
        addButtonAnimation(btnLastHistory)
        addButtonAnimation(btnUpcomingEvents)

        // Set up button listeners
        btnWitelSearch.setOnClickListener {
            val intent = Intent(this, WitelSearchActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        btnUploadProject.setOnClickListener {
            val intent = Intent(this, UploadProjectActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        btnLastHistory.setOnClickListener {
            val intent = Intent(this, LastUpdateActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        btnUpcomingEvents.setOnClickListener {
            toggleUpcomingEvents()
        }

        // Setup calendar with decorators
        setupCalendar()

        // Load event dates for calendar
        loadEventDatesForCalendar()
    }

    private fun addButtonAnimation(button: Button) {
        button.setOnClickListener { view ->
            val animation = AnimationUtils.loadAnimation(this, R.anim.button_animation)
            view.startAnimation(animation)

            // Add a small delay to show the animation before the original click action
            view.postDelayed({
                when (view.id) {
                    R.id.btnWitelSearch -> {
                        val intent = Intent(this, WitelSearchActivity::class.java)
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                    }
                    R.id.btnUploadProject -> {
                        val intent = Intent(this, UploadProjectActivity::class.java)
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                    }
                    R.id.btnLastHistory -> {
                        val intent = Intent(this, LastUpdateActivity::class.java)
                        startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                    }
                    R.id.btnUpcomingEvents -> {
                        toggleUpcomingEvents()
                    }
                }
            }, 200)
        }
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

    private fun toggleUpcomingEvents() {
        if (isUpcomingEventsVisible) {
            // Sembunyikan RecyclerView dengan animasi
            val animation = AnimationUtils.loadAnimation(this, R.anim.slide_out_bottom)
            upcomingEventsRecycler.startAnimation(animation)
            upcomingEventsRecycler.visibility = View.GONE
            btnUpcomingEvents.text = "Load Upcoming Events"
            isUpcomingEventsVisible = false
        } else {
            // Tampilkan RecyclerView dengan animasi dan muat data
            upcomingEventsRecycler.visibility = View.VISIBLE
            val animation = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
            upcomingEventsRecycler.startAnimation(animation)
            btnUpcomingEvents.text = "Hide Upcoming Events"
            isUpcomingEventsVisible = true

            // Muat data
            loadUpcomingEvents()
        }
    }

    private fun loadUpcomingEvents() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Show loading indicator (if you have one)
        // loadingIndicator.visibility = View.VISIBLE

        // Clear existing events
        eventsAdapter.setEvents(listOf())

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

                        // Combine events and group by date
                        val allEvents = (tocEvents + planOaEvents).sortedBy { it.date }

                        // Group by date
                        val groupedEvents = allEvents.groupBy { it.date }
                            .flatMap { entry ->
                                // For each date, limit to 3 events per site ID to avoid repetition
                                entry.value.groupBy { it.siteId }
                                    .flatMap { it.value.take(1) }
                                    .sortedBy { it.siteId }
                            }

                        if (groupedEvents.isNotEmpty()) {
                            eventsAdapter.setEvents(groupedEvents)
                        } else {
                            Toast.makeText(this, "No upcoming events found", Toast.LENGTH_SHORT).show()
                        }

                        // Hide loading indicator (if you have one)
                        // loadingIndicator.visibility = View.GONE
                    }
                    .addOnFailureListener { e ->
                        // Hide loading indicator (if you have one)
                        // loadingIndicator.visibility = View.GONE
                        Toast.makeText(this, "Error loading Plan OA events: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                // Hide loading indicator (if you have one)
                // loadingIndicator.visibility = View.GONE
                Toast.makeText(this, "Error loading TOC events: ${e.message}", Toast.LENGTH_SHORT).show()
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