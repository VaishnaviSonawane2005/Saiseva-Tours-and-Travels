package com.example.saisevatourstravels

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainDashBoard : AppCompatActivity() {

    private lateinit var continueButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_dash_board)

        // Initialize Continue button
        continueButton = findViewById(R.id.buttonContinue)

        // Handle Continue button click
        continueButton.setOnClickListener {
            // Navigate to DashboardActivity and pass the user data
            navigateToDashboard()
        }
    }

    // Navigate to DashboardActivity
    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        // Pass the user data to the DashboardActivity

        startActivity(intent)
        finish() // Optional to prevent returning to MainDashBoard
    }
}
