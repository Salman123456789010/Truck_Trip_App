package com.dadabarbie.TruckTrip.model.subscription

import com.google.gson.annotations.SerializedName

data class SubscriptionVerifyResponseModel(
    @SerializedName("data")
    val data: SubscriptionData? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("success")
    val success: Boolean = false
)

data class SubscriptionData(
    @SerializedName("isPremium")
    val isPremium: Boolean = false,
    @SerializedName("productId")
    val productId: String? = null,
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("expiryDate")
    val expiryDate: String? = null,
    @SerializedName("autoRenewEnabled")
    val autoRenewEnabled: Boolean = true,
    @SerializedName("orderId")
    val orderId: String? = null
)
