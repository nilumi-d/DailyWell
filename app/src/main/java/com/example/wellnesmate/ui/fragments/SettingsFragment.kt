package com.example.wellnesmate.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.wellnesmate.MainActivity
import com.example.wellnesmate.R
import com.example.wellnesmate.data.repository.SharedPreferencesManager
import com.example.wellnesmate.ui.auth.LoginActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Fragment for app settings and preferences
 */
class SettingsFragment : Fragment() {
    
    private lateinit var prefsManager: SharedPreferencesManager
    private lateinit var cardHydrationSettings: MaterialCardView
    private lateinit var cardAppInfo: MaterialCardView
    private lateinit var cardLogout: MaterialCardView  // Add this line
    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var tvAppVersion: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Update toolbar title (removed as toolbar is removed)
        // (activity as? MainActivity)?.updateToolbarTitle(getString(R.string.settings_title))
        
        // Initialize components
        initializeViews(view)
        setupClickListeners()
        loadSettings()
    }
    
    private fun initializeViews(view: View) {
        prefsManager = SharedPreferencesManager.getInstance(requireContext())
        cardHydrationSettings = view.findViewById(R.id.card_hydration_settings)
        cardAppInfo = view.findViewById(R.id.card_app_info)
        cardLogout = view.findViewById(R.id.card_logout)  // Add this line
        // Data management card removed as per user request
        switchNotifications = view.findViewById(R.id.switch_notifications)
        tvAppVersion = view.findViewById(R.id.tv_app_version)
    }
    
    private fun setupClickListeners() {
        cardHydrationSettings.setOnClickListener {
            // Open hydration settings (could navigate to hydration fragment)
            (activity as? MainActivity)?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
                ?.selectedItemId = R.id.nav_hydration
        }
        
        cardAppInfo.setOnClickListener {
            showAboutDialog()
        }
        
        cardLogout.setOnClickListener {
            showLogoutConfirmation()
        }
        
        // Data management click listener removed as per user request
        
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationSettings(isChecked)
        }
    }
    
    private fun loadSettings() {
        val hydrationSettings = prefsManager.getHydrationSettings()
        switchNotifications.isChecked = hydrationSettings.reminderEnabled
        tvAppVersion.text = getString(R.string.version_info)
    }
    
    private fun updateNotificationSettings(enabled: Boolean) {
        val currentSettings = prefsManager.getHydrationSettings()
        val newSettings = currentSettings.copy(reminderEnabled = enabled)
        prefsManager.saveHydrationSettings(newSettings)
        
        if (enabled) {
            // Enable notifications
            android.widget.Toast.makeText(
                requireContext(),
                "Notifications enabled",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } else {
            // Disable notifications
            androidx.work.WorkManager.getInstance(requireContext())
                .cancelUniqueWork("hydration_reminder")
            android.widget.Toast.makeText(
                requireContext(),
                "Notifications disabled",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.about))
            .setMessage(getString(R.string.about_description))
            .setPositiveButton(getString(R.string.ok), null)
            .show()
    }
    
    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun logout() {
        // Clear user login status
        prefsManager.setUserLoggedIn(false)
        
        // Navigate to login screen
        val intent = Intent(activity, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
    
    // Data management methods removed as per user request
    /*
    private fun showDataManagementDialog() {
        val options = arrayOf(
            "Export Data",
            "Reset All Data"
        )
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Data Management")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportData()
                    1 -> showResetDataConfirmation()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun exportData() {
        try {
            val exportData = prefsManager.exportData()
            // In a real app, you'd save this to file or share it
            // For now, we'll just show a toast
            android.widget.Toast.makeText(
                requireContext(),
                "Data exported successfully",
                android.widget.Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                requireContext(),
                "Export failed: ${e.message}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun showResetDataConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reset All Data")
            .setMessage("This will permanently delete all your habits, mood entries, and hydration data. This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                resetAllData()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun resetAllData() {
        prefsManager.clearAllData()
        
        // Cancel any pending work
        androidx.work.WorkManager.getInstance(requireContext())
            .cancelUniqueWork("hydration_reminder")
        
        android.widget.Toast.makeText(
            requireContext(),
            "All data has been reset",
            android.widget.Toast.LENGTH_LONG
        ).show()
        
        // Refresh the current fragment if needed
        loadSettings()
    }
    */
}