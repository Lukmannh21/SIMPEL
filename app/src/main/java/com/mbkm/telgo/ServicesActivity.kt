package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener
import java.text.SimpleDateFormat
import java.util.*

class ServicesActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnWitelSearch: Button
    private lateinit var btnUploadProject: Button
    private lateinit var btnLastHistory: Button
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var upcomingEventsRecycler: RecyclerView
    private val eventsAdapter = EventsAdapter()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services)

        // Initialize UI components
        bottomNavigationView = findViewById(R.id.bottomNavigation)
        btnWitelSearch = findViewById(R.id.btnWitelSearch)
        btnUploadProject = findViewById(R.id.btnUploadProject)
        btnLastHistory = findViewById(R.id.btnLastHistory)
        calendarView = findViewById(R.id.miniCalendar)
        upcomingEventsRecycler = findViewById(R.id.upcomingEventsRecycler)

        // Set listener for bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.selectedItemId = R.id.navigation_services

        // Set up RecyclerView
        upcomingEventsRecycler.layoutManager = LinearLayoutManager(this)
        upcomingEventsRecycler.adapter = eventsAdapter

        // Set up button listeners
        btnWitelSearch.setOnClickListener {
            val intent = Intent(this, WitelSearchActivity::class.java)
            startActivity(intent)
        }

        btnUploadProject.setOnClickListener {
            val intent = Intent(this, UploadProjectActivity::class.java)
            startActivity(intent)
        }

        btnLastHistory.setOnClickListener {
            val intent = Intent(this, LastUpdateActivity::class.java)
            startActivity(intent)
        }

        // Load events into calendar
        loadEvents()

        // Handle calendar date selection
        calendarView.setOnDateChangedListener(OnDateSelectedListener { _, date, _ ->
            showEventsForDate(date)
        })
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_home -> {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.navigation_services -> {
                // We're already in ServicesActivity, no need to start a new activity
                return true
            }
            R.id.navigation_history -> {
                val intent = Intent(this, LastUpdateActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.navigation_account -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return false
    }

    private fun loadEvents() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        firestore.collection("events")
            .whereGreaterThan("date", today)
            .get()
            .addOnSuccessListener { documents ->
                val events = documents.mapNotNull { it.toObject(EventModel::class.java) }
                eventsAdapter.setEvents(events)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading events: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEventsForDate(date: CalendarDay) {
        // Format tanggal yang dipilih menjadi string seperti "2025-06-27"
        val selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            GregorianCalendar(date.year, date.month - 1, date.day).time
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
                            EventsDialogFragment.newInstance(allEvents)
                                .show(supportFragmentManager, "EventsDialog")
                        } else {
                            Toast.makeText(this, "No events found for this date", Toast.LENGTH_SHORT).show()
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
}