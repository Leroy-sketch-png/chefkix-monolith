"""C4 Pool-Level LLM Judge Contract, Runner, and Operational Gates.

Implements the authoritative pool-level candidate-order-blind C4 judge
specified in Section 7 of the SSoT, the L43 decision (lead-l43-judge-family-prompt-decision-20261106.md),
and the 2026-11-08 operational gate addendum:
1. Pinned System Prompt (SHA-256: 96e24ae1f629d9c5f4a58457ae0c148253be2571ca55eb8d4560b1f1281f5db7)
2. Pinned User Template (SHA-256: 713997acc2715d327fda254d3a34a4904999f5806c43b3e990e8dfb98782539b)
3. Opaque-ID pool submission with forward (primary) and reverse passes.
4. Nominal 4-category (A/C/N/U) Cohen's kappa and 2,000-draw whole-case cluster bootstrap CI.
5. Per-cell signed leniency delta (|delta_c| <= 10pp).
6. Conservative order-instability upper bound ((F + M) / D <= 10%).
7. Planned initial pool request invalid-output rate (<= 2%).
8. Fail-closed disposition: any FAIL rejects; any UNAVAILABLE blocks acceptance.

Authority: Protocol v1 Section 3.3; SSoT Section 7; BACKLOG_LEAD.md L43.
"""
from __future__ import annotations

import hashlib
import json
import math
import random
from dataclasses import asdict, dataclass, field, replace
from pathlib import Path
from typing import Any, Final, Literal, Mapping, Optional, Sequence

# Protocol & Specification Identifiers
SPECIFICATION_ID: Final[str] = "iron-chef-l43-c4-pool-judge-v1"
PROTOCOL_SHA256: Final[str] = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
PRIMARY_JUDGE_MODEL: Final[str] = "gemini-2.5-flash"
DIAGNOSTIC_JUDGE_MODEL: Final[str] = "gpt-4o-2024-08-06"
COMMON_SLATE_BASE_ARMS: Final[frozenset[str]] = frozenset({"GISMo", "lookup"})

# Permitted A/C/N/U Labels
PERMITTED_LABELS: Final[tuple[str, ...]] = ("A", "C", "N", "U")

# Gate Thresholds from Protocol Section 3.3 & L43
GATE_KAPPA_MIN: Final[float] = 0.60
GATE_KAPPA_LOWER95_MIN: Final[float] = 0.50
GATE_LENIENCY_DELTA_MAX: Final[float] = 0.10          # 10 percentage points
GATE_INSTABILITY_UPPER_MAX: Final[float] = 0.10       # 10%
GATE_INVALID_OUTPUT_MAX: Final[float] = 0.02          # 2%
MAX_REASON_WORDS: Final[int] = 20                     # Concise reasons <= 20 words

BOOTSTRAP_DRAWS: Final[int] = 2000
BOOTSTRAP_SEED: Final[int] = 20261021
MIN_VALID_BOOTSTRAP_DRAWS: Final[int] = 20

# Pinned System Prompt
SYSTEM_PROMPT: Final[str] = (
    "You are an experienced home cook and recipe editor. Judge ingredient substitutions strictly in the "
    "context of the given recipe. Do not reward creativity; judge whether a normal cook would get the same dish.\n\n"
    "Judge every supplied candidate independently. Do not rank candidates, choose a winner, infer a candidate's "
    "source or generating method, or infer the recorded gold. Do not use candidate order as evidence. This is a "
    "culinary-function judgment only; it is not an allergen, health, clinical, or food-safety assessment.\n\n"
    "Use exactly these labels:\n"
    "A = The swap works as-is; the dish is recognizably the same, with at most a minor taste or texture change.\n"
    "C = The swap works only with a change in quantity, technique, or a companion ingredient; the dish remains recognizably the same.\n"
    "N = The swap breaks the ingredient's culinary function or the dish's identity, is nonsensical, is the target itself, or is too vague to act on.\n"
    "U = You cannot judge this candidate from the supplied evidence. U means unknown, not negative.\n\n"
    "For each candidate, give one concise culinary reason of no more than 20 words. Do not output scores, rankings, "
    "probabilities, or text outside the required JSON object. Follow the user message's response shape exactly; every "
    "input candidate ID must be returned exactly once.\n"
)

SYSTEM_PROMPT_SHA256: Final[str] = "96e24ae1f629d9c5f4a58457ae0c148253be2571ca55eb8d4560b1f1281f5db7"

# Pinned User Template
USER_TEMPLATE: Final[str] = (
    "Recipe title (JSON string):\n"
    "{title}\n\n"
    "Ingredients (JSON array of strings):\n"
    "{ingredient_list}\n\n"
    "Ingredient to replace (JSON string):\n"
    "{target}\n\n"
    "Candidate substitutes (JSON array of objects with exactly the fields \"id\" and \"candidate\"):\n"
    "{numbered_candidates}\n\n"
    "Judge each candidate independently; do not rank or select among them. Return exactly one JSON object. "
    "Its top-level key is \"judgments\", whose value is an object keyed by the exact supplied opaque candidate IDs. "
    "Each value has exactly two properties: \"label\" (one of the strings \"A\", \"C\", \"N\", \"U\") and "
    "\"reason\" (a string of at most 20 words).\n\n"
    "The \"judgments\" object must contain exactly one key for every supplied candidate ID and no other keys. "
    "Each per-candidate value must conform to the bound C4JudgeOutput response object. Do not include any "
    "explanation outside the JSON object.\n"
)

USER_TEMPLATE_SHA256: Final[str] = "713997acc2715d327fda254d3a34a4904999f5806c43b3e990e8dfb98782539b"


GateStatus = Literal["PASS", "FAIL", "UNAVAILABLE"]


@dataclass(frozen=True)
class CandidateItem:
    """An opaque candidate within a case slate."""
    opaque_id: str
    candidate_text: str
    cell_id: str = "default_cell"
    arm: Optional[str] = None
    is_gold: bool = False
    identity_key: Optional[str] = None
    arm_memberships: tuple[str, ...] = ()


@dataclass(frozen=True)
class PoolJudgeCase:
    """A full recipe context with a candidate slate."""
    case_id: str
    recipe_title: str
    ingredients: list[str]
    target_ingredient: str
    candidates: list[CandidateItem]


@dataclass(frozen=True)
class SingleJudgment:
    """Evaluation result for one candidate ID."""
    opaque_id: str
    label: str
    reason: str


@dataclass(frozen=True)
class PoolResponse:
    """Parsed response for a pool request."""
    case_id: str
    pass_name: Literal["FORWARD", "REVERSE"]
    raw_text: str
    is_valid: bool
    error_message: Optional[str] = None
    judgments: dict[str, SingleJudgment] = field(default_factory=dict)
    model_requested: Optional[str] = None
    model_returned: Optional[str] = None
    temperature: Optional[float] = None
    system_prompt_sha256: Optional[str] = None
    user_template_sha256: Optional[str] = None
    attempt_number: int = 1


@dataclass(frozen=True)
class GateResult:
    """Verification result for a single Section 3.3 gate."""
    gate_name: str
    status: GateStatus
    observed_value: Optional[float]
    threshold: float
    comparator: str
    numerator: Optional[float] = None
    denominator: Optional[float] = None
    details: dict[str, Any] = field(default_factory=dict)
    pass_identity: str = "unspecified"
    input_hashes: dict[str, str] = field(default_factory=dict)


@dataclass(frozen=True)
class BootstrapCIResult:
    lower: Optional[float]
    upper: Optional[float]
    requested_draws: int
    valid_draws: int
    degenerate_draws: int


