package com.dadabarbie.TruckTrip.room

import androidx.room.TypeConverter
import com.dadabarbie.TruckTrip.model.CreditModel
import com.dadabarbie.TruckTrip.model.DebitModel
import com.dadabarbie.TruckTrip.room.model.abcd
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


public class Converter {
    @TypeConverter
    fun fromCreditModelList(modelList: List<ArrayList<CreditModel>>): String {
        return Gson().toJson(modelList)
    }

    @TypeConverter
    fun toCreditModelList(modelListString: String): List<ArrayList<CreditModel>> {
        val listType = object : TypeToken<List<ArrayList<CreditModel>>>() {}.type
        return Gson().fromJson(modelListString, listType)
    }

    @TypeConverter
    fun fromDebitModelList(modelList: List<ArrayList<DebitModel>>): String {
        return Gson().toJson(modelList)
    }

    @TypeConverter
    fun toDebitModelList(modelListString: String): List<ArrayList<DebitModel>> {
        val listType = object : TypeToken<List<ArrayList<DebitModel>>>() {}.type
        return Gson().fromJson(modelListString, listType)
    }
}