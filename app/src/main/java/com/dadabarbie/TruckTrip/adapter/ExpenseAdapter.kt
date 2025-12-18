package com.dadabarbie.TruckTrip.adapter

import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
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
        android.util.Log.d("ExpenseAdapter", "=== DIALOG START === Showing dialog for: ${item.note}, type: ${item.type}")

        try {
            val options = arrayOf("Edit", "Delete")

            val dialog = AlertDialog.Builder(context)
                .setTitle("Kya karna hai?")
                .setItems(options) { dialogInterface, which ->
                    android.util.Log.d("ExpenseAdapter", "=== CLICK DETECTED === Position: $which for item: ${item.note}")

                    // Post to main thread to ensure execution
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        when (which) {
                            0 -> {
                                android.util.Log.d("ExpenseAdapter", "=== EDIT ACTION === Calling onAction for EDIT")
                                try {
                                    onAction(item, "EDIT")
                                    android.util.Log.d("ExpenseAdapter", "=== EDIT SUCCESS === onAction called successfully")
                                } catch (e: Exception) {
                                    android.util.Log.e("ExpenseAdapter", "=== EDIT ERROR === ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                            1 -> {
                                android.util.Log.d("ExpenseAdapter", "=== DELETE ACTION === Calling onAction for DELETE")
                                try {
                                    onAction(item, "DELETE")
                                    android.util.Log.d("ExpenseAdapter", "=== DELETE SUCCESS === onAction called successfully")
                                } catch (e: Exception) {
                                    android.util.Log.e("ExpenseAdapter", "=== DELETE ERROR === ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                        }
                    }

                    dialogInterface.dismiss()
                }
                .setCancelable(true)
                .setOnDismissListener {
                    android.util.Log.d("ExpenseAdapter", "=== DIALOG DISMISSED ===")
                }
                .create()

            dialog.show()
            android.util.Log.d("ExpenseAdapter", "=== DIALOG SHOWN === Dialog is now visible")
        } catch (e: Exception) {
            android.util.Log.e("ExpenseAdapter", "=== DIALOG ERROR === ${e.message}")
            e.printStackTrace()
        }
    }

    override fun getItemCount() = items.size
}