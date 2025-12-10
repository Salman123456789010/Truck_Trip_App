package com.dadabarbie.TruckTrip.room

import android.content.Context
import androidx.databinding.adapters.Converters
import androidx.room.BuiltInTypeConverters
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vasyerp.cafvd.room.dao.ProductsDao
import com.vasyerp.cafvd.room.model.Products
import retrofit2.Converter


@Database(entities = [Products::class], version = 3, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productsDao(): ProductsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "products"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }

}