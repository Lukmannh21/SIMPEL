package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.animation.flyTo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import com.mapbox.maps.plugin.animation.MapAnimationOptions.Companion.mapAnimationOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WitelDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBack: ImageButton
    private lateinit var tvWitelTitle: TextView
    private lateinit var tvSiteCount: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var searchView: SearchView
    private lateinit var siteAdapter: SiteAdapter
    private lateinit var siteList: ArrayList<SiteModel>
    private lateinit var filteredSiteList: ArrayList<SiteModel>
    private lateinit var firestore: FirebaseFirestore
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private var witelName: String = ""

    // Add variable for the original site list
    private lateinit var originalSiteList: ArrayList<SiteModel>

    // Tambahkan variabel untuk koordinat provinsi
    private var provinceLat: Double = 0.0
    private var provinceLon: Double = 0.0
    private var hasProvinceCoordinates = false

    // MapBox related properties
    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private val markerColorMap = mapOf(
        "OA" to Color.GREEN,
        "MAT DEL" to Color.YELLOW,
        "DONE" to Color.BLUE,
        "SURVEY" to Color.RED,
        "POWER ON" to Color.CYAN,
        "DROP" to Color.BLACK,
        "MOS" to Color.MAGENTA,
        "INTEGRASI" to Color.GRAY,
        "DONE UT" to Color.GREEN
    )
    private val defaultMarkerColor = Color.RED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_witel_detail)

        // Get witel name from intent
        witelName = intent.getStringExtra("WITEL_NAME") ?: ""

        // Tambahkan kode untuk mendapatkan koordinat provinsi dari intent
        provinceLat = intent.getDoubleExtra("PROVINCE_LAT", 0.0)
        provinceLon = intent.getDoubleExtra("PROVINCE_LON", 0.0)
        hasProvinceCoordinates = provinceLat != 0.0 && provinceLon != 0.0

        if (witelName.isEmpty()) {
            showToast("Witel not specified")
            finish()
            return
        }

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerViewSites)
        btnBack = findViewById(R.id.btnBack)
        tvWitelTitle = findViewById(R.id.tvWitelTitle)
        tvSiteCount = findViewById(R.id.tvSiteCount)
        tvLastUpdated = findViewById(R.id.tvLastUpdated)
        searchView = findViewById(R.id.searchView)
        progressBar = findViewById(R.id.progressBar)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        // Initialize MapBox
        mapView = findViewById(R.id.mapView)
        mapboxMap = mapView.getMapboxMap()

        // Setup swipe refresh
        swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent,
            android.R.color.holo_green_dark
        )
        swipeRefreshLayout.setOnRefreshListener {
            // Reload data
            loadSitesForWitel()
        }

        // Set toolbar title to selected witel
        tvWitelTitle.text = witelName

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize site lists
        siteList = arrayListOf()
        originalSiteList = arrayListOf()
        filteredSiteList = arrayListOf()

        siteAdapter = SiteAdapter(filteredSiteList) { site ->
            // When a site is clicked, either focus the map or navigate to detail view
            focusMapOnSite(site)

            // Navigate to SiteDetailActivity
            val intent = Intent(this, SiteDetailActivity::class.java)
            intent.putExtra("SITE_ID", site.siteId)
            intent.putExtra("WITEL", site.witel)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = siteAdapter

        // Setup search functionality
        setupSearchView()

        // Initialize map layer selector FAB
        val fabMapLayers = findViewById<FloatingActionButton>(R.id.fabMapLayers)
        fabMapLayers.setOnClickListener {
            showMapStyleOptions()
        }

        // Initialize MapBox style
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            // Initialize point annotation manager for markers
            initializePointAnnotationManager()

            // Tambahkan: Tampilkan peta dengan koordinat provinsi jika tersedia
            if (hasProvinceCoordinates) {
                setInitialMapView()
            }

            // Load site data for the selected witel
            loadSitesForWitel()
        }
    }

    private fun setupSearchView() {
        searchView.queryHint = "Search by Site ID or Status..."

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterSites(newText)
                return true
            }
        })
    }

    private fun filterSites(query: String?) {
        filteredSiteList.clear()

        if (query.isNullOrBlank()) {
            // If search query is empty, show all sites
            filteredSiteList.addAll(originalSiteList)
            updateMapMarkers(filteredSiteList)
        } else {
            // Filter sites based on query
            val searchQuery = query.lowercase(Locale.getDefault())

            for (site in originalSiteList) {
                if (site.siteId.lowercase(Locale.getDefault()).contains(searchQuery) ||
                    site.status.lowercase(Locale.getDefault()).contains(searchQuery) ||
                    site.lastIssue.lowercase(Locale.getDefault()).contains(searchQuery)) {
                    filteredSiteList.add(site)
                }
            }

            // Update map markers to show only filtered sites
            updateMapMarkers(filteredSiteList)
        }

        // Update site count to show filtered count
        tvSiteCount.text = "Total Sites: ${filteredSiteList.size}"

        // Notify adapter about the data change
        siteAdapter.notifyDataSetChanged()
    }

    private fun updateMapMarkers(sitesToShow: List<SiteModel>) {
        // Clear existing markers
        pointAnnotationManager.deleteAll()

        // Add markers for the filtered sites
        for (site in sitesToShow) {
            try {
                val coords = parseCoordinates(site.koordinat)
                if (coords != null) {
                    val point = Point.fromLngLat(coords.second, coords.first) // Note the order: lng, lat
                    addMarkerToMap(site, point)
                }
            } catch (e: Exception) {
                // Handle invalid coordinates
                e.printStackTrace()
            }
        }
    }

    // Tambahkan method untuk menampilkan koordinat provinsi
    private fun setInitialMapView() {
        val point = Point.fromLngLat(provinceLon, provinceLat) // Note the order: lng, lat
        val cameraOptions = CameraOptions.Builder()
            .center(point)
            .zoom(6.0)  // Nilai zoom yang lebih kecil (1-22, di mana 1 paling jauh dan 22 paling dekat)
            .pitch(0.0)
            .bearing(0.0)
            .build()

        mapboxMap.setCamera(cameraOptions)
    }

    private fun showMapStyleOptions() {
        val styles = arrayOf("Streets", "Outdoors", "Satellite", "Satellite Streets", "Light", "Dark")
        val styleUris = arrayOf(
            Style.MAPBOX_STREETS,
            Style.OUTDOORS,
            Style.SATELLITE,
            Style.SATELLITE_STREETS,
            Style.LIGHT,
            Style.DARK
        )

        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Map Style")
            .setItems(styles) { _, which ->
                mapboxMap.loadStyleUri(styleUris[which])
            }
            .create()

        dialog.show()
    }

    private fun initializePointAnnotationManager() {
        val annotationPlugin = mapView.annotations
        pointAnnotationManager = annotationPlugin.createPointAnnotationManager()
    }

    private fun loadSitesForWitel() {
        progressBar.visibility = View.VISIBLE

        // Update last updated text with current date and username
        val currentDate = "2025-04-28 01:28:42" // Using the provided date
        val userName = "Lukmannh21" // Using the provided username
        tvLastUpdated.text = "Last updated: $currentDate by $userName"

        firestore.collection("projects")
            .whereEqualTo("witel", witelName)
            .get()
            .addOnSuccessListener { documents ->
                originalSiteList.clear()
                siteList.clear()
                filteredSiteList.clear()
                val sitePoints = mutableListOf<Point>()

                for (document in documents) {
                    val siteId = document.getString("siteId") ?: ""
                    val witel = document.getString("witel") ?: ""
                    val status = document.getString("status") ?: ""
                    val lastIssueHistory = document.get("lastIssueHistory") as? List<String>
                    val lastIssue = if (lastIssueHistory.isNullOrEmpty()) "" else lastIssueHistory[0]
                    val koordinat = document.getString("koordinat") ?: ""

                    val site = SiteModel(
                        siteId = siteId,
                        witel = witel,
                        status = status,
                        lastIssue = lastIssue,
                        koordinat = koordinat
                    )

                    originalSiteList.add(site)
                    siteList.add(site)

                    // Add marker for this site on the map
                    try {
                        val coords = parseCoordinates(koordinat)
                        if (coords != null) {
                            val point = Point.fromLngLat(coords.second, coords.first) // Note the order: lng, lat
                            sitePoints.add(point)
                            addMarkerToMap(site, point)
                        }
                    } catch (e: Exception) {
                        // Handle invalid coordinates
                        e.printStackTrace()
                    }
                }

                // Initialize the filtered list with all sites
                filteredSiteList.addAll(originalSiteList)

                // Update site count and refresh adapter
                tvSiteCount.text = "Total Sites: ${filteredSiteList.size}"
                siteAdapter.notifyDataSetChanged()

                // Focus map to show all markers if we have any and no province coordinates
                if (sitePoints.isNotEmpty() && !hasProvinceCoordinates) {
                    // Hanya fokus ke site pertama jika tidak ada koordinat provinsi
                    focusMapOnSite(filteredSiteList[0])
                }

                // Show message if no sites found
                if (filteredSiteList.isEmpty()) {
                    showToast("No sites found for $witelName")
                }

                // Hide loading indicators
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                showToast("Error loading sites: ${e.message}")
            }
    }

    // Helper function to parse "latitude, longitude" coordinate strings
    private fun parseCoordinates(koordinat: String): Pair<Double, Double>? {
        // Example format: "1.234567, 123.456789"
        try {
            val parts = koordinat.split(",")
            if (parts.size == 2) {
                val lat = parts[0].trim().toDouble()
                val lng = parts[1].trim().toDouble()
                return Pair(lat, lng)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun addMarkerToMap(site: SiteModel, point: Point) {
        // Determine marker color based on site status
        val color = markerColorMap[site.status] ?: defaultMarkerColor
        val markerIcon = createMarkerBitmap(color)

        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(markerIcon)
            .withTextField(site.siteId)
            .withTextSize(12.0)
            .withTextOffset(listOf(0.0, 2.0))
            .withTextColor(Color.BLACK)
            .withTextHaloColor(Color.WHITE)
            .withTextHaloWidth(1.0)

        pointAnnotationManager.create(pointAnnotationOptions)
    }

    private fun createMarkerBitmap(color: Int): Bitmap {
        val width = 30
        val height = 30
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val shadowPaint = Paint().apply {
            this.color = Color.argb(100, 0, 0, 0)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Draw shadow
        canvas.drawCircle(width / 2f, height / 2f + 2, width / 3f, shadowPaint)

        // Draw main circle
        canvas.drawCircle(width / 2f, height / 2f, width / 3f, paint)

        return bitmap
    }

    private fun focusMapOnSite(site: SiteModel) {
        val coords = parseCoordinates(site.koordinat) ?: return

        // Find the position of this site in the list
        val position = filteredSiteList.indexOf(site)
        if (position != -1) {
            // Scroll to position with animation
            recyclerView.smoothScrollToPosition(position)
        }

        val point = Point.fromLngLat(coords.second, coords.first) // Note the order: lng, lat

        // Create camera animation
        val cameraOptions = CameraOptions.Builder()
            .center(point)
            .zoom(15.0)
            .build()

        // Animate camera to position
        mapboxMap.flyTo(
            cameraOptions,
            mapAnimationOptions {
                duration(1000)
                interpolator(AccelerateDecelerateInterpolator())
            }
        )

        // Add a pulsing effect to the marker
        highlightMarker(point)
    }

    // Add this method to highlight a marker
    private fun highlightMarker(point: Point) {
        // Create a temporary marker with animation
        val pulsingMarker = createPulsingMarker()

        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(pulsingMarker)

        val annotation = pointAnnotationManager.create(pointAnnotationOptions)

        // Remove pulsing marker after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            pointAnnotationManager.delete(annotation)
        }, 3000)
    }

    // Add this method to create a pulsing marker
    private fun createPulsingMarker(): Bitmap {
        val width = 50
        val height = 50
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Draw outer circle
        paint.alpha = 80
        canvas.drawCircle(width / 2f, height / 2f, width / 2f, paint)

        // Draw inner circle
        paint.alpha = 255
        canvas.drawCircle(width / 2f, height / 2f, width / 4f, paint)

        return bitmap
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}