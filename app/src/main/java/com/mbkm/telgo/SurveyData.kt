package com.mbkm.telgo

data class SurveyData(
    val id: String, // Ini adalah ID dokumen Firestore
    val projectTitle: String,
    val contractNumber: String,
    val executor: String,
    val location: String,
    val description: String,
    val createdAt: Long,
    val pdfUrl: String? = null // Tambahkan field ini
) {
    fun getFormattedDate(): String {
        val date = java.util.Date(createdAt)
        val format = java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault())
        return format.format(date)
    }
}