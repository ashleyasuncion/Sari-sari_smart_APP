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
    version = 6,
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
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
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

        /**
         * v4 → v5: drop the obsolete `costOfGoods` column from `end_of_day_data`.
         * Cost of Goods was removed from the Closing page with the web Restock Day
         * feature (web progress.txt, July 8 2026) — mobile parity fix.
         * ALTER TABLE DROP COLUMN needs SQLite >= 3.35 but minSdk 24 ships SQLite
         * 3.9, so use the Room-recommended recreate-and-copy pattern.
         * Non-destructive: existing rows carry over with costOfGoods dropped.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `end_of_day_data_new` (" +
                        "`date` TEXT NOT NULL, `cashInDrawer` REAL NOT NULL, " +
                        "`stockCheckDone` INTEGER NOT NULL, `debtPaymentsDone` INTEGER NOT NULL, " +
                        "`finished` INTEGER NOT NULL, `recordedSales` REAL NOT NULL, " +
                        "`actualSales` REAL NOT NULL, `salesDiff` REAL NOT NULL, " +
                        "`profit` REAL NOT NULL, PRIMARY KEY(`date`))"
                )
                db.execSQL(
                    "INSERT INTO `end_of_day_data_new` (`date`, `cashInDrawer`, `stockCheckDone`, " +
                        "`debtPaymentsDone`, `finished`, `recordedSales`, `actualSales`, " +
                        "`salesDiff`, `profit`) " +
                        "SELECT `date`, `cashInDrawer`, `stockCheckDone`, `debtPaymentsDone`, " +
                        "`finished`, `recordedSales`, `actualSales`, `salesDiff`, `profit` " +
                        "FROM `end_of_day_data`"
                )
                db.execSQL("DROP TABLE `end_of_day_data`")
                db.execSQL("ALTER TABLE `end_of_day_data_new` RENAME TO `end_of_day_data`")
            }
        }

        /**
         * v5 → v6: add the per-customer `creditLimit` column (web v2.56 parity).
         * NULL = uses the global default; 0 = no limit for that customer.
         * Non-destructive: existing debts keep their data, new column defaults NULL.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `customer_debts` ADD COLUMN `creditLimit` INTEGER")
            }
        }
    }
}
