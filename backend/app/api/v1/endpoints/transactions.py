from fastapi import APIRouter, Depends
from app.models.schemas import (
    ReconcileBatchRequest,
    ReconcileBatchResponse,
    TokenData
)
from app.services.auth_service import get_current_dealer
from app.services.reconciliation_service import reconciliation_service

router = APIRouter()

@router.post(
    "/reconcile",
    response_model=ReconcileBatchResponse,
    summary="Reconcile Queued Offline Transactions (Atomic & Idempotent)"
)
def reconcile_transactions(
    request: ReconcileBatchRequest,
    current_dealer: TokenData = Depends(get_current_dealer)
):
    """
    Offline Transaction Reconciliation Gateway:
    
    1. Guarantees **Atomicity**: Processes the batch within an isolated transaction boundary.
    2. Guarantees **Idempotency**: Dedupes retried transactions using client-generated idempotency keys.
    3. Multi-shop Conflict Detection: Flags double-dip attempts where the same quota was claimed at another FPS.
    4. Tamper-Evident Audit: Records reconciliation events into the central append-only SHA-256 hash ledger.
    """
    # Enforce authenticated dealer credentials if not specified in request body
    if not request.dealer_id:
        request.dealer_id = current_dealer.dealer_id
    if not request.pos_device_id:
        request.pos_device_id = current_dealer.pos_device_id

    return reconciliation_service.reconcile_batch(request)
