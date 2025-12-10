package com.dadabarbie.TruckTrip.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.databinding.CreditLayoutBinding
import com.dadabarbie.TruckTrip.model.addTrip.Expense


class DebitAdapter(
    var context: Context,
    var deleteClickListner: DeleteClickListner,
    var editClickListner: EditClickListner
) : RecyclerView.Adapter<DebitAdapter.ViewHolder>() {

    private val diffCallBack = object : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(
            oldItem: Expense,
            newItem: Expense
        ): Boolean {
            return oldItem.amount == newItem.amount
        }

        override fun areContentsTheSame(
            oldItem: Expense,
            newItem: Expense
        ): Boolean {
            return oldItem.amount == newItem.amount
        }
    }

    fun submitList(list: List<Expense>) = differ.submitList(list)
    private val differ = AsyncListDiffer(this, diffCallBack)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DebitAdapter.ViewHolder {
        val binding: CreditLayoutBinding =
            CreditLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DebitAdapter.ViewHolder, position: Int) {
        val item = differ.currentList[position]
        val amountSafe = runCatching { item.amount }.getOrElse { "0" }
        val descSafe = runCatching { item.desc }.getOrElse { "" }
        holder.binding.amount.text = "-$amountSafe"
        holder.binding.amount.setTextColor(context.getColor(android.R.color.holo_red_dark))
        holder.binding.amountText.text = descSafe
        holder.binding.mainCardLayout.setStrokeColor(ColorStateList.valueOf(context.getColor(R.color.tamil_txt)))
        val placeSafe = runCatching { item.place }.getOrElse { "" }.trim()
        val dateSafe = runCatching { item.date }.getOrElse { "" }.trim()
        if (placeSafe.isNotEmpty() || dateSafe.isNotEmpty()) {
            holder.binding.placeDateLayout.visibility = android.view.View.VISIBLE
            holder.binding.tvPlace.text = placeSafe
            holder.binding.tvDate.text = dateSafe
        } else {
            holder.binding.placeDateLayout.visibility = android.view.View.GONE
        }
        
        holder.binding.productMenu.setOnClickListener {
            val popupMenu = PopupMenu(context, holder.binding.productMenu)
            popupMenu.menuInflater.inflate(R.menu.export_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener(object : MenuItem.OnMenuItemClickListener,
                PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem): Boolean {
                    when (item.itemId) {
                        R.id.delete -> {
                            deleteClickListner.clickDebitDeleteMethod(position)
                        }

                        R.id.edit -> {
                            editClickListner.clickDebitEditMethod(position)
                        }
                    }
                    return true
                }
            })
            popupMenu.show()
        }
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    class ViewHolder(var binding: CreditLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }

    interface DeleteClickListner {
        fun clickDebitDeleteMethod(position: Int)
    }

    interface EditClickListner {
        fun clickDebitEditMethod(position: Int)

    }
}
