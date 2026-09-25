package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * AuditLog entity representing a tamper-evident audit record in the local ledger.
 * Cryptographically chained via SHA-256 (previousHash -> currentHash).
 */
@Entity(
    tableName = "audit_logs",
    indices = [
        Index(value = ["eventId"], unique = true),
        Index(value = ["cardId"]),
        Index(value = ["timestamp"]),
        Index(value = ["syncStatus"])
    ]
)
data class AuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String,
    val dealerId: String,
    val deviceId: String,
    val cardId: String,
    val action: String,
    val result: String,
    val riskLevel: String = "LOW",
    val previousHash: String = "0000000000000000000000000000000000000000000000000000000000000000",
    val currentHash: String,
    val details: String,
    val syncStatus: String = "PENDING_SYNC"
)
