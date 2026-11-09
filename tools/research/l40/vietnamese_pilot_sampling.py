"""
L40 Vietnamese Pilot Deterministic Sampling Frame Validator and Allocator.

Implements the frozen decision `iron-chef-l40-vietnamese-pilot-v1` (2026-11-06)
governed by Academic Vision and Strategy Section 10 and Campaign Protocol v1.

Core mathematical and protocol invariants:
1. Target N=100 split into two disjoint cohorts:
   - 60 culinary validity cases (North=20, Central=20, South=20).
   - 40 declared-allergen constraint cases (North=14, Central=13, South=13).
2. Exactly one case per source-recipe cluster across both cohorts (strict deduplication).
3. 7 named dish families (noodle_soup, rice, grilled_braised, roll, dipping_sauce,
   sweet_soup, sandwich) plus fallback (other, unknown). Fallback never fills named-family quota.
4. Named family floors: >= 5 per named family in culinary, >= 3 in constraint (where eligible records exist).
5. Sampling seed derived deterministically as big-endian uint64 from first 8 bytes of:
   SHA256(b"iron-chef-l40-sampling-v1\\0" + bytes.fromhex(frame_manifest_sha256)).
6. Deterministic least-filled region x family cell round-robin allocation, tie-breaking
   on seed-derived case rank and stable case_id.
7. Fail-closed shortfall handling: if cohort cannot meet total or regional targets,
   it stops and records an explicit shortfall receipt; named-family shortfalls are preserved.
8. Linguistic integrity: diacritics preserved in UTF-8 (NFC), paired checked English gloss required.
"""

from __future__ import annotations

import dataclasses
import hashlib
import unicodedata
from typing import Any, Dict, List, Optional, Set, Tuple


# Constants and Enumerations
SAMPLING_DOMAIN_PREFIX = b"iron-chef-l40-sampling-v1\0"

REGIONS: Tuple[str, ...] = ("north", "central", "south")
VALID_REGIONS_WITH_UNKNOWN: Tuple[str, ...] = REGIONS + ("unknown",)

NAMED_DISH_FAMILIES: Tuple[str, ...] = (
    "noodle_soup",
    "rice",
    "grilled_braised",
    "roll",
    "dipping_sauce",
    "sweet_soup",
    "sandwich",
)
FALLBACK_DISH_FAMILIES: Tuple[str, ...] = ("other", "unknown")
ALL_DISH_FAMILIES: Tuple[str, ...] = NAMED_DISH_FAMILIES + FALLBACK_DISH_FAMILIES

COHORTS: Tuple[str, ...] = ("culinary", "constraint")

# Target quotas
CULINARY_TARGETS = {
    "total": 60,
    "north": 20,
    "central": 20,
    "south": 20,
    "family_floor": 5,
}

CONSTRAINT_TARGETS = {
    "total": 40,
    "north": 14,
    "central": 13,
    "south": 13,
    "family_floor": 3,
}


def derive_sampling_seed(frame_manifest_sha256_hex: str) -> int:
    """
    Derives deterministic uint64 sampling seed from frame manifest SHA-256.

    seed = int.from_bytes(
        SHA256(b"iron-chef-l40-sampling-v1\\0" + bytes.fromhex(frame_manifest_sha256))[:8],
        byteorder="big"
    )
    """
    cleaned = frame_manifest_sha256_hex.strip().lower()
    if len(cleaned) != 64:
        raise ValueError(f"frame_manifest_sha256 must be 64 hex characters, got {len(cleaned)}")
    manifest_bytes = bytes.fromhex(cleaned)
    hasher = hashlib.sha256()
    hasher.update(SAMPLING_DOMAIN_PREFIX)
    hasher.update(manifest_bytes)
    digest = hasher.digest()
    return int.from_bytes(digest[:8], byteorder="big")


def compute_case_rank(seed: int, case_id: str) -> Tuple[int, str]:
    """
    Computes deterministic uniform rank for a case under the derived seed.
    Returns (rank_int64, case_id) for stable ordering.
    """
    seed_bytes = seed.to_bytes(8, byteorder="big")
    hasher = hashlib.sha256()
    hasher.update(seed_bytes)
    hasher.update(case_id.encode("utf-8"))
    digest = hasher.digest()
    rank = int.from_bytes(digest[:8], byteorder="big")
    return (rank, case_id)


