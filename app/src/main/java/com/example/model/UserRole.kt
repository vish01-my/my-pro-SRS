package com.example.model

enum class UserRole(val displayName: String, val level: String) {
    DEALER("Fair Price Shop Dealer", "FPS Operator"),
    SUPERVISOR("Field Food Inspector", "PDS Supervisor"),
    DISTRICT_OFFICER("District Supply Officer (DSO)", "District Admin"),
    STATE_ADMIN("Director of Civil Supplies", "State Head"),
    CENTRAL_ADMIN("Dept of Food & Public Distribution", "National Admin"),
    AUDITOR("Social Audit & Vigilance Officer", "Independent Auditor")
}

data class DealerSession(
    val dealerId: String = "DL-DEL-0492",
    val dealerName: String = "Rajinder Prasad Sharma",
    val fpsId: String = "FPS-110001-084",
    val shopName: String = "Jan Seva Fair Price Shop #84",
    val district: String = "Central Delhi",
    val state: String = "NCT of Delhi",
    val deviceId: String = "POS-DEV-IND-8841",
    val deviceStatus: String = "AUTHORIZED",
    val role: UserRole = UserRole.DEALER
)
