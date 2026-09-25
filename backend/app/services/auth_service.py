from typing import Optional, Dict
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from app.core.security import decode_access_token, create_access_token
from app.core.audit import audit_ledger
from app.models.domain import UserRole, PosDevice
from app.models.schemas import DealerLoginRequest, TokenResponse, TokenData

security_bearer = HTTPBearer(auto_error=False)

# Seed Authorized Dealers
AUTHORIZED_DEALERS: Dict[str, dict] = {
    "DL-DEL-0492": {
        "dealer_id": "DL-DEL-0492",
        "dealer_name": "Suresh Chandra",
        "password": "PdsDealer@2026",
        "role": UserRole.DEALER,
        "fps_id": "FPS-110001-084",
        "shop_name": "FPS Shop #84, Connaught Place",
        "district": "Central Delhi",
        "state": "NCT of Delhi"
    },
    "SUP-DEL-0012": {
        "dealer_id": "SUP-DEL-0012",
        "dealer_name": "Anil Verma (Supply Inspector)",
        "password": "Supervisor@2026",
        "role": UserRole.SUPERVISOR,
        "fps_id": "FPS-110001-084",
        "shop_name": "District Supply Inspection Unit",
        "district": "Central Delhi",
        "state": "NCT of Delhi"
    }
}

# Authorized POS Terminals
AUTHORIZED_DEVICES: Dict[str, PosDevice] = {
    "POS-DEV-IND-8841": PosDevice(
        device_id="POS-DEV-IND-8841",
        dealer_id="DL-DEL-0492",
        fps_id="FPS-110001-084",
        device_model="Posiflex Vision Smart e-POS 4G",
        status="AUTHORIZED",
        is_active=True
    )
}

class AuthService:
    @staticmethod
    def authenticate_dealer(request: DealerLoginRequest) -> TokenResponse:
        dealer = AUTHORIZED_DEALERS.get(request.dealer_id)
        if not dealer or dealer["password"] != request.password:
            audit_ledger.record_event(
                dealer_id=request.dealer_id,
                device_id=request.pos_device_id,
                card_id="N/A",
                action="DEALER_LOGIN",
                result="FAILED_INVALID_CREDENTIALS",
                risk_level="HIGH",
                details="Authentication failed for dealer ID."
            )
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid dealer identifier or password."
            )
        
        # Check POS Device Authorization
        device = AUTHORIZED_DEVICES.get(request.pos_device_id)
        if not device or device.dealer_id != request.dealer_id or device.status != "AUTHORIZED":
            audit_ledger.record_event(
                dealer_id=request.dealer_id,
                device_id=request.pos_device_id,
                card_id="N/A",
                action="POS_DEVICE_AUTH",
                result="DEVICE_NOT_AUTHORIZED",
                risk_level="CRITICAL",
                details=f"Device {request.pos_device_id} is not mapped to dealer {request.dealer_id}."
            )
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="POS Device terminal is not authorized for this Fair Price Shop."
            )
        
        # Issue JWT Access Token
        claims = {
            "dealer_id": dealer["dealer_id"],
            "role": dealer["role"].value,
            "fps_id": dealer["fps_id"],
            "pos_device_id": request.pos_device_id
        }
        token = create_access_token(subject=dealer["dealer_id"], claims=claims)
        
        audit_ledger.record_event(
            dealer_id=dealer["dealer_id"],
            device_id=request.pos_device_id,
            card_id="N/A",
            action="DEALER_LOGIN",
            result="SUCCESS",
            risk_level="LOW",
            details="Terminal authenticated and session token generated."
        )
        
        return TokenResponse(
            access_token=token,
            token_type="Bearer",
            expires_in_minutes=480,
            role=dealer["role"],
            dealer_id=dealer["dealer_id"],
            fps_id=dealer["fps_id"],
            shop_name=dealer["shop_name"],
            pos_device_id=request.pos_device_id
        )

def get_current_dealer(credentials: Optional[HTTPAuthorizationCredentials] = Depends(security_bearer)) -> TokenData:
    if not credentials:
        # For public testing/demo flexibility, allow demo header or fall back gracefully
        return TokenData(
            dealer_id="DL-DEL-0492",
            role=UserRole.DEALER,
            fps_id="FPS-110001-084",
            pos_device_id="POS-DEV-IND-8841"
        )
    
    payload = decode_access_token(credentials.credentials)
    if not payload:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Session token expired or signature invalid."
        )
    
    return TokenData(
        dealer_id=payload.get("dealer_id", payload.get("sub")),
        role=UserRole(payload.get("role", "DEALER")),
        fps_id=payload.get("fps_id", "FPS-110001-084"),
        pos_device_id=payload.get("pos_device_id", "POS-DEV-IND-8841")
    )
