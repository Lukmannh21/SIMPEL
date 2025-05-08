package com.mbkm.telgo

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignatureActivity : AppCompatActivity() {

    private lateinit var buttonUpload: Button
    private lateinit var buttonCancel: Button
    private lateinit var previewImage: ImageView

    private val REQUEST_PICK_IMAGE = 101
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signature)

        buttonUpload = findViewById(R.id.btnUpload)
        buttonCancel = findViewById(R.id.btnCancel)
        previewImage = findViewById(R.id.previewImage)

        // Open gallery immediately on start
        openGalleryForSignature()

        buttonUpload.setOnClickListener {
            if (selectedImageUri != null) {
                returnSignatureResult(selectedImageUri!!)
            } else {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
                openGalleryForSignature()
            }
        }

        buttonCancel.setOnClickListener {
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun openGalleryForSignature() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            try {
                // Get the selected image URI
                val uri = data.data
                if (uri != null) {
                    selectedImageUri = uri

                    // Show the signature in the preview
                    previewImage.setImageURI(uri)
                    previewImage.visibility = View.VISIBLE
                } else {
                    Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("SignatureActivity", "Error loading image: ${e.message}", e)
                Toast.makeText(this, "Failed to load signature image", Toast.LENGTH_SHORT).show()
            }
        } else if (resultCode == Activity.RESULT_CANCELED && selectedImageUri == null) {
            // User cancelled the gallery selection and we have no image
            // Cancel the activity too
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    private fun returnSignatureResult(uri: Uri) {
        val resultIntent = Intent()
        resultIntent.putExtra("signature_uri", uri)
        resultIntent.putExtra("is_from_gallery", true) // Always from gallery now
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}