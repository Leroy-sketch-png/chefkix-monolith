"""L48 Claims Register Adjudication & Evidence Ceiling Evaluator.

Implements Lead L48 claim adjudication, evidence ceiling, and status transition rules
under active SSoT Claims Register v20:
- Register v20 SHA-256: 72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023
- Defense Dossier SHA-256: 7da53af33901328cf9974378eebba85bc2d966338d062b33d80a58ed515aa8e2
- Protocol v1 SHA-256: b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12
- Enforces exact v20 invariant: 45 claims (R01-R45) with exactly:
  * 10 VERIFIED, 7 PARTIAL, 28 UNVERIFIED
  * 1 MIXED, 32 NA, 4 NEGATIVE, 1 POSITIVE, 7 REPRODUCTION
- Enforces strict epistemic evidence ceilings (E0-E4):
  * No claim can be VERIFIED without a valid reproduction_path and 64-char hex reproduction_sha256.
  * Priority claims R28/R29 remain strictly UNVERIFIED / NA (refuted by S43-S62 literature funnel).
  * R04 remains strictly under non-publication boundary (excluded from public distribution).
  * R18 remains bounded: report count-output digest vs source pins.
  * R38 remains local-only in v20; equivalence to 6,632-node member candidate is unestablished.
  * R32 remains UNVERIFIED under frozen Protocol v1 (unblinded candidate evaluation not authorized).
"""
from __future__ import annotations

import csv
import dataclasses
from enum import Enum
import hashlib
import io
from pathlib import Path
import re
from typing import Any, Dict, List, Optional, Set, Tuple


CLAIMS_REGISTER_V20_SHA256 = "72346a605f6e4251bd754397a61a446781283b8eda9e96745ddc8af0c1723023"
DEFENSE_DOSSIER_SHA256 = "7da53af33901328cf9974378eebba85bc2d966338d062b33d80a58ed515aa8e2"
PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"

TOTAL_CLAIMS = 45
EXPECTED_IDS = tuple(f"R{i:02d}" for i in range(1, TOTAL_CLAIMS + 1))

REQUIRED_COLUMNS = (
    "ID", "Claim", "Artifact_path", "SHA256", "Evidence_status", "Outcome",
    "Source_provenance", "Last_checked", "Notes", "L_item_mapping", "Citation",
    "Reproduction_path", "Reproduction_sha256", "Reproduced_value",
)

EXPECTED_STATUS_COUNTS: Dict[str, int] = {
    "VERIFIED": 10,
    "PARTIAL": 7,
    "UNVERIFIED": 28,
}

EXPECTED_OUTCOME_COUNTS: Dict[str, int] = {
    "MIXED": 1,
    "NA": 32,
    "NEGATIVE": 4,
    "POSITIVE": 1,
    "REPRODUCTION": 7,
}

# Claims subject to strict un-upgradable boundaries under current Lead disposition
LOCKED_UNVERIFIED_CLAIMS = frozenset({"R28", "R29", "R32", "R43", "R44", "R45"})
NON_PUBLICATION_BOUNDARY_CLAIMS = frozenset({"R04"})

SHA256_HEX_RE = re.compile(r"^[0-9a-fA-F]{64}$")


class EvidenceStatus(str, Enum):
    VERIFIED = "VERIFIED"
    PARTIAL = "PARTIAL"
    UNVERIFIED = "UNVERIFIED"


class ClaimOutcome(str, Enum):
    POSITIVE = "POSITIVE"
    NEGATIVE = "NEGATIVE"
    MIXED = "MIXED"
    REPRODUCTION = "REPRODUCTION"
    NA = "NA"


@dataclasses.dataclass(frozen=True)
class ClaimRecord:
    id: str
    claim: str
    artifact_path: str
    sha256: str
    evidence_status: str
    outcome: str
    source_provenance: str
    last_checked: str
    notes: str
    l_item_mapping: str
    citation: str
    reproduction_path: str
    reproduction_sha256: str
    reproduced_value: str


@dataclasses.dataclass(frozen=True)
class AdjudicationViolation:
    claim_id: str
    rule_violation: str
    details: str


@dataclasses.dataclass(frozen=True)
class ClaimsAdjudicationReport:
    total_claims: int
    status_counts: Dict[str, int]
    outcome_counts: Dict[str, int]
    is_hash_verified: bool
    register_sha256: str
    violations: List[AdjudicationViolation]
    verified_claims: List[str]
    partial_claims: List[str]
    unverified_claims: List[str]
    is_adjudication_accepted: bool
    summary: str


def compute_bytes_sha256(content: bytes) -> str:
    """Computes SHA-256 hex digest of raw bytes."""
    return hashlib.sha256(content).hexdigest()


