"""L36 Adjudicated Strict-A@5 Audit Estimand & Partial-Identification Bounds Evaluator.

Implements the Lead L36 operational addendum under frozen Protocol v1:
- Specification ID: iron-chef-l36-case-rule-v1
- Protocol v1 SHA-256: b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12
- Case rule: V=1 if any candidate in frozen top-5 is 'A'; V=0 if no 'A' and all 'C'/'N';
  V=unknown if no 'A' and at least one is 'U' (never imputed as negative).
- Strata & Denominators:
  * unseen_profiled: N=176, n=50, W=3.52
  * unseen_unprofiled: N=3639, n=50, W=72.78
  * Total unseen frame: N=3815, fixed sample n=100.
  * seen_profiled: N=986, n=25, W=39.44
  * seen_unprofiled: N=5946, n=25, W=237.84
  * Total full frame: N=10747, total sample n=150.
- Partial identification point bounds:
  * p_lower = sum_h(W_h * sum_i L_i) / 3815
  * p_upper = sum_h(W_h * sum_i U_i) / 3815
  * Fixed denominator preserved verbatim; never complete-case analysis.
- 2,000-draw recipe-cluster bootstrap (seed 20261021) with linear interpolation quantiles.
- PR10 Rater agreement: nominal Cohen's kappa >= 0.60, Wilson interval, reference-dispute diagnostic.
- H1 Decision: SUPPORT_H1 if CI_lower(p_lower) >= 0.15; REFUTE_H1 if CI_upper(p_upper) < 0.15; else INCONCLUSIVE.
"""
from __future__ import annotations

import dataclasses
from enum import Enum
import hashlib
import json
import math
import random
import re
from typing import Any, Dict, List, Mapping, Optional, Set, Tuple


PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
SPECIFICATION_ID = "iron-chef-l36-case-rule-v1"
SEED = 20261021
BOOTSTRAP_DRAWS = 2000
DECISION_THRESHOLD = 0.15
PR10_KAPPA_THRESHOLD = 0.60

FROZEN_FRAME_SIZES: Dict[str, int] = {
    "unseen_profiled": 176,
    "unseen_unprofiled": 3639,
    "seen_profiled": 986,
    "seen_unprofiled": 5946,
}

FROZEN_SAMPLE_SIZES: Dict[str, int] = {
    "unseen_profiled": 50,
    "unseen_unprofiled": 50,
    "seen_profiled": 25,
    "seen_unprofiled": 25,
}

TOTAL_FRAME = 10747
TOTAL_SAMPLE = 150
UNSEEN_FRAME = 3815   # 176 + 3639
UNSEEN_SAMPLE = 100   # 50 + 50

ALLOWED_LABELS = frozenset({"A", "C", "N", "U"})


class CaseValue(str, Enum):
    SUCCESS = "1"
    FAILURE = "0"
    UNKNOWN = "unknown"


class DecisionStatus(str, Enum):
    SUPPORT_H1 = "SUPPORT_H1"
    REFUTE_H1 = "REFUTE_H1"
    INCONCLUSIVE = "INCONCLUSIVE"
    JOINT_CLUSTER_HOLD = "JOINT_CLUSTER_HOLD"


@dataclasses.dataclass(frozen=True)
class CandidateJudgment:
    candidate_id: str
    label: str  # Must be in ALLOWED_LABELS or empty/unresolved


@dataclasses.dataclass(frozen=True)
class AuditCase:
    case_id: str
    stratum: str
    recipe_cluster_id: str
    candidate_labels: List[str]  # 5 labels on frozen top-5
    is_operational_failure: bool = False
    is_reference_disputed: bool = False  # e.g. both raters marked gold N


@dataclasses.dataclass(frozen=True)
class CaseEvaluationResult:
    case_id: str
    stratum: str
    recipe_cluster_id: str
    case_value: CaseValue
    l_i: int  # 1 if V=1 else 0
    u_i: int  # 1 if V in {1, unknown} else 0
    is_unknown: bool
    integrity_passed: bool
    failure_reason: Optional[str] = None


