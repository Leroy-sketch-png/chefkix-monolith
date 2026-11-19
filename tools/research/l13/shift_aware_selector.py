"""
L13′ Shift-Aware Selector Interface, Leakage Guard, and Routing Evaluator.

Implements the Lead-owned scope decision:
`evidence/lead-l13-shift-aware-selector-scope-20261106.md`
governed by Campaign Protocol v1 (SHA-256 b2952599099c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12)
and Strategy Sections 2-3, 5, 8, and 17.

Invariants:
1. Operation: Chooses only among outputs of already frozen scorers. Does NOT retrain
   scorers, blend signals, alter ranks, or generate new candidates. SAG/dual-encoder rejected.
2. Feature Eligibility & Leakage Guard:
   - Permitted: request-time, permissioned, source-versioned features only (train presence,
     FooDB profile presence, recipe length, catalog membership state).
   - FORBIDDEN: gold/reference identity, adjudication/human labels, realized candidate ranks,
     test metrics, oracle strata. Any presence causes immediate fail-closed rejection.
3. Denominator Preservation:
   Fixed denominator maintained. Failures, invalid outputs, and empty-pool abstentions
   are preserved and never silently dropped or replaced by fallback scorers.
4. H5 Exploratory Boundary:
   Evaluates coverage-versus-set-size tradeoffs on held-out evaluations without test tuning.
   Emits `no_guarantee` with `exploratory_only_unproven_guarantee`.
"""

from __future__ import annotations

import dataclasses
from typing import Any, Callable, Dict, List, Optional, Set, Tuple


FORBIDDEN_FEATURE_KEYWORDS = (
    "gold",
    "reference",
    "label",
    "adjudication",
    "human_valid",
    "judge",
    "score",
    "rank",
    "test_metric",
    "oracle_stratum",
    "realized_outcome",
)

CATALOG_MEMBERSHIP_STATES = ("resolved", "ambiguous", "unknown")


def detect_forbidden_feature_leakage(feature_dict: Dict[str, Any]) -> None:
    """
    Scans request features for post-scoring, oracle, or gold labels.
    Raises ValueError immediately if leakage is detected.
    """
    for key in feature_dict:
        k_lower = key.lower()
        for forbidden in FORBIDDEN_FEATURE_KEYWORDS:
            if forbidden in k_lower:
                raise ValueError(
                    f"Forbidden feature leakage detected: key '{key}' contains forbidden keyword '{forbidden}'. "
                    "Routing features must be strictly request-time and outcome-blind."
                )


@dataclasses.dataclass(frozen=True)
class RequestFeatures:
    case_id: str
    recipe_id: str
    target_slot_id: str
    train_source_presence: bool
    foodb_profile_presence: bool
    catalog_membership_state: str = "resolved"
    recipe_token_count: Optional[int] = None
    ingredient_count: Optional[int] = None
    cuisine_tag: Optional[str] = None
    extra_features: Optional[Dict[str, Any]] = None

    def __post_init__(self):
        if not self.case_id or not self.case_id.strip():
            raise ValueError("case_id must be non-empty")
        if not self.recipe_id or not self.recipe_id.strip():
            raise ValueError("recipe_id must be non-empty")
        if not self.target_slot_id or not self.target_slot_id.strip():
            raise ValueError("target_slot_id must be non-empty")
        if self.catalog_membership_state not in CATALOG_MEMBERSHIP_STATES:
            raise ValueError(
                f"Invalid catalog_membership_state '{self.catalog_membership_state}', "
                f"must be in {CATALOG_MEMBERSHIP_STATES}"
            )
        # Scan extra features for leakage
        if self.extra_features:
            detect_forbidden_feature_leakage(self.extra_features)


@dataclasses.dataclass(frozen=True)
class ScorerOutputSlate:
    case_id: str
    scorer_id: str
    candidates: List[str]  # Ranked list of canonical candidate IDs
    status: str = "AVAILABLE"  # "AVAILABLE" | "EMPTY_POOL_ABSTAIN" | "SCORING_FAILURE"
    failure: bool = False

    def is_abstain(self) -> bool:
        return self.status == "EMPTY_POOL_ABSTAIN"

    def is_failure(self) -> bool:
        return self.failure or self.status == "SCORING_FAILURE"


@dataclasses.dataclass(frozen=True)
class SelectorRoutingDecision:
    case_id: str
    selected_scorer_id: str
    chosen_slate: ScorerOutputSlate
    routed_by_rule: str
    is_fallback: bool
    routing_reason: str


