package com.mbkm.telgo

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfDocument.PageInfo
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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



class CAFActivity : AppCompatActivity() {

    // UI components
    private lateinit var tabLayout: TabLayout
    private lateinit var formContainer: View
    private lateinit var searchContainer: View
    private lateinit var btnBack: ImageButton
    private lateinit var tvTodayDate: TextView

    // Basic info fields
    private lateinit var etColoApplicationDate: EditText
    private lateinit var etRevision: EditText
    private lateinit var etSiteId: EditText
    private lateinit var autoCompleteIsland: AutoCompleteTextView
    private lateinit var etProvince: EditText
    private lateinit var etCity: EditText
    private lateinit var etLatitude: EditText
    private lateinit var etLongitude: EditText
    private lateinit var etSiteType: EditText
    private lateinit var etTowerType: EditText
    private lateinit var etBuildingHeight: EditText
    private lateinit var etTowerHeight: EditText
    private lateinit var rgTowerExtension: RadioGroup

    // Client info fields
    private lateinit var etClient: EditText
    private lateinit var etClientSiteId: EditText
    private lateinit var etClientSiteName: EditText
    private lateinit var etClientContact: EditText
    private lateinit var etContactPhone: EditText
    private lateinit var etSiteAddress: EditText

    // BTS Antennas table
    private lateinit var tableBtsAntennas: TableLayout
    private lateinit var btnAddBtsRow: Button
    private lateinit var etBtsRemarks: EditText

    // MW Antennas table
    private lateinit var tableMwAntennas: TableLayout
    private lateinit var btnAddMwRow: Button
    private lateinit var etMwRemarks: EditText

    // Amplifiers table
    private lateinit var tableAmplifiers: TableLayout
    private lateinit var btnAddAmplifierRow: Button
    private lateinit var etAmplifierRemarks: EditText

    // Shelter/Power fields
    private lateinit var rgEquipmentType: RadioGroup
    private lateinit var etEquipmentPadLength: EditText
    private lateinit var etEquipmentPadWidth: EditText
    private lateinit var etElectricityKVA: EditText
    private lateinit var etElectricityPhases: EditText
    private lateinit var rgGensetRequired: RadioGroup
    private lateinit var etGensetLength: EditText
    private lateinit var etGensetWidth: EditText
    private lateinit var etShelterRemarks: EditText
    private lateinit var btnUploadDrawing: Button
    private lateinit var tvDrawingFileName: TextView

    // Signature components
    private lateinit var btnAccountManagerSignature: Button
    private lateinit var imgAccountManagerSignature: ImageView
    private lateinit var etAccountManagerName: EditText // Added signature name field
    private lateinit var etAccountManagerDate: EditText
    private lateinit var btnQualityControlSignature: Button
    private lateinit var imgQualityControlSignature: ImageView
    private lateinit var etQualityControlName: EditText // Added signature name field
    private lateinit var etQualityControlDate: EditText
    private lateinit var btnColocationSignature: Button
    private lateinit var imgColocationSignature: ImageView
    private lateinit var etColocationName: EditText // Added signature name field
    private lateinit var etColocationDate: EditText
    private lateinit var btnClientSignature: Button
    private lateinit var imgClientSignature: ImageView
    private lateinit var etClientName: EditText // Added signature name field
    private lateinit var etClientDate: EditText

    // Submit buttons
    private lateinit var btnSubmitForm: Button
    private lateinit var btnGenerateExcel: Button

    // Search components
    private lateinit var searchView: SearchView
    private lateinit var rvSearchResults: RecyclerView

    // Firebase components
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Data holders
    private val btsAntennaItems = ArrayList<AntennaItem>()
    private val mwAntennaItems = ArrayList<AntennaItem>()
    private val amplifierItems = ArrayList<AmplifierItem>()
    private val signatureUris = HashMap<String, Uri>()
    private var drawingUri: Uri? = null

    // Search results
    private val searchResults = ArrayList<CAFModel>()
    private lateinit var searchAdapter: CAFAdapter

