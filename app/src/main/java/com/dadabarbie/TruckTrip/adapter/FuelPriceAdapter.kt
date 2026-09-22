package com.dadabarbie.TruckTrip.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.databinding.ItemFuelPriceCityBinding
import com.dadabarbie.TruckTrip.model.fuel.FuelCityRecord

class FuelPriceAdapter() :
    ListAdapter<FuelCityRecord, FuelPriceAdapter.ViewHolder>(DiffCallback()) {

    class DiffCallback : DiffUtil.ItemCallback<FuelCityRecord>() {
        override fun areItemsTheSame(oldItem: FuelCityRecord, newItem: FuelCityRecord): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FuelCityRecord, newItem: FuelCityRecord): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFuelPriceCityBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    class ViewHolder(
        private val binding: ItemFuelPriceCityBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FuelCityRecord) {
            binding.tvCityName.text = "${item.city}, ${item.state}"
            binding.tvPetrolPrice.text = String.format("₹%.2f", item.petrol)
            binding.tvDieselPrice.text = String.format("₹%.2f", item.diesel)
            binding.tvLpgPrice.text = String.format("₹%.0f", item.lpg)
            binding.tvUpdatedAt.text = item.updatedAt

            val nearbyCount = item.nearbyCities.size
            binding.tvNearbyHeader.text =
                binding.root.context.getString(R.string.near_by_cities)+" : "+ nearbyCount

            binding.nearbyContainer.removeAllViews()
            binding.nearbyContainer.visibility = View.GONE
            binding.ivToggle.rotation = 0f

            binding.ivToggle.setOnClickListener {
                toggleNearby(item)
            }
            binding.nearbyHeaderLayout.setOnClickListener {
                toggleNearby(item)
            }
        }

        private fun toggleNearby(item: FuelCityRecord) {
            if (binding.nearbyContainer.visibility == View.VISIBLE) {
                binding.nearbyContainer.visibility = View.GONE
                binding.ivToggle.rotation = 0f
                return
            }

            binding.nearbyContainer.removeAllViews()

            val context = binding.root.context
            val layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            item.nearbyCities.forEach { nearby ->
                val row = LayoutInflater.from(context)
                    .inflate(R.layout.item_nearby_city_row, binding.nearbyContainer, false)

                val nameView = row.findViewById<TextView>(R.id.tvNearbyCityName)
                val kmView = row.findViewById<TextView>(R.id.tvNearbyCityKm)

                nameView.text = nearby.city
                kmView.text = "${nearby.km ?: 0} ${" KM"}"

                row.layoutParams = layoutParams
                binding.nearbyContainer.addView(row)
            }

            binding.nearbyContainer.visibility = View.VISIBLE
            binding.ivToggle.rotation = 180f
        }
    }
}

