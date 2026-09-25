package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LanguageManager
import com.example.localization.SupportedLanguage
import com.example.model.DealerInventoryEntity
import com.example.model.TransactionEntity
import com.example.model.UserRole
import com.example.ui.AppNavScreen
import com.example.ui.PdsUiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: PdsUiState,
    inventory: List<DealerInventoryEntity>,
    transactions: List<TransactionEntity>,
    onNavigate: (AppNavScreen) -> Unit,
    onLanguageChange: (SupportedLanguage) -> Unit,
    onRoleChange: (UserRole) -> Unit,
    onToggleOffline: () -> Unit
) {
    val lang = uiState.selectedLanguage
    var showLangMenu by remember { mutableStateOf(false) }
    var showRoleMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = LanguageManager.getString("app_name", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "NFSA Digital PDS • ${uiState.session.shopName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary
                ),
                actions = {
                    // Language Switcher
                    IconButton(onClick = { showLangMenu = true }) {
                        Icon(Icons.Default.Language, contentDescription = "Change Language", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showLangMenu,
                        onDismissRequest = { showLangMenu = false }
                    ) {
                        SupportedLanguage.values().forEach { l ->
                            DropdownMenuItem(
                                text = { Text(l.nativeName) },
                                onClick = {
                                    onLanguageChange(l)
                                    showLangMenu = false
                                }
                            )
                        }
                    }

                    // Role Switcher
                    IconButton(onClick = { showRoleMenu = true }) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = "Switch Role", tint = SaffronAccent)
                    }
                    DropdownMenu(
                        expanded = showRoleMenu,
                        onDismissRequest = { showRoleMenu = false }
                    ) {
                        UserRole.values().forEach { r ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(r.displayName, fontWeight = FontWeight.Bold)
                                        Text(r.level, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    onRoleChange(r)
                                    showRoleMenu = false
                                }
                            )
                        }
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
            // Header Info & Device Authorization Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.isOfflineMode) WarningAmber else VerifiedGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (uiState.isOfflineMode) "OFFLINE MODE (QUEUED)" else "ONLINE • SECURE TLS 1.3",
                                    color = if (uiState.isOfflineMode) SaffronLight else VerifiedGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            // Offline toggle button
                            FilledTonalButton(
                                onClick = onToggleOffline,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (uiState.isOfflineMode) WarningAmber else Color(0xFF1E293B)
                                ),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isOfflineMode) Icons.Default.CloudOff else Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (uiState.isOfflineMode) Color.Black else Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isOfflineMode) "Go Online" else "Simulate Offline",
                                    fontSize = 11.sp,
                                    color = if (uiState.isOfflineMode) Color.Black else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("FPS Shop ID", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(uiState.session.fpsId, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Dealer ID", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(uiState.session.dealerId, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column {
                                Text("Device Token", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                Text(uiState.session.deviceId, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Current Role: ${uiState.session.role.displayName} (${uiState.session.role.level})",
                            color = SaffronLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // PRIMARY BIG ACTION: VERIFY SMART RATION CARD
            item {
                Button(
                    onClick = { onNavigate(AppNavScreen.CARD_SCANNER) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronAccent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan QR",
                        modifier = Modifier.size(32.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = LanguageManager.getString("verify_card", lang).uppercase(),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Fast QR / Barcode / NFC Card Verification (<2s)",
                            fontSize = 12.sp,
                            color = Color(0xFFFFF7ED)
                        )
                    }
                }
            }

            // 4 Grid Navigation Buttons
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Offline Reconciliation
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .clickable { onNavigate(AppNavScreen.OFFLINE_RECONCILIATION) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = NavyLight)
                                if (uiState.pendingOfflineCount > 0) {
                                    Badge(containerColor = DangerRed) {
                                        Text("${uiState.pendingOfflineCount}", color = Color.White)
                                    }
                                }
                            }
                            Text(
                                text = "Offline Sync Queue",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    // Audit & Fraud Alerts
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .clickable { onNavigate(AppNavScreen.AUDIT_AND_FRAUD) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = TirangaGreenLight)
                            Text(
                                text = "Audit & Fraud AI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Grievance / Complaint
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .clickable { onNavigate(AppNavScreen.COMPLAINT_DESK) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.ReportProblem, contentDescription = null, tint = WarningAmber)
                            Text(
                                text = "Citizen Grievances",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }

                    // Thermal Print History
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .clickable { onNavigate(AppNavScreen.RECEIPT_VIEW) },
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, tint = InfoBlue)
                            Text(
                                text = "Thermal Receipts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Real-Time Dealer Inventory Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fair Price Shop Stock Inventory",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Updated Today",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            items(inventory) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = NavyLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(item.commodityName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Buffer Min: ${item.minThreshold} ${item.unit}", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${item.currentStock} ${item.unit}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = if (item.currentStock <= item.minThreshold) DangerRed else TirangaGreenLight
                                )
                                if (item.currentStock <= item.minThreshold) {
                                    Text("LOW STOCK", color = DangerRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Text("SUFFICIENT", color = VerifiedGreen, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Recent Distribution Transactions Header
            item {
                Text(
                    text = "Recent Distribution Records",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        text = "No distribution transactions recorded yet in current shift.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            } else {
                items(transactions.take(5)) { txn ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                Text(txn.beneficiaryName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Card: ${txn.maskedCardNumber} • ${txn.formattedDateTime}", fontSize = 11.sp, color = TextSecondary)
                                Text("Auth: ${txn.authMethodUsed}", fontSize = 10.sp, color = AshokaBlue)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${txn.totalQuantityKg} kg", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TirangaGreenLight)
                                Text("₹${"%.2f".format(txn.totalAmountPaid)}", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    text = if (txn.isOfflineCreated) "OFFLINE" else "ONLINE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (txn.isOfflineCreated) WarningAmber else VerifiedGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
