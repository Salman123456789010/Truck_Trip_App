package com.dadabarbie.TruckTrip.auth.authmodel

import com.google.gson.annotations.SerializedName

data class LoginUserTokenResponse(
    @SerializedName("token")
    val token: String
)