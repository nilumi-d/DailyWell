package com.example.wellnesmate.ui.fragments

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.animation.Easing
import android.view.LayoutInflater
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wellnesmate.R
import com.example.wellnesmate.data.models.Achievement
import com.example.wellnesmate.data.models.AchievementsData
import com.example.wellnesmate.data.repository.SharedPreferencesManager
import com.example.wellnesmate.ui.adapters.AchievementAdapter
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

/**
 * Home Fragment - Dashboard with wellness summary
 */
class HomeFragment : Fragment() {
    
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var tvGreeting: TextView
    private lateinit var tvHabitsProgress: TextView
    private lateinit var tvHydrationProgress: TextView
    private lateinit var tvCurrentMood: TextView
    private lateinit var progressHydration: ProgressBar
    private lateinit var cardHabits: MaterialCardView
    private lateinit var cardMood: View  // Changed to View since it's a LinearLayout
    private lateinit var cardHydration: View  // Changed to View since it's a LinearLayout
    private lateinit var wellnessScoreView: View
    private lateinit var scoreProgress: ProgressBar
    private lateinit var tvScoreValue: TextView
    private lateinit var tvScoreLabel: TextView
    private lateinit var tvMoodScore: TextView
    private lateinit var tvSleepScore: TextView
    private lateinit var tvHydrationScore: TextView
    private lateinit var tvActivityScore: TextView
    private lateinit var tvStressScore: TextView
    private lateinit var rvAchievements: RecyclerView
    private lateinit var achievementAdapter: AchievementAdapter
    private lateinit var chartWeeklySummary: com.github.mikephil.charting.charts.LineChart
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        try {
            Log.d("HomeFragment", "Starting view initialization")
            initializeViews(view)
            Log.d("HomeFragment", "Views initialized successfully")
            
            Log.d("HomeFragment", "Setting up quick actions")
            setupQuickActions()
            Log.d("HomeFragment", "Quick actions setup complete")
            
            try {
                Log.d("HomeFragment", "Setting up achievements RecyclerView")
                setupAchievementsRecyclerView()
                Log.d("HomeFragment", "Achievements RecyclerView setup complete")
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error in achievements setup", e)
            }
            
            try {
                Log.d("HomeFragment", "Setting up weekly chart")
                // Add wellness score view to the layout
                (view as ViewGroup).addView(wellnessScoreView, 2) // Add after greeting and before achievements
                updateWellnessScore()
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error setting up wellness score", e)
            }
            
            try {
                Log.d("HomeFragment", "Updating dashboard")
                updateDashboard()
                Log.d("HomeFragment", "Dashboard update complete")
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error in dashboard update", e)
            }
            
        } catch (e: Exception) {
            Log.e("HomeFragment", "Critical error in onViewCreated", e)
            // Show error to user
            activity?.runOnUiThread {
                android.widget.Toast.makeText(
                    context,
                    "Error initializing home screen: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateDashboard()
    }
    
    private fun initializeViews(view: View) {
        try {
            prefsManager = SharedPreferencesManager.getInstance(requireContext())
            
            // Initialize text views
            tvGreeting = view.findViewById(R.id.tv_greeting)
            tvHabitsProgress = view.findViewById(R.id.tv_habits_progress)
            tvHydrationProgress = view.findViewById(R.id.tv_hydration_progress)
            tvCurrentMood = view.findViewById(R.id.tv_current_mood)
            progressHydration = view.findViewById(R.id.progress_hydration)
            
            // Initialize cards
            cardHabits = view.findViewById(R.id.card_habits)
            cardMood = view.findViewById(R.id.card_mood)
            cardHydration = view.findViewById(R.id.card_hydration)
            
            // Initialize wellness score views
            wellnessScoreView = layoutInflater.inflate(R.layout.item_wellness_score, null)
            scoreProgress = wellnessScoreView.findViewById(R.id.scoreProgress)
            tvScoreValue = wellnessScoreView.findViewById(R.id.tvScoreValue)
            tvScoreLabel = wellnessScoreView.findViewById(R.id.tvScoreLabel)
            tvMoodScore = wellnessScoreView.findViewById(R.id.tvMoodScore)
            tvSleepScore = wellnessScoreView.findViewById(R.id.tvSleepScore)
            tvHydrationScore = wellnessScoreView.findViewById(R.id.tvHydrationScore)
            tvActivityScore = wellnessScoreView.findViewById(R.id.tvActivityScore)
            tvStressScore = wellnessScoreView.findViewById(R.id.tvStressScore)
            
            // Initialize achievements recycler view
            rvAchievements = view.findViewById(R.id.rv_achievements)
            
            // Initialize weekly summary chart
            chartWeeklySummary = view.findViewById(R.id.chart_weekly_summary)
            setupWeeklyChart()
            
            Log.d("HomeFragment", "Views initialized - " +
                  "tvGreeting: ${::tvGreeting.isInitialized}, " +
                  "cardHabits: ${::cardHabits.isInitialized}, " +
                  "cardMood: ${::cardMood.isInitialized}, " +
                  "cardHydration: ${::cardHydration.isInitialized}, " +
                  "chartWeeklySummary: ${::chartWeeklySummary.isInitialized}, " +
                  "rvAchievements: ${::rvAchievements.isInitialized}")
                  
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error initializing views", e)
            throw e
        }
    }
    
    private fun setupQuickActions() {
        try {
            cardHabits.setOnClickListener {
                navigateToFragment(0) // Habits tab
            }
            
            cardMood.setOnClickListener {
                navigateToFragment(1) // Mood tab
            }
            
            cardHydration.setOnClickListener {
                navigateToFragment(2) // Hydration tab
            }
            
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up quick actions", e)
        }
    }
    
    private fun navigateToFragment(position: Int) {
        // Navigate using MainActivity's bottom navigation
        (activity as? com.example.wellnesmate.MainActivity)?.navigateToTab(position)
    }
    
    private fun updateDashboard() {
        try {
            // Update greeting
            val greeting = getGreeting()
            if (::tvGreeting.isInitialized) {
                tvGreeting.text = "$greeting, Nilumi! 🌞"
            }
            
            // Update habits progress
            if (::tvHabitsProgress.isInitialized) {
                try {
                    val today = java.time.LocalDate.now()
                    val todayStr = today.toString() // Format: yyyy-MM-dd
                    val allHabits = prefsManager.getHabits()
                    // Count only active habits since we don't have day scheduling
                    val activeHabits = allHabits.filter { it.isActive }
                    // Get all habit progress and filter for today's completed habits
                    val allProgress = prefsManager.getHabitProgress()
                    val completedHabits = allProgress.filter { it.date == todayStr && it.isCompleted }
                    
                    tvHabitsProgress.text = "${completedHabits.size} / ${activeHabits.size} habits completed"
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error updating habits progress", e)
                    tvHabitsProgress.text = "0 / 0 habits completed"
                }
            }
            
            // Update hydration progress
            try {
                if (::tvHydrationProgress.isInitialized && ::progressHydration.isInitialized) {
                    val hydrationSettings = prefsManager.getHydrationSettings()
                    val todayIntake = prefsManager.getTodayTotalHydration()
                    val hydrationPercent = if (hydrationSettings.dailyGoalMl > 0) {
                        ((todayIntake.toFloat() / hydrationSettings.dailyGoalMl) * 100).toInt()
                    } else {
                        0
                    }
                    tvHydrationProgress.text = "$hydrationPercent% of daily goal"
                    progressHydration.progress = hydrationPercent.coerceIn(0, 100)
                }
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error updating hydration progress", e)
            }
            
            // Update current mood
            if (::tvCurrentMood.isInitialized) {
                try {
                    val todayMoods = prefsManager.getTodayMoodEntries()
                    tvCurrentMood.text = if (todayMoods.isNotEmpty()) {
                        val latestMood = todayMoods.first()
                        "${latestMood.emoji} ${latestMood.mood.label}"
                    } else {
                        "No mood logged yet"
                    }
                } catch (e: Exception) {
                    Log.e("HomeFragment", "Error updating mood", e)
                    tvCurrentMood.text = "Mood update error"
                }
            }
        } catch (e: Exception) {
            Log.e("HomeFragment", "Error in updateDashboard", e)
        }
    }
    
    private fun setupAchievementsRecyclerView() {
        achievementAdapter = AchievementAdapter(AchievementsData.getSampleAchievements()) { achievement ->
            // Handle achievement click
            // You can show a dialog with achievement details here
        }
        
        rvAchievements.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = achievementAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun updateWellnessScore() {
        // Sample data - in a real app, this would come from your data source
        val moodScore = 80
        val sleepScore = 70
        val hydrationScore = 90
        val activityScore = 60
        val stressScore = 30 // Lower is better
        
        // Calculate overall score with weights
        val overallScore = (moodScore * 0.3 + 
                          sleepScore * 0.25 + 
                          hydrationScore * 0.2 + 
                          activityScore * 0.15 + 
                          (100 - stressScore) * 0.1).toInt()
        
        // Update UI
        scoreProgress.progress = overallScore
        scoreProgress.progressDrawable.colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
            ContextCompat.getColor(requireContext(), when {
                overallScore >= 80 -> R.color.success_green
                overallScore >= 60 -> R.color.primary
                overallScore >= 40 -> R.color.warning_orange
                else -> R.color.error_red
            }),
            BlendModeCompat.SRC_IN
        )
        tvScoreValue.text = overallScore.toString()
        
        // Set score label based on value
        val (label, color) = when {
            overallScore >= 80 -> Pair("Excellent", R.color.success_green)
            overallScore >= 60 -> Pair("Good", R.color.primary)
            overallScore >= 40 -> Pair("Fair", R.color.warning_orange)
            else -> Pair("Needs Improvement", R.color.error_red)
        }
        
        tvScoreLabel.text = label
        tvScoreLabel.setTextColor(ContextCompat.getColor(requireContext(), color))
        
        // Update individual scores
        tvMoodScore.text = "😊 $moodScore"
        tvSleepScore.text = "😴 $sleepScore"
        tvHydrationScore.text = "💧 $hydrationScore"
        tvActivityScore.text = "🏃 $activityScore"
        tvStressScore.text = "🧘 ${100 - stressScore}" // Show as positive (100 - stress)
    }
    private fun setupWeeklyChart() {
        try {
            val entries = ArrayList<Entry>()
            val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

            // Generate some sample data (you would replace this with actual data)
            for (i in days.indices) {
                entries.add(Entry(i.toFloat(), (60..90).random().toFloat()))
            }

            val dataSet = LineDataSet(entries, "Wellness Score").apply {
                color = ContextCompat.getColor(requireContext(), R.color.primary)
                valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
                lineWidth = 2.5f
                circleRadius = 4f
                circleHoleColor = ContextCompat.getColor(requireContext(), R.color.primary)
                circleHoleRadius = 2f
                setCircleColor(ContextCompat.getColor(requireContext(), R.color.primary))
                setDrawCircleHole(true)
                setDrawValues(false)
                mode = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity = 0.2f
            }

            val lineData = LineData(dataSet)
            chartWeeklySummary.data = lineData

            // Customize chart appearance
            chartWeeklySummary.description.isEnabled = false
            chartWeeklySummary.xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
            chartWeeklySummary.axisRight.isEnabled = false
            chartWeeklySummary.legend.isEnabled = false
            chartWeeklySummary.animateY(1000)

            // Set x-axis labels
            val xAxis = chartWeeklySummary.xAxis
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return days.getOrNull(value.toInt()) ?: ""
                }
            }

            // Refresh the chart
            chartWeeklySummary.invalidate()

        } catch (e: Exception) {
            Log.e("HomeFragment", "Error setting up weekly chart", e)
        }
    }
    
    fun onViewWeeklySummaryClick(view: View) {
        // Handle "View All" click for weekly summary
        // You can navigate to a detailed view here
        (activity as? com.example.wellnesmate.MainActivity)?.navigateToTab(1) // Navigate to Mood tab
    }
    
    fun onViewAllAchievementsClick(view: View) {
        // Handle "View All" click for achievements
        // You can navigate to a detailed achievements screen here
        // For now, just show a toast
        android.widget.Toast.makeText(
            requireContext(),
            "Viewing all achievements",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun getGreeting(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }
}
