package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fraud_alerts")
data class FraudAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val alertCode: String,                       // "ANOMALY-VELOCITY-01"
    val severity: String,                        // "MEDIUM", "HIGH", "CRITICAL"
    val title: String,
    val description: String,
    val fpsId: String,
    val cardId: String,
    val timestamp: Long,
    val formattedTime: String,
    val isResolved: Boolean = false,
    val resolutionNotes: String = ""
)

@Entity(tableName = "complaints")
data class ComplaintEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val complaintId: String,                     // "GRV-2026-DEL-1049"
    val cardId: String,
    val beneficiaryName: String,
    val fpsId: String,
    val category: String,                        // "Wrong Quantity", "Dealer Refused", "Biometric Issue", "Excess Charge", "Stock Denied"
    val description: String,
    val status: String,                          // "SUBMITTED", "UNDER_INVESTIGATION", "ACTION_TAKEN", "RESOLVED"
    val filedDate: String,
    val resolutionRemark: String = ""
)

@Entity(tableName = "authorized_devices")
data class AuthorizedDeviceEntity(
    @PrimaryKey val deviceId: String,            // "POS-DEV-IND-8841"
    val dealerId: String,
    val fpsId: String,
    val deviceModel: String,
    val status: String,                          // "AUTHORIZED", "DISABLED", "REMOTE_LOCKED"
    val lastSyncTimestamp: Long,
    val ipAddressMasked: String,
    val isOnline: Boolean
)
