"""Unit tests for L48 Claims Register Adjudication & Evidence Ceiling Evaluator."""
from pathlib import Path
import unittest

from tools.research.l48.claims_adjudication_evaluator import (
    CLAIMS_REGISTER_V20_SHA256,
    DEFENSE_DOSSIER_SHA256,
    EXPECTED_IDS,
    EXPECTED_OUTCOME_COUNTS,
    EXPECTED_STATUS_COUNTS,
    PROTOCOL_SHA256,
    TOTAL_CLAIMS,
    ClaimRecord,
    audit_claim_evidence_ceiling,
    compute_file_sha256,
    parse_claims_register_csv,
    validate_claims_adjudication,
    verify_claim_promotion_request,
)


class TestClaimsAdjudicationConstants(unittest.TestCase):
    def test_pinned_hashes(self):
        self.assertEqual(CLAIMS_REGISTER_V20_SHA256, "72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023")
        self.assertEqual(DEFENSE_DOSSIER_SHA256, "7da53af33901328cf9974378eebba85bc2d966338d062b33d80a58ed515aa8e2")
        self.assertEqual(PROTOCOL_SHA256, "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12")

    def test_expected_counts_sum(self):
        self.assertEqual(TOTAL_CLAIMS, 45)
        self.assertEqual(len(EXPECTED_IDS), 45)
        self.assertEqual(sum(EXPECTED_STATUS_COUNTS.values()), 45)
        self.assertEqual(sum(EXPECTED_OUTCOME_COUNTS.values()), 45)


class TestRealClaimsRegisterV20(unittest.TestCase):
    def setUp(self):
        self.v20_path = Path("evidence/iron-chef-lead-member-handoff-20261105/claims/claims-register-r01-r45-v20.csv")

    def test_actual_file_hash_matches_ssot_pin(self):
        if not self.v20_path.exists():
            self.skipTest(f"File {self.v20_path} not found in test environment")
        computed_sha = compute_file_sha256(self.v20_path)
        self.assertEqual(computed_sha, CLAIMS_REGISTER_V20_SHA256)

    def test_actual_v20_validation_passes_100_percent(self):
        if not self.v20_path.exists():
            self.skipTest(f"File {self.v20_path} not found in test environment")
        records = parse_claims_register_csv(self.v20_path)
        computed_sha = compute_file_sha256(self.v20_path)
        report = validate_claims_adjudication(records, register_sha256=computed_sha)

        self.assertTrue(report.is_hash_verified)
        self.assertTrue(report.is_adjudication_accepted)
        self.assertEqual(len(report.violations), 0)
        self.assertEqual(report.total_claims, 45)
        self.assertEqual(report.status_counts, EXPECTED_STATUS_COUNTS)
        self.assertEqual(report.outcome_counts, EXPECTED_OUTCOME_COUNTS)

        expected_verified = ["R04", "R05", "R09", "R11", "R12", "R13", "R14", "R18", "R19", "R38"]
        expected_partial = ["R06", "R10", "R26", "R39", "R40", "R41", "R42"]
        self.assertEqual(report.verified_claims, expected_verified)
        self.assertEqual(report.partial_claims, expected_partial)
        self.assertEqual(len(report.unverified_claims), 28)


