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
import com.example.localization.LanguageManager
import com.example.model.DealerInventoryEntity
import com.example.model.DistributionPeriodMode
import com.example.ui.PdsUiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributionScreen(
    uiState: PdsUiState,
    inventory: List<DealerInventoryEntity>,
    onBack: () -> Unit,
    onPeriodChange: (DistributionPeriodMode) -> Unit,
    onQuantityChange: (String, Double) -> Unit,
    onSubmitDistribution: () -> Unit
) {
    val beneficiary = uiState.activeBeneficiary ?: return
    val entitlements = uiState.activeEntitlements
    val period = uiState.selectedPeriodMode
    val quantities = uiState.quantitiesToIssue
    val lang = uiState.selectedLanguage
    var showConfirmDialog by remember { mutableStateOf(false) }

    val stockMap = remember(inventory) { inventory.associateBy { it.commodityId } }

    var totalKg by remember { mutableDoubleStateOf(0.0) }
    var totalCost by remember { mutableDoubleStateOf(0.0) }

    LaunchedEffect(quantities, period) {
        var kg = 0.0
        var cost = 0.0
        quantities.forEach { (commId, qty) ->
            val ent = entitlements.find { it.commodityId == commId }
            if (ent != null) {
                kg += qty
                cost += qty * ent.pricePerUnit
            }
        }
        totalKg = kg
        totalCost = cost
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Commodity Distribution", fontWeight = FontWeight.Bold, color = Color.White) },
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
            // Citizen Header Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(beneficiary.headOfFamilyName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Card: ${beneficiary.maskedCardNumber} • ${beneficiary.cardType}", fontSize = 12.sp, color = AshokaBlue)
                        }
                        Box(
                            modifier = Modifier
                                .background(TirangaGreenContainer, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("ACTIVE ELIGIBLE", color = TirangaGreenLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // DISTRIBUTION PERIOD SELECTOR (Monthly vs 3-Month Policy)
            item {
                Text(
                    text = "Distribution Period Policy",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = period == DistributionPeriodMode.MONTHLY,
                        onClick = { onPeriodChange(DistributionPeriodMode.MONTHLY) },
                        label = { Text(period.title) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyLight,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = period == DistributionPeriodMode.THREE_MONTH,
                        onClick = { onPeriodChange(DistributionPeriodMode.THREE_MONTH) },
                        label = { Text("3-Month Advance (Q3)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyLight,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // COMMODITY DISTRIBUTION LIST
            item {
                Text(
                    text = "Issue Quantities",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
            }

            items(entitlements) { ent ->
                val mult = period.multiplier
                val maxAllowed = ((ent.monthlyEntitlement * mult) - ent.alreadyCollectedMonth).coerceAtLeast(0.0)
                val currentStock = stockMap[ent.commodityId]?.currentStock ?: 0.0
                val selectedQty = quantities[ent.commodityId] ?: 0.0
                val isStockSufficient = currentStock >= selectedQty
                val isEntitlementValid = selectedQty <= maxAllowed

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
                                    text = "Max Entitled: ${maxAllowed} ${ent.unit} | FPS Stock: ${currentStock} ${ent.unit}",
                                    fontSize = 11.sp,
                                    color = if (currentStock < maxAllowed) DangerRed else TextSecondary
                                )
                            }
                            Text(
                                text = if (ent.pricePerUnit == 0.0) "FREE" else "₹${ent.pricePerUnit}/${ent.unit}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AshokaBlue
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quantity +/- Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onQuantityChange(ent.commodityId, maxAllowed.coerceAtMost(currentStock)) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Set Max (${maxAllowed.coerceAtMost(currentStock).toInt()} ${ent.unit})", fontSize = 11.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onQuantityChange(ent.commodityId, (selectedQty - 1.0).coerceAtLeast(0.0)) },
                                    enabled = selectedQty > 0
                                ) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                                }

                                Text(
                                    text = "${selectedQty} ${ent.unit}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = if (!isStockSufficient || !isEntitlementValid) DangerRed else TextPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                IconButton(
                                    onClick = { onQuantityChange(ent.commodityId, (selectedQty + 1.0).coerceAtMost(maxAllowed).coerceAtMost(currentStock)) },
                                    enabled = selectedQty < maxAllowed && selectedQty < currentStock
                                ) {
                                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                                }
                            }
                        }

                        if (!isStockSufficient) {
                            Text(
                                "Warning: Requested quantity exceeds shop inventory stock!",
                                color = DangerRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // SUMMARY TOTALS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = NavyDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Commodity Issued", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text("${totalKg} kg", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Subsidized Bill Amount", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            Text("₹${"%.2f".format(totalCost)}", color = SaffronLight, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Idempotency Protection Active • Auto Stock Deduction",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // CONFIRM ACTION BUTTON
            item {
                Button(
                    onClick = { showConfirmDialog = true },
                    enabled = totalKg > 0 && !uiState.isSubmittingDistribution,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TirangaGreenLight)
                ) {
                    if (uiState.isSubmittingDistribution) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = LanguageManager.getString("confirm_distribution", lang),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Final Distribution Approval") },
            text = {
                Column {
                    Text("BENEFICIARY VERIFIED", fontWeight = FontWeight.Bold, color = VerifiedGreen)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Citizen: ${beneficiary.headOfFamilyName} (${beneficiary.maskedCardNumber})")
                    Text("Period: ${if (period == DistributionPeriodMode.THREE_MONTH) "Q3 2026 (Aug-Oct Advance)" else "August 2026"}")
                    Text("Total Quantity: ${totalKg} kg")
                    Text("Amount Payable: ₹${"%.2f".format(totalCost)}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "After confirmation, dealer inventory will be updated atomically and an immutable receipt will be generated.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onSubmitDistribution()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TirangaGreenLight)
                ) {
                    Text("Confirm & Deduct Stock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
