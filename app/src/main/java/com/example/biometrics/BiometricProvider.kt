package com.example.biometrics

data class FaceVerificationResult(
    val isMatched: Boolean,
    val confidencePercentage: Double,
    val livenessVerified: Boolean,
    val matchThreshold: Double = 75.0,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class OtpVerificationResult(
    val isVerified: Boolean,
    val message: String,
    val referenceOtpId: String
)

data class FingerprintVerificationResult(
    val isMatched: Boolean,
    val minutiaeScore: Double,
    val qualityScore: Double,
    val deviceSerial: String
)

interface BiometricProvider {
    suspend fun verifyFace(capturedFeatureVector: String, referenceHash: String): FaceVerificationResult
    suspend fun verifyFingerprint(rawScanData: ByteArray, referenceTemplate: String): FingerprintVerificationResult
    suspend fun sendAadhaarLinkedOtp(maskedMobile: String): String
    suspend fun verifyAadhaarLinkedOtp(otp: String, challengeId: String): OtpVerificationResult
}

class GovernmentMockBiometricAdapter : BiometricProvider {
    override suspend fun verifyFace(capturedFeatureVector: String, referenceHash: String): FaceVerificationResult {
        // Mock algorithmic biometric feature matching
        val similarity = if (capturedFeatureVector == referenceHash || capturedFeatureVector.hashCode() % 100 > 15) {
            92.4 + (capturedFeatureVector.hashCode() % 7)
        } else {
            54.2
        }
        val isMatched = similarity >= 75.0
        return FaceVerificationResult(
            isMatched = isMatched,
            confidencePercentage = similarity,
            livenessVerified = true,
            details = if (isMatched) "Facial features matched with central NFSA biometric reference template."
                      else "Biometric mismatch: Facial landmarks below required 75% threshold."
        )
    }

    override suspend fun verifyFingerprint(rawScanData: ByteArray, referenceTemplate: String): FingerprintVerificationResult {
        return FingerprintVerificationResult(
            isMatched = true,
            minutiaeScore = 88.5,
            qualityScore = 91.0,
            deviceSerial = "STQC-RD-MFS100-88421"
        )
    }

    override suspend fun sendAadhaarLinkedOtp(maskedMobile: String): String {
        return "OTP-REQ-" + System.currentTimeMillis().toString().takeLast(6)
    }

    override suspend fun verifyAadhaarLinkedOtp(otp: String, challengeId: String): OtpVerificationResult {
        // Standard test OTP is 123456 or matching 6 digits
        val isValid = otp == "123456" || otp.length == 6
        return OtpVerificationResult(
            isVerified = isValid,
            message = if (isValid) "OTP successfully verified with UIDAI Auth Gateway."
                      else "Invalid OTP code entered. Please retry.",
            referenceOtpId = challengeId
        )
    }
}
