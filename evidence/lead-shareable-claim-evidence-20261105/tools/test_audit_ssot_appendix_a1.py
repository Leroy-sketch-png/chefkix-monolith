#!/usr/bin/env python3
"""Tests for audit_ssot_appendix_a1.py.

Each defect check is exercised twice: once against the committed listing (must be
CONFIRMED_IN_SOURCE) and once against a corrected listing (must be NOT_FOUND), so a
silent source fix cannot leave the audit green.
"""

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "ai/tools"))

from audit_ssot_appendix_a1 import (
    CANONICAL_REGISTER_COLUMN,
    SELFTEST_SENTINEL,
    audit,
    extract_listing,
    run_selftest,
)

SSOT = ROOT / "academic_vision_and_strategy.md"
REGISTER = ROOT / "ai/evidence/claims-register/instantiated-r01-r45-v1/claims-register-r01-r45-v19.csv"


def _by_code(result, code):
    for defect in result["defects"]:
        if defect["code"] == code:
            return defect
    raise AssertionError("missing defect check %s" % code)


class ExtractListingTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.document = SSOT.read_text(encoding="utf-8")
        cls.source, cls.first, cls.last = extract_listing(cls.document)

    def test_extracts_python_not_markdown(self):
        self.assertTrue(self.source.lstrip().startswith("#!/usr/bin/env python3"))
        self.assertNotIn("```", self.source)
        self.assertNotIn(SELFTEST_SENTINEL, self.source[:200])

    def test_line_range_matches_document(self):
        lines = self.document.split("\n")
        self.assertEqual(lines[self.first - 1].lstrip().startswith("#!/usr/bin/env python3"), True)
        # end_line is the last listing line; the fence itself follows it.
        self.assertEqual(lines[self.last].strip(), "```")

    def test_missing_marker_is_an_error(self):
        with self.assertRaises(ValueError):
            extract_listing("no appendix here")

    def test_missing_fence_is_an_error(self):
        with self.assertRaises(ValueError):
            extract_listing("### A.1 Statistics module\nno fence")


class RealListingTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        document = SSOT.read_text(encoding="utf-8")
        cls.source, _, _ = extract_listing(document)
        header = REGISTER.read_text(encoding="utf-8").splitlines()[0]
        cls.header = [cell.strip() for cell in header.split(",")]
        cls.result = audit(cls.source, cls.header)

    def test_register_header_uses_canonical_column(self):
        self.assertIn(CANONICAL_REGISTER_COLUMN, self.header)

    def test_selftest_harness_and_sentinel_present(self):
        self.assertTrue(self.result["listing"]["selftest_harness_present"])
        self.assertTrue(self.result["listing"]["selftest_sentinel_present"])

    def test_all_named_defects_confirmed(self):
        self.assertEqual(self.result["state"], "DEFECTS_CONFIRMED")
        self.assertEqual(self.result["summary"]["not_found"], 0, self.result["summary"])
        self.assertEqual(self.result["summary"]["confirmed"], len(self.result["defects"]))
        self.assertGreaterEqual(self.result["summary"]["confirmed"], 9)

    def test_every_confirmed_defect_carries_line_evidence(self):
        for defect in self.result["defects"]:
            self.assertEqual(defect["status"], "CONFIRMED_IN_SOURCE", defect["code"])
            self.assertTrue(defect["evidence"], defect["code"])
            for item in defect["evidence"]:
                self.assertGreater(item["line"], 0)
                self.assertTrue(item["text"], defect["code"])

    def test_evidence_lines_point_at_real_source_text(self):
        lines = self.source.split("\n")
        for defect in self.result["defects"]:
            for item in defect["evidence"]:
                self.assertEqual(lines[item["line"] - 1].strip(), item["text"], defect["code"])

    def test_listing_hash_is_pinned_and_stable(self):
        self.assertEqual(
            self.result["listing"]["source_sha256"],
            audit(self.source, self.header)["listing"]["source_sha256"],
        )
        self.assertEqual(len(self.result["listing"]["source_sha256"]), 64)


