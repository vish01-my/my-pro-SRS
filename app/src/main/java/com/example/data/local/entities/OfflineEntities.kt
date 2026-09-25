package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Offline Transaction Entity for POS Local Persistence.
 * 
 * Supports offline queueing, idempotent synchronization, and reconciliation.
 * Idempotency key guarantees no duplicate transactions even under erratic 2G/EDGE network retries.
 */
@Entity(
    tableName = "offline_transactions",
    indices = [
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["syncStatus"]),
        Index(value = ["cardId"]),
        Index(value = ["createdAtTimestamp"])
    ]
)
data class OfflineTransactionEntity(
    @PrimaryKey
    val transactionId: String,
    val idempotencyKey: String,
    val invoiceNumber: String,
    val cardId: String,
    val beneficiaryName: String,
    val maskedCardNumber: String,
    val fpsId: String,
    val dealerId: String,
    val posDeviceId: String,
    val periodMode: String, // "MONTHLY", "THREE_MONTH"
    val periodDescription: String,
    val itemsJson: String, // Serialized list of items: [{commodityId, name, quantity, unit, rate, cost}]
    val totalQuantityKg: Double,
    val totalAmountPaid: Double,
    val authMethodUsed: String,
    val riskScore: Int,
    val riskLevel: String,
    val digitalBillHash: String, // SHA-256 of the invoice contents
    val verificationTokenSignature: String,
    val receiptQrPayload: String,
    val syncStatus: String, // "PENDING_OFFLINE_SYNC", "SYNCED_ONLINE", "CONFLICT_FLAGGED", "SYNC_RECONCILED"
    val isOfflineCreated: Boolean = true,
    val syncAttempts: Int = 0,
    val lastSyncError: String? = null,
    val createdAtTimestamp: Long = System.currentTimeMillis(),
    val syncedAtTimestamp: Long? = null
)

/**
 * Local Tamper-Evident Audit Log Entity.
 * 
 * Implements a local blockchain-style SHA-256 hash chain so that even if the POS terminal
 * is operating completely disconnected, every action (card scan, biometric check, offline sale)
 * is recorded with cryptographic integrity.
 */
@Entity(
    tableName = "local_audit_logs",
    indices = [
        Index(value = ["eventId"], unique = true),
        Index(value = ["cardId"]),
        Index(value = ["syncStatus"]),
        Index(value = ["timestampEpochMs"])
    ]
)
data class LocalAuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: String,
    val timestampEpochMs: Long,
    val formattedTime: String,
    val dealerId: String,
    val deviceId: String,
    val cardId: String,
    val action: String,
    val result: String,
    val riskLevel: String,
    val previousHash: String, // Pointer to previous record's currentHash
    val currentHash: String,  // SHA-256(prevHash:eventId:timestamp:action:result)
    val details: String,
    val syncStatus: String = "PENDING_SYNC" // "PENDING_SYNC", "SYNCED"
)

/**
 * Encrypted Card Token Cache Entity.
 * 
 * Securely caches cryptographically signed Smart Ration Card tokens on the POS terminal.
 * Enables zero-connectivity card verification in remote Fair Price Shops while strictly adhering
 * to Privacy-by-Design (no raw Aadhaar or unmasked PII).
 */
@Entity(
    tableName = "cached_card_tokens",
    indices = [
        Index(value = ["cardId"], unique = true),
        Index(value = ["cardStatus"]),
        Index(value = ["tokenExpiryTimestamp"])
    ]
)
data class EncryptedCardTokenEntity(
    @PrimaryKey
    val cardId: String,
    val encryptedPayload: String, // "SRC:v1:<cardId>:<nonce>:<expiry>:<signature>"
    val maskedCardNumber: String,
    val headOfFamilyName: String,
    val cardType: String, // "PHH", "AAY", "APL"
    val familyMembersCount: Int,
    val registeredFpsId: String,
    val cardStatus: String, // "ACTIVE", "BLOCKED", "LOST", "SUSPENDED"
    val referenceFaceHash: String,
    val registeredMobileMasked: String,
    val entitlementsJson: String, // JSON array of available quotas
    val tokenExpiryTimestamp: Long,
    val cachedAtTimestamp: Long = System.currentTimeMillis(),
    val isRevoked: Boolean = false
)

/**
 * Local Dealer Commodity Inventory Entity.
 * 
 * Maintained locally to ensure atomic inventory decrements during offline distribution,
 * preventing physical stock depletion from diverging from digital quota records.
 */
@Entity(
    tableName = "local_dealer_inventory",
    indices = [
        Index(value = ["commodityId"], unique = true)
    ]
)
data class LocalDealerInventoryEntity(
    @PrimaryKey
    val commodityId: String,
    val commodityName: String,
    val unit: String,
    val currentStock: Double,
    val minThreshold: Double = 50.0,
    val lastUpdatedTimestamp: Long = System.currentTimeMillis()
)