@dataclasses.dataclass(frozen=True)
class PartialIdentificationBounds:
    p_lower: float
    p_upper: float
    weighted_unknown_mass: float
    total_unseen_cases: int
    n_unknown_cases: int
    total_weighted_denominator: float
    stratum_sample_counts: Dict[str, int]


@dataclasses.dataclass(frozen=True)
class ConditionalPartialIdentificationBounds:
    """Descriptive H=0 point bounds with provenance for the membership input.

    This object intentionally contains no interval or H1 decision. The supplied
    source/receipt hashes identify the caller-verified membership provenance;
    this function validates their shape but cannot authenticate those receipts.
    """

    p_lower: Optional[float]
    p_upper: Optional[float]
    weighted_unknown_mass: Optional[float]
    assigned_unseen_cases: int
    h0_unseen_cases: int
    weighted_h0_denominator: float
    stratum_assigned_counts: Dict[str, int]
    stratum_h0_counts: Dict[str, int]
    membership_source_sha256: str
    membership_receipt_sha256: str
    membership_map_sha256: str
    status: str


@dataclasses.dataclass(frozen=True)
class BootstrapInterval:
    p_lower_ci_95: Tuple[float, float]
    p_upper_ci_95: Tuple[float, float]
    requested_draws: int
    valid_draws: int
    cluster_crosses_strata: bool


@dataclasses.dataclass(frozen=True)
class RaterAgreementReport:
    total_pairs: int
    exact_agreed: int
    agreement_rate: float
    agreement_ci_95: Tuple[float, float]
    cohens_kappa: float
    passes_pr10: bool
    reference_dispute_count: int


@dataclasses.dataclass(frozen=True)
class L36AuditReport:
    specification_id: str
    protocol_sha256: str
    partial_bounds: PartialIdentificationBounds
    bootstrap_interval: Optional[BootstrapInterval]
    h1_decision: DecisionStatus
    rater_agreement: Optional[RaterAgreementReport]
    sensitivity_a_plus_c_p_lower: float
    integrity_failures_count: int
    summary: str


def evaluate_case_rule(labels: List[str], is_operational_failure: bool = False) -> Tuple[CaseValue, bool, Optional[str]]:
    """Evaluates case-level V under iron-chef-l36-case-rule-v1.

    Rules:
    - Exactly 5 candidates required on the top-5 slate.
    - If operational failure: V = FAILURE (0), retains case in denominator.
    - If at least one 'A': V = SUCCESS (1), even if other candidates are 'U'.
    - If no 'A' and all 5 are in {'C', 'N'}: V = FAILURE (0).
    - If no 'A' and one or more are 'U' (or unresolved): V = UNKNOWN.
    """
    if is_operational_failure:
        return CaseValue.FAILURE, True, "Operational failure: retained in denominator as V=0"

    if len(labels) != 5:
        return CaseValue.FAILURE, False, f"Malformed slate length {len(labels)}, expected 5"

    clean_labels = [l.strip().upper() for l in labels]
    for l in clean_labels:
        if l not in ALLOWED_LABELS:
            # Unrecognized label is treated as unresolved/unknown
            pass

    # Check for any 'A'
    if any(l == "A" for l in clean_labels):
        return CaseValue.SUCCESS, True, None

    # Check if all are 'C' or 'N'
    if all(l in {"C", "N"} for l in clean_labels):
        return CaseValue.FAILURE, True, None

    # Contains 'U' or unresolved without any 'A'
    return CaseValue.UNKNOWN, True, None


