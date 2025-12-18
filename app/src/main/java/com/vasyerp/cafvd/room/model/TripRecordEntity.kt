package com.vasyerp.cafvd.room.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_records")
data class TripRecordEntity(
    @PrimaryKey val id: String,
    val source: String,
    val destination: String,
    val start_date: String,
    val end_date: String,
    val total_income: String,
    val total_expense: String,
    val driver_income: String,
    val owner_profit: String,
    val truck_average: String,
    val truck_no: String,
    val createdDate: String,
    val updatedDate: String,
    val incomeJson: String,
    val expenseJson: String
)
