package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SiteDetailActivity : AppCompatActivity() {

    // UI Components
    private lateinit var tvSiteId: TextView
    private lateinit var tvWitel: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvLastIssue: TextView
    private lateinit var tvKoordinat: TextView
    private lateinit var btnEditData: Button
    private lateinit var btnBack: Button
    private lateinit var rvDocuments: RecyclerView
    private lateinit var rvImages: RecyclerView

    // Firebase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // Data
    private lateinit var siteId: String
    private lateinit var witel: String

    // Adapters
    private lateinit var documentsAdapter: DocumentsAdapter
    private lateinit var imagesAdapter: ImagesAdapter
    private var documentsList = ArrayList<DocumentModel>()
    private var imagesList = ArrayList<ImageModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_detail)

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
        tvSiteId = findViewById(R.id.tvSiteId)
        tvWitel = findViewById(R.id.tvWitel)
        tvStatus = findViewById(R.id.tvStatus)
        tvLastIssue = findViewById(R.id.tvLastIssue)
        tvKoordinat = findViewById(R.id.tvKoordinat)
        btnEditData = findViewById(R.id.btnEditData)
        btnBack = findViewById(R.id.btnBack)
        rvDocuments = findViewById(R.id.rvDocuments)
        rvImages = findViewById(R.id.rvImages)

        // Set up back button
        btnBack.setOnClickListener {
            finish()
        }

        // Set up edit data button
        btnEditData.setOnClickListener {
            val intent = Intent(this, EditSiteDataActivity::class.java)
            intent.putExtra("SITE_ID", siteId)
            intent.putExtra("WITEL", witel)
            startActivity(intent)
        }

        // Set up RecyclerViews
        setupRecyclerViews()

        // Load site data
        loadSiteData()
    }

    private fun setupRecyclerViews() {
        // Set up Documents RecyclerView
        documentsAdapter = DocumentsAdapter(documentsList)
        rvDocuments.layoutManager = LinearLayoutManager(this)
        rvDocuments.adapter = documentsAdapter

        // Set up Images RecyclerView
        imagesAdapter = ImagesAdapter(imagesList)
        rvImages.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvImages.adapter = imagesAdapter
    }

    private fun loadSiteData() {
        // Show loading indicator
        // ...

        // Get site details from Firestore
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
                val site = document.data

                // Set basic site information
                tvSiteId.text = siteId
                tvWitel.text = witel
                tvStatus.text = site?.get("status").toString()

                val lastIssueHistory = site?.get("lastIssueHistory") as? List<String>
                tvLastIssue.text = if (lastIssueHistory.isNullOrEmpty()) "No issue reported" else lastIssueHistory[0]

                tvKoordinat.text = site?.get("koordinat").toString()

                // Load documents
                loadDocuments()

                // Load images
                loadImages()
            }
            .addOnFailureListener { e ->
                showToast("Error loading site: ${e.message}")
            }
    }

    private fun loadDocuments() {
        documentsList.clear()

        // Define document types to check
        val documentTypes = listOf(
            "email_order" to "Document Email Order",
            "telkomsel_permit" to "Document Telkomsel Permit",
            "tel_partner" to "Document Tel Partner"
        )

        for ((docType, docName) in documentTypes) {
            // Check if document exists in Firebase Storage
            val docRef = storage.reference.child("documents/$witel/$siteId/$docType.pdf")
            docRef.metadata
                .addOnSuccessListener {
                    // Document exists
                    documentsList.add(DocumentModel(docName, docType, docRef.path))
                    documentsAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener {
                    // Document doesn't exist (null)
                    documentsList.add(DocumentModel(docName, docType, null))
                    documentsAdapter.notifyDataSetChanged()
                }
        }
    }

    private fun loadImages() {
        imagesList.clear()

        // Define image types to check
        val imageTypes = listOf(
            "site_location" to "Image Site Location",
            "foundation_shelter" to "Image Foundation/Shelter",
            "installation_process" to "Image Installation Process",
            "cabinet" to "Image Cabinet",
            "3p_inet" to "Image 3P (INET)",
            "3p_uctv" to "Image 3P (UCTV)",
            "3p_telephone" to "Image 3P (Telephone)"
        )

        for ((imageType, imageName) in imageTypes) {
            // Check if image exists in Firebase Storage
            val imageRef = storage.reference.child("images/$witel/$siteId/$imageType.jpg")
            imageRef.metadata
                .addOnSuccessListener {
                    // Image exists
                    imagesList.add(ImageModel(imageName, imageType, imageRef.path))
                    imagesAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener {
                    // Image doesn't exist (null)
                    imagesList.add(ImageModel(imageName, imageType, null))
                    imagesAdapter.notifyDataSetChanged()
                }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload data when returning from EditSiteDataActivity
        loadSiteData()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}