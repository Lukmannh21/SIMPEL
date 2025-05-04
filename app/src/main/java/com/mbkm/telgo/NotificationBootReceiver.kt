package com.mbkm.telgo

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class NotificationBootReceiver : BroadcastReceiver() {
    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted, initializing notifications")

            // Initialize notification system after boot
            NotificationManager.initialize(context)

            // Set up reliable background operation
            NotificationManager.ensureReliableBackgroundOperation(context)
        }
    }
}