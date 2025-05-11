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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import android.widget.Button
import java.io.FileNotFoundException
import kotlin.collections.HashMap

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

        inputProjectTitle = findViewById(R.id.inputProjectTitle)
        inputContractNumber = findViewById(R.id.inputContractNumber)
        inputExecutor = findViewById(R.id.inputExecutor)
        inputLocation = findViewById(R.id.inputLocation)
        inputDescription = findViewById(R.id.inputDescription)
        etTselRegion = findViewById(R.id.etTselRegion)

        btnGeneratePdf = findViewById(R.id.btnGeneratePdf)
        btnSubmitForm = findViewById(R.id.btnSubmitForm)  // Tambahkan baris ini

        // Di dalam initializeUI() atau bagian setupButtons()
        btnSubmitForm.setOnClickListener {
            if (validateForm()) {
                submitForm()
            }
        }

        // Setup signature button listeners
        setupSignatureButtons()

        // Setup generate PDF and submit form buttons
        btnGeneratePdf.setOnClickListener {
            if (validateForm()) {
                CoroutineScope(Dispatchers.IO).launch {
                    val formId = UUID.randomUUID().toString() // Buat formId unik
                    generateStyledPdf() // Panggil fungsi dengan parameter formId
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
                when(tab?.position) {
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
                        createdAt = data["createdAt"] as? Long ?: 0
                    )
                    surveyList.add(survey)
                }

                // Sort by creation date, newest first
                surveyList.sortByDescending { it.createdAt }

                // Update UI
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
        // Show loading indicator
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Download PDF from Firebase Storage
        val pdfRef = storage.reference.child("ba_survey_big_olt_pdf/${baSurvey.id}.pdf")

        val localFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "BA_Survey_Big_OLT_${baSurvey.location}.pdf")

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

    private fun takePhoto() {
        if (checkCameraPermission()) {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")

            tempPhotoUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempPhotoUri)
            startActivityForResult(cameraIntent, CAMERA_PERMISSION_CODE)
        } else {
            requestCameraPermission()
        }
    }

    private fun chooseFromGallery() {
        if (checkStoragePermission()) {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent, STORAGE_PERMISSION_CODE)
        } else {
            requestStoragePermission()
        }
    }

    private fun drawSignature() {
        val signatureDialog = SignatureDialog(this) { bitmap ->
            currentImageView?.setImageBitmap(bitmap)
            currentImageView?.visibility = View.VISIBLE
        }
        signatureDialog.show()
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
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
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
                            val pdfFile = generateStyledPdf()
                            if (!pdfFile.exists() || !pdfFile.canRead()) {
                                throw FileNotFoundException("File PDF tidak berhasil dibuat.")
                            }

                            val pdfPath = "ba_survey_big_olt_pdf/$formId.pdf"
                            val pdfStorageRef = storage.reference.child(pdfPath)
                            pdfStorageRef.putFile(Uri.fromFile(pdfFile)).await()
                            val pdfDownloadUrl = pdfStorageRef.downloadUrl.await().toString()
                            formData["pdfUrl"] = pdfDownloadUrl

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
        val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
        return "Pada hari ini, $currentDate, telah dilakukan survey bersama terhadap pekerjaan \"$projectTitle\" " +
                "yang dilaksanakan oleh $executor yang terikat Perjanjian Pemborongan \"$contractNumber\" " +
                "dengan hasil sebagai berikut:"
    }

    private suspend fun generateStyledPdf(): File {
        var createdFile: File? = null // Ubah ke 'var'

        withContext(Dispatchers.IO) {
            try {
                // Ambil input dari pengguna
                val projectTitle = findViewById<EditText>(R.id.inputProjectTitle).text.toString()
                val contractNumber =
                    findViewById<EditText>(R.id.inputContractNumber).text.toString()
                val executor = findViewById<Spinner>(R.id.inputExecutor).selectedItem.toString()
                val location = findViewById<EditText>(R.id.inputLocation).text.toString()
                // Hasil deskripsi otomatis
                val description = generateDescription(projectTitle, contractNumber, executor)

                // Tampilkan deskripsi otomatis di field deskripsi
                findViewById<EditText>(R.id.inputDescription).setText(description)

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
                val marginBottom = 50f
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

                // Header halaman
                // Header halaman dengan logo dinamis
                fun drawHeader(executor: String) {
                    val centerX = (marginX + maxX) / 2

                    // Tambahkan logo berdasarkan pelaksana
                    val zteLogo =
                        BitmapFactory.decodeResource(resources, R.drawable.logo_zte) // Logo ZTE
                    val huaweiLogo = BitmapFactory.decodeResource(
                        resources,
                        R.drawable.logo_huawei
                    ) // Logo Huawei
                    val telkomLogo = BitmapFactory.decodeResource(
                        resources,
                        R.drawable.logo_telkom
                    ) // Logo Telkom

                    // Ukuran logo
                    val logoWidth = 80 // Lebar logo
                    val logoHeight = 50 // Tinggi logo
                    val topMargin = marginTop // Margin atas untuk logo

                    // Gambar logo pelaksana di pojok kiri atas
                    when (executor) {
                        "PT. ZTE INDONESIA" -> {
                            val scaledZteLogo =
                                Bitmap.createScaledBitmap(zteLogo, logoWidth, logoHeight, false)
                            canvas.drawBitmap(scaledZteLogo, marginX, topMargin, null)
                        }

                        "PT Huawei Tech Investment" -> {
                            val scaledHuaweiLogo =
                                Bitmap.createScaledBitmap(huaweiLogo, logoWidth, logoHeight, false)
                            canvas.drawBitmap(scaledHuaweiLogo, marginX, topMargin, null)
                        }
                    }

                    // Gambar logo Telkom di pojok kanan atas
                    val scaledTelkomLogo =
                        Bitmap.createScaledBitmap(telkomLogo, logoWidth, logoHeight, false)
                    canvas.drawBitmap(scaledTelkomLogo, maxX - logoWidth - marginX, topMargin, null)

                    // Tambahkan jarak di bawah logo
                    val logoBottomY = topMargin + logoHeight + 20f

                    // Teks header
                    canvas.drawText("BERITA ACARA", centerX, logoBottomY, titlePaint)
                    canvas.drawText("SURVEY LOKASI", centerX, logoBottomY + 20f, titlePaint)
                    canvas.drawLine(marginX, logoBottomY + 30f, maxX, logoBottomY + 30f, paint)
                    y = logoBottomY + 40f // Perbarui posisi vertikal
                }

                // Footer halaman
                // Footer halaman
                fun drawFooter() {
                    // Atur ukuran teks lebih kecil
                    paint.textSize = 10f // Ukuran teks lebih kecil

                    // Garis pembatas
                    paint.style = Paint.Style.STROKE
                    canvas.drawLine(
                        marginX, // Garis mulai dari margin kiri
                        pageHeight - 30f, // Posisi Y untuk garis (di atas teks dokumen)
                        pageWidth - marginX, // Garis berakhir di margin kanan
                        pageHeight - 30f,
                        paint
                    )

                    // Tulisan dokumen
                    paint.style = Paint.Style.FILL
                    canvas.drawText(
                        "Dokumen ini telah ditandatangani secara elektronik dan merupakan dokumen sah sesuai ketentuan yang berlaku",
                        marginX, // Tulisan dimulai dari margin kiri
                        pageHeight - 20f, // Posisi Y di bawah garis dan di atas halaman
                        paint
                    )

                    // Halaman di pojok kanan bawah
                    val pageText = "Halaman ${pageCount - 1}"
                    val textWidth = paint.measureText(pageText)
                    canvas.drawText(
                        pageText,
                        pageWidth - marginX - textWidth, // Posisi X di pojok kanan bawah
                        pageHeight - 20f, // Posisi Y sejajar dengan teks dokumen
                        paint
                    )
                }


                // Tambahkan teks di bawah tabel halaman terakhir
                fun drawClosingStatement() {
                    val closingText =
                        "Demikian Berita Acara Hasil Survey ini dibuat berdasarkan kenyataan di lapangan untuk dijadikan pedoman pelaksanaan selanjutnya."
                    val closingMaxWidth = maxX - marginX * 2
                    val closingLines = wrapText(closingText, closingMaxWidth, paint)

                    // Format tanggal hari ini
                    val currentDate =
                        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())

                    // Tinggi minimum untuk menambahkan teks
                    val closingHeight =
                        18f * closingLines.size + 10f + 20f // Tambahkan ruang untuk tanggal
                    if (y + closingHeight > pageHeight - marginBottom) {
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
                    boldPaint: Paint
                ) {
                    val marginX = 50f
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
                    val tselNopSignature =
                        findViewById<ImageView>(R.id.imgTselNopSignature).drawable

                    val tselRtpdsName = findViewById<EditText>(R.id.etTselRtpdsName).text.toString()
                    val tselRtpdsNik = findViewById<EditText>(R.id.etTselRtpdsNik).text.toString()
                    val tselRtpdsSignature =
                        findViewById<ImageView>(R.id.imgTselRtpdsSignature).drawable

                    val tselRtpeName = findViewById<EditText>(R.id.etTselRtpeNfName).text.toString()
                    val tselRtpeNik = findViewById<EditText>(R.id.etTselRtpeNfNik).text.toString()
                    val tselRtpeSignature =
                        findViewById<ImageView>(R.id.imgTselRtpeNfSignature).drawable

                    // Baris pertama (ZTE, TIF, TELKOM)
                    drawSignatureBox(
                        listOf("PT. ZTE INDONESIA", "TIM SURVEY"),
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
                val descLines = wrapText(description, descMaxWidth, paint)
                for (line in descLines) {
                    canvas.drawText(line, marginX, y, paint)
                    y += 18f
                }

                y += 10f // Jarak sebelum tabel

                // Tabel
                val rowHeight = 40f
                val colX = floatArrayOf(marginX, 90f, 250f, 360f, 430f, maxX)

                // Header tabel
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


                // Baris 1
                val itemMaxWidth1 = colX[2] - colX[1] - 10f // Lebar maksimum untuk kolom ITEM
                val remarkMaxWidth1 =
                    colX[5] - colX[4] - 10f // Lebar maksimum untuk kolom KETERANGAN

                val itemLines1 = wrapText(
                    "Propose OLT",
                    itemMaxWidth1,
                    cellPaint
                ) // Text wrapping untuk kolom ITEM
                val remarkLines1 = wrapText(
                    remark1,
                    remarkMaxWidth1,
                    cellPaint
                ) // Text wrapping untuk kolom KETERANGAN

// Hitung tinggi baris secara dinamis berdasarkan jumlah baris di kolom ITEM dan KETERANGAN
                val dynamicRowHeight1 = maxOf(
                    40f, // Tinggi minimum baris
                    20f + (itemLines1.size * 15f), // Tinggi berdasarkan jumlah baris di kolom ITEM
                    20f + (remarkLines1.size * 15f) // Tinggi berdasarkan jumlah baris di kolom KETERANGAN
                )

// Cek apakah baris ini muat di halaman saat ini
                if (y + dynamicRowHeight1 > pageHeight - marginBottom) {
                    drawFooter() // Tambahkan footer sebelum berpindah halaman
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

// Gambarkan tabel untuk baris 1
                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight1, tablePaint)
                canvas.drawText("1", colX[0] + 15f, y + 20f, cellPaint)

// Gambarkan teks di kolom ITEM
                var itemY1 = y + 18f
                for (line in itemLines1) {
                    canvas.drawText(line, colX[1] + 5f, itemY1, cellPaint)
                    itemY1 += 15f
                }

// Gambarkan teks di kolom SATUAN dan AKTUAL
                canvas.drawText("OK/NOK", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual1, colX[3] + 10f, y + 20f, cellPaint)

// Gambarkan teks di kolom KETERANGAN
                var remarkY1 = y + 18f
                for (line in remarkLines1) {
                    canvas.drawText(line, colX[4] + 5f, remarkY1, cellPaint)
                    remarkY1 += 15f
                }

// Tambahkan garis vertikal untuk memisahkan kolom
                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight1, tablePaint)
                }

// Perbarui posisi vertikal untuk baris berikutnya
                y += dynamicRowHeight1

                // Baris 2
                val itemMaxWidth2 = colX[2] - colX[1] - 10f // Lebar maksimum untuk kolom ITEM
                val remarkMaxWidth2 =
                    colX[5] - colX[4] - 10f // Lebar maksimum untuk kolom KETERANGAN

                val itemLines2 = wrapText(
                    "Panjang Bundlecore Uplink (Dari Metro ke FTM-Rack ET)",
                    itemMaxWidth2,
                    cellPaint
                ) // Text wrapping untuk kolom ITEM
                val remarkLines2 = wrapText(
                    remark2,
                    remarkMaxWidth2,
                    cellPaint
                ) // Text wrapping untuk kolom KETERANGAN

// Hitung tinggi baris secara dinamis berdasarkan jumlah baris di kolom ITEM dan KETERANGAN
                val dynamicRowHeight2 = maxOf(
                    40f, // Tinggi minimum baris
                    20f + (itemLines2.size * 15f), // Tinggi berdasarkan jumlah baris di kolom ITEM
                    20f + (remarkLines2.size * 15f) // Tinggi berdasarkan jumlah baris di kolom KETERANGAN
                )

// Cek apakah baris ini muat di halaman saat ini
                if (y + dynamicRowHeight2 > pageHeight - marginBottom) {
                    drawFooter() // Tambahkan footer sebelum berpindah halaman
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

// Gambarkan tabel untuk baris 2
                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight2, tablePaint)
                canvas.drawText("2", colX[0] + 15f, y + 20f, cellPaint)

// Gambarkan teks di kolom ITEM
                var itemY2 = y + 18f
                for (line in itemLines2) {
                    canvas.drawText(line, colX[1] + 5f, itemY2, cellPaint)
                    itemY2 += 15f
                }

// Gambarkan teks di kolom SATUAN dan AKTUAL
                canvas.drawText("Meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual2, colX[3] + 10f, y + 20f, cellPaint)

// Gambarkan teks di kolom KETERANGAN
                var remarkY2 = y + 18f
                for (line in remarkLines2) {
                    canvas.drawText(line, colX[4] + 5f, remarkY2, cellPaint)
                    remarkY2 += 15f
                }

// Tambahkan garis vertikal untuk memisahkan kolom
                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight2, tablePaint)
                }

// Perbarui posisi vertikal untuk baris berikutnya
                y += dynamicRowHeight2

                // Baris 3
                val itemMaxWidth3 = colX[2] - colX[1] - 10f // Lebar maksimum untuk kolom ITEM
                val remarkMaxWidth3 =
                    colX[5] - colX[4] - 10f // Lebar maksimum untuk kolom KETERANGAN

                val itemLines3 = wrapText(
                    "Panjang Bundlecore Uplink (Dari Rack ET ke OLT)",
                    itemMaxWidth3,
                    cellPaint
                ) // Text wrapping untuk kolom ITEM
                val remarkLines3 = wrapText(
                    remark3,
                    remarkMaxWidth3,
                    cellPaint
                ) // Text wrapping untuk kolom KETERANGAN

// Hitung tinggi baris secara dinamis berdasarkan jumlah baris di kolom ITEM dan KETERANGAN
                val dynamicRowHeight3 = maxOf(
                    40f, // Tinggi minimum baris
                    20f + (itemLines3.size * 15f), // Tinggi berdasarkan jumlah baris di kolom ITEM
                    20f + (remarkLines3.size * 15f) // Tinggi berdasarkan jumlah baris di kolom KETERANGAN
                )

// Cek apakah baris ini muat di halaman saat ini
                if (y + dynamicRowHeight3 > pageHeight - marginBottom) {
                    drawFooter() // Tambahkan footer sebelum berpindah halaman
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

// Gambarkan tabel untuk baris 3
                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight3, tablePaint)
                canvas.drawText("3", colX[0] + 15f, y + 20f, cellPaint)

// Gambarkan teks di kolom ITEM
                var itemY3 = y + 18f
                for (line in itemLines3) {
                    canvas.drawText(line, colX[1] + 5f, itemY3, cellPaint)
                    itemY3 += 15f
                }

// Gambarkan teks di kolom SATUAN dan AKTUAL
                canvas.drawText("Meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual3, colX[3] + 10f, y + 20f, cellPaint)

// Gambarkan teks di kolom KETERANGAN
                var remarkY3 = y + 18f
                for (line in remarkLines3) {
                    canvas.drawText(line, colX[4] + 5f, remarkY3, cellPaint)
                    remarkY3 += 15f
                }

// Tambahkan garis vertikal untuk memisahkan kolom
                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight3, tablePaint)
                }

// Perbarui posisi vertikal untuk baris berikutnya
                y += dynamicRowHeight3

                // Baris 4
                val itemMaxWidth4 = colX[2] - colX[1] - 10f // Lebar maksimum untuk kolom ITEM
                val remarkMaxWidth4 =
                    colX[5] - colX[4] - 10f // Lebar maksimum untuk kolom KETERANGAN

                val itemLines4 = wrapText(
                    "Panjang Bundlecore Downlink (Dari Rack EA ke OLT)",
                    itemMaxWidth4,
                    cellPaint
                ) // Text wrapping untuk kolom ITEM
                val remarkLines4 = wrapText(
                    remark4,
                    remarkMaxWidth4,
                    cellPaint
                ) // Text wrapping untuk kolom KETERANGAN

// Hitung tinggi baris secara dinamis berdasarkan jumlah baris di kolom ITEM dan KETERANGAN
                val dynamicRowHeight4 = maxOf(
                    40f, // Tinggi minimum baris
                    20f + (itemLines4.size * 15f), // Tinggi berdasarkan jumlah baris di kolom ITEM
                    20f + (remarkLines4.size * 15f) // Tinggi berdasarkan jumlah baris di kolom KETERANGAN
                )

// Cek apakah baris ini muat di halaman saat ini
                if (y + dynamicRowHeight4 > pageHeight - marginBottom) {
                    drawFooter() // Tambahkan footer sebelum berpindah halaman
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

// Gambarkan tabel untuk baris 4
                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight4, tablePaint)
                canvas.drawText("4", colX[0] + 15f, y + 20f, cellPaint)

// Gambarkan teks di kolom ITEM
                var itemY4 = y + 18f
                for (line in itemLines4) {
                    canvas.drawText(line, colX[1] + 5f, itemY4, cellPaint)
                    itemY4 += 15f
                }

// Gambarkan teks di kolom SATUAN dan AKTUAL
                canvas.drawText("Meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual4, colX[3] + 10f, y + 20f, cellPaint)

// Gambarkan teks di kolom KETERANGAN
                var remarkY4 = y + 18f
                for (line in remarkLines4) {
                    canvas.drawText(line, colX[4] + 5f, remarkY4, cellPaint)
                    remarkY4 += 15f
                }

// Tambahkan garis vertikal untuk memisahkan kolom
                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight4, tablePaint)
                }

