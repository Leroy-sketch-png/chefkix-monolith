"""C5 Section 12 FooDB-Native Chemical Product Graph Evaluator.

Enforces the authoritative LEAD contract for the graph_product_v4 FooDB-native
subgraph under BACKLOG_LEAD.md §12. Strictly distinguishes native Food->Compound
relations from normalized profile projections, enforces typed Content joins,
differentiates publication export eligibility from relation presence, and
guarantees fail-closed tri-state resolution.
"""
from __future__ import annotations

import dataclasses
import re
from enum import Enum
from typing import Any, Mapping, Sequence, Set

PINNED_FOODB_COMPOUNDS_JSON_SHA256 = "8490cd6adadc5bad0e59f5f4edd8d05020b941b987ca47e38ac0cc80d85a04ac"
PINNED_FOODB_SIDECAR_SHA256 = "5117b791bd825b5c7c87f4f816342ff4bc65100b74502a8652157a989ed9d208"
PINNED_FOODB_OFFICIAL_ARCHIVE_SHA256 = "3a37305b2a46d7fbd41969bdc5c3f9233bd837cde7c6f46e34316d08879a999c"

TOTAL_UNIQUE_PAIRS = 125992
EXPORT_ELIGIBLE_PAIRS = 107496
NOT_EXPORT_ELIGIBLE_PAIRS = 18496
TOP_255_ARTIFACT_COMPOUND_COUNT = 206
EXCLUDED_DANGLING_IDS = 49
EXCLUDED_FOOD_RECORDS_WITHOUT_COMPOUNDS = 4

FOOD_NODE_PATTERN = re.compile(r"^foodb:1\.0:food-id:(\d+)$")
COMPOUND_NODE_PATTERN = re.compile(r"^foodb:1\.0:compound-id:(\d+)$")


class PublicationFilterState(str, Enum):
    EXPORT_ELIGIBLE = "EXPORT_ELIGIBLE"
    PRESENT_NOT_EXPORT_ELIGIBLE = "PRESENT_NOT_EXPORT_ELIGIBLE"
    UNKNOWN = "UNKNOWN"


class TriStatePresence(str, Enum):
    PRESENT = "PRESENT"
    ABSENT = "ABSENT"
    UNKNOWN = "UNKNOWN"


@dataclasses.dataclass(frozen=True)
class NativeFoodCompoundEdge:
    food_id: int
    compound_id: int
    food_node_id: str
    compound_node_id: str
    content_row_support: int
    export_filter_state: PublicationFilterState
    presence: TriStatePresence
    source_type: str = "Compound"


@dataclasses.dataclass(frozen=True)
class FooDBNativeGraphEvaluationResult:
    destination_graph: str
    total_unique_pairs: int
    export_eligible_count: int
    not_export_eligible_count: int
    top_255_compound_count: int
    dangling_ids_excluded: int
    food_records_without_compounds_excluded: int
    all_predicates_valid: bool
    lead_status: str
    violations: tuple[str, ...]


def validate_node_ids(food_node_id: str, compound_node_id: str) -> tuple[int, int]:
    """Validate base-10 native node ID formatting."""
    m_food = FOOD_NODE_PATTERN.match(food_node_id)
    if not m_food:
        raise ValueError(f"Invalid food node ID format: {food_node_id}")
    m_compound = COMPOUND_NODE_PATTERN.match(compound_node_id)
    if not m_compound:
        raise ValueError(f"Invalid compound node ID format: {compound_node_id}")
    return int(m_food.group(1)), int(m_compound.group(1))


def create_native_edge(
    food_id: int,
    compound_id: int,
    content_row_count: int,
    is_export: bool,
    is_public_food: bool,
    closed_world_complete: bool = False,
) -> NativeFoodCompoundEdge:
    """Create and validate a native Food->Compound edge."""
    if content_row_count <= 0:
        presence = TriStatePresence.ABSENT if closed_world_complete else TriStatePresence.UNKNOWN
        export_state = PublicationFilterState.UNKNOWN
    else:
        presence = TriStatePresence.PRESENT
        if is_export and is_public_food:
            export_state = PublicationFilterState.EXPORT_ELIGIBLE
        else:
            export_state = PublicationFilterState.PRESENT_NOT_EXPORT_ELIGIBLE

    return NativeFoodCompoundEdge(
        food_id=food_id,
        compound_id=compound_id,
        food_node_id=f"foodb:1.0:food-id:{food_id}",
        compound_node_id=f"foodb:1.0:compound-id:{compound_id}",
        content_row_support=content_row_count,
        export_filter_state=export_state,
        presence=presence,
        source_type="Compound",
    )


