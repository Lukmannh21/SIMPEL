package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnLogout: Button
    private lateinit var btnUploadProject: Button
    private lateinit var btnWitelSearch: Button
    private lateinit var recyclerViewDashboard: RecyclerView
    private lateinit var projectAdapter: ProjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Inisialisasi komponen UI
        bottomNavigationView = findViewById(R.id.bottomNavigation)
        btnLogout = findViewById(R.id.btnLogout)
        btnUploadProject = findViewById(R.id.btnUploadProject)
        btnWitelSearch = findViewById(R.id.btnWitelSearch)
        recyclerViewDashboard = findViewById(R.id.recyclerViewDashboard)

        // Set listener navigasi bawah
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.selectedItemId = R.id.navigation_home

        // Tombol logout
        btnLogout.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Tombol upload proyek
        btnUploadProject.setOnClickListener {
            val intent = Intent(this, UploadProjectActivity::class.java)
            startActivity(intent)
        }

        // Tombol pencarian witel
        btnWitelSearch.setOnClickListener {
            val intent = Intent(this, WitelSearchActivity::class.java)
            startActivity(intent)
        }

        // Setup RecyclerView untuk daftar proyek
        recyclerViewDashboard.layoutManager = LinearLayoutManager(this)
        projectAdapter = ProjectAdapter()
        recyclerViewDashboard.adapter = projectAdapter

        // Load data proyek dari Firestore
        loadProjects()
    }

    private fun loadProjects() {
        val db = FirebaseFirestore.getInstance()
        db.collection("projects")
            .get()
            .addOnSuccessListener { result ->
                val projectList = mutableListOf<ProjectModel>()
                for (document in result) {
                    val project = document.toObject(ProjectModel::class.java)
                    projectList.add(project)
                }
                projectAdapter.setProjects(projectList)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal memuat data: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_home -> return true
            R.id.navigation_history -> return true
            R.id.navigation_account -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return false
    }
}