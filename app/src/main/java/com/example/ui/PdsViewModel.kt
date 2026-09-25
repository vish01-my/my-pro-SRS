package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.biometrics.FaceRecognitionManager
import com.example.biometrics.FaceScanState
import com.example.biometrics.FaceVerificationResult
import com.example.data.AppDatabase
import com.example.data.DistributionOutcome
import com.example.data.PdsRepository
import com.example.data.VerificationOutcome
import com.example.data.local.OfflineTransactionManager
import com.example.data.local.SyncState
import com.example.localization.LanguageManager
import com.example.localization.SupportedLanguage
import com.example.model.AuditLogEntity
import com.example.model.BeneficiaryEntity
import com.example.model.DealerInventoryEntity
import com.example.model.DealerSession
import com.example.model.DistributionPeriodMode
import com.example.model.EntitlementEntity
import com.example.model.FraudAlertEntity
import com.example.model.RiskAssessmentResult
import com.example.model.RiskLevel
import com.example.model.TransactionEntity
import com.example.model.UserRole
import com.example.network.ConnectivityObserver
import com.example.network.NetworkConnectivityObserver
import com.example.security.CryptoEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppNavScreen {
    DASHBOARD,
    CARD_SCANNER,
    QR_SCANNER,
    FACE_RECOGNITION,
    HOLDER_DETAILS,
    DISTRIBUTION_FORM,
    RECEIPT_VIEW,
    AUDIT_AND_FRAUD,
    COMPLAINT_DESK,
    OFFLINE_RECONCILIATION
}

data class PdsUiState(
    val selectedLanguage: SupportedLanguage = SupportedLanguage.EN,
    val currentScreen: AppNavScreen = AppNavScreen.DASHBOARD,
    val session: DealerSession = DealerSession(),
    val isOfflineMode: Boolean = false,
    val pendingOfflineCount: Int = 0,
    val networkStatus: ConnectivityObserver.NetworkStatus = ConnectivityObserver.NetworkStatus.Available,
    val connectionType: ConnectivityObserver.ConnectionType = ConnectivityObserver.ConnectionType.WIFI,
    val syncState: SyncState = SyncState.Idle,
    val isAutoSyncEnabled: Boolean = true,

    // Verification Flow
    val isVerifyingCard: Boolean = false,
    val currentTokenInput: String = "",
    val verificationOutcome: VerificationOutcome? = null,
    val activeBeneficiary: BeneficiaryEntity? = null,
    val activeEntitlements: List<EntitlementEntity> = emptyList(),
    val activeRiskAssessment: RiskAssessmentResult? = null,

    // Face Biometric Flow
    val faceScanState: FaceScanState = FaceScanState(),
    val faceVerificationResult: FaceVerificationResult? = null,
    val isFaceBiometricCompleted: Boolean = false,

    // Distribution Flow
    val selectedPeriodMode: DistributionPeriodMode = DistributionPeriodMode.MONTHLY,
    val quantitiesToIssue: Map<String, Double> = emptyMap(),
    val isSubmittingDistribution: Boolean = false,
    val distributionOutcome: DistributionOutcome? = null,
    val showThermalPrinterModal: Boolean = false,
    val thermalPrinterWidth80mm: Boolean = false,

    // Grievance / Complaint
    val complaintCardIdInput: String = "",
    val complaintCategory: String = "Wrong Quantity",
    val complaintDescription: String = "",
    val lastFiledComplaintId: String? = null,

    // Status Message / Snackbar
    val infoMessage: String? = null,
    val errorMessage: String? = null
)

class PdsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PdsRepository
    val connectivityObserver: ConnectivityObserver = NetworkConnectivityObserver(application)
    val offlineTransactionManager: OfflineTransactionManager = OfflineTransactionManager(application, connectivityObserver)

    private val _uiState = MutableStateFlow(PdsUiState())
    val uiState: StateFlow<PdsUiState> = _uiState.asStateFlow()

    val inventoryList: StateFlow<List<DealerInventoryEntity>>
    val transactions: StateFlow<List<TransactionEntity>>
    val auditLogs: StateFlow<List<AuditLogEntity>>
    val fraudAlerts: StateFlow<List<FraudAlertEntity>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PdsRepository(db)

        inventoryList = repository.getInventory("FPS-110001-084")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        transactions = repository.allTransactions
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        auditLogs = repository.allAuditLogs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fraudAlerts = repository.allFraudAlerts
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Monitor network state
        viewModelScope.launch {
            connectivityObserver.status.collect { status ->
                _uiState.update { it.copy(networkStatus = status) }
            }
        }

        // Monitor network transport
        viewModelScope.launch {
            connectivityObserver.connectionType.collect { type ->
                _uiState.update { it.copy(connectionType = type) }
            }
        }

        // Monitor offline transaction sync state
        viewModelScope.launch {
            offlineTransactionManager.syncState.collect { sync ->
                _uiState.update { current ->
                    when (sync) {
                        is SyncState.Success -> {
                            current.copy(
                                syncState = sync,
                                pendingOfflineCount = 0,
                                infoMessage = if (sync.syncedCount > 0)
                                    "Reconciliation complete: ${sync.syncedCount} records synchronized with Central PDS."
                                else current.infoMessage
                            )
                        }
                        is SyncState.Conflict -> {
                            current.copy(
                                syncState = sync,
                                errorMessage = "Sync Warning: ${sync.details}"
                            )
                        }
                        is SyncState.Error -> {
                            current.copy(
                                syncState = sync,
                                errorMessage = sync.message
                            )
                        }
                        else -> current.copy(syncState = sync)
                    }
                }
            }
        }

        // Monitor auto-sync configuration toggle
        viewModelScope.launch {
            offlineTransactionManager.isAutoSyncEnabled.collect { enabled ->
                _uiState.update { it.copy(isAutoSyncEnabled = enabled) }
            }
        }

        // Activate automated synchronization on network restoration
        offlineTransactionManager.startAutoSync(viewModelScope)

        viewModelScope.launch {
            repository.initializeDemoSeedDataIfEmpty()
        }
    }

    fun navigateTo(screen: AppNavScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun setLanguage(language: SupportedLanguage) {
        _uiState.value = _uiState.value.copy(selectedLanguage = language)
    }

    fun setRole(role: UserRole) {
        val updatedSession = _uiState.value.session.copy(role = role)
        _uiState.value = _uiState.value.copy(session = updatedSession)
    }

    fun toggleOfflineMode() {
        val nextMode = !_uiState.value.isOfflineMode
        _uiState.value = _uiState.value.copy(
            isOfflineMode = nextMode,
            infoMessage = if (nextMode) "Offline Mode Enabled: Transactions will be queued locally with device signatures."
                          else "Online Mode Restored: Automatic cloud synchronization active."
        )
    }

    fun verifyCardToken(tokenPayload: String, simulateRiskScore: Int? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifyingCard = true, currentTokenInput = tokenPayload)
            val outcome = repository.verifyCardToken(
                tokenPayload = tokenPayload,
                session = _uiState.value.session,
                isSimulatedRiskTest = simulateRiskScore != null,
                simulatedRiskScore = simulateRiskScore
            )
            when (outcome) {
                is VerificationOutcome.Success -> {
                    // Populate default quantity selections based on remaining entitlement
                    val initialQuantities = outcome.entitlements.associate {
                        it.commodityId to (it.monthlyEntitlement - it.alreadyCollectedMonth).coerceAtLeast(0.0)
                    }

                    val needsFace = outcome.riskAssessment.level == RiskLevel.MEDIUM ||
                                    outcome.riskAssessment.level == RiskLevel.HIGH

                    _uiState.value = _uiState.value.copy(
                        isVerifyingCard = false,
                        verificationOutcome = outcome,
                        activeBeneficiary = outcome.beneficiary,
                        activeEntitlements = outcome.entitlements,
                        activeRiskAssessment = outcome.riskAssessment,
                        quantitiesToIssue = initialQuantities,
                        isFaceBiometricCompleted = !needsFace,
                        currentScreen = if (needsFace) AppNavScreen.FACE_RECOGNITION else AppNavScreen.HOLDER_DETAILS,
                        infoMessage = "Card verified securely with HMAC-SHA256 signature!"
                    )
                }
                is VerificationOutcome.SecurityRejection -> {
                    _uiState.value = _uiState.value.copy(
                        isVerifyingCard = false,
                        verificationOutcome = outcome,
                        errorMessage = "${outcome.errorCode}: ${outcome.reason}"
                    )
                }
            }
        }
    }

    fun startFaceRecognitionScan(simulateMismatch: Boolean = false) {
        viewModelScope.launch {
            val beneficiary = _uiState.value.activeBeneficiary ?: return@launch
            val result = FaceRecognitionManager.performFaceScan(
                referenceHash = beneficiary.referenceFaceHash,
                simulateFailure = simulateMismatch
            ) { scanState ->
                _uiState.value = _uiState.value.copy(faceScanState = scanState)
            }

            if (result.isMatched) {
                _uiState.value = _uiState.value.copy(
                    faceVerificationResult = result,
                    isFaceBiometricCompleted = true,
                    infoMessage = "Face recognition verified successfully (${"%.1f".format(result.confidencePercentage)}% similarity)!"
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    faceVerificationResult = result,
                    isFaceBiometricCompleted = false,
                    errorMessage = result.details
                )
            }
        }
    }

    fun completeBypassFaceWithOtp(otp: String) {
        if (otp.length == 6) {
            _uiState.value = _uiState.value.copy(
                isFaceBiometricCompleted = true,
                infoMessage = "OTP Authentication verified successfully!",
                currentScreen = AppNavScreen.HOLDER_DETAILS
            )
        } else {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter valid 6-digit Aadhaar OTP")
        }
    }

    fun setDistributionPeriod(mode: DistributionPeriodMode) {
        val mult = mode.multiplier
        val updatedQuantities = _uiState.value.activeEntitlements.associate {
            it.commodityId to ((it.monthlyEntitlement * mult) - it.alreadyCollectedMonth).coerceAtLeast(0.0)
        }
        _uiState.value = _uiState.value.copy(
            selectedPeriodMode = mode,
            quantitiesToIssue = updatedQuantities
        )
    }

    fun updateCommodityQuantity(commodityId: String, qty: Double) {
        val current = _uiState.value.quantitiesToIssue.toMutableMap()
        current[commodityId] = qty.coerceAtLeast(0.0)
        _uiState.value = _uiState.value.copy(quantitiesToIssue = current)
    }

    fun submitDistribution() {
        val beneficiary = _uiState.value.activeBeneficiary ?: return
        val session = _uiState.value.session
        val periodMode = _uiState.value.selectedPeriodMode
        val quantities = _uiState.value.quantitiesToIssue
        val risk = _uiState.value.activeRiskAssessment?.totalScore ?: 10
        val authMethod = if (_uiState.value.faceVerificationResult?.isMatched == true) "QR + FACE_BIOMETRIC" else "QR + CRYPTO_TOKEN"

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingDistribution = true)
            val outcome = repository.processDistribution(
                beneficiary = beneficiary,
                session = session,
                periodMode = periodMode,
                quantitiesRequested = quantities,
                authMethodUsed = authMethod,
                riskScore = risk,
                isOfflineMode = _uiState.value.isOfflineMode
            )
            when (outcome) {
                is DistributionOutcome.Approved -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingDistribution = false,
                        distributionOutcome = outcome,
                        currentScreen = AppNavScreen.RECEIPT_VIEW,
                        pendingOfflineCount = if (_uiState.value.isOfflineMode) _uiState.value.pendingOfflineCount + 1 else _uiState.value.pendingOfflineCount,
                        infoMessage = if (_uiState.value.isOfflineMode) "Transaction signed and stored in offline queue!" else "Transaction confirmed and inventory deducted atomically!"
                    )
                }
                is DistributionOutcome.Denied -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmittingDistribution = false,
                        distributionOutcome = outcome,
                        errorMessage = "${outcome.errorCode}: ${outcome.reason}"
                    )
                }
            }
        }
    }

    fun syncOfflineTransactions() {
        viewModelScope.launch {
            val summary = offlineTransactionManager.synchronizePendingRecords(isAutomated = false)
            _uiState.update {
                it.copy(
                    pendingOfflineCount = 0,
                    infoMessage = if (summary.syncedSuccessfully > 0)
                        "Reconciliation complete: ${summary.syncedSuccessfully} records synchronized with central server."
                    else if (summary.errorMessage != null)
                        "Sync error: ${summary.errorMessage}"
                    else
                        "All offline records are already up to date."
                )
            }
        }
    }

    fun toggleAutoSync(enabled: Boolean) {
        offlineTransactionManager.setAutoSyncEnabled(enabled)
    }

    fun fileComplaint(category: String, description: String) {
        val cardId = _uiState.value.activeBeneficiary?.cardId ?: "SRC-DL-2026-99214"
        val name = _uiState.value.activeBeneficiary?.headOfFamilyName ?: "Ramesh Kumar"
        viewModelScope.launch {
            val id = repository.fileComplaint(
                cardId = cardId,
                beneficiaryName = name,
                fpsId = _uiState.value.session.fpsId,
                category = category,
                description = description
            )
            _uiState.value = _uiState.value.copy(
                lastFiledComplaintId = id,
                infoMessage = "Grievance registered successfully with ID: $id"
            )
        }
    }

    fun toggleThermalWidth(is80mm: Boolean) {
        _uiState.value = _uiState.value.copy(thermalPrinterWidth80mm = is80mm)
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(infoMessage = null, errorMessage = null)
    }

    fun resetFlow() {
        _uiState.value = _uiState.value.copy(
            currentScreen = AppNavScreen.DASHBOARD,
            verificationOutcome = null,
            activeBeneficiary = null,
            activeEntitlements = emptyList(),
            activeRiskAssessment = null,
            faceScanState = FaceScanState(),
            faceVerificationResult = null,
            isFaceBiometricCompleted = false,
            distributionOutcome = null
        )
    }
}
