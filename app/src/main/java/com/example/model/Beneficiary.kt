package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RationCardType(val code: String, val fullName: String, val subsidyTier: String) {
    AAY("AAY", "Antyodaya Anna Yojana (Poorest of Poor)", "Subsidized / Free (35 kg/family)"),
    PHH("PHH", "Priority Household (NFSA)", "Subsidized (5 kg/member)"),
    NPHH("NPHH", "Non-Priority Household", "State Subsidized Rate")
}

enum class CardSecurityStatus {
    ACTIVE,
    BLOCKED,
    LOST,
    EXPIRED,
    SUSPENDED,
    REVOKED,
    REPLACED,
    PENDING_VERIFICATION
}

@Entity(tableName = "beneficiaries")
data class BeneficiaryEntity(
    @PrimaryKey val cardId: String,               // e.g. "SRC-DL-2026-99214"
    val maskedCardNumber: String,                 // e.g. "XXXX-XXXX-2847"
    val headOfFamilyName: String,                 // e.g. "Ramesh Kumar"
    val cardType: String,                         // "PHH", "AAY", "NPHH"
    val familyMembersCount: Int,                  // e.g. 4
    val state: String,                            // "NCT of Delhi"
    val district: String,                         // "Central Delhi"
    val registeredFpsId: String,                  // "FPS-110001-084"
    val cardStatus: String,                       // "ACTIVE", "BLOCKED", etc.
    val secretCardSalt: String,                   // cryptographic key salt
    val lastDistributionDate: String,             // "01-08-2026"
    val nextEligibleDate: String,                 // "01-09-2026"
    val referenceFaceHash: String,                // Feature vector hash for facial biometric match
    val registeredMobileMasked: String            // "******9821"
)

@Entity(tableName = "entitlements")
data class EntitlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: String,
    val commodityId: String,                      // "RICE", "WHEAT", "SUGAR", etc.
    val commodityName: String,
    val unit: String,                             // "kg", "litre"
    val monthlyEntitlement: Double,
    val alreadyCollectedMonth: Double,
    val pricePerUnit: Double,
    val monthYear: String                         // "08-2026"
)

data class BeneficiaryFullDetails(
    val beneficiary: BeneficiaryEntity,
    val entitlements: List<EntitlementEntity>
)