    // Request codes
    private val REQUEST_SIGNATURE_ACCOUNT_MGR = 201
    private val REQUEST_SIGNATURE_QUALITY = 202
    private val REQUEST_SIGNATURE_COLOCATION = 203
    private val REQUEST_SIGNATURE_CLIENT = 204
    private val REQUEST_DRAWING = 205

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_caf)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI components
        initializeUI()
        setupDropdowns()
        setupListeners()

        // Set current date
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        tvTodayDate.text = sdf.format(Date())

        // Populate tables with initial rows (FIXED: sequential numbering)
        addBtsAntennaRow("1.1")
        addBtsAntennaRow("1.2")

        addMwAntennaRow("2.1")
        addMwAntennaRow("2.2")

        addAmplifierRow("3.1")
        addAmplifierRow("3.2")
    }

    private fun initializeUI() {
        // Main layout components
        tabLayout = findViewById(R.id.tabLayout)
        formContainer = findViewById(R.id.formContainer)
        searchContainer = findViewById(R.id.searchContainer)
        btnBack = findViewById(R.id.btnBack)
        tvTodayDate = findViewById(R.id.tvTodayDate)

        // Initialize all form fields
        etColoApplicationDate = findViewById(R.id.etColoApplicationDate)
        etRevision = findViewById(R.id.etRevision)

        // Site info fields
        etSiteId = findViewById(R.id.etSiteId)
        autoCompleteIsland = findViewById(R.id.autoCompleteIsland)
        etProvince = findViewById(R.id.etProvince)
        etCity = findViewById(R.id.etCity)
        etLatitude = findViewById(R.id.etLatitude)
        etLongitude = findViewById(R.id.etLongitude)
        etSiteType = findViewById(R.id.etSiteType)
        etTowerType = findViewById(R.id.etTowerType)
        etBuildingHeight = findViewById(R.id.etBuildingHeight)
        etTowerHeight = findViewById(R.id.etTowerHeight)
        rgTowerExtension = findViewById(R.id.rgTowerExtension)

        // Client info fields
        etClient = findViewById(R.id.etClient)
        etClientSiteId = findViewById(R.id.etClientSiteId)
        etClientSiteName = findViewById(R.id.etClientSiteName)
        etClientContact = findViewById(R.id.etClientContact)
        etContactPhone = findViewById(R.id.etContactPhone)
        etSiteAddress = findViewById(R.id.etSiteAddress)

        // Tables
        tableBtsAntennas = findViewById(R.id.tableBtsAntennas)
        btnAddBtsRow = findViewById(R.id.btnAddBtsRow)
        etBtsRemarks = findViewById(R.id.etBtsRemarks)

        tableMwAntennas = findViewById(R.id.tableMwAntennas)
        btnAddMwRow = findViewById(R.id.btnAddMwRow)
        etMwRemarks = findViewById(R.id.etMwRemarks)

        tableAmplifiers = findViewById(R.id.tableAmplifiers)
        btnAddAmplifierRow = findViewById(R.id.btnAddAmplifierRow)
        etAmplifierRemarks = findViewById(R.id.etAmplifierRemarks)

        // Shelter/Power fields
        rgEquipmentType = findViewById(R.id.rgEquipmentType)
        etEquipmentPadLength = findViewById(R.id.etEquipmentPadLength)
        etEquipmentPadWidth = findViewById(R.id.etEquipmentPadWidth)
        etElectricityKVA = findViewById(R.id.etElectricityKVA)
        etElectricityPhases = findViewById(R.id.etElectricityPhases)
        rgGensetRequired = findViewById(R.id.rgGensetRequired)
        etGensetLength = findViewById(R.id.etGensetLength)
        etGensetWidth = findViewById(R.id.etGensetWidth)
        etShelterRemarks = findViewById(R.id.etShelterRemarks)
        btnUploadDrawing = findViewById(R.id.btnUploadDrawing)
        tvDrawingFileName = findViewById(R.id.tvDrawingFileName)

        // Signature components
        btnAccountManagerSignature = findViewById(R.id.btnAccountManagerSignature)
        imgAccountManagerSignature = findViewById(R.id.imgAccountManagerSignature)
        etAccountManagerName = findViewById(R.id.etAccountManagerName) // Initialize name field
        etAccountManagerDate = findViewById(R.id.etAccountManagerDate)

        btnQualityControlSignature = findViewById(R.id.btnQualityControlSignature)
        imgQualityControlSignature = findViewById(R.id.imgQualityControlSignature)
        etQualityControlName = findViewById(R.id.etQualityControlName) // Initialize name field
        etQualityControlDate = findViewById(R.id.etQualityControlDate)

        btnColocationSignature = findViewById(R.id.btnColocationSignature)
        imgColocationSignature = findViewById(R.id.imgColocationSignature)
        etColocationName = findViewById(R.id.etColocationName) // Initialize name field
        etColocationDate = findViewById(R.id.etColocationDate)

        btnClientSignature = findViewById(R.id.btnClientSignature)
        imgClientSignature = findViewById(R.id.imgClientSignature)
        etClientName = findViewById(R.id.etClientName) // Initialize name field
        etClientDate = findViewById(R.id.etClientDate)

        // Submit buttons
        btnSubmitForm = findViewById(R.id.btnSubmitForm)
        btnGenerateExcel = findViewById(R.id.btnGenerateExcel)

        // Search components
        searchView = findViewById(R.id.searchView)
        rvSearchResults = findViewById(R.id.rvSearchResults)

        // Setup RecyclerView
        setupSearchAdapter()
    }

    private fun setupDropdowns() {
        // Island dropdown
        val islandOptions = listOf("Sumatera", "Jawa", "Kalimantan", "Sulawesi", "Papua", "Bali", "Nusa Tenggara")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, islandOptions)
        autoCompleteIsland.setAdapter(adapter)
    }

    private fun setupListeners() {
        // Tab listener
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
                        loadAllApplications()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Add table row buttons
        btnAddBtsRow.setOnClickListener {
            val nextItemNumber = if (btsAntennaItems.isEmpty()) "1.1" else {
                val lastIdx = btsAntennaItems.last().itemNo.split(".")
                "1.${lastIdx[1].toInt() + 1}"
            }
            addBtsAntennaRow(nextItemNumber)
        }

        btnAddMwRow.setOnClickListener {
            val nextItemNumber = if (mwAntennaItems.isEmpty()) "2.1" else {
                val lastIdx = mwAntennaItems.last().itemNo.split(".")
                "2.${lastIdx[1].toInt() + 1}"
            }
            addMwAntennaRow(nextItemNumber)
        }

        btnAddAmplifierRow.setOnClickListener {
            val nextItemNumber = if (amplifierItems.isEmpty()) "3.1" else {
                val lastIdx = amplifierItems.last().itemNo.split(".")
                "3.${lastIdx[1].toInt() + 1}"
            }
            addAmplifierRow(nextItemNumber)
        }

        // Drawing upload button
        btnUploadDrawing.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/pdf"
            startActivityForResult(intent, REQUEST_DRAWING)
        }

        // Signature buttons
        btnAccountManagerSignature.setOnClickListener {
            openSignatureActivity(REQUEST_SIGNATURE_ACCOUNT_MGR)
        }

        btnQualityControlSignature.setOnClickListener {
            openSignatureActivity(REQUEST_SIGNATURE_QUALITY)
        }

        btnColocationSignature.setOnClickListener {
            openSignatureActivity(REQUEST_SIGNATURE_COLOCATION)
        }

        btnClientSignature.setOnClickListener {
            openSignatureActivity(REQUEST_SIGNATURE_CLIENT)
        }

        // Submit button
        btnSubmitForm.setOnClickListener {
            if (validateForm()) {
                submitForm()
            }
        }

        // Generate PDF button
        btnGenerateExcel.setOnClickListener {
            if (validateForm()) {
                generatePDF()
            }
        }
    }

    private fun setupSearchAdapter() {
        searchAdapter = CAFAdapter(searchResults) { cafModel ->
            // Handle click on search result
            showCafOptionsDialog(cafModel)
        }

        rvSearchResults.layoutManager = LinearLayoutManager(this)
        rvSearchResults.adapter = searchAdapter

        // Setup search functionality
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

    private fun addBtsAntennaRow(itemNo: String) {
        val tableRow = TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }

        // Add Item number (non-editable)
        val itemTextView = TextView(this).apply {
            layoutParams = TableRow.LayoutParams(40, TableRow.LayoutParams.WRAP_CONTENT)
            text = itemNo
            setPadding(4, 4, 4, 4)
            gravity = android.view.Gravity.CENTER
        }
        tableRow.addView(itemTextView)

        // Add editable fields
        val fields = arrayOf(
            createTableEditText(50), // Status
            createTableEditText(60), // Height
            createTableEditText(50), // Quantity
            createTableEditText(100), // Manufacturer
            createTableEditText(100), // Model/Tilting
            createTableEditText(100), // Dimensions
            createTableEditText(70),  // Azimuth
            createTableEditText(50),  // Cable Quantity
            createTableEditText(60)   // Cable Size
        )

        fields.forEach { tableRow.addView(it) }

        // Save item in data list
        val item = AntennaItem(itemNo = itemNo)
        btsAntennaItems.add(item)

        // Add row to table
        tableBtsAntennas.addView(tableRow)
    }

    private fun addMwAntennaRow(itemNo: String) {
        val tableRow = TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }

        // Add Item number (non-editable)
        val itemTextView = TextView(this).apply {
            layoutParams = TableRow.LayoutParams(40, TableRow.LayoutParams.WRAP_CONTENT)
            text = itemNo
            setPadding(4, 4, 4, 4)
            gravity = android.view.Gravity.CENTER
        }
        tableRow.addView(itemTextView)

        // Add editable fields
        val fields = arrayOf(
            createTableEditText(50), // Status
            createTableEditText(60), // Height
            createTableEditText(50), // Quantity
            createTableEditText(100), // Manufacturer
            createTableEditText(100), // Model
            createTableEditText(100), // Diameter
            createTableEditText(70),  // Azimuth
            createTableEditText(50),  // Cable Quantity
            createTableEditText(60)   // Cable Size
        )

        fields.forEach { tableRow.addView(it) }

        // Save item in data list
        val item = AntennaItem(itemNo = itemNo)
        mwAntennaItems.add(item)

        // Add row to table
        tableMwAntennas.addView(tableRow)
    }

    private fun addAmplifierRow(itemNo: String) {
        val tableRow = TableRow(this).apply {
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
        }

        // Add Item number (non-editable)
        val itemTextView = TextView(this).apply {
            layoutParams = TableRow.LayoutParams(40, TableRow.LayoutParams.WRAP_CONTENT)
            text = itemNo
            setPadding(4, 4, 4, 4)
            gravity = android.view.Gravity.CENTER
        }
        tableRow.addView(itemTextView)

        // Add editable fields
        val fields = arrayOf(
            createTableEditText(50), // Status
            createTableEditText(60), // Height
            createTableEditText(50), // Quantity
            createTableEditText(100), // Manufacturer
            createTableEditText(100), // Model
            createTableEditText(100), // Dimensions
            createTableEditText(70),  // Azimuth
            createTableEditText(50),  // Cable Quantity
            createTableEditText(60)   // Cable Size
        )

        fields.forEach { tableRow.addView(it) }

        // Save item in data list
        val item = AmplifierItem(itemNo = itemNo)
        amplifierItems.add(item)

        // Add row to table
        tableAmplifiers.addView(tableRow)
    }

    private fun createTableEditText(width: Int): EditText {
        return EditText(this).apply {
            layoutParams = TableRow.LayoutParams(width, TableRow.LayoutParams.WRAP_CONTENT)
            setPadding(4, 4, 4, 4)
            setBackgroundResource(android.R.drawable.editbox_background)
            minHeight = 40
        }
    }

    private fun openSignatureActivity(requestCode: Int) {
        val intent = Intent(this, SignatureActivity::class.java)
        startActivityForResult(intent, requestCode)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_SIGNATURE_ACCOUNT_MGR -> {
                    handleSignatureResult(data, "accountManager", imgAccountManagerSignature)
                }
                REQUEST_SIGNATURE_QUALITY -> {
                    handleSignatureResult(data, "qualityControl", imgQualityControlSignature)
                }
                REQUEST_SIGNATURE_COLOCATION -> {
                    handleSignatureResult(data, "colocation", imgColocationSignature)
                }
                REQUEST_SIGNATURE_CLIENT -> {
                    handleSignatureResult(data, "client", imgClientSignature)
                }
                REQUEST_DRAWING -> {
                    data?.data?.let { uri ->
                        try {
                            // Create a file in app's files directory
                            val storageDir = File(filesDir, "drawings")
                            if (!storageDir.exists()) {
                                storageDir.mkdirs()
                            }

                            val destinationFile = File(storageDir, "drawing_${System.currentTimeMillis()}.pdf")

                            // Copy the PDF data to our app's storage
                            contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(destinationFile).use { output ->
                                    input.copyTo(output)
                                }
                            }

                            // Store the internal file URI
                            drawingUri = Uri.fromFile(destinationFile)

                            // Update UI
                            tvDrawingFileName.text = "Drawing file selected"
                            tvDrawingFileName.visibility = View.VISIBLE

                        } catch (e: Exception) {
                            Log.e("CAFActivity", "Error copying drawing: ${e.message}")
                            Toast.makeText(this, "Failed to process drawing file", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun handleSignatureResult(data: Intent?, signatureKey: String, imageView: ImageView) {
        val uri = data?.getParcelableExtra<Uri>("signature_uri")
        if (uri != null) {
            try {
                // Create a file in app's files directory
                val storageDir = File(filesDir, "signatures")
                if (!storageDir.exists()) {
                    storageDir.mkdirs()
                }

                val destinationFile = File(storageDir, "${signatureKey}_${System.currentTimeMillis()}.png")

                // Copy the image data to our app's storage
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destinationFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // Store the internal file URI instead of the external one
                val internalUri = Uri.fromFile(destinationFile)
                signatureUris[signatureKey] = internalUri

                // Display the image
                imageView.setImageURI(internalUri)
                imageView.visibility = View.VISIBLE

            } catch (e: Exception) {
                Log.e("CAFActivity", "Error copying signature: ${e.message}")
                Toast.makeText(this, "Failed to process signature", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Check required fields
        if (etSiteId.text.toString().isEmpty()) {
            etSiteId.error = "Required field"
            isValid = false
        }

        if (etProvince.text.toString().isEmpty()) {
            etProvince.error = "Required field"
            isValid = false
        }

        if (etCity.text.toString().isEmpty()) {
            etCity.error = "Required field"
            isValid = false
        }

        if (etLatitude.text.toString().isEmpty()) {
            etLatitude.error = "Required field"
            isValid = false
        }

        if (etLongitude.text.toString().isEmpty()) {
            etLongitude.error = "Required field"
            isValid = false
        }

        if (etSiteType.text.toString().isEmpty()) {
            etSiteType.error = "Required field"
            isValid = false
        }

        if (etClient.text.toString().isEmpty()) {
            etClient.error = "Required field"
            isValid = false
        }

        return isValid
    }

    private fun submitForm() {
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Create data object
        val currentUser = auth.currentUser
        if (currentUser == null) {
            loadingDialog.dismiss()
            Toast.makeText(this, "You must be logged in to submit a form", Toast.LENGTH_SHORT).show()
            return
        }

        // Generate Model from form fields
        val cafData = createCAFModel(currentUser.email ?: "unknown")

        // Add to Firestore
        firestore.collection("caf_applications")
            .add(cafData)
            .addOnSuccessListener { documentReference ->
                val cafId = documentReference.id

                // Upload files
                uploadFilesToFirebase(cafId) { success ->
                    if (success) {
                        // Generate PDF
                        generateAndUploadPdf(cafId, cafData) { pdfSuccess ->
                            loadingDialog.dismiss()

                            if (pdfSuccess) {
                                showSuccessDialog()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Application saved but failed to generate PDF",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } else {
                        loadingDialog.dismiss()
                        Toast.makeText(this, "Failed to upload files", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Error submitting form: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createCAFModel(createdBy: String): CAFModel {
        // Collect data from tables
        collectTableData()

        return CAFModel(
            todayDate = tvTodayDate.text.toString(),
            coloApplicationDate = etColoApplicationDate.text.toString().trim(),
            revision = etRevision.text.toString().trim(),

            // Site Information
            siteId = etSiteId.text.toString().trim(),
            island = autoCompleteIsland.text.toString().trim(),
            province = etProvince.text.toString().trim(),
            city = etCity.text.toString().trim(),
            latitude = etLatitude.text.toString().trim(),
            longitude = etLongitude.text.toString().trim(),
            siteType = etSiteType.text.toString().trim(),
            towerType = etTowerType.text.toString().trim(),
            buildingHeight = etBuildingHeight.text.toString().trim(),
            towerHeight = etTowerHeight.text.toString().trim(),
            towerExtensionRequired = rgTowerExtension.checkedRadioButtonId == R.id.rbTowerExtensionYes,

            // Client Information
            client = etClient.text.toString().trim(),
            clientSiteId = etClientSiteId.text.toString().trim(),
            clientSiteName = etClientSiteName.text.toString().trim(),
            clientContact = etClientContact.text.toString().trim(),
            contactPhone = etContactPhone.text.toString().trim(),
            siteAddress = etSiteAddress.text.toString().trim(),

            // Equipment Details - table data
            btsAntennas = btsAntennaItems,
            mwAntennas = mwAntennaItems,
            amplifiers = amplifierItems,

            // Shelter/Power Requirements
            equipmentType = when (rgEquipmentType.checkedRadioButtonId) {
                R.id.rbEquipmentIndoor -> "INDOOR"
                R.id.rbEquipmentOutdoor -> "OUTDOOR"
                else -> "OTHER"
            },
            equipmentPadLength = etEquipmentPadLength.text.toString().trim(),
            equipmentPadWidth = etEquipmentPadWidth.text.toString().trim(),
            electricityKVA = etElectricityKVA.text.toString().trim(),
            electricityPhases = etElectricityPhases.text.toString().trim(),
            permanentGensetRequired = rgGensetRequired.checkedRadioButtonId == R.id.rbGensetYes,
            gensetLength = etGensetLength.text.toString().trim(),
            gensetWidth = etGensetWidth.text.toString().trim(),
            remarks = etShelterRemarks.text.toString().trim(),

            // Signatures - names and dates (IMPORTANT FIX: Collect names from input fields)
            accountManagerName = etAccountManagerName.text.toString().trim(),
            accountManagerDate = etAccountManagerDate.text.toString().trim(),
            qualityControlName = etQualityControlName.text.toString().trim(),
            qualityControlDate = etQualityControlDate.text.toString().trim(),
            colocationName = etColocationName.text.toString().trim(),
            colocationDate = etColocationDate.text.toString().trim(),
            clientName = etClientName.text.toString().trim(),
            clientDate = etClientDate.text.toString().trim(),

            // Metadata
            createdBy = createdBy,
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
    }

    private fun collectTableData() {
        // Collect BTS Antenna data from table rows
        for (i in 0 until tableBtsAntennas.childCount) {
            val row = tableBtsAntennas.getChildAt(i) as? TableRow ?: continue
            if (row.childCount >= 10) { // Item + 9 fields
                val item = btsAntennaItems[i]
                item.status = (row.getChildAt(1) as EditText).text.toString()
                item.height = (row.getChildAt(2) as EditText).text.toString()
                item.quantity = (row.getChildAt(3) as EditText).text.toString()
                item.manufacturer = (row.getChildAt(4) as EditText).text.toString()
                item.model = (row.getChildAt(5) as EditText).text.toString()
                item.dimensions = (row.getChildAt(6) as EditText).text.toString()
                item.azimuth = (row.getChildAt(7) as EditText).text.toString()
                item.cableQuantity = (row.getChildAt(8) as EditText).text.toString()
                item.cableSize = (row.getChildAt(9) as EditText).text.toString()
            }
        }

        // Collect MW Antenna data similarly
        for (i in 0 until tableMwAntennas.childCount) {
            val row = tableMwAntennas.getChildAt(i) as? TableRow ?: continue
            if (row.childCount >= 10) {
                val item = mwAntennaItems[i]
                item.status = (row.getChildAt(1) as EditText).text.toString()
                item.height = (row.getChildAt(2) as EditText).text.toString()
                item.quantity = (row.getChildAt(3) as EditText).text.toString()
                item.manufacturer = (row.getChildAt(4) as EditText).text.toString()
                item.model = (row.getChildAt(5) as EditText).text.toString()
                item.dimensions = (row.getChildAt(6) as EditText).text.toString()
                item.azimuth = (row.getChildAt(7) as EditText).text.toString()
                item.cableQuantity = (row.getChildAt(8) as EditText).text.toString()
                item.cableSize = (row.getChildAt(9) as EditText).text.toString()
            }
        }

        // Collect Amplifier data similarly
        for (i in 0 until tableAmplifiers.childCount) {
            val row = tableAmplifiers.getChildAt(i) as? TableRow ?: continue
            if (row.childCount >= 10) {
                val item = amplifierItems[i]
                item.status = (row.getChildAt(1) as EditText).text.toString()
                item.height = (row.getChildAt(2) as EditText).text.toString()
                item.quantity = (row.getChildAt(3) as EditText).text.toString()
                item.manufacturer = (row.getChildAt(4) as EditText).text.toString()
                item.model = (row.getChildAt(5) as EditText).text.toString()
                item.dimensions = (row.getChildAt(6) as EditText).text.toString()
                item.azimuth = (row.getChildAt(7) as EditText).text.toString()
                item.cableQuantity = (row.getChildAt(8) as EditText).text.toString()
                item.cableSize = (row.getChildAt(9) as EditText).text.toString()
            }
        }

        // Set remarks
        if (btsAntennaItems.isNotEmpty()) {
            btsAntennaItems.first().remarks = etBtsRemarks.text.toString()
        }

        if (mwAntennaItems.isNotEmpty()) {
            mwAntennaItems.first().remarks = etMwRemarks.text.toString()
        }

        if (amplifierItems.isNotEmpty()) {
            amplifierItems.first().remarks = etAmplifierRemarks.text.toString()
        }
    }

    private fun uploadFilesToFirebase(cafId: String, callback: (Boolean) -> Unit) {
        // Count total uploads needed
        val totalUploads = signatureUris.size + (if(drawingUri != null) 1 else 0)
        var completedUploads = 0
        var failedUploads = 0

        // Handle no files case
        if (totalUploads == 0) {
            callback(true)
            return
        }

        // Upload signatures from internal storage URIs
        for ((key, uri) in signatureUris) {
            // These URIs are now internal file:// URIs, not content:// URIs
            val signatureRef = storage.reference.child("caf_applications/$cafId/signatures/${key}.png")
            signatureRef.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        throw task.exception ?: Exception("Unknown upload error")
                    }
                    signatureRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUrl = task.result.toString()
                        firestore.collection("caf_applications").document(cafId)
                            .update("${key}Signature", downloadUrl)
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

        // Upload drawing if available (also from internal storage URI)
        drawingUri?.let { uri ->
            val drawingRef = storage.reference.child("caf_applications/$cafId/drawing.pdf")
            drawingRef.putFile(uri)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        throw task.exception ?: Exception("Unknown upload error")
                    }
                    drawingRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUrl = task.result.toString()
                        firestore.collection("caf_applications").document(cafId)
                            .update("drawingUrl", downloadUrl)
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

    private fun generatePDF() {
        try {
            val cafData = createCAFModel(auth.currentUser?.email ?: "unknown")

            // Create and get the PDF document
            val pdfDocument = createPdfDocument(cafData)

            // Save to file
            val fileName = "CAF_${cafData.siteId}_${System.currentTimeMillis()}.pdf"
            val filePath = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

            FileOutputStream(filePath).use { fos ->
                pdfDocument.writeTo(fos)
            }

            pdfDocument.close()

            // Open the PDF file
            val uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "com.mbkm.telgo.fileprovider",
                filePath
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            startActivity(Intent.createChooser(intent, "Open PDF file with..."))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to generate PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateAndUploadPdf(cafId: String, cafData: CAFModel, callback: (Boolean) -> Unit) {
        try {
            // Create the PDF document using the shared function
            val pdfDocument = createPdfDocument(cafData)

            // Convert PDF to byte array
            val outputStream = ByteArrayOutputStream()
            pdfDocument.writeTo(outputStream)
            val pdfBytes = outputStream.toByteArray()
            pdfDocument.close()

            // Upload to Firebase Storage
            val pdfRef = storage.reference.child("caf_applications/$cafId/report.pdf")
            pdfRef.putBytes(pdfBytes)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        throw task.exception ?: Exception("Unknown upload error")
                    }
                    pdfRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUrl = task.result.toString()
                        firestore.collection("caf_applications").document(cafId)
                            .update("excelUrl", downloadUrl) // Keep field name for compatibility
                            .addOnSuccessListener {
                                callback(true)
                            }
                            .addOnFailureListener {
                                callback(false)
                            }
                    } else {
                        callback(false)
                    }
                }

        } catch (e: Exception) {
            e.printStackTrace()
            callback(false)
        }
    }

    // Improved PDF document creation function with fixes
    private fun createPdfDocument(cafData: CAFModel): PdfDocument {
        val pdfDocument = PdfDocument()

        // Create a page in A4 portrait format (595 x 842 points)
        val pageInfo = PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Styling definitions
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
        }

        val smallTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 9f
        }

        val linePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        val grayFillPaint = Paint().apply {
            color = Color.parseColor("#DDDDDD")
            style = Paint.Style.FILL
        }

        // Margins and dimensions
        val leftMargin = 40f
        val topMargin = 40f
        val pageWidth = 515f

        // Title centered
        canvas.drawText("Colocation Application", leftMargin + pageWidth/2, topMargin + 12, titlePaint)

        // Draw the top table for dates
        val dateTableTop = topMargin + 25

        // Row 1: Today's Date
        canvas.drawRect(leftMargin, dateTableTop, leftMargin + 150, dateTableTop + 25, linePaint)
        canvas.drawRect(leftMargin + 150, dateTableTop, leftMargin + pageWidth, dateTableTop + 25, linePaint)

        canvas.drawText("Today's Date:", leftMargin + 5, dateTableTop + 17, headerPaint)
        canvas.drawText(cafData.todayDate, leftMargin + 155, dateTableTop + 17, textPaint)

        // Row 2: Colo Application Date and Revision
        canvas.drawRect(leftMargin, dateTableTop + 25, leftMargin + 390, dateTableTop + 50, linePaint)
        canvas.drawRect(leftMargin + 390, dateTableTop + 25, leftMargin + pageWidth, dateTableTop + 50, linePaint)

        canvas.drawText("Colo Application Date:", leftMargin + 5, dateTableTop + 42, headerPaint)
        canvas.drawText(cafData.coloApplicationDate, leftMargin + 155, dateTableTop + 42, textPaint)

        canvas.drawText("Revision:", leftMargin + 395, dateTableTop + 42, headerPaint)
        canvas.drawText(cafData.revision, leftMargin + 445, dateTableTop + 42, textPaint)

        // Site Information section - COMPLETELY FIXED LAYOUT
        val siteInfoTop = dateTableTop + 60

        // Row 1: SITE ID, Island+Province, City
        canvas.drawRect(leftMargin, siteInfoTop, leftMargin + 175, siteInfoTop + 25, linePaint) // SITE ID
        canvas.drawRect(leftMargin + 175, siteInfoTop, leftMargin + 430, siteInfoTop + 25, linePaint) // Province
        canvas.drawRect(leftMargin + 430, siteInfoTop, leftMargin + pageWidth, siteInfoTop + 25, linePaint) // City

        canvas.drawText("SITE ID:", leftMargin + 5, siteInfoTop + 17, headerPaint)
        drawTruncatedText(canvas, cafData.siteId, leftMargin + 65, siteInfoTop + 17, 105f, textPaint)

        canvas.drawText("Province:", leftMargin + 180, siteInfoTop + 17, headerPaint)
        drawTruncatedText(canvas, cafData.province, leftMargin + 235, siteInfoTop + 17, 185f, textPaint)

        canvas.drawText("City:", leftMargin + 435, siteInfoTop + 17, headerPaint)
        drawTruncatedText(canvas, cafData.city, leftMargin + 465, siteInfoTop + 17, 45f, textPaint)

        // Row 2: Latitude, Longitude, Site Type - FIXED LAYOUT WITH CLEAR SEPARATION
        // Row 2: Latitude, Longitude, Site Type - FIXED LAYOUT WITH CLEAR SEPARATION
        canvas.drawRect(leftMargin, siteInfoTop + 25, leftMargin + 175, siteInfoTop + 50, linePaint) // Latitude
        canvas.drawRect(leftMargin + 175, siteInfoTop + 25, leftMargin + 355, siteInfoTop + 50, linePaint) // Longitude
        canvas.drawRect(leftMargin + 355, siteInfoTop + 25, leftMargin + pageWidth, siteInfoTop + 50, linePaint) // Site Type

        // Add latitude back with proper positioning
        canvas.drawText("Latitude:", leftMargin + 5, siteInfoTop + 42, headerPaint)
        // Coba kurangi offset X, misalnya dari 130 menjadi 70 atau 80
        // Sesuaikan juga maxWidth jika perlu agar tidak terpotong atau tumpang tindih dengan Longitude
        drawTruncatedText(canvas, cafData.latitude, leftMargin + 75, siteInfoTop + 42, 95f, textPaint) // X diubah, maxWidth juga disesuaikan

        canvas.drawText("Longitude:", leftMargin + 180, siteInfoTop + 42, headerPaint)
        // Coba kurangi offset X untuk longitude, misalnya dari 305 menjadi 245 atau 255
        // Sesuaikan juga maxWidth agar tidak terpotong
        drawTruncatedText(canvas, cafData.longitude, leftMargin + 250, siteInfoTop + 42, 100f, textPaint) // X diubah, maxWidth juga disesuaikan
        // Clear separation for Site Type
        canvas.drawText("Site Type:", leftMargin + 360, siteInfoTop + 42, headerPaint)
        drawTruncatedText(canvas, cafData.siteType, leftMargin + 410, siteInfoTop + 42, 100f, textPaint)

        // Row 3: Building Height, Tower Type
        canvas.drawRect(leftMargin, siteInfoTop + 50, leftMargin + 260, siteInfoTop + 75, linePaint) // Building Height
        canvas.drawRect(leftMargin + 260, siteInfoTop + 50, leftMargin + pageWidth, siteInfoTop + 75, linePaint) // Tower Type

        canvas.drawText("Building Height:", leftMargin + 5, siteInfoTop + 67, headerPaint)
        drawTruncatedText(canvas, cafData.buildingHeight, leftMargin + 95, siteInfoTop + 67, 160f, textPaint)

        canvas.drawText("Tower Type:", leftMargin + 265, siteInfoTop + 67, headerPaint)
        drawTruncatedText(canvas, cafData.towerType, leftMargin + 335, siteInfoTop + 67, 175f, textPaint)

        // Row 4: Tower Height, Tower Extension
        canvas.drawRect(leftMargin, siteInfoTop + 75, leftMargin + 260, siteInfoTop + 100, linePaint) // Tower Height
        canvas.drawRect(leftMargin + 260, siteInfoTop + 75, leftMargin + pageWidth, siteInfoTop + 100, linePaint) // Tower Extension

        canvas.drawText("Tower Height:", leftMargin + 5, siteInfoTop + 92, headerPaint)
        drawTruncatedText(canvas, cafData.towerHeight, leftMargin + 90, siteInfoTop + 92, 165f, textPaint)

        canvas.drawText("Tower Extension Required:", leftMargin + 265, siteInfoTop + 92, headerPaint)

        // YES checkbox with clearer marks
        val boxSize = 12f
        canvas.drawRect(leftMargin + 410, siteInfoTop + 86, leftMargin + 410 + boxSize, siteInfoTop + 86 + boxSize, linePaint)
        if (cafData.towerExtensionRequired) {
            // Draw X mark
            val x1 = leftMargin + 410
            val y1 = siteInfoTop + 86
            val x2 = leftMargin + 410 + boxSize
            val y2 = siteInfoTop + 86 + boxSize
            canvas.drawLine(x1, y1, x2, y2, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
            canvas.drawLine(x1, y2, x2, y1, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
        }
        canvas.drawText("YES", leftMargin + 425, siteInfoTop + 92, textPaint)

        // NO checkbox
        canvas.drawRect(leftMargin + 455, siteInfoTop + 86, leftMargin + 455 + boxSize, siteInfoTop + 86 + boxSize, linePaint)
        if (!cafData.towerExtensionRequired) {
            // Draw X mark
            val x1 = leftMargin + 455
            val y1 = siteInfoTop + 86
            val x2 = leftMargin + 455 + boxSize
            val y2 = siteInfoTop + 86 + boxSize
            canvas.drawLine(x1, y1, x2, y2, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
            canvas.drawLine(x1, y2, x2, y1, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
        }
        canvas.drawText("NO", leftMargin + 470, siteInfoTop + 92, textPaint)

        // Client Information section - FIXED LAYOUT WITH NO OVERLAPPING TEXT
        val clientInfoTop = siteInfoTop + 110

        // Row 1: CLIENT, Client Site ID, Client Site Name
        canvas.drawRect(leftMargin, clientInfoTop, leftMargin + 175, clientInfoTop + 25, linePaint) // CLIENT
        canvas.drawRect(leftMargin + 175, clientInfoTop, leftMargin + 345, clientInfoTop + 25, linePaint) // Client Site ID
        canvas.drawRect(leftMargin + 345, clientInfoTop, leftMargin + pageWidth, clientInfoTop + 25, linePaint) // Client Site Name

        canvas.drawText("CLIENT:", leftMargin + 5, clientInfoTop + 17, headerPaint)
        drawTruncatedText(canvas, cafData.client, leftMargin + 60, clientInfoTop + 17, 110f, textPaint)

        canvas.drawText("Client Site ID:", leftMargin + 180, clientInfoTop + 17, headerPaint)
        drawTruncatedText(canvas, cafData.clientSiteId, leftMargin + 245, clientInfoTop + 17, 95f, textPaint)

        canvas.drawText("Client Site Name:", leftMargin + 350, clientInfoTop + 17, headerPaint)
        drawTruncatedText(canvas, cafData.clientSiteName, leftMargin + 435, clientInfoTop + 17, 75f, textPaint)

        // Row 2: Client Contact, Contact Phone - with validation text properly placed
        canvas.drawRect(leftMargin, clientInfoTop + 25, leftMargin + 345, clientInfoTop + 50, linePaint) // Client Contact
        canvas.drawRect(leftMargin + 345, clientInfoTop + 25, leftMargin + pageWidth, clientInfoTop + 50, linePaint) // Phone

        // Small validation text in upper part of cell
        canvas.drawText("This application is valid for 15 calendar days.", leftMargin + 5, clientInfoTop + 37, smallTextPaint)

        // Client contact properly placed with adequate spacing from validation text
        canvas.drawText("Client Contact:", leftMargin + 180, clientInfoTop + 42, headerPaint)
        drawTruncatedText(canvas, cafData.clientContact, leftMargin + 250, clientInfoTop + 42, 90f, textPaint)

        canvas.drawText("Contact Phone #:", leftMargin + 350, clientInfoTop + 42, headerPaint)
        drawTruncatedText(canvas, cafData.contactPhone, leftMargin + 430, clientInfoTop + 42, 80f, textPaint)

        // Row 3: SITE ADDRESS
        canvas.drawRect(leftMargin, clientInfoTop + 50, leftMargin + pageWidth, clientInfoTop + 75, linePaint)
        canvas.drawText("SITE ADDRESS:", leftMargin + 5, clientInfoTop + 67, headerPaint)
        drawTruncatedText(canvas, cafData.siteAddress, leftMargin + 95, clientInfoTop + 67, 415f, textPaint)

        // BTS ANTENNAS Table
        val btsTableTop = clientInfoTop + 85

        // Header with gray background
        val btsHeaderRect = RectF(leftMargin, btsTableTop, leftMargin + pageWidth, btsTableTop + 20)
        canvas.drawRect(btsHeaderRect, grayFillPaint)
        canvas.drawRect(btsHeaderRect, linePaint)
        canvas.drawText("1. BTS ANTENNAS (RF)", leftMargin + 10, btsTableTop + 14, headerPaint)

        // Subheader
        val btsSubheaderRect = RectF(leftMargin, btsTableTop + 20, leftMargin + pageWidth, btsTableTop + 35)
        canvas.drawRect(btsSubheaderRect, grayFillPaint)
        canvas.drawRect(btsSubheaderRect, linePaint)

        // Draw ANTENNA DETAILS centered in the first part
        val antennaDetailsWidth = 420f
        canvas.drawText("ANTENNA DETAILS", leftMargin + antennaDetailsWidth/2 - 30, btsTableTop + 32, smallTextPaint)

        // Draw vertical line separating ANTENNA DETAILS and CABLES
        canvas.drawLine(leftMargin + antennaDetailsWidth, btsTableTop + 20,
            leftMargin + antennaDetailsWidth, btsTableTop + 35, linePaint)

        // Draw CABLES centered in the remaining area
        canvas.drawText("CABLES", leftMargin + antennaDetailsWidth + (pageWidth - antennaDetailsWidth)/2 - 15,
            btsTableTop + 32, smallTextPaint)

        // Define column widths for better layout
        val columnWidths = floatArrayOf(28f, 35f, 45f, 35f, 80f, 85f, 70f, 42f, 45f, 50f)
        val columnStartPositions = FloatArray(columnWidths.size)
        var posX = leftMargin
        for (i in columnWidths.indices) {
            columnStartPositions[i] = posX
            posX += columnWidths[i]
        }

        // Draw table headers row
        val headerY = btsTableTop + 35
        val tableHeaders = arrayOf("Item", "Status", "Height (m)", "Quant", "Manufacturer",
            "Model/Tilting", "Dimensions (mm)", "Azim (°)", "Quant", "Size (\")")

        // Draw header cells background
        canvas.drawRect(leftMargin, headerY, leftMargin + pageWidth, headerY + 20, linePaint)

        // Draw header cell text
        for (i in tableHeaders.indices) {
            val x = columnStartPositions[i] + 2
            canvas.drawText(tableHeaders[i], x, headerY + 14, smallTextPaint)

            // Draw vertical lines for columns
            if (i < columnWidths.size) {
                canvas.drawLine(columnStartPositions[i], headerY,
                    columnStartPositions[i], headerY + 20 + (cafData.btsAntennas.size * 20), linePaint)
            }
        }

        // Draw the last vertical line
        canvas.drawLine(leftMargin + pageWidth, headerY,
            leftMargin + pageWidth, headerY + 20 + (cafData.btsAntennas.size * 20), linePaint)
        // Draw horizontal line after headers
        canvas.drawLine(leftMargin, headerY + 20, leftMargin + pageWidth, headerY + 20, linePaint)

        // Draw BTS Antenna rows
        var rowY = headerY + 20
        for (item in cafData.btsAntennas) {
            // Draw row data
            canvas.drawText(item.itemNo, columnStartPositions[0] + 2, rowY + 14, smallTextPaint)

            // Use truncated text drawing for all cells to prevent overflow
            drawTruncatedText(canvas, item.status, columnStartPositions[1] + 2, rowY + 14, columnWidths[1] - 4, smallTextPaint)
            drawTruncatedText(canvas, item.height, columnStartPositions[2] + 2, rowY + 14, columnWidths[2] - 4, smallTextPaint)
            drawTruncatedText(canvas, item.quantity, columnStartPositions[3] + 2, rowY + 14, columnWidths[3] - 4, smallTextPaint)
            drawTruncatedText(canvas, item.manufacturer, columnStartPositions[4] + 2, rowY + 14, columnWidths[4] - 4, smallTextPaint)
            drawTruncatedText(canvas, item.model, columnStartPositions[5] + 2, rowY + 14, columnWidths[5] - 4, smallTextPaint)
            drawTruncatedText(canvas, item.dimensions, columnStartPositions[6] + 2, rowY + 14, columnWidths[6] - 4, smallTextPaint)
            drawTruncatedText(canvas, item.azimuth, columnStartPositions[7] + 2, rowY + 14, columnWidths[7] - 4, smallTextPaint)
            drawTruncatedText(canvas, item.cableQuantity, columnStartPositions[8] + 2, rowY + 14, columnWidths[8] - 4, smallTextPaint)
            drawTruncatedText(canvas, item.cableSize, columnStartPositions[9] + 2, rowY + 14, columnWidths[9] - 4, smallTextPaint)

            // Draw horizontal line after row
            rowY += 20
            canvas.drawLine(leftMargin, rowY, leftMargin + pageWidth, rowY, linePaint)
        }

        // Draw remarks row
        canvas.drawText("Remarks:", leftMargin + 5, rowY + 15, smallTextPaint)
        drawTruncatedText(canvas, etBtsRemarks.text.toString(), leftMargin + 60, rowY + 15, pageWidth - 65, smallTextPaint)

        // Check if we need a new page for MW Antennas
        if (rowY > 600) {
            // Finish first page and create second page
            pdfDocument.finishPage(page)
            val pageInfo2 = PageInfo.Builder(595, 842, 2).create()
            val page2 = pdfDocument.startPage(pageInfo2)
            val canvas2 = page2.canvas

            // Draw MW Antennas table on new page
            drawAntennaTableImproved(canvas2, "2. MW ANTENNAS (Transmission)",
                cafData.mwAntennas, etMwRemarks.text.toString(),
                topMargin, leftMargin, pageWidth, columnWidths,
                grayFillPaint, linePaint, headerPaint, textPaint, smallTextPaint)

            // Draw Amplifiers table
            drawAntennaTableImproved(canvas2, "3. AMPLIFIERS (TMA/ODU)",
                cafData.amplifiers, etAmplifierRemarks.text.toString(),
                topMargin + 160, leftMargin, pageWidth, columnWidths,
                grayFillPaint, linePaint, headerPaint, textPaint, smallTextPaint)

            // Draw Shelter/Power requirements with fixed genset dimensions
            drawShelterSectionFixed(canvas2, cafData, topMargin + 320, leftMargin, pageWidth,
                grayFillPaint, linePaint, headerPaint, textPaint, smallTextPaint)

            // Draw signature section with names
            drawSignatureSectionWithNames(canvas2, cafData, topMargin + 500, leftMargin, pageWidth,
                linePaint, headerPaint, textPaint)

            // Add electronic signature footer
            addElectronicSignatureFooter(canvas2, leftMargin, pageWidth, 790f, textPaint)

            pdfDocument.finishPage(page2)
        } else {
            // Draw MW Antennas table on same page
            val mwTableY = rowY + 30
            drawAntennaTableImproved(canvas, "2. MW ANTENNAS (Transmission)",
                cafData.mwAntennas, etMwRemarks.text.toString(),
                mwTableY, leftMargin, pageWidth, columnWidths,
                grayFillPaint, linePaint, headerPaint, textPaint, smallTextPaint)

            pdfDocument.finishPage(page)

            // Create second page for remaining content
            val pageInfo2 = PageInfo.Builder(595, 842, 2).create()
            val page2 = pdfDocument.startPage(pageInfo2)
            val canvas2 = page2.canvas

            // Draw Amplifiers table
            drawAntennaTableImproved(canvas2, "3. AMPLIFIERS (TMA/ODU)",
                cafData.amplifiers, etAmplifierRemarks.text.toString(),
                topMargin, leftMargin, pageWidth, columnWidths,
                grayFillPaint, linePaint, headerPaint, textPaint, smallTextPaint)

            // Draw Shelter/Power requirements with fixed genset dimensions
            drawShelterSectionFixed(canvas2, cafData, topMargin + 160, leftMargin, pageWidth,
                grayFillPaint, linePaint, headerPaint, textPaint, smallTextPaint)

            // Draw signature section with names
            drawSignatureSectionWithNames(canvas2, cafData, topMargin + 340, leftMargin, pageWidth,
                linePaint, headerPaint, textPaint)

            // Add electronic signature footer
            addElectronicSignatureFooter(canvas2, leftMargin, pageWidth, 790f, textPaint)

            pdfDocument.finishPage(page2)
        }

        return pdfDocument
    }

    // Helper function to truncate text that's too long for a cell
    private fun drawTruncatedText(canvas: Canvas, text: String, x: Float, y: Float, maxWidth: Float, paint: Paint) {
        if (text.isEmpty()) return

        val originalText = text
        var truncText = text
        var textWidth = paint.measureText(truncText)

        // If text fits, just draw it
        if (textWidth <= maxWidth) {
            canvas.drawText(truncText, x, y, paint)
            return
        }

        // Otherwise, truncate it
        var ellipsis = "..."
        var ellipsisWidth = paint.measureText(ellipsis)

        // Find how many characters we can fit
        var truncateAt = truncText.length - 1
        while (truncateAt > 0 && paint.measureText(truncText.substring(0, truncateAt) + ellipsis) > maxWidth) {
            truncateAt--
        }

        // If we can't even fit ellipsis, just truncate as much as possible
        if (truncateAt <= 0) {
            truncateAt = 1
            ellipsis = ""
            ellipsisWidth = 0f
        }

        // Draw the truncated text
        truncText = truncText.substring(0, truncateAt) + ellipsis
        canvas.drawText(truncText, x, y, paint)
    }

    // Add footer for electronic signature
    private fun addElectronicSignatureFooter(canvas: Canvas, leftMargin: Float, pageWidth: Float, bottomY: Float, textPaint: Paint) {
        val footerPaint = Paint(textPaint).apply {
            textAlign = Paint.Align.CENTER
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        canvas.drawText("Dokumen ini telah ditandatangani secara elektronik dan merupakan dokumen sah sesuai ketentuan yang berlaku",
            leftMargin + pageWidth/2, bottomY, footerPaint)
    }

    // Improved antenna table drawing method with better layout
    private fun drawAntennaTableImproved(
        canvas: Canvas, title: String,
        items: List<Any>, remarks: String,
        startY: Float, leftMargin: Float, pageWidth: Float, columnWidths: FloatArray,
        grayPaint: Paint, linePaint: Paint, headerPaint: Paint,
        textPaint: Paint, smallTextPaint: Paint
    ): Float {
        var yPosition = startY

        // Header with gray background
        val headerRect = RectF(leftMargin, yPosition, leftMargin + pageWidth, yPosition + 20)
        canvas.drawRect(headerRect, grayPaint)
        canvas.drawRect(headerRect, linePaint)
        canvas.drawText(title, leftMargin + 10, yPosition + 14, headerPaint)

        // Subheader
        val subheaderRect = RectF(leftMargin, yPosition + 20, leftMargin + pageWidth, yPosition + 35)
        canvas.drawRect(subheaderRect, grayPaint)
        canvas.drawRect(subheaderRect, linePaint)

        // Draw ANTENNA DETAILS centered in the first part
        val antennaDetailsWidth = 420f
        canvas.drawText("ANTENNA DETAILS", leftMargin + antennaDetailsWidth/2 - 30, yPosition + 32, smallTextPaint)

        // Draw vertical line separating ANTENNA DETAILS and CABLES
        canvas.drawLine(leftMargin + antennaDetailsWidth, yPosition + 20,
            leftMargin + antennaDetailsWidth, yPosition + 35, linePaint)

        // Draw CABLES centered in the remaining area
        canvas.drawText("CABLES", leftMargin + antennaDetailsWidth + (pageWidth - antennaDetailsWidth)/2 - 15,
            yPosition + 32, smallTextPaint)

        // Calculate column positions
        val columnStartPositions = FloatArray(columnWidths.size)
        var posX = leftMargin
        for (i in columnWidths.indices) {
            columnStartPositions[i] = posX
            posX += columnWidths[i]
        }

        // Draw table headers row
        val headerY = yPosition + 35
        val tableHeaders = arrayOf("Item", "Status", "Height (m)", "Quant", "Manufacturer",
            "Model/Tilting", "Dimensions (mm)", "Azim (°)", "Quant", "Size (\")")

        // Draw header cells background
        canvas.drawRect(leftMargin, headerY, leftMargin + pageWidth, headerY + 20, linePaint)

        // Draw header cell text
        for (i in tableHeaders.indices) {
            val x = columnStartPositions[i] + 2
            canvas.drawText(tableHeaders[i], x, headerY + 14, smallTextPaint)

            // Draw vertical lines for columns
            if (i < columnWidths.size) {
                canvas.drawLine(columnStartPositions[i], headerY,
                    columnStartPositions[i], headerY + 20 + (items.size * 20), linePaint)
            }
        }

        // Draw the last vertical line
        canvas.drawLine(leftMargin + pageWidth, headerY,
            leftMargin + pageWidth, headerY + 20 + (items.size * 20), linePaint)

        // Draw horizontal line after headers
        canvas.drawLine(leftMargin, headerY + 20, leftMargin + pageWidth, headerY + 20, linePaint)

        // Draw rows
        var rowY = headerY + 20

        if (items.isNotEmpty()) {
            if (items[0] is AntennaItem) {
                for (item in items as List<AntennaItem>) {
                    canvas.drawText(item.itemNo, columnStartPositions[0] + 2, rowY + 14, smallTextPaint)

                    // Use truncated text to prevent overflow
                    drawTruncatedText(canvas, item.status, columnStartPositions[1] + 2, rowY + 14, columnWidths[1] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.height, columnStartPositions[2] + 2, rowY + 14, columnWidths[2] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.quantity, columnStartPositions[3] + 2, rowY + 14, columnWidths[3] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.manufacturer, columnStartPositions[4] + 2, rowY + 14, columnWidths[4] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.model, columnStartPositions[5] + 2, rowY + 14, columnWidths[5] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.dimensions, columnStartPositions[6] + 2, rowY + 14, columnWidths[6] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.azimuth, columnStartPositions[7] + 2, rowY + 14, columnWidths[7] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.cableQuantity, columnStartPositions[8] + 2, rowY + 14, columnWidths[8] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.cableSize, columnStartPositions[9] + 2, rowY + 14, columnWidths[9] - 4, smallTextPaint)

                    // Draw horizontal line after row
                    rowY += 20
                    canvas.drawLine(leftMargin, rowY, leftMargin + pageWidth, rowY, linePaint)
                }
            } else if (items[0] is AmplifierItem) {
                for (item in items as List<AmplifierItem>) {
                    canvas.drawText(item.itemNo, columnStartPositions[0] + 2, rowY + 14, smallTextPaint)

                    // Use truncated text to prevent overflow
                    drawTruncatedText(canvas, item.status, columnStartPositions[1] + 2, rowY + 14, columnWidths[1] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.height, columnStartPositions[2] + 2, rowY + 14, columnWidths[2] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.quantity, columnStartPositions[3] + 2, rowY + 14, columnWidths[3] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.manufacturer, columnStartPositions[4] + 2, rowY + 14, columnWidths[4] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.model, columnStartPositions[5] + 2, rowY + 14, columnWidths[5] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.dimensions, columnStartPositions[6] + 2, rowY + 14, columnWidths[6] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.azimuth, columnStartPositions[7] + 2, rowY + 14, columnWidths[7] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.cableQuantity, columnStartPositions[8] + 2, rowY + 14, columnWidths[8] - 4, smallTextPaint)
                    drawTruncatedText(canvas, item.cableSize, columnStartPositions[9] + 2, rowY + 14, columnWidths[9] - 4, smallTextPaint)

                    // Draw horizontal line after row
                    rowY += 20
                    canvas.drawLine(leftMargin, rowY, leftMargin + pageWidth, rowY, linePaint)
                }
            }
        }

        // Draw remarks row with truncation
        canvas.drawText("Remarks:", leftMargin + 5, rowY + 15, smallTextPaint)
        drawTruncatedText(canvas, remarks, leftMargin + 60, rowY + 15, pageWidth - 65, smallTextPaint)

        return rowY + 30
    }

    // Fixed shelter section with properly displayed genset dimensions
    private fun drawShelterSectionFixed(
        canvas: Canvas, cafData: CAFModel, startY: Float,
        leftMargin: Float, pageWidth: Float,
        grayPaint: Paint, linePaint: Paint, headerPaint: Paint,
        textPaint: Paint, smallTextPaint: Paint
    ) {
        var yPosition = startY

        // Title with gray background
        val shelterHeaderRect = RectF(leftMargin, yPosition, leftMargin + pageWidth, yPosition + 20)
        canvas.drawRect(shelterHeaderRect, grayPaint)
        canvas.drawRect(shelterHeaderRect, linePaint)
        canvas.drawText("4. SHELTER/POWER REQUIREMENTS", leftMargin + 10, yPosition + 14, headerPaint)

        // Main content area - INCREASED HEIGHT for better visibility
        val contentRect = RectF(leftMargin, yPosition + 20, leftMargin + pageWidth, yPosition + 170)
        canvas.drawRect(contentRect, linePaint)

        // Equipment type
        canvas.drawText("TYPE OF EQUIPMENT REQUIRED:", leftMargin + 5, yPosition + 35, headerPaint)

        // Draw the checkboxes
        val isIndoor = cafData.equipmentType == "INDOOR"
        val isOutdoor = cafData.equipmentType == "OUTDOOR"
        val isOther = !isIndoor && !isOutdoor
        val boxSize = 12f

        // INDOOR checkbox
        canvas.drawRect(leftMargin + 170, yPosition + 30, leftMargin + 170 + boxSize, yPosition + 30 + boxSize, linePaint)
        if (isIndoor) {
            // Draw X more clearly
            val x1 = leftMargin + 170
            val y1 = yPosition + 30
            val x2 = leftMargin + 170 + boxSize
            val y2 = yPosition + 30 + boxSize
            canvas.drawLine(x1, y1, x2, y2, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
            canvas.drawLine(x1, y2, x2, y1, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
        }
        canvas.drawText("INDOOR (SHELTER)", leftMargin + 185, yPosition + 35, textPaint)

        // OUTDOOR checkbox
        canvas.drawRect(leftMargin + 300, yPosition + 30, leftMargin + 300 + boxSize, yPosition + 30 + boxSize, linePaint)
        if (isOutdoor) {
            // Draw X more clearly
            val x1 = leftMargin + 300
            val y1 = yPosition + 30
            val x2 = leftMargin + 300 + boxSize
            val y2 = yPosition + 30 + boxSize
            canvas.drawLine(x1, y1, x2, y2, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
            canvas.drawLine(x1, y2, x2, y1, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
        }
        canvas.drawText("OUTDOOR (CABINET)", leftMargin + 315, yPosition + 35, textPaint)

        // OTHER checkbox
        canvas.drawRect(leftMargin + 420, yPosition + 30, leftMargin + 420 + boxSize, yPosition + 30 + boxSize, linePaint)
        if (isOther) {
            // Draw X more clearly
            val x1 = leftMargin + 420
            val y1 = yPosition + 30
            val x2 = leftMargin + 420 + boxSize
            val y2 = yPosition + 30 + boxSize
            canvas.drawLine(x1, y1, x2, y2, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
            canvas.drawLine(x1, y2, x2, y1, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
        }
        canvas.drawText("OTHER (See Remarks)", leftMargin + 435, yPosition + 35, textPaint)

        // Equipment pad dimensions
        canvas.drawText("EQUIPMENT PAD DIMENSIONS:", leftMargin + 5, yPosition + 55, headerPaint)
        canvas.drawText("Length:", leftMargin + 170, yPosition + 55, textPaint)
        canvas.drawText(cafData.equipmentPadLength + " cm", leftMargin + 210, yPosition + 55, textPaint)
        canvas.drawText("Width:", leftMargin + 300, yPosition + 55, textPaint)
        canvas.drawText(cafData.equipmentPadWidth + " cm", leftMargin + 335, yPosition + 55, textPaint)

        // Electricity requirements
        canvas.drawText("ELECTRICITY REQUIREMENTS:", leftMargin + 5, yPosition + 75, headerPaint)
        canvas.drawText("kVA:", leftMargin + 170, yPosition + 75, textPaint)
        canvas.drawText(cafData.electricityKVA, leftMargin + 200, yPosition + 75, textPaint)
        canvas.drawText("n x A (phases):", leftMargin + 300, yPosition + 75, textPaint)
        canvas.drawText(cafData.electricityPhases, leftMargin + 365, yPosition + 75, textPaint)

        // Genset required - IMPROVED VISIBILITY
        canvas.drawText("PERMANENT GENSET REQUIRED:", leftMargin + 5, yPosition + 95, headerPaint)

        // YES checkbox
        canvas.drawRect(leftMargin + 170, yPosition + 90, leftMargin + 170 + boxSize, yPosition + 90 + boxSize, linePaint)
        if (cafData.permanentGensetRequired) {
            val x1 = leftMargin + 170
            val y1 = yPosition + 90
            val x2 = leftMargin + 170 + boxSize
            val y2 = yPosition + 90 + boxSize
            canvas.drawLine(x1, y1, x2, y2, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
            canvas.drawLine(x1, y2, x2, y1, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
        }
        canvas.drawText("YES", leftMargin + 185, yPosition + 95, textPaint)

        // NO checkbox
        canvas.drawRect(leftMargin + 230, yPosition + 90, leftMargin + 230 + boxSize, yPosition + 90 + boxSize, linePaint)
        if (!cafData.permanentGensetRequired) {
            val x1 = leftMargin + 230
            val y1 = yPosition + 90
            val x2 = leftMargin + 230 + boxSize
            val y2 = yPosition + 90 + boxSize
            canvas.drawLine(x1, y1, x2, y2, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
            canvas.drawLine(x1, y2, x2, y1, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
        }
        canvas.drawText("NO", leftMargin + 245, yPosition + 95, textPaint)

        // GENSET DIMENSIONS - HIGHLIGHT AND MAKE CLEARLY VISIBLE
        // Background highlight for genset dimensions
        val gensetRect = RectF(leftMargin + 5, yPosition + 110, leftMargin + 500, yPosition + 130)
        val highlightPaint = Paint().apply {
            color = Color.parseColor("#FFFFCC") // Light yellow highlight
            style = Paint.Style.FILL
        }
        canvas.drawRect(gensetRect, highlightPaint)

        // Draw genset dimensions with bold text for better visibility
        val boldPaint = Paint(headerPaint).apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("GENSET DIMENSIONS:", leftMargin + 5, yPosition + 125, boldPaint)
        canvas.drawText("Length:", leftMargin + 170, yPosition + 125, textPaint)
        canvas.drawText(cafData.gensetLength, leftMargin + 210, yPosition + 125, boldPaint)
        canvas.drawText("Width:", leftMargin + 300, yPosition + 125, textPaint)
        canvas.drawText(cafData.gensetWidth, leftMargin + 335, yPosition + 125, boldPaint)

        // Draw remarks
        canvas.drawText("Remarks:", leftMargin + 5, yPosition + 150, headerPaint)
        drawTruncatedText(canvas, cafData.remarks, leftMargin + 60, yPosition + 150, pageWidth - 65, textPaint)

        // Note about drawings
        val drawingRect = RectF(leftMargin, yPosition + 180, leftMargin + pageWidth, yPosition + 200)
        canvas.drawRect(drawingRect, grayPaint)
        canvas.drawRect(drawingRect, linePaint)

        // Center the text
        headerPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("SHELTER DRAWINGS MUST BE ATTACHED", leftMargin + pageWidth/2, yPosition + 194, headerPaint)
        headerPaint.textAlign = Paint.Align.LEFT
    }

    // Signature section with names that uses input fields
    private fun drawSignatureSectionWithNames(
        canvas: Canvas, cafData: CAFModel, startY: Float,
        leftMargin: Float, pageWidth: Float,
        linePaint: Paint, headerPaint: Paint, textPaint: Paint
    ) {
        val colWidth = pageWidth / 4
        val signatureHeight = 130f

        // Draw main outer rectangle
        canvas.drawRect(leftMargin, startY, leftMargin + pageWidth, startY + signatureHeight, linePaint)

        // Draw header row with gray background
        val headerRect = RectF(leftMargin, startY, leftMargin + pageWidth, startY + 25)
        val grayPaint = Paint().apply {
            color = Color.parseColor("#DDDDDD")
            style = Paint.Style.FILL
        }
        canvas.drawRect(headerRect, grayPaint)
        canvas.drawRect(headerRect, linePaint)

        // Draw horizontal divider after header
        canvas.drawLine(leftMargin, startY + 25, leftMargin + pageWidth, startY + 25, linePaint)

        // Draw vertical dividers for all columns
        for (i in 1..3) {
            // Draw from top to bottom of entire box
            canvas.drawLine(
                leftMargin + i * colWidth, startY,
                leftMargin + i * colWidth, startY + signatureHeight,
                linePaint
            )
        }

        // Draw title in each column - centered
        val headerY = startY + 17
        headerPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Account Manager", leftMargin + colWidth/2, headerY, headerPaint)
        canvas.drawText("Quality Control", leftMargin + colWidth + colWidth/2, headerY, headerPaint)
        canvas.drawText("Colocation", leftMargin + 2*colWidth + colWidth/2, headerY, headerPaint)
        canvas.drawText("Client", leftMargin + 3*colWidth + colWidth/2, headerY, headerPaint)
        headerPaint.textAlign = Paint.Align.LEFT

        // Calculate signature box dimensions
        val signatureBoxHeight = 60f
        val signatureBoxTop = startY + 35
        val signatureBoxBottom = signatureBoxTop + signatureBoxHeight

        // Draw signature boxes with borders
        for (i in 0..3) {
            val left = leftMargin + (i * colWidth) + 10
            val right = leftMargin + ((i + 1) * colWidth) - 10
            canvas.drawRect(left, signatureBoxTop, right, signatureBoxBottom, linePaint)
        }

        // Draw signatures in the boxes
        // Account Manager
        if (signatureUris.containsKey("accountManager")) {
            drawSignatureImageImproved(canvas, signatureUris["accountManager"]!!,
                leftMargin + 10, signatureBoxTop, colWidth - 20, signatureBoxHeight)
        }

        // Quality Control
        if (signatureUris.containsKey("qualityControl")) {
            drawSignatureImageImproved(canvas, signatureUris["qualityControl"]!!,
                leftMargin + colWidth + 10, signatureBoxTop, colWidth - 20, signatureBoxHeight)
        }

        // Colocation
        if (signatureUris.containsKey("colocation")) {
            drawSignatureImageImproved(canvas, signatureUris["colocation"]!!,
                leftMargin + 2*colWidth + 10, signatureBoxTop, colWidth - 20, signatureBoxHeight)
        }

        // Client
        if (signatureUris.containsKey("client")) {
            drawSignatureImageImproved(canvas, signatureUris["client"]!!,
                leftMargin + 3*colWidth + 10, signatureBoxTop, colWidth - 20, signatureBoxHeight)
        }

        // CRITICAL FIX: Draw names using cafData values directly from form input
        // Use bold text for name display
        val boldTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Draw names centered below each signature box
        val nameY = signatureBoxBottom + 15

        // Properly use input fields for names - super important fix!
        canvas.drawText(cafData.accountManagerName, leftMargin + colWidth/2, nameY, boldTextPaint)
        canvas.drawText(cafData.qualityControlName, leftMargin + colWidth + colWidth/2, nameY, boldTextPaint)
        canvas.drawText(cafData.colocationName, leftMargin + 2*colWidth + colWidth/2, nameY, boldTextPaint)
        canvas.drawText(cafData.clientName, leftMargin + 3*colWidth + colWidth/2, nameY, boldTextPaint)

        // Draw "Date:" labels
        val dateY = startY + signatureHeight - 10
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Date:", leftMargin + 5, dateY, textPaint)
        canvas.drawText("Date:", leftMargin + colWidth + 5, dateY, textPaint)
        canvas.drawText("Date:", leftMargin + 2*colWidth + 5, dateY, textPaint)
        canvas.drawText("Date:", leftMargin + 3*colWidth + 5, dateY, textPaint)

        // Draw date values
        canvas.drawText(cafData.accountManagerDate, leftMargin + 35, dateY, textPaint)
        canvas.drawText(cafData.qualityControlDate, leftMargin + colWidth + 35, dateY, textPaint)
        canvas.drawText(cafData.colocationDate, leftMargin + 2*colWidth + 35, dateY, textPaint)
        canvas.drawText(cafData.clientDate, leftMargin + 3*colWidth + 35, dateY, textPaint)
    }

    // Improved signature drawing
    private fun drawSignatureImageImproved(canvas: Canvas, uri: Uri, x: Float, y: Float, width: Float, height: Float) {
        try {
            // Get bitmap from URI using BitmapFactory for better handling
            val inputStream = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (bitmap == null) {
                Log.e("CAFActivity", "Failed to decode signature bitmap")
                // Draw placeholder if bitmap fails
                val paint = Paint().apply {
                    color = Color.BLACK
                    textSize = 12f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("[Signature]", x + width/2, y + height/2, paint)
                return
            }

            // Calculate scaling to fit within the designated area
            val scale = Math.min(width / bitmap.width, height / bitmap.height)

            // Create matrix for scaling
            val matrix = Matrix()
            matrix.postScale(scale, scale)

            // Create scaled bitmap
            val scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            // Calculate position to center the bitmap
            val left = x + (width - scaledBitmap.width) / 2
            val top = y + (height - scaledBitmap.height) / 2

            // Draw the bitmap
            canvas.drawBitmap(scaledBitmap, left, top, Paint())

            // Clean up
            if (bitmap != scaledBitmap) {
                bitmap.recycle()
            }
        } catch (e: Exception) {
            Log.e("CAFActivity", "Error drawing signature: ${e.message}", e)

            // Draw placeholder text as fallback
            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("[Signature]", x + width/2, y + height/2, paint)
        }
    }

    private fun loadAllApplications() {
        // Show loading indicator
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Fetch all CAF documents
        firestore.collection("caf_applications")
            .get()
            .addOnSuccessListener { documents ->
                searchResults.clear()

                for (document in documents) {
                    val data = document.data
                    try {
                        // Create CAF model from document data
                        val caf = CAFModel(
                            id = document.id,
                            siteId = data["siteId"] as? String ?: "",
                            client = data["client"] as? String ?: "",
                            province = data["province"] as? String ?: "",
                            city = data["city"] as? String ?: "",
                            createdAt = data["createdAt"] as? String ?: "",
                            createdBy = data["createdBy"] as? String ?: "",
                            excelUrl = data["excelUrl"] as? String ?: ""
                        )
                        searchResults.add(caf)
                    } catch (e: Exception) {
                        Log.e("CAFActivity", "Error parsing document: ${e.message}")
                    }
                }

                searchAdapter.notifyDataSetChanged()
                loadingDialog.dismiss()
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterSearchResults(query: String) {
        val filteredResults = ArrayList<CAFModel>()

        if (query.isBlank()) {
            // Show all results if query is empty
            filteredResults.addAll(searchResults)
        } else {
            for (caf in searchResults) {
                if (caf.siteId.contains(query, ignoreCase = true) ||
                    caf.client.contains(query, ignoreCase = true) ||
                    caf.province.contains(query, ignoreCase = true) ||
                    caf.city.contains(query, ignoreCase = true)) {

                    filteredResults.add(caf)
                }
            }
        }

        searchAdapter.updateData(filteredResults)
    }

    private fun showCafOptionsDialog(cafModel: CAFModel) {
        val options = arrayOf("View Details", "Download PDF")

        AlertDialog.Builder(this)
            .setTitle("CAF Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewCafDetails(cafModel)
                    1 -> downloadCafExcel(cafModel)
                }
            }
            .show()
    }

    private fun viewCafDetails(cafModel: CAFModel) {
        // Navigate to detail view
        val intent = Intent(this, CAFDetailActivity::class.java)
        intent.putExtra("CAF_ID", cafModel.id)
        startActivity(intent)
    }

    private fun downloadCafExcel(cafModel: CAFModel) {
        if (cafModel.excelUrl.isEmpty()) {
            Toast.makeText(this, "PDF report not available", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Create local file
        val fileName = "CAF_${cafModel.siteId}.pdf"
        val localFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        // Download PDF
        storage.getReferenceFromUrl(cafModel.excelUrl)
            .getFile(localFile)
            .addOnSuccessListener {
                loadingDialog.dismiss()

                // Open the file
                val uri = androidx.core.content.FileProvider.getUriForFile(
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
                Toast.makeText(this, "Error downloading report: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Success")
            .setMessage("Colocation Application Form has been successfully submitted")
            .setPositiveButton("OK") { _, _ ->
                // Reset form
                resetForm()
            }
            .show()
    }

    private fun resetForm() {
        // Clear form fields
        etColoApplicationDate.text.clear()
        etRevision.text.clear()
        etSiteId.text.clear()
        etProvince.text.clear()
        etCity.text.clear()
        etLatitude.text.clear()
        etLongitude.text.clear()
        etSiteType.text.clear()
        etTowerType.text.clear()
        etBuildingHeight.text.clear()
        etTowerHeight.text.clear()
        rgTowerExtension.check(R.id.rbTowerExtensionNo)

        // Client info
        etClient.text.clear()
        etClientSiteId.text.clear()
        etClientSiteName.text.clear()
        etClientContact.text.clear()
        etContactPhone.text.clear()
        etSiteAddress.text.clear()

        // Clear remarks
        etBtsRemarks.text.clear()
        etMwRemarks.text.clear()
        etAmplifierRemarks.text.clear()

        // Reset shelter/power
        rgEquipmentType.check(R.id.rbEquipmentOutdoor)
        etEquipmentPadLength.text.clear()
        etEquipmentPadWidth.text.clear()
        etElectricityKVA.text.clear()
        etElectricityPhases.text.clear()
        rgGensetRequired.check(R.id.rbGensetNo)
        etGensetLength.text.clear()
        etGensetWidth.text.clear()
        etShelterRemarks.text.clear()
        tvDrawingFileName.visibility = View.GONE

        // Reset signature fields and names
        etAccountManagerName.text.clear()
        etAccountManagerDate.text.clear()
        etQualityControlName.text.clear()
        etQualityControlDate.text.clear()
        etColocationName.text.clear()
        etColocationDate.text.clear()
        etClientName.text.clear()
        etClientDate.text.clear()

        // Reset image views
        imgAccountManagerSignature.visibility = View.GONE
        imgQualityControlSignature.visibility = View.GONE
        imgColocationSignature.visibility = View.GONE
        imgClientSignature.visibility = View.GONE

        // Clean up temp files
        fun deleteRecursive(fileOrDirectory: File) {
            if (fileOrDirectory.isDirectory) {
                fileOrDirectory.listFiles()?.forEach { file ->
                    deleteRecursive(file)
                }
            }
            fileOrDirectory.delete()
        }

        try {
            // Clean signature files
            val signaturesDir = File(filesDir, "signatures")
            if (signaturesDir.exists()) {
                deleteRecursive(signaturesDir)
            }

            // Clean drawing files
            val drawingsDir = File(filesDir, "drawings")
            if (drawingsDir.exists()) {
                deleteRecursive(drawingsDir)
            }
        } catch (e: Exception) {
            Log.e("CAFActivity", "Error cleaning temp files: ${e.message}")
        }

        // Clear data structures
        btsAntennaItems.clear()
        mwAntennaItems.clear()
        amplifierItems.clear()
        signatureUris.clear()
        drawingUri = null

        // Reset tables
        tableBtsAntennas.removeAllViews()
        tableMwAntennas.removeAllViews()
        tableAmplifiers.removeAllViews()

        // Add initial rows with sequential numbering (1.1, 1.2, etc.)
        addBtsAntennaRow("1.1")
        addBtsAntennaRow("1.2")

        addMwAntennaRow("2.1")
        addMwAntennaRow("2.2")

        addAmplifierRow("3.1")
        addAmplifierRow("3.2")
    }
}