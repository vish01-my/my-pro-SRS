package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.AuditLogEntity
import com.example.model.AuthorizedDeviceEntity
import com.example.model.BeneficiaryEntity
import com.example.model.CommodityEntity
import com.example.model.ComplaintEntity
import com.example.model.DealerInventoryEntity
import com.example.model.EntitlementEntity
import com.example.model.FraudAlertEntity
import com.example.model.TransactionEntity
import com.example.model.TransactionItemEntity

@Database(
    entities = [
        BeneficiaryEntity::class,
        EntitlementEntity::class,
        CommodityEntity::class,
        DealerInventoryEntity::class,
        TransactionEntity::class,
        TransactionItemEntity::class,
        AuditLogEntity::class,
        FraudAlertEntity::class,
        ComplaintEntity::class,
        AuthorizedDeviceEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun beneficiaryDao(): BeneficiaryDao
    abstract fun entitlementDao(): EntitlementDao
    abstract fun commodityDao(): CommodityDao
    abstract fun dealerInventoryDao(): DealerInventoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun fraudAlertDao(): FraudAlertDao
    abstract fun complaintDao(): ComplaintDao
    abstract fun authorizedDeviceDao(): AuthorizedDeviceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_ration_pds_secure.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
