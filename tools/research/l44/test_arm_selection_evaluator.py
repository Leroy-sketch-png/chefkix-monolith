"""
Unit tests for L44: Scientific Arm and Method Selection Evaluator.

Verifies:
1. Canonical validation diagnostic numbers (Rerank 3/15/29 vs Gen 6/23/28).
2. Diagnostic evaluation & ranking inversion detection: fail-closed no method winner.
3. Denominator integrity: strict 200-case requirement and unmapped string retention.
4. Mistral GPU resource envelope feasibility: fail-closed 40-hour cap rejection.
5. C2 API arm preflight routing under the 80% remaining budget safety gate.
6. Master L44 disposition report and pinned SSoT hashes.
"""

import pytest

from tools.research.l44.arm_selection_evaluator import (
    FALLBACK_SEED,
    FULL_TEST_COHORT_CASES,
    OPENAI_TOTAL_BUDGET_USD,
    PROTOCOL_V1_SHA256,
    STRATIFIED_FALLBACK_CASES,
    T51_RESULTS_CARD_SHA256,
    T72_METHOD_AUDIT_SHA256,
    TOTAL_GPU_BUDGET_HOURS,
    ArmDiagnosticResult,
    GpuResourceProjection,
    MethodWinnerStatus,
    RoutingDestination,
    evaluate_gpu_feasibility,
    evaluate_validation_diagnostic,
    get_canonical_validation_diagnostics,
    route_api_arm,
    run_l44_evaluation,
)


class TestCanonicalValidationDiagnostics:
    def test_canonical_rerank_numbers(self):
        rerank, _ = get_canonical_validation_diagnostics()
        assert rerank.total_cases == 200
        assert rerank.hit_at_1 == 3
        assert rerank.hit_at_5 == 15
        assert rerank.hit_at_10 == 29
        assert rerank.unmapped_count == 0
        assert rerank.median_latency_s == 54.914
        assert rerank.p90_latency_s == 56.850
        assert rerank.failures == 0

    def test_canonical_generation_numbers(self):
        _, gen = get_canonical_validation_diagnostics()
        assert gen.total_cases == 200
        assert gen.hit_at_1 == 6
        assert gen.hit_at_5 == 23
        assert gen.hit_at_10 == 28
        assert gen.unmapped_count == 898
        assert gen.median_latency_s == 30.860
        assert gen.p90_latency_s == 31.515
        assert gen.failures == 0


class TestDiagnosticEvaluation:
    def test_ranking_inversion_yields_no_method_winner(self):
        rerank, gen = get_canonical_validation_diagnostics()
        res = evaluate_validation_diagnostic(rerank, gen)

        assert res["status"] == MethodWinnerStatus.NO_METHOD_WINNER.value
        assert res["method_winner"] is None
        assert res["has_ranking_inversion"] is True
        assert "MIXED_RANKINGS_INCONCLUSIVE" in res["disposition"]
        assert res["is_diagnostic_generalizable_to_test"] is False
        assert res["unmapped_strings_retained_in_denominator"] is True

    def test_invalid_denominator_raises_error(self):
        bad_arm = ArmDiagnosticResult("bad", 150, 5, 10, 15, 0, 1.0, 2.0)
        _, gen = get_canonical_validation_diagnostics()
        with pytest.raises(ValueError, match="exact fixed denominator of 200 cases"):
            evaluate_validation_diagnostic(bad_arm, gen)

    def test_hypothetical_generation_dominance(self):
        hypo_gen = ArmDiagnosticResult("gen", 200, 10, 30, 40, 0, 1.0, 2.0)
        hypo_rerank = ArmDiagnosticResult("rerank", 200, 5, 20, 30, 0, 1.0, 2.0)
        res = evaluate_validation_diagnostic(hypo_rerank, hypo_gen)
        assert res["status"] == MethodWinnerStatus.GENERATION_WINNER.value
        assert res["method_winner"] == MethodWinnerStatus.GENERATION_WINNER.value

    def test_hypothetical_reranking_dominance(self):
        hypo_rerank = ArmDiagnosticResult("rerank", 200, 10, 30, 40, 0, 1.0, 2.0)
        hypo_gen = ArmDiagnosticResult("gen", 200, 5, 20, 30, 0, 1.0, 2.0)
        res = evaluate_validation_diagnostic(hypo_rerank, hypo_gen)
        assert res["status"] == MethodWinnerStatus.RERANKING_WINNER.value
        assert res["method_winner"] == MethodWinnerStatus.RERANKING_WINNER.value