def linear_quantile(values: List[float], q: float) -> float:
    """Computes quantile using linear interpolation (matching numpy method='linear')."""
    if not values:
        raise ValueError("Cannot compute quantile of empty list")
    if not (0.0 <= q <= 1.0):
        raise ValueError(f"Quantile q={q} must be between 0.0 and 1.0")

    sorted_vals = sorted(values)
    n = len(sorted_vals)
    if n == 1:
        return sorted_vals[0]

    # Continuous index
    idx = q * (n - 1)
    lo = int(math.floor(idx))
    hi = int(math.ceil(idx))
    if lo == hi:
        return sorted_vals[lo]
    frac = idx - lo
    return sorted_vals[lo] + frac * (sorted_vals[hi] - sorted_vals[lo])


def wilson_score_interval(successes: int, n: int, z: float = 1.959963984540054) -> Tuple[float, float]:
    """Computes exact two-sided Wilson score confidence interval."""
    if n <= 0:
        return 0.0, 0.0
    p = successes / n
    z2 = z * z
    denom = 1.0 + z2 / n
    center = (p + z2 / (2.0 * n)) / denom
    margin = z * math.sqrt(p * (1.0 - p) / n + z2 / (4.0 * n * n)) / denom
    lower = 0.0 if successes == 0 else max(0.0, center - margin)
    upper = 1.0 if successes == n else min(1.0, center + margin)
    return lower, upper


def cohens_kappa(pairs: List[Tuple[str, str]]) -> float:
    """Computes unweighted Cohen's kappa across 4 categories {'A', 'C', 'N', 'U'}."""
    if not pairs:
        return 0.0
    counts: Dict[str, List[int]] = {label: [0, 0] for label in ALLOWED_LABELS}
    observed = 0
    for left, right in pairs:
        l = left.strip().upper()
        r = right.strip().upper()
        if l not in ALLOWED_LABELS or r not in ALLOWED_LABELS:
            raise ValueError(f"Label outside frozen vocabulary: ({left}, {right})")
        counts[l][0] += 1
        counts[r][1] += 1
        if l == r:
            observed += 1

    n = len(pairs)
    po = observed / n
    pe = sum(a * b for a, b in counts.values()) / (n * n)
    if math.isclose(pe, 1.0):
        return 1.0 if math.isclose(po, 1.0) else 0.0
    return (po - pe) / (1.0 - pe)


