package com.example.saisevatourstravels

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognizerIntent
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class DashboardActivity : AppCompatActivity() {

    // UI Components
    private lateinit var profileIcon: ImageView
    private lateinit var notificationIcon: ImageView
    private lateinit var searchIcon: ImageView
    private lateinit var micIcon: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var enableLocationButton: Button
    private lateinit var bannerCarousel: ViewPager2
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var drawerProfileIcon: ImageView
    private lateinit var drawerUserName: TextView
    private lateinit var drawerUserEmail: TextView
    private lateinit var drawerUserContact: TextView

    // Firebase Components
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var currentUser: FirebaseUser

    // Location
    private lateinit var locationManager: LocationManager

    // Constants
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val PICK_IMAGE_REQUEST = 71
    private val VOICE_RECOGNITION_REQUEST_CODE = 100

    // Banner images
    private val bannerImages = listOf(
        R.drawable.banner1,
        R.drawable.banner2,
        R.drawable.banner3,
        R.drawable.banner4,
        R.drawable.banner5,
        R.drawable.banner6,
        R.drawable.banner7,
        R.drawable.banner8,
        R.drawable.banner9
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize UI components
        initializeUI()

        // Initialize Firebase
        initializeFirebase()

        // Load user profile
        loadUserProfile()

        // Setup UI interactions
        setupUIInteractions()

        // Setup carousel
        setupBannerCarousel()

        // Check and get location
        checkLocationPermission()
    }

    private fun initializeUI() {
        profileIcon = findViewById(R.id.profileIcon)
        notificationIcon = findViewById(R.id.notificationIcon)
        searchIcon = findViewById(R.id.searchIcon)
        micIcon = findViewById(R.id.micIcon)
        searchEditText = findViewById(R.id.searchEditText)
        enableLocationButton = findViewById(R.id.enableLocationButton)
        bannerCarousel = findViewById(R.id.bannerCarousel)
        bottomNav = findViewById(R.id.bottomNav)
        drawerLayout = findViewById(R.id.drawer_layout)
        drawerProfileIcon = findViewById(R.id.drawerProfilePic)
        drawerUserName = findViewById(R.id.drawerUserName)
        drawerUserEmail = findViewById(R.id.drawerUserEmail)
        drawerUserContact = findViewById(R.id.drawerUserContact)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    private fun initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance()
        currentUser = firebaseAuth.currentUser ?: return
        databaseReference = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)
    }

    private fun loadUserProfile() {
        databaseReference.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val userName = snapshot.child("name").value.toString()
                val userEmail = snapshot.child("email").value.toString()
                val userContact = snapshot.child("contact").value.toString()

                drawerUserName.text = userName
                drawerUserEmail.text = userEmail
                drawerUserContact.text = userContact

            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load profile data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUIInteractions() {
        profileIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
            loadUserProfile() // Reload profile data when the drawer is opened
        }

        notificationIcon.setOnClickListener {
            Toast.makeText(this, "Notification Icon Clicked", Toast.LENGTH_SHORT).show()
        }

        searchIcon.setOnClickListener {
            val query = searchEditText.text.toString()
            if (query.isNotBlank()) {
                Toast.makeText(this, "Searching for: $query", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show()
            }
        }

        micIcon.setOnClickListener {
            startVoiceRecognition()
        }

        enableLocationButton.setOnClickListener {
            if (isLocationPermissionGranted()) {
                enableLocationServices()
            } else {
                requestLocationPermission()
            }
        }

        drawerProfileIcon.setOnClickListener {
            openImagePicker()
        }
    }

    private fun setupBannerCarousel() {
        val bannerAdapter = BannerAdapter(this, bannerImages)
        bannerCarousel.adapter = bannerAdapter
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE)
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun updateProfileImage(imageUri: Uri) {
        Picasso.get().load(imageUri).placeholder(R.drawable.profile)
            .error(R.drawable.profileblack).into(drawerProfileIcon)

        val storageRef = FirebaseStorage.getInstance().getReference("profile_pics/${currentUser.uid}")
        storageRef.putFile(imageUri).addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                databaseReference.child("profilePic").setValue(uri.toString())
                Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to upload profile image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    private fun enableLocationServices() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            try {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (location != null) {
                    Toast.makeText(this, "Location: ${location.latitude}, ${location.longitude}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: SecurityException) {
                Toast.makeText(this, "Location access denied", Toast.LENGTH_SHORT).show()
            }
        } else {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        }
    }

    private fun checkLocationPermission() {
        if (isLocationPermissionGranted()) {
            enableLocationServices()
        } else {
            requestLocationPermission()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            searchEditText.setText(results?.get(0) ?: "")
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            imageUri?.let {
                updateProfileImage(it)
            }
        }
    }
}
