from datetime import datetime
from typing import Optional, List
from sqlalchemy import (
    Column, String, Boolean, Integer, Numeric, Text, DateTime, ForeignKey, 
    BigInteger, UniqueConstraint, CheckConstraint, Enum as SQLEnum
)
from sqlalchemy.orm import declarative_base, relationship
from sqlalchemy.dialects.postgresql import UUID
import uuid

from app.models.domain import (
    UserRole, CardCategory, CardStatus, RiskLevel, 
    AuthMethodType, SyncStatus, PeriodMode
)

# For backward compatibility if Enum name varies
CardCategoryEnum = SQLEnum('PHH', 'AAY', 'APL', name='card_category_enum')
CardStatusEnum = SQLEnum('ACTIVE', 'BLOCKED', 'SUSPENDED', 'LOST', 'REISSUE_PENDING', 'SURRENDERED', name='card_status_enum')
UserRoleEnum = SQLEnum('DEALER', 'SUPERVISOR', 'DISTRICT_OFFICER', 'STATE_ADMIN', 'AUDITOR', name='user_role_enum')
RiskLevelEnum = SQLEnum('LOW', 'MEDIUM', 'HIGH', 'CRITICAL', name='risk_level_enum')
AuthMethodEnum = SQLEnum('QR_CRYPTOGRAPHIC', 'NFC_CRYPTOGRAPHIC', 'BARCODE_BACKUP', 'FACE_BIOMETRIC', 'FINGERPRINT', 'AADHAAR_OTP', 'OFFICER_OVERRIDE', name='auth_method_enum')
SyncStatusEnum = SQLEnum('SYNCED_ONLINE', 'PENDING_OFFLINE_SYNC', 'SYNC_RECONCILED', 'CONFLICT_FLAGGED', name='sync_status_enum')

Base = declarative_base()

