package com.example.saisevatourstravels

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val splashTimeout: Long = 5000 // 8 seconds timeout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Force light mode regardless of system settings
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Initialize ImageView and TextView
        val splashImage: ImageView = findViewById(R.id.splashImage)
        val splashText: TextView = findViewById(R.id.splashText)

        // Load animations
        val fadeInLogo = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val bounceInText = AnimationUtils.loadAnimation(this, R.anim.bounce_in)

        // Start logo animation
        splashImage.visibility = View.VISIBLE
        splashImage.startAnimation(fadeInLogo)

        // Set listener to start text animation after logo animation
        fadeInLogo.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                splashText.visibility = View.VISIBLE
                splashText.startAnimation(bounceInText)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        // Transition to the next screen after splashTimeout
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, splashTimeout)
    }

    private fun navigateToNextScreen() {
        // Check if user is logged in and navigate accordingly
        val intent = if (auth.currentUser != null) {
            // Navigate to MainDashboard if logged in
            Intent(this@MainActivity, MainDashBoard::class.java)
        } else {
            // Navigate to EmailVerificationActivity if not logged in
            Intent(this@MainActivity, EmailVerificationActivity::class.java)
        }
        startActivity(intent)
        finish() // Close MainActivity after navigation
    }
}
