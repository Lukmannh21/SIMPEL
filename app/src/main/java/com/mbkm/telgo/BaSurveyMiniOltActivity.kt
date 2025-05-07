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
import android.graphics.drawable.BitmapDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.min


class BaSurveyMiniOltActivity : AppCompatActivity() {

    // UI components for main layout
    private lateinit var tabLayout: TabLayout
    private lateinit var formContainer: CardView
    private lateinit var searchContainer: CardView
    private lateinit var btnBack: ImageButton

    // Form input fields
    private lateinit var etLocation: EditText
    private lateinit var etNoIhld: EditText
    private lateinit var etPlatform: EditText
    private lateinit var etSiteProvider: AutoCompleteTextView
    private lateinit var etContractNumber: EditText
    private lateinit var tvCurrentDate: TextView

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

    private lateinit var etTselRegion: EditText

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

        // Set current date
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        tvCurrentDate.text = sdf.format(Date())

        // Set up button listeners
        setupButtonListeners()

        // Set up search adapter
        setupSearchAdapter()

        // Set up dropdown for site provider
        setupSiteProviderDropdown()
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
        etPlatform = findViewById(R.id.etPlatform)
        etSiteProvider = findViewById(R.id.etSiteProvider)
        etContractNumber = findViewById(R.id.etContractNumber)
        tvCurrentDate = findViewById(R.id.tvCurrentDate)

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

        // Signature fields
        etTselRegion = findViewById(R.id.etTselRegion)

        // Photo containers, buttons and image views - with error handling
        try {
            photoContainers = Array(15) { findViewById(resources.getIdentifier("photoContainer${it+1}", "id", packageName)) }
            photoButtons = Array(15) { findViewById(resources.getIdentifier("btnUploadPhoto${it+1}", "id", packageName)) }
            photoImageViews = Array(15) { findViewById(resources.getIdentifier("imgPhoto${it+1}", "id", packageName)) }
        } catch (e: Exception) {
            Log.e("BaSurveyMiniOlt", "Error initializing photo views: ${e.message}")
            photoContainers = emptyArray()
            photoButtons = emptyArray()
            photoImageViews = emptyArray()
        }

        // Photo labels
        photoLabels = arrayOf(
            "Akses gerbang", "Name plate", "Outdoor", "Pengukuran Catuan Power AC",
            "Catuan Power DC", "Port OTB Exciting", "Cabinet Metro-E (ME Room) Metro-E",
            "Metro-E", "Akses gerbang", "Name plate", "Proposed New Pondasi",
            "Power AC di panel KWH Exciting", "Grounding Busbar", "Proposed Dual Source Power DC",
            "Rectifier"
        )

