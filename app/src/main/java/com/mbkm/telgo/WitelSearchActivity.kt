package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * WitelSearchActivity displays a list of available Witel regions
 *
 * Last Updated: 2025-04-29 02:39:29 UTC
 * Updated By: Lukmannh21
 */
class WitelSearchActivity : AppCompatActivity() {

    // UI Components
    private lateinit var rvWitels: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: ImageButton
    private lateinit var btnBackToHome: ImageButton
    private lateinit var tvNoResults: TextView
    private lateinit var tvWitelCount: TextView


    // Adapter and Data
    private lateinit var witelAdapter: WitelAdapter
    private lateinit var witelList: ArrayList<WitelModel>
    private lateinit var filteredList: ArrayList<WitelModel>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_witel_search)

        // Initialize UI components - using generic IDs that should exist in your layout
        rvWitels = findViewById(R.id.rvWitels)
        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        btnBackToHome = findViewById<ImageButton>(R.id.btnBackToHome)
        tvNoResults = findViewById(R.id.tvNoResults)
        tvWitelCount = findViewById(R.id.tvWitelCount)


        // Animations
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left)

        // Apply animations
        rvWitels.startAnimation(fadeIn)

        // Set up back button
        btnBackToHome.setOnClickListener {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Initialize witel list with data
        initializeWitelList()

        // Set up RecyclerView
        filteredList = ArrayList(witelList)
        witelAdapter = WitelAdapter(filteredList) { witel ->
            // Handle witel item click - navigate to WitelDetailActivity with animation
            val intent = Intent(this, WitelDetailActivity::class.java)
            intent.putExtra("WITEL_NAME", witel.name)
            // Menambahkan koordinat provinsi ke intent
            intent.putExtra("PROVINCE_LAT", witel.provinceCoordinates.first)
            intent.putExtra("PROVINCE_LON", witel.provinceCoordinates.second)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        // Improved layout manager with smooth scrolling
        val layoutManager = LinearLayoutManager(this)
        rvWitels.layoutManager = layoutManager
        rvWitels.adapter = witelAdapter

        // Add custom item animator
        rvWitels.itemAnimator = WitelItemAnimator()

        // Set up search functionality with animation
        btnSearch.setOnClickListener {
            val query = etSearch.text.toString().trim()
            filterWitelList(query)

            // Animate the recycler view when searching
            rvWitels.startAnimation(slideUp)
        }



        // Update Witel count text
        updateWitelCount()

        // Show all witels initially
        updateResultsVisibility()
    }



    private fun updateWitelCount() {
        tvWitelCount.text = "${filteredList.size} Witel Tersedia"
    }

    private fun initializeWitelList() {
        witelList = arrayListOf(
            WitelModel(
                "ACEH",
                "Jl. Sultan Iskandar Muda No.18, Banda Aceh",
                Pair(5.548290, 95.323753), // Koordinat pusat provinsi Aceh
                R.drawable.witel_aceh
            ),
            // Rest of your data remains the same
            WitelModel(
                "BABEL",
                "Jl. Jenderal Sudirman No.105, Pangkalpinang",
                Pair(-2.131627, 106.116959), // Koordinat pusat provinsi Bangka Belitung
                R.drawable.witel_babel
            ),
            WitelModel(
                "BENGKULU",
                "Jl. Pembangunan No.38, Bengkulu",
                Pair(-3.792860, 102.260765), // Koordinat pusat provinsi Bengkulu
                R.drawable.witel_bengkulu
            ),
            WitelModel(
                "JAMBI",
                "Jl. Jenderal Sudirman No.55, Jambi",
                Pair(-1.609972, 103.607254), // Koordinat pusat provinsi Jambi
                R.drawable.witel_jambi
            ),
            WitelModel(
                "LAMPUNG",
                "Jl. Wolter Monginsidi No.12, Bandar Lampung",
                Pair(-4.558625, 105.406581), // Koordinat pusat provinsi Lampung
                R.drawable.witel_lampung
            ),
            WitelModel(
                "RIDAR",
                "Jl. Jenderal Sudirman No.199, Pekanbaru",
                Pair(0.510440, 101.448311), // Koordinat pusat provinsi Riau Daratan
                R.drawable.witel_ridar
            ),
            WitelModel(
                "RIKEP",
                "Jl. Diponegoro No.87, Tanjung Pinang",
                Pair(3.945308, 108.142090), // Koordinat pusat provinsi Riau Kepulauan
                R.drawable.witel_rikep
            ),
            WitelModel(
                "SUMBAR",
                "Jl. Khatib Sulaiman No.1, Padang",
                Pair(-0.739981, 100.800005), // Koordinat pusat provinsi Sumatera Barat
                R.drawable.witel_sumbar
            ),
            WitelModel(
                "SUMSEL",
                "Jl. Jenderal Sudirman No.459, Palembang",
                Pair(-3.319464, 103.914520), // Koordinat pusat provinsi Sumatera Selatan
                R.drawable.witel_sumsel
            ),
            WitelModel(
                "SUMUT",
                "Jl. Prof. HM Yamin No.13, Medan",
                Pair(2.192862, 99.378546), // Koordinat pusat provinsi Sumatera Utara
                R.drawable.witel_sumut
            )
        )
    }

    private fun filterWitelList(query: String) {
        filteredList.clear()

        if (query.isEmpty()) {
            filteredList.addAll(witelList)
        } else {
            val search = query.lowercase()
            for (witel in witelList) {
                if (witel.name.lowercase().contains(search) ||
                    witel.address.lowercase().contains(search)) {
                    filteredList.add(witel)
                }
            }
        }

        // Update the count text
        updateWitelCount()

        // Notify with animation
        witelAdapter.notifyDataSetChanged()
        updateResultsVisibility()
    }

    private fun updateResultsVisibility() {
        if (filteredList.isEmpty()) {
            rvWitels.visibility = View.GONE
            tvNoResults.visibility = View.VISIBLE
            tvNoResults.text = "Tidak ditemukan Witel yang sesuai dengan pencarian Anda"

            // Animate no results text
            tvNoResults.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in))
        } else {
            rvWitels.visibility = View.VISIBLE
            tvNoResults.visibility = View.GONE
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Handle back button press with animation
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}