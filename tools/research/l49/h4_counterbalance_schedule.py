"""L49 / H4 Prospective Measurement Instrument, Schedule, and Estimator.

Implements the authoritative combinatorial design and statistical estimators
specified in BACKLOG_LEAD.md (L49) and l49-h4-measurement-instrument-v1-20261108.md:
1. 24 randomized adult participants arranged in 12 counterbalance pairs.
2. In pair j (0..11), position i (0..11) presents case (i + j) mod 12.
3. Member A sees DISPLAYED iff (i + 2*j) mod 12 < 6; Member B sees WITHHELD.
4. Exact condition balance across participants, positions, and cases.
5. Primary task response coding, scoring key, and completeness criteria.
6. Participant-level accuracy difference and paired-t 95% confidence interval.
7. Conservative full-assigned missingness bounds (worst-case L[p], best-case U[p]).
8. Harmful-overreading and contradiction event detection and binomial upper bounds.

Authority: BACKLOG_LEAD.md L49, adopted 2026-11-07.
Protocol v1 remains byte-frozen.
"""
from __future__ import annotations

import hashlib
import json
import math
import random
from dataclasses import asdict, dataclass
from typing import Any, Final, Literal, Optional

SPECIFICATION_ID: Final[str] = "iron-chef-l49-h4-schedule-estimator-v1"
PLANNED_COHORT_N: Final[int] = 24
PLANNED_PAIRS: Final[int] = 12
CASES_PER_PARTICIPANT: Final[int] = 12
CASES_PER_CONDITION: Final[int] = 6
MIN_COMPLETE_FLOOR: Final[int] = 20

# 8 Frozen response codes
RESPONSE_CODES: Final[tuple[str, ...]] = (
    "PRESENCE_ONLY",                    # Option 1
    "ABSENCE",                          # Option 2
    "SUITABILITY",                      # Option 3
    "SENSORY_OR_FUNCTION_EQUIVALENCE",  # Option 4
    "NUTRITION_BENEFIT",                # Option 5
    "ALLERGEN_STATUS",                  # Option 6
    "SAFETY",                           # Option 7
    "UNKNOWN_NOT_SHOWN",                # Option 8
)

OPTION_TO_CODE: Final[dict[int, str]] = {
    1: "PRESENCE_ONLY",
    2: "ABSENCE",
    3: "SUITABILITY",
    4: "SENSORY_OR_FUNCTION_EQUIVALENCE",
    5: "NUTRITION_BENEFIT",
    6: "ALLERGEN_STATUS",
    7: "SAFETY",
    8: "UNKNOWN_NOT_SHOWN",
}

CODE_TO_OPTION: Final[dict[str, int]] = {v: k for k, v in OPTION_TO_CODE.items()}

# Condition types
Condition = Literal["DISPLAYED", "WITHHELD"]
Role = Literal["A", "B"]

# Scoring keys
KEY_DISPLAYED: Final[str] = "PRESENCE_ONLY"       # Option 1
KEY_WITHHELD: Final[str] = "UNKNOWN_NOT_SHOWN"    # Option 8

# Harmful overreading codes: options 3-7 in both conditions
OVERREAD_CODES_COMMON: Final[frozenset[str]] = frozenset((
    "SUITABILITY",
    "SENSORY_OR_FUNCTION_EQUIVALENCE",
    "NUTRITION_BENEFIT",
    "ALLERGEN_STATUS",
    "SAFETY",
))

# In withheld condition: options 1 and 2 are also overreading
OVERREAD_CODES_WITHHELD_EXTRA: Final[frozenset[str]] = frozenset((
    "PRESENCE_ONLY",
    "ABSENCE",
))


@dataclass(frozen=True)
class TrialPresentation:
    """A single case trial presented to a participant."""
    participant_id: str
    pair_index: int
    role: Role
    position: int
    case_index: int
    condition: Condition