class FairPriceShopDB(Base):
    __tablename__ = "fair_price_shops"

    fps_id = Column(String(32), primary_key=True)
    shop_name = Column(String(128), nullable=False)
    state = Column(String(64), nullable=False)
    district = Column(String(64), nullable=False)
    sub_district = Column(String(64), nullable=False)
    pincode = Column(String(6), nullable=False)
    latitude = Column(Numeric(9, 6), nullable=True)
    longitude = Column(Numeric(9, 6), nullable=True)
    status = Column(String(20), default="ACTIVE")
    contact_phone = Column(String(15), nullable=True)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    updated_at = Column(DateTime(timezone=True), default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    users = relationship("UserDB", back_populates="fps")
    pos_devices = relationship("PosDeviceDB", back_populates="fps")
    ration_cards = relationship("RationCardDB", back_populates="fps")
    inventory = relationship("DealerInventoryDB", back_populates="fps")


class UserDB(Base):
    __tablename__ = "users"

    user_id = Column(String(32), primary_key=True)
    username = Column(String(64), unique=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    full_name = Column(String(128), nullable=False)
    role = Column(UserRoleEnum, nullable=False)
    fps_id = Column(String(32), ForeignKey("fair_price_shops.fps_id", ondelete="SET NULL"), nullable=True)
    state = Column(String(64), nullable=False)
    district = Column(String(64), nullable=False)
    mobile_masked = Column(String(16), nullable=False)
    is_active = Column(Boolean, default=True)
    failed_login_attempts = Column(Integer, default=0)
    locked_until = Column(DateTime(timezone=True), nullable=True)
    last_login_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    updated_at = Column(DateTime(timezone=True), default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    fps = relationship("FairPriceShopDB", back_populates="users")


class PosDeviceDB(Base):
    __tablename__ = "pos_devices"

    device_id = Column(String(64), primary_key=True)
    fps_id = Column(String(32), ForeignKey("fair_price_shops.fps_id", ondelete="RESTRICT"), nullable=False)
    dealer_id = Column(String(32), ForeignKey("users.user_id", ondelete="SET NULL"), nullable=True)
    device_model = Column(String(128), nullable=False)
    hardware_serial = Column(String(128), unique=True, nullable=False)
    device_public_key = Column(Text, nullable=True)
    status = Column(String(32), default="AUTHORIZED")
    firmware_version = Column(String(32), nullable=True)
    ip_address_masked = Column(String(45), nullable=True)
    last_sync_at = Column(DateTime(timezone=True), nullable=True)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    updated_at = Column(DateTime(timezone=True), default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    fps = relationship("FairPriceShopDB", back_populates="pos_devices")


class BeneficiaryDB(Base):
    __tablename__ = "beneficiaries"

    beneficiary_id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    head_of_family_name = Column(String(128), nullable=False)
    masked_aadhaar = Column(String(16), nullable=False)
    aadhaar_vault_reference = Column(String(64), unique=True, nullable=True)
    masked_mobile = Column(String(16), nullable=False)
    gender = Column(String(10), nullable=True)
    family_members_count = Column(Integer, nullable=False)
    residential_state = Column(String(64), nullable=False)
    residential_district = Column(String(64), nullable=False)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    updated_at = Column(DateTime(timezone=True), default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    ration_cards = relationship("RationCardDB", back_populates="beneficiary")


class RationCardDB(Base):
    __tablename__ = "ration_cards"

    card_id = Column(String(32), primary_key=True)
    beneficiary_id = Column(UUID(as_uuid=True), ForeignKey("beneficiaries.beneficiary_id", ondelete="RESTRICT"), nullable=False)
    masked_card_number = Column(String(24), nullable=False)
    category = Column(CardCategoryEnum, nullable=False)
    card_status = Column(CardStatusEnum, default="ACTIVE")
    registered_fps_id = Column(String(32), ForeignKey("fair_price_shops.fps_id", ondelete="RESTRICT"), nullable=False)
    secret_card_salt = Column(String(64), nullable=False)
    reference_face_hash = Column(String(128), nullable=True)
    issue_date = Column(DateTime, nullable=False)
    last_distribution_date = Column(DateTime, nullable=True)
    next_eligible_date = Column(DateTime, nullable=True)
    qr_version = Column(String(8), default="v1")
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)
    updated_at = Column(DateTime(timezone=True), default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    beneficiary = relationship("BeneficiaryDB", back_populates="ration_cards")
    fps = relationship("FairPriceShopDB", back_populates="ration_cards")
    entitlements = relationship("CardEntitlementDB", back_populates="card")
    family_members = relationship("CardFamilyMemberDB", back_populates="card")


class CardFamilyMemberDB(Base):
    __tablename__ = "card_family_members"

    member_id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    card_id = Column(String(32), ForeignKey("ration_cards.card_id", ondelete="CASCADE"), nullable=False)
    member_name = Column(String(128), nullable=False)
    relationship_with_head = Column(String(32), nullable=False)
    masked_aadhaar = Column(String(16), nullable=False)
    aadhaar_vault_ref = Column(String(64), nullable=True)
    age = Column(Integer, nullable=True)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)

    # Relationships
    card = relationship("RationCardDB", back_populates="family_members")


class CommodityDB(Base):
    __tablename__ = "commodities"

    commodity_id = Column(String(16), primary_key=True)
    name = Column(String(64), nullable=False)
    vernacular_name = Column(String(64), nullable=True)
    unit = Column(String(16), nullable=False)
    market_mrp = Column(Numeric(10, 2), nullable=False)
    subsidized_price = Column(Numeric(10, 2), nullable=False)
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)


class DealerInventoryDB(Base):
    __tablename__ = "dealer_inventory"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    fps_id = Column(String(32), ForeignKey("fair_price_shops.fps_id", ondelete="CASCADE"), nullable=False)
    commodity_id = Column(String(16), ForeignKey("commodities.commodity_id", ondelete="RESTRICT"), nullable=False)
    current_stock = Column(Numeric(12, 3), nullable=False)
    min_threshold = Column(Numeric(12, 3), nullable=False)
    last_restocked_at = Column(DateTime(timezone=True), nullable=True)
    updated_at = Column(DateTime(timezone=True), default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    fps = relationship("FairPriceShopDB", back_populates="inventory")


class CardEntitlementDB(Base):
    __tablename__ = "card_entitlements"

    id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    card_id = Column(String(32), ForeignKey("ration_cards.card_id", ondelete="CASCADE"), nullable=False)
    commodity_id = Column(String(16), ForeignKey("commodities.commodity_id", ondelete="RESTRICT"), nullable=False)
    cycle_month_year = Column(String(8), nullable=False)
    monthly_entitlement = Column(Numeric(10, 3), nullable=False)
    already_collected = Column(Numeric(10, 3), default=0.0)
    price_per_unit = Column(Numeric(10, 2), nullable=False)
    updated_at = Column(DateTime(timezone=True), default=datetime.utcnow, onupdate=datetime.utcnow)

    # Relationships
    card = relationship("RationCardDB", back_populates="entitlements")


class TransactionDB(Base):
    __tablename__ = "transactions"

    transaction_id = Column(String(64), primary_key=True)
    idempotency_key = Column(String(64), unique=True, nullable=False)
    invoice_number = Column(String(32), unique=True, nullable=False)
    card_id = Column(String(32), ForeignKey("ration_cards.card_id", ondelete="RESTRICT"), nullable=False)
    beneficiary_name = Column(String(128), nullable=False)
    masked_card_number = Column(String(24), nullable=False)
    fps_id = Column(String(32), ForeignKey("fair_price_shops.fps_id", ondelete="RESTRICT"), nullable=False)
    dealer_id = Column(String(32), ForeignKey("users.user_id", ondelete="RESTRICT"), nullable=False)
    pos_device_id = Column(String(64), ForeignKey("pos_devices.device_id", ondelete="RESTRICT"), nullable=False)
    period_mode = Column(String(16), default="MONTHLY")
    period_description = Column(String(64), nullable=False)
    total_quantity_kg = Column(Numeric(10, 3), nullable=False)
    total_amount_paid = Column(Numeric(10, 2), nullable=False)
    auth_method_used = Column(AuthMethodEnum, nullable=False)
    risk_score = Column(Integer, nullable=False)
    risk_level = Column(RiskLevelEnum, nullable=False)
    digital_bill_hash = Column(String(64), nullable=False)
    verification_token_signature = Column(String(128), nullable=False)
    receipt_qr_payload = Column(Text, nullable=False)
    sync_status = Column(SyncStatusEnum, default="SYNCED_ONLINE")
    is_offline_created = Column(Boolean, default=False)
    client_timestamp = Column(BigInteger, nullable=False)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)

    items = relationship("TransactionItemDB", back_populates="transaction", cascade="all, delete-orphan")


class TransactionItemDB(Base):
    __tablename__ = "transaction_items"

    item_id = Column(UUID(as_uuid=True), primary_key=True, default=uuid.uuid4)
    transaction_id = Column(String(64), ForeignKey("transactions.transaction_id", ondelete="CASCADE"), nullable=False)
    commodity_id = Column(String(16), ForeignKey("commodities.commodity_id", ondelete="RESTRICT"), nullable=False)
    commodity_name = Column(String(64), nullable=False)
    quantity = Column(Numeric(10, 3), nullable=False)
    unit = Column(String(16), nullable=False)
    rate_per_unit = Column(Numeric(10, 2), nullable=False)
    total_cost = Column(Numeric(10, 2), nullable=False)

    transaction = relationship("TransactionDB", back_populates="items")


class AuditLogDB(Base):
    """
    Cryptographically immutable hash-chained audit log.
    Database triggers strictly forbid UPDATE and DELETE operations on this table.
    """
    __tablename__ = "audit_logs"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    event_id = Column(String(64), unique=True, nullable=False)
    timestamp_epoch_ms = Column(BigInteger, nullable=False)
    formatted_time = Column(DateTime(timezone=True), default=datetime.utcnow)
    dealer_id = Column(String(32), nullable=False)
    device_id = Column(String(64), nullable=False)
    card_id = Column(String(32), nullable=False)
    action = Column(String(64), nullable=False)
    result = Column(String(64), nullable=False)
    risk_level = Column(String(16), nullable=False)
    previous_hash = Column(String(64), nullable=False)
    current_hash = Column(String(64), nullable=False)
    details = Column(Text, nullable=False)
    ip_address = Column(String(45), nullable=True)
    created_at = Column(DateTime(timezone=True), default=datetime.utcnow)
