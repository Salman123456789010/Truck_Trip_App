// ==========================================
// 1. DEBIT ADAPTER - FIXED VERSION
// ==========================================
package com.dadabarbie.TruckTrip.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.databinding.CreditLayoutBinding
import com.dadabarbie.TruckTrip.model.addTrip.Expense

class DebitAdapter(
    var context: Context,
    var deleteClickListner: DeleteClickListner,
    var editClickListner: EditClickListner
) : ListAdapter<Expense, DebitAdapter.ViewHolder>(DebitDiffCallback()) {

    // FIXED: Proper DiffUtil implementation
    class DebitDiffCallback : DiffUtil.ItemCallback<Expense>() {

        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem.id == newItem.id   // ✅ ONLY ID
        }

        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem == newItem         // data class handles this
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = CreditLayoutBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Use getItem() instead of differ.currentList
        val item = getItem(position)

        holder.binding.amount.text = "-${item.amount}"
        holder.binding.amount.setTextColor(context.getColor(android.R.color.holo_red_dark))
        holder.binding.amountText.text = item.desc
        holder.binding.mainCardLayout.setStrokeColor(
            ColorStateList.valueOf(context.getColor(R.color.tamil_txt))
        )

        if (item.place.isNotEmpty() || item.date.isNotEmpty()) {
            holder.binding.placeDateLayout.visibility = android.view.View.VISIBLE
            holder.binding.tvPlace.text = item.place
            holder.binding.tvDate.text = item.date
        } else {
            holder.binding.placeDateLayout.visibility = android.view.View.GONE
        }

        holder.binding.productMenu.setOnClickListener {
            val popupMenu = PopupMenu(context, holder.binding.productMenu)
            popupMenu.menuInflater.inflate(R.menu.export_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.delete -> {
                        // Use holder.bindingAdapterPosition for accurate position
                        val currentPosition = holder.bindingAdapterPosition
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            deleteClickListner.clickDebitDeleteMethod(currentPosition)
                        }
                    }
                    R.id.edit -> {
                        val currentPosition = holder.bindingAdapterPosition
                        if (currentPosition != RecyclerView.NO_POSITION) {
                            editClickListner.clickDebitEditMethod(currentPosition)
                        }
                    }
                }
                true
            }
            popupMenu.show()
        }
    }

    class ViewHolder(var binding: CreditLayoutBinding) : RecyclerView.ViewHolder(binding.root)

    interface DeleteClickListner {
        fun clickDebitDeleteMethod(position: Int)
    }

    interface EditClickListner {
        fun clickDebitEditMethod(position: Int)
    }
}
