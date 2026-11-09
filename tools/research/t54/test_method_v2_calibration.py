"""
Unit tests for T54 Method-v2 Scorer, Group-Split, and Conformal Calibration Processor.

Verifies:
1. Recipe group split algorithm (G >= 4 requirement, 30/70 split, input order invariance).
2. CalibrationScoreRow effective scores and outcome types.
3. Conformal evaluation order statistic calculation.
4. Denominator retention of valid no-match, empty pool abstain, and failures.
5. Fail-closed gates: n < 200 and unproven exchangeability yielding null product tHat.
6. Nine-role provenance verification.
"""

import unittest

from tools.research.t54.method_v2_calibration import (
    execute_recipe_group_split,
    CalibrationScoreRow,
    evaluate_conformal_calibration,
    verify_nine_role_bindings,
    REQUIRED_ROLES,
)


class RecipeGroupSplitTests(unittest.TestCase):
    def test_rejection_when_less_than_four_groups(self):
        with self.assertRaises(ValueError):
            execute_recipe_group_split({"rec_1", "rec_2", "rec_3"})

    def test_split_ratio_and_counts(self):
        # G = 10 -> dev = floor(30/10) = 3, cal = 7
        recipe_ids = {f"recipe_{i}" for i in range(10)}
        res = execute_recipe_group_split(recipe_ids)
        self.assertEqual(res.total_recipe_groups, 10)
        self.assertEqual(res.dev_group_count, 3)
        self.assertEqual(res.cal_group_count, 7)
        self.assertEqual(len(res.recipe_to_split), 10)

    def test_input_order_invariance(self):
        recipes_list1 = [f"rec_{i}" for i in range(25)]
        recipes_list2 = list(reversed(recipes_list1))

        res1 = execute_recipe_group_split(set(recipes_list1))
        res2 = execute_recipe_group_split(set(recipes_list2))

        self.assertEqual(res1.global_assignment_digest, res2.global_assignment_digest)
        self.assertEqual(res1.recipe_to_split, res2.recipe_to_split)


class CalibrationScoreRowTests(unittest.TestCase):
    def test_finite_score(self):
        row = CalibrationScoreRow(case_id="c1", cell_id="baseline_0", score=3, failure=False)
        self.assertEqual(row.effective_conformal_score(), 3.0)
        self.assertFalse(row.is_empty_pool_abstain())

    def test_no_reference_match_infinity(self):
        row = CalibrationScoreRow(
            case_id="c2",
            cell_id="baseline_0",
            score="infinity",
            failure=False,
            outcome_code="no_reference_match",
        )
        self.assertEqual(row.effective_conformal_score(), float("inf"))
        self.assertFalse(row.failure)
        self.assertFalse(row.is_empty_pool_abstain())

    def test_empty_pool_abstain(self):
        row = CalibrationScoreRow(
            case_id="c3",
            cell_id="baseline_0",
            score="infinity",
            failure=False,
            outcome_code="empty_filtered_pool",
        )
        self.assertEqual(row.effective_conformal_score(), float("inf"))
        self.assertTrue(row.is_empty_pool_abstain())

    def test_operational_failure(self):
        row = CalibrationScoreRow(
            case_id="c4",
            cell_id="baseline_0",
            score=None,
            failure=True,
            outcome_code="scoring_failure",
        )
        self.assertEqual(row.effective_conformal_score(), float("inf"))
        self.assertTrue(row.failure)


class ConformalEvaluationTests(unittest.TestCase):
    def test_sample_size_below_200_fails_gate(self):
        # 150 rows (< 200)
        rows = [
            CalibrationScoreRow(case_id=f"case_{i}", cell_id="baseline_0", score=(i % 10) + 1, failure=False)
            for i in range(150)
        ]
        res = evaluate_conformal_calibration(rows, cell_id="baseline_0", alpha=0.10)
        self.assertEqual(res.n_calibration, 150)
        self.assertEqual(res.guarantee_status, "no_guarantee")
        self.assertEqual(res.no_guarantee_reason, "insufficient_sample_size_below_200")
        self.assertIsNone(res.product_t_hat)

    def test_unproven_exchangeability_blocks_product_t_hat(self):
        # 250 rows (>= 200), but exchangeability unproven
        rows = [
            CalibrationScoreRow(case_id=f"case_{i}", cell_id="baseline_0", score=(i % 15) + 1, failure=False)
            for i in range(250)
        ]
        res = evaluate_conformal_calibration(
            rows, cell_id="baseline_0", alpha=0.10, assume_exchangeability=False
        )
        self.assertEqual(res.n_calibration, 250)
        self.assertEqual(res.guarantee_status, "no_guarantee")
        self.assertEqual(res.no_guarantee_reason, "inference_assumptions_unresolved")
        self.assertIsNone(res.product_t_hat)
        # But descriptive sample cutoff is computed for analysis reports
        self.assertIsNotNone(res.sample_cutoff_descriptive)
        self.assertGreater(res.sample_empirical_coverage, 0.8)

    def test_order_statistic_exact_calculation(self):
        # n = 200, alpha = 0.10 -> k = ceil(201 * 0.90) = ceil(180.9) = 181
        rows = []
        for i in range(150):
            rows.append(CalibrationScoreRow(f"c_{i}", "baseline_0", score=2, failure=False))
        for i in range(150, 190):
            rows.append(CalibrationScoreRow(f"c_{i}", "baseline_0", score=5, failure=False))
        for i in range(190, 200):
            rows.append(CalibrationScoreRow(f"c_{i}", "baseline_0", score="infinity", failure=False, outcome_code="no_reference_match"))

        res = evaluate_conformal_calibration(rows, cell_id="baseline_0", alpha=0.10)
        self.assertEqual(res.n_calibration, 200)
        self.assertEqual(res.k_order_statistic, 181)
        # Position 181 (1-based) is among the score=5 group (ranks 151 to 190)
        self.assertEqual(res.sample_cutoff_descriptive, 5.0)


class NineRoleProvenanceTests(unittest.TestCase):
    def test_all_nine_roles_verified(self):
        roles = {role: f"hash_{role}" for role in REQUIRED_ROLES}
        ok, status, msgs = verify_nine_role_bindings(roles)
        self.assertTrue(ok)
        self.assertEqual(status, "verified")
        self.assertEqual(len(msgs), 0)

    def test_missing_role_detected(self):
        roles = {role: f"hash_{role}" for role in REQUIRED_ROLES if role != "identity_index"}
        ok, status, msgs = verify_nine_role_bindings(roles)
        self.assertFalse(ok)
        self.assertEqual(status, "missing_roles")
        self.assertTrue(any("identity_index" in m for m in msgs))

    def test_mismatched_role_detected(self):
        roles = {role: f"hash_{role}" for role in REQUIRED_ROLES}
        expected = {role: f"hash_{role}" for role in REQUIRED_ROLES}
        expected["scorer"] = "different_hash"

        ok, status, msgs = verify_nine_role_bindings(roles, expected_digests=expected)
        self.assertFalse(ok)
        self.assertEqual(status, "mismatched_roles")
        self.assertTrue(any("Role scorer hash mismatch" in m for m in msgs))


if __name__ == "__main__":
    unittest.main()
