package com.mbkm.telgo

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.storage.FirebaseStorage
import java.util.Locale
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log

class DocumentsAdapter(
    private val documentsList: ArrayList<DocumentModel>
) : RecyclerView.Adapter<DocumentsAdapter.DocumentViewHolder>() {

    inner class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val documentNameTextView: TextView = itemView.findViewById(R.id.tvDocumentName)
        val documentTypeTextView: TextView = itemView.findViewById(R.id.tvDocumentType)
        val viewDocumentButton: MaterialButton = itemView.findViewById(R.id.btnViewDocument)
        val documentIcon: ImageView = itemView.findViewById(R.id.ivDocumentIcon)

        fun bind(document: DocumentModel) {
            // Set document name without extension in it
            documentNameTextView.text = document.name

            // Set extension text separately
            documentTypeTextView.text = document.extension?.toUpperCase(Locale.ROOT) ?: "N/A"

            // Set document icon based on file type
            setDocumentIcon(document.extension)

            // If document is null, disable the view button
            if (document.path == null) {
                viewDocumentButton.text = "Not Available"
                viewDocumentButton.isEnabled = false
                viewDocumentButton.alpha = 0.5f
                documentIcon.alpha = 0.5f
                documentTypeTextView.alpha = 0.5f
            } else {
                viewDocumentButton.text = "Download"
                viewDocumentButton.isEnabled = true
                viewDocumentButton.alpha = 1f
                documentIcon.alpha = 1f
                documentTypeTextView.alpha = 1f

                viewDocumentButton.setOnClickListener {
                    // Show download indicator
                    viewDocumentButton.isEnabled = false
                    viewDocumentButton.text = "Downloading..."

                    // Get download URL from Firebase Storage and open document
                    val storage = FirebaseStorage.getInstance()
                    val documentRef = storage.reference.child(document.path)

                    documentRef.downloadUrl.addOnSuccessListener { uri ->
                        try {
                            // Download file ke penyimpanan sementara
                            val request = DownloadManager.Request(uri)
                            val fileName = "${document.name}.${document.extension ?: "bin"}"
                            request.setDestinationInExternalFilesDir(itemView.context, Environment.DIRECTORY_DOWNLOADS, fileName)
                            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                            val downloadManager = itemView.context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                            val downloadId = downloadManager.enqueue(request)

                            showToast(itemView.context, "Downloading ${document.name}...")

                            // BroadcastReceiver untuk mendeteksi ketika download selesai
                            val downloadReceiver = object : BroadcastReceiver() {
                                override fun onReceive(context: Context, intent: Intent) {
                                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                                    if (id == downloadId) {
                                        // Reset button state
                                        viewDocumentButton.isEnabled = true
                                        viewDocumentButton.text = "Download"

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
                                                try {
                                                    context.startActivity(Intent.createChooser(openIntent, "Open with"))
                                                } catch (e: Exception) {
                                                    showToast(context, "No app found to open this file type")
                                                }
                                            }
                                        }
                                        cursor.close()

                                        // Unregister receiver
                                        try {
                                            context.unregisterReceiver(this)
                                        } catch (e: Exception) {
                                            Log.e("DocumentsAdapter", "Error unregistering receiver: ${e.message}")
                                        }
                                    }
                                }
                            }

                            // Register receiver untuk download complete
                            try {
                                itemView.context.registerReceiver(
                                    downloadReceiver,
                                    IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                                    Context.RECEIVER_NOT_EXPORTED
                                )
                            } catch (e: Exception) {
                                viewDocumentButton.isEnabled = true
                                viewDocumentButton.text = "Download"
                                Log.e("DocumentsAdapter", "Error registering receiver: ${e.message}")
                                showToast(itemView.context, "Error setting up download: ${e.message}")
                            }

                        } catch (e: Exception) {
                            viewDocumentButton.isEnabled = true
                            viewDocumentButton.text = "Download"
                            Log.e("DocumentsAdapter", "Error downloading document: ${e.message}", e)
                            showToast(itemView.context, "Error downloading document: ${e.message}")
                        }
                    }.addOnFailureListener { e ->
                        viewDocumentButton.isEnabled = true
                        viewDocumentButton.text = "Download"
                        showToast(itemView.context, "Error loading document: ${e.message}")
                    }
                }
            }
        }

        /**
         * Set document icon based on file extension
         */
        private fun setDocumentIcon(extension: String?) {
            when (extension?.toLowerCase(Locale.ROOT)) {
                "pdf" -> {
                    documentIcon.setImageResource(android.R.drawable.ic_menu_agenda)
                    documentIcon.setColorFilter(itemView.context.getColor(R.color.purple_500))
                }
                "doc", "docx" -> {
                    documentIcon.setImageResource(android.R.drawable.ic_menu_edit)
                    documentIcon.setColorFilter(itemView.context.getColor(android.R.color.holo_blue_dark))
                }
                "xls", "xlsx" -> {
                    documentIcon.setImageResource(android.R.drawable.ic_menu_sort_by_size)
                    documentIcon.setColorFilter(itemView.context.getColor(android.R.color.holo_green_dark))
                }
                null -> {
                    documentIcon.setImageResource(android.R.drawable.ic_menu_help)
                    documentIcon.setColorFilter(itemView.context.getColor(android.R.color.darker_gray))
                }
                else -> {
                    documentIcon.setImageResource(android.R.drawable.ic_menu_agenda)
                    documentIcon.setColorFilter(itemView.context.getColor(android.R.color.darker_gray))
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