package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DistributionOutcome
import com.example.localization.LanguageManager
import com.example.ui.PdsUiState
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    uiState: PdsUiState,
    onFinish: () -> Unit,
    onToggleWidth: (Boolean) -> Unit
) {
    val outcome = uiState.distributionOutcome as? DistributionOutcome.Approved
    val txn = outcome?.transaction
    val lang = uiState.selectedLanguage
    val is80mm = uiState.thermalPrinterWidth80mm

    var isPrinting by remember { mutableStateOf(false) }
    var printSuccess by remember { mutableStateOf(false) }
    var smsSent by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val receiptText = if (is80mm) outcome?.receiptText80mm else outcome?.receiptText58mm

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thermal Receipt & Digital Bill", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary),
                actions = {
                    TextButton(onClick = onFinish) {
                        Text("DONE", color = SaffronLight, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SurfaceLight),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Success Header Alert
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = VerifiedGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("TRANSACTION SUCCESSFUL", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 15.sp)
                            Text(
                                if (txn?.isOfflineCreated == true) "Signed & Queued Locally (Offline Sync Active)"
                                else "Central PDS Registry Updated Atomically",
                                color = Color(0xFFDCFCE7),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Printer paper width selector (58mm vs 80mm)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Thermal Paper Roll Format:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !is80mm,
                            onClick = { onToggleWidth(false) },
                            label = { Text("58mm Standard") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NavyLight, selectedLabelColor = Color.White)
                        )
                        FilterChip(
                            selected = is80mm,
                            onClick = { onToggleWidth(true) },
                            label = { Text("80mm Wide") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NavyLight, selectedLabelColor = Color.White)
                        )
                    }
                }
            }

            // THERMAL RECEIPT PAPERING VIEW
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = ThermalPaperBg),
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = receiptText ?: "Receipt generation in progress...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = if (is80mm) 11.sp else 10.sp,
                            color = ThermalInk,
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Simulated Thermal QR Code Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.QrCode,
                                    contentDescription = "Receipt QR",
                                    modifier = Modifier.size(54.dp),
                                    tint = Color.Black
                                )
                                Text(
                                    text = "Scan to verify genuine Government NFSA digital bill",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }

            // THERMAL PRINT ACTION BUTTON
            item {
                Button(
                    onClick = {
                        scope.launch {
                            isPrinting = true
                            delay(1200) // Simulate ESC/POS thermal command stream
                            isPrinting = false
                            printSuccess = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyLight)
                ) {
                    if (isPrinting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sending ESC/POS Bytes to Thermal Printer...")
                    } else {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (printSuccess) "REPRINT THERMAL RECEIPT (${if (is80mm) "80mm" else "58mm"})"
                                   else LanguageManager.getString("print_receipt", lang) + " (${if (is80mm) "80mm" else "58mm"})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // SMS NOTIFICATION SIMULATION
            item {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            smsSent = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (smsSent) Icons.Default.Check else Icons.Default.Sms,
                        contentDescription = null,
                        tint = if (smsSent) VerifiedGreen else NavyLight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (smsSent) "SMS Confirmation Dispatched to ${uiState.activeBeneficiary?.registeredMobileMasked ?: "Citizen"}"
                               else "Dispatch SMS Notification to Beneficiary Mobile",
                        fontSize = 12.sp,
                        color = if (smsSent) VerifiedGreen else NavyLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // NEXT CITIZEN BUTTON
            item {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronAccent)
                ) {
                    Text("Complete & Return to Dashboard", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
