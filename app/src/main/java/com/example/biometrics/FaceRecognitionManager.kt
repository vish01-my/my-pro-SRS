package com.example.biometrics

import kotlinx.coroutines.delay

enum class LivenessChallenge(val prompt: String, val instruction: String) {
    LOOK_STRAIGHT("Align Face", "Position face centrally within the green oval frame"),
    BLINK_EYES("Liveness Verification", "Please slowly blink your eyes twice"),
    HOLD_STILL("Biometric Capture", "Hold steady while scanning facial biometric landmarks...")
}

data class FaceScanState(
    val currentStep: Int = 0,
    val livenessChallenge: LivenessChallenge = LivenessChallenge.LOOK_STRAIGHT,
    val isScanning: Boolean = false,
    val scanProgress: Float = 0f,
    val result: FaceVerificationResult? = null,
    val isSimulationMismatch: Boolean = false
)

object FaceRecognitionManager {

    suspend fun performFaceScan(
        referenceHash: String,
        simulateFailure: Boolean = false,
        onProgress: (FaceScanState) -> Unit
    ): FaceVerificationResult {
        // Step 1: Align Face
        onProgress(
            FaceScanState(
                currentStep = 1,
                livenessChallenge = LivenessChallenge.LOOK_STRAIGHT,
                isScanning = true,
                scanProgress = 0.25f
            )
        )
        delay(600)

        // Step 2: Liveness prompt (Blink)
        onProgress(
            FaceScanState(
                currentStep = 2,
                livenessChallenge = LivenessChallenge.BLINK_EYES,
                isScanning = true,
                scanProgress = 0.60f
            )
        )
        delay(800)

        // Step 3: Biometric feature vector extraction
        onProgress(
            FaceScanState(
                currentStep = 3,
                livenessChallenge = LivenessChallenge.HOLD_STILL,
                isScanning = true,
                scanProgress = 0.90f
            )
        )
        delay(700)

        // Step 4: Biometric comparison
        val adapter = GovernmentMockBiometricAdapter()
        val vectorToTest = if (simulateFailure) "MISMATCH_SAMPLE_HASH_99" else referenceHash
        val result = adapter.verifyFace(vectorToTest, referenceHash)

        val finalResult = if (simulateFailure) {
            result.copy(
                isMatched = false,
                confidencePercentage = 46.2,
                details = "Biometric mismatch: Facial landmarks similarity (46.2%) below 75% threshold. Please use OTP or Fingerprint."
            )
        } else {
            result.copy(
                isMatched = true,
                confidencePercentage = 95.8,
                details = "Face biometric verified (95.8% confidence). Liveness confirmed."
            )
        }

        onProgress(
            FaceScanState(
                currentStep = 4,
                livenessChallenge = LivenessChallenge.HOLD_STILL,
                isScanning = false,
                scanProgress = 1.0f,
                result = finalResult
            )
        )

        return finalResult
    }
}
