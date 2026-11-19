#!/usr/bin/env python3
"""Reproduce only the pinned FooDB presence-service inventory for claim R12."""

from __future__ import annotations

import argparse
import hashlib
import importlib
import json
import os
import platform
import sys
import uuid
from pathlib import Path
from typing import Any


EXPECTED_PROFILE_SHA256 = "8490cd6adadc5bad0e59f5f4edd8d05020b941b987ca47e38ac0cc80d85a04ac"
EXPECTED_PROVENANCE_SHA256 = "5117b791bd825b5c7c87f4f816342ff4bc65100b74502a8652157a989ed9d208"
EXPECTED_SERVICE_SHA256 = "e2bcad3cd81e196490aaa6bcc4f014721e9cb9e69734351520fabedef7463e3a"
EXPECTED_COUNTS = {
    "indexed_food_name_count": 971,
    "resolved_named_compound_count": 206,
    "indexed_food_compound_links": 125_992,
}
EXPECTED_AUTHORITY = "FooDB 1.0 official JSON download"
EXPECTED_LICENSE = "CC BY-NC 4.0; commercial use requires permission from FooDB authors"
EXPECTED_SEMANTICS = (
    "Presence-only profiles over named Compound-authority rows within the 255 globally most "
    "documented Content source IDs; no concentration, flavor, functional-equivalence, or safety claim"
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def require_hash(path: Path, expected: str, label: str) -> str:
    if not path.is_file():
        raise ValueError(f"{label} is missing: {path}")
    actual = sha256(path)
    if actual != expected:
        raise ValueError(f"{label} SHA-256 mismatch: expected {expected}, got {actual}")
    return actual


def count_inventory(profile_data: Any) -> dict[str, int]:
    if not isinstance(profile_data, dict) or not profile_data:
        raise ValueError("profile data must be a non-empty object")
    normalized_foods: set[str] = set()
    named_compounds: set[str] = set()
    link_count = 0
    for food_name, rows in profile_data.items():
        if not isinstance(food_name, str) or not food_name.strip():
            raise ValueError("food names must be non-empty strings")
        normalized_food = food_name.lower().strip()
        if normalized_food in normalized_foods:
            raise ValueError(f"duplicate normalized food identity: {normalized_food!r}")
        normalized_foods.add(normalized_food)
        if not isinstance(rows, list) or not rows:
            raise ValueError(f"food {food_name!r} has no named compound rows")
        per_food_names: set[str] = set()
        for row in rows:
            if not isinstance(row, dict):
                raise ValueError(f"food {food_name!r} has malformed compound metadata")
            name = row.get("name")
            if not isinstance(name, str) or not name.strip():
                raise ValueError(f"food {food_name!r} has an empty or non-string compound name")
            normalized_name = name.lower()
            if normalized_name in per_food_names:
                raise ValueError(f"food {food_name!r} has duplicate normalized compound names")
            per_food_names.add(normalized_name)
            named_compounds.add(normalized_name)
        link_count += len(rows)
    return {
        "indexed_food_name_count": len(normalized_foods),
        "resolved_named_compound_count": len(named_compounds),
        "indexed_food_compound_links": link_count,
    }


def load_inputs(profile_path: Path, provenance_path: Path, service_path: Path):
    hashes = {
        "profile_sha256": require_hash(profile_path, EXPECTED_PROFILE_SHA256, "profile artifact"),
        "provenance_sha256": require_hash(provenance_path, EXPECTED_PROVENANCE_SHA256, "provenance sidecar"),
        "service_sha256": require_hash(service_path, EXPECTED_SERVICE_SHA256, "service source"),
    }
    profile_data = json.loads(profile_path.read_text(encoding="utf-8"))
    provenance = json.loads(provenance_path.read_text(encoding="utf-8"))
    if not isinstance(provenance, dict):
        raise ValueError("provenance sidecar must be a JSON object")
    if provenance.get("data_sha256") != hashes["profile_sha256"]:
        raise ValueError("profile artifact does not match sidecar data_sha256")
    if provenance.get("authority") != EXPECTED_AUTHORITY:
        raise ValueError("unexpected data authority; stop for provenance review")
    if provenance.get("license") != EXPECTED_LICENSE:
        raise ValueError("unexpected license statement; stop for rights review")
    if provenance.get("semantics") != EXPECTED_SEMANTICS:
        raise ValueError("unexpected artifact semantics")
    observed = count_inventory(profile_data)
    for key, expected in EXPECTED_COUNTS.items():
        if observed[key] != expected:
            raise ValueError(f"direct {key} mismatch: expected {expected}, got {observed[key]}")
        if provenance.get(key) != expected:
            raise ValueError(f"sidecar {key} mismatch: expected {expected}, got {provenance.get(key)!r}")
    return profile_data, provenance, hashes, observed


def load_runtime(service_root: Path):
    source = service_root / "src" / "compound_reasoning.py"
    if not source.is_file() or sha256(source) != EXPECTED_SERVICE_SHA256:
        raise ValueError("service source is missing or does not match the pinned runtime")
    root = str(service_root.resolve())
    if root not in sys.path:
        sys.path.insert(0, root)
    module = importlib.import_module("src.compound_reasoning")
    if Path(module.__file__).resolve() != source.resolve():
        raise ValueError("imported runtime is not the pinned service source")
    return module


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


def run(profile_path: Path, provenance_path: Path, service_root: Path) -> dict[str, Any]:
    service_path = service_root / "src" / "compound_reasoning.py"
    _profile_data, provenance, hashes, direct_counts = load_inputs(profile_path, provenance_path, service_path)
    hashes["driver_sha256"] = sha256(Path(__file__).resolve())
    runtime = load_runtime(service_root)
    index = runtime.CompoundIndex(foodb_path=profile_path, provenance_path=provenance_path)
    if not index.foodb_loaded:
        raise ValueError(f"pinned runtime refused the inventory: {index.load_error}")
    runtime_count = index.size
    if runtime_count != direct_counts["indexed_food_name_count"]:
        raise ValueError(f"runtime inventory mismatch: direct={direct_counts['indexed_food_name_count']}, runtime={runtime_count}")
    declared_counts = {key: provenance[key] for key in EXPECTED_COUNTS}
    return {
        "schema": "chefkix.r12_foodb_service_inventory.v1",
        "state": "PASS_NARROW_INVENTORY_ONLY",
        "claim": "R12",
        "claim_scope": "pinned presence-only FooDB service inventory counts",
        "inputs": hashes,
        "runtime": {"python": platform.python_version(), "platform": platform.platform()},
        "provenance": {
            "authority": provenance["authority"],
            "license": provenance["license"],
            "semantics": provenance["semantics"],
        },
        "inventory": {
            "direct_json_counts": direct_counts,
            "sidecar_declared_counts": declared_counts,
            "loaded_runtime_profile_count": runtime_count,
        },
        "limitations": [
            "R12 remains UNVERIFIED pending separate independent review and one-row evidence application.",
            "Counts establish bounded inventory/presence facts only; they do not establish ingredient similarity, usefulness, food-form or functional equivalence, flavor, nutrition, safety, human outcomes, model quality, or C1-C5 acceptance.",
            "The local ignored profile artifact is not copied or redistributed. Use is limited to local attributed noncommercial preparation under the sidecar's declared CC BY-NC 4.0 terms; this is not legal advice or rights clearance.",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--profile", type=Path, required=True)
    parser.add_argument("--provenance", type=Path, required=True)
    parser.add_argument("--service-root", type=Path, required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    receipt = run(args.profile, args.provenance, args.service_root)
    receipt["command"] = sys.argv
    receipt_bytes = (json.dumps(receipt, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")
    receipt_hash = hashlib.sha256(receipt_bytes).hexdigest()
    report = (
        "# R12 pinned FooDB service-inventory reproduction\n\n"
        f"State: `{receipt['state']}`.\n\n"
        f"Receipt SHA-256: `{receipt_hash}`.\n\n"
        "## Observed inventory\n\n"
        "| Measure | Direct JSON | Sidecar | Loaded runtime |\n|---|---:|---:|---:|\n"
    )
    for key, value in receipt["inventory"]["direct_json_counts"].items():
        runtime_value = receipt["inventory"]["loaded_runtime_profile_count"] if key == "indexed_food_name_count" else "not separately exposed"
        report += f"| `{key}` | {value} | {receipt['inventory']['sidecar_declared_counts'][key]} | {runtime_value} |\n"
    report += (
        "\n## Provenance and limits\n\n"
        f"Authority: {receipt['provenance']['authority']}. License declared by the sidecar: {receipt['provenance']['license']}.\n\n"
        f"Semantics: {receipt['provenance']['semantics']}.\n\n"
        "This local reproduction verifies only pinned inventory arithmetic and runtime profile count. It is not rights clearance and does not establish usefulness, safety, quality, human outcomes, or thesis-contribution acceptance. The ignored artifact was not copied or redistributed.\n"
    )
    atomic_write_new(args.receipt, receipt_bytes)
    atomic_write_new(args.report, report.encode("utf-8"))
    print(json.dumps({"state": receipt["state"], "receipt_sha256": receipt_hash, "counts": receipt["inventory"]["direct_json_counts"]}, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
