package com.dadabarbie.TruckTrip.model.fuel

import com.google.gson.annotations.SerializedName

data class FuelPriceResponseModel(
    val success: Boolean,
    val status: Int,
    val data: FuelPriceData?,
    val message: String?
)

data class FuelPriceData(
    @SerializedName("currentPage")
    val currentPage: Int,
    @SerializedName("totalPages")
    val totalPages: Int,
    @SerializedName("pageSize")
    val pageSize: Int,
    @SerializedName("totalRecords")
    val totalRecords: Int,
    @SerializedName("records")
    val records: List<FuelCityRecord>
)

data class FuelCityRecord(
    @SerializedName("_id")
    val id: String,
    @SerializedName("city")
    val city: String,
    @SerializedName("state")
    val state: String,
    @SerializedName("nearby_cities")
    val nearbyCities: List<NearbyCity>,
    @SerializedName("diesel")
    val diesel: Double,
    @SerializedName("lpg")
    val lpg: Double,
    @SerializedName("petrol")
    val petrol: Double,
    @SerializedName("updatedAt")
    val updatedAt: String
)

data class NearbyCity(
    @SerializedName("city")
    val city: String,
    @SerializedName("km")
    val km: Int
)

