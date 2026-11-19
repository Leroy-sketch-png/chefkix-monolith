#!/usr/bin/env python3
"""Reproduce the narrow R13 FooDB profile-pair arithmetic contract.

This checks only shared-name set intersection and Jaccard arithmetic against
the installed, hash-pinned official FooDB profile artifact. It is not evidence
of ingredient similarity, substitution usefulness, safety, or quality.
"""

from __future__ import annotations

import argparse
import hashlib
import importlib
import itertools
import json
import math
import os
import platform
import sys
import uuid
from pathlib import Path
from typing import Any, Callable


EXPECTED_PROFILE_SHA256 = "8490cd6adadc5bad0e59f5f4edd8d05020b941b987ca47e38ac0cc80d85a04ac"
EXPECTED_PROVENANCE_SHA256 = "5117b791bd825b5c7c87f4f816342ff4bc65100b74502a8652157a989ed9d208"
EXPECTED_SERVICE_SHA256 = "e2bcad3cd81e196490aaa6bcc4f014721e9cb9e69734351520fabedef7463e3a"
EXPECTED_PROFILE_COUNT = 971
EXPECTED_PAIR_COUNT = 471_906


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _require_hash(path: Path, expected: str, label: str) -> str:
    if not path.is_file():
        raise ValueError(f"{label} is missing: {path}")
    actual = sha256(path)
    if actual != expected:
        raise ValueError(f"{label} SHA-256 mismatch: expected {expected}, got {actual}")
    return actual


def validate_profiles(profile_data: dict[str, Any]) -> list[str]:
    if not isinstance(profile_data, dict) or not profile_data:
        raise ValueError("profile data must be a non-empty object")
    normalized_to_original: dict[str, str] = {}
    for original, rows in profile_data.items():
        if not isinstance(original, str) or not original.strip():
            raise ValueError("profile contains an empty or non-string identity")
        key = original.lower().strip()
        if key in normalized_to_original:
            raise ValueError(f"duplicate normalized profile identity: {key!r}")
        normalized_to_original[key] = original
        if not isinstance(rows, list) or not rows:
            raise ValueError(f"profile {original!r} has no compound rows")
        names: list[str] = []
        for row in rows:
            if not isinstance(row, dict) or not isinstance(row.get("name"), str) or not row["name"].strip():
                raise ValueError(f"profile {original!r} has invalid compound metadata")
            names.append(row["name"].lower())
        if len(set(names)) != len(names):
            raise ValueError(f"profile {original!r} has duplicate compound names")
    return [normalized_to_original[key] for key in sorted(normalized_to_original)]


def load_inputs(profile_path: Path, provenance_path: Path, service_path: Path) -> tuple[dict[str, Any], dict[str, Any], dict[str, str]]:
    hashes = {
        "profile_sha256": _require_hash(profile_path, EXPECTED_PROFILE_SHA256, "profile artifact"),
        "provenance_sha256": _require_hash(provenance_path, EXPECTED_PROVENANCE_SHA256, "provenance sidecar"),
        "service_sha256": _require_hash(service_path, EXPECTED_SERVICE_SHA256, "service source"),
    }
    provenance = json.loads(provenance_path.read_text(encoding="utf-8"))
    profile_data = json.loads(profile_path.read_text(encoding="utf-8"))
    if not isinstance(provenance, dict) or not isinstance(profile_data, dict) or not profile_data:
        raise ValueError("profile and provenance must be non-empty JSON objects")
    if provenance.get("data_sha256") != hashes["profile_sha256"]:
        raise ValueError("profile artifact does not match the sidecar data_sha256")
    if provenance.get("authority") != "FooDB 1.0 official JSON download":
        raise ValueError("unexpected FooDB authority statement")
    if provenance.get("license") != "CC BY-NC 4.0; commercial use requires permission from FooDB authors":
        raise ValueError("unexpected license statement; stop for rights review")
    if provenance.get("semantics") != (
        "Presence-only profiles over named Compound-authority rows within the 255 globally most "
        "documented Content source IDs; no concentration, flavor, functional-equivalence, or safety claim"
    ):
        raise ValueError("unexpected artifact semantics")
    if len(profile_data) != EXPECTED_PROFILE_COUNT or provenance.get("indexed_food_name_count") != EXPECTED_PROFILE_COUNT:
        raise ValueError("profile inventory count differs from the pinned 971-profile contract")

    validate_profiles(profile_data)
    return profile_data, provenance, hashes


