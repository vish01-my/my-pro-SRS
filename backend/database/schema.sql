-- =============================================================================
-- SECURE SMART RATION DISTRIBUTION SYSTEM (e-PDS) — POSTGRESQL DDL SCHEMA
-- Target Database: PostgreSQL 14+
-- Security Features:
--   1. Role-Based Access Control (RBAC) & Hardware Device Binding
--   2. Privacy-by-Design: Aadhaar data vault tokens, masked credentials
--   3. Idempotent Transaction Ledger (Prevents double debits across retries)
--   4. Cryptographically Linked Tamper-Evident Audit Ledger (SHA-256 Hash Chain)
--   5. Strict Append-Only Immutability Trigger on Audit Logs
-- =============================================================================

-- Enable required cryptographic & UUID extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -----------------------------------------------------------------------------
-- 1. CUSTOM ENUMS & TYPES
-- -----------------------------------------------------------------------------

CREATE TYPE user_role_enum AS ENUM (
    'DEALER',
    'SUPERVISOR',
    'DISTRICT_OFFICER',
    'STATE_ADMIN',
    'AUDITOR'
);

CREATE TYPE card_category_enum AS ENUM (
    'PHH',      -- Priority Household (NFSA)
    'AAY',      -- Antyodaya Anna Yojana (Poorest of poor)
    'APL'       -- Above Poverty Line (State Scheme)
);

CREATE TYPE card_status_enum AS ENUM (
    'ACTIVE',
    'BLOCKED',
    'SUSPENDED',
    'LOST',
    'REISSUE_PENDING',
    'SURRENDERED'
);

CREATE TYPE device_status_enum AS ENUM (
    'AUTHORIZED',
    'SUSPENDED',
    'REVOKED',
    'MAINTENANCE'
);

CREATE TYPE risk_level_enum AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH',
    'CRITICAL'
);

CREATE TYPE auth_method_enum AS ENUM (
    'QR_CRYPTOGRAPHIC',
    'NFC_CRYPTOGRAPHIC',
    'BARCODE_BACKUP',
    'FACE_BIOMETRIC',
    'FINGERPRINT',
    'AADHAAR_OTP',
    'OFFICER_OVERRIDE'
);

CREATE TYPE sync_status_enum AS ENUM (
    'SYNCED_ONLINE',
    'PENDING_OFFLINE_SYNC',
    'SYNC_RECONCILED',
    'CONFLICT_FLAGGED'
);

CREATE TYPE period_mode_enum AS ENUM (
    'MONTHLY',
    'THREE_MONTH'
);

CREATE TYPE grievance_status_enum AS ENUM (
    'SUBMITTED',
    'UNDER_INSPECTION',
    'ESCALATED_TO_DSO',
    'RESOLVED',
    'REJECTED'
);

-- -----------------------------------------------------------------------------
-- 2. CORE REGISTRY TABLES
-- -----------------------------------------------------------------------------

-- Fair Price Shops (FPS) / Ration Outlets
CREATE TABLE IF NOT EXISTS fair_price_shops (
    fps_id VARCHAR(32) PRIMARY KEY,
    shop_name VARCHAR(128) NOT NULL,
    state VARCHAR(64) NOT NULL,
    district VARCHAR(64) NOT NULL,
    sub_district VARCHAR(64) NOT NULL,
    pincode VARCHAR(6) NOT NULL,
    latitude NUMERIC(9, 6),
    longitude NUMERIC(9, 6),
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'SUSPENDED')),
    contact_phone VARCHAR(15),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fps_location ON fair_price_shops(state, district);

