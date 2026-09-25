package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
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
import kotlinx.coroutines.flow.Flow

@Dao
interface BeneficiaryDao {
    @Query("SELECT * FROM beneficiaries WHERE cardId = :cardId LIMIT 1")
    suspend fun getBeneficiaryByCardId(cardId: String): BeneficiaryEntity?

    @Query("SELECT * FROM beneficiaries")
    fun getAllBeneficiaries(): Flow<List<BeneficiaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiaries(beneficiaries: List<BeneficiaryEntity>)

    @Update
    suspend fun updateBeneficiary(beneficiary: BeneficiaryEntity)

    @Query("UPDATE beneficiaries SET cardStatus = :status WHERE cardId = :cardId")
    suspend fun updateCardStatus(cardId: String, status: String)
}

@Dao
interface EntitlementDao {
    @Query("SELECT * FROM entitlements WHERE cardId = :cardId")
    fun getEntitlementsForCard(cardId: String): Flow<List<EntitlementEntity>>

    @Query("SELECT * FROM entitlements WHERE cardId = :cardId")
    suspend fun getEntitlementsList(cardId: String): List<EntitlementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntitlements(entitlements: List<EntitlementEntity>)

    @Update
    suspend fun updateEntitlement(entitlement: EntitlementEntity)
}

@Dao
interface CommodityDao {
    @Query("SELECT * FROM commodities")
    fun getAllCommodities(): Flow<List<CommodityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommodities(commodities: List<CommodityEntity>)
}

@Dao
interface DealerInventoryDao {
    @Query("SELECT * FROM dealer_inventory WHERE fpsId = :fpsId")
    fun getInventoryForFps(fpsId: String): Flow<List<DealerInventoryEntity>>

    @Query("SELECT * FROM dealer_inventory WHERE fpsId = :fpsId")
    suspend fun getInventoryList(fpsId: String): List<DealerInventoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(items: List<DealerInventoryEntity>)

    @Query("UPDATE dealer_inventory SET currentStock = currentStock - :quantity WHERE fpsId = :fpsId AND commodityId = :commodityId")
    suspend fun deductStock(fpsId: String, commodityId: String, quantity: Double)

    @Query("UPDATE dealer_inventory SET currentStock = currentStock + :quantity WHERE fpsId = :fpsId AND commodityId = :commodityId")
    suspend fun addStock(fpsId: String, commodityId: String, quantity: Double)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY timestamp DESC")
    fun getTransactionsForCard(cardId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING_OFFLINE_SYNC'")
    suspend fun getPendingOfflineTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getByIdempotencyKey(key: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionItems(items: List<TransactionItemEntity>)

    @Query("SELECT * FROM transaction_items WHERE transactionId = :transactionId")
    suspend fun getItemsForTransaction(transactionId: String): List<TransactionItemEntity>

    @Query("UPDATE transactions SET syncStatus = :status WHERE id = :transactionId")
    suspend fun updateSyncStatus(transactionId: String, status: String)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY id DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY id DESC LIMIT 1")
    suspend fun getLatestAuditLog(): AuditLogEntity?

    @Insert
    suspend fun insertAuditLog(log: AuditLogEntity)
}

@Dao
interface FraudAlertDao {
    @Query("SELECT * FROM fraud_alerts ORDER BY id DESC")
    fun getAllFraudAlerts(): Flow<List<FraudAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFraudAlert(alert: FraudAlertEntity)

    @Query("UPDATE fraud_alerts SET isResolved = 1, resolutionNotes = :notes WHERE id = :id")
    suspend fun resolveAlert(id: Long, notes: String)
}

@Dao
interface ComplaintDao {
    @Query("SELECT * FROM complaints ORDER BY id DESC")
    fun getAllComplaints(): Flow<List<ComplaintEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaint(complaint: ComplaintEntity)

    @Query("UPDATE complaints SET status = :status, resolutionRemark = :remark WHERE id = :id")
    suspend fun updateComplaintStatus(id: Long, status: String, remark: String)
}

@Dao
interface AuthorizedDeviceDao {
    @Query("SELECT * FROM authorized_devices WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getDeviceById(deviceId: String): AuthorizedDeviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: AuthorizedDeviceEntity)

    @Query("UPDATE authorized_devices SET status = :status WHERE deviceId = :deviceId")
    suspend fun updateDeviceStatus(deviceId: String, status: String)
}