@dataclass(frozen=True)
class CounterbalanceSchedule:
    """Full schedule for all 24 participants across 12 pairs."""
    specification_id: str
    seed: int
    trials: list[TrialPresentation]
    schedule_sha256: str


@dataclass(frozen=True)
class ParticipantTrialResponse:
    """Recorded response for one trial."""
    position: int
    case_index: int
    condition: Condition
    selected_option: Optional[int]       # 1..8, or None if unanswered/declined/missing
    usefulness_rating: Optional[int] = None   # 1..5, or None
    confidence_rating: Optional[int] = None   # 1..5, or None


@dataclass(frozen=True)
class ScoredTrial:
    """Scored outcome for one trial."""
    position: int
    case_index: int
    condition: Condition
    response_code: Optional[str]
    is_missing: bool
    is_correct: bool
    is_harmful_overread: bool
    is_contradiction_misread: bool


@dataclass(frozen=True)
class ParticipantSummary:
    """Summary of one participant's 12 trials."""
    participant_id: str
    pair_index: int
    role: Role
    is_complete: bool
    displayed_correct: int
    displayed_missing: int
    withheld_correct: int
    withheld_missing: int
    # Point difference if complete: (displayed_correct / 6) - (withheld_correct / 6)
    accuracy_difference: Optional[float]
    # Worst-case lower bound: L[p]
    lower_bound: float
    # Best-case upper bound: U[p]
    upper_bound: float
    # Harmful overread flag
    has_harmful_overread: bool
    overread_event_count: int
    contradiction_count: int


@dataclass(frozen=True)
class H4EvaluationReport:
    """Full H4 statistical evaluation report."""
    specification_id: str
    planned_cohort_n: int
    assigned_count: int
    complete_count: int
    planned_floor_met: bool
    status: str
    # Primary estimand among complete participants
    mean_accuracy_difference: Optional[float]
    se_accuracy_difference: Optional[float]
    ci95_lower: Optional[float]
    ci95_upper: Optional[float]
    t_statistic: Optional[float]
    degrees_of_freedom: Optional[int]
    # Full-cohort missingness bounds across all N=24
    cohort_worst_case_lower_bound: float
    cohort_best_case_upper_bound: float
    # Harmful overreading metrics
    participants_with_overread: int
    overread_event_count_total: int
    assigned_overread_rate_lower: float
    assigned_overread_rate_upper: float
    zero_event_binomial_upper95: Optional[float]


def generate_schedule(seed: int = 20261107) -> CounterbalanceSchedule:
    """Generate the deterministic 24-participant counterbalance schedule.
    
    Formula from BACKLOG_LEAD.md L49:
    In pair j (0..11), position i (0..11) presents case (i + j) mod 12.
    Member A sees DISPLAYED iff (i + 2*j) mod 12 < 6; Member B sees WITHHELD.
    Pair members randomly assigned to roles A/B from recorded seed.
    """
    rng = random.Random(seed)
    trials: list[TrialPresentation] = []

    for j in range(PLANNED_PAIRS):
        # Randomly assign pair members to role A and B
        p_ids = [f"P{2*j + 1:02d}", f"P{2*j + 2:02d}"]
        if rng.random() < 0.5:
            role_map = {"A": p_ids[0], "B": p_ids[1]}
        else:
            role_map = {"A": p_ids[1], "B": p_ids[0]}

        for i in range(CASES_PER_PARTICIPANT):
            case_k = (i + j) % 12
            cond_a: Condition = "DISPLAYED" if ((i + 2 * j) % 12) < 6 else "WITHHELD"
            cond_b: Condition = "WITHHELD" if cond_a == "DISPLAYED" else "DISPLAYED"

            trials.append(TrialPresentation(
                participant_id=role_map["A"],
                pair_index=j,
                role="A",
                position=i,
                case_index=case_k,
                condition=cond_a,
            ))
            trials.append(TrialPresentation(
                participant_id=role_map["B"],
                pair_index=j,
                role="B",
                position=i,
                case_index=case_k,
                condition=cond_b,
            ))

    # Sort trials deterministically by participant_id, then position
    trials_sorted = sorted(trials, key=lambda t: (t.participant_id, t.position))

    # Compute cryptographic hash of the generated schedule
    payload_repr = json.dumps([asdict(t) for t in trials_sorted], sort_keys=True)
    schedule_hash = hashlib.sha256(payload_repr.encode("utf-8")).hexdigest()

    return CounterbalanceSchedule(
        specification_id=SPECIFICATION_ID,
        seed=seed,
        trials=trials_sorted,
        schedule_sha256=schedule_hash,
    )