class TestGpuFeasibilityGate:
    def test_canonical_projection_fails_feasibility(self):
        res = evaluate_gpu_feasibility()
        assert res.is_feasible is False
        assert res.budget_hours == TOTAL_GPU_BUDGET_HOURS
        assert res.budget_hours == 40.0
        assert res.total_mean_hours == 512.24
        assert res.total_p90_hours == 527.25
        assert res.shortfall_mean_hours == pytest.approx(472.24)
        assert res.shortfall_p90_hours == pytest.approx(487.25)
        assert res.status == "FEASIBILITY_FAIL_EXCEEDS_CAP"
        assert "NO_NEW_MISTRAL_TRAINING" in res.recommendation

    def test_hypothetical_small_workload_passes(self):
        small_proj = GpuResourceProjection(
            budget_hours=40.0,
            total_mean_hours=20.0,
            total_p90_hours=25.0,
        )
        res = evaluate_gpu_feasibility(small_proj)
        assert res.is_feasible is True
        assert res.status == "FEASIBILITY_PASS"


class TestApiPreflightRouting:
    def test_full_test_cohort_selected_when_within_80_percent_ceiling(self):
        # Budget $100, spent $0, reserved $0 -> remaining $100 -> 80% ceiling is $80
        # Estimated cost $45 <= $80 -> selects Full Matched Cohort (10,747 cases)
        decision = route_api_arm(
            spent_usd=0.0,
            reserved_usd=0.0,
            estimated_cost_usd=45.0,
            budget_usd=100.0,
        )
        assert decision.is_authorized is True
        assert decision.route_selected == RoutingDestination.FULL_MATCHED_TEST_COHORT
        assert decision.target_case_count == FULL_TEST_COHORT_CASES
        assert decision.target_case_count == 10747

    def test_stratified_fallback_selected_when_exceeding_ceiling(self):
        # Budget $100, spent $20, reserved $10 -> remaining $70 -> 80% ceiling is $56
        # Estimated cost $65 > $56 -> selects Stratified Fallback (2,000 cases)
        decision = route_api_arm(
            spent_usd=20.0,
            reserved_usd=10.0,
            estimated_cost_usd=65.0,
            budget_usd=100.0,
        )
        assert decision.is_authorized is True
        assert decision.route_selected == RoutingDestination.STRATIFIED_TEST_FALLBACK
        assert decision.target_case_count == STRATIFIED_FALLBACK_CASES
        assert decision.target_case_count == 2000

    def test_blocked_when_budget_exhausted(self):
        # Budget $100, spent $80, reserved $20 -> remaining $0
        decision = route_api_arm(
            spent_usd=80.0,
            reserved_usd=20.0,
            estimated_cost_usd=10.0,
            budget_usd=100.0,
        )
        assert decision.is_authorized is False
        assert decision.route_selected == RoutingDestination.BLOCKED_BUDGET_EXHAUSTED
        assert decision.target_case_count == 0


class TestMasterL44Report:
    def test_full_l44_evaluation_execution(self):
        report = run_l44_evaluation(spent_usd=10.0, reserved_usd=5.0, estimated_cost_usd=40.0)

        # Hashes match SSoT
        assert report.protocol_v1_sha256 == PROTOCOL_V1_SHA256
        assert report.t51_results_card_sha256 == T51_RESULTS_CARD_SHA256
        assert report.t72_method_audit_sha256 == T72_METHOD_AUDIT_SHA256

        # Diagnostic conclusions
        assert report.method_winner is None
        assert report.is_diagnostic_generalizable_to_test is False

        # GPU feasibility & API routing
        assert report.gpu_feasibility.is_feasible is False
        assert report.api_routing.is_authorized is True
        assert report.api_routing.route_selected == RoutingDestination.FULL_MATCHED_TEST_COHORT

        # Recommendations contain key rules
        rec_text = " ".join(report.recommendations)
        assert "NO METHOD WINNER" in rec_text
        assert "NO_NEW_MISTRAL_TRAINING" in rec_text
        assert "identical case IDs" in rec_text
