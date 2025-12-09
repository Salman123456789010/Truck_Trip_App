package com.dadabarbie.TruckTrip.languagemodel

import com.google.gson.annotations.SerializedName

data class Language(
    val label: String,
    val locale_code: String,
    var flag:Boolean=false,
    var english_label: String,
)