package com.mbkm.telgo

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class BaSurveyMiniOltDetailActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnDownloadPdf: Button

    // Basic info TextViews
    private lateinit var tvLocation: TextView
    private lateinit var tvNoIhld: TextView
    private lateinit var tvPlatform: TextView
    private lateinit var tvSiteProvider: TextView
    private lateinit var tvContractNumber: TextView
    private lateinit var tvSurveyDate: TextView
    private lateinit var tvCreatedBy: TextView
    private lateinit var tvCreatedAt: TextView

    // Table results TextViews
    private lateinit var tvRackResult: TextView
    private lateinit var tvRackProposed: TextView
    private lateinit var tvRectifierResult: TextView
    private lateinit var tvRectifierProposed: TextView
    private lateinit var tvDcPowerResult: TextView
    private lateinit var tvDcPowerProposed: TextView
    private lateinit var tvBatteryResult: TextView
    private lateinit var tvBatteryProposed: TextView
    private lateinit var tvMcbResult: TextView
    private lateinit var tvMcbProposed: TextView
    private lateinit var tvGroundingResult: TextView
    private lateinit var tvGroundingProposed: TextView
    private lateinit var tvIndoorRoomResult: TextView
    private lateinit var tvIndoorRoomProposed: TextView
    private lateinit var tvAcPowerResult: TextView
    private lateinit var tvAcPowerProposed: TextView
    private lateinit var tvUplinkResult: TextView
    private lateinit var tvUplinkProposed: TextView
    private lateinit var tvConduitResult: TextView
    private lateinit var tvConduitProposed: TextView

    // Signature TextViews and ImageViews
    private lateinit var tvZteName: TextView
    private lateinit var tvTselNopName: TextView
    private lateinit var tvTselRtpdsName: TextView
    private lateinit var tvTselRtpeNfName: TextView
    private lateinit var tvTelkomName: TextView
    private lateinit var tvTifName: TextView
    private lateinit var tvTselRegion: TextView

    private lateinit var imgZteSignature: ImageView
    private lateinit var imgTselNopSignature: ImageView
    private lateinit var imgTselRtpdsSignature: ImageView
    private lateinit var imgTselRtpeNfSignature: ImageView
    private lateinit var imgTelkomSignature: ImageView
    private lateinit var imgTifSignature: ImageView

    // Photo ImageViews
    private lateinit var photoContainers: Array<LinearLayout>
    private lateinit var photoImageViews: Array<ImageView>
    private lateinit var photoLabels: Array<String>

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private var surveyId: String = ""
    private var pdfUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ba_survey_mini_olt_detail)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
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

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Set up download PDF button
        btnDownloadPdf.setOnClickListener {
            downloadPdf()
        }

        // Load survey data
        loadSurveyData()
    }

    private fun initializeUI() {
        btnBack = findViewById(R.id.btnBack)
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf)

        // Basic info TextViews
        tvLocation = findViewById(R.id.tvLocation)
        tvNoIhld = findViewById(R.id.tvNoIhld)
        tvPlatform = findViewById(R.id.tvPlatform)
        tvSiteProvider = findViewById(R.id.tvSiteProvider)
        tvContractNumber = findViewById(R.id.tvContractNumber)
        tvSurveyDate = findViewById(R.id.tvSurveyDate)
        tvCreatedBy = findViewById(R.id.tvCreatedBy)
        tvCreatedAt = findViewById(R.id.tvCreatedAt)

        // Table results TextViews
        tvRackResult = findViewById(R.id.tvRackResult)
        tvRackProposed = findViewById(R.id.tvRackProposed)
        tvRectifierResult = findViewById(R.id.tvRectifierResult)
        tvRectifierProposed = findViewById(R.id.tvRectifierProposed)
        tvDcPowerResult = findViewById(R.id.tvDcPowerResult)
        tvDcPowerProposed = findViewById(R.id.tvDcPowerProposed)
        tvBatteryResult = findViewById(R.id.tvBatteryResult)
        tvBatteryProposed = findViewById(R.id.tvBatteryProposed)
        tvMcbResult = findViewById(R.id.tvMcbResult)
        tvMcbProposed = findViewById(R.id.tvMcbProposed)
        tvGroundingResult = findViewById(R.id.tvGroundingResult)
        tvGroundingProposed = findViewById(R.id.tvGroundingProposed)
        tvIndoorRoomResult = findViewById(R.id.tvIndoorRoomResult)
        tvIndoorRoomProposed = findViewById(R.id.tvIndoorRoomProposed)
        tvAcPowerResult = findViewById(R.id.tvAcPowerResult)
        tvAcPowerProposed = findViewById(R.id.tvAcPowerProposed)
        tvUplinkResult = findViewById(R.id.tvUplinkResult)
        tvUplinkProposed = findViewById(R.id.tvUplinkProposed)
        tvConduitResult = findViewById(R.id.tvConduitResult)
        tvConduitProposed = findViewById(R.id.tvConduitProposed)

        // Signature TextViews and ImageViews
        tvZteName = findViewById(R.id.tvZteName)
        tvTselNopName = findViewById(R.id.tvTselNopName)
        tvTselRtpdsName = findViewById(R.id.tvTselRtpdsName)
        tvTselRtpeNfName = findViewById(R.id.tvTselRtpeNfName)
        tvTelkomName = findViewById(R.id.tvTelkomName)
        tvTifName = findViewById(R.id.tvTifName)
        tvTselRegion = findViewById(R.id.tvTselRegion)

        imgZteSignature = findViewById(R.id.imgZteSignature)
        imgTselNopSignature = findViewById(R.id.imgTselNopSignature)
        imgTselRtpdsSignature = findViewById(R.id.imgTselRtpdsSignature)
        imgTselRtpeNfSignature = findViewById(R.id.imgTselRtpeNfSignature)
        imgTelkomSignature = findViewById(R.id.imgTelkomSignature)
        imgTifSignature = findViewById(R.id.imgTifSignature)

        // Photo containers and ImageViews
        photoContainers = Array(15) { findViewById(resources.getIdentifier("photoContainer${it+1}", "id", packageName)) }
        photoImageViews = Array(15) { findViewById(resources.getIdentifier("imgPhoto${it+1}", "id", packageName)) }

        photoLabels = arrayOf(
            "Akses gerbang", "Name plate", "Outdoor", "Pengukuran Catuan Power AC",
            "Catuan Power DC", "Port OTB Exciting", "Cabinet Metro-E (ME Room) Metro-E",
            "Metro-E", "Akses gerbang", "Name plate", "Proposed New Pondasi",
            "Power AC di panel KWH Exciting", "Grounding Busbar", "Proposed Dual Source Power DC",
            "Rectifier"
        )
    }

    private fun loadSurveyData() {
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Fetch survey data from Firestore
        firestore.collection("ba_survey_mini_olt")
            .document(surveyId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data
                    if (data != null) {
                        // Set basic info
                        tvLocation.text = data["location"] as? String ?: ""
                        tvNoIhld.text = data["noIhld"] as? String ?: ""
                        tvPlatform.text = data["platform"] as? String ?: ""
                        tvSiteProvider.text = data["siteProvider"] as? String ?: ""
                        tvContractNumber.text = data["contractNumber"] as? String ?: ""
                        tvSurveyDate.text = data["surveyDate"] as? String ?: ""
                        tvCreatedBy.text = data["createdBy"] as? String ?: ""
                        tvCreatedAt.text = data["createdAt"] as? String ?: ""

                        // Set PDF URL for download button
                        pdfUrl = data["pdfUrl"] as? String ?: ""
                        btnDownloadPdf.isEnabled = pdfUrl.isNotEmpty()

                        // Set table results
                        val tableResults = data["tableResults"] as? Map<String, Any> ?: mapOf()

                        // Rack
                        val rackData = tableResults["rack"] as? Map<String, Any> ?: mapOf()
                        tvRackResult.text = rackData["surveyResult"] as? String ?: ""
                        tvRackProposed.text = rackData["proposed"] as? String ?: ""

                        // Rectifier
                        val rectifierData = tableResults["rectifier"] as? Map<String, Any> ?: mapOf()
                        tvRectifierResult.text = rectifierData["surveyResult"] as? String ?: ""
                        tvRectifierProposed.text = rectifierData["proposed"] as? String ?: ""

                        // DC Power
                        val dcPowerData = tableResults["dcPower"] as? Map<String, Any> ?: mapOf()
                        tvDcPowerResult.text = dcPowerData["surveyResult"] as? String ?: ""
                        tvDcPowerProposed.text = dcPowerData["proposed"] as? String ?: ""

                        // Battery
                        val batteryData = tableResults["battery"] as? Map<String, Any> ?: mapOf()
                        tvBatteryResult.text = batteryData["surveyResult"] as? String ?: ""
                        tvBatteryProposed.text = batteryData["proposed"] as? String ?: ""

                        // MCB
                        val mcbData = tableResults["mcb"] as? Map<String, Any> ?: mapOf()
                        tvMcbResult.text = mcbData["surveyResult"] as? String ?: ""
                        tvMcbProposed.text = mcbData["proposed"] as? String ?: ""

                        // Grounding
                        val groundingData = tableResults["grounding"] as? Map<String, Any> ?: mapOf()
                        tvGroundingResult.text = groundingData["surveyResult"] as? String ?: ""
                        tvGroundingProposed.text = groundingData["proposed"] as? String ?: ""

                        // Indoor Room
                        val indoorRoomData = tableResults["indoorRoom"] as? Map<String, Any> ?: mapOf()
                        tvIndoorRoomResult.text = indoorRoomData["surveyResult"] as? String ?: ""
                        tvIndoorRoomProposed.text = indoorRoomData["proposed"] as? String ?: ""

                        // AC Power
                        val acPowerData = tableResults["acPower"] as? Map<String, Any> ?: mapOf()
                        tvAcPowerResult.text = acPowerData["surveyResult"] as? String ?: ""
                        tvAcPowerProposed.text = acPowerData["proposed"] as? String ?: ""

                        // Uplink
                        val uplinkData = tableResults["uplink"] as? Map<String, Any> ?: mapOf()
                        tvUplinkResult.text = uplinkData["surveyResult"] as? String ?: ""
                        tvUplinkProposed.text = uplinkData["proposed"] as? String ?: ""

                        // Conduit
                        val conduitData = tableResults["conduit"] as? Map<String, Any> ?: mapOf()
                        tvConduitResult.text = conduitData["surveyResult"] as? String ?: ""
                        tvConduitProposed.text = conduitData["proposed"] as? String ?: ""

                        // Set signatures
                        val signaturesData = data["signatures"] as? Map<String, Any> ?: mapOf()

                        // ZTE
                        val zteData = signaturesData["zte"] as? Map<String, Any> ?: mapOf()
                        tvZteName.text = zteData["name"] as? String ?: ""
                        val zteSignatureUrl = zteData["signatureUrl"] as? String
                        if (zteSignatureUrl != null) {
                            Glide.with(this)
                                .load(zteSignatureUrl)
                                .into(imgZteSignature)
                            imgZteSignature.visibility = View.VISIBLE
                        }

                        // TSEL NOP
                        val tselNopData = signaturesData["tselNop"] as? Map<String, Any> ?: mapOf()
                        tvTselNopName.text = tselNopData["name"] as? String ?: ""
                        val tselNopSignatureUrl = tselNopData["signatureUrl"] as? String
                        if (tselNopSignatureUrl != null) {
                            Glide.with(this)
                                .load(tselNopSignatureUrl)
                                .into(imgTselNopSignature)
                            imgTselNopSignature.visibility = View.VISIBLE
                        }

                        // TSEL RTPDS
                        val tselRtpdsData = signaturesData["tselRtpds"] as? Map<String, Any> ?: mapOf()
                        tvTselRtpdsName.text = tselRtpdsData["name"] as? String ?: ""
                        val tselRtpdsSignatureUrl = tselRtpdsData["signatureUrl"] as? String
                        if (tselRtpdsSignatureUrl != null) {
                            Glide.with(this)
                                .load(tselRtpdsSignatureUrl)
                                .into(imgTselRtpdsSignature)
                            imgTselRtpdsSignature.visibility = View.VISIBLE
                        }

                        // TSEL RTPE/NF
                        val tselRtpeNfData = signaturesData["tselRtpeNf"] as? Map<String, Any> ?: mapOf()
                        tvTselRtpeNfName.text = tselRtpeNfData["name"] as? String ?: ""
                        val tselRtpeNfSignatureUrl = tselRtpeNfData["signatureUrl"] as? String
                        if (tselRtpeNfSignatureUrl != null) {
                            Glide.with(this)
                                .load(tselRtpeNfSignatureUrl)
                                .into(imgTselRtpeNfSignature)
                            imgTselRtpeNfSignature.visibility = View.VISIBLE
                        }

                        // TELKOM
                        val telkomData = signaturesData["telkom"] as? Map<String, Any> ?: mapOf()
                        tvTelkomName.text = telkomData["name"] as? String ?: ""
                        val telkomSignatureUrl = telkomData["signatureUrl"] as? String
                        if (telkomSignatureUrl != null) {
                            Glide.with(this)
                                .load(telkomSignatureUrl)
                                .into(imgTelkomSignature)
                            imgTelkomSignature.visibility = View.VISIBLE
                        }

                        // TIF
                        val tifData = signaturesData["tif"] as? Map<String, Any> ?: mapOf()
                        tvTifName.text = tifData["name"] as? String ?: ""
                        val tifSignatureUrl = tifData["signatureUrl"] as? String
                        if (tifSignatureUrl != null) {
                            Glide.with(this)
                                .load(tifSignatureUrl)
                                .into(imgTifSignature)
                            imgTifSignature.visibility = View.VISIBLE
                        }

                        // Load photos
                        val photosData = data["photos"] as? Map<String, Any> ?: mapOf()

                        for (i in 1..15) {
                            val photoUrl = photosData["photo$i"] as? String
                            if (photoUrl != null) {
                                Glide.with(this)
                                    .load(photoUrl)
                                    .into(photoImageViews[i-1])
                                photoImageViews[i-1].visibility = View.VISIBLE
                                photoContainers[i-1].visibility = View.VISIBLE
                            } else {
                                photoContainers[i-1].visibility = View.GONE
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Survey not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
                loadingDialog.dismiss()
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(this, "Error loading survey: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun downloadPdf() {
        if (pdfUrl.isEmpty()) {
            Toast.makeText(this, "PDF not available", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Create local file
        val fileName = "BA_Survey_Mini_OLT_${tvLocation.text}.pdf"
        val localFile = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

        // Download PDF
        storage.getReferenceFromUrl(pdfUrl)
            .getFile(localFile)
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
}