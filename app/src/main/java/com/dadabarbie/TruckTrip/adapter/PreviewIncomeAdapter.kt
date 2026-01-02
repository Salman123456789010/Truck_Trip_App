package com.dadabarbie.TruckTrip.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.databinding.ItemPreviewIncomeBinding
import com.dadabarbie.TruckTrip.model.addTrip.Income

class PreviewIncomeAdapter : ListAdapter<Income, PreviewIncomeAdapter.ViewHolder>(IncomeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPreviewIncomeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemPreviewIncomeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(income: Income) {
            binding.apply {
                tvIncomeDesc.text = income.desc
                tvIncomeAmount.text = "₹${income.amount}"
                if(income.place==""){
                    tvIncomePlace.visibility= View.GONE
                }else{
                    tvIncomePlace.visibility= View.VISIBLE
                }
                if(income.date==""){
                    tvIncomeDate.visibility= View.GONE
                }else{
                    tvIncomeDate.visibility= View.VISIBLE
                }
                tvIncomePlace.text = income.place
                tvIncomeDate.text = income.date

                // Show note if available
                if (!income.note.isNullOrEmpty()) {
                    tvIncomeNote.text = income.note
                    tvIncomeNote.visibility = android.view.View.VISIBLE
                } else {
                    tvIncomeNote.visibility = android.view.View.GONE
                }
            }
        }
    }

    class IncomeDiffCallback : DiffUtil.ItemCallback<Income>() {
        override fun areItemsTheSame(oldItem: Income, newItem: Income): Boolean {
            return oldItem.desc == newItem.desc && oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: Income, newItem: Income): Boolean {
            return oldItem == newItem
        }
    }
}