package com.example.wellnesmate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wellnesmate.R
import com.example.wellnesmate.data.models.MoodEntry
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for mood history list
 */
class MoodHistoryAdapter(
    private val onDeleteClick: (MoodEntry) -> Unit,
    private val onShareClick: (MoodEntry) -> Unit
) : RecyclerView.Adapter<MoodHistoryAdapter.MoodHistoryViewHolder>() {

    private var moodEntries: List<MoodEntry> = emptyList()
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun updateMoodEntries(newEntries: List<MoodEntry>) {
        moodEntries = newEntries
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_history, parent, false)
        return MoodHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoodHistoryViewHolder, position: Int) {
        val entry = moodEntries[position]
        holder.bind(entry)
    }

    override fun getItemCount(): Int = moodEntries.size

    inner class MoodHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMoodEmoji: TextView = itemView.findViewById(R.id.tv_mood_emoji)
        private val tvMoodLabel: TextView = itemView.findViewById(R.id.tv_mood_label)
        private val tvMoodTime: TextView = itemView.findViewById(R.id.tv_mood_time)
        private val tvMoodDate: TextView = itemView.findViewById(R.id.tv_mood_date)
        private val tvMoodNotes: TextView = itemView.findViewById(R.id.tv_mood_notes)
        private val btnShareEntry: ImageButton = itemView.findViewById(R.id.btn_share_entry)
        private val btnDeleteEntry: ImageButton = itemView.findViewById(R.id.btn_delete_entry)

        fun bind(entry: MoodEntry) {
            tvMoodEmoji.text = entry.emoji
            tvMoodLabel.text = entry.mood.label
            tvMoodTime.text = timeFormat.format(entry.timestamp)

            // Format date
            val today = todayFormat.format(Date())
            val entryDate = todayFormat.format(entry.timestamp)
            
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = todayFormat.format(calendar.time)

            tvMoodDate.text = when (entryDate) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> dateFormat.format(entry.timestamp)
            }

            // Show notes if available
            if (entry.notes.isNotBlank()) {
                tvMoodNotes.text = entry.notes
                tvMoodNotes.visibility = View.VISIBLE
            } else {
                tvMoodNotes.visibility = View.GONE
            }

            // Click listeners
            btnShareEntry.setOnClickListener { onShareClick(entry) }
            btnDeleteEntry.setOnClickListener { onDeleteClick(entry) }
        }
    }
}