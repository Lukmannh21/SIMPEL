package com.mbkm.telgo

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context

import android.content.IntentFilter
import android.net.Uri
import android.os.Environment

class DocumentsAdapter(
    private val documentsList: ArrayList<DocumentModel>
) : RecyclerView.Adapter<DocumentsAdapter.DocumentViewHolder>() {

    inner class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val documentNameTextView: TextView = itemView.findViewById(R.id.tvDocumentName)
        val viewDocumentButton: Button = itemView.findViewById(R.id.btnViewDocument)

        fun bind(document: DocumentModel) {
            // Tambahkan informasi format jika tersedia
            if (document.extension != null) {
                documentNameTextView.text = "${document.name} (${document.extension.toUpperCase(Locale.ROOT)})"
            } else {
                documentNameTextView.text = document.name
            }

            // If document is null, disable the view button
            if (document.path == null) {
                viewDocumentButton.text = "Not Available"
                viewDocumentButton.isEnabled = false
            } else {
                viewDocumentButton.text = "Download"
                viewDocumentButton.isEnabled = true

                // Perbaikan pada DocumentsAdapter.kt, method bind pada DocumentViewHolder
                viewDocumentButton.setOnClickListener {
                    // Get download URL from Firebase Storage and open document
                    val storage = FirebaseStorage.getInstance()
                    val documentRef = storage.reference.child(document.path)

                    documentRef.downloadUrl.addOnSuccessListener { uri ->
                        try {
                            // Download file ke penyimpanan sementara dahulu
                            val request = DownloadManager.Request(uri)
                            val fileName = "${document.name}.${document.extension ?: "bin"}"
                            request.setDestinationInExternalFilesDir(itemView.context, Environment.DIRECTORY_DOWNLOADS, fileName)
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                            val downloadManager = itemView.context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val downloadId = downloadManager.enqueue(request)

                            showToast(itemView.context, "Downloading ${document.name}...")

                            // Buat BroadcastReceiver untuk mendeteksi ketika download selesai
                            val downloadReceiver = object : BroadcastReceiver() {
                                override fun onReceive(context: Context, intent: Intent) {
                                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                                    if (id == downloadId) {
                                        // Download selesai, buka file
                                        val query = DownloadManager.Query().setFilterById(downloadId)
                                        val cursor = downloadManager.query(query)
                                        if (cursor.moveToFirst()) {
                                            val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                            if (DownloadManager.STATUS_SUCCESSFUL == cursor.getInt(columnIndex)) {
                                                val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                                                val localUri = Uri.parse(cursor.getString(uriIndex))

                                                // Tentukan MIME type berdasarkan ekstensi
                                                val mimeType = when (document.extension?.toLowerCase(Locale.ROOT)) {
                                                    "pdf" -> "application/pdf"
                                                    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                                                    "doc" -> "application/msword"
                                                    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                                    "xls" -> "application/vnd.ms-excel"
                                                    else -> document.mimeType ?: "application/octet-stream"
                                                }

                                                // Buka file yang didownload
                                                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(localUri, mimeType)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(openIntent, "Open with"))
                                            }
                                        }
                                        cursor.close()

                                        // Unregister receiver
                                        context.unregisterReceiver(this)
                                    }
                                }
                            }

                            // Register receiver untuk download complete
                            itemView.context.registerReceiver(
                                downloadReceiver,
                                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                                Context.RECEIVER_NOT_EXPORTED // Ensure the receiver is not exposed outside the app
                            )

                        } catch (e: Exception) {
                            android.util.Log.e("DocumentsAdapter", "Error downloading document: ${e.message}", e)
                            showToast(itemView.context, "Error downloading document: ${e.message}")
                        }
                    }.addOnFailureListener { e ->
                        showToast(itemView.context, "Error loading document: ${e.message}")
                    }
                }
            }
        }

        private fun showToast(context: android.content.Context, message: String) {
            android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DocumentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        val document = documentsList[position]
        holder.bind(document)
    }

    override fun getItemCount(): Int {
        return documentsList.size
    }
}