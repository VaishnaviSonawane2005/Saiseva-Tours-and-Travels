package com.example.saisevatourstravels

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.picasso.Picasso

class DrawerHeader : AppCompatActivity() {

    private lateinit var hiddenHeader: LinearLayout
    private lateinit var drawerProfilePic: ShapeableImageView
    private lateinit var drawerUserName: TextView
    private lateinit var drawerUserEmail: TextView
    private lateinit var drawerUserContact: TextView
    private lateinit var updateProfileButton: Button
    private lateinit var logoutButton: Button
    private lateinit var deleteAccountButton: Button

    private lateinit var instagramIcon: ImageView
    private lateinit var facebookIcon: ImageView
    private lateinit var youtubeIcon: ImageView
    private lateinit var whatsappIcon: ImageView

    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private lateinit var databaseReference: DatabaseReference
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer_header)

        // Initialize Views
        hiddenHeader = findViewById(R.id.hiddenHeader)
        drawerProfilePic = findViewById(R.id.drawerProfilePic)
        drawerUserName = findViewById(R.id.drawerUserName)
        drawerUserEmail = findViewById(R.id.drawerUserEmail)
        drawerUserContact = findViewById(R.id.drawerUserContact)
        updateProfileButton = findViewById(R.id.updateProfileButton)
        logoutButton = findViewById(R.id.logoutButton)
        deleteAccountButton = findViewById(R.id.deleteAccountButton)

        instagramIcon = findViewById(R.id.instagramButton)
        facebookIcon = findViewById(R.id.facebookButton)
        youtubeIcon = findViewById(R.id.youtubeButton)
        whatsappIcon = findViewById(R.id.whatsappButton)

        // Firebase Reference
        databaseReference = FirebaseDatabase.getInstance().reference

        // Set up the UI
        setUpUserInfo()

        // Handling Update Profile Button
        updateProfileButton.setOnClickListener {
            // Start Update Profile Activity
            startActivity(Intent(this, UpdateProfileActivity::class.java))
        }

        // Handling Logout Button
        logoutButton.setOnClickListener {
            showLoadingDialog("Logging out...")
            firebaseAuth.signOut()
            dismissLoadingDialog()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            // Redirect to Login Activity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Handling Delete Account Button
        deleteAccountButton.setOnClickListener {
            showLoadingDialog("Deleting account...")
            val user = firebaseAuth.currentUser
            user?.delete()?.addOnCompleteListener { task ->
                dismissLoadingDialog()
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                    // Navigate to Registration Activity or Show a Confirmation
                    startActivity(Intent(this, EmailVerificationActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Failed to delete account. Try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Handling Social Media Icons
        instagramIcon.setOnClickListener {
            openSocialMediaLink("https://www.instagram.com/saiseva_toursandtravels_shirdi/profilecard/?igsh=N3psa3RiOXVxYmxm\n")
        }

        facebookIcon.setOnClickListener {
            openSocialMediaLink("https://www.facebook.com/profile.php?id=61568455325431\n")
        }

        youtubeIcon.setOnClickListener {
            openSocialMediaLink("https://www.youtube.com/channel/UCtsekJUL2KVEFbktywkIfGg\n")
        }

        whatsappIcon.setOnClickListener {
            openSocialMediaLink(" https://whatsapp.com/channel/0029VavbdLpD38CTrDmhjr1B\n")
        }
    }

    private fun setUpUserInfo() {
        // Check if user is logged in
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Load data from Realtime Database
            databaseReference.child("users").child(userId).get().addOnSuccessListener { snapshot ->
                val userName = snapshot.child("username").value.toString()
                val userContact = snapshot.child("contact").value.toString()
                val userEmail = currentUser.email.toString()

                // Set User Info in UI
                drawerUserName.text = "Hey, $userName"
                drawerUserEmail.text = userEmail
                drawerUserContact.text = userContact

                // Load Profile Image from Firestore
                firestore.collection("users").document(userId).get().addOnSuccessListener { doc ->
                    val profilePicUrl = doc.getString("profilePic") ?: ""
                    if (profilePicUrl.isNotEmpty()) {
                        Picasso.get().load(profilePicUrl).into(drawerProfilePic)
                    } else {
                        drawerProfilePic.setImageResource(R.drawable.profileblack) // Default Image
                    }
                }
            }

            // Show Hidden Header
            hiddenHeader.visibility = View.VISIBLE
        } else {
            // User not logged in, hide hidden header
            hiddenHeader.visibility = View.GONE
        }
    }

    private fun openSocialMediaLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Reload user info when returning from UpdateProfileActivity
        setUpUserInfo()
    }

    private var loadingDialog: AlertDialog? = null

    private fun showLoadingDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setView(R.layout.progress_dialog)
        builder.setCancelable(false)
        loadingDialog = builder.create()
        loadingDialog?.show()
    }

    private fun dismissLoadingDialog() {
        loadingDialog?.dismiss()
    }
}