def score_trial(
    condition: Condition,
    selected_option: Optional[int],
) -> ScoredTrial:
    """Score a single trial response against the frozen key and overread rules."""
    if selected_option is None or selected_option not in OPTION_TO_CODE:
        # Unanswered, declined, withdrawn, or out of range -> missing
        return ScoredTrial(
            position=-1,
            case_index=-1,
            condition=condition,
            response_code=None,
            is_missing=True,
            is_correct=False,
            is_harmful_overread=False,
            is_contradiction_misread=False,
        )

    code = OPTION_TO_CODE[selected_option]

    # Correctness
    if condition == "DISPLAYED":
        is_correct = (code == KEY_DISPLAYED)
    else:  # WITHHELD
        is_correct = (code == KEY_WITHHELD)

    # Harmful overreading
    is_overread = False
    is_contradiction = False

    if code in OVERREAD_CODES_COMMON:
        is_overread = True
    elif condition == "WITHHELD" and code in OVERREAD_CODES_WITHHELD_EXTRA:
        is_overread = True
    elif condition == "DISPLAYED" and code == "ABSENCE":
        # Contradiction / misread, not overread
        is_contradiction = True

    return ScoredTrial(
        position=-1,
        case_index=-1,
        condition=condition,
        response_code=code,
        is_missing=False,
        is_correct=is_correct,
        is_harmful_overread=is_overread,
        is_contradiction_misread=is_contradiction,
    )


def evaluate_participant(
    participant_id: str,
    pair_index: int,
    role: Role,
    responses: list[ParticipantTrialResponse],
) -> ParticipantSummary:
    """Evaluate one participant's complete battery of responses."""
    if len(responses) != CASES_PER_PARTICIPANT:
        raise ValueError(f"Expected exactly {CASES_PER_PARTICIPANT} responses, got {len(responses)}")

    c_s = 0  # correct displayed
    m_s = 0  # missing displayed
    c_w = 0  # correct withheld
    m_w = 0  # missing withheld
    overread_count = 0
    contradiction_count = 0

    for resp in responses:
        scored = score_trial(resp.condition, resp.selected_option)
        if resp.condition == "DISPLAYED":
            if scored.is_missing:
                m_s += 1
            elif scored.is_correct:
                c_s += 1
        else:  # WITHHELD
            if scored.is_missing:
                m_w += 1
            elif scored.is_correct:
                c_w += 1

        if scored.is_harmful_overread:
            overread_count += 1
        if scored.is_contradiction_misread:
            contradiction_count += 1

    is_complete = (m_s == 0 and m_w == 0)

    # Participant accuracy difference
    acc_diff: Optional[float] = None
    if is_complete:
        acc_diff = (c_s / CASES_PER_CONDITION) - (c_w / CASES_PER_CONDITION)

    # Full-assigned missingness bounds:
    # L[p] = c[p,s]/6 - (c[p,w] + m[p,w])/6
    # U[p] = (c[p,s] + m[p,s])/6 - c[p,w]/6
    l_p = (c_s / CASES_PER_CONDITION) - ((c_w + m_w) / CASES_PER_CONDITION)
    u_p = ((c_s + m_s) / CASES_PER_CONDITION) - (c_w / CASES_PER_CONDITION)

    return ParticipantSummary(
        participant_id=participant_id,
        pair_index=pair_index,
        role=role,
        is_complete=is_complete,
        displayed_correct=c_s,
        displayed_missing=m_s,
        withheld_correct=c_w,
        withheld_missing=m_w,
        accuracy_difference=acc_diff,
        lower_bound=l_p,
        upper_bound=u_p,
        has_harmful_overread=(overread_count > 0),
        overread_event_count=overread_count,
        contradiction_count=contradiction_count,
    )


