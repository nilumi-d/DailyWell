package com.example.wellnesmate.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wellnesmate.MainActivity
import com.example.wellnesmate.R
import com.example.wellnesmate.data.repository.SharedPreferencesManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {
    
    private lateinit var tilEmail: TextInputLayout
    private lateinit var etEmail: TextInputEditText
    private lateinit var tilPassword: TextInputLayout
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var prefsManager: SharedPreferencesManager  // Add this line
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Initialize prefsManager
        prefsManager = SharedPreferencesManager.getInstance(this)
        
        initializeViews()
        setupClickListeners()
    }
    
    private fun initializeViews() {
        tilEmail = findViewById(R.id.til_email)
        etEmail = findViewById(R.id.et_email)
        tilPassword = findViewById(R.id.til_password)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        tvRegister = findViewById(R.id.tv_register)
    }
    
    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            login()
        }
        
        tvRegister.setOnClickListener {
            // Navigate to RegisterActivity
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    
    private fun login() {
        try {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            
            // Validate inputs
            if (!validateInputs(email, password)) {
                return
            }

            if (!prefsManager.isUserRegistered()) {
                Toast.makeText(this, "No account found. Please register first.", Toast.LENGTH_SHORT).show()
                return
            }

            val ok = prefsManager.validateLogin(email, password)
            if (!ok) {
                tilEmail.error = "Invalid email or password"
                tilPassword.error = "Invalid email or password"
                return
            }

            // Successful login
            Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
            prefsManager.setUserLoggedIn(true)
            
            // Start MainActivity in a new task and clear the back stack
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "An error occurred during login. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun validateInputs(email: String, password: String): Boolean {
        var isValid = true
        
        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Please enter a valid email"
            isValid = false
        } else {
            tilEmail.error = null
        }
        
        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        } else {
            tilPassword.error = null
        }
        
        return isValid
    }
    
    // Removed simulateLogin; now validating against stored credentials
}