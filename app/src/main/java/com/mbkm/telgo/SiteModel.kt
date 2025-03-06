package com.mbkm.telgo

/**
 * Data model representing a site in the TelGO app
 *
 * Last Updated: 2025-03-05 07:14:43 UTC
 * Updated By: Lukmannh21
 */
data class SiteModel(
    val siteId: String,
    val witel: String,
    val status: String,
    val lastIssue: String,
    val koordinat: String,
    val createdAt: String = "",
    val updatedAt: String = "",
    val uploadedBy: String = ""
)