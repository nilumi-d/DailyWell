package com.example.wellnesmate.ui.charts

import android.content.Context
import com.example.wellnesmate.R
import com.example.wellnesmate.data.models.MoodEntry
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper class for creating mood trend charts
 */
class MoodChartHelper(private val context: Context) {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    
    fun setupMoodTrendChart(chart: LineChart, moodEntries: List<MoodEntry>) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            setDragEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            
            // Add rounded corners and shadow effect
            setBackgroundColor(context.getColor(R.color.card_background))
            setDrawBorders(true)
            setBorderColor(android.graphics.Color.TRANSPARENT)
            setBorderWidth(0f)
            
            // Add extra offset for shadow effect
            setExtraOffsets(16f, 16f, 16f, 16f)
            
            // Configure X-axis with enhanced styling
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                labelCount = 7
                textColor = context.getColor(R.color.text_secondary)
                textSize = 12f
                setDrawAxisLine(true)
                axisLineColor = context.getColor(R.color.text_hint)
                axisLineWidth = 2f
            }
            
            // Configure Y-axis with enhanced styling
            axisLeft.apply {
                axisMinimum = -3f
                axisMaximum = 3f
                setDrawGridLines(true)
                gridColor = context.getColor(R.color.text_hint)
                gridLineWidth = 1f
                enableGridDashedLine(10f, 5f, 0f)
                textColor = context.getColor(R.color.text_secondary)
                textSize = 12f
                setDrawAxisLine(true)
                axisLineColor = context.getColor(R.color.text_hint)
                axisLineWidth = 2f
            }
            
            axisRight.isEnabled = false
            
            // Configure legend with enhanced styling
            legend.apply {
                isEnabled = true
                textColor = context.getColor(R.color.text_primary)
                textSize = 14f
                form = com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE
                formSize = 12f
                xEntrySpace = 8f
                yEntrySpace = 4f
            }
            
            // Enable animations
            animateX(800)
            animateY(800)
        }
        
        // Process mood data for the last 7 days
        val chartData = prepareMoodData(moodEntries)
        
        if (chartData.isNotEmpty()) {
            val dataSet = LineDataSet(chartData, "Daily Mood ✨").apply {
                // Enhanced line styling with glow effect
                color = context.getColor(R.color.primary_green)
                lineWidth = 4f
                
                // Enhanced circle styling with shadow effect
                setCircleColor(context.getColor(R.color.primary_green))
                circleRadius = 8f
                setDrawCircleHole(true)
                circleHoleRadius = 4f
                circleHoleColor = android.graphics.Color.WHITE
                
                // Add shadow to circles
                setDrawCircles(true)
                
                // Enhanced value text styling
                valueTextSize = 11f
                valueTextColor = context.getColor(R.color.text_primary)
                setDrawValues(true)
                
                // Enhanced fill with gradient effect
                setDrawFilled(true)
                fillColor = context.getColor(R.color.primary_green_light)
                fillAlpha = 80
                
                // Smooth curve
                mode = LineDataSet.Mode.CUBIC_BEZIER
                cubicIntensity = 0.2f
                
                // Highlight styling
                highLightColor = context.getColor(R.color.accent_blue)
                setDrawHighlightIndicators(true)
                highlightLineWidth = 2f
            }
            
            val lineData = LineData(dataSet)
            chart.data = lineData
            
            // Set up X-axis labels
            val labels = getLast7DaysLabels()
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            
            chart.invalidate() // Refresh chart
        } else {
            chart.clear()
            chart.invalidate()
        }
    }
    
    private fun prepareMoodData(moodEntries: List<MoodEntry>): List<Entry> {
        val calendar = Calendar.getInstance()
        val entries = mutableListOf<Entry>()
        
        // Get mood data for the last 7 days
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateString = dateFormat.format(calendar.time)
            
            // Find moods for this day
            val dayMoods = moodEntries.filter { it.date == dateString }
            
            if (dayMoods.isNotEmpty()) {
                // Calculate average mood for the day and center around 0
                // Convert 1-5 scale to -2 to +2 scale (0 in middle)
                val averageMood = dayMoods.map { it.mood.value }.average().toFloat()
                val centeredMood = averageMood - 3f  // Shift from 1-5 to -2 to +2
                entries.add(Entry((6 - i).toFloat(), centeredMood))
            } else {
                // No mood entry for this day
                entries.add(Entry((6 - i).toFloat(), 0f)) // 0 for no data
            }
        }
        
        return entries
    }
    
    private fun getLast7DaysLabels(): List<String> {
        val calendar = Calendar.getInstance()
        val labels = mutableListOf<String>()
        
        for (i in 6 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            labels.add(dayFormat.format(calendar.time))
        }
        
        return labels
    }
    
    /**
     * Optimized method to update only today's mood data point
     * Use this when adding a new mood entry to avoid recalculating all 7 days
     */
    fun updateTodayMoodOnly(chart: LineChart, todayMoodEntries: List<MoodEntry>) {
        if (chart.data == null || chart.data.dataSetCount == 0) {
            // If chart has no data, do a full setup instead
            return
        }
        
        val dataSet = chart.data.getDataSetByIndex(0) as? LineDataSet ?: return
        
        // Calculate today's average mood
        val todayAverage = if (todayMoodEntries.isNotEmpty()) {
            val averageMood = todayMoodEntries.map { it.mood.value }.average().toFloat()
            averageMood - 3f  // Center around 0
        } else {
            0f
        }
        
        // Update the last entry (index 6 = today)
        val todayEntry = Entry(6f, todayAverage)
        
        // Remove old entry by index (6 = today's position)
        if (dataSet.entryCount > 6) {
            dataSet.removeEntry(6)
        }
        dataSet.addEntry(todayEntry)
        
        // Notify chart of data change
        chart.data.notifyDataChanged()
        chart.notifyDataSetChanged()
        
        // Animate only the Y-axis for smooth update
        chart.animateY(300)
    }
}