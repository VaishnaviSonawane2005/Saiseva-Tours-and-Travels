package com.example.saisevatourstravels

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class CabActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var pickupLocationEditText: EditText
    private lateinit var dropLocationEditText: EditText
    private lateinit var startDateTimeTextView: TextView
    private lateinit var saveBookingButton: Button
    private lateinit var viewBookingsButton: Button

    private var startDateTime: Calendar = Calendar.getInstance()
    private lateinit var database: DatabaseReference

    companion object {
        const val AUTOCOMPLETE_REQUEST_CODE = 100
        const val LOCATION_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cab)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        database = FirebaseDatabase.getInstance().getReference("bookings")

        // Initialize UI components
        pickupLocationEditText = findViewById(R.id.pickupLocation)
        dropLocationEditText = findViewById(R.id.dropLocation)
        startDateTimeTextView = findViewById(R.id.rentalStartDateTime)
        saveBookingButton = findViewById(R.id.saveButton)
        viewBookingsButton = findViewById(R.id.viewBookingsButton)

        // Location autocomplete for pickup location
        pickupLocationEditText.setOnClickListener { launchAutocomplete(pickupLocationEditText) }

        // Manual input for drop location
        dropLocationEditText.hint = "Enter drop location manually"

        // DateTime Selector
        startDateTimeTextView.setOnClickListener { selectDateTime(startDateTime, startDateTimeTextView) }

        // Save booking
        saveBookingButton.setOnClickListener { saveBookingToDatabase() }

        // View bookings
        viewBookingsButton.setOnClickListener { fetchAndDisplayBookings() }

        // Handle location permissions
        checkAndRequestLocationPermissions()
    }

    private fun checkAndRequestLocationPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val geocoder = Geocoder(this@CabActivity, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val address = addresses?.firstOrNull()?.getAddressLine(0)
                    pickupLocationEditText.setText(address ?: "Location not found")
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun launchAutocomplete(editText: EditText) {
        // Autocomplete is removed for drop location
        val fields = listOf(com.google.android.libraries.places.api.model.Place.Field.ADDRESS)
        val intent = com.google.android.libraries.places.widget.Autocomplete.IntentBuilder(
            com.google.android.libraries.places.widget.model.AutocompleteActivityMode.FULLSCREEN,
            fields
        ).build(this)
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE && resultCode == RESULT_OK) {
            val place = com.google.android.libraries.places.widget.Autocomplete.getPlaceFromIntent(data!!)
            pickupLocationEditText.setText(place.address)
        }
    }

    private fun selectDateTime(calendar: Calendar, display: TextView) {
        val currentDate = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            TimePickerDialog(this, { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)

                if (calendar.timeInMillis < System.currentTimeMillis()) {
                    Toast.makeText(this, "Date and Time cannot be in the past", Toast.LENGTH_SHORT).show()
                } else {
                    display.text = SimpleDateFormat("dd MMM, yyyy hh:mm a", Locale.getDefault()).format(calendar.time)
                }
            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false).show()
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun saveBookingToDatabase() {
        val pickup = pickupLocationEditText.text.toString().trim()
        val drop = dropLocationEditText.text.toString().trim()
        val startDate = startDateTimeTextView.text.toString().trim()

        if (pickup.isEmpty() || drop.isEmpty() || startDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val bookingRef = database.push()
        val booking = mapOf(
            "pickup" to pickup,
            "drop" to drop,
            "startDate" to startDate
        )

        bookingRef.setValue(booking).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Booking saved successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save booking", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchAndDisplayBookings() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val bookings = mutableListOf<String>()
                for (data in snapshot.children) {
                    val pickup = data.child("pickup").getValue(String::class.java) ?: "Unknown Pickup"
                    val drop = data.child("drop").getValue(String::class.java) ?: "Unknown Drop"
                    val startDate = data.child("startDate").getValue(String::class.java) ?: "Unknown Date/Time"
                    bookings.add("Pickup: $pickup\nDrop: $drop\nStart: $startDate\n\n")
                }

                if (bookings.isEmpty()) {
                    Toast.makeText(this@CabActivity, "No bookings found", Toast.LENGTH_SHORT).show()
                } else {
                    val builder = AlertDialog.Builder(this@CabActivity)
                    builder.setTitle("Bookings")
                    builder.setItems(bookings.toTypedArray(), null)
                    builder.setPositiveButton("OK", null)
                    builder.show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CabActivity, "Failed to fetch bookings", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
