package com.example.saisevatourstravels

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BannerViewPager(private val context: Context, private val recyclerView: RecyclerView) {
    private val handler = Handler(Looper.getMainLooper())

    // Start auto-scrolling for the banners
    fun startAutoScroll(images: List<Int>, interval: Long = 3000) {
        // Initialize the BannerAdapter with the list of images
        val adapter = BannerAdapter(context, images)

        // Set up the RecyclerView with a horizontal LinearLayoutManager
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter

        // Runnable to auto-scroll at regular intervals
        val autoScrollRunnable = object : Runnable {
            override fun run() {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val currentPosition = layoutManager.findFirstVisibleItemPosition()
                val nextPosition = (currentPosition + 1) % images.size // Loop to the first image after the last one
                recyclerView.smoothScrollToPosition(nextPosition) // Smooth scroll to the next image
                handler.postDelayed(this, interval) // Repeat this task after the interval
            }
        }

        // Start auto-scrolling with the specified interval
        handler.postDelayed(autoScrollRunnable, interval)
    }

    // Stop the auto-scrolling functionality
    fun stopAutoScroll() {
        handler.removeCallbacksAndMessages(null) // Remove all pending tasks for auto-scrolling
    }
}
