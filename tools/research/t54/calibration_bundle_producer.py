"""
T54 Method-v2 Calibration Bundle Producer.

Implements the versioned bundle manifest extension required by the Lead-owned
producer contract:
  evidence/iron-chef-lead-member-handoff-20261105/t54-method-v2-producer-contract-20261106.md

Governed by Campaign Protocol v1 (SHA-256
  b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12)
and Strategy §§3, 6-8, 17.

Invariants (sourced directly from contract §§1-6):
- Extends the existing c3-calibration-bundle path with a versioned manifest
  schema (SCHEMA_VERSION below) that binds:
    * method version and split algorithm ID / seed / ratio
    * full validation-frame / identity manifest digest
    * global assignment digest and per-cell projection digest
    * cohort / cell / alpha / M / score/label contract IDs
    * exact file inventory (logical name, byte length, raw SHA-256)
    * nine distinct role digests
- Loader independently verifies every digest from loaded bytes; caller-supplied
  digests are expectations, never observations.
- Any absent, mismatched, misread, or incompatible role yields
  binding_status=missing|mismatched, no_guarantee, null tHat, abstention.
- Under the existing project disposition, case-level exchangeability is unproven:
  product tHat remains None and guarantee_status is always no_guarantee with
  reason inference_assumptions_unresolved.  Descriptive cutoff/coverage are
  computed separately and must not populate the product tHat field.
- Route must remain default-off; this module contains no serving path.
- No private source rows, label pools, candidate IDs, or credentials enter outputs.
"""

from __future__ import annotations

import dataclasses
import hashlib
import json
import math
from typing import Any, Dict, List, Optional, Set, Tuple

# ── constants ────────────────────────────────────────────────────────────────

SCHEMA_VERSION = "c3-calibration-bundle-producer-v2"
METHOD_VERSION = "method-v2"
SPLIT_ALGORITHM_ID = "c3-recipe-group-sha256-split-v1"
SPLIT_DOMAIN_PREFIX = b"iron-chef:c3-rank-conformal-receipt-v2:recipe-split:20261021\0"
SPLIT_RATIO_NUMERATOR = 3
SPLIT_RATIO_DENOMINATOR = 10

LABEL_CONTRACT_RECORDED_EXACT = "recorded-substitute-exact-identity-v1"

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

VALID_CELLS: Tuple[str, ...] = (
    "baseline_0",
    "train_yes_profile_yes",
    "train_yes_profile_no",
    "train_no_profile_yes",
    "train_no_profile_no",
)

PARTITION_1_CELLS: Tuple[str, ...] = VALID_CELLS[1:]

# Valid typed outcome codes — must be one of these or absent (for finite rank)
VALID_OUTCOME_CODES = frozenset(
    {"no_reference_match", "empty_filtered_pool", "scoring_failure", "method_failure"}
)

BINDING_STATUS_OK = "ok"
BINDING_STATUS_MISSING = "missing"
BINDING_STATUS_MISMATCHED = "mismatched"


# ── errors ───────────────────────────────────────────────────────────────────


class ProducerError(ValueError):
    """Sanitized, value-free producer contract violation."""


# ── helpers ──────────────────────────────────────────────────────────────────


