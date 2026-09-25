import time
from typing import Dict, Optional, List
from app.core.security import verify_smart_card_token
from app.core.audit import audit_ledger
from app.models.domain import Beneficiary, CardStatus, CardType, RiskLevel, AuthMethodType, CommodityQuota
from app.models.schemas import (
    VerifyCardTokenRequest,
    VerifyCardTokenResponse,
    BeneficiaryMinResponse,
    RiskAssessmentResponse,
    RiskFactorResponse
)

# Central Beneficiary Registry Database
BENEFICIARY_STORE: Dict[str, Beneficiary] = {
    "SRC-DL-2026-99214": Beneficiary(
        card_id="SRC-DL-2026-99214",
        masked_card_number="XXXX-XXXX-2847",
        head_of_family_name="Ramesh Kumar",
        card_type=CardType.PHH,
        family_members_count=4,
        state="NCT of Delhi",
        district="Central Delhi",
        registered_fps_id="FPS-110001-084",
        card_status=CardStatus.ACTIVE,
        reference_face_hash="FACE_HASH_RAMESH_KUMAR_2847",
        registered_mobile_masked="******9821",
        last_distribution_date="01-08-2026",
        next_eligible_date="01-09-2026",
        entitlements=[
            CommodityQuota(commodity_id="RICE", commodity_name="Fortified Rice", unit="kg", monthly_entitlement=20.0, already_collected_month=5.0, price_per_unit=0.0),
            CommodityQuota(commodity_id="WHEAT", commodity_name="Whole Wheat Grain", unit="kg", monthly_entitlement=5.0, already_collected_month=0.0, price_per_unit=0.0),
            CommodityQuota(commodity_id="SUGAR", commodity_name="Subsidized Sugar", unit="kg", monthly_entitlement=2.0, already_collected_month=0.0, price_per_unit=13.50),
        ]
    ),
    "SRC-DL-2026-88102": Beneficiary(
        card_id="SRC-DL-2026-88102",
        masked_card_number="XXXX-XXXX-5519",
        head_of_family_name="Sunita Devi",
        card_type=CardType.AAY,
        family_members_count=5,
        state="NCT of Delhi",
        district="Central Delhi",
        registered_fps_id="FPS-110001-084",
        card_status=CardStatus.ACTIVE,
        reference_face_hash="FACE_HASH_SUNITA_DEVI_5519",
        registered_mobile_masked="******4412",
        last_distribution_date="28-07-2026",
        next_eligible_date="01-09-2026",
        entitlements=[
            CommodityQuota(commodity_id="RICE", commodity_name="Fortified Rice", unit="kg", monthly_entitlement=25.0, already_collected_month=0.0, price_per_unit=0.0),
            CommodityQuota(commodity_id="WHEAT", commodity_name="Whole Wheat Grain", unit="kg", monthly_entitlement=10.0, already_collected_month=0.0, price_per_unit=0.0),
            CommodityQuota(commodity_id="SUGAR", commodity_name="Subsidized Sugar", unit="kg", monthly_entitlement=1.0, already_collected_month=0.0, price_per_unit=13.50),
            CommodityQuota(commodity_id="OIL", commodity_name="Mustard Oil", unit="L", monthly_entitlement=1.0, already_collected_month=0.0, price_per_unit=65.0)
        ]
    ),
    "SRC-GJ-2026-14029": Beneficiary(
        card_id="SRC-GJ-2026-14029",
        masked_card_number="XXXX-XXXX-9103",
        head_of_family_name="Rajesh Patel (ONORC Portability)",
        card_type=CardType.PHH,
        family_members_count=3,
        state="Gujarat",
        district="Ahmedabad",
        registered_fps_id="FPS-380001-012",
        card_status=CardStatus.ACTIVE,
        reference_face_hash="FACE_HASH_RAJESH_PATEL_9103",
        registered_mobile_masked="******7723",
        last_distribution_date="15-07-2026",
        next_eligible_date="01-09-2026",
        entitlements=[
            CommodityQuota(commodity_id="RICE", commodity_name="Fortified Rice", unit="kg", monthly_entitlement=15.0, already_collected_month=0.0, price_per_unit=0.0),
            CommodityQuota(commodity_id="WHEAT", commodity_name="Whole Wheat Grain", unit="kg", monthly_entitlement=10.0, already_collected_month=0.0, price_per_unit=0.0)
        ]
    ),
    "SRC-UP-2026-77312": Beneficiary(
        card_id="SRC-UP-2026-77312",
        masked_card_number="XXXX-XXXX-4420",
        head_of_family_name="Mohammed Ansari",
        card_type=CardType.PHH,
        family_members_count=4,
        state="Uttar Pradesh",
        district="Ghaziabad",
        registered_fps_id="FPS-201001-045",
        card_status=CardStatus.BLOCKED,
        reference_face_hash="FACE_HASH_MOHAMMED_ANSARI_4420",
        registered_mobile_masked="******3301",
        last_distribution_date="10-06-2026",
        next_eligible_date="N/A (LOCKED)",
        entitlements=[]
    ),
    "SRC-DL-2026-33901": Beneficiary(
        card_id="SRC-DL-2026-33901",
        masked_card_number="XXXX-XXXX-1188",
        head_of_family_name="Priya Sharma",
        card_type=CardType.PHH,
        family_members_count=2,
        state="NCT of Delhi",
        district="Central Delhi",
        registered_fps_id="FPS-110001-084",
        card_status=CardStatus.LOST,
        reference_face_hash="FACE_HASH_PRIYA_SHARMA_1188",
        registered_mobile_masked="******8819",
        last_distribution_date="05-07-2026",
        next_eligible_date="REISSUE_PENDING",
        entitlements=[]
    )
}

