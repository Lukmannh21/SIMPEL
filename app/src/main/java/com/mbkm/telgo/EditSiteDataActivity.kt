package com.mbkm.telgo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditSiteDataActivity : AppCompatActivity() {

    // UI Components
    private lateinit var tvSiteId: TextView
    private lateinit var tvWitel: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etLastIssue: EditText
    private lateinit var tvKoordinat: TextView

    private lateinit var btnUploadEmailOrder: Button
    private lateinit var btnUploadTelkomselPermit: Button
    private lateinit var btnUploadTelPartner: Button

    private lateinit var btnCaptureLocation: Button
    private lateinit var btnCaptureFoundation: Button
    private lateinit var btnCaptureInstallation: Button
    private lateinit var btnCaptureCabinet: Button
    private lateinit var btnCaptureInet: Button
    private lateinit var btnCaptureUctv: Button
    private lateinit var btnCaptureTelephone: Button

    private lateinit var ivLocation: ImageView
    private lateinit var ivFoundation: ImageView
    private lateinit var ivInstallation: ImageView
    private lateinit var ivCabinet: ImageView
    private lateinit var ivInet: ImageView
    private lateinit var ivUctv: ImageView
    private lateinit var ivTelephone: ImageView

    private lateinit var btnSaveChanges: Button
    private lateinit var btnCancel: Button

    private lateinit var tvEmailOrderFileName: TextView
    private lateinit var tvTelkomselPermitFileName: TextView
    private lateinit var tvTelPartnerFileName: TextView

    // Firebase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Data
    private lateinit var siteId: String
    private lateinit var witel: String
    private var documentId: String = ""

    // Request codes
    private val REQUEST_CAMERA_PERMISSION = 101
    private val REQUEST_IMAGE_CAPTURE = 102
    private val REQUEST_DOCUMENT_PICK = 103

    // Currently selected image type for camera
    private var currentImageType: String = ""

    // File URIs
    private val documentUris = mutableMapOf<String, Uri>()
    private val imageUris = mutableMapOf<String, Uri>()
    private var currentPhotoPath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_site_data)

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
        initializeViews()

        // Set up button listeners
        setupButtonListeners()

        // Load site data
        loadSiteData()
    }

    private fun initializeViews() {
        tvSiteId = findViewById(R.id.tvSiteId)
        tvWitel = findViewById(R.id.tvWitel)
        tvStatus = findViewById(R.id.tvStatus)
        etLastIssue = findViewById(R.id.etLastIssue)
        tvKoordinat = findViewById(R.id.tvKoordinat)

        btnUploadEmailOrder = findViewById(R.id.btnUploadEmailOrder)
        btnUploadTelkomselPermit = findViewById(R.id.btnUploadTelkomselPermit)
        btnUploadTelPartner = findViewById(R.id.btnUploadTelPartner)


        tvEmailOrderFileName = findViewById(R.id.tvEmailOrderFileName)
        tvTelkomselPermitFileName = findViewById(R.id.tvTelkomselPermitFileName)
        tvTelPartnerFileName = findViewById(R.id.tvTelPartnerFileName)


        btnCaptureLocation = findViewById(R.id.btnCaptureLocation)
        btnCaptureFoundation = findViewById(R.id.btnCaptureFoundation)
        btnCaptureInstallation = findViewById(R.id.btnCaptureInstallation)
        btnCaptureCabinet = findViewById(R.id.btnCaptureCabinet)
        btnCaptureInet = findViewById(R.id.btnCaptureInet)
        btnCaptureUctv = findViewById(R.id.btnCaptureUctv)
        btnCaptureTelephone = findViewById(R.id.btnCaptureTelephone)

        ivLocation = findViewById(R.id.ivLocation)
        ivFoundation = findViewById(R.id.ivFoundation)
        ivInstallation = findViewById(R.id.ivInstallation)
        ivCabinet = findViewById(R.id.ivCabinet)
        ivInet = findViewById(R.id.ivInet)
        ivUctv = findViewById(R.id.ivUctv)
        ivTelephone = findViewById(R.id.ivTelephone)

        btnSaveChanges = findViewById(R.id.btnSaveChanges)
        btnCancel = findViewById(R.id.btnCancel)
    }

    private fun setupButtonListeners() {
        // Document upload buttons
        btnUploadEmailOrder.setOnClickListener { openDocumentPicker("email_order") }
        btnUploadTelkomselPermit.setOnClickListener { openDocumentPicker("telkomsel_permit") }
        btnUploadTelPartner.setOnClickListener { openDocumentPicker("tel_partner") }

        // Image capture buttons
        btnCaptureLocation.setOnClickListener { captureImage("site_location") }
        btnCaptureFoundation.setOnClickListener { captureImage("foundation_shelter") }
        btnCaptureInstallation.setOnClickListener { captureImage("installation_process") }
        btnCaptureCabinet.setOnClickListener { captureImage("cabinet") }
        btnCaptureInet.setOnClickListener { captureImage("3p_inet") }
        btnCaptureUctv.setOnClickListener { captureImage("3p_uctv") }
        btnCaptureTelephone.setOnClickListener { captureImage("3p_telephone") }

        // Save & Cancel buttons
        btnSaveChanges.setOnClickListener { saveChanges() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun loadSiteData() {
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
                documentId = document.id
                val site = document.data

                // Set basic site information
                tvSiteId.text = siteId
                tvWitel.text = witel
                tvStatus.text = site?.get("status").toString()
                tvKoordinat.text = site?.get("koordinat").toString()

                // Set last issue if available
                val lastIssueHistory = site?.get("lastIssueHistory") as? List<String>
                if (!lastIssueHistory.isNullOrEmpty()) {
                    // Extract just the content part after the timestamp
                    val lastIssue = lastIssueHistory[0]
                    val parts = lastIssue.split(" - ", limit = 2)
                    if (parts.size > 1) {
                        etLastIssue.setText(parts[1])
                    } else {
                        etLastIssue.setText(lastIssue)
                    }
                }

                // Load existing documents and images from storage
                loadExistingFiles()
            }
            .addOnFailureListener { e ->
                showToast("Error loading site: ${e.message}")
            }
    }

    private fun loadExistingFiles() {
        // Load existing documents
        val documentTypes = listOf(
            "email_order" to tvEmailOrderFileName,
            "telkomsel_permit" to tvTelkomselPermitFileName,
            "tel_partner" to tvTelPartnerFileName
        )

        for ((docType, textView) in documentTypes) {
            val docRef = storage.reference.child("documents/$witel/$siteId/$docType.pdf")
            docRef.downloadUrl
                .addOnSuccessListener { uri ->
                    // Mark document as existing
                    updateDocumentButtonStatus(docType, true)

                    // Display filename (using the document type as a fallback)
                    val fileName = "$docType.pdf"
                    textView.text = fileName
                    textView.visibility = View.VISIBLE
                }
                .addOnFailureListener {
                    // Document doesn't exist
                    updateDocumentButtonStatus(docType, false)
                    textView.visibility = View.GONE
                }
        }

        // Load existing images
        val imageTypes = listOf(
            "site_location" to ivLocation,
            "foundation_shelter" to ivFoundation,
            "installation_process" to ivInstallation,
            "cabinet" to ivCabinet,
            "3p_inet" to ivInet,
            "3p_uctv" to ivUctv,
            "3p_telephone" to ivTelephone
        )

        for ((imageType, imageView) in imageTypes) {
            val imageRef = storage.reference.child("images/$witel/$siteId/$imageType.jpg")
            imageRef.downloadUrl
                .addOnSuccessListener { uri ->
                    // Image exists, load it into ImageView
                    loadImageIntoView(uri, imageView)
                    updateImageButtonStatus(imageType, true)
                }
                .addOnFailureListener {
                    // Image doesn't exist
                    updateImageButtonStatus(imageType, false)
                }
        }
    }

    private fun updateDocumentButtonStatus(docType: String, exists: Boolean) {
        val button = when (docType) {
            "email_order" -> btnUploadEmailOrder
            "telkomsel_permit" -> btnUploadTelkomselPermit
            "tel_partner" -> btnUploadTelPartner
            else -> return
        }

        button.text = if (exists) "Replace Document" else "Upload Document"
    }

    private fun updateImageButtonStatus(imageType: String, exists: Boolean) {
        val button = when (imageType) {
            "site_location" -> btnCaptureLocation
            "foundation_shelter" -> btnCaptureFoundation
            "installation_process" -> btnCaptureInstallation
            "cabinet" -> btnCaptureCabinet
            "3p_inet" -> btnCaptureInet
            "3p_uctv" -> btnCaptureUctv
            "3p_telephone" -> btnCaptureTelephone
            else -> return
        }

        button.text = if (exists) "Replace Image" else "Capture Image"
    }

    private fun loadImageIntoView(uri: Uri, imageView: ImageView) {
        try {
            // For images from camera, we need to handle the file:// URI differently
            if (uri.scheme == "file") {
                val bitmap = android.graphics.BitmapFactory.decodeFile(uri.path)
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                    imageView.visibility = View.VISIBLE
                } else {
                    showToast("Error loading image: Could not decode bitmap")
                }
            } else {
                // For content:// URIs (from storage)
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                imageView.setImageBitmap(bitmap)
                imageView.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            showToast("Error loading image: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun openDocumentPicker(docType: String) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }

        // Store the document type to use when handling the result
        currentImageType = docType
        startActivityForResult(intent, REQUEST_DOCUMENT_PICK)
    }

    private fun captureImage(imageType: String) {
        if (checkCameraPermission()) {
            // Store the image type to use when handling the result
            currentImageType = imageType

            // Create file for the photo
            val photoFile = createImageFile()
            if (photoFile != null) {
                val photoURI = FileProvider.getUriForFile(
                    this,
                    "com.mbkm.telgo.fileprovider",
                    photoFile
                )

                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                }

                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun createImageFile(): File? {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            val image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
            )

            // Save the file path for use with the camera
            currentPhotoPath = image.absolutePath
            return image
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun checkCameraPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
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
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, capture image
                    captureImage(currentImageType)
                } else {
                    showToast("Camera permission is required to capture images")
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            return
        }

        when (requestCode) {
            REQUEST_IMAGE_CAPTURE -> {
                // Image captured from camera
                if (currentPhotoPath.isNotEmpty()) {
                    // Create URI from file path
                    val photoFile = File(currentPhotoPath)
                    val photoUri = Uri.fromFile(photoFile)

                    // Store the URI for later upload
                    imageUris[currentImageType] = photoUri

                    // Display the captured image in the appropriate ImageView
                    val imageView = when (currentImageType) {
                        "site_location" -> ivLocation
                        "foundation_shelter" -> ivFoundation
                        "installation_process" -> ivInstallation
                        "cabinet" -> ivCabinet
                        "3p_inet" -> ivInet
                        "3p_uctv" -> ivUctv
                        "3p_telephone" -> ivTelephone
                        else -> null
                    }

                    imageView?.let {
                        // Load and display the image
                        try {
                            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                            it.setImageBitmap(bitmap)
                            it.visibility = View.VISIBLE
                        } catch (e: Exception) {
                            showToast("Error displaying image: ${e.message}")
                            e.printStackTrace()
                        }

                        // Update button text to indicate replacement is possible
                        updateImageButtonStatus(currentImageType, true)
                    }

                    showToast("Image captured successfully")
                } else {
                    showToast("Error: Image file not created")
                }
            }

            REQUEST_DOCUMENT_PICK -> {
                // Document selected (keep your existing code)
                data?.data?.let { uri ->
                    // Take a persistent URI permission
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )

                    // Store the URI for later upload
                    documentUris[currentImageType] = uri

                    // Get the filename from the URI
                    val fileName = getFileNameFromUri(uri)

                    // Update the corresponding TextView based on the document type
                    when (currentImageType) {
                        "email_order" -> {
                            tvEmailOrderFileName.text = fileName
                            tvEmailOrderFileName.visibility = View.VISIBLE
                        }
                        "telkomsel_permit" -> {
                            tvTelkomselPermitFileName.text = fileName
                            tvTelkomselPermitFileName.visibility = View.VISIBLE
                        }
                        "tel_partner" -> {
                            tvTelPartnerFileName.text = fileName
                            tvTelPartnerFileName.visibility = View.VISIBLE
                        }
                    }

                    // Update button text
                    updateDocumentButtonStatus(currentImageType, true)

                    showToast("Document selected: $fileName")
                }
            }
        }
    }

    // Add this helper method to get the filename from a URI
    private fun getFileNameFromUri(uri: Uri): String {
        var fileName = "Unknown file"

        // Try to get the display name from content provider
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }
        }

        return fileName
    }

    private fun saveChanges() {
        val lastIssue = etLastIssue.text.toString().trim()
        if (lastIssue.isEmpty()) {
            showToast("Please enter the last issue")
            return
        }

        // Disable the save button to prevent multiple clicks
        btnSaveChanges.isEnabled = false
        showToast("Saving changes...")

        // Update the last issue in Firestore
        updateLastIssue(lastIssue) {
            // After updating last issue, upload any files
            uploadAllFiles {
                // When all operations are complete
                showToast("All changes saved successfully")
                finish()
            }
        }
    }

    private fun updateLastIssue(lastIssue: String, onComplete: () -> Unit) {
        if (documentId.isEmpty()) {
            showToast("Error: Site document ID not found")
            btnSaveChanges.isEnabled = true
            return
        }

        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val issueWithTimestamp = "$currentTime - $lastIssue"

        // Get existing issue history
        firestore.collection("projects").document(documentId)
            .get()
            .addOnSuccessListener { document ->
                val existingData = document.data
                val existingIssueHistory = existingData?.get("lastIssueHistory") as? List<String> ?: listOf()

                // Create updated issue history with new issue at the beginning
                val updatedIssueHistory = mutableListOf(issueWithTimestamp)
                updatedIssueHistory.addAll(existingIssueHistory)

                // Update in Firestore
                firestore.collection("projects").document(documentId)
                    .update(
                        mapOf(
                            "lastIssueHistory" to updatedIssueHistory,
                            "updatedAt" to currentTime
                        )
                    )
                    .addOnSuccessListener {
                        // Continue with file uploads
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        showToast("Error updating issue: ${e.message}")
                        btnSaveChanges.isEnabled = true
                    }
            }
            .addOnFailureListener { e ->
                showToast("Error retrieving site data: ${e.message}")
                btnSaveChanges.isEnabled = true
            }
    }

    private fun uploadAllFiles(onComplete: () -> Unit) {
        var pendingUploads = documentUris.size + imageUris.size

        // If no files to upload, call completion handler immediately
        if (pendingUploads == 0) {
            onComplete()
            return
        }

        val onFileUploadComplete = {
            pendingUploads--
            if (pendingUploads <= 0) {
                onComplete()
            }
        }

        // Upload documents
        for ((docType, uri) in documentUris) {
            uploadDocument(docType, uri, onFileUploadComplete)
        }

        // Upload images
        for ((imageType, uri) in imageUris) {
            uploadImage(imageType, uri, onFileUploadComplete)
        }
    }

    private fun uploadDocument(docType: String, uri: Uri, onComplete: () -> Unit) {
        val storageRef = storage.reference.child("documents/$witel/$siteId/$docType.pdf")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                showToast("Document $docType uploaded successfully")
                onComplete()
            }
            .addOnFailureListener { e ->
                showToast("Error uploading document $docType: ${e.message}")
                onComplete() // Still call complete to ensure we don't block the process
            }
    }

    private fun uploadImage(imageType: String, uri: Uri, onComplete: () -> Unit) {
        val storageRef = storage.reference.child("images/$witel/$siteId/$imageType.jpg")

        storageRef.putFile(uri)
            .addOnSuccessListener {
                showToast("Image $imageType uploaded successfully")
                onComplete()
            }
            .addOnFailureListener { e ->
                showToast("Error uploading image $imageType: ${e.message}")
                onComplete() // Still call complete to ensure we don't block the process
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}