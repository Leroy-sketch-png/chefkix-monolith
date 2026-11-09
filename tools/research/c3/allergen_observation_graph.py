"""
L39 / C3 Typed Source-Preserving Allergen Observation Graph Model and Admission Validator.

Implements the Lead-owned decision:
"L39/C3 allergen evidence construction and source-admission decision (2026-11-08)"
in BACKLOG_LEAD.md, governed by Academic Vision and Strategy Section 12
and Campaign Protocol v1 (SHA-256 b2952599099c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12).

Core Invariants:
1. Destination Boundary: Admitted source-backed allergen observations belong strictly in
   `graph_product_v4`, NEVER in `graph_eval_v4` (which remains train-derived substitution evidence only).
   Neither graph becomes a scoring input to the frozen 208-case benchmark.
2. Predicate Isolation (FATO guide):
   Distinguishes:
   - INGREDIENT_DECLARATION: explicit ingredient declaration in recipe or formulation.
   - DERIVATIVE: derived from allergen source (e.g., lecithin from soy).
   - ADDITIVE: allergen-source additive or processing aid.
   - IMPORTED_INGREDIENT_DECLARATION: declaration on imported raw ingredient.
   - CROSS_CONTACT: facility / shared equipment cross-contact risk statement.
   - PRECAUTIONARY_LABEL: "may contain" advisory statement.
   - PROTEIN_SOURCE: WHO/IUIS positive protein-to-source-taxon association.
3. Source Admission Boundaries:
   - FoodOn: Adopted ONLY as food/ingredient identity vocabulary and relation semantics
     (`has ingredient`, `has defining ingredient`). Links require version-pinned crosswalk, not name similarity.
   - WHO/IUIS: Retained as positive protein/source-species evidence ONLY. Never infer
     source-species -> ingredient -> allergen presence or clinical significance.
   - FooDB: REJECTED as allergen evidence (compound/nutrient co-presence is not allergen evidence).
   - Open Food Facts (OFF): Deferred from ingredient-level admission. Product allergen tags do NOT
     create ingredient-level edges.
4. Tri-State / Fail-Closed Observation States:
   - PRESENT: explicit positive observation at declared scope with authoritative source record.
   - UNKNOWN (Default): missing, unmatched, ambiguous, out-of-scope, or unrepresented relations.
   - ABSENT: ONLY if source declares a complete closed-world universe for exact entity set,
     jurisdiction, and time window. Never inferred from missing data.
5. Edge Metadata (§12): Every edge binds source, license, split, tier, and artifact_hash.
"""

from __future__ import annotations

import dataclasses
import enum
import hashlib
import json
from typing import Any, Dict, List, Optional, Set, Tuple


class PredicateType(str, enum.Enum):
    INGREDIENT_DECLARATION = "ingredient_declaration"
    DERIVATIVE = "derivative"
    ADDITIVE = "additive"
    IMPORTED_INGREDIENT_DECLARATION = "imported_ingredient_declaration"
    CROSS_CONTACT = "cross_contact"
    PRECAUTIONARY_LABEL = "precautionary_label"
    PROTEIN_SOURCE = "protein_source"


class ObservationState(str, enum.Enum):
    PRESENT = "PRESENT"
    UNKNOWN = "UNKNOWN"
    ABSENT = "ABSENT"


class ObservationScope(str, enum.Enum):
    INGREDIENT = "ingredient"
    PRODUCT = "product"
    BATCH = "batch"
    PROCESS = "process"
    SPECIES_PROTEIN = "species_protein"


class TargetGraphDestination(str, enum.Enum):
    GRAPH_PRODUCT_V4 = "graph_product_v4"
    GRAPH_EVAL_V4 = "graph_eval_v4"  # Strictly FORBIDDEN for allergen observations


