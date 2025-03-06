package com.mbkm.telgo

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class WitelDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBack: Button
    private lateinit var tvWitelTitle: TextView
    private lateinit var tvSiteCount: TextView
    private lateinit var siteAdapter: SiteAdapter
    private lateinit var siteList: ArrayList<SiteModel>
    private lateinit var firestore: FirebaseFirestore
    private var witelName: String = ""

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
        "INTEGRASI" to Color.GRAY
    )
    private val defaultMarkerColor = Color.RED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_witel_detail)

        // Get witel name from intent
        witelName = intent.getStringExtra("WITEL_NAME") ?: ""
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

        // Initialize MapBox
        mapView = findViewById(R.id.mapView)
        mapboxMap = mapView.getMapboxMap()

        // Set toolbar title to selected witel
        tvWitelTitle.text = witelName

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize site list with click listener to focus map on selected site
        siteList = arrayListOf()
        siteAdapter = SiteAdapter(siteList) { site ->
            // When a site is clicked, focus the map on its location
            focusMapOnSite(site)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = siteAdapter

        // Initialize MapBox style
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            // Initialize point annotation manager for markers
            initializePointAnnotationManager()

            // Load site data for the selected witel
            loadSitesForWitel()
        }
    }

    private fun initializePointAnnotationManager() {
        val annotationPlugin = mapView.annotations
        pointAnnotationManager = annotationPlugin.createPointAnnotationManager()
    }

    private fun loadSitesForWitel() {
        firestore.collection("projects")
            .whereEqualTo("witel", witelName)
            .get()
            .addOnSuccessListener { documents ->
                siteList.clear()
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

                // Update site count and refresh adapter
                tvSiteCount.text = "Total Sites: ${siteList.size}"
                siteAdapter.notifyDataSetChanged()

                // Focus map to show all markers if we have any
                if (sitePoints.isNotEmpty()) {
                    // You could focus on first site or calculate bounds to show all sites
                    focusMapOnSite(siteList[0])
                }

                // Show message if no sites found
                if (siteList.isEmpty()) {
                    showToast("No sites found for $witelName")
                }
            }
            .addOnFailureListener { e ->
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

        val point = Point.fromLngLat(coords.second, coords.first) // Note the order: lng, lat
        val cameraOptions = CameraOptions.Builder()
            .center(point)
            .zoom(14.0)
            .pitch(0.0)
            .bearing(0.0)
            .build()

        mapboxMap.setCamera(cameraOptions)

        // Highlight the item in the RecyclerView
        // This would require additional implementation
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}