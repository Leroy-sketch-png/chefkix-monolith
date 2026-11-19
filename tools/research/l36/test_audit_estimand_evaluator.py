"""Unit tests for L36 Adjudicated Strict-A@5 Audit Estimand & Partial-Identification Bounds Evaluator."""
import math
import unittest

from tools.research.l36.audit_estimand_evaluator import (
    ALLOWED_LABELS,
    BOOTSTRAP_DRAWS,
    DECISION_THRESHOLD,
    FROZEN_FRAME_SIZES,
    FROZEN_SAMPLE_SIZES,
    PR10_KAPPA_THRESHOLD,
    PROTOCOL_SHA256,
    SPECIFICATION_ID,
    TOTAL_FRAME,
    TOTAL_SAMPLE,
    UNSEEN_FRAME,
    UNSEEN_SAMPLE,
    AuditCase,
    CaseValue,
    DecisionStatus,
    cohens_kappa,
    compute_partial_bounds,
    evaluate_audit_estimand,
    evaluate_case_rule,
    linear_quantile,
    run_cluster_bootstrap,
    wilson_score_interval,
)


class TestCaseRule(unittest.TestCase):
    def test_single_a_is_success(self):
        val, ok, err = evaluate_case_rule(["N", "A", "N", "N", "N"])
        self.assertEqual(val, CaseValue.SUCCESS)
        self.assertTrue(ok)
        self.assertIsNone(err)

    def test_a_overrides_u(self):
        # Case rule: At least one 'A' => V=1, even if another candidate is 'U'
        val, ok, err = evaluate_case_rule(["A", "U", "C", "N", "N"])
        self.assertEqual(val, CaseValue.SUCCESS)
        self.assertTrue(ok)

    def test_all_c_or_n_is_failure(self):
        val, ok, err = evaluate_case_rule(["C", "C", "N", "N", "N"])
        self.assertEqual(val, CaseValue.FAILURE)
        self.assertTrue(ok)

    def test_u_without_a_is_unknown_not_failure(self):
        # U is not negative and never silently imputed as failure
        val, ok, err = evaluate_case_rule(["C", "N", "U", "N", "N"])
        self.assertEqual(val, CaseValue.UNKNOWN)
        self.assertTrue(ok)

    def test_malformed_slate_fails_integrity(self):
        val, ok, err = evaluate_case_rule(["A", "C", "N"])  # only 3 candidates
        self.assertEqual(val, CaseValue.FAILURE)
        self.assertFalse(ok)
        self.assertIn("Malformed slate", err)

    def test_operational_failure_retained_in_denominator(self):
        val, ok, err = evaluate_case_rule(["A", "A", "A", "A", "A"], is_operational_failure=True)
        self.assertEqual(val, CaseValue.FAILURE)
        self.assertTrue(ok)
        self.assertIn("Operational failure", err)


class TestStrataAndConstants(unittest.TestCase):
    def test_protocol_and_frame_pins(self):
        self.assertEqual(PROTOCOL_SHA256, "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12")
        self.assertEqual(SPECIFICATION_ID, "iron-chef-l36-case-rule-v1")
        self.assertEqual(sum(FROZEN_FRAME_SIZES.values()), TOTAL_FRAME)
        self.assertEqual(TOTAL_FRAME, 10747)
        self.assertEqual(FROZEN_FRAME_SIZES["unseen_profiled"], 176)
        self.assertEqual(FROZEN_FRAME_SIZES["unseen_unprofiled"], 3639)
        self.assertEqual(UNSEEN_FRAME, 176 + 3639)
        self.assertEqual(UNSEEN_SAMPLE, 50 + 50)
        self.assertEqual(TOTAL_SAMPLE, 150)


