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

    @Query("DELETE FROM products WHERE id = :truckId")
    suspend fun deleteTruckById(truckId: Int)

    @Query("UPDATE products SET truckNumber = :truckNumber, srcPlace = :srcPlace, destPlace = :destPlace, srcDate = :srcDate, destDate = :destDate, avg = :avg, modelList1 = :modelList1, modelList2 = :modelList2, routeJson = :routeJson ,updatedAt = :updatedAt WHERE randomNumber = :randomNumber")
    suspend fun update(
        randomNumber: String,
        truckNumber: String,
        srcPlace: String,
        destPlace: String,
        srcDate: String,
        destDate: String,
        avg: String,
        modelList1: String,
        modelList2: String,
        routeJson: String = "",
        updatedAt: Long

    )

    @Query("SELECT * FROM products WHERE randomNumber = :id LIMIT 1")
    suspend fun getDraftById(id: String): Products?

    @Query("SELECT * FROM products ORDER BY updatedAt DESC")
    suspend fun getAllProducts(): List<Products>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Products)

    @Query("DELETE FROM products WHERE randomNumber = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM products WHERE randomNumber = :tripId")
    suspend fun deleteDraftByTripId(tripId: String)

    @Query("DELETE FROM products WHERE randomNumber = :id")
    suspend fun deleteByRandomNumber(id: String)


    @Query("DELETE FROM products")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Products>)


}
