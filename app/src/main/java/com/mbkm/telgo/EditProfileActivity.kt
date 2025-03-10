package com.mbkm.telgo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {
    private lateinit var etFullName: EditText
    private lateinit var etNIK: EditText
    private lateinit var etCompanyName: EditText
    private lateinit var etUnit: EditText
    private lateinit var etPosition: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        etFullName = findViewById(R.id.etFullName)
        etNIK = findViewById(R.id.etNIK)
        etCompanyName = findViewById(R.id.etCompanyName)
        etUnit = findViewById(R.id.etUnit)
        etPosition = findViewById(R.id.etPosition)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        // Load current user data
        loadUserData()

        // Save button listener
        btnSave.setOnClickListener {
            saveUserData()
        }

        // Back button listener
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
                        etFullName.setText(document.getString("fullName") ?: "")
                        etNIK.setText(document.getString("nik") ?: "")
                        etCompanyName.setText(document.getString("companyName") ?: "")
                        etUnit.setText(document.getString("unit") ?: "")
                        etPosition.setText(document.getString("position") ?: "")
                        etEmail.setText(document.getString("email") ?: "")
                        etPhone.setText(document.getString("phone") ?: "")
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            finish()
        }
    }

    private fun saveUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val userData = hashMapOf(
                "fullName" to etFullName.text.toString().trim(),
                "nik" to etNIK.text.toString().trim(),
                "companyName" to etCompanyName.text.toString().trim(),
                "unit" to etUnit.text.toString().trim(),
                "position" to etPosition.text.toString().trim(),
                "email" to etEmail.text.toString().trim(),
                "phone" to etPhone.text.toString().trim()
            )

            firestore.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
