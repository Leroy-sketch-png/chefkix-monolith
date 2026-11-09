"""
T54 Method-v2 Scorer, Group-Split, and Conformal Calibration Processor.

Implements the Lead-owned specification:
`evidence/iron-chef-lead-member-handoff-20261105/t54-method-v2-producer-contract-20261106.md`
governed by Campaign Protocol v1 (SHA-256 b2952599099c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12)
and Academic Vision and Strategy Sections 3, 6-8, and 17.

Invariants:
1. Recipe-grouped deterministic split: `c3-recipe-group-sha256-split-v1`.
   G >= 4 unique recipe IDs required.
   Digest = SHA256(b"iron-chef:c3-rank-conformal-receipt-v2:recipe-split:20261021\\0" + recipe_id.encode("utf-8")).
   Sorted by raw digest bytes ascending, recipe ID ascending as tie-break.
   floor(3*G/10) to development, remaining to calibration.
2. Request-time cells: Baseline-0 (all-input) and Partition-1 (4 cells: train x profile presence).
   Derived from frozen validation frame before outcomes.
   If n < 200 calibration cases in a cell, no_guarantee is emitted.
3. Rank & score semantics:
   Pool -> hard filter -> rank -> truncate to M (primary M=20, sensitivity M=50).
   Conformal score = 1-based rank (1..M) of first candidate satisfying label contract.
   Outcome codes:
   - finite rank 1..M: candidate satisfies contract, score = rank, retained.
   - infinity + outcome_code="no_reference_match": valid computation, nonempty top-M, no match. Retained.
   - infinity + outcome_code="empty_filtered_pool": hard filter leaves no candidate. Serving is abstain. Retained.
   - infinity + failure=True (code scoring_failure/method_failure): operational failure. Retained and counted.
4. Finite-sample order statistic:
   k = ceil((n + 1) * (1 - alpha)).
5. Exchangeability Gate:
   Case-level exchangeability is unproven -> product tHat remains null (None),
   and status is no_guarantee with reason "inference_assumptions_unresolved".
6. Nine-role provenance binding:
   Validates graph_manifest, hard_filter, ranker, scorer, score_contract, label_contract,
   split_assignment, identity_index, calibration_scores.
"""

from __future__ import annotations

import dataclasses
import hashlib
import math
from typing import Any, Dict, List, Optional, Set, Tuple, Union


SPLIT_DOMAIN_PREFIX = b"iron-chef:c3-rank-conformal-receipt-v2:recipe-split:20261021\0"
SPLIT_ALGORITHM_ID = "c3-recipe-group-sha256-split-v1"

LABEL_CONTRACT_RECORDED_EXACT = "recorded-substitute-exact-identity-v1"

PARTITION_1_CELLS: Tuple[str, ...] = (
    "train_yes_profile_yes",
    "train_yes_profile_no",
    "train_no_profile_yes",
    "train_no_profile_no",
)
VALID_CELLS: Tuple[str, ...] = ("baseline_0",) + PARTITION_1_CELLS

REQUIRED_ROLES: Tuple[str, ...] = (
    "graph_manifest",
    "hard_filter",
    "ranker",
    "scorer",
    "score_contract",
    "label_contract",
    "split_assignment",
    "identity_index",
    "calibration_scores",
)


def compute_recipe_split_digest(recipe_id: str) -> bytes:
    """Computes SHA-256 for recipe group split assignment."""
    hasher = hashlib.sha256()
    hasher.update(SPLIT_DOMAIN_PREFIX)
    hasher.update(recipe_id.encode("utf-8"))
    return hasher.digest()


@dataclasses.dataclass(frozen=True)
class RecipeGroupSplitResult:
    total_recipe_groups: int
    dev_group_count: int
    cal_group_count: int
    recipe_to_split: Dict[str, str]  # recipe_id -> "development" | "calibration"
    global_assignment_digest: str


