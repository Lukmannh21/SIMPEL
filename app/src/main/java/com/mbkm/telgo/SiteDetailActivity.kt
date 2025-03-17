package com.mbkm.telgo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
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

    // buat meriksa data telah dimuat apa nggak biar dia gak dobel
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

    private val REQUEST_STORAGE_PERMISSION = 200

    // Firebase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Data
    private lateinit var siteId: String
    private lateinit var witel: String

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

        // Initialize UI components
        initializeUI()

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Set up edit data button
        btnEditData.setOnClickListener {
            val intent = Intent(this, EditSiteDataActivity::class.java)
            intent.putExtra("SITE_ID", siteId)
            intent.putExtra("WITEL", witel)
            startActivity(intent)
        }

        // Check and request storage permissions
        checkAndRequestStoragePermission()

        // Set up RecyclerViews
        setupRecyclerViews()

        // HAPUS pemanggilan loadSiteData() dari sini
        // loadSiteData() dipisahkan ke onResume() saja
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
        documentsAdapter = DocumentsAdapter(documentsList)
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
        // ...

        // Get site details from Firestore
        firestore.collection("projects")
            .whereEqualTo("siteId", siteId)
            .whereEqualTo("witel", witel)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
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

                // Load documents
                loadDocuments()

                // Load images
                loadImages()
            }
            .addOnFailureListener { e ->
                showToast("Error loading site: ${e.message}")
            }
    }

    // Tambahkan fungsi checkDocumentExists yang sebelumnya belum ditambahkan
    private fun checkDocumentExists(docType: String, docName: String, pendingChecks: AtomicInteger) {
        val formats = listOf(
            "pdf" to "application/pdf",
            "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "doc" to "application/msword",
            "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "xls" to "application/vnd.ms-excel"
        )

        val checkQueue = AtomicInteger(formats.size)
        var documentFound = false

        for ((ext, mimeType) in formats) {
            val docRef = storage.reference.child("documents/$witel/$siteId/$docType.$ext")
            docRef.metadata
                .addOnSuccessListener {
                    if (!documentFound && !isFinishing && !isDestroyed) {
                        // Dokumen ditemukan, tambahkan ke list dengan format yang benar
                        documentFound = true
                        documentsList.add(DocumentModel(docName, docType, docRef.path, ext, mimeType))

                        // Jika dokumen ditemukan dalam format ini, tidak perlu menunggu pengecekan format lain
                        // untuk jenis dokumen ini, update adapter jika semua jenis dokumen selesai dicek
                        if (pendingChecks.decrementAndGet() == 0 && !isFinishing && !isDestroyed) {
                            documentsAdapter.notifyDataSetChanged()
                        }
                    }
                }
                .addOnFailureListener {
                    // Format ini tidak ditemukan, lanjutkan ke format berikutnya
                }
                .addOnCompleteListener {
                    // Kurangi counter format yang sudah dicek
                    // Kurangi counter format yang sudah dicek
                    if (checkQueue.decrementAndGet() == 0 && !documentFound) {
                        // Semua format sudah dicek dan tidak ada dokumen yang ditemukan
                        if (!isFinishing && !isDestroyed) {
                            // Tambahkan dengan nilai null untuk menandakan dokumen tidak ada
                            documentsList.add(DocumentModel(docName, docType, null, null, null))
                        }

                        // Kurangi counter untuk jenis dokumen
                        if (pendingChecks.decrementAndGet() == 0 && !isFinishing && !isDestroyed) {
                            // Semua jenis dokumen sudah dicek, update adapter
                            documentsAdapter.notifyDataSetChanged()
                        }
                    }
                }
        }
    }

    private fun loadDocuments() {
        // Bersihkan list terlebih dahulu dan beritahu adapter segera
        documentsList.clear()
        documentsAdapter.notifyDataSetChanged()

        // Gunakan AtomicInteger untuk melacak proses yang sedang berjalan
        val pendingChecks = AtomicInteger(4) // 4 jenis dokumen yang akan diperiksa

        // Tentukan jenis-jenis dokumen yang akan diperiksa
        val documentTypes = listOf(
            "email_order" to "Document Email Order",
            "telkomsel_permit" to "Document Telkomsel Permit",
            "mitra_tel" to "Document Mitra Tel",
            "daftar_mitra" to "Document Daftar Mitra"
        )

        for ((docType, docName) in documentTypes) {
            // Check untuk berbagai format (PDF, Word, Excel)
            checkDocumentExists(docType, docName, pendingChecks)
        }
    }

    private fun loadImages() {
        // Bersihkan list terlebih dahulu dan beritahu adapter segera
        imagesList.clear()
        imagesAdapter.notifyDataSetChanged()

        // Gunakan AtomicInteger untuk melacak proses yang sedang berjalan
        val pendingChecks = AtomicInteger(7) // 7 jenis gambar yang akan diperiksa

        // Tentukan jenis-jenis gambar yang akan diperiksa
        val imageTypes = listOf(
            "site_location" to "Image Site Location",
            "foundation_shelter" to "Image Foundation/Shelter",
            "installation_process" to "Image Installation Process",
            "cabinet" to "Image Cabinet",
            "3p_inet" to "Image 3P (INET)",
            "3p_useetv" to "Image 3P UseeTV", // Nama yang diperbarui
            "3p_telephone" to "Image 3P (Telephone)"
        )

        for ((imageType, imageName) in imageTypes) {
            val imageRef = storage.reference.child("images/$witel/$siteId/$imageType.jpg")
            imageRef.metadata
                .addOnSuccessListener {
                    if (!isFinishing && !isDestroyed) {
                        // Gambar ada
                        imagesList.add(ImageModel(imageName, imageType, imageRef.path))
                    }
                }
                .addOnFailureListener {
                    if (!isFinishing && !isDestroyed) {
                        // Gambar tidak ada (null)
                        imagesList.add(ImageModel(imageName, imageType, null))
                    }
                }
                .addOnCompleteListener {
                    // Update adapter hanya setelah semua pemeriksaan selesai
                    if (pendingChecks.decrementAndGet() == 0 && !isFinishing && !isDestroyed) {
                        imagesAdapter.notifyDataSetChanged()
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        // If you've added the onStart method to ImagesAdapter as suggested earlier
        if (::imagesAdapter.isInitialized) {
            (imagesAdapter as? ImagesAdapter)?.let { adapter ->
                adapter.onStart(this)
            }
        }
    }

    override fun onStop() {
        // If you've added the onStop method to ImagesAdapter as suggested earlier
        if (::imagesAdapter.isInitialized) {
            (imagesAdapter as? ImagesAdapter)?.let { adapter ->
                adapter.onStop()
            }
        }
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Hanya muat data jika belum dimuat atau setelah kembali dari activity lain
        loadSiteData()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    // Clear any pending callbacks to prevent crashes when activity is destroyed
    override fun onDestroy() {
        // Cancel any Firebase callbacks if needed
        imagesList.clear()
        documentsList.clear()
        super.onDestroy()
    }
}

//