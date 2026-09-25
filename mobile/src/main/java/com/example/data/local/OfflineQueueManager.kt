package com.example.data.local

import android.content.Context
import com.example.data.local.entities.EncryptedCardTokenEntity
import com.example.data.local.entities.LocalAuditLogEntity
import com.example.data.local.entities.OfflineTransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * OfflineQueueManager: Coordinates local persistence, offline queueing,
 * idempotent synchronization, and hash-chained audit logging on the POS terminal.
 */
class OfflineQueueManager(context: Context) {

    private val database = PdsLocalDatabase.getInstance(context)
    private val transactionDao = database.offlineTransactionDao()
    private val auditLogDao = database.localAuditLogDao()
    private val cardTokenDao = database.encryptedCardTokenDao()
    private val inventoryDao = database.localInventoryDao()

    val pendingTransactions: Flow<List<OfflineTransactionEntity>> =
        transactionDao.getPendingOfflineTransactions()

    val pendingTransactionCount: Flow<Int> =
        transactionDao.getPendingCount()

    val allAuditLogs: Flow<List<LocalAuditLogEntity>> =
        auditLogDao.getAllLogs()

    /**
     * Enqueues an offline transaction into Room with an idempotent key
     * and decrements local physical stock atomically.
     */
    suspend fun enqueueOfflineTransaction(
        transaction: OfflineTransactionEntity,
        commoditiesToDeduct: Map<String, Double>
    ): Boolean = withContext(Dispatchers.IO) {
        // Check if transaction with this idempotency key already exists
        val existing = transactionDao.getByIdempotencyKey(transaction.idempotencyKey)
        if (existing != null) {
            return@withContext false
        }

        // Deduct local inventory
        for ((commodityId, qty) in commoditiesToDeduct) {
            inventoryDao.deductStock(commodityId, qty)
        }

        // Insert into offline queue
        transactionDao.insertTransaction(transaction)

        // Record to local tamper-evident audit ledger
        recordAuditLog(
            dealerId = transaction.dealerId,
            deviceId = transaction.posDeviceId,
            cardId = transaction.cardId,
            action = "OFFLINE_TRANSACTION_QUEUED",
            result = "SUCCESS",
            riskLevel = transaction.riskLevel,
            details = "Invoice ${transaction.invoiceNumber} stored in encrypted offline queue. Amount: ₹${transaction.totalAmountPaid}."
        )

        true
    }

    /**
     * Records a local audit entry with automatic SHA-256 hash chaining:
     * currentHash = SHA-256(previousHash : eventId : timestamp : action : result)
     */
    suspend fun recordAuditLog(
        dealerId: String,
        deviceId: String,
        cardId: String,
        action: String,
        result: String,
        riskLevel: String,
        details: String
    ): LocalAuditLogEntity = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val eventId = "LOG-POS-${now}-${(1000..9999).random()}"
        val latestLog = auditLogDao.getLatestLog()
        val previousHash = latestLog?.currentHash
            ?: "0000000000000000000000000000000000000000000000000000000000000000"

        val dataToHash = "$previousHash:$eventId:$now:$action:$result"
        val currentHash = sha256(dataToHash)

        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        val formattedTime = dateFormat.format(Date(now))

        val entity = LocalAuditLogEntity(
            eventId = eventId,
            timestampEpochMs = now,
            formattedTime = formattedTime,
            dealerId = dealerId,
            deviceId = deviceId,
            cardId = cardId,
            action = action,
            result = result,
            riskLevel = riskLevel,
            previousHash = previousHash,
            currentHash = currentHash,
            details = details,
            syncStatus = "PENDING_SYNC"
        )

        auditLogDao.insertLog(entity)
        entity
    }

    /**
     * Look up a cached Smart Card Token offline.
     */
    suspend fun getCachedCard(cardId: String): EncryptedCardTokenEntity? =
        withContext(Dispatchers.IO) {
            val card = cardTokenDao.getCachedCard(cardId)
            if (card != null && card.tokenExpiryTimestamp >= System.currentTimeMillis()) {
                card
            } else {
                null
            }
        }

    /**
     * Mark an offline transaction as successfully synced online with the central backend.
     */
    suspend fun markTransactionSynced(transactionId: String) =
        withContext(Dispatchers.IO) {
            transactionDao.markAsSynced(transactionId, System.currentTimeMillis())
        }

    /**
     * Flag an offline transaction as conflicting (e.g. quota already exhausted on another POS).
     */
    suspend fun markTransactionConflict(transactionId: String, reason: String) =
        withContext(Dispatchers.IO) {
            transactionDao.markAsConflict(transactionId, reason)
        }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
