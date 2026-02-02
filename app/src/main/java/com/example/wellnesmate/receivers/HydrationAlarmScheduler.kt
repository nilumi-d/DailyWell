package com.example.wellnesmate.receivers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.example.wellnesmate.data.models.HydrationSettings
import com.example.wellnesmate.data.repository.SharedPreferencesManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Scheduler for hydration alarm notifications
 */
object HydrationAlarmScheduler {
    
    fun scheduleNextAlarm(context: Context) {
        val prefsManager = SharedPreferencesManager.getInstance(context)
        val settings = prefsManager.getHydrationSettings()
        
        // Check if reminders are enabled
        if (!settings.reminderEnabled) {
            cancelAlarm(context)
            return
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HydrationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Calculate next alarm time for exact daily scheduling
        val nextAlarmTime = calculateNextExactAlarmTime(settings)
        
        // Set the alarm using AlarmManagerCompat for better compatibility
        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            nextAlarmTime.timeInMillis,
            pendingIntent
        )
    }
    
    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HydrationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
    }
    
    private fun calculateNextExactAlarmTime(settings: HydrationSettings): Calendar {
        val now = Calendar.getInstance()
        val alarmTime = Calendar.getInstance()
        
        // Set the alarm time to today at the specified hour and minute
        alarmTime.set(Calendar.HOUR_OF_DAY, settings.startTime)
        alarmTime.set(Calendar.MINUTE, settings.startMinute)
        alarmTime.set(Calendar.SECOND, 0)
        alarmTime.set(Calendar.MILLISECOND, 0)
        
        // If the alarm time has already passed today, set it for tomorrow
        if (alarmTime.before(now)) {
            alarmTime.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        return alarmTime
    }
    
    /**
     * Schedule recurring alarms at exact times
     */
    fun scheduleRecurringAlarm(context: Context, settings: HydrationSettings) {
        // Cancel any existing alarms
        cancelAlarm(context)
        
        // Check if reminders are enabled
        if (!settings.reminderEnabled) {
            return
        }
        
        // Schedule the exact time alarm
        scheduleNextAlarm(context)
    }
    
    /**
     * Get a formatted string showing when the next alarm is scheduled
     */
    fun getNextAlarmTimeFormatted(context: Context): String {
        val prefsManager = SharedPreferencesManager.getInstance(context)
        val settings = prefsManager.getHydrationSettings()
        
        if (!settings.reminderEnabled) {
            return "Reminders disabled"
        }
        
        val nextAlarm = calculateNextExactAlarmTime(settings)
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        return "Next reminder: ${formatter.format(nextAlarm.time)}"
    }
    
    /**
     * Schedule alarm at exact time with minute precision
     */
    fun scheduleRecurringAlarmWithMinutes(context: Context, settings: HydrationSettings, startMinute: Int) {
        // Cancel any existing alarms
        cancelAlarm(context)
        
        // Check if reminders are enabled
        if (!settings.reminderEnabled) {
            return
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HydrationAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Calculate exact alarm time with minute precision
        val alarmTime = Calendar.getInstance()
        alarmTime.set(Calendar.HOUR_OF_DAY, settings.startTime)
        alarmTime.set(Calendar.MINUTE, startMinute)
        alarmTime.set(Calendar.SECOND, 0)
        alarmTime.set(Calendar.MILLISECOND, 0)
        
        val now = Calendar.getInstance()
        // If the specified time has already passed today, schedule for tomorrow
        if (alarmTime.before(now)) {
            alarmTime.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        // Schedule the alarm at the exact time
        AlarmManagerCompat.setExactAndAllowWhileIdle(
            alarmManager,
            AlarmManager.RTC_WAKEUP,
            alarmTime.timeInMillis,
            pendingIntent
        )
    }
    
    /**
     * Check if the app can schedule exact alarms
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Older versions don't have this restriction
        }
    }
    
    /**
     * Request exact alarm permission (opens system settings on Android 12+)
     */
    fun requestExactAlarmPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}