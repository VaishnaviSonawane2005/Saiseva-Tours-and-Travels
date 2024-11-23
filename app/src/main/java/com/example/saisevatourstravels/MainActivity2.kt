package com.example.saisevatourstravels

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2) // Ensure this corresponds to the correct layout

        // Set up click listener for Map ImageView
        val mapImageView: ImageView = findViewById(R.id.map_img)
        mapImageView.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.app.goo.gl/wNTm8KygKfve73tx9"))
            startActivity(browserIntent)
        }

        // Set up click listener for Next ImageView
        val nextButton: ImageView = findViewById(R.id.next_button)
        nextButton.setOnClickListener {
            val intent = Intent(this, FourthActivity::class.java)
            startActivity(intent)
        }
    }
}
