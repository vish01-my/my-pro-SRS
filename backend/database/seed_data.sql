-- =============================================================================
-- SMART RATION DISTRIBUTION SYSTEM (e-PDS) — DEMO SEED DATA
-- Matches the core security test suite and Android POS offline dataset
-- =============================================================================

-- 1. Fair Price Shop
INSERT INTO fair_price_shops (fps_id, shop_name, state, district, sub_district, pincode, latitude, longitude, status, contact_phone)
VALUES 
('FPS-110001-084', 'Fair Price Shop #84 (Connaught Place)', 'NCT of Delhi', 'Central Delhi', 'Chanakyapuri', '110001', 28.631500, 77.216700, 'ACTIVE', '+911123456789'),
('FPS-380001-012', 'Fair Price Shop #12 (Navrangpura)', 'Gujarat', 'Ahmedabad', 'Ahmedabad West', '380001', 23.036500, 72.561100, 'ACTIVE', '+917923456789'),
('FPS-201001-045', 'Fair Price Shop #45 (Raj Nagar)', 'Uttar Pradesh', 'Ghaziabad', 'Ghaziabad Sadar', '201001', 28.669200, 77.453800, 'ACTIVE', '+911202345678')
ON CONFLICT (fps_id) DO NOTHING;

-- 2. Users (Dealers, Supervisors, District Officers, State Admin, Auditor)
-- Default demo password hash corresponds to standard test passwords (e.g. PdsDealer@2026)
INSERT INTO users (user_id, username, password_hash, full_name, role, fps_id, state, district, mobile_masked, is_active)
VALUES 
('DL-DEL-0492', 'dealer_cp84', '$2b$12$e8x5a3z9/8s9d7f6g5h4j3k2l1m0n9b8v7c6x5z4a3s2d1f0g9h8j', 'Suresh Chandra', 'DEALER', 'FPS-110001-084', 'NCT of Delhi', 'Central Delhi', '******9821', TRUE),
('SUP-DEL-0012', 'supervisor_del', '$2b$12$e8x5a3z9/8s9d7f6g5h4j3k2l1m0n9b8v7c6x5z4a3s2d1f0g9h8j', 'Anil Verma (Supply Inspector)', 'SUPERVISOR', 'FPS-110001-084', 'NCT of Delhi', 'Central Delhi', '******4412', TRUE),
('DSO-DEL-0001', 'dso_central', '$2b$12$e8x5a3z9/8s9d7f6g5h4j3k2l1m0n9b8v7c6x5z4a3s2d1f0g9h8j', 'Dr. Meenakshi Sundaram (DSO)', 'DISTRICT_OFFICER', NULL, 'NCT of Delhi', 'Central Delhi', '******7723', TRUE),
('ADM-NIC-0001', 'admin_pds_hq', '$2b$12$e8x5a3z9/8s9d7f6g5h4j3k2l1m0n9b8v7c6x5z4a3s2d1f0g9h8j', 'NIC Food Commissioner Admin', 'STATE_ADMIN', NULL, 'NCT of Delhi', 'New Delhi', '******1100', TRUE),
('AUD-CAG-0001', 'auditor_cag', '$2b$12$e8x5a3z9/8s9d7f6g5h4j3k2l1m0n9b8v7c6x5z4a3s2d1f0g9h8j', 'CAG Supply Auditor', 'AUDITOR', NULL, 'NCT of Delhi', 'New Delhi', '******9999', TRUE)
ON CONFLICT (user_id) DO NOTHING;

-- 3. Authorized POS Terminals
INSERT INTO pos_devices (device_id, fps_id, dealer_id, device_model, hardware_serial, status, ip_address_masked)
VALUES
('POS-DEV-IND-8841', 'FPS-110001-084', 'DL-DEL-0492', 'Posiflex Vision Smart e-POS 4G', 'SN-HW-8841-IND-2026', 'AUTHORIZED', '10.14.***.***')
ON CONFLICT (device_id) DO NOTHING;

