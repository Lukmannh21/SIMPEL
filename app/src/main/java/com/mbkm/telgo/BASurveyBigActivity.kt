package com.mbkm.telgo

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.widget.Button
import java.io.FileNotFoundException


// Jika Anda belum punya data class seperti ini, buatlah
// Sesuaikan field lain jika perlu


class BASurveyBigActivity : AppCompatActivity() {

    // Firebase instances
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // UI Components
    private lateinit var tabLayout: TabLayout
    private lateinit var formContainer: View
    private lateinit var searchContainer: View
    private lateinit var searchView: SearchView
    private lateinit var rvSearchResults: RecyclerView

    // Input fields
    private lateinit var inputProjectTitle: EditText
    private lateinit var inputContractNumber: EditText
    private lateinit var inputExecutor: Spinner
    private lateinit var inputLocation: EditText
    private lateinit var inputDescription: EditText
    private lateinit var etTselRegion: EditText

    // Signature components
    private lateinit var etZteName: EditText
    private lateinit var etZteNik: EditText
    private lateinit var imgZteSignature: ImageView
    private lateinit var btnZteSignature: Button

    private lateinit var etTifName: EditText
    private lateinit var etTifNik: EditText
    private lateinit var imgTifSignature: ImageView
    private lateinit var btnTifSignature: Button

    private lateinit var etTelkomName: EditText
    private lateinit var etTelkomNik: EditText
    private lateinit var imgTelkomSignature: ImageView
    private lateinit var btnTelkomSignature: Button

    private lateinit var etTselNopName: EditText
    private lateinit var etTselNopNik: EditText
    private lateinit var imgTselNopSignature: ImageView
    private lateinit var btnTselNopSignature: Button

    private lateinit var etTselRtpdsName: EditText
    private lateinit var etTselRtpdsNik: EditText
    private lateinit var imgTselRtpdsSignature: ImageView
    private lateinit var btnTselRtpdsSignature: Button

    private lateinit var etTselRtpeNfName: EditText
    private lateinit var etTselRtpeNfNik: EditText
    private lateinit var imgTselRtpeNfSignature: ImageView
    private lateinit var btnTselRtpeNfSignature: Button

    private lateinit var btnGeneratePdf: Button
    private lateinit var btnSubmitForm: Button

    // Permissions
    private val CAMERA_PERMISSION_CODE = 100
    private val STORAGE_PERMISSION_CODE = 101

    // Temporary file for camera
    private var tempPhotoUri: Uri? = null
    private var currentImageView: ImageView? = null

