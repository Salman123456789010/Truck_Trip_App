package com.dadabarbie.TruckTrip.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.activity.DashBoardActivity
import com.dadabarbie.TruckTrip.activity.NormalUserDashBoard
import com.dadabarbie.TruckTrip.activity.VahanInfoDetailsActivity
import com.dadabarbie.TruckTrip.ads.AdConfig
import com.dadabarbie.TruckTrip.ads.AdMobManager
import com.dadabarbie.TruckTrip.databinding.FragmentVahanInfoBinding
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VahanInfoFragment : Fragment() {

    private var _binding: FragmentVahanInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVahanInfoBinding.inflate(inflater, container, false)
        setOnClickListeners()
        loadNativeAdIfAllowed()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            (activity as? DashBoardActivity)?.textChanges(2)
        } catch (e: Exception) {}
        try {
            (activity as? NormalUserDashBoard)?.textChanges(2)
        } catch (e: Exception) {}
    }

    private fun loadNativeAdIfAllowed() {
        AdMobManager.loadNativeAd(
            requireActivity(),
            binding.myTemplate,
            AdMobManager.NativePlacement.TOOLS_ADVANCED
        )
    }

    private fun setOnClickListeners() {
        binding.dieselInfo.setOnClickListener {
            if (com.dadabarbie.TruckTrip.billing.SubscriptionManager.isFeatureLocked(com.dadabarbie.TruckTrip.billing.SubscriptionManager.PremiumFeature.DIESEL_PRICES)) {
                com.dadabarbie.TruckTrip.dialog.PremiumPaywallDialog.show(
                    requireActivity(),
                    com.dadabarbie.TruckTrip.billing.SubscriptionManager.PremiumFeature.DIESEL_PRICES
                ) {
                    startActivity(Intent(requireContext(), VahanInfoDetailsActivity::class.java))
                }
            } else {
                startActivity(Intent(requireContext(), VahanInfoDetailsActivity::class.java))
            }
        }

        binding.fuelStopPlanner.setOnClickListener {
            if (com.dadabarbie.TruckTrip.billing.SubscriptionManager.isFeatureLocked(com.dadabarbie.TruckTrip.billing.SubscriptionManager.PremiumFeature.FUEL_STOP_PLANNER)) {
                com.dadabarbie.TruckTrip.dialog.PremiumPaywallDialog.show(
                    requireActivity(),
                    com.dadabarbie.TruckTrip.billing.SubscriptionManager.PremiumFeature.FUEL_STOP_PLANNER
                ) {
                    startActivity(Intent(requireContext(), com.dadabarbie.TruckTrip.activity.FuelStopPlannerActivity::class.java))
                }
            } else {
                startActivity(Intent(requireContext(), com.dadabarbie.TruckTrip.activity.FuelStopPlannerActivity::class.java))
            }
        }

        binding.tripReport.setOnClickListener {
            if (com.dadabarbie.TruckTrip.billing.SubscriptionManager.isFeatureLocked(com.dadabarbie.TruckTrip.billing.SubscriptionManager.PremiumFeature.DETAILED_REPORTS)) {
                com.dadabarbie.TruckTrip.dialog.PremiumPaywallDialog.show(
                    requireActivity(),
                    com.dadabarbie.TruckTrip.billing.SubscriptionManager.PremiumFeature.DETAILED_REPORTS
                ) {
                    val bottomSheet = TripReportBottomSheetFragment.newInstance()
                    bottomSheet.show(childFragmentManager, TripReportBottomSheetFragment.TAG)
                }
            } else {
                val bottomSheet = TripReportBottomSheetFragment.newInstance()
                bottomSheet.show(childFragmentManager, TripReportBottomSheetFragment.TAG)
            }
        }

        binding.tripEstimator.setOnClickListener {
            startActivity(Intent(requireContext(), com.dadabarbie.TruckTrip.activity.TripEstimatorActivity::class.java))
        }

        binding.loadCapacityCalculator.setOnClickListener {
            startActivity(Intent(requireContext(), com.dadabarbie.TruckTrip.activity.LoadCapacityCalculatorActivity::class.java))
        }

        binding.backhaulCalculator.setOnClickListener {
            if (com.dadabarbie.TruckTrip.billing.SubscriptionManager.isFeatureLocked(com.dadabarbie.TruckTrip.billing.SubscriptionManager.PremiumFeature.BACKHAUL_CALCULATOR)) {
                com.dadabarbie.TruckTrip.dialog.PremiumPaywallDialog.show(
                    requireActivity(),
                    com.dadabarbie.TruckTrip.billing.SubscriptionManager.PremiumFeature.BACKHAUL_CALCULATOR
                ) {
                    startActivity(Intent(requireContext(), com.dadabarbie.TruckTrip.activity.BackhaulCalculatorActivity::class.java))
                }
            } else {
                startActivity(Intent(requireContext(), com.dadabarbie.TruckTrip.activity.BackhaulCalculatorActivity::class.java))
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
