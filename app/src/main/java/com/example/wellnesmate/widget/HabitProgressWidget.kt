package com.example.wellnesmate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.wellnesmate.MainActivity
import com.example.wellnesmate.R
import com.example.wellnesmate.data.repository.SharedPreferencesManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Home screen widget showing today's habit completion progress
 */
class HabitProgressWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefsManager = SharedPreferencesManager.getInstance(context)
            val habits = prefsManager.getHabits()
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            
            // Calculate completion statistics
            val completedCount = if (habits.isNotEmpty()) {
                habits.count { habit ->
                    val progress = prefsManager.getHabitProgressForDay(habit.id, today)
                    progress?.isCompleted == true
                }
            } else {
                0
            }
            
            val totalCount = habits.size
            val progressPercentage = if (totalCount > 0) {
                ((completedCount.toFloat() / totalCount.toFloat()) * 100).toInt()
            } else {
                0
            }

            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.widget_habit_progress)
            
            // Update widget content
            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
            views.setTextViewText(
                R.id.widget_progress_text,
                context.getString(R.string.widget_habits_completed, completedCount, totalCount)
            )
            views.setTextViewText(
                R.id.widget_progress_percentage,
                "$progressPercentage%"
            )
            views.setProgressBar(R.id.widget_progress_bar, 100, progressPercentage, false)
            
            // Set up click intent to open the app
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}