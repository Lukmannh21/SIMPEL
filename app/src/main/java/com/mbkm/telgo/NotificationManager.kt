package com.mbkm.telgo

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationManager {
    private const val NOTIFICATION_WORK_NAME = "telgo_notification_work"
    private const val IMMEDIATE_NOTIFICATION_WORK_NAME = "telgo_immediate_notification_check"
    private const val TAG = "NotificationManager"

    // Add these variables directly inside the object
    private var lastRunTimestamp = 0L
    private const val MIN_RUN_INTERVAL = 30 * 60 * 1000 // 30 minutes in milliseconds

    // Initialize notifications system
    fun initialize(context: Context) {
        // Create notification channels
        NotificationHelper.createNotificationChannels(context)

        // Schedule notification worker
        scheduleNotifications(context)

        Log.d(TAG, "Notification system initialized")
    }

    // Schedule periodic notifications check
    fun scheduleNotifications(context: Context) {
        // Create network constraints (preferably with network, but can run without)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create periodic work request that runs every 6 hours
        // Note: Minimum interval for periodic work is 15 minutes
        val notificationRequest = PeriodicWorkRequest.Builder(NotificationWorker::class.java,
            6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(15, TimeUnit.MINUTES) // Small initial delay
            .build()

        // Enqueue the work
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NOTIFICATION_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Update existing work if exists
            notificationRequest
        )

        Log.d(TAG, "Notification worker scheduled for every 6 hours")
    }
    fun setupNotificationsWithoutForegroundService(context: Context) {
        try {
            // Schedule WorkManager tasks
            scheduleNotifications(context)

            // Schedule direct alarm for reliability
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, NotificationAlarmReceiver::class.java)

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Schedule first check in 1 hour
            val oneHour = 60 * 60 * 1000L
            val triggerTime = System.currentTimeMillis() + oneHour

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error setting exact alarm, falling back to inexact", e)
                    alarmManager.set(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }

            Log.d(TAG, "Background alarm scheduled for notification checks")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up notifications: ${e.message}", e)
        }
    }

    // Force an immediate check for notifications
    fun checkNotificationsNow(context: Context) {
        Log.d(TAG, "Performing immediate notification check")

        val oneTimeRequest = OneTimeWorkRequest.Builder(NotificationWorker::class.java)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_NOTIFICATION_WORK_NAME,
            ExistingWorkPolicy.REPLACE, // Replace any existing immediate check
            oneTimeRequest
        )
    }

    // Cancel all scheduled notifications
    fun cancelNotifications(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NOTIFICATION_WORK_NAME)
        Log.d(TAG, "Notification worker cancelled")
    }

    /**
     * Clears all notification data and prevents new notifications from showing for a period
     */
    fun clearNotificationData(context: Context) {
        // Cancel any scheduled immediate notification check
        WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_NOTIFICATION_WORK_NAME)

        // Update last run timestamp to prevent immediate re-checking
        lastRunTimestamp = System.currentTimeMillis()

        // Clear any notification badges
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancelAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing system notifications", e)
        }

        // Clear local notification cache if any
        try {
            val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putLong("last_clear_time", System.currentTimeMillis())
                .putBoolean("notifications_cleared", true)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification preferences", e)
        }

        Log.d(TAG, "Notification data cleared successfully")
    }

    /**
     * Checks if we should be showing notifications right now
     * Returns false if notifications were recently cleared
     */
    fun shouldShowNotifications(context: Context): Boolean {
        // If notifications were cleared recently, don't show new ones
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastRunTimestamp < MIN_RUN_INTERVAL) {
            Log.d(TAG, "Skipping notifications - cleared recently")
            return false
        }

        try {
            val sharedPrefs = context.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)
            val lastClearTime = sharedPrefs.getLong("last_clear_time", 0)
            val notificationsCleared = sharedPrefs.getBoolean("notifications_cleared", false)

            if (notificationsCleared && (currentTime - lastClearTime < MIN_RUN_INTERVAL)) {
                Log.d(TAG, "Notifications were cleared ${(currentTime - lastClearTime) / 1000} seconds ago, skipping")
                return false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification state", e)
        }

        return true
    }

    /**
     * Ensure notifications will run reliably in the background
     */
    fun ensureReliableBackgroundOperation(context: Context) {
        // Schedule repeating alarms that will work even with battery optimization
        scheduleBackgroundAlarm(context)

        // Also schedule the regular WorkManager tasks as fallback
        scheduleNotifications(context)

        Log.d(TAG, "Reliable background operation set up")
    }

    /**
     * Schedules a repeating alarm to check for notifications
     */
    private fun scheduleBackgroundAlarm(context: Context) {
        // Create an alarm that will trigger NotificationAlarmReceiver
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, NotificationAlarmReceiver::class.java)

        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            0,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        // Set alarm to first trigger in 2 hours
        val twoHours = 2 * 60 * 60 * 1000L
        val triggerTime = System.currentTimeMillis() + twoHours

        // Schedule alarm based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
                Log.d(TAG, "Set exact alarm for background notification check")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set exact alarm, using inexact instead", e)
                alarmManager.setInexactRepeating(
                    android.app.AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    twoHours,
                    pendingIntent
                )
            }
        } else {
            alarmManager.setInexactRepeating(
                android.app.AlarmManager.RTC_WAKEUP,
                triggerTime,
                twoHours,
                pendingIntent
            )
        }

        Log.d(TAG, "Background alarm scheduled for notification checks")
    }

    /**
     * Checks if app should request battery optimization exemption
     * You can call this from a settings page
     */
    fun shouldRequestBatteryOptimization(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = context.packageName

            return !powerManager.isIgnoringBatteryOptimizations(packageName)
        }
        return false
    }

    /**
     * Request battery optimization exemption
     * Call from a user-initiated action like a button click
     */
    fun requestBatteryOptimizationExemption(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
                Log.d(TAG, "Requested battery optimization exemption")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request battery optimization exemption", e)
            }
        }
    }
}