package com.example.wellnesmate.onboards

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.wellnesmate.R
import com.example.wellnesmate.databinding.ActivityLaunchingBinding

class Launching : AppCompatActivity() {
    private lateinit var binding: ActivityLaunchingBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLaunchingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Start the animation
        startLogoAnimation()
        
        // Navigate to Onboard1 after 3 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, Onboard1::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 3000)
    }
    
    private fun startLogoAnimation() {
        val logo = binding.logo
        
        // Reset any previous animations
        logo.alpha = 0f
        logo.scaleX = 0.5f
        logo.scaleY = 0.5f
        
        // Create fade and scale animations
        val fadeIn = ObjectAnimator.ofFloat(logo, View.ALPHA, 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(logo, View.SCALE_X, 0.5f, 1f)
        val scaleY = ObjectAnimator.ofFloat(logo, View.SCALE_Y, 0.5f, 1f)
        
        // Set duration
        fadeIn.duration = 1000
        scaleX.duration = 1000
        scaleY.duration = 1000
        
        // Create an animator set to play animations together
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(fadeIn, scaleX, scaleY)
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        
        // Start the animation
        animatorSet.start()
    }
}