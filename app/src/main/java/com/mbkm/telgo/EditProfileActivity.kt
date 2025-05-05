package com.mbkm.telgo

import android.animation.ObjectAnimator
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EditProfileActivity : AppCompatActivity() {
    private lateinit var tilFullName: TextInputLayout
    private lateinit var tilNIK: TextInputLayout
    private lateinit var tilCompanyName: TextInputLayout
    private lateinit var tilUnit: TextInputLayout
    private lateinit var tilPosition: TextInputLayout
    private lateinit var tilPhone: TextInputLayout

    private lateinit var etFullName: TextInputEditText
    private lateinit var etNIK: TextInputEditText
    private lateinit var etCompanyName: TextInputEditText
    private lateinit var etUnit: TextInputEditText
    private lateinit var etPosition: TextInputEditText
    private lateinit var etPhone: TextInputEditText

    private lateinit var btnSave: Button
    private lateinit var btnBack: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingOverlay: View
    private lateinit var successAnimation: View
    private lateinit var cardPersonalInfo: CardView
    private lateinit var cardContactInfo: CardView

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Apply enter animation
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        // Initialize Firebase components
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI components
        initializeViews()
        setupTextWatchers()

        // Animate cards entry
        animateCardsEntry()

        // Load current user data
        loadUserData()

        // Setup button click listeners
        btnSave.setOnClickListener {
            if (validateInputs()) {
                saveUserData()
            } else {
                // Show error message when validation fails
                showError("Please fill in all required fields")
            }
        }

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun initializeViews() {
        btnBack = findViewById(R.id.btnBack)

        tilFullName = findViewById(R.id.tilFullName)
        tilNIK = findViewById(R.id.tilNIK)
        tilCompanyName = findViewById(R.id.tilCompanyName)
        tilUnit = findViewById(R.id.tilUnit)
        tilPosition = findViewById(R.id.tilPosition)
        tilPhone = findViewById(R.id.tilPhone)

        etFullName = findViewById(R.id.etFullName)
        etNIK = findViewById(R.id.etNIK)
        etCompanyName = findViewById(R.id.etCompanyName)
        etUnit = findViewById(R.id.etUnit)
        etPosition = findViewById(R.id.etPosition)
        etPhone = findViewById(R.id.etPhone)

        btnSave = findViewById(R.id.btnSave)
        progressBar = findViewById(R.id.progressBar)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        successAnimation = findViewById(R.id.successAnimation)
        cardPersonalInfo = findViewById(R.id.cardPersonalInfo)
        cardContactInfo = findViewById(R.id.cardContactInfo)
    }

    private fun setupTextWatchers() {
        // Add text changed listeners to provide real-time feedback
        etFullName.addTextChangedListener(createTextWatcher(tilFullName))
        etNIK.addTextChangedListener(createTextWatcher(tilNIK))
        etCompanyName.addTextChangedListener(createTextWatcher(tilCompanyName))
        etUnit.addTextChangedListener(createTextWatcher(tilUnit))
        etPosition.addTextChangedListener(createTextWatcher(tilPosition))
        etPhone.addTextChangedListener(createTextWatcher(tilPhone))
    }

    private fun createTextWatcher(textInputLayout: TextInputLayout): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                textInputLayout.error = null
            }

            override fun afterTextChanged(s: Editable?) {}
        }
    }

    private fun animateCardsEntry() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)

        cardPersonalInfo.startAnimation(fadeIn)
        cardContactInfo.startAnimation(slideUp)

        // Add a slight delay to the second card for a staggered effect
        cardContactInfo.alpha = 0f
        cardContactInfo.visibility = View.VISIBLE
        cardContactInfo.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(200)
            .start()
    }

    private fun loadUserData() {
        showLoading(true)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        // Set fields with existing data with animation
                        val fullName = document.getString("fullName") ?: ""
                        val nik = document.getString("nik") ?: ""
                        val companyName = document.getString("companyName") ?: ""
                        val unit = document.getString("unit") ?: ""
                        val position = document.getString("position") ?: ""
                        val phone = document.getString("phone") ?: ""

                        // Populate fields with animation
                        populateFieldWithAnimation(etFullName, fullName)
                        populateFieldWithAnimation(etNIK, nik)
                        populateFieldWithAnimation(etCompanyName, companyName)
                        populateFieldWithAnimation(etUnit, unit)
                        populateFieldWithAnimation(etPosition, position)
                        populateFieldWithAnimation(etPhone, phone)
                    }

                    showLoading(false)
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    showError("Error loading profile: ${e.message}")
                }
        } else {
            showLoading(false)
            // User not logged in, finish activity
            finish()
        }
    }

    private fun populateFieldWithAnimation(editText: TextInputEditText, value: String) {
        editText.setText("")
        if (value.isNotEmpty()) {
            // Simulate typing effect
            val handler = android.os.Handler()
            for (i in value.indices) {
                handler.postDelayed({
                    editText.setText(value.substring(0, i + 1))
                }, (i * 15).toLong())
            }
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Check if all fields are filled
        if (etFullName.text.toString().trim().isEmpty()) {
            tilFullName.error = "Full Name is required"
            isValid = false
            shakeView(tilFullName)
        }

        if (etNIK.text.toString().trim().isEmpty()) {
            tilNIK.error = "NIK is required"
            isValid = false
            shakeView(tilNIK)
        }

        if (etCompanyName.text.toString().trim().isEmpty()) {
            tilCompanyName.error = "Company Name is required"
            isValid = false
            shakeView(tilCompanyName)
        }

        if (etUnit.text.toString().trim().isEmpty()) {
            tilUnit.error = "Unit is required"
            isValid = false
            shakeView(tilUnit)
        }

        if (etPosition.text.toString().trim().isEmpty()) {
            tilPosition.error = "Position is required"
            isValid = false
            shakeView(tilPosition)
        }

        if (etPhone.text.toString().trim().isEmpty()) {
            tilPhone.error = "Phone Number is required"
            isValid = false
            shakeView(tilPhone)
        } else if (!isValidPhoneNumber(etPhone.text.toString())) {
            tilPhone.error = "Please enter a valid phone number"
            isValid = false
            shakeView(tilPhone)
        }

        return isValid
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        // Basic validation - must be numeric and between 10-15 digits
        return phone.matches(Regex("^[0-9]{10,15}$"))
    }

    private fun shakeView(view: View) {
        val animation = AnimationUtils.loadAnimation(this, R.anim.shake)
        view.startAnimation(animation)
    }

    private fun saveUserData() {
        showLoading(true)

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
                    showLoading(false)
                    showSuccessAnimation()
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    showError("Error updating profile: ${e.message}")
                }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingOverlay.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE

            // Fade in animation
            loadingOverlay.alpha = 0f
            loadingOverlay.animate()
                .alpha(0.7f)
                .setDuration(300)
                .start()
        } else {
            // Fade out animation
            loadingOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction {
                    loadingOverlay.visibility = View.GONE
                    progressBar.visibility = View.GONE
                }
                .start()
        }
    }

    private fun showSuccessAnimation() {
        successAnimation.visibility = View.VISIBLE
        successAnimation.alpha = 0f

        // Show success animation
        successAnimation.animate()
            .alpha(1f)
            .setDuration(200)
            .start()

        // Show success message
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            "Profile updated successfully",
            Snackbar.LENGTH_LONG
        )
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.green))
        snackbar.show()

        // Hide success animation after a delay and finish activity
        android.os.Handler().postDelayed({
            successAnimation.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    finish()
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                }
                .start()
        }, 1500)
    }

    private fun showError(message: String) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        )
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.red))
        snackbar.show()
    }

    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}