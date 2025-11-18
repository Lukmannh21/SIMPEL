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

class BASurveyBigEditActivity : AppCompatActivity() {

    // Firebase instances
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // UI Components
    private lateinit var formContainer: View

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
    private lateinit var btnUpdateForm: Button
    private lateinit var btnCancel: Button

    // Photo upload variables
    private val photoUris = HashMap<Int, Uri>()
    private var currentPhotoIndex = 0
    private lateinit var photoButtons: Array<Button>
    private lateinit var photoImageViews: Array<ImageView>
    private lateinit var photoLabels: Array<TextView>

    // Photo labels
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

    private val marginX = 50f

    // Temporary file for camera
    private var tempPhotoUri: Uri? = null
    private var currentImageView: ImageView? = null

    // Survey data
    private var surveyId: String = ""
    private var executor: String = ""

    // Track old signature URLs for deletion
    private val oldSignatureUrls = mutableMapOf<String, String>()
    private val oldPhotoUrls = mutableMapOf<Int, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basurvey_big_edit)

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

        // Get survey ID from intent
        surveyId = intent.getStringExtra("SURVEY_ID") ?: ""
        if (surveyId.isEmpty()) {
            Toast.makeText(this, "Invalid survey ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize UI components
        initializeUI()

        // Setup back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Check and request permissions
        checkAndRequestPermissions()

        // Load existing survey data
        loadExistingSurveyData()
    }

    private fun initializeUI() {
        // Initialize form fields
        inputProjectTitle = findViewById(R.id.inputProjectTitle)
        inputContractNumber = findViewById(R.id.inputContractNumber)
        inputExecutor = findViewById(R.id.inputExecutor)
        inputLocation = findViewById(R.id.inputLocation)
        inputDescription = findViewById(R.id.inputDescription)
        etTselRegion = findViewById(R.id.etTselRegion)

        // Initialize buttons
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf)
        btnUpdateForm = findViewById(R.id.btnUpdateForm)
        btnCancel = findViewById(R.id.btnCancel)

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

        // Initialize photo buttons
        try {
            photoButtons = Array(19) { i ->
                findViewById<Button>(resources.getIdentifier("btnUploadPhoto${i+1}", "id", packageName)).apply {
                    val finalIndex = i
                    setOnClickListener {
                        Log.d("BASurveyBigEdit", "Photo button ${i+1} clicked, setting index to $finalIndex")
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
            Log.e("BASurveyBigEdit", "Error initializing photo views: ${e.message}", e)
            Toast.makeText(this, "Error initializing photo buttons: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // Setup signature button listeners
        setupSignatureButtons()

        // Setup buttons
        btnGeneratePdf.setOnClickListener {
            if (validateForm()) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        generateStyledPdf()
                    } catch (e: Exception) {
                        Log.e("BASurveyBigEdit", "Error generating PDF: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@BASurveyBigEditActivity,
                                "Gagal membuat PDF: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        btnUpdateForm.setOnClickListener {
            if (validateForm()) {
                updateForm()
            }
        }

        btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun loadExistingSurveyData() {
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        db.collection("big_surveys").document(surveyId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data ?: return@addOnSuccessListener

                    // Load basic info
                    inputProjectTitle.setText(data["projectTitle"] as? String ?: "")
                    inputContractNumber.setText(data["contractNumber"] as? String ?: "")
                    executor = data["executor"] as? String ?: ""

                    val executorAdapter = inputExecutor.adapter as ArrayAdapter<String>
                    val position = (0 until executorAdapter.count).find {
                        executorAdapter.getItem(it) == executor
                    } ?: 0
                    inputExecutor.setSelection(position)

                    inputLocation.setText(data["location"] as? String ?: "")
                    inputDescription.setText(data["description"] as? String ?: "")
                    etTselRegion.setText(data["tselRegion"] as? String ?: "")

                    // Load actual and remark fields
                    for (i in 1..19) {
                        loadFieldIfExists(data, "actual$i", findViewById(resources.getIdentifier("inputAktual$i", "id", packageName)))
                        loadFieldIfExists(data, "remark$i", findViewById(resources.getIdentifier("inputKeterangan$i", "id", packageName)))
                    }

                    // Load signatures
                    loadSignatureData(data, "zteName", "zteNik", "zteSignature", etZteName, etZteNik, imgZteSignature)
                    loadSignatureData(data, "tifName", "tifNik", "tifSignature", etTifName, etTifNik, imgTifSignature)
                    loadSignatureData(data, "telkomName", "telkomNik", "telkomSignature", etTelkomName, etTelkomNik, imgTelkomSignature)
                    loadSignatureData(data, "tselNopName", "tselNopNik", "tselNopSignature", etTselNopName, etTselNopNik, imgTselNopSignature)
                    loadSignatureData(data, "tselRtpdsName", "tselRtpdsNik", "tselRtpdsSignature", etTselRtpdsName, etTselRtpdsNik, imgTselRtpdsSignature)
                    loadSignatureData(data, "tselRtpeNfName", "tselRtpeNfNik", "tselRtpeNfSignature", etTselRtpeNfName, etTselRtpeNfNik, imgTselRtpeNfSignature)

                    // Store old URLs for deletion
                    val zteSignatureUrl = data["zteSignature"] as? String
                    if (!zteSignatureUrl.isNullOrEmpty()) oldSignatureUrls["zteSignature"] = zteSignatureUrl

                    val tifSignatureUrl = data["tifSignature"] as? String
                    if (!tifSignatureUrl.isNullOrEmpty()) oldSignatureUrls["tifSignature"] = tifSignatureUrl

                    val telkomSignatureUrl = data["telkomSignature"] as? String
                    if (!telkomSignatureUrl.isNullOrEmpty()) oldSignatureUrls["telkomSignature"] = telkomSignatureUrl

                    val tselNopSignatureUrl = data["tselNopSignature"] as? String
                    if (!tselNopSignatureUrl.isNullOrEmpty()) oldSignatureUrls["tselNopSignature"] = tselNopSignatureUrl

                    val tselRtpdsSignatureUrl = data["tselRtpdsSignature"] as? String
                    if (!tselRtpdsSignatureUrl.isNullOrEmpty()) oldSignatureUrls["tselRtpdsSignature"] = tselRtpdsSignatureUrl

                    val tselRtpeNfSignatureUrl = data["tselRtpeNfSignature"] as? String
                    if (!tselRtpeNfSignatureUrl.isNullOrEmpty()) oldSignatureUrls["tselRtpeNfSignature"] = tselRtpeNfSignatureUrl

                    // Load photos
                    loadPhotosFromFirebase(data)

                    loadingDialog.dismiss()
                } else {
                    loadingDialog.dismiss()
                    Toast.makeText(this, "Survey not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Log.e("BASurveyBigEdit", "Error loading survey: ${e.message}")
                Toast.makeText(this, "Error loading survey: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun loadPhotosFromFirebase(data: Map<String, Any>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (i in 0 until 19) {
                    val photoUrl = data["photo${i+1}"] as? String
                    if (!photoUrl.isNullOrEmpty()) {
                        try {
                            oldPhotoUrls[i] = photoUrl
                            val storageRef = storage.getReferenceFromUrl(photoUrl)
                            val FIVE_MEGABYTE: Long = 5 * 1024 * 1024

                            val bytes = storageRef.getBytes(FIVE_MEGABYTE).await()
                            val options = BitmapFactory.Options().apply {
                                inPreferredConfig = Bitmap.Config.ARGB_8888
                            }
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                            withContext(Dispatchers.Main) {
                                photoImageViews[i].setImageBitmap(bitmap)
                                photoImageViews[i].visibility = View.VISIBLE
                                photoImageViews[i].tag = bitmap
                            }
                        } catch (e: Exception) {
                            Log.e("BASurveyBigEdit", "Error loading photo ${i+1}: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BASurveyBigEdit", "Error loading photos: ${e.message}")
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
                    Log.e("BASurveyBigEdit", "Error loading signature: ${e.message}")
                }
            }
        }
    }

    private fun setupSignatureButtons() {
        btnZteSignature.setOnClickListener { showSignatureOptions(imgZteSignature) }
        btnTifSignature.setOnClickListener { showSignatureOptions(imgTifSignature) }
        btnTelkomSignature.setOnClickListener { showSignatureOptions(imgTelkomSignature) }
        btnTselNopSignature.setOnClickListener { showSignatureOptions(imgTselNopSignature) }
        btnTselRtpdsSignature.setOnClickListener { showSignatureOptions(imgTselRtpdsSignature) }
        btnTselRtpeNfSignature.setOnClickListener { showSignatureOptions(imgTselRtpeNfSignature) }
    }

    private fun showSignatureOptions(imageView: ImageView) {
        currentImageView = imageView
        currentPhotoIndex = -1

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
        Log.d("BASurveyBigEdit", "Showing photo source dialog for photo index: $currentPhotoIndex")

        try {
            val options = arrayOf("Take Photo", "Choose from Gallery")

            AlertDialog.Builder(this)
                .setTitle("Upload Photo ${currentPhotoIndex + 1}")
                .setItems(options) { dialog, which ->
                    Log.d("BASurveyBigEdit", "Dialog option selected: ${options[which]}")

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
                    Log.d("BASurveyBigEdit", "Photo source dialog cancelled")
                }
                .show()
        } catch (e: Exception) {
            Log.e("BASurveyBigEdit", "Error showing photo source dialog: ${e.message}", e)
            Toast.makeText(this, "Error opening photo options", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        try {
            Log.d("BASurveyBigEdit", "Opening camera for photo index: $currentPhotoIndex")

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
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
            Log.e("BASurveyBigEdit", "Error opening camera: ${e.message}", e)
            Toast.makeText(this, "Cannot open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        try {
            Log.d("BASurveyBigEdit", "Opening gallery for index: $currentPhotoIndex")

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.addCategory(Intent.CATEGORY_OPENABLE)

            startActivityForResult(Intent.createChooser(intent, "Select Image"), REQUEST_GALLERY)
        } catch (e: Exception) {
            Log.e("BASurveyBigEdit", "Error opening gallery: ${e.message}", e)
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
            Log.e("BASurveyBigEdit", "File manager error: ${e.message}")
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

        Log.d("BASurveyBigEdit", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode, currentPhotoIndex=$currentPhotoIndex")

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    try {
                        Log.d("BASurveyBigEdit", "Processing camera result for photo index: $currentPhotoIndex")
                        val uri = tempPhotoUri
                        if (uri != null && currentPhotoIndex >= 0) {
                            photoUris[currentPhotoIndex] = uri
                            displayHighQualityImage(uri, photoImageViews[currentPhotoIndex])
                            Toast.makeText(this, "Photo captured successfully", Toast.LENGTH_SHORT).show()
                        } else if (currentImageView != null) {
                            try {
                                displayHighQualityImage(uri!!, currentImageView!!)
                            } catch (e: Exception) {
                                Log.e("BASurveyBigEdit", "Error setting signature image: ${e.message}")
                            }
                        } else {
                            Log.e("BASurveyBigEdit", "Camera returned null URI or invalid index: $currentPhotoIndex")
                            Toast.makeText(this, "Failed to capture photo", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("BASurveyBigEdit", "Error processing camera image: ${e.message}", e)
                        Toast.makeText(this, "Error processing camera image", Toast.LENGTH_SHORT).show()
                    }
                }

                REQUEST_GALLERY -> {
                    try {
                        Log.d("BASurveyBigEdit", "Processing gallery result")
                        val uri = data?.data
                        if (uri != null) {
                            if (currentImageView != null) {
                                Log.d("BASurveyBigEdit", "Setting signature image")
                                displayHighQualityImage(uri, currentImageView!!)
                                Toast.makeText(this, "Signature image selected", Toast.LENGTH_SHORT).show()
                            } else if (currentPhotoIndex >= 0) {
                                Log.d("BASurveyBigEdit", "Setting photo image for index: $currentPhotoIndex")
                                photoUris[currentPhotoIndex] = uri
                                displayHighQualityImage(uri, photoImageViews[currentPhotoIndex])
                                Toast.makeText(this, "Photo selected successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e("BASurveyBigEdit", "Invalid state: no current image view or photo index")
                                Toast.makeText(this, "Error: couldn't determine target for image", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Log.e("BASurveyBigEdit", "Gallery returned null URI")
                            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("BASurveyBigEdit", "Error processing gallery image: ${e.message}", e)
                        Toast.makeText(this, "Error processing gallery image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun displayHighQualityImage(uri: Uri, imageView: ImageView) {
        try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inJustDecodeBounds = true
            }

            contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            val maxDimension = 1024
            var inSampleSize = 1

            if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
                val heightRatio = Math.round(options.outHeight.toFloat() / maxDimension.toFloat())
                val widthRatio = Math.round(options.outWidth.toFloat() / maxDimension.toFloat())
                inSampleSize = Math.max(1, Math.min(heightRatio, widthRatio))
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize

            contentResolver.openInputStream(uri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input, null, options)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE
                    imageView.tag = uri
                } else {
                    imageView.setImageURI(uri)
                    imageView.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            Log.e("BASurveyBigEdit", "Error loading high quality image: ${e.message}", e)
            imageView.setImageURI(uri)
            imageView.visibility = View.VISIBLE
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

    private fun updateForm() {
        val loadingDialog = AlertDialog.Builder(this)
            .setMessage("Updating form...")
            .setCancelable(false)
            .create()

        loadingDialog.show()

        val updatedData = hashMapOf<String, Any>(
            "projectTitle" to inputProjectTitle.text.toString(),
            "contractNumber" to inputContractNumber.text.toString(),
            "executor" to inputExecutor.selectedItem.toString(),
            "location" to inputLocation.text.toString(),
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
            "updatedAt" to System.currentTimeMillis()
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Upload signatures if changed
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
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, baos)
                        val data = baos.toByteArray()

                        val storageRef = storage.reference.child(storagePath)
                        val uploadTask = storageRef.putBytes(data).await()
                        val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

                        updatedData[fieldName] = downloadUrl

                        // Delete old signature
                        oldSignatureUrls[fieldName]?.let { oldUrl ->
                            try {
                                storage.getReferenceFromUrl(oldUrl).delete().await()
                            } catch (e: Exception) {
                                Log.e("BASurveyBigEdit", "Error deleting old signature: ${e.message}")
                            }
                        }
                    }
                }

                // Upload new/updated photos
                if (photoUris.isNotEmpty()) {
                    for ((index, uri) in photoUris) {
                        val photoPath = "ba_survey_big_olt_photos/${surveyId}/photo${index+1}_${UUID.randomUUID()}.jpg"
                        val photoRef = storage.reference.child(photoPath)

                        try {
                            contentResolver.openInputStream(uri)?.use { inputStream ->
                                val options = BitmapFactory.Options().apply {
                                    inPreferredConfig = Bitmap.Config.ARGB_8888
                                    inJustDecodeBounds = true
                                }
                                BitmapFactory.decodeStream(inputStream, null, options)

                                val sampleSize = calculateOptimalSampleSize(
                                    options.outWidth, options.outHeight,
                                    2048, 2048
                                )

                                options.inJustDecodeBounds = false
                                options.inSampleSize = sampleSize

                                contentResolver.openInputStream(uri)?.use { input2 ->
                                    val bitmap = BitmapFactory.decodeStream(input2, null, options)

                                    val baos = java.io.ByteArrayOutputStream()
                                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 95, baos)
                                    val bytes = baos.toByteArray()

                                    val uploadTask = photoRef.putBytes(bytes).await()
                                    val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

                                    updatedData["photo${index+1}"] = downloadUrl
                                    updatedData["photoLabel${index+1}"] = photoLabelTexts[index]

                                    // Delete old photo
                                    oldPhotoUrls[index]?.let { oldUrl ->
                                        try {
                                            storage.getReferenceFromUrl(oldUrl).delete().await()
                                        } catch (e: Exception) {
                                            Log.e("BASurveyBigEdit", "Error deleting old photo: ${e.message}")
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("BASurveyBigEdit", "Error uploading photo $index: ${e.message}")
                        }
                    }
                }

                // Generate and upload new PDF
                try {
                    val pdfFile = generateStyledPdf()

                    val pdfPath = "ba_survey_big_olt_pdf/${surveyId}.pdf"
                    val pdfStorageRef = storage.reference.child(pdfPath)

                    // Delete old PDF
                    try {
                        pdfStorageRef.delete().await()
                    } catch (e: Exception) {
                        Log.e("BASurveyBigEdit", "Old PDF might not exist: ${e.message}")
                    }

                    // Upload new PDF
                    pdfStorageRef.putFile(Uri.fromFile(pdfFile)).await()
                    val pdfDownloadUrl = pdfStorageRef.downloadUrl.await().toString()
                    updatedData["pdfUrl"] = pdfDownloadUrl
                } catch (e: Exception) {
                    Log.e("BASurveyBigEdit", "Error generating PDF: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@BASurveyBigEditActivity,
                            "PDF tidak berhasil dibuat, melanjutkan tanpa PDF",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                // Update Firestore
                db.collection("big_surveys").document(surveyId)
                    .update(updatedData as Map<String, Any>)
                    .addOnSuccessListener {
                        loadingDialog.dismiss()
                        Toast.makeText(
                            this@BASurveyBigEditActivity,
                            "Form updated successfully",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        loadingDialog.dismiss()
                        Toast.makeText(
                            this@BASurveyBigEditActivity,
                            "Error updating form: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        this@BASurveyBigEditActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun calculateOptimalSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return min(inSampleSize, 4)
    }

    private fun generateDescription(projectTitle: String, contractNumber: String, executor: String): String {
        val currentDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())
        return "Pada hari ini, $currentDate, telah dilakukan survey bersama terhadap pekerjaan \"$projectTitle\" " +
                "yang dilaksanakan oleh $executor yang terikat Perjanjian Pemborongan \"$contractNumber\" " +
                "dengan hasil sebagai berikut:"
    }

    // Bagian generateStyledPdf() yang lengkap dengan Times New Roman

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
                        val appDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        if (appDir != null && (appDir.exists() || appDir.mkdirs())) {
                            Log.i("BASurveyBig", "Using app directory as fallback")
                        } else {
                            throw IOException("Cannot create storage directory")
                        }
                    }
                }

                // Get input from user
                val projectTitle = findViewById<EditText>(R.id.inputProjectTitle).text.toString()
                val contractNumber = findViewById<EditText>(R.id.inputContractNumber).text.toString()
                val executor = inputExecutor.selectedItem.toString()
                val location = findViewById<EditText>(R.id.inputLocation).text.toString()
                val description = generateDescription(projectTitle, contractNumber, executor)

                withContext(Dispatchers.Main) {
                    findViewById<EditText>(R.id.inputDescription).setText(description)
                }

                // Get all actual and remark fields
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
                val marginTop = 50f
                val marginBottom = 60f
                val maxX = pageWidth - marginX

                // ===== TIMES NEW ROMAN TYPEFACE (SERIF) =====
                val timesNewRomanTypeface = Typeface.create("serif", Typeface.NORMAL)
                val timesNewRomanBold = Typeface.create("serif", Typeface.BOLD)
                val timesNewRomanItalic = Typeface.create("serif", Typeface.ITALIC)
                val timesNewRomanBoldItalic = Typeface.create("serif", Typeface.BOLD_ITALIC)

                // Regular paint with Times New Roman
                val paint = Paint().apply {
                    color = Color.BLACK
                    textSize = 11f
                    textAlign = Paint.Align.LEFT
                    typeface = timesNewRomanTypeface
                    isAntiAlias = true
                }

                // Title paint with Times New Roman Bold
                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 16f
                    typeface = timesNewRomanBold
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                // Bold paint with Times New Roman
                val boldPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 11f
                    typeface = timesNewRomanBold
                    textAlign = Paint.Align.LEFT
                    isAntiAlias = true
                }

                // Cell paint for table with Times New Roman
                val cellPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 11f
                    typeface = timesNewRomanTypeface
                    textAlign = Paint.Align.LEFT
                    isAntiAlias = true
                }

                // Table line paint
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
                    val zteLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_zte)
                    val huaweiLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_huawei)
                    val telkomLogo = BitmapFactory.decodeResource(resources, R.drawable.logo_telkom)

                    // Logo size
                    val logoWidth = 80
                    val logoHeight = 50
                    val topMargin = marginTop

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

                    // Header text with Times New Roman
                    canvas.drawText("BERITA ACARA", centerX, logoBottomY, titlePaint)
                    canvas.drawText("SURVEY LOKASI", centerX, logoBottomY + 20f, titlePaint)
                    canvas.drawLine(marginX, logoBottomY + 30f, maxX, logoBottomY + 30f, paint)
                    y = logoBottomY + 40f
                }

                // Improved page footer
                fun drawFooter() {
                    val footerPaint = Paint().apply {
                        color = Color.BLACK
                        textSize = 8f
                        typeface = timesNewRomanTypeface
                        textAlign = Paint.Align.LEFT
                        alpha = 220
                        isAntiAlias = true
                    }

                    val footerY = pageHeight - 30f

                    // Thinner separator line
                    val linePaint = Paint().apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 0.5f
                        color = Color.BLACK
                    }
                    canvas.drawLine(marginX, footerY, pageWidth - marginX, footerY, linePaint)

                    // Document text
                    val documentText = "Dokumen ini telah ditandatangani secara elektronik dan merupakan dokumen sah sesuai ketentuan yang berlaku"
                    val pageText = "Halaman ${pageCount - 1}"

                    val combinedText = "$documentText     $pageText"
                    canvas.drawText(combinedText, marginX, footerY + 15f, footerPaint)
                }

                // Add closing text below the last table
                fun drawClosingStatement() {
                    val closingText = "Demikian Berita Acara Hasil Survey ini dibuat berdasarkan kenyataan di lapangan untuk dijadikan pedoman pelaksanaan selanjutnya."
                    val closingMaxWidth = maxX - marginX * 2
                    val closingLines = wrapText(closingText, closingMaxWidth, paint)

                    val currentDate = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
                    val closingHeight = 18f * closingLines.size + 10f + 20f

                    if (y + closingHeight > pageHeight - marginBottom - 30f) {
                        drawFooter()
                        document.finishPage(page)
                        page = createPage()
                        canvas = page.canvas
                        y = marginTop
                    }

                    y += 20f
                    for (line in closingLines) {
                        canvas.drawText(line, marginX, y, paint)
                        y += 18f
                    }

                    val closingBoldPaint = Paint(boldPaint).apply {
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(currentDate, maxX, y + 10f, closingBoldPaint)
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
                y += 20f

                // Description
                val descMaxWidth = maxX - marginX * 2
                y = drawJustifiedText(canvas, description, marginX, y, descMaxWidth, paint)

                y += 10f

                // Table with adjusted columns
                val colX = floatArrayOf(
                    marginX,
                    marginX + 40f,
                    marginX + 230f,
                    marginX + 300f,
                    marginX + 370f,
                    maxX
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
                drawClosingStatement()

                // Add signatures - ensure enough space
                val signaturesHeight = 2 * 150f + 20f
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
                // Ganti bagian photo generation di generateStyledPdf() dengan ini:

                // Di bagian generateStyledPdf(), ganti logic foto baru dengan ini:

                if (photoImageViews.any { it.drawable != null }) {
                    page = createPage()
                    canvas = page.canvas
                    y = marginTop

                    drawHeader(executor)

                    val photoTitlePaint = Paint(titlePaint).apply {
                        textAlign = Paint.Align.CENTER
                        textSize = 16f
                        typeface = timesNewRomanBold
                    }
                    canvas.drawText("DOKUMENTASI FOTO", (marginX + maxX) / 2, y + 10f, photoTitlePaint)
                    y += 40f

                    val photoAreaWidth = maxX - marginX
                    val photoContainerWidth = photoAreaWidth / 2 - 10f
                    val photoWidth = photoContainerWidth
                    val photoHeight = 190f
                    val captionHeight = 30f
                    val rowHeight = photoHeight + captionHeight + 20f

                    var currentCol = 0
                    var photosOnCurrentPage = 0

                    val photoRenderPaint = Paint().apply {
                        isFilterBitmap = true
                        isAntiAlias = true
                        isDither = true
                    }

                    // ===== ITERASI SEMUA photoImageViews (baik lama maupun baru) =====
                    for (i in 0 until 19) {
                        val imageView = photoImageViews[i]

                        // Skip jika tidak ada drawable
                        if (imageView.drawable == null) {
                            continue
                        }

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

                        val photoX = if (currentCol == 0) marginX else marginX + photoContainerWidth + 20f

                        // Caption dengan Times New Roman
                        val caption = if (i in photoLabelTexts.indices) photoLabelTexts[i] else "Photo ${i + 1}"
                        canvas.drawText(caption, photoX, y + 12f, boldPaint)

                        val photoRect = RectF(photoX, y + 15f, photoX + photoWidth, y + 15f + photoHeight)

                        try {
                            val drawable = imageView.drawable

                            if (drawable is BitmapDrawable) {
                                // ===== UNTUK FOTO LAMA (dari Drawable BitmapDrawable) =====
                                val bitmap = drawable.bitmap
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

                            } else if (i in photoUris) {
                                // ===== UNTUK FOTO BARU (dari photoUris) =====
                                // ===== FIX: Gunakan let untuk null-safety =====
                                val uri = photoUris[i]
                                uri?.let { photoUri ->
                                    contentResolver.openInputStream(photoUri)?.use { inputStream ->
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

                                        contentResolver.openInputStream(photoUri)?.use { input2 ->
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
                                }
                            }

                        } catch (e: Exception) {
                            Log.e("PDF", "Error drawing photo $i: ${e.message}")
                            val borderPaint = Paint().apply {
                                style = Paint.Style.STROKE
                                strokeWidth = 0.5f
                                color = Color.BLACK
                            }
                            canvas.drawRect(photoRect, borderPaint)
                            canvas.drawText("Error loading photo", photoX + 20, y + 100, paint)
                        }

                        currentCol = (currentCol + 1) % 2
                        photosOnCurrentPage++

                        if (currentCol == 0) {
                            y += rowHeight
                        }
                    }

                    drawFooter()
                    document.finishPage(page)
                }

                // Save document with better error handling
                try {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) {
                        downloadsDir.mkdirs()
                    }

                    var fileIndex = 1
                    var file: File
                    do {
                        val fileName = "SurveyLokasi$fileIndex.pdf"
                        file = File(downloadsDir, fileName)
                        fileIndex++
                    } while (file.exists())

                    val fileOutputStream = FileOutputStream(file)
                    document.writeTo(fileOutputStream)
                    fileOutputStream.close()
                    document.close()

                    createdFile = file

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@BASurveyBigEditActivity,
                            "PDF berhasil disimpan di: ${file.absolutePath}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: IOException) {
                    Log.e("BASurveyBig", "Error saving to public Downloads: ${e.message}")

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
                                    this@BASurveyBigEditActivity,
                                    "PDF berhasil disimpan di direktori aplikasi: ${fallbackFile.absolutePath}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } catch (e2: Exception) {
                            Log.e("BASurveyBig", "Error saving to app directory: ${e2.message}")
                            throw e2
                        }
                    } else {
                        throw e
                    }
                }

            } catch (e: Exception) {
                Log.e("BASurveyBig", "Error generating PDF: ${e.message}")
                e.printStackTrace()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@BASurveyBigEditActivity,
                        "Gagal membuat PDF: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

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

    private fun drawSignaturesWithFormattedTitles(
        canvas: Canvas,
        region: String,
        yStart: Float,
        paint: Paint,
        boldPaint: Paint,
        executor: String
    ) {
        val timesNewRomanTypeface = Typeface.create("serif", Typeface.NORMAL)
        val timesNewRomanBold = Typeface.create("serif", Typeface.BOLD)

        val boxWidth = (595 - (marginX * 2)) / 3
        val signatureBoxHeight = 150f
        var y = yStart

        val boxPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }

        fun drawFormattedTitle(
            canvas: Canvas,
            lines: List<String>,
            x: Float,
            y: Float,
            maxWidth: Float,
            boldPaint: Paint
        ): Float {
            val titlePaintLocal = Paint(boldPaint).apply {
                typeface = timesNewRomanBold
                isAntiAlias = true
            }
            val lineHeight = titlePaintLocal.textSize + 4f
            var currentY = y

            for (line in lines) {
                canvas.drawText(line, x, currentY, titlePaintLocal)
                currentY += lineHeight
            }

            return currentY
        }

        fun drawSignatureBox(
            lines: List<String>,
            name: String,
            nik: String,
            signature: Drawable?,
            x: Float,
            y: Float
        ) {
            val rect = RectF(x, y, x + boxWidth, y + signatureBoxHeight)
            canvas.drawRect(rect, boxPaint)

            val titleY = drawFormattedTitle(
                canvas,
                lines,
                x + 10f,
                y + 20f,
                boxWidth - 20f,
                boldPaint
            )

            val signatureY = titleY + 10f
            if (signature != null) {
                try {
                    val bitmap = (signature as BitmapDrawable).bitmap

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

            val namePaint = Paint(paint).apply {
                typeface = timesNewRomanTypeface
                textSize = 11f
                isAntiAlias = true
            }

            val nameY = y + signatureBoxHeight - 40f
            canvas.drawText("($name)", x + 10f, nameY, namePaint)
            canvas.drawText("NIK: $nik", x + 10f, nameY + 20f, namePaint)
        }

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

    private fun drawJustifiedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint
    ): Float {
        val timesNewRomanTypeface = Typeface.create("serif", Typeface.NORMAL)
        val justifiedPaint = Paint(paint).apply {
            typeface = timesNewRomanTypeface
            isAntiAlias = true
        }

        val lines = wrapText(text, maxWidth, justifiedPaint)
        var currentY = y

        val lineSpacing = justifiedPaint.textSize + 10f
        val extraWordSpacing = 4f

        for ((i, line) in lines.withIndex()) {
            val words = line.split(" ")
            val lineWidth = justifiedPaint.measureText(line)
            val gapCount = words.size - 1

            if (gapCount > 0 && i != lines.lastIndex) {
                val extraSpace = ((maxWidth - lineWidth) / gapCount) + extraWordSpacing
                var startX = x
                for (word in words) {
                    canvas.drawText(word, startX, currentY, justifiedPaint)
                    startX += justifiedPaint.measureText(word) + extraSpace
                }
            } else {
                canvas.drawText(line, x, currentY, justifiedPaint)
            }
            currentY += lineSpacing
        }
        return currentY
    }

    private fun wrapText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val timesNewRomanTypeface = Typeface.create("serif", Typeface.NORMAL)
        val wrappingPaint = Paint(paint).apply {
            typeface = timesNewRomanTypeface
            isAntiAlias = true
        }

        if (text.isEmpty()) return listOf("")

        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            if (wrappingPaint.measureText(word) > maxWidth) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = ""
                }

                var remainingWord = word
                while (wrappingPaint.measureText(remainingWord) > maxWidth) {
                    var i = 1
                    while (i < remainingWord.length) {
                        if (wrappingPaint.measureText(remainingWord.substring(0, i)) > maxWidth) {
                            i--
                            break
                        }
                        i++
                    }

                    i = maxOf(1, i)
                    lines.add(remainingWord.substring(0, i))
                    remainingWord = remainingWord.substring(i)
                }

                currentLine = remainingWord
            } else {
                val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (wrappingPaint.measureText(testLine) > maxWidth) {
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