@dataclasses.dataclass(frozen=True)
class EligibleCaseRecord:
    case_id: str
    source_recipe_id: str
    recipe_cluster_id: str
    target_slot_id: str
    cohort_id: str
    region: str
    dish_family: str
    vietnamese_title: str
    vietnamese_ingredient_list: List[str]
    vietnamese_target_slot: str
    vietnamese_context: str
    english_gloss: Dict[str, Any]
    rights_record_id: str
    rights_scope: str
    declared_constraint: Optional[str] = None
    allergen_source_relation: Optional[str] = None

    def validate(self) -> None:
        """Validates all fields against L40 protocol rules."""
        if not self.case_id or not self.case_id.strip():
            raise ValueError("case_id must be non-empty")
        if not self.source_recipe_id or not self.source_recipe_id.strip():
            raise ValueError(f"Case {self.case_id}: source_recipe_id must be non-empty")
        if not self.recipe_cluster_id or not self.recipe_cluster_id.strip():
            raise ValueError(f"Case {self.case_id}: recipe_cluster_id must be non-empty")
        if not self.target_slot_id or not self.target_slot_id.strip():
            raise ValueError(f"Case {self.case_id}: target_slot_id must be non-empty")

        if self.cohort_id not in COHORTS:
            raise ValueError(f"Case {self.case_id}: invalid cohort_id '{self.cohort_id}', must be in {COHORTS}")

        if self.region not in VALID_REGIONS_WITH_UNKNOWN:
            raise ValueError(f"Case {self.case_id}: invalid region '{self.region}', must be in {VALID_REGIONS_WITH_UNKNOWN}")

        if self.dish_family not in ALL_DISH_FAMILIES:
            raise ValueError(f"Case {self.case_id}: invalid dish_family '{self.dish_family}', must be in {ALL_DISH_FAMILIES}")

        # Text and diacritics validation
        for field_name, value in [
            ("vietnamese_title", self.vietnamese_title),
            ("vietnamese_target_slot", self.vietnamese_target_slot),
            ("vietnamese_context", self.vietnamese_context),
        ]:
            if not value or not value.strip():
                raise ValueError(f"Case {self.case_id}: {field_name} must be non-empty")
            # NFC normalization validation
            if unicodedata.normalize("NFC", value) != value:
                raise ValueError(f"Case {self.case_id}: {field_name} is not NFC normalized")

        if not self.vietnamese_ingredient_list or len(self.vietnamese_ingredient_list) == 0:
            raise ValueError(f"Case {self.case_id}: vietnamese_ingredient_list must be non-empty")

        # English gloss
        if not self.english_gloss or not isinstance(self.english_gloss, dict):
            raise ValueError(f"Case {self.case_id}: english_gloss must be a non-empty dictionary")

        # Rights record
        if not self.rights_record_id or not self.rights_record_id.strip():
            raise ValueError(f"Case {self.case_id}: rights_record_id must be non-empty")
        if not self.rights_scope or not self.rights_scope.strip():
            raise ValueError(f"Case {self.case_id}: rights_scope must be non-empty")

        # Constraint-specific fields
        if self.cohort_id == "constraint":
            if not self.declared_constraint or not self.declared_constraint.strip():
                raise ValueError(f"Constraint case {self.case_id}: declared_constraint must be specified")
            if not self.allergen_source_relation or not self.allergen_source_relation.strip():
                raise ValueError(f"Constraint case {self.case_id}: allergen_source_relation citation required")


@dataclasses.dataclass
class CohortAllocationResult:
    cohort_id: str
    target_count: int
    allocated_count: int
    is_success: bool
    status: str
    regional_counts: Dict[str, int]
    regional_targets: Dict[str, int]
    family_counts: Dict[str, int]
    family_floors: Dict[str, int]
    family_shortfalls: Dict[str, int]
    allocated_cases: List[EligibleCaseRecord]
    shortfall_reasons: List[str]


