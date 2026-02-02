package com.example.wellnesmate.ui.fragments

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wellnesmate.MainActivity
import com.example.wellnesmate.R
import com.example.wellnesmate.data.models.MoodEntry
import com.example.wellnesmate.data.models.MoodType
import com.example.wellnesmate.data.repository.SharedPreferencesManager
import com.example.wellnesmate.ui.adapters.MoodSelectorAdapter
import com.example.wellnesmate.ui.adapters.MoodHistoryAdapter
import com.example.wellnesmate.ui.charts.MoodChartHelper
import com.github.mikephil.charting.charts.LineChart
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * Fragment for mood journaling with emoji selector
 */
class MoodFragment : Fragment() {
    
    private lateinit var recyclerMoodSelector: RecyclerView
    private lateinit var recyclerMoodHistory: RecyclerView
    private lateinit var btnSaveMood: MaterialButton
    private lateinit var btnShareMood: MaterialButton
    private lateinit var chartMoodTrend: LineChart
    private lateinit var tabLayoutMoodView: TabLayout
    private lateinit var layoutMoodCalendar: View
    
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var moodSelectorAdapter: MoodSelectorAdapter
    private lateinit var moodHistoryAdapter: MoodHistoryAdapter
    private lateinit var moodChartHelper: MoodChartHelper
    
