from fastapi import APIRouter, Depends
from app.models.schemas import (
    FaceVerificationRequest,
    FaceVerificationResponse,
    TokenData
)
from app.services.auth_service import get_current_dealer
from app.services.face_verification_service import face_verification_service

router = APIRouter()

@router.post(
    "/verify-face",
    response_model=FaceVerificationResponse,
    summary="Verify Beneficiary Face Biometrics (Privacy-Preserving)"
)
def verify_beneficiary_face(
    request: FaceVerificationRequest,
    current_dealer: TokenData = Depends(get_current_dealer)
):
    """
    Privacy-Preserving Face Verification Endpoint:
    
    1. Compares live-captured facial features or embeddings against registered template hashes in the PDS registry.
    2. Enforces anti-spoofing and liveness checks (eye-blink verification, Euler pose limits).
    3. Guarantees Privacy-by-Design: Raw camera frames are processed ephemerally and never stored.
    4. Issues a cryptographically signed biometric authentication token upon match.
    5. Records the outcome into the tamper-evident SHA-256 audit ledger.
    """
    pos_device_id = request.pos_device_id or current_dealer.pos_device_id
    
    return face_verification_service.verify_beneficiary_face(
        request=request,
        dealer_id=current_dealer.dealer_id,
        pos_device_id=pos_device_id
    )
