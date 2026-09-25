package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.EncryptedCardTokenEntity
import com.example.data.local.entities.LocalAuditLogEntity
import com.example.data.local.entities.LocalDealerInventoryEntity
import com.example.data.local.entities.OfflineTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineTransactionDao {
    @Query("SELECT * FROM offline_transactions WHERE syncStatus = 'PENDING_OFFLINE_SYNC' ORDER BY createdAtTimestamp ASC")
    fun getPendingOfflineTransactions(): Flow<List<OfflineTransactionEntity>>

    @Query("SELECT * FROM offline_transactions WHERE syncStatus = 'PENDING_OFFLINE_SYNC' ORDER BY createdAtTimestamp ASC")
    suspend fun getPendingOfflineTransactionsList(): List<OfflineTransactionEntity>

    @Query("SELECT * FROM offline_transactions ORDER BY createdAtTimestamp DESC")
    fun getAllTransactions(): Flow<List<OfflineTransactionEntity>>

    @Query("SELECT * FROM offline_transactions WHERE transactionId = :transactionId LIMIT 1")
    suspend fun getTransactionById(transactionId: String): OfflineTransactionEntity?

    @Query("SELECT * FROM offline_transactions WHERE idempotencyKey = :idempotencyKey LIMIT 1")
    suspend fun getByIdempotencyKey(idempotencyKey: String): OfflineTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: OfflineTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<OfflineTransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: OfflineTransactionEntity)

    @Query("UPDATE offline_transactions SET syncStatus = :newStatus, syncedAtTimestamp = :syncedTimestamp, lastSyncError = NULL WHERE transactionId = :transactionId")
    suspend fun markAsSynced(
        transactionId: String,
        syncedTimestamp: Long = System.currentTimeMillis(),
        newStatus: String = "SYNCED_ONLINE"
    )

    @Query("UPDATE offline_transactions SET syncStatus = 'CONFLICT_FLAGGED', lastSyncError = :errorMessage WHERE transactionId = :transactionId")
    suspend fun markAsConflict(transactionId: String, errorMessage: String)

    @Query("UPDATE offline_transactions SET syncAttempts = syncAttempts + 1, lastSyncError = :errorMessage WHERE transactionId = :transactionId")
    suspend fun incrementSyncAttempts(transactionId: String, errorMessage: String)

    @Query("SELECT COUNT(*) FROM offline_transactions WHERE syncStatus = 'PENDING_OFFLINE_SYNC'")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM offline_transactions WHERE syncStatus = 'PENDING_OFFLINE_SYNC'")
    suspend fun getPendingCountSnapshot(): Int

    @Query("SELECT * FROM offline_transactions WHERE cardId = :cardId ORDER BY createdAtTimestamp DESC")
    fun getTransactionsByCard(cardId: String): Flow<List<OfflineTransactionEntity>>

    @Query("SELECT * FROM offline_transactions WHERE syncStatus = :syncStatus ORDER BY createdAtTimestamp DESC")
    fun getTransactionsByStatus(syncStatus: String): Flow<List<OfflineTransactionEntity>>

    @Query("SELECT * FROM offline_transactions ORDER BY createdAtTimestamp DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int = 50): Flow<List<OfflineTransactionEntity>>

    @Query("DELETE FROM offline_transactions WHERE transactionId = :transactionId")
    suspend fun deleteTransaction(transactionId: String)

    @Query("DELETE FROM offline_transactions WHERE syncStatus = 'SYNCED_ONLINE' AND syncedAtTimestamp < :thresholdTimestamp")
    suspend fun deleteSyncedOlderThan(thresholdTimestamp: Long)
}

@Dao
interface LocalAuditLogDao {
    @Query("SELECT * FROM local_audit_logs ORDER BY id DESC")
    fun getAllLogs(): Flow<List<LocalAuditLogEntity>>

