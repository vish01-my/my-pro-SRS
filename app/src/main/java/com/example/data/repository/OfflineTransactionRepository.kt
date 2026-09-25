package com.example.data.repository

import com.example.data.local.SyncState
import com.example.data.local.SyncSummary
import com.example.data.local.entities.OfflineTransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * OfflineTransactionRepository:
 * Formal repository contract managing offline transactions, cryptographic idempotency,
 * atomic database transactions, and background reconciliation with the central PDS server.
 */
interface OfflineTransactionRepository {

    /**
     * Flow of transactions pending synchronization with the central server.
     */
    val pendingTransactionsFlow: Flow<List<OfflineTransactionEntity>>

    /**
     * Flow of all transactions (both offline queued and synchronized).
     */
    val allTransactionsFlow: Flow<List<OfflineTransactionEntity>>

    /**
     * Flow of the count of pending offline records.
     */
    val pendingCountFlow: Flow<Int>

    /**
     * Observable sync lifecycle state (Idle, Syncing, Success, Conflict, Error).
     */
    val syncState: StateFlow<SyncState>

    /**
     * Observable state for automated synchronization on network restoration.
     */
    val isAutoSyncEnabled: StateFlow<Boolean>

    /**
     * Records a new transaction atomically:
     * 1. Validates idempotency key (zero duplicate processing).
     * 2. Decrements local Fair Price Shop commodity inventory.
     * 3. Inserts transaction into local Room database.
     * 4. Appends a new cryptographic block into the local SHA-256 audit ledger.
     * 
     * All executed within a single ACID Room transaction boundary.
     */
    suspend fun recordOfflineTransactionAtomically(
        transaction: OfflineTransactionEntity,
        itemsToDeduct: Map<String, Double> = emptyMap(),
        actionDetails: String = "Offline sale recorded"
    ): Result<String>

    /**
     * Reconciles locally stored transactions with the backend once connectivity is restored.
     * Guarantees atomicity and idempotency across network retries.
     */
    suspend fun reconcileWithBackend(isAutomated: Boolean = false): SyncSummary

    /**
     * Starts listening to ConnectivityObserver and auto-triggers reconciliation on connection restore.
     */
    fun startAutoSync(scope: CoroutineScope)

    /**
     * Stops the auto-sync listener job.
     */
    fun stopAutoSync()

    /**
     * Toggles whether restored connectivity automatically launches reconciliation.
     */
    fun setAutoSyncEnabled(enabled: Boolean)
}
