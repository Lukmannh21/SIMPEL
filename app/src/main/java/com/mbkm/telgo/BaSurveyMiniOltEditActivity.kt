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
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class BaSurveyMiniOltEditActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Toolbar
    private lateinit var btnBack: ImageButton

    // Basic Info Fields
    private lateinit var etHeaderNo: EditText
    private lateinit var etLocation: AutoCompleteTextView
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

    // Table Results
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

    // Signature Fields
    private lateinit var etZteName: EditText
    private lateinit var etZteNik: EditText
    private lateinit var etTselNopName: EditText
    private lateinit var etTselNopNik: EditText
    private lateinit var etTselNopRegion: EditText
    private lateinit var etTselRtpdsName: EditText
    private lateinit var etTselRtpdsNik: EditText
    private lateinit var etTselRtpdsRegion: EditText
    private lateinit var etTselRtpeNfName: EditText
    private lateinit var etTselRtpeNfNik: EditText
    private lateinit var etTselRtpeNfRegion: EditText
    private lateinit var etTelkomName: EditText
    private lateinit var etTelkomNik: EditText
    private lateinit var etTifName: EditText
    private lateinit var etTifNik: EditText

    // Signature Buttons & Images
    private lateinit var btnZteSignature: Button
    private lateinit var btnTselNopSignature: Button
    private lateinit var btnTselRtpdsSignature: Button
    private lateinit var btnTselRtpeNfSignature: Button
    private lateinit var btnTelkomSignature: Button
    private lateinit var btnTifSignature: Button

    private lateinit var imgZteSignature: ImageView
    private lateinit var imgTselNopSignature: ImageView
    private lateinit var imgTselRtpdsSignature: ImageView
    private lateinit var imgTselRtpeNfSignature: ImageView
    private lateinit var imgTelkomSignature: ImageView
    private lateinit var imgTifSignature: ImageView

    // Photo Components
    private lateinit var photoButtons: Array<Button>
    private lateinit var photoImageViews: Array<ImageView>
    private lateinit var photoLabels: Array<String>

    // Action Buttons
    private lateinit var btnCancel: Button
    private lateinit var btnUpdateForm: Button
    private lateinit var btnGeneratePdf: Button

    // Data
    private var surveyId: String = ""
    private var currentPhotoIndex = 0
    private val photoUris = HashMap<Int, Uri>()
    private val signatureUris = HashMap<Int, Uri>()
    private val validSiteIds = ArrayList<String>()
    private var currentPhotoUri: Uri? = null
    private var isSiteIdValid = false

    // Track old URLs untuk cleanup
    private val oldPhotoUrls = mutableMapOf<Int, String>()
    private val oldSignatureUrls = mutableMapOf<String, String>()

    // Constants
    private val REQUEST_IMAGE_CAPTURE = 102
    private val REQUEST_GALLERY = 103
    private val REQUEST_SIGNATURE_ZTE = 201
    private val REQUEST_SIGNATURE_TSEL_NOP = 202
    private val REQUEST_SIGNATURE_TSEL_RTPDS = 203
    private val REQUEST_SIGNATURE_TSEL_RTPE = 204
    private val REQUEST_SIGNATURE_TELKOM = 205
    private val REQUEST_SIGNATURE_TIF = 206

    private val signatureKeys = arrayOf("zte", "tselNop", "tselRtpds", "tselRtpeNf", "telkom", "tif")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ba_survey_mini_olt_edit)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Get survey ID
        surveyId = intent.getStringExtra("SURVEY_ID") ?: ""
        if (surveyId.isEmpty()) {
            Toast.makeText(this, "Invalid survey ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize UI
        initializeUI()

        // Request permissions
        requestRequiredPermissions()

        // Load survey data
        loadSurveyData()

        // Setup listeners
        setupButtonListeners()
        setupDropdowns()

        // Set date
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        tvCurrentDate.text = sdf.format(Date())
    }

    private fun initializeUI() {
        btnBack = findViewById(R.id.btnBack)

        // Basic Info
        etHeaderNo = findViewById(R.id.etHeaderNo)
        etLocation = findViewById(R.id.etLocation)
        etNoIhld = findViewById(R.id.etNoIhld)
        platformDropdown = findViewById(R.id.platformDropdown)
        etSiteProvider = findViewById(R.id.etSiteProvider)
        etContractNumber = findViewById(R.id.etContractNumber)
        tvCurrentDate = findViewById(R.id.tvCurrentDate)

        // PM/PBM Dropdowns
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

        // Table Results
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

        // Signature Fields
        etZteName = findViewById(R.id.etZteName)
        etZteNik = findViewById(R.id.etZteNik)
        etTselNopName = findViewById(R.id.etTselNopName)
        etTselNopNik = findViewById(R.id.etTselNopNik)
        etTselNopRegion = findViewById(R.id.etTselNopRegion)
        etTselRtpdsName = findViewById(R.id.etTselRtpdsName)
        etTselRtpdsNik = findViewById(R.id.etTselRtpdsNik)
        etTselRtpdsRegion = findViewById(R.id.etTselRtpdsRegion)
        etTselRtpeNfName = findViewById(R.id.etTselRtpeNfName)
        etTselRtpeNfNik = findViewById(R.id.etTselRtpeNfNik)
        etTselRtpeNfRegion = findViewById(R.id.etTselRtpeNfRegion)
        etTelkomName = findViewById(R.id.etTelkomName)
        etTelkomNik = findViewById(R.id.etTelkomNik)
        etTifName = findViewById(R.id.etTifName)
        etTifNik = findViewById(R.id.etTifNik)

        // Signature Buttons
        btnZteSignature = findViewById(R.id.btnZteSignature)
        btnTselNopSignature = findViewById(R.id.btnTselNopSignature)
        btnTselRtpdsSignature = findViewById(R.id.btnTselRtpdsSignature)
        btnTselRtpeNfSignature = findViewById(R.id.btnTselRtpeNfSignature)
        btnTelkomSignature = findViewById(R.id.btnTelkomSignature)
        btnTifSignature = findViewById(R.id.btnTifSignature)

        // Signature ImageViews
        imgZteSignature = findViewById(R.id.imgZteSignature)
        imgTselNopSignature = findViewById(R.id.imgTselNopSignature)
        imgTselRtpdsSignature = findViewById(R.id.imgTselRtpdsSignature)
        imgTselRtpeNfSignature = findViewById(R.id.imgTselRtpeNfSignature)
        imgTelkomSignature = findViewById(R.id.imgTelkomSignature)
        imgTifSignature = findViewById(R.id.imgTifSignature)

        // Photo Components
        try {
            photoButtons = Array(16) {
                findViewById(resources.getIdentifier("btnUploadPhoto${it+1}", "id", packageName))
            }
            photoImageViews = Array(16) {
                findViewById(resources.getIdentifier("imgPhoto${it+1}", "id", packageName))
            }
        } catch (e: Exception) {
            Log.e("BaSurveyMiniOltEdit", "Error: ${e.message}")
            photoButtons = emptyArray()
            photoImageViews = emptyArray()
        }

        photoLabels = arrayOf(
            "AKSES GERBANG", "NAME PLATE", "OUTDOOR", "SHELTER",
            "PONDASI", "GROUNDING BASBAR", "CATUAN POWER DC",
            "PROPOSED DUAL SOURCE POWER DC", "RECTIFIER", "PENGUKURAN CATUAN POWER AC",
            "POWER AC DI PANEL KWH EXCITING", "PORT OTB EXCITING", "CABINET METRO-E",
            "METRO-E", "ALTERNATIF DILUAR SITE", "DI AREA ODC"
        )

        // Action Buttons
        btnCancel = findViewById(R.id.btnCancel)
        btnUpdateForm = findViewById(R.id.btnUpdateForm)
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf)
    }

    private fun requestRequiredPermissions() {
        val permissions = ArrayList<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 999)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        if (!allGranted) {
            Toast.makeText(this, "Permissions required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDropdowns() {
        // Platform
        val platformOptions = listOf("PT. ZTE INDONESIA", "PT. Huawei Tech Investment")
        val platformAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, platformOptions)
        platformDropdown.setAdapter(platformAdapter)

        // PM/PBM
        val pmPbmOptions = listOf("PM", "PBM")
        val pmPbmAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, pmPbmOptions)

        rackDropdown.setAdapter(pmPbmAdapter)
        rectifierDropdown.setAdapter(pmPbmAdapter)
        dcPowerDropdown.setAdapter(pmPbmAdapter)
        batteryDropdown.setAdapter(pmPbmAdapter)
        mcbDropdown.setAdapter(pmPbmAdapter)
        groundingDropdown.setAdapter(pmPbmAdapter)
        indoorRoomDropdown.setAdapter(pmPbmAdapter)
        acPowerDropdown.setAdapter(pmPbmAdapter)
        uplinkDropdown.setAdapter(pmPbmAdapter)
        conduitDropdown.setAdapter(pmPbmAdapter)

        // Site Provider
        val siteProviderOptions = listOf(
            "DMT", "DMT - Bifurcation", "IBS", "PROTELINDO", "PT Centratama",
            "PT Gihon", "PT Quattro", "READY", "TELKOM", "TELKOMSEL", "TIF"
        )
        val siteProviderAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, siteProviderOptions)
        etSiteProvider.setAdapter(siteProviderAdapter)

        // Location
        loadValidSiteIds()
    }

    private fun loadValidSiteIds() {
        firestore.collection("projects").get()
            .addOnSuccessListener { documents ->
                validSiteIds.clear()
                for (doc in documents) {
                    val siteId = doc.getString("siteId")
                    if (!siteId.isNullOrEmpty() && !validSiteIds.contains(siteId)) {
                        validSiteIds.add(siteId)
                    }
                }
                validSiteIds.sort()
                val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, validSiteIds)
                etLocation.setAdapter(adapter)
            }
    }

    private fun setupButtonListeners() {
        btnBack.setOnClickListener { finish() }
        btnCancel.setOnClickListener { finish() }

        btnUpdateForm.setOnClickListener {
            if (validateForm()) {
                showConfirmationDialog()
            }
        }

        btnGeneratePdf.setOnClickListener {
            if (validateForm()) {
                generatePreviewPdf()
            }
        }

        // Signature buttons
        btnZteSignature.setOnClickListener { openSignatureActivity(REQUEST_SIGNATURE_ZTE) }
        btnTselNopSignature.setOnClickListener { openSignatureActivity(REQUEST_SIGNATURE_TSEL_NOP) }
        btnTselRtpdsSignature.setOnClickListener { openSignatureActivity(REQUEST_SIGNATURE_TSEL_RTPDS) }
        btnTselRtpeNfSignature.setOnClickListener { openSignatureActivity(REQUEST_SIGNATURE_TSEL_RTPE) }
        btnTelkomSignature.setOnClickListener { openSignatureActivity(REQUEST_SIGNATURE_TELKOM) }
        btnTifSignature.setOnClickListener { openSignatureActivity(REQUEST_SIGNATURE_TIF) }

        // Photo buttons
        for (i in photoButtons.indices) {
            photoButtons[i].setOnClickListener {
                currentPhotoIndex = i
                showPhotoSourceDialog()
            }
        }
    }

    private fun loadSurveyData() {
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        firestore.collection("ba_survey_mini_olt").document(surveyId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data ?: return@addOnSuccessListener

                    // Load basic info
                    etLocation.setText(data["location"] as? String ?: "")
                    etNoIhld.setText(data["noIhld"] as? String ?: "")
                    platformDropdown.setText(data["platform"] as? String ?: "", false)
                    etSiteProvider.setText(data["siteProvider"] as? String ?: "", false)
                    etContractNumber.setText(data["contractNumber"] as? String ?: "")
                    etHeaderNo.setText(data["headerNo"] as? String ?: "")
                    isSiteIdValid = true

                    // Load table results
                    val tableResults = data["tableResults"] as? Map<String, Any> ?: mapOf()

                    loadTableRow("rack", tableResults, rackDropdown, etRackResult, etRackProposed)
                    loadTableRow("rectifier", tableResults, rectifierDropdown, etRectifierResult, etRectifierProposed)
                    loadTableRow("dcPower", tableResults, dcPowerDropdown, etDcPowerResult, etDcPowerProposed)
                    loadTableRow("battery", tableResults, batteryDropdown, etBatteryResult, etBatteryProposed)
                    loadTableRow("mcb", tableResults, mcbDropdown, etMcbResult, etMcbProposed)
                    loadTableRow("grounding", tableResults, groundingDropdown, etGroundingResult, etGroundingProposed)
                    loadTableRow("indoorRoom", tableResults, indoorRoomDropdown, etIndoorRoomResult, etIndoorRoomProposed)
                    loadTableRow("acPower", tableResults, acPowerDropdown, etAcPowerResult, etAcPowerProposed)
                    loadTableRow("uplink", tableResults, uplinkDropdown, etUplinkResult, etUplinkProposed)
                    loadTableRow("conduit", tableResults, conduitDropdown, etConduitResult, etConduitProposed)

                    // Load signatures
                    val signaturesData = data["signatures"] as? Map<String, Any> ?: mapOf()

                    loadSignature("zte", signaturesData, etZteName, etZteNik, null, imgZteSignature)
                    loadSignature("tselNop", signaturesData, etTselNopName, etTselNopNik, etTselNopRegion, imgTselNopSignature)
                    loadSignature("tselRtpds", signaturesData, etTselRtpdsName, etTselRtpdsNik, etTselRtpdsRegion, imgTselRtpdsSignature)
                    loadSignature("tselRtpeNf", signaturesData, etTselRtpeNfName, etTselRtpeNfNik, etTselRtpeNfRegion, imgTselRtpeNfSignature)
                    loadSignature("telkom", signaturesData, etTelkomName, etTelkomNik, null, imgTelkomSignature)
                    loadSignature("tif", signaturesData, etTifName, etTifNik, null, imgTifSignature)

                    // Load photos & store old URLs
                    val photosData = data["photos"] as? Map<String, Any> ?: mapOf()
                    for (i in 0 until 16) {
                        val photoUrl = photosData["photo${i+1}"] as? String
                        if (!photoUrl.isNullOrEmpty()) {
                            oldPhotoUrls[i] = photoUrl
                            try {
                                com.bumptech.glide.Glide.with(this)
                                    .load(photoUrl)
                                    .into(photoImageViews[i])
                                photoImageViews[i].visibility = View.VISIBLE
                            } catch (e: Exception) {
                                Log.e("Load Photo", "Error: ${e.message}")
                            }
                        }
                    }

                    // Store old signature URLs
                    (signaturesData["zte"] as? Map<String, Any>)?.let {
                        val sigUrl = it["signatureUrl"] as? String
                        if (!sigUrl.isNullOrEmpty()) oldSignatureUrls["zte"] = sigUrl
                    }
                    (signaturesData["tselNop"] as? Map<String, Any>)?.let {
                        val sigUrl = it["signatureUrl"] as? String
                        if (!sigUrl.isNullOrEmpty()) oldSignatureUrls["tselNop"] = sigUrl
                    }
                    (signaturesData["tselRtpds"] as? Map<String, Any>)?.let {
                        val sigUrl = it["signatureUrl"] as? String
                        if (!sigUrl.isNullOrEmpty()) oldSignatureUrls["tselRtpds"] = sigUrl
                    }
                    (signaturesData["tselRtpeNf"] as? Map<String, Any>)?.let {
                        val sigUrl = it["signatureUrl"] as? String
                        if (!sigUrl.isNullOrEmpty()) oldSignatureUrls["tselRtpeNf"] = sigUrl
                    }
                    (signaturesData["telkom"] as? Map<String, Any>)?.let {
                        val sigUrl = it["signatureUrl"] as? String
                        if (!sigUrl.isNullOrEmpty()) oldSignatureUrls["telkom"] = sigUrl
                    }
                    (signaturesData["tif"] as? Map<String, Any>)?.let {
                        val sigUrl = it["signatureUrl"] as? String
                        if (!sigUrl.isNullOrEmpty()) oldSignatureUrls["tif"] = sigUrl
                    }
                }
                loadingDialog.dismiss()
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadTableRow(key: String, tableResults: Map<String, Any>,
                             dropdownView: AutoCompleteTextView,
                             resultView: EditText,
                             proposedView: EditText) {
        val rowData = tableResults[key] as? Map<String, Any> ?: return
        dropdownView.setText(rowData["responsibility"] as? String ?: "", false)
        resultView.setText(rowData["surveyResult"] as? String ?: "")
        proposedView.setText(rowData["proposed"] as? String ?: "")
    }

    private fun loadSignature(key: String, signaturesData: Map<String, Any>,
                              nameView: EditText, nikView: EditText,
                              regionView: EditText?, imageView: ImageView) {
        val sigData = signaturesData[key] as? Map<String, Any> ?: return
        nameView.setText(sigData["name"] as? String ?: "")
        nikView.setText(sigData["nik"] as? String ?: "")

        if (regionView != null) {
            regionView.setText(sigData["region"] as? String ?: "")
        }

        val signatureUrl = sigData["signatureUrl"] as? String
        if (signatureUrl != null) {
            try {
                com.bumptech.glide.Glide.with(this)
                    .load(signatureUrl)
                    .into(imageView)
                imageView.visibility = View.VISIBLE

                val signatureIndex = when(key) {
                    "zte" -> 0
                    "tselNop" -> 1
                    "tselRtpds" -> 2
                    "tselRtpeNf" -> 3
                    "telkom" -> 4
                    "tif" -> 5
                    else -> -1
                }

                if (signatureIndex >= 0) {
                    signatureUris[signatureIndex] = Uri.parse(signatureUrl)
                }
            } catch (e: Exception) {
                Log.e("LoadSignature", "Error: ${e.message}")
            }
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
                    0 -> openCamera()
                    1 -> openGallery()
                }
            }
            .show()
    }

    private fun openCamera() {
        val photoFile = try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "JPEG_${timeStamp}_"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(fileName, ".jpg", storageDir)
        } catch (e: IOException) {
            null
        }

        photoFile?.let {
            val photoURI = FileProvider.getUriForFile(this, "com.mbkm.telgo.fileprovider", it)
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

            currentPhotoUri = photoURI
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_GALLERY)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    try {
                        val uri = currentPhotoUri
                        if (uri != null && currentPhotoIndex < photoImageViews.size) {
                            photoUris[currentPhotoIndex] = uri

                            val options = BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                                inSampleSize = 1
                            }

                            contentResolver.openInputStream(uri)?.use { input ->
                                val bitmap = BitmapFactory.decodeStream(input, null, options)
                                if (bitmap != null) {
                                    photoImageViews[currentPhotoIndex].setImageBitmap(bitmap)
                                    photoImageViews[currentPhotoIndex].visibility = View.VISIBLE
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Camera", "Error: ${e.message}")
                    }
                }
                REQUEST_GALLERY -> {
                    try {
                        val uri = data?.data
                        if (uri != null && currentPhotoIndex < photoImageViews.size) {
                            photoUris[currentPhotoIndex] = uri

                            contentResolver.openInputStream(uri)?.use { input ->
                                val bitmap = BitmapFactory.decodeStream(input)
                                if (bitmap != null) {
                                    photoImageViews[currentPhotoIndex].setImageBitmap(bitmap)
                                    photoImageViews[currentPhotoIndex].visibility = View.VISIBLE
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Gallery", "Error: ${e.message}")
                    }
                }
                REQUEST_SIGNATURE_ZTE,
                REQUEST_SIGNATURE_TSEL_NOP,
                REQUEST_SIGNATURE_TSEL_RTPDS,
                REQUEST_SIGNATURE_TSEL_RTPE,
                REQUEST_SIGNATURE_TELKOM,
                REQUEST_SIGNATURE_TIF -> {
                    val uri = data?.getParcelableExtra<Uri>("signature_uri")
                    if (uri != null) {
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
                            signatureUris[signatureIndex] = uri

                            val imageView = when (signatureIndex) {
                                0 -> imgZteSignature
                                1 -> imgTselNopSignature
                                2 -> imgTselRtpdsSignature
                                3 -> imgTselRtpeNfSignature
                                4 -> imgTelkomSignature
                                5 -> imgTifSignature
                                else -> null
                            }

                            imageView?.setImageURI(uri)
                            imageView?.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }

    private fun validateForm(): Boolean {
        if (etLocation.text.toString().trim().isEmpty()) {
            etLocation.error = "Lokasi wajib diisi"
            return false
        }

        if (etNoIhld.text.toString().isEmpty()) {
            etNoIhld.error = "Required"
            return false
        }

        if (platformDropdown.text.toString().isEmpty()) {
            platformDropdown.error = "Required"
            return false
        }

        return true
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Update Survey")
            .setMessage("Apakah Anda yakin ingin mengupdate survey, photos, signatures, dan PDF?")
            .setPositiveButton("Update") { _, _ -> updateSurvey() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateSurvey() {
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        val surveyData = HashMap<String, Any>()

        // Basic info
        surveyData["location"] = etLocation.text.toString().trim()
        surveyData["noIhld"] = etNoIhld.text.toString().trim()
        surveyData["platform"] = platformDropdown.text.toString().trim()
        surveyData["siteProvider"] = etSiteProvider.text.toString().trim()
        surveyData["contractNumber"] = etContractNumber.text.toString().trim()
        surveyData["headerNo"] = etHeaderNo.text.toString().trim()
        surveyData["updatedAt"] = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        surveyData["updatedBy"] = auth.currentUser?.email ?: "unknown"

        // Table results
        val tableResults = HashMap<String, HashMap<String, String>>()

        tableResults["rack"] = hashMapOf(
            "responsibility" to rackDropdown.text.toString(),
            "surveyResult" to etRackResult.text.toString(),
            "proposed" to etRackProposed.text.toString()
        )

        tableResults["rectifier"] = hashMapOf(
            "responsibility" to rectifierDropdown.text.toString(),
            "surveyResult" to etRectifierResult.text.toString(),
            "proposed" to etRectifierProposed.text.toString()
        )

        tableResults["dcPower"] = hashMapOf(
            "responsibility" to dcPowerDropdown.text.toString(),
            "surveyResult" to etDcPowerResult.text.toString(),
            "proposed" to etDcPowerProposed.text.toString()
        )

        tableResults["battery"] = hashMapOf(
            "responsibility" to batteryDropdown.text.toString(),
            "surveyResult" to etBatteryResult.text.toString(),
            "proposed" to etBatteryProposed.text.toString()
        )

        tableResults["mcb"] = hashMapOf(
            "responsibility" to mcbDropdown.text.toString(),
            "surveyResult" to etMcbResult.text.toString(),
            "proposed" to etMcbProposed.text.toString()
        )

        tableResults["grounding"] = hashMapOf(
            "responsibility" to groundingDropdown.text.toString(),
            "surveyResult" to etGroundingResult.text.toString(),
            "proposed" to etGroundingProposed.text.toString()
        )

        tableResults["indoorRoom"] = hashMapOf(
            "responsibility" to indoorRoomDropdown.text.toString(),
            "surveyResult" to etIndoorRoomResult.text.toString(),
            "proposed" to etIndoorRoomProposed.text.toString()
        )

        tableResults["acPower"] = hashMapOf(
            "responsibility" to acPowerDropdown.text.toString(),
            "surveyResult" to etAcPowerResult.text.toString(),
            "proposed" to etAcPowerProposed.text.toString()
        )

        tableResults["uplink"] = hashMapOf(
            "responsibility" to uplinkDropdown.text.toString(),
            "surveyResult" to etUplinkResult.text.toString(),
            "proposed" to etUplinkProposed.text.toString()
        )

        tableResults["conduit"] = hashMapOf(
            "responsibility" to conduitDropdown.text.toString(),
            "surveyResult" to etConduitResult.text.toString(),
            "proposed" to etConduitProposed.text.toString()
        )

        surveyData["tableResults"] = tableResults

        // Signatures
        val signaturesData = HashMap<String, HashMap<String, String>>()

        signaturesData["zte"] = hashMapOf(
            "name" to etZteName.text.toString(),
            "nik" to etZteNik.text.toString(),
            "role" to "TIM SURVEY",
            "company" to platformDropdown.text.toString()
        )

        signaturesData["tselNop"] = hashMapOf(
            "name" to etTselNopName.text.toString(),
            "nik" to etTselNopNik.text.toString(),
            "role" to "MGR NOP",
            "company" to "PT. TELKOMSEL",
            "region" to etTselNopRegion.text.toString()
        )

        signaturesData["tselRtpds"] = hashMapOf(
            "name" to etTselRtpdsName.text.toString(),
            "nik" to etTselRtpdsNik.text.toString(),
            "role" to "MGR RTPDS",
            "company" to "PT. TELKOMSEL",
            "region" to etTselRtpdsRegion.text.toString()
        )

        signaturesData["tselRtpeNf"] = hashMapOf(
            "name" to etTselRtpeNfName.text.toString(),
            "nik" to etTselRtpeNfNik.text.toString(),
            "role" to "MGR RTPE",
            "company" to "PT. TELKOMSEL",
            "region" to etTselRtpeNfRegion.text.toString()
        )

        signaturesData["telkom"] = hashMapOf(
            "name" to etTelkomName.text.toString(),
            "nik" to etTelkomNik.text.toString(),
            "role" to "MGR NDPS TR1",
            "company" to "PT. TELKOM"
        )

        signaturesData["tif"] = hashMapOf(
            "name" to etTifName.text.toString(),
            "nik" to etTifNik.text.toString(),
            "role" to "TIM SURVEY",
            "company" to "PT. TIF"
        )

        surveyData["signatures"] = signaturesData

        // Update Firestore
        firestore.collection("ba_survey_mini_olt").document(surveyId)
            .update(surveyData)
            .addOnSuccessListener {
                Log.d("Update", "Firestore data updated successfully")
                // Upload updated images
                uploadUpdatedImages {
                    // Generate and upload PDF
                    generateAndUploadPdfUpdate {
                        loadingDialog.dismiss()
                        Toast.makeText(this, "✅ Survey Updated & PDF Generated!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Update", "Error: ${e.message}")
            }
    }

    private fun uploadUpdatedImages(callback: () -> Unit) {
        var uploadCount = 0
        val totalUploads = photoUris.size + signatureUris.size

        Log.d("Upload", "Starting upload of $totalUploads items")

        if (totalUploads == 0) {
            Log.d("Upload", "No items to upload")
            callback()
            return
        }

        // Upload photos
        for ((index, uri) in photoUris) {
            val photoRef = storage.reference.child("ba_survey_mini_olt/$surveyId/photos/photo${index+1}.jpg")
            uploadImageFromUri(uri, photoRef) { success, downloadUrl ->
                if (success && downloadUrl != null) {
                    firestore.collection("ba_survey_mini_olt").document(surveyId)
                        .update("photos.photo${index+1}", downloadUrl)
                        .addOnCompleteListener {
                            uploadCount++
                            Log.d("Upload", "Photo ${index+1} uploaded. Progress: $uploadCount/$totalUploads")
                            if (uploadCount == totalUploads) {
                                Log.d("Upload", "All images uploaded!")
                                callback()
                            }

                            // Cleanup old photo
                            oldPhotoUrls[index]?.let { oldUrl ->
                                try {
                                    storage.getReferenceFromUrl(oldUrl).delete()
                                } catch (e: Exception) {
                                    Log.e("Cleanup", "Error deleting old photo: ${e.message}")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            uploadCount++
                            Log.e("Upload", "Error updating photo URL: ${e.message}")
                            if (uploadCount == totalUploads) callback()
                        }
                } else {
                    uploadCount++
                    Log.e("Upload", "Failed to upload photo ${index+1}")
                    if (uploadCount == totalUploads) callback()
                }
            }
        }

        // Upload signatures
        for ((index, uri) in signatureUris) {
            val signatureRef = storage.reference.child("ba_survey_mini_olt/$surveyId/signatures/${signatureKeys[index]}.png")
            uploadImageFromUri(uri, signatureRef) { success, downloadUrl ->
                if (success && downloadUrl != null) {
                    firestore.collection("ba_survey_mini_olt").document(surveyId)
                        .update("signatures.${signatureKeys[index]}.signatureUrl", downloadUrl)
                        .addOnCompleteListener {
                            uploadCount++
                            Log.d("Upload", "Signature ${signatureKeys[index]} uploaded. Progress: $uploadCount/$totalUploads")
                            if (uploadCount == totalUploads) {
                                Log.d("Upload", "All images uploaded!")
                                callback()
                            }

                            // Cleanup old signature
                            oldSignatureUrls[signatureKeys[index]]?.let { oldUrl ->
                                try {
                                    storage.getReferenceFromUrl(oldUrl).delete()
                                } catch (e: Exception) {
                                    Log.e("Cleanup", "Error deleting old signature: ${e.message}")
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            uploadCount++
                            Log.e("Upload", "Error updating signature URL: ${e.message}")
                            if (uploadCount == totalUploads) callback()
                        }
                } else {
                    uploadCount++
                    Log.e("Upload", "Failed to upload signature ${signatureKeys[index]}")
                    if (uploadCount == totalUploads) callback()
                }
            }
        }
    }

    private fun uploadImageFromUri(uri: Uri, storageRef: com.google.firebase.storage.StorageReference,
                                   callback: (Boolean, String?) -> Unit) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes = inputStream.readBytes()
                val uploadTask = storageRef.putBytes(bytes)
                uploadTask
                    .continueWithTask { task ->
                        if (!task.isSuccessful) {
                            throw task.exception ?: Exception("Unknown error")
                        }
                        storageRef.downloadUrl
                    }
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            callback(true, task.result.toString())
                        } else {
                            callback(false, null)
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("Upload", "Error: ${e.message}")
            callback(false, null)
        }
    }

    private fun generateAndUploadPdfUpdate(callback: () -> Unit) {
        try {
            val pdfFile = generatePdfContentMiniOlt()

            // Upload PDF to Firebase Storage
            val pdfRef = storage.reference.child("ba_survey_mini_olt_pdf/$surveyId.pdf")

            pdfRef.putFile(Uri.fromFile(pdfFile))
                .addOnSuccessListener {
                    pdfRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Update document with new PDF URL
                        firestore.collection("ba_survey_mini_olt").document(surveyId)
                            .update(
                                "pdfUrl", downloadUrl.toString(),
                                "pdfUpdatedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            )
                            .addOnSuccessListener {
                                Log.d("PDF", "PDF updated successfully")
                                callback()
                            }
                            .addOnFailureListener { e ->
                                Log.e("PDF Update", "Error updating PDF URL: ${e.message}")
                                callback()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("PDF Upload", "Error uploading PDF: ${e.message}")
                    callback()
                }
        } catch (e: Exception) {
            Log.e("PDF Generation", "Error: ${e.message}")
            callback()
        }
    }

    private fun generatePdfContentMiniOlt(): File {
        val document = PdfDocument()

        // PAGE 1: Form content with complete header
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

        // PAGE 2: Signatures (without header table, only title)
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
        if (photoImageViews.any { it.drawable != null }) {
            val photosPerPage = 4 // 4 foto per halaman
            val photoPages = (photoImageViews.count { it.drawable != null } + photosPerPage - 1) / photosPerPage

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

        // TSEL NOP dengan region spesifik
        drawSignatureBox(canvas, paint,
            leftMargin, yPosition,
            "PT. TELKOMSEL", "MGR NOP\n${etTselNopRegion.text}", // Tambahkan region NOP
            etTselNopName.text.toString(), boxWidth, boxHeight,
            imgTselNopSignature,
            "NIK. " + etTselNopNik.text.toString()) // NIK untuk TSEL NOP

        // TSEL RTPDS dengan region spesifik
        drawSignatureBox(canvas, paint,
            leftMargin + boxWidth + horizontalGap, yPosition,
            "PT. TELKOMSEL", "MGR RTPDS\n${etTselRtpdsRegion.text}", // Tambahkan region RTPDS
            etTselRtpdsName.text.toString(), boxWidth, boxHeight,
            imgTselRtpdsSignature,
            "NIK. " + etTselRtpdsNik.text.toString()) // NIK untuk TSEL RTPDS

        // TSEL RTPE/NF dengan region spesifik
        drawSignatureBox(canvas, paint,
            leftMargin + (boxWidth + horizontalGap) * 2, yPosition,
            "PT. TELKOMSEL", "MGR RTPE\n${etTselRtpeNfRegion.text}", // Tambahkan region RTPE/NF
            etTselRtpeNfName.text.toString(), boxWidth, boxHeight,
            imgTselRtpeNfSignature,
            "NIK. " + etTselRtpeNfNik.text.toString()) // NIK untuk TSEL RTPE/NF
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

        // Dimensi untuk layout grid 2x2
        val availableWidth = rightMargin - leftMargin
        val columnWidth = availableWidth / 2 - 10f
        val photoHeight = 230f

        // Process photos for this page
        var photoIndexOnPage = 0
        for (i in photoImageViews.indices) {
            if (photoImageViews[i].drawable == null) continue

            // Calculate position in page grid
            val column = photoIndexOnPage % 2
            val row = photoIndexOnPage / 2

            // Calculate position coordinates
            val xPos = leftMargin + column * (columnWidth + 20f)
            val yPos = yPosition + row * (photoHeight + 70f)

            // Draw label
            paint.textSize = 11f
            paint.isFakeBoldText = true
            canvas.drawText("${i + 1}. ${photoLabels[i]}", xPos, yPos, paint)
            paint.isFakeBoldText = false

            // Get and display photo
            try {
                val drawable = photoImageViews[i].drawable
                if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
                    val bitmap = drawable.bitmap
                    val originalRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

                    val displayWidth: Float
                    val displayHeight: Float
                    if (originalRatio > 1) {
                        displayWidth = columnWidth.coerceAtMost(bitmap.width.toFloat())
                        displayHeight = displayWidth / originalRatio
                    } else {
                        displayHeight = (photoHeight - 40f).coerceAtMost(bitmap.height.toFloat())
                        displayWidth = displayHeight * originalRatio
                    }

                    val xOffset = xPos + (columnWidth - displayWidth) / 2
                    val yOffset = yPos + 20f

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

                    canvas.drawBitmap(bitmap, null, displayRect, renderPaint)

                    // Add border
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    paint.color = Color.BLACK
                    canvas.drawRect(displayRect, paint)
                    paint.style = Paint.Style.FILL
                }
            } catch (e: Exception) {
                Log.e("PDF", "Error drawing photo: ${e.message}")
            }

            photoIndexOnPage++
            if (photoIndexOnPage >= photosPerPage) break
        }
    }

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

    private fun generatePreviewPdf() {
        try {
            val pdfFile = generatePdfContentMiniOlt()

            val uri = FileProvider.getUriForFile(this, "com.mbkm.telgo.fileprovider", pdfFile)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(Intent.createChooser(intent, "Open PDF"))
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("PDF", "Error: ${e.message}")
        }
    }
}