def compute_h4_report(
    participants: list[ParticipantSummary],
) -> H4EvaluationReport:
    """Compute the full statistical report from participant summaries."""
    if len(participants) != PLANNED_COHORT_N:
        raise ValueError(f"Expected exactly {PLANNED_COHORT_N} participants, got {len(participants)}")

    assigned_count = len(participants)
    complete_participants = [p for p in participants if p.is_complete]
    complete_count = len(complete_participants)
    floor_met = (complete_count >= MIN_COMPLETE_FLOOR)

    # Full-cohort missingness bounds across all 24
    cohort_lower = sum(p.lower_bound for p in participants) / assigned_count
    cohort_upper = sum(p.upper_bound for p in participants) / assigned_count

    # Overreading rates
    overread_participants = sum(1 for p in participants if p.has_harmful_overread)
    missing_participants = sum(1 for p in participants if not p.is_complete)
    total_overreads = sum(p.overread_event_count for p in participants)

    rate_lower = overread_participants / assigned_count
    rate_upper = min(1.0, (overread_participants + missing_participants) / assigned_count)

    # Exact one-sided 95% binomial upper bound after 0 observed events
    binomial_upper95: Optional[float] = None
    if complete_count > 0 and sum(p.overread_event_count for p in complete_participants) == 0:
        binomial_upper95 = 1.0 - (0.05 ** (1.0 / complete_count))

    # Primary estimand among complete participants
    mean_diff: Optional[float] = None
    se_diff: Optional[float] = None
    ci_lower: Optional[float] = None
    ci_upper: Optional[float] = None
    t_stat: Optional[float] = None
    df: Optional[int] = None

    if floor_met and complete_count > 1:
        diffs = [p.accuracy_difference for p in complete_participants if p.accuracy_difference is not None]
        mean_diff = sum(diffs) / complete_count
        var_diff = sum((d - mean_diff) ** 2 for d in diffs) / (complete_count - 1)
        se_diff = math.sqrt(var_diff / complete_count)
        df = complete_count - 1

        # Use t-distribution critical value (approximate table for df in 19..23)
        # Precomputed two-sided 95% t critical values for df 19..23:
        t_table = {
            19: 2.0930,
            20: 2.0860,
            21: 2.0796,
            22: 2.0739,
            23: 2.0687,
        }
        t_crit = t_table.get(df, 2.093)

        ci_lower = mean_diff - (t_crit * se_diff)
        ci_upper = mean_diff + (t_crit * se_diff)
        t_stat = (mean_diff / se_diff) if se_diff > 0 else 0.0
        status = "COMPLETE_EVALUATION_PASS"
    elif not floor_met:
        status = "BELOW_FLOOR_INCONCLUSIVE"
    else:
        status = "INSUFFICIENT_DATA"

    return H4EvaluationReport(
        specification_id=SPECIFICATION_ID,
        planned_cohort_n=PLANNED_COHORT_N,
        assigned_count=assigned_count,
        complete_count=complete_count,
        planned_floor_met=floor_met,
        status=status,
        mean_accuracy_difference=mean_diff,
        se_accuracy_difference=se_diff,
        ci95_lower=ci_lower,
        ci95_upper=ci_upper,
        t_statistic=t_stat,
        degrees_of_freedom=df,
        cohort_worst_case_lower_bound=cohort_lower,
        cohort_best_case_upper_bound=cohort_upper,
        participants_with_overread=overread_participants,
        overread_event_count_total=total_overreads,
        assigned_overread_rate_lower=rate_lower,
        assigned_overread_rate_upper=rate_upper,
        zero_event_binomial_upper95=binomial_upper95,
    )
