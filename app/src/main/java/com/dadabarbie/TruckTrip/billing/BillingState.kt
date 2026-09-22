package com.dadabarbie.TruckTrip.billing




sealed class BillingState {
    object Idle : BillingState()
    object Loading : BillingState()
//    data class ProductLoaded(val productDetails: ProductDetails) : BillingState()
//    data class PurchaseSuccess(val purchase: Purchase) : BillingState()
//    data class AlreadySubscribed(val purchase: Purchase) : BillingState()
    data class Error(val message: String) : BillingState()
    object BillingNotAvailable : BillingState()
}