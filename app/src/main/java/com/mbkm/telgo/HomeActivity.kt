package com.mbkm.telgo

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Row
import java.io.File
import java.io.FileOutputStream

class HomeActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var btnLogout: Button
    private lateinit var btnUploadProject: Button
    private lateinit var btnWitelSearch: Button
    private lateinit var btnDownload: Button
    private lateinit var btnLastHistory: Button
    private lateinit var recyclerViewDashboard: RecyclerView
    private lateinit var projectAdapter: ProjectAdapter
    private lateinit var spinnerStatus: Spinner

    private val projectList = mutableListOf<ProjectModel>()
    private val filteredProjectList = mutableListOf<ProjectModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Inisialisasi komponen UI
        bottomNavigationView = findViewById(R.id.bottomNavigation)
        btnLogout = findViewById(R.id.btnLogout)
        btnUploadProject = findViewById(R.id.btnUploadProject)
        btnWitelSearch = findViewById(R.id.btnWitelSearch)
        btnDownload = findViewById(R.id.btnDownload)
        btnLastHistory = findViewById(R.id.btnLastHistory)
        recyclerViewDashboard = findViewById(R.id.recyclerViewDashboard)
        spinnerStatus = findViewById(R.id.spinnerStatus)

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

        // Tombol last history
        btnLastHistory.setOnClickListener {
            val intent = Intent(this, LastUpdateActivity::class.java)
            startActivity(intent)
        }

        // Tombol pencarian witel
        btnWitelSearch.setOnClickListener {
            val intent = Intent(this, WitelSearchActivity::class.java)
            startActivity(intent)
        }

        // Tombol download
        btnDownload.setOnClickListener {
            downloadProjectsAsExcel()
        }

        // Setup RecyclerView untuk daftar proyek
        recyclerViewDashboard.layoutManager = LinearLayoutManager(this)
        projectAdapter = ProjectAdapter(filteredProjectList)
        recyclerViewDashboard.adapter = projectAdapter

        // Setup Spinner untuk filter status
        ArrayAdapter.createFromResource(
            this,
            R.array.status_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerStatus.adapter = adapter
        }

        spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedStatus = parent.getItemAtPosition(position).toString()
                filterProjectsByStatus(selectedStatus)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // Load data proyek dari Firestore
        loadProjects()
    }

    private fun loadProjects() {
        val db = FirebaseFirestore.getInstance()
        db.collection("projects")
            .get()
            .addOnSuccessListener { result ->
                projectList.clear()
                for (document in result) {
                    val project = document.toObject(ProjectModel::class.java)
                    projectList.add(project)
                }
                filterProjectsByStatus(spinnerStatus.selectedItem.toString())
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal memuat data: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun filterProjectsByStatus(status: String) {
        filteredProjectList.clear()
        if (status == "Pilih Status") {
            filteredProjectList.addAll(projectList)
        } else {
            for (project in projectList) {
                if (project.status == status) {
                    filteredProjectList.add(project)
                }
            }
        }
        projectAdapter.notifyDataSetChanged()
    }

    private fun downloadProjectsAsExcel() {
        val db = FirebaseFirestore.getInstance()
        db.collection("projects")
            .get()
            .addOnSuccessListener { result ->
                val wb = HSSFWorkbook()
                val sheet = wb.createSheet("Projects")

                // Header row
                val header: Row = sheet.createRow(0)
                header.createCell(0).setCellValue("Witel")
                header.createCell(1).setCellValue("IHLD ID")
                header.createCell(2).setCellValue("Port")
                header.createCell(3).setCellValue("Site Provider")
                header.createCell(4).setCellValue("Last Issue")
                header.createCell(5).setCellValue("Status")
                header.createCell(6).setCellValue("Kendala")
                header.createCell(7).setCellValue("Tgl Plan OA")

                // Data rows
                var rowIndex = 1
                for (document in result) {
                    val project = document.toObject(ProjectModel::class.java)
                    val row: Row = sheet.createRow(rowIndex++)
                    row.createCell(0).setCellValue(project.witel)
                    row.createCell(1).setCellValue(project.kodeIhld)
                    row.createCell(2).setCellValue(project.port)
                    row.createCell(3).setCellValue(project.siteProvider)
                    row.createCell(4).setCellValue(project.lastIssueHistory.joinToString("\n"))
                    row.createCell(5).setCellValue(project.status)
                    row.createCell(6).setCellValue(project.kendala)
                    row.createCell(7).setCellValue(project.tglPlanOa)
                }

                // Write to file
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val filePath = File(downloadsDir, "Dashboard.xls")
                val fileOut = FileOutputStream(filePath)
                wb.write(fileOut)
                fileOut.close()
                wb.close()

                Toast.makeText(this, "Data berhasil diunduh ke folder Download: ${filePath.absolutePath}", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal mengunduh data: ${exception.message}", Toast.LENGTH_LONG).show()
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