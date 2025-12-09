package com.dadabarbie.TruckTrip.room

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.vasyerp.cafvd.room.dao.ProductsDao
import com.vasyerp.cafvd.room.dao.TripsDao
import com.vasyerp.cafvd.room.model.Products
import com.vasyerp.cafvd.room.model.TripEntity
import retrofit2.Converter


@Database(entities = [Products::class, TripEntity::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productsDao(): ProductsDao
    abstract fun tripsDao(): TripsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "products"
                ).addMigrations(MIGRATION_3_4).build()
                INSTANCE = instance
                instance
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS trips (id TEXT NOT NULL, createdDate TEXT NOT NULL, updatedDate TEXT NOT NULL, destination TEXT NOT NULL, driver_income TEXT NOT NULL, end_date TEXT NOT NULL, source TEXT NOT NULL, start_date TEXT NOT NULL, total_days TEXT NOT NULL, total_expense TEXT NOT NULL, total_income TEXT NOT NULL, truck_average TEXT NOT NULL, truck_no TEXT NOT NULL, owner_profit TEXT NOT NULL, mobile TEXT NOT NULL, income_json TEXT NOT NULL, expense_json TEXT NOT NULL, PRIMARY KEY(id))"
                )
            }
        }
    }

}
