from fastapi import APIRouter
from app.core.audit import audit_ledger

router = APIRouter()

@router.get("/logs", summary="Get Immutable Audit Logs")
def get_audit_logs(limit: int = 50):
    """
    Returns latest audit logs with SHA-256 hash chaining.
    """
    return {
        "count": len(audit_ledger.get_logs(limit)),
        "logs": audit_ledger.get_logs(limit)
    }

@router.get("/verify-integrity", summary="Verify Cryptographic Hash-Chain Integrity")
def verify_audit_integrity():
    """
    Verifies that all blocks in the audit ledger are linked via valid SHA-256 hashes
    and that no records have been altered or deleted.
    """
    return audit_ledger.verify_chain_integrity()
