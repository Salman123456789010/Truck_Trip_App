package com.dadabarbie.TruckTrip.dialog

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.ads.AdMobManager
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.billing.BillingManager
import com.dadabarbie.TruckTrip.billing.SubscriptionManager
import com.dadabarbie.TruckTrip.databinding.DialogPremiumPaywallBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vasyerp.freshvegetables.util.NetworkResult

class PremiumPaywallDialog : BottomSheetDialogFragment() {

    private var _binding: DialogPremiumPaywallBinding? = null
    private val binding get() = _binding!!

    private var targetFeature: SubscriptionManager.PremiumFeature? = null
    private var onUnlockedCallback: (() -> Unit)? = null

    private lateinit var billingManager: BillingManager
    private var authViewModel: AuthViewModel? = null

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onStart() {
        super.onStart()
        val bottomSheetDialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.skipCollapsed = true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPremiumPaywallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity()
        billingManager = BillingManager.getInstance(activity)
        billingManager.startConnection()

        if (activity is FragmentActivity) {
            authViewModel = ViewModelProvider(activity)[AuthViewModel::class.java]
        }

        // Preload rewarded ad for instant response if user picks "Watch Ad"
        AdMobManager.preloadRewardedAd(requireContext())

        setupUI()
        observeBillingAndAuth()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnMaybeLater.setOnClickListener { dismiss() }

        // Contextualize subtitle and watch ad button based on target feature or trip limit
        targetFeature?.let { feature ->
            when (feature) {
                SubscriptionManager.PremiumFeature.DIESEL_PRICES -> {
                    binding.tvPaywallSubtitle.text = getString(R.string.unlock_diesel_prices_subtitle)
                    binding.btnWatchAd.text = getString(R.string.watch_ad_to_unlock_tool)
                }
                SubscriptionManager.PremiumFeature.FUEL_STOP_PLANNER -> {
                    binding.tvPaywallSubtitle.text = getString(R.string.unlock_fuel_planner_subtitle)
                    binding.btnWatchAd.text = getString(R.string.watch_ad_to_unlock_tool)
                }
                SubscriptionManager.PremiumFeature.BACKHAUL_CALCULATOR -> {
                    binding.tvPaywallSubtitle.text = getString(R.string.unlock_backhaul_subtitle)
                    binding.btnWatchAd.text = getString(R.string.watch_ad_to_unlock_tool)
                }
                SubscriptionManager.PremiumFeature.DETAILED_REPORTS,
                SubscriptionManager.PremiumFeature.ADVANCED_ANALYTICS -> {
                    binding.tvPaywallSubtitle.text = getString(R.string.unlock_reports_subtitle)
                    binding.btnWatchAd.text = getString(R.string.watch_ad_to_unlock_tool)
                }
                SubscriptionManager.PremiumFeature.UNLIMITED_TRIPS -> {
                    binding.tvPaywallSubtitle.text = getString(R.string.trip_limit_reached_subtitle)
                    binding.btnWatchAd.text = getString(R.string.watch_ad_for_5_trips)
                }
            }
        } ?: run {
            binding.btnWatchAd.text = getString(R.string.watch_ad_for_5_trips)
        }

        // Primary Subscription Flow
        binding.btnSubscribe.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            binding.layoutPaywallLoading.visibility = View.VISIBLE
            binding.tvPaywallLoadingText.text = getString(R.string.connecting_google_play)
            binding.btnSubscribe.isEnabled = false
            binding.btnWatchAd.isEnabled = false

            billingManager.launchPurchaseFlow(act)
        }