@dataclass(frozen=True)
class C4PoolJudgeEvaluationReport:
    """Full operational gate evaluation report for C4 pool judge."""
    specification_id: str
    judge_model_requested: str
    judge_model_returned: str
    overall_status: Literal["ACCEPTED", "REJECTED", "INCOMPLETE_GATES_UNAVAILABLE"]
    assigned_cases: int
    assigned_candidate_units: int
    paired_human_reference_units: int
    # Gate results
    agreement_kappa_gate: GateResult
    kappa_ci_lower_gate: GateResult
    leniency_cell_gates: list[GateResult]
    order_instability_gate: GateResult
    invalid_output_gate: GateResult
    judge_model_identity_gate: GateResult
    generator_family_gate: GateResult
    source_input_gate: GateResult
    human_reference_gate: GateResult
    prompt_freeze_gate: GateResult
    reference_present_candidate_units: int
    judge_valid_candidate_units: int
    missing_human_reference_units: int
    invalid_human_reference_units: int
    judge_u_count: int
    human_u_count: int
    arm_summaries: dict[str, dict[str, int]]
    input_hashes: dict[str, str]
    source_input_hash_status: Literal["PINNED", "MISSING_OR_UNVERIFIED"]
    human_reference_state: Literal["FINAL_ADJUDICATED", "SYNTHETIC_FIXTURE", "MISSING"]
    prompt_freeze_state: Literal["FROZEN", "DEVELOPMENT_CANDIDATE"]
    actual_attempt_count: int
    invalid_attempt_count: int
    unmatched_response_count: int
    unmatched_human_reference_count: int


def verify_prompt_assets() -> dict[str, bool]:
    """Verify code prompts against the committed Lead-selected UTF-8 assets."""
    sys_hash = hashlib.sha256(SYSTEM_PROMPT.encode("utf-8")).hexdigest()
    user_hash = hashlib.sha256(USER_TEMPLATE.encode("utf-8")).hexdigest()
    root = Path(__file__).resolve().parents[3]
    system_path = root / "evidence" / "lead-l43-c4-prompt-s7.1-v1-system.txt"
    user_path = root / "evidence" / "lead-l43-c4-prompt-s7.1-v1-user-template.txt"
    try:
        system_bytes = system_path.read_bytes()
        user_bytes = user_path.read_bytes()
        system_file_hash = hashlib.sha256(system_bytes).hexdigest()
        user_file_hash = hashlib.sha256(user_bytes).hexdigest()
        system_file_matches = system_bytes == SYSTEM_PROMPT.encode("utf-8")
        user_file_matches = user_bytes == USER_TEMPLATE.encode("utf-8")
    except OSError:
        system_file_hash = user_file_hash = ""
        system_file_matches = user_file_matches = False
    return {
        "system_prompt_verified": (sys_hash == SYSTEM_PROMPT_SHA256),
        "user_template_verified": (user_hash == USER_TEMPLATE_SHA256),
        "system_prompt_file_verified": (
            system_file_hash == SYSTEM_PROMPT_SHA256 and system_file_matches
        ),
        "user_template_file_verified": (
            user_file_hash == USER_TEMPLATE_SHA256 and user_file_matches
        ),
    }


def normalize_case_candidates(case: PoolJudgeCase) -> PoolJudgeCase:
    """Deduplicate repeated cross-arm identities while preserving arm membership.

    The caller supplies a stable internal ``identity_key`` for repeated
    candidate identities. That key and all arm provenance stay out of prompts.
    """
    if (
        not isinstance(case.case_id, str)
        or not case.case_id
        or not isinstance(case.recipe_title, str)
        or not isinstance(case.target_ingredient, str)
        or not isinstance(case.ingredients, list)
        or any(not isinstance(value, str) for value in case.ingredients)
        or not case.candidates
    ):
        raise ValueError("malformed_pool_case")
    try:
        for value in [case.case_id, case.recipe_title, case.target_ingredient, *case.ingredients]:
            value.encode("utf-8", errors="strict")
    except UnicodeEncodeError as exc:
        raise ValueError("invalid_pool_case_encoding") from exc
    by_identity: dict[str, CandidateItem] = {}
    opaque_to_identity: dict[str, str] = {}
    for candidate in case.candidates:
        if (
            not isinstance(candidate.opaque_id, str)
            or not candidate.opaque_id
            or not isinstance(candidate.candidate_text, str)
            or not candidate.candidate_text
            or not isinstance(candidate.cell_id, str)
            or not candidate.cell_id
            or type(candidate.is_gold) is not bool
        ):
            raise ValueError("malformed_candidate_item")
        if not isinstance(candidate.arm_memberships, (tuple, list)):
            raise ValueError("malformed_arm_membership")
        identity_key = candidate.identity_key or candidate.opaque_id
        if not isinstance(identity_key, str) or not identity_key:
            raise ValueError("malformed_candidate_identity_key")
        previous_identity = opaque_to_identity.get(candidate.opaque_id)
        if previous_identity is not None and previous_identity != identity_key:
            raise ValueError("opaque_id_reused_for_distinct_candidates")
        opaque_to_identity[candidate.opaque_id] = identity_key
        memberships = set(candidate.arm_memberships)
        if candidate.arm:
            memberships.add(candidate.arm)
        if any(not isinstance(arm, str) or not arm for arm in memberships):
            raise ValueError("malformed_arm_membership")
        try:
            for value in (
                candidate.opaque_id,
                candidate.candidate_text,
                candidate.cell_id,
                identity_key,
                *memberships,
            ):
                value.encode("utf-8", errors="strict")
        except UnicodeEncodeError as exc:
            raise ValueError("invalid_candidate_identity_encoding") from exc
        prior = by_identity.get(identity_key)
        if prior is None:
            by_identity[identity_key] = replace(
                candidate,
                identity_key=identity_key,
                arm_memberships=tuple(sorted(memberships)),
            )
        else:
            prior_memberships = set(prior.arm_memberships)
            if not memberships or not prior_memberships or memberships & prior_memberships:
                raise ValueError("duplicate_candidate_within_arm_or_unassigned_source")
            if (
                prior.opaque_id != candidate.opaque_id
                or prior.candidate_text != candidate.candidate_text
                or prior.cell_id != candidate.cell_id
            ):
                raise ValueError("conflicting_duplicate_candidate_identity")
            if prior.is_gold != candidate.is_gold:
                raise ValueError("conflicting_duplicate_candidate_reference_flag")
            by_identity[identity_key] = replace(
                prior,
                arm_memberships=tuple(
                    sorted(set(prior.arm_memberships) | memberships)
                ),
            )
    # Preserve the frozen slate order. Repeated cross-arm identities collapse
    # at their first occurrence, while arm provenance is accumulated above.
    normalized = list(by_identity.values())
    if len({item.opaque_id for item in normalized}) != len(normalized):
        raise ValueError("duplicate_opaque_id_after_deduplication")
    return replace(case, candidates=normalized)


def normalize_pool_cases(cases: list[PoolJudgeCase]) -> list[PoolJudgeCase]:
    normalized = [normalize_case_candidates(case) for case in cases]
    case_ids = [case.case_id for case in normalized]
    if len(case_ids) != len(set(case_ids)):
        raise ValueError("duplicate_assigned_case_id")
    return normalized


def format_user_prompt(
    case: PoolJudgeCase,
    reverse_order: bool = False,
) -> str:
    """Render the user prompt template with JSON-escaped placeholders."""
    assets = verify_prompt_assets()
    if not assets["system_prompt_file_verified"] or not assets["user_template_file_verified"]:
        raise ValueError("pinned_prompt_assets_unavailable_or_mismatched")
    normalized_case = normalize_case_candidates(case)
    candidates = list(normalized_case.candidates)
    if reverse_order:
        candidates.reverse()

    numbered_candidates = [
        {"id": c.opaque_id, "candidate": c.candidate_text}
        for c in candidates
    ]

    return USER_TEMPLATE.format(
        title=json.dumps(normalized_case.recipe_title, ensure_ascii=False),
        ingredient_list=json.dumps(normalized_case.ingredients, ensure_ascii=False),
        target=json.dumps(normalized_case.target_ingredient, ensure_ascii=False),
        numbered_candidates=json.dumps(numbered_candidates, ensure_ascii=False),
    )


class _DuplicateJSONKey(ValueError):
    pass


def _reject_duplicate_json_keys(pairs: list[tuple[str, object]]) -> dict[str, object]:
    result: dict[str, object] = {}
    for key, value in pairs:
        if key in result:
            raise _DuplicateJSONKey("duplicate_json_key")
        result[key] = value
    return result


def _reject_nonstandard_json_constant(value: str) -> None:
    raise ValueError(f"nonstandard_json_constant:{value}")


