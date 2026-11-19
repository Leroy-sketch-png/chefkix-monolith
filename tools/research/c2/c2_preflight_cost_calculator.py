"""C2 API Preflight Cost and Quota Calculator (T79 / L44).

Implements deterministic cost projection, Protocol-v1 §6 25,000-call quota
verification, and budget ceiling routing for the frozen GPT-4o Batch arm
under BACKLOG_LEAD.md L44/T79.
"""
from __future__ import annotations

import dataclasses
from decimal import Decimal
from enum import Enum
from typing import Mapping

MODEL_ID = "gpt-4o-2024-08-06"
GLOBAL_CALL_CAP = 25000
AUTHORIZED_BUDGET_USD = Decimal("100.00")
BUDGET_CEILING_PERCENT = Decimal("0.80")  # 80% rule

FULL_TEST_COHORT_CASES = 10747
READY_CASES_FOR_DISPATCH = 10499
PREPARATION_ERRORS_RETAINED = 248
FALLBACK_STRATIFIED_CASES = 2000

# Batch API rates (50% discount off standard rates: $2.50/$10.00 -> $1.25/$5.00 per 1M tokens)
BATCH_INPUT_RATE_PER_1M = Decimal("1.25")
BATCH_OUTPUT_RATE_PER_1M = Decimal("5.00")
STANDARD_INPUT_RATE_PER_1M = Decimal("2.50")
STANDARD_OUTPUT_RATE_PER_1M = Decimal("10.00")


class C2RouteDecision(str, Enum):
    ROUTE_FULL_TEST_10747 = "ROUTE_FULL_TEST_10747"
    ROUTE_FALLBACK_2000 = "ROUTE_FALLBACK_2000"
    BUDGET_EXCEEDED_FAIL_CLOSED = "BUDGET_EXCEEDED_FAIL_CLOSED"
    CALL_CAP_EXCEEDED_FAIL_CLOSED = "CALL_CAP_EXCEEDED_FAIL_CLOSED"


@dataclasses.dataclass(frozen=True)
class PreflightCostProjection:
    target_cases: int
    count_calls_required: int
    generation_calls_required: int
    total_calls_consumed: int
    calls_remaining_slack: int
    estimated_input_tokens: int
    estimated_output_tokens: int
    estimated_cost_usd: Decimal
    budget_threshold_usd: Decimal
    is_cost_within_80pct: bool
    is_calls_within_25k_cap: bool


@dataclasses.dataclass(frozen=True)
class C2PreflightResult:
    model: str
    authorized_budget_usd: Decimal
    max_allowable_cost_usd: Decimal  # 80% of authorized budget
    full_cohort_projection: PreflightCostProjection
    fallback_cohort_projection: PreflightCostProjection
    selected_route: C2RouteDecision
    live_dispatch_state: str  # Strictly BLOCKED
    rationale: str


def compute_call_units(
    target_generation_cases: int,
    count_preflight_cases: int = READY_CASES_FOR_DISPATCH,
    already_sent_units: int = 0,
    reserved_uncertain_units: int = 0,
) -> tuple[int, int]:
    """Calculate total call units and available slack under Protocol v1 §6.

    Every HTTP operation (including count calls) consumes one cap unit.
    """
    total_consumed = count_preflight_cases + target_generation_cases + already_sent_units + reserved_uncertain_units
    slack = GLOBAL_CALL_CAP - total_consumed
    return total_consumed, slack


def calculate_cost(
    cases_count: int,
    avg_input_tokens_per_case: int,
    avg_output_tokens_per_case: int,
    is_batch_api: bool = True,
) -> tuple[int, int, Decimal]:
    """Calculate total tokens and dollar cost."""
    total_input_tokens = cases_count * avg_input_tokens_per_case
    total_output_tokens = cases_count * avg_output_tokens_per_case

    in_rate = BATCH_INPUT_RATE_PER_1M if is_batch_api else STANDARD_INPUT_RATE_PER_1M
    out_rate = BATCH_OUTPUT_RATE_PER_1M if is_batch_api else STANDARD_OUTPUT_RATE_PER_1M

    input_cost = (Decimal(total_input_tokens) / Decimal(1_000_000)) * in_rate
    output_cost = (Decimal(total_output_tokens) / Decimal(1_000_000)) * out_rate
    total_cost = (input_cost + output_cost).quantize(Decimal("0.0001"))

    return total_input_tokens, total_output_tokens, total_cost


