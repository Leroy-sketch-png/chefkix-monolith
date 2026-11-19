"""L40 Vietnamese Pilot Evaluation and Estimand Engine.

Implements the authoritative Lead evaluation and outcome contract for the
Vietnamese pilot under BACKLOG_LEAD.md L40 and
evidence/lead-l40-vietnamese-pilot-design-freeze-20261106.md:
1. Candidate-level A/C/N/U evaluation on blinded 5-candidate slates.
2. Case-level strict-A@5 outcome classification (V=1, V=0, V=unknown).
3. Fixed-denominator partial identification bounds (lower/upper success rates).
4. Wilson score intervals for descriptive case proportions.
5. A+C sensitivity bounds.
6. Paired raw vs C3-filtered transition accounting (empty list is ABSTENTION, not safety).
7. Inter-rater agreement classification (Tier-1 kappa >= 0.5, bootstrap lower >= 0.6).
"""
from __future__ import annotations

import dataclasses
import math
from enum import Enum
from typing import Mapping, Sequence

CULINARY_COHORT_N = 60
CONSTRAINT_COHORT_N = 40
SLATE_SIZE = 5

VALID_LABELS = frozenset({"A", "C", "N", "U"})


class CaseOutcome(str, Enum):
    V_ONE_SUCCESS = "V_ONE_SUCCESS"      # At least one final A
    V_ZERO_FAILURE = "V_ZERO_FAILURE"    # All C or N (no A)
    V_UNKNOWN = "V_UNKNOWN"              # Contains U and no A


class AgreementTier(str, Enum):
    RELIABLE_AGREEMENT = "RELIABLE_AGREEMENT"      # Bootstrap lower >= 0.60
    EXPLORATORY_AGREEMENT = "EXPLORATORY_AGREEMENT" # Tier-1 kappa >= 0.50
    INSUFFICIENT_AGREEMENT = "INSUFFICIENT_AGREEMENT" # kappa < 0.50


class FilterTransitionState(str, Enum):
    VIOLATION_REMOVED = "VIOLATION_REMOVED"
    UNKNOWN_WITHHELD = "UNKNOWN_WITHHELD"
    RETAINED_PASS = "RETAINED_PASS"
    ABSTENTION_EMPTY_POOL = "ABSTENTION_EMPTY_POOL"


@dataclasses.dataclass(frozen=True)
class EvaluatedCaseSlate:
    case_id: str
    cohort: str  # "culinary" or "constraint"
    candidate_labels: tuple[str, ...]  # A, C, N, U
    case_outcome: CaseOutcome
    is_ac_success: bool


@dataclasses.dataclass(frozen=True)
class WilsonInterval:
    estimate: float
    lower: float
    upper: float


@dataclasses.dataclass(frozen=True)
class PilotCohortSummary:
    cohort_name: str
    total_denominator_n: int
    v1_success_count: int
    v0_failure_count: int
    v_unknown_count: int
    strict_a_lower_bound: float
    strict_a_upper_bound: float
    strict_a_lower_wilson: WilsonInterval
    strict_a_upper_wilson: WilsonInterval
    ac_sensitivity_rate: float
    ac_sensitivity_wilson: WilsonInterval


@dataclasses.dataclass(frozen=True)
class C3FilterComparisonSummary:
    total_constraint_cases: int
    raw_v1_count: int
    filtered_v1_count: int
    abstentions_count: int
    violations_removed_count: int
    unknowns_withheld_count: int
    is_abstention_never_labeled_safe: bool


def compute_wilson_interval(successes: int, total: int, z: float = 1.96) -> WilsonInterval:
    """Compute two-sided Wilson score interval for a binomial proportion."""
    if total <= 0:
        return WilsonInterval(0.0, 0.0, 0.0)
    p = successes / total
    denom = 1.0 + (z * z) / total
    center = (p + (z * z) / (2.0 * total)) / denom
    margin = (z * math.sqrt((p * (1.0 - p) + (z * z) / (4.0 * total)) / total)) / denom
    lower = max(0.0, center - margin)
    upper = min(1.0, center + margin)
    return WilsonInterval(round(p, 6), round(lower, 6), round(upper, 6))


