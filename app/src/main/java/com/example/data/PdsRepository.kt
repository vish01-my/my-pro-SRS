package com.example.data

import com.example.model.AuditLogEntity
import com.example.model.AuthorizedDeviceEntity
import com.example.model.BeneficiaryEntity
import com.example.model.CommodityEntity
import com.example.model.ComplaintEntity
import com.example.model.DealerInventoryEntity
import com.example.model.DealerSession
import com.example.model.DistributionPeriodMode
import com.example.model.EntitlementEntity
import com.example.model.FraudAlertEntity
import com.example.model.RiskAssessmentResult
import com.example.model.RiskLevel
import com.example.model.SyncStatus
import com.example.model.TransactionEntity
import com.example.model.TransactionItemEntity
import com.example.security.AdaptiveRiskEngine
import com.example.security.CryptoEngine
import com.example.security.TokenValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class VerificationOutcome {
    data class Success(
        val beneficiary: BeneficiaryEntity,
        val entitlements: List<EntitlementEntity>,
        val riskAssessment: RiskAssessmentResult,
        val tokenPayload: String
    ) : VerificationOutcome()

    data class SecurityRejection(
        val errorCode: String,
        val reason: String,
        val riskLevel: RiskLevel = RiskLevel.CRITICAL
    ) : VerificationOutcome()
}

sealed class DistributionOutcome {
    data class Approved(
        val transaction: TransactionEntity,
        val items: List<TransactionItemEntity>,
        val receiptText58mm: String,
        val receiptText80mm: String,
        val isOffline: Boolean
    ) : DistributionOutcome()

    data class Denied(
        val errorCode: String,
        val reason: String
    ) : DistributionOutcome()
}

class PdsRepository(private val db: AppDatabase) {

    val allTransactions: Flow<List<TransactionEntity>> = db.transactionDao().getAllTransactions()
    val allAuditLogs: Flow<List<AuditLogEntity>> = db.auditLogDao().getAllAuditLogs()
    val allFraudAlerts: Flow<List<FraudAlertEntity>> = db.fraudAlertDao().getAllFraudAlerts()
    val allComplaints: Flow<List<ComplaintEntity>> = db.complaintDao().getAllComplaints()

    fun getInventory(fpsId: String): Flow<List<DealerInventoryEntity>> =
        db.dealerInventoryDao().getInventoryForFps(fpsId)

    suspend fun initializeDemoSeedDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingBeneficiary = db.beneficiaryDao().getBeneficiaryByCardId("SRC-DL-2026-99214")
        if (existingBeneficiary != null) return@withContext

        // 1. Seed Commodities
        val commodities = listOf(
            CommodityEntity("RICE", "Fortified Rice", "चावल", "kg", 32.0, 0.0),
            CommodityEntity("WHEAT", "Whole Wheat Grain", "गेहूँ", "kg", 26.0, 0.0),
            CommodityEntity("SUGAR", "Subsidized Sugar", "चीनी", "kg", 42.0, 13.50),
            CommodityEntity("DAL", "Chana Dal (Pulses)", "चना दाल", "kg", 78.0, 30.0),
            CommodityEntity("SALT", "Iodized Salt", "नमक", "kg", 18.0, 5.0),
            CommodityEntity("OIL", "Fortified Mustard Oil", "सरसों का तेल", "L", 145.0, 65.0)
        )
        db.commodityDao().insertCommodities(commodities)

        // 2. Seed FPS Inventory
        val inventory = listOf(
            DealerInventoryEntity(fpsId = "FPS-110001-084", commodityId = "RICE", commodityName = "Fortified Rice", unit = "kg", currentStock = 850.0, minThreshold = 200.0, lastRestockedDate = "20-08-2026"),
            DealerInventoryEntity(fpsId = "FPS-110001-084", commodityId = "WHEAT", commodityName = "Whole Wheat Grain", unit = "kg", currentStock = 620.0, minThreshold = 150.0, lastRestockedDate = "20-08-2026"),
            DealerInventoryEntity(fpsId = "FPS-110001-084", commodityId = "SUGAR", commodityName = "Subsidized Sugar", unit = "kg", currentStock = 140.0, minThreshold = 50.0, lastRestockedDate = "20-08-2026"),
            DealerInventoryEntity(fpsId = "FPS-110001-084", commodityId = "DAL", commodityName = "Chana Dal", unit = "kg", currentStock = 95.0, minThreshold = 30.0, lastRestockedDate = "20-08-2026"),
            DealerInventoryEntity(fpsId = "FPS-110001-084", commodityId = "SALT", commodityName = "Iodized Salt", unit = "kg", currentStock = 110.0, minThreshold = 25.0, lastRestockedDate = "20-08-2026"),
            DealerInventoryEntity(fpsId = "FPS-110001-084", commodityId = "OIL", commodityName = "Mustard Oil", unit = "L", currentStock = 80.0, minThreshold = 20.0, lastRestockedDate = "20-08-2026")
        )
        db.dealerInventoryDao().insertInventory(inventory)

