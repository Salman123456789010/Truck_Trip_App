package com.dadabarbie.TruckTrip.auth.authmodel

import com.google.gson.annotations.SerializedName

data class LoginUserResponseModel(
    @SerializedName("data")
    val data: LoginUserTokenResponse,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Int,
    @SerializedName("success")
    val success: Boolean
)