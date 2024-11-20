package com.example.saisevatourstravels

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.io.File
import java.util.*

class UpdateProfileActivity : AppCompatActivity() {

    private lateinit var profileImageView: ShapeableImageView
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var contactEditText: EditText
    private lateinit var saveButton: Button

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storageReference: StorageReference

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_profile)

        // Initialize UI components
        profileImageView = findViewById(R.id.drawerProfilePic)
        usernameEditText = findViewById(R.id.updateUsername)
        emailEditText = findViewById(R.id.updateEmail)
        contactEditText = findViewById(R.id.updateContact)
        saveButton = findViewById(R.id.saveButton)

        // Firebase setup
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storageReference = FirebaseStorage.getInstance().reference
        val currentUser = firebaseAuth.currentUser ?: return
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)

        // Load existing user details
        loadExistingDetails()

        // Set up profile image click listener
        profileImageView.setOnClickListener { showImagePickerDialog() }

        // Set up save button click listener
        saveButton.setOnClickListener { saveUpdatedDetails() }
    }

    private fun loadExistingDetails() {
        val currentUser = firebaseAuth.currentUser ?: return

        // Load from Realtime Database
        databaseReference.get().addOnSuccessListener { snapshot ->
            usernameEditText.setText(snapshot.child("username").value.toString())
            emailEditText.setText(snapshot.child("email").value.toString())
            contactEditText.setText(snapshot.child("contact").value.toString())
        }

        // Load profile picture from Firestore
        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                val profilePicUrl = document.getString("profilePic")
                if (!profilePicUrl.isNullOrEmpty()) {
                    Picasso.get().load(profilePicUrl).into(profileImageView)
                }
            }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(this)
            .setTitle("Choose an option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> openImagePicker()
                }
            }.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val bitmap = result.data?.extras?.get("data") as? android.graphics.Bitmap
                bitmap?.let {
                    imageUri = saveBitmapToUri(it)
                    profileImageView.setImageBitmap(it)
                }
            }
        }

    private fun saveBitmapToUri(bitmap: android.graphics.Bitmap): Uri {
        val file = File(cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
        file.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, it)
        }
        return Uri.fromFile(file)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imageUri = result.data?.data
                profileImageView.setImageURI(imageUri)
            }
        }

    private fun saveUpdatedDetails() {
        val currentUser = firebaseAuth.currentUser ?: return

        // Update Realtime Database for username, email, and contact
        val updates = mapOf(
            "username" to usernameEditText.text.toString(),
            "email" to emailEditText.text.toString(),
            "contact" to contactEditText.text.toString()
        )
        databaseReference.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(this, "Details updated in Realtime Database", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to update details in Realtime Database", Toast.LENGTH_SHORT).show()
        }

        // Update Firestore for profile picture
        if (imageUri != null) {
            val fileName = "profile_images/${UUID.randomUUID()}.jpg"
            val fileReference = storageReference.child(fileName)

            fileReference.putFile(imageUri!!).addOnSuccessListener {
                fileReference.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    firestore.collection("users").document(currentUser.uid)
                        .set(mapOf("profilePic" to imageUrl))
                        .addOnSuccessListener {
                            Toast.makeText(this, "Profile picture updated in Firestore", Toast.LENGTH_SHORT).show()
                            navigateToDrawerHeader()
                        }
                }
            }
        } else {
            navigateToDrawerHeader()
        }
    }

    private fun navigateToDrawerHeader() {
        val intent = Intent(this, DrawerHeader::class.java)
        startActivity(intent)
        finish()
    }
}