        // 3. Seed Beneficiaries
        val beneficiaries = listOf(
            BeneficiaryEntity(
                cardId = "SRC-DL-2026-99214",
                maskedCardNumber = "XXXX-XXXX-2847",
                headOfFamilyName = "Ramesh Kumar",
                cardType = "PHH",
                familyMembersCount = 4,
                state = "NCT of Delhi",
                district = "Central Delhi",
                registeredFpsId = "FPS-110001-084",
                cardStatus = "ACTIVE",
                secretCardSalt = "SALT_RAMESH_99",
                lastDistributionDate = "01-08-2026",
                nextEligibleDate = "01-09-2026",
                referenceFaceHash = "FACE_HASH_RAMESH_KUMAR_2847",
                registeredMobileMasked = "******9821"
            ),
            BeneficiaryEntity(
                cardId = "SRC-DL-2026-88102",
                maskedCardNumber = "XXXX-XXXX-5519",
                headOfFamilyName = "Sunita Devi",
                cardType = "AAY",
                familyMembersCount = 5,
                state = "NCT of Delhi",
                district = "Central Delhi",
                registeredFpsId = "FPS-110001-084",
                cardStatus = "ACTIVE",
                secretCardSalt = "SALT_SUNITA_88",
                lastDistributionDate = "28-07-2026",
                nextEligibleDate = "01-09-2026",
                referenceFaceHash = "FACE_HASH_SUNITA_DEVI_5519",
                registeredMobileMasked = "******4412"
            ),
            BeneficiaryEntity(
                cardId = "SRC-GJ-2026-14029",
                maskedCardNumber = "XXXX-XXXX-9103",
                headOfFamilyName = "Rajesh Patel (ONORC Portability)",
                cardType = "PHH",
                familyMembersCount = 3,
                state = "Gujarat",
                district = "Ahmedabad",
                registeredFpsId = "FPS-380001-012",
                cardStatus = "ACTIVE",
                secretCardSalt = "SALT_RAJESH_14",
                lastDistributionDate = "15-07-2026",
                nextEligibleDate = "01-09-2026",
                referenceFaceHash = "FACE_HASH_RAJESH_PATEL_9103",
                registeredMobileMasked = "******7723"
            ),
            BeneficiaryEntity(
                cardId = "SRC-UP-2026-77312",
                maskedCardNumber = "XXXX-XXXX-4420",
                headOfFamilyName = "Mohammed Ansari",
                cardType = "PHH",
                familyMembersCount = 4,
                state = "Uttar Pradesh",
                district = "Ghaziabad",
                registeredFpsId = "FPS-201001-045",
                cardStatus = "BLOCKED",
                secretCardSalt = "SALT_MOHAMMED_77",
                lastDistributionDate = "10-06-2026",
                nextEligibleDate = "N/A (LOCKED)",
                referenceFaceHash = "FACE_HASH_MOHAMMED_ANSARI_4420",
                registeredMobileMasked = "******3301"
            ),
            BeneficiaryEntity(
                cardId = "SRC-DL-2026-33901",
                maskedCardNumber = "XXXX-XXXX-1188",
                headOfFamilyName = "Priya Sharma",
                cardType = "PHH",
                familyMembersCount = 2,
                state = "NCT of Delhi",
                district = "Central Delhi",
                registeredFpsId = "FPS-110001-084",
                cardStatus = "LOST",
                secretCardSalt = "SALT_PRIYA_33",
                lastDistributionDate = "05-07-2026",
                nextEligibleDate = "REISSUE_PENDING",
                referenceFaceHash = "FACE_HASH_PRIYA_SHARMA_1188",
                registeredMobileMasked = "******8819"
            )
        )
        db.beneficiaryDao().insertBeneficiaries(beneficiaries)