// Perbarui posisi vertikal untuk baris berikutnya
                y += dynamicRowHeight4

                // Baris 5
                val itemMaxWidth5 = colX[2] - colX[1] - 10f // Lebar maksimum untuk kolom ITEM
                val remarkMaxWidth5 =
                    colX[5] - colX[4] - 10f // Lebar maksimum untuk kolom KETERANGAN

                val itemLines5 = wrapText(
                    "Pengecekan ground bar ruangan dan Panjang kabel grounding ke ground bar ruangan(kabel-25mm)",
                    itemMaxWidth5,
                    cellPaint
                ) // Text wrapping untuk kolom ITEM
                val remarkLines5 = wrapText(
                    remark5,
                    remarkMaxWidth5,
                    cellPaint
                ) // Text wrapping untuk kolom KETERANGAN

                val dynamicRowHeight5 = maxOf(
                    40f,
                    20f + (itemLines5.size * 15f),
                    20f + (remarkLines5.size * 15f)
                )

                if (y + dynamicRowHeight5 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight5, tablePaint)
                canvas.drawText("5", colX[0] + 15f, y + 20f, cellPaint)

                var itemY5 = y + 18f
                for (line in itemLines5) {
                    canvas.drawText(line, colX[1] + 5f, itemY5, cellPaint)
                    itemY5 += 15f
                }

                canvas.drawText("meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual5, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY5 = y + 18f
                for (line in remarkLines5) {
                    canvas.drawText(line, colX[4] + 5f, remarkY5, cellPaint)
                    remarkY5 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight5, tablePaint)
                }

                y += dynamicRowHeight5

