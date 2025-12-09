package com.dadabarbie.TruckTrip.model.news

data class NewsDetails(
    val currentPage: Int,
    val pageSize: Int,
    val records: List<NewsRecord>,
    val totalPages: Int,
    val totalRecords: Int
)