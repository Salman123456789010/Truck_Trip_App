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
import com.dadabarbie.TruckTrip.model.addTrip.Income


class CreditAdapter(var context: Context,var editCreditClickListner: EditCreditClickListner,
    var deleteCreditClickLitsner: DeleteCreditClickLitsner): RecyclerView.Adapter<CreditAdapter.ViewHolder>() {

    private val diffCallBack = object : DiffUtil.ItemCallback<Income>() {
        override fun areItemsTheSame(
            oldItem: Income,
            newItem: Income
        ): Boolean {
            return oldItem.amount == newItem.amount
        }

        override fun areContentsTheSame(
            oldItem: Income,
            newItem: Income
        ): Boolean {
            return oldItem.amount == newItem.amount
        }
    }
    fun submitList(list: List<Income>) = differ.submitList(list)
    private val differ = AsyncListDiffer(this, diffCallBack)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditAdapter.ViewHolder {
        val binding: CreditLayoutBinding =
            CreditLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CreditAdapter.ViewHolder, position: Int) {
        val item = differ.currentList[position]
        val amountSafe = runCatching { item.amount }.getOrElse { "0" }
        val descSafe = runCatching { item.desc }.getOrElse { "" }
        holder.binding.amount.text = "+$amountSafe"
        holder.binding.amount.setTextColor(context.getColor(R.color.greencolor))
        holder.binding.amountText.text = descSafe
        holder.binding.mainCardLayout.setStrokeColor(ColorStateList.valueOf(context.getColor(R.color.gujrati_txt)))
        val placeSafe = runCatching { item.place?.trim() }.getOrNull().orEmpty()
        val dateSafe  = runCatching { item.date?.trim()  }.getOrNull().orEmpty()
        if (placeSafe.isNotEmpty() || dateSafe.isNotEmpty()) {
            holder.binding.placeDateLayout.visibility = android.view.View.VISIBLE
            holder.binding.tvPlace.text = placeSafe
            holder.binding.tvDate.text = dateSafe
        } else {
            holder.binding.placeDateLayout.visibility = android.view.View.GONE
        }
        
        holder.binding.productMenu.setOnClickListener{
            val popupMenu = PopupMenu(context,  holder.binding.productMenu)
            popupMenu.menuInflater.inflate(R.menu.export_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener(object : MenuItem.OnMenuItemClickListener,
                PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem): Boolean {
                    when (item.itemId) {
                        R.id.delete -> {
                            deleteCreditClickLitsner.clickCreditDeleteMethod(position)
                        }

                        R.id.edit -> {
                            editCreditClickListner.clickCreditEditMethod(position)
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
    interface EditCreditClickListner{
        fun clickCreditEditMethod(position: Int)
    }
    interface DeleteCreditClickLitsner{
        fun clickCreditDeleteMethod(position: Int)
    }


}