        // Rewarded Ad Option Flow
        binding.btnWatchAd.setOnClickListener {
            val act = activity ?: return@setOnClickListener
            AdMobManager.showRewardedAd(
                act,
                onRewardEarned = {
                    // Granted after ad is completely finished and closed
                    targetFeature?.let { feat ->
                        SubscriptionManager.grantTemporaryFeatureAccess(feat)
                        Toast.makeText(requireContext(), getString(R.string.feature_unlocked_success), Toast.LENGTH_SHORT).show()
                    } ?: run {
                        SubscriptionManager.addBonusTrips(5)
                        Toast.makeText(requireContext(), getString(R.string.bonus_trips_unlocked_msg, 5), Toast.LENGTH_SHORT).show()
                    }
                    onUnlockedCallback?.invoke()
                    dismiss()
                },
                onAdFailedOrClosed = { reason ->
                    if (reason.contains("before earning", ignoreCase = true) || reason.contains("closed", ignoreCase = true)) {
                        Toast.makeText(requireContext(), getString(R.string.ad_not_finished_warning), Toast.LENGTH_SHORT).show()
                    } else {
                        // Ad network unavailable - grant temporary access gracefully
                        targetFeature?.let { feat ->
                            SubscriptionManager.grantTemporaryFeatureAccess(feat)
                        } ?: SubscriptionManager.addBonusTrips(5)
                        onUnlockedCallback?.invoke()
                        dismiss()
                    }
                }
            )
        }
    }

    private fun observeBillingAndAuth() {
        billingManager.formattedPrice.observe(viewLifecycleOwner) { priceText ->
            binding.btnSubscribe.text = getString(R.string.subscribe_dynamic_cta, priceText)
        }

        billingManager.onPurchaseCompleted = { purchaseToken, productId, orderId, purchaseTime ->
            binding.layoutPaywallLoading.visibility = View.VISIBLE
            binding.tvPaywallLoadingText.text = getString(R.string.verifying_subscription)
            binding.btnSubscribe.isEnabled = false
            binding.btnWatchAd.isEnabled = false

            val userId = SubscriptionManager.getUserId()
            authViewModel?.verifySubscription(
                userId = userId,
                productId = productId,
                purchaseToken = purchaseToken,
                orderId = orderId,
                purchaseTime = purchaseTime
            )
        }

        billingManager.onPurchaseCancelled = {
            binding.layoutPaywallLoading.visibility = View.GONE
            binding.btnSubscribe.isEnabled = true
            binding.btnWatchAd.isEnabled = true
        }

        billingManager.onPurchaseError = { errorMsg ->
            binding.layoutPaywallLoading.visibility = View.GONE
            binding.btnSubscribe.isEnabled = true
            binding.btnWatchAd.isEnabled = true
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
        }

        authViewModel?.subscriptionVerifyResult?.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { result ->
                binding.layoutPaywallLoading.visibility = View.GONE
                binding.btnSubscribe.isEnabled = true
                binding.btnWatchAd.isEnabled = true

                when (result) {
                    is NetworkResult.Success -> {
                        if (result.data?.data?.isPremium == true) {
                            Toast.makeText(requireContext(), getString(R.string.premium_unlocked_welcome), Toast.LENGTH_LONG).show()
                            onUnlockedCallback?.invoke()
                            dismiss()
                        } else {
                            val msg = result.data?.message ?: getString(R.string.subscription_verification_failed)
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        }
                    }
                    is NetworkResult.Error -> {
                        Toast.makeText(requireContext(), result.message ?: "Verification failed", Toast.LENGTH_LONG).show()
                    }
                    is NetworkResult.Loading -> {
                        binding.layoutPaywallLoading.visibility = View.VISIBLE
                        binding.tvPaywallLoadingText.text = getString(R.string.verifying_subscription)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PremiumPaywallDialog"

        fun show(
            activity: FragmentActivity,
            feature: SubscriptionManager.PremiumFeature? = null,
            onUnlocked: (() -> Unit)? = null
        ): PremiumPaywallDialog {
            val dialog = PremiumPaywallDialog().apply {
                this.targetFeature = feature
                this.onUnlockedCallback = onUnlocked
            }
            dialog.show(activity.supportFragmentManager, TAG)
            return dialog
        }
    }
}
