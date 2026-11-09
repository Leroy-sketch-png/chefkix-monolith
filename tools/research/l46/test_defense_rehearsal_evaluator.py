"""Unit tests for L46 Defense Rehearsal Pack & Hostile Committee Challenge Evaluator."""
from pathlib import Path
import unittest

from tools.research.l46.defense_rehearsal_evaluator import (
    CLEAN_GRAPH_MANIFEST_SHA256,
    DEFENSE_DOSSIER_SHA256,
    MAX_TOTAL_POINTS,
    PASSING_FLOOR,
    PROTOCOL_SHA256,
    REQUIRED_CHALLENGES,
    REQUIRED_FIELDS,
    RUBRIC_CATEGORIES,
    compute_file_sha256,
    evaluate_rubric,
    parse_defense_pack,
    scan_forbidden_claims,
    validate_defense_rehearsal_pack,
)


class TestDefensePackConstants(unittest.TestCase):
    def test_pinned_hashes(self):
        self.assertEqual(DEFENSE_DOSSIER_SHA256, "7da53af33901328cf9974378eebba85bc2d966338d062b33d80a58ed515aa8e2")
        self.assertEqual(PROTOCOL_SHA256, "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12")
        self.assertEqual(CLEAN_GRAPH_MANIFEST_SHA256, "ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1")

    def test_rubric_and_challenges_count(self):
        self.assertEqual(len(REQUIRED_CHALLENGES), 28)
        self.assertEqual(len(REQUIRED_FIELDS), 7)
        self.assertEqual(len(RUBRIC_CATEGORIES), 8)
        self.assertEqual(PASSING_FLOOR, 42)
        self.assertEqual(MAX_TOTAL_POINTS, 48)


class TestRealDefensePack(unittest.TestCase):
    def setUp(self):
        self.pack_path = Path("evidence/iron-chef-lead-member-handoff-20261105/defense/iron-chef-final-defense-rehearsal-pack.md")

    def test_real_defense_pack_hash(self):
        if not self.pack_path.exists():
            self.skipTest(f"Pack file {self.pack_path} not found in test environment")
        actual_sha = compute_file_sha256(self.pack_path)
        self.assertEqual(actual_sha, DEFENSE_DOSSIER_SHA256)

    def test_real_defense_pack_all_28_challenges_valid(self):
        if not self.pack_path.exists():
            self.skipTest(f"Pack file {self.pack_path} not found in test environment")
        report = validate_defense_rehearsal_pack(self.pack_path)
        self.assertTrue(report.is_hash_verified)
        self.assertTrue(report.is_accepted)
        self.assertEqual(report.challenges_count, 28)
        self.assertEqual(report.valid_challenges_count, 28)
        self.assertEqual(len(report.forbidden_claim_violations), 0)
        self.assertEqual(report.oral_rehearsal_status, "PACK_VALIDATED_ORAL_REHEARSAL_PENDING")


class TestRubricScoring(unittest.TestCase):
    def test_perfect_score_passes(self):
        scores = {cat: 6 for cat in RUBRIC_CATEGORIES}
        card = evaluate_rubric(scores)
        self.assertEqual(card.total_score, 48)
        self.assertTrue(card.passes_floor)
        self.assertFalse(card.is_disqualified)

    def test_floor_42_passes(self):
        # 6 categories at 5, 2 categories at 6 = 30 + 12 = 42
        scores = {"C1": 5, "C2": 5, "C3": 5, "C4": 5, "C5": 5, "C6": 5, "C7": 6, "C8": 6}
        card = evaluate_rubric(scores)
        self.assertEqual(card.total_score, 42)
        self.assertTrue(card.passes_floor)
        self.assertFalse(card.is_disqualified)

    def test_below_floor_41_fails(self):
        scores = {"C1": 5, "C2": 5, "C3": 5, "C4": 5, "C5": 5, "C6": 5, "C7": 5, "C8": 6}  # 41
        card = evaluate_rubric(scores)
        self.assertEqual(card.total_score, 41)
        self.assertFalse(card.passes_floor)

    def test_critical_zero_disqualification(self):
        # High total score (e.g. 42), but C3 has 0 points
        scores = {"C1": 6, "C2": 6, "C3": 0, "C4": 6, "C5": 6, "C6": 6, "C7": 6, "C8": 6}  # 42 total
        card = evaluate_rubric(scores)
        self.assertEqual(card.total_score, 42)
        self.assertTrue(card.is_disqualified)
        self.assertFalse(card.passes_floor)
        self.assertIn("Critical Category C3", card.disqualification_reason)


class TestForbiddenClaimsScanner(unittest.TestCase):
    def test_detects_prohibited_claims(self):
        bad_text = "Our system is clinically safe and offers 100% safe food swaps."
        violations = scan_forbidden_claims(bad_text)
        self.assertTrue(len(violations) >= 2)
        self.assertTrue(any("clinical safety" in v.lower() for v in violations))
        self.assertTrue(any("absolute safety" in v.lower() for v in violations))

    def test_ignores_negated_boundary_mentions(self):
        clean_text = "Never claim our system is clinically safe; we do not claim 100% safe swaps."
        violations = scan_forbidden_claims(clean_text)
        self.assertEqual(len(violations), 0)


class TestOralRehearsalStatus(unittest.TestCase):
    def test_rehearsal_verified_pass(self):
        scores = {cat: 6 for cat in RUBRIC_CATEGORIES}
        report = validate_defense_rehearsal_pack(
            dossier_path_or_content="dummy",
            observed_rubric_scores=scores,
            observed_timed_rehearsal=True,
        )
        self.assertEqual(report.oral_rehearsal_status, "REHEARSAL_VERIFIED_PASS")

    def test_rehearsal_verified_fail(self):
        scores = {cat: 1 for cat in RUBRIC_CATEGORIES}
        report = validate_defense_rehearsal_pack(
            dossier_path_or_content="dummy",
            observed_rubric_scores=scores,
            observed_timed_rehearsal=True,
        )
        self.assertEqual(report.oral_rehearsal_status, "REHEARSAL_VERIFIED_FAIL")


if __name__ == "__main__":
    unittest.main()
