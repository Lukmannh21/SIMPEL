package com.mbkm.telgo

data class ImageModel(
    val name: String,
    val type: String,
    val path: String? // Path in Firebase Storage, null if image doesn't exist
)