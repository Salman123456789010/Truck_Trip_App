package com.dadabarbie.TruckTrip.room.model

import com.dadabarbie.TruckTrip.model.CreditModel
import com.dadabarbie.TruckTrip.model.DebitModel
import com.dadabarbie.TruckTrip.model.addTrip.Expense
import com.dadabarbie.TruckTrip.model.addTrip.Income

data class TripDataTestModel(
    val truckNumber: String="",
    val srcPlace: String="",
    val destPlace: String="",
    val srcDate: String="",
    val destDate: String="",
    val avg: String="",
    var randomNumber:String="",
    var driverIncome:String="",
    val modelList1: List<Income> = arrayListOf(),
    val modelList2: List<Expense> = arrayListOf(),
    var id:String=""

)