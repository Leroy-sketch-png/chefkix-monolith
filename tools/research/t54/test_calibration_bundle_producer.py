"""
Tests for T54 calibration_bundle_producer.py.

Verifies contract invariants from:
  evidence/iron-chef-lead-member-handoff-20261105/t54-method-v2-producer-contract-20261106.md

All fixtures are synthetic and source-free. No private data, labels, real
calibration results, empirical acceptance, or route activation is claimed.
"""

from __future__ import annotations

import hashlib
import math
import unittest
from typing import Dict, List, Optional, Set

from calibration_bundle_producer import (
    BINDING_STATUS_MISSING,
    BINDING_STATUS_MISMATCHED,
    BINDING_STATUS_OK,
    LABEL_CONTRACT_RECORDED_EXACT,
    REQUIRED_ROLES,
    SCHEMA_VERSION,
    CalibrationReceipt,
    FrameCase,
    ProducerError,
    RecipeGroupAssignment,
    RoleBinding,
    ScoredCase,
    build_cell_projections,
    execute_recipe_group_split,
    produce_calibration_receipt,
    validate_frame,
    validate_scored_row,
    verify_role_bindings,
)


# ── helpers ──────────────────────────────────────────────────────────────────

def _dummy_sha256(tag: str) -> str:
    return hashlib.sha256(tag.encode()).hexdigest()


def _make_cases(
    n: int,
    recipes: Optional[List[str]] = None,
    train_yes: bool = True,
    profile_yes: bool = True,
) -> List[FrameCase]:
    if recipes is None:
        # Distribute across 4 synthetic recipe IDs for a minimal valid G=4 split
        recipe_ids = [f"recipe-{i % 4}" for i in range(n)]
    else:
        recipe_ids = recipes
    return [
        FrameCase(
            case_id=f"case-{i:04d}",
            recipe_id=recipe_ids[i],
            train_source_presence=train_yes,
            foodb_profile_presence=profile_yes,
        )
        for i in range(n)
    ]


def _make_scored_rows(cases: List[FrameCase], m: int = 20) -> List[ScoredCase]:
    """Synthetic scored rows: finite rank=1 for all cases."""
    return [
        ScoredCase(case_id=c.case_id, score=1, failure=False, outcome_code=None)
        for c in cases
    ]


def _make_role_bindings(match: bool = True) -> List[RoleBinding]:
    bindings = []
    for role in REQUIRED_ROLES:
        obs = _dummy_sha256(role)
        decl = obs if match else _dummy_sha256(role + "-wrong")
        bindings.append(RoleBinding(role_name=role, declared_sha256=decl, observed_sha256=obs))
    return bindings


def _full_receipt(
    n: int = 20,
    cell_id: str = "baseline_0",
    alpha: float = 0.1,
    m: int = 20,
    role_match: bool = True,
) -> CalibrationReceipt:
    cases = _make_cases(n)
    scored = _make_scored_rows(cases, m)
    return produce_calibration_receipt(
        cohort_id="synthetic-cohort",
        cell_id=cell_id,
        alpha=alpha,
        m_cutoff=m,
        score_contract_id="test-score-contract-v1",
        label_contract_id=LABEL_CONTRACT_RECORDED_EXACT,
        cases=cases,
        scored_rows=scored,
        role_bindings=_make_role_bindings(role_match),
        frame_manifest_sha256=_dummy_sha256("frame"),
    )


# ── T1: recipe group split determinism ───────────────────────────────────────

class TestRecipeGroupSplit(unittest.TestCase):

    def test_split_requires_at_least_4_recipes(self):
        with self.assertRaises(ProducerError):
            execute_recipe_group_split({"r1", "r2", "r3"})

    def test_split_exactly_4_recipes(self):
        result = execute_recipe_group_split({"r1", "r2", "r3", "r4"})
        self.assertEqual(result.total_recipe_groups, 4)
        self.assertEqual(result.dev_group_count, math.floor(3 * 4 / 10))  # = 1
        self.assertEqual(result.cal_group_count, 3)

    def test_split_input_order_invariant(self):
        ids_a = {"apple", "banana", "cherry", "date", "elderberry", "fig", "grape"}
        ids_b = set(sorted(ids_a, reverse=True))
        result_a = execute_recipe_group_split(ids_a)
        result_b = execute_recipe_group_split(ids_b)
        self.assertEqual(result_a.recipe_to_split, result_b.recipe_to_split)
        self.assertEqual(result_a.global_assignment_digest, result_b.global_assignment_digest)

    def test_split_30_70_ratio(self):
        ids = {f"r{i}" for i in range(10)}
        result = execute_recipe_group_split(ids)
        # floor(3*10/10) = 3 dev, 7 cal
        self.assertEqual(result.dev_group_count, 3)
        self.assertEqual(result.cal_group_count, 7)

    def test_split_no_recipe_crosses_partition(self):
        ids = {f"recipe-{i}" for i in range(8)}
        result = execute_recipe_group_split(ids)
        splits = set(result.recipe_to_split.values())
        self.assertLessEqual(splits, {"development", "calibration"})
        for r_id, split in result.recipe_to_split.items():
            self.assertIn(split, ("development", "calibration"))

    def test_split_global_digest_stable(self):
        ids = {"alpha", "beta", "gamma", "delta"}
        r1 = execute_recipe_group_split(ids)
        r2 = execute_recipe_group_split(ids)
        self.assertEqual(r1.global_assignment_digest, r2.global_assignment_digest)