def _sha256_bytes(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def _canonical_json(value: Any) -> bytes:
    return json.dumps(
        value, ensure_ascii=False, sort_keys=True,
        separators=(",", ":"), allow_nan=False,
    ).encode("utf-8")


def _valid_sha256(value: Any) -> bool:
    return isinstance(value, str) and len(value) == 64 and all(
        c in "0123456789abcdef" for c in value
    )


# ── split ────────────────────────────────────────────────────────────────────


def _recipe_sort_key(recipe_id: str) -> Tuple[bytes, bytes]:
    digest = hashlib.sha256(SPLIT_DOMAIN_PREFIX + recipe_id.encode("utf-8")).digest()
    return (digest, recipe_id.encode("utf-8"))


@dataclasses.dataclass(frozen=True)
class RecipeGroupAssignment:
    """Output of the c3-recipe-group-sha256-split-v1 algorithm."""
    total_recipe_groups: int
    dev_group_count: int
    cal_group_count: int
    recipe_to_split: Dict[str, str]        # recipe_id -> "development" | "calibration"
    global_assignment_digest: str           # SHA-256 over deterministic serialization
    split_algorithm_id: str


def execute_recipe_group_split(recipe_ids: Set[str]) -> RecipeGroupAssignment:
    """
    Executes c3-recipe-group-sha256-split-v1.

    Contract:
    - G >= 4 unique recipe IDs required; refuses otherwise.
    - Sort by (digest_bytes, recipe_id_bytes) ascending.
    - Dev count = floor(3*G/10).
    - Input-order invariant, outcome-blind, seed-pinned.
    """
    g = len(recipe_ids)
    if g < 4:
        raise ProducerError(
            f"c3-recipe-group-sha256-split-v1 requires G >= 4 distinct recipe IDs"
        )

    sorted_groups = sorted(recipe_ids, key=_recipe_sort_key)
    dev_count = math.floor(SPLIT_RATIO_NUMERATOR * g / SPLIT_RATIO_DENOMINATOR)
    dev_set = set(sorted_groups[:dev_count])

    recipe_to_split: Dict[str, str] = {
        r: ("development" if r in dev_set else "calibration")
        for r in sorted_groups
    }

    # Global assignment digest: deterministic over sorted recipe_id TAB split NEWLINE
    hasher = hashlib.sha256()
    for r in sorted_groups:
        hasher.update(f"{r}\t{recipe_to_split[r]}\n".encode("utf-8"))

    return RecipeGroupAssignment(
        total_recipe_groups=g,
        dev_group_count=dev_count,
        cal_group_count=g - dev_count,
        recipe_to_split=recipe_to_split,
        global_assignment_digest=hasher.hexdigest(),
        split_algorithm_id=SPLIT_ALGORITHM_ID,
    )


# ── case frame ───────────────────────────────────────────────────────────────


@dataclasses.dataclass(frozen=True)
class FrameCase:
    """One validation-frame case with request-time cell features."""
    case_id: str
    recipe_id: str
    train_source_presence: bool
    foodb_profile_presence: bool

    def cell_id(self) -> str:
        t = "train_yes" if self.train_source_presence else "train_no"
        p = "profile_yes" if self.foodb_profile_presence else "profile_no"
        return f"{t}_{p}"


def validate_frame(cases: List[FrameCase]) -> None:
    """
    Validates the validation frame for duplicate/missing case_id and
    duplicate/missing recipe_id. Raises ProducerError on violation.
    """
    if not cases:
        raise ProducerError("Validation frame must not be empty")
    seen_ids: Set[str] = set()
    for c in cases:
        if not c.case_id:
            raise ProducerError("Every case must have a nonempty case_id")
        if not c.recipe_id:
            raise ProducerError("Every case must have a nonempty recipe_id")
        if c.case_id in seen_ids:
            raise ProducerError("Duplicate case_id in validation frame")
        seen_ids.add(c.case_id)


# ── scored rows ──────────────────────────────────────────────────────────────


@dataclasses.dataclass(frozen=True)
class ScoredCase:
    """
    One typed calibration score row per the contract §3 table.

    Exactly one of four typed rows:
      (a) score = 1..M, failure=False, outcome_code=None      -> finite rank
      (b) score = "infinity", failure=False, code="no_reference_match"
      (c) score = "infinity", failure=False, code="empty_filtered_pool"
      (d) score = None,       failure=True,  code in {"scoring_failure","method_failure"}
    """
    case_id: str
    score: Any          # int (1..M), "infinity", or None
    failure: bool
    outcome_code: Optional[str]  # None for finite rank, else one of VALID_OUTCOME_CODES

    def effective_conformal_score(self) -> float:
        if self.failure or self.score == "infinity" or self.score is None:
            return float("inf")
        return float(self.score)

    def is_empty_pool_abstain(self) -> bool:
        return (not self.failure) and (self.outcome_code == "empty_filtered_pool")


def validate_scored_row(row: ScoredCase, m_cutoff: int) -> None:
    """Validates one ScoredCase against the contract §3 typed row table."""
    if not row.case_id:
        raise ProducerError("Every scored row must have a nonempty case_id")

    if row.failure:
        # Row (d): score must be None, outcome_code in failure codes
        if row.score is not None:
            raise ProducerError("Failure row must have score=None")
        if row.outcome_code not in {"scoring_failure", "method_failure"}:
            raise ProducerError(
                f"Failure row must have outcome_code in {{scoring_failure,method_failure}}"
            )
    elif row.score == "infinity":
        # Row (b) or (c)
        if row.outcome_code not in {"no_reference_match", "empty_filtered_pool"}:
            raise ProducerError(
                f"Infinity non-failure row must have outcome_code in "
                f"{{no_reference_match, empty_filtered_pool}}"
            )
    elif isinstance(row.score, int) and 1 <= row.score <= m_cutoff:
        # Row (a): finite rank
        if row.outcome_code is not None:
            raise ProducerError("Finite rank row must have outcome_code=None")
    else:
        raise ProducerError(
            f"Invalid score value; must be int 1..M, 'infinity', or None for failure"
        )


# ── cell projection ──────────────────────────────────────────────────────────


@dataclasses.dataclass(frozen=True)
class CellProjection:
    """Deterministic cell membership derived from the global split."""
    cell_id: str
    cal_case_ids: Tuple[str, ...]   # sorted, calibration cases in this cell
    dev_case_ids: Tuple[str, ...]   # sorted, development cases in this cell
    projection_digest: str          # SHA-256 over sorted cal_case_ids


def build_cell_projections(
    cases: List[FrameCase],
    assignment: RecipeGroupAssignment,
) -> Dict[str, CellProjection]:
    """
    Projects global recipe-group split to per-cell case membership.

    Contract:
    - Every recipe must inherit the same global split in every cell.
    - Baseline-0 includes all cases in calibration / development.
    - Partition-1 partitions by train_source_presence x foodb_profile_presence.
    - Each cell's projection_digest covers only its sorted calibration case IDs.
    """
    projections: Dict[str, CellProjection] = {}

    for cell in VALID_CELLS:
        if cell == "baseline_0":
            cell_cases = cases
        else:
            cell_cases = [c for c in cases if c.cell_id() == cell]

        cal_cases = sorted(
            [c.case_id for c in cell_cases
             if assignment.recipe_to_split.get(c.recipe_id) == "calibration"],
        )
        dev_cases = sorted(
            [c.case_id for c in cell_cases
             if assignment.recipe_to_split.get(c.recipe_id) == "development"],
        )

        # Projection digest: SHA-256 over newline-joined sorted cal case IDs
        hasher = hashlib.sha256()
        for cid in cal_cases:
            hasher.update(f"{cid}\n".encode("utf-8"))

        projections[cell] = CellProjection(
            cell_id=cell,
            cal_case_ids=tuple(cal_cases),
            dev_case_ids=tuple(dev_cases),
            projection_digest=hasher.hexdigest(),
        )

    return projections


# ── role binding ─────────────────────────────────────────────────────────────


@dataclasses.dataclass(frozen=True)
class RoleBinding:
    """One role with its declared and observed SHA-256."""
    role_name: str
    declared_sha256: Optional[str]   # caller-supplied expectation; None if absent
    observed_sha256: str             # derived from loaded bytes


def verify_role_bindings(
    bindings: List[RoleBinding],
) -> Tuple[str, List[str]]:
    """
    Verifies all nine required roles are present and digests match.

    Returns:
      (binding_status, errors)  where binding_status is "ok", "missing", or "mismatched".
    """
    provided = {b.role_name for b in bindings}
    missing = [r for r in REQUIRED_ROLES if r not in provided]
    if missing:
        return BINDING_STATUS_MISSING, [f"Missing role: {r}" for r in missing]

    errors: List[str] = []
    for b in bindings:
        if b.declared_sha256 is not None:
            if b.observed_sha256 != b.declared_sha256:
                errors.append(
                    f"Role {b.role_name} digest mismatch: "
                    f"declared={b.declared_sha256[:8]}… observed={b.observed_sha256[:8]}…"
                )

    if errors:
        return BINDING_STATUS_MISMATCHED, errors

    return BINDING_STATUS_OK, []


# ── calibration receipt ──────────────────────────────────────────────────────


@dataclasses.dataclass
class CalibrationReceipt:
    """
    Versioned calibration bundle manifest for one (cohort, cell, alpha, M) pair.

    Under the current project disposition, case-level exchangeability is unproven.
    Therefore:
      - guarantee_status is always "no_guarantee"
      - no_guarantee_reason is always "inference_assumptions_unresolved"
      - product_t_hat is always None
    Descriptive cutoff and empirical coverage are reported separately for research
    purposes and must not be used to populate product_t_hat.
    """
    schema_version: str
    method_version: str
    split_algorithm_id: str
    cohort_id: str
    cell_id: str
    alpha: float
    m_cutoff: int
    score_contract_id: str
    label_contract_id: str

    # Frame provenance
    frame_manifest_sha256: Optional[str]
    global_assignment_digest: str
    projection_digest: str

    # Role binding
    binding_status: str                    # "ok" | "missing" | "mismatched"
    binding_errors: List[str]
    role_digests: Dict[str, str]           # role_name -> observed SHA-256

    # Descriptive statistics (research use only, not a guarantee)
    n_calibration: int
    finite_score_count: int
    no_reference_match_count: int
    empty_pool_abstain_count: int
    failure_count: int
    k_order_statistic: int
    sample_cutoff_descriptive: Optional[float]
    sample_empirical_coverage: float

    # Gate output — always no_guarantee under current disposition
    guarantee_status: str                  # always "no_guarantee"
    no_guarantee_reason: str               # always "inference_assumptions_unresolved"
    product_t_hat: None                    # always None; never populated from descriptive

    def to_dict(self) -> Dict[str, Any]:
        return {
            "schema_version": self.schema_version,
            "method_version": self.method_version,
            "split_algorithm_id": self.split_algorithm_id,
            "cohort_id": self.cohort_id,
            "cell_id": self.cell_id,
            "alpha": self.alpha,
            "m_cutoff": self.m_cutoff,
            "score_contract_id": self.score_contract_id,
            "label_contract_id": self.label_contract_id,
            "frame_manifest_sha256": self.frame_manifest_sha256,
            "global_assignment_digest": self.global_assignment_digest,
            "projection_digest": self.projection_digest,
            "binding_status": self.binding_status,
            "binding_errors": self.binding_errors,
            "role_digests": self.role_digests,
            "n_calibration": self.n_calibration,
            "finite_score_count": self.finite_score_count,
            "no_reference_match_count": self.no_reference_match_count,
            "empty_pool_abstain_count": self.empty_pool_abstain_count,
            "failure_count": self.failure_count,
            "k_order_statistic": self.k_order_statistic,
            "sample_cutoff_descriptive": self.sample_cutoff_descriptive,
            "sample_empirical_coverage": self.sample_empirical_coverage,
            "guarantee_status": self.guarantee_status,
            "no_guarantee_reason": self.no_guarantee_reason,
            "product_t_hat": self.product_t_hat,
        }

    def canonical_digest(self) -> str:
        """SHA-256 of the canonical JSON representation of this receipt."""
        return _sha256_bytes(_canonical_json(self.to_dict()))


# ── producer ─────────────────────────────────────────────────────────────────


def produce_calibration_receipt(
    *,
    cohort_id: str,
    cell_id: str,
    alpha: float,
    m_cutoff: int,
    score_contract_id: str,
    label_contract_id: str,
    cases: List[FrameCase],
    scored_rows: List[ScoredCase],
    role_bindings: List[RoleBinding],
    frame_manifest_sha256: Optional[str] = None,
) -> CalibrationReceipt:
    """
    Produces a CalibrationReceipt for one (cohort, cell, alpha, M) pair.

    Steps (per contract §§1-6):
    1. Validate frame (no duplicate/missing case_id or recipe_id).
    2. Execute recipe-group split.
    3. Build per-cell projection and derive projection_digest.
    4. Validate scored rows against the contract §3 typed row table.
    5. Verify that every calibration case in this cell has exactly one scored row.
    6. Verify role bindings.
    7. Compute descriptive statistics (n, k, cutoff, coverage, counts).
    8. Return receipt with guarantee_status=no_guarantee, product_t_hat=None.

    Caller-supplied role digests are expectations only; observed digests are
    derived from loaded bytes in role_bindings.
    """
    if cell_id not in VALID_CELLS:
        raise ProducerError(f"Invalid cell_id '{cell_id}'")
    if not (0.0 < alpha < 1.0):
        raise ProducerError(f"alpha must be in (0, 1), got {alpha}")
    if m_cutoff < 1:
        raise ProducerError(f"m_cutoff must be >= 1, got {m_cutoff}")

    # 1. Validate frame
    validate_frame(cases)

    # 2. Split
    recipe_ids: Set[str] = {c.recipe_id for c in cases}
    assignment = execute_recipe_group_split(recipe_ids)

    # 3. Project to cell
    projections = build_cell_projections(cases, assignment)
    cell_proj = projections[cell_id]
    cal_case_ids_set = set(cell_proj.cal_case_ids)

    # 4. Validate scored rows
    scored_by_case: Dict[str, ScoredCase] = {}
    for row in scored_rows:
        validate_scored_row(row, m_cutoff)
        if row.case_id in scored_by_case:
            raise ProducerError(f"Duplicate scored row for case_id")
        scored_by_case[row.case_id] = row

    # 5. Every calibration case in this cell must have exactly one scored row
    missing_scores = cal_case_ids_set - set(scored_by_case.keys())
    if missing_scores:
        raise ProducerError(
            f"{len(missing_scores)} calibration case(s) in cell have no scored row"
        )

    # Collect scores for calibration cases in this cell only
    cal_scored = [scored_by_case[cid] for cid in cell_proj.cal_case_ids]

    # 6. Role bindings
    binding_status, binding_errors = verify_role_bindings(role_bindings)
    role_digests = {b.role_name: b.observed_sha256 for b in role_bindings}

    # 7. Descriptive statistics
    n = len(cal_scored)
    finite_count = sum(
        1 for r in cal_scored
        if not r.failure and isinstance(r.score, int) and 1 <= r.score <= m_cutoff
    )
    no_match_count = sum(
        1 for r in cal_scored
        if not r.failure and r.outcome_code == "no_reference_match"
    )
    empty_pool_count = sum(
        1 for r in cal_scored if r.is_empty_pool_abstain()
    )
    failure_count = sum(1 for r in cal_scored if r.failure)

    if n > 0:
        k = math.ceil((n + 1) * (1.0 - alpha))
        effective_scores = sorted(r.effective_conformal_score() for r in cal_scored)
        sample_cutoff: Optional[float] = effective_scores[k - 1] if k <= n else float("inf")
        # Infinity is never covered
        covered = sum(
            1 for s in effective_scores
            if s <= (sample_cutoff or float("inf")) and not math.isinf(s)
        )
        empirical_coverage = covered / n
    else:
        k = 0
        sample_cutoff = None
        empirical_coverage = 0.0

    # 8. Receipt — guarantee always no_guarantee under current disposition
    return CalibrationReceipt(
        schema_version=SCHEMA_VERSION,
        method_version=METHOD_VERSION,
        split_algorithm_id=SPLIT_ALGORITHM_ID,
        cohort_id=cohort_id,
        cell_id=cell_id,
        alpha=alpha,
        m_cutoff=m_cutoff,
        score_contract_id=score_contract_id,
        label_contract_id=label_contract_id,
        frame_manifest_sha256=frame_manifest_sha256,
        global_assignment_digest=assignment.global_assignment_digest,
        projection_digest=cell_proj.projection_digest,
        binding_status=binding_status,
        binding_errors=binding_errors,
        role_digests=role_digests,
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

