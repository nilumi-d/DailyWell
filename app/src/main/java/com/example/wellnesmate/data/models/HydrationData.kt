package com.example.wellnesmate.data.models

import java.util.*

/**
 * Data class representing hydration settings and preferences
 */
data class HydrationSettings(
    val dailyGoalMl: Int = 2000, // Default 2L per day
    val reminderEnabled: Boolean = true,
    val reminderIntervalMinutes: Int = 60, // Default every hour
    val startTime: Int = 8, // 8 AM
    val endTime: Int = 22, // 10 PM
    val startMinute: Int = 0, // Added minute precision for start time
    val lastUpdated: Date = Date()
) {
    companion object {
        const val MIN_GOAL = 500 // 500ml minimum
        const val MAX_GOAL = 5000 // 5L maximum
        const val DEFAULT_GOAL = 2000 // 2L default
        
        // Reminder intervals in minutes
        const val INTERVAL_30_MIN = 30
        const val INTERVAL_1_HOUR = 60
        const val INTERVAL_2_HOURS = 120
        const val INTERVAL_3_HOURS = 180
        const val INTERVAL_4_HOURS = 240
    }
}

/**
 * Data class representing daily hydration intake
 */
data class HydrationIntake(
    val id: String = UUID.randomUUID().toString(),
    val date: String, // Format: "yyyy-MM-dd"
    val amountMl: Int,
    val timestamp: Date = Date(),
    val note: String = ""
) {
    companion object {
        // Common water amounts in ml
        const val SMALL_GLASS = 200
        const val MEDIUM_GLASS = 250
        const val LARGE_GLASS = 300
        const val BOTTLE_SMALL = 330
        const val BOTTLE_MEDIUM = 500
        const val BOTTLE_LARGE = 750
        const val CUSTOM = -1
    }
}

/**
 * Data class representing daily hydration summary
 */
data class DailyHydration(
    val date: String, // Format: "yyyy-MM-dd"
    val totalIntakeMl: Int = 0,
    val goalMl: Int = 2000,
    val entries: List<HydrationIntake> = emptyList(),
    val goalReached: Boolean = false,
    val goalReachedTime: Date? = null
) {
    fun getProgressPercentage(): Int {
        return if (goalMl > 0) {
            ((totalIntakeMl.toFloat() / goalMl.toFloat()) * 100).coerceAtMost(100f).toInt()
        } else {
            0
        }
    }
    
    fun getRemainingMl(): Int {
        return (goalMl - totalIntakeMl).coerceAtLeast(0)
    }
}

/**
 * Data class for hydration statistics
 */
data class HydrationStats(
    val averageDailyIntake: Int = 0,
    val goalAchievementRate: Float = 0f, // Percentage
    val totalDaysTracked: Int = 0,
    val currentStreak: Int = 0, // Days of reaching goal consecutively
    val longestStreak: Int = 0,
    val weeklyAverage: List<Int> = emptyList() // Average intake for each day of the week
)