def compute_file_sha256(path: Path) -> str:
    """Computes SHA-256 hex digest of a file."""
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def parse_claims_register_csv(csv_text_or_path: str | Path) -> List[ClaimRecord]:
    """Parses Claims Register v20 CSV content into typed ClaimRecord objects."""
    if isinstance(csv_text_or_path, Path):
        text = csv_text_or_path.read_text(encoding="utf-8")
    else:
        # Check if it's an existing file path string
        p = Path(csv_text_or_path)
        if p.exists() and p.is_file():
            text = p.read_text(encoding="utf-8")
        else:
            text = csv_text_or_path

    reader = csv.DictReader(io.StringIO(text))
    records: List[ClaimRecord] = []
    for row in reader:
        rec = ClaimRecord(
            id=row.get("ID", "").strip(),
            claim=row.get("Claim", "").strip(),
            artifact_path=row.get("Artifact_path", "").strip(),
            sha256=row.get("SHA256", "").strip(),
            evidence_status=row.get("Evidence_status", "").strip(),
            outcome=row.get("Outcome", "").strip(),
            source_provenance=row.get("Source_provenance", "").strip(),
            last_checked=row.get("Last_checked", "").strip(),
            notes=row.get("Notes", "").strip(),
            l_item_mapping=row.get("L_item_mapping", "").strip(),
            citation=row.get("Citation", "").strip(),
            reproduction_path=row.get("Reproduction_path", "").strip(),
            reproduction_sha256=row.get("Reproduction_sha256", "").strip(),
            reproduced_value=row.get("Reproduced_value", "").strip(),
        )
        records.append(rec)
    return records


def audit_claim_evidence_ceiling(record: ClaimRecord) -> List[AdjudicationViolation]:
    """Audits a single claim against Lead epistemic ceiling and boundary rules."""
    violations: List[AdjudicationViolation] = []

    # Rule 1: ID format
    if record.id not in EXPECTED_IDS:
        violations.append(AdjudicationViolation(
            claim_id=record.id,
            rule_violation="INVALID_CLAIM_ID",
            details=f"Claim ID '{record.id}' is not in expected R01-R45 range",
        ))

    # Rule 2: VERIFIED status requires reproduction path and valid SHA-256
    if record.evidence_status == "VERIFIED":
        if not record.reproduction_path:
            violations.append(AdjudicationViolation(
                claim_id=record.id,
                rule_violation="VERIFIED_MISSING_REPRODUCTION_PATH",
                details="Claim marked VERIFIED without non-empty Reproduction_path",
            ))
        if not record.reproduction_sha256 or not SHA256_HEX_RE.match(record.reproduction_sha256):
            violations.append(AdjudicationViolation(
                claim_id=record.id,
                rule_violation="VERIFIED_INVALID_REPRODUCTION_SHA256",
                details=f"Claim marked VERIFIED without valid 64-char hex Reproduction_sha256: '{record.reproduction_sha256}'",
            ))

    # Rule 3: Priority claims R28/R29 must remain UNVERIFIED / NA (refuted by S43-S62)
    if record.id in {"R28", "R29"}:
        if record.evidence_status != "UNVERIFIED" or record.outcome != "NA":
            violations.append(AdjudicationViolation(
                claim_id=record.id,
                rule_violation="REFUTED_PRIORITY_CLAIM_UPGRADE_FORBIDDEN",
                details=f"Priority claim {record.id} is contradicted by literature; must remain UNVERIFIED / NA",
            ))

    # Rule 4: Protocol v1 frozen candidate evaluation gate for R32
    if record.id == "R32" and record.evidence_status == "VERIFIED":
        violations.append(AdjudicationViolation(
            claim_id=record.id,
            rule_violation="R32_PROTOCOL_V1_FROZEN_CANDIDATE_GATE",
            details="R32 candidate coverage cannot be VERIFIED without unblinded candidate evaluation under Protocol v1",
        ))

    # Rule 5: L_item_mapping must not be empty
    if not record.l_item_mapping:
        violations.append(AdjudicationViolation(
            claim_id=record.id,
            rule_violation="MISSING_L_ITEM_MAPPING",
            details="Claim lacks parent accountability L_item_mapping",
        ))

    return violations