def project_cohort(
    cases: int,
    avg_input_tokens: int,
    avg_output_tokens: int,
    budget_threshold: Decimal,
    already_sent_units: int = 0,
    reserved_uncertain_units: int = 0,
    is_batch_api: bool = True,
) -> PreflightCostProjection:
    """Project quota consumption and dollar cost for a specific cohort size."""
    consumed_calls, slack = compute_call_units(
        target_generation_cases=cases,
        count_preflight_cases=READY_CASES_FOR_DISPATCH,
        already_sent_units=already_sent_units,
        reserved_uncertain_units=reserved_uncertain_units,
    )
    in_tok, out_tok, cost = calculate_cost(
        cases_count=cases,
        avg_input_tokens_per_case=avg_input_tokens,
        avg_output_tokens_per_case=avg_output_tokens,
        is_batch_api=is_batch_api,
    )
    is_cost_ok = cost <= budget_threshold
    is_calls_ok = consumed_calls <= GLOBAL_CALL_CAP

    return PreflightCostProjection(
        target_cases=cases,
        count_calls_required=READY_CASES_FOR_DISPATCH,
        generation_calls_required=cases,
        total_calls_consumed=consumed_calls,
        calls_remaining_slack=slack,
        estimated_input_tokens=in_tok,
        estimated_output_tokens=out_tok,
        estimated_cost_usd=cost,
        budget_threshold_usd=budget_threshold,
        is_cost_within_80pct=is_cost_ok,
        is_calls_within_25k_cap=is_calls_ok,
    )


def evaluate_c2_preflight(
    avg_input_tokens_per_case: int = 450,
    avg_output_tokens_per_case: int = 150,
    authorized_budget_usd: Decimal = AUTHORIZED_BUDGET_USD,
    already_sent_units: int = 0,
    reserved_uncertain_units: int = 0,
    is_batch_api: bool = True,
) -> C2PreflightResult:
    """Evaluate full preflight and route selection under Lead L44/T79 rules."""
    max_allowable_cost = (authorized_budget_usd * BUDGET_CEILING_PERCENT).quantize(Decimal("0.01"))

    proj_full = project_cohort(
        cases=READY_CASES_FOR_DISPATCH,
        avg_input_tokens=avg_input_tokens_per_case,
        avg_output_tokens=avg_output_tokens_per_case,
        budget_threshold=max_allowable_cost,
        already_sent_units=already_sent_units,
        reserved_uncertain_units=reserved_uncertain_units,
        is_batch_api=is_batch_api,
    )

    proj_fallback = project_cohort(
        cases=FALLBACK_STRATIFIED_CASES,
        avg_input_tokens=avg_input_tokens_per_case,
        avg_output_tokens=avg_output_tokens_per_case,
        budget_threshold=max_allowable_cost,
        already_sent_units=already_sent_units,
        reserved_uncertain_units=reserved_uncertain_units,
        is_batch_api=is_batch_api,
    )

    # Decision logic
    if not proj_full.is_calls_within_25k_cap and not proj_fallback.is_calls_within_25k_cap:
        route = C2RouteDecision.CALL_CAP_EXCEEDED_FAIL_CLOSED
        rationale = f"Both cohorts exceed the {GLOBAL_CALL_CAP} total call quota under Protocol v1 §6."
    elif proj_full.is_cost_within_80pct and proj_full.is_calls_within_25k_cap:
        route = C2RouteDecision.ROUTE_FULL_TEST_10747
        rationale = (
            f"Full matched test cohort ({READY_CASES_FOR_DISPATCH} cases) estimated at "
            f"${proj_full.estimated_cost_usd} USD (<= ${max_allowable_cost} USD 80% ceiling) "
            f"and {proj_full.total_calls_consumed} calls (<= {GLOBAL_CALL_CAP} cap)."
        )
    elif proj_fallback.is_cost_within_80pct and proj_fallback.is_calls_within_25k_cap:
        route = C2RouteDecision.ROUTE_FALLBACK_2000
        rationale = (
            f"Full cohort cost (${proj_full.estimated_cost_usd}) or calls ({proj_full.total_calls_consumed}) "
            f"exceeded threshold; routed to Protocol v1 seeded 2,000-case stratified test fallback "
            f"(${proj_fallback.estimated_cost_usd} USD, {proj_fallback.total_calls_consumed} calls)."
        )
    else:
        route = C2RouteDecision.BUDGET_EXCEEDED_FAIL_CLOSED
        rationale = (
            f"Fallback cohort cost (${proj_fallback.estimated_cost_usd}) exceeds "
            f"80% threshold (${max_allowable_cost} USD); fail-closed without dispatch."
        )

    return C2PreflightResult(
        model=MODEL_ID,
        authorized_budget_usd=authorized_budget_usd,
        max_allowable_cost_usd=max_allowable_cost,
        full_cohort_projection=proj_full,
        fallback_cohort_projection=proj_fallback,
        selected_route=route,
        live_dispatch_state="BLOCKED_DEFAULT_OFF",
        rationale=rationale,
    )