-- 4. Commodities
INSERT INTO commodities (commodity_id, name, vernacular_name, unit, market_mrp, subsidized_price, is_active)
VALUES
('RICE', 'Fortified Rice', 'चावल (फोर्टिफाइड)', 'kg', 34.00, 0.00, TRUE),
('WHEAT', 'Whole Wheat Grain', 'गेहूँ', 'kg', 28.00, 0.00, TRUE),
('SUGAR', 'Subsidized Sugar', 'चीनी', 'kg', 42.00, 13.50, TRUE),
('DAL', 'Chana Dal (Pulses)', 'चना दाल', 'kg', 78.00, 30.00, TRUE),
('SALT', 'Iodized Salt', 'आयोडीन युक्त नमक', 'kg', 18.00, 5.00, TRUE),
('OIL', 'Fortified Mustard Oil', 'सरसों का तेल', 'L', 145.00, 65.00, TRUE)
ON CONFLICT (commodity_id) DO NOTHING;

-- 5. Fair Price Shop Stock Inventory
INSERT INTO dealer_inventory (fps_id, commodity_id, current_stock, min_threshold, last_restocked_at)
VALUES
('FPS-110001-084', 'RICE', 850.000, 200.000, CURRENT_TIMESTAMP - INTERVAL '3 days'),
('FPS-110001-084', 'WHEAT', 620.000, 150.000, CURRENT_TIMESTAMP - INTERVAL '3 days'),
('FPS-110001-084', 'SUGAR', 140.000, 50.000, CURRENT_TIMESTAMP - INTERVAL '3 days'),
('FPS-110001-084', 'DAL', 95.000, 30.000, CURRENT_TIMESTAMP - INTERVAL '3 days'),
('FPS-110001-084', 'SALT', 110.000, 25.000, CURRENT_TIMESTAMP - INTERVAL '3 days'),
('FPS-110001-084', 'OIL', 80.000, 20.000, CURRENT_TIMESTAMP - INTERVAL '3 days')
ON CONFLICT (fps_id, commodity_id) DO NOTHING;

-- 6. Beneficiaries & Smart Ration Cards
-- Beneficiary 1: Ramesh Kumar (Active PHH)
INSERT INTO beneficiaries (beneficiary_id, head_of_family_name, masked_aadhaar, aadhaar_vault_reference, masked_mobile, gender, family_members_count, residential_state, residential_district)
VALUES
('11111111-1111-1111-1111-111111111111', 'Ramesh Kumar', 'XXXX-XXXX-2847', 'AV-VAULT-DEL-99214-X', '******9821', 'MALE', 4, 'NCT of Delhi', 'Central Delhi')
ON CONFLICT (beneficiary_id) DO NOTHING;

INSERT INTO ration_cards (card_id, beneficiary_id, masked_card_number, category, card_status, registered_fps_id, secret_card_salt, reference_face_hash, issue_date, last_distribution_date, next_eligible_date)
VALUES
('SRC-DL-2026-99214', '11111111-1111-1111-1111-111111111111', 'XXXX-XXXX-2847', 'PHH', 'ACTIVE', 'FPS-110001-084', 'SALT_RAMESH_99', 'FACE_HASH_RAMESH_KUMAR_2847', '2022-01-15', '2026-08-01', '2026-09-01')
ON CONFLICT (card_id) DO NOTHING;

-- Beneficiary 2: Sunita Devi (Active AAY)
INSERT INTO beneficiaries (beneficiary_id, head_of_family_name, masked_aadhaar, aadhaar_vault_reference, masked_mobile, gender, family_members_count, residential_state, residential_district)
VALUES
('22222222-2222-2222-2222-222222222222', 'Sunita Devi', 'XXXX-XXXX-5519', 'AV-VAULT-DEL-88102-Y', '******4412', 'FEMALE', 5, 'NCT of Delhi', 'Central Delhi')
ON CONFLICT (beneficiary_id) DO NOTHING;

INSERT INTO ration_cards (card_id, beneficiary_id, masked_card_number, category, card_status, registered_fps_id, secret_card_salt, reference_face_hash, issue_date, last_distribution_date, next_eligible_date)
VALUES
('SRC-DL-2026-88102', '22222222-2222-2222-2222-222222222222', 'XXXX-XXXX-5519', 'AAY', 'ACTIVE', 'FPS-110001-084', 'SALT_SUNITA_88', 'FACE_HASH_SUNITA_DEVI_5519', '2021-06-10', '2026-07-28', '2026-09-01')
ON CONFLICT (card_id) DO NOTHING;

