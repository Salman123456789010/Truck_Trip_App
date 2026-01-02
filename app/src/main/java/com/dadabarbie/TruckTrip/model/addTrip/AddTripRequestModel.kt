package com.dadabarbie.TruckTrip.model.addTrip

data class AddTripRequestModel(
    val id:String,
    val destination: String,
    val driver_income: String,
    val end_date: String,
    val route: ArrayList<String> = arrayListOf(),
    val expense: List<Expense>,
    val income: List<Income>,
    val owner_profit: String,
    val source: String,
    val start_date: String,
    val total_days: String,
    val total_expense: String,
    val total_income: String,
    val truck_average: String,
    val truck_no: String,
    val startOdometer: String,
    val endOdometer: String,
    val isOdometer: Boolean,
    val endKm: String,

)