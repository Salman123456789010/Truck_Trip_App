package com.dadabarbie.TruckTrip.adapter

import android.R
import android.adservices.topics.GetTopicsRequest
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
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
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class TripListAdapter(val context: Context, val editTripDataListner: EditTripDataListner,val shareTripDataListner: ShareTripDataListner,
 val deleteTripListner: DeleteTripListner,val dowanloadListner: DowanloadListner,
    val viewTripDataListner: ViewTripDataListner) :
    RecyclerView.Adapter<TripListAdapter.ViewHolder>() {

    private val diffCallBack = object : DiffUtil.ItemCallback<Record>() {
        override fun areItemsTheSame(
            oldItem: Record,
            newItem: Record
        ): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(
            oldItem: Record,
            newItem: Record
        ): Boolean {
            return oldItem._id == newItem._id
        }
    }

    fun submitList(list: List<Record>) = differ.submitList(list)
    private val differ = AsyncListDiffer(this, diffCallBack)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripListAdapter.ViewHolder {
        val binding: TripListLayoutBinding =
            TripListLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TripListAdapter.ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: TripListAdapter.ViewHolder, position: Int) {
        val item = differ.currentList[position]
        holder.binding.editIcon.setOnClickListener {
            editTripDataListner.editTripData(item)
        }
        holder.binding.dowanloadIcon.setOnClickListener {
            viewTripDataListner.viewTripDataMethod(item)
        }
        holder.binding.srcName.text=item.source
        holder.binding.dest.text=item.destination
        holder.binding.srcDate.text=item.start_date
        holder.binding.destDate.text=item.end_date
        holder.binding.totalAvak.text="₹" +item.total_income
        holder.binding.totalJavak.text="₹" +item.total_expense
        holder.binding.driverAvak.text="₹" +item.driver_income
        val income = item.total_income?.toIntOrNull() ?: 0
        val expense = item.total_expense?.toIntOrNull() ?: 0
        holder.binding.totalProfit.text = "₹${income - expense}"
        holder.binding.truckNumber.text=item.truck_no
          holder.binding.shareIcon.setOnClickListener{
              shareTripDataListner.shareTripDataMethod(position)
          }
        holder.binding.deleteIcon.setOnClickListener {
            deleteTripListner.deleteTripMethod(position)
        }
//        holder.binding.dowanloadIcon.setOnClickListener{
//            dowanloadListner.dowanloadMethod(position)
//        }
       if(position%3==0){
           holder.binding.myTemplate.visible()
           GlobalScope.launch {
//               ca-app-pub-8808039515208362/1007625609  main
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

    class ViewHolder(var binding: TripListLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }

    interface EditTripDataListner {
        fun editTripData(trip: Record)
    }
    interface ViewTripDataListner {
        fun viewTripDataMethod(trip: Record)  // Pass Record instead of position
    }
    interface ShareTripDataListner{
        fun shareTripDataMethod(position: Int)
    }
    interface DeleteTripListner{
        fun deleteTripMethod(position: Int)
    }

    interface DowanloadListner{
        fun dowanloadMethod(position: Int)
    }
}