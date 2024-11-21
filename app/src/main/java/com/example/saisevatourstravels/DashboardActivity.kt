package com.example.saisevatourstravels

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView

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
    private lateinit var locationManager: LocationManager
    private lateinit var carRentals: ImageView
    private lateinit var ourPackages: ImageView

    // Constants
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val VOICE_RECOGNITION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Initialize UI components
        initializeUI()

        // Setup UI interactions
        setupUIInteractions()

        // Setup carousel
        setupBannerCarousel()

        // Check and get location
        checkLocationPermission()

        // Setup Bottom Navigation
        setupBottomNavigation()
    }

    private fun initializeUI() {
        profileIcon = findViewById(R.id.profileIcon)
        notificationIcon = findViewById(R.id.notificationIcon)
        searchIcon = findViewById(R.id.searchIcon)
        micIcon = findViewById(R.id.micIcon)
        searchEditText = findViewById(R.id.searchEditText)
        enableLocationButton = findViewById(R.id.enableLocationButton)
        bannerCarousel = findViewById(R.id.viewPager)
        bottomNav = findViewById(R.id.bottomNav)
        carRentals = findViewById(R.id.carRentals) // Initialize carRentals TextView
        ourPackages = findViewById(R.id.ourPackages)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }

    private fun setupUIInteractions() {
        profileIcon.setOnClickListener {
            val intent = Intent(this, DrawerHeader::class.java)
            startActivity(intent)
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

        ourPackages.setOnClickListener {
            val intent = Intent(this, OurServices::class.java)
            startActivity(intent)
        }

        // Navigate to CabActivity when carRentals is clicked
        carRentals.setOnClickListener {
            val intent = Intent(this, CabActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBannerCarousel() {
        val bannerImages = listOf(
            R.drawable.banner1,
            R.drawable.banner2,
            R.drawable.banner3
        )
        val bannerAdapter = BannerAdapter(this, bannerImages)
        bannerCarousel.adapter = bannerAdapter

        val handler = android.os.Handler()
        val runnable = object : Runnable {
            var currentIndex = 0
            override fun run() {
                currentIndex = (currentIndex + 1) % bannerImages.size
                bannerCarousel.setCurrentItem(currentIndex, true)
                handler.postDelayed(this, 3000)
            }
        }
        handler.post(runnable)
    }

    private fun setupBottomNavigation() {
        bottomNav.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Stay on the Dashboard
                    Toast.makeText(this, "Home selected", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_activity -> {
                    // Navigate to BookingDetails
                    val intent = Intent(this, BookingDetails::class.java)
                    startActivity(intent)
                    true
                }
//                R.id.nav_services -> {
//                    // Navigate to Profile Page
//                    val intent = Intent(this, ProfileActivity::class.java)
//                    startActivity(intent)
//                    true
//                }
                else -> false
            }
        }
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE)
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
}
