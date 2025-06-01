package com.mbkm.telgo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class SignatureActivity : AppCompatActivity() {

    private lateinit var drawingView: DrawingView
    private lateinit var btnClear: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var btnGallery: Button
    private lateinit var drawLayout: LinearLayout
    private lateinit var choiceLayout: LinearLayout
    private lateinit var previewLayout: LinearLayout
    private lateinit var previewImage: ImageView
    private lateinit var btnUseImage: Button
    private lateinit var btnBackToOptions: Button
    private lateinit var titleText: TextView

    private val GALLERY_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signature)

        // Initialize views
        drawingView = findViewById(R.id.drawingView)
        btnClear = findViewById(R.id.btnClear)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        btnGallery = findViewById(R.id.btnGallery)
        drawLayout = findViewById(R.id.drawLayout)
        choiceLayout = findViewById(R.id.choiceLayout)
        previewLayout = findViewById(R.id.previewLayout)
        previewImage = findViewById(R.id.previewImage)
        btnUseImage = findViewById(R.id.btnUseImage)
        btnBackToOptions = findViewById(R.id.btnBackToOptions)
        titleText = findViewById(R.id.titleText)

        // Set up initial UI state - show choice layout first
        showChoiceLayout()

        // Button to choose drawing option
        findViewById<Button>(R.id.btnDrawSignature).setOnClickListener {
            showDrawingLayout()
        }

        // Button to choose gallery option
        findViewById<Button>(R.id.btnGallerySignature).setOnClickListener {
            openGallery()
        }

        // Button to clear drawing
        btnClear.setOnClickListener {
            drawingView.clearCanvas()
        }

        // Button to save drawing
        btnSave.setOnClickListener {
            if (drawingView.isEmpty()) {
                Toast.makeText(this, "Please draw a signature first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveSignature(drawingView.getBitmap())
        }

        // Button to use selected image
        btnUseImage.setOnClickListener {
            val drawable = previewImage.drawable
            if (drawable != null) {
                // Convert drawable to bitmap
                val bitmap = Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bitmap)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)

                saveSignature(bitmap)
            }
        }

        // Button to cancel and go back to previous screen
        btnCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        // Button to go back to options from preview
        btnBackToOptions.setOnClickListener {
            showChoiceLayout()
        }
    }

    private fun showChoiceLayout() {
        titleText.text = "Signature Options"
        choiceLayout.visibility = View.VISIBLE
        drawLayout.visibility = View.GONE
        previewLayout.visibility = View.GONE
    }

    private fun showDrawingLayout() {
        titleText.text = "Draw Signature"
        choiceLayout.visibility = View.GONE
        drawLayout.visibility = View.VISIBLE
        previewLayout.visibility = View.GONE
    }

    private fun showPreviewLayout() {
        titleText.text = "Preview Signature"
        choiceLayout.visibility = View.GONE
        drawLayout.visibility = View.GONE
        previewLayout.visibility = View.VISIBLE
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    private fun saveSignature(bitmap: Bitmap) {
        try {
            // Create a temp file to store the signature
            val tempFile = File(cacheDir, "signature_${System.currentTimeMillis()}.png")
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // Create a content URI using FileProvider
            val uri = FileProvider.getUriForFile(
                this,
                "com.mbkm.telgo.fileprovider",
                tempFile
            )

            // Return the URI to the calling activity
            val resultIntent = Intent()
            resultIntent.putExtra("signature_uri", uri)
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        } catch (e: IOException) {
            Toast.makeText(this, "Error saving signature: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            if (imageUri != null) {
                try {
                    // Load the image into the preview
                    previewImage.setImageURI(imageUri)

                    // Show the preview layout
                    showPreviewLayout()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}