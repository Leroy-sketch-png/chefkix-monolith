"""L46 Defense Rehearsal Pack & Hostile Committee Challenge Evaluator.

Implements Lead L46 defense answers, adversarial committee rubric, and oral rehearsal gates:
- Defense Pack SHA-256: 7da53af33901328cf9974378eebba85bc2d966338d062b33d80a58ed515aa8e2
- Protocol v1 SHA-256: b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12
- Clean Graph Manifest SHA-256: ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1
- Validates all 28 hostile committee challenges (Q01-Q28) across 7 epistemic fields:
  * Committee Question
  * Naive / Fatal Trap
  * Defensible SSoT Answer
  * Epistemic Status
  * Evidence Pointers
  * Falsifier / Reversal
  * Forbidden Claim Boundary
- Evaluates 42/48 Rubric across 8 categories (C1-C8, 6 pts max each) with fail-closed disqualification on zero scores.
- Scans for 12 non-negotiable forbidden claims (clinical safety, 100% safe, judge human validity, etc.).
- Enforces oral presentation boundary: automated checks do not substitute for observed timed human walkthrough.
"""
from __future__ import annotations

import dataclasses
import hashlib
from pathlib import Path
import re
from typing import Any, Dict, List, Optional, Set, Tuple


DEFENSE_DOSSIER_SHA256 = "7da53af33901328cf9974378eebba85bc2d966338d062b33d80a58ed515aa8e2"
PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
CLEAN_GRAPH_MANIFEST_SHA256 = "ade85c3312b8d72c851c525d34869e70d759aec144b0d1c2d6d2be6f114c1ef1"

REQUIRED_CHALLENGES = tuple(f"Q{i:02d}" for i in range(1, 29))

REQUIRED_FIELDS = (
    "Committee Question:",
    "Naive / Fatal Trap:",
    "Defensible SSoT Answer:",
    "Epistemic Status:",
    "Evidence Pointers:",
    "Falsifier / Reversal:",
    "Forbidden Claim Boundary:",
)

RUBRIC_CATEGORIES = {
    "C1": "Research Problem & Grounding",
    "C2": "Methodological Rigor & Protocol v1 Adherence",
    "C3": "Negative & Bounded Results Integrity",
    "C4": "Allergen & Safety Claim Calibration",
    "C5": "Engineering, Implementation & Architecture",
    "C6": "Evaluation, Baselines & Metrics",
    "C7": "Epistemic Honesty & Scientific Boundaries",
    "C8": "Defense Rehearsal & Live Evidence Demo",
}

PASSING_FLOOR = 42
MAX_TOTAL_POINTS = 48
MAX_POINTS_PER_CATEGORY = 6

# Zero score on any of these critical categories results in immediate disqualification
CRITICAL_CATEGORIES = frozenset({"C2", "C3", "C4", "C7"})

FORBIDDEN_PHRASES = (
    (r"\bclinically safe\b", "Prohibited clinical safety claim"),
    (r"\b100% safe\b", "Prohibited absolute safety claim"),
    (r"\bzero true violations\b", "Prohibited absolute safety claim"),
    (r"\bjudge provides human validity\b", "Prohibited proxy validity claim"),
    (r"\bwe invented conformal prediction\b", "Prohibited algorithmic priority claim"),
    (r"\bmistral.*ran to completion at 561\b", "Prohibited Mistral execution claim"),
    (r"\bsignal count as novelty\b", "Prohibited signal count novelty claim"),
    (r"\buniversal conformal guarantee\b", "Prohibited unconditioned conformal claim"),
    (r"\bfirst substitution system\b", "Refuted priority claim"),
    (r"\bfirst human-evaluated substitution\b", "Refuted priority claim"),
    (r"\bcomplete-case analysis\b", "Prohibited survivor-bias analysis"),
    (r"\bimpute.*U.*as failure\b", "Prohibited unknown imputation"),
)


@dataclasses.dataclass(frozen=True)
class DefenseChallenge:
    id: str
    question: str
    trap: str
    defensible_answer: str
    epistemic_status: str
    evidence_pointers: str
    falsifier: str
    forbidden_boundary: str
    is_valid: bool
    missing_fields: List[str]