// Baris 6
                val itemMaxWidth6 = colX[2] - colX[1] - 10f // Lebar maksimum untuk kolom ITEM
                val remarkMaxWidth6 =
                    colX[5] - colX[4] - 10f // Lebar maksimum untuk kolom KETERANGAN

                val itemLines6 = wrapText(
                    "Panjang Kabel Power (25mm) Dari OLT ke DCPDB Eksisting/New (Untuk 2 source)",
                    itemMaxWidth6,
                    cellPaint
                ) // Text wrapping untuk kolom ITEM
                val remarkLines6 = wrapText(
                    remark6,
                    remarkMaxWidth6,
                    cellPaint
                ) // Text wrapping untuk kolom KETERANGAN

                val dynamicRowHeight6 = maxOf(
                    40f,
                    20f + (itemLines6.size * 15f),
                    20f + (remarkLines6.size * 15f)
                )

                if (y + dynamicRowHeight6 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight6, tablePaint)
                canvas.drawText("6", colX[0] + 15f, y + 20f, cellPaint)

                var itemY6 = y + 18f
                for (line in itemLines6) {
                    canvas.drawText(line, colX[1] + 5f, itemY6, cellPaint)
                    itemY6 += 15f
                }

                canvas.drawText("Meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual6, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY6 = y + 18f
                for (line in remarkLines6) {
                    canvas.drawText(line, colX[4] + 5f, remarkY6, cellPaint)
                    remarkY6 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight6, tablePaint)
                }

                y += dynamicRowHeight6

                // Baris 7
                val itemMaxWidth7 = colX[2] - colX[1] - 10f
                val remarkMaxWidth7 = colX[5] - colX[4] - 10f

                val itemLines7 = wrapText(
                    "Panjang Kabel Power (35mm). Kebutuhan yang diperlukan jika tidak ada DCPDB Eksisting / Menggunakan DCPDB New",
                    itemMaxWidth7,
                    cellPaint
                )
                val remarkLines7 = wrapText(remark7, remarkMaxWidth7, cellPaint)

                val dynamicRowHeight7 = maxOf(
                    40f,
                    20f + (itemLines7.size * 15f),
                    20f + (remarkLines7.size * 15f)
                )

                if (y + dynamicRowHeight7 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight7, tablePaint)
                canvas.drawText("7", colX[0] + 15f, y + 20f, cellPaint)

                var itemY7 = y + 18f
                for (line in itemLines7) {
                    canvas.drawText(line, colX[1] + 5f, itemY7, cellPaint)
                    itemY7 += 15f
                }

                canvas.drawText("meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual7, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY7 = y + 18f
                for (line in remarkLines7) {
                    canvas.drawText(line, colX[4] + 5f, remarkY7, cellPaint)
                    remarkY7 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight7, tablePaint)
                }

                y += dynamicRowHeight7