-- Users & RBAC (Dealers, Supply Inspectors, District Officers, State Admins, Auditors)
CREATE TABLE IF NOT EXISTS users (
    user_id VARCHAR(32) PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(128) NOT NULL,
    role user_role_enum NOT NULL,
    fps_id VARCHAR(32) REFERENCES fair_price_shops(fps_id) ON DELETE SET NULL,
    state VARCHAR(64) NOT NULL,
    district VARCHAR(64) NOT NULL,
    mobile_masked VARCHAR(16) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_fps ON users(fps_id);

-- Authorized POS Terminals (Hardware binding & device-level trust)
CREATE TABLE IF NOT EXISTS pos_devices (
    device_id VARCHAR(64) PRIMARY KEY,
    fps_id VARCHAR(32) NOT NULL REFERENCES fair_price_shops(fps_id) ON DELETE RESTRICT,
    dealer_id VARCHAR(32) REFERENCES users(user_id) ON DELETE SET NULL,
    device_model VARCHAR(128) NOT NULL,
    hardware_serial VARCHAR(128) UNIQUE NOT NULL,
    device_public_key TEXT,
    status device_status_enum DEFAULT 'AUTHORIZED',
    firmware_version VARCHAR(32),
    ip_address_masked VARCHAR(45),
    last_sync_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pos_devices_fps ON pos_devices(fps_id);

-- Beneficiaries (Privacy-First Data Architecture)
-- Note: Raw 12-digit Aadhaar is NEVER stored. Aadhaar vault reference tokens & masked display only.
CREATE TABLE IF NOT EXISTS beneficiaries (
    beneficiary_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    head_of_family_name VARCHAR(128) NOT NULL,
    masked_aadhaar VARCHAR(16) NOT NULL,              -- Format: XXXX-XXXX-1234
    aadhaar_vault_reference VARCHAR(64) UNIQUE,        -- Encrypted surrogate identifier in Govt Vault
    masked_mobile VARCHAR(16) NOT NULL,               -- Format: ******9821
    gender VARCHAR(10) CHECK (gender IN ('FEMALE', 'MALE', 'OTHER')),
    family_members_count INT NOT NULL CHECK (family_members_count > 0),
    residential_state VARCHAR(64) NOT NULL,
    residential_district VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Smart Ration Cards (Cryptographic Card Registry)
CREATE TABLE IF NOT EXISTS ration_cards (
    card_id VARCHAR(32) PRIMARY KEY,                   -- Conceptual format: SRC-DL-2026-99214
    beneficiary_id UUID NOT NULL REFERENCES beneficiaries(beneficiary_id) ON DELETE RESTRICT,
    masked_card_number VARCHAR(24) NOT NULL,           -- Format: XXXX-XXXX-2847
    category card_category_enum NOT NULL,              -- PHH, AAY, APL
    card_status card_status_enum DEFAULT 'ACTIVE',
    registered_fps_id VARCHAR(32) NOT NULL REFERENCES fair_price_shops(fps_id) ON DELETE RESTRICT,
    secret_card_salt VARCHAR(64) NOT NULL,             -- Unique card salt used for token signing
    reference_face_hash VARCHAR(128),                 -- Cryptographic hash of reference biometric template
    issue_date DATE NOT NULL,
    last_distribution_date DATE,
    next_eligible_date DATE,
    qr_version VARCHAR(8) DEFAULT 'v1',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ration_cards_status ON ration_cards(card_status);
CREATE INDEX idx_ration_cards_fps ON ration_cards(registered_fps_id);
CREATE INDEX idx_ration_cards_beneficiary ON ration_cards(beneficiary_id);

-- Family Members linked to Card (For NFSA member entitlement calculations)
CREATE TABLE IF NOT EXISTS card_family_members (
    member_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id VARCHAR(32) NOT NULL REFERENCES ration_cards(card_id) ON DELETE CASCADE,
    member_name VARCHAR(128) NOT NULL,
    relationship_with_head VARCHAR(32) NOT NULL,
    masked_aadhaar VARCHAR(16) NOT NULL,
    aadhaar_vault_ref VARCHAR(64),
    age INT CHECK (age >= 0),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_family_members_card ON card_family_members(card_id);

-- Commodities Master Catalog
CREATE TABLE IF NOT EXISTS commodities (
    commodity_id VARCHAR(16) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    vernacular_name VARCHAR(64),                      -- Hindi / State language title
    unit VARCHAR(16) NOT NULL,                        -- kg, L
    market_mrp NUMERIC(10, 2) NOT NULL,
    subsidized_price NUMERIC(10, 2) NOT NULL,         -- Subsidized rate under NFSA (e.g. 0.00 for Rice/Wheat)
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Monthly / Period Quota Entitlements per Card
CREATE TABLE IF NOT EXISTS card_entitlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id VARCHAR(32) NOT NULL REFERENCES ration_cards(card_id) ON DELETE CASCADE,
    commodity_id VARCHAR(16) NOT NULL REFERENCES commodities(commodity_id) ON DELETE RESTRICT,
    cycle_month_year VARCHAR(8) NOT NULL,             -- e.g. "08-2026"
    monthly_entitlement NUMERIC(10, 3) NOT NULL,
    already_collected NUMERIC(10, 3) DEFAULT 0.000,
    price_per_unit NUMERIC(10, 2) NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_card_commodity_cycle UNIQUE (card_id, commodity_id, cycle_month_year)
);

CREATE INDEX idx_entitlements_card_cycle ON card_entitlements(card_id, cycle_month_year);

-- Fair Price Shop Live Inventory Stock
CREATE TABLE IF NOT EXISTS dealer_inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fps_id VARCHAR(32) NOT NULL REFERENCES fair_price_shops(fps_id) ON DELETE CASCADE,
    commodity_id VARCHAR(16) NOT NULL REFERENCES commodities(commodity_id) ON DELETE RESTRICT,
    current_stock NUMERIC(12, 3) NOT NULL CHECK (current_stock >= 0),
    min_threshold NUMERIC(12, 3) NOT NULL,
    last_restocked_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_fps_commodity_stock UNIQUE (fps_id, commodity_id)
);

CREATE INDEX idx_inventory_fps ON dealer_inventory(fps_id);

-- -----------------------------------------------------------------------------
-- 3. TRANSACTIONS & COMMODITY DISTRIBUTION
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS transactions (
    transaction_id VARCHAR(64) PRIMARY KEY,           -- e.g. "TXN-1727092100-AB8841"
    idempotency_key VARCHAR(64) UNIQUE NOT NULL,      -- SHA-256 (card:dealer:period:window)
    invoice_number VARCHAR(32) UNIQUE NOT NULL,
    card_id VARCHAR(32) NOT NULL REFERENCES ration_cards(card_id) ON DELETE RESTRICT,
    beneficiary_name VARCHAR(128) NOT NULL,
    masked_card_number VARCHAR(24) NOT NULL,
    fps_id VARCHAR(32) NOT NULL REFERENCES fair_price_shops(fps_id) ON DELETE RESTRICT,
    dealer_id VARCHAR(32) NOT NULL REFERENCES users(user_id) ON DELETE RESTRICT,
    pos_device_id VARCHAR(64) NOT NULL REFERENCES pos_devices(device_id) ON DELETE RESTRICT,
    period_mode period_mode_enum NOT NULL DEFAULT 'MONTHLY',
    period_description VARCHAR(64) NOT NULL,
    total_quantity_kg NUMERIC(10, 3) NOT NULL,
    total_amount_paid NUMERIC(10, 2) NOT NULL,
    auth_method_used auth_method_enum NOT NULL,
    risk_score INT NOT NULL CHECK (risk_score BETWEEN 0 AND 100),
    risk_level risk_level_enum NOT NULL,
    digital_bill_hash VARCHAR(64) NOT NULL,           -- SHA-256 hash of invoice contents
    verification_token_signature VARCHAR(128) NOT NULL,
    receipt_qr_payload TEXT NOT NULL,
    sync_status sync_status_enum NOT NULL DEFAULT 'SYNCED_ONLINE',
    is_offline_created BOOLEAN NOT NULL DEFAULT FALSE,
    client_timestamp BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_transactions_card ON transactions(card_id);
CREATE INDEX idx_transactions_fps_date ON transactions(fps_id, created_at);
CREATE INDEX idx_transactions_sync ON transactions(sync_status);

-- Individual Distributed Items within Transaction
CREATE TABLE IF NOT EXISTS transaction_items (
    item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id VARCHAR(64) NOT NULL REFERENCES transactions(transaction_id) ON DELETE CASCADE,
    commodity_id VARCHAR(16) NOT NULL REFERENCES commodities(commodity_id) ON DELETE RESTRICT,
    commodity_name VARCHAR(64) NOT NULL,
    quantity NUMERIC(10, 3) NOT NULL CHECK (quantity > 0),
    unit VARCHAR(16) NOT NULL,
    rate_per_unit NUMERIC(10, 2) NOT NULL,
    total_cost NUMERIC(10, 2) NOT NULL
);

CREATE INDEX idx_transaction_items_txn ON transaction_items(transaction_id);

-- -----------------------------------------------------------------------------
-- 4. IMMUTABLE TAMPER-EVIDENT AUDIT LEDGER
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(64) UNIQUE NOT NULL,
    timestamp_epoch_ms BIGINT NOT NULL,
    formatted_time TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    dealer_id VARCHAR(32) NOT NULL,
    device_id VARCHAR(64) NOT NULL,
    card_id VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    result VARCHAR(64) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    previous_hash VARCHAR(64) NOT NULL,                -- Pointer to previous block's current_hash
    current_hash VARCHAR(64) NOT NULL,                 -- SHA-256 (prev_hash : event_id : timestamp : action : result)
    details TEXT NOT NULL,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_logs_event ON audit_logs(event_id);
CREATE INDEX idx_audit_logs_card ON audit_logs(card_id);
CREATE INDEX idx_audit_logs_dealer ON audit_logs(dealer_id);
CREATE INDEX idx_audit_logs_prev_hash ON audit_logs(previous_hash);

-- Strict Immutability Trigger: PREVENT ANY UPDATE OR DELETE ON AUDIT_LOGS
CREATE OR REPLACE FUNCTION enforce_audit_immutability()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'CRITICAL SECURITY BREACH ATTEMPT: Audit records are cryptographically immutable. UPDATE and DELETE operations are strictly forbidden.';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_audit_logs_immutable ON audit_logs;
CREATE TRIGGER trg_audit_logs_immutable
BEFORE UPDATE OR DELETE ON audit_logs
FOR EACH ROW EXECUTE FUNCTION enforce_audit_immutability();

-- -----------------------------------------------------------------------------
-- 5. AI FRAUD ALERTS & CITIZEN GRIEVANCE REDRESSAL
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS fraud_alerts (
    alert_id BIGSERIAL PRIMARY KEY,
    alert_code VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    fps_id VARCHAR(32) REFERENCES fair_price_shops(fps_id),
    card_id VARCHAR(32) REFERENCES ration_cards(card_id),
    pos_device_id VARCHAR(64),
    anomaly_score NUMERIC(5, 2) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description TEXT NOT NULL,
    is_resolved BOOLEAN DEFAULT FALSE,
    resolved_by VARCHAR(32) REFERENCES users(user_id),
    resolution_notes TEXT,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fraud_alerts_severity ON fraud_alerts(severity, is_resolved);

CREATE TABLE IF NOT EXISTS complaints (
    complaint_id VARCHAR(32) PRIMARY KEY,             -- Format: GRV-2026-XXXX
    card_id VARCHAR(32) NOT NULL REFERENCES ration_cards(card_id),
    beneficiary_name VARCHAR(128) NOT NULL,
    fps_id VARCHAR(32) NOT NULL REFERENCES fair_price_shops(fps_id),
    category VARCHAR(64) NOT NULL,                    -- e.g. "Wrong quantity given", "Dealer refused"
    description TEXT NOT NULL,
    status grievance_status_enum DEFAULT 'SUBMITTED',
    resolution_remark TEXT,
    investigating_officer_id VARCHAR(32) REFERENCES users(user_id),
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_complaints_card ON complaints(card_id);
CREATE INDEX idx_complaints_fps ON complaints(fps_id);
CREATE INDEX idx_complaints_status ON complaints(status);