@dataclasses.dataclass(frozen=True)
class AllergenObservationEdge:
    edge_id: str
    source_id: str
    release_version: str
    subject_id: str  # FoodOn URI or ChefKix canonical ID
    predicate_type: PredicateType
    object_id: str  # Allergen protein, taxon, or allergen group
    observation_state: ObservationState
    scope: ObservationScope
    geography_or_population: str
    effective_date: str
    license: str
    tier: str
    artifact_hash: str
    target_destination: TargetGraphDestination = TargetGraphDestination.GRAPH_PRODUCT_V4
    source_record_pointer: Optional[str] = None
    closed_world_universe_proof: Optional[str] = None

    def validate(self) -> None:
        """Enforces all L39/C3 source admission and structural invariants."""
        # Invariant 1: Destination boundary
        if self.target_destination != TargetGraphDestination.GRAPH_PRODUCT_V4:
            raise ValueError(
                f"Edge {self.edge_id}: Allergen observations are strictly forbidden from "
                f"destination '{self.target_destination.value}'. Must target 'graph_product_v4'."
            )

        # Invariant 2: FooDB rejection as allergen evidence
        if "foodb" in self.source_id.lower():
            raise ValueError(
                f"Edge {self.edge_id}: Source '{self.source_id}' is FooDB. FooDB compound/nutrient "
                "co-presence is strictly rejected as allergen evidence."
            )

        # Invariant 3: Open Food Facts ingredient-level deferral
        if "openfoodfacts" in self.source_id.lower() or "off" == self.source_id.lower():
            if self.scope == ObservationScope.INGREDIENT:
                raise ValueError(
                    f"Edge {self.edge_id}: Open Food Facts is deferred from ingredient-level admission. "
                    "Product allergen tags do not support ingredient-specific edges."
                )

        # Invariant 4: WHO/IUIS scope restriction
        if "who_iuis" in self.source_id.lower() or "iuis" in self.source_id.lower():
            if self.predicate_type != PredicateType.PROTEIN_SOURCE:
                raise ValueError(
                    f"Edge {self.edge_id}: WHO/IUIS source may only instantiate predicate "
                    f"PROTEIN_SOURCE, got '{self.predicate_type.value}'."
                )
            if self.scope != ObservationScope.SPECIES_PROTEIN:
                raise ValueError(
                    f"Edge {self.edge_id}: WHO/IUIS source must have scope 'species_protein', got '{self.scope.value}'."
                )

        # Invariant 5: ABSENT requires explicit closed-world universe proof
        if self.observation_state == ObservationState.ABSENT:
            if not self.closed_world_universe_proof or not self.closed_world_universe_proof.strip():
                raise ValueError(
                    f"Edge {self.edge_id}: State ABSENT is strictly forbidden without an explicit, "
                    "authoritative closed-world universe declaration proof."
                )

        # Required fields validation
        if not self.edge_id or not self.edge_id.strip():
            raise ValueError("edge_id must be non-empty")
        if not self.subject_id or not self.subject_id.strip():
            raise ValueError("subject_id must be non-empty")
        if not self.object_id or not self.object_id.strip():
            raise ValueError("object_id must be non-empty")
        if not self.artifact_hash or len(self.artifact_hash.strip()) < 8:
            raise ValueError(f"Edge {self.edge_id}: artifact_hash must be valid hash string")


class AllergenObservationGraph:
    """
    In-memory source-preserving typed allergen observation graph container.
    """

    def __init__(self, manifest_name: str = "graph_product_v4_allergen_observations"):
        self.manifest_name = manifest_name
        self._edges: Dict[str, AllergenObservationEdge] = {}
        # Multi-index: (subject_id, object_id) -> list of edge_ids
        self._subject_object_index: Dict[Tuple[str, str], List[str]] = {}

    def add_edge(self, edge: AllergenObservationEdge) -> None:
        """Validates and adds an observation edge to the graph."""
        edge.validate()
        if edge.edge_id in self._edges:
            raise ValueError(f"Duplicate edge_id: {edge.edge_id}")
        self._edges[edge.edge_id] = edge

        idx_key = (edge.subject_id, edge.object_id)
        if idx_key not in self._subject_object_index:
            self._subject_object_index[idx_key] = []
        self._subject_object_index[idx_key].append(edge.edge_id)

    def query_relation_state(
        self,
        subject_id: str,
        object_id: str,
        predicate: Optional[PredicateType] = None,
    ) -> ObservationState:
        """
        Queries relationship state under fail-closed tri-state rules.
        - If any matching edge has state PRESENT -> returns PRESENT.
        - If matching edge has state ABSENT with closed-world proof -> returns ABSENT.
        - Otherwise (missing, ambiguous, unrepresented) -> returns UNKNOWN.
        """
        idx_key = (subject_id, object_id)
        edge_ids = self._subject_object_index.get(idx_key, [])
        if not edge_ids:
            return ObservationState.UNKNOWN

        matching_edges = [self._edges[eid] for eid in edge_ids]
        if predicate is not None:
            matching_edges = [e for e in matching_edges if e.predicate_type == predicate]

        if not matching_edges:
            return ObservationState.UNKNOWN

        # Positive observation check
        if any(e.observation_state == ObservationState.PRESENT for e in matching_edges):
            return ObservationState.PRESENT

        # Explicit closed-world absence check
        if any(e.observation_state == ObservationState.ABSENT for e in matching_edges):
            return ObservationState.ABSENT

        return ObservationState.UNKNOWN

    def compute_manifest_digest(self) -> str:
        """Computes deterministic SHA-256 digest over sorted edge serializations."""
        hasher = hashlib.sha256()
        sorted_edge_ids = sorted(self._edges.keys())
        for eid in sorted_edge_ids:
            edge = self._edges[eid]
            line = (
                f"{edge.edge_id}\t{edge.source_id}\t{edge.subject_id}\t"
                f"{edge.predicate_type.value}\t{edge.object_id}\t"
                f"{edge.observation_state.value}\t{edge.scope.value}\t"
                f"{edge.artifact_hash}\n"
            )
            hasher.update(line.encode("utf-8"))
        return hasher.hexdigest()

    def get_summary(self) -> Dict[str, Any]:
        """Returns structural summary of admitted observations."""
        pred_counts: Dict[str, int] = {}
        state_counts: Dict[str, int] = {}
        for e in self._edges.values():
            p_val = e.predicate_type.value
            s_val = e.observation_state.value
            pred_counts[p_val] = pred_counts.get(p_val, 0) + 1
            state_counts[s_val] = state_counts.get(s_val, 0) + 1

        return {
            "manifest_name": self.manifest_name,
            "target_destination": TargetGraphDestination.GRAPH_PRODUCT_V4.value,
            "total_edges": len(self._edges),
            "manifest_sha256": self.compute_manifest_digest(),
            "predicate_distribution": pred_counts,
            "observation_state_distribution": state_counts,
        }
