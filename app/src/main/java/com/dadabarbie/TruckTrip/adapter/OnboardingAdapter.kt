package com.dadabarbie.TruckTrip.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.databinding.ItemOnboardingBinding
import com.dadabarbie.TruckTrip.model.OnboardingItem

class OnboardingAdapter(private val voiceListener: OnboardingVoiceListener? = null) : RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<OnboardingItem>() {
        override fun areItemsTheSame(oldItem: OnboardingItem, newItem: OnboardingItem): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: OnboardingItem, newItem: OnboardingItem): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    fun submitList(list: List<OnboardingItem>) = differ.submitList(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = differ.currentList[position]
        holder.binding.imageHowTo.setImageResource(item.imageRes)
        holder.binding.textDescription.text = item.description
        // other binds

        holder.binding.btnSpeak.setOnClickListener {
            voiceListener?.onRequestSpeak(item.description)
        }
        holder.binding.btnStopSpeak.setOnClickListener {
            voiceListener?.onRequestStop()
        }
        holder.binding.textTitle.text = item.title
        holder.binding.textDescription.text = item.description
    }

    override fun getItemCount(): Int = differ.currentList.size

    class ViewHolder(val binding: ItemOnboardingBinding) : RecyclerView.ViewHolder(binding.root)
}

// define interface
interface OnboardingVoiceListener {
    fun onRequestSpeak(text: String)
    fun onRequestStop()
}


