from typing import Optional, List, Dict
from pydantic import BaseModel, Field
from app.models.domain import UserRole, CardStatus, CardType, RiskLevel, AuthMethodType, CommodityQuota

# -------------------------------------------------------------
# Auth Schemas
# -------------------------------------------------------------

class DealerLoginRequest(BaseModel):
    dealer_id: str = Field(..., example="DL-DEL-0492")
    password: str = Field(..., example="PdsDealer@2026")
    pos_device_id: str = Field(..., example="POS-DEV-IND-8841")

class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "Bearer"
    expires_in_minutes: int
    role: UserRole
    dealer_id: str
    fps_id: str
    shop_name: str
    pos_device_id: str

class TokenData(BaseModel):
    dealer_id: str
    role: UserRole
    fps_id: str
    pos_device_id: str

# -------------------------------------------------------------
# Card Verification Schemas (Privacy-by-Design)
# -------------------------------------------------------------

class VerifyCardTokenRequest(BaseModel):
    token_payload: str = Field(
        ...,
        description="Encrypted token: 'SRC:v1:CARD-ID:NONCE:EXPIRY:SIGNATURE'",
        example="SRC:v1:SRC-DL-2026-99214:NONCE_9941:1790175000000:a1b2c3..."
    )
    pos_device_id: Optional[str] = Field(None, example="POS-DEV-IND-8841")
    fps_id: Optional[str] = Field(None, example="FPS-110001-084")
    simulate_risk_score: Optional[int] = Field(None, description="Optional override for security testing")

class RiskFactorResponse(BaseModel):
    factor_name: str
    score_contribution: int
    description: str
    is_anomaly: bool

class RiskAssessmentResponse(BaseModel):
    total_score: int
    level: RiskLevel
    factors: List[RiskFactorResponse]
    required_next_auth: List[AuthMethodType]
    is_transaction_permitted: bool

class BeneficiaryMinResponse(BaseModel):
    card_id: str
    masked_card_number: str
    head_of_family_name: str
    card_type: CardType
    family_members_count: int
    state: str
    district: str
    card_status: CardStatus
    last_distribution_date: str
    next_eligible_date: str
    entitlements: List[CommodityQuota]
    registered_mobile_masked: str
    reference_face_hash: str

class VerifyCardTokenResponse(BaseModel):
    is_valid: bool
    status: str
    error_code: Optional[str] = None
    error_message: Optional[str] = None
    beneficiary: Optional[BeneficiaryMinResponse] = None
    risk_assessment: Optional[RiskAssessmentResponse] = None
    latency_ms: float

# -------------------------------------------------------------
# Test Helper Schema
# -------------------------------------------------------------

class GenerateTestTokenRequest(BaseModel):
    card_id: str = Field(..., example="SRC-DL-2026-99214")
    ttl_hours: int = Field(24, example=24)

class GenerateTestTokenResponse(BaseModel):
    token_payload: str
    card_id: str
    expiry_timestamp: int

# -------------------------------------------------------------
# Biometric Face Verification Schemas (Privacy-by-Design)
# -------------------------------------------------------------

class FaceVerificationRequest(BaseModel):
    card_id: str = Field(..., example="SRC-DL-2026-99214")
    captured_face_hash: Optional[str] = Field(
        None,
        description="Client-side computed biometric feature hash (e.g. ML Kit landmarks / FaceNet hash)",
        example="FACE_HASH_RAMESH_KUMAR_2847"
    )
    face_embedding: Optional[List[float]] = Field(
        None,
        description="Optional normalized 128-d or 512-d biometric embedding vector"
    )
    live_frame_base64: Optional[str] = Field(
        None,
        description="Optional live JPEG/PNG frame payload (processed ephemerally in-memory, never stored)"
    )
    liveness_blink_verified: bool = Field(
        True,
        description="Whether on-device ML Kit blink challenge passed"
    )
    head_euler_yaw: Optional[float] = Field(
        0.0,
        description="Horizontal head rotation angle in degrees from ML Kit"
    )
    head_euler_pitch: Optional[float] = Field(
        0.0,
        description="Vertical head tilt angle in degrees from ML Kit"
    )
    pos_device_id: Optional[str] = Field(
        None,
        example="POS-DEV-IND-8841"
    )
    simulate_mismatch: bool = Field(
        False,
        description="Testing flag to simulate biometric mismatch"
    )

class FaceVerificationResponse(BaseModel):
    is_matched: bool
    confidence_percentage: float
    threshold_percentage: float = 75.0
    liveness_verified: bool
    anti_spoofing_score: float
    card_id: str
    beneficiary_name: Optional[str] = None
    biometric_token: Optional[str] = None
    error_message: Optional[str] = None
    latency_ms: float
    privacy_guarantee: str = "Raw biometric frames purged. Zero persistent biometric storage."

# -------------------------------------------------------------
# Transaction Reconciliation Schemas (Atomicity & Idempotency)
# -------------------------------------------------------------

class ReconcileTransactionItem(BaseModel):
    transaction_id: str = Field(..., example="TXN-2026-99214-8841")
    idempotency_key: str = Field(..., example="IDEM-99214-MONTHLY-202609-A49F")
    invoice_number: str = Field(..., example="INV-2026-99214")
    card_id: str = Field(..., example="SRC-DL-2026-99214")
    beneficiary_name: str = Field(..., example="Ramesh Kumar")
    fps_id: str = Field(..., example="FPS-110001-084")
    period_mode: str = Field(..., example="MONTHLY")
    period_description: str = Field(..., example="September 2026")
    total_quantity_kg: float = Field(..., example=35.0)
    total_amount_paid: float = Field(..., example=13.50)
    auth_method_used: str = Field(..., example="QR + FACE_BIOMETRIC")
    digital_bill_hash: str = Field(..., example="e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    verification_token_signature: str = Field(..., example="SIG_VALID_CARD_99214")
    created_at_timestamp: int
    items_json: Optional[str] = None

class ReconcileBatchRequest(BaseModel):
    dealer_id: str = Field(..., example="DL-DEL-0492")
    pos_device_id: str = Field(..., example="POS-DEV-IND-8841")
    batch_id: str = Field(..., example="BATCH-SYNC-2026-001")
    transactions: List[ReconcileTransactionItem]

class TransactionReconcileResult(BaseModel):
    transaction_id: str
    idempotency_key: str
    status: str  # "RECONCILED", "DUPLICATE_IDEMPOTENT_IGNORED", "CONFLICT_FLAGGED"
    message: str
    synced_timestamp: int

class ReconcileBatchResponse(BaseModel):
    batch_id: str
    total_received: int
    reconciled_count: int
    duplicate_count: int
    conflict_count: int
    results: List[TransactionReconcileResult]
    audit_event_id: str
    atomicity_guarantee: str = "Batch processed in single transaction isolation"

