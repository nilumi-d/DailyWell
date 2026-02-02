package com.example.wellnesmate.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.wellnesmate.data.repository.SharedPreferencesManager


class BootCompletedReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Check if we can schedule exact alarms
            if (HydrationAlarmScheduler.canScheduleExactAlarms(context)) {
                // Reschedule hydration alarms
                val prefsManager = SharedPreferencesManager.getInstance(context)
                val settings = prefsManager.getHydrationSettings()
                
                if (settings.reminderEnabled) {
                    HydrationAlarmScheduler.scheduleRecurringAlarm(context, settings)
                }
            }
            // If we can't schedule exact alarms, the alarms will remain unscheduled
            // The user will need to manually enable them in the app
        }
    }
}