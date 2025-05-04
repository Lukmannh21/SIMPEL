package com.mbkm.telgo

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.atomic.AtomicInteger

class TelgoFirebaseMessagingService : FirebaseMessagingService() {

    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "TelgoMessagingService"

    // Counter for notification IDs
    companion object {
        private val notificationIdCounter = AtomicInteger(100)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains data
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            val data = remoteMessage.data

            // Get event details from data
            val eventType = data["eventType"] ?: ""
            val siteId = data["siteId"] ?: ""
            val witel = data["witel"] ?: ""
            val eventDate = data["eventDate"] ?: ""
            val title = data["title"] ?: "TelGo Event"
            val message = data["message"] ?: "You have an upcoming event"

            // Show notification
            when (eventType.lowercase()) {
                "toc" -> {
                    NotificationHelper.showEventNotification(
                        this,
                        getNextNotificationId(),
                        title,
                        message,
                        NotificationHelper.NOTIFICATION_TYPE_TOC,
                        siteId,
                        witel,
                        eventDate
                    )
                }
                "plan_oa" -> {
                    NotificationHelper.showEventNotification(
                        this,
                        getNextNotificationId(),
                        title,
                        message,
                        NotificationHelper.NOTIFICATION_TYPE_PLAN_OA,
                        siteId,
                        witel,
                        eventDate
                    )
                }
                else -> {
                    // Generic notification
                    NotificationHelper.showEventNotification(
                        this,
                        getNextNotificationId(),
                        title,
                        message,
                        "default",
                        siteId,
                        witel,
                        eventDate
                    )
                }
            }
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message notification body: ${it.body}")

            // If we get a notification directly, create our own custom notification with it
            NotificationHelper.showEventNotification(
                this,
                getNextNotificationId(),
                it.title ?: "TelGo Notification",
                it.body ?: "You have a new notification",
                "default",
                "",
                "",
                ""
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed token: $token")

        // Send new token to the server
        sendRegistrationTokenToServer(token)
    }

    private fun sendRegistrationTokenToServer(token: String) {
        // Update user's FCM token in Firestore
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        user?.let { currentUser ->
            firestore.collection("users").document(currentUser.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token successfully updated")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to update FCM token", e)
                }
        }
    }

    private fun getNextNotificationId(): Int {
        return notificationIdCounter.incrementAndGet()
    }
}