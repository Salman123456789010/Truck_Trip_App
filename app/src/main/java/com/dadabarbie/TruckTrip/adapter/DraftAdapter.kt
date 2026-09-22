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
import com.dadabarbie.TruckTrip.databinding.TripListLayoutBinding
import com.dadabarbie.TruckTrip.room.model.TripDataTestModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class DraftAdapter(val context: Context,val draftEditListner: DraftEditListner,val deleteListner: DeleteListner) : RecyclerView.Adapter<DraftAdapter.ViewHolder>() {

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



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DraftAdapter.ViewHolder {
        val binding: DraftLayoutListBinding =
            DraftLayoutListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DraftAdapter.ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DraftAdapter.ViewHolder, position: Int) {
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

        holder.binding.myTemplate.gone()


    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
    class ViewHolder(var binding: DraftLayoutListBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }
    interface DraftEditListner{
        fun editMethod(position: Int)
    }
    interface DeleteListner{
        fun deleteMethod(position: Int)
    }
}