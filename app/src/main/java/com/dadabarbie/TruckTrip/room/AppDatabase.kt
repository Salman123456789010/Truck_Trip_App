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
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productsDao(): ProductsDao
    abstract fun tripRecordDao(): TripRecordDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example: new column add kar rahe ho
                // database.execSQL("ALTER TABLE TripRecordEntity ADD COLUMN newField TEXT")

                // Agar schema same hai aur sirf version bump chahiye:
                // to yahan kuch bhi likhne ki zarurat nahi
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "truck_trip_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}