    // Data for search
    private var surveyList = mutableListOf<SurveyData>()
    private lateinit var searchAdapter: SurveyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basurvey_big)

        // Request storage permissions for Android < 10 (API 29)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_CODE
            )
        }

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI components
        initializeUI()

        // Setup TabLayout
        setupTabs()

        // Setup Search
        setupSearch()

        // Setup back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Check and request permissions
        checkAndRequestPermissions()
    }

    private fun initializeUI() {
        // Initialize tabs and containers
        tabLayout = findViewById(R.id.tabLayout)
        formContainer = findViewById(R.id.formContainer)
        searchContainer = findViewById(R.id.searchContainer)
        searchView = findViewById(R.id.searchView)
        tabLayout = findViewById(R.id.tabLayout)
        formContainer = findViewById(R.id.formContainer)
        searchContainer = findViewById(R.id.searchContainer)
        rvSearchResults = findViewById(R.id.rvSearchResults)

        // Initialize form fields
        inputProjectTitle = findViewById(R.id.inputProjectTitle)
        inputContractNumber = findViewById(R.id.inputContractNumber)
        inputExecutor = findViewById(R.id.inputExecutor)
        inputLocation = findViewById(R.id.inputLocation)
        inputDescription = findViewById(R.id.inputDescription)
        etTselRegion = findViewById(R.id.etTselRegion)

        // Initialize buttons
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf)

        btnSubmitForm = findViewById(R.id.btnSubmitForm)

        // Setup executor spinner
        val executors = arrayOf("PT. ZTE INDONESIA", "PT Huawei Tech Investment")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, executors)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        inputExecutor.adapter = adapter

        // Initialize signature fields and buttons
        etZteName = findViewById(R.id.etZteName)
        etZteNik = findViewById(R.id.etZteNik)
        imgZteSignature = findViewById(R.id.imgZteSignature)
        btnZteSignature = findViewById(R.id.btnZteSignature)

        etTifName = findViewById(R.id.etTifName)
        etTifNik = findViewById(R.id.etTifNik)
        imgTifSignature = findViewById(R.id.imgTifSignature)
        btnTifSignature = findViewById(R.id.btnTifSignature)

        etTelkomName = findViewById(R.id.etTelkomName)
        etTelkomNik = findViewById(R.id.etTelkomNik)
        imgTelkomSignature = findViewById(R.id.imgTelkomSignature)
        btnTelkomSignature = findViewById(R.id.btnTelkomSignature)

        etTselNopName = findViewById(R.id.etTselNopName)
        etTselNopNik = findViewById(R.id.etTselNopNik)
        imgTselNopSignature = findViewById(R.id.imgTselNopSignature)
        btnTselNopSignature = findViewById(R.id.btnTselNopSignature)

        etTselRtpdsName = findViewById(R.id.etTselRtpdsName)
        etTselRtpdsNik = findViewById(R.id.etTselRtpdsNik)
        imgTselRtpdsSignature = findViewById(R.id.imgTselRtpdsSignature)
        btnTselRtpdsSignature = findViewById(R.id.btnTselRtpdsSignature)

        etTselRtpeNfName = findViewById(R.id.etTselRtpeNfName)
        etTselRtpeNfNik = findViewById(R.id.etTselRtpeNfNik)
        imgTselRtpeNfSignature = findViewById(R.id.imgTselRtpeNfSignature)
        btnTselRtpeNfSignature = findViewById(R.id.btnTselRtpeNfSignature)

        btnGeneratePdf = findViewById(R.id.btnGeneratePdf)
        btnSubmitForm = findViewById(R.id.btnSubmitForm)

        // Setup signature button listeners
        setupSignatureButtons()

        // Setup generate PDF and submit form buttons
        btnGeneratePdf.setOnClickListener {
            if (validateForm()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        generateStyledPdf() // Panggil fungsi untuk generate PDF
                    } catch (e: Exception) {
                        Log.e("BASurveyBig", "Error generating PDF: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@BASurveyBigActivity,
                                "Gagal membuat PDF: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        btnSubmitForm.setOnClickListener {
            if (validateForm()) {
                submitForm()
            }
        }
    }

    private fun setupTabs() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { // Form tab
                        formContainer.visibility = View.VISIBLE
                        searchContainer.visibility = View.GONE
                    }
                    1 -> { // Search tab
                        formContainer.visibility = View.GONE
                        searchContainer.visibility = View.VISIBLE
                        loadSurveyData()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearch() {
        // Initialize RecyclerView
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        searchAdapter = SurveyAdapter(surveyList) { baSurvey ->
            // Handle item click - load data into form
            showSurveyOptionsDialog(baSurvey)
            tabLayout.getTabAt(0)?.select() // Switch to form tab
        }
        rvSearchResults.adapter = searchAdapter

        // Setup SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterResults(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterResults(newText)
                return false
            }
        })
    }

    private fun filterResults(query: String?) {
        if (query.isNullOrBlank()) {
            searchAdapter.updateData(surveyList)
            return
        }

        val filteredList = surveyList.filter {
            it.location.contains(query, ignoreCase = true) ||
                    it.projectTitle.contains(query, ignoreCase = true) ||
                    it.contractNumber.contains(query, ignoreCase = true)
        }
        searchAdapter.updateData(filteredList)
    }

    private fun loadSurveyData() {
        // Show loading UI
        val loadingView = findViewById<View>(R.id.loadingEventsShimmer)
        val emptyView = findViewById<View>(R.id.emptyEventsView)

        loadingView.visibility = View.VISIBLE
        rvSearchResults.visibility = View.GONE
        emptyView.visibility = View.GONE

        db.collection("big_surveys")
            .get()
            .addOnSuccessListener { documents ->
                surveyList.clear()
                for (document in documents) {
                    val data = document.data
                    val survey = SurveyData(
                        id = document.id,
                        projectTitle = data["projectTitle"] as? String ?: "",
                        contractNumber = data["contractNumber"] as? String ?: "",
                        executor = data["executor"] as? String ?: "",
                        location = data["location"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        createdAt = data["createdAt"] as? Long ?: 0,
                        pdfUrl = data["pdfUrl"] as? String // << AMBIL PDF URL DI SINI
                    )
                    surveyList.add(survey)
                }

                surveyList.sortByDescending { it.createdAt }

                loadingView.visibility = View.GONE
                if (surveyList.isEmpty()) {
                    emptyView.visibility = View.VISIBLE
                    rvSearchResults.visibility = View.GONE
                } else {
                    emptyView.visibility = View.GONE
                    rvSearchResults.visibility = View.VISIBLE
                    searchAdapter.updateData(surveyList) // Pastikan searchAdapter.updateData bisa menerima List<SurveyData>
                }
            }
            .addOnFailureListener { e ->
                Log.w("BASurveyBig", "Error loading data", e)
                loadingView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                rvSearchResults.visibility = View.GONE
                Toast.makeText(this, "Failed to load data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadSurveyIntoForm(survey: SurveyData) {
        inputProjectTitle.setText(survey.projectTitle)
        inputContractNumber.setText(survey.contractNumber)
        inputLocation.setText(survey.location)
        inputDescription.setText(survey.description)

        // Set executor spinner
        val executorAdapter = inputExecutor.adapter as ArrayAdapter<String>
        val position = (0 until executorAdapter.count).find {
            executorAdapter.getItem(it) == survey.executor
        } ?: 0
        inputExecutor.setSelection(position)

        // Load additional data
        db.collection("big_surveys").document(survey.id)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    // Load all form field values
                    val data = document.data ?: return@addOnSuccessListener

                    // Load signatures and other fields
                    etTselRegion.setText(data["tselRegion"] as? String ?: "")

                    // Load actual and remark fields
                    loadFieldIfExists(data, "actual1", findViewById(R.id.inputAktual1))
                    loadFieldIfExists(data, "remark1", findViewById(R.id.inputKeterangan1))
                    loadFieldIfExists(data, "actual2", findViewById(R.id.inputAktual2))
                    loadFieldIfExists(data, "remark2", findViewById(R.id.inputKeterangan2))
                    loadFieldIfExists(data, "actual3", findViewById(R.id.inputAktual3))
                    loadFieldIfExists(data, "remark3", findViewById(R.id.inputKeterangan3))
                    loadFieldIfExists(data, "actual4", findViewById(R.id.inputAktual4))
                    loadFieldIfExists(data, "remark4", findViewById(R.id.inputKeterangan4))
                    loadFieldIfExists(data, "actual5", findViewById(R.id.inputAktual5))
                    loadFieldIfExists(data, "remark5", findViewById(R.id.inputKeterangan5))
                    loadFieldIfExists(data, "actual6", findViewById(R.id.inputAktual6))
                    loadFieldIfExists(data, "remark6", findViewById(R.id.inputKeterangan6))
                    loadFieldIfExists(data, "actual7", findViewById(R.id.inputAktual7))
                    loadFieldIfExists(data, "remark7", findViewById(R.id.inputKeterangan7))
                    loadFieldIfExists(data, "actual8", findViewById(R.id.inputAktual8))
                    loadFieldIfExists(data, "remark8", findViewById(R.id.inputKeterangan8))
                    loadFieldIfExists(data, "actual9", findViewById(R.id.inputAktual9))
                    loadFieldIfExists(data, "remark9", findViewById(R.id.inputKeterangan9))
                    loadFieldIfExists(data, "actual10", findViewById(R.id.inputAktual10))
                    loadFieldIfExists(data, "remark10", findViewById(R.id.inputKeterangan10))
                    loadFieldIfExists(data, "actual11", findViewById(R.id.inputAktual11))
                    loadFieldIfExists(data, "remark11", findViewById(R.id.inputKeterangan11))
                    loadFieldIfExists(data, "actual12", findViewById(R.id.inputAktual12))
                    loadFieldIfExists(data, "remark12", findViewById(R.id.inputKeterangan12))
                    loadFieldIfExists(data, "actual13", findViewById(R.id.inputAktual13))
                    loadFieldIfExists(data, "remark13", findViewById(R.id.inputKeterangan13))
                    loadFieldIfExists(data, "actual14", findViewById(R.id.inputAktual14))
                    loadFieldIfExists(data, "remark14", findViewById(R.id.inputKeterangan14))
                    loadFieldIfExists(data, "actual15", findViewById(R.id.inputAktual15))
                    loadFieldIfExists(data, "remark15", findViewById(R.id.inputKeterangan15))
                    loadFieldIfExists(data, "actual16", findViewById(R.id.inputAktual16))
                    loadFieldIfExists(data, "remark16", findViewById(R.id.inputKeterangan16))
                    loadFieldIfExists(data, "actual17", findViewById(R.id.inputAktual17))
                    loadFieldIfExists(data, "remark17", findViewById(R.id.inputKeterangan17))
                    loadFieldIfExists(data, "actual18", findViewById(R.id.inputAktual18))
                    loadFieldIfExists(data, "remark18", findViewById(R.id.inputKeterangan18))
                    loadFieldIfExists(data, "actual19", findViewById(R.id.inputAktual19))
                    loadFieldIfExists(data, "remark19", findViewById(R.id.inputKeterangan19))

                    // Load signature fields
                    loadSignatureData(data, "zteName", "zteNik", "zteSignature", etZteName, etZteNik, imgZteSignature)
                    loadSignatureData(data, "tifName", "tifNik", "tifSignature", etTifName, etTifNik, imgTifSignature)
                    loadSignatureData(data, "telkomName", "telkomNik", "telkomSignature", etTelkomName, etTelkomNik, imgTelkomSignature)
                    loadSignatureData(data, "tselNopName", "tselNopNik", "tselNopSignature", etTselNopName, etTselNopNik, imgTselNopSignature)
                    loadSignatureData(data, "tselRtpdsName", "tselRtpdsNik", "tselRtpdsSignature", etTselRtpdsName, etTselRtpdsNik, imgTselRtpdsSignature)
                    loadSignatureData(data, "tselRtpeNfName", "tselRtpeNfNik", "tselRtpeNfSignature", etTselRtpeNfName, etTselRtpeNfNik, imgTselRtpeNfSignature)
                }
            }
            .addOnFailureListener { e ->
                Log.w("BASurveyBig", "Error loading detailed data", e)
                Toast.makeText(this, "Failed to load detailed data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadFieldIfExists(data: Map<String, Any>, fieldName: String, editText: EditText) {
        if (data.containsKey(fieldName)) {
            editText.setText(data[fieldName] as? String ?: "")
        }
    }

    private fun showSurveyOptionsDialog(baSurvey: SurveyData) {
        val options = arrayOf("View Details", "Download PDF")

        AlertDialog.Builder(this)
            .setTitle("Survey Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewSurveyDetails(baSurvey)
                    1 -> downloadSurveyPdf(baSurvey)
                }
            }
            .show()
    }

    private fun viewSurveyDetails(baSurvey: SurveyData) {
        // Navigate to detail view activity
        val intent = Intent(this, BASurveyBigDetailActivity::class.java)
        intent.putExtra("SURVEY_ID", baSurvey.id)
        startActivity(intent)
    }

    private fun downloadSurveyPdf(baSurvey: SurveyData) {
        // Periksa apakah pdfUrl ada dan tidak kosong
        if (baSurvey.pdfUrl.isNullOrEmpty()) {
            Toast.makeText(this, "PDF URL tidak ditemukan untuk survei ini.", Toast.LENGTH_LONG).show()
            Log.e("BASurveyBig", "PDF URL is null or empty for survey ID: ${baSurvey.id}")
            return
        }

        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading) // Pastikan Anda memiliki layout ini
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Gunakan pdfUrl yang sudah tersimpan di SurveyData
        val pdfRef = storage.getReferenceFromUrl(baSurvey.pdfUrl!!) // pdfUrl sudah mengandung path yang benar

        // Membuat nama file lokal yang lebih deskriptif (opsional, tapi baik)
        // Ganti karakter yang tidak valid untuk nama file jika ada di lokasi
        val safeLocation = baSurvey.location.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val localFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "BA_Survey_Big_OLT_${safeLocation}_${baSurvey.id}.pdf")

        pdfRef.getFile(localFile)
            .addOnSuccessListener {
                loadingDialog.dismiss()
                try {
                    val uri = FileProvider.getUriForFile(
                        this,
                        "com.mbkm.telgo.fileprovider", // Pastikan authority ini benar
                        localFile
                    )

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }

                    // Verifikasi apakah ada aplikasi yang bisa menangani intent PDF
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(Intent.createChooser(intent, "Open PDF with..."))
                    } else {
                        Toast.makeText(this, "Tidak ada aplikasi untuk membuka PDF.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: IllegalArgumentException) {
                    Log.e("BASurveyBig", "FileProvider error: ${e.message}", e)
                    Toast.makeText(this, "Gagal membuka PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Log.e("BASurveyBig", "Error downloading PDF from search: ${e.message}", e)
                Toast.makeText(this, "Error downloading PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun loadSignatureData(data: Map<String, Any>, nameField: String, nikField: String, signatureField: String,
                                  nameEditText: EditText, nikEditText: EditText, signatureImageView: ImageView) {
        nameEditText.setText(data[nameField] as? String ?: "")
        nikEditText.setText(data[nikField] as? String ?: "")

        val signatureUrl = data[signatureField] as? String
        if (!signatureUrl.isNullOrEmpty()) {
            // Load signature image from URL
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val storageRef = storage.getReferenceFromUrl(signatureUrl)
                    val ONE_MEGABYTE: Long = 1024 * 1024
                    val bytes = storageRef.getBytes(ONE_MEGABYTE).await()
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                    withContext(Dispatchers.Main) {
                        signatureImageView.setImageBitmap(bitmap)
                        signatureImageView.visibility = View.VISIBLE
                    }
                } catch (e: Exception) {
                    Log.e("BASurveyBig", "Error loading signature image", e)
                }
            }
        }
    }

    private fun setupSignatureButtons() {
        // ZTE Signature
        btnZteSignature.setOnClickListener {
            showSignatureOptions(imgZteSignature)
        }

        // TIF Signature
        btnTifSignature.setOnClickListener {
            showSignatureOptions(imgTifSignature)
        }

        // Telkom Signature
        btnTelkomSignature.setOnClickListener {
            showSignatureOptions(imgTelkomSignature)
        }

        // Telkomsel NOP Signature
        btnTselNopSignature.setOnClickListener {
            showSignatureOptions(imgTselNopSignature)
        }

        // Telkomsel RTPDS Signature
        btnTselRtpdsSignature.setOnClickListener {
            showSignatureOptions(imgTselRtpdsSignature)
        }

        // Telkomsel RTPE/NF Signature
        btnTselRtpeNfSignature.setOnClickListener {
            showSignatureOptions(imgTselRtpeNfSignature)
        }
    }

    private fun showSignatureOptions(imageView: ImageView) {
        currentImageView = imageView

        // Ubah opsi yang ditampilkan
        AlertDialog.Builder(this)
            .setTitle("Add Signature")
            .setItems(arrayOf("Choose from Gallery", "File Manager")) { _, which ->
                when (which) {
                    0 -> openGallery()
                    1 -> openFileManager()
                }
            }
            .show()
    }

    // Metode khusus untuk membuka galeri
    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, GALLERY_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open gallery: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("BASurveyBig", "Gallery error: ${e.message}")
        }
    }

    // Metode untuk membuka file manager
    private fun openFileManager() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(Intent.createChooser(intent, "Select Image"), FILE_MANAGER_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open file manager: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("BASurveyBig", "File manager error: ${e.message}")
        }
    }

    // Tambahkan kode konstanta untuk request code
    companion object {
        private const val GALLERY_REQUEST_CODE = 101
        private const val FILE_MANAGER_REQUEST_CODE = 102
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE, FILE_MANAGER_REQUEST_CODE -> {
                    try {
                        val selectedImageUri = data?.data
                        if (selectedImageUri != null && currentImageView != null) {
                            // Gunakan content resolver untuk membuka stream
                            val inputStream = contentResolver.openInputStream(selectedImageUri)
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream?.close()

                            if (bitmap != null) {
                                // Tampilkan gambar di ImageView
                                currentImageView?.setImageBitmap(bitmap)
                                currentImageView?.visibility = View.VISIBLE
                            } else {
                                Toast.makeText(this, "Cannot load selected image", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("BASurveyBig", "Error processing selected image: ${e.message}")
                        Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Image selection cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (!checkCameraPermission()) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (!checkStoragePermission()) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                100
            )
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (inputProjectTitle.text.isNullOrEmpty()) {
            inputProjectTitle.error = "Project title cannot be empty"
            isValid = false
        }

        if (inputLocation.text.isNullOrEmpty()) {
            inputLocation.error = "Location cannot be empty"
            isValid = false
        }

        if (inputContractNumber.text.isNullOrEmpty()) {
            inputContractNumber.error = "Contract number cannot be empty"
            isValid = false
        }

        return isValid
    }

    private fun submitForm() {
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Submitting form...")
            .setCancelable(false)
            .create()

        loadingDialog.show()

        val inputLocationValue = inputLocation.text.toString() // Ambil nilai lokasi dari input
        val formId = UUID.randomUUID().toString() // Buat formId unik

        // Periksa apakah lokasi sudah ada di Firestore
        db.collection("big_surveys")
            .whereEqualTo("location", inputLocationValue)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Jika lokasi sudah ada, tampilkan pesan dan hentikan proses
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@BASurveyBigActivity,
                        "Lokasi sudah ada di database. Tidak dapat melakukan submit.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Jika lokasi belum ada, lanjutkan proses submit
                    val formData = hashMapOf(
                        "projectTitle" to inputProjectTitle.text.toString(),
                        "contractNumber" to inputContractNumber.text.toString(),
                        "executor" to inputExecutor.selectedItem.toString(),
                        "location" to inputLocationValue,
                        "description" to inputDescription.text.toString(),
                        "tselRegion" to etTselRegion.text.toString(),

                        // Actual and remarks data
                        "actual1" to findViewById<EditText>(R.id.inputAktual1).text.toString(),
                        "remark1" to findViewById<EditText>(R.id.inputKeterangan1).text.toString(),
                        "actual2" to findViewById<EditText>(R.id.inputAktual2).text.toString(),
                        "remark2" to findViewById<EditText>(R.id.inputKeterangan2).text.toString(),
                        "actual3" to findViewById<EditText>(R.id.inputAktual3).text.toString(),
                        "remark3" to findViewById<EditText>(R.id.inputKeterangan3).text.toString(),
                        "actual4" to findViewById<EditText>(R.id.inputAktual4).text.toString(),
                        "remark4" to findViewById<EditText>(R.id.inputKeterangan4).text.toString(),
                        "actual5" to findViewById<EditText>(R.id.inputAktual5).text.toString(),
                        "remark5" to findViewById<EditText>(R.id.inputKeterangan5).text.toString(),
                        "actual6" to findViewById<EditText>(R.id.inputAktual6).text.toString(),
                        "remark6" to findViewById<EditText>(R.id.inputKeterangan6).text.toString(),
                        "actual7" to findViewById<EditText>(R.id.inputAktual7).text.toString(),
                        "remark7" to findViewById<EditText>(R.id.inputKeterangan7).text.toString(),
                        "actual8" to findViewById<EditText>(R.id.inputAktual8).text.toString(),
                        "remark8" to findViewById<EditText>(R.id.inputKeterangan8).text.toString(),
                        "actual9" to findViewById<EditText>(R.id.inputAktual9).text.toString(),
                        "remark9" to findViewById<EditText>(R.id.inputKeterangan9).text.toString(),
                        "actual10" to findViewById<EditText>(R.id.inputAktual10).text.toString(),
                        "remark10" to findViewById<EditText>(R.id.inputKeterangan10).text.toString(),
                        "actual11" to findViewById<EditText>(R.id.inputAktual11).text.toString(),
                        "remark11" to findViewById<EditText>(R.id.inputKeterangan11).text.toString(),
                        "actual12" to findViewById<EditText>(R.id.inputAktual12).text.toString(),
                        "remark12" to findViewById<EditText>(R.id.inputKeterangan12).text.toString(),
                        "actual13" to findViewById<EditText>(R.id.inputAktual13).text.toString(),
                        "remark13" to findViewById<EditText>(R.id.inputKeterangan13).text.toString(),
                        "actual14" to findViewById<EditText>(R.id.inputAktual14).text.toString(),
                        "remark14" to findViewById<EditText>(R.id.inputKeterangan14).text.toString(),
                        "actual15" to findViewById<EditText>(R.id.inputAktual15).text.toString(),
                        "remark15" to findViewById<EditText>(R.id.inputKeterangan15).text.toString(),
                        "actual16" to findViewById<EditText>(R.id.inputAktual16).text.toString(),
                        "remark16" to findViewById<EditText>(R.id.inputKeterangan16).text.toString(),
                        "actual17" to findViewById<EditText>(R.id.inputAktual17).text.toString(),
                        "remark17" to findViewById<EditText>(R.id.inputKeterangan17).text.toString(),
                        "actual18" to findViewById<EditText>(R.id.inputAktual18).text.toString(),
                        "remark18" to findViewById<EditText>(R.id.inputKeterangan18).text.toString(),
                        "actual19" to findViewById<EditText>(R.id.inputAktual19).text.toString(),
                        "remark19" to findViewById<EditText>(R.id.inputKeterangan19).text.toString(),

                        // Signature names and NIKs
                        "zteName" to etZteName.text.toString(),
                        "zteNik" to etZteNik.text.toString(),

                        "tifName" to etTifName.text.toString(),
                        "tifNik" to etTifNik.text.toString(),

                        "telkomName" to etTelkomName.text.toString(),
                        "telkomNik" to etTelkomNik.text.toString(),

                        "tselNopName" to etTselNopName.text.toString(),
                        "tselNopNik" to etTselNopNik.text.toString(),

                        "tselRtpdsName" to etTselRtpdsName.text.toString(),
                        "tselRtpdsNik" to etTselRtpdsNik.text.toString(),

                        "tselRtpeNfName" to etTselRtpeNfName.text.toString(),
                        "tselRtpeNfNik" to etTselRtpeNfNik.text.toString(),

                        "createdAt" to System.currentTimeMillis()
                    )

                    // Upload signatures first if available
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Upload signatures jika ada
                            val signatureFields = listOf(
                                Triple("zteSignature", imgZteSignature, "ba_survey_big_olt_signatures/zte_${UUID.randomUUID()}.jpg"),
                                Triple("tifSignature", imgTifSignature, "ba_survey_big_olt_signatures/tif_${UUID.randomUUID()}.jpg"),
                                Triple("telkomSignature", imgTelkomSignature, "ba_survey_big_olt_signatures/telkom_${UUID.randomUUID()}.jpg"),
                                Triple("tselNopSignature", imgTselNopSignature, "ba_survey_big_olt_signatures/tsel_nop_${UUID.randomUUID()}.jpg"),
                                Triple("tselRtpdsSignature", imgTselRtpdsSignature, "ba_survey_big_olt_signatures/tsel_rtpds_${UUID.randomUUID()}.jpg"),
                                Triple("tselRtpeNfSignature", imgTselRtpeNfSignature, "ba_survey_big_olt_signatures/tsel_rtpe_${UUID.randomUUID()}.jpg")
                            )

                            for ((fieldName, imageView, storagePath) in signatureFields) {
                                if (imageView.drawable != null && imageView.visibility == View.VISIBLE) {
                                    val bitmap = (imageView.drawable as BitmapDrawable).bitmap
                                    val baos = java.io.ByteArrayOutputStream()
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                                    val data = baos.toByteArray()

                                    val storageRef = storage.reference.child(storagePath)
                                    val uploadTask = storageRef.putBytes(data).await()
                                    val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

                                    formData[fieldName] = downloadUrl
                                }
                            }

                            // Upload PDF jika ada
                            try {
                                val pdfFile = generateStyledPdf()

                                val pdfPath = "ba_survey_big_olt_pdf/$formId.pdf"
                                val pdfStorageRef = storage.reference.child(pdfPath)
                                pdfStorageRef.putFile(Uri.fromFile(pdfFile)).await()
                                val pdfDownloadUrl = pdfStorageRef.downloadUrl.await().toString()
                                formData["pdfUrl"] = pdfDownloadUrl
                            } catch (e: Exception) {
                                Log.e("BASurveyBig", "Error generating PDF: ${e.message}")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@BASurveyBigActivity,
                                        "PDF tidak berhasil dibuat, melanjutkan tanpa PDF",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }

                            // Simpan data ke Firestore
                            db.collection("big_surveys")
                                .add(formData)
                                .addOnSuccessListener { documentReference ->
                                    loadingDialog.dismiss()
                                    Toast.makeText(
                                        this@BASurveyBigActivity,
                                        "Form submitted successfully with ID: ${documentReference.id}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    resetForm()
                                }
                                .addOnFailureListener { e ->
                                    loadingDialog.dismiss()
                                    Toast.makeText(
                                        this@BASurveyBigActivity,
                                        "Error submitting form: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }

                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                loadingDialog.dismiss()
                                Toast.makeText(
                                    this@BASurveyBigActivity,
                                    "Error uploading signatures: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(
                    this@BASurveyBigActivity,
                    "Error checking location: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun resetForm() {
        // Reset all form fields
        inputProjectTitle.text.clear()
        inputContractNumber.text.clear()
        inputExecutor.setSelection(0)
        inputLocation.text.clear()
        inputDescription.text.clear()
        etTselRegion.text.clear()

        // Reset actual and remarks fields
        for (i in 1..19) {
            try {
                findViewById<EditText>(resources.getIdentifier("inputAktual$i", "id", packageName)).text.clear()
                findViewById<EditText>(resources.getIdentifier("inputKeterangan$i", "id", packageName)).text.clear()
            } catch (e: Exception) {
                Log.e("BASurveyBig", "Error resetting field $i: ${e.message}")
            }
        }

        // Reset signature fields
        resetSignatureFields(etZteName, etZteNik, imgZteSignature)
        resetSignatureFields(etTifName, etTifNik, imgTifSignature)
        resetSignatureFields(etTelkomName, etTelkomNik, imgTelkomSignature)
        resetSignatureFields(etTselNopName, etTselNopNik, imgTselNopSignature)
        resetSignatureFields(etTselRtpdsName, etTselRtpdsNik, imgTselRtpdsSignature)
        resetSignatureFields(etTselRtpeNfName, etTselRtpeNfNik, imgTselRtpeNfSignature)
    }

    private fun resetSignatureFields(nameField: EditText, nikField: EditText, signatureImage: ImageView) {
        nameField.text.clear()
        nikField.text.clear()
        signatureImage.setImageDrawable(null)
        signatureImage.visibility = View.GONE
    }

    private fun generateDescription(projectTitle: String, contractNumber: String, executor: String): String {
        val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())
        return "Pada hari ini, $currentDate, telah dilakukan survey bersama terhadap pekerjaan \"$projectTitle\" " +
                "yang dilaksanakan oleh $executor yang terikat Perjanjian Pemborongan \"$contractNumber\" " +
                "dengan hasil sebagai berikut:"
    }

    private suspend fun generateStyledPdf(): File {
        var createdFile: File? = null

        withContext(Dispatchers.IO) {
            try {
                // Cek dan buat direktori penyimpanan terlebih dahulu
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    val dirCreated = downloadsDir.mkdirs()
                    if (!dirCreated) {
                        Log.e("BASurveyBig", "Gagal membuat direktori downloads")
                        // Coba gunakan direktori apps sebagai fallback
                        val appDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        if (appDir != null && (appDir.exists() || appDir.mkdirs())) {
                            // Gunakan direktori aplikasi sebagai fallback
                            Log.i("BASurveyBig", "Menggunakan direktori aplikasi sebagai fallback")
                        } else {
                            throw IOException("Tidak bisa membuat direktori penyimpanan")
                        }
                    }
                }

                // Ambil input dari pengguna
                val projectTitle = findViewById<EditText>(R.id.inputProjectTitle).text.toString()
                val contractNumber = findViewById<EditText>(R.id.inputContractNumber).text.toString()
                val executor = findViewById<Spinner>(R.id.inputExecutor).selectedItem.toString()
                val location = findViewById<EditText>(R.id.inputLocation).text.toString()
                // Hasil deskripsi otomatis
                val description = generateDescription(projectTitle, contractNumber, executor)

                // Tampilkan deskripsi otomatis di field deskripsi
                withContext(Dispatchers.Main) {
                    findViewById<EditText>(R.id.inputDescription).setText(description)
                }

                // Baca semua input aktual dan keterangan
                val actual1 = findViewById<EditText>(R.id.inputAktual1).text.toString()
                val remark1 = findViewById<EditText>(R.id.inputKeterangan1).text.toString()
                val actual2 = findViewById<EditText>(R.id.inputAktual2).text.toString()
                val remark2 = findViewById<EditText>(R.id.inputKeterangan2).text.toString()
                val actual3 = findViewById<EditText>(R.id.inputAktual3).text.toString()
                val remark3 = findViewById<EditText>(R.id.inputKeterangan3).text.toString()
                val actual4 = findViewById<EditText>(R.id.inputAktual4).text.toString()
                val remark4 = findViewById<EditText>(R.id.inputKeterangan4).text.toString()
                val actual5 = findViewById<EditText>(R.id.inputAktual5).text.toString()
                val remark5 = findViewById<EditText>(R.id.inputKeterangan5).text.toString()
                val actual6 = findViewById<EditText>(R.id.inputAktual6).text.toString()
                val remark6 = findViewById<EditText>(R.id.inputKeterangan6).text.toString()
                val actual7 = findViewById<EditText>(R.id.inputAktual7).text.toString()
                val remark7 = findViewById<EditText>(R.id.inputKeterangan7).text.toString()
                val actual8 = findViewById<EditText>(R.id.inputAktual8).text.toString()
                val remark8 = findViewById<EditText>(R.id.inputKeterangan8).text.toString()
                val actual9 = findViewById<EditText>(R.id.inputAktual9).text.toString()
                val remark9 = findViewById<EditText>(R.id.inputKeterangan9).text.toString()
                val actual10 = findViewById<EditText>(R.id.inputAktual10).text.toString()
                val remark10 = findViewById<EditText>(R.id.inputKeterangan10).text.toString()
                val actual11 = findViewById<EditText>(R.id.inputAktual11).text.toString()
                val remark11 = findViewById<EditText>(R.id.inputKeterangan11).text.toString()
                val actual12 = findViewById<EditText>(R.id.inputAktual12).text.toString()
                val remark12 = findViewById<EditText>(R.id.inputKeterangan12).text.toString()
                val actual13 = findViewById<EditText>(R.id.inputAktual13).text.toString()
                val remark13 = findViewById<EditText>(R.id.inputKeterangan13).text.toString()
                val actual14 = findViewById<EditText>(R.id.inputAktual14).text.toString()
                val remark14 = findViewById<EditText>(R.id.inputKeterangan14).text.toString()
                val actual15 = findViewById<EditText>(R.id.inputAktual15).text.toString()
                val remark15 = findViewById<EditText>(R.id.inputKeterangan15).text.toString()
                val actual16 = findViewById<EditText>(R.id.inputAktual16).text.toString()
                val remark16 = findViewById<EditText>(R.id.inputKeterangan16).text.toString()
                val actual17 = findViewById<EditText>(R.id.inputAktual17).text.toString()
                val remark17 = findViewById<EditText>(R.id.inputKeterangan17).text.toString()
                val actual18 = findViewById<EditText>(R.id.inputAktual18).text.toString()
                val remark18 = findViewById<EditText>(R.id.inputKeterangan18).text.toString()
                val actual19 = findViewById<EditText>(R.id.inputAktual19).text.toString()
                val remark19 = findViewById<EditText>(R.id.inputKeterangan19).text.toString()

                val document = PdfDocument()
                var pageCount = 1

                // Konstanta halaman
                val pageWidth = 595f
                val pageHeight = 842f
                val marginX = 50f
                val marginTop = 50f
                val marginBottom = 60f  // Tambah margin bottom untuk footer yang lebih rapi
                val maxX = pageWidth - marginX

                // Paints
                val paint = Paint().apply {
                    color = Color.BLACK
                    textSize = 11f
                    textAlign = Paint.Align.LEFT
                }
                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.CENTER
                }
                val boldPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    textAlign = Paint.Align.LEFT
                }
                val cellPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 11f
                    textAlign = Paint.Align.LEFT
                }
                val tablePaint = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }

                // Fungsi untuk membuat halaman baru
                fun createPage(): PdfDocument.Page {
                    val pageInfo = PdfDocument.PageInfo.Builder(
                        pageWidth.toInt(),
                        pageHeight.toInt(),
                        pageCount++
                    ).create()
                    return document.startPage(pageInfo)
                }

                var page = createPage()
                var canvas = page.canvas
                var y = marginTop

                // Header halaman dengan logo dinamis
                fun drawHeader(executor: String) {
                    val centerX = (marginX + maxX) / 2

                    // Tambahkan logo berdasarkan pelaksana
                    val zteLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_zte) // Logo ZTE
                    val huaweiLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_huawei) // Logo Huawei
                    val telkomLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_telkom) // Logo Telkom

                    // Ukuran logo
                    val logoWidth = 80 // Lebar logo
                    val logoHeight = 50 // Tinggi logo
                    val topMargin = marginTop // Margin atas untuk logo

                    // Gambar logo pelaksana di pojok kiri atas
                    when (executor) {
                        "PT. ZTE INDONESIA" -> {
                            val scaledZteLogo = Bitmap.createScaledBitmap(zteLogo, logoWidth, logoHeight, false)
                            canvas.drawBitmap(scaledZteLogo, marginX, topMargin, null)
                        }
                        "PT Huawei Tech Investment" -> {
                            val scaledHuaweiLogo = Bitmap.createScaledBitmap(huaweiLogo, logoWidth, logoHeight, false)
                            canvas.drawBitmap(scaledHuaweiLogo, marginX, topMargin, null)
                        }
                    }

                    // Gambar logo Telkom di pojok kanan atas
                    val scaledTelkomLogo = Bitmap.createScaledBitmap(telkomLogo, logoWidth, logoHeight, false)
                    canvas.drawBitmap(scaledTelkomLogo, maxX - logoWidth, topMargin, null)

                    // Tambahkan jarak di bawah logo
                    val logoBottomY = topMargin + logoHeight + 20f

                    // Teks header
                    canvas.drawText("BERITA ACARA", centerX, logoBottomY, titlePaint)
                    canvas.drawText("SURVEY LOKASI", centerX, logoBottomY + 20f, titlePaint)
                    canvas.drawLine(marginX, logoBottomY + 30f, maxX, logoBottomY + 30f, paint)
                    y = logoBottomY + 40f // Perbarui posisi vertikal
                }

                // Footer halaman yang diperbaiki
                fun drawFooter() {
                    // Atur ukuran teks lebih kecil untuk tulisan footer
                    paint.textSize = 8f // Ukuran teks lebih kecil
                    paint.color = Color.BLACK
                    paint.alpha = 220 // Sedikit transparan agar terlihat lebih profesional

                    // Posisi footer yang lebih rapi
                    val footerY = pageHeight - 30f

                    // Garis pembatas yang lebih tipis
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.5f
                    canvas.drawLine(
                        marginX,          // Garis mulai dari margin kiri
                        footerY,          // Posisi Y untuk garis
                        pageWidth - marginX, // Garis berakhir di margin kanan
                        footerY,
                        paint
                    )

                    // Tulisan dokumen
                    paint.style = Paint.Style.FILL
                    paint.textAlign = Paint.Align.LEFT
                    val documentText = "Dokumen ini telah ditandatangani secara elektronik dan merupakan dokumen sah sesuai ketentuan yang berlaku"
                    val pageText = "Halaman ${pageCount - 1}"

                    // Gabungkan kedua teks dengan spasi
                    val combinedText = "$documentText     $pageText" // Tambahkan spasi di antara kedua teks
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText(
                        combinedText,
                        marginX,          // Tulisan dimulai dari margin kiri
                        footerY + 15f,    // Posisi Y di bawah garis
                        paint
                    )

                    // Reset paint properties
                    paint.textSize = 11f
                    paint.alpha = 255
                    paint.textAlign = Paint.Align.LEFT
                }

                // Tambahkan teks di bawah tabel halaman terakhir
                fun drawClosingStatement() {
                    val closingText = "Demikian Berita Acara Hasil Survey ini dibuat berdasarkan kenyataan di lapangan untuk dijadikan pedoman pelaksanaan selanjutnya."
                    val closingMaxWidth = maxX - marginX * 2
                    val closingLines = wrapText(closingText, closingMaxWidth, paint)

                    // Format tanggal hari ini
                    val currentDate = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
                    // Tinggi minimum untuk menambahkan teks
                    val closingHeight = 18f * closingLines.size + 10f + 20f // Tambahkan ruang untuk tanggal
                    if (y + closingHeight > pageHeight - marginBottom - 30f) {
                        drawFooter()
                        document.finishPage(page)
                        page = createPage()
                        canvas = page.canvas
                        y = marginTop
                    }

                    y += 20f // Jarak 2 baris dari tabel terakhir
                    for (line in closingLines) {
                        canvas.drawText(line, marginX, y, paint)
                        y += 18f
                    }

                    // Tulis tanggal dengan bold dan align right
                    val boldPaint = Paint(paint).apply {
                        typeface = Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(currentDate, maxX, y + 10f, boldPaint) // Posisi align right
                }

                // Tambahkan tanda tangan di halaman terakhir
                fun drawSignaturesWithFormattedTitles(
                    canvas: Canvas,
                    region: String,
                    yStart: Float,
                    paint: Paint,
                    boldPaint: Paint,
                    executor: String // Tambah parameter executor
                ) {
                    val boxWidth = (595 - (marginX * 2)) / 3 // Lebar kotak tanda tangan
                    val signatureBoxHeight = 150f // Tinggi kotak tanda tangan
                    var y = yStart

                    // Paint untuk menggambar garis luar kotak (stroke)
                    val boxPaint = Paint().apply {
                        color = Color.BLACK
                        style = Paint.Style.STROKE // Hanya menggambar garis luar
                        strokeWidth = 2f
                    }

                    // Fungsi untuk menggambar teks nama perusahaan dengan format khusus (2 atau 3 baris)
                    fun drawFormattedTitle(
                        canvas: Canvas,
                        lines: List<String>,
                        x: Float,
                        y: Float,
                        maxWidth: Float,
                        boldPaint: Paint
                    ): Float {
                        val lineHeight = boldPaint.textSize + 4f // Tinggi setiap baris
                        var currentY = y

                        for (line in lines) {
                            canvas.drawText(line, x, currentY, boldPaint)
                            currentY += lineHeight
                        }

                        return currentY // Kembalikan posisi Y setelah teks terakhir
                    }

                    // Fungsi untuk menggambar kotak tanda tangan
                    fun drawSignatureBox(
                        lines: List<String>,
                        name: String,
                        nik: String,
                        signature: Drawable?,
                        x: Float,
                        y: Float
                    ) {
                        // Gambar kotak
                        val rect = RectF(x, y, x + boxWidth, y + signatureBoxHeight)
                        canvas.drawRect(rect, boxPaint)

                        // Tulis nama perusahaan dengan format 2 atau 3 baris
                        val titleY = drawFormattedTitle(
                            canvas,
                            lines,
                            x + 10f,
                            y + 20f,
                            boxWidth - 20f,
                            boldPaint
                        )

                        // Gambar tanda tangan di tengah kotak
                        val signatureY = titleY + 10f
                        if (signature != null) {
                            val bitmap = (signature as BitmapDrawable).bitmap
                            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 100, 50, false)
                            canvas.drawBitmap(
                                scaledBitmap,
                                x + (boxWidth / 2 - 50f),
                                signatureY,
                                null
                            )
                        }

                        // Tulis nama dan NIK di bawah tanda tangan
                        val nameY = y + signatureBoxHeight - 40f // Posisi tetap sejajar
                        canvas.drawText("($name)", x + 10f, nameY, paint)
                        canvas.drawText("NIK: $nik", x + 10f, nameY + 20f, paint)
                    }

                    // Ambil input untuk tanda tangan
                    val zteName = findViewById<EditText>(R.id.etZteName).text.toString()
                    val zteNik = findViewById<EditText>(R.id.etZteNik).text.toString()
                    val zteSignature = findViewById<ImageView>(R.id.imgZteSignature).drawable

                    val tifName = findViewById<EditText>(R.id.etTifName).text.toString()
                    val tifNik = findViewById<EditText>(R.id.etTifNik).text.toString()
                    val tifSignature = findViewById<ImageView>(R.id.imgTifSignature).drawable

                    val telkomName = findViewById<EditText>(R.id.etTelkomName).text.toString()
                    val telkomNik = findViewById<EditText>(R.id.etTelkomNik).text.toString()
                    val telkomSignature = findViewById<ImageView>(R.id.imgTelkomSignature).drawable

                    val tselNopName = findViewById<EditText>(R.id.etTselNopName).text.toString()
                    val tselNopNik = findViewById<EditText>(R.id.etTselNopNik).text.toString()
                    val tselNopSignature = findViewById<ImageView>(R.id.imgTselNopSignature).drawable
                    val tselRtpdsName = findViewById<EditText>(R.id.etTselRtpdsName).text.toString()
                    val tselRtpdsNik = findViewById<EditText>(R.id.etTselRtpdsNik).text.toString()
                    val tselRtpdsSignature = findViewById<ImageView>(R.id.imgTselRtpdsSignature).drawable

                    val tselRtpeName = findViewById<EditText>(R.id.etTselRtpeNfName).text.toString()
                    val tselRtpeNik = findViewById<EditText>(R.id.etTselRtpeNfNik).text.toString()
                    val tselRtpeSignature = findViewById<ImageView>(R.id.imgTselRtpeNfSignature).drawable

                    // Baris pertama (Executor, TIF, TELKOM)
                    val surveyCompany = if (executor == "PT Huawei Tech Investment")
                        listOf("PT. Huawei Tech Investment", "TIM SURVEY")
                    else
                        listOf("PT. ZTE INDONESIA", "TIM SURVEY")

                    drawSignatureBox(
                        surveyCompany,
                        zteName, zteNik, zteSignature, marginX, y
                    )
                    drawSignatureBox(
                        listOf("PT. TIF", "TIM SURVEY"),
                        tifName, tifNik, tifSignature, marginX + boxWidth, y
                    )
                    drawSignatureBox(
                        listOf("PT. TELKOM", "MGR NDPS TR1"),
                        telkomName, telkomNik, telkomSignature, marginX + (2 * boxWidth), y
                    )

                    // Baris kedua (NOP, RTPDS, RTPE)
                    y += signatureBoxHeight + 20f
                    drawSignatureBox(
                        listOf("PT. TELKOMSEL", "MGR NOP", region),
                        tselNopName, tselNopNik, tselNopSignature, marginX, y
                    )
                    drawSignatureBox(
                        listOf("PT. TELKOMSEL", "MGR RTPDS", region),
                        tselRtpdsName, tselRtpdsNik, tselRtpdsSignature, marginX + boxWidth, y
                    )
                    drawSignatureBox(
                        listOf("PT. TELKOMSEL", "MGR RTPE", region),
                        tselRtpeName, tselRtpeNik, tselRtpeSignature, marginX + (2 * boxWidth), y
                    )
                }

                drawHeader(executor)

                // Informasi Proyek
                val labelX = marginX
                val colonX = 180f
                val valueX = 200f
                val infoMaxWidth = maxX - valueX

                fun drawInfo(label: String, value: String, isBold: Boolean = false) {
                    val valuePaint = if (isBold) boldPaint else paint
                    canvas.drawText(label, labelX, y, paint)
                    canvas.drawText(":", colonX, y, paint)
                    val lines = wrapText(value, infoMaxWidth, valuePaint)
                    for (line in lines) {
                        canvas.drawText(line, valueX, y, valuePaint)
                        y += 18f
                    }
                }

                drawInfo("Proyek", projectTitle, isBold = true)
                drawInfo("Nomor Kontrak KHS", contractNumber)
                drawInfo("Pelaksana", executor)
                drawInfo("Lokasi", location, isBold = true)

                // Garis pembatas di bawah lokasi
                canvas.drawLine(marginX, y + 5f, maxX, y + 5f, paint)
                y += 20f // Tambahkan jarak setelah garis pembatas

                // Deskripsi
                val descMaxWidth = maxX - marginX * 2
                y = drawJustifiedText(canvas, description, marginX, y, descMaxWidth, paint) // Gambar teks dan perbarui posisi Y

                y += 10f // Jarak sebelum tabel

                // Tabel dengan kolom yang disesuaikan - PENTING: Memberikan ruang lebih untuk kolom AKTUAL
                val colX = floatArrayOf(
                    marginX,        // NO
                    marginX + 40f,  // ITEM - lebih lebar
                    marginX + 230f, // SATUAN - sedang
                    marginX + 300f, // AKTUAL - lebih lebar dari sebelumnya
                    marginX + 370f, // KETERANGAN - lebar
                    maxX           // Batas kanan tabel
                )

                // Header tabel
                val rowHeight = 40f
                canvas.drawRect(colX[0], y, colX[5], y + rowHeight, tablePaint)
                canvas.drawText("NO", colX[0] + 15f, y + 25f, boldPaint)
                canvas.drawText("ITEM", colX[1] + 10f, y + 25f, boldPaint)
                canvas.drawText("SATUAN", colX[2] + 10f, y + 25f, boldPaint)
                canvas.drawText("AKTUAL", colX[3] + 10f, y + 25f, boldPaint)
                canvas.drawText("KETERANGAN", colX[4] + 10f, y + 25f, boldPaint)

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + rowHeight, tablePaint)
                }
                y += rowHeight

                // FUNCTION TO CREATE TABLE ROW WITH WRAPPING TEXT
                fun drawTableRow(rowNum: Int, itemText: String, unitText: String, actualText: String, remarkText: String) {
                    val itemMaxWidth = colX[2] - colX[1] - 10f
                    val actualMaxWidth = colX[4] - colX[3] - 10f
                    val remarkMaxWidth = colX[5] - colX[4] - 10f

                    val itemLines = wrapText(itemText, itemMaxWidth, cellPaint)
                    val actualLines = wrapText(actualText, actualMaxWidth, cellPaint)
                    val remarkLines = wrapText(remarkText, remarkMaxWidth, cellPaint)

                    val dynamicRowHeight = maxOf(
                        40f,
                        20f + (itemLines.size * 15f),
                        20f + (actualLines.size * 15f),
                        20f + (remarkLines.size * 15f)
                    )

                    if (y + dynamicRowHeight > pageHeight - marginBottom) {
                        drawFooter()
                        document.finishPage(page)
                        page = createPage()
                        canvas = page.canvas
                        y = marginTop
                        drawHeader(executor)
                    }

                    canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight, tablePaint)
                    canvas.drawText(rowNum.toString(), colX[0] + 15f, y + 20f, cellPaint)

                    var itemY = y + 18f
                    for (line in itemLines) {
                        canvas.drawText(line, colX[1] + 5f, itemY, cellPaint)
                        itemY += 15f
                    }

                    canvas.drawText(unitText, colX[2] + 10f, y + 20f, cellPaint)

                    var actualY = y + 18f
                    for (line in actualLines) {
                        canvas.drawText(line, colX[3] + 5f, actualY, cellPaint)
                        actualY += 15f
                    }

                    var remarkY = y + 18f
                    for (line in remarkLines) {
                        canvas.drawText(line, colX[4] + 5f, remarkY, cellPaint)
                        remarkY += 15f
                    }

                    for (x in colX) {
                        canvas.drawLine(x, y, x, y + dynamicRowHeight, tablePaint)
                    }

                    y += dynamicRowHeight
                }

                // Baris 1 dengan handling khusus karena formatnya berbeda
                val itemMaxWidth1 = colX[2] - colX[1] - 10f
                val actualMaxWidth1 = colX[4] - colX[3] - 10f
                val remarkMaxWidth1 = colX[5] - colX[4] - 10f

                val itemLines1 = wrapText("Propose OLT", itemMaxWidth1, cellPaint)
                val actualLines1 = wrapText(actual1, actualMaxWidth1, cellPaint)
                val remarkLines1 = wrapText(remark1, remarkMaxWidth1, cellPaint)

                val dynamicRowHeight1 = maxOf(
                    40f,
                    20f + (itemLines1.size * 15f),
                    20f + (actualLines1.size * 15f),
                    20f + (remarkLines1.size * 15f)
                )

                if (y + dynamicRowHeight1 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop
                    drawHeader(executor)
                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight1, tablePaint)
                canvas.drawText("1", colX[0] + 15f, y + 20f, cellPaint)

                var itemY1 = y + 18f
                for (line in itemLines1) {
                    canvas.drawText(line, colX[1] + 5f, itemY1, cellPaint)
                    itemY1 += 15f
                }

                canvas.drawText("OK/NOK", colX[2] + 10f, y + 20f, cellPaint)

                var actualY1 = y + 18f
                for (line in actualLines1) {
                    canvas.drawText(line, colX[3] + 5f, actualY1, cellPaint)
                    actualY1 += 15f
                }

                var remarkY1 = y + 18f
                for (line in remarkLines1) {
                    canvas.drawText(line, colX[4] + 5f, remarkY1, cellPaint)
                    remarkY1 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight1, tablePaint)
                }

                y += dynamicRowHeight1

                // Draw rows 2-19 using the function
                drawTableRow(2, "Panjang Bundlecore Uplink (Dari Metro ke FTM-Rack ET)", "Meter", actual2, remark2)
                drawTableRow(3, "Panjang Bundlecore Uplink (Dari Rack ET ke OLT)", "Meter", actual3, remark3)
                drawTableRow(4, "Panjang Bundlecore Downlink (Dari Rack EA ke OLT)", "Meter", actual4, remark4)
                drawTableRow(5, "Pengecekan ground bar ruangan dan Panjang kabel grounding ke ground bar ruangan(kabel-25mm)", "meter", actual5, remark5)
                drawTableRow(6, "Panjang Kabel Power (25mm) Dari OLT ke DCPDB Eksisting/New (Untuk 2 source)", "Meter", actual6, remark6)
                drawTableRow(7, "Panjang Kabel Power (35mm). Kebutuhan yang diperlukan jika tidak ada DCPDB Eksisting / Menggunakan DCPDB New", "meter", actual7, remark7)
                drawTableRow(8, "Kebutuhan catuan daya di Recti", "OK/NOK", actual8, remark8)
                drawTableRow(9, "Kebutuhan DCPDB New, jika dibutuhkan dan Propose DCPDBnya", "Pcs", actual9, remark9)
                drawTableRow(10, "Kebutuhan Tray @3m (pcs) dari Tray Eksisting ke OLT-turunan Dan Rack FTM-EA kalau diperlukan", "Meter", actual10, remark10)
                drawTableRow(11, "Kebutuhan MCB 63A-Schneider", "Pcs", actual11, remark11)
                drawTableRow(12, "Space 2 pcs FTB untuk di install di rack EA", "Meter", actual12, remark12)
                drawTableRow(13, "FTB yang kita gunakan FTB Type TDS/MDT tidak bisa mengikuti FTB Eksisting jika ada yang beda", "OK/NOK", actual13, remark13)
                drawTableRow(14, "Kebutuhan Rack EA", "Pcs", actual14, remark14)
                drawTableRow(15, "Alokasi Port Metro", "Port", actual15, remark15)
                drawTableRow(16, "Kebutuhan SFP", "Pcs", actual16, remark16)
                drawTableRow(17, "Alokasi Core di FTB Eksisting (di Rack ET)", "Core", actual17, remark17)
                drawTableRow(18, "Kondisi Penerangan di ruangan OLT", "OK/NOK", actual18, remark18)
                drawTableRow(19, "CME – Kebutuhan Air Conditioner", "Pcs", actual19, remark19)

                // Setelah semua baris tabel selesai
                drawClosingStatement() // Tambahkan teks penutup di bawah tabel terakhir

                // Tambahkan tanda tangan - pastikan ada cukup ruang
                val region = findViewById<EditText>(R.id.etTselRegion).text.toString()

                // Check if signatures will fit on current page
                val signaturesHeight = 2 * 150f + 20f // 2 rows of signatures + spacing
                if (y + signaturesHeight > pageHeight - marginBottom - 50f) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop
                    drawHeader(executor)
                }

                drawSignaturesWithFormattedTitles(canvas, region, y + 30f, paint, boldPaint, executor)

                drawFooter() // Tambahkan footer di halaman terakhir
                document.finishPage(page)

                // Simpan dokumen - dengan penanganan error yang lebih baik
                try {
                    // Coba simpan di direktori Downloads publik terlebih dahulu
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }

                    // Buat nama file unik
                    var fileIndex = 1
                    var file: File
                    do {
                        val fileName = "SurveyLokasi$fileIndex.pdf"
                        file = File(downloadsDir, fileName)
                        fileIndex++
                    } while (file.exists())

                    // Simpan file
                    val fileOutputStream = FileOutputStream(file)
                    document.writeTo(fileOutputStream)
                    fileOutputStream.close()
                    document.close()

                    createdFile = file

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@BASurveyBigActivity,
                            "PDF berhasil disimpan di: ${file.absolutePath}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: IOException) {
                    Log.e("BASurveyBig", "Error saving to public Downloads: ${e.message}")

                    // Jika gagal, coba simpan ke direktori aplikasi sebagai fallback
                    val fallbackDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    if (fallbackDir != null) {
                        if (!fallbackDir.exists()) {
                            fallbackDir.mkdirs()
                        }

                        val fallbackFile = File(fallbackDir, "SurveyLokasi_${System.currentTimeMillis()}.pdf")
                        try {
                            document.writeTo(FileOutputStream(fallbackFile))
                            document.close()
                            createdFile = fallbackFile

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@BASurveyBigActivity,
                                    "PDF berhasil disimpan di direktori aplikasi: ${fallbackFile.absolutePath}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e2: Exception) {
                            Log.e("BASurveyBig", "Error saving to app directory: ${e2.message}")
                            throw e2 // Re-throw exception jika masih gagal
                        }
                    } else {
                        throw e // Re-throw exception jika tidak ada fallback
                    }
                }

            } catch (e: Exception) {
                Log.e("BASurveyBig", "Error generating PDF: ${e.message}")
                e.printStackTrace() // Log stacktrace untuk debugging

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@BASurveyBigActivity,
                        "Gagal membuat PDF: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Gunakan file kosong "dummy" sebagai fallback terakhir untuk menghindari crash
        if (createdFile == null) {
            val fallbackEmptyFile = File(cacheDir, "empty_survey_${System.currentTimeMillis()}.pdf")
            try {
                fallbackEmptyFile.createNewFile()
                return fallbackEmptyFile
            } catch (e: Exception) {
                Log.e("BASurveyBig", "Fatal error creating fallback file: ${e.message}")
                throw IllegalStateException("File PDF tidak berhasil dibuat: ${e.message}")
            }
        }

        return createdFile!!
    }

    // Fungsi untuk menggambar teks justify menggunakan wrapText
    private fun drawJustifiedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint
    ): Float {
        val lines = wrapText(text, maxWidth, paint)
        var currentY = y

        // Set spasi antar baris lebih renggang (textSize + 10f)
        val lineSpacing = paint.textSize + 10f

        // Untuk spasi antar kata lebih renggang, tambahkan extra space
        val extraWordSpacing = 4f // px, bisa dibesarkan lagi jika mau

        for ((i, line) in lines.withIndex()) {
            val words = line.split(" ")
            val lineWidth = paint.measureText(line)
            val gapCount = words.size - 1

            if (gapCount > 0 && i != lines.lastIndex) {
                // hitung jarak antar kata (lebih renggang dari biasanya)
                val extraSpace = ((maxWidth - lineWidth) / gapCount) + extraWordSpacing
                var startX = x
                for (word in words) {
                    canvas.drawText(word, startX, currentY, paint)
                    startX += paint.measureText(word) + extraSpace
                }
            } else {
                // baris terakhir tanpa justify
                canvas.drawText(line, x, currentY, paint)
            }
            currentY += lineSpacing
        }
        return currentY
    }

    // Fungsi wrapText yang diperbaiki untuk mengatasi masalah overflow text
    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        // Jika text kosong, return list kosong
        if (text.isEmpty()) return listOf("")

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        // Handle single long words that exceed maxWidth
        for (word in words) {
            // Jika kata tunggal lebih panjang dari maxWidth, pecah kata tersebut
            if (paint.measureText(word) > maxWidth) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = ""
                }

                // Pecah kata panjang menjadi beberapa bagian
                var remainingWord = word
                while (paint.measureText(remainingWord) > maxWidth) {
                    var i = 1
                    while (i < remainingWord.length) {
                        if (paint.measureText(remainingWord.substring(0, i)) > maxWidth) {
                            i--
                            break
                        }
                        i++
                    }

                    i = maxOf(1, i) // Minimal ambil 1 karakter
                    lines.add(remainingWord.substring(0, i))
                    remainingWord = remainingWord.substring(i)
                }

                currentLine = remainingWord
            } else {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(testLine) > maxWidth) {
                    lines.add(currentLine)
                    currentLine = word
                } else {
                    currentLine = testLine
                }
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return if (lines.isEmpty()) listOf("") else lines
    }
}