class ShiftAwareSelector:
    """
    Deterministic shift-aware selector over frozen scorers.
    """

    def __init__(
        self,
        selector_id: str,
        available_scorer_ids: Set[str],
        fallback_scorer_id: str,
        routing_policy: Callable[[RequestFeatures], Tuple[str, str]],
    ):
        if not available_scorer_ids:
            raise ValueError("available_scorer_ids must be non-empty")
        if fallback_scorer_id not in available_scorer_ids:
            raise ValueError(f"fallback_scorer_id '{fallback_scorer_id}' must be in available_scorer_ids")

        self.selector_id = selector_id
        self.available_scorer_ids = available_scorer_ids
        self.fallback_scorer_id = fallback_scorer_id
        self.routing_policy = routing_policy

    def route(
        self,
        features: RequestFeatures,
        slates: Dict[str, ScorerOutputSlate],
    ) -> SelectorRoutingDecision:
        """
        Executes routing decision for a request case.
        Invariants:
        - Validates request features.
        - Obtains chosen scorer ID from policy.
        - Enforces fallback if chosen scorer is unavailable in candidate slates.
        - Preserves failure or abstention of the chosen scorer (never silently swaps).
        """
        # Step 1: Execute policy
        try:
            chosen_arm, rule_name = self.routing_policy(features)
        except Exception as ex:
            chosen_arm, rule_name = self.fallback_scorer_id, f"policy_exception_fallback: {ex}"

        is_fallback = False
        if chosen_arm not in self.available_scorer_ids or chosen_arm not in slates:
            is_fallback = True
            routing_reason = (
                f"Chosen arm '{chosen_arm}' not in available slates; "
                f"falling back to '{self.fallback_scorer_id}'"
            )
            chosen_arm = self.fallback_scorer_id
        else:
            routing_reason = f"Routed to '{chosen_arm}' by rule '{rule_name}'"

        if chosen_arm not in slates:
            raise ValueError(f"Fallback arm '{chosen_arm}' is missing from provided slates for case {features.case_id}")

        slate = slates[chosen_arm]
        return SelectorRoutingDecision(
            case_id=features.case_id,
            selected_scorer_id=chosen_arm,
            chosen_slate=slate,
            routed_by_rule=rule_name,
            is_fallback=is_fallback,
            routing_reason=routing_reason,
        )


@dataclasses.dataclass
class SelectorEvaluationCase:
    features: RequestFeatures
    slates: Dict[str, ScorerOutputSlate]
    candidate_labels: Dict[str, str]  # candidate_id -> "A" | "C" | "N" | "U"


@dataclasses.dataclass
class ArmPerformanceSummary:
    arm_id: str
    total_cases: int
    available_cases: int
    abstain_cases: int
    failure_cases: int
    hit_at_1_count: int
    hit_at_5_count: int
    hit_at_1_rate: float
    hit_at_5_rate: float
    mean_set_size: float


@dataclasses.dataclass
class SelectorTradeoffReport:
    selector_id: str
    total_eval_cases: int
    selector_summary: ArmPerformanceSummary
    individual_arm_summaries: Dict[str, ArmPerformanceSummary]
    routing_distribution: Dict[str, int]
    status: str = "EXPLORATORY_DESCRIPTIVE_ONLY"
    guarantee_status: str = "no_guarantee"
    reason: str = "unproven_exchangeability_and_exploratory_h5"


def evaluate_arm_performance(
    arm_id: str,
    cases: List[SelectorEvaluationCase],
    get_slate_fn: Callable[[SelectorEvaluationCase], ScorerOutputSlate],
) -> ArmPerformanceSummary:
    """Evaluates performance of an arm or selector over evaluation cases."""
    total = len(cases)
    if total == 0:
        return ArmPerformanceSummary(
            arm_id=arm_id,
            total_cases=0,
            available_cases=0,
            abstain_cases=0,
            failure_cases=0,
            hit_at_1_count=0,
            hit_at_5_count=0,
            hit_at_1_rate=0.0,
            hit_at_5_rate=0.0,
            mean_set_size=0.0,
        )

    available = 0
    abstains = 0
    failures = 0
    hit1 = 0
    hit5 = 0
    set_sizes = []

    for c in cases:
        slate = get_slate_fn(c)
        if slate.is_abstain():
            abstains += 1
            set_sizes.append(0)
            continue
        if slate.is_failure():
            failures += 1
            set_sizes.append(0)
            continue

        available += 1
        set_sizes.append(len(slate.candidates))

        # Check Hit@1
        if len(slate.candidates) >= 1:
            first_c = slate.candidates[0]
            if c.candidate_labels.get(first_c) in ("A", "C"):
                hit1 += 1

        # Check Hit@5
        top5 = slate.candidates[:5]
        if any(c.candidate_labels.get(cand) in ("A", "C") for cand in top5):
            hit5 += 1

    return ArmPerformanceSummary(
        arm_id=arm_id,
        total_cases=total,
        available_cases=available,
        abstain_cases=abstains,
        failure_cases=failures,
        hit_at_1_count=hit1,
        hit_at_5_count=hit5,
        hit_at_1_rate=hit1 / total,
        hit_at_5_rate=hit5 / total,
        mean_set_size=sum(set_sizes) / total,
    )


def evaluate_shift_aware_selector(
    selector: ShiftAwareSelector,
    eval_cases: List[SelectorEvaluationCase],
) -> SelectorTradeoffReport:
    """
    Evaluates selector routing and compares with individual standalone arms.
    Invariants:
    - Fixed denominator retained across all comparisons.
    - No guarantee is emitted.
    """
    routing_dist: Dict[str, int] = {arm: 0 for arm in selector.available_scorer_ids}

    def selector_slate(case: SelectorEvaluationCase) -> ScorerOutputSlate:
        dec = selector.route(case.features, case.slates)
        routing_dist[dec.selected_scorer_id] = routing_dist.get(dec.selected_scorer_id, 0) + 1
        return dec.chosen_slate

    sel_summary = evaluate_arm_performance(selector.selector_id, eval_cases, selector_slate)

    arm_summaries: Dict[str, ArmPerformanceSummary] = {}
    for arm in selector.available_scorer_ids:
        def arm_slate(case: SelectorEvaluationCase, a=arm) -> ScorerOutputSlate:
            return case.slates.get(a, ScorerOutputSlate(case.features.case_id, a, [], "SCORING_FAILURE", True))
        arm_summaries[arm] = evaluate_arm_performance(arm, eval_cases, arm_slate)

    return SelectorTradeoffReport(
        selector_id=selector.selector_id,
        total_eval_cases=len(eval_cases),
        selector_summary=sel_summary,
        individual_arm_summaries=arm_summaries,
        routing_distribution=routing_dist,
    )
