package com.vasyerp.cafvd.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vasyerp.cafvd.room.model.TripRecordEntity

@Dao
interface TripRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(trips: List<TripRecordEntity>)

    @Query("SELECT * FROM trip_records ORDER BY start_date DESC")
    suspend fun getAll(): List<TripRecordEntity>

    @Query("SELECT COUNT(*) FROM trip_records")
    suspend fun getCount(): Int

    @Query("SELECT * FROM trip_records WHERE start_date >= :fromDate AND end_date <= :toDate ORDER BY start_date DESC")
    suspend fun getTripsInRange(fromDate: String, toDate: String): List<TripRecordEntity>

    @Query("DELETE FROM trip_records WHERE id = :id")
    suspend fun deleteById(id: String): Int  // Returns number of rows deleted

    @Query("DELETE FROM trip_records")
    suspend fun clearAll()

    // Add this for verification
    @Query("SELECT COUNT(*) FROM trip_records WHERE id = :id")
    suspend fun existsById(id: String): Int
}
