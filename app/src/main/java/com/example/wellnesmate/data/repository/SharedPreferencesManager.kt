package com.example.wellnesmate.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.wellnesmate.data.models.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manager class for handling SharedPreferences operations
 * Provides methods to store and retrieve all app data
 */
class SharedPreferencesManager(context: Context) {
    
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    companion object {
        private const val PREFS_NAME = "wellnesmate_prefs"
        
        // Legacy keys for backward compatibility
        private const val KEY_HABITS = "habits"
        private const val KEY_HABIT_PROGRESS = "habit_progress"
        private const val KEY_MOOD_ENTRIES = "mood_entries"
        private const val KEY_HYDRATION_SETTINGS = "hydration_settings"
        private const val KEY_HYDRATION_INTAKE = "hydration_intake"
        private const val KEY_APP_SETTINGS = "app_settings"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_LAST_BACKUP = "last_backup"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_PASSWORD_HASH = "user_password_hash"
        
        // New keys for direct storage
        private const val KEY_HABITS_COUNT = "habits_count"
        private const val KEY_HABIT_PREFIX = "habit"
        private const val KEY_HABIT_PROGRESS_COUNT = "habit_progress_count"
        private const val KEY_HABIT_PROGRESS_PREFIX = "habit_progress"
        private const val KEY_MOOD_ENTRIES_COUNT = "mood_entries_count"
        private const val KEY_MOOD_ENTRY_PREFIX = "mood_entry"
        private const val KEY_HYDRATION_INTAKE_COUNT = "hydration_intake_count"
        private const val KEY_HYDRATION_INTAKE_PREFIX = "hydration_intake"
        
        @Volatile
        private var INSTANCE: SharedPreferencesManager? = null
        
        fun getInstance(context: Context): SharedPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SharedPreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    // HABIT MANAGEMENT
    
    fun saveHabits(habits: List<Habit>) {
        val editor = sharedPrefs.edit()
        // Clear existing habits
        editor.remove(KEY_HABITS_COUNT)
        for (i in 0 until getHabitCount()) {
            val habitPrefix = "${KEY_HABIT_PREFIX}_$i"
            editor.remove("${habitPrefix}_id")
            editor.remove("${habitPrefix}_name")
            editor.remove("${habitPrefix}_description")
            editor.remove("${habitPrefix}_targetValue")
            editor.remove("${habitPrefix}_unit")
            editor.remove("${habitPrefix}_createdDate")
            editor.remove("${habitPrefix}_isActive")
        }
        
        // Save new habits
        editor.putInt(KEY_HABITS_COUNT, habits.size)
        habits.forEachIndexed { index, habit ->
            val habitPrefix = "${KEY_HABIT_PREFIX}_$index"
            editor.putString("${habitPrefix}_id", habit.id)
            editor.putString("${habitPrefix}_name", habit.name)
            editor.putString("${habitPrefix}_description", habit.description)
            editor.putInt("${habitPrefix}_targetValue", habit.targetValue)
            editor.putString("${habitPrefix}_unit", habit.unit)
            editor.putLong("${habitPrefix}_createdDate", habit.createdDate.time)
            editor.putBoolean("${habitPrefix}_isActive", habit.isActive)
        }
        editor.apply()
    }
    
    fun getHabits(): List<Habit> {
        val habits = mutableListOf<Habit>()
        val count = getHabitCount()
        
        for (i in 0 until count) {
            val habitPrefix = "${KEY_HABIT_PREFIX}_$i"
            val id = sharedPrefs.getString("${habitPrefix}_id", "") ?: ""
            if (id.isNotEmpty()) {
                val name = sharedPrefs.getString("${habitPrefix}_name", "") ?: ""
                val description = sharedPrefs.getString("${habitPrefix}_description", "") ?: ""
                val targetValue = sharedPrefs.getInt("${habitPrefix}_targetValue", 1)
                val unit = sharedPrefs.getString("${habitPrefix}_unit", Habit.UNIT_TIMES) ?: Habit.UNIT_TIMES
                val createdDate = Date(sharedPrefs.getLong("${habitPrefix}_createdDate", System.currentTimeMillis()))
                val isActive = sharedPrefs.getBoolean("${habitPrefix}_isActive", true)
                
                habits.add(Habit(id, name, description, targetValue, unit, createdDate, isActive))
            }
        }
        
        return habits
    }
    
    private fun getHabitCount(): Int {
        return sharedPrefs.getInt(KEY_HABITS_COUNT, 0)
    }
    
    fun saveHabit(habit: Habit) {
        val habits = getHabits().toMutableList()
        val existingIndex = habits.indexOfFirst { it.id == habit.id }
        if (existingIndex >= 0) {
            habits[existingIndex] = habit
        } else {
            habits.add(habit)
        }
        saveHabits(habits)
    }
    
    fun deleteHabit(habitId: String) {
        val habits = getHabits().toMutableList()
        habits.removeAll { it.id == habitId }
        saveHabits(habits)
        
        // Also delete related progress data
        deleteHabitProgress(habitId)
    }
    
    // HABIT PROGRESS MANAGEMENT
    
    fun saveHabitProgress(progress: List<HabitProgress>) {
        val editor = sharedPrefs.edit()
        // Clear existing progress
        editor.remove(KEY_HABIT_PROGRESS_COUNT)
        for (i in 0 until getHabitProgressCount()) {
            val progressPrefix = "${KEY_HABIT_PROGRESS_PREFIX}_$i"
            editor.remove("${progressPrefix}_habitId")
            editor.remove("${progressPrefix}_date")
            editor.remove("${progressPrefix}_currentValue")
            editor.remove("${progressPrefix}_isCompleted")
            editor.remove("${progressPrefix}_completionTime")
        }
        
        // Save new progress entries
        editor.putInt(KEY_HABIT_PROGRESS_COUNT, progress.size)
        progress.forEachIndexed { index, item ->
            val progressPrefix = "${KEY_HABIT_PROGRESS_PREFIX}_$index"
            editor.putString("${progressPrefix}_habitId", item.habitId)
            editor.putString("${progressPrefix}_date", item.date)
            editor.putInt("${progressPrefix}_currentValue", item.currentValue)
            editor.putBoolean("${progressPrefix}_isCompleted", item.isCompleted)
            item.completionTime?.let {
                editor.putLong("${progressPrefix}_completionTime", it.time)
            }
        }
        editor.apply()
    }
    
    fun getHabitProgress(): List<HabitProgress> {
        val progressList = mutableListOf<HabitProgress>()
        val count = getHabitProgressCount()
        
        for (i in 0 until count) {
            val progressPrefix = "${KEY_HABIT_PROGRESS_PREFIX}_$i"
            val habitId = sharedPrefs.getString("${progressPrefix}_habitId", "") ?: ""
            val date = sharedPrefs.getString("${progressPrefix}_date", "") ?: ""
            
            if (habitId.isNotEmpty() && date.isNotEmpty()) {
                val currentValue = sharedPrefs.getInt("${progressPrefix}_currentValue", 0)
                val isCompleted = sharedPrefs.getBoolean("${progressPrefix}_isCompleted", false)
                val completionTimeMillis = sharedPrefs.getLong("${progressPrefix}_completionTime", -1)
                val completionTime = if (completionTimeMillis != -1L) Date(completionTimeMillis) else null
                
                progressList.add(HabitProgress(habitId, date, currentValue, isCompleted, completionTime))
            }
        }
        
        return progressList
    }
    
    private fun getHabitProgressCount(): Int {
        return sharedPrefs.getInt(KEY_HABIT_PROGRESS_COUNT, 0)
    }
    
    fun saveHabitProgressForDay(habitId: String, date: String, progress: HabitProgress) {
        val allProgress = getHabitProgress().toMutableList()
        val existingIndex = allProgress.indexOfFirst { it.habitId == habitId && it.date == date }
        
        if (existingIndex >= 0) {
            allProgress[existingIndex] = progress
        } else {
            allProgress.add(progress)
        }
        saveHabitProgress(allProgress)
    }
    
    fun getHabitProgressForDay(habitId: String, date: String): HabitProgress? {
        return getHabitProgress().find { it.habitId == habitId && it.date == date }
    }
    
    fun getTodayHabitProgress(habitId: String): HabitProgress? {
        val today = dateFormat.format(Date())
        return getHabitProgressForDay(habitId, today)
    }
    
    private fun deleteHabitProgress(habitId: String) {
        val progress = getHabitProgress().toMutableList()
        progress.removeAll { it.habitId == habitId }
        saveHabitProgress(progress)
    }
    
    // MOOD MANAGEMENT
    
    fun saveMoodEntries(entries: List<MoodEntry>) {
        val editor = sharedPrefs.edit()
        // Clear existing entries
        editor.remove(KEY_MOOD_ENTRIES_COUNT)
        for (i in 0 until getMoodEntriesCount()) {
            val entryPrefix = "${KEY_MOOD_ENTRY_PREFIX}_$i"
            editor.remove("${entryPrefix}_id")
            editor.remove("${entryPrefix}_moodType")
            editor.remove("${entryPrefix}_emoji")
            editor.remove("${entryPrefix}_notes")
            editor.remove("${entryPrefix}_timestamp")
            editor.remove("${entryPrefix}_date")
        }
        
        // Save new entries
        editor.putInt(KEY_MOOD_ENTRIES_COUNT, entries.size)
        entries.forEachIndexed { index, entry ->
            val entryPrefix = "${KEY_MOOD_ENTRY_PREFIX}_$index"
            editor.putString("${entryPrefix}_id", entry.id)
            editor.putString("${entryPrefix}_moodType", entry.mood.name)
            editor.putString("${entryPrefix}_emoji", entry.emoji)
            editor.putString("${entryPrefix}_notes", entry.notes)
            editor.putLong("${entryPrefix}_timestamp", entry.timestamp.time)
            editor.putString("${entryPrefix}_date", entry.date)
        }
        editor.apply()
    }
    
    fun getMoodEntries(): List<MoodEntry> {
        val entries = mutableListOf<MoodEntry>()
        val count = getMoodEntriesCount()
        
        for (i in 0 until count) {
            val entryPrefix = "${KEY_MOOD_ENTRY_PREFIX}_$i"
            val id = sharedPrefs.getString("${entryPrefix}_id", "") ?: ""
            
            if (id.isNotEmpty()) {
                val moodTypeName = sharedPrefs.getString("${entryPrefix}_moodType", MoodType.NEUTRAL.name) ?: MoodType.NEUTRAL.name
                val moodType = try {
                    MoodType.valueOf(moodTypeName)
                } catch (e: Exception) {
                    MoodType.NEUTRAL
                }
                val emoji = sharedPrefs.getString("${entryPrefix}_emoji", moodType.emoji) ?: moodType.emoji
                val notes = sharedPrefs.getString("${entryPrefix}_notes", "") ?: ""
                val timestamp = Date(sharedPrefs.getLong("${entryPrefix}_timestamp", System.currentTimeMillis()))
                val date = sharedPrefs.getString("${entryPrefix}_date", dateFormat.format(timestamp)) ?: dateFormat.format(timestamp)
                
                entries.add(MoodEntry(id, moodType, emoji, notes, timestamp, date))
            }
        }
        
        return entries.sortedByDescending { it.timestamp }
    }
    
    private fun getMoodEntriesCount(): Int {
        return sharedPrefs.getInt(KEY_MOOD_ENTRIES_COUNT, 0)
    }
    
    fun saveMoodEntry(entry: MoodEntry) {
        val entries = getMoodEntries().toMutableList()
        val existingIndex = entries.indexOfFirst { it.id == entry.id }
        if (existingIndex >= 0) {
            entries[existingIndex] = entry
        } else {
            entries.add(0, entry) // Add to beginning for chronological order
        }
        saveMoodEntries(entries)
    }
    
    fun deleteMoodEntry(entryId: String) {
        val entries = getMoodEntries().toMutableList()
        entries.removeAll { it.id == entryId }
        saveMoodEntries(entries)
    }
    
    fun getTodayMoodEntries(): List<MoodEntry> {
        val today = dateFormat.format(Date())
        return getMoodEntries().filter { it.date == today }
    }
    
    // HYDRATION MANAGEMENT
    
    fun saveHydrationSettings(settings: HydrationSettings) {
        val editor = sharedPrefs.edit()
        editor.putInt("hydration_dailyGoalMl", settings.dailyGoalMl)
        editor.putBoolean("hydration_reminderEnabled", settings.reminderEnabled)
        editor.putInt("hydration_reminderIntervalMinutes", settings.reminderIntervalMinutes)
        editor.putInt("hydration_startTime", settings.startTime)
        editor.putInt("hydration_endTime", settings.endTime)
        editor.putInt("hydration_startMinute", settings.startMinute)
        editor.putLong("hydration_lastUpdated", settings.lastUpdated.time)
        editor.apply()
    }
    
    fun getHydrationSettings(): HydrationSettings {
        return HydrationSettings(
            dailyGoalMl = sharedPrefs.getInt("hydration_dailyGoalMl", HydrationSettings.DEFAULT_GOAL),
            reminderEnabled = sharedPrefs.getBoolean("hydration_reminderEnabled", true),
            reminderIntervalMinutes = sharedPrefs.getInt("hydration_reminderIntervalMinutes", HydrationSettings.INTERVAL_1_HOUR),
            startTime = sharedPrefs.getInt("hydration_startTime", 8),
            endTime = sharedPrefs.getInt("hydration_endTime", 22),
            startMinute = sharedPrefs.getInt("hydration_startMinute", 0),
            lastUpdated = Date(sharedPrefs.getLong("hydration_lastUpdated", System.currentTimeMillis()))
        )
    }
    
    fun saveHydrationIntake(intake: List<HydrationIntake>) {
        val editor = sharedPrefs.edit()
        // Clear existing entries
        editor.remove(KEY_HYDRATION_INTAKE_COUNT)
        for (i in 0 until getHydrationIntakeCount()) {
            val intakePrefix = "${KEY_HYDRATION_INTAKE_PREFIX}_$i"
            editor.remove("${intakePrefix}_id")
            editor.remove("${intakePrefix}_date")
            editor.remove("${intakePrefix}_amountMl")
            editor.remove("${intakePrefix}_timestamp")
            editor.remove("${intakePrefix}_note")
        }
        
        // Save new entries
        editor.putInt(KEY_HYDRATION_INTAKE_COUNT, intake.size)
        intake.forEachIndexed { index, item ->
            val intakePrefix = "${KEY_HYDRATION_INTAKE_PREFIX}_$index"
            editor.putString("${intakePrefix}_id", item.id)
            editor.putString("${intakePrefix}_date", item.date)
            editor.putInt("${intakePrefix}_amountMl", item.amountMl)
            editor.putLong("${intakePrefix}_timestamp", item.timestamp.time)
            editor.putString("${intakePrefix}_note", item.note)
        }
        editor.apply()
    }
    
    fun getHydrationIntake(): List<HydrationIntake> {
        val intakeList = mutableListOf<HydrationIntake>()
        val count = getHydrationIntakeCount()
        
        for (i in 0 until count) {
            val intakePrefix = "${KEY_HYDRATION_INTAKE_PREFIX}_$i"
            val id = sharedPrefs.getString("${intakePrefix}_id", "") ?: ""
            val date = sharedPrefs.getString("${intakePrefix}_date", "") ?: ""
            
            if (id.isNotEmpty() && date.isNotEmpty()) {
                val amountMl = sharedPrefs.getInt("${intakePrefix}_amountMl", 0)
                val timestamp = Date(sharedPrefs.getLong("${intakePrefix}_timestamp", System.currentTimeMillis()))
                val note = sharedPrefs.getString("${intakePrefix}_note", "") ?: ""
                
                intakeList.add(HydrationIntake(id, date, amountMl, timestamp, note))
            }
        }
        
        return intakeList.sortedByDescending { it.timestamp }
    }
    
    private fun getHydrationIntakeCount(): Int {
        return sharedPrefs.getInt(KEY_HYDRATION_INTAKE_COUNT, 0)
    }
    
    fun addHydrationIntake(intake: HydrationIntake) {
        val intakes = getHydrationIntake().toMutableList()
        intakes.add(0, intake) // Add to beginning for chronological order
        saveHydrationIntake(intakes)
    }
    
    fun getTodayHydrationIntake(): List<HydrationIntake> {
        val today = dateFormat.format(Date())
        return getHydrationIntake().filter { it.date == today }
    }
    
    fun getTodayTotalHydration(): Int {
        return getTodayHydrationIntake().sumOf { it.amountMl }
    }
    
    // APP SETTINGS
    
    fun setFirstLaunch(isFirst: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_FIRST_LAUNCH, isFirst).apply()
    }
    
