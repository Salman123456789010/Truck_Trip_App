package com.dadabarbie.TruckTrip.activity

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.dadabarbie.TruckTrip.R
import com.dadabarbie.TruckTrip.Utils.SystemUiUtils
import com.dadabarbie.TruckTrip.auth.viewmodel.AuthViewModel
import com.dadabarbie.TruckTrip.billing.BillingManager
import com.dadabarbie.TruckTrip.billing.SubscriptionManager
import com.dadabarbie.TruckTrip.databinding.ActivitySubscriptionBinding
import com.vasyerp.freshvegetables.util.NetworkResult
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SubscriptionActivity : BaseActivity() {

    private val binding: ActivitySubscriptionBinding by lazy {
        ActivitySubscriptionBinding.inflate(layoutInflater)
    }

    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        SystemUiUtils.setupStatusBar(this, R.color.color_primary, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            enableEdgeToEdge()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        billingManager = BillingManager.getInstance(this)
        billingManager.startConnection()

        setupUI()
        observeBillingAndAuth()
        updateUIState()
    }

    private fun setupUI() {
        binding.closeBtn.setOnClickListener {
            finish()
        }

        binding.btnRestore.setOnClickListener {
            binding.layoutLoading.visibility = View.VISIBLE
            binding.tvLoadingText.text = getString(R.string.restoring_purchases)
            billingManager.queryPurchases { purchases ->
                if (purchases.isEmpty()) {
                    // Check backend status if Play Store cache was empty
                    authViewModel.checkSubscriptionStatus()
                }
            }
        }

        binding.btnSubscribe.setOnClickListener {
            if (SubscriptionManager.isPremium()) {
                Toast.makeText(this, getString(R.string.already_premium_user), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.layoutLoading.visibility = View.VISIBLE
            binding.tvLoadingText.text = getString(R.string.connecting_google_play)
            billingManager.launchPurchaseFlow(this)
        }
    }

    private fun updateUIState() {
        val isPremium = SubscriptionManager.isPremium()
        if (isPremium) {
            binding.tvStatusBadge.text = getString(R.string.status_premium_active)
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_gradient_green_circle)
            binding.tvStatusBadge.setTextColor(resources.getColor(R.color.white, theme))
            binding.btnSubscribe.text = getString(R.string.premium_active_button)
            binding.btnSubscribe.isEnabled = false
        } else {
            binding.tvStatusBadge.text = getString(R.string.status_free_plan)
            binding.tvStatusBadge.setBackgroundResource(R.drawable.bg_summary_badge)
            binding.tvStatusBadge.setTextColor(resources.getColor(R.color.brand_primary_dark, theme))
            binding.btnSubscribe.isEnabled = true
        }
    }

    private fun observeBillingAndAuth() {
        billingManager.formattedPrice.observe(this) { priceText ->
            binding.tvPrice.text = priceText
            if (!SubscriptionManager.isPremium()) {
                binding.btnSubscribe.text = getString(R.string.subscribe_dynamic_cta, priceText)
            }
        }

        billingManager.onPurchaseCompleted = { purchaseToken, productId, orderId, purchaseTime ->
            binding.layoutLoading.visibility = View.VISIBLE
            binding.tvLoadingText.text = getString(R.string.verifying_subscription)

            val userId = SubscriptionManager.getUserId()
            authViewModel.verifySubscription(
                userId = userId,
                productId = productId,
                purchaseToken = purchaseToken,
                orderId = orderId,
                purchaseTime = purchaseTime
            )
        }

        billingManager.onPurchaseCancelled = {
            binding.layoutLoading.visibility = View.GONE
        }

        billingManager.onPurchaseError = { errorMsg ->
            binding.layoutLoading.visibility = View.GONE
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        }

        authViewModel.subscriptionVerifyResult.observe(this) { event ->
            event.getContentIfNotHandled()?.let { result ->
                binding.layoutLoading.visibility = View.GONE
                when (result) {
                    is NetworkResult.Success -> {
                        if (result.data?.data?.isPremium == true) {
                            Toast.makeText(this, getString(R.string.premium_unlocked_welcome), Toast.LENGTH_LONG).show()
                            updateUIState()
                        } else {
                            val msg = result.data?.message ?: getString(R.string.subscription_verification_failed)
                            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                    is NetworkResult.Error -> {
                        Toast.makeText(this, result.message ?: "Verification error", Toast.LENGTH_LONG).show()
                    }
                    is NetworkResult.Loading -> {
                        binding.layoutLoading.visibility = View.VISIBLE
                    }
                }
            }
        }

        authViewModel.subscriptionStatusResult.observe(this) { event ->
            event.getContentIfNotHandled()?.let { result ->
                binding.layoutLoading.visibility = View.GONE
                when (result) {
                    is NetworkResult.Success -> {
                        val isPremium = result.data?.data?.isPremium == true
                        updateUIState()
                        if (isPremium) {
                            Toast.makeText(this, getString(R.string.subscription_restored_success), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, getString(R.string.no_active_subscription_found), Toast.LENGTH_SHORT).show()
                        }
                    }
                    is NetworkResult.Error -> {
                        Toast.makeText(this, result.message ?: "Could not check status", Toast.LENGTH_SHORT).show()
                    }
                    is NetworkResult.Loading -> {
                        binding.layoutLoading.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUIState()
        billingManager.queryPurchases()
    }
}