def execute_recipe_group_split(recipe_ids: Set[str]) -> RecipeGroupSplitResult:
    """
    Executes c3-recipe-group-sha256-split-v1 over distinct recipe IDs.
    Requires G >= 4; otherwise refuses.
    """
    g = len(recipe_ids)
    if g < 4:
        raise ValueError(f"c3-recipe-group-sha256-split-v1 requires G >= 4 distinct recipe IDs, got {g}")

    # For each recipe ID, compute digest and sort
    sorted_groups = sorted(
        recipe_ids,
        key=lambda r_id: (compute_recipe_split_digest(r_id), r_id.encode("utf-8")),
    )

    dev_count = math.floor(3 * g / 10)
    dev_groups = set(sorted_groups[:dev_count])
    cal_groups = set(sorted_groups[dev_count:])

    recipe_to_split: Dict[str, str] = {}
    for r_id in sorted_groups:
        recipe_to_split[r_id] = "development" if r_id in dev_groups else "calibration"

    # Global assignment digest over deterministic serialization: recipe_id\t split\n
    hasher = hashlib.sha256()
    for r_id in sorted_groups:
        split_name = recipe_to_split[r_id]
        hasher.update(f"{r_id}\t{split_name}\n".encode("utf-8"))
    assignment_digest = hasher.hexdigest()

    return RecipeGroupSplitResult(
        total_recipe_groups=g,
        dev_group_count=dev_count,
        cal_group_count=g - dev_count,
        recipe_to_split=recipe_to_split,
        global_assignment_digest=assignment_digest,
    )


@dataclasses.dataclass(frozen=True)
class CaseValidationRecord:
    case_id: str
    recipe_id: str
    train_source_presence: bool
    foodb_profile_presence: bool

    def get_cell_id(self) -> str:
        train_str = "train_yes" if self.train_source_presence else "train_no"
        profile_str = "profile_yes" if self.foodb_profile_presence else "profile_no"
        return f"{train_str}_{profile_str}"


@dataclasses.dataclass(frozen=True)
class CalibrationScoreRow:
    case_id: str
    cell_id: str
    score: Union[int, float, str, None]  # 1..M, "infinity", or None if failure
    failure: bool
    outcome_code: Optional[str] = None  # "no_reference_match", "empty_filtered_pool", "scoring_failure", "method_failure"

    def effective_conformal_score(self) -> float:
        """
        Conformal score value:
        - 1..M: returns float(rank)
        - "infinity", None with failure: returns float("inf")
        """
        if self.failure:
            return float("inf")
        if isinstance(self.score, (int, float)):
            return float(self.score)
        if self.score == "infinity":
            return float("inf")
        return float("inf")

    def is_empty_pool_abstain(self) -> bool:
        return (not self.failure) and (self.outcome_code == "empty_filtered_pool")


@dataclasses.dataclass
class ConformalEvaluationResult:
    cell_id: str
    alpha: float
    m_cutoff: int
    n_calibration: int
    finite_score_count: int
    no_reference_match_count: int
    empty_pool_abstain_count: int
    failure_count: int
    k_order_statistic: int
    sample_cutoff_descriptive: Optional[float]
    sample_empirical_coverage: float
    guarantee_status: str  # "ok" | "no_guarantee"
    no_guarantee_reason: Optional[str]
    product_t_hat: Optional[float]  # Null when exchangeability unproven or n < 200


