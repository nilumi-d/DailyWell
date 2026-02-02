package com.example.wellnesmate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wellnesmate.R
import com.example.wellnesmate.data.models.MoodType

/**
 * Adapter for mood selector grid
 */
class MoodSelectorAdapter(
    private val onMoodSelected: (MoodType) -> Unit
) : RecyclerView.Adapter<MoodSelectorAdapter.MoodSelectorViewHolder>() {

    private var moods: List<MoodType> = emptyList()
    private var selectedPosition: Int = -1

    fun updateMoods(newMoods: List<MoodType>) {
        moods = newMoods
        notifyDataSetChanged()
    }

    fun clearSelection() {
        val previousPosition = selectedPosition
        selectedPosition = -1
        if (previousPosition != -1) {
            notifyItemChanged(previousPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MoodSelectorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_mood_selector, parent, false)
        return MoodSelectorViewHolder(view)
    }

    override fun onBindViewHolder(holder: MoodSelectorViewHolder, position: Int) {
        val mood = moods[position]
        holder.bind(mood, position == selectedPosition)
    }

    override fun getItemCount(): Int = moods.size

    inner class MoodSelectorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMoodEmoji: TextView = itemView.findViewById(R.id.tv_mood_emoji)
        private val tvMoodLabel: TextView = itemView.findViewById(R.id.tv_mood_label)

        fun bind(mood: MoodType, isSelected: Boolean) {
            tvMoodEmoji.text = mood.emoji
            tvMoodLabel.text = mood.label

            itemView.isSelected = isSelected

            itemView.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = adapterPosition
                
                // Update the previous selected item
                if (previousPosition != -1) {
                    notifyItemChanged(previousPosition)
                }
                // Update the current selected item
                notifyItemChanged(selectedPosition)
                
                onMoodSelected(mood)
            }
        }
    }
}