def parse_and_validate_pool_response(
    raw_response_text: str,
    case: PoolJudgeCase,
    pass_name: Literal["FORWARD", "REVERSE"],
    *,
    model_requested: str | None = None,
    model_returned: str | None = None,
    temperature: float | None = None,
    system_prompt_sha256: str | None = None,
    user_template_sha256: str | None = None,
    attempt_number: int = 1,
) -> PoolResponse:
    """Validate a model response against the opaque-ID schema and reason length."""

    def response(
        is_valid: bool,
        error_message: Optional[str] = None,
        judgments: Optional[dict[str, SingleJudgment]] = None,
    ) -> PoolResponse:
        return PoolResponse(
            case_id=case.case_id,
            pass_name=pass_name,
            raw_text=raw_response_text,
            is_valid=is_valid,
            error_message=error_message,
            judgments=judgments or {},
            model_requested=model_requested,
            model_returned=model_returned,
            temperature=temperature,
            system_prompt_sha256=system_prompt_sha256,
            user_template_sha256=user_template_sha256,
            attempt_number=attempt_number,
        )

    try:
        normalized_case = normalize_case_candidates(case)
    except ValueError as exc:
        return response(False, str(exc))
    expected_ids = {c.opaque_id for c in normalized_case.candidates}

    try:
        data = json.loads(
            raw_response_text,
            object_pairs_hook=_reject_duplicate_json_keys,
            parse_constant=_reject_nonstandard_json_constant,
        )
    except Exception as exc:
        return response(False, f"json_response_invalid:{type(exc).__name__}")

    if not isinstance(data, dict) or set(data) != {"judgments"}:
        return response(False, "response_must_contain_only_judgments_object")

    judgments_dict = data["judgments"]
    if not isinstance(judgments_dict, dict):
        return response(False, "judgments_must_be_object")

    observed_ids = set(judgments_dict.keys())
    if observed_ids != expected_ids:
        missing = expected_ids - observed_ids
        foreign = observed_ids - expected_ids
        return response(
            False,
            f"opaque_id_coverage_mismatch:missing={len(missing)},foreign={len(foreign)}",
        )

    parsed_judgments: dict[str, SingleJudgment] = {}
    for cid, val in judgments_dict.items():
        if not isinstance(val, dict) or set(val) != {"label", "reason"}:
            return response(False, "candidate_judgment_shape_invalid")
        label = val.get("label")
        reason = val.get("reason")
        if not isinstance(label, str) or label not in PERMITTED_LABELS:
            return response(False, "candidate_label_invalid")
        if not isinstance(reason, str) or not reason.strip():
            return response(False, "candidate_reason_missing_or_invalid")
        words = reason.strip().split()
        if len(words) > MAX_REASON_WORDS:
            return response(False, "candidate_reason_over_20_words")
        parsed_judgments[cid] = SingleJudgment(opaque_id=cid, label=label, reason=reason)

    return response(True, judgments=parsed_judgments)


def compute_unweighted_cohens_kappa(
    paired_labels: list[tuple[str, str]],
) -> Optional[float]:
    """Compute unweighted nominal Cohen's kappa on A/C/N/U pairs.
    
    Returns None if no pairs or if marginals produce a degenerate denominator.
    """
    n = len(paired_labels)
    if n == 0:
        return None
    if any(
        judge not in PERMITTED_LABELS or human not in PERMITTED_LABELS
        for judge, human in paired_labels
    ):
        raise ValueError("kappa_requires_exact_A_C_N_U_pairs")

    categories = PERMITTED_LABELS
    # Exact label agreement
    agree_count = sum(1 for (j, h) in paired_labels if j == h)
    p_o = agree_count / n

    # Marginals
    j_counts = {c: 0 for c in categories}
    h_counts = {c: 0 for c in categories}
    for (j, h) in paired_labels:
        if j in j_counts:
            j_counts[j] += 1
        if h in h_counts:
            h_counts[h] += 1

    p_e = sum((j_counts[c] / n) * (h_counts[c] / n) for c in categories)

    if math.isclose(p_e, 1.0, abs_tol=1e-12):
        return None

    return (p_o - p_e) / (1.0 - p_e)


def compute_case_cluster_bootstrap_ci(
    case_paired_units: dict[str, list[tuple[str, str]]],
    draws: int = BOOTSTRAP_DRAWS,
    seed: int = BOOTSTRAP_SEED,
) -> tuple[Optional[float], Optional[float], int]:
    """Compatibility tuple over the detailed frozen whole-case bootstrap."""
    result = compute_case_cluster_bootstrap_result(case_paired_units, draws, seed)
    return result.lower, result.upper, result.valid_draws


def compute_case_cluster_bootstrap_result(
    case_paired_units: dict[str, list[tuple[str, str]]],
    draws: int = BOOTSTRAP_DRAWS,
    seed: int = BOOTSTRAP_SEED,
) -> BootstrapCIResult:
    """Compute the frozen whole-case bootstrap and retain invalid-draw counts."""
    if isinstance(draws, bool) or not isinstance(draws, int) or draws < 1:
        raise ValueError("bootstrap_draws_must_be_positive_integer")
    if isinstance(seed, bool) or not isinstance(seed, int):
        raise ValueError("bootstrap_seed_must_be_integer")
    if any(not units for units in case_paired_units.values()):
        raise ValueError("bootstrap_case_clusters_must_not_be_empty")
    case_ids = sorted(case_paired_units, key=lambda value: value.encode("utf-8"))
    n_cases = len(case_ids)
    if n_cases == 0:
        return BootstrapCIResult(None, None, draws, 0, draws)

    rng = random.Random(seed)
    valid_kappas: list[float] = []

    for _ in range(draws):
        sampled_cases = rng.choices(case_ids, k=n_cases)
        sample_pairs: list[tuple[str, str]] = []
        for cid in sampled_cases:
            sample_pairs.extend(case_paired_units[cid])

        k = compute_unweighted_cohens_kappa(sample_pairs)
        if k is not None and not math.isnan(k):
            valid_kappas.append(k)

    b_valid = len(valid_kappas)
    b_degenerate = draws - b_valid
    if b_valid < MIN_VALID_BOOTSTRAP_DRAWS:
        return BootstrapCIResult(None, None, draws, b_valid, b_degenerate)

    valid_kappas.sort()
    lower_idx = math.floor(0.025 * b_valid)
    upper_idx = math.floor(0.975 * b_valid) - 1
    upper_idx = max(0, min(upper_idx, b_valid - 1))

    return BootstrapCIResult(
        valid_kappas[lower_idx],
        valid_kappas[upper_idx],
        draws,
        b_valid,
        b_degenerate,
    )


@dataclass(frozen=True)
class _PlannedRequest:
    case_id: str
    pass_name: Literal["FORWARD", "REVERSE"]
    attempts: tuple[PoolResponse, ...]
    first_attempt: Optional[PoolResponse]
    terminal_attempt: Optional[PoolResponse]
    attempt_sequence_valid: bool
    initial_failed: bool
    terminal_payload_valid: bool


def _response_payload_valid(
    response: Optional[PoolResponse],
    case: PoolJudgeCase,
    pass_name: Literal["FORWARD", "REVERSE"],
) -> bool:
    if (
        response is None
        or response.case_id != case.case_id
        or response.pass_name != pass_name
        or not response.is_valid
    ):
        return False
    parsed = parse_and_validate_pool_response(
        response.raw_text,
        case,
        pass_name,
        model_requested=response.model_requested,
        model_returned=response.model_returned,
        temperature=response.temperature,
        system_prompt_sha256=response.system_prompt_sha256,
        user_template_sha256=response.user_template_sha256,
        attempt_number=response.attempt_number,
    )
    return parsed.is_valid and parsed.judgments == response.judgments


def _response_method_identity_valid(response: PoolResponse, expected_model: str) -> bool:
    return (
        response.model_requested == expected_model
        and response.model_returned == expected_model
        and isinstance(response.temperature, (int, float))
        and not isinstance(response.temperature, bool)
        and float(response.temperature) == 0.0
        and response.system_prompt_sha256 == SYSTEM_PROMPT_SHA256
        and response.user_template_sha256 == USER_TEMPLATE_SHA256
    )


