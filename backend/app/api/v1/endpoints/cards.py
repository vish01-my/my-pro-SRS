import time
from fastapi import APIRouter, Depends
from app.core.security import compute_card_signature
from app.models.schemas import (
    VerifyCardTokenRequest,
    VerifyCardTokenResponse,
    GenerateTestTokenRequest,
    GenerateTestTokenResponse,
    FaceVerificationRequest,
    FaceVerificationResponse,
    TokenData
)
from app.services.auth_service import get_current_dealer
from app.services.card_service import CardService
from app.services.face_verification_service import face_verification_service

router = APIRouter()

@router.post("/verify-face", response_model=FaceVerificationResponse, summary="Verify Beneficiary Face Biometrics")
def verify_face_biometrics(
    request: FaceVerificationRequest,
    current_dealer: TokenData = Depends(get_current_dealer)
):
    """
    Biometric Face Verification within the Smart Card verification lifecycle.
    """
    pos_device_id = request.pos_device_id or current_dealer.pos_device_id
    return face_verification_service.verify_beneficiary_face(
        request=request,
        dealer_id=current_dealer.dealer_id,
        pos_device_id=pos_device_id
    )

@router.post("/verify-token", response_model=VerifyCardTokenResponse, summary="Verify Secure Smart Ration Card Token")
def verify_card_token(
    request: VerifyCardTokenRequest,
    current_dealer: TokenData = Depends(get_current_dealer)
):
    """
    Core verification endpoint for QR / Barcode / NFC smart card tokens:
    1. Cryptographically validates HMAC-SHA256 signature and nonce.
    2. Checks card lifecycle status (Active / Blocked / Lost).
    3. Evaluates Adaptive Risk score & portability (ONORC).
    4. Records event in tamper-evident SHA-256 audit ledger.
    5. Returns minimal beneficiary entitlement data (Privacy-by-Design).
    """
    device_id = request.pos_device_id or current_dealer.pos_device_id
    fps_id = request.fps_id or current_dealer.fps_id
    
    return CardService.verify_card(
        request=request,
        dealer_id=current_dealer.dealer_id,
        pos_device_id=device_id,
        fps_id=fps_id
    )

@router.post("/generate-test-token", response_model=GenerateTestTokenResponse, summary="Generate Cryptographically Signed Test Token")
def generate_test_token(request: GenerateTestTokenRequest):
    """
    Developer / Testing utility to generate authentic signed tokens matching the 
    Smart Card format:
    'SRC:v1:<cardId>:<nonce>:<expiryTimestamp>:<signature>'
    """
    nonce = f"NONCE_{int(time.time() * 1000) % 10000}"
    expiry = int(time.time() * 1000) + (request.ttl_hours * 3600 * 1000)
    raw = f"SRC:v1:{request.card_id}:{nonce}:{expiry}"
    signature = compute_card_signature(raw)
    token_payload = f"{raw}:{signature}"
    
    return GenerateTestTokenResponse(
        token_payload=token_payload,
        card_id=request.card_id,
        expiry_timestamp=expiry
    )
