package com.mbkm.telgo

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage

class DocumentsAdapter(
    private val documentsList: ArrayList<DocumentModel>
) : RecyclerView.Adapter<DocumentsAdapter.DocumentViewHolder>() {

    inner class DocumentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val documentNameTextView: TextView = itemView.findViewById(R.id.tvDocumentName)
        val viewDocumentButton: Button = itemView.findViewById(R.id.btnViewDocument)

        fun bind(document: DocumentModel) {
            documentNameTextView.text = document.name

            // If document is null, disable the view button
            if (document.path == null) {
                viewDocumentButton.text = "Not Available"
                viewDocumentButton.isEnabled = false
            } else {
                viewDocumentButton.text = "View PDF"
                viewDocumentButton.isEnabled = true

                viewDocumentButton.setOnClickListener {
                    // Get download URL from Firebase Storage and open PDF
                    val storage = FirebaseStorage.getInstance()
                    val documentRef = storage.reference.child(document.path)

                    documentRef.downloadUrl.addOnSuccessListener { uri ->
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/pdf")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }

                        try {
                            itemView.context.startActivity(intent)
                        } catch (e: Exception) {
                            showToast(itemView.context, "No PDF viewer app found")
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
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_document, parent, false)
        return DocumentViewHolder(view)
    }

    override fun onBindViewHolder(holder: DocumentViewHolder, position: Int) {
        holder.bind(documentsList[position])
    }

    override fun getItemCount(): Int {
        return documentsList.size
    }
}