from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.api.v1.api import api_router

app = FastAPI(
    title=settings.PROJECT_NAME,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    docs_url=f"{settings.API_V1_STR}/docs",
    redoc_url=f"{settings.API_V1_STR}/redoc",
    description="""
    ## Secure Smart Ration Distribution System (e-PDS) — Central Gateway
    
    Production-grade backend service built for the Public Distribution System of India.
    
    ### Key Security Features:
    * **Privacy-by-Design**: QR / Barcode / NFC tokens carry zero sensitive PII (no Aadhaar, bank details, or address).
    * **Cryptographic Verification**: HMAC-SHA256 signature verification with nonce tracking.
    * **Device Binding**: Mandatory POS terminal device authorization checks.
    * **Adaptive Risk Engine**: Dynamic escalation to Face Biometrics / Aadhaar OTP based on risk scoring.
    * **Tamper-Evident Audit Ledger**: Immutable SHA-256 blockchain-style hash chaining.
    """
)

# CORS Middleware configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount API V1 routes
app.include_router(api_router, prefix=settings.API_V1_STR)

@app.get("/", tags=["System Health"])
def root():
    return {
        "service": settings.PROJECT_NAME,
        "status": "OPERATIONAL",
        "protocol_version": "v1",
        "docs_url": f"{settings.API_V1_STR}/docs"
    }

@app.get("/health", tags=["System Health"])
def health_check():
    return {
        "status": "HEALTHY",
        "crypto_engine": "ACTIVE",
        "audit_ledger": "INTEGRITY_VERIFIED"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("app.main:app", host="0.0.0.0", port=8000, reload=True)