    private var selectedMood: MoodType? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mood, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize components
        initializeViews(view)
        setupMoodSelector()
        setupMoodHistory()
        setupMoodViewTabs()
        setupClickListeners()
        loadMoodHistory()
    }
    
    override fun onResume() {
        super.onResume()
        loadMoodHistory()
    }
    
    private fun initializeViews(view: View) {
        prefsManager = SharedPreferencesManager.getInstance(requireContext())
        moodChartHelper = MoodChartHelper(requireContext())
        recyclerMoodSelector = view.findViewById(R.id.recycler_mood_selector)
        recyclerMoodHistory = view.findViewById(R.id.recycler_mood_history)
        btnSaveMood = view.findViewById(R.id.btn_save_mood)
        btnShareMood = view.findViewById(R.id.btn_share_mood)
        chartMoodTrend = view.findViewById(R.id.chart_mood_trend)
        tabLayoutMoodView = view.findViewById(R.id.tab_layout_mood_view)
        layoutMoodCalendar = view.findViewById(R.id.layout_mood_calendar)
    }
    
    private fun setupMoodSelector() {
        moodSelectorAdapter = MoodSelectorAdapter { mood ->
            selectedMood = mood
            updateSaveButtonState()
        }
        
        recyclerMoodSelector.apply {
            layoutManager = GridLayoutManager(context, 5) // 5 columns for emojis
            adapter = moodSelectorAdapter
            setHasFixedSize(true)
        }
        
        // Load all available moods
        moodSelectorAdapter.updateMoods(MoodType.getAllMoods())
    }
    
    private fun setupMoodHistory() {
        moodHistoryAdapter = MoodHistoryAdapter(
            onDeleteClick = { entry -> deleteMoodEntry(entry) },
            onShareClick = { entry -> shareMoodEntry(entry) }
        )
        
        recyclerMoodHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = moodHistoryAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun setupMoodViewTabs() {
        // Add tabs for List and Calendar views
        val listTab = tabLayoutMoodView.newTab().setText("List")
        val calendarTab = tabLayoutMoodView.newTab().setText("Calendar")
        tabLayoutMoodView.addTab(listTab)
        tabLayoutMoodView.addTab(calendarTab)
        
        // Set up tab listener
        tabLayoutMoodView.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showListView()
                    1 -> showCalendarView()
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupClickListeners() {
        btnSaveMood.setOnClickListener {
            saveMoodEntry()
        }
        
        btnShareMood.setOnClickListener {
            shareTodaysMood()
        }
    }
    
    private fun updateSaveButtonState() {
        btnSaveMood.isEnabled = selectedMood != null
    }
    
    private fun saveMoodEntry() {
        val mood = selectedMood ?: return
        
        val moodEntry = MoodEntry(
            mood = mood,
            emoji = mood.emoji,
            notes = "", // Removed notes field
            timestamp = Date()
        )
        
        prefsManager.saveMoodEntry(moodEntry)
        
        // Reset form
        selectedMood = null
        moodSelectorAdapter.clearSelection()
        updateSaveButtonState()
        
        // Optimized: Only update today's mood data point in chart
        val todayEntries = prefsManager.getTodayMoodEntries()
        moodChartHelper.updateTodayMoodOnly(chartMoodTrend, todayEntries)
        
        // Update history list (only new entries)
        val moodEntries = prefsManager.getMoodEntries()
        moodHistoryAdapter.updateMoodEntries(moodEntries)
        
        // Update share button state
        btnShareMood.isEnabled = todayEntries.isNotEmpty()
        
        // Update empty state visibility
        val isEmpty = moodEntries.isEmpty()
        recyclerMoodHistory.visibility = if (isEmpty) View.GONE else View.VISIBLE
        view?.findViewById<View>(R.id.layout_empty_mood_history)?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        
        // If calendar view is visible, update it too
        if (layoutMoodCalendar.visibility == View.VISIBLE) {
            generateCalendarView()
        }
        
        // Show success message
        android.widget.Toast.makeText(
            requireContext(),
            "Mood saved successfully!",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun loadMoodHistory() {
        val moodEntries = prefsManager.getMoodEntries()
        moodHistoryAdapter.updateMoodEntries(moodEntries)
        
        // Update mood trend chart
        moodChartHelper.setupMoodTrendChart(chartMoodTrend, moodEntries)
        
        // Update share button state
        val todayEntries = prefsManager.getTodayMoodEntries()
        btnShareMood.isEnabled = todayEntries.isNotEmpty()
        
        // Update empty state visibility
        val isEmpty = moodEntries.isEmpty()
        recyclerMoodHistory.visibility = if (isEmpty) View.GONE else View.VISIBLE
        view?.findViewById<View>(R.id.layout_empty_mood_history)?.visibility = if (isEmpty) View.VISIBLE else View.GONE
        
        // If calendar view is visible, update it too
        if (layoutMoodCalendar.visibility == View.VISIBLE) {
            generateCalendarView()
        }
    }
    
    private fun deleteMoodEntry(entry: MoodEntry) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Mood Entry")
            .setMessage("Are you sure you want to delete this mood entry?")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                prefsManager.deleteMoodEntry(entry.id)
                loadMoodHistory()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun shareMoodEntry(entry: MoodEntry) {
        val shareText = "My mood: ${entry.mood.label} ${entry.emoji}"
        val formattedText = getString(R.string.share_mood_summary, shareText)
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, formattedText)
        }
        
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
    }
    
    private fun shareTodaysMood() {
        val todayEntries = prefsManager.getTodayMoodEntries()
        if (todayEntries.isEmpty()) return
        
        val moodSummary = if (todayEntries.size == 1) {
            "${todayEntries.first().mood.label} ${todayEntries.first().emoji}"
        } else {
            val moods = todayEntries.joinToString(", ") { "${it.mood.label} ${it.emoji}" }
            "Multiple moods today: $moods"
        }
        
        val shareText = getString(R.string.share_mood_summary, moodSummary)
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
    }
    
    private fun showListView() {
        recyclerMoodHistory.visibility = View.VISIBLE
        layoutMoodCalendar.visibility = View.GONE
    }
    
    private fun showCalendarView() {
        recyclerMoodHistory.visibility = View.GONE
        layoutMoodCalendar.visibility = View.VISIBLE
        
        // Generate calendar view
        generateCalendarView()
    }
    
    private fun generateCalendarView() {
        // Clear existing calendar content
        if (layoutMoodCalendar is ViewGroup) {
            (layoutMoodCalendar as ViewGroup).removeAllViews()
        }
        
        // Get mood entries grouped by date
        val moodEntries = prefsManager.getMoodEntries()
        if (moodEntries.isEmpty()) {
            // Show empty state for calendar view
            val emptyView = TextView(requireContext()).apply {
                text = getString(R.string.no_mood_entries_calendar)
                textSize = 16f
                setTextColor(requireContext().getColor(R.color.text_secondary))
                gravity = android.view.Gravity.CENTER
                setPadding(32, 64, 32, 64)
            }
            if (layoutMoodCalendar is ViewGroup) {
                (layoutMoodCalendar as ViewGroup).addView(emptyView)
            }
            return
        }
        
        // Group entries by date
        val entriesByDate = moodEntries.groupBy { it.date }
        
        // Sort dates in descending order (newest first)
        val sortedDates = entriesByDate.keys.sortedDescending()
        
        // Create calendar layout
        val calendarLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        // Add title
        val title = TextView(requireContext()).apply {
            text = getString(R.string.mood_calendar_title)
            textSize = 20f
            setTextColor(requireContext().getColor(R.color.text_primary))
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 16)
            gravity = android.view.Gravity.CENTER
        }
        calendarLayout.addView(title)
        
        // Add explanation text
        val explanation = TextView(requireContext()).apply {
            text = getString(R.string.mood_calendar_explanation)
            textSize = 14f
            setTextColor(requireContext().getColor(R.color.text_secondary))
            setPadding(0, 0, 0, 16)
            gravity = android.view.Gravity.CENTER
        }
        calendarLayout.addView(explanation)
        
        // Display entries grouped by date
        sortedDates.forEachIndexed { index, date ->
            val entries = entriesByDate[date] ?: emptyList()
            
            // Create a card for each date
            val dateCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, 16)
                }
                setCardBackgroundColor(requireContext().getColor(R.color.card_background))
                radius = 12f
                cardElevation = 4f
            }
            
            val dateCardContent = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 16, 16, 16)
            }
            
            // Date header with better styling
            val dateHeader = TextView(requireContext()).apply {
                text = formatDateHeader(date)
                textSize = 18f
                setTextColor(requireContext().getColor(R.color.primary_green))
                setTypeface(null, Typeface.BOLD)
                setPadding(0, 0, 0, 16)
            }
            dateCardContent.addView(dateHeader)
            
            // Mood entries for this date
            entries.forEachIndexed { entryIndex, entry ->
                val entryView = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 8, 0, 8)
                }
                
                // Emoji with larger size
                val emojiView = TextView(requireContext()).apply {
                    text = entry.emoji
                    textSize = 24f
                    setPadding(0, 0, 16, 0)
                    minWidth = 48
                    gravity = android.view.Gravity.CENTER
                }
                entryView.addView(emojiView)
                
                // Mood label and time
                val infoLayout = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                }
                
                val moodLabel = TextView(requireContext()).apply {
                    text = entry.mood.label
                    textSize = 16f
                    setTextColor(requireContext().getColor(R.color.text_primary))
                    setTypeface(null, Typeface.BOLD)
                }
                infoLayout.addView(moodLabel)
                
                val timeText = TextView(requireContext()).apply {
                    text = formatTime(entry.timestamp)
                    textSize = 14f
                    setTextColor(requireContext().getColor(R.color.text_secondary))
                    setPadding(0, 4, 0, 0)
                }
                infoLayout.addView(timeText)
                
                entryView.addView(infoLayout)
                dateCardContent.addView(entryView)
                
                // Add separator between entries (except for the last one)
                if (entryIndex < entries.size - 1) {
                    val divider = View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            1
                        ).apply {
                            setMargins(0, 8, 0, 8)
                        }
                        setBackgroundColor(requireContext().getColor(R.color.divider_color))
                    }
                    dateCardContent.addView(divider)
                }
            }
            
            dateCard.addView(dateCardContent)
            calendarLayout.addView(dateCard)
        }
        
        // Add to calendar layout
        if (layoutMoodCalendar is ViewGroup) {
            (layoutMoodCalendar as ViewGroup).addView(calendarLayout)
        }
    }
    
    private fun formatDateHeader(dateString: String): String {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = dateFormat.parse(dateString) ?: return dateString
            val today = Date()
            val todayString = dateFormat.format(today)
            
            val displayFormat = SimpleDateFormat("EEEE, MMM dd, yyyy", Locale.getDefault())
            val formattedDate = displayFormat.format(date)
            
            // Add "Today" indicator if this is today's date
            if (dateString == todayString) {
                "$formattedDate (Today)"
            } else {
                formattedDate
            }
        } catch (e: Exception) {
            dateString
        }
    }
    
    private fun formatTime(date: Date): String {
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        return timeFormat.format(date)
    }
}