package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventId: String,
    val timestamp: Long,
    val formattedTime: String,
    val dealerId: String,
    val deviceId: String,
    val cardId: String,
    val action: String,
    val result: String,
    val riskLevel: String,
    val previousHash: String,
    val currentHash: String,
    val details: String
)
