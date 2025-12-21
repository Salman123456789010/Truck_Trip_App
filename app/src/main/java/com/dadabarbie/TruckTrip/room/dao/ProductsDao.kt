package com.vasyerp.cafvd.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.TypeConverters
import androidx.room.Update
import com.dadabarbie.TruckTrip.room.Converter
import com.vasyerp.cafvd.room.model.Products


@Dao
interface ProductsDao {
    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<Products>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(products: Products)

    @Query("DELETE FROM products WHERE id = :truckId")
    suspend fun deleteTruckById(truckId: Int)


    @Query("UPDATE products SET truckNumber = :truckNumber, srcPlace = :srcPlace, destPlace = :destPlace, srcDate = :srcDate, destDate = :destDate, avg = :avg,modelList1=:modelList1,modelList2=:modelList2 WHERE randomNumber = :id")
    suspend fun update(
        id: String,
        truckNumber: String,
        srcPlace: String,
        destPlace: String,
        srcDate: String,
        destDate: String,
        avg: String,
        modelList1: String, // We will store these lists as JSON strings
        modelList2: String

    )

    @Query("SELECT * FROM products WHERE randomNumber = :randomNumber LIMIT 1")
    suspend fun getDraftById(randomNumber: String): Products?

    @Query("DELETE FROM products WHERE randomNumber = :tripId")
    suspend fun deleteDraftByTripId(tripId: String)

    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Products>)
}