def _input_hashes(
    cases: list[PoolJudgeCase],
    human_adjudications: Mapping[str, Mapping[str, object]],
    source_file_hashes: Optional[Mapping[str, str]],
) -> tuple[dict[str, str], bool]:
    def sha256(value: object) -> str:
        raw = json.dumps(
            value,
            ensure_ascii=False,
            sort_keys=True,
            separators=(",", ":"),
            allow_nan=False,
        ).encode("utf-8")
        return hashlib.sha256(raw).hexdigest()

    case_material = [
        {
            "case_id": case.case_id,
            "recipe_title": case.recipe_title,
            "ingredients": list(case.ingredients),
            "target_ingredient": case.target_ingredient,
            "candidates": [
                {
                    "opaque_id": candidate.opaque_id,
                    "identity_key": candidate.identity_key,
                    "candidate_text": candidate.candidate_text,
                    "cell_id": candidate.cell_id,
                    "arm_memberships": list(candidate.arm_memberships),
                    "is_gold": candidate.is_gold,
                }
                for candidate in case.candidates
            ],
        }
        for case in sorted(cases, key=lambda item: item.case_id.encode("utf-8"))
    ]
    human_material = [
        {
            "case_id": case_id,
            "judgments": [
                {"opaque_id": opaque_id, "label": label}
                for opaque_id, label in sorted(
                    labels.items(), key=lambda item: str(item[0]).encode("utf-8")
                )
            ] if isinstance(labels, Mapping) else {"invalid_mapping_type": type(labels).__name__},
        }
        for case_id, labels in sorted(
            human_adjudications.items(), key=lambda item: str(item[0]).encode("utf-8")
        )
    ]
    hashes = {
        "case_slate_canonical_sha256": sha256(case_material),
        "human_adjudications_canonical_sha256": sha256(human_material),
        "protocol_sha256": PROTOCOL_SHA256,
        "system_prompt_sha256": SYSTEM_PROMPT_SHA256,
        "user_template_sha256": USER_TEMPLATE_SHA256,
    }
    required_source_names = {
        "assignment_manifest_sha256",
        "candidate_pool_manifest_sha256",
        "human_adjudication_manifest_sha256",
    }
    valid_source_hashes = (
        isinstance(source_file_hashes, Mapping)
        and required_source_names.issubset(source_file_hashes)
        and all(
            isinstance(name, str) and isinstance(value, str)
            and len(value) == 64 and value == value.lower()
            and all(char in "0123456789abcdef" for char in value)
            for name, value in source_file_hashes.items()
        )
    )
    if isinstance(source_file_hashes, Mapping):
        hashes.update(
            {
                f"source_{name}": value
                for name, value in source_file_hashes.items()
                if isinstance(name, str) and isinstance(value, str)
                and len(value) == 64 and value == value.lower()
                and all(char in "0123456789abcdef" for char in value)
            }
        )
    return hashes, bool(valid_source_hashes)


def _resolve_planned_requests(
    cases: list[PoolJudgeCase],
    responses: list[PoolResponse],
    pass_name: Literal["FORWARD", "REVERSE"],
) -> tuple[dict[str, _PlannedRequest], int]:
    grouped: dict[tuple[str, str], list[PoolResponse]] = {}
    expected = {(case.case_id, pass_name) for case in cases}
    unmatched = 0
    for response in responses:
        key = (response.case_id, response.pass_name)
        if key not in expected:
            unmatched += 1
            continue
        grouped.setdefault(key, []).append(response)
    result: dict[str, _PlannedRequest] = {}
    for case in cases:
        attempts = grouped.get((case.case_id, pass_name), [])
        ordered = sorted(
            attempts,
            key=lambda response: (
                response.attempt_number
                if isinstance(response.attempt_number, int)
                and not isinstance(response.attempt_number, bool)
                else -1
            ),
        )
        numbers = [response.attempt_number for response in ordered]
        sequence_valid = (
            all(
                isinstance(number, int) and not isinstance(number, bool) and number >= 1
                for number in numbers
            )
            and numbers == list(range(1, len(numbers) + 1))
        )
        first = ordered[0] if sequence_valid and ordered else None
        terminal = ordered[-1] if sequence_valid and ordered else None
        first_valid = _response_payload_valid(first, case, pass_name)
        terminal_valid = _response_payload_valid(terminal, case, pass_name)
        result[case.case_id] = _PlannedRequest(
            case_id=case.case_id,
            pass_name=pass_name,
            attempts=tuple(ordered),
            first_attempt=first,
            terminal_attempt=terminal,
            attempt_sequence_valid=sequence_valid,
            initial_failed=(not sequence_valid or not first_valid),
            terminal_payload_valid=sequence_valid and terminal_valid,
        )
    return result, unmatched


def _request_usable(request: _PlannedRequest, expected_model: str) -> Optional[PoolResponse]:
    terminal = request.terminal_attempt
    if (
        not request.terminal_payload_valid
        or terminal is None
        or not _response_method_identity_valid(terminal, expected_model)
    ):
        return None
    return terminal


def _gate_with_provenance(
    gate: GateResult,
    *,
    pass_identity: str,
    input_hashes: Mapping[str, str],
) -> GateResult:
    return replace(
        gate,
        pass_identity=pass_identity,
        input_hashes=dict(input_hashes),
    )