# ── T2: frame validation ──────────────────────────────────────────────────────

class TestFrameValidation(unittest.TestCase):

    def test_empty_frame_raises(self):
        with self.assertRaises(ProducerError):
            validate_frame([])

    def test_duplicate_case_id_raises(self):
        cases = [
            FrameCase("case-1", "r1", True, True),
            FrameCase("case-1", "r2", True, False),
        ]
        with self.assertRaises(ProducerError):
            validate_frame(cases)

    def test_empty_case_id_raises(self):
        with self.assertRaises(ProducerError):
            validate_frame([FrameCase("", "r1", True, True)])

    def test_empty_recipe_id_raises(self):
        with self.assertRaises(ProducerError):
            validate_frame([FrameCase("case-1", "", True, True)])


# ── T3: scored row validation ─────────────────────────────────────────────────

class TestScoredRowValidation(unittest.TestCase):

    def test_finite_rank_valid(self):
        validate_scored_row(ScoredCase("c1", 1, False, None), 20)
        validate_scored_row(ScoredCase("c1", 20, False, None), 20)

    def test_no_reference_match_valid(self):
        validate_scored_row(ScoredCase("c1", "infinity", False, "no_reference_match"), 20)

    def test_empty_filtered_pool_valid(self):
        validate_scored_row(ScoredCase("c1", "infinity", False, "empty_filtered_pool"), 20)

    def test_scoring_failure_valid(self):
        validate_scored_row(ScoredCase("c1", None, True, "scoring_failure"), 20)

    def test_method_failure_valid(self):
        validate_scored_row(ScoredCase("c1", None, True, "method_failure"), 20)

    def test_finite_rank_with_outcome_code_raises(self):
        with self.assertRaises(ProducerError):
            validate_scored_row(ScoredCase("c1", 5, False, "no_reference_match"), 20)

    def test_rank_above_m_raises(self):
        with self.assertRaises(ProducerError):
            validate_scored_row(ScoredCase("c1", 21, False, None), 20)

    def test_rank_zero_raises(self):
        with self.assertRaises(ProducerError):
            validate_scored_row(ScoredCase("c1", 0, False, None), 20)

    def test_failure_with_non_none_score_raises(self):
        with self.assertRaises(ProducerError):
            validate_scored_row(ScoredCase("c1", 1, True, "scoring_failure"), 20)

    def test_failure_with_bad_outcome_code_raises(self):
        with self.assertRaises(ProducerError):
            validate_scored_row(ScoredCase("c1", None, True, "no_reference_match"), 20)


# ── T4: role binding verification ─────────────────────────────────────────────

class TestRoleBindingVerification(unittest.TestCase):

    def test_all_roles_match_ok(self):
        bindings = _make_role_bindings(match=True)
        status, errors = verify_role_bindings(bindings)
        self.assertEqual(status, BINDING_STATUS_OK)
        self.assertEqual(errors, [])

    def test_missing_role_returns_missing(self):
        bindings = [
            RoleBinding(r, _dummy_sha256(r), _dummy_sha256(r))
            for r in list(REQUIRED_ROLES)[:-1]  # drop last role
        ]
        status, errors = verify_role_bindings(bindings)
        self.assertEqual(status, BINDING_STATUS_MISSING)
        self.assertTrue(any(REQUIRED_ROLES[-1] in e for e in errors))

    def test_digest_mismatch_returns_mismatched(self):
        bindings = []
        for i, role in enumerate(REQUIRED_ROLES):
            obs = _dummy_sha256(role)
            decl = _dummy_sha256(role + "-wrong") if i == 0 else obs
            bindings.append(RoleBinding(role, decl, obs))
        status, errors = verify_role_bindings(bindings)
        self.assertEqual(status, BINDING_STATUS_MISMATCHED)

    def test_undeclared_role_ok_if_observed(self):
        """Caller-supplied declared_sha256=None means no expectation; still OK."""
        bindings = [
            RoleBinding(r, None, _dummy_sha256(r))
            for r in REQUIRED_ROLES
        ]
        status, errors = verify_role_bindings(bindings)
        self.assertEqual(status, BINDING_STATUS_OK)


