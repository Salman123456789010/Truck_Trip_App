package com.dadabarbie.TruckTrip.model.getTrip

import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.dadabarbie.TruckTrip.model.addTrip.Income

data class Record(
    val __v: Int,
    val _id: String,
    val createdDate: String,
    val destination: String,
    val driver_income: String,
    val end_date: String,
    val expense: List<Expense>,
    val income: List<Income>,
    val mobile: String,
    val owner_profit: String,
    val source: String,
    val start_date: String,
    val total_days: String,
    val total_expense: String,
    val total_income: String,
    val truck_average: String,
    val truck_no: String,
    val updatedDate: String
)