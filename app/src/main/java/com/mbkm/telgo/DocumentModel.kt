package com.mbkm.telgo

data class DocumentModel(
    val name: String,
    val type: String,
    val path: String?, // Path in Firebase Storage, null if document doesn't exist
    val extension: String? = null, // File extension (pdf, docx, xlsx, etc.)
    val mimeType: String? = null // MIME type for content handling
)