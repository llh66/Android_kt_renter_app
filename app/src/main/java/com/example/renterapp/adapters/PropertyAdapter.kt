package com.example.renterapp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.renterapp.R
import com.example.renterapp.databinding.RowPropertyBinding
import com.example.renterapp.interfaces.ClickDetectorInterface
import com.example.renterapp.models.Property

class PropertyAdapter(val properties: MutableList<Property>, val clickInterface: ClickDetectorInterface): RecyclerView.Adapter<PropertyAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: RowPropertyBinding): RecyclerView.ViewHolder(binding.root) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowPropertyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return properties.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val property = this.properties[position]

        holder.binding.apply {
            tvPrice.text = "$${property.price} / month"
            tvAddress.text = property.address
            tvBedrooms.text = "${property.bedrooms} bedroom" +
                    "${if (property.bedrooms > 1) "s" else "" }"
            btnRemove.setOnClickListener { clickInterface.removeRow(position) }
        }

        Glide
            .with(holder.itemView.context)
            .load(property.imageUrl)
            .placeholder(R.drawable.baseline_insert_photo_24)
            .into(holder.binding.imgPropertyImage)
    }

}