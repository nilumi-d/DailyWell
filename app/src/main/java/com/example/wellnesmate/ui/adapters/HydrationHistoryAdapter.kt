package com.example.wellnesmate.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wellnesmate.R
import com.example.wellnesmate.data.models.HydrationIntake
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for hydration history list
 */
class HydrationHistoryAdapter : RecyclerView.Adapter<HydrationHistoryAdapter.HydrationHistoryViewHolder>() {

    private var intakes: List<HydrationIntake> = emptyList()
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val todayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun updateIntakes(newIntakes: List<HydrationIntake>) {
        intakes = newIntakes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HydrationHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hydration_history, parent, false)
        return HydrationHistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HydrationHistoryViewHolder, position: Int) {
        val intake = intakes[position]
        holder.bind(intake)
    }

    override fun getItemCount(): Int = intakes.size

    inner class HydrationHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvIntakeAmount: TextView = itemView.findViewById(R.id.tv_intake_amount)
        private val tvIntakeTime: TextView = itemView.findViewById(R.id.tv_intake_time)
        private val tvIntakeDate: TextView = itemView.findViewById(R.id.tv_intake_date)

        fun bind(intake: HydrationIntake) {
            tvIntakeAmount.text = "${intake.amountMl} ml"
            tvIntakeTime.text = timeFormat.format(intake.timestamp)

            // Check if it's today
            val today = todayFormat.format(Date())
            val intakeDate = todayFormat.format(intake.timestamp)
            
            if (intakeDate == today) {
                tvIntakeTime.text = "${timeFormat.format(intake.timestamp)} • Today"
                tvIntakeDate.visibility = View.GONE
            } else {
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val yesterday = todayFormat.format(calendar.time)
                
                if (intakeDate == yesterday) {
                    tvIntakeTime.text = "${timeFormat.format(intake.timestamp)} • Yesterday"
                    tvIntakeDate.visibility = View.GONE
                } else {
                    tvIntakeTime.text = timeFormat.format(intake.timestamp)
                    tvIntakeDate.text = dateFormat.format(intake.timestamp)
                    tvIntakeDate.visibility = View.VISIBLE
                }
            }
        }
    }
}