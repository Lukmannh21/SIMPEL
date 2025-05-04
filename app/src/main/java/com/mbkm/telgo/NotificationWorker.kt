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
        private const val MIN_RUN_INTERVAL = 2 * 60 * 60 * 1000 // 2 hours in milliseconds
    }

    // Method for direct calling outside WorkManager (from AlarmManager, etc.)
    fun checkNotificationsDirectly() {
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
                Log.d(TAG, "Skipping notification check - ran recently")
                return Result.success()
            }

            // Update last run timestamp
            lastRunTimestamp = currentTime

            Log.d(TAG, "Starting notification check work")

            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.d(TAG, "No user logged in, skipping notification check")
                return Result.success()
            }

            val currentUserId = currentUser.uid

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
            val tocEnabled = isCategoryEnabled(currentUserId, "toc_enabled")
            if (tocEnabled) {
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
            val planOaEnabled = isCategoryEnabled(currentUserId, "plan_oa_enabled")
            if (planOaEnabled) {
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

                val sites = firestore.collection("projects")
                    .whereIn("siteId", batchSites)
                    .get()
                    .await()

                for (site in sites.documents) {
                    val eventDateStr = site.getString(dateField) ?: continue
                    val siteId = site.getString("siteId") ?: continue
                    val witel = site.getString("witel") ?: continue

                    try {
                        // Calculate days remaining properly
                        val daysRemaining = calculateDaysRemaining(eventDateStr, todayCalendar)

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

                            // Generate unique notification ID
                            val notificationId = Random.nextInt(1000, 9999)

                            // Determine event type for the notification
                            val eventTypeCode = if (dateField == "toc") {
                                NotificationHelper.NOTIFICATION_TYPE_TOC
                            } else {
                                NotificationHelper.NOTIFICATION_TYPE_PLAN_OA
                            }

                            Log.d(TAG, "Sending notification for $siteId: $title ($daysRemaining days remaining)")

                            // Show the notification - use the application context for stability
                            withContext(Dispatchers.Main) {
                                NotificationHelper.showEventNotification(
                                    context,
                                    notificationId,
                                    title,
                                    message,
                                    eventTypeCode,
                                    siteId,
                                    witel,
                                    eventDateStr,
                                    daysRemaining
                                )
                            }

                            // Log successful notification for debugging
                            Log.d(TAG, "Successfully sent notification for $siteId with ID $notificationId")
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

            return !result.isEmpty
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
    private suspend fun isCategoryEnabled(userId: String, categoryKey: String): Boolean {
        try {
            val prefs = firestore.collection("user_preferences")
                .document(userId)
                .get()
                .await()

            // If category doesn't exist, default to true
            return prefs?.getBoolean(categoryKey) ?: true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if category $categoryKey is enabled", e)
            return true // Default to enabled if error
        }
    }
}