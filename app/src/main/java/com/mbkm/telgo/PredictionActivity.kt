package com.mbkm.telgo

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class PredictionActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var rvPredictions: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvSummary: TextView
    private lateinit var adapter: PredictionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prediction)

        firestore = FirebaseFirestore.getInstance()
        rvPredictions = findViewById(R.id.rvPredictions)
        progressBar = findViewById(R.id.progressBar)
        tvSummary = findViewById(R.id.tvPredictionSummary)

        setupRecyclerView()
        loadAndPredictProjects()
    }

    private fun setupRecyclerView() {
        adapter = PredictionAdapter()
        rvPredictions.layoutManager = LinearLayoutManager(this)
        rvPredictions.adapter = adapter
    }

    private fun loadAndPredictProjects() {
        progressBar.visibility = View.VISIBLE

        // Ambil proyek yang belum selesai (Status bukan OA atau Done UT)
        firestore.collection("projects")
            .whereNotIn("status", listOf("OA", "6. OA", "DONE UT", "DONE"))
            .get()
            .addOnSuccessListener { documents ->
                val predictionList = ArrayList<PredictionModel>()
                var highRiskCount = 0

                for (doc in documents) {
                    try {
                        // 1. Ambil Data Raw
                        // ... (kode atas sama)

// 1. Ambil field dari Firestore
                        val siteId = doc.getString("siteId") ?: "Unknown"
                        val witel = doc.getString("witel")
                        val sto = doc.getString("sto")
                        val weekPlan = doc.getString("weekPlan")
                        val catuanAc = doc.getString("catuanAc")
                        val sfp = doc.getString("sfp")
                        val siteProvider = doc.getString("siteProvider")
                        val kendala = doc.getString("kendala")
                        val portMetro = doc.getString("portMetro") // Untuk Binary
                        val startProject = doc.getString("startProject")
                        val jmlModulStr = doc.getString("jmlModul") ?: "0"

// 2. Preprocessing
                        val featureUmur = ModelUtils.calculateUmur(startProject)
                        val featureWeekPlan = ModelUtils.encodeWeekPlan(weekPlan)
                        val featureSto = ModelUtils.encodeSto(sto)
                        val featureWitel = ModelUtils.encodeWitel(witel)
                        val featureCatuanAC = ModelUtils.encodeCatuanAC(catuanAc)
                        val featureSFP = ModelUtils.encodeSFP(sfp)
                        val featureSiteProvider = ModelUtils.encodeSiteProvider(siteProvider)
                        val featureKendala = ModelUtils.encodeKendala(kendala)
                        val featureJmlModul = jmlModulStr.toDoubleOrNull() ?: 0.0
                        val featurePortMetroBinary = ModelUtils.encodePortMetroBinary(portMetro) // Fitur Baru

// 3. Masukkan ke Array Input Model
// (URUTAN INI HARUS SAMA PERSIS DENGAN DEPLOYMENT_CONFIG v19)
                        val inputFeatures = doubleArrayOf(
                            featureUmur,               // 1. Umur_Numeric
                            featureWeekPlan,           // 2. WEEK PLAN OA_Encoded
                            featureSto,                // 3. Sto_Encoded
                            featureWitel,              // 4. Witel_Encoded
                            featureCatuanAC,           // 5. CATUAN AC_Encoded
                            featureSFP,                // 6. SFP_Encoded
                            featureSiteProvider,       // 7. SITE PROVIDER_Encoded
                            featureKendala,            // 8. Kendala_Encoded
                            featureJmlModul,           // 9. JML MODUL
                            featurePortMetroBinary     // 10. PORT_METRO_BINARY
                        )

// 4. Prediksi menggunakan Model Java (PredictorOLT)
                        val probabilities = PredictorOLT.score(inputFeatures)
                        val probTerlambat = if (probabilities.size > 1) probabilities[1] else probabilities[0]
                        val isLate = probTerlambat > 0.30 // Samakan dengan threshold Python (0.4)



                        if (isLate) highRiskCount++

                        predictionList.add(
                            PredictionModel(
                                siteId = siteId,
                                witel = witel ?: "-",
                                isHighRisk = isLate,
                                probability = probTerlambat
                            )
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 5. Update UI
                progressBar.visibility = View.GONE
                tvSummary.text = "Ditemukan ${predictionList.size} proyek aktif.\n$highRiskCount proyek berisiko terlambat."

                // Urutkan yang berisiko paling atas
                adapter.setData(predictionList.sortedByDescending { it.probability })

            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                tvSummary.text = "Gagal memuat data."
            }
    }
}