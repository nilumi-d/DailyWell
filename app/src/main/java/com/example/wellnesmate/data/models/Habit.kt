package com.example.wellnesmate.data.models

import java.util.*

/**
 * Data class representing a daily habit
 */
data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val targetValue: Int = 1,
    val unit: String = "times",
    val createdDate: Date = Date(),
    val isActive: Boolean = true
) {
    companion object {
        const val UNIT_TIMES = "times"
        const val UNIT_MINUTES = "minutes"
        const val UNIT_HOURS = "hours"
        const val UNIT_GLASSES = "glasses"
        const val UNIT_STEPS = "steps"
        const val UNIT_PAGES = "pages"
        const val UNIT_KILOMETERS = "km"
    }
}

/**
 * Data class representing daily progress for a habit
 */
data class HabitProgress(
    val habitId: String,
    val date: String, // Format: "yyyy-MM-dd"
    val currentValue: Int = 0,
    val isCompleted: Boolean = false,
    val completionTime: Date? = null
) {
    fun getProgressPercentage(targetValue: Int): Int {
        return if (targetValue > 0) {
            ((currentValue.toFloat() / targetValue.toFloat()) * 100).coerceAtMost(100f).toInt()
        } else {
            0
        }
    }
}

/**
 * Data class for habit statistics
 */
data class HabitStats(
    val habitId: String,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalCompletions: Int = 0,
    val completionRate: Float = 0f // Percentage
)