def allocate_cohort(
    records: List[EligibleCaseRecord],
    cohort_id: str,
    seed: int,
) -> CohortAllocationResult:
    """
    Executes deterministic least-filled region x family cell round-robin allocation.
    """
    if cohort_id == "culinary":
        targets = CULINARY_TARGETS
    elif cohort_id == "constraint":
        targets = CONSTRAINT_TARGETS
    else:
        raise ValueError(f"Unknown cohort {cohort_id}")

    target_total = targets["total"]
    reg_targets = {
        "north": targets["north"],
        "central": targets["central"],
        "south": targets["south"],
    }
    family_floor = targets["family_floor"]

    cohort_records = [r for r in records if r.cohort_id == cohort_id]

    # Group into cells: (region, dish_family)
    # Note: cases with region="unknown" cannot fill fixed regional targets
    cell_cases: Dict[Tuple[str, str], List[EligibleCaseRecord]] = {}
    for r in cohort_records:
        key = (r.region, r.dish_family)
        if key not in cell_cases:
            cell_cases[key] = []
        cell_cases[key].append(r)

    # Sort each cell's cases by seed-derived case rank
    for key in cell_cases:
        cell_cases[key].sort(key=lambda rec: compute_case_rank(seed, rec.case_id))

    allocated_in_cell: Dict[Tuple[str, str], List[EligibleCaseRecord]] = {
        key: [] for key in cell_cases
    }
    regional_allocated: Dict[str, int] = {"north": 0, "central": 0, "south": 0}
    family_allocated: Dict[str, int] = {f: 0 for f in ALL_DISH_FAMILIES}

    allocated_list: List[EligibleCaseRecord] = []
    available_indices: Dict[Tuple[str, str], int] = {key: 0 for key in cell_cases}

    shortfall_reasons: List[str] = []

    while len(allocated_list) < target_total:
        # Find candidate cells that:
        # 1. Have remaining cases
        # 2. Belong to a region that has not yet reached its regional target
        candidate_cells = [
            key
            for key, cases in cell_cases.items()
            if available_indices[key] < len(cases)
            and key[0] in reg_targets
            and regional_allocated[key[0]] < reg_targets[key[0]]
        ]

        if not candidate_cells:
            # Cannot proceed further: candidate cells exhausted before reaching target
            shortfall_reasons.append(
                f"Eligible records exhausted at {len(allocated_list)}/{target_total} cases."
            )
            break

        # Least-filled cell rule:
        # Find cells with minimum currently allocated count
        min_alloc = min(len(allocated_in_cell[c]) for c in candidate_cells)
        tied_cells = [c for c in candidate_cells if len(allocated_in_cell[c]) == min_alloc]

        # Tie-breaking hierarchy:
        # 1. Prioritize named families currently under the family floor
        def tie_break_key(cell: Tuple[str, str]):
            region, family = cell
            is_under_floor = 0 if (family in NAMED_DISH_FAMILIES and family_allocated[family] < family_floor) else 1
            region_order = REGIONS.index(region) if region in REGIONS else 99
            family_order = (
                NAMED_DISH_FAMILIES.index(family)
                if family in NAMED_DISH_FAMILIES
                else len(NAMED_DISH_FAMILIES) + FALLBACK_DISH_FAMILIES.index(family)
            )
            # Peek next case rank in that cell
            next_idx = available_indices[cell]
            next_case = cell_cases[cell][next_idx]
            rank_val, case_id = compute_case_rank(seed, next_case.case_id)
            return (is_under_floor, region_order, family_order, rank_val, case_id)

        chosen_cell = min(tied_cells, key=tie_break_key)

        # Allocate top case from chosen cell
        idx = available_indices[chosen_cell]
        case_to_add = cell_cases[chosen_cell][idx]
        available_indices[chosen_cell] += 1

        allocated_in_cell[chosen_cell].append(case_to_add)
        allocated_list.append(case_to_add)
        regional_allocated[chosen_cell[0]] += 1
        family_allocated[chosen_cell[1]] += 1

    # Check regional shortfalls
    for reg, target_val in reg_targets.items():
        if regional_allocated[reg] < target_val:
            shortfall_reasons.append(
                f"Regional shortfall in {reg}: attained {regional_allocated[reg]}/{target_val}."
            )

    # Check named family floors
    family_shortfalls: Dict[str, int] = {}
    family_floors: Dict[str, int] = {f: family_floor for f in NAMED_DISH_FAMILIES}
    for fam in NAMED_DISH_FAMILIES:
        actual = family_allocated[fam]
        if actual < family_floor:
            family_shortfalls[fam] = family_floor - actual
            shortfall_reasons.append(
                f"Named family floor shortfall for '{fam}': attained {actual}/{family_floor}."
            )

    is_success = (
        len(allocated_list) == target_total
        and all(regional_allocated[r] == reg_targets[r] for r in reg_targets)
    )
    status = "SUCCESS" if is_success else "SHORTFALL"

    return CohortAllocationResult(
        cohort_id=cohort_id,
        target_count=target_total,
        allocated_count=len(allocated_list),
        is_success=is_success,
        status=status,
        regional_counts=regional_allocated,
        regional_targets=reg_targets,
        family_counts=family_allocated,
        family_floors=family_floors,
        family_shortfalls=family_shortfalls,
        allocated_cases=allocated_list,
        shortfall_reasons=shortfall_reasons,
    )


