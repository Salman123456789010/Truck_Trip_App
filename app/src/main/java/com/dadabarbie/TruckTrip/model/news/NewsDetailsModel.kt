package com.dadabarbie.TruckTrip.model.news

data class NewsDetailsModel(
    val data: NewsDetails,
    val message: String,
    val status: Int,
    val success: Boolean
)