def load_runtime(service_root: Path, expected_service_hash: str = EXPECTED_SERVICE_SHA256):
    """Import the exact checked runtime from the nested AI-service checkout."""
    service_path = service_root / "src" / "compound_reasoning.py"
    if not service_path.is_file() or sha256(service_path) != expected_service_hash:
        raise ValueError("service source missing or hash does not match the approved runtime")
    root = str(service_root.resolve())
    if root not in sys.path:
        sys.path.insert(0, root)
    module = importlib.import_module("src.compound_reasoning")
    if Path(module.__file__).resolve() != service_path.resolve():
        raise ValueError("imported runtime module is not the pinned service source")
    return module


def _names(rows: list[Any]) -> set[str]:
    result: set[str] = set()
    for row in rows:
        name = row.get("name") if isinstance(row, dict) else getattr(row, "name", None)
        if not isinstance(name, str) or not name.strip():
            raise ValueError("runtime returned invalid compound metadata")
        result.add(name.lower())
    return result


def direct_oracle(left: list[Any], right: list[Any]) -> tuple[set[str], float]:
    left_names, right_names = _names(left), _names(right)
    shared = left_names & right_names
    union = left_names | right_names
    return shared, (len(shared) / len(union) if union else 0.0)


def pair_denominators(profile_count: int) -> dict[str, int]:
    if not isinstance(profile_count, int) or profile_count < 1:
        raise ValueError("profile count must be a positive integer")
    inclusive = profile_count * (profile_count + 1) // 2
    diagonal = profile_count
    return {
        "profile_count": profile_count,
        "inclusive_pairs_with_diagonal": inclusive,
        "diagonal_self_pairs": diagonal,
        "distinct_cross_profile_pairs": inclusive - diagonal,
    }


def compare_pair(left_name: str, right_name: str, left: list[Any], right: list[Any], runtime_analysis: Any) -> bool:
    expected_shared, expected_overlap = direct_oracle(left, right)
    actual_shared = set(runtime_analysis.shared_compound_names)
    actual_overlap = float(runtime_analysis.overlap_percentage)
    return actual_shared == expected_shared and math.isfinite(actual_overlap) and math.isclose(
        actual_overlap, expected_overlap, rel_tol=0.0, abs_tol=1e-15
    )


def run_reproduction(
    profile_data: dict[str, Any],
    analyzer: Any,
    *,
    expected_profile_count: int = EXPECTED_PROFILE_COUNT,
    expected_pair_count: int = EXPECTED_PAIR_COUNT,
) -> dict[str, Any]:
    names = validate_profiles(profile_data)
    if len(names) != expected_profile_count:
        raise ValueError(f"expected {expected_profile_count} indexed profiles, found {len(names)}")
    denominators = pair_denominators(len(names))
    if denominators["inclusive_pairs_with_diagonal"] != expected_pair_count:
        raise ValueError(f"pair denominator does not match the expected {expected_pair_count} inclusive pairs")

    zero_pairs = nonzero_pairs = mismatch_count = 0
    first_mismatches: list[dict[str, str]] = []
    for left_name, right_name in itertools.combinations_with_replacement(names, 2):
        left_rows, right_rows = profile_data[left_name], profile_data[right_name]
        oracle_shared, _oracle_overlap = direct_oracle(left_rows, right_rows)
        if oracle_shared:
            nonzero_pairs += 1
        else:
            zero_pairs += 1
        runtime = analyzer.analyze(left_name, right_name)
        mismatch = runtime is None or not compare_pair(left_name, right_name, left_rows, right_rows, runtime)
        if mismatch:
            mismatch_count += 1
            if len(first_mismatches) < 10:
                first_mismatches.append({"left": left_name, "right": right_name})
    if zero_pairs + nonzero_pairs != denominators["inclusive_pairs_with_diagonal"]:
        raise ValueError("internal pair accounting does not equal the inclusive denominator")
    return {
        **denominators,
        "zero_shared_name_pairs": zero_pairs,
        "nonzero_shared_name_pairs": nonzero_pairs,
        "runtime_oracle_mismatches": mismatch_count,
        "first_mismatch_pairs": first_mismatches,
        "result": "PASS_NARROW_ARITHMETIC_ONLY" if mismatch_count == 0 else "MISMATCH",
    }


def atomic_write_new(path: Path, content: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        raise FileExistsError(f"refusing to overwrite existing evidence: {path}")
    temporary = path.with_name(f".{path.name}.{uuid.uuid4().hex}.tmp")
    try:
        with temporary.open("xb") as stream:
            stream.write(content)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary, path)
    finally:
        if temporary.exists():
            temporary.unlink()


