package com.example.model

enum class RiskLevel(val label: String, val thresholdDesc: String) {
    LOW("LOW RISK", "Normal transaction. QR / NFC tap sufficient."),
    MEDIUM("MEDIUM RISK", "Elevated signal. Additional authentication required (Face Verification or OTP)."),
    HIGH("HIGH RISK", "High anomaly signal. Mandatory biometric Face Verification + Dealer confirmation."),
    CRITICAL("CRITICAL RISK", "Security alert! Transaction locked, alert dispatched to District Supply Officer.")
}

enum class AuthMethodType(val displayName: String, val iconName: String) {
    QR_NFC("Smart Card QR / NFC", "qr_code_scanner"),
    FACE_BIOMETRIC("AI Face Recognition", "face"),
    FINGERPRINT("Biometric Fingerprint", "fingerprint"),
    IRIS("Biometric Iris Scan", "visibility"),
    OTP("Aadhaar-Linked Mobile OTP", "sms"),
    OFFICER_OVERRIDE("DSO Special Authorization", "admin_panel_settings")
}

data class RiskFactor(
    val factorName: String,
    val scoreContribution: Int,
    val description: String,
    val isAnomaly: Boolean
)

data class RiskAssessmentResult(
    val totalScore: Int, // 0 to 100
    val level: RiskLevel,
    val factors: List<RiskFactor>,
    val requiredNextAuth: List<AuthMethodType>,
    val isTransactionPermitted: Boolean
)
