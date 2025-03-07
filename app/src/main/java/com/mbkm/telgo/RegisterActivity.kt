package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {
    private lateinit var etName: EditText
    private lateinit var etNik: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etCompanyName: EditText
    private lateinit var etUnit: EditText
    private lateinit var etPosition: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivShowPassword: ImageView
    private lateinit var btnSignUp: Button
    private lateinit var btnBack: ImageView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        etName = findViewById(R.id.etName)
        etNik = findViewById(R.id.etNik)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etCompanyName = findViewById(R.id.etCompanyName)
        etUnit = findViewById(R.id.etUnit)
        etPosition = findViewById(R.id.etPosition)
        etPassword = findViewById(R.id.etPassword)
        ivShowPassword = findViewById(R.id.ivShowPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnBack = findViewById(R.id.btnBack)

        // Toggle password visibility
        ivShowPassword.setOnClickListener {
            if (etPassword.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                ivShowPassword.setImageResource(R.drawable.ic_visibility)
            } else {
                etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                ivShowPassword.setImageResource(R.drawable.ic_visibility_off)
            }
            etPassword.setSelection(etPassword.text.length)
        }

        // Sign up button
        btnSignUp.setOnClickListener {
            val name = etName.text.toString().trim()
            val nik = etNik.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val companyName = etCompanyName.text.toString().trim()
            val unit = etUnit.text.toString().trim()
            val position = etPosition.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateInputs(name, nik, email, phone, companyName, unit, position, password)) {
                registerUser(name, nik, email, phone, companyName, unit, position, password)
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun validateInputs(
        name: String, nik: String, email: String, phone: String,
        companyName: String, unit: String, position: String, password: String
    ): Boolean {
        if (name.isEmpty()) {
            showToast("Name must be filled")
            return false
        }
        if (nik.isEmpty()) {
            showToast("NIK must be filled")
            return false
        }
        if (email.isEmpty()) {
            showToast("Email must be filled")
            return false
        }
        if (phone.isEmpty()) {
            showToast("Phone number must be filled")
            return false
        }
        if (companyName.isEmpty()) {
            showToast("Company name must be filled")
            return false
        }
        if (unit.isEmpty()) {
            showToast("Unit must be filled")
            return false
        }
        if (position.isEmpty()) {
            showToast("Position must be filled")
            return false
        }
        if (password.isEmpty()) {
            showToast("Password must be filled")
            return false
        }
        if (password.length < 6) {
            showToast("Password must be at least 6 characters")
            return false
        }
        return true
    }

    private fun registerUser(
        name: String, nik: String, email: String, phone: String,
        companyName: String, unit: String, position: String, password: String
    ) {
        // Show loading indicator or disable button here
        btnSignUp.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid

                    if (userId != null) {
                        // Create user data map
                        val userData = hashMapOf(
                            "fullName" to name,
                            "nik" to nik,
                            "email" to email,
                            "phone" to phone,
                            "companyName" to companyName,
                            "unit" to unit,
                            "position" to position,
                            "birthDate" to "",
                            "gender" to "",
                            "witelRegion" to "",
                            "siteId" to "",
                            "createdAt" to "2025-03-07 01:51:29",
                            "updatedAt" to "2025-03-07 01:51:29",
                            "createdBy" to "Lukmannh21"
                        )

                        // Save to Firestore
                        firestore.collection("users").document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                showToast("Registration successful!")
                                // Navigate to login
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                showToast("Error saving user data: ${e.message}")
                                btnSignUp.isEnabled = true
                            }
                    } else {
                        showToast("Failed to get user ID")
                        btnSignUp.isEnabled = true
                    }
                } else {
                    showToast("Registration failed: ${task.exception?.message}")
                    btnSignUp.isEnabled = true
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}