def audit_foodb_native_graph(
    destination: str,
    unique_pairs_count: int,
    export_eligible_count: int,
    not_export_eligible_count: int,
    artifact_compound_count: int,
    dangling_ids_count: int,
    empty_foods_count: int,
    joined_by_normalized_name: bool = False,
    inferred_absence_from_omission: bool = False,
    inferred_allergen_from_compound: bool = False,
) -> FooDBNativeGraphEvaluationResult:
    """Audit the FooDB native product graph against LEAD §12 invariants."""
    violations = []

    # Invariant 1: Destination must be graph_product_v4, never graph_eval_v4
    if destination != "graph_product_v4":
        violations.append(
            f"Destination graph must be 'graph_product_v4', got '{destination}'. "
            "Never add product chemistry to frozen train-derived graph_eval_v4."
        )

    # Invariant 2: Exactly 125,992 unique pairs
    if unique_pairs_count != TOTAL_UNIQUE_PAIRS:
        violations.append(
            f"Unique pair count mismatch: expected {TOTAL_UNIQUE_PAIRS}, got {unique_pairs_count}."
        )

    # Invariant 3: Export eligibility partition (107,496 + 18,496 = 125,992)
    if export_eligible_count != EXPORT_ELIGIBLE_PAIRS:
        violations.append(
            f"Export eligible pair count mismatch: expected {EXPORT_ELIGIBLE_PAIRS}, got {export_eligible_count}."
        )
    if not_export_eligible_count != NOT_EXPORT_ELIGIBLE_PAIRS:
        violations.append(
            f"Non-export pair count mismatch: expected {NOT_EXPORT_ELIGIBLE_PAIRS}, got {not_export_eligible_count}."
        )
    if export_eligible_count + not_export_eligible_count != unique_pairs_count:
        violations.append(
            "Export partition does not sum to total unique pairs."
        )

    # Invariant 4: Top-255 scope
    if artifact_compound_count != TOP_255_ARTIFACT_COMPOUND_COUNT:
        violations.append(
            f"Artifact compound count mismatch: expected {TOP_255_ARTIFACT_COMPOUND_COUNT}, got {artifact_compound_count}."
        )
    if dangling_ids_count != EXCLUDED_DANGLING_IDS:
        violations.append(
            f"Dangling IDs excluded count mismatch: expected {EXCLUDED_DANGLING_IDS}, got {dangling_ids_count}."
        )
    if empty_foods_count != EXCLUDED_FOOD_RECORDS_WITHOUT_COMPOUNDS:
        violations.append(
            f"Foods without compounds count mismatch: expected {EXCLUDED_FOOD_RECORDS_WITHOUT_COMPOUNDS}, got {empty_foods_count}."
        )

    # Invariant 5: No joining by normalized name
    if joined_by_normalized_name:
        violations.append(
            "Edges must NOT be derived by expanding/grouping normalized profile names. "
            "Edges require exact typed Content.food_id -> Food.id and Content.source_id -> Compound.id."
        )

    # Invariant 6: No inferring absence from omission
    if inferred_absence_from_omission:
        violations.append(
            "Missing profile/dimension means UNKNOWN, never ABSENCE without closed-world proof."
        )

    # Invariant 7: No inferring allergen from compound
    if inferred_allergen_from_compound:
        violations.append(
            "Allergen edges require separate authoritative allergen sources. "
            "Compound co-presence CANNOT imply allergens."
        )

    all_valid = len(violations) == 0

    return FooDBNativeGraphEvaluationResult(
        destination_graph=destination,
        total_unique_pairs=unique_pairs_count,
        export_eligible_count=export_eligible_count,
        not_export_eligible_count=not_export_eligible_count,
        top_255_compound_count=artifact_compound_count,
        dangling_ids_excluded=dangling_ids_count,
        food_records_without_compounds_excluded=empty_foods_count,
        all_predicates_valid=all_valid,
        lead_status="OPEN",
        violations=tuple(violations),
    )