@dataclasses.dataclass(frozen=True)
class RubricScoreCard:
    category_scores: Dict[str, int]
    total_score: int
    passes_floor: bool
    is_disqualified: bool
    disqualification_reason: Optional[str]


@dataclasses.dataclass(frozen=True)
class DefenseAuditReport:
    is_hash_verified: bool
    dossier_sha256: str
    challenges_count: int
    valid_challenges_count: int
    forbidden_claim_violations: List[str]
    rubric_evaluation: Optional[RubricScoreCard]
    oral_rehearsal_status: str
    is_accepted: bool
    summary: str


def compute_file_sha256(path: Path) -> str:
    """Computes SHA-256 hex digest of a file."""
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def scan_forbidden_claims(text: str) -> List[str]:
    """Scans text for forbidden phrases and antipatterns."""
    violations: List[str] = []
    # Avoid flagging occurrences inside "Forbidden Claim Boundary:" descriptions
    # by splitting or scanning answers
    for pattern, reason in FORBIDDEN_PHRASES:
        for match in re.finditer(pattern, text, flags=re.IGNORECASE):
            # Check context to ensure it's not preceded by 'Forbidden' or 'Never claim'
            start = max(0, match.start() - 60)
            prefix = text[start:match.start()].lower()
            if "forbidden" in prefix or "never" in prefix or "do not" in prefix or "cannot" in prefix:
                continue
            violations.append(f"Match '{match.group(0)}': {reason}")
    return violations


def parse_defense_pack(content: str) -> Dict[str, Dict[str, str]]:
    """Parses challenges Q01-Q28 from defense pack markdown."""
    challenges: Dict[str, Dict[str, str]] = {}
    
    # Match blocks starting with '### Challenge Qxx:' up to next challenge, divider, or EOF
    challenge_blocks = re.findall(
        r"###\s+Challenge\s+(Q\d{2})[:\s]+(.*?)(?=(?:###\s+Challenge\s+Q|\n---\n|\Z))",
        content,
        re.DOTALL,
    )
    for qid, block in challenge_blocks:
        fields: Dict[str, str] = {}
        for req_field in REQUIRED_FIELDS:
            # Matches '- **Field:** value'
            field_pattern = rf"\*\*{re.escape(req_field)}\*\*\s*(.*?)(?=(?:\n-\s+\*\*|\Z))"
            m = re.search(field_pattern, block, re.DOTALL)
            if m:
                fields[req_field] = m.group(1).strip()
            else:
                fields[req_field] = ""
        challenges[qid] = fields
    return challenges


def evaluate_rubric(scores: Dict[str, int]) -> RubricScoreCard:
    """Evaluates 42/48 Rubric with fail-closed critical zero-score disqualification."""
    clean_scores: Dict[str, int] = {}
    for cat in RUBRIC_CATEGORIES:
        score = scores.get(cat, 0)
        # Cap between 0 and 6
        clean_scores[cat] = max(0, min(MAX_POINTS_PER_CATEGORY, score))

    total = sum(clean_scores.values())

    # Disqualification check on critical categories
    is_disqualified = False
    disq_reason: Optional[str] = None
    for crit in CRITICAL_CATEGORIES:
        if clean_scores.get(crit, 0) == 0:
            is_disqualified = True
            disq_reason = f"Critical Category {crit} ({RUBRIC_CATEGORIES[crit]}) received 0 points: automatic disqualification"
            break

    passes = (total >= PASSING_FLOOR) and not is_disqualified

    return RubricScoreCard(
        category_scores=clean_scores,
        total_score=total,
        passes_floor=passes,
        is_disqualified=is_disqualified,
        disqualification_reason=disq_reason,
    )


