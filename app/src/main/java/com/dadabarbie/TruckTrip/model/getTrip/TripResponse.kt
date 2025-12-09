package com.dadabarbie.TruckTrip.model.getTrip

import com.google.gson.annotations.SerializedName

data class TripResponse(
    @SerializedName("currentPage")
    val currentPage: Int,
    @SerializedName("pageSize")
    val pageSize: Int,
    @SerializedName("records")
    val records: List<Record>,
    @SerializedName("totalPages")
    val totalPages: Int,
    @SerializedName("totalRecords")
    val totalRecords: Int,
    @SerializedName("totalDriverIncome")
    val totalDriverIncome:Int,
    @SerializedName("totalOwnerIncome")
    val totalOwnerIncome:Int
)