package com.dadabarbie.TruckTrip.model

import com.google.gson.annotations.SerializedName

data class FeedBackRequest(
    @SerializedName("feedback")
    val feedback: String,
    @SerializedName("message")
    val message: String,

)