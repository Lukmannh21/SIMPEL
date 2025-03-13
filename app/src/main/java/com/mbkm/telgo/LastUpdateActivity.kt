package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class LastUpdateActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBack: Button
    private lateinit var tvLastUpdateTitle: TextView
    private lateinit var tvTotalSites: TextView
    private lateinit var tvUploadedBy: TextView
    private lateinit var tvEditedSites: TextView
    private lateinit var tvNoSites: TextView
    private lateinit var siteAdapter: SiteAdapter
    private lateinit var siteList: ArrayList<SiteModelLastUpdate>
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var currentUserEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_last_update)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Get current user
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("User not logged in")
            finish()
            return
        }

        currentUserEmail = currentUser.email ?: ""

        // Initialize UI components
        initializeUI()

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Initialize site list
        siteList = arrayListOf()
        siteAdapter = SiteAdapter(siteList) { site ->
            // Handle item click
            val intent = Intent(this, SiteDetailActivity::class.java)
            intent.putExtra("SITE_ID", site.siteId)
            intent.putExtra("WITEL", site.witel)
            startActivity(intent)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = siteAdapter

        // Display current user info
        tvUploadedBy.text = "User: $currentUserEmail"

        // Load user's updated/uploaded sites
        loadUserSites()
    }

    private fun initializeUI() {
        recyclerView = findViewById(R.id.recyclerViewSites)
        btnBack = findViewById(R.id.btnBack)
        tvLastUpdateTitle = findViewById(R.id.tvLastUpdateTitle)
        tvTotalSites = findViewById(R.id.tvTotalSites)
        tvUploadedBy = findViewById(R.id.tvUploadedBy)
        tvEditedSites = findViewById(R.id.tvEditedSites)
        tvNoSites = findViewById(R.id.tvNoSites)

        tvLastUpdateTitle.text = "My Site Updates"
        tvTotalSites.text = "Loading data..."
        tvEditedSites.text = "Recent Updates"
        tvNoSites.visibility = View.GONE
    }

    private fun loadUserSites() {
        // Pertama, dapatkan daftar siteId yang diupdate oleh user dari koleksi users
        firestore.collection("users")
            .whereEqualTo("email", currentUserEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    handleNoSites("User profile not found")
                    return@addOnSuccessListener
                }

                // Get user document
                val userDoc = documents.documents[0]

                // Get list of sites edited by user
                val editedSites = userDoc.get("editedSites") as? List<String> ?: listOf()

                if (editedSites.isEmpty()) {
                    tvEditedSites.text = "No edited sites found"
                    // Lanjutkan dengan memeriksa lastUpdatedBy/uploadedBy dalam project
                    loadSitesByUpdateField()
                    return@addOnSuccessListener
                }

                tvEditedSites.text = "Edited Sites: ${editedSites.size}"

                // Jika ada editedSites, ambil semua project yang memiliki siteId dalam daftar
                firestore.collection("projects")
                    .whereIn("siteId", editedSites)
                    .orderBy("updatedAt", Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { projectDocs ->
                        if (projectDocs.isEmpty) {
                            loadSitesByUpdateField() // Coba cara lain
                            return@addOnSuccessListener
                        }

                        processSiteDocuments(projectDocs.documents)
                    }
                    .addOnFailureListener { e ->
                        showToast("Error loading edited sites: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showToast("Error loading user data: ${e.message}")
                loadSitesByUpdateField() // Fallback ke metode lain jika gagal
            }
    }

    private fun loadSitesByUpdateField() {
        // Alternatif: Cari project berdasarkan lastUpdatedBy
        firestore.collection("projects")
            .whereEqualTo("lastUpdatedBy", currentUserEmail)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { projectDocs ->
                if (projectDocs.isEmpty) {
                    // Jika tidak ada, coba dengan uploadedBy
                    loadSitesByUploader()
                    return@addOnSuccessListener
                }

                processSiteDocuments(projectDocs.documents)
            }
            .addOnFailureListener { e ->
                showToast("Error loading updated sites: ${e.message}")
                loadSitesByUploader() // Fallback ke uploadedBy
            }
    }

    private fun loadSitesByUploader() {
        // Mencoba dengan field uploadedBy
        firestore.collection("projects")
            .whereEqualTo("uploadedBy", currentUserEmail)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { projectDocs ->
                if (projectDocs.isEmpty) {
                    handleNoSites("You haven't updated or uploaded any sites yet")
                    return@addOnSuccessListener
                }

                processSiteDocuments(projectDocs.documents)
            }
            .addOnFailureListener { e ->
                handleNoSites("Error: ${e.message}")
            }
    }

    private fun processSiteDocuments(documents: List<com.google.firebase.firestore.DocumentSnapshot>) {
        siteList.clear()

        for (document in documents) {
            val siteId = document.getString("siteId") ?: ""
            val witel = document.getString("witel") ?: ""
            val status = document.getString("status") ?: ""
            val lastIssueHistory = document.get("lastIssueHistory") as? List<String>
            val lastIssue = if (lastIssueHistory.isNullOrEmpty()) "" else lastIssueHistory[0]
            val koordinat = document.getString("koordinat") ?: ""
            val updatedAt = document.getString("updatedAt") ?: "Unknown"
            val lastUpdatedBy = document.getString("lastUpdatedBy")
                ?: document.getString("uploadedBy") ?: "Unknown"

            val site = SiteModelLastUpdate(
                siteId = siteId,
                witel = witel,
                status = status,
                lastIssue = lastIssue,
                koordinat = koordinat,
                updatedBy = lastUpdatedBy,
                updatedAt = updatedAt
            )

            siteList.add(site)
        }

        if (siteList.isEmpty()) {
            handleNoSites("No sites found")
        } else {
            tvTotalSites.text = "Total Sites: ${siteList.size}"
            tvNoSites.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            siteAdapter.notifyDataSetChanged()

            // Scroll to top to show newest update
            recyclerView.scrollToPosition(0)
        }
    }

    private fun handleNoSites(message: String) {
        tvTotalSites.text = "Total Sites: 0"
        tvNoSites.visibility = View.VISIBLE
        tvNoSites.text = message
        recyclerView.visibility = View.GONE
        showToast(message)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Inner adapter class
    private inner class SiteAdapter(
        private val siteList: ArrayList<SiteModelLastUpdate>,
        private val onItemClick: (SiteModelLastUpdate) -> Unit
    ) : RecyclerView.Adapter<SiteAdapter.SiteViewHolder>() {

        inner class SiteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvSiteId: TextView = itemView.findViewById(R.id.tvSiteId)
            val tvWitel: TextView = itemView.findViewById(R.id.tvWitel)
            val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
            val tvLastIssue: TextView = itemView.findViewById(R.id.tvLastIssue)
            val tvKoordinat: TextView = itemView.findViewById(R.id.tvKoordinat)
            val tvUpdatedAt: TextView = itemView.findViewById(R.id.tvUpdatedAt)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_last_update, parent, false)
            return SiteViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
            val currentItem = siteList[position]

            holder.tvSiteId.text = currentItem.siteId
            holder.tvWitel.text = currentItem.witel
            holder.tvStatus.text = "Status: ${currentItem.status}"

            // Format and display last issue
            val issue = if (currentItem.lastIssue.contains(" - ")) {
                val parts = currentItem.lastIssue.split(" - ", limit = 2)
                parts[1]
            } else {
                currentItem.lastIssue
            }
            holder.tvLastIssue.text = if (issue.isEmpty()) "No issues" else issue

            // Display koordinat
            holder.tvKoordinat.text = "Location: ${currentItem.koordinat}"

            // Format and display update time
            try {
                holder.tvUpdatedAt.text = "Updated: ${formatDateTime(currentItem.updatedAt)}"
            } catch (e: Exception) {
                // Jika tidak ada tvUpdatedAt di layout
            }

            // Set click listener for the entire item
            holder.itemView.setOnClickListener {
                onItemClick(currentItem)
            }
        }

        override fun getItemCount() = siteList.size

        private fun formatDateTime(dateTime: String): String {
            if (dateTime == "Unknown" || dateTime.isEmpty()) return "Unknown"

            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                val date = inputFormat.parse(dateTime) ?: return dateTime
                return outputFormat.format(date)
            } catch (e: Exception) {
                return dateTime
            }
        }
    }
}