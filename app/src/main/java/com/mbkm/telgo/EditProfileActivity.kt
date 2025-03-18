package com.mbkm.telgo

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EditProfileActivity : AppCompatActivity() {
    private lateinit var etFullName: EditText
    private lateinit var etNIK: EditText
    private lateinit var etCompanyName: EditText
    private lateinit var etUnit: EditText
    private lateinit var etPosition: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        btnBack = findViewById(R.id.btnBack)
        etFullName = findViewById(R.id.etFullName)
        etNIK = findViewById(R.id.etNIK)
        etCompanyName = findViewById(R.id.etCompanyName)
        etUnit = findViewById(R.id.etUnit)
        etPosition = findViewById(R.id.etPosition)
        etPhone = findViewById(R.id.etPhone)
        btnSave = findViewById(R.id.btnSave)

        // Load current user data
        loadUserData()

        // Setup button click listeners
        btnSave.setOnClickListener {
            saveUserData()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Set fields with existing data
                        val fullName = document.getString("fullName") ?: ""
                        val nik = document.getString("nik") ?: ""
                        val companyName = document.getString("companyName") ?: ""
                        val unit = document.getString("unit") ?: ""
                        val position = document.getString("position") ?: ""
                        val phone = document.getString("phone") ?: ""

                        etFullName.setText(fullName)
                        etNIK.setText(nik)
                        etCompanyName.setText(companyName)
                        etUnit.setText(unit)
                        etPosition.setText(position)
                        etPhone.setText(phone)
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // User not logged in, finish activity
            finish()
        }
    }

    private fun saveUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val fullName = etFullName.text.toString().trim()
            val nik = etNIK.text.toString().trim()
            val companyName = etCompanyName.text.toString().trim()
            val unit = etUnit.text.toString().trim()
            val position = etPosition.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            // Create user data hashmap
            val userData = hashMapOf(
                "fullName" to fullName,
                "nik" to nik,
                "companyName" to companyName,
                "unit" to unit,
                "position" to position,
                "phone" to phone,
                "updatedAt" to getCurrentDateTime()
            )

            // Save to Firestore
            firestore.collection("users").document(userId)
                .update(userData as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}