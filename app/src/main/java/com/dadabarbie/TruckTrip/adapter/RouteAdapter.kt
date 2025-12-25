package com.dadabarbie.TruckTrip.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.R

class RouteAdapter(
    private val routeList: ArrayList<String>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    inner class RouteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivPlaceIcon: ImageView = itemView.findViewById(R.id.ivPlaceIcon)
        val tvPlaceLabel: TextView = itemView.findViewById(R.id.tvPlaceLabel)
        val tvPlace: TextView = itemView.findViewById(R.id.tvPlace)
        val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)
        val viewConnector: View = itemView.findViewById(R.id.viewConnector)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val place = routeList[position]

        holder.tvPlace.text = place

        // Show appropriate icon and label based on position
        when (position) {
            0 -> {
                // Source place
                holder.ivPlaceIcon.setImageResource(R.drawable.ic_source)
                holder.tvPlaceLabel.text = holder.itemView.context.getString(R.string.source)
                holder.btnDelete.visibility = View.GONE
            }
            routeList.size - 1 -> {
                // Destination place
                holder.ivPlaceIcon.setImageResource(R.drawable.ic_destination)
                holder.tvPlaceLabel.text = holder.itemView.context.getString(R.string.destination)
                holder.btnDelete.visibility = View.GONE
            }
            else -> {
                // Middle places
                holder.ivPlaceIcon.setImageResource(R.drawable.ic_middle_place)
                holder.tvPlaceLabel.text = holder.itemView.context.getString(R.string.via)
                holder.btnDelete.visibility = View.VISIBLE
            }
        }

        // Delete button click
        holder.btnDelete.setOnClickListener {
            onDeleteClick(position)
        }

        // Show/hide connector line (hide for last item)
        holder.viewConnector.visibility = if (position < routeList.size - 1) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun getItemCount(): Int = routeList.size

    /**
     * Update the route list and refresh the adapter
     */
    fun updateRoutes(newRoutes: ArrayList<String>) {
        routeList.clear()
        routeList.addAll(newRoutes)
        notifyDataSetChanged()
    }
}