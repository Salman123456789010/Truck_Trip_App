package com.vasyerp.cafvd.room.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey val id: String,
    val createdDate: String,
    val updatedDate: String,
    val destination: String,
    val driver_income: String,
    val end_date: String,
    val source: String,
    val start_date: String,
    val total_days: String,
    val total_expense: String,
    val total_income: String,
    val truck_average: String,
    val truck_no: String,
    val owner_profit: String,
    val mobile: String,
    val income_json: String,
    val expense_json: String
)

