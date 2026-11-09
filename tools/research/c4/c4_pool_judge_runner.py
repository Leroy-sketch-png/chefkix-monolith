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
from dataclasses import asdict, dataclass, field
from typing import Any, Callable, Final, Literal, Optional

# Protocol & Specification Identifiers
SPECIFICATION_ID: Final[str] = "iron-chef-l43-c4-pool-judge-v1"
PROTOCOL_SHA256: Final[str] = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"

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


def verify_prompt_assets() -> dict[str, bool]:
    """Verify that hardcoded prompts match their exact pinned SHA-256 hashes."""
    sys_hash = hashlib.sha256(SYSTEM_PROMPT.encode("utf-8")).hexdigest()
    user_hash = hashlib.sha256(USER_TEMPLATE.encode("utf-8")).hexdigest()
    return {
        "system_prompt_verified": (sys_hash == SYSTEM_PROMPT_SHA256),
        "user_template_verified": (user_hash == USER_TEMPLATE_SHA256),
    }


def format_user_prompt(
    case: PoolJudgeCase,
    reverse_order: bool = False,
) -> str:
    """Render the user prompt template with JSON-escaped placeholders."""
    candidates = list(case.candidates)
    if reverse_order:
        candidates.reverse()

    numbered_candidates = [
        {"id": c.opaque_id, "candidate": c.candidate_text}
        for c in candidates
    ]

    return USER_TEMPLATE.format(
        title=json.dumps(case.recipe_title),
        ingredient_list=json.dumps(case.ingredients),
        target=json.dumps(case.target_ingredient),
        numbered_candidates=json.dumps(numbered_candidates),
    )


def parse_and_validate_pool_response(
    raw_response_text: str,
    case: PoolJudgeCase,
    pass_name: Literal["FORWARD", "REVERSE"],
) -> PoolResponse:
    """Validate a model response against the opaque-ID schema and reason length."""
    expected_ids = {c.opaque_id for c in case.candidates}

    try:
        # Strip potential markdown code block markers
        text = raw_response_text.strip()
        if text.startswith("```json"):
            text = text[7:]
        elif text.startswith("```"):
            text = text[3:]
        if text.endswith("```"):
            text = text[:-3]
        text = text.strip()

        data = json.loads(text)
    except Exception as e:
        return PoolResponse(
            case_id=case.case_id,
            pass_name=pass_name,
            raw_text=raw_response_text,
            is_valid=False,
            error_message=f"JSON parse error: {e}",
        )

    if not isinstance(data, dict) or "judgments" not in data:
        return PoolResponse(
            case_id=case.case_id,
            pass_name=pass_name,
            raw_text=raw_response_text,
            is_valid=False,
            error_message="Missing top-level 'judgments' key",
        )

    judgments_dict = data["judgments"]
    if not isinstance(judgments_dict, dict):
        return PoolResponse(
            case_id=case.case_id,
            pass_name=pass_name,
            raw_text=raw_response_text,
            is_valid=False,
            error_message="'judgments' must be an object",
        )

    observed_ids = set(judgments_dict.keys())
    if observed_ids != expected_ids:
        missing = expected_ids - observed_ids
        foreign = observed_ids - expected_ids
        return PoolResponse(
            case_id=case.case_id,
            pass_name=pass_name,
            raw_text=raw_response_text,
            is_valid=False,
            error_message=f"Opaque ID mismatch. Missing: {missing}, Foreign: {foreign}",
        )

    parsed_judgments: dict[str, SingleJudgment] = {}
    for cid, val in judgments_dict.items():
        if not isinstance(val, dict):
            return PoolResponse(
                case_id=case.case_id,
                pass_name=pass_name,
                raw_text=raw_response_text,
                is_valid=False,
                error_message=f"Candidate {cid} judgment must be an object",
            )
        label = val.get("label")
        reason = val.get("reason")
        if label not in PERMITTED_LABELS:
            return PoolResponse(
                case_id=case.case_id,
                pass_name=pass_name,
                raw_text=raw_response_text,
                is_valid=False,
                error_message=f"Candidate {cid} invalid label: {label!r}",
            )
        if not isinstance(reason, str):
            return PoolResponse(
                case_id=case.case_id,
                pass_name=pass_name,
                raw_text=raw_response_text,
                is_valid=False,
                error_message=f"Candidate {cid} reason must be string",
            )
        words = reason.strip().split()
        if len(words) > MAX_REASON_WORDS:
            return PoolResponse(
                case_id=case.case_id,
                pass_name=pass_name,
                raw_text=raw_response_text,
                is_valid=False,
                error_message=f"Candidate {cid} reason exceeds 20 words ({len(words)} words)",
            )
        parsed_judgments[cid] = SingleJudgment(opaque_id=cid, label=label, reason=reason)

    return PoolResponse(
        case_id=case.case_id,
        pass_name=pass_name,
        raw_text=raw_response_text,
        is_valid=True,
        judgments=parsed_judgments,
    )


