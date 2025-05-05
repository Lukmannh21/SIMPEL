package com.mbkm.telgo

import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class NotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val TAG = "NotificationWorker"

    // Track when this worker last ran to prevent too frequent execution
    companion object {
        private var lastRunTimestamp = 0L
        private const val MIN_RUN_INTERVAL = 30 * 60 * 1000 // 30 minutes in milliseconds (reduced from 2 hours)
    }

    // Method for direct calling outside WorkManager (from AlarmManager, etc.)
    fun checkNotificationsDirectly() {
        Log.d(TAG, "Starting direct notification check at ${Date()}")
        // Use a wake lock to ensure the operation completes even if device is sleeping
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TelGo:NotificationWakeLock"
        )
        wakeLock.acquire(5 * 60 * 1000L) // 5 minute max

        try {
            // Run the Worker synchronously
            runBlocking {
                doWork()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during direct notification check: ${e.message}", e)
        } finally {
            // Always release the wake lock
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    override suspend fun doWork(): Result {
        try {
            // Check if this worker has run recently
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastRunTimestamp < MIN_RUN_INTERVAL) {
                Log.d(TAG, "Skipping notification check - ran recently (${(currentTime - lastRunTimestamp) / 1000 / 60} minutes ago)")
                return Result.success()
            }

            // Update last run timestamp
            lastRunTimestamp = currentTime

            Log.d(TAG, "Starting notification check work at ${Date()}")

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.d(TAG, "No user logged in, skipping notification check")
                return Result.success()
            }

            val currentUserId = currentUser.uid
            Log.d(TAG, "Processing notifications for user: ${currentUserId}")

            // Create a calendar for today with time set to 00:00:00 for accurate date comparison
            val todayCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val todayDateString = dateFormat.format(todayCalendar.time)

            Log.d(TAG, "Checking notifications for date: $todayDateString")

            // Check if app notification preferences are enabled
            val notificationsEnabled = checkNotificationsEnabled(currentUserId)
            if (!notificationsEnabled) {
                Log.d(TAG, "Notifications are disabled in user preferences")
                return Result.success()
            }

            // Check if all notifications for this user were recently cleared
            val lastClearTime = getLastNotificationClearTime(currentUserId)
            if (lastClearTime > 0 && currentTime - lastClearTime < 4 * 60 * 60 * 1000) {
                Log.d(TAG, "Notifications were cleared recently, skipping check")
                return Result.success()
            }

            // Calculate relevant dates for notifications (H-7, H-3, H-1, H-day)
            val dayThresholds = listOf(0, 1, 3, 7)
            val dayMessages = mapOf(
                0 to "Today is the day!", // H-day
                1 to "Tomorrow is the day!", // H-1
                3 to "In 3 days", // H-3
                7 to "In a week" // H-7
            )

            // Get user's edited sites
            val userEditedSites = getUserEditedSites(currentUserId)
            if (userEditedSites.isEmpty()) {
                Log.d(TAG, "No edited sites found for user")
                return Result.success()
            }

            Log.d(TAG, "Found ${userEditedSites.size} sites for notification check")

            // Check for TOC events
            val tocEnabled = isCategoryEnabled(currentUserId, "toc_enabled", true)
            if (tocEnabled) {
                Log.d(TAG, "TOC notifications are enabled, checking TOC events")
                processUserSitesInBatches(
                    "toc",
                    "TOC Event",
                    userEditedSites,
                    dayThresholds,
                    dayMessages,
                    todayCalendar,
                    currentUserId
                )
            } else {
                Log.d(TAG, "TOC notifications are disabled, skipping")
            }

            // Check for Plan OA events
            val planOaEnabled = isCategoryEnabled(currentUserId, "plan_oa_enabled", true)
            if (planOaEnabled) {
                Log.d(TAG, "Plan OA notifications are enabled, checking Plan OA events")
                processUserSitesInBatches(
                    "tglPlanOa",
                    "Plan OA Event",
                    userEditedSites,
                    dayThresholds,
                    dayMessages,
                    todayCalendar,
                    currentUserId
                )
            } else {
                Log.d(TAG, "Plan OA notifications are disabled, skipping")
            }

            Log.d(TAG, "Notification check completed successfully")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notifications", e)
            return Result.retry()
        }
    }

    private suspend fun getUserEditedSites(userId: String): List<String> {
        try {
            Log.d(TAG, "Getting edited sites for user: $userId")

            // First try to get user's edited sites list from user document
            val userDocument = firestore.collection("users")
                .document(userId)
                .get()
                .await()

            if (userDocument.exists()) {
                val editedSites = userDocument.get("editedSites") as? List<String>
                if (!editedSites.isNullOrEmpty()) {
                    Log.d(TAG, "Found ${editedSites.size} edited sites in user document")
                    return editedSites
                }
            }

            // If no edited sites found in user document, look for sites where user is listed as lastUpdatedBy or uploadedBy
            val sitesByUpdatedBy = firestore.collection("projects")
                .whereEqualTo("lastUpdatedBy", userId)
                .get()
                .await()

            val sitesByUploadedBy = firestore.collection("projects")
                .whereEqualTo("uploadedBy", userId)
                .get()
                .await()

            val siteIds = mutableListOf<String>()

            // Add sites from lastUpdatedBy query
            sitesByUpdatedBy.documents.forEach { doc ->
                doc.getString("siteId")?.let { siteIds.add(it) }
            }

            // Add sites from uploadedBy query
            sitesByUploadedBy.documents.forEach { doc ->
                doc.getString("siteId")?.let { siteIds.add(it) }
            }

            val uniqueSiteIds = siteIds.distinct()
            Log.d(TAG, "Found ${uniqueSiteIds.size} sites by user update/upload history")

            return uniqueSiteIds

        } catch (e: Exception) {
            Log.e(TAG, "Error getting user's edited sites", e)
            return emptyList()
        }
    }

    private suspend fun processUserSitesInBatches(
        dateField: String,
        eventTypeName: String,
        userSites: List<String>,
        dayThresholds: List<Int>,
        dayMessages: Map<Int, String>,
        todayCalendar: Calendar,
        userId: String
    ) {
        try {
            Log.d(TAG, "Processing $eventTypeName events for ${userSites.size} sites")

            // Process sites in batches of 10 (Firestore limit for whereIn)
            for (i in userSites.indices step 10) {
                val batchSites = userSites.subList(i, minOf(i + 10, userSites.size))

                Log.d(TAG, "Processing batch ${i/10 + 1} with ${batchSites.size} sites")

                val sites = firestore.collection("projects")
                    .whereIn("siteId", batchSites)
                    .get()
                    .await()

                Log.d(TAG, "Retrieved ${sites.size()} sites from Firestore")

                for (site in sites.documents) {
                    val eventDateStr = site.getString(dateField)
                    if (eventDateStr.isNullOrEmpty()) {
                        continue
                    }

                    val siteId = site.getString("siteId") ?: continue
                    val witel = site.getString("witel") ?: continue

                    try {
                        // Calculate days remaining properly
                        val daysRemaining = calculateDaysRemaining(eventDateStr, todayCalendar)

                        Log.d(TAG, "Site $siteId has $daysRemaining days remaining for $eventTypeName")

                        // Check if this is one of our notification days
                        if (daysRemaining in dayThresholds) {
                            val notificationMessage = dayMessages[daysRemaining] ?: "Upcoming event"

                            // Create proper notification title based on accurate days remaining
                            val title = when (daysRemaining) {
                                0 -> "$eventTypeName Today!"
                                1 -> "$eventTypeName Tomorrow!"
                                else -> "$eventTypeName in $daysRemaining days"
                            }

                            // Create notification message
                            val message = "$notificationMessage $eventTypeName for site $siteId in $witel"

                            Log.d(TAG, "Found notification condition: $title - $message")

                            // Check if user has set to mute this specific event notification
                            if (isNotificationMuted(userId, siteId, dateField)) {
                                Log.d(TAG, "Notification for $siteId is muted, skipping")
                                continue
                            }

                            // Check if we've already sent this notification
                            if (hasRecentNotification(userId, siteId, eventDateStr, dateField, daysRemaining)) {
                                Log.d(TAG, "Skipping duplicate notification for $siteId with $daysRemaining days remaining")
                                continue
                            }

                            // Generate unique notification ID based on site and event type for consistency
                            val notificationId = generateConsistentNotificationId(siteId, eventTypeName, daysRemaining)

                            // Determine event type for the notification
                            val eventTypeCode = if (dateField == "toc") {
                                NotificationHelper.NOTIFICATION_TYPE_TOC
                            } else {
                                NotificationHelper.NOTIFICATION_TYPE_PLAN_OA
                            }

                            Log.d(TAG, "Sending notification for $siteId: $title ($daysRemaining days remaining) with ID $notificationId")

                            // Show the notification - use the application context for stability
                            withContext(Dispatchers.Main) {
                                try {
                                    NotificationHelper.showEventNotification(
                                        context.applicationContext,
                                        notificationId,
                                        title,
                                        message,
                                        eventTypeCode,
                                        siteId,
                                        witel,
                                        eventDateStr,
                                        daysRemaining
                                    )

                                    // Record the notification to prevent duplicates
                                    recordSentNotification(userId, siteId, eventDateStr, eventTypeCode, daysRemaining)

                                    Log.d(TAG, "Successfully sent notification for $siteId with ID $notificationId")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to show notification: ${e.message}", e)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing date $eventDateStr for site $siteId", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking $eventTypeName events", e)
        }
    }

    private fun calculateDaysRemaining(eventDateStr: String, todayCalendar: Calendar): Int {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val eventDate = dateFormat.parse(eventDateStr) ?: return -1

        // Create calendar for event with time set to 00:00:00
        val eventCalendar = Calendar.getInstance().apply {
            time = eventDate
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Calculate difference in days - properly accounting for time
        val diffInMillis = eventCalendar.timeInMillis - todayCalendar.timeInMillis
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    // Generate a consistent notification ID based on the site and event
    private fun generateConsistentNotificationId(siteId: String, eventType: String, daysRemaining: Int): Int {
        val idBase = "${siteId}_${eventType}_${daysRemaining}".hashCode()
        return Math.abs(idBase) % 10000 + 1000 // Keep between 1000-9999
    }

    // Record that we sent a notification to prevent duplicates
    private suspend fun recordSentNotification(
        userId: String,
        siteId: String,
        eventDate: String,
        eventType: String,
        daysRemaining: Int
    ) {
        try {
            val notification = hashMapOf(
                "userId" to userId,
                "siteId" to siteId,
                "eventDate" to eventDate,
                "eventType" to eventType,
                "daysBefore" to daysRemaining,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection("notifications")
                .add(notification)
                .await()

            Log.d(TAG, "Recorded sent notification for $siteId")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording sent notification", e)
        }
    }

    // Check if we've already sent this notification recently
    private suspend fun hasRecentNotification(
        userId: String,
        siteId: String,
        eventDate: String,
        eventField: String,
        daysRemaining: Int
    ): Boolean {
        try {
            // Check for existing notification in the past 24 hours with same parameters
            val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

            val eventType = if (eventField == "toc") {
                NotificationHelper.NOTIFICATION_TYPE_TOC
            } else {
                NotificationHelper.NOTIFICATION_TYPE_PLAN_OA
            }

            // Query for recent notifications for this event
            val result = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("siteId", siteId)
                .whereEqualTo("eventDate", eventDate)
                .whereEqualTo("eventType", eventType)
                .whereEqualTo("daysBefore", daysRemaining)
                .whereGreaterThan("timestamp", oneDayAgo)
                .get()
                .await()

            val hasPreviousNotification = !result.isEmpty
            if (hasPreviousNotification) {
                Log.d(TAG, "Found previous notification for $siteId sent ${(System.currentTimeMillis() - result.documents[0].getLong("timestamp")!!) / (60 * 60 * 1000)} hours ago")
            }

            return hasPreviousNotification
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for recent notifications", e)
            return false // If error, assume no recent notification to be safe
        }
    }

    // Get the timestamp of when notifications were last cleared
    private suspend fun getLastNotificationClearTime(userId: String): Long {
        try {
            val prefs = firestore.collection("user_preferences")
                .document(userId)
                .get()
                .await()

            return prefs?.getLong("last_notification_clear_time") ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Error getting last notification clear time", e)
            return 0L
        }
    }

    // Check if user has muted notifications for this specific event
    private suspend fun isNotificationMuted(userId: String, siteId: String, eventField: String): Boolean {
        try {
            val eventType = if (eventField == "toc") "toc_muted" else "plan_oa_muted"

            val prefs = firestore.collection("user_preferences")
                .document(userId)
                .get()
                .await()

            val mutedSites = prefs?.get(eventType) as? List<String>
            return mutedSites?.contains(siteId) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if notification is muted", e)
            return false
        }
    }

    // Check if notifications are generally enabled for the user
    private suspend fun checkNotificationsEnabled(userId: String): Boolean {
        try {
            val prefs = firestore.collection("user_preferences")
                .document(userId)
                .get()
                .await()

            // If notifications_enabled doesn't exist, default to true
            return prefs?.getBoolean("notifications_enabled") ?: true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if notifications are enabled", e)
            return true // Default to enabled if error
        }
    }

    // Check if a specific notification category is enabled
    private suspend fun isCategoryEnabled(userId: String, categoryKey: String, defaultValue: Boolean = true): Boolean {
        try {
            val prefs = firestore.collection("user_preferences")
                .document(userId)
                .get()
                .await()

            // If no document exists yet, create one with default values
            if (!prefs.exists()) {
                Log.d(TAG, "No preferences found for user, creating default preferences")
                val defaultPrefs = hashMapOf(
                    "notifications_enabled" to true,
                    "toc_enabled" to true,
                    "plan_oa_enabled" to true
                )
                firestore.collection("user_preferences")
                    .document(userId)
                    .set(defaultPrefs)
                    .await()

                return defaultValue
            }

            // Get the value, defaulting to provided default
            val enabled = prefs.getBoolean(categoryKey)
            Log.d(TAG, "Category $categoryKey enabled: ${enabled ?: defaultValue}")

            return enabled ?: defaultValue
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if category $categoryKey is enabled", e)
            return defaultValue // Default to provided value if error
        }
    }
}