package com.dadabarbie.TruckTrip.model.addTrip

data class Expense(
    val id: Long = System.currentTimeMillis(), // ✅ UNIQUE ID
    var desc: String,
    var amount: String,
    var note: String,
    var place: String,
    var date: String,
    var type: String,
    var liters: String = "",
    var km: String = "",
    var isOdometerMode: Boolean = true
)
