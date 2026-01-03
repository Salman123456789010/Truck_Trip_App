package com.dadabarbie.TruckTrip.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vasyerp.cafvd.room.dao.ProductsDao
import com.vasyerp.cafvd.room.dao.TripRecordDao
import com.vasyerp.cafvd.room.model.Products
import com.vasyerp.cafvd.room.model.TripRecordEntity

@Database(
    entities = [Products::class, TripRecordEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productsDao(): ProductsDao
    abstract fun tripRecordDao(): TripRecordDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "truck_trip_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }

            }
        }
    }
}


