package com.dadabarbie.TruckTrip.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.databinding.ItemOnboardingBinding
import com.dadabarbie.TruckTrip.model.OnboardingItem

class OnboardingAdapter(private val voiceListener: OnboardingVoiceListener? = null) :
    RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {

    private val diffCallback = object : DiffUtil.ItemCallback<OnboardingItem>() {
        override fun areItemsTheSame(oldItem: OnboardingItem, newItem: OnboardingItem): Boolean {
            return oldItem.title == newItem.title
        }

        override fun areContentsTheSame(oldItem: OnboardingItem, newItem: OnboardingItem): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    // NEW: Track loading state
    private var isLoading = false
    private var currentViewHolder: ViewHolder? = null

    fun submitList(list: List<OnboardingItem>) = differ.submitList(list)

    // NEW: Method to show/hide loading indicator
    fun setLoadingState(loading: Boolean) {
        isLoading = loading
        currentViewHolder?.updateLoadingState(loading)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOnboardingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = differ.currentList[position]

        // Store reference to current visible holder
        currentViewHolder = holder

        holder.binding.imageHowTo.setImageResource(item.imageRes)
        holder.binding.textTitle.text = item.title
        holder.binding.textDescription.text = item.description

        // Set initial loading state
        holder.updateLoadingState(isLoading)

        holder.binding.btnSpeak.setOnClickListener {
            voiceListener?.onRequestSpeak(item.description)
        }

        holder.binding.btnStopSpeak.setOnClickListener {
            voiceListener?.onRequestStop()
        }
    }

    override fun getItemCount(): Int = differ.currentList.size

    class ViewHolder(val binding: ItemOnboardingBinding) : RecyclerView.ViewHolder(binding.root) {

        // NEW: Method to update loading UI
        fun updateLoadingState(loading: Boolean) {
            // Option 1: Show/hide a progress bar (if you have one in your layout)
            // binding.progressBar?.visibility = if (loading) View.VISIBLE else View.GONE

            // Option 2: Disable speak button and show loading state
            binding.btnSpeak.isEnabled = !loading
            binding.btnSpeak.alpha = if (loading) 0.5f else 1.0f

            // Option 3: Change button text/icon during loading (optional)
            // if (loading) {
            //     binding.btnSpeak.text = "Loading..."
            // } else {
            //     binding.btnSpeak.text = "Speak"
            // }
        }
    }
}

// define interface
interface OnboardingVoiceListener {
    fun onRequestSpeak(text: String)
    fun onRequestStop()
}