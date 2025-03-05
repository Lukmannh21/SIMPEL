package com.mbkm.telgo.model

data class Project(
    val id: String = "",
    val siteId: String = "",
    val witel: String = "",
    val status: String = "",
    val lastIssueHistory: List<String> = listOf(),
    val koordinat: String = "",
    val uploadedBy: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)