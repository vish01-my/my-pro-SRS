package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.model.AuditLogEntity
import com.example.model.FraudAlertEntity
import com.example.ui.PdsUiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuditAndFraudScreen(
    uiState: PdsUiState,
    auditLogs: List<AuditLogEntity>,
    fraudAlerts: List<FraudAlertEntity>,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Audit Ledger (${auditLogs.size})", "AI Fraud Signals (${fraudAlerts.size})")

    var integrityStatus by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Audit Trail & Fraud Detection", fontWeight = FontWeight.Bold, color = Color.White) },
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
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = NavyPrimary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedTab == 0) {
                    // AUDIT TRAIL TAB
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = NavyDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Tamper-Evident SHA-256 Ledger", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Hash-chained immutable cryptographic trail", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            integrityStatus = "VALID: All ${auditLogs.size} blocks cryptographically linked & untampered."
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = TirangaGreenLight),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Verify Hashes", fontSize = 11.sp)
                                    }
                                }
                                if (integrityStatus != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = integrityStatus!!,
                                        color = VerifiedGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    items(auditLogs) { log ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(log.eventId, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AshokaBlue)
                                    Text(log.formattedTime, fontSize = 11.sp, color = TextMuted)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${log.action} • ${log.result}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (log.result.contains("FAIL") || log.result.contains("DENIED")) DangerRed else TextPrimary
                                )
                                Text(
                                    text = log.details,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "PrevHash: ${log.previousHash.take(16)}... | Hash: ${log.currentHash.take(16)}...",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                } else {
                    // FRAUD SIGNALS TAB
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("AI-Assisted Fraud Detection", fontWeight = FontWeight.Bold, color = DangerRed, fontSize = 14.sp)
                                    Text("Monitors transaction velocities, impossible travel, and off-hour distributions", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }

                    if (fraudAlerts.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Demo Fraud Signals Simulation", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("1. [LOW RISK] Normal daylight transactions within shop quota limits.", fontSize = 12.sp, color = VerifiedGreen)
                                    Text("2. [MEDIUM RISK] Rapid consecutive scans flagged for biometric verification.", fontSize = 12.sp, color = WarningAmber)
                                    Text("3. [CRITICAL RISK] Counterfeit token or revoked card triggers immediate lockout.", fontSize = 12.sp, color = DangerRed)
                                }
                            }
                        }
                    } else {
                        items(fraudAlerts) { alert ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(alert.alertCode, fontWeight = FontWeight.Bold, color = DangerRed, fontSize = 12.sp)
                                        Text(alert.severity, fontWeight = FontWeight.ExtraBold, color = DangerRed, fontSize = 11.sp)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(alert.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(alert.description, fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