class FalsifierTests(unittest.TestCase):
    """A corrected listing must report NOT_FOUND for that specific check."""

    @classmethod
    def setUpClass(cls):
        document = SSOT.read_text(encoding="utf-8")
        cls.source, _, _ = extract_listing(document)
        cls.header = ["ID", "Evidence_status", "Outcome"]

    def _audit(self, mutated, header=None):
        if header is None:
            header = self.header
        elif header == "__NO_HEADER__":
            header = None
        return audit(mutated, header)

    def test_infinite_score_guard_clears_evaluator_defect(self):
        mutated = self.source.replace(
            "        if s <= t:\n",
            "        if math.isfinite(t) and math.isfinite(s) and s <= t:\n",
        )
        self.assertNotEqual(mutated, self.source)
        self.assertEqual(
            _by_code(self._audit(mutated), "EVALUATOR_INF_LE_INF_COVERED")["status"],
            "NOT_FOUND",
        )

    def test_rejecting_unknown_cells_clears_absent_calibration_defect(self):
        mutated = self.source.replace(
            '        t = thresholds.get(cell, {}).get("threshold", math.inf)\n',
            '        t = thresholds[cell]["threshold"]\n',
        )
        self.assertNotEqual(mutated, self.source)
        self.assertEqual(
            _by_code(self._audit(mutated), "ABSENT_CALIBRATION_AS_INFINITY")["status"],
            "NOT_FOUND",
        )

    def test_removing_silent_truncation_clears_sampler_defect(self):
        mutated = self.source.replace(
            "        m = min(alloc[s], len(v))\n",
            "        m = alloc[s]\n",
        )
        self.assertNotEqual(mutated, self.source)
        result = self._audit(mutated)
        self.assertEqual(_by_code(result, "SAMPLER_SILENT_SHORTFALL")["status"], "NOT_FOUND")

    def test_setdefault_clears_duplicate_rank_overwrite(self):
        mutated = self.source.replace(
            "            key[cand.strip().lower()][arm] = rank\n",
            "            key[cand.strip().lower()].setdefault(arm, rank)\n",
        )
        self.assertNotEqual(mutated, self.source)
        self.assertEqual(
            _by_code(self._audit(mutated), "DUPLICATE_CANDIDATE_RANK_OVERWRITE")["status"],
            "NOT_FOUND",
        )

    def test_bounded_signature_clears_unbounded_rank(self):
        mutated = self.source.replace(
            "def _synthetic_scores(rng, n, p_none, geo_p):",
            "def _synthetic_scores(rng, n, p_none, geo_p, M=None):",
        )
        self.assertNotEqual(mutated, self.source)
        self.assertEqual(
            _by_code(self._audit(mutated), "SYNTHETIC_RANK_UNBOUNDED_BY_M")["status"],
            "NOT_FOUND",
        )

    def test_matching_register_header_clears_field_divergence(self):
        result = self._audit(self.source, header=["ID", "Status", "Outcome"])
        defect = _by_code(result, "SCOREBOARD_FIELD_NAME_DIVERGENCE")
        self.assertEqual(defect["status"], "NOT_FOUND")
        self.assertTrue(defect["register_header_has_Status"])
        self.assertFalse(defect["register_header_has_Evidence_status"])

    def test_missing_register_header_cannot_confirm_field_divergence(self):
        result = self._audit(self.source, header="__NO_HEADER__")
        self.assertEqual(
            _by_code(result, "SCOREBOARD_FIELD_NAME_DIVERGENCE")["status"], "NOT_FOUND"
        )

    def test_explicit_undefined_kappa_clears_nan_defect(self):
        mutated = self.source.replace(
            '        return float("nan")\n',
            '        raise ValueError("undefined kappa")\n',
        )
        self.assertNotEqual(mutated, self.source)
        self.assertEqual(
            _by_code(self._audit(mutated), "DEGENERATE_KAPPA_NAN_VACUOUS_BOOTSTRAP")["status"],
            "NOT_FOUND",
        )

    def test_alpha_validation_clears_unvalidated_alpha(self):
        mutated = self.source.replace(
            "    k = math.ceil((n + 1) * (1 - alpha))\n",
            "    if not (0.0 < alpha < 1.0):\n        raise ValueError('alpha')\n"
            "    k = math.ceil((n + 1) * (1 - alpha))\n",
        )
        self.assertNotEqual(mutated, self.source)
        self.assertEqual(
            _by_code(self._audit(mutated), "CONFORMAL_ALPHA_UNVALIDATED")["status"],
            "NOT_FOUND",
        )


class SelftestExecutionTests(unittest.TestCase):
    def test_sentinel_script_is_detected(self):
        result = run_selftest('print("%s")\n' % SELFTEST_SENTINEL, timeout=60)
        self.assertTrue(result["executed"])
        self.assertTrue(result["passed"], result)
        self.assertTrue(result["sentinel_present"])

    def test_failing_script_is_not_a_pass(self):
        result = run_selftest('raise SystemExit(3)\n', timeout=60)
        self.assertTrue(result["executed"])
        self.assertFalse(result["passed"])
        self.assertEqual(result["returncode"], 3)


if __name__ == "__main__":
    unittest.main()
