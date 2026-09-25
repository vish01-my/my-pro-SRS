package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.security.CryptoEngine
import com.example.ui.PdsUiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardVerificationScreen(
    uiState: PdsUiState,
    onBack: () -> Unit,
    onVerifyPayload: (String, Int?) -> Unit,
    onOpenLiveScanner: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Scan QR", "Barcode", "NFC Tap", "Smart Cards")

    var customPayloadInput by remember { mutableStateOf("") }

    // Pre-computed signed tokens for demo testing
    val rameshToken = remember { CryptoEngine.generateSignedPayload("SRC-DL-2026-99214", "NONCE_9941", 1790175000000L) }
    val sunitaToken = remember { CryptoEngine.generateSignedPayload("SRC-DL-2026-88102", "NONCE_8812", 1790175000000L) }
    val rajeshToken = remember { CryptoEngine.generateSignedPayload("SRC-GJ-2026-14029", "NONCE_1409", 1790175000000L) }
    val ansariToken = remember { CryptoEngine.generateSignedPayload("SRC-UP-2026-77312", "NONCE_7731", 1790175000000L) }
    val priyaToken = remember { CryptoEngine.generateSignedPayload("SRC-DL-2026-33901", "NONCE_3390", 1790175000000L) }
    val tamperedToken = remember { "SRC:v1:SRC-DL-2026-99214:TAMPERED_NONCE:1790175000000:FAKE_BAD_SIGNATURE" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verify Smart Ration Card", fontWeight = FontWeight.Bold, color = Color.White) },
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // QR Code Scanner Viewfinder
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Black),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Position Smart QR inside Frame",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Viewfinder Box
                                    Box(
                                        modifier = Modifier
                                            .size(220.dp)
                                            .border(3.dp, SaffronAccent, RoundedCornerShape(16.dp))
                                            .background(Color(0xFF1E293B))
                                            .clickable { onOpenLiveScanner() },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = "Open live camera scanner",
                                            modifier = Modifier.size(100.dp),
                                            tint = Color(0xFF64748B)
                                        )
                                        // Red laser scanning animation line simulation
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(2.dp)
                                                .background(DangerRed)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Automated detection active • Tap to launch CameraX Scanner",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = onOpenLiveScanner,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = SaffronAccent),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Launch CameraX QR Scanner", fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    OutlinedButton(
                                        onClick = { onVerifyPayload(rameshToken, null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Science, contentDescription = null, tint = SaffronAccent)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Quick Demo Scan (Ramesh Kumar)")
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Barcode Scan view
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ViewWeek,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = NavyLight
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Optical Barcode Fallback Reader",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        "Align 1D/2D Barcode on Smart Card rear panel",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { onVerifyPayload(sunitaToken, null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyLight),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Read Barcode ID")
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // NFC Tap
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape)
                                            .background(TirangaGreenContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Nfc,
                                            contentDescription = null,
                                            modifier = Modifier.size(44.dp),
                                            tint = TirangaGreenLight
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "Tap Smart Card on POS Reader",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = TextPrimary
                                    )
                                    Text(
                                        "ISO 14443 Type A contactless interface • Reads token ID without personal data leakage",
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))

                                    Button(
                                        onClick = { onVerifyPayload(rameshToken, null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TirangaGreenLight),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Tap NFC Card Now")
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Demonstration Smart Cards
                        item {
                            Text(
                                "Government Test Smart Cards",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                            Text(
                                "Select a test citizen card to verify cryptographic payload, privacy masking, and adaptive security:",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        item {
                            TestCardItem(
                                name = "Ramesh Kumar (Active PHH Card)",
                                cardNo = "XXXX-XXXX-2847",
                                subtitle = "Low Risk • Central Delhi • 4 Members",
                                badgeText = "NORMAL (LOW RISK)",
                                badgeColor = VerifiedGreen,
                                onClick = { onVerifyPayload(rameshToken, 10) }
                            )
                        }

                        item {
                            TestCardItem(
                                name = "Sunita Devi (AAY Subsidized Card)",
                                cardNo = "XXXX-XXXX-5519",
                                subtitle = "Medium Risk • Face / OTP Escalation • 5 Members",
                                badgeText = "ADAPTIVE ESCALATION",
                                badgeColor = WarningAmber,
                                onClick = { onVerifyPayload(sunitaToken, 45) }
                            )
                        }

                        item {
                            TestCardItem(
                                name = "Rajesh Patel (Gujarat ONORC Portability)",
                                cardNo = "XXXX-XXXX-9103",
                                subtitle = "Inter-State Portability Visit • Ahmedabad to Delhi",
                                badgeText = "ONORC VISIT",
                                badgeColor = InfoBlue,
                                onClick = { onVerifyPayload(rajeshToken, 20) }
                            )
                        }

                        item {
                            TestCardItem(
                                name = "Mohammed Ansari (BLOCKED Card)",
                                cardNo = "XXXX-XXXX-4420",
                                subtitle = "Suspicious duplicate flag • Rejection test",
                                badgeText = "BLOCKED / DENY",
                                badgeColor = DangerRed,
                                onClick = { onVerifyPayload(ansariToken, null) }
                            )
                        }

                        item {
                            TestCardItem(
                                name = "Priya Sharma (LOST Card)",
                                cardNo = "XXXX-XXXX-1188",
                                subtitle = "Reported lost • Replaced credential",
                                badgeText = "INVALIDATED",
                                badgeColor = DangerRed,
                                onClick = { onVerifyPayload(priyaToken, null) }
                            )
                        }

                        item {
                            TestCardItem(
                                name = "Counterfeit / Tampered QR Token",
                                cardNo = "TAMPERED PAYLOAD",
                                subtitle = "Altered signature or nonce • Cryptographic rejection",
                                badgeText = "FORGERY CHECK",
                                badgeColor = CriticalRed,
                                onClick = { onVerifyPayload(tamperedToken, null) }
                            )
                        }
                    }
                }

                // Security & Privacy Note
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AshokaBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Privacy Assurance: The QR code or NFC payload contains zero personal data, no Aadhaar number, and no family records. Only the cryptographic token is transmitted over TLS 1.3.",
                                fontSize = 11.sp,
                                color = NavyPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TestCardItem(
    name: String,
    cardNo: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(cardNo, fontSize = 12.sp, color = AshokaBlue, fontWeight = FontWeight.SemiBold)
                Text(subtitle, fontSize = 11.sp, color = TextSecondary)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(badgeText, color = badgeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
