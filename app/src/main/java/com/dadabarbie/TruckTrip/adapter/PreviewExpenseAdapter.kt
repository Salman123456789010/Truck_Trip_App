package com.dadabarbie.TruckTrip.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.databinding.ItemPreviewExpenseBinding
import com.dadabarbie.TruckTrip.model.addTrip.Expense

class PreviewExpenseAdapter : ListAdapter<Expense, PreviewExpenseAdapter.ViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPreviewExpenseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(private val binding: ItemPreviewExpenseBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(expense: Expense) {
            binding.apply {
                tvExpenseDesc.text = expense.desc
                tvExpenseAmount.text = "₹${expense.amount}"
                if(expense.place==""){
                    tvExpensePlace.visibility= View.GONE
                }else{
                    tvExpensePlace.visibility= View.VISIBLE
                }
                if(expense.date==""){
                    tvExpenseDate.visibility= View.GONE
                }else{
                    tvExpenseDate.visibility= View.VISIBLE
                }
                tvExpensePlace.text = expense.place
                tvExpenseDate.text = expense.date
                tvExpenseType.text = expense.type ?: "Expense"

                // Show fuel details if it's a fuel expense
                if (expense.type == "Fuel") {
                    layoutFuelDetails.visibility = View.VISIBLE
                    tvFuelLiters.text = "${expense.liters} L"
                    tvFuelKm.text = "${expense.km} km"
                } else {
                    layoutFuelDetails.visibility = View.GONE
                }

                // Show note if available
                if (!expense.note.isNullOrEmpty()) {
                    tvExpenseNote.text = expense.note
                    tvExpenseNote.visibility = View.VISIBLE
                } else {
                    tvExpenseNote.visibility = View.GONE
                }
            }
        }
    }

    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem.desc == newItem.desc && oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem == newItem
        }
    }
}