package com.dadabarbie.TruckTrip.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.databinding.LanguageSelectionLayoutBinding
import com.dadabarbie.TruckTrip.languagemodel.Language
import java.util.Random


class LanguageAdapter(val context: Context,val onClickListner:OnItemClick) : RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {

    private val diffCallBack = object : DiffUtil.ItemCallback<Language>() {
        override fun areItemsTheSame(
            oldItem: Language,
            newItem: Language
        ): Boolean {
            return oldItem.label == newItem.label
        }

        override fun areContentsTheSame(
            oldItem: Language,
            newItem: Language
        ): Boolean {
            return oldItem.label == newItem.label
        }
    }
    fun submitList(list: List<Language>) = differ.submitList(list)
    private val differ = AsyncListDiffer(this, diffCallBack)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageAdapter.ViewHolder {
        val binding: LanguageSelectionLayoutBinding =
            LanguageSelectionLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LanguageAdapter.ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LanguageAdapter.ViewHolder, position: Int) {
        val item = differ.currentList[position]
        holder.binding.englishTxt.text = item.label+" (${item.english_label})"
        if(item.flag){
//            holder.binding.englishLayout.alpha=0.3f
            holder.binding.languageLayout.setStrokeColor(ColorStateList.valueOf(
                ResourcesCompat.getColor(context.resources, R.color.color_primary, null)))
            holder.binding.languageLayout.strokeWidth=4

        }else{
            holder.binding.languageLayout.setStrokeColor(ColorStateList.valueOf(
                ResourcesCompat.getColor(context.resources, R.color.grey, null)))
//            holder.binding.englishLayout.alpha=1f
            holder.binding.languageLayout.strokeWidth=1

        }
        holder.binding.englishLayout.setOnClickListener{
            onClickListner.clickEvent(position)
        }
        when(item.label){
            Constants.english -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.english, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.english_txt, null))
            }
            Constants.hindi -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.hindi, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.hindi_txt, null))
            }
            Constants.telgu -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.telgu, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.telgu_txt, null))
            }
            Constants.gujrati -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.gujrati, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.gujrati_txt, null))
            }
            Constants.punjabi -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.punjabi, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.punjabi_txt, null))
            }
            Constants.tamil -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.tamil, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.tamil_txt, null))
            }
            Constants.marathi -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.marathi, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.marathi_txt, null))
            }
            Constants.bangla -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.hindi, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.hindi_txt, null))
            }
            Constants.malyalam -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.telgu, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.telgu_txt, null))
            }
            Constants.kannada -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.gujrati, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.gujrati_txt, null))
            }
            Constants.aasamise -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.tamil, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.tamil_txt, null))
            }
            Constants.maithili -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.marathi, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.marathi_txt, null))
            }
            Constants.odisa -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.telgu, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.telgu_txt, null))
            }
            Constants.nepali -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.gujrati, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.gujrati_txt, null))
            }
            Constants.dogri -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.english, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.english_txt, null))
            }
            Constants.bhojpuri -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.hindi, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.hindi_txt, null))
            }
            Constants.rajastani -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.marathi, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.marathi_txt, null))
            }
            else -> {
                holder.binding.languageLayout.backgroundTintList = ColorStateList.valueOf(
                    ResourcesCompat.getColor(context.resources, R.color.bangla, null))
                holder.binding.englishTxt.setTextColor(ResourcesCompat.getColor(context.resources, R.color.bangla_txt, null))
            }
        }


    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
    class ViewHolder(var binding: LanguageSelectionLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

    }
    interface OnItemClick{
        fun clickEvent(position: Int)
    }

    fun getRandomColor(): Int {
        val random = Random()
        var color: Int
        do {
            // Generate random color
            color = -0x1000000 or random.nextInt(0xFFFFFF)
        } while (color == -0x1) // Exclude white
        return color
    }

}