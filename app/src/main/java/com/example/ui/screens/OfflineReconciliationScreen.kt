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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SyncState
import com.example.model.TransactionEntity
import com.example.network.ConnectivityObserver
import com.example.ui.PdsUiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineReconciliationScreen(
    uiState: PdsUiState,
    transactions: List<TransactionEntity>,
    onBack: () -> Unit,
    onSyncNow: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit = {}
) {
    val pendingTransactions = transactions.filter { it.isOfflineCreated && it.syncStatus.contains("PENDING") }
    val isNetworkAvailable = uiState.networkStatus == ConnectivityObserver.NetworkStatus.Available
    val isSyncing = uiState.syncState is SyncState.Syncing

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Mode & Synchronization", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
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
            // Live Connectivity & Queue Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNetworkAvailable) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = when {
                                        !isNetworkAvailable -> Icons.Default.CloudOff
                                        uiState.connectionType == ConnectivityObserver.ConnectionType.WIFI -> Icons.Default.Wifi
                                        else -> Icons.Default.SignalCellularAlt
                                    },
                                    contentDescription = null,
                                    tint = if (isNetworkAvailable) VerifiedGreen else WarningAmber,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (isNetworkAvailable) "NETWORK CONNECTED (${uiState.connectionType.name})"
                                               else "OFFLINE / DISCONNECTED",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = if (isNetworkAvailable) Color(0xFF14532D) else Color(0xFF78350F)
                                    )
                                    Text(
                                        text = if (isNetworkAvailable)
                                            "Connected to State PDS Gateway. Auto-sync active."
                                        else
                                            "${pendingTransactions.size} transactions waiting in local encrypted queue",
                                        fontSize = 12.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Automated Synchronization on Restoration Toggle Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.SyncLock,
                                contentDescription = null,
                                tint = AshokaBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Auto-Sync on Network Restoration",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    "ConnectivityObserver detects network return and auto-submits offline records",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = uiState.isAutoSyncEnabled,
                            onCheckedChange = { onToggleAutoSync(it) }
                        )
                    }
                }
            }

            // Sync State Banner (Progress / Success / Conflict / Error)
            when (val sync = uiState.syncState) {
                is SyncState.Syncing -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = AshokaBlue
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    sync.message,
                                    fontSize = 13.sp,
                                    color = AshokaBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                is SyncState.Conflict -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    sync.details,
                                    fontSize = 12.sp,
                                    color = DangerRed,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                else -> Unit
            }

            // Manual Sync Button
            item {
                Button(
                    onClick = onSyncNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TirangaGreenLight),
                    enabled = !isSyncing && (pendingTransactions.isNotEmpty() || uiState.pendingOfflineCount > 0)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SYNCHRONIZING...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    } else {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SYNCHRONIZE NOW (MANUAL TRIGGER)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Offline Architecture Strategy Explanatory Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Government Offline Architecture Strategy", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "1. Secure Local Storage: SQLite database with device hardware keystore signature prevents tampering on the terminal.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "2. Cryptographic Idempotency: Every transaction gets a hash-bound nonce + card ID + period key, ensuring zero double debits.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "3. Restored Link Replay: Background ConnectivityObserver detects connectivity and automatically submits queued transactions.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "4. Server Reconciliation: Multi-shop conflict detection and inventory settlement against regional godown buffers.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Pending Queue List
            item {
                Text("Queued Offline Transactions", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
            }

            if (pendingTransactions.isEmpty()) {
                item {
                    Text(
                        "No pending offline records. All transactions are fully synchronized with the state cloud database.",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            } else {
                items(pendingTransactions) { txn ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(txn.beneficiaryName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("Card: ${txn.maskedCardNumber} • ${txn.formattedDateTime}", fontSize = 11.sp, color = TextSecondary)
                                Text("Signature: ${txn.verificationTokenSignature.take(16)}...", fontSize = 10.sp, color = AshokaBlue)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${txn.totalQuantityKg} kg", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("₹${"%.2f".format(txn.totalAmountPaid)}", fontSize = 11.sp, color = TextMuted)
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFEF3C7), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("QUEUED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = WarningAmber)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

