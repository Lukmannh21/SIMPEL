package com.mbkm.telgo

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.gcacace.signaturepad.views.SignaturePad
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

class SignatureActivity : AppCompatActivity() {

    private lateinit var signaturePad: SignaturePad
    private lateinit var btnClear: Button
    private lateinit var btnDone: Button
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signature)

        // Initialize UI components
        signaturePad = findViewById(R.id.signaturePad)
        btnClear = findViewById(R.id.btnClear)
        btnDone = findViewById(R.id.btnDone)
        btnBack = findViewById(R.id.btnBack)

        // Set up signature pad events
        signaturePad.setOnSignedListener(object : SignaturePad.OnSignedListener {
            override fun onStartSigning() {
                // No action needed
            }

            override fun onSigned() {
                btnDone.isEnabled = true
            }

            override fun onClear() {
                btnDone.isEnabled = false
            }
        })

        // Set button click listeners
        btnClear.setOnClickListener {
            signaturePad.clear()
        }

        btnDone.setOnClickListener {
            if (!signaturePad.isEmpty) {
                try {
                    val uri = saveSignature()
                    val resultIntent = Intent()
                    resultIntent.putExtra("signature_uri", uri)
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } catch (e: IOException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Failed to save signature", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please sign first", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    @Throws(IOException::class)
    private fun saveSignature(): Uri {
        // Get the signature bitmap
        val signatureBitmap = signaturePad.signatureBitmap

        // Create white background for the signature
        // Fix: Use a default config (ARGB_8888) if signatureBitmap.config is null
        val backgroundBitmap = Bitmap.createBitmap(
            signatureBitmap.width,
            signatureBitmap.height,
            signatureBitmap.config ?: Bitmap.Config.ARGB_8888
        )

        // Create a canvas with white background
        val canvas = Canvas(backgroundBitmap)
        canvas.drawColor(Color.WHITE)

        // Draw the signature on the white background
        canvas.drawBitmap(signatureBitmap, 0f, 0f, null)

        // Save to MediaStore
        val fileName = "signature_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Failed to create new MediaStore record.")

        contentResolver.openOutputStream(uri)?.use { outputStream ->
            backgroundBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        } ?: throw IOException("Failed to open output stream.")

        return uri
    }
}