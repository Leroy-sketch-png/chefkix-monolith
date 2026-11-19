#!/usr/bin/env python3
"""Recount allergen/trace tags from a hash-pinned OFF snapshot parquet."""

from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
from typing import Any

import pyarrow.parquet as pq


TAG_COLUMNS = ("allergens_tags", "traces_tags")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def manifest_digest(manifest: dict[str, Any]) -> str:
    payload = dict(manifest)
    payload.pop("manifest_sha256", None)
    canonical = json.dumps(payload, ensure_ascii=False, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


def normalized_tags(value: Any) -> set[str]:
    if not isinstance(value, list):
        return set()
    return {str(tag).strip().lower() for tag in value if str(tag).strip()}


def reproduce(manifest_path: Path, snapshot_path: Path, repo_root: Path) -> dict[str, Any]:
    manifest_path = manifest_path.resolve(strict=True)
    snapshot_path = snapshot_path.resolve(strict=True)
    repo_root = repo_root.resolve(strict=True)
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    derived = manifest.get("derived")
    if not isinstance(derived, dict):
        raise ValueError("manifest derived section is missing")
    if manifest_digest(manifest) != manifest.get("manifest_sha256"):
        raise ValueError("snapshot manifest canonical hash mismatch")
    manifest_snapshot_path = Path(derived.get("path", ""))
    if not manifest_snapshot_path.is_absolute():
        manifest_snapshot_path = repo_root / manifest_snapshot_path
    if manifest_snapshot_path.resolve(strict=True) != snapshot_path:
        raise ValueError("snapshot path does not match manifest")
    if snapshot_path.stat().st_size != derived.get("bytes"):
        raise ValueError("snapshot byte length does not match manifest")
    snapshot_hash = sha256(snapshot_path)
    if snapshot_hash != derived.get("sha256"):
        raise ValueError("snapshot SHA-256 does not match manifest")

    allergens: Counter[str] = Counter()
    traces: Counter[str] = Counter()
    rows = 0
    for batch in pq.ParquetFile(snapshot_path).iter_batches(
        batch_size=8192, columns=list(TAG_COLUMNS), use_threads=True
    ):
        for row in batch.to_pylist():
            rows += 1
            allergens.update(normalized_tags(row.get("allergens_tags")))
            traces.update(normalized_tags(row.get("traces_tags")))

    observed_allergens = dict(allergens)
    observed_traces = dict(traces)
    if rows != derived.get("rows"):
        raise ValueError("snapshot row count does not match manifest")
    if observed_allergens != derived.get("allergen_tag_counts"):
        raise ValueError("recomputed allergen tag counts do not match manifest")
    if observed_traces != derived.get("trace_tag_counts"):
        raise ValueError("recomputed trace tag counts do not match manifest")

    return {
        "schema_version": 1,
        "state": "PASS_OFF_SNAPSHOT_TAG_COUNTS_REPRODUCED",
        "manifest_path": str(manifest_path),
        "manifest_file_sha256": sha256(manifest_path),
        "manifest_canonical_sha256": manifest["manifest_sha256"],
        "snapshot_path": str(snapshot_path),
        "snapshot_sha256": snapshot_hash,
        "snapshot_rows": rows,
        "allergen_tag_count": len(allergens),
        "trace_tag_count": len(traces),
        "distinct_tag_union_count": len(allergens.keys() | traces.keys()),
        "allergen_occurrences": sum(allergens.values()),
        "trace_occurrences": sum(traces.values()),
        "total_occurrences": sum(allergens.values()) + sum(traces.values()),
        "claim_boundary": (
            "Reconstructs product-label tag counts in the frozen OFF snapshot; not a "
            "clinical allergen truth set, safety validation, or evidence that missing tags are safe."
        ),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--snapshot", type=Path, required=True)
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    result = reproduce(args.manifest, args.snapshot, args.repo_root)
    rendered = json.dumps(result, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8", newline="\n")
    print(rendered, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
