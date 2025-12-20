package com.dadabarbie.TruckTrip.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.Utils.Constants.gone
import com.dadabarbie.TruckTrip.Utils.Constants.visible
import com.dadabarbie.TruckTrip.databinding.DraftLayoutListBinding
import com.dadabarbie.TruckTrip.databinding.NewDraftLayoutBinding
import com.dadabarbie.TruckTrip.room.model.TripDataTestModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NewDraftAdapter(val context: Context,val draftEditListner: DraftEditListner,val deleteListner: DeleteListner) : RecyclerView.Adapter<NewDraftAdapter.ViewHolder>() {

    private val diffCallBack = object : DiffUtil.ItemCallback<TripDataTestModel>() {
        override fun areItemsTheSame(
            oldItem: TripDataTestModel,
            newItem: TripDataTestModel
        ): Boolean {
            return oldItem.randomNumber == newItem.randomNumber
        }

        override fun areContentsTheSame(
            oldItem: TripDataTestModel,
            newItem: TripDataTestModel
        ): Boolean {
            return oldItem.randomNumber == newItem.randomNumber
        }
    }
    fun submitList(list: List<TripDataTestModel>) = differ.submitList(list)
    private val differ = AsyncListDiffer(this, diffCallBack)



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewDraftAdapter.ViewHolder {
        val binding: NewDraftLayoutBinding =
            NewDraftLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = differ.currentList[position]
        holder.binding.srcName.text=item.srcPlace
        holder.binding.dest.text=item.destPlace
        holder.binding.srcDate.text=item.srcDate
        holder.binding.destDate.text=item.destDate
        holder.binding.truckAvg.text=item.avg
        holder.binding.truckNumber.text=item.truckNumber
        holder.binding.editIcon.setOnClickListener {
            draftEditListner.editMethod(position)
        }
        holder.binding.deleteIcon.setOnClickListener {
            deleteListner.deleteMethod(position)
        }

        if(position%3==0){
            holder.binding.myTemplate.visible()
            GlobalScope.launch {
                val adLoader = AdLoader.Builder(context, "ca-app-pub-8808039515208362/1007625609")
                    .forNativeAd { nativeAd ->
                        holder.binding.myTemplate.setNativeAd(nativeAd)
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

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
    class ViewHolder(var binding: NewDraftLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }
    interface DraftEditListner{
        fun editMethod(position: Int)
    }
    interface DeleteListner{
        fun deleteMethod(position: Int)
    }
}