# ── T5: full receipt production ───────────────────────────────────────────────

class TestReceiptProduction(unittest.TestCase):

    def test_receipt_always_no_guarantee(self):
        receipt = _full_receipt(n=300)
        self.assertEqual(receipt.guarantee_status, "no_guarantee")
        self.assertEqual(receipt.no_guarantee_reason, "inference_assumptions_unresolved")
        self.assertIsNone(receipt.product_t_hat)

    def test_receipt_schema_version(self):
        receipt = _full_receipt(n=20)
        self.assertEqual(receipt.schema_version, SCHEMA_VERSION)

    def test_missing_scored_case_raises(self):
        cases = _make_cases(10)
        # Only provide 9 of 10 calibration cases (cases all go to cal in baseline_0)
        scored = _make_scored_rows(cases[:-1])
        with self.assertRaises(ProducerError):
            produce_calibration_receipt(
                cohort_id="c",
                cell_id="baseline_0",
                alpha=0.1,
                m_cutoff=20,
                score_contract_id="sc-v1",
                label_contract_id=LABEL_CONTRACT_RECORDED_EXACT,
                cases=cases,
                scored_rows=scored,
                role_bindings=_make_role_bindings(),
            )

    def test_duplicate_scored_case_raises(self):
        cases = _make_cases(5)
        scored = _make_scored_rows(cases)
        scored.append(ScoredCase(cases[0].case_id, 1, False, None))  # duplicate
        with self.assertRaises(ProducerError):
            produce_calibration_receipt(
                cohort_id="c",
                cell_id="baseline_0",
                alpha=0.1,
                m_cutoff=20,
                score_contract_id="sc-v1",
                label_contract_id=LABEL_CONTRACT_RECORDED_EXACT,
                cases=cases,
                scored_rows=scored,
                role_bindings=_make_role_bindings(),
            )

    def test_binding_errors_propagate_to_receipt(self):
        receipt = _full_receipt(n=20, role_match=False)
        self.assertEqual(receipt.binding_status, BINDING_STATUS_MISMATCHED)
        self.assertTrue(len(receipt.binding_errors) > 0)
        # Still no_guarantee even with mismatch
        self.assertEqual(receipt.guarantee_status, "no_guarantee")

    def test_n_counts_correctly(self):
        # 20 cases, all going to calibration in baseline_0
        receipt = _full_receipt(n=20, cell_id="baseline_0")
        # All cases: 4 unique recipes, dev = floor(3*4/10) = 1, cal = 3
        # With 20 cases across 4 recipes, most go to cal
        cal_expected = sum(
            1 for c in _make_cases(20)
            if execute_recipe_group_split(
                {f"recipe-{i % 4}" for i in range(20)}
            ).recipe_to_split.get(c.recipe_id) == "calibration"
        )
        self.assertEqual(receipt.n_calibration, cal_expected)

    def test_finite_scores_counted_correctly(self):
        cases = _make_cases(8)
        scored = [
            ScoredCase(c.case_id, 1, False, None) for c in cases[:4]
        ] + [
            ScoredCase(c.case_id, "infinity", False, "no_reference_match") for c in cases[4:6]
        ] + [
            ScoredCase(c.case_id, "infinity", False, "empty_filtered_pool") for c in cases[6:7]
        ] + [
            ScoredCase(c.case_id, None, True, "scoring_failure") for c in cases[7:]
        ]
        receipt = produce_calibration_receipt(
            cohort_id="c",
            cell_id="baseline_0",
            alpha=0.1,
            m_cutoff=20,
            score_contract_id="sc-v1",
            label_contract_id=LABEL_CONTRACT_RECORDED_EXACT,
            cases=cases,
            scored_rows=scored,
            role_bindings=_make_role_bindings(),
        )
        # Only calibration cases are counted (dev_count = floor(3*4/10) = 1 recipe)
        # Most of 8 cases will be in calibration; check counts sum to n_calibration
        total = (
            receipt.finite_score_count
            + receipt.no_reference_match_count
            + receipt.empty_pool_abstain_count
            + receipt.failure_count
        )
        self.assertEqual(total, receipt.n_calibration)

    def test_invalid_cell_raises(self):
        with self.assertRaises(ProducerError):
            _full_receipt(cell_id="invalid_cell")

    def test_alpha_out_of_range_raises(self):
        cases = _make_cases(10)
        with self.assertRaises(ProducerError):
            produce_calibration_receipt(
                cohort_id="c", cell_id="baseline_0", alpha=0.0, m_cutoff=20,
                score_contract_id="sc", label_contract_id=LABEL_CONTRACT_RECORDED_EXACT,
                cases=cases, scored_rows=_make_scored_rows(cases),
                role_bindings=_make_role_bindings(),
            )

    def test_receipt_canonical_digest_stable(self):
        receipt = _full_receipt(n=20)
        d1 = receipt.canonical_digest()
        d2 = receipt.canonical_digest()
        self.assertEqual(d1, d2)
        self.assertEqual(len(d1), 64)

    def test_product_t_hat_never_populated(self):
        """Even with large n and finite scores, product_t_hat must be None."""
        cases = _make_cases(300)
        scored = _make_scored_rows(cases)
        receipt = produce_calibration_receipt(
            cohort_id="c", cell_id="baseline_0", alpha=0.1, m_cutoff=20,
            score_contract_id="sc", label_contract_id=LABEL_CONTRACT_RECORDED_EXACT,
            cases=cases, scored_rows=scored,
            role_bindings=_make_role_bindings(),
        )
        self.assertIsNone(receipt.product_t_hat)

    def test_global_assignment_digest_in_receipt(self):
        receipt = _full_receipt(n=16)
        # Must be a 64-char hex string
        self.assertEqual(len(receipt.global_assignment_digest), 64)
        self.assertTrue(all(c in "0123456789abcdef" for c in receipt.global_assignment_digest))

    def test_projection_digest_in_receipt(self):
        receipt = _full_receipt(n=16)
        self.assertEqual(len(receipt.projection_digest), 64)

    def test_partition1_cell_isolates_subset(self):
        # Create 20 cases: half train_yes, half train_no, all profile_yes
        cases = []
        for i in range(20):
            cases.append(FrameCase(
                case_id=f"case-{i:03d}",
                recipe_id=f"recipe-{i % 4}",
                train_source_presence=(i < 10),
                foodb_profile_presence=True,
            ))
        scored = _make_scored_rows(cases)
        receipt_train_yes = produce_calibration_receipt(
            cohort_id="c", cell_id="train_yes_profile_yes", alpha=0.1, m_cutoff=20,
            score_contract_id="sc", label_contract_id=LABEL_CONTRACT_RECORDED_EXACT,
            cases=cases, scored_rows=scored,
            role_bindings=_make_role_bindings(),
        )
        receipt_train_no = produce_calibration_receipt(
            cohort_id="c", cell_id="train_no_profile_yes", alpha=0.1, m_cutoff=20,
            score_contract_id="sc", label_contract_id=LABEL_CONTRACT_RECORDED_EXACT,
            cases=cases, scored_rows=scored,
            role_bindings=_make_role_bindings(),
        )
        # Cells are disjoint, so n counts should not overlap
        total_cell = receipt_train_yes.n_calibration + receipt_train_no.n_calibration
        receipt_all = produce_calibration_receipt(
            cohort_id="c", cell_id="baseline_0", alpha=0.1, m_cutoff=20,
            score_contract_id="sc", label_contract_id=LABEL_CONTRACT_RECORDED_EXACT,
            cases=cases, scored_rows=scored,
            role_bindings=_make_role_bindings(),
        )
        self.assertEqual(total_cell, receipt_all.n_calibration)

    def test_role_digests_in_receipt_dict(self):
        receipt = _full_receipt(n=10)
        for role in REQUIRED_ROLES:
            self.assertIn(role, receipt.role_digests)
            self.assertEqual(receipt.role_digests[role], _dummy_sha256(role))

    def test_to_dict_has_required_keys(self):
        receipt = _full_receipt(n=10)
        d = receipt.to_dict()
        for key in [
            "schema_version", "method_version", "split_algorithm_id",
            "cohort_id", "cell_id", "alpha", "m_cutoff",
            "score_contract_id", "label_contract_id",
            "frame_manifest_sha256", "global_assignment_digest", "projection_digest",
            "binding_status", "binding_errors", "role_digests",
            "n_calibration", "finite_score_count", "no_reference_match_count",
            "empty_pool_abstain_count", "failure_count",
            "k_order_statistic", "sample_cutoff_descriptive", "sample_empirical_coverage",
            "guarantee_status", "no_guarantee_reason", "product_t_hat",
        ]:
            self.assertIn(key, d, f"Missing key: {key}")


if __name__ == "__main__":
    unittest.main()

