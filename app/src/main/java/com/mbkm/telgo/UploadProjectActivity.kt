package com.mbkm.telgo

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UploadProjectActivity : AppCompatActivity() {

    // UI components
    private lateinit var witelDropdown: AutoCompleteTextView
    private lateinit var siteIdInput: EditText
    private lateinit var statusDropdown: AutoCompleteTextView
    private lateinit var lastIssueInput: EditText
    private lateinit var koordinatInput: EditText
    private lateinit var btnCurrentLocation: Button
    private lateinit var btnAddData: Button
    private lateinit var btnBack: Button

    private lateinit var kodeStoInput: EditText
    private lateinit var namaStoInput: EditText
    private lateinit var portMetroInput: EditText
    private lateinit var sfpInput: EditText
    private lateinit var hostnameInput: EditText
    private lateinit var sizeOltDropdown: AutoCompleteTextView
    private lateinit var platformDropdown: AutoCompleteTextView
    private lateinit var typeDropdown: AutoCompleteTextView
    private lateinit var jmlModulInput: EditText
    private lateinit var siteProviderInput: AutoCompleteTextView  // Match XML ID
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
        "DROP", "MOS", "INTEGRASI", "DONE SURVEY", "DONE UT"
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
        "RELOC", "SFP BIDI", "WAITING OTN", "WAITING UPLINK"
    )

    private val siteProviderOptions = listOf(
        "DMT", "DMT - Bifurcation", "DMT- Reseller", "IBS", "NO NEED SITAC",
        "NOT READY", "PROTELINDO", "PT Centratama Menara Indonesia",
        "PT Gihon Telekomunikasi Indonesia", "PT Quattro International",
        "PT.Era Bangun Towerindo", "PT.Protelindo", "READY", "STO ROOM",
        "STP", "TBG", "TELKOM", "TELKOMSEL", "TSEL"
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

        // Set up date pickers
        setupDatePickers()

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
        siteProviderInput = findViewById(R.id.siteProviderInput) // Match exact ID from XML
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

        val kendalaAdapter = ArrayAdapter(this, R.layout.dropdown_item, kendalaOptions)
        kendalaDropdown.setAdapter(kendalaAdapter)

        // Updated to use siteProviderInput matching the XML ID
        val siteProviderAdapter = ArrayAdapter(this, R.layout.dropdown_item, siteProviderOptions)
        siteProviderInput.setAdapter(siteProviderAdapter)
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

        DatePickerDialog(
            this,
            dateSetListener,
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
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
        val siteProvider = siteProviderInput.text.toString() // Updated to match XML ID
        val kecamatanLokasi = kecamatanLokasiInput.text.toString().trim()
        val kodeIhld = kodeIhldInput.text.toString().trim()
        val lopDownlink = lopDownlinkInput.text.toString().trim()
        val kontrakPengadaan = kontrakPengadaanInput.text.toString().trim()
        val toc = tocInput.text.toString().trim()
        val startProject = startProjectInput.text.toString().trim()
        val catuanAc = catuanAcDropdown.text.toString()
        val kendala = kendalaDropdown.text.toString()
        val tglPlanOa = tglPlanOaInput.text.toString().trim()
        val weekPlanOa = weekPlanOaInput.text.toString().trim()
        val odp = odpInput.text.toString().trim()
        val port = portInput.text.toString().trim()

        // Validate required inputs
        if (witel.isEmpty() || !witelOptions.contains(witel)) {
            showToast("Silakan pilih Witel")
            return
        }

        if (siteId.isEmpty()) {
            showToast("Site ID Location tidak boleh kosong")
            return
        }

        if (status.isEmpty() || !statusOptions.contains(status)) {
            showToast("Silakan pilih Status")
            return
        }

        if (lastIssue.isEmpty()) {
            showToast("Last Issue tidak boleh kosong")
            return
        }

        if (koordinat.isEmpty()) {
            showToast("Koordinat tidak boleh kosong")
            return
        }

        // Additional validation for new required fields
        if (kodeSto.isEmpty()) {
            showToast("Kode STO tidak boleh kosong")
            return
        }

        if (platform.isEmpty() || !platformOptions.contains(platform)) {
            showToast("Silakan pilih Platform")
            return
        }

        if (sizeOlt.isEmpty() || !sizeOltOptions.contains(sizeOlt)) {
            showToast("Silakan pilih Size OLT")
            return
        }

        if (jmlModul.isEmpty()) {
            showToast("Jumlah Modul tidak boleh kosong")
            return
        }

        if (kodeIhld.isEmpty()) {
            showToast("Kode IHLD tidak boleh kosong")
            return
        }

        if (toc.isEmpty()) {
            showToast("TOC tidak boleh kosong")
            return
        }

        if (startProject.isEmpty()) {
            showToast("Start Project tidak boleh kosong")
            return
        }

        // Validation for site provider using the correct field name
        if (siteProvider.isEmpty() || !siteProviderOptions.contains(siteProvider)) {
            showToast("Silakan pilih Site Provider")
            return
        }

        // Check if site ID already exists in database
        checkSiteIdExists(siteId, witel, status, lastIssue, koordinat, kodeSto, namaSto,
            portMetro, sfp, hostname, sizeOlt, platform, type, jmlModul,
            siteProvider, kecamatanLokasi, kodeIhld, lopDownlink, kontrakPengadaan,
            toc, startProject, catuanAc, kendala, tglPlanOa, weekPlanOa, odp, port)
    }

    private fun checkSiteIdExists(
        siteId: String, witel: String, status: String, lastIssue: String, koordinat: String,
        kodeSto: String, namaSto: String, portMetro: String, sfp: String, hostname: String,
        sizeOlt: String, platform: String, type: String, jmlModul: String,
        siteProvider: String, kecamatanLokasi: String, kodeIhld: String, lopDownlink: String,
        kontrakPengadaan: String, toc: String, startProject: String, catuanAc: String,
        kendala: String, tglPlanOa: String, weekPlanOa: String, odp: String, port: String
    ) {
        firestore.collection("projects")
            .whereEqualTo("siteId", siteId)
            .get()
            .addOnSuccessListener { documents ->
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
                        isNewProject = true
                    )
                } else {
                    // Site ID exists, show edit dialog
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
                        isNewProject = false,
                        existingProjectId = documents.documents[0].id
                    )
                }
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
            }
    }

    private fun showConfirmationDialog(
        siteId: String, witel: String, status: String, lastIssue: String, koordinat: String,
        kodeSto: String, namaSto: String, portMetro: String, sfp: String, hostname: String,
        sizeOlt: String, platform: String, type: String, jmlModul: String,
        siteProvider: String, kecamatanLokasi: String, kodeIhld: String, lopDownlink: String,
        kontrakPengadaan: String, toc: String, startProject: String, catuanAc: String,
        kendala: String, tglPlanOa: String, weekPlanOa: String, odp: String, port: String,
        isNewProject: Boolean, existingProjectId: String = ""
    ) {
        val title = if (isNewProject) "Konfirmasi Data Baru" else "Edit Data Projek"
        val message = """
            Witel: $witel
            Site ID: $siteId
            Status: $status
            Kode STO: $kodeSto
            Size OLT: $sizeOlt
            Platform: $platform
            Type: $type
            Jumlah Modul: $jmlModul
            Site Provider: $siteProvider
            Kode IHLD: $kodeIhld
            TOC: $toc
            Start Project: $startProject
            Kendala: $kendala
            
            ${if (isNewProject) "Apakah data di atas sudah benar?" else "Anda akan mengedit data yang sudah ada. Lanjutkan?"}
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Submit") { _, _ ->
                if (isNewProject) {
                    saveNewProject(
                        siteId, witel, status, lastIssue, koordinat, kodeSto, namaSto,
                        portMetro, sfp, hostname, sizeOlt, platform, type, jmlModul,
                        siteProvider, kecamatanLokasi, kodeIhld, lopDownlink, kontrakPengadaan,
                        toc, startProject, catuanAc, kendala, tglPlanOa, weekPlanOa, odp, port
                    )
                } else {
                    updateExistingProject(
                        existingProjectId, siteId, witel, status, lastIssue, koordinat, kodeSto, namaSto,
                        portMetro, sfp, hostname, sizeOlt, platform, type, jmlModul,
                        siteProvider, kecamatanLokasi, kodeIhld, lopDownlink, kontrakPengadaan,
                        toc, startProject, catuanAc, kendala, tglPlanOa, weekPlanOa, odp, port
                    )
                }
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun saveNewProject(
        siteId: String, witel: String, status: String, lastIssue: String, koordinat: String,
        kodeSto: String, namaSto: String, portMetro: String, sfp: String, hostname: String,
        sizeOlt: String, platform: String, type: String, jmlModul: String,
        siteProvider: String, kecamatanLokasi: String, kodeIhld: String, lopDownlink: String,
        kontrakPengadaan: String, toc: String, startProject: String, catuanAc: String,
        kendala: String, tglPlanOa: String, weekPlanOa: String, odp: String, port: String
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Anda harus login terlebih dahulu")
            return
        }

        // Use provided timestamp for testing, or get current time in production
        // val currentTime = getCurrentDateTime()
        val currentTime = "2025-03-17 01:47:00" // Using provided time for consistency
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
            "uploadedBy" to "Lukmannh21", // Using provided user for consistency
            "createdAt" to currentTime,
            "updatedAt" to currentTime,

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

            // Calculated fields
            "idLopOlt" to idLopOlt,
            "durasiPekerjaan" to durasiPekerjaan,
            "sisaHariThdpPlanOa" to sisaHariThdpPlanOa,
            "sisaHariThdpToc" to sisaHariThdpToc
        )

        firestore.collection("projects")
            .add(projectData)
            .addOnSuccessListener {
                showToast("Data berhasil disimpan")
                finish()
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
            }
    }

    private fun updateExistingProject(
        projectId: String, siteId: String, witel: String, status: String, lastIssue: String, koordinat: String,
        kodeSto: String, namaSto: String, portMetro: String, sfp: String, hostname: String,
        sizeOlt: String, platform: String, type: String, jmlModul: String,
        siteProvider: String, kecamatanLokasi: String, kodeIhld: String, lopDownlink: String,
        kontrakPengadaan: String, toc: String, startProject: String, catuanAc: String,
        kendala: String, tglPlanOa: String, weekPlanOa: String, odp: String, port: String
    ) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            showToast("Anda harus login terlebih dahulu")
            return
        }

        // Use provided timestamp for testing, or get current time in production
        // val currentTime = getCurrentDateTime()
        val currentTime = "2025-03-17 01:47:00" // Using provided time for consistency
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

                // Create updated issue history with new issue at the beginning (most recent)
                val updatedIssueHistory = mutableListOf(issueWithTimestamp)
                updatedIssueHistory.addAll(existingIssueHistory)

                // Update project
                val updateData = hashMapOf(
                    // Original fields
                    "witel" to witel,
                    "status" to status,
                    "lastIssueHistory" to updatedIssueHistory,
                    "koordinat" to koordinat,
                    "updatedAt" to currentTime,

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

                    // Calculated fields
                    "idLopOlt" to idLopOlt,
                    "durasiPekerjaan" to durasiPekerjaan,
                    "sisaHariThdpPlanOa" to sisaHariThdpPlanOa,
                    "sisaHariThdpToc" to sisaHariThdpToc
                )

                firestore.collection("projects").document(projectId)
                    .set(updateData, SetOptions.merge())
                    .addOnSuccessListener {
                        showToast("Data berhasil diperbarui")
                        finish()
                    }
                    .addOnFailureListener { e ->
                        showToast("Error: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                showToast("Error: ${e.message}")
            }
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
                // Show explanation dialog if needed
                AlertDialog.Builder(this)
                    .setTitle("Izin Lokasi Dibutuhkan")
                    .setMessage("Aplikasi membutuhkan izin untuk mengakses lokasi Anda")
                    .setPositiveButton("OK") { _, _ ->
                        requestLocationPermission()
                    }
                    .create()
                    .show()
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
            showToast("Mencari lokasi...")
            fusedLocationProviderClient.lastLocation
                .addOnSuccessListener { location ->
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
}
