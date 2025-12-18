package com.dadabarbie.TruckTrip.room.model

import com.dadabarbie.TruckTrip.model.CreditModel
import com.dadabarbie.TruckTrip.model.DebitModel
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.dadabarbie.TruckTrip.model.addTrip.Income

data class TripDataTestModel(
    val truckNumber: String,
    val srcPlace: String,
    val destPlace: String,
    val srcDate: String,
    val destDate: String,
    val avg: String,
    val randomNumber: String,
    val driverIncome: String = "",
    val modelList1: List<Income>,
    val modelList2: List<Expense>,
    val startOdometer: String = "",  // NEW: Start odometer reading
    val endOdometer: String = "",    // NEW: End odometer reading
    val endTripKm: String = "",
    val id: String=""// NEW: Manual KM if not using odometer
)