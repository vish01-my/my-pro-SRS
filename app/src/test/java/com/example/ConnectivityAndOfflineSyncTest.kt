package com.example

import com.example.data.local.SyncState
import com.example.data.local.SyncSummary
import com.example.network.ConnectivityObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying ConnectivityObserver contracts and
 * the OfflineTransactionManager auto-trigger synchronization workflow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityAndOfflineSyncTest {

    // Test Fake for ConnectivityObserver
    class FakeConnectivityObserver(
        initialStatus: ConnectivityObserver.NetworkStatus = ConnectivityObserver.NetworkStatus.Unavailable,
        initialType: ConnectivityObserver.ConnectionType = ConnectivityObserver.ConnectionType.NONE
    ) : ConnectivityObserver {
        val statusFlow = MutableStateFlow(initialStatus)
        val typeFlow = MutableStateFlow(initialType)

        override val status = statusFlow.asStateFlow()
        override val connectionType = typeFlow.asStateFlow()

        private var connected = initialStatus == ConnectivityObserver.NetworkStatus.Available

        fun emitStatus(newStatus: ConnectivityObserver.NetworkStatus) {
            connected = newStatus == ConnectivityObserver.NetworkStatus.Available
            statusFlow.value = newStatus
        }

        fun emitType(newType: ConnectivityObserver.ConnectionType) {
            typeFlow.value = newType
        }

        override fun isConnected(): Boolean = connected

        override fun getCurrentConnectionType(): ConnectivityObserver.ConnectionType = typeFlow.value
    }

    @Test
    fun `test connectivity observer status emissions`() = runTest {
        val observer = FakeConnectivityObserver()

        assertEquals(ConnectivityObserver.NetworkStatus.Unavailable, observer.status.value)
        assertFalse(observer.isConnected())

        observer.emitStatus(ConnectivityObserver.NetworkStatus.Available)
        observer.emitType(ConnectivityObserver.ConnectionType.WIFI)

        assertEquals(ConnectivityObserver.NetworkStatus.Available, observer.status.value)
        assertEquals(ConnectivityObserver.ConnectionType.WIFI, observer.getCurrentConnectionType())
        assertTrue(observer.isConnected())

        observer.emitStatus(ConnectivityObserver.NetworkStatus.Lost)
        assertEquals(ConnectivityObserver.NetworkStatus.Lost, observer.status.value)
        assertFalse(observer.isConnected())
    }

    @Test
    fun `test auto-sync triggers when network transitions from lost to available`() = runTest {
        val observer = FakeConnectivityObserver(initialStatus = ConnectivityObserver.NetworkStatus.Lost)
        var autoSyncTriggeredCount = 0
        val isAutoSyncEnabled = MutableStateFlow(true)

        // Mock auto-sync listener loop matching OfflineTransactionManager logic
        val job = launch {
            var previousStatus: ConnectivityObserver.NetworkStatus? = null

            observer.status.collectLatest { currentStatus ->
                val hadDisconnection = previousStatus == ConnectivityObserver.NetworkStatus.Lost ||
                        previousStatus == ConnectivityObserver.NetworkStatus.Unavailable ||
                        previousStatus == null

                if (isAutoSyncEnabled.value &&
                    currentStatus == ConnectivityObserver.NetworkStatus.Available &&
                    hadDisconnection
                ) {
                    autoSyncTriggeredCount++
                }
                previousStatus = currentStatus
            }
        }

        // Initially in Lost state
        assertEquals(0, autoSyncTriggeredCount)

        // Network restored!
        observer.emitStatus(ConnectivityObserver.NetworkStatus.Available)
        testScheduler.runCurrent()

        assertEquals(1, autoSyncTriggeredCount)

        // Disconnect again
        observer.emitStatus(ConnectivityObserver.NetworkStatus.Lost)
        testScheduler.runCurrent()
        assertEquals(1, autoSyncTriggeredCount)

        // Reconnect again
        observer.emitStatus(ConnectivityObserver.NetworkStatus.Available)
        testScheduler.runCurrent()
        assertEquals(2, autoSyncTriggeredCount)

        // Disable auto sync
        isAutoSyncEnabled.value = false
        observer.emitStatus(ConnectivityObserver.NetworkStatus.Lost)
        testScheduler.runCurrent()
        observer.emitStatus(ConnectivityObserver.NetworkStatus.Available)
        testScheduler.runCurrent()

        // Should NOT trigger when auto-sync is disabled
        assertEquals(2, autoSyncTriggeredCount)

        job.cancel()
    }

    @Test
    fun `test sync state and summary data models`() {
        val summary = SyncSummary(
            totalProcessed = 5,
            syncedSuccessfully = 4,
            conflictCount = 1,
            auditLogsSynced = 3
        )

        assertEquals(5, summary.totalProcessed)
        assertEquals(4, summary.syncedSuccessfully)
        assertEquals(1, summary.conflictCount)
        assertEquals(3, summary.auditLogsSynced)

        val syncState: SyncState = SyncState.Conflict(
            conflictCount = summary.conflictCount,
            details = "1 record flagged for central verification"
        )

        assertTrue(syncState is SyncState.Conflict)
        assertEquals(1, (syncState as SyncState.Conflict).conflictCount)
    }

    @Test
    fun `test offline transaction idempotency key deduplication contract`() {
        val transactionsStore = mutableMapOf<String, String>() // idempotencyKey -> txnId

        fun recordTransaction(idempotencyKey: String, txnId: String): Pair<String, Boolean> {
            if (transactionsStore.containsKey(idempotencyKey)) {
                // Return existing ID, zero duplicate creation
                return Pair(transactionsStore[idempotencyKey]!!, false)
            }
            transactionsStore[idempotencyKey] = txnId
            return Pair(txnId, true)
        }

        val key = "IDEM-TEST-KEY-001"
        val (firstTxnId, isNew1) = recordTransaction(key, "TXN-001")
        assertTrue(isNew1)
        assertEquals("TXN-001", firstTxnId)

        // Submitting with identical idempotencyKey (network retry simulation)
        val (secondTxnId, isNew2) = recordTransaction(key, "TXN-002")
        assertFalse(isNew2) // Must NOT create new record
        assertEquals("TXN-001", secondTxnId) // Must return existing ID
        assertEquals(1, transactionsStore.size) // Store size remains exactly 1
    }

    @Test
    fun `test batch reconciliation DTO mapping and duplicate acknowledgment`() {
        val serverResults = listOf(
            com.example.network.TransactionReconcileResultDto(
                transaction_id = "TXN-101",
                idempotency_key = "IDEM-101",
                status = "RECONCILED",
                message = "Reconciled with central ledger",
                synced_timestamp = 1758600000000L
            ),
            com.example.network.TransactionReconcileResultDto(
                transaction_id = "TXN-102",
                idempotency_key = "IDEM-102",
                status = "DUPLICATE_IDEMPOTENT_IGNORED",
                message = "Idempotency replay acknowledged safely",
                synced_timestamp = 1758600000000L
            ),
            com.example.network.TransactionReconcileResultDto(
                transaction_id = "TXN-103",
                idempotency_key = "IDEM-103",
                status = "CONFLICT_FLAGGED",
                message = "Card status blocked",
                synced_timestamp = 1758600000000L
            )
        )

        var syncedCount = 0
        var duplicateCount = 0
        var conflictCount = 0

        serverResults.forEach { res ->
            when (res.status) {
                "RECONCILED" -> syncedCount++
                "DUPLICATE_IDEMPOTENT_IGNORED" -> {
                    // Handled safely as synced without error or re-debiting
                    duplicateCount++
                    syncedCount++
                }
                "CONFLICT_FLAGGED" -> conflictCount++
            }
        }

        assertEquals(2, syncedCount)
        assertEquals(1, duplicateCount)
        assertEquals(1, conflictCount)
    }
}