    fun isFirstLaunch(): Boolean {
        return sharedPrefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    fun setLastBackupTime(timestamp: Long) {
        sharedPrefs.edit().putLong(KEY_LAST_BACKUP, timestamp).apply()
    }
    
    fun getLastBackupTime(): Long {
        return sharedPrefs.getLong(KEY_LAST_BACKUP, 0)
    }
    
    // User login methods
    fun setUserLoggedIn(isLoggedIn: Boolean) {
        sharedPrefs.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply()
    }
    
    fun isUserLoggedIn(): Boolean {
        return sharedPrefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    // AUTH MANAGEMENT
    fun isUserRegistered(): Boolean {
        val email = sharedPrefs.getString(KEY_USER_EMAIL, null)
        val pwd = sharedPrefs.getString(KEY_USER_PASSWORD_HASH, null)
        return !email.isNullOrEmpty() && !pwd.isNullOrEmpty()
    }

    fun getRegisteredEmail(): String? = sharedPrefs.getString(KEY_USER_EMAIL, null)

    fun registerUser(name: String, email: String, plainPassword: String): Boolean {
        // If already registered with same email, treat as duplicate
        val existingEmail = sharedPrefs.getString(KEY_USER_EMAIL, null)
        if (!existingEmail.isNullOrEmpty() && existingEmail.equals(email, ignoreCase = true)) {
            return false
        }
        val editor = sharedPrefs.edit()
        editor.putString(KEY_USER_NAME, name)
        editor.putString(KEY_USER_EMAIL, email)
        editor.putString(KEY_USER_PASSWORD_HASH, hashPassword(plainPassword))
        editor.apply()
        return true
    }

    fun validateLogin(email: String, plainPassword: String): Boolean {
        val storedEmail = sharedPrefs.getString(KEY_USER_EMAIL, null) ?: return false
        val storedHash = sharedPrefs.getString(KEY_USER_PASSWORD_HASH, null) ?: return false
        if (!storedEmail.equals(email, ignoreCase = true)) return false
        val providedHash = hashPassword(plainPassword)
        return storedHash == providedHash
    }

    fun clearUser() {
        sharedPrefs.edit()
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PASSWORD_HASH)
            .apply()
    }

    private fun hashPassword(plain: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(plain.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { b -> "%02x".format(b) }
        } catch (e: Exception) {
            // Fallback to simple hashCode (not secure), but avoids crash
            plain.hashCode().toString()
        }
    }
    
    // UTILITY METHODS
    
    fun clearAllData() {
        sharedPrefs.edit().clear().apply()
    }
    
    fun exportData(): String {
        // For export functionality, we'll need to create a formatted string manually
        // This is a simplified JSON structure created manually
        val sb = StringBuilder()
        sb.append("{")
        
        // Export habits
        sb.append("\"habits\":[")
        val habits = getHabits()
        habits.forEachIndexed { index, habit ->
            sb.append("{")
            sb.append("\"id\":\"${escapeJson(habit.id)}\",")
            sb.append("\"name\":\"${escapeJson(habit.name)}\",")
            sb.append("\"description\":\"${escapeJson(habit.description)}\",")
            sb.append("\"targetValue\":${habit.targetValue},")
            sb.append("\"unit\":\"${escapeJson(habit.unit)}\",")
            sb.append("\"createdDate\":${habit.createdDate.time},")
            sb.append("\"isActive\":${habit.isActive}")
            sb.append("}")
            if (index < habits.size - 1) sb.append(",")
        }
        sb.append("],")
        
        // Export habit progress
        sb.append("\"habitProgress\":[")
        val habitProgress = getHabitProgress()
        habitProgress.forEachIndexed { index, progress ->
            sb.append("{")
            sb.append("\"habitId\":\"${escapeJson(progress.habitId)}\",")
            sb.append("\"date\":\"${escapeJson(progress.date)}\",")
            sb.append("\"currentValue\":${progress.currentValue},")
            sb.append("\"isCompleted\":${progress.isCompleted},")
            sb.append("\"completionTime\":${progress.completionTime?.time ?: "null"}")
            sb.append("}")
            if (index < habitProgress.size - 1) sb.append(",")
        }
        sb.append("],")
        
        // Export mood entries
        sb.append("\"moodEntries\":[")
        val moodEntries = getMoodEntries()
        moodEntries.forEachIndexed { index, entry ->
            sb.append("{")
            sb.append("\"id\":\"${escapeJson(entry.id)}\",")
            sb.append("\"mood\":\"${escapeJson(entry.mood.name)}\",")
            sb.append("\"emoji\":\"${escapeJson(entry.emoji)}\",")
            sb.append("\"notes\":\"${escapeJson(entry.notes)}\",")
            sb.append("\"timestamp\":${entry.timestamp.time},")
            sb.append("\"date\":\"${escapeJson(entry.date)}\"")
            sb.append("}")
            if (index < moodEntries.size - 1) sb.append(",")
        }
        sb.append("],")
        
        // Export hydration settings
        val settings = getHydrationSettings()
        sb.append("\"hydrationSettings\":{")
        sb.append("\"dailyGoalMl\":${settings.dailyGoalMl},")
        sb.append("\"reminderEnabled\":${settings.reminderEnabled},")
        sb.append("\"reminderIntervalMinutes\":${settings.reminderIntervalMinutes},")
        sb.append("\"startTime\":${settings.startTime},")
        sb.append("\"endTime\":${settings.endTime},")
        sb.append("\"startMinute\":${settings.startMinute},")
        sb.append("\"lastUpdated\":${settings.lastUpdated.time}")
        sb.append("},")
        
        // Export hydration intake
        sb.append("\"hydrationIntake\":[")
        val hydrationIntake = getHydrationIntake()
        hydrationIntake.forEachIndexed { index, intake ->
            sb.append("{")
            sb.append("\"id\":\"${escapeJson(intake.id)}\",")
            sb.append("\"date\":\"${escapeJson(intake.date)}\",")
            sb.append("\"amountMl\":${intake.amountMl},")
            sb.append("\"timestamp\":${intake.timestamp.time},")
            sb.append("\"note\":\"${escapeJson(intake.note)}\"")
            sb.append("}")
            if (index < hydrationIntake.size - 1) sb.append(",")
        }
        sb.append("],")
        
        // Export date
        sb.append("\"exportDate\":${Date().time}")
        
        sb.append("}")
        return sb.toString()
    }
    
    // Helper function to escape JSON strings
    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
    
    // Get current date string
    fun getCurrentDateString(): String {
        return dateFormat.format(Date())
    }
    
    // Calculate habit streak
    fun calculateHabitStreak(habitId: String): Int {
        val progress = getHabitProgress()
            .filter { it.habitId == habitId && it.isCompleted }
            .sortedByDescending { it.date }
        
        if (progress.isEmpty()) return 0
        
        var streak = 0
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        // Start from today and go backwards
        for (i in 0 until 365) { // Check up to a year back
            val dateString = dateFormat.format(calendar.time)
            val hasProgress = progress.any { it.date == dateString }
            
            if (hasProgress) {
                streak++
            } else {
                break
            }
            
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }
        
        return streak
    }
}