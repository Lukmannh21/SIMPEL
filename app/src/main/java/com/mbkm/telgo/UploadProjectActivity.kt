package com.mbkm.telgo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UploadProjectActivity : AppCompatActivity() {

    private lateinit var witelDropdown: AutoCompleteTextView
    private lateinit var siteIdInput: EditText
    private lateinit var statusDropdown: AutoCompleteTextView
    private lateinit var lastIssueInput: EditText
    private lateinit var koordinatInput: EditText
    private lateinit var btnCurrentLocation: Button
    private lateinit var btnAddData: Button
    private lateinit var btnBack: Button

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Menggunakan FusedLocationProviderClient seperti di MainActivity
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // Witel options
    private val witelOptions = listOf(
        "ACEH", "BABEL", "BENGKULU", "JAMBI", "LAMPUNG",
        "RIDAR", "RIKEP", "SUMBAR", "SUMSEL", "SUMUT"
    )

    // Status options
    private val statusOptions = listOf(
        "OA", "MAT DEL", "DONE", "SURVEY", "POWER ON",
        "DROP", "MOS", "INTEGRASI", "SURVEY"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_project)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize UI components
        witelDropdown = findViewById(R.id.witelDropdown)
        siteIdInput = findViewById(R.id.siteIdInput)
        statusDropdown = findViewById(R.id.statusDropdown)
        lastIssueInput = findViewById(R.id.lastIssueInput)
        koordinatInput = findViewById(R.id.koordinatInput)
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation)
        btnAddData = findViewById(R.id.btnAddData)
        btnBack = findViewById(R.id.btnBack)

        // Initialize FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up witel dropdown
        val witelAdapter = ArrayAdapter(this, R.layout.dropdown_item, witelOptions)
        witelDropdown.setAdapter(witelAdapter)

        // Set up status dropdown
        val statusAdapter = ArrayAdapter(this, R.layout.dropdown_item, statusOptions)
        statusDropdown.setAdapter(statusAdapter)

        // Set up current location button
        btnCurrentLocation.setOnClickListener {
            if (checkLocationPermission()) {
                getCurrentLocation()
            }
        }

        // Set up add data button
        btnAddData.setOnClickListener {
            validateAndProceed()
        }

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun validateAndProceed() {
        val witel = witelDropdown.text.toString()
        val siteId = siteIdInput.text.toString().trim()
        val status = statusDropdown.text.toString()
        val lastIssue = lastIssueInput.text.toString().trim()
        val koordinat = koordinatInput.text.toString().trim()

        // Validate inputs
        if (witel.isEmpty() || !witelOptions.contains(witel)) {
            showToast("Silakan pilih Witel")
            return
        }

        if (siteId.isEmpty()) {
            showToast("Site ID Location tidak boleh kosong")
            return
        }

        if (status.isEmpty() || !statusOptions.contains(status)) {
            showToast("Silakan pilih Status")
            return
        }

        if (lastIssue.isEmpty()) {
            showToast("Last Issue tidak boleh kosong")
            return
        }

        if (koordinat.isEmpty()) {
            showToast("Koordinat tidak boleh kosong")
            return
        }

        // Check if site ID already exists in database
        checkSiteIdExists(siteId, witel, status, lastIssue, koordinat)
    }

    private fun checkSiteIdExists(siteId: String, witel: String, status: String, lastIssue: String, koordinat: String) {
        firestore.collection("projects")
            .whereEqualTo("siteId", siteId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Site ID doesn't exist, show confirmation dialog for new data
                    showConfirmationDialog(
                        siteId = siteId,
                        witel = witel,
                        status = status,
                        lastIssue = lastIssue,
                        koordinat = koordinat,
                        isNewProject = true
                    )
                } else {
                    // Site ID exists, show edit dialog
                    val existingProject = documents.documents[0].data
                    showConfirmationDialog(
                        siteId = siteId,
                        witel = witel,
                        status = status,
                        lastIssue = lastIssue,
                        koordinat = koordinat,
                        isNewProject = false,
                        existingProjectId = documents.documents[0].id
                    )
                }
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
            }
    }

    private fun showConfirmationDialog(
        siteId: String,
        witel: String,
        status: String,
        lastIssue: String,
        koordinat: String,
        isNewProject: Boolean,
        existingProjectId: String = ""
    ) {
        val title = if (isNewProject) "Konfirmasi Data Baru" else "Edit Data Projek"
        val message = """
            Witel: $witel
            Site ID: $siteId
            Status: $status
            Last Issue: $lastIssue
            Koordinat: $koordinat
            
            ${if (isNewProject) "Apakah data di atas sudah benar?" else "Anda akan mengedit data yang sudah ada. Lanjutkan?"}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Submit") { _, _ ->
                if (isNewProject) {
                    saveNewProject(siteId, witel, status, lastIssue, koordinat)
                } else {
                    updateExistingProject(existingProjectId, siteId, witel, status, lastIssue, koordinat)
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun saveNewProject(siteId: String, witel: String, status: String, lastIssue: String, koordinat: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Anda harus login terlebih dahulu")
            return
        }

        val currentTime = getCurrentDateTime()
        val issueWithTimestamp = "$currentTime - $lastIssue"

        val projectData = hashMapOf(
            "siteId" to siteId,
            "witel" to witel,
            "status" to status,
            "lastIssueHistory" to listOf(issueWithTimestamp),
            "koordinat" to koordinat,
            "uploadedBy" to currentUser.email,
            "createdAt" to currentTime,
            "updatedAt" to currentTime
        )

        firestore.collection("projects")
            .add(projectData)
            .addOnSuccessListener {
                showToast("Data berhasil disimpan")
                finish()
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
            }
    }

    private fun updateExistingProject(
        projectId: String,
        siteId: String,
        witel: String,
        status: String,
        lastIssue: String,
        koordinat: String
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Anda harus login terlebih dahulu")
            return
        }

        val currentTime = getCurrentDateTime()
        val issueWithTimestamp = "$currentTime - $lastIssue"

        // Get existing project to append to lastIssueHistory
        firestore.collection("projects").document(projectId)
            .get()
            .addOnSuccessListener { document ->
                val existingData = document.data
                val existingIssueHistory = existingData?.get("lastIssueHistory") as? List<String> ?: listOf()

                // Create updated issue history with new issue at the beginning (most recent)
                val updatedIssueHistory = mutableListOf(issueWithTimestamp)
                updatedIssueHistory.addAll(existingIssueHistory)

                // Update project
                val updateData = hashMapOf(
                    "witel" to witel,
                    "status" to status,
                    "lastIssueHistory" to updatedIssueHistory,
                    "koordinat" to koordinat,
                    "updatedAt" to currentTime
                )

                firestore.collection("projects").document(projectId)
                    .set(updateData, SetOptions.merge())
                    .addOnSuccessListener {
                        showToast("Data berhasil diperbarui")
                        finish()
                    }
                    .addOnFailureListener { e ->
                        showToast("Error: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
            }
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Menggunakan metode lokasi seperti di MainActivity
    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show explanation dialog if needed
                AlertDialog.Builder(this)
                    .setTitle("Izin Lokasi Dibutuhkan")
                    .setMessage("Aplikasi membutuhkan izin untuk mengakses lokasi Anda")
                    .setPositiveButton("OK") { _, _ ->
                        requestLocationPermission()
                    }
                    .create()
                    .show()
            } else {
                // No explanation needed, request the permission
                requestLocationPermission()
            }
            return false
        }
        return true
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (checkLocationPermission()) {
            showToast("Mencari lokasi...")
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        val koordinatText = "$latitude, $longitude"

                        koordinatInput.setText(koordinatText)
                        showToast("Lokasi didapatkan")
                    } else {
                        showToast("Gagal mendapatkan lokasi. Pastikan GPS aktif.")
                    }
                }
                .addOnFailureListener { e ->
                    showToast("Gagal mendapatkan lokasi: ${e.message}")
                }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation()
                } else {
                    showToast("Izin lokasi tidak diberikan")
                }
            }
        }
    }
}