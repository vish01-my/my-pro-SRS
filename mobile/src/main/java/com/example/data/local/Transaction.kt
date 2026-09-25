package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Transaction entity for local distribution transactions.
 * Stores transaction records locally with idempotency keys, commodity allocations,
 * and sync status for offline POS operations and reconciliation.
 */
@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["syncStatus"]),
        Index(value = ["cardId"]),
        Index(value = ["timestamp"])
    ]
)
data class Transaction(
    @PrimaryKey
    val id: String,                               // e.g. "TXN-2026-DEL-8841-0001"
    val idempotencyKey: String,                   // SHA-256 for network deduplication
    val cardId: String,
    val beneficiaryName: String,
    val maskedCardNumber: String,
    val fpsId: String,
    val dealerId: String,
    val deviceId: String,
    val periodMode: String = "MONTHLY",           // "MONTHLY", "THREE_MONTH"
    val periodDescription: String,
    val totalQuantityKg: Double,
    val totalAmountPaid: Double,
    val authMethodUsed: String,                   // "QR + FACE_BIOMETRIC", "QR + OTP"
    val riskScore: Int = 0,
    val riskLevel: String = "LOW",
    val timestamp: Long = System.currentTimeMillis(),
    val formattedDateTime: String,
    val invoiceNumber: String,
    val syncStatus: String = "PENDING_OFFLINE_SYNC", // "SYNCED_ONLINE", "PENDING_OFFLINE_SYNC", "CONFLICT_FLAGGED"
    val isOfflineCreated: Boolean = true,
    val verificationTokenSignature: String = "",
    val receiptQrPayload: String = "",
    val digitalBillHash: String = "",
    val itemsJson: String = "[]",
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null,
    val syncedAtTimestamp: Long? = null
)
