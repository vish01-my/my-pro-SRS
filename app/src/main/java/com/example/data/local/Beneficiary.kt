package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Beneficiary entity representing local cached ration card holder details
 * for offline validation, biometric matching, and quota allocation.
 */
@Entity(
    tableName = "beneficiaries",
    indices = [
        Index(value = ["cardId"], unique = true),
        Index(value = ["cardStatus"]),
        Index(value = ["registeredFpsId"])
    ]
)
data class Beneficiary(
    @PrimaryKey
    val cardId: String,                           // e.g. "SRC-DL-2026-99214"
    val maskedCardNumber: String,                 // e.g. "XXXX-XXXX-2847"
    val headOfFamilyName: String,                 // e.g. "Ramesh Kumar"
    val cardType: String,                         // "PHH", "AAY", "NPHH"
    val familyMembersCount: Int,                  // e.g. 4
    val state: String = "NCT of Delhi",
    val district: String = "Central Delhi",
    val registeredFpsId: String,                  // "FPS-110001-084"
    val cardStatus: String = "ACTIVE",            // "ACTIVE", "BLOCKED", "SUSPENDED"
    val secretCardSalt: String = "",
    val lastDistributionDate: String = "",
    val nextEligibleDate: String = "",
    val referenceFaceHash: String = "",           // Biometric template hash
    val registeredMobileMasked: String = "",       // "******9821"
    val encryptedPayload: String = "",            // Cryptographic QR payload
    val entitlementsJson: String = "[]",          // JSON array of commodity allocations
    val tokenExpiryTimestamp: Long = 0L,
    val cachedAtTimestamp: Long = System.currentTimeMillis()
)
