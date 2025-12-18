package com.dadabarbie.TruckTrip.room

import android.content.Context
import androidx.databinding.adapters.Converters
import androidx.room.BuiltInTypeConverters
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.vasyerp.cafvd.room.dao.ProductsDao
import com.vasyerp.cafvd.room.dao.TripRecordDao
import com.vasyerp.cafvd.room.model.Products
import com.vasyerp.cafvd.room.model.TripRecordEntity
import retrofit2.Converter


@Database(entities = [Products::class, TripRecordEntity::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productsDao(): ProductsDao
    abstract fun tripRecordDao(): TripRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val MIGRATION_3_4 = object : Migration(3, 4) {
                    override fun migrate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS trip_records (" +
                                    "id TEXT NOT NULL PRIMARY KEY, " +
                                    "source TEXT NOT NULL, " +
                                    "destination TEXT NOT NULL, " +
                                    "start_date TEXT NOT NULL, " +
                                    "end_date TEXT NOT NULL, " +
                                    "total_income TEXT NOT NULL, " +
                                    "total_expense TEXT NOT NULL, " +
                                    "driver_income TEXT NOT NULL, " +
                                    "owner_profit TEXT NOT NULL, " +
                                    "truck_average TEXT NOT NULL, " +
                                    "truck_no TEXT NOT NULL, " +
                                    "createdDate TEXT NOT NULL, " +
                                    "updatedDate TEXT NOT NULL, " +
                                    "incomeJson TEXT NOT NULL, " +
                                    "expenseJson TEXT NOT NULL"
                                    + ")"
                        )
                    }
                }
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "products"
                ).addMigrations(MIGRATION_3_4).build()
                INSTANCE = instance
                instance
            }
        }
    }

}