// Baris 8
                val itemMaxWidth8 = colX[2] - colX[1] - 10f
                val remarkMaxWidth8 = colX[5] - colX[4] - 10f

                val itemLines8 =
                    wrapText("Kebutuhan catuan daya di Recti", itemMaxWidth8, cellPaint)
                val remarkLines8 = wrapText(remark8, remarkMaxWidth8, cellPaint)

                val dynamicRowHeight8 = maxOf(
                    40f,
                    20f + (itemLines8.size * 15f),
                    20f + (remarkLines8.size * 15f)
                )

                if (y + dynamicRowHeight8 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight8, tablePaint)
                canvas.drawText("8", colX[0] + 15f, y + 20f, cellPaint)

                var itemY8 = y + 18f
                for (line in itemLines8) {
                    canvas.drawText(line, colX[1] + 5f, itemY8, cellPaint)
                    itemY8 += 15f
                }

                canvas.drawText("OK/NOK", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual8, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY8 = y + 18f
                for (line in remarkLines8) {
                    canvas.drawText(line, colX[4] + 5f, remarkY8, cellPaint)
                    remarkY8 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight8, tablePaint)
                }

                y += dynamicRowHeight8

// Baris 9
                val itemMaxWidth9 = colX[2] - colX[1] - 10f
                val remarkMaxWidth9 = colX[5] - colX[4] - 10f

                val itemLines9 = wrapText(
                    "Kebutuhan DCPDB New, jika dibutuhkan dan Propose DCPDBnya",
                    itemMaxWidth9,
                    cellPaint
                )
                val remarkLines9 = wrapText(remark9, remarkMaxWidth9, cellPaint)

                val dynamicRowHeight9 = maxOf(
                    40f,
                    20f + (itemLines9.size * 15f),
                    20f + (remarkLines9.size * 15f)
                )

                if (y + dynamicRowHeight9 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight9, tablePaint)
                canvas.drawText("9", colX[0] + 15f, y + 20f, cellPaint)

                var itemY9 = y + 18f
                for (line in itemLines9) {
                    canvas.drawText(line, colX[1] + 5f, itemY9, cellPaint)
                    itemY9 += 15f
                }

                canvas.drawText("Pcs", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual9, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY9 = y + 18f
                for (line in remarkLines9) {
                    canvas.drawText(line, colX[4] + 5f, remarkY9, cellPaint)
                    remarkY9 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight9, tablePaint)
                }

                y += dynamicRowHeight9

                // Baris 10
                val itemMaxWidth10 = colX[2] - colX[1] - 10f
                val remarkMaxWidth10 = colX[5] - colX[4] - 10f

                val itemLines10 = wrapText(
                    "Kebutuhan Tray @3m (pcs) dari Tray Eksisting ke OLT-turunan Dan Rack FTM-EA kalau diperlukan",
                    itemMaxWidth10,
                    cellPaint
                )
                val remarkLines10 = wrapText(remark10, remarkMaxWidth10, cellPaint)

                val dynamicRowHeight10 = maxOf(
                    40f,
                    20f + (itemLines10.size * 15f),
                    20f + (remarkLines10.size * 15f)
                )

                if (y + dynamicRowHeight10 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight10, tablePaint)
                canvas.drawText("10", colX[0] + 15f, y + 20f, cellPaint)

                var itemY10 = y + 18f
                for (line in itemLines10) {
                    canvas.drawText(line, colX[1] + 5f, itemY10, cellPaint)
                    itemY10 += 15f
                }

                canvas.drawText("Meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual10, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY10 = y + 18f
                for (line in remarkLines10) {
                    canvas.drawText(line, colX[4] + 5f, remarkY10, cellPaint)
                    remarkY10 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight10, tablePaint)
                }

                y += dynamicRowHeight10

