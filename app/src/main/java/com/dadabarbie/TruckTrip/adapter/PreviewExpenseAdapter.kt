package com.dadabarbie.TruckTrip.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.databinding.ItemPreviewExpenseBinding
import com.dadabarbie.TruckTrip.model.addTrip.Expense

import android.content.Context
import com.dadabarbie.TruckTrip.R
import java.util.Locale

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
            val context = binding.root.context
            binding.apply {
                tvExpenseDesc.text = getLocalizedExpenseDesc(context, expense.desc)
                tvExpenseAmount.text = "₹${expense.amount}"
                if (expense.place.isNullOrEmpty()) {
                    tvExpensePlace.visibility = View.GONE
                } else {
                    tvExpensePlace.visibility = View.VISIBLE
                    tvExpensePlace.text = expense.place
                }
                if (expense.date.isNullOrEmpty()) {
                    tvExpenseDate.visibility = View.GONE
                } else {
                    tvExpenseDate.visibility = View.VISIBLE
                    tvExpenseDate.text = expense.date
                }
                tvExpenseType.text = context.getString(R.string.expense_label)

                // Show fuel details if it's a fuel expense
                val isFuel = (expense.type ?: "").equals("Fuel", ignoreCase = true) ||
                             (expense.desc ?: "").lowercase(Locale.ENGLISH).contains("diesel") ||
                             (expense.desc ?: "").lowercase(Locale.ENGLISH).contains("fuel")

                if (isFuel && (!expense.liters.isNullOrEmpty() || !expense.km.isNullOrEmpty())) {
                    layoutFuelDetails.visibility = View.VISIBLE
                    tvFuelLiters.text = if (!expense.liters.isNullOrEmpty()) "${expense.liters} L" else ""
                    tvFuelKm.text = if (!expense.km.isNullOrEmpty()) "${expense.km} km" else ""
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

        private fun getLocalizedExpenseDesc(context: Context, rawDesc: String?): String {
            if (rawDesc.isNullOrEmpty()) return context.getString(R.string.other_expenses_label)
            return when (rawDesc.trim().lowercase(Locale.ENGLISH)) {
                "diesel", "fuel", "diesel / fuel", "fuel cost" -> context.getString(R.string.diesel_charges_label)
                "toll", "toll / fasttag", "toll charges" -> context.getString(R.string.toll_charges_label)
                "bhatta", "driver bhatta" -> context.getString(R.string.driver_bhatta_label)
                "hamali", "hamali / loading", "loading" -> context.getString(R.string.hamali_loading_label)
                "other expense", "other" -> context.getString(R.string.other_expenses_label)
                else -> rawDesc
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