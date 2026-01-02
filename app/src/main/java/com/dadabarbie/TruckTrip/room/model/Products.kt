package com.vasyerp.cafvd.room.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.dadabarbie.TruckTrip.room.Converter


@Entity(tableName = "products")
data class Products(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val truckNumber: String,

    @ColumnInfo(name = "srcPlace")
    val srcPlace: String,

    @ColumnInfo(name = "destPlace")
    val destPlace: String,

    @ColumnInfo(name = "srcDate")
    val srcDate: String,

    @ColumnInfo(name = "destDate")
    val destDate: String,

    @ColumnInfo(name = "avg")
    val avg: String,

    val  randomNumber:String,
    @ColumnInfo(name = "modelList1")
    val modelList1: String,

    @ColumnInfo(name = "modelList2")
    val modelList2: String,

    @ColumnInfo(name = "routeJson")
    val routeJson: String = ""  ,// Add this field

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis()
)