-- Beneficiary 3: Rajesh Patel (Gujarat ONORC Portability)
INSERT INTO beneficiaries (beneficiary_id, head_of_family_name, masked_aadhaar, aadhaar_vault_reference, masked_mobile, gender, family_members_count, residential_state, residential_district)
VALUES
('33333333-3333-3333-3333-333333333333', 'Rajesh Patel', 'XXXX-XXXX-9103', 'AV-VAULT-GJ-14029-Z', '******7723', 'MALE', 3, 'Gujarat', 'Ahmedabad')
ON CONFLICT (beneficiary_id) DO NOTHING;

INSERT INTO ration_cards (card_id, beneficiary_id, masked_card_number, category, card_status, registered_fps_id, secret_card_salt, reference_face_hash, issue_date, last_distribution_date, next_eligible_date)
VALUES
('SRC-GJ-2026-14029', '33333333-3333-3333-3333-333333333333', 'XXXX-XXXX-9103', 'PHH', 'ACTIVE', 'FPS-380001-012', 'SALT_RAJESH_14', 'FACE_HASH_RAJESH_PATEL_9103', '2023-03-20', '2026-07-15', '2026-09-01')
ON CONFLICT (card_id) DO NOTHING;

-- Beneficiary 4: Mohammed Ansari (BLOCKED Card)
INSERT INTO beneficiaries (beneficiary_id, head_of_family_name, masked_aadhaar, aadhaar_vault_reference, masked_mobile, gender, family_members_count, residential_state, residential_district)
VALUES
('44444444-4444-4444-4444-444444444444', 'Mohammed Ansari', 'XXXX-XXXX-4420', 'AV-VAULT-UP-77312-W', '******3301', 'MALE', 4, 'Uttar Pradesh', 'Ghaziabad')
ON CONFLICT (beneficiary_id) DO NOTHING;

INSERT INTO ration_cards (card_id, beneficiary_id, masked_card_number, category, card_status, registered_fps_id, secret_card_salt, reference_face_hash, issue_date, last_distribution_date, next_eligible_date)
VALUES
('SRC-UP-2026-77312', '44444444-4444-4444-4444-444444444444', 'XXXX-XXXX-4420', 'PHH', 'BLOCKED', 'FPS-201001-045', 'SALT_MOHAMMED_77', 'FACE_HASH_MOHAMMED_ANSARI_4420', '2020-09-12', '2026-06-10', '2026-09-01')
ON CONFLICT (card_id) DO NOTHING;

-- 7. Monthly Card Entitlements (Current cycle: "08-2026")
INSERT INTO card_entitlements (card_id, commodity_id, cycle_month_year, monthly_entitlement, already_collected, price_per_unit)
VALUES
('SRC-DL-2026-99214', 'RICE', '08-2026', 20.000, 5.000, 0.00),
('SRC-DL-2026-99214', 'WHEAT', '08-2026', 5.000, 0.000, 0.00),
('SRC-DL-2026-99214', 'SUGAR', '08-2026', 2.000, 0.000, 13.50),

('SRC-DL-2026-88102', 'RICE', '08-2026', 25.000, 0.000, 0.00),
('SRC-DL-2026-88102', 'WHEAT', '08-2026', 10.000, 0.000, 0.00),
('SRC-DL-2026-88102', 'SUGAR', '08-2026', 1.000, 0.000, 13.50),
('SRC-DL-2026-88102', 'OIL', '08-2026', 1.000, 0.000, 65.00),

('SRC-GJ-2026-14029', 'RICE', '08-2026', 15.000, 0.000, 0.00),
('SRC-GJ-2026-14029', 'WHEAT', '08-2026', 10.000, 0.000, 0.00)
ON CONFLICT (card_id, commodity_id, cycle_month_year) DO NOTHING;

-- 8. Genesis Audit Block
INSERT INTO audit_logs (event_id, timestamp_epoch_ms, dealer_id, device_id, card_id, action, result, risk_level, previous_hash, current_hash, details)
VALUES
('AUD-GENESIS-001', 1727082000000, 'SYSTEM_INIT', 'CENTRAL_PDS_SRV', 'N/A', 'DATABASE_INITIALIZATION', 'SUCCESS', 'LOW', 
 '0000000000000000000000000000000000000000000000000000000000000000', 
 encode(digest('GENESIS_BLOCK_PDS_2026', 'sha256'), 'hex'), 
 'National e-PDS Central Database schema initialized with cryptographic tamper-evident audit ledger.')
ON CONFLICT (event_id) DO NOTHING;