// Baris 11
                val itemMaxWidth11 = colX[2] - colX[1] - 10f
                val remarkMaxWidth11 = colX[5] - colX[4] - 10f

                val itemLines11 = wrapText("Kebutuhan MCB 63A-Schneider", itemMaxWidth11, cellPaint)
                val remarkLines11 = wrapText(remark11, remarkMaxWidth11, cellPaint)

                val dynamicRowHeight11 = maxOf(
                    40f,
                    20f + (itemLines11.size * 15f),
                    20f + (remarkLines11.size * 15f)
                )

                if (y + dynamicRowHeight11 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight11, tablePaint)
                canvas.drawText("11", colX[0] + 15f, y + 20f, cellPaint)

                var itemY11 = y + 18f
                for (line in itemLines11) {
                    canvas.drawText(line, colX[1] + 5f, itemY11, cellPaint)
                    itemY11 += 15f
                }

                canvas.drawText("Pcs", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual11, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY11 = y + 18f
                for (line in remarkLines11) {
                    canvas.drawText(line, colX[4] + 5f, remarkY11, cellPaint)
                    remarkY11 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight11, tablePaint)
                }

                y += dynamicRowHeight11

// Baris 12
                val itemMaxWidth12 = colX[2] - colX[1] - 10f
                val remarkMaxWidth12 = colX[5] - colX[4] - 10f

                val itemLines12 = wrapText(
                    "Space 2 pcs FTB untuk di install di rack EA",
                    itemMaxWidth12,
                    cellPaint
                )
                val remarkLines12 = wrapText(remark12, remarkMaxWidth12, cellPaint)

                val dynamicRowHeight12 = maxOf(
                    40f,
                    20f + (itemLines12.size * 15f),
                    20f + (remarkLines12.size * 15f)
                )

                if (y + dynamicRowHeight12 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight12, tablePaint)
                canvas.drawText("12", colX[0] + 15f, y + 20f, cellPaint)

                var itemY12 = y + 18f
                for (line in itemLines12) {
                    canvas.drawText(line, colX[1] + 5f, itemY12, cellPaint)
                    itemY12 += 15f
                }

                canvas.drawText("Meter", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual12, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY12 = y + 18f
                for (line in remarkLines12) {
                    canvas.drawText(line, colX[4] + 5f, remarkY12, cellPaint)
                    remarkY12 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight12, tablePaint)
                }

                y += dynamicRowHeight12

