package com.mbkm.telgo

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputLayout

class UploadProjectActivity : AppCompatActivity() {

    // UI components
    private lateinit var witelDropdown: AutoCompleteTextView
    private lateinit var siteIdInput: EditText
    private lateinit var statusDropdown: AutoCompleteTextView
    private lateinit var lastIssueInput: EditText
    private lateinit var koordinatInput: EditText
    private lateinit var btnCurrentLocation: MaterialButton
    private lateinit var btnAddData: MaterialButton
    private lateinit var btnBack: MaterialButton
    private lateinit var formProgressBar: LinearProgressIndicator

    private lateinit var kodeStoInput: EditText
    private lateinit var namaStoInput: EditText
    private lateinit var portMetroInput: EditText
    private lateinit var sfpInput: EditText
    private lateinit var hostnameInput: EditText
    private lateinit var sizeOltDropdown: AutoCompleteTextView
    private lateinit var platformDropdown: AutoCompleteTextView
    private lateinit var typeDropdown: AutoCompleteTextView
    private lateinit var jmlModulInput: EditText
    private lateinit var siteProviderInput: AutoCompleteTextView
    private lateinit var kecamatanLokasiInput: EditText
    private lateinit var kodeIhldInput: EditText
    private lateinit var lopDownlinkInput: EditText
    private lateinit var kontrakPengadaanInput: EditText
    private lateinit var tocInput: EditText
    private lateinit var startProjectInput: EditText
    private lateinit var catuanAcDropdown: AutoCompleteTextView
    private lateinit var kendalaDropdown: AutoCompleteTextView
    private lateinit var tglPlanOaInput: EditText
    private lateinit var weekPlanOaInput: EditText
    private lateinit var odpInput: EditText
    private lateinit var portInput: EditText
    // New field for Project Type
    private lateinit var projectTypeDropdown: AutoCompleteTextView

    // Multi-select kendala components
    private lateinit var kendalaInputLayout: TextInputLayout
    private lateinit var kendalaChipGroup: ChipGroup
    private val selectedKendalaList = mutableListOf<String>()

    // Firebase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Location services
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    // Calendar for date inputs
    private val calendar = Calendar.getInstance()

    // Dropdown options
    private val witelOptions = listOf(
        "ACEH", "BABEL", "BENGKULU", "JAMBI", "LAMPUNG",
        "RIDAR", "RIKEP", "SUMBAR", "SUMSEL", "SUMUT"
    )

    private val statusOptions = listOf(
        "OA", "MAT DEL", "DONE", "SURVEY", "POWER ON",
        "DROP", "MOS", "INTEGRASI", "DONE SURVEY", "DONE UT", "INSTALL RACK"
    )

    private val sizeOltOptions = listOf(
        "Big XGSPON", "MINI XGSPON", "INSERT CARD"
    )

    private val platformOptions = listOf("HW", "ZTE")

    private val typeOptions = listOf(
        "C600", "C620", "MA5800-X17", "MA5800-X2"
    )

    private val catuanAcOptions = listOf(
        "EKSISTING STO", "EKSISTING TSEL", "PASCABAYAR", "PRABAYAR (PULSA)"
    )

    private val kendalaOptions = listOf(
        "COMMCASE", "NEW PLN", "NO ISSUE", "PERMIT", "PONDASI",
        "RELOC", "SFP BIDI", "WAITING OTN", "WAITING UPLINK", "L2SWITCH", "MIGRASI", "UPGRADE PLN", "BA SURVEY", "TEST 3P", "CORE NODE B", "WAITING REDAMAN",
        "SITAC"
    )

    private val siteProviderOptions = listOf(
        "DMT", "DMT - Bifurcation", "DMT- Reseller", "IBS", "NO NEED SITAC",
        "NOT READY", "PROTELINDO", "PT Centratama Menara Indonesia",
        "PT Gihon Telekomunikasi Indonesia", "PT Quattro International",
        "PT.Era Bangun Towerindo", "PT.Protelindo", "READY", "STO ROOM",
        "STP", "TBG", "TELKOM", "TELKOMSEL", "TSEL", "Camat", "Kades", "Pemko"
    )

    // New options for Project Type
    private val projectTypeOptions = listOf(
        "Project Mitratel", "Project TA"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_project)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize FusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize all UI components
        initializeUI()

        // Set up adapters for dropdowns
        setupDropdowns()

        // Set up multi-select kendala
        setupMultiSelectKendala()

        // Set up date pickers
        setupDatePickers()

        // Set up progress bar listeners
        addProgressBarListeners()

        // Set up current location button
        btnCurrentLocation.setOnClickListener {
            if (checkLocationPermission()) {
                getCurrentLocation()
            }
        }

        // Set up add data button
        btnAddData.setOnClickListener {
            validateAndProceed()
        }

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun initializeUI() {
        // Initialize all UI components by finding them by ID
        formProgressBar = findViewById(R.id.formProgressBar)

        witelDropdown = findViewById(R.id.witelDropdown)
        siteIdInput = findViewById(R.id.siteIdInput)
        statusDropdown = findViewById(R.id.statusDropdown)
        lastIssueInput = findViewById(R.id.lastIssueInput)
        koordinatInput = findViewById(R.id.koordinatInput)
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation)
        btnAddData = findViewById(R.id.btnAddData)
        btnBack = findViewById(R.id.btnBack)

