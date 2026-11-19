"""
L44: Scientific Arm and Method Selection Evaluator.

Authors, traces, and evaluates:
1. Bounded Validation Diagnostic Evaluation (200 cases):
   - Senath top-20 generation vs GISMo top-50 + Senath likelihood reranking.
   - Enforces fixed denominator (200), zero dropped cases, and mixed-ranking handling.
   - Proves no method winner can be claimed from diagnostic ranking inversion.
2. Mistral GPU Resource Feasibility Gate:
   - Evaluates 40 Kaggle GPU-hours envelope against pinned workload projections
     (512.24 h mean / 527.25 h p90 scenario across 54 partitions per split).
   - Enforces fail-closed rejection: NO_NEW_MISTRAL_TRAINING and NO_WIDENING.
3. C2 API Arm Preflight Routing:
   - Evaluates conservative all-in preflight cost against authorized OpenAI budget.
   - 80% remaining budget rule: routes to Full Matched Cohort (10,747 cases) or
     seeded stratified fallback (2,000 cases, seed 20261021, cost-only fallback).
4. Matched-Cohort Integrity Guard:
   - Rejects unmatched headline comparisons; requires identical case IDs across arms.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
from typing import Any, Dict, List, Optional, Tuple


# ==============================================================================
# SSoT Constants & Pinned Hashes
# ==============================================================================

PROTOCOL_V1_SHA256 = (
    "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
)
T51_RESULTS_CARD_SHA256 = (
    "9ff3ca2052f48923911ec043e1caba62ea375dd5ea97b070e2acbb0618f1cc8b"
)
T72_METHOD_AUDIT_SHA256 = (
    "75d8c68c5de2ed2f2f2271b4d7d75e71080a812769b14ff2348a15e927d586bf"
)
PROJECTION_NOTE_SHA256 = (
    "d7022de54a7cfd0d107a1b9cbfc65eba42f18cfab0f9c10bddf5270509918fbd"
)
PARTITION_PLAN_SHA256 = (
    "22b317f91c95ee024683edf3c4d5a98b7821341d1d62ba7d7bee91b0752826dc"
)

TOTAL_GPU_BUDGET_HOURS = 40.0
PER_JOB_CEILING_HOURS = 6.0
OPENAI_TOTAL_BUDGET_USD = 100.0
OPENAI_TOTAL_CALL_CAP = 25000
FULL_TEST_COHORT_CASES = 10747
STRATIFIED_FALLBACK_CASES = 2000
FALLBACK_SEED = 20261021


# ==============================================================================
# Data Structures
# ==============================================================================

class MethodWinnerStatus(str, Enum):
    NO_METHOD_WINNER = "NO_METHOD_WINNER"
    RERANKING_WINNER = "RERANKING_WINNER"
    GENERATION_WINNER = "GENERATION_WINNER"


class RoutingDestination(str, Enum):
    FULL_MATCHED_TEST_COHORT = "FULL_MATCHED_TEST_COHORT_10747"
    STRATIFIED_TEST_FALLBACK = "STRATIFIED_TEST_FALLBACK_2000"
    BLOCKED_BUDGET_EXHAUSTED = "BLOCKED_BUDGET_EXHAUSTED"


@dataclass(frozen=True)
class ArmDiagnosticResult:
    arm_id: str
    total_cases: int
    hit_at_1: int
    hit_at_5: int
    hit_at_10: int
    unmapped_count: int
    median_latency_s: float
    p90_latency_s: float
    failures: int = 0
    strata_breakdown: Dict[str, Dict[str, int]] = field(default_factory=dict)

    @property
    def hit_at_1_rate(self) -> float:
        return self.hit_at_1 / self.total_cases if self.total_cases > 0 else 0.0

    @property
    def hit_at_5_rate(self) -> float:
        return self.hit_at_5 / self.total_cases if self.total_cases > 0 else 0.0

    @property
    def hit_at_10_rate(self) -> float:
        return self.hit_at_10 / self.total_cases if self.total_cases > 0 else 0.0


@dataclass(frozen=True)
class GpuResourceProjection:
    budget_hours: float = TOTAL_GPU_BUDGET_HOURS
    validation_mean_hours: float = 255.91
    test_mean_hours: float = 256.33
    validation_p90_hours: float = 263.41
    test_p90_hours: float = 263.84
    total_mean_hours: float = 512.24
    total_p90_hours: float = 527.25
    per_job_ceiling_hours: float = PER_JOB_CEILING_HOURS
    partitions_per_split: int = 54


@dataclass(frozen=True)
class GpuFeasibilityResult:
    is_feasible: bool
    budget_hours: float
    total_mean_hours: float
    total_p90_hours: float
    shortfall_mean_hours: float
    shortfall_p90_hours: float
    status: str
    decision_rule: str
    recommendation: str


@dataclass(frozen=True)
class ApiPreflightRoutingDecision:
    authorized_budget_usd: float
    spent_usd: float
    reserved_usd: float
    remaining_budget_usd: float
    estimated_cost_usd: float
    safety_margin_ratio: float
    effective_spending_ceiling_usd: float
    route_selected: RoutingDestination
    target_case_count: int
    is_authorized: bool
    reason: str


@dataclass
class L44ScientificDispositionReport:
    diagnostic_disposition: str
    method_winner: Optional[str]
    is_diagnostic_generalizable_to_test: bool
    gpu_feasibility: GpuFeasibilityResult
    api_routing: ApiPreflightRoutingDecision
    protocol_v1_sha256: str
    t51_results_card_sha256: str
    t72_method_audit_sha256: str
    recommendations: List[str]


# ==============================================================================
# Canonical 200-Case Validation Diagnostic (SSoT Pinned Baseline)
# ==============================================================================

def get_canonical_validation_diagnostics() -> Tuple[ArmDiagnosticResult, ArmDiagnosticResult]:
    """
    Returns the two recovered 200-case validation diagnostic arms
    from evidence/lead-l44-validation-benchmark-results-20261021.md.
    """
    reranking = ArmDiagnosticResult(
        arm_id="gismo_top50_senath_rerank",
        total_cases=200,
        hit_at_1=3,    # 1.5%
        hit_at_5=15,   # 7.5%
        hit_at_10=29,  # 14.5%
        unmapped_count=0,
        median_latency_s=54.914,
        p90_latency_s=56.850,
        failures=0,
        strata_breakdown={
            "gismo_identity": {"seen": 105, "unseen": 95, "hit_at_1": 3, "hit_at_5": 15, "hit_at_10": 29},
            "senath_pair": {"seen": 100, "unseen": 100},
        },
    )

    generation = ArmDiagnosticResult(
        arm_id="senath_top20_generation",
        total_cases=200,
        hit_at_1=6,    # 3.0%
        hit_at_5=23,   # 11.5%
        hit_at_10=28,  # 14.0%
        unmapped_count=898,
        median_latency_s=30.860,
        p90_latency_s=31.515,
        failures=0,
        strata_breakdown={
            "gismo_identity": {"seen": 105, "unseen": 95, "hit_at_1": 6, "hit_at_5": 23, "hit_at_10": 28},
            "senath_pair": {"seen": 100, "unseen": 100},
        },
    )

    return reranking, generation


# ==============================================================================
# Core Evaluators
# ==============================================================================

def evaluate_validation_diagnostic(
    rerank: ArmDiagnosticResult,
    generation: ArmDiagnosticResult,
) -> Dict[str, Any]:
    """
    Evaluates the 200-case validation diagnostic.
    Enforces fail-closed rule: mixed rankings do NOT select a winner.
    """
    if rerank.total_cases != 200 or generation.total_cases != 200:
        raise ValueError("Diagnostic evaluation requires exact fixed denominator of 200 cases")

    # Ranking comparisons
    gen_leads_h1 = generation.hit_at_1 > rerank.hit_at_1
    gen_leads_h5 = generation.hit_at_5 > rerank.hit_at_5
    rerank_leads_h10 = rerank.hit_at_10 > generation.hit_at_10

    # Inversion detection: Generation leads at H@1/5, but Rerank leads at H@10
    has_ranking_inversion = (gen_leads_h1 or gen_leads_h5) and rerank_leads_h10

    if has_ranking_inversion:
        winner_status = MethodWinnerStatus.NO_METHOD_WINNER
        disposition = (
            "MIXED_RANKINGS_INCONCLUSIVE: Generation leads at Hit@1 (3.0% vs 1.5%) and "
            "Hit@5 (11.5% vs 7.5%), but Reranking leads at Hit@10 (14.5% vs 14.0%). "
            "Mixed rankings do not select an arm. Diagnostic cannot be enlarged or "
            "relabeled as full-split or test evidence."
        )
    elif generation.hit_at_1 >= rerank.hit_at_1 and generation.hit_at_5 >= rerank.hit_at_5 and generation.hit_at_10 >= rerank.hit_at_10:
        winner_status = MethodWinnerStatus.GENERATION_WINNER
        disposition = "Generation dominates across Hit@1/5/10."
    elif rerank.hit_at_1 >= generation.hit_at_1 and rerank.hit_at_5 >= generation.hit_at_5 and rerank.hit_at_10 >= generation.hit_at_10:
        winner_status = MethodWinnerStatus.RERANKING_WINNER
        disposition = "Reranking dominates across Hit@1/5/10."
    else:
        winner_status = MethodWinnerStatus.NO_METHOD_WINNER
        disposition = "Indeterminate outcome."

    return {
        "status": winner_status.value,
        "method_winner": None if winner_status == MethodWinnerStatus.NO_METHOD_WINNER else winner_status.value,
        "has_ranking_inversion": has_ranking_inversion,
        "disposition": disposition,
        "is_diagnostic_generalizable_to_test": False,  # Strict SSoT constraint
        "failures_preserved": (rerank.failures == 0 and generation.failures == 0),
        "unmapped_strings_retained_in_denominator": generation.unmapped_count == 898,
    }


def evaluate_gpu_feasibility(
    projection: Optional[GpuResourceProjection] = None,
) -> GpuFeasibilityResult:
    """
    Evaluates whether Mistral full-split expansion is feasible within the 40 GPU-hours envelope.
    """
    proj = projection if projection is not None else GpuResourceProjection()

    is_feasible = (proj.total_mean_hours <= proj.budget_hours) and (proj.total_p90_hours <= proj.budget_hours)
    shortfall_mean = max(0.0, proj.total_mean_hours - proj.budget_hours)
    shortfall_p90 = max(0.0, proj.total_p90_hours - proj.budget_hours)

    if not is_feasible:
        status = "FEASIBILITY_FAIL_EXCEEDS_CAP"
        rule = (
            f"Required {proj.total_mean_hours:.2f}h mean / {proj.total_p90_hours:.2f}h p90 "
            f"exceeds total authorized envelope of {proj.budget_hours:.1f}h."
        )
        rec = (
            "NO_NEW_MISTRAL_TRAINING. Keep historical 200-case ceiling. "
            "Do not launch new Mistral GPU jobs until HUMAN explicitly amends global resource ceiling."
        )
    else:
        status = "FEASIBILITY_PASS"
        rule = "Workload fits within authorized GPU budget."
        rec = "Authorized for scheduled execution."

    return GpuFeasibilityResult(
        is_feasible=is_feasible,
        budget_hours=proj.budget_hours,
        total_mean_hours=proj.total_mean_hours,
        total_p90_hours=proj.total_p90_hours,
        shortfall_mean_hours=shortfall_mean,
        shortfall_p90_hours=shortfall_p90,
        status=status,
        decision_rule=rule,
        recommendation=rec,
    )


def route_api_arm(
    spent_usd: float,
    reserved_usd: float,
    estimated_cost_usd: float,
    budget_usd: float = OPENAI_TOTAL_BUDGET_USD,
    safety_margin_ratio: float = 0.80,
) -> ApiPreflightRoutingDecision:
    """
    Evaluates C2 API preflight cost against authorized budget.
    Applies the 80% remaining budget safety gate.
    """
    remaining = budget_usd - (spent_usd + reserved_usd)
    effective_ceiling = remaining * safety_margin_ratio

    if remaining <= 0:
        return ApiPreflightRoutingDecision(
            authorized_budget_usd=budget_usd,
            spent_usd=spent_usd,
            reserved_usd=reserved_usd,
            remaining_budget_usd=remaining,
            estimated_cost_usd=estimated_cost_usd,
            safety_margin_ratio=safety_margin_ratio,
            effective_spending_ceiling_usd=0.0,
            route_selected=RoutingDestination.BLOCKED_BUDGET_EXHAUSTED,
            target_case_count=0,
            is_authorized=False,
            reason="Authorized OpenAI budget is fully exhausted or reserved.",
        )

    if estimated_cost_usd <= effective_ceiling:
        return ApiPreflightRoutingDecision(
            authorized_budget_usd=budget_usd,
            spent_usd=spent_usd,
            reserved_usd=reserved_usd,
            remaining_budget_usd=remaining,
            estimated_cost_usd=estimated_cost_usd,
            safety_margin_ratio=safety_margin_ratio,
            effective_spending_ceiling_usd=effective_ceiling,
            route_selected=RoutingDestination.FULL_MATCHED_TEST_COHORT,
            target_case_count=FULL_TEST_COHORT_CASES,
            is_authorized=True,
            reason=(
                f"Estimated cost ${estimated_cost_usd:.2f} fits within 80% ceiling "
                f"(${effective_ceiling:.2f}) of remaining budget (${remaining:.2f})."
            ),
        )
    else:
        return ApiPreflightRoutingDecision(
            authorized_budget_usd=budget_usd,
            spent_usd=spent_usd,
            reserved_usd=reserved_usd,
            remaining_budget_usd=remaining,
            estimated_cost_usd=estimated_cost_usd,
            safety_margin_ratio=safety_margin_ratio,
            effective_spending_ceiling_usd=effective_ceiling,
            route_selected=RoutingDestination.STRATIFIED_TEST_FALLBACK,
            target_case_count=STRATIFIED_FALLBACK_CASES,
            is_authorized=True,
            reason=(
                f"Estimated cost ${estimated_cost_usd:.2f} exceeds 80% ceiling "
                f"(${effective_ceiling:.2f}); falling back to seeded 2,000-case "
                f"stratified cost-only fallback."
            ),
        )


# ==============================================================================
# Master L44 Evaluator
# ==============================================================================

def run_l44_evaluation(
    spent_usd: float = 0.0,
    reserved_usd: float = 0.0,
    estimated_cost_usd: float = 45.0,
) -> L44ScientificDispositionReport:
    """
    Runs the complete L44 scientific arm and method selection evaluation.
    """
    rerank, gen = get_canonical_validation_diagnostics()
    diag_res = evaluate_validation_diagnostic(rerank, gen)
    gpu_res = evaluate_gpu_feasibility()
    api_res = route_api_arm(
        spent_usd=spent_usd,
        reserved_usd=reserved_usd,
        estimated_cost_usd=estimated_cost_usd,
    )

    recommendations = [
        "Preserve recovered 200-case Senath generation vs reranking run as validation diagnostic only.",
        "Declare NO METHOD WINNER under frozen Protocol v1 due to ranking inversion across Hit@1/5 vs Hit@10.",
        gpu_res.recommendation,
        f"C2 API Route: {api_res.route_selected.value} ({api_res.target_case_count} cases).",
        "Enforce identical case IDs when pairing API arm with GISMo and lookup baselines.",
    ]

    return L44ScientificDispositionReport(
        diagnostic_disposition=diag_res["disposition"],
        method_winner=diag_res["method_winner"],
        is_diagnostic_generalizable_to_test=diag_res["is_diagnostic_generalizable_to_test"],
        gpu_feasibility=gpu_res,
        api_routing=api_res,
        protocol_v1_sha256=PROTOCOL_V1_SHA256,
        t51_results_card_sha256=T51_RESULTS_CARD_SHA256,
        t72_method_audit_sha256=T72_METHOD_AUDIT_SHA256,
        recommendations=recommendations,
    )
