package com.mbkm.telgo

/**
 * Data class representing a notification in the TelGo app.
 */
data class NotificationModel(
    val id: String,
    val title: String,
    val message: String,
    val eventType: String,
    val siteId: String,
    val witel: String,
    val eventDate: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val daysBefore: Int? = null
)