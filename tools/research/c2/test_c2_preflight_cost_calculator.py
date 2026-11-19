"""Unit tests for C2 API Preflight Cost and Quota Calculator (T79 / L44)."""
from __future__ import annotations

import sys
import unittest
from decimal import Decimal
from pathlib import Path

# Add monolith root to sys.path
monolith_root = Path(__file__).resolve().parents[3]
if str(monolith_root) not in sys.path:
    sys.path.insert(0, str(monolith_root))

try:
    from tools.research.c2.c2_preflight_cost_calculator import (
        AUTHORIZED_BUDGET_USD,
        GLOBAL_CALL_CAP,
        READY_CASES_FOR_DISPATCH,
        C2RouteDecision,
        calculate_cost,
        compute_call_units,
        evaluate_c2_preflight,
        project_cohort,
    )
except ModuleNotFoundError:
    from c2_preflight_cost_calculator import (
        AUTHORIZED_BUDGET_USD,
        GLOBAL_CALL_CAP,
        READY_CASES_FOR_DISPATCH,
        C2RouteDecision,
        calculate_cost,
        compute_call_units,
        evaluate_c2_preflight,
        project_cohort,
    )


class TestC2PreflightCostCalculator(unittest.TestCase):
    def test_call_cap_arithmetic_full_cohort(self):
        # 10,499 count calls + 10,499 gen calls = 20,998 consumed; slack = 4,002
        consumed, slack = compute_call_units(READY_CASES_FOR_DISPATCH)
        self.assertEqual(consumed, 20998)
        self.assertEqual(slack, 4002)
        self.assertLessEqual(consumed, GLOBAL_CALL_CAP)

    def test_call_cap_arithmetic_fallback_cohort(self):
        # 10,499 count calls + 2,000 gen calls = 12,499 consumed; slack = 12,501
        consumed, slack = compute_call_units(2000)
        self.assertEqual(consumed, 12499)
        self.assertEqual(slack, 12501)
        self.assertLessEqual(consumed, GLOBAL_CALL_CAP)

    def test_call_cap_exceeded_with_prior_spent(self):
        # If 5,000 units already spent: 10,499 + 10,499 + 5,000 = 25,998 > 25,000
        consumed, slack = compute_call_units(READY_CASES_FOR_DISPATCH, already_sent_units=5000)
        self.assertEqual(consumed, 25998)
        self.assertLess(slack, 0)

    def test_cost_calculation_batch_rates(self):
        # 1,000,000 input tokens at $1.25, 1,000,000 output tokens at $5.00 = $6.25
        in_tok, out_tok, cost = calculate_cost(
            cases_count=1000,
            avg_input_tokens_per_case=1000,
            avg_output_tokens_per_case=1000,
            is_batch_api=True,
        )
        self.assertEqual(in_tok, 1_000_000)
        self.assertEqual(out_tok, 1_000_000)
        self.assertEqual(cost, Decimal("6.2500"))

    def test_cost_calculation_standard_rates(self):
        # Standard: $2.50 + $10.00 = $12.50
        in_tok, out_tok, cost = calculate_cost(
            cases_count=1000,
            avg_input_tokens_per_case=1000,
            avg_output_tokens_per_case=1000,
            is_batch_api=False,
        )
        self.assertEqual(cost, Decimal("12.5000"))

    def test_evaluate_c2_preflight_normal_case_routes_full(self):
        # 450 in, 150 out tokens per case for 10,499 cases:
        # In tokens: 4,724,550 * 1.25 / 1M = $5.9057
        # Out tokens: 1,574,850 * 5.00 / 1M = $7.8743
        # Total cost ~= $13.78 USD, well within $80 threshold (80% of $100)
        res = evaluate_c2_preflight(avg_input_tokens_per_case=450, avg_output_tokens_per_case=150)
        self.assertEqual(res.selected_route, C2RouteDecision.ROUTE_FULL_TEST_10747)
        self.assertEqual(res.live_dispatch_state, "BLOCKED_DEFAULT_OFF")
        self.assertTrue(res.full_cohort_projection.is_cost_within_80pct)
        self.assertTrue(res.full_cohort_projection.is_calls_within_25k_cap)

    def test_evaluate_c2_preflight_high_tokens_routes_fallback(self):
        # If tokens are huge so full cost > $80, but fallback cost <= $80
        # E.g. avg_input=5000, avg_output=2000 -> full cost ~= $65 + $105 = $170 > $80
        # Fallback (2,000 cases) -> cost ~= $32.50 <= $80
        res = evaluate_c2_preflight(avg_input_tokens_per_case=5000, avg_output_tokens_per_case=2000)
        self.assertEqual(res.selected_route, C2RouteDecision.ROUTE_FALLBACK_2000)
        self.assertFalse(res.full_cohort_projection.is_cost_within_80pct)
        self.assertTrue(res.fallback_cohort_projection.is_cost_within_80pct)

    def test_evaluate_c2_preflight_extreme_cost_fails_closed(self):
        # If even fallback exceeds 80% of $100
        res = evaluate_c2_preflight(avg_input_tokens_per_case=50000, avg_output_tokens_per_case=20000)
        self.assertEqual(res.selected_route, C2RouteDecision.BUDGET_EXCEEDED_FAIL_CLOSED)

    def test_evaluate_c2_preflight_call_cap_exhaustion(self):
        # If 15,000 calls already consumed, full needs 20,998 + 15,000 = 35,998; fallback needs 12,499 + 15,000 = 27,499 > 25,000
        res = evaluate_c2_preflight(already_sent_units=15000)
        self.assertEqual(res.selected_route, C2RouteDecision.CALL_CAP_EXCEEDED_FAIL_CLOSED)


if __name__ == "__main__":
    unittest.main()
