package com.example.wellnesmate.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.wellnesmate.MainActivity
import com.example.wellnesmate.R
import com.example.wellnesmate.data.repository.SharedPreferencesManager

/**
 * WorkManager worker for hydration reminders
 */
class HydrationReminderWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        private const val CHANNEL_ID = "hydration_reminders"
        private const val NOTIFICATION_ID = 1001
    }

    override fun doWork(): Result {
        return try {
            val prefsManager = SharedPreferencesManager.getInstance(applicationContext)
            val settings = prefsManager.getHydrationSettings()
            
            // Check if reminders are enabled
            if (!settings.reminderEnabled) {
                return Result.success()
            }
            
            // Check if it's within the reminder time window
            val calendar = java.util.Calendar.getInstance()
            val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(java.util.Calendar.MINUTE)
            
            // Convert current time to minutes since midnight
            val currentTimeInMinutes = currentHour * 60 + currentMinute
            
            // Convert start and end times to minutes since midnight
            val startTimeInMinutes = settings.startTime * 60 + settings.startMinute
            val endTimeInMinutes = settings.endTime * 60 + settings.startMinute
            
            // Handle case where end time is next day (e.g., 22:00 to 08:00)
            val isWithinTimeWindow = if (endTimeInMinutes > startTimeInMinutes) {
                // Same day range (e.g., 08:00 to 22:00)
                currentTimeInMinutes >= startTimeInMinutes && currentTimeInMinutes < endTimeInMinutes
            } else {
                // Overnight range (e.g., 22:00 to 08:00)
                currentTimeInMinutes >= startTimeInMinutes || currentTimeInMinutes < endTimeInMinutes
            }
            
            if (!isWithinTimeWindow) {
                return Result.success()
            }
            
            // Check if user has already reached their goal today
            val todayIntake = prefsManager.getTodayTotalHydration()
            if (todayIntake >= settings.dailyGoalMl) {
                return Result.success()
            }
            
            createNotificationChannel()
            sendHydrationReminder()
            
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = applicationContext.getString(R.string.channel_hydration_name)
            val descriptionText = applicationContext.getString(R.string.channel_hydration_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun sendHydrationReminder() {
        // Create intent to open the app when notification is tapped
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_hydration", true) // Extra to navigate to hydration fragment
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle(applicationContext.getString(R.string.hydration_reminder))
            .setContentText(applicationContext.getString(R.string.time_to_drink_water))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(applicationContext.getString(R.string.stay_hydrated))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_water_drop,
                "Add Water",
                createAddWaterPendingIntent()
            )
            .build()
        
        // Show the notification
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun createAddWaterPendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_hydration", true)
            putExtra("quick_add_water", true)
        }
        
        return PendingIntent.getActivity(
            applicationContext,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}