// Baris 13
                val itemMaxWidth13 = colX[2] - colX[1] - 10f
                val remarkMaxWidth13 = colX[5] - colX[4] - 10f

                val itemLines13 = wrapText(
                    "FTB yang kita gunakan FTB Type TDS/MDT tidak bisa mengikuti FTB Eksisting jika ada yang beda",
                    itemMaxWidth13,
                    cellPaint
                )
                val remarkLines13 = wrapText(remark13, remarkMaxWidth13, cellPaint)

                val dynamicRowHeight13 = maxOf(
                    40f,
                    20f + (itemLines13.size * 15f),
                    20f + (remarkLines13.size * 15f)
                )

                if (y + dynamicRowHeight13 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight13, tablePaint)
                canvas.drawText("13", colX[0] + 15f, y + 20f, cellPaint)

                var itemY13 = y + 18f
                for (line in itemLines13) {
                    canvas.drawText(line, colX[1] + 5f, itemY13, cellPaint)
                    itemY13 += 15f
                }

                canvas.drawText("OK/NOK", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual13, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY13 = y + 18f
                for (line in remarkLines13) {
                    canvas.drawText(line, colX[4] + 5f, remarkY13, cellPaint)
                    remarkY13 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight13, tablePaint)
                }

                y += dynamicRowHeight13

                // Baris 14
                val itemMaxWidth14 = colX[2] - colX[1] - 10f
                val remarkMaxWidth14 = colX[5] - colX[4] - 10f

                val itemLines14 = wrapText("Kebutuhan Rack EA", itemMaxWidth14, cellPaint)
                val remarkLines14 = wrapText(remark14, remarkMaxWidth14, cellPaint)

                val dynamicRowHeight14 = maxOf(
                    40f,
                    20f + (itemLines14.size * 15f),
                    20f + (remarkLines14.size * 15f)
                )

                if (y + dynamicRowHeight14 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight14, tablePaint)
                canvas.drawText("14", colX[0] + 15f, y + 20f, cellPaint)

                var itemY14 = y + 18f
                for (line in itemLines14) {
                    canvas.drawText(line, colX[1] + 5f, itemY14, cellPaint)
                    itemY14 += 15f
                }

                canvas.drawText("Pcs", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual14, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY14 = y + 18f
                for (line in remarkLines14) {
                    canvas.drawText(line, colX[4] + 5f, remarkY14, cellPaint)
                    remarkY14 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight14, tablePaint)
                }

                y += dynamicRowHeight14

