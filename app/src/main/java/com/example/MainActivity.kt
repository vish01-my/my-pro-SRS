package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppNavScreen
import com.example.ui.PdsViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                PdsMainApp()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

@Composable
fun PdsMainApp(viewModel: PdsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val inventory by viewModel.inventoryList.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val fraudAlerts by viewModel.fraudAlerts.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { err ->
            snackbarHostState.showSnackbar(err)
            viewModel.clearMessages()
        }
    }

    BackHandler(enabled = uiState.currentScreen != AppNavScreen.DASHBOARD) {
        when (uiState.currentScreen) {
            AppNavScreen.CARD_SCANNER,
            AppNavScreen.AUDIT_AND_FRAUD,
            AppNavScreen.COMPLAINT_DESK,
            AppNavScreen.OFFLINE_RECONCILIATION -> viewModel.navigateTo(AppNavScreen.DASHBOARD)
            AppNavScreen.QR_SCANNER -> viewModel.navigateTo(AppNavScreen.CARD_SCANNER)
            AppNavScreen.FACE_RECOGNITION -> viewModel.navigateTo(AppNavScreen.CARD_SCANNER)
            AppNavScreen.HOLDER_DETAILS -> viewModel.navigateTo(AppNavScreen.CARD_SCANNER)
            AppNavScreen.DISTRIBUTION_FORM -> viewModel.navigateTo(AppNavScreen.HOLDER_DETAILS)
            AppNavScreen.RECEIPT_VIEW -> viewModel.resetFlow()
            else -> viewModel.navigateTo(AppNavScreen.DASHBOARD)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Modifier.padding(innerPadding)

        when (uiState.currentScreen) {
            AppNavScreen.DASHBOARD -> {
                DashboardScreen(
                    uiState = uiState,
                    inventory = inventory,
                    transactions = transactions,
                    onNavigate = { viewModel.navigateTo(it) },
                    onLanguageChange = { viewModel.setLanguage(it) },
                    onRoleChange = { viewModel.setRole(it) },
                    onToggleOffline = { viewModel.toggleOfflineMode() }
                )
            }

            AppNavScreen.CARD_SCANNER -> {
                CardVerificationScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateTo(AppNavScreen.DASHBOARD) },
                    onVerifyPayload = { payload, simulateScore ->
                        viewModel.verifyCardToken(payload, simulateScore)
                    },
                    onOpenLiveScanner = { viewModel.navigateTo(AppNavScreen.QR_SCANNER) }
                )
            }

            AppNavScreen.QR_SCANNER -> {
                QrScanScreen(
                    onBack = { viewModel.navigateTo(AppNavScreen.CARD_SCANNER) },
                    onTokenValidated = { parsedCard ->
                        viewModel.verifyCardToken(parsedCard.rawPayload, null)
                    }
                )
            }

            AppNavScreen.FACE_RECOGNITION -> {
                FaceRecognitionScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateTo(AppNavScreen.CARD_SCANNER) },
                    onStartScan = { simulateMismatch ->
                        viewModel.startFaceRecognitionScan(simulateMismatch)
                    },
                    onOtpBypass = { otp ->
                        viewModel.completeBypassFaceWithOtp(otp)
                    },
                    onProceed = {
                        viewModel.navigateTo(AppNavScreen.HOLDER_DETAILS)
                    }
                )
            }

            AppNavScreen.HOLDER_DETAILS -> {
                BeneficiaryHolderScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateTo(AppNavScreen.CARD_SCANNER) },
                    onProceedToDistribute = { viewModel.navigateTo(AppNavScreen.DISTRIBUTION_FORM) }
                )
            }

            AppNavScreen.DISTRIBUTION_FORM -> {
                DistributionScreen(
                    uiState = uiState,
                    inventory = inventory,
                    onBack = { viewModel.navigateTo(AppNavScreen.HOLDER_DETAILS) },
                    onPeriodChange = { viewModel.setDistributionPeriod(it) },
                    onQuantityChange = { commId, qty -> viewModel.updateCommodityQuantity(commId, qty) },
                    onSubmitDistribution = { viewModel.submitDistribution() }
                )
            }

            AppNavScreen.RECEIPT_VIEW -> {
                ReceiptScreen(
                    uiState = uiState,
                    onFinish = { viewModel.resetFlow() },
                    onToggleWidth = { viewModel.toggleThermalWidth(it) }
                )
            }

            AppNavScreen.AUDIT_AND_FRAUD -> {
                AuditAndFraudScreen(
                    uiState = uiState,
                    auditLogs = auditLogs,
                    fraudAlerts = fraudAlerts,
                    onBack = { viewModel.navigateTo(AppNavScreen.DASHBOARD) }
                )
            }

            AppNavScreen.COMPLAINT_DESK -> {
                ComplaintScreen(
                    uiState = uiState,
                    onBack = { viewModel.navigateTo(AppNavScreen.DASHBOARD) },
                    onSubmitComplaint = { cat, desc -> viewModel.fileComplaint(cat, desc) }
                )
            }

            AppNavScreen.OFFLINE_RECONCILIATION -> {
                OfflineReconciliationScreen(
                    uiState = uiState,
                    transactions = transactions,
                    onBack = { viewModel.navigateTo(AppNavScreen.DASHBOARD) },
                    onSyncNow = { viewModel.syncOfflineTransactions() },
                    onToggleAutoSync = { viewModel.toggleAutoSync(it) }
                )
            }
        }
    }
}
