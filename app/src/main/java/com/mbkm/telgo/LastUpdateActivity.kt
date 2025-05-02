package com.mbkm.telgo

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class LastUpdateActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBack: ImageButton
    private lateinit var tvLastUpdateTitle: TextView
    private lateinit var tvTotalSites: TextView
    private lateinit var tvUploadedBy: TextView
    private lateinit var tvEditedSites: TextView
    private lateinit var tvNoSites: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var emptyStateView: View
    private lateinit var btnRefresh: MaterialButton
    private lateinit var bottomNavigationView: BottomNavigationView

    private lateinit var siteAdapter: SiteAdapter
    private lateinit var siteList: ArrayList<SiteModelLastUpdate>
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var currentUserEmail: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_last_update)

        // Apply activity transition animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Get current user
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("User not logged in")
            redirectToLogin()
            return
        }

        currentUserEmail = currentUser.email ?: ""

        // Initialize UI components
        initializeUI()

        // Set up back button
        btnBack.setOnClickListener {
            finishWithAnimation()
        }

        // Set up refresh button in empty state
        btnRefresh.setOnClickListener {
            loadUserSites()
        }

        // Setup RecyclerView
        setupRecyclerView()

        // Setup SwipeRefreshLayout
        swipeRefreshLayout.setColorSchemeColors(
            ContextCompat.getColor(this, R.color.red_telkomsel_dark),
            ContextCompat.getColor(this, R.color.red_telkomsel_light)
        )
        swipeRefreshLayout.setOnRefreshListener {
            loadUserSites()
        }

        // Setup bottom navigation
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.selectedItemId = R.id.navigation_history

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
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        emptyStateView = findViewById(R.id.emptyStateView)
        btnRefresh = findViewById(R.id.btnRefresh)
        bottomNavigationView = findViewById(R.id.bottomNavigation)

        tvLastUpdateTitle.text = "My Site Updates"
        tvTotalSites.text = "0"
        tvEditedSites.text = "0"
        emptyStateView.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        siteList = arrayListOf()
        siteAdapter = SiteAdapter(siteList) { site ->
            // Handle item click
            val intent = Intent(this, LastUpdateDetailActivity::class.java)
            intent.putExtra("SITE_ID", site.siteId)
            intent.putExtra("WITEL", site.witel)

            // Create animation for transition
            val options = ActivityOptions.makeCustomAnimation(
                this,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            startActivity(intent, options.toBundle())
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = siteAdapter

        // Set layout animation for recycler view
        val animation = AnimationUtils.loadLayoutAnimation(this, R.anim.layout_animation_fall_down)
        recyclerView.layoutAnimation = animation
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_home -> {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                return true
            }
            R.id.navigation_services -> {
                val intent = Intent(this, ServicesActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return true
            }
            R.id.navigation_history -> {
                // We're already in LastUpdateActivity, no need to start a new activity
                return true
            }
            R.id.navigation_account -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        // Ensure the correct navigation item is selected
        bottomNavigationView.selectedItemId = R.id.navigation_history
    }

    override fun finish() {
        super.finish()
        // Apply exit animation
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun loadUserSites() {
        // Show loading indicator
        swipeRefreshLayout.isRefreshing = true
        emptyStateView.visibility = View.GONE

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
                    tvEditedSites.text = "0"
                    // Lanjutkan dengan memeriksa lastUpdatedBy/uploadedBy dalam project
                    loadSitesByUpdateField()
                    return@addOnSuccessListener
                }

                tvEditedSites.text = editedSites.size.toString()

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
                        swipeRefreshLayout.isRefreshing = false
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
                swipeRefreshLayout.isRefreshing = false
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
            tvTotalSites.text = siteList.size.toString()
            emptyStateView.visibility = View.GONE
            swipeRefreshLayout.visibility = View.VISIBLE

            // Apply animation to recyclerView
            recyclerView.scheduleLayoutAnimation()
            siteAdapter.notifyDataSetChanged()

            // Scroll to top to show newest update
            recyclerView.scrollToPosition(0)
        }

        // Hide loading indicator
        swipeRefreshLayout.isRefreshing = false
    }

    private fun handleNoSites(message: String) {
        tvTotalSites.text = "0"
        tvEditedSites.text = "0"
        emptyStateView.visibility = View.VISIBLE
        tvNoSites.text = message
        swipeRefreshLayout.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        finish()
    }

    private fun finishWithAnimation() {
        finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    @Override
    override fun onBackPressed() {
        super.onBackPressed()
        // Apply exit animation
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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
            val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
            val cardView: MaterialCardView = itemView as MaterialCardView
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

            // Set status indicator color
            when (currentItem.status.lowercase()) {
                "open" -> holder.statusIndicator.setBackgroundColor(Color.GREEN)
                "in progress" -> holder.statusIndicator.setBackgroundColor(Color.YELLOW)
                "closed" -> holder.statusIndicator.setBackgroundColor(Color.RED)
                else -> holder.statusIndicator.setBackgroundColor(Color.GRAY)
            }

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
                holder.tvUpdatedAt.text = "Updated: ${currentItem.updatedAt}"
            }

            // Add click animation
            holder.itemView.setOnClickListener {
                // Add animation effect
                it.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction {
                        it.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .withEndAction {
                                onItemClick(currentItem)
                            }
                            .start()
                    }
                    .start()
            }

            // Apply animation to each item
            val animation = AnimationUtils.loadAnimation(
                holder.itemView.context,
                R.anim.item_animation_fall_down
            )
            holder.itemView.startAnimation(animation)
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