package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE cardId = :cardId ORDER BY timestamp DESC")
    fun getTransactionsForCard(cardId: String): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING_OFFLINE_SYNC' ORDER BY timestamp ASC")
    fun getPendingTransactionsFlow(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE syncStatus = 'PENDING_OFFLINE_SYNC' ORDER BY timestamp ASC")
    suspend fun getPendingOfflineTransactions(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): Transaction?

    @Query("SELECT * FROM transactions WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getByIdempotencyKey(key: String): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("UPDATE transactions SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE transactions SET syncStatus = 'SYNCED_ONLINE', syncedAtTimestamp = :syncedTimestamp WHERE id = :id")
    suspend fun markAsSynced(id: String, syncedTimestamp: Long = System.currentTimeMillis())

    @Query("UPDATE transactions SET syncStatus = 'CONFLICT_FLAGGED', lastSyncError = :errorMessage WHERE id = :id")
    suspend fun markAsConflict(id: String, errorMessage: String)

    @Query("SELECT COUNT(*) FROM transactions WHERE syncStatus = 'PENDING_OFFLINE_SYNC'")
    fun getPendingCount(): Flow<Int>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: String)
}
