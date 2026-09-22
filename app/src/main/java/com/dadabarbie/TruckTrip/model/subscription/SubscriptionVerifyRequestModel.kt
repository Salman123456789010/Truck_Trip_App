package com.dadabarbie.TruckTrip.model.subscription

import com.google.gson.annotations.SerializedName

data class SubscriptionVerifyRequestModel(
    @SerializedName("userId")
    val userId: String,
    @SerializedName("productId")
    val productId: String,
    @SerializedName("purchaseToken")
    val purchaseToken: String,
    @SerializedName("orderId")
    val orderId: String? = null,
    @SerializedName("purchaseTime")
    val purchaseTime: Long? = null
)
