package com.dadabarbie.TruckTrip.model.getTrip

import com.google.gson.annotations.SerializedName

data class TripGetResponseModel(
    @SerializedName("data")
    val data: TripResponse,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("success")
    val success: Boolean
)