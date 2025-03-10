package com.mbkm.telgo

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {
    private lateinit var etFullName: EditText
    private lateinit var rgGender: RadioGroup
    private lateinit var rbMan: RadioButton
    private lateinit var rbWoman: RadioButton
    private lateinit var etBirthDate: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        etFullName = findViewById(R.id.etFullName)
        rgGender = findViewById(R.id.rgGender)
        rbMan = findViewById(R.id.rbMan)
        rbWoman = findViewById(R.id.rbWoman)
        etBirthDate = findViewById(R.id.etBirthDate)
        etPhone = findViewById(R.id.etPhone)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        // Load current user data
        loadUserData()

        // Setup date picker
        setupDatePicker()

        // Setup button click listeners
        btnSave.setOnClickListener {
            saveUserData()
        }

        btnCancel.setOnClickListener {
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
                        val gender = document.getString("gender") ?: ""
                        val birthDate = document.getString("birthDate") ?: ""
                        val phone = document.getString("phone") ?: ""

                        etFullName.setText(fullName)

                        when (gender) {
                            "Man" -> rbMan.isChecked = true
                            "Woman" -> rbWoman.isChecked = true
                        }

                        etBirthDate.setText(birthDate)
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

    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, monthOfYear)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        etBirthDate.setOnClickListener {
            DatePickerDialog(
                this, dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun updateDateInView() {
        val format = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        etBirthDate.setText(sdf.format(calendar.time))
    }

    private fun saveUserData() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val fullName = etFullName.text.toString().trim()
            val gender = when (rgGender.checkedRadioButtonId) {
                R.id.rbMan -> "Man"
                R.id.rbWoman -> "Woman"
                else -> ""
            }
            val birthDate = etBirthDate.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            // Create user data hashmap
            val userData = hashMapOf(
                "fullName" to fullName,
                "gender" to gender,
                "birthDate" to birthDate,
                "phone" to phone,
                "email" to currentUser.email // Store email in Firestore as well
            )

            // Save to Firestore
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