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
import com.dadabarbie.TruckTrip.databinding.TripNewListLayoutBinding
import com.dadabarbie.TruckTrip.model.getTrip.Record
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.unaryMinus

class TripNewListAdapter(val context: Context, val editTripDataListner: EditTripDataListner,val shareTripDataListner: ShareTripDataListner,
                      val deleteTripListner: DeleteTripListner,
                         val viewTripDataListner: TripNewListAdapter.ViewTripDataListner
) :
    RecyclerView.Adapter<TripNewListAdapter.ViewHolder>() {

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
        val binding: TripNewListLayoutBinding =
            TripNewListLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
    // 🔥 IMPROVED: Return current list for debugging
    fun getCurrentList(): List<Record> = differ.currentList

    // 🔥 NEW: Get item by ID
    fun getItemById(id: String): Record? {
        return differ.currentList.find { it._id == id }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = differ.currentList[position]
        holder.binding.editIcon.setOnClickListener {
            editTripDataListner.editTripData(item)
        }
        holder.binding.srcName.text=item.source
        holder.binding.dest.text=item.destination
        holder.binding.srcDate.text=item.start_date
        holder.binding.destDate.text=item.end_date
        holder.binding.totalAvak.text="₹" +item.total_income
        holder.binding.totalJavak.text="₹" +item.total_expense
//        holder.binding.driverAvak.text="₹" +item.driver_income
        val income = item.total_income?.toIntOrNull() ?: 0
        val expense = item.total_expense?.toIntOrNull() ?: 0
        holder.binding.totalProfit.text = "₹${income - expense}"
        holder.binding.truckNumber.text=item.truck_no
        holder.binding.shareIcon.setOnClickListener{
            shareTripDataListner.shareTripDataMethod(position)
        }
        if (item.route.isNotEmpty()) {
            holder.binding.tvRoute.visibility = View.VISIBLE

            val routeText = buildString {
                append(item.route[0])

                item.route.drop(1).forEach { routeItem ->
                    append(" → ")
                    append(routeItem)
                }
            }

            holder.binding.tvRoute.text = routeText
        } else {
            holder.binding.tvRoute.visibility = View.GONE
        }


        holder.binding.deleteIcon.setOnClickListener {
            deleteTripListner.deleteTripMethod(position)
        }
        holder.binding.dowanloadIcon.setOnClickListener{
            viewTripDataListner.viewTripDataMethod(item)
        }
        if(position%3==0){
            holder.binding.myTemplate.visible()
            GlobalScope.launch {
                /*ca-app-pub-8808039515208362/1007625609*/
                val adLoader = AdLoader.Builder(context, "ca-app-pub-8808039515208362/1007625609")
                    .forNativeAd { nativeAd ->
                        holder.binding.myTemplate.setNativeAd(nativeAd)
                        holder.binding.myTemplate.visibility = View.VISIBLE
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.e("AdMob", "Ad failed to load: ${error.message}")
                        }
                    })
                    .build()

                adLoader.loadAd(AdRequest.Builder().build())
            }
        }else{
            holder.binding.myTemplate.gone()
        }


    }

    class ViewHolder(var binding: TripNewListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
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