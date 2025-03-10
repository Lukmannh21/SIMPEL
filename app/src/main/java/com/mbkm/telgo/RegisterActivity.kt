package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {
    private lateinit var etFullName: EditText
    private lateinit var etNIK: EditText
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etFullName = findViewById(R.id.etFullName)
        etNIK = findViewById(R.id.etNIK)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etCompanyName = findViewById(R.id.etCompanyName)
        etUnit = findViewById(R.id.etUnit)
        etPosition = findViewById(R.id.etPosition)
        etPassword = findViewById(R.id.etPassword)
        ivShowPassword = findViewById(R.id.ivShowPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnBack = findViewById(R.id.btnBack)
        auth = FirebaseAuth.getInstance()

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

        btnSignUp.setOnClickListener {
            val fullName = etFullName.text.toString().trim()
            val nik = etNIK.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val companyName = etCompanyName.text.toString().trim()
            val unit = etUnit.text.toString().trim()
            val position = etPosition.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (fullName.isEmpty() || nik.isEmpty() || email.isEmpty() || phone.isEmpty() ||
                companyName.isEmpty() || unit.isEmpty() || position.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nik.length != 16) {
                Toast.makeText(this, "NIK must be 16 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Registration failed: " + task.exception?.message, Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}