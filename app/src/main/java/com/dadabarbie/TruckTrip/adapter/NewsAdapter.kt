package com.dadabarbie.TruckTrip.adapter

import android.R
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.dadabarbie.TruckTrip.Utils.Constants.gone
import com.dadabarbie.TruckTrip.Utils.Constants.visible
import com.dadabarbie.TruckTrip.databinding.ItemNewsLayoutBinding
import com.dadabarbie.TruckTrip.model.news.NewsRecord
import com.google.android.ads.nativetemplates.NativeTemplateStyle
import com.google.android.ads.nativetemplates.TemplateView
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class NewsAdapter(val context: Context,val clickNews: ClickNews):RecyclerView.Adapter<NewsAdapter.ViewHolder>() {


    private val diffCallBack = object : DiffUtil.ItemCallback<NewsRecord>() {
        override fun areItemsTheSame(
            oldItem: NewsRecord,
            newItem: NewsRecord
        ): Boolean {
            return oldItem._id == newItem._id
        }

        override fun areContentsTheSame(
            oldItem: NewsRecord,
            newItem: NewsRecord
        ): Boolean {
            return oldItem._id == newItem._id
        }
    }

    fun submitList(list: List<NewsRecord>) = differ.submitList(list)
    private val differ = AsyncListDiffer(this, diffCallBack)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsAdapter.ViewHolder {
        val binding: ItemNewsLayoutBinding =
            ItemNewsLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return NewsAdapter.ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NewsAdapter.ViewHolder, position: Int) {
        val item = differ.currentList[position]
        holder.binding.newsHeader.text=item.title.toString()
        holder.binding.newsDetails.text=item.description.toString()
        Glide.with(context).load(item.image).into(holder.binding.imagNews)
        holder.binding.auther.text="Author : ${item.author}"
        holder.binding.itemNewsLayout.setOnClickListener{
            clickNews.clickMethod(position)
        }
//        if(position%3 ==0){
//            GlobalScope.launch {
//                MobileAds.initialize(context)
//                val adLoader = AdLoader.Builder(context, "ca-app-pub-3940256099942544/2247696110")
//                    .forNativeAd { p0 ->
//                        holder.binding.myTemplate.setNativeAd(p0)
//                    }
//                    .build()
//
//                adLoader.loadAd(AdRequest.Builder().build())
//            }
//            holder.binding.myTemplate.visible()
//        }else{
//            holder.binding.myTemplate.gone()
//        }





    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
    class ViewHolder(var binding: ItemNewsLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
    }
    interface ClickNews{
        fun clickMethod(position: Int)
    }
}