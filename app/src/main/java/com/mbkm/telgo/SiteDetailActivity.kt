package com.mbkm.telgo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.concurrent.atomic.AtomicInteger
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.util.Locale

class SiteDetailActivity : AppCompatActivity() {

    // UI Components - Original
    private lateinit var tvSiteId: TextView
    private lateinit var tvWitel: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvLastIssue: TextView
    private lateinit var tvKoordinat: TextView
    private lateinit var btnEditData: Button
    private lateinit var btnBack: Button
    private lateinit var rvDocuments: RecyclerView
    private lateinit var rvImages: RecyclerView

    // To prevent duplicate data loading
    private var isDataInitialized = false

    // Additional UI Components
    private lateinit var tvIdLopOlt: TextView
    private lateinit var tvKodeSto: TextView
    private lateinit var tvNamaSto: TextView
    private lateinit var tvPortMetro: TextView
    private lateinit var tvSfp: TextView
    private lateinit var tvHostname: TextView
    private lateinit var tvSizeOlt: TextView
    private lateinit var tvPlatform: TextView
    private lateinit var tvType: TextView
    private lateinit var tvJmlModul: TextView
    private lateinit var tvSiteProvider: TextView
    private lateinit var tvKecamatanLokasi: TextView
    private lateinit var tvKodeIhld: TextView
    private lateinit var tvLopDownlink: TextView
    private lateinit var tvKontrakPengadaan: TextView
    private lateinit var tvToc: TextView
    private lateinit var tvStartProject: TextView
    private lateinit var tvCatuanAc: TextView
    private lateinit var tvKendala: TextView
    private lateinit var tvTglPlanOa: TextView
    private lateinit var tvWeekPlanOa: TextView
    private lateinit var tvDurasiPekerjaan: TextView
    private lateinit var tvOdp: TextView
    private lateinit var tvPort: TextView
    private lateinit var tvSisaHariThdpPlanOa: TextView
    private lateinit var tvSisaHariThdpToc: TextView

    // User verification status
    private var userStatus: String = "unverified"
    private var userRole: String = "user"

    private val REQUEST_STORAGE_PERMISSION = 200

    // Firebase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Data
    private lateinit var siteId: String
    private lateinit var witel: String

    // Map to store document direct URLs
    private val documentUrlMap = HashMap<String, String>()

