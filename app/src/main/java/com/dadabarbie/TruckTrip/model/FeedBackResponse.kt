package com.dadabarbie.TruckTrip.model

import com.google.gson.annotations.SerializedName

data class FeedBackResponse(
    @SerializedName("feedback")
    val feedback: String,
    @SerializedName("message")
    val message: String,

)