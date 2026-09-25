package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.LanguageManager
import com.example.model.RiskLevel
import com.example.ui.PdsUiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryHolderScreen(
    uiState: PdsUiState,
    onBack: () -> Unit,
    onProceedToDistribute: () -> Unit
) {
    val beneficiary = uiState.activeBeneficiary ?: return
    val entitlements = uiState.activeEntitlements
    val risk = uiState.activeRiskAssessment
    val lang = uiState.selectedLanguage

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Beneficiary Verification", fontWeight = FontWeight.Bold, color = Color.White) },
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
            // VERIFIED STATUS BADGE BANNER
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("BENEFICIARY VERIFIED", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 16.sp)
                                Text("Cryptographic Token & Biometrics Confirmed", color = Color(0xFFDCFCE7), fontSize = 11.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("STATUS: ${beneficiary.cardStatus}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            // BENEFICIARY PROFILE CARD (Data Minimization)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(NavyLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = beneficiary.headOfFamilyName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Ration Card: ${beneficiary.maskedCardNumber}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = AshokaBlue
                                )
                                Text(
                                    text = "Card Type: ${if (beneficiary.cardType == "PHH") "Priority Household (NFSA)" else "Antyodaya Anna Yojana (AAY)"}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = BorderLight)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Family Members", fontSize = 11.sp, color = TextMuted)
                                Text("${beneficiary.familyMembersCount} Units", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Home State / Dist", fontSize = 11.sp, color = TextMuted)
                                Text("${beneficiary.state}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Column {
                                Text("Mobile (Masked)", fontSize = 11.sp, color = TextMuted)
                                Text(beneficiary.registeredMobileMasked, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Last Distribution", fontSize = 11.sp, color = TextMuted)
                                Text(beneficiary.lastDistributionDate, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Next Eligible Cycle", fontSize = 11.sp, color = TextMuted)
                                Text(beneficiary.nextEligibleDate, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = VerifiedGreen)
                            }
                        }
                    }
                }
            }

            // ADAPTIVE RISK & AUTH SUMMARY
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (risk?.level) {
                            RiskLevel.LOW -> Color(0xFFF0FDF4)
                            RiskLevel.MEDIUM -> Color(0xFFFFFBEB)
                            RiskLevel.HIGH -> Color(0xFFFEF2F2)
                            else -> Color(0xFFF8FAFC)
                        }
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = AshokaBlue)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Security & Risk Rating", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(
                                text = "${risk?.level?.label} (${risk?.totalScore ?: 10}/100)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = when (risk?.level) {
                                    RiskLevel.LOW -> VerifiedGreen
                                    RiskLevel.MEDIUM -> WarningAmber
                                    RiskLevel.HIGH -> DangerRed
                                    else -> TextPrimary
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Authentication Method: ${if (uiState.faceVerificationResult?.isMatched == true) "QR Token + Biometric Face Recognition" else "QR Secure Cryptographic Validation"}",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // ENTITLEMENT TABLE
            item {
                Text(
                    text = "Current Month Quota Entitlement",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            items(entitlements) { ent ->
                val remaining = (ent.monthlyEntitlement - ent.alreadyCollectedMonth).coerceAtLeast(0.0)
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
                            Column {
                                Text(ent.commodityName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    text = if (ent.pricePerUnit == 0.0) "NFSA Free Allocation" else "Subsidized: ₹${ent.pricePerUnit}/${ent.unit}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Remaining: ${remaining} ${ent.unit}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = if (remaining > 0) TirangaGreenLight else DangerRed
                                )
                                Text(
                                    text = "Quota: ${ent.monthlyEntitlement}${ent.unit} | Issued: ${ent.alreadyCollectedMonth}${ent.unit}",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // PROCEED BUTTON
            item {
                Button(
                    onClick = onProceedToDistribute,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SaffronAccent)
                ) {
                    Text(
                        text = LanguageManager.getString("distribute", lang),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