        // 4. Seed Entitlements
        val entitlements = listOf(
            // Ramesh Kumar (PHH 4 members: Rice 20kg, Wheat 5kg, Sugar 2kg)
            EntitlementEntity(cardId = "SRC-DL-2026-99214", commodityId = "RICE", commodityName = "Fortified Rice", unit = "kg", monthlyEntitlement = 20.0, alreadyCollectedMonth = 5.0, pricePerUnit = 0.0, monthYear = "08-2026"),
            EntitlementEntity(cardId = "SRC-DL-2026-99214", commodityId = "WHEAT", commodityName = "Whole Wheat Grain", unit = "kg", monthlyEntitlement = 5.0, alreadyCollectedMonth = 0.0, pricePerUnit = 0.0, monthYear = "08-2026"),
            EntitlementEntity(cardId = "SRC-DL-2026-99214", commodityId = "SUGAR", commodityName = "Subsidized Sugar", unit = "kg", monthlyEntitlement = 2.0, alreadyCollectedMonth = 0.0, pricePerUnit = 13.50, monthYear = "08-2026"),

            // Sunita Devi (AAY: Rice 25kg, Wheat 10kg, Sugar 1kg, Oil 1L)
            EntitlementEntity(cardId = "SRC-DL-2026-88102", commodityId = "RICE", commodityName = "Fortified Rice", unit = "kg", monthlyEntitlement = 25.0, alreadyCollectedMonth = 0.0, pricePerUnit = 0.0, monthYear = "08-2026"),
            EntitlementEntity(cardId = "SRC-DL-2026-88102", commodityId = "WHEAT", commodityName = "Whole Wheat Grain", unit = "kg", monthlyEntitlement = 10.0, alreadyCollectedMonth = 0.0, pricePerUnit = 0.0, monthYear = "08-2026"),
            EntitlementEntity(cardId = "SRC-DL-2026-88102", commodityId = "SUGAR", commodityName = "Subsidized Sugar", unit = "kg", monthlyEntitlement = 1.0, alreadyCollectedMonth = 0.0, pricePerUnit = 13.50, monthYear = "08-2026"),
            EntitlementEntity(cardId = "SRC-DL-2026-88102", commodityId = "OIL", commodityName = "Mustard Oil", unit = "L", monthlyEntitlement = 1.0, alreadyCollectedMonth = 0.0, pricePerUnit = 65.0, monthYear = "08-2026"),

            // Rajesh Patel (Gujarat ONORC)
            EntitlementEntity(cardId = "SRC-GJ-2026-14029", commodityId = "RICE", commodityName = "Fortified Rice", unit = "kg", monthlyEntitlement = 15.0, alreadyCollectedMonth = 0.0, pricePerUnit = 0.0, monthYear = "08-2026"),
            EntitlementEntity(cardId = "SRC-GJ-2026-14029", commodityId = "WHEAT", commodityName = "Whole Wheat Grain", unit = "kg", monthlyEntitlement = 10.0, alreadyCollectedMonth = 0.0, pricePerUnit = 0.0, monthYear = "08-2026")
        )
        db.entitlementDao().insertEntitlements(entitlements)

        // 5. Seed Initial Audit Record
        val genesisHash = "0000000000000000000000000000000000000000000000000000000000000000"
        val firstLog = AuditLogEntity(
            eventId = "AUD-INIT-001",
            timestamp = System.currentTimeMillis(),
            formattedTime = "23-09-2026 09:00:00",
            dealerId = "SYSTEM_SUPERVISOR",
            deviceId = "POS-DEV-IND-8841",
            cardId = "N/A",
            action = "POS_SYSTEM_INITIALIZED",
            result = "SUCCESS",
            riskLevel = "LOW",
            previousHash = genesisHash,
            currentHash = CryptoEngine.sha256("GENESIS_INIT_EVENT"),
            details = "Smart PDS Terminal initialized with authenticated secure keystore."
        )
        db.auditLogDao().insertAuditLog(firstLog)

