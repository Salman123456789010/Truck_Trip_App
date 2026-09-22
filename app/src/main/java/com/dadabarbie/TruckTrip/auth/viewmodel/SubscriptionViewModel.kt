package com.dadabarbie.TruckTrip.auth.viewmodel

import android.app.Application
import android.preference.PreferenceManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dadabarbie.TruckTrip.billing.BillingManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class SubscriptionViewModel(application: Application) : AndroidViewModel(application) {

//    private val repository = SubscriptionRepository()
//    private val prefManager = PreferenceManager(application)
//    private val billingManager = BillingManager(application, viewModelScope)
//
//    private val _uiState = MutableStateFlow(UiState())
//    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
//
//    val billingState: StateFlow<BillingState> = billingManager.billingState
//
//    init {
//        startBilling()
//        observeBillingState()
//        loadLocalSubscriptionStatus()
//    }
//
//    // ─── Initialize Billing ───────────────────────────────────────────────────
//
//    private fun startBilling() {
//        billingManager.startConnection()
//    }
//
//    private fun loadLocalSubscriptionStatus() {
//        viewModelScope.launch {
//            prefManager.isSubscribed.collect { isActive ->
//                _uiState.update { it.copy(isSubscribed = isActive) }
//            }
//        }
//    }
//
//    // ─── Observe Billing Events ───────────────────────────────────────────────
//
//    private fun observeBillingState() {
//        viewModelScope.launch {
//            billingState.collect { state ->
//                when (state) {
//                    is BillingState.Idle -> {
//                        _uiState.update { it.copy(isLoading = false) }
//                    }
//                    is BillingState.Loading -> {
//                        _uiState.update { it.copy(isLoading = true) }
//                    }
//                    is BillingState.ProductLoaded -> {
//                        val price = state.productDetails
//                            .subscriptionOfferDetails
//                            ?.firstOrNull()
//                            ?.pricingPhases
//                            ?.pricingPhaseList
//                            ?.firstOrNull()
//                            ?.formattedPrice ?: "N/A"
//
//                        _uiState.update {
//                            it.copy(isLoading = false, price = price)
//                        }
//                    }
//                    is BillingState.PurchaseSuccess -> {
//                        verifyAndActivate(state.purchase)
//                    }
//                    is BillingState.AlreadySubscribed -> {
//                        _uiState.update {
//                            it.copy(isLoading = false, isSubscribed = true)
//                        }
//                    }
//                    is BillingState.Error -> {
//                        _uiState.update {
//                            it.copy(isLoading = false, errorMessage = state.message)
//                        }
//                    }
//                    is BillingState.BillingNotAvailable -> {
//                        _uiState.update {
//                            it.copy(
//                                isLoading = false,
//                                errorMessage = "Google Play Billing is not available on this device"
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    // ─── Launch Purchase ──────────────────────────────────────────────────────
//
//    fun subscribe(activity: Activity) {
//        billingManager.launchPurchaseFlow(activity)
//    }
//
//    // ─── Verify with Backend ──────────────────────────────────────────────────
//
//    private fun verifyAndActivate(purchase: Purchase) {
//        viewModelScope.launch {
//            _uiState.update { it.copy(isLoading = true) }
//
//            val token = prefManager.userToken.first()
//            // ⚠️ Replace with how you get your userId
//            val userId = "USER_ID_FROM_SESSION"
//
//            when (val result = repository.verifyPurchase(token, userId, purchase)) {
//                is Result.Success -> {
//                    if (result.data.isActive) {
//                        prefManager.saveSubscriptionStatus(true, result.data.expiryDate)
//                        _uiState.update {
//                            it.copy(
//                                isLoading = false,
//                                isSubscribed = true,
//                                expiryDate = result.data.expiryDate ?: "",
//                                successMessage = "🎉 Subscription activated successfully!"
//                            )
//                        }
//                    } else {
//                        _uiState.update {
//                            it.copy(
//                                isLoading = false,
//                                errorMessage = "Purchase could not be verified. Contact support."
//                            )
//                        }
//                    }
//                }
//                is Result.Error -> {
//                    Log.e("SubscriptionVM", result.message)
//                    // Even if backend fails, Google billing succeeded — save locally
//                    prefManager.saveSubscriptionStatus(true, null)
//                    _uiState.update {
//                        it.copy(
//                            isLoading = false,
//                            isSubscribed = true,
//                            successMessage = "Subscribed! Syncing with server in background..."
//                        )
//                    }
//                }
//            }
//        }
//    }
//
//    // ─── Refresh Status from Backend ─────────────────────────────────────────
//
//    fun refreshSubscriptionStatus() {
//        viewModelScope.launch {
//            val token = prefManager.userToken.first()
//            if (token.isEmpty()) return@launch
//
//            when (val result = repository.getSubscriptionStatus(token)) {
//                is Result.Success -> {
//                    prefManager.saveSubscriptionStatus(
//                        result.data.isActive,
//                        result.data.expiryDate
//                    )
//                    _uiState.update {
//                        it.copy(
//                            isSubscribed = result.data.isActive,
//                            expiryDate = result.data.expiryDate ?: ""
//                        )
//                    }
//                }
//                is Result.Error -> {
//                    Log.e("SubscriptionVM", "Refresh failed: ${result.message}")
//                }
//            }
//        }
//    }
//
//    fun clearError() {
//        _uiState.update { it.copy(errorMessage = null) }
//    }
//
//    fun clearSuccess() {
//        _uiState.update { it.copy(successMessage = null) }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        billingManager.disconnect()
//    }
}