    @Query("SELECT * FROM local_audit_logs ORDER BY id DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<LocalAuditLogEntity>>

    @Query("SELECT * FROM local_audit_logs WHERE cardId = :cardId ORDER BY id DESC")
    fun getLogsByCard(cardId: String): Flow<List<LocalAuditLogEntity>>

    @Query("SELECT * FROM local_audit_logs WHERE action = :action ORDER BY id DESC")
    fun getLogsByAction(action: String): Flow<List<LocalAuditLogEntity>>

    @Query("SELECT * FROM local_audit_logs WHERE syncStatus = 'PENDING_SYNC' ORDER BY id ASC")
    suspend fun getPendingSyncLogs(): List<LocalAuditLogEntity>

    @Query("SELECT * FROM local_audit_logs ORDER BY id DESC LIMIT 1")
    suspend fun getLatestLog(): LocalAuditLogEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLog(log: LocalAuditLogEntity): Long

    @Query("UPDATE local_audit_logs SET syncStatus = 'SYNCED' WHERE eventId IN (:eventIds)")
    suspend fun markLogsAsSynced(eventIds: List<String>)

    @Query("SELECT COUNT(*) FROM local_audit_logs WHERE syncStatus = 'PENDING_SYNC'")
    fun getPendingAuditCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM local_audit_logs")
    fun getTotalAuditCount(): Flow<Int>
}

@Dao
interface EncryptedCardTokenDao {
    @Query("SELECT * FROM cached_card_tokens WHERE cardId = :cardId AND isRevoked = 0 LIMIT 1")
    suspend fun getCachedCard(cardId: String): EncryptedCardTokenEntity?

    @Query("SELECT * FROM cached_card_tokens WHERE cardId = :cardId AND isRevoked = 0 AND cardStatus = 'ACTIVE' AND tokenExpiryTimestamp > :currentTimestamp LIMIT 1")
    suspend fun getActiveValidCard(cardId: String, currentTimestamp: Long = System.currentTimeMillis()): EncryptedCardTokenEntity?

    @Query("SELECT * FROM cached_card_tokens ORDER BY headOfFamilyName ASC")
    fun getAllCachedCards(): Flow<List<EncryptedCardTokenEntity>>

    @Query("SELECT COUNT(*) FROM cached_card_tokens WHERE isRevoked = 0")
    fun getCachedCardCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM cached_card_tokens WHERE cardId = :cardId AND isRevoked = 0)")
    suspend fun isCardCached(cardId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: EncryptedCardTokenEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<EncryptedCardTokenEntity>)

    @Query("UPDATE cached_card_tokens SET cardStatus = :newStatus WHERE cardId = :cardId")
    suspend fun updateCardStatus(cardId: String, newStatus: String)

    @Query("UPDATE cached_card_tokens SET isRevoked = 1 WHERE cardId = :cardId")
    suspend fun revokeCard(cardId: String)

    @Query("DELETE FROM cached_card_tokens WHERE cardId = :cardId")
    suspend fun deleteCard(cardId: String)

    @Query("DELETE FROM cached_card_tokens WHERE tokenExpiryTimestamp < :currentTimestamp")
    suspend fun deleteExpiredTokens(currentTimestamp: Long = System.currentTimeMillis())
}

@Dao
interface LocalInventoryDao {
    @Query("SELECT * FROM local_dealer_inventory ORDER BY commodityId ASC")
    fun getAllInventory(): Flow<List<LocalDealerInventoryEntity>>

    @Query("SELECT * FROM local_dealer_inventory WHERE commodityId = :commodityId LIMIT 1")
    suspend fun getStock(commodityId: String): LocalDealerInventoryEntity?

    @Query("UPDATE local_dealer_inventory SET currentStock = currentStock - :quantity, lastUpdatedTimestamp = :timestamp WHERE commodityId = :commodityId AND currentStock >= :quantity")
    suspend fun deductStock(
        commodityId: String,
        quantity: Double,
        timestamp: Long = System.currentTimeMillis()
    ): Int

    @Query("UPDATE local_dealer_inventory SET currentStock = :newStock, lastUpdatedTimestamp = :timestamp WHERE commodityId = :commodityId")
    suspend fun updateStock(
        commodityId: String,
        newStock: Double,
        timestamp: Long = System.currentTimeMillis()
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventory(items: List<LocalDealerInventoryEntity>)
}