        kodeStoInput = findViewById(R.id.kodeStoInput)
        namaStoInput = findViewById(R.id.namaStoInput)
        portMetroInput = findViewById(R.id.portMetroInput)
        sfpInput = findViewById(R.id.sfpInput)
        hostnameInput = findViewById(R.id.hostnameInput)
        sizeOltDropdown = findViewById(R.id.sizeOltDropdown)
        platformDropdown = findViewById(R.id.platformDropdown)
        typeDropdown = findViewById(R.id.typeDropdown)
        jmlModulInput = findViewById(R.id.jmlModulInput)
        siteProviderInput = findViewById(R.id.siteProviderInput)
        kecamatanLokasiInput = findViewById(R.id.kecamatanLokasiInput)
        kodeIhldInput = findViewById(R.id.kodeIhldInput)
        lopDownlinkInput = findViewById(R.id.lopDownlinkInput)
        kontrakPengadaanInput = findViewById(R.id.kontrakPengadaanInput)
        tocInput = findViewById(R.id.tocInput)
        startProjectInput = findViewById(R.id.startProjectInput)
        catuanAcDropdown = findViewById(R.id.catuanAcDropdown)
        kendalaDropdown = findViewById(R.id.kendalaDropdown)
        tglPlanOaInput = findViewById(R.id.tglPlanOaInput)
        weekPlanOaInput = findViewById(R.id.weekPlanOaInput)
        odpInput = findViewById(R.id.odpInput)
        portInput = findViewById(R.id.portInput)
        // Initialize the new Project Type dropdown
        projectTypeDropdown = findViewById(R.id.projectTypeDropdown)

        // Initialize multi-select kendala components
        kendalaInputLayout = findViewById(R.id.kendalaInputLayout)

        // Create ChipGroup programmatically and add it to the layout
        kendalaChipGroup = ChipGroup(this)
        kendalaChipGroup.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        kendalaChipGroup.visibility = View.GONE // Initially hidden

