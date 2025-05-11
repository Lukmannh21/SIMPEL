package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

class BASurveyBigDetailActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // UI elements for basic information
    private lateinit var tvProjectTitle: TextView
    private lateinit var tvContractNumber: TextView
    private lateinit var tvExecutor: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvSurveyDate: TextView

    // UI elements for survey results
    private lateinit var tvAktual1: TextView
    private lateinit var tvKeterangan1: TextView
    private lateinit var tvAktual2: TextView
    private lateinit var tvKeterangan2: TextView
    private lateinit var tvAktual3: TextView
    private lateinit var tvKeterangan3: TextView
    private lateinit var tvAktual4: TextView
    private lateinit var tvKeterangan4: TextView
    private lateinit var tvAktual5: TextView
    private lateinit var tvKeterangan5: TextView
    private lateinit var tvAktual6: TextView
    private lateinit var tvKeterangan6: TextView
    private lateinit var tvAktual7: TextView
    private lateinit var tvKeterangan7: TextView
    private lateinit var tvAktual8: TextView
    private lateinit var tvKeterangan8: TextView
    private lateinit var tvAktual9: TextView
    private lateinit var tvKeterangan9: TextView
    private lateinit var tvAktual10: TextView
    private lateinit var tvKeterangan10: TextView
    private lateinit var tvAktual11: TextView
    private lateinit var tvKeterangan11: TextView
    private lateinit var tvAktual12: TextView
    private lateinit var tvKeterangan12: TextView
    private lateinit var tvAktual13: TextView
    private lateinit var tvKeterangan13: TextView
    private lateinit var tvAktual14: TextView
    private lateinit var tvKeterangan14: TextView
    private lateinit var tvAktual15: TextView
    private lateinit var tvKeterangan15: TextView
    private lateinit var tvAktual16: TextView
    private lateinit var tvKeterangan16: TextView
    private lateinit var tvAktual17: TextView
    private lateinit var tvKeterangan17: TextView
    private lateinit var tvAktual18: TextView
    private lateinit var tvKeterangan18: TextView
    private lateinit var tvAktual19: TextView
    private lateinit var tvKeterangan19: TextView


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

    private lateinit var btnBack: ImageButton
    private lateinit var btnDownloadPdf: Button

    private var surveyId: String = ""
    private var pdfUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basurvey_big_detail)

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



    private fun  initializeUI() {
        // Basic information
        tvProjectTitle = findViewById(R.id.tvProjectTitle)
        tvContractNumber = findViewById(R.id.tvContractNumber)
        tvExecutor = findViewById(R.id.tvExecutor)
        tvLocation = findViewById(R.id.tvLocation)
        tvSurveyDate = findViewById(R.id.tvSurveyDate)

        // Survey results
        tvAktual1 = findViewById(R.id.tvAktual1)
        tvKeterangan1 = findViewById(R.id.tvKeterangan1)
        tvAktual2 = findViewById(R.id.tvAktual2)
        tvKeterangan2 = findViewById(R.id.tvKeterangan2)
        tvAktual3 = findViewById(R.id.tvAktual3)
        tvKeterangan3 = findViewById(R.id.tvKeterangan3)
        tvAktual4 = findViewById(R.id.tvAktual4)
        tvKeterangan4 = findViewById(R.id.tvKeterangan4)
        tvAktual5 = findViewById(R.id.tvAktual5)
        tvKeterangan5 = findViewById(R.id.tvKeterangan5)
        tvAktual6 = findViewById(R.id.tvAktual6)
        tvKeterangan6 = findViewById(R.id.tvKeterangan6)
        tvAktual7 = findViewById(R.id.tvAktual7)
        tvKeterangan7 = findViewById(R.id.tvKeterangan7)
        tvAktual8 = findViewById(R.id.tvAktual8)
        tvKeterangan8 = findViewById(R.id.tvKeterangan8)
        tvAktual9 = findViewById(R.id.tvAktual9)
        tvKeterangan9 = findViewById(R.id.tvKeterangan9)
        tvAktual10 = findViewById(R.id.tvAktual10)
        tvKeterangan10 = findViewById(R.id.tvKeterangan10)
        tvAktual11 = findViewById(R.id.tvAktual11)
        tvKeterangan11 = findViewById(R.id.tvKeterangan11)
        tvAktual12 = findViewById(R.id.tvAktual12)
        tvKeterangan12 = findViewById(R.id.tvKeterangan12)
        tvAktual13 = findViewById(R.id.tvAktual13)
        tvKeterangan13 = findViewById(R.id.tvKeterangan13)
        tvAktual14 = findViewById(R.id.tvAktual14)
        tvKeterangan14 = findViewById(R.id.tvKeterangan14)
        tvAktual15 = findViewById(R.id.tvAktual15)
        tvKeterangan15 = findViewById(R.id.tvKeterangan15)
        tvAktual16 = findViewById(R.id.tvAktual16)
        tvKeterangan16 = findViewById(R.id.tvKeterangan16)
        tvAktual17 = findViewById(R.id.tvAktual17)
        tvKeterangan17 = findViewById(R.id.tvKeterangan17)
        tvAktual18 = findViewById(R.id.tvAktual18)
        tvKeterangan18 = findViewById(R.id.tvKeterangan18)
        tvAktual19 = findViewById(R.id.tvAktual19)
        tvKeterangan19 = findViewById(R.id.tvKeterangan19)


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

        // Buttons
        btnBack = findViewById(R.id.btnBack)
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf)
    }

    private fun loadSurveyData() {
        // Show loading dialog
        val loadingDialog = AlertDialog.Builder(this)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        // Fetch survey data from Firestore
        firestore.collection("big_surveys")
            .document(surveyId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val data = document.data
                    if (data != null) {
                        // Set basic info
                        tvProjectTitle.text = data["projectTitle"] as? String ?: ""
                        tvContractNumber.text = data["contractNumber"] as? String ?: ""
                        tvExecutor.text = data["executor"] as? String ?: ""
                        tvLocation.text = data["location"] as? String ?: ""
                        tvSurveyDate.text = data["surveyDate"] as? String ?: ""

                        // Set PDF URL for download button
                        pdfUrl = data["pdfUrl"] as? String ?: ""
                        btnDownloadPdf.isEnabled = pdfUrl.isNotEmpty()

                        // Populate survey results
                        tvAktual1.text = data["actual1"] as? String ?: ""
                        tvKeterangan1.text = data["remark1"] as? String ?: ""
                        tvAktual2.text = data["actual2"] as? String ?: ""
                        tvKeterangan2.text = data["remark2"] as? String ?: ""
                        tvAktual3.text = data["actual3"] as? String ?: ""
                        tvKeterangan3.text = data["remark3"] as? String ?: ""
                        tvAktual4.text = data["actual4"] as? String ?: ""
                        tvKeterangan4.text = data["remark4"] as? String ?: ""
                        tvAktual5.text = data["actual5"] as? String ?: ""
                        tvKeterangan5.text = data["remark5"] as? String ?: ""
                        tvAktual6.text = data["actual6"] as? String ?: ""
                        tvKeterangan6.text = data["remark6"] as? String ?: ""
                        tvAktual7.text = data["actual7"] as? String ?: ""
                        tvKeterangan7.text = data["remark7"] as? String ?: ""
                        tvAktual8.text = data["actual8"] as? String ?: ""
                        tvKeterangan8.text = data["remark8"] as? String ?: ""
                        tvAktual9.text = data["actual9"] as? String ?: ""
                        tvKeterangan9.text = data["remark9"] as? String ?: ""
                        tvAktual10.text = data["actual10"] as? String ?: ""
                        tvKeterangan10.text = data["remark10"] as? String ?: ""
                        tvAktual11.text = data["actual11"] as? String ?: ""
                        tvKeterangan11.text = data["remark11"] as? String ?: ""
                        tvAktual12.text = data["actual12"] as? String ?: ""
                        tvKeterangan12.text = data["remark12"] as? String ?: ""
                        tvAktual13.text = data["actual13"] as? String ?: ""
                        tvKeterangan13.text = data["remark13"] as? String ?: ""
                        tvAktual14.text = data["actual14"] as? String ?: ""
                        tvKeterangan14.text = data["remark14"] as? String ?: ""
                        tvAktual15.text = data["actual15"] as? String ?: ""
                        tvKeterangan15.text = data["remark15"] as? String ?: ""
                        tvAktual16.text = data["actual16"] as? String ?: ""
                        tvKeterangan16.text = data["remark16"] as? String ?: ""
                        tvAktual17.text = data["actual17"] as? String ?: ""
                        tvKeterangan17.text = data["remark17"] as? String ?: ""
                        tvAktual18.text = data["actual18"] as? String ?: ""
                        tvKeterangan18.text = data["remark18"] as? String ?: ""
                        tvAktual19.text = data["actual19"] as? String ?: ""
                        tvKeterangan19.text = data["remark19"] as? String ?: ""


                        // Populate signatures
                        tvTselRegion.text = data["tselRegion"] as? String ?: ""



                        tvZteName.text = data["zteName"] as? String ?: ""
                        val zteSignature = data["zteSignature"] as? String
                        if (zteSignature != null) {
                            Glide.with(this)
                                .load(zteSignature)
                                .into(imgZteSignature)
                            imgZteSignature.visibility = View.VISIBLE
                        }

                        // TSEL NOP
                        tvTselNopName.text = data["tselNopName"] as? String ?: ""
                        val tselNopSignature = data["tselNopSignature"] as? String
                        if (tselNopSignature != null) {
                            Glide.with(this)
                                .load(tselNopSignature)
                                .into(imgTselNopSignature)
                            imgTselNopSignature.visibility = View.VISIBLE
                        }

                        // TSEL RTPDS
                        tvTselRtpdsName.text = data["tvTselRtpdsName"] as? String ?: ""
                        val tselRtpdsSignature = data["tselRtpdsSignature"] as? String
                        if (tselRtpdsSignature != null) {
                            Glide.with(this)
                                .load(tselRtpdsSignature)
                                .into(imgTselRtpdsSignature)
                            imgTselRtpdsSignature.visibility = View.VISIBLE
                        }

                        // TSEL RTPE/NF
                        tvTselRtpeNfName.text = data["tvTselRtpeNfName"] as? String ?: ""
                        val tselRtpeNfSignature = data["tselRtpeNfSignature"] as? String
                        if (tselRtpeNfSignature != null) {
                            Glide.with(this)
                                .load(tselRtpeNfSignature)
                                .into(imgTselRtpeNfSignature)
                            imgTselRtpeNfSignature.visibility = View.VISIBLE
                        }

                        // TELKOM
                        tvTelkomName.text = data["tvTelkomName"] as? String ?: ""
                        val telkomSignature = data["telkomSignature"] as? String
                        if (telkomSignature != null) {
                            Glide.with(this)
                                .load(telkomSignature)
                                .into(imgTelkomSignature)
                            imgTelkomSignature.visibility = View.VISIBLE
                        }

                        // TIF
                        tvTifName.text = data["name"] as? String ?: ""
                        val tifSignature = data["tifSignature"] as? String
                        if (tifSignature != null) {
                            Glide.with(this)
                                .load(tifSignature)
                                .into(imgTifSignature)
                            imgTifSignature.visibility = View.VISIBLE
                        }


                    } else {
                        Toast.makeText(this, "Survey not found", Toast.LENGTH_SHORT).show()
                        finish()
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
            val fileName = "BA_Survey_Big_OLT_${tvLocation.text}.pdf"
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