def compute_partial_bounds(cases: List[AuditCase]) -> Tuple[PartialIdentificationBounds, List[CaseEvaluationResult], float]:
    """Computes fixed-denominator partial identification bounds over the unseen strata.

    Population: unseen_profiled (N=176, n=50) and unseen_unprofiled (N=3639, n=50).
    Total unseen frame = 3,815.
    """
    unseen_cases = [c for c in cases if c.stratum in {"unseen_profiled", "unseen_unprofiled"}]
    eval_results: List[CaseEvaluationResult] = []
    
    # Track stratum counts
    stratum_counts: Dict[str, int] = {"unseen_profiled": 0, "unseen_unprofiled": 0}
    a_plus_c_successes: Dict[str, int] = {"unseen_profiled": 0, "unseen_unprofiled": 0}

    for c in unseen_cases:
        stratum_counts[c.stratum] = stratum_counts.get(c.stratum, 0) + 1
        v, ok, err = evaluate_case_rule(c.candidate_labels, c.is_operational_failure)
        l_i = 1 if v == CaseValue.SUCCESS else 0
        u_i = 1 if v in {CaseValue.SUCCESS, CaseValue.UNKNOWN} else 0
        is_unk = (v == CaseValue.UNKNOWN)
        eval_results.append(CaseEvaluationResult(
            case_id=c.case_id,
            stratum=c.stratum,
            recipe_cluster_id=c.recipe_cluster_id,
            case_value=v,
            l_i=l_i,
            u_i=u_i,
            is_unknown=is_unk,
            integrity_passed=ok,
            failure_reason=err,
        ))

        # Check A+C sensitivity: at least one A or C
        if any(lbl.strip().upper() in {"A", "C"} for lbl in c.candidate_labels):
            a_plus_c_successes[c.stratum] += 1

    # Fixed frame weights
    # W_h = N_h / n_h
    # p_lower = sum_h (W_h * sum_{i in h} L_i) / sum_h (W_h * n_h)
    # sum_h (W_h * n_h) = sum_h N_h = 3815
    sum_w_l = 0.0
    sum_w_u = 0.0
    sum_w_ac = 0.0
    total_weighted_denom = 0.0

    for stratum_name in ("unseen_profiled", "unseen_unprofiled"):
        n_h = stratum_counts.get(stratum_name, 0)
        n_frame = FROZEN_FRAME_SIZES[stratum_name]
        if n_h == 0:
            continue
        w_h = n_frame / n_h
        total_weighted_denom += w_h * n_h

        stratum_cases = [r for r in eval_results if r.stratum == stratum_name]
        l_sum = sum(r.l_i for r in stratum_cases)
        u_sum = sum(r.u_i for r in stratum_cases)
        sum_w_l += w_h * l_sum
        sum_w_u += w_h * u_sum
        sum_w_ac += w_h * a_plus_c_successes.get(stratum_name, 0)

    if total_weighted_denom <= 0.0:
        p_lower = 0.0
        p_upper = 0.0
        p_ac = 0.0
    else:
        p_lower = sum_w_l / total_weighted_denom
        p_upper = sum_w_u / total_weighted_denom
        p_ac = sum_w_ac / total_weighted_denom

    weighted_unk_mass = p_upper - p_lower
    n_unknown = sum(1 for r in eval_results if r.is_unknown)

    bounds = PartialIdentificationBounds(
        p_lower=p_lower,
        p_upper=p_upper,
        weighted_unknown_mass=weighted_unk_mass,
        total_unseen_cases=len(unseen_cases),
        n_unknown_cases=n_unknown,
        total_weighted_denominator=total_weighted_denom,
        stratum_sample_counts=stratum_counts,
    )
    return bounds, eval_results, p_ac