        // Submit and search components
        btnSubmitForm = findViewById(R.id.btnSubmitForm)
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf)
        searchView = findViewById(R.id.searchView)
        rvSearchResults = findViewById(R.id.rvSearchResults)
    }

    private fun setupSiteProviderDropdown() {
        val siteProviderOptions = listOf(
            "DMT", "DMT - Bifurcation", "DMT- Reseller", "IBS", "NO NEED SITAC",
            "NOT READY", "PROTELINDO", "PT Centratama Menara Indonesia",
            "PT Gihon Telekomunikasi Indonesia", "PT Quattro International",
            "PT.Era Bangun Towerindo", "PT.Protelindo", "READY", "STO ROOM",
            "STP", "TBG", "TELKOM", "TELKOMSEL", "TSEL"
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
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
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
                        val imageBitmap = data?.extras?.get("data") as? Bitmap
                        if (imageBitmap != null) {
                            // Save high-quality bitmap
                            val uri = getImageUriFromBitmap(imageBitmap)
                            photoUris[currentPhotoIndex] = uri

                            // Display optimized for UI
                            photoImageViews[currentPhotoIndex].apply {
                                scaleType = ImageView.ScaleType.FIT_CENTER
                                setImageBitmap(imageBitmap)
                                visibility = View.VISIBLE
                            }

                            Log.d("Camera", "Photo captured successfully")
                        } else {
                            Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("Camera", "Error processing camera image: ${e.message}")
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
                            val bitmap = ImageUtils.loadBitmapForUI(this, uri)

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
                REQUEST_SIGNATURE_ZTE -> {
                    val uri = data?.getParcelableExtra<Uri>("signature_uri")
                    if (uri != null) {
                        signatureUris[0] = uri
                        imgZteSignature.setImageURI(uri)
                        imgZteSignature.visibility = View.VISIBLE
                    }
                }
                REQUEST_SIGNATURE_TSEL_NOP -> {
                    val uri = data?.getParcelableExtra<Uri>("signature_uri")
                    if (uri != null) {
                        signatureUris[1] = uri
                        imgTselNopSignature.setImageURI(uri)
                        imgTselNopSignature.visibility = View.VISIBLE
                    }
                }
                REQUEST_SIGNATURE_TSEL_RTPDS -> {
                    val uri = data?.getParcelableExtra<Uri>("signature_uri")
                    if (uri != null) {
                        signatureUris[2] = uri
                        imgTselRtpdsSignature.setImageURI(uri)
                        imgTselRtpdsSignature.visibility = View.VISIBLE
                    }
                }
                REQUEST_SIGNATURE_TSEL_RTPE -> {
                    val uri = data?.getParcelableExtra<Uri>("signature_uri")
                    if (uri != null) {
                        signatureUris[3] = uri
                        imgTselRtpeNfSignature.setImageURI(uri)
                        imgTselRtpeNfSignature.visibility = View.VISIBLE
                    }
                }
                REQUEST_SIGNATURE_TELKOM -> {
                    val uri = data?.getParcelableExtra<Uri>("signature_uri")
                    if (uri != null) {
                        signatureUris[4] = uri
                        imgTelkomSignature.setImageURI(uri)
                        imgTelkomSignature.visibility = View.VISIBLE
                    }
                }
                REQUEST_SIGNATURE_TIF -> {
                    val uri = data?.getParcelableExtra<Uri>("signature_uri")
                    if (uri != null) {
                        signatureUris[5] = uri
                        imgTifSignature.setImageURI(uri)
                        imgTifSignature.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    // Improved method to get high quality image URI from bitmap
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

        if (etPlatform.text.toString().isEmpty()) {
            etPlatform.error = "Required field"
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

        val surveyData = HashMap<String, Any>()

        // Basic info
        surveyData["location"] = etLocation.text.toString().trim()
        surveyData["noIhld"] = etNoIhld.text.toString().trim()
        surveyData["platform"] = etPlatform.text.toString().trim()
        surveyData["siteProvider"] = etSiteProvider.text.toString().trim()
        surveyData["contractNumber"] = etContractNumber.text.toString().trim()
        surveyData["surveyDate"] = tvCurrentDate.text.toString()
        surveyData["createdBy"] = currentUser.email ?: "unknown"
        surveyData["createdAt"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Table results
        val tableResults = HashMap<String, HashMap<String, String>>()

        // Rack
        val rackData = HashMap<String, String>()
        rackData["responsibility"] = "PM"
        rackData["surveyResult"] = etRackResult.text.toString().trim()
        rackData["proposed"] = etRackProposed.text.toString().trim()
        tableResults["rack"] = rackData

        // Rectifier
        val rectifierData = HashMap<String, String>()
        rectifierData["responsibility"] = "PBM"
        rectifierData["surveyResult"] = etRectifierResult.text.toString().trim()
        rectifierData["proposed"] = etRectifierProposed.text.toString().trim()
        tableResults["rectifier"] = rectifierData

        // DC Power
        val dcPowerData = HashMap<String, String>()
        dcPowerData["responsibility"] = "PBM"
        dcPowerData["surveyResult"] = etDcPowerResult.text.toString().trim()
        dcPowerData["proposed"] = etDcPowerProposed.text.toString().trim()
        tableResults["dcPower"] = dcPowerData

        // Battery
        val batteryData = HashMap<String, String>()
        batteryData["responsibility"] = "PBM"
        batteryData["surveyResult"] = etBatteryResult.text.toString().trim()
        batteryData["proposed"] = etBatteryProposed.text.toString().trim()
        tableResults["battery"] = batteryData

        // MCB
        val mcbData = HashMap<String, String>()
        mcbData["responsibility"] = "PM"
        mcbData["surveyResult"] = etMcbResult.text.toString().trim()
        mcbData["proposed"] = etMcbProposed.text.toString().trim()
        tableResults["mcb"] = mcbData

        // Grounding
        val groundingData = HashMap<String, String>()
        groundingData["responsibility"] = "PM"
        groundingData["surveyResult"] = etGroundingResult.text.toString().trim()
        groundingData["proposed"] = etGroundingProposed.text.toString().trim()
        tableResults["grounding"] = groundingData

        // Indoor Room
        val indoorRoomData = HashMap<String, String>()
        indoorRoomData["responsibility"] = "PM"
        indoorRoomData["surveyResult"] = etIndoorRoomResult.text.toString().trim()
        indoorRoomData["proposed"] = etIndoorRoomProposed.text.toString().trim()
        tableResults["indoorRoom"] = indoorRoomData

        // AC Power
        val acPowerData = HashMap<String, String>()
        acPowerData["responsibility"] = "PM"
        acPowerData["surveyResult"] = etAcPowerResult.text.toString().trim()
        acPowerData["proposed"] = etAcPowerProposed.text.toString().trim()
        tableResults["acPower"] = acPowerData

        // Uplink
        val uplinkData = HashMap<String, String>()
        uplinkData["responsibility"] = "PBM"
        uplinkData["surveyResult"] = etUplinkResult.text.toString().trim()
        uplinkData["proposed"] = etUplinkProposed.text.toString().trim()
        tableResults["uplink"] = uplinkData

        // Conduit
        val conduitData = HashMap<String, String>()
        conduitData["responsibility"] = "PM"
        conduitData["surveyResult"] = etConduitResult.text.toString().trim()
        conduitData["proposed"] = etConduitProposed.text.toString().trim()
        tableResults["conduit"] = conduitData

        surveyData["tableResults"] = tableResults

        // Signatures
        val signaturesData = HashMap<String, HashMap<String, String>>()

        // ZTE
        val zteData = HashMap<String, String>()
        zteData["name"] = etZteName.text.toString().trim()
        zteData["role"] = "PT. ZTE INDONESIA\nTIM SURVEY"
        signaturesData["zte"] = zteData

        // TSEL NOP
        val tselNopData = HashMap<String, String>()
        tselNopData["name"] = etTselNopName.text.toString().trim()
        tselNopData["role"] = "PT. TELKOMSEL\nMGR NOP\n${tvTselRegion.text}"
        signaturesData["tselNop"] = tselNopData

        // TSEL RTPDS
        val tselRtpdsData = HashMap<String, String>()
        tselRtpdsData["name"] = etTselRtpdsName.text.toString().trim()
        tselRtpdsData["role"] = "PT. TELKOMSEL\nMGR RTPDS\n${tvTselRegion.text}"
        signaturesData["tselRtpds"] = tselRtpdsData

        // TSEL RTPE/NF
        val tselRtpeNfData = HashMap<String, String>()
        tselRtpeNfData["name"] = etTselRtpeNfName.text.toString().trim()
        tselRtpeNfData["role"] = "PT. TELKOMSEL\nMGR RTPE\n${tvTselRegion.text}"
        signaturesData["tselRtpeNf"] = tselRtpeNfData

        // TELKOM
        val telkomData = HashMap<String, String>()
        telkomData["name"] = etTelkomName.text.toString().trim()
        telkomData["role"] = "PT. TELKOM\nMGR NDPS TR1"
        signaturesData["telkom"] = telkomData

        // TIF
        val tifData = HashMap<String, String>()
        tifData["name"] = etTifName.text.toString().trim()
        tifData["role"] = "PT. TIF\nTIM SURVEY"
        signaturesData["tif"] = tifData

        surveyData["signatures"] = signaturesData

        // Add to Firestore
        firestore.collection("ba_survey_mini_olt")
            .add(surveyData)
            .addOnSuccessListener { documentReference ->
                val surveyId = documentReference.id

                // Upload images
                uploadImagesToFirebase(surveyId) { success ->
                    if (success) {
                        // Generate and upload PDF
                        generateAndUploadPdf(surveyId) { pdfSuccess ->
                            loadingDialog.dismiss()

                            if (pdfSuccess) {
                                showSuccessDialog()
                            } else {
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

        // Upload photos
        for ((index, uri) in photoUris) {
            val photoRef = storage.reference.child("ba_survey_mini_olt/$surveyId/photos/photo${index+1}.jpg")

            photoRef.putFile(uri)
                .addOnSuccessListener {
                    photoRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Update document with download URL
                        firestore.collection("ba_survey_mini_olt").document(surveyId)
                            .update("photos.photo${index+1}", downloadUrl.toString())
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
                    }
                }
                .addOnFailureListener {
                    failedUploads++
                    if (completedUploads + failedUploads == totalUploads) {
                        callback(failedUploads == 0)
                    }
                }
        }

        // Upload signatures
        val signatureKeys = arrayOf("zte", "tselNop", "tselRtpds", "tselRtpeNf", "telkom", "tif")

        for ((index, uri) in signatureUris) {
            val signatureRef = storage.reference.child("ba_survey_mini_olt/$surveyId/signatures/${signatureKeys[index]}.png")

            signatureRef.putFile(uri)
                .addOnSuccessListener {
                    signatureRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Update document with download URL
                        firestore.collection("ba_survey_mini_olt").document(surveyId)
                            .update("signatures.${signatureKeys[index]}.signatureUrl", downloadUrl.toString())
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
                    }
                }
                .addOnFailureListener {
                    failedUploads++
                    if (completedUploads + failedUploads == totalUploads) {
                        callback(failedUploads == 0)
                    }
                }
        }
    }

    private fun generateAndUploadPdf(surveyId: String, callback: (Boolean) -> Unit) {
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
                                callback(true)
                            }
                            .addOnFailureListener {
                                callback(false)
                            }
                    }
                }
                .addOnFailureListener {
                    callback(false)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            callback(false)
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

    // Improved PDF generation method with better photo quality and text handling
    private fun generatePdfFile(): File {
        // Create PDF document
        val document = PdfDocument()

        // Create first page for form data
        val pageInfo1 = PdfDocument.PageInfo.Builder(612, 842, 1).create()
        val page1 = document.startPage(pageInfo1)
        val canvas1 = page1.canvas
        val paint = Paint()

        // Draw main form content on first page
        drawPdfContent(canvas1, paint)
        document.finishPage(page1)

        // Draw signatures on a dedicated page
        drawSignaturesPage(document)

        // Draw photos on their own page(s) - with improved quality
        drawPhotoContentPages(document)

        // Create PDF file
        val fileName = "BA_Survey_Mini_OLT_${etLocation.text}_${System.currentTimeMillis()}.pdf"
        val filePath = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        try {
            val fos = FileOutputStream(filePath)
            document.writeTo(fos)
            fos.close()
        } catch (e: Exception) {
            Log.e("PDF Generation", "Error writing to PDF: ${e.message}")
        }

        document.close()

        return filePath
    }

    // Content for the first page (basic information and table)
    private fun drawPdfContent(canvas: Canvas, paint: Paint) {
        // Set initial positions and constants
        val pageWidth = 612f
        val leftMargin = 40f
        val rightMargin = pageWidth - 40f
        var yPosition = 60f

        // Set text properties
        paint.textSize = 12f
        paint.color = Color.BLACK
        paint.textAlign = Paint.Align.LEFT

        // Draw header
        try {
            val logo = BitmapFactory.decodeResource(resources, R.drawable.ic_check_circle)
            if (logo != null) {
                // Scale logo to appropriate size
                val scaledLogo = Bitmap.createScaledBitmap(logo, 80, 40, true)
                canvas.drawBitmap(scaledLogo, leftMargin, yPosition, paint)
            }
        } catch (e: Exception) {
            Log.e("PDF Generation", "Error drawing logo: ${e.message}")
        }

        // Draw title
        paint.textSize = 16f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("BERITA ACARA SURVEY MINI OLT", pageWidth / 2, yPosition + 20f, paint)
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.LEFT
        yPosition += 60f

        // Draw separator line
        paint.strokeWidth = 1f
        canvas.drawLine(leftMargin, yPosition, rightMargin, yPosition, paint)
        yPosition += 20f

        // Draw basic information
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Basic Information", leftMargin, yPosition, paint)
        paint.isFakeBoldText = false
        yPosition += 20f

        paint.textSize = 12f
        canvas.drawText("Lokasi: " + etLocation.text.toString(), leftMargin, yPosition, paint)
        yPosition += 15f

        canvas.drawText("NO IHLD/LOP: " + etNoIhld.text.toString(), leftMargin, yPosition, paint)
        yPosition += 15f

        canvas.drawText("Platform: " + etPlatform.text.toString(), leftMargin, yPosition, paint)
        yPosition += 15f

        canvas.drawText("Site Provider: " + etSiteProvider.text.toString(), leftMargin, yPosition, paint)
        yPosition += 15f

        canvas.drawText("Nomor Kontrak: " + etContractNumber.text.toString(), leftMargin, yPosition, paint)
        yPosition += 15f

        // Fix for "pada hari ini..." text visibility - make it wrap properly with shorter line
        val dateText = "Pada hari ini " + tvCurrentDate.text.toString() + " telah dilaksanakan"
        canvas.drawText(dateText, leftMargin, yPosition, paint)
        yPosition += 15f
        canvas.drawText("survey sarana penunjang (SARANA) dengan hasil sebagai berikut:", leftMargin, yPosition, paint)
        yPosition += 25f

        // Draw table header
        val tableWidth = rightMargin - leftMargin
        val col1Width = tableWidth * 0.35f  // Specification column - slightly wider
        val col2Width = tableWidth * 0.1f   // PM/PBM column
        val col3Width = tableWidth * 0.275f // Result column
        val col4Width = tableWidth * 0.275f // Proposed column

        // Fix header text overlap issue
        drawTableHeader(canvas, paint, leftMargin, yPosition, col1Width, col2Width, col3Width, col4Width)
        yPosition += 25f

        // Draw table rows
        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "1. Rack tempat Perangkat MINI OLT & OTB", "PM",
            etRackResult.text.toString(), etRackProposed.text.toString(),
            col1Width, col2Width, col3Width, col4Width)

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "2. Rectifier", "PBM",
            etRectifierResult.text.toString(), etRectifierProposed.text.toString(),
            col1Width, col2Width, col3Width, col4Width)

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "3. Ketersediaan Daya DC", "PBM",
            etDcPowerResult.text.toString(), etDcPowerProposed.text.toString(),
            col1Width, col2Width, col3Width, col4Width)

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "4. Baterai", "PBM",
            etBatteryResult.text.toString(), etBatteryProposed.text.toString(),
            col1Width, col2Width, col3Width, col4Width)

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "5. MCB untuk Pemasangan Mini OLT", "PM",
            etMcbResult.text.toString(), etMcbProposed.text.toString(),
            col1Width, col2Width, col3Width, col4Width)

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "6. Grounding", "PM",
            etGroundingResult.text.toString(), etGroundingProposed.text.toString(),
            col1Width, col2Width, col3Width, col4Width)

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "7. Indoor Room", "PM",
            etIndoorRoomResult.text.toString(), etIndoorRoomProposed.text.toString(),
            col1Width, col2Width, col3Width, col4Width)

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "8. Ketersediaan Daya AC", "PM",
            etAcPowerResult.text.toString(), etAcPowerProposed.text.toString(),
            col1Width, col2Width, col3Width, col4Width)

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "9. BA Kesiapan Uplink", "PBM",
            etUplinkResult.text.toString(), etUplinkProposed.text.toString(),
            col1Width, col2Width, col3Width, col4Width)

        yPosition = drawTableRow(canvas, paint, leftMargin, yPosition,
            "10. Conduit", "PM",
            etConduitResult.text.toString(), etConduitProposed.text.toString(),
            col1Width, col2Width, col3Width, col4Width)

        // Add PM/PBM legend below the table
        yPosition += 15f
        paint.textSize = 10f
        canvas.drawText("*PM : Permintaan Memungkinkan", leftMargin, yPosition, paint)
        yPosition += 12f
        canvas.drawText("**PBM : Permintaan Belum Memungkinkan", leftMargin, yPosition, paint)
        yPosition += 20f
        // Add Attachments section
        paint.textSize = 12f
        paint.isFakeBoldText = true
        canvas.drawText("Lampiran:", leftMargin, yPosition, paint)
        paint.isFakeBoldText = false
        yPosition += 15f

        canvas.drawText("1. Layout Lokasi", leftMargin + 10f, yPosition, paint)
        yPosition += 12f
        canvas.drawText("2. Foto Dokumentasi", leftMargin + 10f, yPosition, paint)
        yPosition += 12f
        canvas.drawText("3. Proposed Penempatan", leftMargin + 10f, yPosition, paint)
    }

    // Create a separate page for signatures to ensure they all fit and aren't cut off
    private fun drawSignaturesPage(document: PdfDocument) {
        val pageInfo = PdfDocument.PageInfo.Builder(612, 842, 2).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Set initial positions and constants
        val pageWidth = 612f
        val leftMargin = 40f
        val rightMargin = pageWidth - 40f
        var yPosition = 60f

        // Draw page title
        paint.textSize = 16f
        paint.color = Color.BLACK
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Signatures", pageWidth / 2, yPosition, paint)
        paint.isFakeBoldText = false
        paint.textAlign = Paint.Align.LEFT
        yPosition += 40f

        // Get Telkomsel Region text safely
        val tselRegionText = try {
            findViewById<EditText>(R.id.etTselRegion)?.text?.toString() ?: "REGION"
        } catch (e: Exception) {
            "REGION"
        }

        // Draw signature boxes with actual signatures - use a 2x3 grid for better visibility
        val tableWidth = rightMargin - leftMargin
        val signatureWidth = (tableWidth - 20f) / 2  // 2 columns

        // First row of signatures - 2 signatures per row
        drawSignatureBoxWithImage(canvas, paint, leftMargin, yPosition,
            "PT. ZTE INDONESIA", "TIM SURVEY",
            etZteName.text.toString(), signatureWidth,
            imgZteSignature)

        drawSignatureBoxWithImage(canvas, paint, leftMargin + signatureWidth + 20f, yPosition,
            "PT. TELKOMSEL", "MGR NOP\n" + tselRegionText,
            etTselNopName.text.toString(), signatureWidth,
            imgTselNopSignature)

        yPosition += 120f // More space between rows

        // Second row of signatures
        drawSignatureBoxWithImage(canvas, paint, leftMargin, yPosition,
            "PT. TELKOMSEL", "MGR RTPDS\n" + tselRegionText,
            etTselRtpdsName.text.toString(), signatureWidth,
            imgTselRtpdsSignature)

        drawSignatureBoxWithImage(canvas, paint, leftMargin + signatureWidth + 20f, yPosition,
            "PT. TELKOMSEL", "MGR RTPE\n" + tselRegionText,
            etTselRtpeNfName.text.toString(), signatureWidth,
            imgTselRtpeNfSignature)

        yPosition += 120f // More space between rows

        // Third row of signatures
        drawSignatureBoxWithImage(canvas, paint, leftMargin, yPosition,
            "PT. TELKOM", "MGR NDPS TR1",
            etTelkomName.text.toString(), signatureWidth,
            imgTelkomSignature)

        drawSignatureBoxWithImage(canvas, paint, leftMargin + signatureWidth + 20f, yPosition,
            "PT. TIF", "TIM SURVEY",
            etTifName.text.toString(), signatureWidth,
            imgTifSignature)

        document.finishPage(page)
    }

    // High-quality signature image rendering
    private fun drawSignatureBoxWithImage(canvas: Canvas, paint: Paint, x: Float, y: Float,
                                          company: String, role: String, name: String, width: Float,
                                          signatureImageView: ImageView) {

        val height = 100f

        // Draw box
        paint.color = Color.WHITE
        canvas.drawRect(x, y, x + width, y + height, paint)

        // Draw border
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawLine(x, y, x + width, y, paint)
        canvas.drawLine(x, y, x, y + height, paint)
        canvas.drawLine(x + width, y, x + width, y + height, paint)
        canvas.drawLine(x, y + height, x + width, y + height, paint)

        // Draw text
        paint.textSize = 10f
        paint.isFakeBoldText = true
        canvas.drawText(company, x + 5f, y + 15f, paint)
        paint.isFakeBoldText = false

        // Draw role on multiple lines if needed
        val roleLines = role.split("\n")
        for (i in roleLines.indices) {
            canvas.drawText(roleLines[i], x + 5f, y + 30f + (i * 12f), paint)
        }

        // Draw signature image if available - IMPROVED QUALITY
        try {
            if (signatureImageView.visibility == View.VISIBLE && signatureImageView.drawable != null) {
                // Convert ImageView's drawable to bitmap with high quality
                val drawable = signatureImageView.drawable
                if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
                    // Get original bitmap at full quality
                    val originalBitmap = drawable.bitmap

                    // Scale the bitmap to fit in the box while maintaining quality
                    val signatureWidth = width - 20f
                    val signatureHeight = 40f

                    // Create a matrix for high-quality scaling
                    val matrix = Matrix()
                    val scaleX = signatureWidth / originalBitmap.width
                    val scaleY = signatureHeight / originalBitmap.height
                    matrix.setScale(scaleX, scaleY)

                    // Create a new bitmap with the correct size and high quality
                    val scaledBitmap = Bitmap.createBitmap(
                        signatureWidth.toInt(),
                        signatureHeight.toInt(),
                        Bitmap.Config.ARGB_8888
                    )

                    // Draw the original bitmap onto the scaled bitmap using a canvas
                    val tempCanvas = Canvas(scaledBitmap)
                    tempCanvas.drawBitmap(originalBitmap, matrix, paint)

                    // Draw with anti-aliasing to make it smoother
                    paint.isAntiAlias = true
                    paint.isFilterBitmap = true

                    // Draw the high-quality scaled bitmap
                    canvas.drawBitmap(scaledBitmap, x + 10f, y + 40f, paint)
                } else {
                    // Draw signature line if bitmap not available
                    canvas.drawLine(x + 10f, y + 60f, x + width - 10f, y + 60f, paint)
                }
            } else {
                // Draw signature line if no image
                canvas.drawLine(x + 10f, y + 60f, x + width - 10f, y + 60f, paint)
            }
        } catch (e: Exception) {
            Log.e("PDF Generation", "Error drawing signature: ${e.message}")
            // If we can't draw the signature, add a signature line instead
            canvas.drawLine(x + 10f, y + 60f, x + width - 10f, y + 60f, paint)
        }

        // Draw name
        canvas.drawText(name, x + 5f, y + 85f, paint)
    }

    // Completely revised photo rendering method for PDF - 2 photos per page for better quality
    private fun drawPhotoContentPages(document: PdfDocument) {
        // Count available photos
        val availablePhotos = ArrayList<Int>()
        for (i in 0 until photoImageViews.size) {
            if (photoImageViews[i].visibility == View.VISIBLE &&
                photoUris.containsKey(i) &&
                photoImageViews[i].drawable != null) {
                availablePhotos.add(i)
            }
        }

        // Return if no photos
        if (availablePhotos.isEmpty()) {
            val pageInfo = PdfDocument.PageInfo.Builder(612, 842, 3).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            paint.textSize = 16f
            paint.color = Color.BLACK
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("FOTO DOKUMENTASI", 612f / 2, 60f, paint)
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.LEFT

            paint.textSize = 12f
            canvas.drawText("No photos have been uploaded.", 40f, 100f, paint)

            document.finishPage(page)
            return
        }

        // Constants for photo layout - 2 photos per page
        val photosPerPage = 2
        val pageWidth = 612f
        val leftMargin = 40f
        val rightMargin = pageWidth - 40f
        val availableWidth = rightMargin - leftMargin
        val columnWidth = (availableWidth - 20f) / 2  // Split width with 20pt gap
        val maxPhotoHeight = 320f  // Taller for better quality

        // Process photos in pairs
        var pageCount = 3
        var currentPhotoIndex = 0

        while (currentPhotoIndex < availablePhotos.size) {
            // Create a new page
            val pageInfo = PdfDocument.PageInfo.Builder(612, 842, pageCount++).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()

            // Draw page title
            paint.textSize = 16f
            paint.color = Color.BLACK
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("FOTO DOKUMENTASI", pageWidth / 2, 60f, paint)
            paint.isFakeBoldText = false
            paint.textAlign = Paint.Align.LEFT
            var yPosition = 100f

            // Process photos for this page (up to 2)
            for (column in 0 until photosPerPage) {
                if (currentPhotoIndex >= availablePhotos.size) break

                val photoIdx = availablePhotos[currentPhotoIndex]
                val uri = photoUris[photoIdx] ?: continue
                val xStart = leftMargin + (column * (columnWidth + 20f))

                try {
                    // Draw label
                    paint.textSize = 12f
                    paint.isFakeBoldText = true
                    val label = if (photoIdx < photoLabels.size) photoLabels[photoIdx] else "Photo ${photoIdx+1}"
                    canvas.drawText("${photoIdx+1}. $label", xStart, yPosition, paint)
                    paint.isFakeBoldText = false

                    // Load high quality bitmap for PDF
                    val bitmap = ImageUtils.loadBitmapForPDF(this, uri)

                    if (bitmap != null) {
                        // Calculate dimensions preserving aspect ratio
                        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        var finalWidth: Float
                        var finalHeight: Float

                        if (aspectRatio > 1) { // Landscape
                            finalWidth = columnWidth
                            finalHeight = finalWidth / aspectRatio
                            if (finalHeight > maxPhotoHeight) {
                                finalHeight = maxPhotoHeight
                                finalWidth = finalHeight * aspectRatio
                            }
                        } else { // Portrait
                            finalHeight = maxPhotoHeight
                            finalWidth = finalHeight * aspectRatio
                            if (finalWidth > columnWidth) {
                                finalWidth = columnWidth
                                finalHeight = finalWidth / aspectRatio
                            }
                        }

                        // Center image in column
                        val xOffset = xStart + ((columnWidth - finalWidth) / 2)
                        val imageY = yPosition + 20f

                        // HIGH-QUALITY RENDERING
                        val renderOptions = Paint().apply {
                            isFilterBitmap = true
                            isAntiAlias = true
                            isDither = true
                        }

                        // Draw the bitmap using RectF for smoother scaling
                        val rect = RectF(xOffset, imageY, xOffset + finalWidth, imageY + finalHeight)
                        canvas.drawBitmap(bitmap, null, rect, renderOptions)

                        // Draw border
                        paint.style = Paint.Style.STROKE
                        paint.color = Color.BLACK
                        paint.strokeWidth = 1f
                        canvas.drawRect(rect, paint)
                        paint.style = Paint.Style.FILL

                        // Recycle the bitmap to prevent memory leaks
                        bitmap.recycle()
                    } else {
                        canvas.drawText("Error loading image", xStart, yPosition + 30f, paint)
                    }
                } catch (e: Exception) {
                    Log.e("PDF Photos", "Error processing photo ${photoIdx+1}: ${e.message}")
                    canvas.drawText("Error processing image", xStart, yPosition + 30f, paint)
                }

                currentPhotoIndex++
            }

            document.finishPage(page)
        }
    }

    // Fixed table header function to prevent text overlap
    private fun drawTableHeader(canvas: Canvas, paint: Paint, x: Float, y: Float, col1Width: Float, col2Width: Float, col3Width: Float, col4Width: Float) {
        val headerHeight = 25f

        // Draw header background
        paint.color = Color.parseColor("#1e88e5")
        canvas.drawRect(x, y, x + col1Width + col2Width + col3Width + col4Width, y + headerHeight, paint)

        // Draw header text - use smaller font size to prevent overlap
        paint.color = Color.WHITE
        paint.textSize = 8f  // Smaller text size
        paint.isFakeBoldText = true

        canvas.drawText("SPESIFIKASI DAN KEBUTUHAN", x + 5f, y + 17f, paint)
        canvas.drawText("PM/PBM", x + col1Width + 5f, y + 17f, paint)
        canvas.drawText("HASIL SURVEY", x + col1Width + col2Width + 5f, y + 17f, paint)
        canvas.drawText("KESEPAKATAN/PROPOSED", x + col1Width + col2Width + col3Width + 5f, y + 17f, paint)

        paint.isFakeBoldText = false

        // Draw header borders
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawLine(x, y, x + col1Width + col2Width + col3Width + col4Width, y, paint)
        canvas.drawLine(x, y + headerHeight, x + col1Width + col2Width + col3Width + col4Width, y + headerHeight, paint)
        canvas.drawLine(x, y, x, y + headerHeight, paint)
        canvas.drawLine(x + col1Width, y, x + col1Width, y + headerHeight, paint)
        canvas.drawLine(x + col1Width + col2Width, y, x + col1Width + col2Width, y + headerHeight, paint)
        canvas.drawLine(x + col1Width + col2Width + col3Width, y, x + col1Width + col2Width + col3Width, y + headerHeight, paint)
        canvas.drawLine(x + col1Width + col2Width + col3Width + col4Width, y, x + col1Width + col2Width + col3Width + col4Width, y + headerHeight, paint)
    }

    // Fixed table row function to properly handle long text
    private fun drawTableRow(canvas: Canvas, paint: Paint, x: Float, y: Float,
                             spec: String, pmPbm: String, result: String, proposed: String,
                             col1Width: Float, col2Width: Float, col3Width: Float, col4Width: Float): Float {

        // Set smaller font size for potentially long text
        paint.textSize = 8f  // Smaller text to fit more content

        // Calculate row height based on content length with improved text wrapping
        val textWidth1 = col1Width - 10f
        val textWidth3 = col3Width - 10f
        val textWidth4 = col4Width - 10f

        // Split text into multiple lines with proper word wrapping
        val specLines = getMultiLineTextImproved(spec, paint, textWidth1)
        val resultLines = getMultiLineTextImproved(result, paint, textWidth3)
        val proposedLines = getMultiLineTextImproved(proposed, paint, textWidth4)

        val maxLines = maxOf(specLines.size, resultLines.size, proposedLines.size, 1)
        val lineHeight = 12f  // Slightly smaller line height for compact text
        val rowHeight = maxLines * lineHeight + 10f

        // Draw row background
        paint.color = Color.WHITE
        canvas.drawRect(x, y, x + col1Width + col2Width + col3Width + col4Width, y + rowHeight, paint)

        // Draw row text
        paint.color = Color.BLACK

        // Draw spec text
        for (i in specLines.indices) {
            canvas.drawText(specLines[i], x + 5f, y + 12f + (i * lineHeight), paint)
        }

        // Draw PM/PBM
        canvas.drawText(pmPbm, x + col1Width + 5f, y + 12f, paint)

        // Draw result text
        for (i in resultLines.indices) {
            canvas.drawText(resultLines[i], x + col1Width + col2Width + 5f, y + 12f + (i * lineHeight), paint)
        }

        // Draw proposed text
        for (i in proposedLines.indices) {
            canvas.drawText(proposedLines[i], x + col1Width + col2Width + col3Width + 5f, y + 12f + (i * lineHeight), paint)
        }

        // Draw row borders
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        canvas.drawLine(x, y, x + col1Width + col2Width + col3Width + col4Width, y, paint)
        canvas.drawLine(x, y + rowHeight, x + col1Width + col2Width + col3Width + col4Width, y + rowHeight, paint)
        canvas.drawLine(x, y, x, y + rowHeight, paint)
        canvas.drawLine(x + col1Width, y, x + col1Width, y + rowHeight, paint)
        canvas.drawLine(x + col1Width + col2Width, y, x + col1Width + col2Width, y + rowHeight, paint)
        canvas.drawLine(x + col1Width + col2Width + col3Width, y, x + col1Width + col2Width + col3Width, y + rowHeight, paint)
        canvas.drawLine(x + col1Width + col2Width + col3Width + col4Width, y, x + col1Width + col2Width + col3Width + col4Width, y + rowHeight, paint)

        return y + rowHeight
    }

    // Improved text wrapping function that handles long text better
    private fun getMultiLineTextImproved(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = ArrayList<String>()
        if (text.isEmpty()) {
            lines.add("")
            return lines
        }

        // Handle very long text with no spaces
        if (!text.contains(" ")) {
            val chars = text.toCharArray()
            val stringBuilder = StringBuilder()

            for (char in chars) {
                val testStr = stringBuilder.toString() + char
                if (paint.measureText(testStr) <= maxWidth) {
                    stringBuilder.append(char)
                } else {
                    lines.add(stringBuilder.toString())
                    stringBuilder.clear()
                    stringBuilder.append(char)
                }
            }

            if (stringBuilder.isNotEmpty()) {
                lines.add(stringBuilder.toString())
            }

            return lines
        }

        // Normal word-based wrapping
        val words = text.split(" ")
        var currentLine = StringBuilder(words[0])

        for (i in 1 until words.size) {
            val word = words[i]
            val testLine = "${currentLine} $word"
            val testWidth = paint.measureText(testLine)

            if (testWidth <= maxWidth) {
                currentLine.append(" ").append(word)
            } else {
                // Check if single word is too long
                if (currentLine.isEmpty() || currentLine.toString() == words[i-1]) {
                    val longWord = word
                    var subWord = ""

                    for (char in longWord) {
                        if (paint.measureText(subWord + char) <= maxWidth) {
                            subWord += char
                        } else {
                            lines.add(subWord)
                            subWord = char.toString()
                        }
                    }

                    if (subWord.isNotEmpty()) {
                        currentLine = StringBuilder(subWord)
                    } else {
                        currentLine = StringBuilder()
                    }
                } else {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                }
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }

        return lines
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
        etPlatform.text.clear()
        etSiteProvider.text.clear()
        etContractNumber.text.clear()

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