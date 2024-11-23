package com.example.saisevatourstravels

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
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
    private lateinit var userDatabase: DatabaseReference

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cab)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        database = FirebaseDatabase.getInstance().getReference("bookings")
        userDatabase = FirebaseDatabase.getInstance().getReference("users")

        // Initialize UI components
        pickupLocationEditText = findViewById(R.id.pickupLocation)
        dropLocationEditText = findViewById(R.id.dropLocation)
        startDateTimeTextView = findViewById(R.id.rentalStartDateTime)
        saveBookingButton = findViewById(R.id.saveButton)
        viewBookingsButton = findViewById(R.id.viewBookingsButton)

        // DateTime Selector
        startDateTimeTextView.setOnClickListener { selectDateTime(startDateTime, startDateTimeTextView) }

        // Save booking
        saveBookingButton.setOnClickListener {
            val userId = getCurrentUserId()
            if (userId != null) {
                saveBookingToUserDatabase(userId)
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }

        // View bookings
        viewBookingsButton.setOnClickListener {
            val userId = getCurrentUserId()
            if (userId != null) {
                fetchBookingsForUser(userId)
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            }
        }

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

    private fun getCurrentUserId(): String? {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.uid
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
                    val address = getAddressFromLocation(geocoder, location.latitude, location.longitude)
                    pickupLocationEditText.setText(address ?: "Location not found")
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun getAddressFromLocation(geocoder: Geocoder, latitude: Double, longitude: Double): String? {
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        } catch (e: Exception) {
            e.printStackTrace()
            null
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

    private fun saveBookingToUserDatabase(userId: String) {
        val pickup = pickupLocationEditText.text.toString().trim()
        val drop = dropLocationEditText.text.toString().trim()
        val startDate = startDateTimeTextView.text.toString().trim()

        if (pickup.isEmpty() || drop.isEmpty() || startDate.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val newBooking = CarBooking(pickup = pickup, drop = drop, startDate = startDate)
        val bookingId = userDatabase.child(userId).child("bookings").push().key

        if (bookingId != null) {
            newBooking.bookingId = bookingId
            userDatabase.child(userId).child("bookings").child(bookingId).setValue(newBooking)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Booking saved successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to save booking", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            Toast.makeText(this, "Failed to generate booking ID", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchBookingsForUser(userId: String) {
        val userRef = userDatabase.child(userId).child("bookings")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val bookings = snapshot.children.mapNotNull { it.getValue(CarBooking::class.java) }
                    if (bookings.isNotEmpty()) {
                        val bookingDetails = bookings.map { booking ->
                            "Pickup: ${booking.pickup}\nDrop: ${booking.drop}\nStart: ${booking.startDate}\n\n"
                        }
                        AlertDialog.Builder(this@CabActivity)
                            .setTitle("Your Bookings")
                            .setItems(bookingDetails.toTypedArray(), null)
                            .setPositiveButton("OK", null)
                            .show()
                    } else {
                        Toast.makeText(this@CabActivity, "No bookings found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@CabActivity, "No bookings found for this user", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CabActivity, "Failed to fetch bookings", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
