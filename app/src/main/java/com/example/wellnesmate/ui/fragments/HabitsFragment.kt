package com.example.wellnesmate.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wellnesmate.MainActivity
import com.example.wellnesmate.R
import com.example.wellnesmate.data.models.Habit
import com.example.wellnesmate.data.models.HabitProgress
import com.example.wellnesmate.data.repository.SharedPreferencesManager
import com.example.wellnesmate.ui.adapters.HabitsAdapter
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.chip.ChipGroup
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment for displaying and managing daily habits
 */
class HabitsFragment : Fragment() {
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAddHabit: ExtendedFloatingActionButton
    private lateinit var tvProgressSummary: TextView
    private lateinit var progressBarDaily: ProgressBar
    private lateinit var layoutEmptyState: View
    private lateinit var tvHabitCount: TextView  // Add this line
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var habitsAdapter: HabitsAdapter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_habits, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Update toolbar title (commented out as toolbar is removed)
        // (activity as? MainActivity)?.updateToolbarTitle(getString(R.string.habits_title))
        
        // Initialize components
        initializeViews(view)
        setupRecyclerView()
        setupClickListeners()
        loadHabits()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh data when fragment becomes visible
        loadHabits()
    }
    
    private fun initializeViews(view: View) {
        prefsManager = SharedPreferencesManager.getInstance(requireContext())
        recyclerView = view.findViewById(R.id.recycler_habits)
        fabAddHabit = view.findViewById(R.id.fab_add_habit)
        tvProgressSummary = view.findViewById(R.id.tv_progress_summary)
        progressBarDaily = view.findViewById(R.id.progress_bar_daily)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)
        tvHabitCount = view.findViewById(R.id.tv_habit_count)  // Add this line
    }
    
    private fun setupRecyclerView() {
        habitsAdapter = HabitsAdapter(
            onHabitClick = { habit -> editHabit(habit) },
            onProgressClick = { habit, progress -> toggleHabitProgress(habit, progress) },
            onDeleteClick = { habit -> deleteHabit(habit) },
            onShareClick = { habit -> shareHabitProgress(habit) }
        )
        
        // Use different layout managers based on screen size
        val layoutManager = if (isTablet()) {
            androidx.recyclerview.widget.GridLayoutManager(context, 2)
        } else {
            LinearLayoutManager(context)
        }
        
        recyclerView.apply {
            this.layoutManager = layoutManager
            adapter = habitsAdapter
            setHasFixedSize(true)
        }
    }
    
    private fun setupClickListeners() {
        fabAddHabit.setOnClickListener {
            addNewHabit()
        }
    }
    
    private fun loadHabits() {
        val habits = prefsManager.getHabits()
        val today = dateFormat.format(Date())
        
        // Update habit count
        tvHabitCount.text = "${habits.size} ${if (habits.size == 1) "habit" else "habits"}"
        
        if (habits.isEmpty()) {
            // Show empty state
            recyclerView.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
            tvProgressSummary.text = "0 of 0 habits completed (0%)"
            progressBarDaily.progress = 0
            return
        }
        
        // Hide empty state
        recyclerView.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE
        
        // Get today's progress for each habit
        val habitsWithProgress = habits.map { habit ->
            val progress = prefsManager.getHabitProgressForDay(habit.id, today)
                ?: HabitProgress(habit.id, today)
            Pair(habit, progress)
        }
        
        // Update progress summary
        val completedCount = habitsWithProgress.count { it.second.isCompleted }
        val totalCount = habits.size
        val progressPercentage = if (totalCount > 0) {
            ((completedCount.toFloat() / totalCount.toFloat()) * 100).toInt()
        } else {
            0
        }
        
        tvProgressSummary.text = "$completedCount of $totalCount habits completed ($progressPercentage%)"
        progressBarDaily.progress = progressPercentage
        
        habitsAdapter.updateHabits(habitsWithProgress)
    }
    
    private fun addNewHabit() {
        showAddHabitDialog()
    }
    
    private fun editHabit(habit: Habit) {
        showEditHabitDialog(habit)
    }
    
    private fun toggleHabitProgress(habit: Habit, currentProgress: HabitProgress) {
        val today = dateFormat.format(Date())
        val newProgress = if (currentProgress.isCompleted) {
            // Mark as incomplete
            currentProgress.copy(
                isCompleted = false,
                currentValue = 0,
                completionTime = null
            )
        } else {
            // Mark as complete
            currentProgress.copy(
                isCompleted = true,
                currentValue = habit.targetValue,
                completionTime = Date()
            )
        }
        
        prefsManager.saveHabitProgressForDay(habit.id, today, newProgress)
        loadHabits() // Refresh the list
    }
    
    private fun deleteHabit(habit: Habit) {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_habit))
            .setMessage("Are you sure you want to delete \"${habit.name}\"?")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                prefsManager.deleteHabit(habit.id)
                loadHabits()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun shareHabitProgress(habit: Habit) {
        val today = dateFormat.format(Date())
        val progress = prefsManager.getHabitProgressForDay(habit.id, today)
        val progressText = if (progress?.isCompleted == true) {
            "Completed: ${habit.name}"
        } else {
            "Working on: ${habit.name}"
        }
        
        val shareText = getString(R.string.share_habit_progress, progressText)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)))
    }
    
    private fun setupUnitSelector(dialogView: View, spinnerUnit: AutoCompleteTextView, chipGroup: ChipGroup) {
        // Set up chip group click listeners
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.setOnClickListener {
                spinnerUnit.setText(chip.text.toString(), false)
                chipGroup.visibility = View.GONE
            }
        }
        
        // Set up spinner click to show chips
        spinnerUnit.setOnClickListener {
            chipGroup.visibility = if (chipGroup.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
    }
    
    private fun showAddHabitDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_habit, null)
        
        val etHabitName = dialogView.findViewById<TextInputEditText>(R.id.et_habit_name)
        val etHabitDescription = dialogView.findViewById<TextInputEditText>(R.id.et_habit_description)
        val etTargetValue = dialogView.findViewById<TextInputEditText>(R.id.et_target_value)
        val spinnerUnit = dialogView.findViewById<AutoCompleteTextView>(R.id.spinner_unit)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chip_group_units)
        
        // Setup unit selector
        val units = arrayOf("times", "minutes", "hours", "glasses", "pages", "km")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        spinnerUnit.setAdapter(adapter)
        spinnerUnit.setText("times", false)
        
        // Setup unit selector with chips
        setupUnitSelector(dialogView, spinnerUnit, chipGroup)
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.add_habit))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = etHabitName.text?.toString()?.trim()
                val description = etHabitDescription.text?.toString()?.trim() ?: ""
                val targetText = etTargetValue.text?.toString()?.trim()
                val unit = spinnerUnit.text?.toString() ?: "times"
                
                if (!name.isNullOrBlank() && !targetText.isNullOrBlank()) {
                    try {
                        val target = targetText.toInt()
                        if (target > 0) {
                            val habit = Habit(
                                name = name,
                                description = description,
                                targetValue = target,
                                unit = unit
                            )
                            prefsManager.saveHabit(habit)
                            loadHabits()
                            
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Habit added successfully!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Target value must be greater than 0",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: NumberFormatException) {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Please enter a valid target number",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Please fill in all required fields",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showEditHabitDialog(habit: Habit) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_habit, null)
        
        val etHabitName = dialogView.findViewById<TextInputEditText>(R.id.et_habit_name)
        val etHabitDescription = dialogView.findViewById<TextInputEditText>(R.id.et_habit_description)
        val etTargetValue = dialogView.findViewById<TextInputEditText>(R.id.et_target_value)
        val spinnerUnit = dialogView.findViewById<AutoCompleteTextView>(R.id.spinner_unit)
        val chipGroup = dialogView.findViewById<ChipGroup>(R.id.chip_group_units)
        
        // Pre-fill with current values
        etHabitName.setText(habit.name)
        etHabitDescription.setText(habit.description)
        etTargetValue.setText(habit.targetValue.toString())
        
        // Setup unit selector
        val units = arrayOf("times", "minutes", "hours", "glasses", "pages", "km")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
        spinnerUnit.setAdapter(adapter)
        spinnerUnit.setText(habit.unit, false)
        
        // Setup unit selector with chips
        setupUnitSelector(dialogView, spinnerUnit, chipGroup)
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.edit_habit))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = etHabitName.text?.toString()?.trim()
                val description = etHabitDescription.text?.toString()?.trim() ?: ""
                val targetText = etTargetValue.text?.toString()?.trim()
                val unit = spinnerUnit.text?.toString() ?: "times"
                
                if (!name.isNullOrBlank() && !targetText.isNullOrBlank()) {
                    try {
                        val target = targetText.toInt()
                        if (target > 0) {
                            val updatedHabit = habit.copy(
                                name = name,
                                description = description,
                                targetValue = target,
                                unit = unit
                            )
                            prefsManager.saveHabit(updatedHabit)
                            loadHabits()
                            
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Habit updated successfully!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                requireContext(),
                                "Target value must be greater than 0",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: NumberFormatException) {
                        android.widget.Toast.makeText(
                            requireContext(),
                            "Please enter a valid target number",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Please fill in all required fields",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun isTablet(): Boolean {
        val configuration = resources.configuration
        return configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK >= android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE
    }
}