        // Add ChipGroup below the kendala input layout
        val parentLayout = kendalaInputLayout.parent as LinearLayout
        val kendalaIndex = parentLayout.indexOfChild(kendalaInputLayout)
        parentLayout.addView(kendalaChipGroup, kendalaIndex + 1)
    }

    private fun setupDropdowns() {
        // Set up adapters for all dropdown menus
        val witelAdapter = ArrayAdapter(this, R.layout.dropdown_item, witelOptions)
        witelDropdown.setAdapter(witelAdapter)

        val statusAdapter = ArrayAdapter(this, R.layout.dropdown_item, statusOptions)
        statusDropdown.setAdapter(statusAdapter)

        val sizeOltAdapter = ArrayAdapter(this, R.layout.dropdown_item, sizeOltOptions)
        sizeOltDropdown.setAdapter(sizeOltAdapter)

        val platformAdapter = ArrayAdapter(this, R.layout.dropdown_item, platformOptions)
        platformDropdown.setAdapter(platformAdapter)

        val typeAdapter = ArrayAdapter(this, R.layout.dropdown_item, typeOptions)
        typeDropdown.setAdapter(typeAdapter)

        val catuanAcAdapter = ArrayAdapter(this, R.layout.dropdown_item, catuanAcOptions)
        catuanAcDropdown.setAdapter(catuanAcAdapter)

        val siteProviderAdapter = ArrayAdapter(this, R.layout.dropdown_item, siteProviderOptions)
        siteProviderInput.setAdapter(siteProviderAdapter)

        // Set up adapter for the new Project Type dropdown
        val projectTypeAdapter = ArrayAdapter(this, R.layout.dropdown_item, projectTypeOptions)
        projectTypeDropdown.setAdapter(projectTypeAdapter)
    }

    private fun setupMultiSelectKendala() {
        // Set up adapter for kendala dropdown
        val kendalaAdapter = ArrayAdapter(this, R.layout.dropdown_item, kendalaOptions)
        kendalaDropdown.setAdapter(kendalaAdapter)

        // Handle selection from dropdown
        kendalaDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedKendala = kendalaOptions[position]

            // Add to selected list if not already present
            if (!selectedKendalaList.contains(selectedKendala)) {
                selectedKendalaList.add(selectedKendala)
                addKendalaChip(selectedKendala)
                updateKendalaChipGroupVisibility()
                updateProgressBar()
            }

            // Clear the dropdown selection
            kendalaDropdown.setText("")
        }
    }

    private fun addKendalaChip(kendala: String) {
        val chip = Chip(this)
        chip.text = kendala
        chip.isCloseIconVisible = true
        chip.setChipBackgroundColorResource(R.color.chip_background_color) // Add this color to your colors.xml
        chip.setTextColor(ContextCompat.getColor(this, R.color.chip_text_color)) // Add this color to your colors.xml
        chip.setCloseIconTintResource(R.color.chip_close_icon_color) // Add this color to your colors.xml

        // Handle chip removal
        chip.setOnCloseIconClickListener {
            selectedKendalaList.remove(kendala)
            kendalaChipGroup.removeView(chip)
            updateKendalaChipGroupVisibility()
            updateProgressBar()
        }

        kendalaChipGroup.addView(chip)
    }

    private fun updateKendalaChipGroupVisibility() {
        kendalaChipGroup.visibility = if (selectedKendalaList.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun setupDatePickers() {
        // Set click listeners for date input fields
        tocInput.setOnClickListener {
            showDatePicker(tocInput)
        }

        startProjectInput.setOnClickListener {
            showDatePicker(startProjectInput)
        }

        tglPlanOaInput.setOnClickListener {
            showDatePicker(tglPlanOaInput, true)
        }
    }

    private fun addProgressBarListeners() {
        // Buat daftar semua field yang perlu dipantau
        val fields = listOf(
            siteIdInput, witelDropdown, statusDropdown, koordinatInput,
            kodeStoInput, namaStoInput, portMetroInput, sfpInput,
            hostnameInput, sizeOltDropdown, platformDropdown, typeDropdown,
            jmlModulInput, siteProviderInput, kecamatanLokasiInput,
            kodeIhldInput, lopDownlinkInput, kontrakPengadaanInput,
            tocInput, startProjectInput, catuanAcDropdown,
            tglPlanOaInput, odpInput, portInput, lastIssueInput,
            projectTypeDropdown // Add the new Project Type field to the list
        )

        // Jumlah total field (termasuk kendala multi-select)
        val totalFields = fields.size + 1 // +1 for kendala multi-select

        // Tambahkan TextWatcher ke setiap field
        fields.forEach { field ->
            when (field) {
                is EditText -> {
                    field.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            updateProgressBar(fields, totalFields)
                        }
                    })
                }
                is AutoCompleteTextView -> {
                    field.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            updateProgressBar(fields, totalFields)
                        }
                    })
                }
            }
        }
    }

    private fun updateProgressBar(fields: List<Any> = listOf(), totalFields: Int = 0) {
        // If called without parameters, get current field list
        val currentFields = if (fields.isEmpty()) {
            listOf(
                siteIdInput, witelDropdown, statusDropdown, koordinatInput,
                kodeStoInput, namaStoInput, portMetroInput, sfpInput,
                hostnameInput, sizeOltDropdown, platformDropdown, typeDropdown,
                jmlModulInput, siteProviderInput, kecamatanLokasiInput,
                kodeIhldInput, lopDownlinkInput, kontrakPengadaanInput,
                tocInput, startProjectInput, catuanAcDropdown,
                tglPlanOaInput, odpInput, portInput, lastIssueInput,
                projectTypeDropdown // Add the new Project Type field here too
            )
        } else fields

        val currentTotalFields = if (totalFields == 0) currentFields.size + 1 else totalFields

        var filledFields = 0

        // Hitung berapa field yang sudah diisi
        currentFields.forEach { field ->
            when (field) {
                is EditText -> {
                    if (field.text.toString().isNotEmpty()) filledFields++
                }
                is AutoCompleteTextView -> {
                    if (field.text.toString().isNotEmpty()) filledFields++
                }
            }
        }

        // Add kendala multi-select to filled count
        if (selectedKendalaList.isNotEmpty()) filledFields++

        // Update progress bar
        val progressPercentage = (filledFields * 100) / currentTotalFields
        formProgressBar.progress = progressPercentage
    }

    private fun showDatePicker(dateInput: EditText, isPlanOa: Boolean = false) {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateInput.setText(dateFormat.format(calendar.time))

            // If this is the Plan OA date, also update the Week Plan OA field
            if (isPlanOa) {
                updateWeekPlanOa(calendar)
            }
        }

        // Use Material DatePicker with animation
        val datePickerDialog = DatePickerDialog(
            this,
            R.style.DatePickerDialogTheme, // Add this style to your styles.xml
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Add animation to the dialog
        datePickerDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        datePickerDialog.show()
    }

    private fun updateWeekPlanOa(calendar: Calendar) {
        val month = when (calendar.get(Calendar.MONTH)) {
            Calendar.JANUARY -> "Jan"
            Calendar.FEBRUARY -> "Feb"
            Calendar.MARCH -> "Mar"
            Calendar.APRIL -> "Apr"
            Calendar.MAY -> "May"
            Calendar.JUNE -> "Jun"
            Calendar.JULY -> "Jul"
            Calendar.AUGUST -> "Aug"
            Calendar.SEPTEMBER -> "Sep"
            Calendar.OCTOBER -> "Oct"
            Calendar.NOVEMBER -> "Nov"
            Calendar.DECEMBER -> "Dec"
            else -> ""
        }

        // Calculate week of month (W1, W2, W3, W4, W5)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val weekOfMonth = when {
            dayOfMonth <= 7 -> "W1"
            dayOfMonth <= 14 -> "W2"
            dayOfMonth <= 21 -> "W3"
            dayOfMonth <= 28 -> "W4"
            else -> "W5"
        }

        // Format "Mar W2"
        val weekPlan = "$month $weekOfMonth"
        weekPlanOaInput.setText(weekPlan)
    }

    private fun validateAndProceed() {
        // Animated effect on the add button
        btnAddData.isEnabled = false
        btnAddData.alpha = 0.7f

        // Get all input values
        val witel = witelDropdown.text.toString()
        val siteId = siteIdInput.text.toString().trim()
        val status = statusDropdown.text.toString()
        val lastIssue = lastIssueInput.text.toString().trim()
        val koordinat = koordinatInput.text.toString().trim()

        val kodeSto = kodeStoInput.text.toString().trim()
        val namaSto = namaStoInput.text.toString().trim()
        val portMetro = portMetroInput.text.toString().trim()
        val sfp = sfpInput.text.toString().trim()
        val hostname = hostnameInput.text.toString().trim()
        val sizeOlt = sizeOltDropdown.text.toString()
        val platform = platformDropdown.text.toString()
        val type = typeDropdown.text.toString()
        val jmlModul = jmlModulInput.text.toString().trim()
        val siteProvider = siteProviderInput.text.toString()
        val kecamatanLokasi = kecamatanLokasiInput.text.toString().trim()
        val kodeIhld = kodeIhldInput.text.toString().trim()
        val lopDownlink = lopDownlinkInput.text.toString().trim()
        val kontrakPengadaan = kontrakPengadaanInput.text.toString().trim()
        val toc = tocInput.text.toString().trim()
        val startProject = startProjectInput.text.toString().trim()
        val catuanAc = catuanAcDropdown.text.toString()
        // Get the new Project Type value
        val projectType = projectTypeDropdown.text.toString()

        // Get selected kendala as comma-separated string
        val kendala = selectedKendalaList.joinToString(", ")

        val tglPlanOa = tglPlanOaInput.text.toString().trim()
        val weekPlanOa = weekPlanOaInput.text.toString().trim()
        val odp = odpInput.text.toString().trim()
        val port = portInput.text.toString().trim()

        // IMPORTANT: Check user verification status first
        val preferences = getSharedPreferences("TelGoPrefs", MODE_PRIVATE)
        val userStatus = preferences.getString("userStatus", "unverified") ?: "unverified"
        val userRole = preferences.getString("userRole", "user") ?: "user"

        // If user is not verified and not an admin, show verification required dialog
        if (userStatus != "verified" && userRole != "admin") {
            // Re-enable the button
            btnAddData.isEnabled = true
            btnAddData.alpha = 1.0f

            // Show verification required dialog
            showVerificationRequiredDialog()
            return
        }

        // Validasi untuk siteId (wajib diisi)
        if (siteId.isEmpty()) {
            showError(siteIdInput, "Site ID Location tidak boleh kosong")
            btnAddData.isEnabled = true
            btnAddData.alpha = 1.0f
            return
        }

        // Validasi untuk witel (wajib dipilih)
        if (witel.isEmpty()) {
            showError(witelDropdown, "Witel harus dipilih")
            btnAddData.isEnabled = true
            btnAddData.alpha = 1.0f
            return
        }

        // Langsung lanjut ke pengecekan Site ID di database
        checkSiteIdExists(siteId, witel, status, lastIssue, koordinat, kodeSto, namaSto,
            portMetro, sfp, hostname, sizeOlt, platform, type, jmlModul,
            siteProvider, kecamatanLokasi, kodeIhld, lopDownlink, kontrakPengadaan,
            toc, startProject, catuanAc, kendala, tglPlanOa, weekPlanOa, odp, port,
            projectType) // Pass the new Project Type field
    }

    // Add this new method to show the verification required dialog
    private fun showVerificationRequiredDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Verifikasi Diperlukan")
            .setMessage("Akun Anda memerlukan verifikasi oleh administrator sebelum dapat menambahkan atau mengubah data proyek. Ini memastikan kualitas dan keamanan data.")
            .setIcon(R.drawable.ic_image_error) // Make sure you have this icon or use another appropriate one
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Lihat Profil") { dialog, _ ->
                dialog.dismiss()
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }

        val dialog = builder.create()
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
    }

    private fun showError(input: EditText, message: String) {
        input.error = message
        input.requestFocus()
        showToast(message)
    }

    private fun showError(input: AutoCompleteTextView, message: String) {
        input.error = message
        input.requestFocus()
        showToast(message)
    }

    private fun checkSiteIdExists(
        siteId: String, witel: String, status: String, lastIssue: String, koordinat: String,
        kodeSto: String, namaSto: String, portMetro: String, sfp: String, hostname: String,
        sizeOlt: String, platform: String, type: String, jmlModul: String,
        siteProvider: String, kecamatanLokasi: String, kodeIhld: String, lopDownlink: String,
        kontrakPengadaan: String, toc: String, startProject: String, catuanAc: String,
        kendala: String, tglPlanOa: String, weekPlanOa: String, odp: String, port: String,
        projectType: String // Add the new Project Type parameter
    ) {
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        loadingDialog.show()

        firestore.collection("projects")
            .whereEqualTo("siteId", siteId)
            .get()
            .addOnSuccessListener { documents ->
                loadingDialog.dismiss()
                btnAddData.isEnabled = true
                btnAddData.alpha = 1.0f

                if (documents.isEmpty) {
                    // Site ID doesn't exist, show confirmation dialog for new data
                    showConfirmationDialog(
                        siteId = siteId,
                        witel = witel,
                        status = status,
                        lastIssue = lastIssue,
                        koordinat = koordinat,
                        kodeSto = kodeSto,
                        namaSto = namaSto,
                        portMetro = portMetro,
                        sfp = sfp,
                        hostname = hostname,
                        sizeOlt = sizeOlt,
                        platform = platform,
                        type = type,
                        jmlModul = jmlModul,
                        siteProvider = siteProvider,
                        kecamatanLokasi = kecamatanLokasi,
                        kodeIhld = kodeIhld,
                        lopDownlink = lopDownlink,
                        kontrakPengadaan = kontrakPengadaan,
                        toc = toc,
                        startProject = startProject,
                        catuanAc = catuanAc,
                        kendala = kendala,
                        tglPlanOa = tglPlanOa,
                        weekPlanOa = weekPlanOa,
                        odp = odp,
                        port = port,
                        projectType = projectType, // Add the new Project Type field
                        isNewProject = true
                    )
                } else {
                    // Site ID already exists - show error message instead of edit dialog
                    showSiteIdExistsErrorDialog(siteId)
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                btnAddData.isEnabled = true
                btnAddData.alpha = 1.0f
                showToast("Error: ${e.message}")
            }
    }

    // New method to show error when site ID already exists
    private fun showSiteIdExistsErrorDialog(siteId: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Site ID Sudah Ada")
            .setMessage("Site ID '$siteId' sudah terdaftar dalam sistem. Tidak dapat menambahkan proyek dengan ID yang sama.")
            .setIcon(R.drawable.ic_image_error)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()
    }

    // Add new parameter to the showConfirmationDialog method
    private fun showConfirmationDialog(
        siteId: String, witel: String, status: String, lastIssue: String, koordinat: String,
        kodeSto: String, namaSto: String, portMetro: String, sfp: String, hostname: String,
        sizeOlt: String, platform: String, type: String, jmlModul: String,
        siteProvider: String, kecamatanLokasi: String, kodeIhld: String, lopDownlink: String,
        kontrakPengadaan: String, toc: String, startProject: String, catuanAc: String,
        kendala: String, tglPlanOa: String, weekPlanOa: String, odp: String, port: String,
        projectType: String, // Add the new Project Type field
        isNewProject: Boolean, existingProjectId: String = ""
    ) {
        val title = if (isNewProject) "Konfirmasi Data Baru" else "Edit Data Projek"
        val message = buildString {
            append("Witel: $witel\n")
            append("Site ID: $siteId\n")
            append("Status: $status\n")
            append("Kode STO: $kodeSto\n")
            if (sizeOlt.isNotEmpty()) append("Size OLT: $sizeOlt\n")
            if (platform.isNotEmpty()) append("Platform: $platform\n")
            if (type.isNotEmpty()) append("Type: $type\n")
            if (jmlModul.isNotEmpty()) append("Jumlah Modul: $jmlModul\n")
            if (siteProvider.isNotEmpty()) append("Site Provider: $siteProvider\n")
            if (kodeIhld.isNotEmpty()) append("Kode IHLD: $kodeIhld\n")
            if (toc.isNotEmpty()) append("TOC: $toc\n")
            if (startProject.isNotEmpty()) append("Start Project: $startProject\n")
            if (kendala.isNotEmpty()) append("Kendala: $kendala\n")
            if (projectType.isNotEmpty()) append("Project Type: $projectType\n") // Display Project Type in confirmation
            append("\n")

            append(if (isNewProject)
                "Apakah data di atas sudah benar?"
            else
                "Anda akan mengedit data yang sudah ada. Lanjutkan?")
        }

        val alertDialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Submit") { _, _ ->
                // Show loading animation
                val loadingDialog = AlertDialog.Builder(this)
                    .setView(R.layout.dialog_loading)
                    .setCancelable(false)
                    .create()
                loadingDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
                loadingDialog.show()

                if (isNewProject) {
                    saveNewProject(
                        siteId, witel, status, lastIssue, koordinat, kodeSto, namaSto,
                        portMetro, sfp, hostname, sizeOlt, platform, type, jmlModul,
                        siteProvider, kecamatanLokasi, kodeIhld, lopDownlink, kontrakPengadaan,
                        toc, startProject, catuanAc, kendala, tglPlanOa, weekPlanOa, odp, port,
                        projectType, // Add the new Project Type field
                        onSuccess = {
                            loadingDialog.dismiss()
                        },
                        onFailure = { errorMessage ->
                            loadingDialog.dismiss()
                            showToast(errorMessage)
                        }
                    )
                } else {
                    updateExistingProject(
                        existingProjectId, siteId, witel, status, lastIssue, koordinat, kodeSto, namaSto,
                        portMetro, sfp, hostname, sizeOlt, platform, type, jmlModul,
                        siteProvider, kecamatanLokasi, kodeIhld, lopDownlink, kontrakPengadaan,
                        toc, startProject, catuanAc, kendala, tglPlanOa, weekPlanOa, odp, port,
                        projectType, // Add the new Project Type field
                        onSuccess = {
                            loadingDialog.dismiss()
                        },
                        onFailure = { errorMessage ->
                            loadingDialog.dismiss()
                            showToast(errorMessage)
                        }
                    )
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        // Use animation for dialog
        alertDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        alertDialog.show()
    }

    // Add new function to update the user's edit history
    private fun updateUserEditHistory(userEmail: String, siteId: String, onComplete: () -> Unit) {
        // Get user document by email
        firestore.collection("users")
            .whereEqualTo("email", userEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // User document not found, complete anyway
                    onComplete()
                    return@addOnSuccessListener
                }

                val userDoc = documents.documents[0]
                val userId = userDoc.id

                // Get current list of edited sites or create new one
                var editedSites = userDoc.get("editedSites") as? MutableList<String> ?: mutableListOf()

                // Check if this site is already in the list
                if (!editedSites.contains(siteId)) {
                    // Add site if not already in the list
                    editedSites.add(siteId)

                    // Update user document with the new list
                    firestore.collection("users").document(userId)
                        .update("editedSites", editedSites)
                        .addOnSuccessListener {
                            // Continue to next step
                            onComplete()
                        }
                        .addOnFailureListener { e ->
                            // Log error but continue anyway to not block the flow
                            Log.e("UploadProjectActivity", "Error updating user's edit history: ${e.message}")
                            onComplete()
                        }
                } else {
                    // Site already in the list, just complete
                    onComplete()
                }
            }
            .addOnFailureListener { e ->
                // Log error but continue anyway to not block the flow
                Log.e("UploadProjectActivity", "Error finding user document: ${e.message}")
                onComplete()
            }
    }

    // Update saveNewProject method to include the new Project Type field
    private fun saveNewProject(
        siteId: String, witel: String, status: String, lastIssue: String, koordinat: String,
        kodeSto: String, namaSto: String, portMetro: String, sfp: String, hostname: String,
        sizeOlt: String, platform: String, type: String, jmlModul: String,
        siteProvider: String, kecamatanLokasi: String, kodeIhld: String, lopDownlink: String,
        kontrakPengadaan: String, toc: String, startProject: String, catuanAc: String,
        kendala: String, tglPlanOa: String, weekPlanOa: String, odp: String, port: String,
        projectType: String, // Add the new Project Type parameter
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onFailure("Anda harus login terlebih dahulu")
            return
        }

        // Get current user's email
        val userEmail = currentUser.email ?: "unknown@email.com"

        // Use provided timestamp for testing, or get current time in production
        // val currentTime = getCurrentDateTime()
        val currentTime = "2025-04-28 07:40:37" // Using provided time from prompt
        val issueWithTimestamp = "$currentTime - $lastIssue"

        // Compute calculated fields
        val idLopOlt = calculateIdLopOlt(platform, kontrakPengadaan, kodeSto, sizeOlt, jmlModul, siteId, kodeIhld, status)
        val durasiPekerjaan = calculateDurasiPekerjaan(status, tglPlanOa, startProject)
        val sisaHariThdpPlanOa = calculateSisaHariThdpPlanOa(status, tglPlanOa)
        val sisaHariThdpToc = calculateSisaHariThdpToc(status, toc)

        val projectData = hashMapOf(
            // Original fields
            "siteId" to siteId,
            "witel" to witel,
            "status" to status,
            "lastIssueHistory" to listOf(issueWithTimestamp),
            "koordinat" to koordinat,
            "uploadedBy" to userEmail, // Using logged-in user's email
            "createdAt" to currentTime,
            "updatedAt" to currentTime,
            "editHistory" to listOf("$currentTime - Created by $userEmail"), // Track edit history

            // New fields
            "kodeSto" to kodeSto,
            "namaSto" to namaSto,
            "portMetro" to portMetro,
            "sfp" to sfp,
            "hostname" to hostname,
            "sizeOlt" to sizeOlt,
            "platform" to platform,
            "type" to type,
            "jmlModul" to jmlModul,
            "siteProvider" to siteProvider,
            "kecamatanLokasi" to kecamatanLokasi,
            "kodeIhld" to kodeIhld,
            "lopDownlink" to lopDownlink,
            "kontrakPengadaan" to kontrakPengadaan,
            "toc" to toc,
            "startProject" to startProject,
            "catuanAc" to catuanAc,
            "kendala" to kendala,
            "tglPlanOa" to tglPlanOa,
            "weekPlanOa" to weekPlanOa,
            "odp" to odp,
            "port" to port,
            "projectType" to projectType, // Add the new Project Type field

            // Calculated fields
            "idLopOlt" to idLopOlt,
            "durasiPekerjaan" to durasiPekerjaan,
            "sisaHariThdpPlanOa" to sisaHariThdpPlanOa,
            "sisaHariThdpToc" to sisaHariThdpToc
        )

        firestore.collection("projects")
            .add(projectData)
            .addOnSuccessListener {
                // Update user's edit history
                updateUserEditHistory(userEmail, siteId) {
                    showSuccessDialog("Data berhasil disimpan")
                    onSuccess()
                }
            }
            .addOnFailureListener { e ->
                onFailure("Error: ${e.message}")
            }
    }

    // Update updateExistingProject method to include the new Project Type field
    private fun updateExistingProject(
        projectId: String, siteId: String, witel: String, status: String, lastIssue: String, koordinat: String,
        kodeSto: String, namaSto: String, portMetro: String, sfp: String, hostname: String,
        sizeOlt: String, platform: String, type: String, jmlModul: String,
        siteProvider: String, kecamatanLokasi: String, kodeIhld: String, lopDownlink: String,
        kontrakPengadaan: String, toc: String, startProject: String, catuanAc: String,
        kendala: String, tglPlanOa: String, weekPlanOa: String, odp: String, port: String,
        projectType: String, // Add the new Project Type parameter
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            onFailure("Anda harus login terlebih dahulu")
            return
        }

        // Get current user's email
        val userEmail = currentUser.email ?: "unknown@email.com"

        // Use provided timestamp for testing
        val currentTime = "2025-04-28 07:40:37" // Using provided time from prompt
        val issueWithTimestamp = "$currentTime - $lastIssue"

        // Compute calculated fields
        val idLopOlt = calculateIdLopOlt(platform, kontrakPengadaan, kodeSto, sizeOlt, jmlModul, siteId, kodeIhld, status)
        val durasiPekerjaan = calculateDurasiPekerjaan(status, tglPlanOa, startProject)
        val sisaHariThdpPlanOa = calculateSisaHariThdpPlanOa(status, tglPlanOa)
        val sisaHariThdpToc = calculateSisaHariThdpToc(status, toc)

        // Get existing project to append to lastIssueHistory
        firestore.collection("projects").document(projectId)
            .get()
            .addOnSuccessListener { document ->
                val existingData = document.data
                val existingIssueHistory = existingData?.get("lastIssueHistory") as? List<String> ?: listOf()

                // Get existing edit history or create new list
                val existingEditHistory = existingData?.get("editHistory") as? List<String> ?: listOf()

                // Create updated issue history with new issue at the beginning (most recent)
                val updatedIssueHistory = mutableListOf<String>()
                if (lastIssue.isNotEmpty()) {
                    updatedIssueHistory.add(issueWithTimestamp)
                }
                updatedIssueHistory.addAll(existingIssueHistory)

                // Create updated edit history with new edit at beginning (most recent)
                val updatedEditHistory = mutableListOf<String>()
                updatedEditHistory.add("$currentTime - Edited by $userEmail")
                updatedEditHistory.addAll(existingEditHistory)

                // Update project
                val updateData = hashMapOf(
                    // Original fields
                    "witel" to witel,
                    "status" to status,
                    "lastIssueHistory" to updatedIssueHistory,
                    "koordinat" to koordinat,
                    "uploadedBy" to userEmail, // Using logged-in user's email
                    "updatedAt" to currentTime,
                    "editHistory" to updatedEditHistory, // Track edit history

                    // New fields
                    "kodeSto" to kodeSto,
                    "namaSto" to namaSto,
                    "portMetro" to portMetro,
                    "sfp" to sfp,
                    "hostname" to hostname,
                    "sizeOlt" to sizeOlt,
                    "platform" to platform,
                    "type" to type,
                    "jmlModul" to jmlModul,
                    "siteProvider" to siteProvider,
                    "kecamatanLokasi" to kecamatanLokasi,
                    "kodeIhld" to kodeIhld,
                    "lopDownlink" to lopDownlink,
                    "kontrakPengadaan" to kontrakPengadaan,
                    "toc" to toc,
                    "startProject" to startProject,
                    "catuanAc" to catuanAc,
                    "kendala" to kendala,
                    "tglPlanOa" to tglPlanOa,
                    "weekPlanOa" to weekPlanOa,
                    "odp" to odp,
                    "port" to port,
                    "projectType" to projectType, // Add the new Project Type field

                    // Calculated fields
                    "idLopOlt" to idLopOlt,
                    "durasiPekerjaan" to durasiPekerjaan,
                    "sisaHariThdpPlanOa" to sisaHariThdpPlanOa,
                    "sisaHariThdpToc" to sisaHariThdpToc
                )

                firestore.collection("projects").document(projectId)
                    .set(updateData, SetOptions.merge())
                    .addOnSuccessListener {
                        // Update user's edit history
                        updateUserEditHistory(userEmail, siteId) {
                            showSuccessDialog("Data berhasil diperbarui")
                            onSuccess()
                        }
                    }
                    .addOnFailureListener { e ->
                        onFailure("Error: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                onFailure("Error: ${e.message}")
            }
    }

    private fun showSuccessDialog(message: String) {
        val view = layoutInflater.inflate(R.layout.dialog_success, null)
        val messageText = view.findViewById<android.widget.TextView>(R.id.successMessage)
        messageText.text = message

        val dialog = AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
            .create()

        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
        dialog.show()

        // Auto dismiss after 1.5 seconds
        android.os.Handler().postDelayed({
            dialog.dismiss()
            finish()
        }, 1500)
    }

    // Calculated field functions
    private fun calculateIdLopOlt(
        platform: String,
        kontrakPengadaan: String,
        kodeSto: String,
        sizeOlt: String,
        jmlModul: String,
        siteId: String,
        kodeIhld: String,
        status: String
    ): String {
        // Formula: J3&"/"&LEFT(S3;12)&"/"&D3&"/"&I3&"/"&L3&"/"&M3&"/"&Q3&"==>"&B3
        // J3=platform, S3=kontrakPengadaan (get first 12 chars), D3=kodeSto, I3=sizeOlt,
        // L3=jmlModul, M3=siteId, Q3=kodeIhld, B3=status

        val kontrakPrefix = if (kontrakPengadaan.isNotEmpty()) {
            if (kontrakPengadaan.length > 12) kontrakPengadaan.substring(0, 12) else kontrakPengadaan
        } else ""

        return "$platform/$kontrakPrefix/$kodeSto/$sizeOlt/$jmlModul/$siteId/$kodeIhld==>$status"
    }

    private fun calculateDurasiPekerjaan(status: String, tglPlanOa: String, startProject: String): String {
        // Formula: =IF(B3="drop";;IF(OR(B3="OA";B3="DONE UT");Y3-U3;TODAY()-U3))
        if (status.equals("drop", ignoreCase = true)) {
            return ""
        }

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Date()
            val startDate = if (startProject.isNotEmpty()) dateFormat.parse(startProject) else null

            if (startDate == null) return ""

            if (status.equals("OA", ignoreCase = true) || status.equals("DONE UT", ignoreCase = true)) {
                val planOaDate = if (tglPlanOa.isNotEmpty()) dateFormat.parse(tglPlanOa) else return ""
                val diffInDays = (planOaDate.time - startDate.time) / (1000 * 60 * 60 * 24)
                return diffInDays.toString()
            } else {
                val diffInDays = (today.time - startDate.time) / (1000 * 60 * 60 * 24)
                return diffInDays.toString()
            }
        } catch (e: Exception) {
            return ""
        }
    }

    private fun calculateSisaHariThdpPlanOa(status: String, tglPlanOa: String): String {
        // Formula: =IF(B365="0. drop";;IF(OR(B365="7. OA";B365="8. DONE UT");;Y365-TODAY()))
        if (status.equals("drop", ignoreCase = true)) {
            return ""
        }

        if (status.equals("OA", ignoreCase = true) || status.equals("DONE UT", ignoreCase = true)) {
            return ""
        }

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Date()
            val planOaDate = if (tglPlanOa.isNotEmpty()) dateFormat.parse(tglPlanOa) else return ""

            val diffInDays = (planOaDate.time - today.time) / (1000 * 60 * 60 * 24)
            return diffInDays.toString()
        } catch (e: Exception) {
            return ""
        }
    }

    private fun calculateSisaHariThdpToc(status: String, toc: String): String {
        // Formula: =IF(OR(B246="7. OA";B246="8. DONE UT");;IF(T246-TODAY()<0;"Need Amandemen";T246-TODAY()))
        if (status.equals("OA", ignoreCase = true) || status.equals("DONE UT", ignoreCase = true)) {
            return ""
        }

        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = Date()
            val tocDate = if (toc.isNotEmpty()) dateFormat.parse(toc) else return ""

            val diffInDays = (tocDate.time - today.time) / (1000 * 60 * 60 * 24)
            return if (diffInDays < 0) "Need Amandemen" else diffInDays.toString()
        } catch (e: Exception) {
            return ""
        }
    }

    private fun getCurrentDateTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    // Location permission and retrieval methods
    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                // Show explanation dialog if needed with animation
                val alertDialog = AlertDialog.Builder(this)
                    .setTitle("Izin Lokasi Dibutuhkan")
                    .setMessage("Aplikasi membutuhkan izin untuk mengakses lokasi Anda")
                    .setPositiveButton("OK") { _, _ ->
                        requestLocationPermission()
                    }
                    .create()

                alertDialog.window?.attributes?.windowAnimations = R.style.DialogAnimation
                alertDialog.show()
            } else {
                // No explanation needed, request the permission
                requestLocationPermission()
            }
            return false
        }
        return true
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        if (checkLocationPermission()) {
            // Show loading indicator for location
            btnCurrentLocation.isEnabled = false
            btnCurrentLocation.text = "Mendapatkan Lokasi..."

            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
                    // Reset button state
                    btnCurrentLocation.isEnabled = true
                    btnCurrentLocation.text = "Gunakan Lokasi Saat Ini"

                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        val koordinatText = "$latitude, $longitude"

                        koordinatInput.setText(koordinatText)
                        showToast("Lokasi didapatkan")
                    } else {
                        showToast("Gagal mendapatkan lokasi. Pastikan GPS aktif.")
                    }
                }
                .addOnFailureListener { e ->
                    // Reset button state
                    btnCurrentLocation.isEnabled = true
                    btnCurrentLocation.text = "Gunakan Lokasi Saat Ini"

                    showToast("Gagal mendapatkan lokasi: ${e.message}")
                }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getCurrentLocation()
                } else {
                    showToast("Izin lokasi tidak diberikan")
                }
            }
        }
    }

    // Helper method to load existing kendala when editing
    fun loadExistingKendala(kendalaString: String) {
        if (kendalaString.isNotEmpty()) {
            val kendalaList = kendalaString.split(", ").map { it.trim() }
            selectedKendalaList.clear()
            kendalaChipGroup.removeAllViews()

            kendalaList.forEach { kendala ->
                if (kendala.isNotEmpty()) {
                    selectedKendalaList.add(kendala)
                    addKendalaChip(kendala)
                }
            }
            updateKendalaChipGroupVisibility()
            updateProgressBar()
        }
    }
}