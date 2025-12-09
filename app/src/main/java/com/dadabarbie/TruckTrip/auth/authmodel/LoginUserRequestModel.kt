package com.dadabarbie.TruckTrip.auth.authmodel

import com.google.gson.annotations.SerializedName

data class LoginUserRequestModel(
    @SerializedName("fcm_token")
    val fcm_token: String,
    @SerializedName("mobile")
    val mobile: String,
    @SerializedName("id_token")
    val id_token:String,
    @SerializedName("lang")
    val lang:String
)