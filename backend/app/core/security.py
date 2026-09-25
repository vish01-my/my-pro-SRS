import hmac
import hashlib
import time
from datetime import datetime, timedelta, timezone
from typing import Optional, Dict, Any, Tuple
import jwt
from passlib.context import CryptContext
from app.core.config import settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password: str) -> str:
    return pwd_context.hash(password)

def create_access_token(subject: str, claims: Optional[Dict[str, Any]] = None, expires_delta: Optional[timedelta] = None) -> str:
    if expires_delta:
        expire = datetime.now(timezone.utc) + expires_delta
    else:
        expire = datetime.now(timezone.utc) + timedelta(minutes=settings.ACCESS_TOKEN_EXPIRE_MINUTES)
    
    to_encode = {"exp": expire, "sub": str(subject)}
    if claims:
        to_encode.update(claims)
    
    encoded_jwt = jwt.encode(to_encode, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)
    return encoded_jwt

def decode_access_token(token: str) -> Optional[Dict[str, Any]]:
    try:
        decoded = jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=[settings.JWT_ALGORITHM])
        return decoded
    except jwt.PyJWTError:
        return None

def compute_card_signature(raw_payload: str, secret: str = settings.CARD_HMAC_SECRET) -> str:
    """
    Computes HMAC-SHA256 signature for the given raw payload.
    """
    return hmac.new(
        secret.encode("utf-8"),
        raw_payload.encode("utf-8"),
        hashlib.sha256
    ).hexdigest()

def verify_smart_card_token(token_payload: str) -> Tuple[bool, str, Optional[Dict[str, Any]]]:
    """
    Validates token payload in the format:
    SRC:v1:<cardId>:<nonce>:<expiryTimestamp>:<signature>
    
    Returns:
    (is_valid, error_code_or_message, parsed_data)
    """
    parts = token_payload.strip().split(":")
    if len(parts) != 6:
        return False, "INVALID_TOKEN_FORMAT: Expected 6 delimited segments", None
    
    prefix, version, card_id, nonce, expiry_str, signature = parts
    
    if prefix != "SRC" or version != "v1":
        return False, "UNSUPPORTED_TOKEN_VERSION: Protocol mismatch", None
    
    # Check expiry
    try:
        expiry_ts = int(expiry_str)
        current_ts = int(time.time() * 1000)
        if current_ts > expiry_ts:
            return False, "TOKEN_EXPIRED: Cryptographic card token has expired", None
    except ValueError:
        return False, "INVALID_EXPIRY_TIMESTAMP: Malformed timestamp", None
    
    # Verify HMAC-SHA256
    data_to_verify = f"{prefix}:{version}:{card_id}:{nonce}:{expiry_str}"
    expected_signature = compute_card_signature(data_to_verify, settings.CARD_HMAC_SECRET)
    
    # Constant-time comparison to prevent side-channel timing attacks
    if not hmac.compare_digest(signature.lower(), expected_signature.lower()):
        return False, "SIGNATURE_FORGERY_DETECTED: Cryptographic signature mismatch", None
    
    return True, "SUCCESS", {
        "card_id": card_id,
        "nonce": nonce,
        "expiry_timestamp": expiry_ts,
        "version": version
    }

def generate_idempotency_key(card_id: str, dealer_id: str, period_key: str, timestamp_epoch_ms: int) -> str:
    """
    Creates a unique idempotency hash preventing double-debits across retry windows.
    """
    window = timestamp_epoch_ms // (1000 * 60 * 5)  # 5-minute deduplication window
    raw = f"{card_id}:{dealer_id}:{period_key}:{window}"
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()
