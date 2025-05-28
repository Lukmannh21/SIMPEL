package com.mbkm.telgo

import android.Manifest
import android.app.AlertDialog
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
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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
import kotlin.math.min

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

    // Photo upload variables
    private val photoUris = HashMap<Int, Uri>()
    private var currentPhotoIndex = 0
    private lateinit var photoButtons: Array<Button>
    private lateinit var photoImageViews: Array<ImageView>
    private lateinit var photoLabels: Array<TextView>

    // Photo labels as defined in the XML
    private val photoLabelTexts = arrayOf(
        "1. Akses Gerbang",
        "2. Name Plate",
        "3. Sto Tampak Depan",
        "4. Foto Ruangan",
        "5. Rectifier A",
        "6. Rectifier B",
        "7. Panel SDP AC",
        "8. Propose Space Cabinet Big OLT",
        "9. Foto Rack Cabinet (Insert Frame)",
        "10. Foto Runaway",
        "11. PLN Fase R",
        "12. PLN Fase S",
        "13. PLN Fase T",
        "14. Grounding",
        "15. Port Metro",
        "16. OTB FA",
        "17. Bundle Core",
        "18. Ruang FTM",
        "19. Foto Selfie Teknisi (Surveyor)"
    )

    // Permissions
    private val CAMERA_PERMISSION_CODE = 100
    private val STORAGE_PERMISSION_CODE = 101

    // Request codes
    private val REQUEST_IMAGE_CAPTURE = 102
    private val REQUEST_GALLERY = 103
    private val REQUEST_SIGNATURE_ZTE = 201
    private val REQUEST_SIGNATURE_TSEL_NOP = 202
    private val REQUEST_SIGNATURE_TSEL_RTPDS = 203
    private val REQUEST_SIGNATURE_TSEL_RTPE = 204
    private val REQUEST_SIGNATURE_TELKOM = 205
    private val REQUEST_SIGNATURE_TIF = 206

    private val marginX = 50f

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

        // Initialize photo buttons directly with better logging
        try {
            photoButtons = Array(19) { i ->
                findViewById<Button>(resources.getIdentifier("btnUploadPhoto${i+1}", "id", packageName)).apply {
                    val finalIndex = i // Save the index for the closure
                    setOnClickListener {
                        Log.d("BASurveyBig", "Photo button ${i+1} clicked, setting index to $finalIndex")
                        currentPhotoIndex = finalIndex
                        showPhotoSourceDialog()
                    }
                }
            }

            photoImageViews = Array(19) { i ->
                findViewById(resources.getIdentifier("imgPhoto${i+1}", "id", packageName))
            }

            photoLabels = Array(19) { i ->
                findViewById(resources.getIdentifier("tvPhotoLabel${i+1}", "id", packageName))
            }

        } catch (e: Exception) {
            Log.e("BASurveyBig", "Error initializing photo views: ${e.message}", e)
            Toast.makeText(this, "Error initializing photo buttons: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // Setup signature button listeners
        setupSignatureButtons()

        // Setup generate PDF and submit form buttons
        btnGeneratePdf.setOnClickListener {
            if (validateForm()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        generateStyledPdf() // Call function to generate PDF
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
                        pdfUrl = data["pdfUrl"] as? String
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
                    searchAdapter.updateData(surveyList)
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

                    // Load photos with improved quality
                    loadPhotosFromFirebase(data)
                }
            }
            .addOnFailureListener { e ->
                Log.w("BASurveyBig", "Error loading detailed data", e)
                Toast.makeText(this, "Failed to load detailed data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPhotosFromFirebase(data: Map<String, Any>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Check for photo URLs using pattern photo1, photo2, etc.
                for (i in 0 until 19) {
                    val photoUrl = data["photo${i+1}"] as? String
                    if (!photoUrl.isNullOrEmpty()) {
                        try {
                            val storageRef = storage.getReferenceFromUrl(photoUrl)

                            // Tambahkan opsi untuk mendapatkan resolusi lebih tinggi
                            val FIVE_MEGABYTE: Long = 5 * 1024 * 1024  // Naikkan ke 5MB untuk kualitas lebih baik

                            val bytes = storageRef.getBytes(FIVE_MEGABYTE).await()

                            // Decode dengan opsi konfigurasi berkualitas tinggi
                            val options = BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                            }
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                            withContext(Dispatchers.Main) {
                                photoImageViews[i].setImageBitmap(bitmap)
                                photoImageViews[i].visibility = View.VISIBLE
                                // Simpan bitmap dalam tag untuk digunakan saat generate PDF
                                photoImageViews[i].tag = bitmap
                            }
                        } catch (e: Exception) {
                            Log.e("BASurveyBig", "Error loading photo ${i+1}: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BASurveyBig", "Error loading photos: ${e.message}")
            }
        }
    }

    private fun loadFieldIfExists(data: Map<String, Any>, fieldName: String, editText: EditText) {
        if (data.containsKey(fieldName)) {
            editText.setText(data[fieldName] as? String ?: "")
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
        // Check if pdfUrl exists and is not empty
        if (baSurvey.pdfUrl.isNullOrEmpty()) {
            Toast.makeText(this, "PDF URL tidak ditemukan untuk survei ini.", Toast.LENGTH_LONG).show()
            Log.e("BASurveyBig", "PDF URL is null or empty for survey ID: ${baSurvey.id}")
            return
        }

        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Use the pdfUrl stored in SurveyData
        val pdfRef = storage.getReferenceFromUrl(baSurvey.pdfUrl!!)

        // Create a descriptive local file name
        val safeLocation = baSurvey.location.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val localFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "BA_Survey_Big_OLT_${safeLocation}_${baSurvey.id}.pdf")

        pdfRef.getFile(localFile)
            .addOnSuccessListener {
                loadingDialog.dismiss()
                try {
                    val uri = FileProvider.getUriForFile(
                        this,
                        "com.mbkm.telgo.fileprovider",
                        localFile
                    )

                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }

                    // Verify if there is an app that can handle the PDF intent
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
        currentPhotoIndex = -1 // Reset photo index when working with signatures

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

    private fun showPhotoSourceDialog() {
        // Log that dialog is opening
        Log.d("BASurveyBig", "Showing photo source dialog for photo index: $currentPhotoIndex")

        try {
            val options = arrayOf("Take Photo", "Choose from Gallery")

            AlertDialog.Builder(this)
                .setTitle("Upload Photo ${currentPhotoIndex + 1}")
                .setItems(options) { dialog, which ->
                    Log.d("BASurveyBig", "Dialog option selected: ${options[which]}")

                    when (which) {
                        0 -> {
                            if (checkCameraPermission()) {
                                openCamera()
                            } else {
                                requestCameraPermission()
                                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
                            }
                        }
                        1 -> {
                            if (checkStoragePermission()) {
                                openGallery()
                            } else {
                                requestStoragePermission()
                                Toast.makeText(this, "Storage permission required", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setOnCancelListener {
                    Log.d("BASurveyBig", "Photo source dialog cancelled")
                }
                .show()
        } catch (e: Exception) {
            Log.e("BASurveyBig", "Error showing photo source dialog: ${e.message}", e)
            Toast.makeText(this, "Error opening photo options", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        try {
            Log.d("BASurveyBig", "Opening camera for photo index: $currentPhotoIndex")

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            // Create temporary file
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val photoFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)

            val photoURI = FileProvider.getUriForFile(
                this,
                "com.mbkm.telgo.fileprovider",
                photoFile
            )

            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            tempPhotoUri = photoURI
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)

        } catch (e: Exception) {
            Log.e("BASurveyBig", "Error opening camera: ${e.message}", e)
            Toast.makeText(this, "Cannot open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        try {
            Log.d("BASurveyBig", "Opening gallery for index: $currentPhotoIndex")

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_GALLERY)
        } catch (e: Exception) {
            Log.e("BASurveyBig", "Error opening gallery: ${e.message}", e)
            Toast.makeText(this, "Cannot open gallery: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFileManager() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_GALLERY)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open file manager: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("BASurveyBig", "File manager error: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCamera()
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openGallery()
                } else {
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.d("BASurveyBig", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode, currentPhotoIndex=$currentPhotoIndex")

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    try {
                        Log.d("BASurveyBig", "Processing camera result for photo index: $currentPhotoIndex")
                        val uri = tempPhotoUri
                        if (uri != null && currentPhotoIndex >= 0) {
                            // Store URI for later use
                            photoUris[currentPhotoIndex] = uri

                            // Show the photo in UI with high quality
                            displayHighQualityImage(uri, photoImageViews[currentPhotoIndex])

                            Toast.makeText(this, "Photo captured successfully", Toast.LENGTH_SHORT).show()
                        } else if (currentImageView != null) {
                            // For signature
                            try {
                                displayHighQualityImage(uri!!, currentImageView!!)
                            } catch (e: Exception) {
                                Log.e("BASurveyBig", "Error setting signature image: ${e.message}")
                            }
                        } else {
                            Log.e("BASurveyBig", "Camera returned null URI or invalid index: $currentPhotoIndex")
                            Toast.makeText(this, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("BASurveyBig", "Error processing camera image: ${e.message}", e)
                        Toast.makeText(this, "Error processing camera image", Toast.LENGTH_SHORT).show()
                    }
                }

                REQUEST_GALLERY -> {
                    try {
                        Log.d("BASurveyBig", "Processing gallery result")
                        val uri = data?.data
                        if (uri != null) {
                            if (currentImageView != null) {
                                // For signature
                                Log.d("BASurveyBig", "Setting signature image")
                                displayHighQualityImage(uri, currentImageView!!)
                                Toast.makeText(this, "Signature image selected", Toast.LENGTH_SHORT).show()
                            } else if (currentPhotoIndex >= 0) {
                                // For photo
                                Log.d("BASurveyBig", "Setting photo image for index: $currentPhotoIndex")
                                photoUris[currentPhotoIndex] = uri
                                displayHighQualityImage(uri, photoImageViews[currentPhotoIndex])
                                Toast.makeText(this, "Photo selected successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("BASurveyBig", "Invalid state: no current image view or photo index")
                                Toast.makeText(this, "Error: couldn't determine target for image", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e("BASurveyBig", "Gallery returned null URI")
                            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("BASurveyBig", "Error processing gallery image: ${e.message}", e)
                        Toast.makeText(this, "Error processing gallery image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            Log.d("BASurveyBig", "Image selection cancelled")
            Toast.makeText(this, "Image selection cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // New method for displaying high quality images
    private fun displayHighQualityImage(uri: Uri, imageView: ImageView) {
        try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inJustDecodeBounds = true
            }

            // First decode bounds only to determine dimensions
            contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            // Calculate optimal sample size for UI display (preserving memory)
            val maxDimension = 1024 // Maximum dimension for UI
            var inSampleSize = 1

            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val heightRatio = Math.round(options.outHeight.toFloat() / maxDimension.toFloat())
                val widthRatio = Math.round(options.outWidth.toFloat() / maxDimension.toFloat())
                inSampleSize = Math.max(1, Math.min(heightRatio, widthRatio))
            }

            // Decode again with sample size
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize

            contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input, null, options)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE

                    // For keeping original URI for later use
                    imageView.tag = uri
                } else {
                    // Fallback to simple URI loading if bitmap loading fails
                    imageView.setImageURI(uri)
                    imageView.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            Log.e("BASurveyBig", "Error loading high quality image: ${e.message}", e)
            // Fallback to basic URI display
            imageView.setImageURI(uri)
            imageView.visibility = View.VISIBLE
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                // First decode with inJustDecodeBounds=true to check dimensions
                inJustDecodeBounds = true
            }

            contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            // Calculate inSampleSize to load a scaled bitmap that is not too large
            val maxDimension = 1024
            var inSampleSize = 1
            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val heightRatio = Math.round(options.outHeight.toFloat() / maxDimension.toFloat())
                val widthRatio = Math.round(options.outWidth.toFloat() / maxDimension.toFloat())
                inSampleSize = Math.max(heightRatio, widthRatio)
            }

            // Decode bitmap with inSampleSize set
            options.apply {
                inJustDecodeBounds = false
                inSampleSize = inSampleSize
            }

            contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }
        } catch (e: Exception) {
            Log.e("BASurveyBig", "Error loading bitmap: ${e.message}")
            null
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_CODE
        )
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                STORAGE_PERMISSION_CODE
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (!checkCameraPermission()) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (!checkStoragePermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
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

        val inputLocationValue = inputLocation.text.toString() // Get location value from input
        val formId = UUID.randomUUID().toString() // Create unique formId

        // Check if location already exists in Firestore
        db.collection("big_surveys")
            .whereEqualTo("location", inputLocationValue)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // If location already exists, show message and stop process
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@BASurveyBigActivity,
                        "Lokasi sudah ada di database. Tidak dapat melakukan submit.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // If location doesn't exist, continue with submission
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
                            // Upload signatures if available
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
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, baos) // Tingkatkan kualitas ke 95
                                    val data = baos.toByteArray()

                                    val storageRef = storage.reference.child(storagePath)
                                    val uploadTask = storageRef.putBytes(data).await()
                                    val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

                                    formData[fieldName] = downloadUrl
                                }
                            }

                            // Handle photo uploads with high quality
                            if (photoUris.isNotEmpty()) {
                                for ((index, uri) in photoUris) {
                                    val photoPath = "ba_survey_big_olt_photos/${formId}/photo${index+1}_${UUID.randomUUID()}.jpg"
                                    val photoRef = storage.reference.child(photoPath)

                                    try {
                                        // Upload high-quality image
                                        contentResolver.openInputStream(uri)?.use { inputStream ->
                                            // Load high-quality bitmap first
                                            val options = BitmapFactory.Options().apply {
                                                inPreferredConfig = Bitmap.Config.ARGB_8888
                                                inJustDecodeBounds = true
                                            }
                                            BitmapFactory.decodeStream(inputStream, null, options)

                                            // Calculate sample size - use small sample size for high quality
                                            val sampleSize = calculateOptimalSampleSize(
                                                options.outWidth, options.outHeight,
                                                2048, 2048 // Target high but reasonable resolution
                                            )

                                            // Set options for actual decoding
                                            options.inJustDecodeBounds = false
                                            options.inSampleSize = sampleSize

                                            // Decode with optimal quality
                                            contentResolver.openInputStream(uri)?.use { input2 ->
                                                val bitmap = BitmapFactory.decodeStream(input2, null, options)

                                                // Compress with high quality
                                                val baos = java.io.ByteArrayOutputStream()
                                                bitmap?.compress(Bitmap.CompressFormat.JPEG, 95, baos)
                                                val bytes = baos.toByteArray()

                                                // Upload and get URL
                                                val uploadTask = photoRef.putBytes(bytes).await()
                                                val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

                                                // Add directly to formData
                                                formData["photo${index+1}"] = downloadUrl
                                                formData["photoLabel${index+1}"] = photoLabelTexts[index]
                                            }
                                        } ?: run {
                                            // Fallback if loading bitmap fails
                                            val bytes = contentResolver.openInputStream(uri)?.readBytes()
                                            if (bytes != null) {
                                                val uploadTask = photoRef.putBytes(bytes).await()
                                                val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

                                                // Add directly to formData
                                                formData["photo${index+1}"] = downloadUrl
                                                formData["photoLabel${index+1}"] = photoLabelTexts[index]
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("BASurveyBig", "Error uploading photo $index: ${e.message}")
                                    }
                                }
                            }

                            // Upload PDF if possible
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

                            // Save data to Firestore
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

        // Reset photo fields
        photoUris.clear()
        for (imageView in photoImageViews) {
            imageView.setImageDrawable(null)
            imageView.visibility = View.GONE
        }
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

    // Helper function to calculate optimal sampling size for bitmap loading
    private fun calculateOptimalSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        // Calculate optimal sample size for memory efficiency
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            // Use power-of-2 for efficient memory usage
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        // Important: ensure we don't downsample too much
        // Limit maximum inSampleSize to maintain quality
        return min(inSampleSize, 4) // Maximum downsampling 1/4 of original size
    }

    // Improved high-quality bitmap scaling
    private fun highQualityScaleBitmap(source: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = source.width
        val height = source.height

        val ratio = Math.min(
            maxWidth.toFloat() / width.toFloat(),
            maxHeight.toFloat() / height.toFloat()
        )

        // If minimal scaling needed, use original bitmap
        if (ratio > 0.9) {
            return source
        }

        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()

        // Create high-quality bitmap configuration
        val scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val scaleCanvas = Canvas(scaledBitmap)

        // Configure paint for maximum quality
        val paint = Paint().apply {
            isFilterBitmap = true  // Crucial to prevent pixelation when scaling
            isAntiAlias = true     // Smooth edges
            isDither = true        // Improve color quality
            color = Color.BLACK    // Ensure correct color
        }

        // Scale with high-accuracy matrix
        val scaleMatrix = Matrix().apply {
            setScale(ratio, ratio)
        }

        // Draw bitmap to canvas with high quality
        scaleCanvas.drawBitmap(source, scaleMatrix, paint)

        return scaledBitmap
    }

    private suspend fun generateStyledPdf(): File {
        var createdFile: File? = null

        withContext(Dispatchers.IO) {
            try {
                // Check and create storage directory first
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    val dirCreated = downloadsDir.mkdirs()
                    if (!dirCreated) {
                        Log.e("BASurveyBig", "Failed to create downloads directory")
                        // Try using app directory as fallback
                        val appDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        if (appDir != null && (appDir.exists() || appDir.mkdirs())) {
                            // Use app directory as fallback
                            Log.i("BASurveyBig", "Using app directory as fallback")
                        } else {
                            throw IOException("Cannot create storage directory")
                        }
                    }
                }

                // Get input from user
                val projectTitle = findViewById<EditText>(R.id.inputProjectTitle).text.toString()
                val contractNumber = findViewById<EditText>(R.id.inputContractNumber).text.toString()
                val executor = findViewById<Spinner>(R.id.inputExecutor).selectedItem.toString()
                val location = findViewById<EditText>(R.id.inputLocation).text.toString()
                // Generate automatic description
                val description = generateDescription(projectTitle, contractNumber, executor)

                // Show automatic description in description field
                withContext(Dispatchers.Main) {
                    findViewById<EditText>(R.id.inputDescription).setText(description)
                }

                // Ensure all actual/remark fields are properly retrieved
                val actual1 = findViewById<EditText>(R.id.inputAktual1)?.text?.toString() ?: ""
                val remark1 = findViewById<EditText>(R.id.inputKeterangan1)?.text?.toString() ?: ""
                val actual2 = findViewById<EditText>(R.id.inputAktual2)?.text?.toString() ?: ""
                val remark2 = findViewById<EditText>(R.id.inputKeterangan2)?.text?.toString() ?: ""
                val actual3 = findViewById<EditText>(R.id.inputAktual3)?.text?.toString() ?: ""
                val remark3 = findViewById<EditText>(R.id.inputKeterangan3)?.text?.toString() ?: ""
                val actual4 = findViewById<EditText>(R.id.inputAktual4)?.text?.toString() ?: ""
                val remark4 = findViewById<EditText>(R.id.inputKeterangan4)?.text?.toString() ?: ""
                val actual5 = findViewById<EditText>(R.id.inputAktual5)?.text?.toString() ?: ""
                val remark5 = findViewById<EditText>(R.id.inputKeterangan5)?.text?.toString() ?: ""
                val actual6 = findViewById<EditText>(R.id.inputAktual6)?.text?.toString() ?: ""
                val remark6 = findViewById<EditText>(R.id.inputKeterangan6)?.text?.toString() ?: ""
                val actual7 = findViewById<EditText>(R.id.inputAktual7)?.text?.toString() ?: ""
                val remark7 = findViewById<EditText>(R.id.inputKeterangan7)?.text?.toString() ?: ""
                val actual8 = findViewById<EditText>(R.id.inputAktual8)?.text?.toString() ?: ""
                val remark8 = findViewById<EditText>(R.id.inputKeterangan8)?.text?.toString() ?: ""
                val actual9 = findViewById<EditText>(R.id.inputAktual9)?.text?.toString() ?: ""
                val remark9 = findViewById<EditText>(R.id.inputKeterangan9)?.text?.toString() ?: ""
                val actual10 = findViewById<EditText>(R.id.inputAktual10)?.text?.toString() ?: ""
                val remark10 = findViewById<EditText>(R.id.inputKeterangan10)?.text?.toString() ?: ""
                val actual11 = findViewById<EditText>(R.id.inputAktual11)?.text?.toString() ?: ""
                val remark11 = findViewById<EditText>(R.id.inputKeterangan11)?.text?.toString() ?: ""
                val actual12 = findViewById<EditText>(R.id.inputAktual12)?.text?.toString() ?: ""
                val remark12 = findViewById<EditText>(R.id.inputKeterangan12)?.text?.toString() ?: ""
                val actual13 = findViewById<EditText>(R.id.inputAktual13)?.text?.toString() ?: ""
                val remark13 = findViewById<EditText>(R.id.inputKeterangan13)?.text?.toString() ?: ""
                val actual14 = findViewById<EditText>(R.id.inputAktual14)?.text?.toString() ?: ""
                val remark14 = findViewById<EditText>(R.id.inputKeterangan14)?.text?.toString() ?: ""
                val actual15 = findViewById<EditText>(R.id.inputAktual15)?.text?.toString() ?: ""
                val remark15 = findViewById<EditText>(R.id.inputKeterangan15)?.text?.toString() ?: ""
                val actual16 = findViewById<EditText>(R.id.inputAktual16)?.text?.toString() ?: ""
                val remark16 = findViewById<EditText>(R.id.inputKeterangan16)?.text?.toString() ?: ""
                val actual17 = findViewById<EditText>(R.id.inputAktual17)?.text?.toString() ?: ""
                val remark17 = findViewById<EditText>(R.id.inputKeterangan17)?.text?.toString() ?: ""
                val actual18 = findViewById<EditText>(R.id.inputAktual18)?.text?.toString() ?: ""
                val remark18 = findViewById<EditText>(R.id.inputKeterangan18)?.text?.toString() ?: ""
                val actual19 = findViewById<EditText>(R.id.inputAktual19)?.text?.toString() ?: ""
                val remark19 = findViewById<EditText>(R.id.inputKeterangan19)?.text?.toString() ?: ""

                val document = PdfDocument()
                var pageCount = 1

                // Page constants
                val pageWidth = 595f
                val pageHeight = 842f
                val marginX = 50f
                val marginTop = 50f
                val marginBottom = 60f  // Add bottom margin for a cleaner footer
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

                // Function to create a new page
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

                // Page header with dynamic logo
                fun drawHeader(executor: String) {
                    val centerX = (marginX + maxX) / 2

                    // Add logo based on executor
                    val zteLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_zte) // ZTE logo
                    val huaweiLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_huawei) // Huawei logo
                    val telkomLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_telkom) // Telkom logo

                    // Logo size
                    val logoWidth = 80 // Logo width
                    val logoHeight = 50 // Logo height
                    val topMargin = marginTop // Top margin for logo

                    // Draw executor logo in top left corner
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

                    // Draw Telkom logo in top right corner
                    val scaledTelkomLogo = Bitmap.createScaledBitmap(telkomLogo, logoWidth, logoHeight, false)
                    canvas.drawBitmap(scaledTelkomLogo, maxX - logoWidth, topMargin, null)

                    // Add space below logo
                    val logoBottomY = topMargin + logoHeight + 20f

                    // Header text
                    canvas.drawText("BERITA ACARA", centerX, logoBottomY, titlePaint)
                    canvas.drawText("SURVEY LOKASI", centerX, logoBottomY + 20f, titlePaint)
                    canvas.drawLine(marginX, logoBottomY + 30f, maxX, logoBottomY + 30f, paint)
                    y = logoBottomY + 40f // Update vertical position
                }

                // Improved page footer
                fun drawFooter() {
                    // Use smaller text size for footer text
                    paint.textSize = 8f // Smaller text size
                    paint.color = Color.BLACK
                    paint.alpha = 220 // Slightly transparent for professional look

                    // Cleaner footer position
                    val footerY = pageHeight - 30f

                    // Thinner separator line
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.5f
                    canvas.drawLine(
                        marginX,          // Line starts at left margin
                        footerY,          // Y position for line
                        pageWidth - marginX, // Line ends at right margin
                        footerY,
                        paint
                    )

                    // Document text
                    paint.style = Paint.Style.FILL
                    paint.textAlign = Paint.Align.LEFT
                    val documentText = "Dokumen ini telah ditandatangani secara elektronik dan merupakan dokumen sah sesuai ketentuan yang berlaku"
                    val pageText = "Halaman ${pageCount - 1}"

                    // Combine both texts with spacing
                    val combinedText = "$documentText     $pageText" // Add spacing between texts
                    paint.textAlign = Paint.Align.LEFT
                    canvas.drawText(
                        combinedText,
                        marginX,          // Text starts at left margin
                        footerY + 15f,    // Y position below line
                        paint
                    )

                    // Reset paint properties
                    paint.textSize = 11f
                    paint.alpha = 255
                    paint.textAlign = Paint.Align.LEFT
                }

                // Add closing text below the last table
                fun drawClosingStatement() {
                    val closingText = "Demikian Berita Acara Hasil Survey ini dibuat berdasarkan kenyataan di lapangan untuk dijadikan pedoman pelaksanaan selanjutnya."
                    val closingMaxWidth = maxX - marginX * 2
                    val closingLines = wrapText(closingText, closingMaxWidth, paint)

                    // Format today's date
                    val currentDate = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
                    // Minimum height needed for text
                    val closingHeight = 18f * closingLines.size + 10f + 20f // Add space for date
                    if (y + closingHeight > pageHeight - marginBottom - 30f) {
                        drawFooter()
                        document.finishPage(page)
                        page = createPage()
                        canvas = page.canvas
                        y = marginTop
                    }

                    y += 20f // 2-line space from last table
                    for (line in closingLines) {
                        canvas.drawText(line, marginX, y, paint)
                        y += 18f
                    }

                    // Write date with bold and right-aligned
                    val boldPaint = Paint(paint).apply {
                        typeface = Typeface.DEFAULT_BOLD
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(currentDate, maxX, y + 10f, boldPaint) // Right-aligned position
                }

                drawHeader(executor)

                // Project Information
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

                // Separator line below location
                canvas.drawLine(marginX, y + 5f, maxX, y + 5f, paint)
                y += 20f // Add space after separator line

                // Description
                val descMaxWidth = maxX - marginX * 2
                y = drawJustifiedText(canvas, description, marginX, y, descMaxWidth, paint) // Draw text and update Y position

                y += 10f // Space before table

                // Table with adjusted columns - IMPORTANT: Give more room to ACTUAL column
                val colX = floatArrayOf(
                    marginX,        // NO
                    marginX + 40f,  // ITEM - wider
                    marginX + 230f, // SATUAN - medium
                    marginX + 300f, // ACTUAL - wider than before
                    marginX + 370f, // KETERANGAN - wide
                    maxX           // Right table border
                )

                // Table header
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

                // Row 1 with special handling due to different format
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

                // After all table rows are finished
                drawClosingStatement() // Add closing text below last table

                // Add signatures - ensure enough space
                val signaturesHeight = 2 * 150f + 20f // 2 rows of signatures + spacing
                if (y + signaturesHeight > pageHeight - marginBottom - 50f) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop
                    drawHeader(executor)
                }

                drawSignaturesWithFormattedTitles(canvas, etTselRegion.text.toString(), y + 30f, paint, boldPaint, executor)

                // Finish current page with signatures
                drawFooter()
                document.finishPage(page)

                // Enhanced photo rendering paint for high quality images
                val photoRenderPaint = Paint().apply {
                    isFilterBitmap = true
                    isAntiAlias = true
                    isDither = true
                }

                // *** IMPROVED PHOTO DOCUMENTATION SECTION (for HashMap<Int, Uri>) ***
                if (photoUris.isNotEmpty()) {
                    // Buat halaman baru untuk foto
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                    // Header halaman foto
                    drawHeader(executor)

                    // Judul section foto
                    val photoTitlePaint = Paint(titlePaint).apply {
                        textAlign = Paint.Align.CENTER
                        textSize = 16f
                    }
                    canvas.drawText("DOKUMENTASI FOTO", (marginX + maxX) / 2, y + 10f, photoTitlePaint)
                    y += 40f

                    // Konstanta grid foto (2 kolom)
                    val photoAreaWidth = maxX - marginX
                    val photoContainerWidth = photoAreaWidth / 2 - 10f
                    val photoWidth = photoContainerWidth
                    val photoHeight = 190f
                    val captionHeight = 30f
                    val rowHeight = photoHeight + captionHeight + 20f

                    var currentCol = 0
                    var photosOnCurrentPage = 0

                    // Urutkan foto berdasar key, lalu pasangkan dengan caption
                    val photoList = photoUris.entries
                        .sortedBy { it.key }
                        .map { entry ->
                            val key = entry.key // biasanya mulai dari 1
                            val uri = entry.value
                            // Caption ambil dari photoLabelTexts jika ada, fallback ke "Photo <key>"
                            val caption = if (key in photoLabelTexts.indices) photoLabelTexts[key] else "Photo ${key + 1}"
                            Pair(uri, caption)
                        }

                    for ((_, photoPair) in photoList.withIndex()) {
                        val (uri, caption) = photoPair

                        // Cek perlu halaman baru
                        if (currentCol == 0 && (photosOnCurrentPage == 4 || y + rowHeight > pageHeight - marginBottom)) {
                            drawFooter()
                            document.finishPage(page)
                            page = createPage()
                            canvas = page.canvas
                            y = marginTop
                            drawHeader(executor)
                            canvas.drawText("DOKUMENTASI FOTO", (marginX + maxX) / 2, y + 10f, photoTitlePaint)
                            y += 40f
                            photosOnCurrentPage = 0
                        }

                        // Hitung posisi kolom
                        val photoX = if (currentCol == 0) marginX else marginX + photoContainerWidth + 20f

                        // Gambar caption
                        canvas.drawText(caption, photoX, y + 12f, boldPaint)

                        // Frame foto
                        val photoRect = RectF(photoX, y + 15f, photoX + photoWidth, y + 15f + photoHeight)

                        // Render foto kualitas tinggi
                        try {
                            contentResolver.openInputStream(uri)?.use { inputStream ->
                                val options = BitmapFactory.Options().apply {
                                    inPreferredConfig = Bitmap.Config.ARGB_8888
                                    inJustDecodeBounds = true
                                }
                                BitmapFactory.decodeStream(inputStream, null, options)
                                val sampleSize = calculateOptimalSampleSize(
                                    options.outWidth, options.outHeight,
                                    photoWidth.toInt(), photoHeight.toInt()
                                )
                                options.inJustDecodeBounds = false
                                options.inSampleSize = sampleSize
                                contentResolver.openInputStream(uri)?.use { input2 ->
                                    val bitmap = BitmapFactory.decodeStream(input2, null, options)
                                    if (bitmap != null) {
                                        val originalRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                                        val displayWidth: Float
                                        val displayHeight: Float
                                        if (originalRatio > 1) {
                                            displayWidth = photoWidth
                                            displayHeight = photoWidth / originalRatio
                                        } else {
                                            displayHeight = photoHeight
                                            displayWidth = photoHeight * originalRatio
                                        }
                                        val xOffset = photoX + (photoWidth - displayWidth) / 2
                                        val yOffset = y + 15f + (photoHeight - displayHeight) / 2
                                        val destRect = RectF(
                                            xOffset, yOffset,
                                            xOffset + displayWidth, yOffset + displayHeight
                                        )
                                        canvas.drawBitmap(bitmap, null, destRect, photoRenderPaint)
                                        val borderPaint = Paint().apply {
                                            style = Paint.Style.STROKE
                                            strokeWidth = 0.5f
                                            color = Color.BLACK
                                        }
                                        canvas.drawRect(destRect, borderPaint)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("PDF", "Error drawing photo: ${e.message}")
                            val borderPaint = Paint().apply {
                                style = Paint.Style.STROKE
                                strokeWidth = 0.5f
                                color = Color.BLACK
                            }
                            canvas.drawRect(photoRect, borderPaint)
                            canvas.drawText("Error loading photo", photoX + 20, y + 100, paint)
                        }

                        // Update posisi grid
                        currentCol = (currentCol + 1) % 2
                        photosOnCurrentPage++

                        // Pindah baris jika sudah dua kolom
                        if (currentCol == 0) {
                            y += rowHeight
                        }
                    }

                    // Halaman foto terakhir, beri footer
                    drawFooter()
                    document.finishPage(page)
                }

                // Save document with better error handling
                try {
                    // Try to save in public Downloads directory first
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }

                    // Create unique filename
                    var fileIndex = 1
                    var file: File
                    do {
                        val fileName = "SurveyLokasi$fileIndex.pdf"
                        file = File(downloadsDir, fileName)
                        fileIndex++
                    } while (file.exists())

                    // Save file
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

                    // If fails, try saving to app directory as fallback
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
                            throw e2 // Re-throw exception if still fails
                        }
                    } else {
                        throw e // Re-throw exception if no fallback
                    }
                }

            } catch (e: Exception) {
                Log.e("BASurveyBig", "Error generating PDF: ${e.message}")
                e.printStackTrace() // Log stacktrace for debugging

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@BASurveyBigActivity,
                        "Gagal membuat PDF: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Use empty "dummy" file as last fallback to avoid crash
        if (createdFile == null) {
            val fallbackEmptyFile = File(cacheDir, "empty_survey_${System.currentTimeMillis()}.pdf")
            try {
                fallbackEmptyFile.createNewFile()
                return fallbackEmptyFile
            } catch (e: Exception) {
                Log.e("BASurveyBig", "Fatal error creating fallback file: ${e.message}")
                throw IllegalStateException("PDF file could not be created: ${e.message}")
            }
        }

        return createdFile!!
    }

    // Function to draw signatures grid with formatted titles
    private fun drawSignaturesWithFormattedTitles(
        canvas: Canvas,
        region: String,
        yStart: Float,
        paint: Paint,
        boldPaint: Paint,
        executor: String
    ) {
        val boxWidth = (595 - (marginX * 2)) / 3 // Signature box width
        val signatureBoxHeight = 150f // Signature box height
        var y = yStart

        // Paint for drawing box outline
        val boxPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        // Function to draw formatted company name with special format (2 or 3 lines)
        fun drawFormattedTitle(
            canvas: Canvas,
            lines: List<String>,
            x: Float,
            y: Float,
            maxWidth: Float,
            boldPaint: Paint
        ): Float {
            val lineHeight = boldPaint.textSize + 4f // Height of each line
            var currentY = y

            for (line in lines) {
                canvas.drawText(line, x, currentY, boldPaint)
                currentY += lineHeight
            }

            return currentY // Return Y position after last text
        }

        // Function to draw signature box
        fun drawSignatureBox(
            lines: List<String>,
            name: String,
            nik: String,
            signature: Drawable?,
            x: Float,
            y: Float
        ) {
            // Draw box
            val rect = RectF(x, y, x + boxWidth, y + signatureBoxHeight)
            canvas.drawRect(rect, boxPaint)

            // Write company name with 2 or 3-line format
            val titleY = drawFormattedTitle(
                canvas,
                lines,
                x + 10f,
                y + 20f,
                boxWidth - 20f,
                boldPaint
            )

            // Draw signature in center of box
            val signatureY = titleY + 10f
            if (signature != null) {
                try {
                    val bitmap = (signature as BitmapDrawable).bitmap

                    // Enhanced signature rendering
                    val signatureWidth = 100f
                    val signatureHeight = 50f

                    val renderPaint = Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                        isDither = true
                    }

                    val destRect = RectF(
                        x + (boxWidth / 2 - signatureWidth / 2),
                        signatureY,
                        x + (boxWidth / 2 + signatureWidth / 2),
                        signatureY + signatureHeight
                    )

                    canvas.drawBitmap(bitmap, null, destRect, renderPaint)
                } catch (e: Exception) {
                    Log.e("BASurveyBig", "Error rendering signature: ${e.message}")
                }
            }

            // Write name and NIK below signature
            val nameY = y + signatureBoxHeight - 40f // Fixed aligned position
            canvas.drawText("($name)", x + 10f, nameY, paint)
            canvas.drawText("NIK: $nik", x + 10f, nameY + 20f, paint)
        }

        // Get signature inputs
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

        // First row (Executor, TIF, TELKOM)
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

        // Second row (NOP, RTPDS, RTPE)
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

    // Function to draw justified text using wrapText
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

        // Set more spacious line spacing (textSize + 10f)
        val lineSpacing = paint.textSize + 10f

        // For more spacious word spacing, add extra space
        val extraWordSpacing = 4f // px, can be increased for wider spacing

        for ((i, line) in lines.withIndex()) {
            val words = line.split(" ")
            val lineWidth = paint.measureText(line)
            val gapCount = words.size - 1

            if (gapCount > 0 && i != lines.lastIndex) {
                // calculate inter-word spacing (wider than usual)
                val extraSpace = ((maxWidth - lineWidth) / gapCount) + extraWordSpacing
                var startX = x
                for (word in words) {
                    canvas.drawText(word, startX, currentY, paint)
                    startX += paint.measureText(word) + extraSpace
                }
            } else {
                // last line without justification
                canvas.drawText(line, x, currentY, paint)
            }
            currentY += lineSpacing
        }
        return currentY
    }

    // Improved wrapText function with better handling of very long words
    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        // If text is empty, return empty list
        if (text.isEmpty()) return listOf("")

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        // Handle single long words that exceed maxWidth
        for (word in words) {
            // If single word is longer than maxWidth, break it into parts
            if (paint.measureText(word) > maxWidth) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = ""
                }

                // Break long word into several parts
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

                    i = maxOf(1, i) // Take at least 1 character
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