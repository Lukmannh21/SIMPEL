package com.mbkm.telgo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Date

class NotificationAlarmReceiver : BroadcastReceiver() {

    private val TAG = "NotificationAlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm triggered, checking for notifications at ${Date()}")

        // Acquire a wake lock to make sure we complete our work
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "TelGo:NotificationAlarmWakeLock"
        )
        wakeLock.acquire(3 * 60 * 1000L) // 3 minutes max

        try {
            // Schedule via WorkManager for immediate execution
            val immediateCheckRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "immediate_notification_check_${System.currentTimeMillis()}",
                ExistingWorkPolicy.REPLACE,
                immediateCheckRequest
            )

            Log.d(TAG, "Scheduled immediate notification check via WorkManager")

            // Schedule the next alarm (in 4 hours) - this is in addition to the daily checks
            scheduleNextAlarm(context)

        } finally {
            // Make sure to release the wake lock
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun scheduleNextAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationAlarmReceiver::class.java)
        intent.action = "PERIODIC_CHECK"

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0, // Use standard request code for periodic checks
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the next check in 4 hours
        val fourHours = 4 * 60 * 60 * 1000L
        val triggerTime = System.currentTimeMillis() + fourHours

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error setting exact alarm, falling back to inexact", e)
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }

        Log.d(TAG, "Next notification check scheduled for ${Date(triggerTime)}")
    }
}