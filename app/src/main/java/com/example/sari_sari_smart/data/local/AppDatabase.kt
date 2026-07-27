package com.example.sari_sari_smart.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.sari_sari_smart.data.local.dao.*
import com.example.sari_sari_smart.data.local.entity.*

@Database(
    entities = [
        ProductEntity::class,
        DailyEntryEntity::class,
        SpecificSaleEntity::class,
        CustomerDebtEntity::class,
        EndOfDayEntity::class,
        RestockLogEntity::class,
        DebtPaymentEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun dailyEntryDao(): DailyEntryDao
    abstract fun specificSaleDao(): SpecificSaleDao
    abstract fun customerDebtDao(): CustomerDebtDao
    abstract fun endOfDayDao(): EndOfDayDao
    abstract fun restockLogDao(): RestockLogDao
    abstract fun debtPaymentDao(): DebtPaymentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sari_sari_smart_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
