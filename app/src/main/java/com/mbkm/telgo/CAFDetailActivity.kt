package com.mbkm.telgo

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class CAFDetailActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // UI Components
    private lateinit var btnBack: ImageButton
    private lateinit var tvSiteId: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvClient: TextView
    private lateinit var tvSiteType: TextView
    private lateinit var tvTowerType: TextView
    private lateinit var tvTowerHeight: TextView
    private lateinit var tvCreatedDate: TextView
    private lateinit var tvCreatedBy: TextView
    private lateinit var btnDownloadExcel: Button

    private var cafId: String = ""
    private var excelUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caf_detail)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Get CAF ID from intent
        cafId = intent.getStringExtra("CAF_ID") ?: ""
        if (cafId.isEmpty()) {
            Toast.makeText(this, "Invalid CAF ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize UI components
        initializeUI()

        // Load CAF data
        loadCafData()
    }

    private fun initializeUI() {
        btnBack = findViewById(R.id.btnBack)
        tvSiteId = findViewById(R.id.tvSiteId)
        tvLocation = findViewById(R.id.tvLocation)
        tvClient = findViewById(R.id.tvClient)
        tvSiteType = findViewById(R.id.tvSiteType)
        tvTowerType = findViewById(R.id.tvTowerType)
        tvTowerHeight = findViewById(R.id.tvTowerHeight)
        tvCreatedDate = findViewById(R.id.tvCreatedDate)
        tvCreatedBy = findViewById(R.id.tvCreatedBy)
        btnDownloadExcel = findViewById(R.id.btnDownloadExcel)

        // Set button listeners
        btnBack.setOnClickListener {
            finish()
        }

        btnDownloadExcel.setOnClickListener {
            downloadExcelFile()
        }
    }

    private fun loadCafData() {
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Fetch CAF document from Firestore
        firestore.collection("caf_applications").document(cafId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data
                    if (data != null) {
                        // Set UI values
                        tvSiteId.text = data["siteId"] as? String ?: "N/A"
                        tvLocation.text = "${data["province"] as? String ?: ""}, ${data["city"] as? String ?: ""}"
                        tvClient.text = data["client"] as? String ?: "N/A"
                        tvSiteType.text = data["siteType"] as? String ?: "N/A"
                        tvTowerType.text = data["towerType"] as? String ?: "N/A"
                        tvTowerHeight.text = data["towerHeight"] as? String ?: "N/A"
                        tvCreatedDate.text = data["createdAt"] as? String ?: "N/A"
                        tvCreatedBy.text = data["createdBy"] as? String ?: "N/A"

                        // Store Excel URL for download
                        excelUrl = data["excelUrl"] as? String ?: ""
                        btnDownloadExcel.isEnabled = excelUrl.isNotEmpty()
                    }
                } else {
                    Toast.makeText(this, "CAF not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
                loadingDialog.dismiss()
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Error loading CAF: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun downloadExcelFile() {
        if (excelUrl.isEmpty()) {
            Toast.makeText(this, "Excel file not available", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Create local file
        val siteId = tvSiteId.text.toString().replace(" ", "_")
        val fileName = "CAF_${siteId}.xls"
        val localFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        // Download Excel
        storage.getReferenceFromUrl(excelUrl)
            .getFile(localFile)
            .addOnSuccessListener {
                loadingDialog.dismiss()

                // Open the Excel file
                val uri = FileProvider.getUriForFile(
                    this,
                    "com.mbkm.telgo.fileprovider",
                    localFile
                )

                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, "application/vnd.ms-excel")
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

                startActivity(Intent.createChooser(intent, "Open Excel with..."))
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Error downloading Excel: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}