// Baris 15
                val itemMaxWidth15 = colX[2] - colX[1] - 10f
                val remarkMaxWidth15 = colX[5] - colX[4] - 10f

                val itemLines15 = wrapText("Alokasi Port Metro", itemMaxWidth15, cellPaint)
                val remarkLines15 = wrapText(remark15, remarkMaxWidth15, cellPaint)

                val dynamicRowHeight15 = maxOf(
                    40f,
                    20f + (itemLines15.size * 15f),
                    20f + (remarkLines15.size * 15f)
                )

                if (y + dynamicRowHeight15 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight15, tablePaint)
                canvas.drawText("15", colX[0] + 15f, y + 20f, cellPaint)

                var itemY15 = y + 18f
                for (line in itemLines15) {
                    canvas.drawText(line, colX[1] + 5f, itemY15, cellPaint)
                    itemY15 += 15f
                }

                canvas.drawText("Port", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual15, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY15 = y + 18f
                for (line in remarkLines15) {
                    canvas.drawText(line, colX[4] + 5f, remarkY15, cellPaint)
                    remarkY15 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight15, tablePaint)
                }

                y += dynamicRowHeight15

// Baris 16
                val itemMaxWidth16 = colX[2] - colX[1] - 10f
                val remarkMaxWidth16 = colX[5] - colX[4] - 10f

                val itemLines16 = wrapText("Kebutuhan SFP", itemMaxWidth16, cellPaint)
                val remarkLines16 = wrapText(remark16, remarkMaxWidth16, cellPaint)

                val dynamicRowHeight16 = maxOf(
                    40f,
                    20f + (itemLines16.size * 15f),
                    20f + (remarkLines16.size * 15f)
                )

                if (y + dynamicRowHeight16 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight16, tablePaint)
                canvas.drawText("16", colX[0] + 15f, y + 20f, cellPaint)

                var itemY16 = y + 18f
                for (line in itemLines16) {
                    canvas.drawText(line, colX[1] + 5f, itemY16, cellPaint)
                    itemY16 += 15f
                }

                canvas.drawText("Pcs", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual16, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY16 = y + 18f
                for (line in remarkLines16) {
                    canvas.drawText(line, colX[4] + 5f, remarkY16, cellPaint)
                    remarkY16 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight16, tablePaint)
                }

                y += dynamicRowHeight16