def evaluate_c4_operational_gates(
    cases: list[PoolJudgeCase],
    forward_responses: list[PoolResponse],
    reverse_responses: list[PoolResponse],
    human_adjudications: dict[str, dict[str, str]],  # case_id -> {opaque_id: label}
    judge_model_requested: str = PRIMARY_JUDGE_MODEL,
    judge_model_returned: str = PRIMARY_JUDGE_MODEL,
    *,
    source_manifest_bytes: Optional[Mapping[str, bytes]] = None,
    expected_source_file_hashes: Optional[Mapping[str, str]] = None,
    human_reference_state: Literal["FINAL_ADJUDICATED", "SYNTHETIC_FIXTURE", "MISSING"] = "SYNTHETIC_FIXTURE",
    prompt_freeze_state: Literal["FROZEN", "DEVELOPMENT_CANDIDATE"] = "DEVELOPMENT_CANDIDATE",
    generator_family_by_arm: Optional[Mapping[str, str]] = None,
    bootstrap_draws: int = BOOTSTRAP_DRAWS,
    bootstrap_seed: int = BOOTSTRAP_SEED,
) -> C4PoolJudgeEvaluationReport:
    """Evaluate the frozen L43 gates without hiding bad attempts or missing inputs.

    The function accepts saved responses only; it performs no provider call. The
    default states are intentionally non-accepting for source-free fixtures.
    """
    normalized_cases = normalize_pool_cases(cases)
    if not isinstance(human_adjudications, Mapping):
        raise ValueError("human_adjudications_must_be_mapping")
    if generator_family_by_arm is not None and (
        not isinstance(generator_family_by_arm, Mapping)
        or any(not isinstance(key, str) or not isinstance(value, str)
               for key, value in generator_family_by_arm.items())
    ):
        raise ValueError("generator_family_map_must_be_string_mapping")
    n_assigned_cases = len(normalized_cases)
    total_assigned_units = sum(len(case.candidates) for case in normalized_cases)
    expected_model = judge_model_requested
    if expected_model not in {PRIMARY_JUDGE_MODEL, DIAGNOSTIC_JUDGE_MODEL}:
        expected_model = "<unsupported-model>"

    required_source_names = {
        "assignment_manifest_sha256",
        "candidate_pool_manifest_sha256",
        "human_adjudication_manifest_sha256",
    }
    observed_source_hashes: dict[str, str] = {}
    if isinstance(source_manifest_bytes, Mapping):
        for name in sorted(required_source_names):
            raw = source_manifest_bytes.get(name.removesuffix("_sha256"))
            if isinstance(raw, bytes) and raw:
                observed_source_hashes[name] = hashlib.sha256(raw).hexdigest()
    source_hashes_valid = (
        set(observed_source_hashes) == required_source_names
        and isinstance(expected_source_file_hashes, Mapping)
        and set(expected_source_file_hashes) == required_source_names
        and all(
            isinstance(expected_source_file_hashes.get(name), str)
            and expected_source_file_hashes[name] == observed_source_hashes[name]
            for name in required_source_names
        )
    )
    source_inputs_complete = (
        isinstance(source_manifest_bytes, Mapping)
        and isinstance(expected_source_file_hashes, Mapping)
        and set(expected_source_file_hashes) == required_source_names
        and all(
            isinstance(source_manifest_bytes.get(name.removesuffix("_sha256")), bytes)
            and bool(source_manifest_bytes[name.removesuffix("_sha256")])
            for name in required_source_names
        )
    )
    input_hashes, canonical_source_hashes_valid = _input_hashes(
        normalized_cases,
        human_adjudications,
        observed_source_hashes if observed_source_hashes else None,
    )
    source_hashes_valid = source_hashes_valid and canonical_source_hashes_valid
    matched_source_pin_count = sum(
        1 for name, observed in observed_source_hashes.items()
        if isinstance(expected_source_file_hashes, Mapping)
        and expected_source_file_hashes.get(name) == observed
    )
    def canonical_hash(value: object) -> str:
        payload = json.dumps(
            value, ensure_ascii=False, sort_keys=True, separators=(",", ":"), allow_nan=False
        ).encode("utf-8")
        return hashlib.sha256(payload).hexdigest()

    def response_records(responses: Sequence[PoolResponse]) -> list[dict[str, object]]:
        return [
            {
                "case_id": response.case_id,
                "pass_name": response.pass_name,
                "attempt_number": response.attempt_number,
                "raw_text": response.raw_text,
                "model_requested": response.model_requested,
                "model_returned": response.model_returned,
                "temperature": (
                    response.temperature
                    if response.temperature is None or (
                        isinstance(response.temperature, (int, float))
                        and not isinstance(response.temperature, bool)
                        and math.isfinite(float(response.temperature))
                    )
                    else "non_finite_or_invalid_temperature"
                ),
                "system_prompt_sha256": response.system_prompt_sha256,
                "user_template_sha256": response.user_template_sha256,
                "caller_valid_flag": response.is_valid,
            }
            for response in responses
        ]

    input_hashes.update(
        {
            "forward_response_log_canonical_sha256": canonical_hash(response_records(forward_responses)),
            "reverse_response_log_canonical_sha256": canonical_hash(response_records(reverse_responses)),
            "generator_family_map_canonical_sha256": canonical_hash(
                dict(sorted((generator_family_by_arm or {}).items()))
            ),
            "analysis_scope_canonical_sha256": canonical_hash(
                {
                    "judge_model_requested": judge_model_requested,
                    "judge_model_returned": judge_model_returned,
                    "human_reference_state": human_reference_state,
                    "prompt_freeze_state": prompt_freeze_state,
                    "bootstrap_draws": bootstrap_draws,
                    "bootstrap_seed": bootstrap_seed,
                }
            ),
        }
    )
    prompt_assets = verify_prompt_assets()
    source_status: Literal["PINNED", "MISSING_OR_UNVERIFIED"] = (
        "PINNED" if source_hashes_valid else "MISSING_OR_UNVERIFIED"
    )

    forward, forward_orphans = _resolve_planned_requests(
        normalized_cases, forward_responses, "FORWARD"
    )
    reverse, reverse_orphans = _resolve_planned_requests(
        normalized_cases, reverse_responses, "REVERSE"
    )
    unmatched_responses = forward_orphans + reverse_orphans
    all_responses = list(forward_responses) + list(reverse_responses)
    actual_attempt_count = len(all_responses)
    case_by_id = {case.case_id: case for case in normalized_cases}

    def response_disposition(
        response: Optional[PoolResponse],
        request: Optional[_PlannedRequest],
        case: PoolJudgeCase,
        pass_name: Literal["FORWARD", "REVERSE"],
    ) -> str:
        if response is None:
            return "missing"
        if request is not None and not request.attempt_sequence_valid:
            return "invalid_sequence"
        if not _response_payload_valid(response, case, pass_name):
            return "invalid_payload"
        return "valid"

    initial_dispositions = {key: 0 for key in ("valid", "invalid_payload", "missing", "invalid_sequence")}
    terminal_dispositions = dict(initial_dispositions)
    invalid_attempt_count = 0
    method_identity_errors = 0
    for response in all_responses:
        case = case_by_id.get(response.case_id)
        if case is None or response.pass_name not in {"FORWARD", "REVERSE"}:
            invalid_attempt_count += 1
            method_identity_errors += 1
            continue
        request = (forward if response.pass_name == "FORWARD" else reverse).get(response.case_id)
        if request is None or not request.attempt_sequence_valid:
            invalid_attempt_count += 1
        elif not _response_payload_valid(response, case, response.pass_name):
            invalid_attempt_count += 1
        if not _response_method_identity_valid(response, expected_model):
            method_identity_errors += 1
    for case in normalized_cases:
        for pass_name, requests, dispositions in (
            ("FORWARD", forward, initial_dispositions),
            ("REVERSE", reverse, initial_dispositions),
        ):
            request = requests[case.case_id]
            disposition = response_disposition(
                request.first_attempt, request, case, pass_name
            )
            dispositions[disposition] += 1
        for pass_name, requests in (("FORWARD", forward), ("REVERSE", reverse)):
            request = requests[case.case_id]
            disposition = response_disposition(
                request.terminal_attempt, request, case, pass_name
            )
            terminal_dispositions[disposition] += 1
    invalid_attempt_count += unmatched_responses

    # Human references are paired only by exact case and candidate identity.
    all_pairs: list[tuple[str, str]] = []
    case_paired_units: dict[str, list[tuple[str, str]]] = {}
    cell_pairs: dict[str, list[tuple[str, str]]] = {}
    missing_human = 0
    invalid_human = 0
    human_u_count = 0
    unmatched_human = 0
    extra_human_case_ids = set(human_adjudications) - set(case_by_id)
    for extra_case_id in extra_human_case_ids:
        extra_value = human_adjudications[extra_case_id]
        unmatched_human += max(1, len(extra_value)) if isinstance(extra_value, Mapping) else 1
    judge_u_count = 0
    judge_valid_units = 0
    reference_present_units = 0
    arm_summaries: dict[str, dict[str, int]] = {}

    for case in normalized_cases:
        human_case = human_adjudications.get(case.case_id, {})
        malformed_human_case = not isinstance(human_case, Mapping)
        if malformed_human_case:
            invalid_human += len(case.candidates)
            human_case = {}
        known_ids = {candidate.opaque_id for candidate in case.candidates}
        unmatched_human += sum(1 for opaque_id in human_case if opaque_id not in known_ids)
        f_response = _request_usable(forward[case.case_id], expected_model)
        case_pairs: list[tuple[str, str]] = []
        for candidate in case.candidates:
            label_value = human_case.get(candidate.opaque_id)
            human_label: Optional[str]
            if malformed_human_case:
                human_label = None
            elif candidate.opaque_id not in human_case or label_value is None:
                missing_human += 1
                human_label = None
            elif isinstance(label_value, str) and label_value in PERMITTED_LABELS:
                human_label = label_value
                reference_present_units += 1
                if human_label == "U":
                    human_u_count += 1
            else:
                invalid_human += 1
                human_label = None

            f_judgment = f_response.judgments.get(candidate.opaque_id) if f_response else None
            if f_judgment is not None:
                judge_valid_units += 1
                if f_judgment.label == "U":
                    judge_u_count += 1
            if f_judgment is not None and human_label is not None:
                pair = (f_judgment.label, human_label)
                all_pairs.append(pair)
                case_pairs.append(pair)
                cell_pairs.setdefault(candidate.cell_id, []).append(pair)
            arms = candidate.arm_memberships or ("unassigned",)
            for arm in arms:
                summary = arm_summaries.setdefault(
                    arm,
                    {
                        "assigned_candidate_units": 0,
                        "judge_A": 0,
                        "judge_C": 0,
                        "judge_N": 0,
                        "judge_U": 0,
                        "human_A": 0,
                        "human_C": 0,
                        "human_N": 0,
                        "human_U": 0,
                        "paired_units": 0,
                    },
                )
                summary["assigned_candidate_units"] += 1
                if f_judgment is not None:
                    summary[f"judge_{f_judgment.label}"] += 1
                if human_label is not None:
                    summary[f"human_{human_label}"] += 1
                if f_judgment is not None and human_label is not None:
                    summary["paired_units"] += 1
        if case_pairs:
            case_paired_units[case.case_id] = case_pairs

    kappa_val = compute_unweighted_cohens_kappa(all_pairs)
    n_paired = len(all_pairs)
    if kappa_val is None:
        kappa_gate = GateResult(
            gate_name="Agreement Kappa Point Gate", status="UNAVAILABLE",
            observed_value=None, threshold=GATE_KAPPA_MIN, comparator=">=",
            numerator=None, denominator=n_paired,
            details={"paired_units": n_paired, "reason": "kappa_not_estimable"},
        )
    else:
        kappa_gate = GateResult(
            gate_name="Agreement Kappa Point Gate",
            status="PASS" if kappa_val >= GATE_KAPPA_MIN else "FAIL",
            observed_value=kappa_val, threshold=GATE_KAPPA_MIN, comparator=">=",
            numerator=sum(1 for judge, human in all_pairs if judge == human),
            denominator=n_paired,
            details={"paired_units": n_paired},
        )
    bootstrap = compute_case_cluster_bootstrap_result(
        case_paired_units, draws=bootstrap_draws, seed=bootstrap_seed
    )
    if bootstrap.lower is None:
        kappa_ci_gate = GateResult(
            gate_name="Agreement Kappa CI Lower Gate", status="UNAVAILABLE",
            observed_value=None, threshold=GATE_KAPPA_LOWER95_MIN, comparator=">=",
            numerator=bootstrap.valid_draws, denominator=bootstrap.requested_draws,
            details={
                "ci95_upper": None,
                "requested_draws": bootstrap.requested_draws,
                "valid_draws": bootstrap.valid_draws,
                "degenerate_draws": bootstrap.degenerate_draws,
                "seed": bootstrap_seed,
                "minimum_valid_draws": MIN_VALID_BOOTSTRAP_DRAWS,
            },
        )
    else:
        kappa_ci_gate = GateResult(
            gate_name="Agreement Kappa CI Lower Gate",
            status="PASS" if bootstrap.lower >= GATE_KAPPA_LOWER95_MIN else "FAIL",
            observed_value=bootstrap.lower, threshold=GATE_KAPPA_LOWER95_MIN, comparator=">=",
            numerator=bootstrap.valid_draws, denominator=bootstrap.requested_draws,
            details={
                "ci95_upper": bootstrap.upper,
                "requested_draws": bootstrap.requested_draws,
                "valid_draws": bootstrap.valid_draws,
                "degenerate_draws": bootstrap.degenerate_draws,
                "seed": bootstrap_seed,
                "minimum_valid_draws": MIN_VALID_BOOTSTRAP_DRAWS,
            },
        )

    # Cell delta uses every assigned nonempty cell and only paired A/C/N/U units.
    leniency_gates: list[GateResult] = []
    prespecified_cells = sorted(
        {candidate.cell_id for case in normalized_cases for candidate in case.candidates},
        key=lambda value: value.encode("utf-8"),
    )
    for cell_id in prespecified_cells:
        pairs = cell_pairs.get(cell_id, [])
        if not pairs:
            gate = GateResult(
                gate_name=f"Leniency Gate ({cell_id})", status="UNAVAILABLE",
                observed_value=None, threshold=GATE_LENIENCY_DELTA_MAX, comparator="<=",
                numerator=None, denominator=0,
                details={"cell_id": cell_id, "paired_units": 0, "reference_unavailable": True},
            )
        else:
            judge_a = sum(judge == "A" for judge, _ in pairs)
            human_a = sum(human == "A" for _, human in pairs)
            signed_delta = (judge_a - human_a) / len(pairs)
            gate = GateResult(
                gate_name=f"Leniency Gate ({cell_id})",
                status="PASS" if abs(signed_delta) <= GATE_LENIENCY_DELTA_MAX else "FAIL",
                observed_value=abs(signed_delta), threshold=GATE_LENIENCY_DELTA_MAX,
                comparator="<=", numerator=abs(judge_a - human_a), denominator=len(pairs),
                details={
                    "cell_id": cell_id,
                    "signed_delta_c": signed_delta,
                    "judge_A_count": judge_a,
                    "human_A_count": human_a,
                    "judge_A_rate": judge_a / len(pairs),
                    "human_A_rate": human_a / len(pairs),
                    "paired_units": len(pairs),
                    "cell_pass_identity": "FORWARD_VS_FINAL_HUMAN",
                },
            )
        leniency_gates.append(gate)

    # Fixed planned-request denominator includes one initial request per case and order.
    planned_requests = 2 * n_assigned_cases
    failed_initial_requests = (
        sum(request.initial_failed for request in forward.values())
        + sum(request.initial_failed for request in reverse.values())
    )
    orphaned_output = unmatched_responses > 0
    if planned_requests == 0:
        invalid_gate = GateResult(
            gate_name="Invalid Output Gate", status="UNAVAILABLE", observed_value=None,
            threshold=GATE_INVALID_OUTPUT_MAX, comparator="<=", numerator=0, denominator=0,
            details={"unmatched_responses": unmatched_responses},
        )
    else:
        invalid_rate = failed_initial_requests / planned_requests
        invalid_status: GateStatus = (
            "FAIL" if invalid_rate > GATE_INVALID_OUTPUT_MAX or orphaned_output else "PASS"
        )
        invalid_gate = GateResult(
            gate_name="Invalid Output Gate", status=invalid_status, observed_value=invalid_rate,
            threshold=GATE_INVALID_OUTPUT_MAX, comparator="<=",
            numerator=failed_initial_requests, denominator=planned_requests,
            details={
                "initial_failed_requests": failed_initial_requests,
                "planned_initial_requests": planned_requests,
                "unmatched_responses": unmatched_responses,
                "first_attempt_dispositions": initial_dispositions,
                "terminal_attempt_dispositions": terminal_dispositions,
                "attempt_count_histogram": {
                    "forward": {
                        str(attempt_count): sum(
                            len(req.attempts) == attempt_count for req in forward.values()
                        )
                        for attempt_count in sorted({len(req.attempts) for req in forward.values()})
                    },
                    "reverse": {
                        str(attempt_count): sum(
                            len(req.attempts) == attempt_count for req in reverse.values()
                        )
                        for attempt_count in sorted({len(req.attempts) for req in reverse.values()})
                    },
                },
            },
        )

    # D counts unique assigned case-candidate identities after cross-arm deduplication.
    D = total_assigned_units
    F = 0
    M = 0
    for case in normalized_cases:
        f_response = _request_usable(forward[case.case_id], expected_model)
        r_response = _request_usable(reverse[case.case_id], expected_model)
        for candidate in case.candidates:
            forward_j = f_response.judgments.get(candidate.opaque_id) if f_response else None
            reverse_j = r_response.judgments.get(candidate.opaque_id) if r_response else None
            if forward_j is None or reverse_j is None:
                M += 1
            elif forward_j.label != reverse_j.label:
                F += 1
    if D == 0:
        instability_gate = GateResult(
            gate_name="Order Instability Gate", status="UNAVAILABLE", observed_value=None,
            threshold=GATE_INSTABILITY_UPPER_MAX, comparator="<=", numerator=0, denominator=0,
            details={"different_valid_labels_F": 0, "missing_either_M": 0},
        )
    else:
        lower_bound = F / D
        upper_bound = (F + M) / D
        conditional = F / (D - M) if D > M else None
        instability_gate = GateResult(
            gate_name="Order Instability Gate",
            status="PASS" if upper_bound <= GATE_INSTABILITY_UPPER_MAX else "FAIL",
            observed_value=upper_bound, threshold=GATE_INSTABILITY_UPPER_MAX,
            comparator="<=", numerator=F + M, denominator=D,
            details={
                "different_valid_labels_F": F,
                "missing_either_M": M,
                "assigned_unique_units_D": D,
                "conditional_instability": conditional,
                "full_assignment_lower_bound": lower_bound,
                "full_assignment_upper_bound": upper_bound,
            },
        )

    # Readiness gates prevent synthetic or development-only evidence from being accepted.
    if source_hashes_valid:
        source_status_gate: GateStatus = "PASS"
    elif source_inputs_complete:
        source_status_gate = "FAIL"
    else:
        source_status_gate = "UNAVAILABLE"
    if source_status_gate == "PASS":
        source_gate = GateResult(
            gate_name="Source Input Pins", status="PASS", observed_value=1.0,
            threshold=1.0, comparator="=", numerator=matched_source_pin_count,
            denominator=len(required_source_names),
            details={"required_source_pins": sorted(required_source_names), "raw_manifest_bytes_verified": True},
        )
    else:
        source_gate = GateResult(
            gate_name="Source Input Pins", status=source_status_gate, observed_value=None,
            threshold=1.0, comparator="=", numerator=matched_source_pin_count,
            denominator=len(required_source_names),
            details={
                "required_source_pins": sorted(required_source_names),
                "raw_manifest_bytes_verified": bool(observed_source_hashes),
                "observed_source_pin_count": len(observed_source_hashes),
                "matched_source_pin_count": matched_source_pin_count,
                "expected_pins_match_observed_bytes": source_hashes_valid,
            },
        )
    ref_complete = (
        human_reference_state == "FINAL_ADJUDICATED"
        and reference_present_units == total_assigned_units
        and invalid_human == 0
        and missing_human == 0
        and unmatched_human == 0
    )
    if invalid_human or unmatched_human:
        reference_status: GateStatus = "FAIL"
    elif ref_complete:
        reference_status = "PASS"
    else:
        reference_status = "UNAVAILABLE"
    if reference_status == "PASS":
        reference_gate = GateResult(
            gate_name="Human Reference Readiness", status="PASS", observed_value=1.0,
            threshold=1.0, comparator="=", numerator=reference_present_units,
            denominator=total_assigned_units,
            details={"reference_state": human_reference_state, "missing": missing_human,
                     "invalid": invalid_human, "unmatched": unmatched_human},
        )
    else:
        reference_gate = GateResult(
            gate_name="Human Reference Readiness", status=reference_status, observed_value=None,
            threshold=1.0, comparator="=", numerator=reference_present_units,
            denominator=total_assigned_units,
            details={"reference_state": human_reference_state, "missing": missing_human,
                     "invalid": invalid_human, "unmatched": unmatched_human},
        )
    prompt_assets_match = (
        prompt_assets["system_prompt_file_verified"]
        and prompt_assets["user_template_file_verified"]
    )
    prompt_ready = prompt_freeze_state == "FROZEN" and prompt_assets_match
    prompt_status: GateStatus = (
        "PASS" if prompt_ready
        else "FAIL" if prompt_freeze_state == "FROZEN" and not prompt_assets_match
        else "UNAVAILABLE"
    )
    prompt_gate = GateResult(
        gate_name="Prompt Freeze Readiness",
        status=prompt_status,
        observed_value=1.0 if prompt_ready else None, threshold=1.0, comparator="=",
        numerator=int(prompt_ready), denominator=1,
        details={"prompt_freeze_state": prompt_freeze_state, **prompt_assets},
    )

    family_map = dict(generator_family_by_arm or {})
    known_families = {"google", "openai", "mistral", "non_llm"}
    missing_arm_family_units = 0
    self_family_units = 0
    for case in normalized_cases:
        for candidate in case.candidates:
            memberships = candidate.arm_memberships
            if not memberships:
                missing_arm_family_units += 1
                continue
            families = [family_map.get(arm) for arm in memberships]
            if any(family not in known_families for family in families):
                missing_arm_family_units += 1
                continue
            if (expected_model == PRIMARY_JUDGE_MODEL and "google" in families) or (
                expected_model == DIAGNOSTIC_JUDGE_MODEL and "openai" in families
            ):
                self_family_units += 1
    identity_denominator = actual_attempt_count
    summary_identity_mismatch = (
        judge_model_returned != expected_model or judge_model_requested != expected_model
        or expected_model == "<unsupported-model>"
    )
    if method_identity_errors or summary_identity_mismatch:
        identity_status: GateStatus = "FAIL"
    elif identity_denominator == 0:
        identity_status = "UNAVAILABLE"
    else:
        identity_status = "PASS"
    identity_gate = GateResult(
        gate_name="Judge Model and Family Identity Gate", status=identity_status,
        observed_value=(method_identity_errors / identity_denominator) if identity_denominator else None,
        threshold=0.0, comparator="=", numerator=method_identity_errors,
        denominator=identity_denominator,
        details={
            "expected_model": expected_model,
            "requested_model_summary": judge_model_requested,
            "returned_model_summary": judge_model_returned,
            "response_identity_failures": method_identity_errors,
            "summary_identity_mismatch": summary_identity_mismatch,
        },
    )
    family_failures = self_family_units + missing_arm_family_units
    family_status: GateStatus
    if self_family_units:
        family_status = "FAIL"
    elif missing_arm_family_units or total_assigned_units == 0:
        family_status = "UNAVAILABLE"
    else:
        family_status = "PASS"
    family_gate = GateResult(
        gate_name="Candidate Generator Family Exclusion Gate",
        status=family_status,
        observed_value=(family_failures / total_assigned_units) if total_assigned_units else None,
        threshold=0.0,
        comparator="=",
        numerator=family_failures,
        denominator=total_assigned_units,
        details={
            "self_family_candidate_units": self_family_units,
            "candidate_units_missing_generator_family": missing_arm_family_units,
            "allowed_generator_families": sorted(known_families),
        },
    )

    all_gates = [
        invalid_gate, instability_gate, kappa_gate, kappa_ci_gate,
        *leniency_gates, source_gate, reference_gate, prompt_gate, identity_gate, family_gate,
    ]
    if any(gate.status == "FAIL" for gate in all_gates):
        overall: Literal["ACCEPTED", "REJECTED", "INCOMPLETE_GATES_UNAVAILABLE"] = "REJECTED"
    elif any(gate.status == "UNAVAILABLE" for gate in all_gates):
        overall = "INCOMPLETE_GATES_UNAVAILABLE"
    else:
        overall = "ACCEPTED"

    def provenance(gate: GateResult, pass_identity: str) -> GateResult:
        return _gate_with_provenance(
            gate, pass_identity=pass_identity, input_hashes=input_hashes
        )

    kappa_gate = provenance(kappa_gate, "FORWARD_VS_FINAL_HUMAN_A_C_N_U")
    kappa_ci_gate = provenance(kappa_ci_gate, "FORWARD_VS_FINAL_HUMAN_CASE_CLUSTER_BOOTSTRAP")
    leniency_gates = [provenance(gate, gate.details.get("cell_id", "cell-unavailable")) for gate in leniency_gates]
    invalid_gate = provenance(invalid_gate, "FORWARD_AND_REVERSE_FIXED_INITIAL_REQUESTS")
    instability_gate = provenance(instability_gate, "FORWARD_VS_REVERSE_UNIQUE_CANDIDATE_UNITS")
    identity_gate = provenance(identity_gate, "PER_RESPONSE_MODEL_AND_GENERATOR_FAMILY")
    family_gate = provenance(family_gate, "CANDIDATE_GENERATOR_FAMILY_EXCLUSION")
    source_gate = provenance(source_gate, "SOURCE_MANIFEST_INPUTS")
    reference_gate = provenance(reference_gate, "FINAL_ADJUDICATED_REFERENCES")
    prompt_gate = provenance(prompt_gate, "FROZEN_PROMPT_ASSETS")
    return C4PoolJudgeEvaluationReport(
        specification_id=SPECIFICATION_ID,
        judge_model_requested=judge_model_requested,
        judge_model_returned=judge_model_returned,
        overall_status=overall,
        assigned_cases=n_assigned_cases,
        assigned_candidate_units=total_assigned_units,
        paired_human_reference_units=n_paired,
        agreement_kappa_gate=kappa_gate,
        kappa_ci_lower_gate=kappa_ci_gate,
        leniency_cell_gates=leniency_gates,
        order_instability_gate=instability_gate,
        invalid_output_gate=invalid_gate,
        judge_model_identity_gate=identity_gate,
        generator_family_gate=family_gate,
        source_input_gate=source_gate,
        human_reference_gate=reference_gate,
        prompt_freeze_gate=prompt_gate,
        reference_present_candidate_units=reference_present_units,
        judge_valid_candidate_units=judge_valid_units,
        missing_human_reference_units=missing_human,
        invalid_human_reference_units=invalid_human,
        judge_u_count=judge_u_count,
        human_u_count=human_u_count,
        arm_summaries=arm_summaries,
        input_hashes=input_hashes,
        source_input_hash_status=source_status,
        human_reference_state=human_reference_state,
        prompt_freeze_state=prompt_freeze_state,
        actual_attempt_count=actual_attempt_count,
        invalid_attempt_count=invalid_attempt_count,
        unmatched_response_count=unmatched_responses,
        unmatched_human_reference_count=unmatched_human,
    )


