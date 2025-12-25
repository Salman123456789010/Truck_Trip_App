package com.dadabarbie.TruckTrip.fragment

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.activity.DashBoardActivity
import com.dadabarbie.TruckTrip.activity.NormalUserDashBoard
import com.dadabarbie.TruckTrip.activity.VahanInfoDetailsActivity
import com.dadabarbie.TruckTrip.databinding.FragmentVahanInfoBinding
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class VahanInfoFragment : Fragment() {
    lateinit var binding:FragmentVahanInfoBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment


        binding= FragmentVahanInfoBinding.inflate(inflater,container,false)
        setOnClickListner()
//        MobileAds.initialize(requireActivity())
//        GlobalScope.launch {
//            val adLoader =
//                AdLoader.Builder(requireContext(), "ca-app-pub-3940256099942544/2247696110")
//                    .forNativeAd { p0 ->
//                        binding.myTemplate.setNativeAd(p0)
//                    }
//                    .build()
//
//            adLoader.loadAd(AdRequest.Builder().build())
//        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            (activity as DashBoardActivity).textChanges(2)
        }catch (e: Exception){}
        try {
            (activity as NormalUserDashBoard).textChanges(2)
        }catch (e: Exception){}
    }

    private fun setOnClickListner() {
//        binding.searchBtn.setOnClickListener(this)
    }




}