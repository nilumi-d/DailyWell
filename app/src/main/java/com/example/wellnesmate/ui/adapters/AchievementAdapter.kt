package com.example.wellnesmate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wellnesmate.R
import com.example.wellnesmate.data.models.Achievement
import com.google.android.material.card.MaterialCardView

class AchievementAdapter(
    private val achievements: List<Achievement>,
    private val onAchievementClick: (Achievement) -> Unit = {}
) : RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]
        holder.bind(achievement)
        
        holder.itemView.setOnClickListener {
            onAchievementClick(achievement)
        }
    }

    override fun getItemCount(): Int = achievements.size

    class AchievementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: MaterialCardView = itemView.findViewById(R.id.card_achievement)
        private val iconView: ImageView = itemView.findViewById(R.id.iv_achievement_icon)
        private val titleView: TextView = itemView.findViewById(R.id.tv_achievement_title)
        private val descView: TextView = itemView.findViewById(R.id.tv_achievement_desc)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.pb_achievement)
        private val progressText: TextView = itemView.findViewById(R.id.tv_achievement_progress)
        private val unlockedView: View = itemView.findViewById(R.id.view_achievement_unlocked)

        fun bind(achievement: Achievement) {
            iconView.setImageResource(achievement.iconResId)
            titleView.text = achievement.title
            descView.text = achievement.description
            
            if (achievement.isUnlocked) {
                // Show unlocked state
                cardView.alpha = 1.0f
                progressBar.visibility = View.GONE
                progressText.visibility = View.GONE
                unlockedView.visibility = View.VISIBLE
                
                // Apply a subtle elevation and stroke for unlocked achievements
                cardView.strokeWidth = 0
                cardView.elevation = 4f
            } else {
                // Show in-progress state
                cardView.alpha = 0.6f
                progressBar.visibility = View.VISIBLE
                progressText.visibility = View.VISIBLE
                unlockedView.visibility = View.GONE
                
                // Show progress
                progressBar.progress = achievement.progress
                progressText.text = "${achievement.progress}%"
                
                // Style for locked achievements
                cardView.strokeWidth = 2
                cardView.elevation = 2f
            }
        }
    }
}