def classify_case_outcome(labels: Sequence[str]) -> tuple[CaseOutcome, bool]:
    """Classify 5-candidate slate into strict-A@5 outcome and A+C sensitivity.

    - V=1: At least one A.
    - V=0: No A, all C or N.
    - V=unknown: No A, contains at least one U.
    - A+C success: At least one A or C.
    """
    for l in labels:
        if l not in VALID_LABELS:
            raise ValueError(f"Invalid candidate label: {l}")

    has_a = "A" in labels
    has_u = "U" in labels
    has_c = "C" in labels

    if has_a:
        outcome = CaseOutcome.V_ONE_SUCCESS
    elif has_u:
        outcome = CaseOutcome.V_UNKNOWN
    else:
        outcome = CaseOutcome.V_ZERO_FAILURE

    ac_success = has_a or has_c
    return outcome, ac_success


def evaluate_cohort(
    cohort_name: str,
    cases: Sequence[EvaluatedCaseSlate],
    fixed_n: int,
) -> PilotCohortSummary:
    """Evaluate full cohort of cases with partial identification bounds."""
    if len(cases) != fixed_n:
        raise ValueError(f"Cohort {cohort_name} expects {fixed_n} cases, got {len(cases)}")

    v1_cnt = sum(1 for c in cases if c.case_outcome == CaseOutcome.V_ONE_SUCCESS)
    v0_cnt = sum(1 for c in cases if c.case_outcome == CaseOutcome.V_ZERO_FAILURE)
    vu_cnt = sum(1 for c in cases if c.case_outcome == CaseOutcome.V_UNKNOWN)
    ac_cnt = sum(1 for c in cases if c.is_ac_success)

    lower_rate = v1_cnt / fixed_n
    upper_rate = (v1_cnt + vu_cnt) / fixed_n
    ac_rate = ac_cnt / fixed_n

    return PilotCohortSummary(
        cohort_name=cohort_name,
        total_denominator_n=fixed_n,
        v1_success_count=v1_cnt,
        v0_failure_count=v0_cnt,
        v_unknown_count=vu_cnt,
        strict_a_lower_bound=round(lower_rate, 6),
        strict_a_upper_bound=round(upper_rate, 6),
        strict_a_lower_wilson=compute_wilson_interval(v1_cnt, fixed_n),
        strict_a_upper_wilson=compute_wilson_interval(v1_cnt + vu_cnt, fixed_n),
        ac_sensitivity_rate=round(ac_rate, 6),
        ac_sensitivity_wilson=compute_wilson_interval(ac_cnt, fixed_n),
    )


def evaluate_filter_transitions(
    raw_cases: Sequence[EvaluatedCaseSlate],
    filtered_candidate_counts: Sequence[int],  # Number of surviving candidates per case
    filtered_cases: Sequence[EvaluatedCaseSlate | None],  # None if pool is empty
    violations_removed: int,
    unknowns_withheld: int,
) -> C3FilterComparisonSummary:
    """Evaluate C3 filter impact on the constraint cohort."""
    total = len(raw_cases)
    if len(filtered_candidate_counts) != total or len(filtered_cases) != total:
        raise ValueError("Mismatched case counts for filter evaluation")

    raw_v1 = sum(1 for c in raw_cases if c.case_outcome == CaseOutcome.V_ONE_SUCCESS)
    filt_v1 = sum(1 for c in filtered_cases if c is not None and c.case_outcome == CaseOutcome.V_ONE_SUCCESS)
    abstentions = sum(1 for count in filtered_candidate_counts if count == 0)

    return C3FilterComparisonSummary(
        total_constraint_cases=total,
        raw_v1_count=raw_v1,
        filtered_v1_count=filt_v1,
        abstentions_count=abstentions,
        violations_removed_count=violations_removed,
        unknowns_withheld_count=unknowns_withheld,
        is_abstention_never_labeled_safe=True,  # Invariant: empty pool is never "safe"
    )


def classify_agreement(nominal_kappa: float, bootstrap_lower_95: float) -> AgreementTier:
    """Classify inter-rater agreement tier under Protocol v1."""
    if bootstrap_lower_95 >= 0.60:
        return AgreementTier.RELIABLE_AGREEMENT
    elif nominal_kappa >= 0.50:
        return AgreementTier.EXPLORATORY_AGREEMENT
    else:
        return AgreementTier.INSUFFICIENT_AGREEMENT
