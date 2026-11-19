"""
Tests for L45 Scientific Argument Evaluator.

Verifies:
1. Pinned 14 chapter hashes against SOURCE.json.
2. Negative thread coverage in Chapter 12.
3. Descriptive calibration boundaries in Chapter 12.
4. Frozen PR rules and PR13 ethics gate in Chapter 13.
5. Reconciled campaign progress (19/25, 76.0%) and C5 proof in Chapter 13.
"""

from __future__ import annotations

import unittest
from pathlib import Path

from scientific_argument_evaluator import (
    PINNED_CHAPTER_HASHES,
    REQUIRED_CH12_NEGATIVE_THREADS,
    audit_chapter_12_text,
    audit_chapter_13_text,
    audit_scientific_argument,
    compute_file_sha256,
    verify_chapter_hashes,
)


REPO_ROOT = Path(__file__).resolve().parents[3]
CHAPTERS_DIR = REPO_ROOT / "evidence" / "iron-chef-lead-member-handoff-20261105" / "chapters"


class ChapterHashTests(unittest.TestCase):

    def test_pinned_chapter_count_is_14(self):
        self.assertEqual(len(PINNED_CHAPTER_HASHES), 14)

    def test_all_14_chapters_exist(self):
        for filename in PINNED_CHAPTER_HASHES:
            ch_path = CHAPTERS_DIR / filename
            self.assertTrue(ch_path.exists(), f"Missing chapter: {filename}")

    def test_all_14_chapter_hashes_match_pins(self):
        checks = verify_chapter_hashes(CHAPTERS_DIR)
        for check in checks:
            self.assertTrue(
                check.matches,
                f"Hash mismatch on {check.filename}: expected {check.expected_sha256}, got {check.actual_sha256}",
            )


class Chapter12NegativeAuditTests(unittest.TestCase):

    def setUp(self):
        ch12_path = CHAPTERS_DIR / "12-bounded-negatives.md"
        self.text = ch12_path.read_text(encoding="utf-8")
        self.audit = audit_chapter_12_text(self.text)

    def test_all_seven_negative_threads_present(self):
        self.assertTrue(self.audit.all_threads_present)
        for th in REQUIRED_CH12_NEGATIVE_THREADS:
            self.assertTrue(self.audit.thread_coverage.get(th, False), f"Missing thread: {th}")

    def test_flavor_pairing_bound_verified(self):
        self.assertTrue(self.audit.flavor_pairing_p_value_bounded)

    def test_smolvlm_withdrawal_hash_present(self):
        self.assertTrue(self.audit.smolvlm_withdrawn_recorded)

    def test_descriptive_t_hat_bounded(self):
        self.assertTrue(self.audit.descriptive_calibration_t_hat_bounded)

    def test_formal_guarantee_disabled_explicit(self):
        self.assertTrue(self.audit.formal_guarantee_disabled)


class Chapter13DiscussionAuditTests(unittest.TestCase):

    def setUp(self):
        ch13_path = CHAPTERS_DIR / "13-discussion-and-threats.md"
        self.text = ch13_path.read_text(encoding="utf-8")
        self.audit = audit_chapter_13_text(self.text)

    def test_pr_freeze_acknowledged(self):
        self.assertTrue(self.audit.pr_freeze_acknowledged)

    def test_pr13_ethics_gate_preserved(self):
        self.assertTrue(self.audit.pr13_ethics_gate_preserved)

    def test_l36_operational_mapping_acknowledged(self):
        self.assertTrue(self.audit.l36_operational_mapping_acknowledged)

    def test_campaign_progress_reconciled(self):
        self.assertTrue(self.audit.campaign_progress_reconciled)

    def test_c5_clean_graph_metrics_verified(self):
        self.assertTrue(self.audit.c5_clean_graph_metrics_verified)


class FullScientificArgumentAuditTests(unittest.TestCase):

    def test_full_audit_passes_clean(self):
        result = audit_scientific_argument(CHAPTERS_DIR)
        self.assertEqual(result.overall_status, "PASS")
        self.assertEqual(len(result.discrepancies), 0)
        self.assertTrue(result.all_14_chapters_intact)


if __name__ == "__main__":
    unittest.main()