def compute_unweighted_cohens_kappa(
    paired_labels: list[tuple[str, str]],
) -> Optional[float]:
    """Compute unweighted nominal Cohen's kappa on A/C/N/U pairs.
    
    Returns None if no pairs or if marginals produce a degenerate denominator.
    """
    n = len(paired_labels)
    if n == 0:
        return None

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
    """Compute whole-case cluster bootstrap confidence interval for kappa.
    
    Order-statistic percentile convention:
    Indices floor(0.025 * B_valid) and floor(0.975 * B_valid) - 1.
    """
    case_ids = sorted(case_paired_units.keys())
    n_cases = len(case_ids)
    if n_cases == 0:
        return None, None, 0

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
    if b_valid < MIN_VALID_BOOTSTRAP_DRAWS:
        return None, None, b_valid

    valid_kappas.sort()
    lower_idx = math.floor(0.025 * b_valid)
    upper_idx = math.floor(0.975 * b_valid) - 1
    upper_idx = max(0, min(upper_idx, b_valid - 1))

    return valid_kappas[lower_idx], valid_kappas[upper_idx], b_valid


def evaluate_c4_operational_gates(
    cases: list[PoolJudgeCase],
    forward_responses: list[PoolResponse],
    reverse_responses: list[PoolResponse],
    human_adjudications: dict[str, dict[str, str]],  # case_id -> {opaque_id: label}
    judge_model_requested: str = "gemini-2.5-flash",
    judge_model_returned: str = "gemini-2.5-flash",
) -> C4PoolJudgeEvaluationReport:
    """Evaluate all Section 3.3 and L43 operational gates from forward/reverse passes."""
    n_assigned_cases = len(cases)
    total_assigned_units = sum(len(c.candidates) for c in cases)

    # 1. Invalid Output Gate: Proportion of failed requests across 2 * n_cases planned
    total_planned_requests = 2 * n_assigned_cases
    failed_requests = 0
    fwd_by_case = {r.case_id: r for r in forward_responses}
    rev_by_case = {r.case_id: r for r in reverse_responses}

    for c in cases:
        f_resp = fwd_by_case.get(c.case_id)
        if f_resp is None or not f_resp.is_valid:
            failed_requests += 1
        r_resp = rev_by_case.get(c.case_id)
        if r_resp is None or not r_resp.is_valid:
            failed_requests += 1

    if total_planned_requests == 0:
        invalid_gate = GateResult(
            gate_name="Invalid Output Gate",
            status="UNAVAILABLE",
            observed_value=None,
            threshold=GATE_INVALID_OUTPUT_MAX,
            comparator="<=",
            numerator=0,
            denominator=0,
        )
    else:
        invalid_rate = failed_requests / total_planned_requests
        invalid_pass = (invalid_rate <= GATE_INVALID_OUTPUT_MAX)
        invalid_gate = GateResult(
            gate_name="Invalid Output Gate",
            status="PASS" if invalid_pass else "FAIL",
            observed_value=invalid_rate,
            threshold=GATE_INVALID_OUTPUT_MAX,
            comparator="<=",
            numerator=failed_requests,
            denominator=total_planned_requests,
        )

    # 2. Order Instability Gate
    # D: assigned case-candidate units
    # F: number with valid different labels between forward and reverse
    # M: number lacking either valid label
    D = total_assigned_units
    F = 0
    M = 0

    for c in cases:
        f_resp = fwd_by_case.get(c.case_id)
        r_resp = rev_by_case.get(c.case_id)
        for cand in c.candidates:
            f_j = f_resp.judgments.get(cand.opaque_id) if f_resp and f_resp.is_valid else None
            r_j = r_resp.judgments.get(cand.opaque_id) if r_resp and r_resp.is_valid else None

            if f_j is None or r_j is None:
                M += 1
            else:
                if f_j.label != r_j.label:
                    F += 1

    if D == 0:
        instability_gate = GateResult(
            gate_name="Order Instability Gate",
            status="UNAVAILABLE",
            observed_value=None,
            threshold=GATE_INSTABILITY_UPPER_MAX,
            comparator="<=",
            numerator=0,
            denominator=0,
        )
    else:
        upper_instability = (F + M) / D
        cond_instability = (F / (D - M)) if (D - M) > 0 else None
        instability_pass = (upper_instability <= GATE_INSTABILITY_UPPER_MAX)
        instability_gate = GateResult(
            gate_name="Order Instability Gate",
            status="PASS" if instability_pass else "FAIL",
            observed_value=upper_instability,
            threshold=GATE_INSTABILITY_UPPER_MAX,
            comparator="<=",
            numerator=F + M,
            denominator=D,
            details={
                "different_valid_labels_F": F,
                "missing_either_M": M,
                "conditional_instability": cond_instability,
            },
        )

    # 3. Agreement Gates (Cohen's Kappa & 95% Bootstrap CI Lower Bound)
    # Forward pass is primary pass compared with human adjudications
    all_pairs: list[tuple[str, str]] = []
    case_paired_units: dict[str, list[tuple[str, str]]] = {}
    cell_pairs: dict[str, list[tuple[str, str]]] = {}

    for c in cases:
        f_resp = fwd_by_case.get(c.case_id)
        human_case = human_adjudications.get(c.case_id, {})
        c_pairs: list[tuple[str, str]] = []

        for cand in c.candidates:
            f_j = f_resp.judgments.get(cand.opaque_id) if f_resp and f_resp.is_valid else None
            h_label = human_case.get(cand.opaque_id)

            if f_j is not None and h_label is not None and h_label in PERMITTED_LABELS:
                pair = (f_j.label, h_label)
                all_pairs.append(pair)
                c_pairs.append(pair)
                cell_pairs.setdefault(cand.cell_id, []).append(pair)

        if c_pairs:
            case_paired_units[c.case_id] = c_pairs

    n_paired = len(all_pairs)
    kappa_val = compute_unweighted_cohens_kappa(all_pairs)

    if kappa_val is None:
        kappa_gate = GateResult(
            gate_name="Agreement Kappa Point Gate",
            status="UNAVAILABLE",
            observed_value=None,
            threshold=GATE_KAPPA_MIN,
            comparator=">=",
            numerator=0,
            denominator=n_paired,
        )
        kappa_ci_gate = GateResult(
            gate_name="Agreement Kappa CI Lower Gate",
            status="UNAVAILABLE",
            observed_value=None,
            threshold=GATE_KAPPA_LOWER95_MIN,
            comparator=">=",
            numerator=0,
            denominator=n_paired,
        )
    else:
        kappa_pass = (kappa_val >= GATE_KAPPA_MIN)
        kappa_gate = GateResult(
            gate_name="Agreement Kappa Point Gate",
            status="PASS" if kappa_pass else "FAIL",
            observed_value=kappa_val,
            threshold=GATE_KAPPA_MIN,
            comparator=">=",
            numerator=sum(1 for j, h in all_pairs if j == h),
            denominator=n_paired,
        )

        ci_lower, ci_upper, b_valid = compute_case_cluster_bootstrap_ci(case_paired_units)
        if ci_lower is None:
            kappa_ci_gate = GateResult(
                gate_name="Agreement Kappa CI Lower Gate",
                status="UNAVAILABLE",
                observed_value=None,
                threshold=GATE_KAPPA_LOWER95_MIN,
                comparator=">=",
                details={"valid_bootstrap_draws": b_valid},
            )
        else:
            ci_pass = (ci_lower >= GATE_KAPPA_LOWER95_MIN)
            kappa_ci_gate = GateResult(
                gate_name="Agreement Kappa CI Lower Gate",
                status="PASS" if ci_pass else "FAIL",
                observed_value=ci_lower,
                threshold=GATE_KAPPA_LOWER95_MIN,
                comparator=">=",
                details={"ci95_upper": ci_upper, "valid_bootstrap_draws": b_valid},
            )

    # 4. Leniency Gates: |delta_c| <= 10pp for each nonempty cell c
    leniency_gates: list[GateResult] = []
    prespecified_cells = sorted(set(cand.cell_id for c in cases for cand in c.candidates))

    for cell_id in prespecified_cells:
        cpairs = cell_pairs.get(cell_id, [])
        n_c = len(cpairs)
        if n_c == 0:
            leniency_gates.append(GateResult(
                gate_name=f"Leniency Gate ({cell_id})",
                status="UNAVAILABLE",
                observed_value=None,
                threshold=GATE_LENIENCY_DELTA_MAX,
                comparator="<=",
                numerator=0,
                denominator=0,
            ))
        else:
            judge_a_rate = sum(1 for j, h in cpairs if j == "A") / n_c
            human_a_rate = sum(1 for j, h in cpairs if h == "A") / n_c
            signed_delta = judge_a_rate - human_a_rate
            abs_delta = abs(signed_delta)
            l_pass = (abs_delta <= GATE_LENIENCY_DELTA_MAX)
            leniency_gates.append(GateResult(
                gate_name=f"Leniency Gate ({cell_id})",
                status="PASS" if l_pass else "FAIL",
                observed_value=abs_delta,
                threshold=GATE_LENIENCY_DELTA_MAX,
                comparator="<=",
                numerator=sum(1 for j, h in cpairs if j == "A"),
                denominator=n_c,
                details={
                    "signed_delta_c": signed_delta,
                    "judge_a_rate": judge_a_rate,
                    "human_a_rate": human_a_rate,
                },
            ))

    # Overall Disposition
    all_gates = [invalid_gate, instability_gate, kappa_gate, kappa_ci_gate] + leniency_gates
    has_fail = any(g.status == "FAIL" for g in all_gates)
    has_unavailable = any(g.status == "UNAVAILABLE" for g in all_gates)

    if has_fail:
        overall = "REJECTED"
    elif has_unavailable:
        overall = "INCOMPLETE_GATES_UNAVAILABLE"
    else:
        overall = "ACCEPTED"

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
    )
