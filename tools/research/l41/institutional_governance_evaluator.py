"""L41 Institutional, Permission, and Data Source Governance Evaluator.

Enforces the authoritative HUMAN standing authority boundary, attestation
classification, operation scoping, and fail-closed permission gates under
Protocol v1 and BACKLOG_LEAD.md L41.
"""
from __future__ import annotations

import dataclasses
from enum import Enum
from typing import Any, Sequence


class OperationScope(str, Enum):
    INTERNAL_ARTIFACT_READ = "INTERNAL_ARTIFACT_READ"
    INTERNAL_CONFIDENTIAL_STORE = "INTERNAL_CONFIDENTIAL_STORE"
    EXTERNAL_PUBLICATION = "EXTERNAL_PUBLICATION"
    EXTERNAL_REDISTRIBUTION = "EXTERNAL_REDISTRIBUTION"
    EXTERNAL_SPENDING = "EXTERNAL_SPENDING"
    DESTRUCTIVE_ACTION = "DESTRUCTIVE_ACTION"
    HUMAN_PARTICIPANT_CONTACT = "HUMAN_PARTICIPANT_CONTACT"
    RIGHTS_TRANSFER = "RIGHTS_TRANSFER"


class EpistemicAttestationStatus(str, Enum):
    ATTESTED_HUMAN_DECLARATION = "ATTESTED_HUMAN_DECLARATION"
    INDEPENDENTLY_DOCUMENTARY_VERIFIED = "INDEPENDENTLY_DOCUMENTARY_VERIFIED"
    UNVERIFIED_OR_MISSING = "UNVERIFIED_OR_MISSING"


class GateDisposition(str, Enum):
    PERMITTED_STANDING_AUTHORITY = "PERMITTED_STANDING_AUTHORITY"
    PROHIBITED_CONFIDENTIAL_STORE = "PROHIBITED_CONFIDENTIAL_STORE"
    BLOCKED_REQUIRING_EXPLICIT_AUTHORIZATION = "BLOCKED_REQUIRING_EXPLICIT_AUTHORIZATION"
    BLOCKED_REQUIRING_DOCUMENTARY_CLEARANCE = "BLOCKED_REQUIRING_DOCUMENTARY_CLEARANCE"


@dataclasses.dataclass(frozen=True)
class OperationEvaluation:
    operation: OperationScope
    disposition: GateDisposition
    allowed: bool
    rationale: str


@dataclasses.dataclass(frozen=True)
class SourceLicenseRecord:
    source_name: str
    license_declared: str
    permitted_internal_research: bool
    permitted_public_redistribution: bool
    commercial_use_permitted: bool
    clinical_safety_established: bool
    notes: str


@dataclasses.dataclass(frozen=True)
class GovernanceAuditResult:
    standing_authority_active: bool
    ethics_attestation_status: EpistemicAttestationStatus
    evaluated_operations: tuple[OperationEvaluation, ...]
    source_records: tuple[SourceLicenseRecord, ...]
    all_gates_pass: bool
    lead_status: str
    warnings: tuple[str, ...]


KNOWN_SOURCES: tuple[SourceLicenseRecord, ...] = (
    SourceLicenseRecord(
        source_name="Recipe1M / Recipe1MSubs",
        license_declared="Non-commercial research and educational use only",
        permitted_internal_research=True,
        permitted_public_redistribution=False,
        commercial_use_permitted=False,
        clinical_safety_established=False,
        notes="Authorized by HUMAN for private Kaggle/local execution only; no public redistribution.",
    ),
    SourceLicenseRecord(
        source_name="FooDB 1.0",
        license_declared="CC BY-NC 4.0",
        permitted_internal_research=True,
        permitted_public_redistribution=False,
        commercial_use_permitted=False,
        clinical_safety_established=False,
        notes="Non-commercial attribution; private compound graph construction allowed; commercial use requires separate permission.",
    ),
    SourceLicenseRecord(
        source_name="WHO/IUIS Allergen Nomenclature",
        license_declared="Public domain scientific nomenclature",
        permitted_internal_research=True,
        permitted_public_redistribution=True,
        commercial_use_permitted=True,
        clinical_safety_established=False,
        notes="Systematic protein nomenclature only; explicitly does NOT establish clinical significance or allergen absence.",
    ),
)


