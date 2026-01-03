package com.dadabarbie.TruckTrip.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.Utils.Constants.gone
import com.dadabarbie.TruckTrip.Utils.Constants.visible
import com.dadabarbie.TruckTrip.databinding.TripListLayoutBinding
import com.dadabarbie.TruckTrip.model.getTrip.Record
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TripListAdapter(
    val context: Context,
    val editTripDataListner: EditTripDataListner,
    val shareTripDataListner: ShareTripDataListner,
    val deleteTripListner: DeleteTripListner,
    val dowanloadListner: DowanloadListner,
    val viewTripDataListner: ViewTripDataListner
) : RecyclerView.Adapter<TripListAdapter.ViewHolder>() {

    companion object {
        private const val TAG = "TripListAdapter"
    }

    // 🔥 CRITICAL FIX: Proper DiffUtil implementation
    private val diffCallBack = object : DiffUtil.ItemCallback<Record>() {
        override fun areItemsTheSame(oldItem: Record, newItem: Record): Boolean {
            // Items are same if they have same ID
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(oldItem: Record, newItem: Record): Boolean {
            // Check if content is actually the same
            return oldItem._id == newItem._id &&
                    oldItem.source == newItem.source &&
                    oldItem.destination == newItem.destination &&
                    oldItem.start_date == newItem.start_date &&
                    oldItem.end_date == newItem.end_date &&
                    oldItem.total_income == newItem.total_income &&
                    oldItem.total_expense == newItem.total_expense &&
                    oldItem.driver_income == newItem.driver_income &&
                    oldItem.truck_no == newItem.truck_no
        }
    }

    private val differ = AsyncListDiffer(this, diffCallBack)

    // 🔥 CRITICAL FIX: Proper list submission
    fun submitList(list: List<Record>?) {
        Log.d(TAG, "submitList called with ${list?.size ?: 0} items")

        if (list == null) {
            differ.submitList(null)
            return
        }

        // Create a new list instance to ensure DiffUtil detects changes
        val newList = ArrayList(list)
        differ.submitList(newList)

        Log.d(TAG, "List submitted. Current size: ${differ.currentList.size}")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = TripListLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 🔥 CRITICAL: Validate position before accessing
        if (position < 0 || position >= differ.currentList.size) {
            Log.e(TAG, "Invalid position in onBindViewHolder: $position")
            return
        }

        val item = differ.currentList[position]

        // 🔥 CRITICAL: Validate item has ID
        if (item._id.isNullOrEmpty()) {
            Log.e(TAG, "Item at position $position has no ID")
            return
        }

        holder.bind(item, position)
    }

    inner class ViewHolder(var binding: TripListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Record, position: Int) {
            // Set text fields
            binding.srcName.text = item.source ?: ""
            binding.dest.text = item.destination ?: ""
            binding.srcDate.text = item.start_date ?: ""
            binding.destDate.text = item.end_date ?: ""
            binding.totalAvak.text = "₹${item.total_income ?: "0"}"
            binding.totalJavak.text = "₹${item.total_expense ?: "0"}"
            binding.driverAvak.text = "₹${item.driver_income ?: "0"}"
            binding.truckNumber.text = item.truck_no ?: ""

            // Calculate profit
            val income = item.total_income?.toIntOrNull() ?: 0
            val expense = item.total_expense?.toIntOrNull() ?: 0
            binding.totalProfit.text = "₹${income - expense}"

            // 🔥 CRITICAL FIX: Pass Record object instead of position for edit/view
            binding.editIcon.setOnClickListener {
                Log.d(TAG, "Edit clicked - Position: $position, ID: ${item._id}")
                editTripDataListner.editTripData(item)
            }

            binding.dowanloadIcon.setOnClickListener {
                Log.d(TAG, "View clicked - Position: $position, ID: ${item._id}")
                viewTripDataListner.viewTripDataMethod(item)
            }

            // 🔥 CRITICAL FIX: Pass position for share (needs current position)
            binding.shareIcon.setOnClickListener {
                // Get current position to ensure accuracy
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    Log.d(TAG, "Share clicked - Position: $currentPosition, ID: ${item._id}")
                    shareTripDataListner.shareTripDataMethod(currentPosition)
                }
            }

            // 🔥 CRITICAL FIX: Pass position for delete (needs current position)
            binding.deleteIcon.setOnClickListener {
                // Get current position to ensure accuracy
                val currentPosition = bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    Log.d(TAG, "Delete clicked - Position: $currentPosition, ID: ${item._id}")
                    deleteTripListner.deleteTripMethod(currentPosition)
                } else {
                    Log.e(TAG, "Delete clicked but position is NO_POSITION")
                }
            }

            // Handle ads every 3rd item
            handleAdDisplay(position)
        }

        private fun handleAdDisplay(position: Int) {
            if (position % 3 == 0) {
                binding.myTemplate.visible()

                // Use proper coroutine scope instead of GlobalScope
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val adLoader = AdLoader.Builder(
                            context,
                            "ca-app-pub-8808039515208362/1007625609"
                        )
                            .forNativeAd { nativeAd ->
                                binding.myTemplate.setNativeAd(nativeAd)
                                binding.myTemplate.visibility = View.VISIBLE
                            }
                            .withAdListener(object : AdListener() {
                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    Log.e(TAG, "Ad failed to load: ${error.message}")
                                    binding.myTemplate.gone()
                                }
                            })
                            .build()

                        adLoader.loadAd(AdRequest.Builder().build())
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading ad: ${e.message}")
                        binding.myTemplate.gone()
                    }
                }
            } else {
                binding.myTemplate.gone()
            }
        }
    }

    // 🔥 IMPROVED: Return current list for debugging
    fun getCurrentList(): List<Record> = differ.currentList

    // 🔥 NEW: Get item by ID
    fun getItemById(id: String): Record? {
        return differ.currentList.find { it._id == id }
    }

    interface EditTripDataListner {
        fun editTripData(trip: Record)
    }

    interface ViewTripDataListner {
        fun viewTripDataMethod(trip: Record)
    }

    interface ShareTripDataListner {
        fun shareTripDataMethod(position: Int)
    }

    interface DeleteTripListner {
        fun deleteTripMethod(position: Int)
    }

    interface DowanloadListner {
        fun dowanloadMethod(position: Int)
    }
}