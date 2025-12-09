package com.dadabarbie.TruckTrip.languagemodel

import com.google.gson.annotations.SerializedName

data class LanguageResponseModel(
    @SerializedName("data")
    val data: LanguageDetailsModel,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("success")
    val success: Boolean
)