// Baris 17
                val itemMaxWidth17 = colX[2] - colX[1] - 10f
                val remarkMaxWidth17 = colX[5] - colX[4] - 10f

                val itemLines17 = wrapText(
                    "Alokasi Core di FTB Eksisting (di Rack ET)",
                    itemMaxWidth17,
                    cellPaint
                )
                val remarkLines17 = wrapText(remark17, remarkMaxWidth17, cellPaint)

                val dynamicRowHeight17 = maxOf(
                    40f,
                    20f + (itemLines17.size * 15f),
                    20f + (remarkLines17.size * 15f)
                )

                if (y + dynamicRowHeight17 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight17, tablePaint)
                canvas.drawText("17", colX[0] + 15f, y + 20f, cellPaint)

                var itemY17 = y + 18f
                for (line in itemLines17) {
                    canvas.drawText(line, colX[1] + 5f, itemY17, cellPaint)
                    itemY17 += 15f
                }

                canvas.drawText("Core", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual17, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY17 = y + 18f
                for (line in remarkLines17) {
                    canvas.drawText(line, colX[4] + 5f, remarkY17, cellPaint)
                    remarkY17 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight17, tablePaint)
                }

                y += dynamicRowHeight17

                // Baris 18
                val itemMaxWidth18 = colX[2] - colX[1] - 10f
                val remarkMaxWidth18 = colX[5] - colX[4] - 10f

                val itemLines18 =
                    wrapText("Kondisi Penerangan di ruangan OLT", itemMaxWidth18, cellPaint)
                val remarkLines18 = wrapText(remark18, remarkMaxWidth18, cellPaint)

                val dynamicRowHeight18 = maxOf(
                    40f,
                    20f + (itemLines18.size * 15f),
                    20f + (remarkLines18.size * 15f)
                )

                if (y + dynamicRowHeight18 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight18, tablePaint)
                canvas.drawText("18", colX[0] + 15f, y + 20f, cellPaint)

                var itemY18 = y + 18f
                for (line in itemLines18) {
                    canvas.drawText(line, colX[1] + 5f, itemY18, cellPaint)
                    itemY18 += 15f
                }

                canvas.drawText("OK/NOK", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual18, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY18 = y + 18f
                for (line in remarkLines18) {
                    canvas.drawText(line, colX[4] + 5f, remarkY18, cellPaint)
                    remarkY18 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight18, tablePaint)
                }

                y += dynamicRowHeight18

// Baris 19
                val itemMaxWidth19 = colX[2] - colX[1] - 10f
                val remarkMaxWidth19 = colX[5] - colX[4] - 10f

                val itemLines19 =
                    wrapText("CME – Kebutuhan Air Conditioner", itemMaxWidth19, cellPaint)
                val remarkLines19 = wrapText(remark19, remarkMaxWidth19, cellPaint)

                val dynamicRowHeight19 = maxOf(
                    40f,
                    20f + (itemLines19.size * 15f),
                    20f + (remarkLines19.size * 15f)
                )

                if (y + dynamicRowHeight19 > pageHeight - marginBottom) {
                    drawFooter()
                    document.finishPage(page)
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                }

                canvas.drawRect(colX[0], y, colX[5], y + dynamicRowHeight19, tablePaint)
                canvas.drawText("19", colX[0] + 15f, y + 20f, cellPaint)

                var itemY19 = y + 18f
                for (line in itemLines19) {
                    canvas.drawText(line, colX[1] + 5f, itemY19, cellPaint)
                    itemY19 += 15f
                }

                canvas.drawText("Pcs", colX[2] + 10f, y + 20f, cellPaint)
                canvas.drawText(actual19, colX[3] + 10f, y + 20f, cellPaint)

                var remarkY19 = y + 18f
                for (line in remarkLines19) {
                    canvas.drawText(line, colX[4] + 5f, remarkY19, cellPaint)
                    remarkY19 += 15f
                }

                for (x in colX) {
                    canvas.drawLine(x, y, x, y + dynamicRowHeight19, tablePaint)
                }

                y += dynamicRowHeight19

                // Setelah semua baris tabel selesai
                drawClosingStatement() // Tambahkan teks penutup di bawah tabel terakhir
                // Panggil drawSignatures di halaman terakhir
                val region = findViewById<EditText>(R.id.etTselRegion).text.toString()
                drawSignaturesWithFormattedTitles(canvas, region, y + 30f, paint, boldPaint)

                drawFooter() // Tambahkan footer di halaman terakhir
                document.finishPage(page)



                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()

                // Cek nama file yang belum digunakan
                var fileIndex = 1
                var file: File
                do {
                    val fileName = "SurveyLokasi$fileIndex.pdf"
                    file = File(downloadsDir, fileName)
                    fileIndex++
                } while (file.exists())

                // Simpan dokumen
                document.writeTo(FileOutputStream(file))
                document.close()

                createdFile = file // 🔥 Inilah bagian penting yang sebelumnya tidak ada!

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@BASurveyBigActivity,
                        "PDF berhasil disimpan di: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@BASurveyBigActivity,
                        "Gagal membuat PDF: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
        }

        return createdFile ?: throw IllegalStateException("File PDF tidak berhasil dibuat.")
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) > maxWidth) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }

        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        return lines
    }
}