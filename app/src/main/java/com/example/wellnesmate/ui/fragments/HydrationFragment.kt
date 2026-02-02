package com.example.wellnesmate.ui.fragments

import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.wellnesmate.MainActivity
import com.example.wellnesmate.R
import com.example.wellnesmate.data.models.HydrationIntake
import com.example.wellnesmate.data.models.HydrationSettings
import com.example.wellnesmate.data.repository.SharedPreferencesManager
import com.example.wellnesmate.receivers.HydrationAlarmScheduler
import com.example.wellnesmate.ui.adapters.HydrationHistoryAdapter
import com.example.wellnesmate.workers.HydrationReminderWorker
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Fragment for hydration tracking and reminders
 */
class HydrationFragment : Fragment() {
    
    private lateinit var tvDailyGoal: TextView
    private lateinit var tvCurrentIntake: TextView
    private lateinit var tvProgressText: TextView
    private lateinit var tvEncouragement: TextView
    private lateinit var waterBottleView: com.example.wellnesmate.views.WaterBottleView
    private lateinit var btnAddWater: MaterialButton
    private lateinit var btnSetReminder: MaterialButton
    private lateinit var recyclerHydrationHistory: RecyclerView
    
    private val encouragementMessages = listOf(
        "Keep going! 💧",
        "Stay hydrated! 💦",
        "You're doing great! 🌊",
        "Almost there! 🌧️",
        "Hydration hero! 🦸",
        "Water you waiting for? 😄"
    )
    private var currentEncouragementIndex = 0
    
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var hydrationHistoryAdapter: HydrationHistoryAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hydration, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize components
        initializeViews(view)
        setupHydrationHistory()
        setupClickListeners()
        updateHydrationDisplay()
    }
    
    override fun onResume() {
        super.onResume()
        updateHydrationDisplay()
    }
    
    private fun initializeViews(view: View) {
        prefsManager = SharedPreferencesManager.getInstance(requireContext())
        tvDailyGoal = view.findViewById(R.id.tv_daily_goal)
        tvCurrentIntake = view.findViewById(R.id.tv_current_intake)
        tvProgressText = view.findViewById(R.id.tv_progress_text)
        tvEncouragement = view.findViewById(R.id.tv_encouragement)
        waterBottleView = view.findViewById(R.id.waterBottleView)
        btnAddWater = view.findViewById(R.id.btn_add_water)
        btnSetReminder = view.findViewById(R.id.btn_set_reminder)
        recyclerHydrationHistory = view.findViewById(R.id.recycler_hydration_history)
    }
    
    private fun setupHydrationHistory() {
        hydrationHistoryAdapter = HydrationHistoryAdapter()
        
        recyclerHydrationHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = hydrationHistoryAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun setupClickListeners() {
        btnAddWater.setOnClickListener {
            showAddWaterDialog()
        }
        
        btnSetReminder.setOnClickListener {
            showReminderSettingsDialog()
        }
    }
    
    private fun updateHydrationDisplay() {
        val settings = prefsManager.getHydrationSettings()
        val todayIntake = prefsManager.getTodayTotalHydration()
        val todayIntakeList = prefsManager.getTodayHydrationIntake()
        
        // Update goal and current intake
        tvDailyGoal.text = "${settings.dailyGoalMl} ${getString(R.string.ml_unit)}"
        tvCurrentIntake.text = "$todayIntake ${getString(R.string.ml_unit)}"
        
        // Calculate progress (0.0 to 1.5 for extra water)
        val progress = if (settings.dailyGoalMl > 0) {
            (todayIntake.toFloat() / settings.dailyGoalMl).coerceIn(0f, 1.5f)
        } else {
            0f
        }
        
        // Update water bottle animation with current and goal amounts
        waterBottleView.setProgress(progress, todayIntake, settings.dailyGoalMl, true)
        
        // Update progress text
        val progressPercentage = (progress * 100).toInt()
        tvProgressText.text = "$progressPercentage% of daily goal"
        
        // Update history
        val allIntakes = prefsManager.getHydrationIntake().take(10) // Show last 10 entries
        hydrationHistoryAdapter.updateIntakes(allIntakes)
        
        // Show encouragement message
        showEncouragement(progress)
        
        // Check if goal is reached
        if (progress >= 1f) {
            // Show congratulations
            tvEncouragement.text = "Goal reached! 🎉"
            tvEncouragement.visibility = View.VISIBLE
            
            // Schedule a reset for the encouragement message
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isAdded) {
                    tvEncouragement.visibility = View.INVISIBLE
                }
            }, 3000)
        }
    }
    
    private fun showAddWaterDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_water, null)
        
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.et_water_amount)
        
        // Setup preset buttons
        val btnPreset200 = dialogView.findViewById<MaterialButton>(R.id.btn_preset_200)
        val btnPreset250 = dialogView.findViewById<MaterialButton>(R.id.btn_preset_250)
        val btnPreset300 = dialogView.findViewById<MaterialButton>(R.id.btn_preset_300)
        val btnPreset330 = dialogView.findViewById<MaterialButton>(R.id.btn_preset_330)
        val btnPreset500 = dialogView.findViewById<MaterialButton>(R.id.btn_preset_500)
        val btnPreset750 = dialogView.findViewById<MaterialButton>(R.id.btn_preset_750)
        
        // Set up click listeners for preset buttons
        btnPreset200.setOnClickListener { etAmount.setText("200") }
        btnPreset250.setOnClickListener { etAmount.setText("250") }
        btnPreset300.setOnClickListener { etAmount.setText("300") }
        btnPreset330.setOnClickListener { etAmount.setText("330") }
        btnPreset500.setOnClickListener { etAmount.setText("500") }
        btnPreset750.setOnClickListener { etAmount.setText("750") }
        
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_water))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val amountText = etAmount.text?.toString()
                if (!amountText.isNullOrBlank()) {
                    try {
                        val amount = amountText.toInt()
                        if (amount > 0) {
                            addWaterIntake(amount)
                        } else {
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Please enter a valid amount",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: NumberFormatException) {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Please enter a valid number",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Please enter an amount",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }
    
    private fun showEncouragement(progress: Float) {
        // Only show encouragement if progress is between 10% and 90%
        if (progress in 0.1f..0.9f) {
            // Show encouragement message with a 30% chance when adding water
            if ((0..100).random() < 30) {
                // Cycle through encouragement messages
                currentEncouragementIndex = (currentEncouragementIndex + 1) % encouragementMessages.size
                tvEncouragement.text = encouragementMessages[currentEncouragementIndex]
                tvEncouragement.visibility = View.VISIBLE
                
                // Hide the message after 3 seconds
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    if (isAdded) {
                        tvEncouragement.visibility = View.INVISIBLE
                    }
                }, 3000)
            }
        } else {
            tvEncouragement.visibility = View.INVISIBLE
        }
    }
    
    private fun addWaterIntake(amount: Int) {
        val intake = HydrationIntake(
            id = UUID.randomUUID().toString(),
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
            amountMl = amount
        )
        
        prefsManager.addHydrationIntake(intake)
        updateHydrationDisplay()
        
        // Check if goal is reached and send notification
        checkAndNotifyGoalReached()
        
        // Show success message with a water droplet emoji
        android.widget.Toast.makeText(
            requireContext(),
            "💧 Added $amount ml of water!",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun checkAndNotifyGoalReached() {
        val settings = prefsManager.getHydrationSettings()
        val todayIntake = prefsManager.getTodayTotalHydration()
        
        // Check if user just reached their goal
        if (todayIntake >= settings.dailyGoalMl) {
            sendGoalReachedNotification()
        }
    }
    
    private fun sendGoalReachedNotification() {
        // Create intent to open the app when notification is tapped
        val intent = Intent(requireContext(), MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_hydration", true)
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            requireContext(),
            2, // Different request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Get default alarm sound
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        
        // Create notification channel first
        createNotificationChannel()
        
        // Build the goal reached notification
        val notification = NotificationCompat.Builder(requireContext(), "hydration_reminders")
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle("Goal Reached! 🎉")
            .setContentText("Congratulations! You've reached your daily hydration goal.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Great job staying hydrated! You've reached your daily water intake goal.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setSound(soundUri) // Add alarm sound for goal reached
            .setVibrate(longArrayOf(0, 500, 250, 500)) // Vibrate pattern
            .build()
        
        // Show the notification
        try {
            NotificationManagerCompat.from(requireContext()).notify(1002, notification)
        } catch (e: SecurityException) {
            // Handle the case where notification permission is not granted
            android.widget.Toast.makeText(
                requireContext(),
                "Please enable notification permission to receive goal alerts",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_hydration_name)
            val descriptionText = getString(R.string.channel_hydration_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            // Create the NotificationChannel with sound and vibration
            val channel = NotificationChannel("hydration_reminders", name, importance).apply {
                description = descriptionText
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                
                // Set the default notification sound
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .build()
                setSound(soundUri, audioAttributes)
            }
            
            // Register the channel with the system
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showReminderSettingsDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_reminder_settings, null)
        
        val etGoal = dialogView.findViewById<TextInputEditText>(R.id.et_daily_goal)
        val tvReminderTime = dialogView.findViewById<TextView>(R.id.tv_reminder_time)
        val cardNotificationPermission = dialogView.findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_notification_permission)
        val btnGrantPermission = dialogView.findViewById<MaterialButton>(R.id.btn_grant_permission)
        
        // Frequency preset buttons
        val btnFreq30 = dialogView.findViewById<MaterialButton>(R.id.btn_freq_30)
        val btnFreq1 = dialogView.findViewById<MaterialButton>(R.id.btn_freq_1)
        val btnFreq2 = dialogView.findViewById<MaterialButton>(R.id.btn_freq_2)
        val btnFreq3 = dialogView.findViewById<MaterialButton>(R.id.btn_freq_3)
        val btnFreq4 = dialogView.findViewById<MaterialButton>(R.id.btn_freq_4)
        val btnFreqCustom = dialogView.findViewById<MaterialButton>(R.id.btn_freq_custom)
        
        val currentSettings = prefsManager.getHydrationSettings()
        
        // Set current values
        etGoal.setText(currentSettings.dailyGoalMl.toString())
        
        // Format reminder time (now with minutes)
        val reminderTimeText = String.format("%02d:%02d", currentSettings.startTime, currentSettings.startMinute)
        tvReminderTime.text = reminderTimeText
        
        // Variable to track selected frequency
        var selectedFrequency = currentSettings.reminderIntervalMinutes
        
        // Variable to track selected time (hour and minute)
        var selectedHour = currentSettings.startTime
        var selectedMinute = currentSettings.startMinute
        
        // Set up frequency button listeners
        btnFreq30.setOnClickListener { 
            selectedFrequency = 30
            // Update button states
            btnFreq30.setStrokeColorResource(R.color.primary_green)
            btnFreq1.setStrokeColorResource(R.color.text_hint)
            btnFreq2.setStrokeColorResource(R.color.text_hint)
            btnFreq3.setStrokeColorResource(R.color.text_hint)
            btnFreq4.setStrokeColorResource(R.color.text_hint)
            btnFreqCustom.setStrokeColorResource(R.color.text_hint)
        }
        
        btnFreq1.setOnClickListener { 
            selectedFrequency = 60
            // Update button states
            btnFreq30.setStrokeColorResource(R.color.text_hint)
            btnFreq1.setStrokeColorResource(R.color.primary_green)
            btnFreq2.setStrokeColorResource(R.color.text_hint)
            btnFreq3.setStrokeColorResource(R.color.text_hint)
            btnFreq4.setStrokeColorResource(R.color.text_hint)
            btnFreqCustom.setStrokeColorResource(R.color.text_hint)
        }
        
        btnFreq2.setOnClickListener { 
            selectedFrequency = 120
            // Update button states
            btnFreq30.setStrokeColorResource(R.color.text_hint)
            btnFreq1.setStrokeColorResource(R.color.text_hint)
            btnFreq2.setStrokeColorResource(R.color.primary_green)
            btnFreq3.setStrokeColorResource(R.color.text_hint)
            btnFreq4.setStrokeColorResource(R.color.text_hint)
            btnFreqCustom.setStrokeColorResource(R.color.text_hint)
        }
        
        btnFreq3.setOnClickListener { 
            selectedFrequency = 180
            // Update button states
            btnFreq30.setStrokeColorResource(R.color.text_hint)
            btnFreq1.setStrokeColorResource(R.color.text_hint)
            btnFreq2.setStrokeColorResource(R.color.text_hint)
            btnFreq3.setStrokeColorResource(R.color.primary_green)
            btnFreq4.setStrokeColorResource(R.color.text_hint)
            btnFreqCustom.setStrokeColorResource(R.color.text_hint)
        }
        
        btnFreq4.setOnClickListener { 
            selectedFrequency = 240
            // Update button states
            btnFreq30.setStrokeColorResource(R.color.text_hint)
            btnFreq1.setStrokeColorResource(R.color.text_hint)
            btnFreq2.setStrokeColorResource(R.color.text_hint)
            btnFreq3.setStrokeColorResource(R.color.text_hint)
            btnFreq4.setStrokeColorResource(R.color.primary_green)
            btnFreqCustom.setStrokeColorResource(R.color.text_hint)
        }
        
        btnFreqCustom.setOnClickListener { 
            selectedFrequency = 60 // Default custom to 1 hour
            // Update button states
            btnFreq30.setStrokeColorResource(R.color.text_hint)
            btnFreq1.setStrokeColorResource(R.color.text_hint)
            btnFreq2.setStrokeColorResource(R.color.text_hint)
            btnFreq3.setStrokeColorResource(R.color.text_hint)
            btnFreq4.setStrokeColorResource(R.color.text_hint)
            btnFreqCustom.setStrokeColorResource(R.color.primary_green)
        }
        
        // Set initial selected button based on current settings
        when (currentSettings.reminderIntervalMinutes) {
            30 -> {
                selectedFrequency = 30
                btnFreq30.setStrokeColorResource(R.color.primary_green)
            }
            60 -> {
                selectedFrequency = 60
                btnFreq1.setStrokeColorResource(R.color.primary_green)
            }
            120 -> {
                selectedFrequency = 120
                btnFreq2.setStrokeColorResource(R.color.primary_green)
            }
            180 -> {
                selectedFrequency = 180
                btnFreq3.setStrokeColorResource(R.color.primary_green)
            }
            240 -> {
                selectedFrequency = 240
                btnFreq4.setStrokeColorResource(R.color.primary_green)
            }
            else -> {
                selectedFrequency = 60
                btnFreqCustom.setStrokeColorResource(R.color.primary_green)
            }
        }
        
        // Set up time picker (now with minutes)
        tvReminderTime.setOnClickListener {
            val timePicker = TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    selectedHour = hour
                    selectedMinute = minute
                    tvReminderTime.text = String.format("%02d:%02d", hour, minute)
                },
                selectedHour,
                selectedMinute,
                true
            )
            timePicker.show()
        }
        
        // Check notification permission and show/hide permission card
        if (checkNotificationPermission()) {
            cardNotificationPermission.visibility = View.GONE
        } else {
            cardNotificationPermission.visibility = View.VISIBLE
            btnGrantPermission.setOnClickListener {
                requestNotificationPermission()
            }
        }
        
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.set_reminder))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val goalText = etGoal.text?.toString()
                
                if (!goalText.isNullOrBlank()) {
                    try {
                        val newGoal = goalText.toInt()
                        
                        val newSettings = currentSettings.copy(
                            dailyGoalMl = newGoal,
                            reminderIntervalMinutes = selectedFrequency,
                            startTime = selectedHour,
                            startMinute = selectedMinute,
                            endTime = if (selectedHour + 1 > 23) 23 else selectedHour + 1 // Set end time to 1 hour after start
                        )
                        
                        prefsManager.saveHydrationSettings(newSettings)
                        
                        // Check for exact alarm permission on Android 12+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (HydrationAlarmScheduler.canScheduleExactAlarms(requireContext())) {
                                // Use the enhanced alarm scheduler with minute precision
                                HydrationAlarmScheduler.scheduleRecurringAlarmWithMinutes(
                                    requireContext(), 
                                    newSettings, 
                                    selectedMinute
                                )
                            } else {
                                // Request exact alarm permission
                                HydrationAlarmScheduler.requestExactAlarmPermission(requireContext())
                                android.widget.Toast.makeText(
                                    requireContext(),
                                    "Please grant exact alarm permission in settings",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                return@setPositiveButton
                            }
                        } else {
                            // For older versions, directly schedule the alarm
                            HydrationAlarmScheduler.scheduleRecurringAlarmWithMinutes(
                                requireContext(), 
                                newSettings, 
                                selectedMinute
                            )
                        }
                        
                        updateHydrationDisplay()
                        
                        // Show next alarm time
                        val nextAlarmText = HydrationAlarmScheduler.getNextAlarmTimeFormatted(requireContext())
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Settings saved. $nextAlarmText",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        
                    } catch (e: NumberFormatException) {
                        // Invalid number
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Please enter a valid number for the goal",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Permissions are automatically granted on older versions
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Only request if we don't have permission
            if (!checkNotificationPermission()) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            } else {
                // If we already have permission, ensure channel is created
                createNotificationChannel()
            }
        } else {
            // For older versions, just create the notification channel
            createNotificationChannel()
        }
    }
    
    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1001 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Notification permission granted",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Notification permission denied",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    
    private fun setupHydrationReminders(settings: HydrationSettings) {
        if (!settings.reminderEnabled) {
            HydrationAlarmScheduler.cancelAlarm(requireContext())
            return
        }
        
        // Check for exact alarm permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (HydrationAlarmScheduler.canScheduleExactAlarms(requireContext())) {
                // Use the new alarm scheduler
                HydrationAlarmScheduler.scheduleRecurringAlarm(requireContext(), settings)
            }
            // If we can't schedule exact alarms, the alarms will remain unscheduled
            // The user will need to manually enable them in the app
        } else {
            // For older versions, directly schedule the alarm
            HydrationAlarmScheduler.scheduleRecurringAlarm(requireContext(), settings)
        }
    }
    
    companion object {
        private const val NOTIFICATION_PERMISSION_CODE = 1001
    }
}