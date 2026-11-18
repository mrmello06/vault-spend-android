package com.moneytracker.vault.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        LearnedMerchantEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class VaultDatabase : RoomDatabase() {

    abstract fun vaultDao(): VaultDao

    companion object {
        @Volatile
        private var INSTANCE: VaultDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN note TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isIncludedInSpend INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE transactions ADD COLUMN isSelfTransfer INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transactions ADD COLUMN parserVersion INTEGER NOT NULL DEFAULT 1")
                db.execSQL("DELETE FROM transactions WHERE parserVersion < 2 AND isUserModified = 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM transactions WHERE parserVersion < 3 AND isUserModified = 0 AND note = '' AND isIncludedInSpend = 1"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN fullSmsBody TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM transactions WHERE parserVersion < 5 AND isUserModified = 0 AND note = '' AND isIncludedInSpend = 1"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Clean any accidental CRED -> SELF_TRANSFER learned rule
                db.execSQL("DELETE FROM learned_merchants WHERE normalizedKey IN ('CRED', 'CREDCLUB', 'SELFTTRANSFER', 'SELFTRANSFER')")
                // 2. Fix all CRED / Gokiwi rows (even if opened & auto-saved in v1.4 without notes) to CREDIT_CARD_BILL!
                db.execSQL(
                    """
                    UPDATE transactions 
                    SET isSelfTransfer = 0, 
                        category = 'CREDIT_CARD_BILL', 
                        merchantDisplayName = 'CRED Club',
                        isUserModified = 0
                    WHERE (UPPER(merchantRawCode) IN ('CRED', 'CRED CLUB') OR UPPER(merchantDisplayName) = 'CRED CLUB')
                      AND note = ''
                    """
                )
                // 3. Delete duplicate BBPS Credit Card received rows that were touched earlier without notes
                db.execSQL(
                    "DELETE FROM transactions WHERE merchantDisplayName = 'Credit Card Bill Received' AND note = ''"
                )
                // 4. Re-parse all unedited rows with v6 multi-pass engine while keeping genuine user edits & notes intact
                db.execSQL(
                    "DELETE FROM transactions WHERE parserVersion < 6 AND isUserModified = 0 AND note = '' AND isIncludedInSpend = 1"
                )
            }
        }

        fun getInstance(context: Context): VaultDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VaultDatabase::class.java,
                    "vault_offline_ledger.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
