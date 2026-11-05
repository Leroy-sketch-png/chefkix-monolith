#!/usr/bin/env python3
"""Reproduce Claim R19: Frozen 208-case/13-group allergen protocol specification.

Verifies the frozen protocol specification file:
  chefkix-ai-service/models/allergen_benchmark_protocol_v2.json
confirming its SHA-256 digest, schema version, query count (208), unique case families (208),
and 13 allergen groups with non-empty prompts and valid prompt hashes.
"""

from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path

EXPECTED_PROTOCOL_SHA256 = "0e97c829cccfd6b43bf1dd974db9ae54dd9b561d037ebbca18eb4fbd2bc14fc9"
EXPECTED_GROUPS = [
    "tree_nuts", "peanuts", "milk", "eggs", "wheat", "fish",
    "shellfish", "soybeans", "sesame", "mustard", "celery",
    "sulphites", "lupin"
]


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(65536), b""):
            digest.update(chunk)
    return digest.hexdigest()


def reproduce_r19(repo_root: Path) -> dict:
    protocol_path = repo_root / "chefkix-ai-service" / "models" / "allergen_benchmark_protocol_v2.json"
    if not protocol_path.is_file():
        raise FileNotFoundError(f"Missing protocol file: {protocol_path}")

    actual_hash = sha256_file(protocol_path)
    if actual_hash != EXPECTED_PROTOCOL_SHA256:
        raise ValueError(f"Hash mismatch: expected {EXPECTED_PROTOCOL_SHA256}, got {actual_hash}")

    data = json.loads(protocol_path.read_text(encoding="utf-8"))

    if data.get("schema_version") != 2:
        raise ValueError(f"Unexpected schema_version: {data.get('schema_version')}")
    if data.get("protocol_id") != "chefkix-allergen-substitution-v2":
        raise ValueError(f"Unexpected protocol_id: {data.get('protocol_id')}")
    if data.get("state") != "FROZEN_READY_FOR_RAW_EXECUTION_REVIEW_PENDING":
        raise ValueError(f"Unexpected state: {data.get('state')}")

    queries = data.get("queries", [])
    if len(queries) != 208:
        raise ValueError(f"Expected 208 queries, got {len(queries)}")
    if data.get("n_queries") != 208:
        raise ValueError(f"n_queries field mismatch: {data.get('n_queries')}")
    if data.get("n_unique_case_families") != 208:
        raise ValueError(f"n_unique_case_families field mismatch: {data.get('n_unique_case_families')}")

    declared_groups = data.get("groups", [])
    if sorted(declared_groups) != sorted(EXPECTED_GROUPS):
        raise ValueError(f"Groups mismatch: expected {EXPECTED_GROUPS}, got {declared_groups}")

    group_counts: dict[str, int] = {}
    for q in queries:
        qid = q.get("query_id")
        prompt = q.get("prompt", "")
        prompt_sha = q.get("prompt_sha256", "")
        groups = q.get("declared_allergen_groups", [])

        if not qid or not prompt or not prompt_sha or not groups:
            raise ValueError(f"Query {qid} has missing required fields")

        computed_prompt_sha = hashlib.sha256(prompt.encode("utf-8")).hexdigest()
        if computed_prompt_sha != prompt_sha:
            raise ValueError(f"Query {qid} prompt hash mismatch: {computed_prompt_sha} vs {prompt_sha}")

        for g in groups:
            if g not in EXPECTED_GROUPS:
                raise ValueError(f"Query {qid} has unknown group {g}")
            group_counts[g] = group_counts.get(g, 0) + 1

    result = {
        "schema_version": 1,
        "record_id": "r19-benchmark-protocol-reproduction-20261102",
        "claim_id": "R19",
        "claim_text": "Frozen 208-case/13-group protocol",
        "protocol_file": "chefkix-ai-service/models/allergen_benchmark_protocol_v2.json",
        "protocol_sha256": actual_hash,
        "n_queries": len(queries),
        "n_groups": len(declared_groups),
        "groups": declared_groups,
        "group_occurrences": dict(sorted(group_counts.items())),
        "missing_fields_count": 0,
        "status": "PASS",
        "reproduced_value": (
            f"208 queries; 13 groups: celery ({group_counts.get('celery', 0)}), "
            f"eggs ({group_counts.get('eggs', 0)}), fish ({group_counts.get('fish', 0)}), "
            f"lupin ({group_counts.get('lupin', 0)}), milk ({group_counts.get('milk', 0)}), "
            f"mustard ({group_counts.get('mustard', 0)}), peanuts ({group_counts.get('peanuts', 0)}), "
            f"sesame ({group_counts.get('sesame', 0)}), shellfish ({group_counts.get('shellfish', 0)}), "
            f"soybeans ({group_counts.get('soybeans', 0)}), sulphites ({group_counts.get('sulphites', 0)}), "
            f"tree_nuts ({group_counts.get('tree_nuts', 0)}), wheat ({group_counts.get('wheat', 0)}); "
            f"0 missing fields; SHA-256 {actual_hash}."
        )
    }
    return result


def main():
    repo_root = Path(__file__).resolve().parent.parent.parent
    result = reproduce_r19(repo_root)
    out_path = repo_root / "ai" / "evidence" / "claims-register" / "instantiated-r01-r45-v1" / "r19-benchmark-protocol-reproduction-20261102.json"
    out_path.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(f"Reproduced R19: {result['reproduced_value']}")
    print(f"Wrote receipt to {out_path}")


if __name__ == "__main__":
    main()