def build_receipt(
    result: dict[str, Any],
    hashes: dict[str, str],
    provenance: dict[str, Any],
    *,
    command: list[str],
) -> dict[str, Any]:
    return {
        "schema": "chefkix.r13_profile_pair_arithmetic.v1",
        "state": result["result"],
        "claim": "R13",
        "claim_scope": "profile-pair shared-name set and Jaccard arithmetic only",
        "command": command,
        "runtime": {"python": platform.python_version(), "platform": platform.platform()},
        "inputs": hashes,
        "provenance": {
            "authority": provenance["authority"],
            "license": provenance["license"],
            "semantics": provenance["semantics"],
        },
        "pair_accounting": result,
        "limitations": [
            "R13 remains UNVERIFIED pending separate review and register application.",
            "This does not establish usefulness, food-form or functional equivalence, flavor, nutrition, safety, quality, or C1-C5 acceptance.",
            "The profile artifact is locally excluded and is not redistributed; use is local and noncommercial under the stated CC BY-NC 4.0 terms.",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", type=Path, required=True)
    parser.add_argument("--provenance", type=Path, required=True)
    parser.add_argument("--service-root", type=Path, required=True, help="nested chefkix-ai-service checkout")
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()

    service_path = args.service_root / "src" / "compound_reasoning.py"
    profile_data, provenance, hashes = load_inputs(args.profile, args.provenance, service_path)
    hashes["driver_sha256"] = sha256(Path(__file__).resolve())
    runtime_module = load_runtime(args.service_root)
    index = runtime_module.CompoundIndex(foodb_path=args.profile, provenance_path=args.provenance)
    if not index.foodb_loaded or index.size != EXPECTED_PROFILE_COUNT:
        raise ValueError(f"pinned runtime refused the profile artifact: {index.load_error}")
    analyzer = runtime_module.SharedCompoundAnalyzer(index)
    result = run_reproduction(profile_data, analyzer)
    receipt = build_receipt(result, hashes, provenance, command=sys.argv)
    json_bytes = (json.dumps(receipt, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")
    report = (
        "# R13 local FooDB profile-pair arithmetic reproduction\n\n"
        f"**State:** `{result['result']}`. **Claim status remains UNVERIFIED pending independent review.**\n\n"
        "This is a noncommercial, attributed, local-only check of shared-compound-name set intersection and Jaccard arithmetic. "
        "FooDB presence profiles do not measure flavor, food-form/functional equivalence, substitution usefulness, nutrition, or safety.\n\n"
        "## Observed pair accounting\n\n"
        f"- Indexed profiles: {result['profile_count']}\n"
        f"- Unordered pairs with replacement, including diagonals: {result['inclusive_pairs_with_diagonal']}\n"
        f"- Diagonal self-pairs: {result['diagonal_self_pairs']}\n"
        f"- Distinct cross-profile pairs: {result['distinct_cross_profile_pairs']}\n"
        f"- Zero shared-name pairs: {result['zero_shared_name_pairs']}\n"
        f"- Nonzero shared-name pairs: {result['nonzero_shared_name_pairs']}\n"
        f"- Runtime/direct-oracle mismatches: {result['runtime_oracle_mismatches']}\n\n"
        "## Pinned bytes\n\n"
        f"- Profile SHA-256: `{hashes['profile_sha256']}`\n"
        f"- Provenance sidecar SHA-256: `{hashes['provenance_sha256']}`\n"
        f"- Runtime source SHA-256: `{hashes['service_sha256']}`\n"
        f"- Reproduction driver SHA-256: `{hashes['driver_sha256']}`\n"
        f"- Provenance: {provenance['authority']}; {provenance['license']}\n"
        f"- Machine receipt: `{args.receipt.as_posix()}`; SHA-256 `{hashlib.sha256(json_bytes).hexdigest()}`\n\n"
        "## Limitations\n\n"
        "This result is limited to arithmetic consistency over the pinned installed artifact. It is not paper-method equivalence, "
        "ingredient similarity, recommendation quality, usefulness, safety, rights clearance beyond the stated local noncommercial "
        "boundary, or contribution acceptance. The locally excluded FooDB bytes are not copied or redistributed.\n"
    ).encode("utf-8")
    atomic_write_new(args.receipt, json_bytes)
    atomic_write_new(args.report, report)
    print(json.dumps({"state": result["result"], "receipt_sha256": hashlib.sha256(json_bytes).hexdigest(), "report_sha256": hashlib.sha256(report).hexdigest(), "pairs": result["inclusive_pairs_with_diagonal"], "mismatches": result["runtime_oracle_mismatches"]}, sort_keys=True))
    return 0 if result["runtime_oracle_mismatches"] == 0 else 2


if __name__ == "__main__":
    raise SystemExit(main())
