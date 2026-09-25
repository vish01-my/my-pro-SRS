import time
from fastapi.testclient import TestClient
from app.main import app
from app.services.auth_service import auth_service

client = TestClient(app)

def get_auth_token():
    # Login dealer to obtain Bearer JWT
    resp = client.post(
        "/api/v1/auth/token",
        data={
            "username": "DL-DEL-0492",
            "password": "Dealer@Demo2026!",
            "pos_device_id": "POS-DEV-IND-8841"
        }
    )
    assert resp.status_code == 200
    return resp.json()["access_token"]

def test_offline_reconciliation_atomicity_and_idempotency():
    token = get_auth_token()
    headers = {"Authorization": f"Bearer {token}"}

    unique_key = f"IDEM-TEST-{int(time.time() * 1000)}"
    txn_id = f"TXN-TEST-{int(time.time() * 1000)}"

    payload = {
        "dealer_id": "DL-DEL-0492",
        "pos_device_id": "POS-DEV-IND-8841",
        "batch_id": "BATCH-TEST-001",
        "transactions": [
            {
                "transaction_id": txn_id,
                "idempotency_key": unique_key,
                "invoice_number": "INV-TEST-001",
                "card_id": "SRC-DL-2026-99214",
                "beneficiary_name": "Ramesh Kumar",
                "fps_id": "FPS-110001-084",
                "period_mode": "MONTHLY",
                "period_description": f"Period-{unique_key}",
                "total_quantity_kg": 35.0,
                "total_amount_paid": 0.0,
                "auth_method_used": "QR + FACE_BIOMETRIC",
                "digital_bill_hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "verification_token_signature": "TEST_SIG_VALID",
                "created_at_timestamp": int(time.time() * 1000)
            }
        ]
    }

    # 1. First Submission -> Should be RECONCILED
    resp1 = client.post("/api/v1/transactions/reconcile", json=payload, headers=headers)
    assert resp1.status_code == 200
    data1 = resp1.json()
    assert data1["reconciled_count"] == 1
    assert data1["duplicate_count"] == 0
    assert data1["conflict_count"] == 0
    assert data1["results"][0]["status"] == "RECONCILED"
    assert "Batch processed in single transaction isolation" in data1["atomicity_guarantee"]

    # 2. Replay with identical idempotency_key (simulating network dropped response retry)
    # -> Should be DUPLICATE_IDEMPOTENT_IGNORED without errors or double quota deduction
    resp2 = client.post("/api/v1/transactions/reconcile", json=payload, headers=headers)
    assert resp2.status_code == 200
    data2 = resp2.json()
    assert data2["reconciled_count"] == 0
    assert data2["duplicate_count"] == 1
    assert data2["conflict_count"] == 0
    assert data2["results"][0]["status"] == "DUPLICATE_IDEMPOTENT_IGNORED"

def test_offline_reconciliation_conflict_blocked_card():
    token = get_auth_token()
    headers = {"Authorization": f"Bearer {token}"}

    unique_key = f"IDEM-BLOCKED-{int(time.time() * 1000)}"
    txn_id = f"TXN-BLOCKED-{int(time.time() * 1000)}"

    payload = {
        "dealer_id": "DL-DEL-0492",
        "pos_device_id": "POS-DEV-IND-8841",
        "batch_id": "BATCH-TEST-BLOCKED",
        "transactions": [
            {
                "transaction_id": txn_id,
                "idempotency_key": unique_key,
                "invoice_number": "INV-BLOCKED-001",
                "card_id": "SRC-UP-2026-77312",  # Blocked card in beneficiary store
                "beneficiary_name": "Mohammed Ansari",
                "fps_id": "FPS-110001-084",
                "period_mode": "MONTHLY",
                "period_description": "Current Period",
                "total_quantity_kg": 20.0,
                "total_amount_paid": 0.0,
                "auth_method_used": "QR_MANUAL",
                "digital_bill_hash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                "verification_token_signature": "TEST_SIG",
                "created_at_timestamp": int(time.time() * 1000)
            }
        ]
    }

    resp = client.post("/api/v1/transactions/reconcile", json=payload, headers=headers)
    assert resp.status_code == 200
    data = resp.json()
    assert data["conflict_count"] == 1
    assert data["results"][0]["status"] == "CONFLICT_FLAGGED"
    assert "BLOCKED" in data["results"][0]["message"]
