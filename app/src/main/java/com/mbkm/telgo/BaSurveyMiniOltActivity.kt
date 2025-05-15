package com.mbkm.telgo

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class BaSurveyMiniOltActivity : AppCompatActivity() {

    // UI components for main layout
    private lateinit var tabLayout: TabLayout
    private lateinit var formContainer: CardView
    private lateinit var searchContainer: CardView
    private lateinit var btnBack: ImageButton

    private lateinit var etHeaderNo: EditText

    // Form input fields
    private lateinit var etLocation: EditText
    private lateinit var etNoIhld: EditText
    private lateinit var platformDropdown: AutoCompleteTextView
    private lateinit var etSiteProvider: AutoCompleteTextView
    private lateinit var etContractNumber: EditText
    private lateinit var tvCurrentDate: TextView

    // PM/PBM Dropdowns
    private lateinit var rackDropdown: AutoCompleteTextView
    private lateinit var rectifierDropdown: AutoCompleteTextView
    private lateinit var dcPowerDropdown: AutoCompleteTextView
    private lateinit var batteryDropdown: AutoCompleteTextView
    private lateinit var mcbDropdown: AutoCompleteTextView
    private lateinit var groundingDropdown: AutoCompleteTextView
    private lateinit var indoorRoomDropdown: AutoCompleteTextView
    private lateinit var acPowerDropdown: AutoCompleteTextView
    private lateinit var uplinkDropdown: AutoCompleteTextView
    private lateinit var conduitDropdown: AutoCompleteTextView

    // Table inputs
    private lateinit var etRackResult: EditText
    private lateinit var etRackProposed: EditText
    private lateinit var etRectifierResult: EditText
    private lateinit var etRectifierProposed: EditText
    private lateinit var etDcPowerResult: EditText
    private lateinit var etDcPowerProposed: EditText
    private lateinit var etBatteryResult: EditText
    private lateinit var etBatteryProposed: EditText
    private lateinit var etMcbResult: EditText
    private lateinit var etMcbProposed: EditText
    private lateinit var etGroundingResult: EditText
    private lateinit var etGroundingProposed: EditText
    private lateinit var etIndoorRoomResult: EditText
    private lateinit var etIndoorRoomProposed: EditText
    private lateinit var etAcPowerResult: EditText
    private lateinit var etAcPowerProposed: EditText
    private lateinit var etUplinkResult: EditText
    private lateinit var etUplinkProposed: EditText
    private lateinit var etConduitResult: EditText
    private lateinit var etConduitProposed: EditText

    // Signature fields
    private lateinit var etZteName: EditText
    private lateinit var etTselNopName: EditText
    private lateinit var etTselRtpdsName: EditText
    private lateinit var etTselRtpeNfName: EditText
    private lateinit var etTelkomName: EditText
    private lateinit var etTifName: EditText
    private lateinit var tvTselRegion: EditText

    // NIK fields
    private lateinit var etZteNik: EditText
    private lateinit var etTselNopNik: EditText
    private lateinit var etTselRtpdsNik: EditText
    private lateinit var etTselRtpeNfNik: EditText
    private lateinit var etTelkomNik: EditText
    private lateinit var etTifNik: EditText

    // Company Name TextViews
    private lateinit var tvZteCompany: TextView

    // Signature image buttons
    private lateinit var btnZteSignature: Button
    private lateinit var btnTselNopSignature: Button
    private lateinit var btnTselRtpdsSignature: Button
    private lateinit var btnTselRtpeNfSignature: Button
    private lateinit var btnTelkomSignature: Button
    private lateinit var btnTifSignature: Button

    // Signature image views
    private lateinit var imgZteSignature: ImageView
    private lateinit var imgTselNopSignature: ImageView
    private lateinit var imgTselRtpdsSignature: ImageView
    private lateinit var imgTselRtpeNfSignature: ImageView
    private lateinit var imgTelkomSignature: ImageView
    private lateinit var imgTifSignature: ImageView

    // Photo upload buttons and image views
    private lateinit var photoContainers: Array<LinearLayout>
    private lateinit var photoButtons: Array<Button>
    private lateinit var photoImageViews: Array<ImageView>
    private lateinit var photoLabels: Array<String>

    // Submit and search components
    private lateinit var btnSubmitForm: Button
    private lateinit var btnGeneratePdf: Button
    private lateinit var searchView: SearchView
    private lateinit var rvSearchResults: RecyclerView

    // Firebase components
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Data
    private var currentPhotoIndex = 0
    private val photoUris = HashMap<Int, Uri>()
    private val signatureUris = HashMap<Int, Uri>()
    private val searchResults = ArrayList<BaSurveyMiniOltModel>()
    private lateinit var searchAdapter: BaSurveyMiniOltAdapter

    // Request codes
    private val REQUEST_CAMERA_PERMISSION = 100
    private val REQUEST_STORAGE_PERMISSION = 101
    private val REQUEST_IMAGE_CAPTURE = 102
    private val REQUEST_GALLERY = 103
    private val REQUEST_SIGNATURE_ZTE = 201
    private val REQUEST_SIGNATURE_TSEL_NOP = 202
    private val REQUEST_SIGNATURE_TSEL_RTPDS = 203
    private val REQUEST_SIGNATURE_TSEL_RTPE = 204
    private val REQUEST_SIGNATURE_TELKOM = 205
    private val REQUEST_SIGNATURE_TIF = 206
    private val REQUEST_PERMISSION_SETTINGS = 999

    // Permission tracking
    private var pendingGalleryLaunch = false
    private var permissionExplanationShown = false

    private var currentPhotoUri: Uri? = null

    // Signature keys for Firebase
    private val signatureKeys = arrayOf("zte", "tselNop", "tselRtpds", "tselRtpeNf", "telkom", "tif")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ba_survey_mini_olt)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI components
        initializeUI()

        // Request permissions immediately
        requestRequiredPermissions()

        // Set up tab listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> { // Form tab
                        formContainer.visibility = View.VISIBLE
                        searchContainer.visibility = View.GONE
                    }
                    1 -> { // Search tab
                        formContainer.visibility = View.GONE
                        searchContainer.visibility = View.VISIBLE
                        loadAllSurveys()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Set current date using Indonesian locale
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        tvCurrentDate.text = sdf.format(Date())

        // Set up button listeners
        setupButtonListeners()

        // Set up search adapter
        setupSearchAdapter()

        // Set up dropdowns
        setupSiteProviderDropdown()
        setupPlatformDropdown()
        setupPmPbmDropdowns()
    }

    private fun requestRequiredPermissions() {
        val permissions = ArrayList<String>()

        // Check for camera permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        // Check for storage permissions - handle different Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ needs specific photo picker permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            // Older Android versions need storage access
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)

                // For writing files on older Android
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_PERMISSION_SETTINGS)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PERMISSION_SETTINGS || requestCode == REQUEST_STORAGE_PERMISSION) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()

                // If we were waiting to open gallery, do it now
                if (pendingGalleryLaunch) {
                    pendingGalleryLaunch = false
                    openGalleryInternal()
                }
            } else {
                // Show explanation dialog if permissions denied
                showPermissionExplanationDialog()
            }
        }
    }

    private fun showPermissionExplanationDialog() {
        if (permissionExplanationShown) return

        permissionExplanationShown = true

        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("This app needs storage permissions to access your gallery photos. Please grant these permissions in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                // Open app settings so user can enable permissions
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivityForResult(intent, REQUEST_PERMISSION_SETTINGS)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun initializeUI() {
        // Main layout components
        tabLayout = findViewById(R.id.tabLayout)
        formContainer = findViewById(R.id.formContainer)
        searchContainer = findViewById(R.id.searchContainer)
        btnBack = findViewById(R.id.btnBack)

        // Form input fields
        etLocation = findViewById(R.id.etLocation)
        etNoIhld = findViewById(R.id.etNoIhld)
        platformDropdown = findViewById(R.id.platformDropdown)
        etSiteProvider = findViewById(R.id.etSiteProvider)
        etContractNumber = findViewById(R.id.etContractNumber)
        tvCurrentDate = findViewById(R.id.tvCurrentDate)

        // PM/PBM dropdowns
        rackDropdown = findViewById(R.id.rackDropdown)
        rectifierDropdown = findViewById(R.id.rectifierDropdown)
        dcPowerDropdown = findViewById(R.id.dcPowerDropdown)
        batteryDropdown = findViewById(R.id.batteryDropdown)
        mcbDropdown = findViewById(R.id.mcbDropdown)
        groundingDropdown = findViewById(R.id.groundingDropdown)
        indoorRoomDropdown = findViewById(R.id.indoorRoomDropdown)
        acPowerDropdown = findViewById(R.id.acPowerDropdown)
        uplinkDropdown = findViewById(R.id.uplinkDropdown)
        conduitDropdown = findViewById(R.id.conduitDropdown)

        // Table inputs
        etRackResult = findViewById(R.id.etRackResult)
        etRackProposed = findViewById(R.id.etRackProposed)
        etRectifierResult = findViewById(R.id.etRectifierResult)
        etRectifierProposed = findViewById(R.id.etRectifierProposed)
        etDcPowerResult = findViewById(R.id.etDcPowerResult)
        etDcPowerProposed = findViewById(R.id.etDcPowerProposed)
        etBatteryResult = findViewById(R.id.etBatteryResult)
        etBatteryProposed = findViewById(R.id.etBatteryProposed)
        etMcbResult = findViewById(R.id.etMcbResult)
        etMcbProposed = findViewById(R.id.etMcbProposed)
        etGroundingResult = findViewById(R.id.etGroundingResult)
        etGroundingProposed = findViewById(R.id.etGroundingProposed)
        etIndoorRoomResult = findViewById(R.id.etIndoorRoomResult)
        etIndoorRoomProposed = findViewById(R.id.etIndoorRoomProposed)
        etAcPowerResult = findViewById(R.id.etAcPowerResult)
        etAcPowerProposed = findViewById(R.id.etAcPowerProposed)
        etUplinkResult = findViewById(R.id.etUplinkResult)
        etUplinkProposed = findViewById(R.id.etUplinkProposed)
        etConduitResult = findViewById(R.id.etConduitResult)
        etConduitProposed = findViewById(R.id.etConduitProposed)

        // Signature fields
        etZteName = findViewById(R.id.etZteName)
        etTselNopName = findViewById(R.id.etTselNopName)
        etTselRtpdsName = findViewById(R.id.etTselRtpdsName)
        etTselRtpeNfName = findViewById(R.id.etTselRtpeNfName)
        etTelkomName = findViewById(R.id.etTelkomName)
        etTifName = findViewById(R.id.etTifName)
        tvTselRegion = findViewById(R.id.etTselRegion)

        // NIK fields
        etZteNik = findViewById(R.id.etZteNik)
        etTselNopNik = findViewById(R.id.etTselNopNik)
        etTselRtpdsNik = findViewById(R.id.etTselRtpdsNik)
        etTselRtpeNfNik = findViewById(R.id.etTselRtpeNfNik)
        etTelkomNik = findViewById(R.id.etTelkomNik)
        etTifNik = findViewById(R.id.etTifNik)

        // Company name TextView
        tvZteCompany = findViewById(R.id.tvZteCompany)

        // Signature buttons
        btnZteSignature = findViewById(R.id.btnZteSignature)
        btnTselNopSignature = findViewById(R.id.btnTselNopSignature)
        btnTselRtpdsSignature = findViewById(R.id.btnTselRtpdsSignature)
        btnTselRtpeNfSignature = findViewById(R.id.btnTselRtpeNfSignature)
        btnTelkomSignature = findViewById(R.id.btnTelkomSignature)
        btnTifSignature = findViewById(R.id.btnTifSignature)

        // Signature image views
        imgZteSignature = findViewById(R.id.imgZteSignature)
        imgTselNopSignature = findViewById(R.id.imgTselNopSignature)
        imgTselRtpdsSignature = findViewById(R.id.imgTselRtpdsSignature)
        imgTselRtpeNfSignature = findViewById(R.id.imgTselRtpeNfSignature)
        imgTelkomSignature = findViewById(R.id.imgTelkomSignature)
        imgTifSignature = findViewById(R.id.imgTifSignature)

        // Dan inisialisasi di initializeUI()
        etHeaderNo = findViewById(R.id.etHeaderNo)

        // Photo containers, buttons and image views
        try {
            photoContainers = Array(16) { findViewById(resources.getIdentifier("photoContainer${it+1}", "id", packageName)) }
            photoButtons = Array(16) { findViewById(resources.getIdentifier("btnUploadPhoto${it+1}", "id", packageName)) }
            photoImageViews = Array(16) { findViewById(resources.getIdentifier("imgPhoto${it+1}", "id", packageName)) }
        } catch (e: Exception) {
            Log.e("BaSurveyMiniOlt", "Error initializing photo views: ${e.message}")
            photoContainers = emptyArray()
            photoButtons = emptyArray()
            photoImageViews = emptyArray()
        }

        // Photo labels
        photoLabels = arrayOf(
            "AKSES GERBANG", "NAME PLATE", "OUTDOOR", "SHELTER",
            "PONDASI", "GROUNDING BASBAR", "CATUAN POWER DC",
            "PROPOSED DUAL SOURCE POWER DC", "RECTIFIER", "PENGUKURAN CATUAN POWER AC", "POWER AC DI PANEL KWH EXCITING",
            "PORT OTB EXCITING", "CABINET METRO-E (ME ROOM)", "METRO-E",
            "ALTERNATIF DILUAR SITE (OUTDOOR)", "DI AREA ODC"
        )

        // Submit and search components
        btnSubmitForm = findViewById(R.id.btnSubmitForm)
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf)
        searchView = findViewById(R.id.searchView)
        rvSearchResults = findViewById(R.id.rvSearchResults)
    }

    private fun setupPlatformDropdown() {
        val platformOptions = listOf(
            "PT. ZTE INDONESIA",
            "PT. Huawei Tech Investment"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, platformOptions)
        platformDropdown.setAdapter(adapter)

        // Update signature company name when platform changes
        platformDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedPlatform = platformOptions[position]
            tvZteCompany.text = "$selectedPlatform - TIM SURVEY"
        }
    }

    private fun setupPmPbmDropdowns() {
        val pmPbmOptions = listOf("PM", "PBM")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, pmPbmOptions)

        // Set up each dropdown with the PM/PBM adapter
        rackDropdown.setAdapter(adapter)
        rectifierDropdown.setAdapter(adapter)
        dcPowerDropdown.setAdapter(adapter)
        batteryDropdown.setAdapter(adapter)
        mcbDropdown.setAdapter(adapter)
        groundingDropdown.setAdapter(adapter)
        indoorRoomDropdown.setAdapter(adapter)
        acPowerDropdown.setAdapter(adapter)
        uplinkDropdown.setAdapter(adapter)
        conduitDropdown.setAdapter(adapter)

        // Set default values
        rackDropdown.setText("PM", false)
        rectifierDropdown.setText("PBM", false)
        dcPowerDropdown.setText("PBM", false)
        batteryDropdown.setText("PBM", false)
        mcbDropdown.setText("PM", false)
        groundingDropdown.setText("PM", false)
        indoorRoomDropdown.setText("PM", false)
        acPowerDropdown.setText("PM", false)
        uplinkDropdown.setText("PBM", false)
        conduitDropdown.setText("PM", false)
    }

    private fun setupSiteProviderDropdown() {
        val siteProviderOptions = listOf(
            "DMT", "DMT - Bifurcation", "DMT- Reseller", "IBS", "NO NEED SITAC",
            "NOT READY", "PROTELINDO", "PT Centratama Menara Indonesia",
            "PT Gihon Telekomunikasi Indonesia", "PT Quattro International",
            "PT.Era Bangun Towerindo", "PT.Protelindo", "READY", "STO ROOM",
            "STP", "TBG", "TELKOM", "TELKOMSEL", "TSEL", "PT Daya Mitra Telekomunikasi"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, siteProviderOptions)
        etSiteProvider.setAdapter(adapter)
    }

    private fun setupButtonListeners() {
        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Submit button
        btnSubmitForm.setOnClickListener {
            if (validateForm()) {
                showConfirmationDialog()
            }
        }

        // Generate PDF button
        btnGeneratePdf.setOnClickListener {
            if (validateForm()) {
                generatePdf()
            }
        }

        // Signature buttons
        btnZteSignature.setOnClickListener {
            openSignatureActivity(REQUEST_SIGNATURE_ZTE)
        }

        btnTselNopSignature.setOnClickListener {
            openSignatureActivity(REQUEST_SIGNATURE_TSEL_NOP)
        }

        btnTselRtpdsSignature.setOnClickListener {
            openSignatureActivity(REQUEST_SIGNATURE_TSEL_RTPDS)
        }

        btnTselRtpeNfSignature.setOnClickListener {
            openSignatureActivity(REQUEST_SIGNATURE_TSEL_RTPE)
        }

        btnTelkomSignature.setOnClickListener {
            openSignatureActivity(REQUEST_SIGNATURE_TELKOM)
        }

        btnTifSignature.setOnClickListener {
            openSignatureActivity(REQUEST_SIGNATURE_TIF)
        }

        // Photo buttons
        for (i in photoButtons.indices) {
            photoButtons[i].setOnClickListener {
                currentPhotoIndex = i
                showPhotoSourceDialog()
            }
        }
    }

    private fun setupSearchAdapter() {
        searchAdapter = BaSurveyMiniOltAdapter(searchResults) { baSurvey ->
            // Handle click on search result - view details or download PDF
            showSurveyOptionsDialog(baSurvey)
        }

        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = searchAdapter

        // Set up search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                filterSearchResults(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                filterSearchResults(newText)
                return true
            }
        })
    }

    private fun filterSearchResults(query: String) {
        val filteredResults = ArrayList<BaSurveyMiniOltModel>()

        for (survey in searchResults) {
            if (survey.location.contains(query, ignoreCase = true) ||
                survey.noIhld.contains(query, ignoreCase = true) ||
                survey.platform.contains(query, ignoreCase = true) ||
                survey.siteProvider.contains(query, ignoreCase = true)) {

                filteredResults.add(survey)
            }
        }

        searchAdapter.updateData(filteredResults)
    }

    private fun loadAllSurveys() {
        // Show loading indicator
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Fetch all BA Survey Mini OLT documents
        firestore.collection("ba_survey_mini_olt")
            .get()
            .addOnSuccessListener { documents ->
                searchResults.clear()

                for (document in documents) {
                    val data = document.data
                    val baSurvey = BaSurveyMiniOltModel(
                        id = document.id,
                        location = data["location"] as String,
                        noIhld = data["noIhld"] as String,
                        platform = data["platform"] as String,
                        siteProvider = data["siteProvider"] as String,
                        contractNumber = data["contractNumber"] as String,
                        createdBy = data["createdBy"] as String,
                        createdAt = data["createdAt"] as String
                    )

                    searchResults.add(baSurvey)
                }

                searchAdapter.updateData(searchResults)
                loadingDialog.dismiss()
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSurveyOptionsDialog(baSurvey: BaSurveyMiniOltModel) {
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

    private fun viewSurveyDetails(baSurvey: BaSurveyMiniOltModel) {
        // Navigate to detail view activity
        val intent = Intent(this, BaSurveyMiniOltDetailActivity::class.java)
        intent.putExtra("SURVEY_ID", baSurvey.id)
        startActivity(intent)
    }

    private fun downloadSurveyPdf(baSurvey: BaSurveyMiniOltModel) {
        // Show loading indicator
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Download PDF from Firebase Storage
        val pdfRef = storage.reference.child("ba_survey_mini_olt_pdf/${baSurvey.id}.pdf")

        val localFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "BA_Survey_Mini_OLT_${baSurvey.location}.pdf")

        pdfRef.getFile(localFile)
            .addOnSuccessListener {
                loadingDialog.dismiss()

                // Share/open the PDF
                val uri = FileProvider.getUriForFile(
                    this,
                    "com.mbkm.telgo.fileprovider",
                    localFile
                )

                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(uri, "application/pdf")
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

                startActivity(Intent.createChooser(intent, "Open PDF with..."))
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Error downloading PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openSignatureActivity(requestCode: Int) {
        val intent = Intent(this, SignatureActivity::class.java)
        startActivityForResult(intent, requestCode)
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")

        AlertDialog.Builder(this)
            .setTitle("Select Photo Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (checkCameraPermission()) {
                            openCamera()
                        }
                    }
                    1 -> {
                        if (checkStoragePermission()) {
                            openGallery()
                        }
                    }
                }
            }
            .show()
    }

    private fun checkCameraPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
            return false
        }
        return true
    }

    private fun checkStoragePermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            pendingGalleryLaunch = true
            ActivityCompat.requestPermissions(this, arrayOf(permission), REQUEST_STORAGE_PERMISSION)
            return false
        }
        return true
    }

    private fun openCamera() {
        // Create a temporary file for the full-resolution photo
        val photoFile = try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "JPEG_${timeStamp}_"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            File.createTempFile(fileName, ".jpg", storageDir)
        } catch (e: IOException) {
            Log.e("Camera", "Error creating image file: ${e.message}")
            null
        }

        photoFile?.let {
            val photoURI = FileProvider.getUriForFile(
                this,
                "com.mbkm.telgo.fileprovider",
                it
            )

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

            try {
                // Store the URI temporarily to retrieve it in onActivityResult
                currentPhotoUri = photoURI
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } catch (e: Exception) {
                Log.e("Camera", "Error launching camera: ${e.message}")
                Toast.makeText(this, "Could not open camera app", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        if (!checkStoragePermission()) {
            // Will call openGalleryInternal() when permission is granted
            return
        }

        openGalleryInternal()
    }

    private fun openGalleryInternal() {
        // For Android 13+, use the modern photo picker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val intent = Intent(MediaStore.ACTION_PICK_IMAGES)
                intent.putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, 1)
                startActivityForResult(intent, REQUEST_GALLERY)
                return
            } catch (e: Exception) {
                Log.e("Gallery", "Error with modern photo picker: ${e.message}")
            }
        }

        // For Android 10-12, prefer ACTION_OPEN_DOCUMENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(intent, REQUEST_GALLERY)
                return
            } catch (e: Exception) {
                Log.e("Gallery", "Error with ACTION_OPEN_DOCUMENT: ${e.message}")
            }
        }

        // Fallback methods for all Android versions
        try {
            // Simple GET_CONTENT approach
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_GALLERY)
            return
        } catch (e: Exception) {
            Log.e("Gallery", "Error with ACTION_GET_CONTENT: ${e.message}")
        }

        try {
            // Last resort: MediaStore approach
            val intent = Intent(Intent.ACTION_PICK)
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            startActivityForResult(intent, REQUEST_GALLERY)
            return
        } catch (e: Exception) {
            Log.e("Gallery", "Error with PICK from MediaStore: ${e.message}")
        }

        // If we got here, nothing worked - show an error
        Toast.makeText(this, "Could not open gallery. Please check your device permissions.", Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    try {
                        // Process the full resolution image from URI
                        val uri = currentPhotoUri
                        if (uri != null) {
                            // Store URI for later use with high-quality retrieval
                            photoUris[currentPhotoIndex] = uri

                            // Properly decode for maximum quality
                            val options = BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                                inSampleSize = 1  // Full resolution
                            }

                            contentResolver.openInputStream(uri)?.use { input ->
                                val bitmap = BitmapFactory.decodeStream(input, null, options)
                                if (bitmap != null) {
                                    // Display high quality image
                                    photoImageViews[currentPhotoIndex].apply {
                                        scaleType = ImageView.ScaleType.FIT_CENTER
                                        setImageBitmap(bitmap)
                                        visibility = View.VISIBLE
                                    }
                                    Log.d("Camera", "Full resolution photo loaded successfully")
                                } else {
                                    // Fallback to simple URI display if bitmap loading fails
                                    photoImageViews[currentPhotoIndex].apply {
                                        scaleType = ImageView.ScaleType.FIT_CENTER
                                        setImageURI(uri)
                                        visibility = View.VISIBLE
                                    }
                                    Log.d("Camera", "Fallback to URI display")
                                }
                            }
                        } else {
                            // Fallback to thumbnail if URI approach failed
                            val imageBitmap = data?.extras?.get("data") as? Bitmap
                            if (imageBitmap != null) {
                                val uri = getImageUriFromBitmap(imageBitmap)
                                photoUris[currentPhotoIndex] = uri

                                photoImageViews[currentPhotoIndex].apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                    setImageBitmap(imageBitmap)
                                    visibility = View.VISIBLE
                                }
                            } else {
                                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Camera", "Error processing camera image: ${e.message}")
                        e.printStackTrace()
                        Toast.makeText(this, "Error processing camera image", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_GALLERY -> {
                    try {
                        val uri = data?.data
                        if (uri != null) {
                            // Store URI for later use with high-quality retrieval
                            photoUris[currentPhotoIndex] = uri

                            // Load a high-quality but memory-efficient bitmap for UI display
                            val bitmap = loadBitmapFromUri(uri)

                            if (bitmap != null) {
                                // Set up the ImageView
                                photoImageViews[currentPhotoIndex].apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                    setImageBitmap(bitmap)
                                    visibility = View.VISIBLE
                                }
                                Log.d("Gallery", "Image loaded successfully from uri: $uri")
                            } else {
                                // Fallback to basic URI loading
                                photoImageViews[currentPhotoIndex].apply {
                                    scaleType = ImageView.ScaleType.FIT_CENTER
                                    setImageURI(uri)
                                    visibility = View.VISIBLE
                                }
                                Log.d("Gallery", "Fallback image loading from uri: $uri")
                            }
                        } else {
                            Log.e("Gallery", "No URI returned from gallery selection")
                            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("Gallery", "Error processing gallery image: ${e.message}")
                        Toast.makeText(this, "Error processing gallery image", Toast.LENGTH_SHORT).show()
                    }
                }
                REQUEST_PERMISSION_SETTINGS -> {
                    // User returned from settings, check if we can proceed with pending operations
                    if (pendingGalleryLaunch && checkStoragePermission()) {
                        pendingGalleryLaunch = false
                        openGalleryInternal()
                    }
                }
                REQUEST_SIGNATURE_ZTE,
                REQUEST_SIGNATURE_TSEL_NOP,
                REQUEST_SIGNATURE_TSEL_RTPDS,
                REQUEST_SIGNATURE_TSEL_RTPE,
                REQUEST_SIGNATURE_TELKOM,
                REQUEST_SIGNATURE_TIF -> {
                    // Get URI from result
                    val uri = data?.getParcelableExtra<Uri>("signature_uri")

                    if (uri != null) {
                        // Determine which signature we're handling
                        val signatureIndex = when (requestCode) {
                            REQUEST_SIGNATURE_ZTE -> 0
                            REQUEST_SIGNATURE_TSEL_NOP -> 1
                            REQUEST_SIGNATURE_TSEL_RTPDS -> 2
                            REQUEST_SIGNATURE_TSEL_RTPE -> 3
                            REQUEST_SIGNATURE_TELKOM -> 4
                            REQUEST_SIGNATURE_TIF -> 5
                            else -> -1
                        }

                        if (signatureIndex >= 0) {
                            try {
                                // Take a persistent permission to access this URI
                                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                contentResolver.takePersistableUriPermission(uri, takeFlags)
                            } catch (e: Exception) {
                                // Not all URIs support persistable permissions, which is OK
                                Log.d("Signature", "Could not take persistable permission: ${e.message}")
                            }

                            // Create a local copy if possible to avoid permission issues
                            val localUri = try {
                                // Try to make a local copy of the URI content
                                contentResolver.openInputStream(uri)?.use { input ->
                                    // Create a temporary file in the app's cache directory
                                    val file = File(cacheDir, "signature_${signatureIndex}_${System.currentTimeMillis()}.png")
                                    FileOutputStream(file).use { output ->
                                        input.copyTo(output)
                                    }
                                    Uri.fromFile(file) // Use the file URI which we fully control
                                }
                            } catch (e: Exception) {
                                // If copying fails, use the original URI
                                Log.e("Signature", "Failed to copy URI to local file: ${e.message}")
                                uri
                            }

                            // Store URI for upload
                            signatureUris[signatureIndex] = localUri ?: uri

                            // Show the signature in the corresponding ImageView
                            val imageView = when (signatureIndex) {
                                0 -> imgZteSignature
                                1 -> imgTselNopSignature
                                2 -> imgTselRtpdsSignature
                                3 -> imgTselRtpeNfSignature
                                4 -> imgTelkomSignature
                                5 -> imgTifSignature
                                else -> null
                            }

                            imageView?.setImageURI(localUri ?: uri)
                            imageView?.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    // Helper method to load bitmap from URI
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
            Log.e("BitmapLoader", "Error loading bitmap: ${e.message}")
            null
        }
    }

    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        // Use 100% quality JPEG compression
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Survey_Photo_${System.currentTimeMillis()}", null)
        return Uri.parse(path)
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Check required fields
        if (etLocation.text.toString().isEmpty()) {
            etLocation.error = "Required field"
            isValid = false
        }

        if (etNoIhld.text.toString().isEmpty()) {
            etNoIhld.error = "Required field"
            isValid = false
        }

        if (platformDropdown.text.toString().isEmpty()) {
            platformDropdown.error = "Required field"
            isValid = false
        }

        if (etSiteProvider.text.toString().isEmpty()) {
            etSiteProvider.error = "Required field"
            isValid = false
        }

        if (etContractNumber.text.toString().isEmpty()) {
            etContractNumber.error = "Required field"
            isValid = false
        }

        // Check if region is entered
        if (tvTselRegion.text.toString().isEmpty()) {
            tvTselRegion.error = "Required field"
            isValid = false
        }

        // Check if at least one signature is uploaded
        if (signatureUris.isEmpty()) {
            Toast.makeText(this, "At least one signature is required", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Submit Survey")
            .setMessage("Are you sure you want to submit this survey?")
            .setPositiveButton("Submit") { _, _ ->
                submitForm()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitForm() {
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Create survey data object
        val currentUser = auth.currentUser
        if (currentUser == null) {
            loadingDialog.dismiss()
            Toast.makeText(this, "You must be logged in to submit a form", Toast.LENGTH_SHORT).show()
            return
        }

        val locationToCheck = etLocation.text.toString().trim()

        // Check if location already exists in Firestore
        firestore.collection("ba_survey_mini_olt")
            .whereEqualTo("location", locationToCheck) // Filter berdasarkan field 'location'
            .limit(1) // Kita hanya butuh tahu apakah ada setidaknya satu
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Document with the same location already exists
                    loadingDialog.dismiss()
                    etLocation.error = "Lokasi dengan ID ini sudah terdaftar"
                    Toast.makeText(this, "Error: Lokasi survei dengan ID '${locationToCheck}' sudah ada. Submit dibatalkan.", Toast.LENGTH_LONG).show()
                } else {
                    // Location is unique, proceed with submission
                    proceedWithSubmission(loadingDialog, currentUser, locationToCheck)
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Error saat pengecekan lokasi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Buat fungsi baru untuk melanjutkan proses submit agar tidak terlalu nested
    private fun proceedWithSubmission(loadingDialog: AlertDialog, currentUser: com.google.firebase.auth.FirebaseUser, location: String) {

        val surveyData = HashMap<String, Any>()

        // Basic info
        surveyData["location"] = etLocation.text.toString().trim()
        surveyData["noIhld"] = etNoIhld.text.toString().trim()
        surveyData["platform"] = platformDropdown.text.toString().trim()
        surveyData["siteProvider"] = etSiteProvider.text.toString().trim()
        surveyData["contractNumber"] = etContractNumber.text.toString().trim()
        surveyData["surveyDate"] = tvCurrentDate.text.toString()
        surveyData["createdBy"] = currentUser.email ?: "unknown"
        surveyData["createdAt"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Table results with PM/PBM selections
        val tableResults = HashMap<String, HashMap<String, String>>()

        // Rack
        val rackData = HashMap<String, String>()
        rackData["responsibility"] = rackDropdown.text.toString()
        rackData["surveyResult"] = etRackResult.text.toString().trim()
        rackData["proposed"] = etRackProposed.text.toString().trim()
        tableResults["rack"] = rackData

        // Rectifier
        val rectifierData = HashMap<String, String>()
        rectifierData["responsibility"] = rectifierDropdown.text.toString()
        rectifierData["surveyResult"] = etRectifierResult.text.toString().trim()
        rectifierData["proposed"] = etRectifierProposed.text.toString().trim()
        tableResults["rectifier"] = rectifierData

        // DC Power
        val dcPowerData = HashMap<String, String>()
        dcPowerData["responsibility"] = dcPowerDropdown.text.toString()
        dcPowerData["surveyResult"] = etDcPowerResult.text.toString().trim()
        dcPowerData["proposed"] = etDcPowerProposed.text.toString().trim()
        tableResults["dcPower"] = dcPowerData

        // Battery
        val batteryData = HashMap<String, String>()
        batteryData["responsibility"] = batteryDropdown.text.toString()
        batteryData["surveyResult"] = etBatteryResult.text.toString().trim()
        batteryData["proposed"] = etBatteryProposed.text.toString().trim()
        tableResults["battery"] = batteryData

        // MCB
        val mcbData = HashMap<String, String>()
        mcbData["responsibility"] = mcbDropdown.text.toString()
        mcbData["surveyResult"] = etMcbResult.text.toString().trim()
        mcbData["proposed"] = etMcbProposed.text.toString().trim()
        tableResults["mcb"] = mcbData

        // Grounding
        val groundingData = HashMap<String, String>()
        groundingData["responsibility"] = groundingDropdown.text.toString()
        groundingData["surveyResult"] = etGroundingResult.text.toString().trim()
        groundingData["proposed"] = etGroundingProposed.text.toString().trim()
        tableResults["grounding"] = groundingData

        // Indoor Room
        val indoorRoomData = HashMap<String, String>()
        indoorRoomData["responsibility"] = indoorRoomDropdown.text.toString()
        indoorRoomData["surveyResult"] = etIndoorRoomResult.text.toString().trim()
        indoorRoomData["proposed"] = etIndoorRoomProposed.text.toString().trim()
        tableResults["indoorRoom"] = indoorRoomData

        // AC Power
        val acPowerData = HashMap<String, String>()
        acPowerData["responsibility"] = acPowerDropdown.text.toString()
        acPowerData["surveyResult"] = etAcPowerResult.text.toString().trim()
        acPowerData["proposed"] = etAcPowerProposed.text.toString().trim()
        tableResults["acPower"] = acPowerData

        // Uplink
        val uplinkData = HashMap<String, String>()
        uplinkData["responsibility"] = uplinkDropdown.text.toString()
        uplinkData["surveyResult"] = etUplinkResult.text.toString().trim()
        uplinkData["proposed"] = etUplinkProposed.text.toString().trim()
        tableResults["uplink"] = uplinkData

        // Conduit
        val conduitData = HashMap<String, String>()
        conduitData["responsibility"] = conduitDropdown.text.toString()
        conduitData["surveyResult"] = etConduitResult.text.toString().trim()
        conduitData["proposed"] = etConduitProposed.text.toString().trim()
        tableResults["conduit"] = conduitData

        surveyData["tableResults"] = tableResults

        // Signatures including NIK data
        val signaturesData = HashMap<String, HashMap<String, String>>()

        // ZTE/Huawei
        val zteData = HashMap<String, String>()
        zteData["name"] = etZteName.text.toString().trim()
        zteData["nik"] = etZteNik.text.toString().trim()
        zteData["role"] = "TIM SURVEY"
        zteData["company"] = platformDropdown.text.toString()
        signaturesData["zte"] = zteData

        // TSEL NOP
        val tselNopData = HashMap<String, String>()
        tselNopData["name"] = etTselNopName.text.toString().trim()
        tselNopData["nik"] = etTselNopNik.text.toString().trim()
        tselNopData["role"] = "MGR NOP"
        tselNopData["company"] = "PT. TELKOMSEL"
        tselNopData["region"] = tvTselRegion.text.toString().trim()
        signaturesData["tselNop"] = tselNopData

        // TSEL RTPDS
        val tselRtpdsData = HashMap<String, String>()
        tselRtpdsData["name"] = etTselRtpdsName.text.toString().trim()
        tselRtpdsData["nik"] = etTselRtpdsNik.text.toString().trim()
        tselRtpdsData["role"] = "MGR RTPDS"
        tselRtpdsData["company"] = "PT. TELKOMSEL"
        tselRtpdsData["region"] = tvTselRegion.text.toString().trim()
        signaturesData["tselRtpds"] = tselRtpdsData

        // TSEL RTPE/NF
        val tselRtpeNfData = HashMap<String, String>()
        tselRtpeNfData["name"] = etTselRtpeNfName.text.toString().trim()
        tselRtpeNfData["nik"] = etTselRtpeNfNik.text.toString().trim()
        tselRtpeNfData["role"] = "MGR RTPE"
        tselRtpeNfData["company"] = "PT. TELKOMSEL"
        tselRtpeNfData["region"] = tvTselRegion.text.toString().trim()
        signaturesData["tselRtpeNf"] = tselRtpeNfData

        // TELKOM
        val telkomData = HashMap<String, String>()
        telkomData["name"] = etTelkomName.text.toString().trim()
        telkomData["nik"] = etTelkomNik.text.toString().trim()
        telkomData["role"] = "MGR NDPS TR1"
        telkomData["company"] = "PT. TELKOM"
        signaturesData["telkom"] = telkomData

        // TIF
        val tifData = HashMap<String, String>()
        tifData["name"] = etTifName.text.toString().trim()
        tifData["nik"] = etTifNik.text.toString().trim()
        tifData["role"] = "TIM SURVEY"
        tifData["company"] = "PT. TIF"
        signaturesData["tif"] = tifData

        surveyData["signatures"] = signaturesData
        surveyData["siteId"] = location  // Make sure this matches the Site ID in projects collection

        // Add to Firestore
        firestore.collection("ba_survey_mini_olt")
            .add(surveyData)
            .addOnSuccessListener { documentReference ->
                val surveyId = documentReference.id

                // Upload images
                uploadImagesToFirebase(surveyId) { success ->
                    if (success) {
                        generateAndUploadPdf(surveyId) { pdfSuccess, pdfUrl ->
                            if (pdfSuccess && pdfUrl != null) {
                                // NEW CODE: Update the projects collection with the BA document reference
                                updateProjectWithBaDocument(location, pdfUrl)
                                loadingDialog.dismiss()
                                showSuccessDialog()
                            } else {
                                loadingDialog.dismiss()
                                Toast.makeText(this, "Survey saved but failed to generate PDF", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        loadingDialog.dismiss()
                        Toast.makeText(this, "Failed to upload images", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Error submitting form: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // New method to update project document with BA reference
    private fun updateProjectWithBaDocument(siteId: String, pdfUrl: String) {
        // Query the projects collection to find the document with matching siteId
        firestore.collection("projects")
            .whereEqualTo("siteId", siteId)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Get the first matching document
                    val projectDoc = documents.documents[0]

                    // Update the document with the BA document URL
                    firestore.collection("projects").document(projectDoc.id)
                        .update("documentBA", pdfUrl)
                        .addOnSuccessListener {
                            Log.d("BaSurvey", "Successfully linked BA document to project $siteId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("BaSurvey", "Error linking BA document: ${e.message}")
                        }
                } else {
                    Log.w("BaSurvey", "No matching project found for siteId: $siteId")
                }
            }
            .addOnFailureListener { e ->
                Log.e("BaSurvey", "Error finding project: ${e.message}")
            }
    }

    // MODIFIED: Improved uploadImagesToFirebase method to handle non-sequential photo uploads
    private fun uploadImagesToFirebase(surveyId: String, callback: (Boolean) -> Unit) {
        // Counter for uploaded images
        val totalUploads = photoUris.size + signatureUris.size
        var completedUploads = 0
        var failedUploads = 0

        // Handle no photos/signatures case
        if (totalUploads == 0) {
            callback(true)
            return
        }

        // Create a map to store photo indices information
        val photoIndexMap = HashMap<Int, Int>()

        // Get sorted keys for consistent processing
        val sortedPhotoKeys = photoUris.keys.sorted()

        // Upload photos in order of their keys
        for ((uploadIndex, originalIndex) in sortedPhotoKeys.withIndex()) {
            // Map original index to a sequential upload index
            photoIndexMap[originalIndex] = uploadIndex

            val uri = photoUris[originalIndex] ?: continue

            // Use the upload index for the storage path (creates sequential files)
            val photoRef = storage.reference.child("ba_survey_mini_olt/$surveyId/photos/photo${uploadIndex+1}.jpg")

            uploadImageFromUri(uri, photoRef) { success, downloadUrl ->
                if (success && downloadUrl != null) {
                    // Store the original index in Firestore so we know which photo container it came from
                    firestore.collection("ba_survey_mini_olt").document(surveyId)
                        .update("photos.photo${originalIndex+1}", downloadUrl)
                        .addOnSuccessListener {
                            completedUploads++
                            if (completedUploads + failedUploads == totalUploads) {
                                callback(failedUploads == 0)
                            }
                        }
                        .addOnFailureListener {
                            failedUploads++
                            if (completedUploads + failedUploads == totalUploads) {
                                callback(failedUploads == 0)
                            }
                        }
                } else {
                    failedUploads++
                    if (completedUploads + failedUploads == totalUploads) {
                        callback(failedUploads == 0)
                    }
                }
            }
        }

        // Store the mapping of original indices to upload indices
        if (photoIndexMap.isNotEmpty()) {
            val photoIndexData = HashMap<String, Any>()
            photoIndexMap.forEach { (originalIndex, uploadIndex) ->
                photoIndexData["idx_$originalIndex"] = uploadIndex
            }

            firestore.collection("ba_survey_mini_olt").document(surveyId)
                .update("photoIndexMapping", photoIndexData)
        }

        // Upload signatures - all from gallery now
        for ((index, uri) in signatureUris) {
            val signatureRef = storage.reference.child("ba_survey_mini_olt/$surveyId/signatures/${signatureKeys[index]}.png")
            uploadImageFromUri(uri, signatureRef) { success, downloadUrl ->
                if (success && downloadUrl != null) {
                    firestore.collection("ba_survey_mini_olt").document(surveyId)
                        .update("signatures.${signatureKeys[index]}.signatureUrl", downloadUrl)
                        .addOnSuccessListener {
                            completedUploads++
                            if (completedUploads + failedUploads == totalUploads) {
                                callback(failedUploads == 0)
                            }
                        }
                        .addOnFailureListener {
                            failedUploads++
                            if (completedUploads + failedUploads == totalUploads) {
                                callback(failedUploads == 0)
                            }
                        }
                } else {
                    failedUploads++
                    if (completedUploads + failedUploads == totalUploads) {
                        callback(failedUploads == 0)
                    }
                }
            }
        }
    }

    // A robust helper method that handles all types of URI issues
    private fun uploadImageFromUri(uri: Uri, storageRef: StorageReference, callback: (Boolean, String?) -> Unit) {
        try {
            // First try to get input stream from URI
            contentResolver.openInputStream(uri)?.use { inputStream ->
                // Read all bytes from input stream
                val bytes = inputStream.readBytes()

                // Upload bytes directly to Firebase
                val uploadTask = storageRef.putBytes(bytes)
                uploadTask
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            throw task.exception ?: Exception("Unknown upload error")
                        }
                        storageRef.downloadUrl
                    }
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            callback(true, task.result.toString())
                        } else {
                            Log.e("Upload", "Failed to get download URL: ${task.exception?.message}")
                            callback(false, null)
                        }
                    }
            } ?: run {
                // If opening input stream failed, try bitmap approach
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val imageData = baos.toByteArray()

                // Upload the byte array instead of the file
                val uploadTask = storageRef.putBytes(imageData)
                uploadTask
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            throw task.exception ?: Exception("Unknown upload error")
                        }
                        storageRef.downloadUrl
                    }
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            callback(true, task.result.toString())
                        } else {
                            Log.e("Upload", "Failed to get download URL: ${task.exception?.message}")
                            callback(false, null)
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("Upload", "Error processing URI: ${e.message}", e)
            callback(false, null)
        }
    }

    private fun generateAndUploadPdf(surveyId: String, callback: (Boolean, String?) -> Unit) {
        try {
            // Create PDF file
            val pdfFile = generatePdfFile()

            // Upload PDF to Firebase Storage
            val pdfRef = storage.reference.child("ba_survey_mini_olt_pdf/$surveyId.pdf")

            pdfRef.putFile(Uri.fromFile(pdfFile))
                .addOnSuccessListener {
                    pdfRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Update document with download URL
                        firestore.collection("ba_survey_mini_olt").document(surveyId)
                            .update("pdfUrl", downloadUrl.toString())
                            .addOnSuccessListener {
                                callback(true, downloadUrl.toString())
                            }
                            .addOnFailureListener {
                                callback(false, null)
                            }
                    }
                }
                .addOnFailureListener {
                    callback(false, null)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            callback(false, null)
        }
    }

    private fun generatePdf() {
        try {
            val pdfFile = generatePdfFile()

            // Open the PDF
            val uri = FileProvider.getUriForFile(
                this,
                "com.mbkm.telgo.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            startActivity(Intent.createChooser(intent, "Open PDF with..."))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generatePdfFile(): File {
        // Create PDF document
        val document = PdfDocument()

        // Page 1: Form content with complete header
        val pageInfo1 = PdfDocument.PageInfo.Builder(612, 842, 1).create()
        val page1 = document.startPage(pageInfo1)
        val canvas1 = page1.canvas

        // Draw header table on first page only, termasuk logo dan judul
        drawHeaderTable(canvas1, etHeaderNo.text.toString())

        // Draw main content with appropriate starting position
        drawFirstPageContent(canvas1)

        // Draw footer
        drawPageFooter(canvas1, 1)
        document.finishPage(page1)

        // Page 2: Signatures (without header table, only title)
        val pageInfo2 = PdfDocument.PageInfo.Builder(612, 842, 2).create()
        val page2 = document.startPage(pageInfo2)
        val canvas2 = page2.canvas

        // Draw only simple title, not full header
        drawSimplePageTitle(canvas2, "TANDA TANGAN PERSETUJUAN")

        // Draw signatures content
        drawSignaturesPageContent(canvas2)

        // Draw footer
        drawPageFooter(canvas2, 2)
        document.finishPage(page2)

        // Photo pages: 4 photos per page
        // MODIFIED: Use the improved photo handling approach
        if (photoUris.isNotEmpty()) {
            val sortedPhotoKeys = photoUris.keys.sorted()
            val photosPerPage = 4 // 4 foto per halaman
            val photoPages = (sortedPhotoKeys.size + photosPerPage - 1) / photosPerPage

            for (i in 0 until photoPages) {
                val pageNum = i + 3
                val pageInfo = PdfDocument.PageInfo.Builder(612, 842, pageNum).create()
                val page = document.startPage(pageInfo)

                // Use the improved photo drawing method
                drawPhotosPageContent(page.canvas, i)

                // Draw footer
                drawPageFooter(page.canvas, pageNum)
                document.finishPage(page)
            }
        }

        // Tulis PDF ke file
        val fileName = "BA_Survey_Mini_OLT_${etLocation.text}_${System.currentTimeMillis()}.pdf"
        val filePath = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        FileOutputStream(filePath).use { fos ->
            document.writeTo(fos)
        }

        document.close()
        return filePath
    }

    // Fungsi baru untuk judul sederhana tanpa tabel header kompleks
    private fun drawSimplePageTitle(canvas: Canvas, title: String) {
        val paint = Paint()
        paint.color = Color.BLACK

        // Set dimensions
        val pageWidth = 612f
        var yPosition = 50f

        // Perbaikan: Ukuran font dikurangi sedikit dan menggunakan typeface normal
        paint.textSize = 14f // Sebelumnya 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) // Pastikan menggunakan font normal
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true // Masih bold tapi tidak terlalu ekstrim
        canvas.drawText(title, pageWidth / 2, yPosition, paint)
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.LEFT
    }

    private fun calculateRequiredPages(): Int {
        // Calculate required pages for photos
        val photoCount = photoUris.size
        val photosPerPage = 4  // 4 photos per page
        val photoPages = if (photoCount > 0) (photoCount + photosPerPage - 1) / photosPerPage else 0

        // 1 page for form, 1 page for signatures, plus photo pages
        return 2 + photoPages
    }

    private fun drawHeaderTable(canvas: Canvas, noIhld: String) {
        // Set up paint
        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = 10f

        // Dimensi untuk header
        val pageWidth = 612f
        val leftMargin = 40f
        val rightMargin = pageWidth - 40f
        var yPosition = 40f

        // Get logo based on platform choice
        val logo = when (platformDropdown.text.toString()) {
            "PT. ZTE INDONESIA" -> BitmapFactory.decodeResource(resources, R.drawable.logo_zte)
            "PT. Huawei Tech Investment" -> BitmapFactory.decodeResource(resources, R.drawable.logo_huawei)
            else -> BitmapFactory.decodeResource(resources, R.drawable.logo_zte)
        }

        // Draw logo
        if (logo != null) {
            val logoWidth = 80f
            val logoHeight = 40f
            val scaledLogo = Bitmap.createScaledBitmap(logo, logoWidth.toInt(), logoHeight.toInt(), true)
            canvas.drawBitmap(scaledLogo, leftMargin, yPosition, paint)
        }

        // Draw title
        paint.textSize = 16f
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        canvas.drawText("BERITA ACARA SURVEY MINI OLT", pageWidth / 2, yPosition + 30f, paint)
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 10f

        // Draw header table
        val tableTop = yPosition + 45f
        val rowHeight = 25f
        paint.strokeWidth = 1f

        // First row
        paint.style = Paint.Style.STROKE // Just draw borders, not filled rectangles

        // "Prepared" cell
        canvas.drawRect(leftMargin, tableTop, (leftMargin + rightMargin) / 2, tableTop + rowHeight, paint)
        paint.style = Paint.Style.FILL // Switch back to fill style for text
        canvas.drawText("Prepared (also subject responsible if other)", leftMargin + 5f, tableTop + 17f, paint)

        // "No." cell
        paint.style = Paint.Style.STROKE
        canvas.drawRect((leftMargin + rightMargin) / 2, tableTop, rightMargin, tableTop + rowHeight, paint)
        paint.style = Paint.Style.FILL
        canvas.drawText("No.", (leftMargin + rightMargin) / 2 + 5f, tableTop + 17f, paint)
        canvas.drawText(etHeaderNo.text.toString(), (leftMargin + rightMargin) / 2 + 30f, tableTop + 17f, paint)

        // Second row
        paint.style = Paint.Style.STROKE

        // "Approved" cell
        canvas.drawRect(leftMargin, tableTop + rowHeight, (leftMargin + rightMargin) / 3, tableTop + rowHeight * 2, paint)
        paint.style = Paint.Style.FILL
        canvas.drawText("Approved", leftMargin + 5f, tableTop + rowHeight + 17f, paint)

        // "Checked" cell
        paint.style = Paint.Style.STROKE
        canvas.drawRect((leftMargin + rightMargin) / 3, tableTop + rowHeight, (leftMargin + rightMargin) * 2/3, tableTop + rowHeight * 2, paint)
        paint.style = Paint.Style.FILL
        canvas.drawText("Checked", (leftMargin + rightMargin) / 3 + 5f, tableTop + rowHeight + 17f, paint)

        // "Date" cell
        paint.style = Paint.Style.STROKE
        canvas.drawRect((leftMargin + rightMargin) * 2/3, tableTop + rowHeight, (leftMargin + rightMargin) * 5/6, tableTop + rowHeight * 2, paint)
        paint.style = Paint.Style.FILL
        canvas.drawText("Date", (leftMargin + rightMargin) * 2/3 + 5f, tableTop + rowHeight + 17f, paint)

        // "Ref" cell
        paint.style = Paint.Style.STROKE
        canvas.drawRect((leftMargin + rightMargin) * 5/6, tableTop + rowHeight, rightMargin, tableTop + rowHeight * 2, paint)
        paint.style = Paint.Style.FILL
        canvas.drawText("Reference", (leftMargin + rightMargin) * 5/6 + 5f, tableTop + rowHeight + 17f, paint)
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int) {
        val pageWidth = 612f
        val footerY = 800f // Position near bottom
        val leftMargin = 40f

        // Set footer text properties
        val paint = Paint()
        paint.textSize = 8f
        paint.color = Color.GRAY
        paint.strokeWidth = 0.5f

        // Draw footer line
        canvas.drawLine(leftMargin, footerY - 10f, pageWidth - leftMargin, footerY - 10f, paint)

        // Draw footer text
        canvas.drawText("Dokumen ini telah ditandatangani secara elektronik dan merupakan dokumen sah sesuai ketentuan yang berlaku",
            leftMargin, footerY, paint)

        // Draw page number on right side
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Halaman $pageNumber", pageWidth - leftMargin, footerY, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawFirstPageContent(canvas: Canvas) {
        val paint = Paint()
        paint.color = Color.BLACK

        // Set initial positions and constants
        val pageWidth = 612f
        val leftMargin = 40f

        // PENTING: Mulai yPosition lebih jauh setelah header table
        var yPosition = 180f // Sebelumnya 120f - tambahkan jarak yang cukup

        // Draw basic information with improved formatting
        paint.textSize = 11f

        // Column width parameters
        val labelWidth = 100f
        val colonX = leftMargin + labelWidth

        // Lokasi
        canvas.drawText("Lokasi", leftMargin, yPosition, paint)
        canvas.drawText(":", colonX, yPosition, paint)
        canvas.drawText(etLocation.text.toString(), colonX + 10f, yPosition, paint)
        yPosition += 20f

        // NO IHLD/LOP
        canvas.drawText("NO IHLD / LOP", leftMargin, yPosition, paint)
        canvas.drawText(":", colonX, yPosition, paint)
        canvas.drawText(etNoIhld.text.toString(), colonX + 10f, yPosition, paint)
        yPosition += 20f

        // Platform
        canvas.drawText("Platform", leftMargin, yPosition, paint)
        canvas.drawText(":", colonX, yPosition, paint)
        canvas.drawText(platformDropdown.text.toString(), colonX + 10f, yPosition, paint)
        yPosition += 20f

        // Site Provider
        canvas.drawText("Site Provider", leftMargin, yPosition, paint)
        canvas.drawText(":", colonX, yPosition, paint)
        canvas.drawText(etSiteProvider.text.toString(), colonX + 10f, yPosition, paint)
        yPosition += 20f

        // Nomor Kontrak
        canvas.drawText("Nomor Kontrak", leftMargin, yPosition, paint)
        canvas.drawText(":", colonX, yPosition, paint)
        canvas.drawText(etContractNumber.text.toString(), colonX + 10f, yPosition, paint)
        yPosition += 30f

        // Introduction text in Indonesian
        val introText = "Pada hari ini ${tvCurrentDate.text} telah dilaksanakan survey sarana penunjang (SARPEN) dengan hasil sebagai berikut:"

        // Draw text with proper wrapping for longer lines
        val maxWidth = pageWidth - (2 * leftMargin)
        val lines = wrapText(introText, paint, maxWidth)
        for (line in lines) {
            canvas.drawText(line, leftMargin, yPosition, paint)
            yPosition += 20f
        }
        yPosition += 10f

        // Draw table header
        drawTableHeader(canvas, paint, leftMargin, yPosition)
        yPosition += 25f

        // Draw table rows with PM/PBM selections
        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "1. Rack tempat Perangkat MINI OLT & OTB", rackDropdown.text.toString(),
            etRackResult.text.toString(), etRackProposed.text.toString())

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "2. Rectifier", rectifierDropdown.text.toString(),
            etRectifierResult.text.toString(), etRectifierProposed.text.toString())

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "3. Ketersediaan Daya DC", dcPowerDropdown.text.toString(),
            etDcPowerResult.text.toString(), etDcPowerProposed.text.toString())

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "4. Baterai", batteryDropdown.text.toString(),
            etBatteryResult.text.toString(), etBatteryProposed.text.toString())

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "5. MCB untuk Pemasangan Mini OLT", mcbDropdown.text.toString(),
            etMcbResult.text.toString(), etMcbProposed.text.toString())

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "6. Grounding", groundingDropdown.text.toString(),
            etGroundingResult.text.toString(), etGroundingProposed.text.toString())

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "7. Indoor Room", indoorRoomDropdown.text.toString(),
            etIndoorRoomResult.text.toString(), etIndoorRoomProposed.text.toString())

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "8. Ketersediaan Daya AC", acPowerDropdown.text.toString(),
            etAcPowerResult.text.toString(), etAcPowerProposed.text.toString())

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "9. BA Kesiapan Uplink", uplinkDropdown.text.toString(),
            etUplinkResult.text.toString(), etUplinkProposed.text.toString())

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "10. Conduit", conduitDropdown.text.toString(),
            etConduitResult.text.toString(), etConduitProposed.text.toString())

        // Add PM/PBM legend
        yPosition += 15f
        paint.textSize = 10f
        canvas.drawText("*PM : Permintaan Memungkinkan", leftMargin, yPosition, paint)
        yPosition += 12f
        canvas.drawText("**PBM : Permintaan Belum Memungkinkan", leftMargin, yPosition, paint)
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val result = ArrayList<String>()

        // Jika teks kosong, kembalikan list kosong
        if (text.isBlank()) {
            return result
        }

        // Pecah teks berdasarkan spasi
        val words = text.split(" ")

        // Jika hanya satu kata, cek apakah perlu dipecah karakter per karakter
        if (words.size == 1) {
            val word = words[0]
            if (paint.measureText(word) <= maxWidth) {
                // Jika muat dalam satu baris, langsung tambahkan
                result.add(word)
            } else {
                // Jika tidak muat, pecah karakter demi karakter
                var line = ""
                for (char in word) {
                    val testLine = line + char
                    if (paint.measureText(testLine) <= maxWidth) {
                        line = testLine
                    } else {
                        // Tambahkan baris sekarang ke hasil jika sudah penuh
                        if (line.isNotEmpty()) {
                            result.add(line)
                        }
                        line = char.toString()
                    }
                }
                // Tambahkan sisa baris terakhir
                if (line.isNotEmpty()) {
                    result.add(line)
                }
            }
            return result
        }

        // Proses multi-word text
        var currentLine = StringBuilder()

        for (word in words) {
            // Coba tambahkan kata ke baris sekarang
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            val testWidth = paint.measureText(testLine)

            if (testWidth <= maxWidth) {
                // Kata muat dalam baris saat ini
                currentLine = StringBuilder(testLine)
            } else {
                // Baris sudah penuh, tambahkan ke hasil dan mulai baris baru

                // Jika currentLine tidak kosong, tambahkan ke hasil
                if (currentLine.isNotEmpty()) {
                    result.add(currentLine.toString())
                    currentLine = StringBuilder()
                }

                // Periksa apakah kata tunggal ini lebih panjang dari maxWidth
                if (paint.measureText(word) > maxWidth) {
                    // Kata terlalu panjang, potong karakter per karakter
                    var line = ""
                    for (char in word) {
                        val charTestLine = line + char
                        if (paint.measureText(charTestLine) <= maxWidth) {
                            line = charTestLine
                        } else {
                            // Tambahkan baris sekarang ke hasil
                            result.add(line)
                            line = char.toString()
                        }
                    }
                    // Tambahkan sisa baris terakhir jika ada
                    if (line.isNotEmpty()) {
                        currentLine = StringBuilder(line)
                    }
                } else {
                    // Kata muat dalam baris baru
                    currentLine = StringBuilder(word)
                }
            }
        }

        // Tambahkan baris terakhir jika ada
        if (currentLine.isNotEmpty()) {
            result.add(currentLine.toString())
        }

        return result
    }

    // Fungsi baru khusus untuk menggambar hanya tabel header saja
    private fun drawHeaderTableOnly(canvas: Canvas, noValue: String) {
        // Set up paint
        val paint = Paint()
        paint.color = Color.BLACK
        paint.textSize = 10f

        // Dimensi untuk header
        val pageWidth = 612f
        val leftMargin = 40f
        val rightMargin = pageWidth - 40f
        val tableTop = 40f // Mulai dari posisi yang cukup tinggi
        val rowHeight = 25f

        paint.strokeWidth = 1f

        // First row
        paint.style = Paint.Style.STROKE // Just draw borders, not filled rectangles

        // "Prepared" cell
        canvas.drawRect(leftMargin, tableTop, (leftMargin + rightMargin) / 2, tableTop + rowHeight, paint)
        paint.style = Paint.Style.FILL // Switch back to fill style for text
        canvas.drawText("Prepared (also subject responsible if other)", leftMargin + 5f, tableTop + 17f, paint)

        // "No." cell
        paint.style = Paint.Style.STROKE
        canvas.drawRect((leftMargin + rightMargin) / 2, tableTop, rightMargin, tableTop + rowHeight, paint)
        paint.style = Paint.Style.FILL
        canvas.drawText("No.", (leftMargin + rightMargin) / 2 + 5f, tableTop + 17f, paint)
        canvas.drawText(noValue, (leftMargin + rightMargin) / 2 + 30f, tableTop + 17f, paint)

        // Second row
        paint.style = Paint.Style.STROKE

        // "Approved" cell
        canvas.drawRect(leftMargin, tableTop + rowHeight, (leftMargin + rightMargin) / 3, tableTop + rowHeight * 2, paint)
        paint.style = Paint.Style.FILL
        canvas.drawText("Approved", leftMargin + 5f, tableTop + rowHeight + 17f, paint)

        // "Checked" cell
        paint.style = Paint.Style.STROKE
        canvas.drawRect((leftMargin + rightMargin) / 3, tableTop + rowHeight, (leftMargin + rightMargin) * 2/3, tableTop + rowHeight * 2, paint)
        paint.style = Paint.Style.FILL
        canvas.drawText("Checked", (leftMargin + rightMargin) / 3 + 5f, tableTop + rowHeight + 17f, paint)

        // "Date" cell
        paint.style = Paint.Style.STROKE
        canvas.drawRect((leftMargin + rightMargin) * 2/3, tableTop + rowHeight, (leftMargin + rightMargin) * 5/6, tableTop + rowHeight * 2, paint)
        paint.style = Paint.Style.FILL
        canvas.drawText("Date", (leftMargin + rightMargin) * 2/3 + 5f, tableTop + rowHeight + 17f, paint)

        // "Ref" cell
        paint.style = Paint.Style.STROKE
        canvas.drawRect((leftMargin + rightMargin) * 5/6, tableTop + rowHeight, rightMargin, tableTop + rowHeight * 2, paint)
        paint.style = Paint.Style.FILL
        canvas.drawText("Reference", (leftMargin + rightMargin) * 5/6 + 5f, tableTop + rowHeight + 17f, paint)
    }

    private fun drawTableHeader(canvas: Canvas, paint: Paint, x: Float, y: Float) {
        val pageWidth = 612f
        val leftMargin = x
        val rightMargin = pageWidth - x
        val headerHeight = 30f

        // Draw header background
        paint.color = Color.parseColor("#1e88e5")
        paint.style = Paint.Style.FILL
        canvas.drawRect(leftMargin, y, rightMargin, y + headerHeight, paint)

        // Calculate column widths - SAMA dengan di drawTableRow untuk konsistensi
        val availableWidth = rightMargin - leftMargin
        val col1Width = availableWidth * 0.35f
        val col2Width = availableWidth * 0.15f
        val col3Width = availableWidth * 0.25f
        val col4Width = availableWidth * 0.25f

        // Draw header text
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.isFakeBoldText = true

        // Center text vertically
        val textY = y + headerHeight / 2 + paint.textSize / 3

        // Draw header texts with horizontal padding
        canvas.drawText("SPESIFIKASI DAN KEBUTUHAN", leftMargin + 5f, textY, paint)

        // Center PM/PBM text
        val pmPbmX = leftMargin + col1Width + (col2Width / 2) - (paint.measureText("PM/PBM") / 2)
        canvas.drawText("PM/PBM", pmPbmX, textY, paint)

        canvas.drawText("HASIL SURVEY", leftMargin + col1Width + col2Width + 5f, textY, paint)
        canvas.drawText("KESEPAKATAN/PROPOSED", leftMargin + col1Width + col2Width + col3Width + 5f, textY, paint)

        paint.isFakeBoldText = false

        // Draw header borders
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f

        // Horizontal lines
        canvas.drawLine(leftMargin, y, rightMargin, y, paint)
        canvas.drawLine(leftMargin, y + headerHeight, rightMargin, y + headerHeight, paint)

        // Vertical lines
        canvas.drawLine(leftMargin, y, leftMargin, y + headerHeight, paint)
        canvas.drawLine(leftMargin + col1Width, y, leftMargin + col1Width, y + headerHeight, paint)
        canvas.drawLine(leftMargin + col1Width + col2Width, y, leftMargin + col1Width + col2Width, y + headerHeight, paint)
        canvas.drawLine(leftMargin + col1Width + col2Width + col3Width, y, leftMargin + col1Width + col2Width + col3Width, y + headerHeight, paint)
        canvas.drawLine(rightMargin, y, rightMargin, y + headerHeight, paint)
    }

    private fun drawTableRow(canvas: Canvas, paint: Paint, x: Float, y: Float,
                             spec: String, pmPbm: String, result: String, proposed: String): Float {
        val pageWidth = 612f
        val leftMargin = x
        val rightMargin = pageWidth - x

        // Calculate column widths
        val availableWidth = rightMargin - leftMargin
        val col1Width = availableWidth * 0.35f
        val col2Width = availableWidth * 0.15f
        val col3Width = availableWidth * 0.25f
        val col4Width = availableWidth * 0.25f

        paint.textSize = 9f

        // PERBAIKAN: Kurangi padding horizontal untuk memberikan ruang lebih banyak untuk teks
        val horizontalPadding = 5f

        // Split text for each column to handle wrapping - dengan padding yang lebih kecil
        val specLines = wrapText(spec, paint, col1Width - (2 * horizontalPadding))
        val resultLines = wrapText(result, paint, col3Width - (2 * horizontalPadding))
        val proposedLines = wrapText(proposed, paint, col4Width - (2 * horizontalPadding))

        // Calculate DYNAMIC row height based on content
        val lineHeight = 12f
        val verticalPadding = 5f // Padding untuk atas dan bawah sel
        val maxLines = maxOf(specLines.size, resultLines.size, proposedLines.size, 1)

        // PERBAIKAN: Tinggi minimum sel ditingkatkan dan pastikan cukup ruang untuk semua teks
        val minRowHeight = 30f
        val contentHeight = (maxLines * lineHeight) + (2 * verticalPadding)
        val rowHeight = maxOf(contentHeight, minRowHeight)

        // Gambar background sel
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRect(leftMargin, y, rightMargin, y + rowHeight, paint)

        // Draw borders
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f

        // Vertical lines
        canvas.drawLine(leftMargin, y, leftMargin, y + rowHeight, paint)
        canvas.drawLine(leftMargin + col1Width, y, leftMargin + col1Width, y + rowHeight, paint)
        canvas.drawLine(leftMargin + col1Width + col2Width, y, leftMargin + col1Width + col2Width, y + rowHeight, paint)
        canvas.drawLine(leftMargin + col1Width + col2Width + col3Width, y, leftMargin + col1Width + col2Width + col3Width, y + rowHeight, paint)
        canvas.drawLine(rightMargin, y, rightMargin, y + rowHeight, paint)

        // Horizontal lines
        canvas.drawLine(leftMargin, y, rightMargin, y, paint)
        canvas.drawLine(leftMargin, y + rowHeight, rightMargin, y + rowHeight, paint)

        // Draw cell content
        paint.style = Paint.Style.FILL
        paint.color = Color.BLACK

        // PERBAIKAN: Posisi y awal untuk teks, beri ruang dari bagian atas sel
        val initialTextY = y + verticalPadding + lineHeight

        // Draw specification text
        for (i in specLines.indices) {
            canvas.drawText(specLines[i], leftMargin + horizontalPadding, initialTextY + (i * lineHeight), paint)
        }

        // Draw PM/PBM text (centered vertical and horizontal)
        val pmPbmX = leftMargin + col1Width + (col2Width / 2) - (paint.measureText(pmPbm) / 2)
        val pmPbmY = y + (rowHeight / 2) + (paint.textSize / 3) // Approximately centered vertically
        canvas.drawText(pmPbm, pmPbmX, pmPbmY, paint)

        // Draw result text
        for (i in resultLines.indices) {
            canvas.drawText(resultLines[i], leftMargin + col1Width + col2Width + horizontalPadding, initialTextY + (i * lineHeight), paint)
        }

        // Draw proposed text
        for (i in proposedLines.indices) {
            canvas.drawText(proposedLines[i], leftMargin + col1Width + col2Width + col3Width + horizontalPadding, initialTextY + (i * lineHeight), paint)
        }

        // Return next y position
        return y + rowHeight
    }

    private fun drawSignaturesPageContent(canvas: Canvas) {
        val paint = Paint()
        paint.color = Color.BLACK

        // Set initial positions and constants
        val pageWidth = 612f
        val leftMargin = 40f
        val rightMargin = pageWidth - 40f
        var yPosition = 80f  // Start lower below the title

        // Calculate dimensions for a 3×2 grid layout with improved spacing
        val availableWidth = rightMargin - leftMargin
        val boxWidth = availableWidth / 3f - 10f
        val boxHeight = 150f
        val horizontalGap = 15f
        val verticalGap = 20f

        // Perbaikan: Tambahkan deskripsi singkat tentang halaman
        paint.textSize = 11f
        canvas.drawText("Berikut adalah tanda tangan persetujuan pihak pihak yang terlibat dan mengetahui pelaksanaan survey ini:",
            leftMargin, yPosition, paint)
        yPosition += 30f

        // First row: Platform company, Telkom, TIF
        drawSignatureBox(canvas, paint,
            leftMargin, yPosition,
            platformDropdown.text.toString(), "TIM SURVEY",
            etZteName.text.toString(), boxWidth, boxHeight,
            imgZteSignature,
            "NIK. " + etZteNik.text.toString())

        drawSignatureBox(canvas, paint,
            leftMargin + boxWidth + horizontalGap, yPosition,
            "PT. TELKOM", "MGR NDPS TR1",
            etTelkomName.text.toString(), boxWidth, boxHeight,
            imgTelkomSignature,
            "NIK. " + etTelkomNik.text.toString())

        drawSignatureBox(canvas, paint,
            leftMargin + (boxWidth + horizontalGap) * 2, yPosition,
            "PT. TIF", "TIM SURVEY",
            etTifName.text.toString(), boxWidth, boxHeight,
            imgTifSignature,
            "NIK. " + etTifNik.text.toString())

        // Second row: Telkomsel positions
        yPosition += boxHeight + verticalGap

        drawSignatureBox(canvas, paint,
            leftMargin, yPosition,
            "PT. TELKOMSEL", "MGR NOP\n" + tvTselRegion.text,
            etTselNopName.text.toString(), boxWidth, boxHeight,
            imgTselNopSignature,
            "NIK. " + etTselNopNik.text.toString())

        drawSignatureBox(canvas, paint,
            leftMargin + boxWidth + horizontalGap, yPosition,
            "PT. TELKOMSEL", "MGR RTPDS\n" + tvTselRegion.text,
            etTselRtpdsName.text.toString(), boxWidth, boxHeight,
            imgTselRtpdsSignature,
            "NIK. " + etTselRtpdsNik.text.toString())

        drawSignatureBox(canvas, paint,
            leftMargin + (boxWidth + horizontalGap) * 2, yPosition,
            "PT. TELKOMSEL", "MGR RTPE\n" + tvTselRegion.text,
            etTselRtpeNfName.text.toString(), boxWidth, boxHeight,
            imgTselRtpeNfSignature,
            "NIK. " + etTselRtpeNfNik.text.toString())
    }

    private fun drawSignatureBox(canvas: Canvas, paint: Paint, x: Float, y: Float,
                                 company: String, role: String, name: String, width: Float, height: Float,
                                 signatureImageView: ImageView, nipText: String) {
        // Draw box with border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(x, y, x + width, y + height, paint)

        // Set text style
        paint.style = Paint.Style.FILL
        paint.textSize = 11f

        // Draw company name
        paint.isFakeBoldText = true
        canvas.drawText(company, x + 5f, y + 15f, paint)
        paint.isFakeBoldText = false

        // Draw role (with multi-line support)
        val roleLines = role.split("\n")
        for (i in roleLines.indices) {
            canvas.drawText(roleLines[i], x + 5f, y + 30f + (i * 15f), paint)
        }

        // IMPROVED SIGNATURE RENDERING - Higher quality
        try {
            if (signatureImageView.visibility == View.VISIBLE && signatureImageView.drawable != null) {
                val drawable = signatureImageView.drawable
                if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
                    // Get the original bitmap in full quality
                    val originalBitmap = drawable.bitmap

                    // Prepare high-quality rendering
                    val renderPaint = Paint().apply {
                        isAntiAlias = true  // Anti-aliasing for smoother edges
                        isFilterBitmap = true  // Enable bitmap filtering for better scaling
                        isDither = true  // Improve color rendering when downsampling
                    }

                    // Calculate dimensions maintaining aspect ratio
                    val signatureWidth = width - 20f
                    val signatureHeight = 60f // Ketinggian area yang dialokasikan untuk tanda tangan

                    // Determine scaling to fit while maintaining aspect ratio
                    val originalRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                    // Target area untuk tanda tangan (bukan keseluruhan signatureWidth/signatureHeight box)
                    // Kita ingin tanda tangan muat di dalam area yang ditentukan, misal 80% lebar dan 80% tinggi area tanda tangan
                    val targetPhotoWidth = signatureWidth * 0.9f // Gunakan 90% dari lebar yang tersedia untuk gambar
                    val targetPhotoHeight = signatureHeight * 0.9f // Gunakan 90% dari tinggi yang tersedia untuk gambar


                    val scaledWidth: Float
                    val scaledHeight: Float

                    if (originalRatio > (targetPhotoWidth / targetPhotoHeight)) {
                        // Width constrained
                        scaledWidth = targetPhotoWidth
                        scaledHeight = targetPhotoWidth / originalRatio
                    } else {
                        // Height constrained
                        scaledHeight = targetPhotoHeight
                        scaledWidth = targetPhotoHeight * originalRatio
                    }

                    // Position signature centered in available space
                    // y + 45f adalah perkiraan posisi atas area tanda tangan
                    val xOffset = x + 10f + (signatureWidth - scaledWidth) / 2
                    val yOffset = y + 45f + (signatureHeight - scaledHeight) / 2 // Disesuaikan agar terpusat dalam area signatureHeight

                    // Define the target rectangle for drawing
                    val destRect = RectF(xOffset, yOffset, xOffset + scaledWidth, yOffset + scaledHeight)

                    // Draw with high quality rendering
                    canvas.drawBitmap(originalBitmap, null, destRect, renderPaint)
                } else {
                    // Jika drawable bukan BitmapDrawable atau bitmap null/recycled, JANGAN GAMBAR GARIS
                    // Tidak ada tindakan yang perlu dilakukan di sini jika ingin kosong
                }
            } else {
                // Jika ImageView tidak visible atau tidak ada drawable, JANGAN GAMBAR GARIS
                // Tidak ada tindakan yang perlu dilakukan di sini jika ingin kosong
            }
        } catch (e: Exception) {
            // Jika terjadi error saat menggambar signature, JANGAN GAMBAR GARIS
            Log.e("PDF", "Error drawing signature: ${e.message}")
        }

        // Draw name
        paint.textSize = 11f
        canvas.drawText(name, x + 5f, y + height - 25f, paint)

        // Draw NIP/NIK
        paint.textSize = 9f
        canvas.drawText(nipText, x + 5f, y + height - 10f, paint)
    }

    // MODIFIED: Improved photo page rendering to handle non-sequential photos
    private fun drawPhotosPageContent(canvas: Canvas, photoPageIndex: Int) {
        // Tetapkan photosPerPage = 4 untuk layout 2x2
        val photosPerPage = 4

        val paint = Paint()
        paint.color = Color.BLACK

        // Tambahkan tabel header dulu
        drawHeaderTableOnly(canvas, etHeaderNo.text.toString())

        // Set positions and constants
        val pageWidth = 612f
        val leftMargin = 40f
        val rightMargin = pageWidth - 40f
        var yPosition = 120f

        // Judul halaman foto
        paint.textSize = 14f
        paint.textAlign = Paint.Align.CENTER
        paint.isFakeBoldText = true
        canvas.drawText("DOKUMENTASI FOTO", pageWidth / 2, yPosition, paint)
        paint.textAlign = Paint.Align.LEFT
        paint.isFakeBoldText = false

        yPosition += 30f

        // IMPROVEMENT: Get sorted keys for consistent ordering and all available photos
        val availablePhotoIndices = photoUris.keys.sorted()
        val totalPhotos = availablePhotoIndices.size

        // Calculate range for this page
        val startIdx = photoPageIndex * photosPerPage
        val endIdx = minOf(startIdx + photosPerPage, totalPhotos)

        // If no photos to display on this page
        if (startIdx >= totalPhotos) {
            paint.textSize = 12f
            canvas.drawText("No more photos available.", leftMargin, yPosition + 50f, paint)
            return
        }

        // Dimensi untuk layout grid 2x2
        val availableWidth = rightMargin - leftMargin
        val columnWidth = availableWidth / 2 - 10f
        val photoHeight = 230f

        // Process photos for this page
        for (i in startIdx until endIdx) {
            // Calculate position in page grid (0-3)
            val positionInPage = i - startIdx

            // Grid position: column (0=left, 1=right), row (0=top, 1=bottom)
            val column = positionInPage % 2
            val row = positionInPage / 2

            // Calculate position coordinates
            val xPos = leftMargin + column * (columnWidth + 20f)
            val yPos = yPosition + row * (photoHeight + 70f)

            // Get the original index from sorted list
            val originalIndex = availablePhotoIndices[i]

            // Get corresponding label from photo labels array
            val photoLabel = if (originalIndex < photoLabels.size) {
                photoLabels[originalIndex]
            } else {
                "Foto #${originalIndex + 1}"
            }

            // Draw label with original index number (from 1)
            paint.textSize = 11f
            paint.isFakeBoldText = true
            canvas.drawText("${originalIndex + 1}. $photoLabel", xPos, yPos, paint)
            paint.isFakeBoldText = false

            // Get and display photo
            val uri = photoUris[originalIndex]

            if (uri != null) {
                try {
                    // Load photo bitmap
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inJustDecodeBounds = true
                    }

                    contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, options)
                    }

                    // Calculate scaling
                    val photoScaleFactor = calculateScaleFactor(
                        options.outWidth, options.outHeight,
                        columnWidth.toInt(), (photoHeight - 20f).toInt()
                    )

                    // Load actual bitmap
                    options.apply {
                        inJustDecodeBounds = false
                        inSampleSize = photoScaleFactor
                    }

                    val photoBitmap = contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input, null, options)
                    }

                    if (photoBitmap != null) {
                        // Calculate display dimensions maintaining aspect ratio
                        val bitmapRatio = photoBitmap.width.toFloat() / photoBitmap.height.toFloat()

                        val displayWidth: Float
                        val displayHeight: Float

                        if (bitmapRatio > 1) { // Landscape
                            displayWidth = columnWidth.coerceAtMost(photoBitmap.width.toFloat())
                            displayHeight = displayWidth / bitmapRatio
                        } else { // Portrait
                            displayHeight = (photoHeight - 40f).coerceAtMost(photoBitmap.height.toFloat())
                            displayWidth = displayHeight * bitmapRatio
                        }

                        // Center the photo
                        val xOffset = xPos + (columnWidth - displayWidth) / 2
                        val yOffset = yPos + 20f

                        // Draw with high quality
                        val renderPaint = Paint().apply {
                            isFilterBitmap = true
                            isAntiAlias = true
                        }

                        val displayRect = RectF(
                            xOffset,
                            yOffset,
                            xOffset + displayWidth,
                            yOffset + displayHeight
                        )

                        canvas.drawBitmap(photoBitmap, null, displayRect, renderPaint)

                        // Add border
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 1f
                        paint.color = Color.BLACK
                        canvas.drawRect(displayRect, paint)
                        paint.style = Paint.Style.FILL
                    } else {
                        // Draw placeholder text
                        canvas.drawText("Unable to load photo", xPos, yPos + 50f, paint)
                    }
                } catch (e: Exception) {
                    // Draw error text
                    canvas.drawText("Error loading photo: ${e.message}", xPos, yPos + 50f, paint)
                }
            } else {
                // Draw placeholder for missing photos
                paint.style = Paint.Style.STROKE
                canvas.drawRect(xPos, yPos + 20f, xPos + columnWidth, yPos + 20f + photoHeight - 40f, paint)
                paint.style = Paint.Style.FILL
                canvas.drawText("No photo available", xPos + 20f, yPos + 50f, paint)
            }
        }
    }

    // Helper function to calculate optimal scale factor for bitmap decoding
    private fun calculateScaleFactor(originalWidth: Int, originalHeight: Int, targetWidth: Int, targetHeight: Int): Int {
        var inSampleSize = 1

        if (originalHeight > targetHeight || originalWidth > targetWidth) {
            val halfHeight = originalHeight / 2
            val halfWidth = originalWidth / 2

            // Calculate the largest inSampleSize that is a power of 2 and keeps both
            // height and width larger than the requested height and width
            while ((halfHeight / inSampleSize) >= targetHeight && (halfWidth / inSampleSize) >= targetWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage("BA Survey Mini OLT has been successfully submitted")
            .setPositiveButton("OK") { _, _ ->
                // Reset form
                resetForm()
            }
            .show()
    }

    private fun resetForm() {
        // Clear all form fields
        etLocation.text.clear()
        etNoIhld.text.clear()
        platformDropdown.text.clear()
        etSiteProvider.text.clear()
        etContractNumber.text.clear()

        // Reset PM/PBM dropdowns
        rackDropdown.setText("PM", false)
        rectifierDropdown.setText("PBM", false)
        dcPowerDropdown.setText("PBM", false)
        batteryDropdown.setText("PBM", false)
        mcbDropdown.setText("PM", false)
        groundingDropdown.setText("PM", false)
        indoorRoomDropdown.setText("PM", false)
        acPowerDropdown.setText("PM", false)
        uplinkDropdown.setText("PBM", false)
        conduitDropdown.setText("PM", false)

        // Clear table inputs
        etRackResult.text.clear()
        etRackProposed.text.clear()
        etRectifierResult.text.clear()
        etRectifierProposed.text.clear()
        etDcPowerResult.text.clear()
        etDcPowerProposed.text.clear()
        etBatteryResult.text.clear()
        etBatteryProposed.text.clear()
        etMcbResult.text.clear()
        etMcbProposed.text.clear()
        etGroundingResult.text.clear()
        etGroundingProposed.text.clear()
        etIndoorRoomResult.text.clear()
        etIndoorRoomProposed.text.clear()
        etAcPowerResult.text.clear()
        etAcPowerProposed.text.clear()
        etUplinkResult.text.clear()
        etUplinkProposed.text.clear()
        etConduitResult.text.clear()
        etConduitProposed.text.clear()

        // Clear signature fields
        etZteName.text.clear()
        etTselNopName.text.clear()
        etTselRtpdsName.text.clear()
        etTselRtpeNfName.text.clear()
        etTelkomName.text.clear()
        etTifName.text.clear()
        tvTselRegion.text.clear()

        // Clear NIK fields
        etZteNik.text.clear()
        etTselNopNik.text.clear()
        etTselRtpdsNik.text.clear()
        etTselRtpeNfNik.text.clear()
        etTelkomNik.text.clear()
        etTifNik.text.clear()

        // Clear signature image views
        imgZteSignature.visibility = View.GONE
        imgTselNopSignature.visibility = View.GONE
        imgTselRtpdsSignature.visibility = View.GONE
        imgTselRtpeNfSignature.visibility = View.GONE
        imgTelkomSignature.visibility = View.GONE
        imgTifSignature.visibility = View.GONE

        // Clear photo image views
        for (imageView in photoImageViews) {
            imageView.visibility = View.GONE
        }

        // Clear URI maps
        photoUris.clear()
        signatureUris.clear()

        // Reset permission tracking variables
        pendingGalleryLaunch = false
        permissionExplanationShown = false
    }
}