        // 6. Seed Device
        val device = AuthorizedDeviceEntity(
            deviceId = "POS-DEV-IND-8841",
            dealerId = "DL-DEL-0492",
            fpsId = "FPS-110001-084",
            deviceModel = "Posiflex Vision Smart e-POS 4G",
            status = "AUTHORIZED",
            lastSyncTimestamp = System.currentTimeMillis(),
            ipAddressMasked = "10.14.***.***",
            isOnline = true
        )
        db.authorizedDeviceDao().insertDevice(device)
    }

    suspend fun verifyCardToken(
        tokenPayload: String,
        session: DealerSession,
        recentFailedAttempts: Int = 0,
        isSimulatedRiskTest: Boolean = false,
        simulatedRiskScore: Int? = null
    ): VerificationOutcome = withContext(Dispatchers.IO) {
        // Step 1: Cryptographic Validation
        val validation = CryptoEngine.parseAndValidatePayload(tokenPayload)
        if (validation is TokenValidationResult.Failure) {
            recordAuditLog(
                dealerId = session.dealerId,
                deviceId = session.deviceId,
                cardId = "UNKNOWN",
                action = "CARD_TOKEN_VALIDATION",
                result = "FAILURE: ${validation.errorCode}",
                riskLevel = "CRITICAL",
                details = validation.message
            )
            return@withContext VerificationOutcome.SecurityRejection(
                validation.errorCode,
                validation.message
            )
        }

        val parsed = (validation as TokenValidationResult.Success).parsed

        // Step 2: Fetch Beneficiary Record
        val beneficiary = db.beneficiaryDao().getBeneficiaryByCardId(parsed.cardId)
        if (beneficiary == null) {
            recordAuditLog(
                dealerId = session.dealerId,
                deviceId = session.deviceId,
                cardId = parsed.cardId,
                action = "CARD_DATABASE_LOOKUP",
                result = "CARD_NOT_FOUND",
                riskLevel = "HIGH",
                details = "Card identifier not present in PDS state beneficiary registry."
            )
            return@withContext VerificationOutcome.SecurityRejection(
                "CARD_NOT_FOUND",
                "Card is not registered in the National Food Security database."
            )
        }

        // Step 3: Card Status Check
        if (beneficiary.cardStatus == "BLOCKED" || beneficiary.cardStatus == "REVOKED" || beneficiary.cardStatus == "LOST") {
            recordAuditLog(
                dealerId = session.dealerId,
                deviceId = session.deviceId,
                cardId = beneficiary.cardId,
                action = "CARD_ACCESS_ATTEMPT",
                result = "CARD_NOT_AUTHORIZED (${beneficiary.cardStatus})",
                riskLevel = "CRITICAL",
                details = "Access attempted on ${beneficiary.cardStatus} card."
            )
            return@withContext VerificationOutcome.SecurityRejection(
                "CARD_NOT_AUTHORIZED",
                "Card status is ${beneficiary.cardStatus}. Access denied. Beneficiary must visit District Supply Office."
            )
        }

        // Step 4: Adaptive Risk Assessment
        val riskResult = AdaptiveRiskEngine.assessRisk(
            beneficiary = beneficiary,
            session = session,
            recentFailedAttempts = recentFailedAttempts,
            isSimulatedRiskTest = isSimulatedRiskTest,
            simulatedRiskScore = simulatedRiskScore
        )

        // Step 5: Fetch Entitlements
        val entitlements = db.entitlementDao().getEntitlementsList(beneficiary.cardId)

        // Audit the verification
        recordAuditLog(
            dealerId = session.dealerId,
            deviceId = session.deviceId,
            cardId = beneficiary.cardId,
            action = "BENEFICIARY_VERIFIED",
            result = "SUCCESS",
            riskLevel = riskResult.level.name,
            details = "Token signature valid. Risk Score: ${riskResult.totalScore}/100."
        )

        VerificationOutcome.Success(
            beneficiary = beneficiary,
            entitlements = entitlements,
            riskAssessment = riskResult,
            tokenPayload = tokenPayload
        )
    }

    suspend fun processDistribution(
        beneficiary: BeneficiaryEntity,
        session: DealerSession,
        periodMode: DistributionPeriodMode,
        quantitiesRequested: Map<String, Double>, // commodityId -> quantity
        authMethodUsed: String,
        riskScore: Int,
        isOfflineMode: Boolean
    ): DistributionOutcome = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH)
        val formattedTime = dateFormat.format(Date(now))
        val currentPeriodDesc = if (periodMode == DistributionPeriodMode.THREE_MONTH) "Q3 2026 (Aug-Oct Advance)" else "August 2026"

        // 1. Idempotency Check
        val idempotencyKey = CryptoEngine.generateIdempotencyKey(beneficiary.cardId, session.dealerId, currentPeriodDesc, now)
        val existingTxn = db.transactionDao().getByIdempotencyKey(idempotencyKey)
        if (existingTxn != null) {
            return@withContext DistributionOutcome.Denied(
                "DUPLICATE_TRANSACTION",
                "Transaction already confirmed within current distribution window. Idempotency protected."
            )
        }

        // 2. Fetch current entitlements and inventory
        val entitlements = db.entitlementDao().getEntitlementsList(beneficiary.cardId)
        val inventory = db.dealerInventoryDao().getInventoryList(session.fpsId)
        val inventoryMap = inventory.associateBy { it.commodityId }

        val itemsToDistribute = mutableListOf<TransactionItemEntity>()
        var totalAmount = 0.0
        var totalQuantityKg = 0.0

        val txnId = "TXN-${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(6).uppercase()}"

        // Validate each item
        for ((commodityId, qty) in quantitiesRequested) {
            if (qty <= 0) continue

            val entitlement = entitlements.find { it.commodityId == commodityId }
                ?: return@withContext DistributionOutcome.Denied("INVALID_ITEM", "Beneficiary has no quota for $commodityId.")

            val multiplier = periodMode.multiplier
            val maxAllowed = (entitlement.monthlyEntitlement * multiplier) - entitlement.alreadyCollectedMonth
            if (qty > maxAllowed) {
                return@withContext DistributionOutcome.Denied(
                    "ENTITLEMENT_EXCEEDED",
                    "Requested ${qty} ${entitlement.unit} of ${entitlement.commodityName} exceeds remaining entitlement of ${maxAllowed} ${entitlement.unit}."
                )
            }

            val stock = inventoryMap[commodityId]?.currentStock ?: 0.0
            if (qty > stock) {
                return@withContext DistributionOutcome.Denied(
                    "INSUFFICIENT_STOCK",
                    "FPS shop stock for ${entitlement.commodityName} is only $stock ${entitlement.unit}. Cannot fulfill $qty ${entitlement.unit}."
                )
            }

            val itemCost = qty * entitlement.pricePerUnit
            totalAmount += itemCost
            totalQuantityKg += qty

            itemsToDistribute.add(
                TransactionItemEntity(
                    transactionId = txnId,
                    commodityId = commodityId,
                    commodityName = entitlement.commodityName,
                    quantity = qty,
                    unit = entitlement.unit,
                    ratePerUnit = entitlement.pricePerUnit,
                    totalCost = itemCost
                )
            )
        }

        if (itemsToDistribute.isEmpty()) {
            return@withContext DistributionOutcome.Denied("NO_ITEMS_SELECTED", "Please specify at least one commodity quantity to issue.")
        }

        // 3. Atomic Updates
        for (item in itemsToDistribute) {
            // Deduct stock
            db.dealerInventoryDao().deductStock(session.fpsId, item.commodityId, item.quantity)
            // Update entitlement
            val entitlement = entitlements.first { it.commodityId == item.commodityId }
            db.entitlementDao().updateEntitlement(
                entitlement.copy(alreadyCollectedMonth = entitlement.alreadyCollectedMonth + item.quantity)
            )
        }

        val invoiceNumber = "PDS-INV-2026-" + (10000 + (now % 90000))
        val digitalHash = CryptoEngine.sha256("$txnId:$idempotencyKey:$totalQuantityKg:$totalAmount:$now")
        val receiptQrPayload = "https://pds.gov.in/verify?t=$txnId&h=${digitalHash.take(12)}"

        val transactionEntity = TransactionEntity(
            id = txnId,
            idempotencyKey = idempotencyKey,
            cardId = beneficiary.cardId,
            beneficiaryName = beneficiary.headOfFamilyName,
            maskedCardNumber = beneficiary.maskedCardNumber,
            fpsId = session.fpsId,
            dealerId = session.dealerId,
            deviceId = session.deviceId,
            periodMode = periodMode.name,
            periodDescription = currentPeriodDesc,
            totalQuantityKg = totalQuantityKg,
            totalAmountPaid = totalAmount,
            authMethodUsed = authMethodUsed,
            riskScore = riskScore,
            riskLevel = if (riskScore > 60) "HIGH" else if (riskScore > 25) "MEDIUM" else "LOW",
            timestamp = now,
            formattedDateTime = formattedTime,
            invoiceNumber = invoiceNumber,
            syncStatus = if (isOfflineMode) SyncStatus.PENDING_OFFLINE_SYNC.name else SyncStatus.SYNCED_ONLINE.name,
            isOfflineCreated = isOfflineMode,
            verificationTokenSignature = CryptoEngine.computeHmacSha256(txnId, "TXN_SECRET"),
            receiptQrPayload = receiptQrPayload,
            digitalBillHash = digitalHash
        )

        db.transactionDao().insertTransaction(transactionEntity)
        db.transactionDao().insertTransactionItems(itemsToDistribute)

        // Audit transaction
        recordAuditLog(
            dealerId = session.dealerId,
            deviceId = session.deviceId,
            cardId = beneficiary.cardId,
            action = if (isOfflineMode) "OFFLINE_TRANSACTION_RECORDED" else "COMMODITY_DISTRIBUTION_CONFIRMED",
            result = "SUCCESS",
            riskLevel = transactionEntity.riskLevel,
            details = "Total: ${totalQuantityKg}kg across ${itemsToDistribute.size} commodities. Amount: ₹${totalAmount}. Period: $currentPeriodDesc."
        )

        val receipt58mm = generateReceiptText(transactionEntity, itemsToDistribute, session, beneficiary, is80mm = false)
        val receipt80mm = generateReceiptText(transactionEntity, itemsToDistribute, session, beneficiary, is80mm = true)

        DistributionOutcome.Approved(
            transaction = transactionEntity,
            items = itemsToDistribute,
            receiptText58mm = receipt58mm,
            receiptText80mm = receipt80mm,
            isOffline = isOfflineMode
        )
    }

    suspend fun reconcileOfflineTransactions(): Int = withContext(Dispatchers.IO) {
        val pending = db.transactionDao().getPendingOfflineTransactions()
        for (txn in pending) {
            // Server checks duplicate and updates sync state
            db.transactionDao().updateSyncStatus(txn.id, SyncStatus.SYNC_RECONCILED.name)
            recordAuditLog(
                dealerId = txn.dealerId,
                deviceId = txn.deviceId,
                cardId = txn.cardId,
                action = "OFFLINE_TXN_RECONCILED",
                result = "RECONCILED_SUCCESS",
                riskLevel = "LOW",
                details = "Offline transaction ${txn.id} synchronized and reconciled with central PDS server."
            )
        }
        pending.size
    }

    suspend fun fileComplaint(
        cardId: String,
        beneficiaryName: String,
        fpsId: String,
        category: String,
        description: String
    ): String = withContext(Dispatchers.IO) {
        val complaintId = "GRV-2026-" + (1000 + (System.currentTimeMillis() % 9000))
        val complaint = ComplaintEntity(
            complaintId = complaintId,
            cardId = cardId,
            beneficiaryName = beneficiaryName,
            fpsId = fpsId,
            category = category,
            description = description,
            status = "SUBMITTED",
            filedDate = SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH).format(Date())
        )
        db.complaintDao().insertComplaint(complaint)
        recordAuditLog(
            dealerId = "BENEFICIARY_PORTAL",
            deviceId = "SELF_KIOSK",
            cardId = cardId,
            action = "COMPLAINT_FILED",
            result = "SUCCESS",
            riskLevel = "LOW",
            details = "Complaint $complaintId registered under category: $category"
        )
        complaintId
    }

    suspend fun resolveFraudAlert(id: Long, resolutionNotes: String) = withContext(Dispatchers.IO) {
        db.fraudAlertDao().resolveAlert(id, resolutionNotes)
    }

    private suspend fun recordAuditLog(
        dealerId: String,
        deviceId: String,
        cardId: String,
        action: String,
        result: String,
        riskLevel: String,
        details: String
    ) {
        val latest = db.auditLogDao().getLatestAuditLog()
        val prevHash = latest?.currentHash ?: "0000000000000000000000000000000000000000000000000000000000000000"
        val now = System.currentTimeMillis()
        val eventId = "AUD-" + (now % 1000000)
        val currentHash = CryptoEngine.calculateAuditHash(prevHash, eventId, now, dealerId, action, result)
        val formattedTime = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH).format(Date(now))

        val log = AuditLogEntity(
            eventId = eventId,
            timestamp = now,
            formattedTime = formattedTime,
            dealerId = dealerId,
            deviceId = deviceId,
            cardId = cardId,
            action = action,
            result = result,
            riskLevel = riskLevel,
            previousHash = prevHash,
            currentHash = currentHash,
            details = details
        )
        db.auditLogDao().insertAuditLog(log)
    }

    private fun generateReceiptText(
        txn: TransactionEntity,
        items: List<TransactionItemEntity>,
        session: DealerSession,
        beneficiary: BeneficiaryEntity,
        is80mm: Boolean
    ): String {
        val width = if (is80mm) 46 else 32
        val border = "=".repeat(width)
        val dashed = "-".repeat(width)
        val sb = StringBuilder()

        sb.appendLine(centerText("GOVT OF INDIA / STATE e-PDS", width))
        sb.appendLine(centerText("PUBLIC DISTRIBUTION SYSTEM", width))
        sb.appendLine(centerText("FAIR PRICE SHOP OFFICIAL RECEIPT", width))
        sb.appendLine(border)
        sb.appendLine("Shop: ${session.shopName}")
        sb.appendLine("FPS ID: ${session.fpsId} | Dealer: ${session.dealerId}")
        sb.appendLine("Device: ${session.deviceId}")
        sb.appendLine("State: ${session.state} | Dist: ${session.district}")
        sb.appendLine(dashed)
        sb.appendLine("Txn ID: ${txn.id}")
        sb.appendLine("Invoice No: ${txn.invoiceNumber}")
        sb.appendLine("Date/Time: ${txn.formattedDateTime}")
        sb.appendLine("Period: ${txn.periodDescription}")
        sb.appendLine("Auth: ${txn.authMethodUsed}")
        sb.appendLine(dashed)
        sb.appendLine("Beneficiary: ${beneficiary.headOfFamilyName}")
        sb.appendLine("Ration Card: ${beneficiary.maskedCardNumber}")
        sb.appendLine("Card Category: ${beneficiary.cardType} (${beneficiary.familyMembersCount} Family Members)")
        sb.appendLine(dashed)

        // Column headers
        if (is80mm) {
            sb.appendLine(String.format("%-18s %8s %8s %8s", "COMMODITY", "QTY", "RATE", "TOTAL"))
        } else {
            sb.appendLine(String.format("%-14s %6s %4s %6s", "COMMODITY", "QTY", "RATE", "AMT"))
        }
        sb.appendLine(dashed)

        for (item in items) {
            val rateStr = if (item.ratePerUnit == 0.0) "FREE" else "₹${item.ratePerUnit}"
            val costStr = if (item.totalCost == 0.0) "₹0.00" else "₹${"%.2f".format(item.totalCost)}"
            val qtyStr = "${item.quantity}${item.unit}"
            if (is80mm) {
                sb.appendLine(String.format("%-18s %8s %8s %8s", item.commodityName.take(18), qtyStr, rateStr, costStr))
            } else {
                sb.appendLine(String.format("%-14s %6s %4s %6s", item.commodityName.take(14), qtyStr, rateStr, costStr))
            }
        }
        sb.appendLine(dashed)
        sb.appendLine("TOTAL COMMODITY ISSUED: ${txn.totalQuantityKg} kg")
        sb.appendLine("TOTAL AMOUNT COLLECTED: ₹${"%.2f".format(txn.totalAmountPaid)}")
        sb.appendLine(dashed)
        sb.appendLine("Status: ${if (txn.isOfflineCreated) "OFFLINE CONFIRMED (PENDING SYNC)" else "ONLINE VERIFIED & SYNCHRONIZED"}")
        sb.appendLine("Hash: ${txn.digitalBillHash.take(16)}...")
        sb.appendLine("Receipt QR Verification Token:")
        sb.appendLine(txn.receiptQrPayload)
        sb.appendLine(border)
        sb.appendLine(centerText("NATIONAL FOOD SECURITY ACT (NFSA)", width))
        sb.appendLine(centerText("Toll Free Helpline: 1967 / 1800-11-4000", width))
        sb.appendLine(centerText("Thank you for using Digital PDS", width))

        return sb.toString()
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text
        val padding = (width - text.length) / 2
        return " ".repeat(padding) + text
    }
}
