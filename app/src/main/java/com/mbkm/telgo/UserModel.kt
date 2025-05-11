package com.mbkm.telgo

data class UserModel(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val nik: String = "",
    val companyName: String = "",
    val unit: String = "",
    val position: String = "",
    val phone: String = "",
    val role: String = "user",  // "user" or "admin"
    val status: String = "unverified",  // "unverified" or "verified"
    val registrationDate: Long = 0,
    val lastLoginDate: Long = 0,
    val editedSites: List<String> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = "",
    val createdBy: String = ""
)