package com.dadabarbie.TruckTrip.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.databinding.ItemPreviewIncomeBinding
import com.dadabarbie.TruckTrip.model.addTrip.Income

import android.content.Context
import com.dadabarbie.TruckTrip.R
import java.util.Locale

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
            val context = binding.root.context
            binding.apply {
                tvIncomeDesc.text = getLocalizedIncomeDesc(context, income.desc)
                tvIncomeAmount.text = "₹${income.amount}"
                if (income.place.isNullOrEmpty()) {
                    tvIncomePlace.visibility = View.GONE
                } else {
                    tvIncomePlace.visibility = View.VISIBLE
                    tvIncomePlace.text = income.place
                }
                if (income.date.isNullOrEmpty()) {
                    tvIncomeDate.visibility = View.GONE
                } else {
                    tvIncomeDate.visibility = View.VISIBLE
                    tvIncomeDate.text = income.date
                }

                // Show note if available
                if (!income.note.isNullOrEmpty()) {
                    tvIncomeNote.text = income.note
                    tvIncomeNote.visibility = View.VISIBLE
                } else {
                    tvIncomeNote.visibility = View.GONE
                }
            }
        }

        private fun getLocalizedIncomeDesc(context: Context, rawDesc: String?): String {
            if (rawDesc.isNullOrEmpty()) return context.getString(R.string.total_freight_label)
            return when (rawDesc.trim().lowercase(Locale.ENGLISH)) {
                "freight", "total freight" -> context.getString(R.string.total_freight_label)
                "advance received", "advance" -> context.getString(R.string.advance_received)
                else -> rawDesc
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