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
        override fun areItemsTheSame(oldItem: Income, newItem: Income): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Income, newItem: Income): Boolean {
            return oldItem == newItem
        }
    }
    fun submitList(list: List<Income>, function: () -> Unit) = differ.submitList(list)
    private val differ = AsyncListDiffer(this, diffCallBack)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CreditAdapter.ViewHolder {
        val binding: CreditLayoutBinding =
            CreditLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CreditAdapter.ViewHolder, position: Int) {
        val item = differ.currentList[position]
        // Show amount with plus sign for credit
        holder.binding.amount.text = "+${item.amount}"
        holder.binding.amount.setTextColor(context.getColor(R.color.greencolor))
        holder.binding.amountText.text = item.desc
        holder.binding.mainCardLayout.setStrokeColor(ColorStateList.valueOf(context.getColor(R.color.gujrati_txt)))
        
        // Show place and date if available
        if (item.place.isNotEmpty() || item.date.isNotEmpty()) {
            holder.binding.placeDateLayout.visibility = android.view.View.VISIBLE
            holder.binding.tvPlace.text = item.place
            holder.binding.tvDate.text = item.date
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
//                        sheetFlag = true
//                        productSheetFormat = getString(R.string.xlsx)
//                        productListViewModel.getProductFile(getString(R.string.file_type_excel))
                        }

                        R.id.edit -> {
                            editCreditClickListner.clickCreditEditMethod(position)
//                        sheetFlag = true
//                        productSheetFormat = getString(R.string.pdf)
//                        productListViewModel.getProductFile(getString(R.string.file_type_pdf))
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
