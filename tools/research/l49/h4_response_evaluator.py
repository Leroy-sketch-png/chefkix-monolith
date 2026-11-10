"""L49 H4 Response Evaluator and Estimand Engine.

Implements the authoritative H4 prospective comprehension/usability measurement
contract under BACKLOG_LEAD.md L49:
1. Response scoring against frozen 8-code schema.
2. Complete-participant paired estimand with 95% paired-t interval.
3. Floor verification (N >= 20 complete participants out of N=24).
4. Full-assigned-cohort worst/best-case missingness bounds (N=24 fixed).
5. Harmful over-reading detection and assigned-cohort event-rate bounds.
6. Exact one-sided 95% binomial upper bound on zero events for complete cases.
7. Fail-closed disqualification on observed harmful over-reading.
"""
from __future__ import annotations

import dataclasses
import math
from enum import Enum
from typing import Mapping, Sequence

TOTAL_RANDOMIZED_N = 24
MINIMUM_COMPLETE_FLOOR = 20
ITEMS_PER_CONDITION = 6
TOTAL_ITEMS_PER_PARTICIPANT = 12


class H4ResponseCode(str, Enum):
    PRESENCE_ONLY = "PRESENCE_ONLY"
    ABSENCE = "ABSENCE"
    SUITABILITY = "SUITABILITY"
    SENSORY_OR_FUNCTION_EQUIVALENCE = "SENSORY_OR_FUNCTION_EQUIVALENCE"
    NUTRITION_BENEFIT = "NUTRITION_BENEFIT"
    ALLERGEN_STATUS = "ALLERGEN_STATUS"
    SAFETY = "SAFETY"
    UNKNOWN_NOT_SHOWN = "UNKNOWN_NOT_SHOWN"


class StimulusCondition(str, Enum):
    DISPLAYED = "DISPLAYED"
    WITHHELD = "WITHHELD"


HARMFUL_CODES_UNIVERSAL = frozenset({
    H4ResponseCode.SUITABILITY,
    H4ResponseCode.SENSORY_OR_FUNCTION_EQUIVALENCE,
    H4ResponseCode.NUTRITION_BENEFIT,
    H4ResponseCode.ALLERGEN_STATUS,
    H4ResponseCode.SAFETY,
})


@dataclasses.dataclass(frozen=True)
class SingleTrialResponse:
    trial_index: int
    condition: StimulusCondition
    response_code: H4ResponseCode | None  # None indicates missing/unavailable
    confidence_rating: int | None = None  # 1-5 descriptive
    usefulness_rating: int | None = None  # 1-5 descriptive


@dataclasses.dataclass(frozen=True)
class ParticipantScoredRecord:
    participant_id: str
    pair_id: int
    pair_member: str  # 'A' or 'B'
    displayed_correct: int
    displayed_missing: int
    withheld_correct: int
    withheld_missing: int
    is_complete: bool
    observed_harmful_flag: bool
    observed_contradiction_flag: bool
    displayed_accuracy: float | None
    withheld_accuracy: float | None
    paired_difference: float | None  # displayed_acc - withheld_acc
    bound_lower: float  # L_p
    bound_upper: float  # U_p


@dataclasses.dataclass(frozen=True)
class H4EvaluationResult:
    total_randomized: int
    complete_participants_count: int
    meets_planned_floor: bool
    primary_mean_difference: float | None
    paired_t_ci_lower: float | None
    paired_t_ci_upper: float | None
    full_cohort_bound_lower: float  # Average L_p over 24
    full_cohort_bound_upper: float  # Average U_p over 24
    total_overread_trials_displayed: int
    total_overread_trials_withheld: int
    participants_with_overread_count: int
    participants_with_missing_count: int
    overread_event_rate_lower_bound: float  # O / 24
    overread_event_rate_upper_bound: float  # (O + M) / 24
    binomial_zero_event_upper_bound: float | None  # If O == 0 among completes
    favorable_interpretation_allowed: bool
    status_summary: str


def score_single_trial(
    condition: StimulusCondition,
    response_code: H4ResponseCode | None,
) -> tuple[int | None, bool, bool]:
    """Score trial: (score_0_or_1_or_None, is_harmful_overread, is_contradiction)."""
    if response_code is None:
        return None, False, False

    # Key mapping
    expected_key = (
        H4ResponseCode.PRESENCE_ONLY
        if condition == StimulusCondition.DISPLAYED
        else H4ResponseCode.UNKNOWN_NOT_SHOWN
    )
    is_correct = 1 if response_code == expected_key else 0

    # Harmful overread detection
    is_harmful = False
    is_contradiction = False

    if response_code in HARMFUL_CODES_UNIVERSAL:
        is_harmful = True
    elif condition == StimulusCondition.WITHHELD and response_code in (
        H4ResponseCode.PRESENCE_ONLY,
        H4ResponseCode.ABSENCE,
    ):
        is_harmful = True
    elif condition == StimulusCondition.DISPLAYED and response_code == H4ResponseCode.ABSENCE:
        is_contradiction = True

    return is_correct, is_harmful, is_contradiction


