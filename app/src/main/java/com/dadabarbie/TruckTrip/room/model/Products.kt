package com.vasyerp.cafvd.room.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.dadabarbie.TruckTrip.room.Converter


@Entity(tableName = "products")
data class Products(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val truckNumber: String,
    val srcPlace: String,
    val destPlace: String,
    val srcDate: String,
    val destDate: String,
    val avg: String,
    val  randomNumber:String,
    val modelList1: String, // We will store these lists as JSON strings
    val modelList2: String
)