def compute_conditional_partial_bounds(
    cases: List[AuditCase],
    h0_by_case: Mapping[str, bool],
    *,
    membership_source_sha256: str,
    membership_receipt_sha256: str,
) -> ConditionalPartialIdentificationBounds:
    """Compute descriptive weighted partial bounds conditional on H=0.

    ``h0_by_case`` must be the caller-verified gold-not-in-frozen-top-five map
    for the complete unseen cohort. Hashes are recorded as provenance pins, but
    their authenticity and relationship to the map must be verified by the
    caller; this function does not load private inputs or claim interval
    coverage. All unseen assigned cases remain in the cohort counts.
    """
    unseen_cases = [c for c in cases if c.stratum in {"unseen_profiled", "unseen_unprofiled"}]
    case_ids = [c.case_id for c in unseen_cases]
    if len(case_ids) != len(set(case_ids)):
        raise ValueError("Unseen cohort contains duplicate case identities")

    for digest_name, digest in (
        ("membership_source_sha256", membership_source_sha256),
        ("membership_receipt_sha256", membership_receipt_sha256),
    ):
        if not isinstance(digest, str) or re.fullmatch(r"[0-9a-fA-F]{64}", digest) is None:
            raise ValueError(f"{digest_name} must be a 64-character SHA-256 hex digest")

    expected_ids = set(case_ids)
    supplied_ids = set(h0_by_case)
    if expected_ids != supplied_ids:
        raise ValueError(
            "H=0 membership must cover exactly the unseen cohort "
            f"(missing={len(expected_ids - supplied_ids)}, extra={len(supplied_ids - expected_ids)})"
        )
    if any(type(value) is not bool for value in h0_by_case.values()):
        raise ValueError("H=0 membership values must be explicit booleans")

    stratum_cases: Dict[str, List[AuditCase]] = {
        name: [c for c in unseen_cases if c.stratum == name]
        for name in ("unseen_profiled", "unseen_unprofiled")
    }
    assigned_counts = {name: len(rows) for name, rows in stratum_cases.items()}
    if assigned_counts != {name: FROZEN_SAMPLE_SIZES[name] for name in stratum_cases}:
        raise ValueError("Unseen case counts do not match the frozen stratified allocation")

    h0_counts = {
        name: sum(1 for case in rows if h0_by_case[case.case_id])
        for name, rows in stratum_cases.items()
    }
    weighted_denominator = 0.0
    weighted_lower_numerator = 0.0
    weighted_upper_numerator = 0.0

    for stratum_name, rows in stratum_cases.items():
        frame_n = FROZEN_FRAME_SIZES[stratum_name]
        sample_n = FROZEN_SAMPLE_SIZES[stratum_name]
        weight = frame_n / sample_n
        for case in rows:
            if not h0_by_case[case.case_id]:
                continue
            value, integrity_passed, _ = evaluate_case_rule(
                case.candidate_labels, case.is_operational_failure
            )
            if not integrity_passed:
                raise ValueError("Malformed unseen case prevents conditional estimation")
            lower = 1 if value == CaseValue.SUCCESS else 0
            upper = 1 if value in {CaseValue.SUCCESS, CaseValue.UNKNOWN} else 0
            weighted_denominator += weight
            weighted_lower_numerator += weight * lower
            weighted_upper_numerator += weight * upper

    membership_payload = json.dumps(
        sorted((case_id, h0_by_case[case_id]) for case_id in case_ids),
        ensure_ascii=False,
        separators=(",", ":"),
    ).encode("utf-8")
    membership_map_sha256 = hashlib.sha256(membership_payload).hexdigest()

    if weighted_denominator == 0.0:
        p_lower = p_upper = unknown_mass = None
        status = "UNAVAILABLE_ZERO_H0_WEIGHT"
    else:
        p_lower = weighted_lower_numerator / weighted_denominator
        p_upper = weighted_upper_numerator / weighted_denominator
        unknown_mass = p_upper - p_lower
        status = "AVAILABLE_DESCRIPTIVE_ONLY"

    return ConditionalPartialIdentificationBounds(
        p_lower=p_lower,
        p_upper=p_upper,
        weighted_unknown_mass=unknown_mass,
        assigned_unseen_cases=len(unseen_cases),
        h0_unseen_cases=sum(h0_counts.values()),
        weighted_h0_denominator=weighted_denominator,
        stratum_assigned_counts=assigned_counts,
        stratum_h0_counts=h0_counts,
        membership_source_sha256=membership_source_sha256.lower(),
        membership_receipt_sha256=membership_receipt_sha256.lower(),
        membership_map_sha256=membership_map_sha256,
        status=status,
    )


