"""
L37: Strata and Cluster-Aware Comparative Metric Evaluator.

Authors, traces, and evaluates:
1. Deterministic Oracle Strata mapping:
   - 4 descriptive oracle strata: seen_profiled, seen_unprofiled, unseen_profiled, unseen_unprofiled.
   - Canonical 10,747 test denominator (Claim R14: 1,162 profiled total = 986 seen + 176 unseen).
2. Partition-1 request-time feature cells:
   - (target_in_train: bool) x (target_profiled: bool) without gold labels.
3. Standardized metrics calculation with fail-closed denominator preservation:
   - Hit@1, Hit@5, Hit@10, MRR@20 (truncated).
   - Failures and abstentions retained verbatim in denominators.
   - Unmatched rate calculation and >5% flagging.
4. Statistical confidence intervals:
   - Two-sided Wilson score intervals for proportions.
   - Cluster-aware bootstrap intervals (case/recipe cluster, 2,000 resamples, deterministic seed).
5. Primary conditional rescue estimand P(V=1 | H=0):
   - Conditional rescue on unseen GISMo-identity stratum.
   - Decision threshold evaluation: support H1 if lower bound >= 0.15; refute if upper bound < 0.15.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
import math
import random
from typing import Any, Dict, List, Optional, Sequence, Set, Tuple


# ==============================================================================
# SSoT Constants & Protocol v1 Pins
# ==============================================================================

PROTOCOL_V1_SHA256 = (
    "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
)
CANONICAL_TEST_DENOMINATOR = 10747
CANONICAL_PROFILED_TOTAL = 1162

EXPECTED_ORACLE_STRATA_COUNTS = {
    "seen_unprofiled": 5946,
    "unseen_unprofiled": 3639,
    "seen_profiled": 986,
    "unseen_profiled": 176,
}

RESCUE_DECISION_THRESHOLD = 0.15
DEFAULT_BOOTSTRAP_RESAMPLES = 2000
DEFAULT_SEED = 20261021
Z_95 = 1.959963984540054


# ==============================================================================
# Enums and Data Models
# ==============================================================================

class OracleStratum(str, Enum):
    SEEN_UNPROFILED = "seen_unprofiled"
    UNSEEN_UNPROFILED = "unseen_unprofiled"
    SEEN_PROFILED = "seen_profiled"
    UNSEEN_PROFILED = "unseen_profiled"


class Partition1Cell(str, Enum):
    NOT_TRAIN_NOT_PROFILED = "cell_00"  # target_in_train=False, target_profiled=False
    NOT_TRAIN_PROFILED = "cell_01"      # target_in_train=False, target_profiled=True
    TRAIN_NOT_PROFILED = "cell_10"      # target_in_train=True,  target_profiled=False
    TRAIN_PROFILED = "cell_11"          # target_in_train=True,  target_profiled=True


class DecisionStatus(str, Enum):
    SUPPORT_H1 = "SUPPORT_H1"
    REFUTE_H1 = "REFUTE_H1"
    INCONCLUSIVE = "INCONCLUSIVE"


@dataclass(frozen=True)
class StrataCaseRecord:
    case_id: str
    recipe_id: str
    oracle_stratum: OracleStratum
    partition1_cell: Partition1Cell
    hit_at_1: bool
    hit_at_5: bool
    hit_at_10: bool
    reciprocal_rank_20: float  # 1/rank if rank <= 20 else 0.0
    is_valid_candidate_at_5: Optional[bool] = None  # V=1 if any acceptable candidate in top 5
    is_gold_matched_at_5: Optional[bool] = None     # H=1 if exact gold matched in top 5
    is_unmatched: bool = False
    is_failure: bool = False


@dataclass(frozen=True)
class MetricSummary:
    point_estimate: float
    ci_lower: float
    ci_upper: float
    denominator: int
    numerator: float
    ci_method: str  # "wilson" | "cluster_bootstrap"


@dataclass
class StratifiedMetricReport:
    total_cases: int
    hit_at_1: MetricSummary
    hit_at_5: MetricSummary
    hit_at_10: MetricSummary
    mrr_at_20: MetricSummary
    unmatched_count: int
    unmatched_rate: float
    unmatched_rate_flag: bool
    failure_count: int
    oracle_strata_breakdown: Dict[str, Dict[str, MetricSummary]] = field(default_factory=dict)
    partition1_breakdown: Dict[str, Dict[str, MetricSummary]] = field(default_factory=dict)


@dataclass
class ConditionalRescueReport:
    stratum: str
    h0_denominator: int  # Cases where H=0 (gold not matched)
    v1_count: int        # Cases where V=1 among H=0
    conditional_rescue_rate: float
    ci_95_lower: float
    ci_95_upper: float
    bootstrap_resamples: int
    decision_threshold: float
    decision: DecisionStatus
    joint_p_v1_h0: MetricSummary


# ==============================================================================
# Statistical Calculations
# ==============================================================================

def compute_wilson_interval(
    k: int, n: int, confidence_z: float = Z_95
) -> Tuple[float, float]:
    """
    Computes two-sided Wilson score confidence interval for a proportion k/n.
    Returns (lower_bound, upper_bound).
    """
    if n <= 0:
        return 0.0, 0.0
    p_hat = k / n
    z2 = confidence_z ** 2
    denom = 1.0 + z2 / n
    center = (p_hat + z2 / (2.0 * n)) / denom
    margin = (
        confidence_z
        * math.sqrt((p_hat * (1.0 - p_hat) + z2 / (4.0 * n)) / n)
        / denom
    )
    lower = 0.0 if k == 0 else max(0.0, center - margin)
    upper = 1.0 if k == n else min(1.0, center + margin)
    return lower, upper


def compute_cluster_bootstrap_interval(
    cases: Sequence[StrataCaseRecord],
    metric_extractor: Any,
    num_resamples: int = DEFAULT_BOOTSTRAP_RESAMPLES,
    seed: int = DEFAULT_SEED,
    alpha: float = 0.05,
) -> Tuple[float, float, float]:
    """
    Computes whole-recipe cluster bootstrap confidence interval.
    Clusters cases by recipe_id, resamples clusters with replacement,
    and returns (point_estimate, ci_lower, ci_upper).
    """
    if not cases:
        return 0.0, 0.0, 0.0

    # Group by recipe_id
    clusters: Dict[str, List[StrataCaseRecord]] = {}
    for case in cases:
        clusters.setdefault(case.recipe_id, []).append(case)

    cluster_keys = list(clusters.keys())
    point_estimate = metric_extractor(cases)

    rng = random.Random(seed)
    bootstrap_estimates: List[float] = []

    for _ in range(num_resamples):
        resampled_clusters = rng.choices(cluster_keys, k=len(cluster_keys))
        resampled_cases: List[StrataCaseRecord] = []
        for ck in resampled_clusters:
            resampled_cases.extend(clusters[ck])
        bootstrap_estimates.append(metric_extractor(resampled_cases))

    bootstrap_estimates.sort()
    # Order-statistic percentile convention
    k_low = int(math.floor(num_resamples * (alpha / 2.0)))
    k_high = int(math.ceil(num_resamples * (1.0 - alpha / 2.0))) - 1

    k_low = max(0, min(k_low, num_resamples - 1))
    k_high = max(0, min(k_high, num_resamples - 1))

    ci_lower = bootstrap_estimates[k_low]
    ci_upper = bootstrap_estimates[k_high]

    return point_estimate, ci_lower, ci_upper


# ==============================================================================
# Metric Evaluators
# ==============================================================================

def extract_hit_1(cases: Sequence[StrataCaseRecord]) -> float:
    if not cases:
        return 0.0
    return sum(1 for c in cases if c.hit_at_1) / len(cases)


def extract_hit_5(cases: Sequence[StrataCaseRecord]) -> float:
    if not cases:
        return 0.0
    return sum(1 for c in cases if c.hit_at_5) / len(cases)


def extract_hit_10(cases: Sequence[StrataCaseRecord]) -> float:
    if not cases:
        return 0.0
    return sum(1 for c in cases if c.hit_at_10) / len(cases)


def extract_mrr_20(cases: Sequence[StrataCaseRecord]) -> float:
    if not cases:
        return 0.0
    return sum(c.reciprocal_rank_20 for c in cases) / len(cases)


def evaluate_metric_with_intervals(
    cases: Sequence[StrataCaseRecord],
    metric_extractor: Any,
    is_proportion: bool = True,
    use_cluster_bootstrap: bool = True,
    seed: int = DEFAULT_SEED,
) -> MetricSummary:
    """
    Evaluates a metric across cases with either Wilson or cluster bootstrap intervals.
    """
    n = len(cases)
    if n == 0:
        return MetricSummary(0.0, 0.0, 0.0, 0, 0.0, "none")

    point = metric_extractor(cases)
    numerator = point * n

    if use_cluster_bootstrap:
        pt, low, high = compute_cluster_bootstrap_interval(
            cases, metric_extractor, seed=seed
        )
        return MetricSummary(
            point_estimate=pt,
            ci_lower=low,
            ci_upper=high,
            denominator=n,
            numerator=numerator,
            ci_method="cluster_bootstrap",
        )
    elif is_proportion:
        k = round(numerator)
        low, high = compute_wilson_interval(k, n)
        return MetricSummary(
            point_estimate=point,
            ci_lower=low,
            ci_upper=high,
            denominator=n,
            numerator=numerator,
            ci_method="wilson",
        )
    else:
        return MetricSummary(
            point_estimate=point,
            ci_lower=point,
            ci_upper=point,
            denominator=n,
            numerator=numerator,
            ci_method="unbounded_mean",
        )


# ==============================================================================
# Full Stratified Analysis Runner
# ==============================================================================

def analyze_stratified_cohort(
    cases: List[StrataCaseRecord],
    seed: int = DEFAULT_SEED,
) -> StratifiedMetricReport:
    """
    Runs complete stratified comparative analysis across full cohort,
    oracle strata, and Partition-1 request-time cells.
    """
    total = len(cases)
    if total == 0:
        raise ValueError("Cannot analyze empty cases cohort")

    unmatched = sum(1 for c in cases if c.is_unmatched)
    unmatched_rate = unmatched / total
    unmatched_flag = unmatched_rate > 0.05
    failures = sum(1 for c in cases if c.is_failure)

    # Full cohort metrics
    h1 = evaluate_metric_with_intervals(cases, extract_hit_1, seed=seed)
    h5 = evaluate_metric_with_intervals(cases, extract_hit_5, seed=seed)
    h10 = evaluate_metric_with_intervals(cases, extract_hit_10, seed=seed)
    mrr = evaluate_metric_with_intervals(cases, extract_mrr_20, is_proportion=False, seed=seed)

    # Oracle Strata Breakdown
    oracle_groups: Dict[str, List[StrataCaseRecord]] = {}
    for c in cases:
        oracle_groups.setdefault(c.oracle_stratum.value, []).append(c)

    oracle_breakdown: Dict[str, Dict[str, MetricSummary]] = {}
    for stratum_name, scases in oracle_groups.items():
        oracle_breakdown[stratum_name] = {
            "hit_at_1": evaluate_metric_with_intervals(scases, extract_hit_1, seed=seed),
            "hit_at_5": evaluate_metric_with_intervals(scases, extract_hit_5, seed=seed),
            "hit_at_10": evaluate_metric_with_intervals(scases, extract_hit_10, seed=seed),
            "mrr_at_20": evaluate_metric_with_intervals(scases, extract_mrr_20, is_proportion=False, seed=seed),
        }

    # Partition-1 Cells Breakdown
    part1_groups: Dict[str, List[StrataCaseRecord]] = {}
    for c in cases:
        part1_groups.setdefault(c.partition1_cell.value, []).append(c)

    part1_breakdown: Dict[str, Dict[str, MetricSummary]] = {}
    for cell_name, ccases in part1_groups.items():
        part1_breakdown[cell_name] = {
            "hit_at_1": evaluate_metric_with_intervals(ccases, extract_hit_1, seed=seed),
            "hit_at_5": evaluate_metric_with_intervals(ccases, extract_hit_5, seed=seed),
            "hit_at_10": evaluate_metric_with_intervals(ccases, extract_hit_10, seed=seed),
            "mrr_at_20": evaluate_metric_with_intervals(ccases, extract_mrr_20, is_proportion=False, seed=seed),
        }

    return StratifiedMetricReport(
        total_cases=total,
        hit_at_1=h1,
        hit_at_5=h5,
        hit_at_10=h10,
        mrr_at_20=mrr,
        unmatched_count=unmatched,
        unmatched_rate=unmatched_rate,
        unmatched_rate_flag=unmatched_flag,
        failure_count=failures,
        oracle_strata_breakdown=oracle_breakdown,
        partition1_breakdown=part1_breakdown,
    )


# ==============================================================================
# Conditional Rescue Estimator (Primary H1 Endpoint)
# ==============================================================================

def evaluate_conditional_rescue(
    cases: List[StrataCaseRecord],
    target_strata: Sequence[OracleStratum] = (OracleStratum.UNSEEN_UNPROFILED, OracleStratum.UNSEEN_PROFILED),
    threshold: float = RESCUE_DECISION_THRESHOLD,
    num_resamples: int = DEFAULT_BOOTSTRAP_RESAMPLES,
    seed: int = DEFAULT_SEED,
) -> ConditionalRescueReport:
    """
    Evaluates the primary estimand P(V=1 | H=0) for strict valid@5 on unseen GISMo-identity cases.
    """
    # Filter cases to target strata
    strata_set = set(target_strata)
    cohort = [c for c in cases if c.oracle_stratum in strata_set]

    if not cohort:
        raise ValueError("No cases matching target strata for conditional rescue")

    # Cases where H=0 (exact gold not matched in top 5)
    h0_cohort = [c for c in cohort if c.is_gold_matched_at_5 is False or (not c.hit_at_5)]
    h0_denom = len(h0_cohort)

    if h0_denom == 0:
        return ConditionalRescueReport(
            stratum="+".join(s.value for s in target_strata),
            h0_denominator=0,
            v1_count=0,
            conditional_rescue_rate=0.0,
            ci_95_lower=0.0,
            ci_95_upper=0.0,
            bootstrap_resamples=num_resamples,
            decision_threshold=threshold,
            decision=DecisionStatus.INCONCLUSIVE,
            joint_p_v1_h0=MetricSummary(0.0, 0.0, 0.0, len(cohort), 0.0, "none"),
        )

    # Number where V=1 among H=0
    v1_count = sum(1 for c in h0_cohort if c.is_valid_candidate_at_5 is True)
    rescue_rate = v1_count / h0_denom

    # Cluster bootstrap over the cohort clusters for conditional ratio P(V=1 | H=0)
    def rescue_extractor(sample_cases: Sequence[StrataCaseRecord]) -> float:
        h0 = [c for c in sample_cases if c.is_gold_matched_at_5 is False or (not c.hit_at_5)]
        if not h0:
            return 0.0
        v1 = sum(1 for c in h0 if c.is_valid_candidate_at_5 is True)
        return v1 / len(h0)

    pt, ci_low, ci_high = compute_cluster_bootstrap_interval(
        h0_cohort, rescue_extractor, num_resamples=num_resamples, seed=seed
    )

    # Decision rule
    if ci_low >= threshold:
        decision = DecisionStatus.SUPPORT_H1
    elif ci_high < threshold:
        decision = DecisionStatus.REFUTE_H1
    else:
        decision = DecisionStatus.INCONCLUSIVE

    # Joint P(V=1, H=0) over the full stratum cohort
    def joint_extractor(sample_cases: Sequence[StrataCaseRecord]) -> float:
        if not sample_cases:
            return 0.0
        joint_count = sum(
            1 for c in sample_cases
            if (c.is_gold_matched_at_5 is False or not c.hit_at_5) and c.is_valid_candidate_at_5 is True
        )
        return joint_count / len(sample_cases)

    j_pt, j_low, j_high = compute_cluster_bootstrap_interval(
        cohort, joint_extractor, num_resamples=num_resamples, seed=seed
    )
    joint_summary = MetricSummary(
        point_estimate=j_pt,
        ci_lower=j_low,
        ci_upper=j_high,
        denominator=len(cohort),
        numerator=j_pt * len(cohort),
        ci_method="cluster_bootstrap",
    )

    return ConditionalRescueReport(
        stratum="+".join(s.value for s in target_strata),
        h0_denominator=h0_denom,
        v1_count=v1_count,
        conditional_rescue_rate=rescue_rate,
        ci_95_lower=ci_low,
        ci_95_upper=ci_high,
        bootstrap_resamples=num_resamples,
        decision_threshold=threshold,
        decision=decision,
        joint_p_v1_h0=joint_summary,
    )
