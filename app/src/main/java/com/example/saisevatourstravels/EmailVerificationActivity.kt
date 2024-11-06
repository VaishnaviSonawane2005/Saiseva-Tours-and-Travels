package com.example.saisevatourstravels

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.database.FirebaseDatabase

class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var usernameField: EditText
    private lateinit var contactField: EditText
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var confirmPasswordField: EditText
    private lateinit var signUpButton: Button
    private lateinit var passwordEye: ImageView
    private lateinit var confirmPasswordEye: ImageView
    private lateinit var loginRedirect: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)

        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        usernameField = findViewById(R.id.username_input)
        contactField = findViewById(R.id.contact_input)
        emailField = findViewById(R.id.email_input)
        passwordField = findViewById(R.id.password_input)
        confirmPasswordField = findViewById(R.id.confirm_password_input)
        signUpButton = findViewById(R.id.sign_up_button)
        passwordEye = findViewById(R.id.password_eye)
        confirmPasswordEye = findViewById(R.id.confirm_password_eye)
        loginRedirect = findViewById(R.id.already_have_account)

        // Password visibility toggle
        passwordEye.setOnClickListener { togglePasswordVisibility(passwordField, passwordEye) }
        confirmPasswordEye.setOnClickListener { togglePasswordVisibility(confirmPasswordField, confirmPasswordEye) }

        // Redirect to LoginActivity
        loginRedirect.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Sign Up button functionality
        signUpButton.setOnClickListener {
            val username = usernameField.text.toString().trim()
            val contact = contactField.text.toString().trim()
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()
            val confirmPassword = confirmPasswordField.text.toString().trim()

            if (validateInputs(username, contact, email, password, confirmPassword)) {
                // Check if the email already exists in Firebase Authentication
                checkEmailExistsInAuth(email) { emailExists ->
                    if (emailExists) {
                        // If email exists in Authentication, show that it is already registered
                        Toast.makeText(this, "Email is already registered in Firebase", Toast.LENGTH_SHORT).show()
                    } else {
                        // Check if email already exists in Realtime Database
                        checkEmailExistsInDatabase(email) { emailExistsInDatabase ->
                            if (emailExistsInDatabase) {
                                // If email exists in Realtime Database, show that it's already taken
                                Toast.makeText(this, "Email is already registered", Toast.LENGTH_SHORT).show()
                            } else {
                                // If email doesn't exist, proceed to create account
                                createUserAccount(username, contact, email, password)
                            }
                        }
                    }
                }
            }
        }
    }

    // Validate input fields
    private fun validateInputs(username: String, contact: String, email: String, password: String, confirmPassword: String): Boolean {
        return when {
            username.isEmpty() || contact.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() -> {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                false
            }
            contact.length != 10 || !contact.matches("\\d+".toRegex()) -> {
                Toast.makeText(this, "Enter a valid 10-digit contact number", Toast.LENGTH_SHORT).show()
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(this, "Enter a valid email address", Toast.LENGTH_SHORT).show()
                false
            }
            password != confirmPassword -> {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                false
            }
            password.length < 6 -> {
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    // Check if email exists in Firebase Authentication
    private fun checkEmailExistsInAuth(email: String, callback: (Boolean) -> Unit) {
        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val emailExists = task.result?.signInMethods?.isNotEmpty() ?: false
                callback(emailExists)
            } else {
                Toast.makeText(this, "Error checking email in Firebase: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                callback(false)
            }
        }
    }

    // Check if email exists in Realtime Database
    private fun checkEmailExistsInDatabase(email: String, callback: (Boolean) -> Unit) {
        val database = FirebaseDatabase.getInstance("https://saiseva-tours-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val usersRef = database.getReference("users")
        usersRef.orderByChild("email").equalTo(email).get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val dataSnapshot = task.result
                callback(dataSnapshot?.exists() == true)
            } else {
                Log.e("FirebaseError", "Error checking email in Database", task.exception)

                callback(false)
            }
        }
    }

    // Create user account
    private fun createUserAccount(username: String, contact: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { signUpTask ->
            if (signUpTask.isSuccessful) {
                val user = User(username, contact, email, password)
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    saveUserData(userId, user)
                }
            } else {
                Toast.makeText(this, "Sign-up failed: ${signUpTask.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Save user data to Realtime Database
    private fun saveUserData(userId: String, user: User) {
        val database = FirebaseDatabase.getInstance()
        val usersRef = database.getReference("users")
        usersRef.child(userId).setValue(user).addOnCompleteListener { saveTask ->
            if (saveTask.isSuccessful) {
                Toast.makeText(this, "User registered successfully", Toast.LENGTH_SHORT).show()
                navigateToMainDashBoard(user)
            } else {
                Toast.makeText(this, "Failed to save user data: ${saveTask.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Navigate to MainDashBoard
    private fun navigateToMainDashBoard(user: User) {
        val intent = Intent(this, MainDashBoard::class.java)
        intent.putExtra("userName", user.username)
        intent.putExtra("userEmail", user.email)
        intent.putExtra("userContact", user.contact)
        startActivity(intent)
        finish()
    }

    // Toggle password visibility
    private fun togglePasswordVisibility(field: EditText, eye: ImageView) {
        if (field.inputType == InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT) {
            field.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            eye.setImageResource(R.drawable.open_eye)
        } else {
            field.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
            eye.setImageResource(R.drawable.close_eye)
        }
        field.setSelection(field.text.length)
    }
}
