# Smart Ration Distribution System (e-PDS) — FastAPI Backend

Production-ready, privacy-first FastAPI backend service for the Indian Public Distribution System (PDS).

---

## Architecture Overview

```
backend/
├── app/
│   ├── api/
│   │   └── v1/
│   │       ├── endpoints/
│   │       │   ├── auth.py         # Dealer & POS terminal authentication
│   │       │   ├── cards.py        # Cryptographic smart card verification
│   │       │   └── audit.py        # SHA-256 tamper-evident audit ledger
│   │       └── api.py              # V1 router aggregation
│   ├── core/
│   │   ├── config.py               # Environment & security configurations
│   │   ├── security.py             # HMAC-SHA256, JWT, timing-attack prevention
│   │   └── audit.py                # Blockchain-style hash-chained ledger
│   ├── models/
│   │   ├── domain.py               # Domain models, enums (UserRole, CardStatus)
│   │   └── schemas.py              # Pydantic request/response schemas
│   ├── services/
│   │   ├── auth_service.py         # Dealer credential & device binding logic
│   │   └── card_service.py         # Token validation, risk scoring & privacy masking
│   └── main.py                     # FastAPI application factory & CORS setup
├── tests/
│   └── test_api.py                 # Full unit test suite (Auth, Crypto, Privacy)
├── requirements.txt                # Python dependencies
└── README.md
```

---

## Key Security Features

1. **Privacy-by-Design**:
   - The Smart Card payload (`SRC:v1:<cardId>:<nonce>:<expiry>:<signature>`) contains zero personal information.
   - Unmasked Aadhaar numbers, bank account details, and full residential addresses are **never** returned to the POS terminal.
2. **Cryptographic Integrity**:
   - `HMAC-SHA256` signature verification using `hmac.compare_digest` to prevent side-channel timing attacks.
   - Nonce freshness and expiration timestamp enforcement.
3. **Mutual Device & Operator Trust**:
   - Every POS terminal must be authorized in the hardware registry (`POS-DEV-IND-8841`) and bound to the authenticated Fair Price Shop dealer.
4. **Adaptive Risk Engine**:
   - Dynamically flags transactions based on inter-state portability (ONORC), card status, and velocity.
5. **Tamper-Evident SHA-256 Audit Ledger**:
   - Every login, scan, and validation event is linked in a cryptographic hash chain (`prev_hash -> current_hash`).

---

## Quickstart

### 1. Install Dependencies
```bash
cd backend
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

### 2. Run the Development Server
```bash
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

The interactive Swagger documentation will be available at:
`http://localhost:8000/api/v1/docs`

### 3. Run Tests
```bash
pytest tests/
```

---

## Core API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/auth/login` | Authenticate dealer and verify POS hardware authorization |
| `GET` | `/api/v1/auth/me` | Retrieve authenticated dealer profile & FPS shop info |
| `POST` | `/api/v1/cards/verify-token` | Validate HMAC signature, evaluate risk, return minimal profile |
| `POST` | `/api/v1/cards/generate-test-token` | Generate cryptographically signed test card token |
| `GET` | `/api/v1/audit/logs` | Fetch immutable audit ledger entries |
| `GET` | `/api/v1/audit/verify-integrity` | Verify cryptographic hash chain across all audit blocks |
