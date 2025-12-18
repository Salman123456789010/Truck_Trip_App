package com.dadabarbie.TruckTrip.model

data class DebitModel(
    var desc: String,
    var amount: String,
    var note: String,
    var place: String,
    var date: String,
    var type: String,
    var liters: String = "",
    var km: String = "",
    var isOdometerMode: Boolean = true  // NEW: Track if KM is odometer reading or distance
)