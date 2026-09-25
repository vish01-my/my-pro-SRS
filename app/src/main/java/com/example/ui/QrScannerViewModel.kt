package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.security.CryptoEngine
import com.example.security.ParsedCardPayload
import com.example.security.TokenValidationResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State representing the status of the cryptographic QR code scan and validation.
 */
sealed class QrScanStatus {
    object Idle : QrScanStatus()
    object Scanning : QrScanStatus()
    data class Validating(val rawPayload: String) : QrScanStatus()
    data class Success(
        val parsedCard: ParsedCardPayload,
        val validationMessage: String = "Cryptographic signature verified successfully"
    ) : QrScanStatus()
    data class Error(
        val errorCode: String,
        val errorMessage: String,
        val rawPayload: String? = null
    ) : QrScanStatus()
}

data class ScannedTokenHistoryItem(
    val cardId: String,
    val timestamp: Long,
    val isValid: Boolean,
    val authSignature: String
)

data class QrScannerUiState(
    val scanStatus: QrScanStatus = QrScanStatus.Scanning,
    val hasCameraPermission: Boolean = false,
    val isTorchEnabled: Boolean = false,
    val isScanningPaused: Boolean = false,
    val lastScannedRawToken: String? = null,
    val validatedCardPayload: ParsedCardPayload? = null,
    val recentScanHistory: List<ScannedTokenHistoryItem> = emptyList(),
    val totalScansCount: Int = 0,
    val validTokensCount: Int = 0,
    val rejectedTokensCount: Int = 0
)

/**
 * ViewModel for CameraX and ML Kit powered Barcode/QR Code scanning.
 * Captures and validates Government PDS Smart Card cryptographic tokens in real-time.
 */
class QrScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QrScannerUiState())
    val uiState: StateFlow<QrScannerUiState> = _uiState.asStateFlow()

    // Anti-replay cache: track processed nonces within active session
    private val processedNonces = mutableSetOf<String>()
    private var lastScannedTimestamp: Long = 0L

    /**
     * Updates camera permission state.
     */
    fun onCameraPermissionResult(granted: Boolean) {
        _uiState.update { 
            it.copy(
                hasCameraPermission = granted,
                scanStatus = if (granted) QrScanStatus.Scanning else QrScanStatus.Error(
                    errorCode = "PERMISSION_DENIED",
                    errorMessage = "Camera permission is required to scan Smart Ration Cards."
                )
            ) 
        }
    }

    /**
     * Toggles flashlight / torch state.
     */
    fun toggleTorch() {
        _uiState.update { it.copy(isTorchEnabled = !it.isTorchEnabled) }
    }

    /**
     * Pauses barcode frame analysis.
     */
    fun pauseScanning() {
        _uiState.update { it.copy(isScanningPaused = true) }
    }

    /**
     * Resumes barcode scanning and resets validation status.
     */
    fun resumeScanning() {
        _uiState.update { 
            it.copy(
                isScanningPaused = false,
                scanStatus = QrScanStatus.Scanning,
                lastScannedRawToken = null
            ) 
        }
    }

    /**
     * Resets the scanner to a clean scanning state.
     */
    fun resetScanner() {
        _uiState.update {
            it.copy(
                scanStatus = QrScanStatus.Scanning,
                lastScannedRawToken = null,
                validatedCardPayload = null,
                isScanningPaused = false
            )
        }
    }

    /**
     * Invoked by CameraX ML Kit ImageAnalysis analyzer whenever a barcode is detected.
     */
    fun onBarcodeDetected(rawPayload: String) {
        val trimmed = rawPayload.trim()
        if (trimmed.isEmpty()) return

        // Throttle rapid repeated scans of identical content (500ms debounce)
        val now = System.currentTimeMillis()
        if (_uiState.value.isScanningPaused) return
        if (trimmed == _uiState.value.lastScannedRawToken && now - lastScannedTimestamp < 1500L) {
            return
        }
        lastScannedTimestamp = now

        // Begin validation
        validateToken(trimmed)
    }

    /**
     * Validates cryptographic payload:
     * 1. Header and structure checks ("SRC:v1:cardId:nonce:timestamp:signature")
     * 2. HMAC-SHA256 signature verification against Government root secret
     * 3. Nonce replay protection
     * 4. Timestamp validity checks
     */
    fun validateToken(rawPayload: String) {
        _uiState.update { 
            it.copy(
                isScanningPaused = true,
                lastScannedRawToken = rawPayload,
                scanStatus = QrScanStatus.Validating(rawPayload)
            ) 
        }

        viewModelScope.launch {
            // Artificial micro-delay for clean visual scanner feedback transition
            delay(150)

            when (val result = CryptoEngine.parseAndValidatePayload(rawPayload)) {
                is TokenValidationResult.Success -> {
                    val parsed = result.parsed

                    // Check for token replay within active session
                    if (processedNonces.contains(parsed.nonce)) {
                        // Potential token replay warning (still accepted if recent, but flagged)
                        _uiState.update { state ->
                            state.copy(
                                scanStatus = QrScanStatus.Error(
                                    errorCode = "TOKEN_REPLAY_DETECTED",
                                    errorMessage = "Warning: Smart token nonce was already presented in this session. Possible token replay attack.",
                                    rawPayload = rawPayload
                                ),
                                rejectedTokensCount = state.rejectedTokensCount + 1,
                                totalScansCount = state.totalScansCount + 1
                            )
                        }
                        return@launch
                    }

                    // Register nonce to prevent duplicate scans
                    processedNonces.add(parsed.nonce)

                    // Successfully validated
                    val historyItem = ScannedTokenHistoryItem(
                        cardId = parsed.cardId,
                        timestamp = System.currentTimeMillis(),
                        isValid = true,
                        authSignature = parsed.signature
                    )

                    _uiState.update { state ->
                        state.copy(
                            scanStatus = QrScanStatus.Success(parsed),
                            validatedCardPayload = parsed,
                            recentScanHistory = (listOf(historyItem) + state.recentScanHistory).take(10),
                            validTokensCount = state.validTokensCount + 1,
                            totalScansCount = state.totalScansCount + 1
                        )
                    }
                }

                is TokenValidationResult.Failure -> {
                    val historyItem = ScannedTokenHistoryItem(
                        cardId = "INVALID",
                        timestamp = System.currentTimeMillis(),
                        isValid = false,
                        authSignature = "FAILED"
                    )

                    _uiState.update { state ->
                        state.copy(
                            scanStatus = QrScanStatus.Error(
                                errorCode = result.errorCode,
                                errorMessage = result.message,
                                rawPayload = rawPayload
                            ),
                            recentScanHistory = (listOf(historyItem) + state.recentScanHistory).take(10),
                            rejectedTokensCount = state.rejectedTokensCount + 1,
                            totalScansCount = state.totalScansCount + 1
                        )
                    }
                }
            }
        }
    }

    /**
     * Simulation support for testing in emulator or headless environments.
     */
    fun simulatePreloadedCard(cardId: String, isTampered: Boolean = false) {
        val payload = if (isTampered) {
            "SRC:v1:$cardId:TAMPERED_NONCE_${System.currentTimeMillis()}:1790175000000:BAD_FORGED_SIG_123456"
        } else {
            val nonce = "NONCE_${System.currentTimeMillis() % 10000}"
            val timestamp = System.currentTimeMillis() + (30L * 86400 * 1000)
            CryptoEngine.generateSignedPayload(cardId, nonce, timestamp)
        }
        validateToken(payload)
    }
}
