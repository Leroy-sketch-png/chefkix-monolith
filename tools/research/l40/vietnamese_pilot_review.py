"""Prepare separate blinded L40 reviewer payloads and a restricted identity key."""

import hashlib
import hmac
import json
import re
from dataclasses import dataclass
from typing import Mapping, Sequence

from .vietnamese_pilot_sampling import CONSTRAINT_TARGETS, CULINARY_TARGETS, REGIONS, FullPilotSamplingResult


@dataclass(frozen=True)
class ReviewCandidate:
    identity_id: str
    display_names: tuple[str, ...]
    extra_recorded_gold: bool = False


def canonical_bytes(payload: dict) -> bytes:
    return json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def build_reviewer_payloads(
    sample: FullPilotSamplingResult,
    cohort_id: str,
    slates: Mapping[str, Sequence[ReviewCandidate]],
    blind_secret: bytes,
) -> tuple[dict[str, dict], dict]:
    """Project a completed frozen cohort into R1/R2 payloads and a private join key.

    The caller owns source eligibility, slate identity/deduplication, private key
    storage, and the real study's approved reviewer assignments. An empty slate
    preserves the selected case; its underlying failure record stays private.
    """
    cohorts = {
        "culinary": (sample.culinary, CULINARY_TARGETS),
        "constraint": (sample.constraint, CONSTRAINT_TARGETS),
    }
    if cohort_id not in cohorts:
        raise ValueError("Unknown L40 cohort")
    cohort, target = cohorts[cohort_id]
    if (
        not cohort.is_success
        or len(cohort.allocated_cases) != target["total"]
        or any(cohort.regional_counts.get(region) != target[region] for region in REGIONS)
    ):
        raise ValueError("A cohort must meet its fixed allocation before issuing reviewer payloads")
    frame_manifest_sha256 = sample.frame_manifest_sha256
    if not re.fullmatch(r"[0-9a-fA-F]{64}", frame_manifest_sha256) or len(blind_secret) < 32:
        raise ValueError("Expected a SHA-256 frame pin and a private secret of at least 32 bytes")

    cases = {case.case_id: case for case in cohort.allocated_cases}
    if (
        len(cases) != target["total"] or set(cases) != set(slates)
        or len({case.recipe_cluster_id for case in cases.values()}) != target["total"]
        or any(case.cohort_id != cohort.cohort_id for case in cases.values())
    ):
        raise ValueError("Slate keys and cohort must match unique selected cases and recipe clusters")
    for candidates in slates.values():
        ids = {candidate.identity_id for candidate in candidates}
        extra_gold_count = sum(candidate.extra_recorded_gold for candidate in candidates)
        if (
            extra_gold_count > 1 or len(candidates) - extra_gold_count > 5
            or len(ids) != len(candidates)
            or any(
                not candidate.identity_id.strip() or not candidate.display_names
                or any(not name.strip() for name in candidate.display_names)
                for candidate in candidates
            )
        ):
            raise ValueError("Expected up to five deduplicated model candidates and one optional gold")

    def opaque(*parts: str) -> str:
        material = canonical_bytes({
            "format": "l40-review-payload-v1", "frame": frame_manifest_sha256.lower(),
            "cohort": cohort.cohort_id, "parts": parts,
        })
        return hmac.digest(blind_secret, material, "sha256").hex()

    packs = {}
    private_entries = []
    for reviewer in ("R1", "R2"):
        views = []
        for case_id in sorted(cases, key=lambda case_id: opaque(reviewer, "view", case_id)):
            case = cases[case_id]
            view_id = opaque(reviewer, "view", case_id)
            candidates = []
            for candidate in slates[case_id]:
                token = opaque(reviewer, "candidate", case_id, candidate.identity_id)
                candidates.append({"blind_candidate": token, "display_names": list(candidate.display_names)})
                private_entries.append({
                    "reviewer": reviewer, "view_id": view_id, "case_id": case_id,
                    "blind_candidate": token, "identity_id": candidate.identity_id,
                })
            if not candidates:
                private_entries.append({"reviewer": reviewer, "view_id": view_id, "case_id": case_id})
            views.append({
                "view_id": view_id, "recipe_title": case.vietnamese_title,
                "ingredients": list(case.vietnamese_ingredient_list), "target": case.vietnamese_target_slot,
                "context": case.vietnamese_context, "declared_constraint": case.declared_constraint,
                "candidates": sorted(candidates, key=lambda candidate: candidate["blind_candidate"]),
            })

        payload = {"reviewer": reviewer, "label_codes": ["A", "C", "N", "U"], "views": views}
        packs[reviewer] = {**payload, "pack_id": hashlib.sha256(canonical_bytes(payload)).hexdigest()}

    restricted_key = {
        "format": "l40-private-key-v1", "frame_manifest_sha256": frame_manifest_sha256.lower(),
        "cohort": cohort.cohort_id, "pack_ids": {reviewer: pack["pack_id"] for reviewer, pack in packs.items()},
        "entries": sorted(private_entries, key=lambda row: (row["reviewer"], row["view_id"], row.get("blind_candidate", ""))),
    }
    return packs, restricted_key