def build_gpt4o_common_slate(
    preselected_cases: list[PoolJudgeCase],
    *,
    eligible_arm_ids: Sequence[str],
    gpt4o_generator_arm_ids: Sequence[str],
    mistral_arm_id: Optional[str] = None,
    mistral_dr04_eligible: bool = False,
) -> list[PoolJudgeCase]:
    """Build the exact matched diagnostic slate; never shrink the case set.

    ``preselected_cases`` must already be the fixed audit assignment. The only
    candidate arms admitted are the explicitly named GISMo/lookup arms and,
    when separately eligible under DR-04, the exact Mistral arm ID. Every
    GPT-4o-generated identity is excluded even if it also appears in an
    otherwise eligible arm.
    """
    if not preselected_cases or not eligible_arm_ids or not gpt4o_generator_arm_ids:
        raise ValueError("common_slate_assignment_or_arm_ids_missing")
    eligible = set(eligible_arm_ids)
    self_family = set(gpt4o_generator_arm_ids)
    if any(not isinstance(value, str) or not value for value in eligible | self_family):
        raise ValueError("common_slate_arm_id_invalid")
    if eligible != COMMON_SLATE_BASE_ARMS:
        raise ValueError("common_slate_requires_exact_gismo_lookup_base_arms")
    if mistral_dr04_eligible:
        if not isinstance(mistral_arm_id, str) or not mistral_arm_id:
            raise ValueError("mistral_dr04_arm_id_required")
        eligible.add(mistral_arm_id)
    cases = normalize_pool_cases(preselected_cases)
    output: list[PoolJudgeCase] = []
    for case in cases:
        candidates: list[CandidateItem] = []
        for candidate in case.candidates:
            memberships = set(candidate.arm_memberships)
            if not memberships:
                raise ValueError("common_slate_candidate_arm_membership_missing")
            if memberships & self_family:
                continue
            if memberships & eligible:
                candidates.append(candidate)
        if not candidates:
            raise ValueError("common_slate_would_drop_preselected_case")
        output.append(replace(case, candidates=candidates))
    return output


