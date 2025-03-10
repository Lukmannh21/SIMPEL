package com.mbkm.telgo

data class ProjectModel(
    val siteId: String = "",
    val witel: String = "",
    val status: String = "",
    val lastIssueHistory: List<String> = emptyList(),
    val koordinat: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)
