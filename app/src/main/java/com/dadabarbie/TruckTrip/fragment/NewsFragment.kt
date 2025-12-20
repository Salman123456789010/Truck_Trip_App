package com.dadabarbie.TruckTrip.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.Constants
import com.dadabarbie.TruckTrip.Utils.Constants.gone
import com.dadabarbie.TruckTrip.Utils.Constants.visible
import com.dadabarbie.TruckTrip.activity.DashBoardActivity
import com.dadabarbie.TruckTrip.activity.NewsDetailsActivity
import com.dadabarbie.TruckTrip.adapter.NewsAdapter
import com.dadabarbie.TruckTrip.adapter.TripListAdapter
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.databinding.FragmentNewsBinding
import com.dadabarbie.TruckTrip.model.getTrip.Record
import com.dadabarbie.TruckTrip.model.news.NewsDetails
import com.dadabarbie.TruckTrip.model.news.NewsRecord
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.vasyerp.freshvegetables.util.NetworkResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


@AndroidEntryPoint
class NewsFragment : Fragment(),NewsAdapter.ClickNews {

    lateinit var binding: FragmentNewsBinding
    private val authViewModel: AuthViewModel by viewModels()
    private var newsList: ArrayList<NewsRecord> = arrayListOf()
    lateinit var newsAdapter: NewsAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding=FragmentNewsBinding.inflate(inflater, container, false)
        MobileAds.initialize(requireActivity())
        GlobalScope.launch {
            val adLoader =
                AdLoader.Builder(requireContext(), "ca-app-pub-8808039515208362/9048738516")
                    .forNativeAd { p0 ->
                        binding.myTemplate.setNativeAd(p0)
                    }
                    .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            (activity as DashBoardActivity).textChanges(3)
        }catch (e: Exception){

        }

//        initViews()
//        initAdapter()
//        setObserver()
    }

    private fun setObserver() {

    }


    private fun initAdapter() {

    }

    private fun initViews() {
        MobileAds.initialize(requireContext()) { }
        authViewModel.page_news=0
        newsList.clear()
        authViewModel.getAllNews()
    }

    private fun setProgressBarVisibility() {

    }



    override fun clickMethod(position: Int) {
//        startActivity(Intent(requireActivity(),NewsDetailsActivity::class.java)
//            .putExtra("tittle",newsList[position].title)
//            .putExtra("descreption",newsList[position].description)
//            .putExtra("image",newsList[position].image)
//            .putExtra("videoUrl",newsList[position].url)
//            .putExtra("newsTime",newsList[position].published)
//
//        )
//    }
    }
}