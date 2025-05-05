package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class RegisterActivity : AppCompatActivity() {
    private lateinit var tilName: TextInputLayout
    private lateinit var tilNik: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPhone: TextInputLayout
    private lateinit var tilCompanyName: TextInputLayout
    private lateinit var tilUnit: TextInputLayout
    private lateinit var tilPosition: TextInputLayout
    private lateinit var tilPassword: TextInputLayout

    private lateinit var etName: TextInputEditText
    private lateinit var etNik: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var etCompanyName: TextInputEditText
    private lateinit var etUnit: TextInputEditText
    private lateinit var etPosition: TextInputEditText
    private lateinit var etPassword: TextInputEditText

    private lateinit var btnSignUp: Button
    private lateinit var btnBack: ImageButton
    private lateinit var ivShowPassword: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingOverlay: View
    private lateinit var scrollView: NestedScrollView
    private lateinit var cardPersonalInfo: View
    private lateinit var cardAccountInfo: View

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

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

        // Toggle password visibility
        ivShowPassword.setOnClickListener {
            togglePasswordVisibility()
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
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun initializeViews() {
        tilName = findViewById(R.id.tilName)
        tilNik = findViewById(R.id.tilNik)
        tilEmail = findViewById(R.id.tilEmail)
        tilPhone = findViewById(R.id.tilPhone)
        tilCompanyName = findViewById(R.id.tilCompanyName)
        tilUnit = findViewById(R.id.tilUnit)
        tilPosition = findViewById(R.id.tilPosition)
        tilPassword = findViewById(R.id.tilPassword)

        etName = findViewById(R.id.etName)
        etNik = findViewById(R.id.etNik)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etCompanyName = findViewById(R.id.etCompanyName)
        etUnit = findViewById(R.id.etUnit)
        etPosition = findViewById(R.id.etPosition)
        etPassword = findViewById(R.id.etPassword)

        btnSignUp = findViewById(R.id.btnSignUp)
        btnBack = findViewById(R.id.btnBack)
        ivShowPassword = findViewById(R.id.ivShowPassword)
        progressBar = findViewById(R.id.progressBar)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        scrollView = findViewById(R.id.scrollView)
        cardPersonalInfo = findViewById(R.id.cardPersonalInfo)
        cardAccountInfo = findViewById(R.id.cardAccountInfo)
    }

    private fun setupTextWatchers() {
        // Add text changed listeners to provide real-time feedback
        etName.addTextChangedListener(createTextWatcher(tilName))
        etNik.addTextChangedListener(createTextWatcher(tilNik))
        etEmail.addTextChangedListener(createTextWatcher(tilEmail))
        etPhone.addTextChangedListener(createTextWatcher(tilPhone))
        etCompanyName.addTextChangedListener(createTextWatcher(tilCompanyName))
        etUnit.addTextChangedListener(createTextWatcher(tilUnit))
        etPosition.addTextChangedListener(createTextWatcher(tilPosition))
        etPassword.addTextChangedListener(createTextWatcher(tilPassword))
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

        // Add a slight delay to the second card for a staggered effect
        cardAccountInfo.alpha = 0f
        cardAccountInfo.visibility = View.VISIBLE
        cardAccountInfo.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(200)
            .start()

        btnSignUp.alpha = 0f
        btnSignUp.visibility = View.VISIBLE
        btnSignUp.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(400)
            .start()
    }

    private fun togglePasswordVisibility() {
        if (etPassword.inputType == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
            etPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            ivShowPassword.setImageResource(R.drawable.ic_visibility)
        } else {
            etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            ivShowPassword.setImageResource(R.drawable.ic_visibility_off)
        }
        etPassword.setSelection(etPassword.text?.length ?: 0)
    }

    private fun validateInputs(
        name: String, nik: String, email: String, phone: String,
        companyName: String, unit: String, position: String, password: String
    ): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            tilName.error = "Name is required"
            isValid = false
            shakeView(tilName)
        }

        if (nik.isEmpty()) {
            tilNik.error = "NIK is required"
            isValid = false
            shakeView(tilNik)
        }

        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            isValid = false
            shakeView(tilEmail)
        } else if (!isValidEmail(email)) {
            tilEmail.error = "Please enter a valid email"
            isValid = false
            shakeView(tilEmail)
        }

        if (phone.isEmpty()) {
            tilPhone.error = "Phone number is required"
            isValid = false
            shakeView(tilPhone)
        } else if (!isValidPhone(phone)) {
            tilPhone.error = "Please enter a valid phone number"
            isValid = false
            shakeView(tilPhone)
        }

        if (companyName.isEmpty()) {
            tilCompanyName.error = "Company name is required"
            isValid = false
            shakeView(tilCompanyName)
        }

        if (unit.isEmpty()) {
            tilUnit.error = "Unit is required"
            isValid = false
            shakeView(tilUnit)
        }

        if (position.isEmpty()) {
            tilPosition.error = "Position is required"
            isValid = false
            shakeView(tilPosition)
        }

        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            isValid = false
            shakeView(tilPassword)
        } else if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            isValid = false
            shakeView(tilPassword)
        }

        // If any field is invalid, scroll to the first error
        if (!isValid) {
            showError("Please complete all required fields")
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhone(phone: String): Boolean {
        return phone.length >= 10 && phone.all { it.isDigit() }
    }

    private fun shakeView(view: View) {
        val animation = AnimationUtils.loadAnimation(this, R.anim.shake)
        view.startAnimation(animation)

        // Scroll to the first error
        scrollView.post {
            scrollView.smoothScrollTo(0, view.top - 20)
        }
    }

    private fun registerUser(
        name: String, nik: String, email: String, phone: String,
        companyName: String, unit: String, position: String, password: String
    ) {
        showLoading(true)

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
                            "createdAt" to getCurrentDateTime(),
                            "updatedAt" to getCurrentDateTime(),
                            "createdBy" to "Lukmannh21"
                        )

                        // Save to Firestore
                        firestore.collection("users").document(userId)
                            .set(userData)
                            .addOnSuccessListener {
                                showLoading(false)
                                showSuccess("Registration successful!")
                                // Navigate to login with delay
                                android.os.Handler().postDelayed({
                                    // Navigate to login
                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                    finish()
                                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                                }, 1500)
                            }
                            .addOnFailureListener { e ->
                                showLoading(false)
                                showError("Error saving user data: ${e.message}")
                            }
                    } else {
                        showLoading(false)
                        showError("Failed to get user ID")
                    }
                } else {
                    showLoading(false)
                    showError("Registration failed: ${task.exception?.message}")
                }
            }
    }

    private fun showLoading(isLoading: Boolean) {
        btnSignUp.isEnabled = !isLoading
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

    private fun showSuccess(message: String) {
        val snackbar = Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_LONG
        )
        snackbar.setBackgroundTint(ContextCompat.getColor(this, R.color.green))
        snackbar.show()
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