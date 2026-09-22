package com.dadabarbie.TruckTrip.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.databinding.ItemBannerSlideBinding

data class BannerItem(val imageRes: Int, val url: String)

class BannerAdapter(
    private val items: List<BannerItem>,
    private val onItemClick: (url: String) -> Unit
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    inner class BannerViewHolder(val binding: ItemBannerSlideBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val binding = ItemBannerSlideBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return BannerViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val item = items[position]
        holder.binding.bannerImage.setImageResource(item.imageRes)
        holder.binding.bannerImage.setOnClickListener {
            if (item.url.isNotEmpty()) onItemClick(item.url)
        }
    }
}
