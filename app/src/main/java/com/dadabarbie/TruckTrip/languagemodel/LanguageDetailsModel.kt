package com.dadabarbie.TruckTrip.languagemodel

import com.google.gson.annotations.SerializedName

data class LanguageDetailsModel(
    @SerializedName("languages")
    val languages: List<Language>
)