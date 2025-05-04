package com.mbkm.telgo

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Date

class NotificationAlarmReceiver : BroadcastReceiver() {

    private val TAG = "NotificationAlarmReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm triggered, checking for notifications at ${Date()}")

        // Use WorkManager to schedule an immediate notification check
        val immediateCheckRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "immediate_notification_check",
            ExistingWorkPolicy.REPLACE,
            immediateCheckRequest
        )

        Log.d(TAG, "Scheduled immediate notification check via WorkManager")

        // Schedule the next alarm
        scheduleNextAlarm(context)
    }

    private fun scheduleNextAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationAlarmReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the next check in 2 hours
        val twoHours = 2 * 60 * 60 * 1000L
        val triggerTime = System.currentTimeMillis() + twoHours

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