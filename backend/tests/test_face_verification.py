import base64
import pytest
from fastapi.testclient import TestClient
from app.main import app
from app.services.face_verification_service import (
    FaceVerificationService,
    CryptographicTemplateAdapter,
    EmbeddingBiometricAdapter
)
from app.models.schemas import FaceVerificationRequest

client = TestClient(app)

def test_face_verification_match_success():
    payload = {
        "card_id": "SRC-DL-2026-99214",
        "captured_face_hash": "FACE_HASH_RAMESH_KUMAR_2847",
        "liveness_blink_verified": True,
        "head_euler_yaw": 2.5,
        "head_euler_pitch": -1.0,
        "pos_device_id": "POS-DEV-IND-8841"
    }
    response = client.post("/api/v1/biometrics/verify-face", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["is_matched"] is True
    assert data["confidence_percentage"] >= 75.0
    assert data["liveness_verified"] is True
    assert data["beneficiary_name"] == "Ramesh Kumar"
    assert data["biometric_token"] is not None
    assert "BIO:v1:SRC-DL-2026-99214" in data["biometric_token"]
    assert "Raw biometric frames purged" in data["privacy_guarantee"]

def test_face_verification_alias_in_cards_endpoint():
    payload = {
        "card_id": "SRC-DL-2026-88102",
        "captured_face_hash": "FACE_HASH_SUNITA_DEVI_5519",
        "liveness_blink_verified": True,
        "pos_device_id": "POS-DEV-IND-8841"
    }
    response = client.post("/api/v1/cards/verify-face", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["is_matched"] is True
    assert data["beneficiary_name"] == "Sunita Devi"

def test_face_verification_simulated_mismatch():
    payload = {
        "card_id": "SRC-DL-2026-99214",
        "captured_face_hash": "FACE_HASH_UNKNOWN_PERSON",
        "simulate_mismatch": True,
        "pos_device_id": "POS-DEV-IND-8841"
    }
    response = client.post("/api/v1/biometrics/verify-face", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["is_matched"] is False
    assert data["confidence_percentage"] < 75.0
    assert "Biometric mismatch" in data["error_message"]

def test_face_verification_liveness_failure():
    payload = {
        "card_id": "SRC-DL-2026-99214",
        "captured_face_hash": "FACE_HASH_RAMESH_KUMAR_2847",
        "liveness_blink_verified": False,
        "head_euler_yaw": 35.0,  # Extreme angle indicates invalid pose
        "pos_device_id": "POS-DEV-IND-8841"
    }
    response = client.post("/api/v1/biometrics/verify-face", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["is_matched"] is False
    assert data["liveness_verified"] is False
    assert "Liveness check failed" in data["error_message"]

def test_face_verification_blocked_card_rejection():
    payload = {
        "card_id": "SRC-UP-2026-77312",
        "captured_face_hash": "FACE_HASH_MOHAMMED_ANSARI_4420",
        "pos_device_id": "POS-DEV-IND-8841"
    }
    response = client.post("/api/v1/biometrics/verify-face", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["is_matched"] is False
    assert "BLOCKED" in data["error_message"]

def test_face_verification_ephemeral_live_frame_purged():
    # Simulate a 100-byte dummy frame base64 payload
    dummy_frame = base64.b64encode(b"SIMULATED_CAMERA_FRAME_DATA_1234567890" * 4).decode("ascii")
    payload = {
        "card_id": "SRC-DL-2026-99214",
        "live_frame_base64": dummy_frame,
        "liveness_blink_verified": True,
        "pos_device_id": "POS-DEV-IND-8841"
    }
    response = client.post("/api/v1/biometrics/verify-face", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["is_matched"] is True
    assert "Zero persistent biometric storage" in data["privacy_guarantee"]

def test_embedding_biometric_adapter():
    service = FaceVerificationService(adapter=EmbeddingBiometricAdapter())
    request = FaceVerificationRequest(
        card_id="SRC-DL-2026-99214",
        face_embedding=[0.05 * i for i in range(32)],
        liveness_blink_verified=True
    )
    result = service.verify_beneficiary_face(request)
    assert result.card_id == "SRC-DL-2026-99214"
    assert result.confidence_percentage >= 0.0
