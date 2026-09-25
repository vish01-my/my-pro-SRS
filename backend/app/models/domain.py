from enum import Enum
from typing import Dict, Optional, List
from pydantic import BaseModel

class UserRole(str, Enum):
    DEALER = "DEALER"
    SUPERVISOR = "SUPERVISOR"
    DISTRICT_OFFICER = "DISTRICT_OFFICER"
    STATE_ADMIN = "STATE_ADMIN"
    AUDITOR = "AUDITOR"

class CardStatus(str, Enum):
    ACTIVE = "ACTIVE"
    BLOCKED = "BLOCKED"
    SUSPENDED = "SUSPENDED"
    LOST = "LOST"
    REISSUE_PENDING = "REISSUE_PENDING"

class CardType(str, Enum):
    PHH = "PHH"  # Priority Household (NFSA)
    AAY = "AAY"  # Antyodaya Anna Yojana (Poorest of poor)
    APL = "APL"  # Above Poverty Line (State Scheme)

CardCategory = CardType

class SyncStatus(str, Enum):
    SYNCED_ONLINE = "SYNCED_ONLINE"
    PENDING_OFFLINE_SYNC = "PENDING_OFFLINE_SYNC"
    SYNC_RECONCILED = "SYNC_RECONCILED"
    CONFLICT_FLAGGED = "CONFLICT_FLAGGED"

class PeriodMode(str, Enum):
    MONTHLY = "MONTHLY"
    THREE_MONTH = "THREE_MONTH"

class RiskLevel(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"

class AuthMethodType(str, Enum):
    QR_CRYPTOGRAPHIC = "QR_CRYPTOGRAPHIC"
    NFC_CRYPTOGRAPHIC = "NFC_CRYPTOGRAPHIC"
    BARCODE_BACKUP = "BARCODE_BACKUP"
    FACE_BIOMETRIC = "FACE_BIOMETRIC"
    FINGERPRINT = "FINGERPRINT"
    AADHAAR_OTP = "AADHAAR_OTP"
    OFFICER_OVERRIDE = "OFFICER_OVERRIDE"

class CommodityQuota(BaseModel):
    commodity_id: str
    commodity_name: str
    unit: str
    monthly_entitlement: float
    already_collected_month: float
    price_per_unit: float

class Beneficiary(BaseModel):
    card_id: str
    masked_card_number: str
    head_of_family_name: str
    card_type: CardType
    family_members_count: int
    state: str
    district: str
    registered_fps_id: str
    card_status: CardStatus
    reference_face_hash: str
    registered_mobile_masked: str
    last_distribution_date: str
    next_eligible_date: str
    entitlements: List[CommodityQuota]

class PosDevice(BaseModel):
    device_id: str
    dealer_id: str
    fps_id: str
    device_model: str
    status: str  # "AUTHORIZED", "SUSPENDED", "DECOMMISSIONED"
    is_active: bool
