package com.dadabarbie.TruckTrip.model.versionModel

data class AppVersionModel(
    val `data`: AppVersionName,
    val message: String,
    val status: Int,
    val success: Boolean
)