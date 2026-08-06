package com.example.sari_sari_smart.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        DebtPaymentEntity::class,
        DebtTransactionEntity::class
    ],
    version = 4,
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
    abstract fun debtTransactionDao(): DebtTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?:                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sari_sari_smart_db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        /**
         * v3 → v4: add the per-debt transaction ledger (web transactions[] parity).
         * Created non-destructively so existing user data survives the upgrade.
         * Schema must match DebtTransactionEntity exactly.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `debt_transactions` (`id` INTEGER NOT NULL, " +
                        "`debtId` INTEGER NOT NULL, `type` TEXT NOT NULL, `description` TEXT, " +
                        "`amount` REAL NOT NULL, `timestamp` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`), " +
                        "FOREIGN KEY(`debtId`) REFERENCES `customer_debts`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_debt_transactions_debtId` ON `debt_transactions` (`debtId`)"
                )
            }
        }
    }
}
