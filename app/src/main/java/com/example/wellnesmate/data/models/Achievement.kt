package com.example.wellnesmate.data.models

import androidx.annotation.DrawableRes
import com.example.wellnesmate.R

/**
 * Data class representing an achievement or badge in the app
 * @property id Unique identifier for the achievement
 * @property title Title of the achievement
 * @property description Description of what the achievement is for
 * @property iconResId Drawable resource ID for the achievement icon
 * @property isUnlocked Whether the user has unlocked this achievement
 * @property progress Current progress towards unlocking (0-100)
 * @property dateUnlocked The date when the achievement was unlocked (null if not unlocked)
 */
data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    @DrawableRes val iconResId: Int,
    val isUnlocked: Boolean = false,
    val progress: Int = if (isUnlocked) 100 else 0,
    val dateUnlocked: Long? = null
)

/**
 * Sample achievements data
 */
object AchievementsData {
    fun getSampleAchievements(): List<Achievement> = listOf(
        Achievement(
            id = "first_step",
            title = "First Step",
            description = "Log your first mood entry",
            iconResId = R.drawable.ic_achievement_first,
            isUnlocked = true,
            dateUnlocked = System.currentTimeMillis() - 86400000 * 5 // 5 days ago
        ),
        Achievement(
            id = "water_enthusiast",
            title = "Water Enthusiast",
            description = "Log water for 3 days in a row",
            iconResId = R.drawable.ic_achievement_water,
            isUnlocked = false,
            progress = 66 // 2 out of 3 days
        ),
        Achievement(
            id = "mood_explorer",
            title = "Mood Explorer",
            description = "Use 5 different mood types",
            iconResId = R.drawable.ic_achievement_mood,
            isUnlocked = false,
            progress = 60 // 3 out of 5 moods used
        ),
        Achievement(
            id = "streak_beginner",
            title = "Streak Beginner",
            description = "Maintain a 3-day habit streak",
            iconResId = R.drawable.ic_achievement_streak,
            isUnlocked = true,
            dateUnlocked = System.currentTimeMillis() - 86400000 * 2 // 2 days ago
        )
    )
}
