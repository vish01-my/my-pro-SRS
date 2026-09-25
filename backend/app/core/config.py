import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "Smart Ration PDS Central Backend"
    API_V1_STR: str = "/api/v1"
    
    # Cryptographic keys
    # In production, injected securely via environment secrets
    CARD_HMAC_SECRET: str = os.getenv("CARD_HMAC_SECRET", "NFSA_CENTRAL_GOVT_MASTER_KEY_2026_X982")
    JWT_SECRET_KEY: str = os.getenv("JWT_SECRET_KEY", "PDS_POS_STATION_JWT_SECRET_KEY_8841_SECURE")
    JWT_ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 480  # 8 hour POS terminal operator shift
    
    # Risk Engine Thresholds
    RISK_THRESHOLD_MEDIUM: int = 25
    RISK_THRESHOLD_HIGH: int = 60
    RISK_THRESHOLD_CRITICAL: int = 85

    class Config:
        case_sensitive = True

settings = Settings()
