package com.example.saisevatourstravels

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class BannerAdapter(private val context: Context, private val images: List<Int>) :
    RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    inner class BannerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.bannerImageView) // Ensure this matches your XML layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.activity_banner_adapter, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        // Set the image resource using the list of drawable resources
        holder.imageView.setImageResource(images[position])
    }

    override fun getItemCount(): Int = images.size
}
