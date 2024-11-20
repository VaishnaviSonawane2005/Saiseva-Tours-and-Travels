package com.example.saisevatourstravels

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BookingDetails : AppCompatActivity() {

    private lateinit var bookingDetailsLayout: LinearLayout
    private lateinit var noBookingsMessage: TextView

    // Firebase
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val bookingsRef: DatabaseReference = database.getReference("CarBookings")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking_details)

        // Initialize UI Elements
        bookingDetailsLayout = findViewById(R.id.bookingDetailsLayout)
        noBookingsMessage = findViewById(R.id.noBookingsMessage)

        // Fetch user bookings
        fetchUserBookings()
    }

    private fun fetchUserBookings() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            bookingsRef.orderByKey().addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        noBookingsMessage.visibility = View.GONE
                        bookingDetailsLayout.removeAllViews() // Clear existing views

                        for (bookingSnapshot in snapshot.children) {
                            val booking = bookingSnapshot.getValue(CarBookings::class.java)
                            if (booking != null) {
                                displayBookingDetails(booking)
                            }
                        }
                    } else {
                        noBookingsMessage.visibility = View.VISIBLE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@BookingDetails,
                        "Failed to load bookings: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } else {
            Toast.makeText(this, "Please log in to view your bookings.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayBookingDetails(booking: CarBookings) {
        // Create a CardView for each booking
        val cardView = CardView(this).apply {
            radius = 16f
            setCardBackgroundColor(resources.getColor(R.color.background_light, theme))
            cardElevation = 8f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
        }

        // Create a TableLayout for the booking details
        val tableLayout = TableLayout(this).apply {
            setPadding(16, 16, 16, 16)
        }

        // Function to create a TableRow with two TextViews
        fun createTableRow(label: String, value: String): TableRow {
            return TableRow(this).apply {
                val labelText = TextView(this@BookingDetails).apply {
                    text = "$label:"
                    textSize = 16f
                    setPadding(8, 8, 8, 8)
                    setTextColor(resources.getColor(R.color.black, theme))
                }

                val valueText = TextView(this@BookingDetails).apply {
                    text = value
                    textSize = 16f
                    setPadding(8, 8, 8, 8)
                    setTextColor(resources.getColor(R.color.black, theme))
                }

                addView(labelText)
                addView(valueText)

                // Add divider (line) after each row except the last one
                if (this@BookingDetails.bookingDetailsLayout.indexOfChild(this) < tableLayout.childCount - 1) {
                    val divider = View(this@BookingDetails).apply {
                        setLayoutParams(TableRow.LayoutParams(
                            TableRow.LayoutParams.MATCH_PARENT, 1
                        ))
                        setBackgroundColor(resources.getColor(R.color.primaryLightColor, theme))
                    }
                    addView(divider)
                }
            }
        }

        // Add rows to TableLayout
        tableLayout.addView(createTableRow("Cab Type", booking.cabType ?: "N/A"))
        tableLayout.addView(createTableRow("Pickup Location", booking.pickupLocation ?: "N/A"))
        tableLayout.addView(createTableRow("Drop Location", booking.dropLocation ?: "N/A"))
        tableLayout.addView(createTableRow("Start Date & Time", booking.startDateTime ?: "N/A"))
        tableLayout.addView(createTableRow("End Date & Time", booking.endDateTime ?: "N/A"))
        tableLayout.addView(createTableRow("Trip Type", booking.tripType ?: "N/A"))

        // Add TableLayout to CardView
        cardView.addView(tableLayout)
        bookingDetailsLayout.addView(cardView)
    }



    private fun addTableRow(tableLayout: TableLayout, label: String, value: String) {
        val tableRow = TableRow(this).apply {
            setPadding(0, 10, 0, 10)
        }

        val labelTextView = TextView(this).apply {
            text = label
            setTextColor(resources.getColor(R.color.black, theme))
            textSize = 16f
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)
        }

        val valueTextView = TextView(this).apply {
            text = value
            setTextColor(resources.getColor(R.color.black, theme))
            textSize = 16f
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f)
        }

        tableRow.addView(labelTextView)
        tableRow.addView(valueTextView)
        tableLayout.addView(tableRow)
    }

    data class CarBookings(
        val cabType: String? = null,
        val pickupLocation: String? = null,
        val dropLocation: String? = null,
        val startDateTime: String? = null,
        val endDateTime: String? = null,
        val tripType: String? = null
    )
}
