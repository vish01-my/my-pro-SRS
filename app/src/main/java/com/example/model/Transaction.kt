package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DistributionPeriodMode(val title: String, val description: String, val multiplier: Int) {
    MONTHLY("Current Month (1 Month)", "Distribution for current billing cycle", 1),
    THREE_MONTH("3-Month Advance (Quarterly)", "Advance distribution approved by State Food Policy", 3)
}

enum class SyncStatus {
    SYNCED_ONLINE,
    PENDING_OFFLINE_SYNC,
    SYNC_CONFLICT,
    SYNC_RECONCILED
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,                     // e.g. "TXN-2026-DEL-8841-0001"
    val idempotencyKey: String,                     // SHA-256(cardId + timestamp + dealerId + nonce)
    val cardId: String,
    val beneficiaryName: String,
    val maskedCardNumber: String,
    val fpsId: String,
    val dealerId: String,
    val deviceId: String,
    val periodMode: String,                         // "MONTHLY" or "THREE_MONTH"
    val periodDescription: String,                  // "August 2026" or "Q3 2026 (Jul-Sep)"
    val totalQuantityKg: Double,
    val totalAmountPaid: Double,
    val authMethodUsed: String,                     // "QR + FACE_BIOMETRIC", "QR + OTP", etc.
    val riskScore: Int,                             // 0 to 100
    val riskLevel: String,                          // "LOW", "MEDIUM", "HIGH", "CRITICAL"
    val timestamp: Long,
    val formattedDateTime: String,
    val invoiceNumber: String,                      // "PDS-INV-2026-9938"
    val syncStatus: String,                         // SYNCED_ONLINE, PENDING_OFFLINE_SYNC, etc.
    val isOfflineCreated: Boolean,
    val verificationTokenSignature: String,         // HMAC digital signature for verification
    val receiptQrPayload: String,                   // Secure link / token for public thermal receipt scan
    val digitalBillHash: String                     // SHA-256 for immutability
)

@Entity(tableName = "transaction_items")
data class TransactionItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: String,
    val commodityId: String,
    val commodityName: String,
    val quantity: Double,
    val unit: String,
    val ratePerUnit: Double,
    val totalCost: Double
)

data class TransactionWithItems(
    val transaction: TransactionEntity,
    val items: List<TransactionItemEntity>
)
