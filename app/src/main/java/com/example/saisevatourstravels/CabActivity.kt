package com.example.saisevatourstravels

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class CabActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    // UI Elements
    private lateinit var pickupLocationEditText: EditText
    private lateinit var dropLocationEditText: EditText
    private lateinit var dateTimeTextView: TextView
    private lateinit var oneWayRadioButton: RadioButton
    private lateinit var roundTripRadioButton: RadioButton
    private lateinit var cabTypeHeaderTextView: TextView
    private lateinit var outstationCabsTab: TextView
    private lateinit var airportCabsTab: TextView
    private lateinit var hourlyRentalsTab: TextView
    private lateinit var fetchLocationButton: Button
    private lateinit var startDateTimeTextView: TextView
    private lateinit var endDateTimeTextView: TextView
    private lateinit var saveBookingButton: Button

    // Selected Options
    private var selectedCabType: String = "Outstation Cabs" // Default cab type
    private var selectedTripType: String = "One Way" // Default trip type
    private var startDateTime: Calendar = Calendar.getInstance()
    private var endDateTime: Calendar = Calendar.getInstance()

    companion object {
        const val AUTOCOMPLETE_REQUEST_CODE = 100
        const val LOCATION_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cab)

        // Initialize Google Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "YOUR_API_KEY")
        }

        // Initialize Fused Location Provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize UI Elements
        pickupLocationEditText = findViewById(R.id.pickupLocation)
        dropLocationEditText = findViewById(R.id.dropLocation)
        dateTimeTextView = findViewById(R.id.dateTimeDisplay)
        oneWayRadioButton = findViewById(R.id.oneWayRadioButton)
        roundTripRadioButton = findViewById(R.id.roundTripRadioButton)
        cabTypeHeaderTextView = findViewById(R.id.cabHeaderText)
        outstationCabsTab = findViewById(R.id.outstationCabs)
        airportCabsTab = findViewById(R.id.airportCabs)
        hourlyRentalsTab = findViewById(R.id.hourlyRentals)
        fetchLocationButton = findViewById(R.id.fetchLocationButton)
        startDateTimeTextView = findViewById(R.id.rentalStartDateTime)
        endDateTimeTextView = findViewById(R.id.rentalEndDateTime)
        saveBookingButton = findViewById(R.id.saveButton)

        // Set dynamic date and time
        dateTimeTextView.text = getCurrentDateTime()

        // Configure location updates
        createLocationRequest()
        setupLocationCallback()

        // Add listeners for location input
        pickupLocationEditText.setOnClickListener { launchAutocomplete(pickupLocationEditText) }
        dropLocationEditText.setOnClickListener { launchAutocomplete(dropLocationEditText) }

        // Search button functionality
        fetchLocationButton.setOnClickListener {
            // Ensure location permission is granted
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        // Use Geocoder to convert latitude and longitude to an address
                        val geocoder = Geocoder(this)
                        val addresses: MutableList<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (addresses != null) {
                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                // Set the pickup location field with the current location
                                pickupLocationEditText.setText(address.getAddressLine(0))
                            }
                        }
                    } else {
                        Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Request location permissions if not granted
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
        }

        // Handle trip type selection
        val oneWayRadioButton: RadioButton = findViewById(R.id.oneWayRadioButton)
        val roundTripRadioButton: RadioButton = findViewById(R.id.roundTripRadioButton)

        oneWayRadioButton.setOnClickListener {
            oneWayRadioButton.isChecked = true
            roundTripRadioButton.isChecked = false
        }

        roundTripRadioButton.setOnClickListener {
            roundTripRadioButton.isChecked = true
            oneWayRadioButton.isChecked = false
        }



        // Tab click listeners
        setupTabListeners()

        // Date and Time Selectors
        startDateTimeTextView.setOnClickListener { selectDateTime(startDateTime, startDateTimeTextView) }
        endDateTimeTextView.setOnClickListener { selectDateTime(endDateTime, endDateTimeTextView) }

        // Save booking
        saveBookingButton.setOnClickListener { saveBookingToDatabase() }
    }

    private fun selectDateTime(calendar: Calendar, display: TextView) {
        val currentDate = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                display.text = SimpleDateFormat("dd MMM, yyyy hh:mm a", Locale.getDefault()).format(calendar.time)
            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show()
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveBookingToDatabase() {
        val pickup = pickupLocationEditText.text.toString().trim()
        val drop = dropLocationEditText.text.toString().trim()
        val startDate = startDateTimeTextView.text.toString().trim()
        val endDate = endDateTimeTextView.text.toString().trim()

        if (pickup.isEmpty() || drop.isEmpty() || startDate == "Select" || endDate == "Select") {
            Toast.makeText(this, "Please fill all fields before saving.", Toast.LENGTH_SHORT).show()
            return
        }

        val bookingDetails = mapOf(
            "pickupLocation" to pickup,
            "dropLocation" to drop,
            "startDateTime" to startDate,
            "endDateTime" to endDate,
            "cabType" to selectedCabType,
            "tripType" to selectedTripType
        )

        val databaseReference = FirebaseDatabase.getInstance().getReference("CarBookings")
        databaseReference.push().setValue(bookingDetails).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Booking saved successfully.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save booking. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                val location: Location? = locationResult.lastLocation
                if (location != null) {
                    updateTripType(location.toString())
                }
            }
        }
    }

    private fun getCurrentDateTime(): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM, yyyy hh:mm a", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun launchAutocomplete(locationEditText: EditText) {
        val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS)
        val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields).build(this)
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!hasLocationPermissions()) {
            requestLocationPermissions()
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun hasLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onResume() {
        super.onResume()
        startLocationUpdates() // Start location updates when the activity is in the foreground
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback) // Stop location updates when the activity is not visible
    }


    private fun updateTripType(type: String) {
        selectedTripType = type
    }

    private fun setupTabListeners() {
        outstationCabsTab.setOnClickListener {
            selectedCabType = "Outstation Cabs"
            updateTabColors()
        }
        airportCabsTab.setOnClickListener {
            selectedCabType = "Airport Cabs"
            updateTabColors()
        }
        hourlyRentalsTab.setOnClickListener {
            selectedCabType = "Hourly Rentals"
            updateTabColors()
        }
    }

    private fun updateTabColors() {
        outstationCabsTab.setBackgroundColor(if (selectedCabType == "Outstation Cabs") getColor(R.color.primaryLightColor) else getColor(R.color.text_primary_dark))
        airportCabsTab.setBackgroundColor(if (selectedCabType == "Airport Cabs") getColor(R.color.primaryLightColor) else getColor(R.color.text_primary_dark))
        hourlyRentalsTab.setBackgroundColor(if (selectedCabType == "Hourly Rentals") getColor(R.color.primaryLightColor) else getColor(R.color.text_primary_dark))
    }
}