def validate_defense_rehearsal_pack(
    dossier_path_or_content: Path | str,
    observed_rubric_scores: Optional[Dict[str, int]] = None,
    observed_timed_rehearsal: bool = False,
) -> DefenseAuditReport:
    """Full L46 audit of the Defense Rehearsal Pack and oral readiness boundary."""
    if isinstance(dossier_path_or_content, Path):
        raw_bytes = dossier_path_or_content.read_bytes()
        file_sha256 = hashlib.sha256(raw_bytes).hexdigest()
        content = raw_bytes.decode("utf-8")
    else:
        content = dossier_path_or_content
        file_sha256 = hashlib.sha256(content.encode("utf-8")).hexdigest()

    is_hash_ok = (file_sha256.lower() == DEFENSE_DOSSIER_SHA256.lower())

    # Check pins inside content
    has_protocol_pin = PROTOCOL_SHA256 in content
    has_clean_graph_pin = CLEAN_GRAPH_MANIFEST_SHA256 in content

    # Parse and validate challenges
    parsed = parse_defense_pack(content)
    valid_count = 0
    challenges_data: List[DefenseChallenge] = []

    for qid in REQUIRED_CHALLENGES:
        fields = parsed.get(qid, {})
        missing = [f for f in REQUIRED_FIELDS if not fields.get(f)]
        is_valid = len(missing) == 0
        if is_valid:
            valid_count += 1
        challenges_data.append(DefenseChallenge(
            id=qid,
            question=fields.get("Committee Question:", ""),
            trap=fields.get("Naive / Fatal Trap:", ""),
            defensible_answer=fields.get("Defensible SSoT Answer:", ""),
            epistemic_status=fields.get("Epistemic Status:", ""),
            evidence_pointers=fields.get("Evidence Pointers:", ""),
            falsifier=fields.get("Falsifier / Reversal:", ""),
            forbidden_boundary=fields.get("Forbidden Claim Boundary:", ""),
            is_valid=is_valid,
            missing_fields=missing,
        ))

    # Scan for forbidden claims in defensible answers
    answers_text = "\n".join(c.defensible_answer for c in challenges_data)
    forbidden_violations = scan_forbidden_claims(answers_text)

    # Evaluate Rubric
    rubric_card: Optional[RubricScoreCard] = None
    if observed_rubric_scores:
        rubric_card = evaluate_rubric(observed_rubric_scores)

    # Oral rehearsal boundary check
    if observed_timed_rehearsal and rubric_card and rubric_card.passes_floor:
        oral_status = "REHEARSAL_VERIFIED_PASS"
    elif observed_timed_rehearsal:
        oral_status = "REHEARSAL_VERIFIED_FAIL"
    else:
        oral_status = "PACK_VALIDATED_ORAL_REHEARSAL_PENDING"

    is_pack_structurally_valid = (
        is_hash_ok and
        has_protocol_pin and
        has_clean_graph_pin and
        valid_count == len(REQUIRED_CHALLENGES) and
        len(forbidden_violations) == 0
    )

    summary_lines = [
        f"L46 Defense Rehearsal Pack Audit Report",
        f"Dossier SHA-256: {file_sha256} (SSoT match: {is_hash_ok})",
        f"Protocol v1 Pin: {'FOUND' if has_protocol_pin else 'MISSING'}",
        f"Clean Graph Pin: {'FOUND' if has_clean_graph_pin else 'MISSING'}",
        f"Hostile Challenges: {valid_count}/{len(REQUIRED_CHALLENGES)} complete",
        f"Forbidden Claims Violations: {len(forbidden_violations)}",
        f"Oral Rehearsal Status: {oral_status}",
        f"Structural Acceptance: {'ACCEPTED' if is_pack_structurally_valid else 'REJECTED'}",
    ]
    if rubric_card:
        summary_lines.append(
            f"Rubric Score: {rubric_card.total_score}/{MAX_TOTAL_POINTS} "
            f"({'PASS' if rubric_card.passes_floor else 'FAIL'}, Disqualified: {rubric_card.is_disqualified})"
        )

    return DefenseAuditReport(
        is_hash_verified=is_hash_ok,
        dossier_sha256=file_sha256,
        challenges_count=len(parsed),
        valid_challenges_count=valid_count,
        forbidden_claim_violations=forbidden_violations,
        rubric_evaluation=rubric_card,
        oral_rehearsal_status=oral_status,
        is_accepted=is_pack_structurally_valid,
        summary="\n".join(summary_lines),
    )
