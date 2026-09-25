package com.example.security

import com.example.model.AuthMethodType
import com.example.model.BeneficiaryEntity
import com.example.model.DealerSession
import com.example.model.RiskAssessmentResult
import com.example.model.RiskFactor
import com.example.model.RiskLevel
import java.util.Calendar

object AdaptiveRiskEngine {

    fun assessRisk(
        beneficiary: BeneficiaryEntity,
        session: DealerSession,
        recentFailedAttempts: Int = 0,
        isSimulatedRiskTest: Boolean = false,
        simulatedRiskScore: Int? = null
    ): RiskAssessmentResult {
        val factors = mutableListOf<RiskFactor>()
        var score = 0

        // 1. Check Card Security Status
        if (beneficiary.cardStatus == "BLOCKED" || beneficiary.cardStatus == "REVOKED" || beneficiary.cardStatus == "LOST") {
            factors.add(
                RiskFactor(
                    factorName = "CARD_SECURITY_FLAG",
                    scoreContribution = 95,
                    description = "Card status in central PDS registry is ${beneficiary.cardStatus}. Potential unauthorized card use.",
                    isAnomaly = true
                )
            )
            score += 95
        } else {
            factors.add(
                RiskFactor(
                    factorName = "CARD_STATUS_CHECK",
                    scoreContribution = 0,
                    description = "Card is in ACTIVE and GOOD standing.",
                    isAnomaly = false
                )
            )
        }

        // 2. Portability / Inter-district Shop Check (ONORC - One Nation One Ration Card)
        if (beneficiary.registeredFpsId != session.fpsId) {
            val interDistrict = beneficiary.district != session.district
            val portScore = if (interDistrict) 20 else 10
            factors.add(
                RiskFactor(
                    factorName = "ONORC_PORTABILITY_VISIT",
                    scoreContribution = portScore,
                    description = if (interDistrict) "Beneficiary visiting from outside district (${beneficiary.district}). Portability verified."
                                  else "Beneficiary visiting neighbor FPS shop in same district.",
                    isAnomaly = false
                )
            )
            score += portScore
        } else {
            factors.add(
                RiskFactor(
                    factorName = "HOME_FPS_SHOP",
                    scoreContribution = 0,
                    description = "Transaction at beneficiary's registered home FPS shop (${session.fpsId}).",
                    isAnomaly = false
                )
            )
        }

        // 3. Device Authorization & Integrity
        if (session.deviceStatus != "AUTHORIZED") {
            factors.add(
                RiskFactor(
                    factorName = "DEVICE_INTEGRITY_ALERT",
                    scoreContribution = 45,
                    description = "POS terminal device status is ${session.deviceStatus}.",
                    isAnomaly = true
                )
            )
            score += 45
        }

        // 4. Repeated Failed Attempts Velocity
        if (recentFailedAttempts > 0) {
            val failScore = recentFailedAttempts * 15
            factors.add(
                RiskFactor(
                    factorName = "RECENT_AUTH_FAILURES",
                    scoreContribution = failScore,
                    description = "$recentFailedAttempts failed biometric/OTP attempts registered recently.",
                    isAnomaly = true
                )
            )
            score += failScore
        }

        // 5. Operating Hours Signal
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (currentHour < 6 || currentHour >= 21) {
            factors.add(
                RiskFactor(
                    factorName = "OFF_HOURS_DISTRIBUTION",
                    scoreContribution = 15,
                    description = "Transaction outside standard fair price shop daylight hours.",
                    isAnomaly = true
                )
            )
            score += 15
        }

        // Override if simulated in interactive testing
        if (isSimulatedRiskTest && simulatedRiskScore != null) {
            score = simulatedRiskScore
            factors.clear()
            factors.add(
                RiskFactor(
                    factorName = "SIMULATED_TEST_SCENARIO",
                    scoreContribution = simulatedRiskScore,
                    description = "Configured simulation mode for testing escalation workflows.",
                    isAnomaly = simulatedRiskScore > 25
                )
            )
        }

        // Bound score 0 to 100
        val finalScore = score.coerceIn(0, 100)

        val (level, requiredAuth, isPermitted) = when {
            finalScore >= 85 -> Triple(
                RiskLevel.CRITICAL,
                listOf(AuthMethodType.OFFICER_OVERRIDE),
                false
            )
            finalScore >= 60 -> Triple(
                RiskLevel.HIGH,
                listOf(AuthMethodType.FACE_BIOMETRIC, AuthMethodType.OFFICER_OVERRIDE),
                true
            )
            finalScore >= 25 -> Triple(
                RiskLevel.MEDIUM,
                listOf(AuthMethodType.FACE_BIOMETRIC, AuthMethodType.OTP, AuthMethodType.FINGERPRINT),
                true
            )
            else -> Triple(
                RiskLevel.LOW,
                listOf(AuthMethodType.QR_NFC),
                true
            )
        }

        return RiskAssessmentResult(
            totalScore = finalScore,
            level = level,
            factors = factors,
            requiredNextAuth = requiredAuth,
            isTransactionPermitted = isPermitted
        )
    }
}