def evaluate_operation(
    operation: OperationScope,
    has_explicit_user_action_auth: bool = False,
    is_confidential_store: bool = False,
) -> OperationEvaluation:
    """Evaluate an operation against the L41 standing authority and permission boundary."""
    if is_confidential_store or operation == OperationScope.INTERNAL_CONFIDENTIAL_STORE:
        return OperationEvaluation(
            operation=operation,
            disposition=GateDisposition.PROHIBITED_CONFIDENTIAL_STORE,
            allowed=False,
            rationale="Agents are strictly prohibited from probing or searching confidential internal stores.",
        )

    if operation == OperationScope.INTERNAL_ARTIFACT_READ:
        return OperationEvaluation(
            operation=operation,
            disposition=GateDisposition.PERMITTED_STANDING_AUTHORITY,
            allowed=True,
            rationale="HUMAN grants standing authority to read/use already-accessible project artifacts for this mission.",
        )

    if operation in (
        OperationScope.EXTERNAL_PUBLICATION,
        OperationScope.EXTERNAL_REDISTRIBUTION,
        OperationScope.EXTERNAL_SPENDING,
        OperationScope.DESTRUCTIVE_ACTION,
        OperationScope.RIGHTS_TRANSFER,
    ):
        if has_explicit_user_action_auth:
            return OperationEvaluation(
                operation=operation,
                disposition=GateDisposition.PERMITTED_STANDING_AUTHORITY,
                allowed=True,
                rationale="Explicit per-action authorization provided by HUMAN.",
            )
        return OperationEvaluation(
            operation=operation,
            disposition=GateDisposition.BLOCKED_REQUIRING_EXPLICIT_AUTHORIZATION,
            allowed=False,
            rationale=f"{operation.value} is separately scoped and cannot be inferred from standing read authority.",
        )

    if operation == OperationScope.HUMAN_PARTICIPANT_CONTACT:
        return OperationEvaluation(
            operation=operation,
            disposition=GateDisposition.BLOCKED_REQUIRING_DOCUMENTARY_CLEARANCE,
            allowed=False,
            rationale="Actual participant contact requires verified documentary IRB/ethics approval and consent verification.",
        )

    return OperationEvaluation(
        operation=operation,
        disposition=GateDisposition.BLOCKED_REQUIRING_EXPLICIT_AUTHORIZATION,
        allowed=False,
        rationale="Unknown operation scope fails closed.",
    )


def audit_governance_posture(
    human_attestation_present: bool = True,
    independent_document_inspected: bool = False,
    planned_operations: Sequence[OperationScope] = (),
    explicit_authorizations: Sequence[OperationScope] = (),
) -> GovernanceAuditResult:
    """Audit the complete L41 governance posture."""
    if independent_document_inspected:
        attestation_status = EpistemicAttestationStatus.INDEPENDENTLY_DOCUMENTARY_VERIFIED
    elif human_attestation_present:
        attestation_status = EpistemicAttestationStatus.ATTESTED_HUMAN_DECLARATION
    else:
        attestation_status = EpistemicAttestationStatus.UNVERIFIED_OR_MISSING

    evals = []
    warnings = []

    # Default evaluation across standard scopes if none provided
    ops_to_eval = list(planned_operations) if planned_operations else list(OperationScope)

    for op in ops_to_eval:
        has_auth = op in explicit_authorizations
        res = evaluate_operation(op, has_explicit_user_action_auth=has_auth)
        evals.append(res)
        if not res.allowed and op in (OperationScope.EXTERNAL_PUBLICATION, OperationScope.EXTERNAL_SPENDING):
            warnings.append(f"{op.value} is currently blocked pending explicit authorization.")

    if attestation_status == EpistemicAttestationStatus.ATTESTED_HUMAN_DECLARATION:
        warnings.append(
            "Ethics/consent is recorded as HUMAN attestation, NOT independently documentary verified."
        )

    all_pass = all(e.allowed for e in evals if e.operation == OperationScope.INTERNAL_ARTIFACT_READ)

    return GovernanceAuditResult(
        standing_authority_active=True,
        ethics_attestation_status=attestation_status,
        evaluated_operations=tuple(evals),
        source_records=KNOWN_SOURCES,
        all_gates_pass=all_pass,
        lead_status="OPEN",
        warnings=tuple(warnings),
    )
