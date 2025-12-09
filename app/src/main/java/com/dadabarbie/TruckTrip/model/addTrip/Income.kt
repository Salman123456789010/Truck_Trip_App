package com.dadabarbie.TruckTrip.model.addTrip

data class Income(
    var desc: String,
    var amount: String,
    var totalIncome: String = "",
    var advanceTaken: String = "",
    var balance: String = "",
    var note: String = "",
    var place: String = "",
    var date: String = ""
)