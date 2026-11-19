"""Unit tests for L41 Institutional, Permission, and Governance Evaluator."""
from __future__ import annotations

import unittest

import sys
from pathlib import Path

# Add monolith root to sys.path
monolith_root = Path(__file__).resolve().parents[3]
if str(monolith_root) not in sys.path:
    sys.path.insert(0, str(monolith_root))

try:
    from tools.research.l41.institutional_governance_evaluator import (
        EpistemicAttestationStatus,
        GateDisposition,
        KNOWN_SOURCES,
        OperationScope,
        audit_governance_posture,
        evaluate_operation,
    )
except ModuleNotFoundError:
    from institutional_governance_evaluator import (
        EpistemicAttestationStatus,
        GateDisposition,
        KNOWN_SOURCES,
        OperationScope,
        audit_governance_posture,
        evaluate_operation,
    )


class TestInstitutionalGovernanceEvaluator(unittest.TestCase):
    def test_internal_artifact_read_permitted_under_standing_authority(self):
        res = evaluate_operation(OperationScope.INTERNAL_ARTIFACT_READ)
        self.assertTrue(res.allowed)
        self.assertEqual(res.disposition, GateDisposition.PERMITTED_STANDING_AUTHORITY)

    def test_confidential_store_strictly_prohibited(self):
        res = evaluate_operation(OperationScope.INTERNAL_CONFIDENTIAL_STORE)
        self.assertFalse(res.allowed)
        self.assertEqual(res.disposition, GateDisposition.PROHIBITED_CONFIDENTIAL_STORE)

    def test_confidential_store_flag_blocks_any_op(self):
        res = evaluate_operation(OperationScope.INTERNAL_ARTIFACT_READ, is_confidential_store=True)
        self.assertFalse(res.allowed)
        self.assertEqual(res.disposition, GateDisposition.PROHIBITED_CONFIDENTIAL_STORE)

    def test_external_spending_blocked_without_explicit_auth(self):
        res = evaluate_operation(OperationScope.EXTERNAL_SPENDING)
        self.assertFalse(res.allowed)
        self.assertEqual(res.disposition, GateDisposition.BLOCKED_REQUIRING_EXPLICIT_AUTHORIZATION)

    def test_external_spending_allowed_with_explicit_auth(self):
        res = evaluate_operation(OperationScope.EXTERNAL_SPENDING, has_explicit_user_action_auth=True)
        self.assertTrue(res.allowed)
        self.assertEqual(res.disposition, GateDisposition.PERMITTED_STANDING_AUTHORITY)

    def test_external_publication_blocked_without_explicit_auth(self):
        res = evaluate_operation(OperationScope.EXTERNAL_PUBLICATION)
        self.assertFalse(res.allowed)
        self.assertEqual(res.disposition, GateDisposition.BLOCKED_REQUIRING_EXPLICIT_AUTHORIZATION)

    def test_human_participant_contact_requires_documentary_clearance(self):
        res = evaluate_operation(OperationScope.HUMAN_PARTICIPANT_CONTACT)
        self.assertFalse(res.allowed)
        self.assertEqual(res.disposition, GateDisposition.BLOCKED_REQUIRING_DOCUMENTARY_CLEARANCE)

    def test_attestation_status_classification(self):
        # Case 1: Attestation only (default)
        res1 = audit_governance_posture(human_attestation_present=True, independent_document_inspected=False)
        self.assertEqual(res1.ethics_attestation_status, EpistemicAttestationStatus.ATTESTED_HUMAN_DECLARATION)
        self.assertTrue(any("HUMAN attestation" in w for w in res1.warnings))

        # Case 2: Document inspected
        res2 = audit_governance_posture(human_attestation_present=True, independent_document_inspected=True)
        self.assertEqual(res2.ethics_attestation_status, EpistemicAttestationStatus.INDEPENDENTLY_DOCUMENTARY_VERIFIED)

        # Case 3: Missing
        res3 = audit_governance_posture(human_attestation_present=False, independent_document_inspected=False)
        self.assertEqual(res3.ethics_attestation_status, EpistemicAttestationStatus.UNVERIFIED_OR_MISSING)

    def test_known_sources_invariants(self):
        sources = {s.source_name: s for s in KNOWN_SOURCES}
        self.assertIn("Recipe1M / Recipe1MSubs", sources)
        self.assertIn("FooDB 1.0", sources)
        self.assertIn("WHO/IUIS Allergen Nomenclature", sources)

        # None of them establish clinical safety
        for s in KNOWN_SOURCES:
            self.assertFalse(s.clinical_safety_established)

        # Recipe1M does not allow public redistribution
        self.assertFalse(sources["Recipe1M / Recipe1MSubs"].permitted_public_redistribution)
        # FooDB is non-commercial
        self.assertFalse(sources["FooDB 1.0"].commercial_use_permitted)

    def test_lead_status_remains_open(self):
        audit = audit_governance_posture()
        self.assertEqual(audit.lead_status, "OPEN")


if __name__ == "__main__":
    unittest.main()
