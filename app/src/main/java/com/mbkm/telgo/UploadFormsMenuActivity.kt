package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class UploadFormsMenuActivity : AppCompatActivity() {

    private lateinit var btnBaSurveyMiniOlt: Button
    private lateinit var btnBaSurveyBigOlt: Button
    private lateinit var btnCAF: Button
    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_forms_menu)

        // Initialize UI components
        btnBaSurveyMiniOlt = findViewById(R.id.btnBaSurveyMiniOlt)
        btnBaSurveyBigOlt = findViewById(R.id.btnBaSurveyBigOlt)
        btnCAF = findViewById(R.id.btnCAF)
        btnBack = findViewById(R.id.btnBack)

        // Set up button click listeners
        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        btnBaSurveyMiniOlt.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.button_animation)
            it.startAnimation(animation)
            it.postDelayed({
                val intent = Intent(this, BaSurveyMiniOltActivity::class.java)
                startActivity(intent)
            }, 200)
        }

        btnBaSurveyBigOlt.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.button_animation)
            it.startAnimation(animation)
            it.postDelayed({
                // Will implement later
                val intent = Intent(this, BASurveyBigActivity::class.java)
                startActivity(intent)
            }, 200)
        }

        btnCAF.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.button_animation)
            it.startAnimation(animation)
            it.postDelayed({
                // Will implement later
                showToast("Feature coming soon")
            }, 200)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}