class TestEvidenceCeilingAndAdjudicationRules(unittest.TestCase):
    def make_dummy_record(self, cid: str, status: str = "UNVERIFIED", outcome: str = "NA") -> ClaimRecord:
        return ClaimRecord(
            id=cid,
            claim=f"Claim {cid}",
            artifact_path="evidence/dummy.txt",
            sha256="a" * 64,
            evidence_status=status,
            outcome=outcome,
            source_provenance="LOCAL",
            last_checked="2026-11-09",
            notes="Dummy note",
            l_item_mapping="L48",
            citation="None",
            reproduction_path="tools/rep.py" if status == "VERIFIED" else "",
            reproduction_sha256="b" * 64 if status == "VERIFIED" else "",
            reproduced_value="1.0" if status == "VERIFIED" else "",
        )

    def test_verified_missing_reproduction_path_fails(self):
        rec = ClaimRecord(
            id="R01",
            claim="Test",
            artifact_path="evidence/test.txt",
            sha256="a" * 64,
            evidence_status="VERIFIED",
            outcome="REPRODUCTION",
            source_provenance="LOCAL",
            last_checked="2026-11-09",
            notes="Notes",
            l_item_mapping="L48",
            citation="",
            reproduction_path="",  # MISSING!
            reproduction_sha256="b" * 64,
            reproduced_value="val",
        )
        violations = audit_claim_evidence_ceiling(rec)
        self.assertTrue(any(v.rule_violation == "VERIFIED_MISSING_REPRODUCTION_PATH" for v in violations))

    def test_verified_invalid_sha256_fails(self):
        rec = ClaimRecord(
            id="R01",
            claim="Test",
            artifact_path="evidence/test.txt",
            sha256="a" * 64,
            evidence_status="VERIFIED",
            outcome="REPRODUCTION",
            source_provenance="LOCAL",
            last_checked="2026-11-09",
            notes="Notes",
            l_item_mapping="L48",
            citation="",
            reproduction_path="path/to/rep.py",
            reproduction_sha256="invalid_hex_length",  # INVALID!
            reproduced_value="val",
        )
        violations = audit_claim_evidence_ceiling(rec)
        self.assertTrue(any(v.rule_violation == "VERIFIED_INVALID_REPRODUCTION_SHA256" for v in violations))

    def test_refuted_priority_claims_r28_r29_cannot_be_upgraded(self):
        # Attempt to upgrade R28 to VERIFIED
        rec_r28 = ClaimRecord(
            id="R28",
            claim="First substitution system",
            artifact_path="evidence/test.txt",
            sha256="a" * 64,
            evidence_status="VERIFIED",  # FORBIDDEN!
            outcome="POSITIVE",
            source_provenance="LOCAL",
            last_checked="2026-11-09",
            notes="Notes",
            l_item_mapping="L42",
            citation="",
            reproduction_path="path.py",
            reproduction_sha256="b" * 64,
            reproduced_value="",
        )
        violations = audit_claim_evidence_ceiling(rec_r28)
        self.assertTrue(any(v.rule_violation == "REFUTED_PRIORITY_CLAIM_UPGRADE_FORBIDDEN" for v in violations))

    def test_r32_cannot_be_verified_under_protocol_v1_candidate_gate(self):
        rec_r32 = ClaimRecord(
            id="R32",
            claim="Candidate pool recall",
            artifact_path="evidence/test.txt",
            sha256="a" * 64,
            evidence_status="VERIFIED",  # Gated under Protocol v1
            outcome="POSITIVE",
            source_provenance="LOCAL",
            last_checked="2026-11-09",
            notes="Notes",
            l_item_mapping="L38",
            citation="",
            reproduction_path="path.py",
            reproduction_sha256="b" * 64,
            reproduced_value="",
        )
        violations = audit_claim_evidence_ceiling(rec_r32)
        self.assertTrue(any(v.rule_violation == "R32_PROTOCOL_V1_FROZEN_CANDIDATE_GATE" for v in violations))

    def test_missing_l_item_mapping_fails(self):
        rec = ClaimRecord(
            id="R03",
            claim="Test",
            artifact_path="evidence/test.txt",
            sha256="a" * 64,
            evidence_status="UNVERIFIED",
            outcome="NA",
            source_provenance="LOCAL",
            last_checked="2026-11-09",
            notes="Notes",
            l_item_mapping="",  # MISSING!
            citation="",
            reproduction_path="",
            reproduction_sha256="",
            reproduced_value="",
        )
        violations = audit_claim_evidence_ceiling(rec)
        self.assertTrue(any(v.rule_violation == "MISSING_L_ITEM_MAPPING" for v in violations))


class TestClaimPromotionGating(unittest.TestCase):
    def test_promotion_rejections(self):
        records = [
            ClaimRecord(
                id=cid,
                claim=f"Claim {cid}",
                artifact_path="",
                sha256="a" * 64,
                evidence_status="UNVERIFIED",
                outcome="NA",
                source_provenance="",
                last_checked="",
                notes="",
                l_item_mapping="L48",
                citation="",
                reproduction_path="",
                reproduction_sha256="",
                reproduced_value="",
            )
            for cid in EXPECTED_IDS
        ]

        # Locked claim R28
        ok, msg = verify_claim_promotion_request(records, "R28", "VERIFIED", "path.py", "c" * 64)
        self.assertFalse(ok)
        self.assertIn("permanently locked", msg)

        # Missing reproduction path
        ok, msg = verify_claim_promotion_request(records, "R01", "VERIFIED", "", "c" * 64)
        self.assertFalse(ok)
        self.assertIn("requires non-empty reproduction_path", msg)

        # Invalid hex SHA
        ok, msg = verify_claim_promotion_request(records, "R01", "VERIFIED", "path.py", "bad_hex")
        self.assertFalse(ok)
        self.assertIn("requires valid 64-char hex", msg)

        # Valid promotion request
        ok, msg = verify_claim_promotion_request(records, "R01", "VERIFIED", "path.py", "c" * 64)
        self.assertTrue(ok)
        self.assertIn("structurally admissible", msg)


if __name__ == "__main__":
    unittest.main()