def run_cluster_bootstrap(
    eval_results: List[CaseEvaluationResult],
    n_draws: int = BOOTSTRAP_DRAWS,
    seed: int = SEED,
) -> BootstrapInterval:
    """Runs 2,000-draw recipe-cluster bootstrap resampling across unseen strata.

    Invariant: If any recipe cluster crosses both unseen strata, returns
    cluster_crosses_strata=True to signal JOINT_CLUSTER_HOLD.
    """
    rng = random.Random(seed)

    # Check cluster cross-stratum integrity
    clusters_by_stratum: Dict[str, Set[str]] = {
        "unseen_profiled": set(),
        "unseen_unprofiled": set(),
    }
    cases_by_stratum_cluster: Dict[str, Dict[str, List[CaseEvaluationResult]]] = {
        "unseen_profiled": {},
        "unseen_unprofiled": {},
    }

    for r in eval_results:
        if r.stratum in clusters_by_stratum:
            clusters_by_stratum[r.stratum].add(r.recipe_cluster_id)
            if r.recipe_cluster_id not in cases_by_stratum_cluster[r.stratum]:
                cases_by_stratum_cluster[r.stratum][r.recipe_cluster_id] = []
            cases_by_stratum_cluster[r.stratum][r.recipe_cluster_id].append(r)

    # Cross stratum check
    overlap = clusters_by_stratum["unseen_profiled"].intersection(clusters_by_stratum["unseen_unprofiled"])
    if overlap:
        return BootstrapInterval(
            p_lower_ci_95=(0.0, 0.0),
            p_upper_ci_95=(0.0, 0.0),
            requested_draws=n_draws,
            valid_draws=0,
            cluster_crosses_strata=True,
        )

    # Perform bootstrap draws
    p_lower_draws: List[float] = []
    p_upper_draws: List[float] = []

    profiled_clusters = sorted(clusters_by_stratum["unseen_profiled"])
    unprofiled_clusters = sorted(clusters_by_stratum["unseen_unprofiled"])

    n_p_clusters = len(profiled_clusters)
    n_u_clusters = len(unprofiled_clusters)

    n_frame_p = FROZEN_FRAME_SIZES["unseen_profiled"]
    n_frame_u = FROZEN_FRAME_SIZES["unseen_unprofiled"]
    total_n_frame = n_frame_p + n_frame_u

    for _ in range(n_draws):
        # Sample clusters with replacement
        sampled_p_clusters = [rng.choice(profiled_clusters) for _ in range(n_p_clusters)]
        sampled_u_clusters = [rng.choice(unprofiled_clusters) for _ in range(n_u_clusters)]

        # Aggregate L and U in profiled replicate
        rep_p_cases = [case for c_id in sampled_p_clusters for case in cases_by_stratum_cluster["unseen_profiled"][c_id]]
        rep_u_cases = [case for c_id in sampled_u_clusters for case in cases_by_stratum_cluster["unseen_unprofiled"][c_id]]

        if not rep_p_cases or not rep_u_cases:
            continue

        ratio_l_p = sum(c.l_i for c in rep_p_cases) / len(rep_p_cases)
        ratio_u_p = sum(c.u_i for c in rep_p_cases) / len(rep_p_cases)

        ratio_l_u = sum(c.l_i for c in rep_u_cases) / len(rep_u_cases)
        ratio_u_u = sum(c.u_i for c in rep_u_cases) / len(rep_u_cases)

        # Combine with fixed frame weights N_h
        draw_p_lower = (n_frame_p * ratio_l_p + n_frame_u * ratio_l_u) / total_n_frame
        draw_p_upper = (n_frame_p * ratio_u_p + n_frame_u * ratio_u_u) / total_n_frame

        p_lower_draws.append(draw_p_lower)
        p_upper_draws.append(draw_p_upper)

    if len(p_lower_draws) < 20:
        return BootstrapInterval(
            p_lower_ci_95=(0.0, 0.0),
            p_upper_ci_95=(0.0, 0.0),
            requested_draws=n_draws,
            valid_draws=len(p_lower_draws),
            cluster_crosses_strata=False,
        )

    p_lower_ci = (linear_quantile(p_lower_draws, 0.025), linear_quantile(p_lower_draws, 0.975))
    p_upper_ci = (linear_quantile(p_upper_draws, 0.025), linear_quantile(p_upper_draws, 0.975))

    return BootstrapInterval(
        p_lower_ci_95=p_lower_ci,
        p_upper_ci_95=p_upper_ci,
        requested_draws=n_draws,
        valid_draws=len(p_lower_draws),
        cluster_crosses_strata=False,
    )


