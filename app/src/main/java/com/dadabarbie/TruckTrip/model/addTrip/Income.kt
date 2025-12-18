package com.dadabarbie.TruckTrip.model.addTrip

data class Income(
    var desc: String,
    var amount: String="0",
    var note: String = "",
    var place: String = "",
    var date: String = ""
)