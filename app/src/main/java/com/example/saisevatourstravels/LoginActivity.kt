package com.example.saisevatourstravels

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.android.material.snackbar.Snackbar

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var passwordToggle: ImageView
    private lateinit var loginButton: Button
    private lateinit var forgotPassword: TextView
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        emailField = findViewById(R.id.username)
        passwordField = findViewById(R.id.password)
        passwordToggle = findViewById(R.id.password_toggle)
        loginButton = findViewById(R.id.loginbtn)
        forgotPassword = findViewById(R.id.forgotpass)

        // Toggle password visibility
        passwordToggle.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                passwordField.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                passwordToggle.setImageResource(R.drawable.open_eye)
            } else {
                passwordField.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                passwordToggle.setImageResource(R.drawable.close_eye)
            }
            passwordField.setSelection(passwordField.text.length)
        }

        // Login button functionality
        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(findViewById(android.R.id.content), "Please enter email and password", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Login successful
                        Snackbar.make(findViewById(android.R.id.content), "Login successful", Snackbar.LENGTH_SHORT).show()

                        // Navigate to the main dashboard
                        navigateToMainDashboard()
                    } else {
                        // Login failed
                        val errorMessage = task.exception?.message ?: "Login failed. Please check your email and password."
                        Snackbar.make(findViewById(android.R.id.content), errorMessage, Snackbar.LENGTH_SHORT).show()
                    }
                }
        }

        // Forgot Password functionality
        forgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Forgot Password")
        val input = EditText(this)
        input.hint = "Enter your email address"
        builder.setView(input)

        builder.setPositiveButton("Send Reset Email") { _, _ ->
            val email = input.text.toString().trim()
            if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Snackbar.make(findViewById(android.R.id.content), "Please enter a valid email address", Snackbar.LENGTH_SHORT).show()
            } else {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Snackbar.make(findViewById(android.R.id.content), "Password reset email sent!", Snackbar.LENGTH_SHORT).show()
                            updatePasswordInDatabase(email)
                        } else {
                            Snackbar.make(findViewById(android.R.id.content), "Failed to send reset email: ${task.exception?.message}", Snackbar.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun updatePasswordInDatabase(email: String) {
        // Query the database to find the user by email
        val databaseRef = FirebaseDatabase.getInstance().getReference("users")
        databaseRef.orderByChild("email").equalTo(email)
            .addListenerForSingleValueEvent(object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val userId = userSnapshot.key ?: continue
                            val updates = mapOf("password" to "Reset Through Email") // Placeholder for password reset
                            databaseRef.child(userId).updateChildren(updates)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        Snackbar.make(
                                            findViewById(android.R.id.content),
                                            "Password updated in database!",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Snackbar.make(
                                            findViewById(android.R.id.content),
                                            "Failed to update password in database: ${task.exception?.message}",
                                            Snackbar.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        }
                    } else {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "Email not found in database.",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Database error: ${error.message}",
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun navigateToMainDashboard() {
        Log.d("LoginActivity", "Navigating to Main Dashboard")
        val intent = Intent(this@LoginActivity, MainDashBoard::class.java)
        startActivity(intent)
        finish() // Close the login activity so the user cannot navigate back to it
    }
}