@dataclasses.dataclass
class FullPilotSamplingResult:
    frame_manifest_sha256: str
    seed: int
    is_success: bool
    status: str
    culinary: CohortAllocationResult
    constraint: CohortAllocationResult
    total_allocated: int
    cluster_deduplication_passed: bool
    summary_report: Dict[str, Any]


def run_vietnamese_pilot_sampling(
    eligible_records: List[EligibleCaseRecord],
    frame_manifest_sha256: str,
) -> FullPilotSamplingResult:
    """
    Main entry point for deterministic L40 Vietnamese pilot sampling.
    Validates eligible records, ensures cluster deduplication across cohorts,
    derives the seed, and performs round-robin allocation for both cohorts.
    """
    # Step 1: Validate all records
    for r in eligible_records:
        r.validate()

    # Step 2: Check cross-cohort recipe cluster uniqueness
    seen_clusters: Dict[str, str] = {}
    cluster_dup_errors: List[str] = []
    for r in eligible_records:
        if r.recipe_cluster_id in seen_clusters:
            # Note: Multiple records for the same cluster in the input frame
            # are allowed in the candidate pool, BUT only one can be selected across cohorts.
            pass
        else:
            seen_clusters[r.recipe_cluster_id] = r.case_id

    # Step 3: Derive seed
    seed = derive_sampling_seed(frame_manifest_sha256)

    # Step 4: Run allocation
    # To guarantee strict cross-cohort cluster uniqueness (at most one case per cluster
    # across both cohorts), when a cluster is allocated in culinary, it is unavailable in constraint.
    # Per protocol, culinary cohort is allocated first, then constraint cohort.
    culinary_alloc = allocate_cohort(eligible_records, "culinary", seed)

    # Filter remaining eligible records for constraint cohort by removing allocated clusters
    used_clusters = {c.recipe_cluster_id for c in culinary_alloc.allocated_cases}
    eligible_for_constraint = [
        r for r in eligible_records
        if r.cohort_id == "constraint" and r.recipe_cluster_id not in used_clusters
    ]

    constraint_alloc = allocate_cohort(eligible_for_constraint, "constraint", seed)

    # Verify cluster deduplication across both allocations
    all_selected_clusters = [c.recipe_cluster_id for c in culinary_alloc.allocated_cases] + [
        c.recipe_cluster_id for c in constraint_alloc.allocated_cases
    ]
    cluster_dedup_passed = len(all_selected_clusters) == len(set(all_selected_clusters))

    total_allocated = culinary_alloc.allocated_count + constraint_alloc.allocated_count
    is_success = culinary_alloc.is_success and constraint_alloc.is_success and cluster_dedup_passed
    status = "SUCCESS" if is_success else "SHORTFALL"

    summary_report = {
        "status": status,
        "is_success": is_success,
        "frame_manifest_sha256": frame_manifest_sha256,
        "seed": seed,
        "total_target": 100,
        "total_allocated": total_allocated,
        "cluster_deduplication_passed": cluster_dedup_passed,
        "culinary": {
            "status": culinary_alloc.status,
            "target": culinary_alloc.target_count,
            "allocated": culinary_alloc.allocated_count,
            "regional_counts": culinary_alloc.regional_counts,
            "regional_targets": culinary_alloc.regional_targets,
            "family_counts": culinary_alloc.family_counts,
            "family_shortfalls": culinary_alloc.family_shortfalls,
            "shortfall_reasons": culinary_alloc.shortfall_reasons,
        },
        "constraint": {
            "status": constraint_alloc.status,
            "target": constraint_alloc.target_count,
            "allocated": constraint_alloc.allocated_count,
            "regional_counts": constraint_alloc.regional_counts,
            "regional_targets": constraint_alloc.regional_targets,
            "family_counts": constraint_alloc.family_counts,
            "family_shortfalls": constraint_alloc.family_shortfalls,
            "shortfall_reasons": constraint_alloc.shortfall_reasons,
        },
    }

    return FullPilotSamplingResult(
        frame_manifest_sha256=frame_manifest_sha256,
        seed=seed,
        is_success=is_success,
        status=status,
        culinary=culinary_alloc,
        constraint=constraint_alloc,
        total_allocated=total_allocated,
        cluster_deduplication_passed=cluster_dedup_passed,
        summary_report=summary_report,
    )
