package com.mbkm.telgo

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserDetailActivity : AppCompatActivity() {

    private lateinit var tvFullName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvNIK: TextView
    private lateinit var tvCompanyName: TextView
    private lateinit var tvUnit: TextView
    private lateinit var tvPosition: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvRegistrationDate: TextView
    private lateinit var tvLastLogin: TextView
    private lateinit var btnVerifyUser: Button
    private lateinit var btnBack: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var ivUserProfile: ImageView
    private lateinit var statusBadge: View
    private lateinit var statusText: TextView

    private lateinit var firestore: FirebaseFirestore
    private var userId: String = ""
    private var userStatus: String = "unverified"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_detail)

        // Apply enter animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        // Get user ID from intent
        userId = intent.getStringExtra("USER_ID") ?: ""
        userStatus = intent.getStringExtra("USER_STATUS") ?: "unverified"

        if (userId.isEmpty()) {
            Toast.makeText(this, "User ID not provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        tvFullName = findViewById(R.id.tvFullName)
        tvEmail = findViewById(R.id.tvEmail)
        tvNIK = findViewById(R.id.tvNIK)
        tvCompanyName = findViewById(R.id.tvCompanyName)
        tvUnit = findViewById(R.id.tvUnit)
        tvPosition = findViewById(R.id.tvPosition)
        tvPhone = findViewById(R.id.tvPhone)
        tvRegistrationDate = findViewById(R.id.tvRegistrationDate)
        tvLastLogin = findViewById(R.id.tvLastLogin)
        btnVerifyUser = findViewById(R.id.btnVerifyUser)
        btnBack = findViewById(R.id.btnBack)
        progressBar = findViewById(R.id.progressBar)
        ivUserProfile = findViewById(R.id.ivUserProfile)
        statusBadge = findViewById(R.id.statusBadge)
        statusText = findViewById(R.id.statusText)

        // Set up back button
        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        // Set up verification button based on current status
        setupVerifyButton()

        // Load user data
        loadUserData()
    }

    private fun setupVerifyButton() {
        if (userStatus == "verified") {
            btnVerifyUser.text = "Remove Verification"
            btnVerifyUser.backgroundTintList = ContextCompat.getColorStateList(this, R.color.red_telkomsel)

            btnVerifyUser.setOnClickListener {
                showConfirmationDialog(
                    title = "Remove Verification",
                    message = "Are you sure you want to remove verification for this user? They will lose access to upload and edit features.",
                    action = "Remove Verification",
                    onConfirm = { updateUserStatus("unverified") }
                )
            }
        } else {
            btnVerifyUser.text = "Verify User"
            btnVerifyUser.backgroundTintList = ContextCompat.getColorStateList(this, R.color.green)

            btnVerifyUser.setOnClickListener {
                showConfirmationDialog(
                    title = "Verify User",
                    message = "Are you sure you want to verify this user? They will gain access to upload and edit features.",
                    action = "Verify",
                    onConfirm = { updateUserStatus("verified") }
                )
            }
        }
    }

    private fun loadUserData() {
        progressBar.visibility = View.VISIBLE

        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                progressBar.visibility = View.GONE

                if (document != null && document.exists()) {
                    // Basic info
                    tvFullName.text = document.getString("fullName") ?: "N/A"
                    tvEmail.text = document.getString("email") ?: "N/A"

                    // Personal info
                    tvNIK.text = document.getString("nik")?.takeIf { it.isNotEmpty() } ?: "Not set"
                    tvCompanyName.text = document.getString("companyName")?.takeIf { it.isNotEmpty() } ?: "Not set"
                    tvUnit.text = document.getString("unit")?.takeIf { it.isNotEmpty() } ?: "Not set"
                    tvPosition.text = document.getString("position")?.takeIf { it.isNotEmpty() } ?: "Not set"
                    tvPhone.text = document.getString("phone")?.takeIf { it.isNotEmpty() } ?: "Not set"

                    // Dates
                    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

                    val regDate = document.getLong("registrationDate")
                    tvRegistrationDate.text = if (regDate != null && regDate > 0) {
                        dateFormat.format(Date(regDate))
                    } else {
                        "Unknown"
                    }

                    val loginDate = document.getLong("lastLoginDate")
                    tvLastLogin.text = if (loginDate != null && loginDate > 0) {
                        dateFormat.format(Date(loginDate))
                    } else {
                        "Never logged in"
                    }

                    // Status
                    val status = document.getString("status") ?: "unverified"
                    updateStatusUI(status)
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Error loading user: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun updateStatusUI(status: String) {
        userStatus = status

        // Update status indicator
        when (status) {
            "verified" -> {
                statusBadge.background = ContextCompat.getDrawable(this, R.drawable.bg_verified)
                statusText.text = "Verified"
                statusText.setTextColor(ContextCompat.getColor(this, R.color.green))
            }
            else -> {
                statusBadge.background = ContextCompat.getDrawable(this, R.drawable.bg_unverified)
                statusText.text = "Pending Verification"
                statusText.setTextColor(ContextCompat.getColor(this, R.color.orange))
            }
        }

        // Update verify button
        setupVerifyButton()
    }

    private fun updateUserStatus(newStatus: String) {
        progressBar.visibility = View.VISIBLE
        btnVerifyUser.isEnabled = false

        val update = hashMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to "2025-05-10 15:53:29", // Using the provided timestamp
            "lastUpdatedBy" to "Lukmannh21" // Using the provided username
        )

        firestore.collection("users").document(userId)
            .update(update)
            .addOnSuccessListener {
                progressBar.visibility = View.GONE
                btnVerifyUser.isEnabled = true

                val message = if (newStatus == "verified") {
                    "User successfully verified"
                } else {
                    "Verification removed from user"
                }

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                // Update UI
                updateStatusUI(newStatus)
            }
            .addOnFailureListener { e ->
                progressBar.visibility = View.GONE
                btnVerifyUser.isEnabled = true

                Toast.makeText(this, "Failed to update user status: ${e.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun showConfirmationDialog(
        title: String,
        message: String,
        action: String,
        onConfirm: () -> Unit
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(action) { dialog, _ ->
                dialog.dismiss()
                onConfirm()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}