    // Adapters
    private lateinit var documentsAdapter: DocumentsAdapter
    private lateinit var imagesAdapter: ImagesAdapter
    private var documentsList = ArrayList<DocumentModel>()
    private var imagesList = ArrayList<ImageModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_detail)

        // Get data from intent
        siteId = intent.getStringExtra("SITE_ID") ?: ""
        witel = intent.getStringExtra("WITEL") ?: ""

        if (siteId.isEmpty() || witel.isEmpty()) {
            showToast("Invalid site information")
            finish()
            return
        }

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Get user status from SharedPreferences
        val preferences = getSharedPreferences("TelGoPrefs", MODE_PRIVATE)
        userStatus = preferences.getString("userStatus", "unverified") ?: "unverified"
        userRole = preferences.getString("userRole", "user") ?: "user"

        // Initialize UI components
        initializeUI()

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Set up edit data button with verification check
        btnEditData.setOnClickListener {
            // Check if user is verified or an admin before allowing edit
            if (userStatus == "verified" || userRole == "admin") {
                val intent = Intent(this, EditSiteDataActivity::class.java)
                intent.putExtra("SITE_ID", siteId)
                intent.putExtra("WITEL", witel)
                startActivity(intent)
            } else {
                showVerificationRequiredDialog()
            }
        }

        // Check and request storage permissions
        checkAndRequestStoragePermission()

        // Set up RecyclerViews
        setupRecyclerViews()
    }

    private fun initializeUI() {
        // Original UI Components
        tvSiteId = findViewById(R.id.tvSiteId)
        tvWitel = findViewById(R.id.tvWitel)
        tvStatus = findViewById(R.id.tvStatus)
        tvLastIssue = findViewById(R.id.tvLastIssue)
        tvKoordinat = findViewById(R.id.tvKoordinat)
        btnEditData = findViewById(R.id.btnEditData)
        btnBack = findViewById(R.id.btnBack)
        rvDocuments = findViewById(R.id.rvDocuments)
        rvImages = findViewById(R.id.rvImages)

        // Additional UI Components
        tvIdLopOlt = findViewById(R.id.tvIdLopOlt)
        tvKodeSto = findViewById(R.id.tvKodeSto)
        tvNamaSto = findViewById(R.id.tvNamaSto)
        tvPortMetro = findViewById(R.id.tvPortMetro)
        tvSfp = findViewById(R.id.tvSfp)
        tvHostname = findViewById(R.id.tvHostname)
        tvSizeOlt = findViewById(R.id.tvSizeOlt)
        tvPlatform = findViewById(R.id.tvPlatform)
        tvType = findViewById(R.id.tvType)
        tvJmlModul = findViewById(R.id.tvJmlModul)
        tvSiteProvider = findViewById(R.id.tvSiteProvider)
        tvKecamatanLokasi = findViewById(R.id.tvKecamatanLokasi)
        tvKodeIhld = findViewById(R.id.tvKodeIhld)
        tvLopDownlink = findViewById(R.id.tvLopDownlink)
        tvKontrakPengadaan = findViewById(R.id.tvKontrakPengadaan)
        tvToc = findViewById(R.id.tvToc)
        tvStartProject = findViewById(R.id.tvStartProject)
        tvCatuanAc = findViewById(R.id.tvCatuanAc)
        tvKendala = findViewById(R.id.tvKendala)
        tvTglPlanOa = findViewById(R.id.tvTglPlanOa)
        tvWeekPlanOa = findViewById(R.id.tvWeekPlanOa)
        tvDurasiPekerjaan = findViewById(R.id.tvDurasiPekerjaan)
        tvOdp = findViewById(R.id.tvOdp)
        tvPort = findViewById(R.id.tvPort)
        tvSisaHariThdpPlanOa = findViewById(R.id.tvSisaHariThdpPlanOa)
        tvSisaHariThdpToc = findViewById(R.id.tvSisaHariThdpToc)
    }

    // Add verification required dialog method
    private fun showVerificationRequiredDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verifikasi Diperlukan")
            .setMessage("Akun Anda memerlukan verifikasi oleh administrator sebelum dapat mengedit data proyek. Ini memastikan kualitas dan keamanan data.")
            .setIcon(R.drawable.ic_image_error) // Make sure you have this icon
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Lihat Profil") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }

        val dialog = builder.create()
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
    }

    private fun checkAndRequestStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Android 10 and above, we don't need to ask for storage permission
            // for saving to Pictures/Downloads using MediaStore
            return true
        }

        // For older versions, we need WRITE_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with operation
                    showToast("Storage permission granted")
                } else {
                    showToast("Storage permission is required to save images")
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        // Set up Documents RecyclerView
        documentsAdapter = DocumentsAdapter(documentsList, documentUrlMap)
        rvDocuments.layoutManager = LinearLayoutManager(this)
        rvDocuments.adapter = documentsAdapter

        // Set up Images RecyclerView
        imagesAdapter =
            ImagesAdapter(imagesList, this)  // Pass activity context for permission checking
        rvImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvImages.adapter = imagesAdapter
    }

    private fun loadSiteData() {
        // Show loading indicator
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Get site details from Firestore
        firestore.collection("projects")
            .whereEqualTo("siteId", siteId)
            .whereEqualTo("witel", witel)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    loadingDialog.dismiss()
                    showToast("Site not found")
                    finish()
                    return@addOnSuccessListener
                }

                val document = documents.documents[0]
                val site = document.data

                // Set basic site information (original)
                tvSiteId.text = siteId
                tvWitel.text = witel
                tvStatus.text = site?.get("status")?.toString() ?: ""

                val lastIssueHistory = site?.get("lastIssueHistory") as? List<String>
                tvLastIssue.text =
                    if (lastIssueHistory.isNullOrEmpty()) "No issue reported" else lastIssueHistory[0]

                tvKoordinat.text = site?.get("koordinat")?.toString() ?: ""

                // Set additional site information
                tvIdLopOlt.text = site?.get("idLopOlt")?.toString() ?: ""
                tvKodeSto.text = site?.get("kodeSto")?.toString() ?: ""
                tvNamaSto.text = site?.get("namaSto")?.toString() ?: ""
                tvPortMetro.text = site?.get("portMetro")?.toString() ?: ""
                tvSfp.text = site?.get("sfp")?.toString() ?: ""
                tvHostname.text = site?.get("hostname")?.toString() ?: ""
                tvSizeOlt.text = site?.get("sizeOlt")?.toString() ?: ""
                tvPlatform.text = site?.get("platform")?.toString() ?: ""
                tvType.text = site?.get("type")?.toString() ?: ""
                tvJmlModul.text = site?.get("jmlModul")?.toString() ?: ""
                tvSiteProvider.text = site?.get("siteProvider")?.toString() ?: ""
                tvKecamatanLokasi.text = site?.get("kecamatanLokasi")?.toString() ?: ""
                tvKodeIhld.text = site?.get("kodeIhld")?.toString() ?: ""
                tvLopDownlink.text = site?.get("lopDownlink")?.toString() ?: ""
                tvKontrakPengadaan.text = site?.get("kontrakPengadaan")?.toString() ?: ""
                tvToc.text = site?.get("toc")?.toString() ?: ""
                tvStartProject.text = site?.get("startProject")?.toString() ?: ""
                tvCatuanAc.text = site?.get("catuanAc")?.toString() ?: ""
                tvKendala.text = site?.get("kendala")?.toString() ?: ""
                tvTglPlanOa.text = site?.get("tglPlanOa")?.toString() ?: ""
                tvWeekPlanOa.text = site?.get("weekPlanOa")?.toString() ?: ""
                tvDurasiPekerjaan.text = site?.get("durasiPekerjaan")?.toString() ?: ""
                tvOdp.text = site?.get("odp")?.toString() ?: ""
                tvPort.text = site?.get("port")?.toString() ?: ""
                tvSisaHariThdpPlanOa.text = site?.get("sisaHariThdpPlanOa")?.toString() ?: ""
                tvSisaHariThdpToc.text = site?.get("sisaHariThdpToc")?.toString() ?: ""

                // Set status color based on status value
                when (site?.get("status")?.toString()?.toLowerCase(Locale.ROOT)) {
                    "done" -> tvStatus.setBackgroundResource(R.drawable.status_badge_done)
                    "in progress" -> tvStatus.setBackgroundResource(R.drawable.status_badge_progress)
                    "pending" -> tvStatus.setBackgroundResource(R.drawable.status_badge_pending)
                    else -> tvStatus.setBackgroundResource(R.drawable.status_badge_background)
                }

                // Check for direct document BA URL from project
                val documentBAUrl = site?.get("documentBA")?.toString()
                if (!documentBAUrl.isNullOrEmpty()) {
                    // Found a direct link to a BA Survey document
                    documentUrlMap["document_ba"] = documentBAUrl

                    // Add to documents list for display
                    documentsList.add(DocumentModel(
                        name = "Document BA Survey",
                        type = "document_ba",
                        path = null,  // null path indicates to use direct URL instead
                        extension = "pdf",
                        mimeType = "application/pdf"
                    ))
                }

                // Load standard documents
                loadDocuments()

                loadCafDocuments()

                // Load images
                loadImages()

                loadingDialog.dismiss()
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                showToast("Error loading site: ${e.message}")
            }
    }

    private fun checkDocumentExists(docType: String, docName: String, pendingChecks: AtomicInteger) {
        val formats = listOf(
            "pdf" to "application/pdf",
            "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "doc" to "application/msword",
            "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "xls" to "application/vnd.ms-excel"
        )

        // Skip checking if this document type already exists in the directUrl map
        if (documentUrlMap.containsKey(docType)) {
            if (pendingChecks.decrementAndGet() == 0 && !isFinishing && !isDestroyed) {
                documentsAdapter.notifyDataSetChanged()
            }
            return
        }

        val checkQueue = AtomicInteger(formats.size)
        var documentFound = false

        for ((ext, mimeType) in formats) {
            val docRef = storage.reference.child("documents/$witel/$siteId/$docType.$ext")
            docRef.metadata
                .addOnSuccessListener {
                    if (!documentFound && !isFinishing && !isDestroyed) {
                        // Document found, add to list with correct format
                        documentFound = true
                        documentsList.add(DocumentModel(docName, docType, docRef.path, ext, mimeType))

                        // If document found in this format, no need to wait for other format checks
                        // for this document type, update adapter if all document types checked
                        if (pendingChecks.decrementAndGet() == 0 && !isFinishing && !isDestroyed) {
                            documentsAdapter.notifyDataSetChanged()
                        }
                    }
                }
                .addOnFailureListener {
                    // Format not found, continue to next format
                }
                .addOnCompleteListener {
                    // Decrement format check counter
                    if (checkQueue.decrementAndGet() == 0 && !documentFound) {
                        // All formats checked and no document found
                        if (!isFinishing && !isDestroyed && !documentUrlMap.containsKey(docType)) {
                            // Only add null document if we don't already have a direct URL
                            documentsList.add(DocumentModel(docName, docType, null, null, null))
                        }

                        // Decrement document type counter
                        if (pendingChecks.decrementAndGet() == 0 && !isFinishing && !isDestroyed) {
                            // All document types checked, update adapter
                            documentsAdapter.notifyDataSetChanged()
                        }
                    }
                }
        }
    }

    private fun loadDocuments() {
        // Clear list and notify adapter immediately
        documentsList.clear()
        documentsAdapter.notifyDataSetChanged()

        // Use AtomicInteger to track pending processes
        val pendingChecks = AtomicInteger(5) // 5 document types to check (including document_ba)

        // Define document types to check
        val documentTypes = listOf(
            "document_ba" to "Document BA",
            "email_order" to "Document Email Order",
            "telkomsel_permit" to "Document Telkomsel Permit",
            "mitra_tel" to "Document Mitra Tel",
            "daftar_mitra" to "Document Daftar Mitra"
        )

        for ((docType, docName) in documentTypes) {
            // Skip if we already have a direct URL for this document type
            if (documentUrlMap.containsKey(docType)) {
                // Create a document model for this direct URL
                documentsList.add(DocumentModel(
                    name = docName,
                    type = docType,
                    path = null,
                    extension = "pdf",
                    mimeType = "application/pdf"
                ))

                // Decrement counter
                if (pendingChecks.decrementAndGet() == 0 && !isFinishing && !isDestroyed) {
                    documentsAdapter.notifyDataSetChanged()
                }
                continue
            }

            // Check for various formats (PDF, Word, Excel)
            checkDocumentExists(docType, docName, pendingChecks)
        }
    }
    // Add this method to SiteDetailActivity.kt
    private fun loadCafDocuments() {
        // Check if we're already loading the site data
        if (isDataInitialized) return

        // Query the caf_applications collection for documents with matching siteId
        firestore.collection("caf_applications")
            .whereEqualTo("siteId", siteId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (doc in documents) {
                        // Get the PDF URL from the document
                        val pdfUrl = doc.getString("excelUrl") // Using excelUrl since that's where the PDF is stored
                        if (!pdfUrl.isNullOrEmpty()) {
                            // Get client name for better document identification
                            val clientName = doc.getString("client") ?: "Client"
                            val cafDate = doc.getString("todayDate") ?: ""

                            // Create a document name that includes identifiable information
                            val docName = if (cafDate.isNotEmpty()) {
                                "CAF - $clientName ($cafDate)"
                            } else {
                                "CAF - $clientName"
                            }

                            // Add to URL map for direct access
                            documentUrlMap["caf_${doc.id}"] = pdfUrl

                            // Add to documents list for display
                            documentsList.add(DocumentModel(
                                name = docName,
                                type = "caf_${doc.id}",
                                path = null,  // null path indicates to use direct URL
                                extension = "pdf",
                                mimeType = "application/pdf"
                            ))
                        }
                    }

                    // Notify the adapter if we found any documents
                    if (documents.size() > 0) {
                        documentsAdapter.notifyDataSetChanged()
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("SiteDetail", "Error loading CAF documents: ${e.message}")
            }
    }

    private fun loadImages() {
        // Clear list and notify adapter immediately
        imagesList.clear()
        imagesAdapter.notifyDataSetChanged()

        // Use AtomicInteger to track pending processes
        val pendingChecks = AtomicInteger(7) // 7 image types to check

        // Define image types to check
        val imageTypes = listOf(
            "site_location" to "Image Site Location",
            "foundation_shelter" to "Image Foundation/Shelter",
            "installation_process" to "Image Installation Process",
            "cabinet" to "Image Cabinet",
            "3p_inet" to "Image 3P (INET)",
            "3p_useetv" to "Image 3P UseeTV",
            "3p_telephone" to "Image 3P (Telephone)"
        )

        for ((imageType, imageName) in imageTypes) {
            val imageRef = storage.reference.child("images/$witel/$siteId/$imageType.jpg")
            imageRef.metadata
                .addOnSuccessListener {
                    if (!isFinishing && !isDestroyed) {
                        // Image exists
                        imagesList.add(ImageModel(imageName, imageType, imageRef.path))
                    }
                }
                .addOnFailureListener {
                    if (!isFinishing && !isDestroyed) {
                        // Image does not exist (null)
                        imagesList.add(ImageModel(imageName, imageType, null))
                    }
                }
                .addOnCompleteListener {
                    // Update adapter only after all checks complete
                    if (pendingChecks.decrementAndGet() == 0 && !isFinishing && !isDestroyed) {
                        imagesAdapter.notifyDataSetChanged()
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        if (::imagesAdapter.isInitialized) {
            (imagesAdapter as? ImagesAdapter)?.let { adapter ->
                adapter.onStart(this)
            }
        }
    }

    override fun onStop() {
        if (::imagesAdapter.isInitialized) {
            (imagesAdapter as? ImagesAdapter)?.let { adapter ->
                adapter.onStop()
            }
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Refresh user verification status
        val preferences = getSharedPreferences("TelGoPrefs", MODE_PRIVATE)
        userStatus = preferences.getString("userStatus", "unverified") ?: "unverified"
        userRole = preferences.getString("userRole", "user") ?: "user"

        // Only load data if not already loaded or when returning from other activity
        loadSiteData()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        // Cancel any Firebase callbacks if needed
        imagesList.clear()
        documentsList.clear()
        documentUrlMap.clear()
        super.onDestroy()
    }
}