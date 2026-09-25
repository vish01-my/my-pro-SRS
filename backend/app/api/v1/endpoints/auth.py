from fastapi import APIRouter, Depends
from app.models.schemas import DealerLoginRequest, TokenResponse, TokenData
from app.services.auth_service import AuthService, get_current_dealer

router = APIRouter()

@router.post("/login", response_model=TokenResponse, summary="Dealer & POS Terminal Authentication")
def login(request: DealerLoginRequest):
    """
    Authenticates Fair Price Shop dealer and validates that the POS terminal device
    is authorized and active in the central hardware registry.
    """
    return AuthService.authenticate_dealer(request)

@router.get("/me", response_model=TokenData, summary="Get Current Session Profile")
def get_current_user_profile(current_dealer: TokenData = Depends(get_current_dealer)):
    """
    Returns the authenticated dealer identity and Fair Price Shop mapping.
    """
    return current_dealer
