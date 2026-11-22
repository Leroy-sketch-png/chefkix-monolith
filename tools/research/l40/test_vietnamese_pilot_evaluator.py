"""Unit tests for L40 Vietnamese Pilot Evaluation and Estimand Engine."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

# Add monolith root to sys.path
monolith_root = Path(__file__).resolve().parents[3]
if str(monolith_root) not in sys.path:
    sys.path.insert(0, str(monolith_root))

try:
    from tools.research.l40.vietnamese_pilot_evaluator import (
        CONSTRAINT_COHORT_N,
        CULINARY_COHORT_N,
        AgreementTier,
        CaseOutcome,
        EvaluatedCaseSlate,
        classify_agreement,
        classify_case_outcome,
        compute_wilson_interval,
        evaluate_cohort,
        evaluate_filter_transitions,
    )
except ModuleNotFoundError:
    from vietnamese_pilot_evaluator import (
        CONSTRAINT_COHORT_N,
        CULINARY_COHORT_N,
        AgreementTier,
        CaseOutcome,
        EvaluatedCaseSlate,
        classify_agreement,
        classify_case_outcome,
        compute_wilson_interval,
        evaluate_cohort,
        evaluate_filter_transitions,
    )


class TestVietnamesePilotEvaluator(unittest.TestCase):
    def test_classify_case_outcome_v1_success(self):
        # Slate with at least one A
        outcome, ac_succ = classify_case_outcome(["C", "N", "A", "N", "C"])
        self.assertEqual(outcome, CaseOutcome.V_ONE_SUCCESS)
        self.assertTrue(ac_succ)

    def test_classify_case_outcome_v0_failure(self):
        # All C or N, no A, no U
        outcome, ac_succ = classify_case_outcome(["C", "N", "N", "C", "N"])
        self.assertEqual(outcome, CaseOutcome.V_ZERO_FAILURE)
        self.assertTrue(ac_succ)  # contains C

        # All N
        outcome_all_n, ac_succ_n = classify_case_outcome(["N", "N", "N", "N", "N"])
        self.assertEqual(outcome_all_n, CaseOutcome.V_ZERO_FAILURE)
        self.assertFalse(ac_succ_n)

    def test_classify_case_outcome_v_unknown(self):
        # Contains U, no A
        outcome, ac_succ = classify_case_outcome(["C", "U", "N", "C", "N"])
        self.assertEqual(outcome, CaseOutcome.V_UNKNOWN)
        self.assertTrue(ac_succ)

        outcome2, ac_succ2 = classify_case_outcome(["N", "U", "N", "N", "N"])
        self.assertEqual(outcome2, CaseOutcome.V_UNKNOWN)
        self.assertFalse(ac_succ2)

    def test_incomplete_no_a_is_unknown_and_gold_is_outside_top_five(self):
        for labels in ([], ["C", "N", "N", "N"]):
            with self.subTest(labels=labels):
                self.assertEqual(classify_case_outcome(labels)[0], CaseOutcome.V_UNKNOWN)
        with self.assertRaisesRegex(ValueError, "gold"):
            classify_case_outcome(["N"] * 5 + ["A"])

    def test_classify_case_outcome_invalid_label(self):
        with self.assertRaises(ValueError):
            classify_case_outcome(["A", "C", "INVALID", "N", "U"])

    def test_wilson_interval_bounds(self):
        w = compute_wilson_interval(successes=30, total=60)
        self.assertAlmostEqual(w.estimate, 0.5)
        self.assertLess(w.lower, 0.5)
        self.assertGreater(w.upper, 0.5)
        self.assertGreater(w.lower, 0.0)
        self.assertLess(w.upper, 1.0)

    def test_evaluate_culinary_cohort_bounds(self):
        # 60 culinary cases: 20 V=1, 30 V=0, 10 V=unknown, 50 A+C
        cases = []
        for i in range(20):
            cases.append(EvaluatedCaseSlate(f"c_{i}", "culinary", ("A", "N", "N", "N", "N"), CaseOutcome.V_ONE_SUCCESS, True))
        for i in range(30):
            cases.append(EvaluatedCaseSlate(f"c_{i+20}", "culinary", ("C", "N", "N", "N", "N"), CaseOutcome.V_ZERO_FAILURE, True))
        for i in range(10):
            cases.append(EvaluatedCaseSlate(f"c_{i+50}", "culinary", ("U", "N", "N", "N", "N"), CaseOutcome.V_UNKNOWN, False))

        summary = evaluate_cohort("culinary", cases, CULINARY_COHORT_N)
        self.assertEqual(summary.v1_success_count, 20)
        self.assertEqual(summary.v0_failure_count, 30)
        self.assertEqual(summary.v_unknown_count, 10)
        self.assertAlmostEqual(summary.strict_a_lower_bound, 20 / 60, places=5)
        self.assertAlmostEqual(summary.strict_a_upper_bound, 30 / 60, places=5)
        self.assertAlmostEqual(summary.ac_sensitivity_rate, 50 / 60, places=5)

    def test_evaluate_filter_transitions_and_abstentions(self):
        raw_cases = [
            EvaluatedCaseSlate(f"rc_{i}", "constraint", ("A", "C", "N", "N", "N"), CaseOutcome.V_ONE_SUCCESS, True)
            for i in range(40)
        ]
        # Suppose 5 cases have 0 surviving candidates (abstentions)
        counts = [0] * 5 + [3] * 35
        filtered_cases = [None] * 5 + [
            EvaluatedCaseSlate(f"rc_{i}", "constraint", ("A", "C", "N"), CaseOutcome.V_ONE_SUCCESS, True)
            for i in range(5, 40)
        ]

        res = evaluate_filter_transitions(
            raw_cases=raw_cases,
            filtered_candidate_counts=counts,
            filtered_cases=filtered_cases,
            violations_removed=12,
            unknowns_withheld=8,
        )
        self.assertEqual(res.total_constraint_cases, 40)
        self.assertEqual(res.raw_v1_count, 40)
        self.assertEqual(res.filtered_v1_count, 35)
        self.assertEqual(res.abstentions_count, 5)
        self.assertTrue(res.is_abstention_never_labeled_safe)
        self.assertEqual(res.case_transitions, {
            "V_ONE_SUCCESS->ABSTENTION_EMPTY_POOL": 5,
            "V_ONE_SUCCESS->V_ONE_SUCCESS": 35,
        })

    def test_filtered_slate_must_match_case_and_survivors(self):
        raw = EvaluatedCaseSlate("r", "constraint", ("N",) * 5, CaseOutcome.V_ZERO_FAILURE, False)
        wrong_case = EvaluatedCaseSlate("s", "constraint", ("N",), CaseOutcome.V_UNKNOWN, False)
        new_label = EvaluatedCaseSlate("r", "constraint", ("A",), CaseOutcome.V_ONE_SUCCESS, True)
        for count, filtered in ((1, wrong_case), (1, new_label), (1, None)):
            with self.subTest(count=count, filtered=filtered), self.assertRaises(ValueError):
                evaluate_filter_transitions([raw], [count], [filtered], 0, 0)

    def test_cohort_rejects_duplicate_rows_and_unjustified_failure(self):
        raw = EvaluatedCaseSlate("r", "culinary", ("N",) * 5, CaseOutcome.V_ZERO_FAILURE, False)
        with self.assertRaisesRegex(ValueError, "unique case IDs"):
            evaluate_cohort("culinary", [raw, raw], 2)
        incomplete = EvaluatedCaseSlate("i", "culinary", ("N",), CaseOutcome.V_ZERO_FAILURE, False)
        with self.assertRaisesRegex(ValueError, "outcome disagrees"):
            evaluate_cohort("culinary", [incomplete], 1)

    def test_agreement_classification_tiers(self):
        # Tier 1: bootstrap lower >= 0.60 -> RELIABLE
        self.assertEqual(
            classify_agreement(nominal_kappa=0.75, bootstrap_lower_95=0.62),
            AgreementTier.RELIABLE_AGREEMENT,
        )
        # Tier 2: nominal >= 0.50 but lower < 0.60 -> EXPLORATORY
        self.assertEqual(
            classify_agreement(nominal_kappa=0.55, bootstrap_lower_95=0.48),
            AgreementTier.EXPLORATORY_AGREEMENT,
        )
        # Tier 3: nominal < 0.50 -> INSUFFICIENT
        self.assertEqual(
            classify_agreement(nominal_kappa=0.42, bootstrap_lower_95=0.65),
            AgreementTier.INSUFFICIENT_AGREEMENT,
        )

    def test_unavailable_interval_cannot_establish_reliability(self):
        self.assertEqual(classify_agreement(0.70, None), AgreementTier.EXPLORATORY_AGREEMENT)
        with self.assertRaises(ValueError):
            classify_agreement(float("nan"), None)


if __name__ == "__main__":
    unittest.main()