def common_slate_fingerprint(cases: list[PoolJudgeCase]) -> str:
    """Hash the exact ordered prompt-visible slate shared by both judges."""
    normalized = normalize_pool_cases(cases)
    material = [
        {
            "case_id": case.case_id,
            "recipe_title": case.recipe_title,
            "ingredients": list(case.ingredients),
            "target_ingredient": case.target_ingredient,
            "candidates": [
                {"id": candidate.opaque_id, "candidate": candidate.candidate_text}
                for candidate in case.candidates
            ],
        }
        for case in normalized
    ]
    raw = json.dumps(
        material, ensure_ascii=False, separators=(",", ":"), allow_nan=False
    ).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()


def render_c4_report_markdown(report: C4PoolJudgeEvaluationReport) -> str:
    """Render a review-ready report without exposing case IDs or raw judgments."""
    gates = [
        report.agreement_kappa_gate,
        report.kappa_ci_lower_gate,
        report.order_instability_gate,
        report.invalid_output_gate,
        report.judge_model_identity_gate,
        report.generator_family_gate,
        report.source_input_gate,
        report.human_reference_gate,
        report.prompt_freeze_gate,
        *report.leniency_cell_gates,
    ]

    def number(value: Optional[float]) -> str:
        return "—" if value is None else f"{value:.6f}"

    def count(value: Optional[float]) -> str:
        if value is None:
            return "—"
        return str(int(value)) if float(value).is_integer() else f"{value:.6f}"

    def details(gate: GateResult) -> str:
        values = gate.details
        if "requested_draws" in values:
            return (
                f"bootstrap requested={values['requested_draws']}; "
                f"valid={values.get('valid_draws', 0)}; "
                f"degenerate={values.get('degenerate_draws', 0)}; seed {values.get('seed')}"
            )
        if "full_assignment_upper_bound" in values:
            return (
                f"F={values.get('different_valid_labels_F')}; M={values.get('missing_either_M')}; "
                f"D={values.get('assigned_unique_units_D')}; "
                f"bounds [{number(values.get('full_assignment_lower_bound'))}, "
                f"{number(values.get('full_assignment_upper_bound'))}]; "
                f"conditional={number(values.get('conditional_instability'))}"
            )
        if "signed_delta_c" in values:
            return (
                f"signed delta={number(values['signed_delta_c'])}; "
                f"judge A={values.get('judge_A_count')}; human A={values.get('human_A_count')}"
            )
        if "initial_failed_requests" in values:
            return (
                f"initial failures={values.get('initial_failed_requests')}; "
                f"unmatched outputs={values.get('unmatched_responses')}; "
                f"first={values.get('first_attempt_dispositions')}; "
                f"terminal={values.get('terminal_attempt_dispositions')}; "
                f"attempt-count histogram={values.get('attempt_count_histogram')}"
            )
        if "missing" in values and "invalid" in values:
            return (
                f"missing={values.get('missing')}; invalid={values.get('invalid')}; "
                f"unmatched={values.get('unmatched')}"
            )
        if "response_identity_failures" in values:
            return (
                f"response identity failures={values.get('response_identity_failures')}; "
                f"summary mismatch={values.get('summary_identity_mismatch')}"
            )
        if "self_family_candidate_units" in values:
            return (
                f"self-family units={values.get('self_family_candidate_units')}; "
                f"missing family map={values.get('candidate_units_missing_generator_family')}"
            )
        return ""

    lines = [
        "# C4 pool-judge gate report",
        "",
        f"**Disposition:** `{report.overall_status}`  ",
        f"**Model:** requested `{report.judge_model_requested}`; returned summary `{report.judge_model_returned}`  ",
        f"**Specification:** `{report.specification_id}`",
        "",
        "## Coverage and completeness",
        "",
        "| Measure | Count |",
        "|---|---:|",
        f"| Assigned cases | {report.assigned_cases} |",
        f"| Assigned unique candidate units | {report.assigned_candidate_units} |",
        f"| Human-reference-present units | {report.reference_present_candidate_units} |",
        f"| Judge-valid forward units | {report.judge_valid_candidate_units} |",
        f"| Paired judge/reference units | {report.paired_human_reference_units} |",
        f"| Missing human references | {report.missing_human_reference_units} |",
        f"| Invalid human references | {report.invalid_human_reference_units} |",
        f"| Judge U labels | {report.judge_u_count} |",
        f"| Human U labels | {report.human_u_count} |",
        f"| Actual attempts / invalid attempts | {report.actual_attempt_count} / {report.invalid_attempt_count} |",
        f"| Unmatched judge / human records | {report.unmatched_response_count} / {report.unmatched_human_reference_count} |",
        f"| Source inputs | `{report.source_input_hash_status}` |",
        f"| Human references | `{report.human_reference_state}` |",
        f"| Prompt state | `{report.prompt_freeze_state}` |",
        "",
        "## Gates",
        "",
        "| Gate | Status | Observed | Rule | Numerator / denominator | Pass identity | Details |",
        "|---|---|---:|---:|---:|---|---|",
    ]
    for gate in gates:
        rule = f"{gate.comparator} {gate.threshold:.6f}"
        ratio = f"{count(gate.numerator)} / {count(gate.denominator)}"
        lines.append(
            f"| {gate.gate_name} | **{gate.status}** | {number(gate.observed_value)} | "
            f"{rule} | {ratio} | `{gate.pass_identity}` | {details(gate)} |"
        )
    if report.arm_summaries:
        lines.extend([
            "",
            "## Arm summaries",
            "",
            "| Arm | Assigned units | Paired units | Judge A/C/N/U | Human A/C/N/U |",
            "|---|---:|---:|---:|---:|",
        ])
        for arm in sorted(report.arm_summaries, key=lambda value: value.encode("utf-8")):
            summary = report.arm_summaries[arm]
            judge_counts = "/".join(str(summary[f"judge_{label}"]) for label in PERMITTED_LABELS)
            human_counts = "/".join(str(summary[f"human_{label}"]) for label in PERMITTED_LABELS)
            lines.append(
                f"| {arm} | {summary['assigned_candidate_units']} | {summary['paired_units']} | "
                f"{judge_counts} | {human_counts} |"
            )
    lines.extend(["", "## Input hashes", "", "| Input | SHA-256 |", "|---|---|"])
    for name, digest in sorted(report.input_hashes.items()):
        lines.append(f"| `{name}` | `{digest}` |")
    lines.extend([
        "",
        "The report contains no raw candidate text, case identifiers, or raw judge responses. "
        "A source-free or synthetic report is implementation evidence only; it does not establish "
        "human validity, prompt freeze, empirical C4 performance, or C4 acceptance.",
    ])
    return "\n".join(lines) + "\n"
