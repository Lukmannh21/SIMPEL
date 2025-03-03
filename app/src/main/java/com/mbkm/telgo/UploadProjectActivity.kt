package com.mbkm.telgo

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class UploadProjectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_project)

        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            // Just finish this activity to go back to HomeActivity
            finish()
        }
    }
}