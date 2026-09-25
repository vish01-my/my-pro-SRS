package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AuditLogDao
import com.example.data.local.dao.BeneficiaryDao
import com.example.data.local.dao.TransactionDao

/**
 * Room AppDatabase configuration class in com.example.data.local
 * defining the local persistence layer for offline operations.
 * Caches Beneficiary profiles, Transactions, and Tamper-Evident AuditLogs.
 */
@Database(
    entities = [
        Beneficiary::class,
        Transaction::class,
        AuditLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun beneficiaryDao(): BeneficiaryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pds_mobile_local.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
