package com.mbkm.telgo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class WitelDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBack: Button
    private lateinit var tvWitelTitle: TextView
    private lateinit var tvSiteCount: TextView
    private lateinit var siteAdapter: SiteAdapter
    private lateinit var siteList: ArrayList<SiteModel>
    private lateinit var firestore: FirebaseFirestore
    private var witelName: String = ""

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

        // Set toolbar title to selected witel
        tvWitelTitle.text = witelName

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize site list
        siteList = arrayListOf()
        siteAdapter = SiteAdapter(siteList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = siteAdapter

        // Load site data for the selected witel
        loadSitesForWitel()
    }

    private fun loadSitesForWitel() {
        firestore.collection("projects")
            .whereEqualTo("witel", witelName)
            .get()
            .addOnSuccessListener { documents ->
                siteList.clear()

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
                }

                // Update site count and refresh adapter
                tvSiteCount.text = "Total Sites: ${siteList.size}"
                siteAdapter.notifyDataSetChanged()

                // Show message if no sites found
                if (siteList.isEmpty()) {
                    showToast("No sites found for $witelName")
                }
            }
            .addOnFailureListener { e ->
                showToast("Error loading sites: ${e.message}")
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}