def evaluate_audit_estimand(
    cases: List[AuditCase],
    rater_pairs: Optional[List[Tuple[str, str]]] = None,
    n_bootstrap_draws: int = BOOTSTRAP_DRAWS,
    seed: int = SEED,
) -> L36AuditReport:
    """Full L36 audit evaluation combining partial-identification bounds, bootstrap, and decision rule."""
    bounds, eval_results, p_ac = compute_partial_bounds(cases)
    bootstrap = run_cluster_bootstrap(eval_results, n_draws=n_bootstrap_draws, seed=seed)

    # Determine H1 decision
    if bootstrap.cluster_crosses_strata:
        decision = DecisionStatus.JOINT_CLUSTER_HOLD
    elif bootstrap.valid_draws < 20:
        decision = DecisionStatus.INCONCLUSIVE
    elif bootstrap.p_lower_ci_95[0] >= DECISION_THRESHOLD:
        decision = DecisionStatus.SUPPORT_H1
    elif bootstrap.p_upper_ci_95[1] < DECISION_THRESHOLD:
        decision = DecisionStatus.REFUTE_H1
    else:
        decision = DecisionStatus.INCONCLUSIVE

    # Rater agreement if provided
    agreement_report: Optional[RaterAgreementReport] = None
    if rater_pairs:
        agreed = sum(1 for a, b in rater_pairs if a.strip().upper() == b.strip().upper())
        total_p = len(rater_pairs)
        rate = agreed / total_p if total_p > 0 else 0.0
        ci_rate = wilson_score_interval(agreed, total_p)
        kappa = cohens_kappa(rater_pairs)
        passes = kappa >= PR10_KAPPA_THRESHOLD
        
        # Disputed references (both N on gold candidate)
        disputed = sum(1 for c in cases if c.is_reference_disputed)

        agreement_report = RaterAgreementReport(
            total_pairs=total_p,
            exact_agreed=agreed,
            agreement_rate=rate,
            agreement_ci_95=ci_rate,
            cohens_kappa=kappa,
            passes_pr10=passes,
            reference_dispute_count=disputed,
        )

    # Integrity failures count
    integrity_failures = sum(1 for r in eval_results if not r.integrity_passed)

    summary_lines = [
        f"L36 Audit Estimand Report [{SPECIFICATION_ID}]",
        f"Protocol v1 SHA-256: {PROTOCOL_SHA256}",
        f"Unseen cases evaluated: {bounds.total_unseen_cases} (weighted denominator: {bounds.total_weighted_denominator:.1f})",
        f"Partial identification bounds: [{bounds.p_lower:.4f}, {bounds.p_upper:.4f}] (weighted unknown mass: {bounds.weighted_unknown_mass:.4f}, n_unknown: {bounds.n_unknown_cases})",
        f"Sensitivity A+C p_lower: {p_ac:.4f}",
    ]
    if bootstrap and not bootstrap.cluster_crosses_strata and bootstrap.valid_draws >= 20:
        summary_lines.append(
            f"Bootstrap 95% CIs: p_lower in [{bootstrap.p_lower_ci_95[0]:.4f}, {bootstrap.p_lower_ci_95[1]:.4f}], "
            f"p_upper in [{bootstrap.p_upper_ci_95[0]:.4f}, {bootstrap.p_upper_ci_95[1]:.4f}]"
        )
    summary_lines.append(f"H1 Decision: {decision.value} (threshold={DECISION_THRESHOLD})")
    if agreement_report:
        summary_lines.append(
            f"Rater Agreement: {agreement_report.exact_agreed}/{agreement_report.total_pairs} ({agreement_report.agreement_rate:.1%}), "
            f"kappa={agreement_report.cohens_kappa:.4f} (PR10 {'PASS' if agreement_report.passes_pr10 else 'FAIL'})"
        )

    return L36AuditReport(
        specification_id=SPECIFICATION_ID,
        protocol_sha256=PROTOCOL_SHA256,
        partial_bounds=bounds,
        bootstrap_interval=bootstrap,
        h1_decision=decision,
        rater_agreement=agreement_report,
        sensitivity_a_plus_c_p_lower=p_ac,
        integrity_failures_count=integrity_failures,
        summary="\n".join(summary_lines),
    )
