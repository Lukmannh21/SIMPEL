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
    private lateinit var etAccountManagerDate: EditText
    private lateinit var btnQualityControlSignature: Button
    private lateinit var imgQualityControlSignature: ImageView
    private lateinit var etQualityControlDate: EditText
    private lateinit var btnColocationSignature: Button
    private lateinit var imgColocationSignature: ImageView
    private lateinit var etColocationDate: EditText
    private lateinit var btnClientSignature: Button
    private lateinit var imgClientSignature: ImageView
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

        // Populate tables with initial rows
        addBtsAntennaRow("1.1")
        addBtsAntennaRow("1.8")

        addMwAntennaRow("2.1")
        addMwAntennaRow("2.11")

        addAmplifierRow("3.1")
        addAmplifierRow("3.5")
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
        etAccountManagerDate = findViewById(R.id.etAccountManagerDate)
        btnQualityControlSignature = findViewById(R.id.btnQualityControlSignature)
        imgQualityControlSignature = findViewById(R.id.imgQualityControlSignature)
        etQualityControlDate = findViewById(R.id.etQualityControlDate)
        btnColocationSignature = findViewById(R.id.btnColocationSignature)
        imgColocationSignature = findViewById(R.id.imgColocationSignature)
        etColocationDate = findViewById(R.id.etColocationDate)
        btnClientSignature = findViewById(R.id.btnClientSignature)
        imgClientSignature = findViewById(R.id.imgClientSignature)
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

            // Signatures - dates
            accountManagerDate = etAccountManagerDate.text.toString().trim(),
            qualityControlDate = etQualityControlDate.text.toString().trim(),
            colocationDate = etColocationDate.text.toString().trim(),
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

    // Shared function to create and populate a PDF document that matches the provided image layout
    private fun createPdfDocument(cafData: CAFModel): PdfDocument {
        val pdfDocument = PdfDocument()

        // Create a page in A4 portrait format (595 x 842 points)
        val pageInfo = PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Define styling with improved text sizes for better readability
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
            color = Color.parseColor("#DDDDDD") // Lighter gray for better readability
            style = Paint.Style.FILL
        }

        // Start drawing content - margins
        val leftMargin = 40f
        val topMargin = 40f
        val pageWidth = 515f

        // Title centered
        canvas.drawText("Colocation Application", leftMargin + pageWidth/2, topMargin + 12, titlePaint)

        // Draw the top table for dates - 2x3 grid (fixed layout)
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

        // Site Information section
        val siteInfoTop = dateTableTop + 60

        // Row 1: Site ID, Island, Province, City
        canvas.drawRect(leftMargin, siteInfoTop, leftMargin + 100, siteInfoTop + 25, linePaint)
        canvas.drawRect(leftMargin + 100, siteInfoTop, leftMargin + 180, siteInfoTop + 25, linePaint)
        canvas.drawRect(leftMargin + 180, siteInfoTop, leftMargin + 350, siteInfoTop + 25, linePaint)
        canvas.drawRect(leftMargin + 350, siteInfoTop, leftMargin + pageWidth, siteInfoTop + 25, linePaint)

        canvas.drawText("SITE ID:", leftMargin + 5, siteInfoTop + 17, headerPaint)
        canvas.drawText(cafData.siteId, leftMargin + 105, siteInfoTop + 17, textPaint)

        canvas.drawText("Island:", leftMargin + 185, siteInfoTop + 17, headerPaint)
        canvas.drawText(cafData.island, leftMargin + 220, siteInfoTop + 17, textPaint)

        canvas.drawText("Province:", leftMargin + 280, siteInfoTop + 17, headerPaint)
        canvas.drawText(cafData.province, leftMargin + 330, siteInfoTop + 17, textPaint)

        canvas.drawText("City:", leftMargin + 355, siteInfoTop + 17, headerPaint)
        canvas.drawText(cafData.city, leftMargin + 380, siteInfoTop + 17, textPaint)

        // Row 2: Latitude, Longitude, Site Type
        canvas.drawRect(leftMargin, siteInfoTop + 25, leftMargin + 180, siteInfoTop + 50, linePaint)
        canvas.drawRect(leftMargin + 180, siteInfoTop + 25, leftMargin + 350, siteInfoTop + 50, linePaint)
        canvas.drawRect(leftMargin + 350, siteInfoTop + 25, leftMargin + pageWidth, siteInfoTop + 50, linePaint)

        canvas.drawText("Latitude (Decimal/DMS):", leftMargin + 5, siteInfoTop + 42, headerPaint)
        canvas.drawText(cafData.latitude, leftMargin + 120, siteInfoTop + 42, textPaint)

        canvas.drawText("Longitude (Decimal/DMS):", leftMargin + 185, siteInfoTop + 42, headerPaint)
        canvas.drawText(cafData.longitude, leftMargin + 310, siteInfoTop + 42, textPaint)

        canvas.drawText("Site Type:", leftMargin + 355, siteInfoTop + 42, headerPaint)
        canvas.drawText(cafData.siteType, leftMargin + 410, siteInfoTop + 42, textPaint)

        // Row 3: Building Height, Tower Type
        canvas.drawRect(leftMargin, siteInfoTop + 50, leftMargin + 180, siteInfoTop + 75, linePaint)
        canvas.drawRect(leftMargin + 180, siteInfoTop + 50, leftMargin + pageWidth, siteInfoTop + 75, linePaint)

        canvas.drawText("Building Height:", leftMargin + 5, siteInfoTop + 67, headerPaint)
        canvas.drawText(cafData.buildingHeight, leftMargin + 85, siteInfoTop + 67, textPaint)

        canvas.drawText("Tower Type:", leftMargin + 185, siteInfoTop + 67, headerPaint)
        canvas.drawText(cafData.towerType, leftMargin + 250, siteInfoTop + 67, textPaint)

        // Row 4: Tower Height, Tower Extension
        canvas.drawRect(leftMargin, siteInfoTop + 75, leftMargin + 180, siteInfoTop + 100, linePaint)
        canvas.drawRect(leftMargin + 180, siteInfoTop + 75, leftMargin + pageWidth, siteInfoTop + 100, linePaint)

        canvas.drawText("Tower Height:", leftMargin + 5, siteInfoTop + 92, headerPaint)
        canvas.drawText(cafData.towerHeight, leftMargin + 80, siteInfoTop + 92, textPaint)

        canvas.drawText("Tower Extension Required:", leftMargin + 185, siteInfoTop + 92, headerPaint)

        // YES checkbox with clearer marks
        val boxSize = 10f
        canvas.drawRect(leftMargin + 320, siteInfoTop + 86, leftMargin + 320 + boxSize, siteInfoTop + 86 + boxSize, linePaint)
        if (cafData.towerExtensionRequired) {
            // Draw X more clearly
            val x1 = leftMargin + 320
            val y1 = siteInfoTop + 86
            val x2 = leftMargin + 320 + boxSize
            val y2 = siteInfoTop + 86 + boxSize
            canvas.drawLine(x1, y1, x2, y2, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
            canvas.drawLine(x1, y2, x2, y1, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
        }
        canvas.drawText("YES", leftMargin + 335, siteInfoTop + 92, textPaint)

        // NO checkbox
        canvas.drawRect(leftMargin + 360, siteInfoTop + 86, leftMargin + 360 + boxSize, siteInfoTop + 86 + boxSize, linePaint)
        if (!cafData.towerExtensionRequired) {
            // Draw X more clearly
            val x1 = leftMargin + 360
            val y1 = siteInfoTop + 86
            val x2 = leftMargin + 360 + boxSize
            val y2 = siteInfoTop + 86 + boxSize
            canvas.drawLine(x1, y1, x2, y2, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
            canvas.drawLine(x1, y2, x2, y1, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
        }
        canvas.drawText("NO", leftMargin + 375, siteInfoTop + 92, textPaint)

        // Client Information section
        val clientInfoTop = siteInfoTop + 110

        // Row 1: CLIENT, Client Site ID, Client Site Name
        canvas.drawRect(leftMargin, clientInfoTop, leftMargin + 100, clientInfoTop + 25, linePaint)
        canvas.drawRect(leftMargin + 100, clientInfoTop, leftMargin + 250, clientInfoTop + 25, linePaint)
        canvas.drawRect(leftMargin + 250, clientInfoTop, leftMargin + pageWidth, clientInfoTop + 25, linePaint)

        canvas.drawText("CLIENT:", leftMargin + 5, clientInfoTop + 17, headerPaint)
        canvas.drawText(cafData.client, leftMargin + 105, clientInfoTop + 17, textPaint)

        canvas.drawText("Client Site ID:", leftMargin + 185, clientInfoTop + 17, headerPaint)
        canvas.drawText(cafData.clientSiteId, leftMargin + 240, clientInfoTop + 17, textPaint)

        canvas.drawText("Client Site Name:", leftMargin + 255, clientInfoTop + 17, headerPaint)
        canvas.drawText(cafData.clientSiteName, leftMargin + 340, clientInfoTop + 17, textPaint)

        // Row 2: Client Contact, Contact Phone
        canvas.drawRect(leftMargin, clientInfoTop + 25, leftMargin + 250, clientInfoTop + 50, linePaint)
        canvas.drawRect(leftMargin + 250, clientInfoTop + 25, leftMargin + pageWidth, clientInfoTop + 50, linePaint)

        canvas.drawText("This application is valid for 15 calendar days.", leftMargin + 5, clientInfoTop + 35, smallTextPaint)

        canvas.drawText("Client Contact:", leftMargin + 185, clientInfoTop + 42, headerPaint)
        canvas.drawText(cafData.clientContact, leftMargin + 240, clientInfoTop + 42, textPaint)

        canvas.drawText("Contact Phone #:", leftMargin + 355, clientInfoTop + 42, headerPaint)
        canvas.drawText(cafData.contactPhone, leftMargin + 430, clientInfoTop + 42, textPaint)

        // Row 3: SITE ADDRESS
        canvas.drawRect(leftMargin, clientInfoTop + 50, leftMargin + pageWidth, clientInfoTop + 75, linePaint)

        canvas.drawText("SITE ADDRESS:", leftMargin + 5, clientInfoTop + 67, headerPaint)

        // Handle multiline site address
        val addressLines = cafData.siteAddress.split("\n")
        var addressY = clientInfoTop + 67
        for (line in addressLines) {
            canvas.drawText(line, leftMargin + 85, addressY, textPaint)
            addressY += 12
        }

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
            canvas.drawText(item.status, columnStartPositions[1] + 2, rowY + 14, smallTextPaint)
            canvas.drawText(item.height, columnStartPositions[2] + 2, rowY + 14, smallTextPaint)
            canvas.drawText(item.quantity, columnStartPositions[3] + 2, rowY + 14, smallTextPaint)
            canvas.drawText(item.manufacturer, columnStartPositions[4] + 2, rowY + 14, smallTextPaint)
            canvas.drawText(item.model, columnStartPositions[5] + 2, rowY + 14, smallTextPaint)
            canvas.drawText(item.dimensions, columnStartPositions[6] + 2, rowY + 14, smallTextPaint)
            canvas.drawText(item.azimuth, columnStartPositions[7] + 2, rowY + 14, smallTextPaint)
            canvas.drawText(item.cableQuantity, columnStartPositions[8] + 2, rowY + 14, smallTextPaint)
            canvas.drawText(item.cableSize, columnStartPositions[9] + 2, rowY + 14, smallTextPaint)

            // Draw horizontal line after row
            rowY += 20
            canvas.drawLine(leftMargin, rowY, leftMargin + pageWidth, rowY, linePaint)
        }

        // Draw remarks row
        canvas.drawText("Remarks:", leftMargin + 5, rowY + 15, smallTextPaint)
        canvas.drawText(etBtsRemarks.text.toString(), leftMargin + 60, rowY + 15, smallTextPaint)

        // Check if we need a new page for MW Antennas
        if (rowY > 650) {
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

            // Draw Shelter/Power requirements
            drawShelterSection(canvas2, cafData, topMargin + 320, leftMargin, pageWidth,
                grayFillPaint, linePaint, headerPaint, textPaint, smallTextPaint)

            // Draw signature section
            drawSignatureSection(canvas2, cafData, topMargin + 500, leftMargin, pageWidth,
                linePaint, headerPaint, textPaint)

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

            // Draw Shelter/Power requirements
            drawShelterSection(canvas2, cafData, topMargin + 160, leftMargin, pageWidth,
                grayFillPaint, linePaint, headerPaint, textPaint, smallTextPaint)

            // Draw signature section
            drawSignatureSection(canvas2, cafData, topMargin + 340, leftMargin, pageWidth,
                linePaint, headerPaint, textPaint)

            pdfDocument.finishPage(page2)
        }

        return pdfDocument
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
                    canvas.drawText(item.status, columnStartPositions[1] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.height, columnStartPositions[2] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.quantity, columnStartPositions[3] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.manufacturer, columnStartPositions[4] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.model, columnStartPositions[5] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.dimensions, columnStartPositions[6] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.azimuth, columnStartPositions[7] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.cableQuantity, columnStartPositions[8] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.cableSize, columnStartPositions[9] + 2, rowY + 14, smallTextPaint)

                    // Draw horizontal line after row
                    rowY += 20
                    canvas.drawLine(leftMargin, rowY, leftMargin + pageWidth, rowY, linePaint)
                }
            } else if (items[0] is AmplifierItem) {
                for (item in items as List<AmplifierItem>) {
                    canvas.drawText(item.itemNo, columnStartPositions[0] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.status, columnStartPositions[1] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.height, columnStartPositions[2] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.quantity, columnStartPositions[3] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.manufacturer, columnStartPositions[4] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.model, columnStartPositions[5] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.dimensions, columnStartPositions[6] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.azimuth, columnStartPositions[7] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.cableQuantity, columnStartPositions[8] + 2, rowY + 14, smallTextPaint)
                    canvas.drawText(item.cableSize, columnStartPositions[9] + 2, rowY + 14, smallTextPaint)

                    // Draw horizontal line after row
                    rowY += 20
                    canvas.drawLine(leftMargin, rowY, leftMargin + pageWidth, rowY, linePaint)
                }
            }
        }

        // Draw remarks row
        canvas.drawText("Remarks:", leftMargin + 5, rowY + 15, smallTextPaint)
        canvas.drawText(remarks, leftMargin + 60, rowY + 15, smallTextPaint)

        return rowY + 30
    }

    private fun drawShelterSection(
        canvas: Canvas, cafData: CAFModel, startY: Float,
        leftMargin: Float, pageWidth: Float,
        grayPaint: Paint, linePaint: Paint, headerPaint: Paint,
        textPaint: Paint, smallTextPaint: Paint
    ) {
        val yPosition = startY

        // Title with gray background
        val shelterHeaderRect = RectF(leftMargin, yPosition, leftMargin + pageWidth, yPosition + 20)
        canvas.drawRect(shelterHeaderRect, grayPaint)
        canvas.drawRect(shelterHeaderRect, linePaint)
        canvas.drawText("4. SHELTER/POWER REQUIREMENTS", leftMargin + 10, yPosition + 14, headerPaint)

        // Main content area
        val contentRect = RectF(leftMargin, yPosition + 20, leftMargin + pageWidth, yPosition + 120)
        canvas.drawRect(contentRect, linePaint)

        // Equipment type
        canvas.drawText("TYPE OF EQUIPMENT REQUIRED:", leftMargin + 5, yPosition + 35, headerPaint)

        // Draw the checkboxes
        val isIndoor = cafData.equipmentType == "INDOOR"
        val isOutdoor = cafData.equipmentType == "OUTDOOR"
        val isOther = !isIndoor && !isOutdoor
        val boxSize = 10f

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
        canvas.drawRect(leftMargin + 280, yPosition + 30, leftMargin + 280 + boxSize, yPosition + 30 + boxSize, linePaint)
        if (isOutdoor) {
            // Draw X more clearly
            val x1 = leftMargin + 280
            val y1 = yPosition + 30
            val x2 = leftMargin + 280 + boxSize
            val y2 = yPosition + 30 + boxSize
            canvas.drawLine(x1, y1, x2, y2, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
            canvas.drawLine(x1, y2, x2, y1, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
        }
        canvas.drawText("OUTDOOR (CABINET)", leftMargin + 295, yPosition + 35, textPaint)

        // OTHER checkbox
        canvas.drawRect(leftMargin + 400, yPosition + 30, leftMargin + 400 + boxSize, yPosition + 30 + boxSize, linePaint)
        if (isOther) {
            // Draw X more clearly
            val x1 = leftMargin + 400
            val y1 = yPosition + 30
            val x2 = leftMargin + 400 + boxSize
            val y2 = yPosition + 30 + boxSize
            canvas.drawLine(x1, y1, x2, y2, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
            canvas.drawLine(x1, y2, x2, y1, Paint().apply { color = Color.BLACK; strokeWidth = 2f })
        }
        canvas.drawText("OTHER (See Remarks)", leftMargin + 415, yPosition + 35, textPaint)

        // Equipment pad dimensions
        canvas.drawText("EQUIPMENT PAD DIMENSIONS:", leftMargin + 5, yPosition + 55, headerPaint)
        canvas.drawText("Length:", leftMargin + 170, yPosition + 55, textPaint)
        canvas.drawText(cafData.equipmentPadLength + " cm", leftMargin + 210, yPosition + 55, textPaint)
        canvas.drawText("Width:", leftMargin + 280, yPosition + 55, textPaint)
        canvas.drawText(cafData.equipmentPadWidth + " cm", leftMargin + 320, yPosition + 55, textPaint)

        // Electricity requirements
        canvas.drawText("ELECTRICITY REQUIREMENTS:", leftMargin + 5, yPosition + 75, headerPaint)
        canvas.drawText("kVA:", leftMargin + 170, yPosition + 75, textPaint)
        canvas.drawText(cafData.electricityKVA, leftMargin + 200, yPosition + 75, textPaint)
        canvas.drawText("n x A (phases):", leftMargin + 280, yPosition + 75, textPaint)
        canvas.drawText(cafData.electricityPhases, leftMargin + 350, yPosition + 75, textPaint)

        // Genset required
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

        // Draw remarks
        canvas.drawText("Remarks:", leftMargin + 5, yPosition + 115, headerPaint)
        canvas.drawText(cafData.remarks, leftMargin + 60, yPosition + 115, textPaint)

        // Note about drawings
        val drawingRect = RectF(leftMargin, yPosition + 130, leftMargin + pageWidth, yPosition + 150)
        canvas.drawRect(drawingRect, grayPaint)
        canvas.drawRect(drawingRect, linePaint)

        // Center the text
        headerPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("SHELTER DRAWINGS MUST BE ATTACHED", leftMargin + pageWidth/2, yPosition + 144, headerPaint)
        headerPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawSignatureSection(
        canvas: Canvas, cafData: CAFModel, startY: Float,
        leftMargin: Float, pageWidth: Float,
        linePaint: Paint, headerPaint: Paint, textPaint: Paint
    ) {
        val colWidth = pageWidth / 4
        val signatureHeight = 80f

        // Draw signature box with all cells
        canvas.drawRect(leftMargin, startY, leftMargin + pageWidth, startY + signatureHeight, linePaint)

        // Draw horizontal divider
        canvas.drawLine(leftMargin, startY + 20, leftMargin + pageWidth, startY + 20, linePaint)

        // Draw vertical dividers
        for (i in 1..3) {
            canvas.drawLine(
                leftMargin + i * colWidth, startY,
                leftMargin + i * colWidth, startY + signatureHeight,
                linePaint
            )
        }

        // Draw title in each column - centered
        val headerY = startY + 15
        headerPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Account Manager", leftMargin + colWidth/2, headerY, headerPaint)
        canvas.drawText("Quality Control", leftMargin + colWidth + colWidth/2, headerY, headerPaint)
        canvas.drawText("Colocation", leftMargin + 2*colWidth + colWidth/2, headerY, headerPaint)
        canvas.drawText("Client", leftMargin + 3*colWidth + colWidth/2, headerY, headerPaint)
        headerPaint.textAlign = Paint.Align.LEFT

        // Draw signatures - with improved signature drawing
        if (signatureUris.containsKey("accountManager")) {
            drawSignatureImageImproved(canvas, signatureUris["accountManager"]!!,
                leftMargin + 5, startY + 25, colWidth - 10, 40f)
        }

        if (signatureUris.containsKey("qualityControl")) {
            drawSignatureImageImproved(canvas, signatureUris["qualityControl"]!!,
                leftMargin + colWidth + 5, startY + 25, colWidth - 10, 40f)
        }

        if (signatureUris.containsKey("colocation")) {
            drawSignatureImageImproved(canvas, signatureUris["colocation"]!!,
                leftMargin + 2*colWidth + 5, startY + 25, colWidth - 10, 40f)
        }

        if (signatureUris.containsKey("client")) {
            drawSignatureImageImproved(canvas, signatureUris["client"]!!,
                leftMargin + 3*colWidth + 5, startY + 25, colWidth - 10, 40f)
        }

        // Draw dates at bottom of each column
        val dateY = startY + 75
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Date: " + cafData.accountManagerDate, leftMargin + 5, dateY, textPaint)
        canvas.drawText("Date: " + cafData.qualityControlDate, leftMargin + colWidth + 5, dateY, textPaint)
        canvas.drawText("Date: " + cafData.colocationDate, leftMargin + 2*colWidth + 5, dateY, textPaint)
        canvas.drawText("Date: " + cafData.clientDate, leftMargin + 3*colWidth + 5, dateY, textPaint)

        // Draw disclaimer at bottom
        val disclaimer1 = "* The requested Coax Cable size is greater than the specification allowed in the MLA. Lessee must install the cables using a feeder cable clamp to reduce the wind loading effect on the tower."
        val disclaimer2 = "Client acknowledges certain sites require permanent genset (no PLN available) and will be responsible for providing its own permanent power."

        textPaint.textSize = 8f
        canvas.drawText(disclaimer1, leftMargin + 5, startY + signatureHeight + 15, textPaint)
        canvas.drawText(disclaimer2, leftMargin + 5, startY + signatureHeight + 25, textPaint)
        textPaint.textSize = 10f
    }

    // Improved signature drawing method that ensures signatures appear
    private fun drawSignatureImageImproved(canvas: Canvas, uri: Uri, x: Float, y: Float, width: Float, height: Float) {
        try {
            // Get bitmap from URI
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Make sure we have a valid bitmap
            if (bitmap == null) {
                Log.e("CAFActivity", "Failed to decode signature bitmap")
                return
            }

            // Calculate scaling to fit within the designated area while maintaining aspect ratio
            val scale = Math.min(width / bitmap.width, height / bitmap.height)

            // Create matrix for scaling
            val matrix = Matrix()
            matrix.postScale(scale, scale)

            // Create scaled bitmap
            val scaledBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

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
            val paint = Paint()
            paint.color = Color.BLACK
            paint.textSize = 12f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("[Signature]", x + width/2, y + height/2, paint)
        }
    }

    private fun drawTable(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float,
                          rows: Int, cols: Int, paint: Paint) {
        // Draw outer rectangle
        canvas.drawRect(left, top, right, bottom, paint)

        // Draw horizontal lines
        val rowHeight = (bottom - top) / rows
        for (i in 1 until rows) {
            canvas.drawLine(left, top + i * rowHeight, right, top + i * rowHeight, paint)
        }

        // Draw vertical lines
        val colWidth = (right - left) / cols
        for (i in 1 until cols) {
            canvas.drawLine(left + i * colWidth, top, left + i * colWidth, bottom, paint)
        }
    }

    private fun drawCheckbox(canvas: Canvas, x: Float, y: Float, checked: Boolean, paint: Paint) {
        // Draw square
        canvas.drawRect(x, y, x + 10, y + 10, paint)

        // If checked, draw X
        if (checked) {
            canvas.drawLine(x, y, x + 10, y + 10, paint)
            canvas.drawLine(x + 10, y, x, y + 10, paint)
        }
    }

    private fun drawAntennaTable(canvas: Canvas, title: String, items: List<Any>, remarks: String,
                                 startY: Float, leftMargin: Float, pageWidth: Float,
                                 grayPaint: Paint, linePaint: Paint, headerPaint: Paint,
                                 textPaint: Paint, smallTextPaint: Paint): Float {
        var yPosition = startY

        // Title with gray background
        val titleRect = RectF(leftMargin, yPosition, leftMargin + pageWidth, yPosition + 20)
        canvas.drawRect(titleRect, grayPaint)
        canvas.drawRect(titleRect, linePaint)
        canvas.drawText(title, leftMargin + 10, yPosition + 14, headerPaint)
        yPosition += 20f

        // Subtitle
        val subtitleRect = RectF(leftMargin, yPosition, leftMargin + pageWidth, yPosition + 15)
        canvas.drawRect(subtitleRect, grayPaint)
        canvas.drawRect(subtitleRect, linePaint)
        canvas.drawText("ANTENNA DETAILS", leftMargin + (pageWidth/2) - 40, yPosition + 10, smallTextPaint)

        // Draw "CABLES" header
        canvas.drawText("CABLES", leftMargin + pageWidth - 80, yPosition + 10, smallTextPaint)
        yPosition += 15f
        // Table headers
        val columnWidths = floatArrayOf(30f, 40f, 45f, 40f, 80f, 75f, 70f, 40f, 40f, 40f)
        val tableHeaders = arrayOf("Item", "Status", "Height (m)", "Quant", "Manufacturer",
            "Model/Tilting", "Dimensions (mm)", "Azim (°)", "Quant", "Size (\")")

        val headerRect = RectF(leftMargin, yPosition, leftMargin + pageWidth, yPosition + 20)
        canvas.drawRect(headerRect, linePaint)

        var colX = leftMargin
        for (i in tableHeaders.indices) {
            if (i > 0) {
                canvas.drawLine(colX, yPosition, colX, yPosition + 20 + (items.size * 15), linePaint)
            }
            canvas.drawText(tableHeaders[i], colX + 2, yPosition + 14, smallTextPaint)
            colX += columnWidths[i]
        }
        canvas.drawLine(colX, yPosition, colX, yPosition + 20 + (items.size * 15), linePaint)
        yPosition += 20f

        // Draw rows
        if (items.isNotEmpty()) {
            val firstItem = items[0]
            if (firstItem is AntennaItem) {
                for (item in items as List<AntennaItem>) {
                    canvas.drawLine(leftMargin, yPosition, leftMargin + pageWidth, yPosition, linePaint)

                    colX = leftMargin
                    canvas.drawText(item.itemNo, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[0]

                    canvas.drawText(item.status, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[1]

                    canvas.drawText(item.height, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[2]

                    canvas.drawText(item.quantity, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[3]

                    canvas.drawText(item.manufacturer, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[4]

                    canvas.drawText(item.model, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[5]

                    canvas.drawText(item.dimensions, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[6]

                    canvas.drawText(item.azimuth, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[7]

                    canvas.drawText(item.cableQuantity, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[8]

                    canvas.drawText(item.cableSize, colX + 2, yPosition + 10, smallTextPaint)

                    yPosition += 15f
                }
            } else if (firstItem is AmplifierItem) {
                for (item in items as List<AmplifierItem>) {
                    canvas.drawLine(leftMargin, yPosition, leftMargin + pageWidth, yPosition, linePaint)

                    colX = leftMargin
                    canvas.drawText(item.itemNo, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[0]

                    canvas.drawText(item.status, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[1]

                    canvas.drawText(item.height, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[2]

                    canvas.drawText(item.quantity, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[3]

                    canvas.drawText(item.manufacturer, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[4]

                    canvas.drawText(item.model, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[5]

                    canvas.drawText(item.dimensions, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[6]

                    canvas.drawText(item.azimuth, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[7]

                    canvas.drawText(item.cableQuantity, colX + 2, yPosition + 10, smallTextPaint)
                    colX += columnWidths[8]

                    canvas.drawText(item.cableSize, colX + 2, yPosition + 10, smallTextPaint)

                    yPosition += 15f
                }
            }
        }

        // Close the table
        canvas.drawLine(leftMargin, yPosition, leftMargin + pageWidth, yPosition, linePaint)

        // Remarks row
        yPosition += 5f
        canvas.drawText("Remarks:", leftMargin + 5, yPosition + 10, smallTextPaint)
        canvas.drawText(remarks, leftMargin + 60, yPosition + 10, smallTextPaint)

        return yPosition + 20f // Return final Y position
    }

    private fun drawSignatureImage(canvas: Canvas, uri: Uri, x: Float, y: Float, width: Float, height: Float) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)

            // Calculate scaling to fit in the designated area while maintaining aspect ratio
            val scale = Math.min(width / bitmap.width, height / bitmap.height)

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            val scaledBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            // Center the bitmap in the designated area
            val left = x + (width - scaledBitmap.width) / 2
            val top = y + (height - scaledBitmap.height) / 2

            canvas.drawBitmap(scaledBitmap, left, top, null)
        } catch (e: Exception) {
            Log.e("CAFActivity", "Error drawing signature: ${e.message}")
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

        // Reset signature dates
        etAccountManagerDate.text.clear()
        etQualityControlDate.text.clear()
        etColocationDate.text.clear()
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

        // Add initial rows
        addBtsAntennaRow("1.1")
        addBtsAntennaRow("1.2")

        addMwAntennaRow("2.1")
        addMwAntennaRow("2.2")

        addAmplifierRow("3.1")
        addAmplifierRow("3.2")
    }
}