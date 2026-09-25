package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biometrics.CameraFaceDetectionGuidanceComponent
import com.example.biometrics.FaceGuidanceState
import com.example.model.RiskLevel
import com.example.ui.AppNavScreen
import com.example.ui.PdsUiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceRecognitionScreen(
    uiState: PdsUiState,
    onBack: () -> Unit,
    onStartScan: (Boolean) -> Unit,
    onOtpBypass: (String) -> Unit,
    onProceed: () -> Unit
) {
    val beneficiary = uiState.activeBeneficiary
    val scanState = uiState.faceScanState
    val result = uiState.faceVerificationResult
    var showOtpDialog by remember { mutableStateOf(false) }
    var otpInput by remember { mutableStateOf("123456") }
    var captureModeTab by remember { mutableStateOf(0) } // 0: Live CameraX ML Kit, 1: Simulation Scan

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biometric Face Recognition", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Adaptive Trigger Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = WarningAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Adaptive Authentication Escalation",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF78350F)
                        )
                        Text(
                            text = "Security Risk Score: ${uiState.activeRiskAssessment?.totalScore ?: 45}/100 (${uiState.activeRiskAssessment?.level?.label ?: "MEDIUM RISK"}). Face verification required.",
                            fontSize = 11.sp,
                            color = Color(0xFF92400E)
                        )
                    }
                }
            }

            // Tab Selector for Camera vs Test Simulation
            TabRow(
                selectedTabIndex = captureModeTab,
                containerColor = Color.White,
                contentColor = NavyPrimary,
                modifier = Modifier.clip(RoundedCornerShape(8.dp))
            ) {
                Tab(
                    selected = captureModeTab == 0,
                    onClick = { captureModeTab = 0 },
                    text = { Text("Live CameraX + ML Kit", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = captureModeTab == 1,
                    onClick = { captureModeTab = 1 },
                    text = { Text("Terminal Sensor HUD", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            if (captureModeTab == 0) {
                // Live CameraX + ML Kit Face Guidance Component
                CameraFaceDetectionGuidanceComponent(
                    onFaceVerifiedAndCaptured = { guidance ->
                        if (!scanState.isScanning && (result == null || !result.isMatched)) {
                            onStartScan(false)
                        }
                    },
                    onManualCaptureRequested = {
                        onStartScan(false)
                    }
                )
            } else {
                // Secondary Terminal Sensor HUD Mode
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = beneficiary?.headOfFamilyName ?: "Beneficiary",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Ref: ${beneficiary?.referenceFaceHash?.take(16) ?: "NFSA_BIOMETRIC_REF"}",
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Result Display Card
            if (result != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.isMatched) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (result.isMatched) "BIOMETRIC MATCH CONFIRMED" else "BIOMETRIC MISMATCH",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = if (result.isMatched) VerifiedGreen else DangerRed
                            )
                            Text(
                                text = "Confidence: ${"%.1f".format(result.confidencePercentage)}% (Min: 75.0%)",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "Liveness: ML Kit Face Landmark & Blink Verified",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Icon(
                            imageVector = if (result.isMatched) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (result.isMatched) VerifiedGreen else DangerRed,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Action Buttons
            if (scanState.isScanning) {
                LinearProgressIndicator(
                    progress = { scanState.scanProgress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = SaffronAccent
                )
                Text(
                    text = "${scanState.livenessChallenge.prompt} (${(scanState.scanProgress * 100).toInt()}%)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onStartScan(false) },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronAccent)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Capture & Match", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onStartScan(true) },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Simulate Mismatch", fontSize = 12.sp)
                    }
                }

                // Fallback & Proceed Options
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { showOtpDialog = true },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Aadhaar OTP", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onProceed,
                        modifier = Modifier.weight(1f).height(46.dp),
                        enabled = uiState.isFaceBiometricCompleted,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TirangaGreenLight)
                    ) {
                        Text("Proceed", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }

    if (showOtpDialog) {
        AlertDialog(
            onDismissRequest = { showOtpDialog = false },
            title = { Text("Aadhaar-Linked Mobile OTP") },
            text = {
                Column {
                    Text(
                        "An OTP has been dispatched to registered mobile ${beneficiary?.registeredMobileMasked ?: "******9821"}.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { otpInput = it },
                        label = { Text("6-Digit OTP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Demo Test OTP: 123456", fontSize = 11.sp, color = TextMuted)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onOtpBypass(otpInput)
                        showOtpDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyLight)
                ) {
                    Text("Verify OTP")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOtpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
