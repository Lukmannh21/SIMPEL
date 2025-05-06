package com.mbkm.telgo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object NotificationHelper {
    // Notification channel IDs
    const val CHANNEL_EVENTS = "events_channel"
    const val CHANNEL_REMINDERS = "reminders_channel"

    // Notification types
    const val NOTIFICATION_TYPE_TOC = "toc"
    const val NOTIFICATION_TYPE_PLAN_OA = "plan_oa"

    private const val TAG = "NotificationHelper"

    // Initialize notification channels
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Events channel - high importance
            val eventsChannel = NotificationChannel(
                CHANNEL_EVENTS,
                "Event Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications about TOC and Plan OA events"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            // Reminders channel - default importance
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Event Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for upcoming TOC and Plan OA events"
                enableLights(true)
                lightColor = Color.YELLOW
            }

            // Register the channels
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(eventsChannel)
            notificationManager.createNotificationChannel(remindersChannel)
        }
    }

    // Show event notification
    fun showEventNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        eventType: String,
        siteId: String,
        witel: String,
        eventDate: String,
        daysBefore: Int = -1
    ) {
        // Check global notification lock first
        if (!com.mbkm.telgo.NotificationManager.canShowNotification(context)) {
            Log.d(TAG, "⛔ Notification blocked by global timestamp lock")

            // Still save to Firestore for in-app viewing
            saveNotificationToFirestore(context, title, message, eventType, siteId, witel, eventDate, daysBefore)
            return
        }

        // Continue with existing permission checks
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Notification permission not granted, cannot show notification")
            // Save the notification to Firestore so it can be shown in the app later
            saveNotificationToFirestore(context, title, message, eventType, siteId, witel, eventDate, daysBefore)
            return
        }

        val intent = Intent(context, ServicesActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("SHOW_EVENTS", true)
            putExtra("EVENT_TYPE", eventType)
            putExtra("SITE_ID", siteId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val largeIcon = BitmapFactory.decodeResource(context.resources, R.drawable.logo_baru)

        val notificationColor = when(eventType) {
            NOTIFICATION_TYPE_TOC -> ContextCompat.getColor(context, R.color.toc_event_color)
            NOTIFICATION_TYPE_PLAN_OA -> ContextCompat.getColor(context, R.color.plan_oa_color)
            else -> ContextCompat.getColor(context, R.color.colorPrimary)
        }

        val channelId = if (isWithinOneDay(eventDate)) CHANNEL_EVENTS else CHANNEL_REMINDERS

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(message)
            .setColor(notificationColor)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "$message\nSite ID: $siteId\nWitel: $witel\nDate: $eventDate"
            ))
            .build()

        try {
            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(notificationId, notification)

                    // Record that we showed a notification in the global timestamp
                    com.mbkm.telgo.NotificationManager.recordNotificationShown(context, eventType)

                    Log.d(TAG, "✅ Notification shown with ID: $notificationId")
                }
            }
            // Also save to Firestore for display in the app
            saveNotificationToFirestore(context, title, message, eventType, siteId, witel, eventDate, daysBefore)
        } catch (e: SecurityException) {
            Log.e(TAG, "Error posting notification: ${e.message}")
            // Save the notification to Firestore so it can be shown in the app later
            saveNotificationToFirestore(context, title, message, eventType, siteId, witel, eventDate, daysBefore)
        }
    }

    // Check if notification permission is granted
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and higher, check for the POST_NOTIFICATIONS permission
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and lower, notification permissions were granted at install time
            true
        }
    }

    // Helper function to determine if date is within the next 24 hours
    private fun isWithinOneDay(dateString: String): Boolean {
        try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val eventDate = format.parse(dateString) ?: return false

            val now = Calendar.getInstance().time
            val difference = eventDate.time - now.time
            val daysDifference = TimeUnit.MILLISECONDS.toDays(difference)

            return daysDifference <= 1
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date: $dateString", e)
            return false
        }
    }

    // Save notification to Firestore for in-app display
    private fun saveNotificationToFirestore(
        context: Context,
        title: String,
        message: String,
        eventType: String,
        siteId: String,
        witel: String,
        eventDate: String,
        daysBefore: Int? = null
    ) {
        try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser ?: return

            val notification = hashMapOf(
                "userId" to user.uid,
                "title" to title,
                "message" to message,
                "eventType" to eventType,
                "siteId" to siteId,
                "witel" to witel,
                "eventDate" to eventDate,
                "timestamp" to System.currentTimeMillis(),
                "isRead" to false,
                "daysBefore" to daysBefore
            )

            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("notifications")
                .add(notification)
                .addOnSuccessListener {
                    Log.d(TAG, "Notification saved to Firestore")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error saving notification to Firestore", e)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notification", e)
        }
    }

    // Calculate days until event for reminders
    fun getDaysUntilEvent(eventDateStr: String): Int {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val eventDate = dateFormat.parse(eventDateStr) ?: return -1
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            // Calculate difference in days
            val diffInMillis = eventDate.time - today.time
            return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating days until event", e)
            return -1
        }
    }

    // Check if we already sent a similar notification recently
    suspend fun hasRecentNotification(
        siteId: String,
        eventDate: String,
        eventType: String,
        daysBefore: Int
    ): Boolean {
        try {
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser ?: return false

            // Look for notifications in the past 24 hours
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

            val firestore = FirebaseFirestore.getInstance()
            val result = firestore.collection("notifications")
                .whereEqualTo("userId", user.uid)
                .whereEqualTo("siteId", siteId)
                .whereEqualTo("eventDate", eventDate)
                .whereEqualTo("eventType", eventType)
                .whereEqualTo("daysBefore", daysBefore)
                .whereGreaterThan("timestamp", oneDayAgo)
                .get()
                .await()

            return !result.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for recent notifications", e)
            return false
        }
    }
}