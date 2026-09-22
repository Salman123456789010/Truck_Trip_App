package com.dadabarbie.TruckTrip.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dadabarbie.TruckTrip.activity.DashBoardActivity
import com.dadabarbie.TruckTrip.activity.NormalUserDashBoard
import com.dadabarbie.TruckTrip.adapter.NewsAdapter
import com.dadabarbie.TruckTrip.ads.AdConfig
import com.dadabarbie.TruckTrip.billing.BillingManager
import com.dadabarbie.TruckTrip.databinding.FragmentNewsBinding
import com.dadabarbie.TruckTrip.model.news.NewsRecord
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NewsFragment : Fragment(), NewsAdapter.ClickNews {

    private var _binding: FragmentNewsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsBinding.inflate(inflater, container, false)
        loadNativeAdIfAllowed()
        return binding.root
    }

    private fun loadNativeAdIfAllowed() {
        com.dadabarbie.TruckTrip.ads.AdMobManager.loadNativeAd(
            requireActivity(),
            binding.myTemplate,
            com.dadabarbie.TruckTrip.ads.AdMobManager.NativePlacement.NEWS_LIST
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            (activity as? DashBoardActivity)?.textChanges(3)
        } catch (e: Exception) {}
        try {
            (activity as? NormalUserDashBoard)?.textChanges(3)
        } catch (e: Exception) {}
    }

    override fun clickMethod(position: Int) {
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}