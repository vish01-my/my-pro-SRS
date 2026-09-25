from fastapi import APIRouter
from app.api.v1.endpoints import auth, cards, audit, biometrics, transactions

api_router = APIRouter()

api_router.include_router(auth.router, prefix="/auth", tags=["Dealer & Device Authentication"])
api_router.include_router(cards.router, prefix="/cards", tags=["Smart Ration Card Verification"])
api_router.include_router(biometrics.router, prefix="/biometrics", tags=["Biometric Verification & Anti-Spoofing"])
api_router.include_router(transactions.router, prefix="/transactions", tags=["Offline Transaction Reconciliation"])
api_router.include_router(audit.router, prefix="/audit", tags=["Tamper-Evident Audit Ledger"])

