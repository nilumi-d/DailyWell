package com.example.wellnesmate

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.wellnesmate.data.repository.SharedPreferencesManager
import com.example.wellnesmate.receivers.HydrationAlarmScheduler
import com.example.wellnesmate.ui.auth.LoginActivity
import com.example.wellnesmate.ui.fragments.HomeFragment
import com.example.wellnesmate.ui.fragments.HabitsFragment
import com.example.wellnesmate.ui.fragments.HydrationFragment
import com.example.wellnesmate.ui.fragments.MoodFragment
import com.example.wellnesmate.ui.fragments.SettingsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var prefsManager: SharedPreferencesManager
    
    // Permission request code
    private val PERMISSION_REQUEST_CODE = 1001
    
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.d("MainActivity", "onCreate: Starting MainActivity")
            super.onCreate(savedInstanceState)
            Log.d("MainActivity", "onCreate: After super.onCreate")
            
            // Check if user is logged in
            prefsManager = SharedPreferencesManager.getInstance(this)
            Log.d("MainActivity", "prefsManager initialized")
            
            if (!prefsManager.isUserLoggedIn()) {
                Log.d("MainActivity", "User not logged in, redirecting to LoginActivity")
                // Redirect to login screen
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
                return
            }
            
            Log.d("MainActivity", "Setting content view")
            setContentView(R.layout.activity_main)
        
            Log.d("MainActivity", "Initializing bottom navigation")
            bottomNavigation = findViewById(R.id.bottom_navigation)
            setupBottomNavigation()
            
            Log.d("MainActivity", "Setting up window insets")
            setupWindowInsets()
            
            // Load default fragment (Home)
            try {
                if (savedInstanceState == null) {
                    Log.d("MainActivity", "Loading HomeFragment")
                    loadFragment(HomeFragment())
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error loading fragment", e)
                // Fallback to a simple view if fragment loading fails
                setContentView(android.R.layout.simple_list_item_1)
                throw e
            }
            
            Log.d("MainActivity", "Checking and requesting permissions")
            // Check and request notification permissions on app start
            checkAndRequestNotificationPermission()
            
            // Check and request exact alarm permissions on app start
            checkAndRequestExactAlarmPermission()
            
            // Handle intent extras
            handleIntent(intent)
            Log.d("MainActivity", "onCreate completed successfully")
            
        } catch (e: Exception) {
            Log.e("MainActivity", "Critical error in onCreate", e)
            // Show error to user
            runOnUiThread {
                android.widget.Toast.makeText(this, "An error occurred: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
            throw e
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    override fun onResume() {
        super.onResume()
    }
    
    override fun onPause() {
        super.onPause()
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_habits -> HabitsFragment()
                R.id.nav_mood -> MoodFragment()
                R.id.nav_hydration -> HydrationFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }
        
        // Set the default selected item
        bottomNavigation.selectedItemId = R.id.nav_home
    }
    
    // Public method to navigate to specific tab from fragments
    fun navigateToTab(position: Int) {
        val itemId = when (position) {
            0 -> R.id.nav_home
            1 -> R.id.nav_habits
            2 -> R.id.nav_mood
            3 -> R.id.nav_hydration
            4 -> R.id.nav_settings
            else -> R.id.nav_home
        }
        bottomNavigation.selectedItemId = itemId
    }
    
    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            insets
        }
    }
    
    private fun loadFragment(fragment: Fragment) {
        try {
            Log.d("MainActivity", "Starting fragment transaction for ${fragment::class.simpleName}")
            val container = findViewById<androidx.fragment.app.FragmentContainerView>(R.id.nav_host_fragment)
            if (container == null) {
                Log.e("MainActivity", "Fragment container not found!")
                throw IllegalStateException("Fragment container not found in layout")
            }
            
            // Clear the back stack to prevent navigation issues
            val manager = supportFragmentManager
            if (manager.backStackEntryCount > 0) {
                val first = manager.getBackStackEntryAt(0)
                manager.popBackStack(first.id, android.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
            
            Log.d("MainActivity", "Found fragment container, proceeding with transaction")
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment, fragment::class.simpleName)
                .setReorderingAllowed(true)
                .commit()
                
            Log.d("MainActivity", "Fragment transaction committed for ${fragment::class.simpleName}")
            
        } catch (e: Exception) {
            val errorMsg = "Failed to load fragment: ${fragment::class.simpleName}"
            Log.e("MainActivity", errorMsg, e)
            
            // Show error to user
            runOnUiThread {
                android.widget.Toast.makeText(
                    this, 
                    "Error loading content. Please restart the app.", 
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            
            // Re-throw to ensure we see the full stack trace
            throw RuntimeException(errorMsg, e)
        }
    }
    
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Show explanation dialog before requesting permission
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Notification Permission Required")
                    .setMessage("WellnesMate needs notification permission to send you hydration reminders. Please grant this permission to receive timely reminders.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                            PERMISSION_REQUEST_CODE
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
    
    private fun checkAndRequestExactAlarmPermission() {
        // Only check on Android 12+ (API 31)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!HydrationAlarmScheduler.canScheduleExactAlarms(this)) {
                // Show explanation dialog before requesting permission
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Exact Alarm Permission Required")
                    .setMessage("WellnesMate needs permission to schedule exact alarms for precise hydration reminders. Please grant this permission to receive timely reminders.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        HydrationAlarmScheduler.requestExactAlarmPermission(this)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    android.widget.Toast.makeText(
                        this,
                        "Notification permission granted. You'll now receive hydration reminders!",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Show explanation why permission is needed
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("Without notification permission, you won't receive hydration reminders. You can enable this permission later in Settings > Apps > WellnesMate > Permissions.")
                        .setPositiveButton("Open Settings") { _, _ ->
                            val intent = android.content.Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            )
                            val uri = android.net.Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        }
    }
    
    // Method to update the toolbar title from fragments (no longer needed)
    /*
    fun updateToolbarTitle(title: String) {
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)?.title = title
    }
    */
    
    private fun handleIntent(intent: Intent) {
        when {
            intent.getBooleanExtra("open_hydration", false) -> {
                bottomNavigation.selectedItemId = R.id.nav_hydration
                loadFragment(HydrationFragment())
                
                // Handle quick add water from notification
                if (intent.getBooleanExtra("quick_add_water", false)) {
                    // Add default amount of water (250ml)
                    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val intake = com.example.wellnesmate.data.models.HydrationIntake(
                        date = today,
                        amountMl = 250,
                        timestamp = java.util.Date()
                    )
                    prefsManager.addHydrationIntake(intake)
                    
                    android.widget.Toast.makeText(
                        this,
                        "Added 250ml of water",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }
            else -> {
                // Default to home fragment
                loadFragment(HomeFragment())
            }
        }
    }
}