package com.dadabarbie.TruckTrip.model.news

import retrofit2.http.Query

data class NewsGetRequestModel(
    val page: Int = 1,
    val size: Int
)