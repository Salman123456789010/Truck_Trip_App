package com.dadabarbie.TruckTrip.adapter

import android.graphics.Color
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.activity.ThirdExpenseScreen
import com.dadabarbie.TruckTrip.databinding.ItemExpenseBinding

class ExpenseAdapter(
    private val items: List<ThirdExpenseScreen.ExpenseItem>,
    private val onAction: (ThirdExpenseScreen.ExpenseItem, String) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpenseBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvNote.text = item.note
        holder.binding.tvAmount.text = "₹${item.amount}"

        // ✅ CORRECT WAY - Direct enum comparison
        when (item.type) {
            ThirdExpenseScreen.ExpenseType.AAVAK -> {
                // Green color for income (AAVAK)
                holder.binding.tvAmount.setTextColor(Color.parseColor("#4CAF50"))
                android.util.Log.d("ExpenseAdapter", "✅ Set GREEN for income: ${item.note}")
            }
            ThirdExpenseScreen.ExpenseType.KHARCHA -> {
                // Red color for expense (KHARCHA)
                holder.binding.tvAmount.setTextColor(Color.parseColor("#F44336"))
                android.util.Log.d("ExpenseAdapter", "✅ Set RED for expense: ${item.note}")
            }
        }

        holder.binding.root.setOnClickListener {
            android.util.Log.d("ExpenseAdapter", "Item clicked: ${item.note}, type: ${item.type}")
            showActionDialog(holder.itemView.context, item)
        }

        holder.binding.root.setOnLongClickListener {
            android.util.Log.d("ExpenseAdapter", "Long click detected for: ${item.note}")
            showActionDialog(holder.itemView.context, item)
            true
        }
    }

    private fun showActionDialog(context: android.content.Context, item: ThirdExpenseScreen.ExpenseItem) {
        android.util.Log.d("ExpenseAdapter", "Showing action dialog for: ${item.note}")

        AlertDialog.Builder(context)
            .setTitle("${item.note} - ₹${item.amount}")
            .setMessage(context.getString(R.string.kya_karna_hai))
            .setPositiveButton(context.getString(R.string.edit)) { dialog, _ ->
                dialog.dismiss()
                android.util.Log.d("ExpenseAdapter", "Edit clicked for: ${item.note}")
                onAction(item, "EDIT")
            }
            .setNegativeButton(context.getString(R.string.delete)) { dialog, _ ->
                dialog.dismiss()
                android.util.Log.d("ExpenseAdapter", "Delete clicked for: ${item.note}")
                showDeleteConfirmation(context, item)
            }
            .setNeutralButton("❌"+context.getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }
    private fun showDeleteConfirmation(context: android.content.Context, item: ThirdExpenseScreen.ExpenseItem) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.delete_confirmation))
            .setMessage(
                context.getString(
                    R.string.kya_aap_ko_delete_karna_chahte_ho_amount,
                    item.note,
                    item.amount
                ))
            .setPositiveButton(context.getString(R.string.haan_delete_karo)) { dialog, _ ->
                dialog.dismiss()
                android.util.Log.d("ExpenseAdapter", "Confirmed delete for: ${item.note}")
                onAction(item, "DELETE")
            }
            .setNegativeButton(context.getString(R.string.nahi_cancel_karo)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }


    override fun getItemCount() = items.size
}