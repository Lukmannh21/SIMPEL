package com.mbkm.telgo

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.atomic.AtomicBoolean

class NotificationForegroundService : Service() {
    private val NOTIFICATION_CHANNEL_ID = "telgo_service_channel"
    private val NOTIFICATION_ID = 1001
    private val handler = Handler(Looper.getMainLooper())
    private var isServiceRunning = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service starting")

        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        if (!isServiceRunning.getAndSet(true)) {
            // Start periodic notification check
            schedulePeriodicChecks()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning.set(false)
        Log.d(TAG, "Service destroyed")

        // Try to restart ourselves
        val restartIntent = Intent(this, NotificationForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent)
        } else {
            startService(restartIntent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TelGo Background Service"
            val descriptionText = "Keeps notifications working properly"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, ServicesActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("TelGo Active")
            .setContentText("Ensuring your notifications arrive on time")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun schedulePeriodicChecks() {
        // Periodic runnable to check for notifications
        val checkRunnable = object : Runnable {
            override fun run() {
                if (!isServiceRunning.get()) {
                    return
                }

                try {
                    // Trigger a notification check via WorkManager directly
                    Log.d(TAG, "Foreground service triggering notification check")
                    triggerNotificationCheck()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in periodic notification check", e)
                }

                // Re-schedule if still running
                if (isServiceRunning.get()) {
                    handler.postDelayed(this, CHECK_INTERVAL)
                }
            }
        }

        // Start the checks
        handler.postDelayed(checkRunnable, INITIAL_DELAY)
    }

    // Method to directly schedule a notification check using WorkManager
    private fun triggerNotificationCheck() {
        try {
            val oneTimeRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .build()

            WorkManager.getInstance(applicationContext).enqueueUniqueWork(
                "foreground_service_notification_check_${System.currentTimeMillis()}",
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )

            Log.d(TAG, "Scheduled notification check from foreground service")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling notification check: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "NotificationForegroundService"
        private const val CHECK_INTERVAL = 60 * 60 * 1000L // 1 hour
        private const val INITIAL_DELAY = 5 * 60 * 1000L // 5 minutes

        fun startService(context: Context) {
            try {
                val intent = Intent(context, NotificationForegroundService::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }

                Log.d(TAG, "Service start requested")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start service: ${e.message}", e)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, NotificationForegroundService::class.java)
            context.stopService(intent)
            Log.d(TAG, "Service stop requested")
        }
    }
}