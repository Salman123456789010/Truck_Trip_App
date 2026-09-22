package com.dadabarbie.TruckTrip.model.subscription

import com.google.gson.annotations.SerializedName

data class SubscriptionStatusResponseModel(
    @SerializedName("data")
    val data: SubscriptionData? = null,
    @SerializedName("message")
    val message: String? = null,
    @SerializedName("status")
    val status: Int? = null,
    @SerializedName("success")
    val success: Boolean = false
)