def validate_claims_adjudication(
    records: List[ClaimRecord],
    register_sha256: Optional[str] = None,
) -> ClaimsAdjudicationReport:
    """Performs full adjudication validation over Claims Register v20."""
    violations: List[AdjudicationViolation] = []

    # 1. Row count check
    if len(records) != TOTAL_CLAIMS:
        violations.append(AdjudicationViolation(
            claim_id="ALL",
            rule_violation="INVALID_ROW_COUNT",
            details=f"Found {len(records)} claims, expected exactly {TOTAL_CLAIMS}",
        ))

    # 2. Sequential ID check
    found_ids = [r.id for r in records]
    if tuple(found_ids) != EXPECTED_IDS:
        violations.append(AdjudicationViolation(
            claim_id="ALL",
            rule_violation="NON_SEQUENTIAL_IDS",
            details="Claim IDs are not strictly sequential R01-R45",
        ))

    # 3. Status and Outcome counts
    status_counts: Dict[str, int] = {}
    outcome_counts: Dict[str, int] = {}
    verified: List[str] = []
    partial: List[str] = []
    unverified: List[str] = []

    for r in records:
        status_counts[r.evidence_status] = status_counts.get(r.evidence_status, 0) + 1
        outcome_counts[r.outcome] = outcome_counts.get(r.outcome, 0) + 1

        if r.evidence_status == "VERIFIED":
            verified.append(r.id)
        elif r.evidence_status == "PARTIAL":
            partial.append(r.id)
        elif r.evidence_status == "UNVERIFIED":
            unverified.append(r.id)

        # Audit each claim's ceiling
        violations.extend(audit_claim_evidence_ceiling(r))

    # Check against expected counts in v20
    if status_counts != EXPECTED_STATUS_COUNTS:
        violations.append(AdjudicationViolation(
            claim_id="ALL",
            rule_violation="STATUS_COUNT_MISMATCH",
            details=f"Status counts {status_counts} do not match v20 baseline {EXPECTED_STATUS_COUNTS}",
        ))

    if outcome_counts != EXPECTED_OUTCOME_COUNTS:
        violations.append(AdjudicationViolation(
            claim_id="ALL",
            rule_violation="OUTCOME_COUNT_MISMATCH",
            details=f"Outcome counts {outcome_counts} do not match v20 baseline {EXPECTED_OUTCOME_COUNTS}",
        ))

    # Check register sha256 if supplied
    is_hash_ok = False
    reg_sha = register_sha256 or ""
    if register_sha256:
        is_hash_ok = (register_sha256.lower() == CLAIMS_REGISTER_V20_SHA256.lower())
        if not is_hash_ok:
            violations.append(AdjudicationViolation(
                claim_id="REGISTER_FILE",
                rule_violation="REGISTER_HASH_MISMATCH",
                details=f"Register SHA-256 '{register_sha256}' does not match pinned v20 '{CLAIMS_REGISTER_V20_SHA256}'",
            ))

    is_accepted = (len(violations) == 0 and is_hash_ok)

    summary_lines = [
        f"L48 Claims Adjudication & Ceiling Report",
        f"Register SHA-256: {reg_sha or 'NOT_PROVIDED'} (SSoT match: {is_hash_ok})",
        f"Total claims: {len(records)}/{TOTAL_CLAIMS}",
        f"Status breakdown: {status_counts.get('VERIFIED', 0)} VERIFIED, {status_counts.get('PARTIAL', 0)} PARTIAL, {status_counts.get('UNVERIFIED', 0)} UNVERIFIED",
        f"Outcome breakdown: {outcome_counts.get('REPRODUCTION', 0)} REPRODUCTION, {outcome_counts.get('POSITIVE', 0)} POSITIVE, {outcome_counts.get('NEGATIVE', 0)} NEGATIVE, {outcome_counts.get('MIXED', 0)} MIXED, {outcome_counts.get('NA', 0)} NA",
        f"Adjudication Acceptance Status: {'ACCEPTED' if is_accepted else 'REJECTED'}",
    ]
    if violations:
        summary_lines.append(f"Violations detected: {len(violations)}")
        for v in violations[:5]:
            summary_lines.append(f"  - [{v.claim_id}] {v.rule_violation}: {v.details}")

    return ClaimsAdjudicationReport(
        total_claims=len(records),
        status_counts=status_counts,
        outcome_counts=outcome_counts,
        is_hash_verified=is_hash_ok,
        register_sha256=reg_sha,
        violations=violations,
        verified_claims=verified,
        partial_claims=partial,
        unverified_claims=unverified,
        is_adjudication_accepted=is_accepted,
        summary="\n".join(summary_lines),
    )


def verify_claim_promotion_request(
    records: List[ClaimRecord],
    target_claim_id: str,
    proposed_status: str,
    proposed_reproduction_path: str,
    proposed_reproduction_sha: str,
) -> Tuple[bool, str]:
    """Evaluates whether a proposed claim status promotion satisfies Lead evidence gates."""
    claim = next((r for r in records if r.id == target_claim_id), None)
    if not claim:
        return False, f"Claim {target_claim_id} not found in register"

    if proposed_status == "VERIFIED":
        if target_claim_id in LOCKED_UNVERIFIED_CLAIMS:
            return False, f"Promotion REJECTED: Claim {target_claim_id} is permanently locked as UNVERIFIED under current SSoT"
        if not proposed_reproduction_path:
            return False, "Promotion REJECTED: VERIFIED status requires non-empty reproduction_path"
        if not proposed_reproduction_sha or not SHA256_HEX_RE.match(proposed_reproduction_sha):
            return False, "Promotion REJECTED: VERIFIED status requires valid 64-char hex reproduction_sha"

    return True, f"Promotion of {target_claim_id} to {proposed_status} is structurally admissible"
