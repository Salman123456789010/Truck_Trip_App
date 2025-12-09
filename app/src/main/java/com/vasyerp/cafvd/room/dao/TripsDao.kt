package com.vasyerp.cafvd.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vasyerp.cafvd.room.model.TripEntity

@Dao
interface TripsDao {
    @Query("SELECT * FROM trips WHERE start_date >= :fromDate AND end_date <= :toDate ORDER BY createdDate DESC")
    suspend fun getTripsByRange(fromDate: String, toDate: String): List<TripEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(trips: List<TripEntity>)

    @Query("DELETE FROM trips")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun count(): Int
}