class TestPartialIdentificationBounds(unittest.TestCase):
    def setUp(self):
        # Construct synthetic sample: 50 unseen_profiled, 50 unseen_unprofiled
        self.cases = []
        # Unseen profiled: 50 cases (clusters p_1 .. p_10)
        for i in range(50):
            cluster = f"p_clust_{i // 5}"
            # 10 successes (A), 5 unknowns (U), 35 failures (N)
            if i < 10:
                labels = ["A", "N", "N", "N", "N"]
            elif i < 15:
                labels = ["U", "N", "N", "N", "N"]
            else:
                labels = ["N", "N", "N", "N", "N"]
            self.cases.append(AuditCase(f"case_p_{i}", "unseen_profiled", cluster, labels))

        # Unseen unprofiled: 50 cases (clusters u_1 .. u_10)
        for i in range(50):
            cluster = f"u_clust_{i // 5}"
            # 15 successes (A), 10 unknowns (U), 25 failures (N)
            if i < 15:
                labels = ["A", "N", "N", "N", "N"]
            elif i < 25:
                labels = ["U", "N", "N", "N", "N"]
            else:
                labels = ["N", "N", "N", "N", "N"]
            self.cases.append(AuditCase(f"case_u_{i}", "unseen_unprofiled", cluster, labels))

        # Add 50 seen cases to verify they are excluded from the unseen estimand
        for i in range(25):
            self.cases.append(AuditCase(f"case_sp_{i}", "seen_profiled", f"sp_{i}", ["A"] * 5))
        for i in range(25):
            self.cases.append(AuditCase(f"case_su_{i}", "seen_unprofiled", f"su_{i}", ["A"] * 5))

    def test_bounds_computation_unseen_population(self):
        bounds, eval_results, p_ac = compute_partial_bounds(self.cases)
        self.assertEqual(bounds.total_unseen_cases, 100)
        self.assertEqual(bounds.stratum_sample_counts["unseen_profiled"], 50)
        self.assertEqual(bounds.stratum_sample_counts["unseen_unprofiled"], 50)
        self.assertAlmostEqual(bounds.total_weighted_denominator, 3815.0)

        # Profiled: 10/50 L, 15/50 U. Weight = 176 / 50 = 3.52
        # Unprofiled: 15/50 L, 25/50 U. Weight = 3639 / 50 = 72.78
        # Weighted L = 3.52 * 10 + 72.78 * 15 = 35.2 + 1091.7 = 1126.9
        # Weighted U = 3.52 * 15 + 72.78 * 25 = 52.8 + 1819.5 = 1872.3
        expected_p_lower = 1126.9 / 3815.0
        expected_p_upper = 1872.3 / 3815.0

        self.assertAlmostEqual(bounds.p_lower, expected_p_lower, places=5)
        self.assertAlmostEqual(bounds.p_upper, expected_p_upper, places=5)
        self.assertLess(bounds.p_lower, bounds.p_upper)
        self.assertAlmostEqual(bounds.weighted_unknown_mass, expected_p_upper - expected_p_lower, places=5)
        self.assertEqual(bounds.n_unknown_cases, 15)  # 5 + 10

    def test_zero_unknowns_collapses_bounds(self):
        # Sample with no 'U' labels
        clean_cases = [
            AuditCase(f"c_{i}", "unseen_profiled" if i < 50 else "unseen_unprofiled", f"cl_{i}", ["A" if i % 2 == 0 else "N"] * 5)
            for i in range(100)
        ]
        bounds, _, _ = compute_partial_bounds(clean_cases)
        self.assertEqual(bounds.n_unknown_cases, 0)
        self.assertAlmostEqual(bounds.p_lower, bounds.p_upper)
        self.assertAlmostEqual(bounds.weighted_unknown_mass, 0.0)


class TestLinearQuantile(unittest.TestCase):
    def test_exact_points(self):
        vals = [0.0, 10.0, 20.0, 30.0, 40.0]
        self.assertEqual(linear_quantile(vals, 0.0), 0.0)
        self.assertEqual(linear_quantile(vals, 0.5), 20.0)
        self.assertEqual(linear_quantile(vals, 1.0), 40.0)

    def test_interpolation(self):
        vals = [10.0, 20.0]
        self.assertAlmostEqual(linear_quantile(vals, 0.25), 12.5)
        self.assertAlmostEqual(linear_quantile(vals, 0.75), 17.5)

    def test_validation(self):
        with self.assertRaises(ValueError):
            linear_quantile([], 0.5)
        with self.assertRaises(ValueError):
            linear_quantile([1.0], -0.1)
        with self.assertRaises(ValueError):
            linear_quantile([1.0], 1.1)


