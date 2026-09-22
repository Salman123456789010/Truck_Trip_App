package com.dadabarbie.TruckTrip.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.databinding.ItemFuelStopBinding
import com.dadabarbie.TruckTrip.fuelplanner.model.FuelStop
import java.util.Locale

class FuelStopAdapter(
    private var stops: List<FuelStop> = emptyList(),
    private var currencySymbol: String = "₹"
) : RecyclerView.Adapter<FuelStopAdapter.FuelStopViewHolder>() {

    fun updateData(newStops: List<FuelStop>, newCurrency: String = currencySymbol) {
        this.stops = newStops
        this.currencySymbol = newCurrency
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FuelStopViewHolder {
        val binding = ItemFuelStopBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FuelStopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FuelStopViewHolder, position: Int) {
        holder.bind(stops[position], position)
    }

    override fun getItemCount(): Int = stops.size

    inner class FuelStopViewHolder(
        private val binding: ItemFuelStopBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FuelStop, position: Int) {
            val stopName = item.stationName ?: "Fuel Stop ${item.sequence}"
            binding.tvStopTitle.text = "⛽ $stopName"

            binding.tvDistanceFromStart.text = String.format(
                Locale.getDefault(),
                "At: %,.1f km",
                item.distanceFromStartKm
            )

            binding.tvLegDistance.text = String.format(
                Locale.getDefault(),
                "(+%,.1f km from previous)",
                item.distanceFromPreviousKm
            )

            binding.tvFuelBeforeStop.text = String.format(
                Locale.getDefault(),
                "~%,.1f L",
                item.estimatedFuelBeforeStopLiters
            )

            binding.tvRecommendedFuel.text = String.format(
                Locale.getDefault(),
                "~%,.1f L",
                item.recommendedFuelQuantityLiters
            )

            if (item.estimatedCost != null && item.estimatedCost > 0.0) {
                binding.tvEstimatedCost.visibility = View.VISIBLE
                binding.tvEstimatedCost.text = String.format(
                    Locale.getDefault(),
                    "Est. Cost: %s %,.2f",
                    currencySymbol,
                    item.estimatedCost
                )
            } else {
                binding.tvEstimatedCost.visibility = View.GONE
            }

            binding.tvRemainingDistance.text = String.format(
                Locale.getDefault(),
                "Remaining: %,.1f km",
                item.remainingDistanceKm
            )

            binding.tvTruckFriendlyBadge.visibility = if (item.truckFriendly) View.VISIBLE else View.GONE
        }
    }
}