def evaluate_conformal_calibration(
    score_rows: List[CalibrationScoreRow],
    cell_id: str,
    alpha: float,
    m_cutoff: int = 20,
    assume_exchangeability: bool = False,
) -> ConformalEvaluationResult:
    """
    Computes order statistic and evaluates conformal calibration gate.

    Invariants:
    - n is the distinct calibration cases in the cell.
    - If n < 200, guarantee_status = "no_guarantee", reason = "insufficient_sample_size", product_t_hat = None.
    - If exchangeability unproven (assume_exchangeability == False),
      product_t_hat = None, guarantee_status = "no_guarantee",
      reason = "inference_assumptions_unresolved".
    - Scores: finite ranks (1..M), +infinity (no_match, empty_pool, failure).
    - k = ceil((n + 1) * (1 - alpha)).
    """
    if cell_id not in VALID_CELLS:
        raise ValueError(f"Invalid cell_id '{cell_id}', must be in {VALID_CELLS}")

    cell_rows = [r for r in score_rows if cell_id == "baseline_0" or r.cell_id == cell_id]
    n = len(cell_rows)

    finite_count = sum(1 for r in cell_rows if not r.failure and isinstance(r.score, (int, float)))
    no_match_count = sum(1 for r in cell_rows if not r.failure and r.outcome_code == "no_reference_match")
    empty_pool_count = sum(1 for r in cell_rows if r.is_empty_pool_abstain())
    failure_count = sum(1 for r in cell_rows if r.failure)

    if n == 0:
        return ConformalEvaluationResult(
            cell_id=cell_id,
            alpha=alpha,
            m_cutoff=m_cutoff,
            n_calibration=0,
            finite_score_count=0,
            no_reference_match_count=0,
            empty_pool_abstain_count=0,
            failure_count=0,
            k_order_statistic=0,
            sample_cutoff_descriptive=None,
            sample_empirical_coverage=0.0,
            guarantee_status="no_guarantee",
            no_guarantee_reason="empty_cell",
            product_t_hat=None,
        )

    # Order statistic calculation
    k = math.ceil((n + 1) * (1.0 - alpha))
    effective_scores = sorted(r.effective_conformal_score() for r in cell_rows)

    if k <= n:
        sample_cutoff = effective_scores[k - 1]
    else:
        sample_cutoff = float("inf")

    # Sample empirical coverage: fraction with score <= sample_cutoff (finite only, inf never covered)
    covered_count = sum(
        1 for s in effective_scores if s <= sample_cutoff and not math.isinf(s)
    )
    empirical_coverage = covered_count / n

    # Gate evaluation
    if n < 200:
        return ConformalEvaluationResult(
            cell_id=cell_id,
            alpha=alpha,
            m_cutoff=m_cutoff,
            n_calibration=n,
            finite_score_count=finite_count,
            no_reference_match_count=no_match_count,
            empty_pool_abstain_count=empty_pool_count,
            failure_count=failure_count,
            k_order_statistic=k,
            sample_cutoff_descriptive=sample_cutoff,
            sample_empirical_coverage=empirical_coverage,
            guarantee_status="no_guarantee",
            no_guarantee_reason="insufficient_sample_size_below_200",
            product_t_hat=None,
        )

    if not assume_exchangeability:
        return ConformalEvaluationResult(
            cell_id=cell_id,
            alpha=alpha,
            m_cutoff=m_cutoff,
            n_calibration=n,
            finite_score_count=finite_count,
            no_reference_match_count=no_match_count,
            empty_pool_abstain_count=empty_pool_count,
            failure_count=failure_count,
            k_order_statistic=k,
            sample_cutoff_descriptive=sample_cutoff,
            sample_empirical_coverage=empirical_coverage,
            guarantee_status="no_guarantee",
            no_guarantee_reason="inference_assumptions_unresolved",
            product_t_hat=None,
        )

    # If all gates passed and exchangeability proved
    t_hat = sample_cutoff if not math.isinf(sample_cutoff) else None
    guar_status = "ok" if t_hat is not None else "no_guarantee"
    reason = None if guar_status == "ok" else "infinite_threshold"

    return ConformalEvaluationResult(
        cell_id=cell_id,
        alpha=alpha,
        m_cutoff=m_cutoff,
        n_calibration=n,
        finite_score_count=finite_count,
        no_reference_match_count=no_match_count,
        empty_pool_abstain_count=empty_pool_count,
        failure_count=failure_count,
        k_order_statistic=k,
        sample_cutoff_descriptive=sample_cutoff,
        sample_empirical_coverage=empirical_coverage,
        guarantee_status=guar_status,
        no_guarantee_reason=reason,
        product_t_hat=t_hat,
    )


def verify_nine_role_bindings(
    role_digests: Dict[str, str],
    expected_digests: Optional[Dict[str, str]] = None,
) -> Tuple[bool, str, List[str]]:
    """
    Verifies the 9 required provenance roles:
    graph_manifest, hard_filter, ranker, scorer, score_contract, label_contract,
    split_assignment, identity_index, calibration_scores.
    """
    missing_roles = [r for r in REQUIRED_ROLES if r not in role_digests or not role_digests[r]]
    if missing_roles:
        return False, "missing_roles", [f"Missing required role: {r}" for r in missing_roles]

    if expected_digests:
        mismatched = []
        for role, exp_hash in expected_digests.items():
            actual = role_digests.get(role, "").strip().lower()
            if actual != exp_hash.strip().lower():
                mismatched.append(f"Role {role} hash mismatch: expected {exp_hash}, got {actual}")
        if mismatched:
            return False, "mismatched_roles", mismatched

    return True, "verified", []