class TestClusterBootstrapAndDecision(unittest.TestCase):
    def test_bootstrap_determinism(self):
        cases = []
        for i in range(50):
            cases.append(AuditCase(f"p_{i}", "unseen_profiled", f"clust_p_{i // 5}", ["A", "N", "N", "N", "N"]))
        for i in range(50):
            cases.append(AuditCase(f"u_{i}", "unseen_unprofiled", f"clust_u_{i // 5}", ["A", "N", "N", "N", "N"]))

        _, results, _ = compute_partial_bounds(cases)
        b1 = run_cluster_bootstrap(results, n_draws=100, seed=20261021)
        b2 = run_cluster_bootstrap(results, n_draws=100, seed=20261021)
        self.assertEqual(b1.p_lower_ci_95, b2.p_lower_ci_95)
        self.assertEqual(b1.p_upper_ci_95, b2.p_upper_ci_95)
        self.assertEqual(b1.valid_draws, 100)
        self.assertFalse(b1.cluster_crosses_strata)

    def test_cross_stratum_cluster_triggers_hold(self):
        cases = []
        # Shared cluster between profiled and unprofiled!
        for i in range(50):
            cases.append(AuditCase(f"p_{i}", "unseen_profiled", "shared_recipe_cluster", ["A"] * 5))
        for i in range(50):
            cases.append(AuditCase(f"u_{i}", "unseen_unprofiled", "shared_recipe_cluster", ["N"] * 5))

        _, results, _ = compute_partial_bounds(cases)
        boot = run_cluster_bootstrap(results, n_draws=50, seed=20261021)
        self.assertTrue(boot.cluster_crosses_strata)
        self.assertEqual(boot.valid_draws, 0)

        report = evaluate_audit_estimand(cases, n_bootstrap_draws=50)
        self.assertEqual(report.h1_decision, DecisionStatus.JOINT_CLUSTER_HOLD)

    def test_support_h1_decision(self):
        # High success rate (e.g. 80% A across both strata)
        cases = []
        for i in range(50):
            cases.append(AuditCase(f"p_{i}", "unseen_profiled", f"p_c_{i // 5}", ["A", "N", "N", "N", "N"]))
        for i in range(50):
            cases.append(AuditCase(f"u_{i}", "unseen_unprofiled", f"u_c_{i // 5}", ["A", "N", "N", "N", "N"]))

        report = evaluate_audit_estimand(cases, n_bootstrap_draws=200, seed=20261021)
        self.assertEqual(report.h1_decision, DecisionStatus.SUPPORT_H1)
        self.assertGreaterEqual(report.bootstrap_interval.p_lower_ci_95[0], DECISION_THRESHOLD)

    def test_refute_h1_decision(self):
        # Zero successes across both strata
        cases = []
        for i in range(50):
            cases.append(AuditCase(f"p_{i}", "unseen_profiled", f"p_c_{i // 5}", ["N", "N", "N", "N", "N"]))
        for i in range(50):
            cases.append(AuditCase(f"u_{i}", "unseen_unprofiled", f"u_c_{i // 5}", ["N", "N", "N", "N", "N"]))

        report = evaluate_audit_estimand(cases, n_bootstrap_draws=200, seed=20261021)
        self.assertEqual(report.h1_decision, DecisionStatus.REFUTE_H1)
        self.assertLess(report.bootstrap_interval.p_upper_ci_95[1], DECISION_THRESHOLD)


class TestRaterAgreementAndDiagnostics(unittest.TestCase):
    def test_perfect_kappa(self):
        pairs = [("A", "A"), ("C", "C"), ("N", "N"), ("U", "U")]
        self.assertEqual(cohens_kappa(pairs), 1.0)

    def test_chance_kappa(self):
        pairs = [("A", "A"), ("A", "C"), ("C", "A"), ("C", "C")]
        self.assertAlmostEqual(cohens_kappa(pairs), 0.0)

    def test_invalid_label_raises_error(self):
        with self.assertRaises(ValueError):
            cohens_kappa([("A", "INVALID")])

    def test_pr10_evaluation_and_disputed_reference(self):
        # 82.7% agreement scenario across A, C, N, U (717 agreed, 150 disagreed)
        pairs = (
            [("A", "A")] * 300
            + [("C", "C")] * 100
            + [("N", "N")] * 300
            + [("U", "U")] * 17
            + [("A", "C")] * 50
            + [("C", "N")] * 50
            + [("N", "U")] * 50
        )
        self.assertEqual(len(pairs), 867)

        cases = [
            AuditCase(f"c_{i}", "unseen_profiled", f"cl_{i}", ["A"] * 5, is_reference_disputed=(i < 50))
            for i in range(50)
        ] + [
            AuditCase(f"c_u_{i}", "unseen_unprofiled", f"cl_u_{i}", ["N"] * 5, is_reference_disputed=(i < 8))
            for i in range(50)
        ]

        report = evaluate_audit_estimand(cases, rater_pairs=pairs, n_bootstrap_draws=50)
        self.assertIsNotNone(report.rater_agreement)
        self.assertEqual(report.rater_agreement.total_pairs, 867)
        self.assertEqual(report.rater_agreement.exact_agreed, 717)
        self.assertEqual(report.rater_agreement.reference_dispute_count, 58)
        self.assertTrue(report.rater_agreement.passes_pr10)
        self.assertGreaterEqual(report.rater_agreement.cohens_kappa, PR10_KAPPA_THRESHOLD)


if __name__ == "__main__":
    unittest.main()