class CardService:
    @staticmethod
    def verify_card(
        request: VerifyCardTokenRequest,
        dealer_id: str = "DL-DEL-0492",
        pos_device_id: str = "POS-DEV-IND-8841",
        fps_id: str = "FPS-110001-084"
    ) -> VerifyCardTokenResponse:
        start_time = time.time()

        # Step 1: Cryptographic Token Signature Validation
        is_valid, error_code, parsed_data = verify_smart_card_token(request.token_payload)
        if not is_valid or not parsed_data:
            audit_ledger.record_event(
                dealer_id=dealer_id,
                device_id=pos_device_id,
                card_id="UNKNOWN",
                action="CARD_TOKEN_VALIDATION",
                result=f"FAILED: {error_code}",
                risk_level="CRITICAL",
                details="Invalid digital signature or expired nonce in token payload."
            )
            latency = round((time.time() - start_time) * 1000, 2)
            return VerifyCardTokenResponse(
                is_valid=False,
                status="REJECTED",
                error_code=error_code,
                error_message="Cryptographic verification failed. Token is counterfeit, modified, or expired.",
                latency_ms=latency
            )

        card_id = parsed_data["card_id"]

        # Step 2: Beneficiary Lookup in PDS Registry
        beneficiary = BENEFICIARY_STORE.get(card_id)
        if not beneficiary:
            audit_ledger.record_event(
                dealer_id=dealer_id,
                device_id=pos_device_id,
                card_id=card_id,
                action="BENEFICIARY_LOOKUP",
                result="CARD_NOT_FOUND",
                risk_level="HIGH",
                details="Card ID is cryptographically valid but absent from State PDS database."
            )
            latency = round((time.time() - start_time) * 1000, 2)
            return VerifyCardTokenResponse(
                is_valid=False,
                status="NOT_FOUND",
                error_code="CARD_NOT_REGISTERED",
                error_message="Card record not found in National Food Security Registry.",
                latency_ms=latency
            )

        # Step 3: Card Status Check
        if beneficiary.card_status in [CardStatus.BLOCKED, CardStatus.SUSPENDED, CardStatus.LOST]:
            audit_ledger.record_event(
                dealer_id=dealer_id,
                device_id=pos_device_id,
                card_id=card_id,
                action="CARD_STATUS_CHECK",
                result=f"CARD_STATUS_{beneficiary.card_status}",
                risk_level="CRITICAL",
                details=f"Access blocked. Card status is {beneficiary.card_status}."
            )
            latency = round((time.time() - start_time) * 1000, 2)
            return VerifyCardTokenResponse(
                is_valid=False,
                status="BLOCKED",
                error_code=f"CARD_{beneficiary.card_status}",
                error_message=f"Ration Card status is {beneficiary.card_status}. Transaction rejected by regulatory rules.",
                latency_ms=latency
            )

        # Step 4: Adaptive Risk Assessment
        factors: List[RiskFactorResponse] = []
        score = 0

        # Portability (ONORC check)
        if beneficiary.registered_fps_id != fps_id:
            inter_state = beneficiary.state != "NCT of Delhi"
            port_score = 25 if inter_state else 10
            factors.append(RiskFactorResponse(
                factor_name="ONORC_PORTABILITY_VISIT",
                score_contribution=port_score,
                description=f"Beneficiary visiting under One Nation One Ration Card from {beneficiary.state}.",
                is_anomaly=False
            ))
            score += port_score
        else:
            factors.append(RiskFactorResponse(
                factor_name="HOME_FPS_SHOP",
                score_contribution=0,
                description="Transaction at beneficiary's registered Fair Price Shop.",
                is_anomaly=False
            ))

        # Check simulation override
        if request.simulate_risk_score is not None:
            score = request.simulate_risk_score
            factors = [RiskFactorResponse(
                factor_name="SECURITY_SIMULATION_OVERRIDE",
                score_contribution=score,
                description=f"Simulation test mode score set to {score}.",
                is_anomaly=(score > 25)
            )]

        final_score = min(max(score, 0), 100)
        level = RiskLevel.LOW if final_score < 25 else (RiskLevel.MEDIUM if final_score < 60 else RiskLevel.HIGH)

        next_auth = [AuthMethodType.QR_CRYPTOGRAPHIC]
        if level in [RiskLevel.MEDIUM, RiskLevel.HIGH]:
            next_auth.extend([AuthMethodType.FACE_BIOMETRIC, AuthMethodType.AADHAAR_OTP])

        risk_assessment = RiskAssessmentResponse(
            total_score=final_score,
            level=level,
            factors=factors,
            required_next_auth=next_auth,
            is_transaction_permitted=True
        )

        # Step 5: Data Minimization (Privacy-by-Design)
        # Excludes sensitive PII (never returns unmasked Aadhaar, bank details, or address)
        beneficiary_resp = BeneficiaryMinResponse(
            card_id=beneficiary.card_id,
            masked_card_number=beneficiary.masked_card_number,
            head_of_family_name=beneficiary.head_of_family_name,
            card_type=beneficiary.card_type,
            family_members_count=beneficiary.family_members_count,
            state=beneficiary.state,
            district=beneficiary.district,
            card_status=beneficiary.card_status,
            last_distribution_date=beneficiary.last_distribution_date,
            next_eligible_date=beneficiary.next_eligible_date,
            entitlements=beneficiary.entitlements,
            registered_mobile_masked=beneficiary.registered_mobile_masked,
            reference_face_hash=beneficiary.reference_face_hash
        )

        latency = round((time.time() - start_time) * 1000, 2)

        audit_ledger.record_event(
            dealer_id=dealer_id,
            device_id=pos_device_id,
            card_id=card_id,
            action="BENEFICIARY_VERIFIED",
            result="SUCCESS",
            risk_level=level.value,
            details=f"Token signature verified. Risk score: {final_score}/100 in {latency}ms."
        )

        return VerifyCardTokenResponse(
            is_valid=True,
            status="VERIFIED",
            beneficiary=beneficiary_resp,
            risk_assessment=risk_assessment,
            latency_ms=latency
        )
