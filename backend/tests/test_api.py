import pytest
import time
from fastapi.testclient import TestClient
from app.main import app
from app.core.security import compute_card_signature

client = TestClient(app)

def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "HEALTHY"
    assert data["crypto_engine"] == "ACTIVE"

def test_dealer_login_success():
    payload = {
        "dealer_id": "DL-DEL-0492",
        "password": "PdsDealer@2026",
        "pos_device_id": "POS-DEV-IND-8841"
    }
    response = client.post("/api/v1/auth/login", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert data["role"] == "DEALER"
    assert data["fps_id"] == "FPS-110001-084"

def test_dealer_login_invalid_password():
    payload = {
        "dealer_id": "DL-DEL-0492",
        "password": "WrongPassword123",
        "pos_device_id": "POS-DEV-IND-8841"
    }
    response = client.post("/api/v1/auth/login", json=payload)
    assert response.status_code == 401
    assert "Invalid dealer identifier" in response.json()["detail"]

def test_dealer_login_unauthorized_device():
    payload = {
        "dealer_id": "DL-DEL-0492",
        "password": "PdsDealer@2026",
        "pos_device_id": "UNAUTHORIZED_DEVICE_999"
    }
    response = client.post("/api/v1/auth/login", json=payload)
    assert response.status_code == 403
    assert "POS Device terminal is not authorized" in response.json()["detail"]

def test_verify_authentic_smart_card_token():
    # 1. Generate valid signed token for Ramesh Kumar
    card_id = "SRC-DL-2026-99214"
    expiry = int(time.time() * 1000) + (24 * 3600 * 1000)
    raw = f"SRC:v1:{card_id}:NONCE_9941:{expiry}"
    sig = compute_card_signature(raw)
    valid_token = f"{raw}:{sig}"

    # 2. Verify with API
    response = client.post("/api/v1/cards/verify-token", json={"token_payload": valid_token})
    assert response.status_code == 200
    data = response.json()
    assert data["is_valid"] is True
    assert data["status"] == "VERIFIED"
    assert data["beneficiary"]["head_of_family_name"] == "Ramesh Kumar"
    assert data["beneficiary"]["masked_card_number"] == "XXXX-XXXX-2847"
    # Privacy verification: Ensure no unmasked sensitive PII is returned
    assert "aadhaar" not in str(data).lower() or "xxxx" in str(data).lower()
    assert data["risk_assessment"]["total_score"] >= 0

def test_reject_counterfeit_tampered_token():
    # Forged signature token
    tampered_token = "SRC:v1:SRC-DL-2026-99214:NONCE_9941:1790175000000:FAKE_BAD_SIGNATURE_HERE"
    response = client.post("/api/v1/cards/verify-token", json={"token_payload": tampered_token})
    assert response.status_code == 200
    data = response.json()
    assert data["is_valid"] is False
    assert data["status"] == "REJECTED"
    assert "SIGNATURE_FORGERY_DETECTED" in data["error_code"]

def test_reject_expired_token():
    # Expired token in the past (timestamp = 1000)
    raw = "SRC:v1:SRC-DL-2026-99214:NONCE_9941:1000"
    sig = compute_card_signature(raw)
    expired_token = f"{raw}:{sig}"

    response = client.post("/api/v1/cards/verify-token", json={"token_payload": expired_token})
    assert response.status_code == 200
    data = response.json()
    assert data["is_valid"] is False
    assert "TOKEN_EXPIRED" in data["error_code"]

def test_reject_blocked_card():
    # Mohammed Ansari's blocked card
    card_id = "SRC-UP-2026-77312"
    expiry = int(time.time() * 1000) + (24 * 3600 * 1000)
    raw = f"SRC:v1:{card_id}:NONCE_7731:{expiry}"
    sig = compute_card_signature(raw)
    blocked_token = f"{raw}:{sig}"

    response = client.post("/api/v1/cards/verify-token", json={"token_payload": blocked_token})
    assert response.status_code == 200
    data = response.json()
    assert data["is_valid"] is False
    assert data["status"] == "BLOCKED"
    assert "CARD_BLOCKED" in data["error_code"]

def test_audit_ledger_integrity():
    response = client.get("/api/v1/audit/verify-integrity")
    assert response.status_code == 200
    data = response.json()
    assert data["valid"] is True
    assert data["total_blocks"] > 0
    assert "All audit blocks cryptographically linked" in data["message"]