def score_participant(
    participant_id: str,
    pair_id: int,
    pair_member: str,
    trials: Sequence[SingleTrialResponse],
) -> ParticipantScoredRecord:
    """Score all 12 trials for a participant."""
    if len(trials) != TOTAL_ITEMS_PER_PARTICIPANT:
        raise ValueError(
            f"Expected {TOTAL_ITEMS_PER_PARTICIPANT} trials, got {len(trials)} for {participant_id}"
        )

    disp_correct = 0
    disp_missing = 0
    with_correct = 0
    with_missing = 0
    harmful_observed = False
    contradiction_observed = False

    for t in trials:
        score, harmful, contra = score_single_trial(t.condition, t.response_code)
        if harmful:
            harmful_observed = True
        if contra:
            contradiction_observed = True

        if t.condition == StimulusCondition.DISPLAYED:
            if score is None:
                disp_missing += 1
            else:
                disp_correct += score
        else:
            if score is None:
                with_missing += 1
            else:
                with_correct += score

    is_complete = (disp_missing == 0) and (with_missing == 0)

    if is_complete:
        disp_acc = disp_correct / ITEMS_PER_CONDITION
        with_acc = with_correct / ITEMS_PER_CONDITION
        diff = disp_acc - with_acc
    else:
        disp_acc = None
        with_acc = None
        diff = None

    # Worst/best-case bounds per participant
    bound_lower = (disp_correct / ITEMS_PER_CONDITION) - (
        (with_correct + with_missing) / ITEMS_PER_CONDITION
    )
    bound_upper = ((disp_correct + disp_missing) / ITEMS_PER_CONDITION) - (
        with_correct / ITEMS_PER_CONDITION
    )

    return ParticipantScoredRecord(
        participant_id=participant_id,
        pair_id=pair_id,
        pair_member=pair_member,
        displayed_correct=disp_correct,
        displayed_missing=disp_missing,
        withheld_correct=with_correct,
        withheld_missing=with_missing,
        is_complete=is_complete,
        observed_harmful_flag=harmful_observed,
        observed_contradiction_flag=contradiction_observed,
        displayed_accuracy=disp_acc,
        withheld_accuracy=with_acc,
        paired_difference=diff,
        bound_lower=bound_lower,
        bound_upper=bound_upper,
    )


def compute_t_critical_95(df: int) -> float:
    """Precomputed standard 2-sided 95% Student-t critical values."""
    # Approximate or lookup table for df from 1 to 30
    t_table = {
        1: 12.706, 2: 4.303, 3: 3.182, 4: 2.776, 5: 2.571,
        6: 2.447, 7: 2.365, 8: 2.306, 9: 2.262, 10: 2.228,
        11: 2.201, 12: 2.179, 13: 2.160, 14: 2.145, 15: 2.131,
        16: 2.120, 17: 2.110, 18: 2.101, 19: 2.093, 20: 2.086,
        21: 2.080, 22: 2.074, 23: 2.069, 24: 2.064, 25: 2.060,
    }
    return t_table.get(df, 2.086 if df >= 20 else 2.228)


def evaluate_cohort_responses(
    participants: Sequence[ParticipantScoredRecord],
    total_assigned_n: int = TOTAL_RANDOMIZED_N,
) -> H4EvaluationResult:
    """Evaluate complete cohort under L49 specification."""
    if len(participants) != total_assigned_n:
        raise ValueError(
            f"Expected {total_assigned_n} randomized participant records, got {len(participants)}"
        )

    complete_records = [p for p in participants if p.is_complete]
    n_complete = len(complete_records)
    meets_floor = n_complete >= MINIMUM_COMPLETE_FLOOR

    # Primary paired estimand
    if meets_floor and n_complete > 1:
        diffs = [p.paired_difference for p in complete_records if p.paired_difference is not None]
        mean_diff = sum(diffs) / n_complete
        variance = sum((d - mean_diff) ** 2 for d in diffs) / (n_complete - 1)
        std_err = math.sqrt(variance / n_complete)
        t_crit = compute_t_critical_95(n_complete - 1)
        ci_lower = mean_diff - t_crit * std_err
        ci_upper = mean_diff + t_crit * std_err
    else:
        mean_diff = None
        ci_lower = None
        ci_upper = None

    # Full cohort missingness bounds
    full_bound_lower = sum(p.bound_lower for p in participants) / total_assigned_n
    full_bound_upper = sum(p.bound_upper for p in participants) / total_assigned_n

    # Over-reading counts
    overread_participants = sum(1 for p in participants if p.observed_harmful_flag)
    missing_participants = sum(1 for p in participants if not p.is_complete)

    event_rate_lower = overread_participants / total_assigned_n
    event_rate_upper = min(1.0, (overread_participants + missing_participants) / total_assigned_n)

    # Binomial upper bound on zero events among completes
    if overread_participants == 0 and n_complete > 0:
        binom_upper = 1.0 - math.pow(0.05, 1.0 / n_complete)
    else:
        binom_upper = None

    favorable_allowed = (overread_participants == 0) and meets_floor

    if not meets_floor:
        status = "BELOW_FLOOR_INCONCLUSIVE"
    elif overread_participants > 0:
        status = "HARMFUL_OVERREAD_DETECTED"
    else:
        status = "SUPPORT_H4_ESTIMAND"

    return H4EvaluationResult(
        total_randomized=total_assigned_n,
        complete_participants_count=n_complete,
        meets_planned_floor=meets_floor,
        primary_mean_difference=mean_diff,
        paired_t_ci_lower=ci_lower,
        paired_t_ci_upper=ci_upper,
        full_cohort_bound_lower=full_bound_lower,
        full_cohort_bound_upper=full_bound_upper,
        total_overread_trials_displayed=0,
        total_overread_trials_withheld=0,
        participants_with_overread_count=overread_participants,
        participants_with_missing_count=missing_participants,
        overread_event_rate_lower_bound=event_rate_lower,
        overread_event_rate_upper_bound=event_rate_upper,
        binomial_zero_event_upper_bound=binom_upper,
        favorable_interpretation_allowed=favorable_allowed,
        status_summary=status,
    )
