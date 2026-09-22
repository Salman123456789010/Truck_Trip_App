package com.dadabarbie.TruckTrip.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.databinding.ItemCargoBinding
import com.dadabarbie.TruckTrip.model.CargoItem
import java.util.Locale

class CargoAdapter(
    private var cargoList: MutableList<CargoItem>,
    private val onEditClick: (CargoItem, Int) -> Unit,
    private val onDeleteClick: (CargoItem, Int) -> Unit
) : RecyclerView.Adapter<CargoAdapter.CargoViewHolder>() {

    inner class CargoViewHolder(val binding: ItemCargoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CargoViewHolder {
        val binding = ItemCargoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CargoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CargoViewHolder, position: Int) {
        val item = cargoList[position]
        val context = holder.itemView.context

        holder.binding.tvCargoName.text = item.name

        val itemWeightFormatted = formatNumber(item.weightPerItem)
        val totalWeightInKg = item.totalWeightInKg()
        val totalWeightFormatted = if (item.weightUnit.equals("ton", ignoreCase = true)) {
            val totalInTon = totalWeightInKg / 1000.0
            "${formatNumber(totalInTon)} ${context.getString(R.string.unit_ton)}"
        } else {
            "${formatNumber(totalWeightInKg)} ${context.getString(R.string.unit_kg)}"
        }

        val weightUnitStr = if (item.weightUnit.equals("ton", ignoreCase = true)) {
            context.getString(R.string.unit_ton)
        } else {
            context.getString(R.string.unit_kg)
        }

        holder.binding.tvWeightCalc.text = String.format(
            Locale.getDefault(),
            "%s %s × %d = %s",
            itemWeightFormatted,
            weightUnitStr,
            item.quantity,
            totalWeightFormatted
        )

        if (item.hasDimensions()) {
            holder.binding.tvDimensions.visibility = View.VISIBLE
            val l = formatNumber(item.length ?: 0.0)
            val w = formatNumber(item.width ?: 0.0)
            val h = formatNumber(item.height ?: 0.0)
            val dimUnitStr = if (item.dimensionUnit.equals("m", ignoreCase = true)) {
                context.getString(R.string.unit_m)
            } else {
                context.getString(R.string.unit_ft)
            }
            holder.binding.tvDimensions.text = String.format(
                Locale.getDefault(),
                "%s: %s × %s × %s %s",
                context.getString(R.string.cargo_dimensions),
                l, w, h, dimUnitStr
            )
        } else {
            holder.binding.tvDimensions.visibility = View.GONE
        }

        holder.binding.btnEditCargo.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onEditClick(item, pos)
            }
        }

        holder.binding.btnDeleteCargo.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDeleteClick(item, pos)
            }
        }
    }

    override fun getItemCount(): Int = cargoList.size

    fun updateList(newList: MutableList<CargoItem>) {
        cargoList = newList
        notifyDataSetChanged()
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            String.format(Locale.getDefault(), "%,d", value.toLong())
        } else {
            String.format(Locale.getDefault(), "%,.2f", value)
        }
    }
}
