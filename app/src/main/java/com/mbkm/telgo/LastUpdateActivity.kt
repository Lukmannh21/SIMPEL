package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class LastUpdateActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBack: Button
    private lateinit var tvLastUpdateTitle: TextView
    private lateinit var tvTotalSites: TextView
    private lateinit var tvUploadedBy: TextView
    private lateinit var tvEditedSites: TextView
    private lateinit var siteAdapter: SiteAdapterLastUpdate
    private lateinit var siteList: ArrayList<SiteModelLastUpdate>
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_last_update)

        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerViewSites)
        btnBack = findViewById(R.id.btnBack)
        tvLastUpdateTitle = findViewById(R.id.tvLastUpdateTitle)
        tvTotalSites = findViewById(R.id.tvTotalSites)
        tvUploadedBy = findViewById(R.id.tvUploadedBy)
        tvEditedSites = findViewById(R.id.tvEditedSites)

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize site list
        siteList = arrayListOf()
        siteAdapter = SiteAdapterLastUpdate(siteList) { site ->
            // Handle item click
            val intent = Intent(this, SiteDetailActivity::class.java)
            intent.putExtra("SITE_ID", site.siteId)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = siteAdapter

        // Load site data for the last update
        loadSitesForLastUpdate()
    }

    private fun loadSitesForLastUpdate() {
        firestore.collection("projects")
            .orderBy("uploadedBy")
            .get()
            .addOnSuccessListener { documents ->
                siteList.clear()
                val uploadedByMap = mutableMapOf<String, MutableList<SiteModelLastUpdate>>()
                val editedSitesMap = mutableMapOf<String, MutableList<SiteModelLastUpdate>>()

                for (document in documents) {
                    val siteId = document.getString("siteId") ?: ""
                    val witel = document.getString("witel") ?: ""
                    val status = document.getString("status") ?: ""
                    val lastIssueHistory = document.get("lastIssueHistory") as? List<String>
                    val lastIssue = if (lastIssueHistory.isNullOrEmpty()) "" else lastIssueHistory[0]
                    val koordinat = document.getString("koordinat") ?: ""
                    val uploadedBy = document.getString("uploadedBy") ?: ""
                    val editedSites = document.getString("editedSites") ?: ""

                    val site = SiteModelLastUpdate(
                        siteId = siteId,
                        witel = witel,
                        status = status,
                        lastIssue = lastIssue,
                        koordinat = koordinat
                    )

                    if (!uploadedByMap.containsKey(uploadedBy)) {
                        uploadedByMap[uploadedBy] = mutableListOf()
                    }
                    uploadedByMap[uploadedBy]?.add(site)

                    if (!editedSitesMap.containsKey(editedSites)) {
                        editedSitesMap[editedSites] = mutableListOf()
                    }
                    editedSitesMap[editedSites]?.add(site)
                }

                // Update UI
                tvTotalSites.text = "Total Sites: ${siteList.size}"
                tvUploadedBy.text = "Uploaded By: ${uploadedByMap.keys.joinToString(", ")}"
                tvEditedSites.text = "Edited Sites: ${editedSitesMap.keys.joinToString(", ")}"

                // Add all sites to the list for the adapter
                uploadedByMap.values.flatten().forEach {
                    siteList.add(it)
                }

                siteAdapter.notifyDataSetChanged()

                // Show message if no sites found
                if (siteList.isEmpty()) {
                    showToast("No sites found for last update")
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