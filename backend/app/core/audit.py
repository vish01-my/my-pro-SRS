import hashlib
import time
from typing import List, Dict, Any

class TamperEvidentAuditLedger:
    def __init__(self):
        self._chain: List[Dict[str, Any]] = []
        self._genesis_hash = "0" * 64
        # Seed Genesis block
        self.record_event(
            dealer_id="SYSTEM",
            device_id="CENTRAL_PDS_SRV",
            card_id="GENESIS",
            action="LEDGER_INITIALIZED",
            result="SUCCESS",
            risk_level="LOW",
            details="Central e-PDS Audit Ledger initialized with SHA-256 tamper-evident hash chaining."
        )

    def record_event(
        self,
        dealer_id: str,
        device_id: str,
        card_id: str,
        action: str,
        result: str,
        risk_level: str,
        details: str
    ) -> Dict[str, Any]:
        prev_hash = self._chain[-1]["current_hash"] if self._chain else self._genesis_hash
        event_id = f"AUD-{int(time.time() * 1000)}"
        timestamp = int(time.time() * 1000)
        
        # Calculate SHA-256 over linked fields
        payload = f"{prev_hash}:{event_id}:{timestamp}:{dealer_id}:{device_id}:{card_id}:{action}:{result}:{details}"
        current_hash = hashlib.sha256(payload.encode("utf-8")).hexdigest()
        
        block = {
            "index": len(self._chain),
            "event_id": event_id,
            "timestamp": timestamp,
            "dealer_id": dealer_id,
            "device_id": device_id,
            "card_id": card_id,
            "action": action,
            "result": result,
            "risk_level": risk_level,
            "previous_hash": prev_hash,
            "current_hash": current_hash,
            "details": details
        }
        self._chain.append(block)
        return block

    def verify_chain_integrity(self) -> Dict[str, Any]:
        if not self._chain:
            return {"valid": True, "total_blocks": 0, "message": "Empty ledger"}
        
        for i in range(1, len(self._chain)):
            prev = self._chain[i - 1]
            curr = self._chain[i]
            
            # Check previous hash pointer
            if curr["previous_hash"] != prev["current_hash"]:
                return {
                    "valid": False,
                    "corrupted_block_index": i,
                    "message": f"Hash chain broken at index {i}. PrevHash pointer mismatch."
                }
            
            # Recalculate and verify hash
            payload = f"{curr['previous_hash']}:{curr['event_id']}:{curr['timestamp']}:{curr['dealer_id']}:{curr['device_id']}:{curr['card_id']}:{curr['action']}:{curr['result']}:{curr['details']}"
            recomputed = hashlib.sha256(payload.encode("utf-8")).hexdigest()
            if recomputed != curr["current_hash"]:
                return {
                    "valid": False,
                    "corrupted_block_index": i,
                    "message": f"Block content tampered at index {i}. Hash verification failed."
                }
        
        return {
            "valid": True,
            "total_blocks": len(self._chain),
            "latest_hash": self._chain[-1]["current_hash"],
            "message": "All audit blocks cryptographically linked and untampered."
        }

    def get_logs(self, limit: int = 50) -> List[Dict[str, Any]]:
        return list(reversed(self._chain))[:limit]

# Singleton instance for backend runtime
audit_ledger = TamperEvidentAuditLedger()
