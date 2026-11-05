#!/usr/bin/env python3
"""Reproduce the fixed Recipe1MSubs CSV split row counts from pinned bytes."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
import tempfile
from pathlib import Path
from typing import Any


EXPECTED = {
    "train": ("train_comments_subs_with_titles.csv", 49_044),
    "validation": ("val_comments_subs_with_titles.csv", 10_729),
    "test": ("test_comments_subs_with_titles.csv", 10_747),
}
DEFAULT_DATA = Path("personal/projects/chefkix/lead_recipe1msubs/senath-llm/Datasets")
DEFAULT_MANIFEST = Path("ai/out/iron-chef-lead-v26/selective_manifest.json")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def row_count(path: Path) -> int:
    with path.open("r", encoding="utf-8", newline="") as stream:
        reader = csv.DictReader(stream)
        if not reader.fieldnames:
            raise ValueError(f"missing CSV header: {path.name}")
        return sum(1 for _ in reader)


def build_report(root: Path, data_dir: Path, manifest_path: Path) -> dict[str, Any]:
    root = root.resolve(strict=True)
    data_dir = (root / data_dir).resolve(strict=True)
    manifest_path = (root / manifest_path).resolve(strict=True)
    for candidate in (data_dir, manifest_path):
        try:
            candidate.relative_to(root)
        except ValueError as exc:
            raise ValueError("input path escapes repository root") from exc

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    declared = manifest.get("dataset_hashes")
    if not isinstance(declared, dict) or set(declared) != set(EXPECTED):
        raise ValueError("manifest must declare exactly train, validation, and test hashes")

    splits: dict[str, dict[str, Any]] = {}
    for split, (filename, expected_count) in EXPECTED.items():
        path = data_dir / filename
        if path.is_symlink() or not path.is_file():
            raise ValueError(f"missing or symlinked split input: {split}")
        digest = sha256(path)
        if digest != declared[split]:
            raise ValueError(f"manifest hash mismatch for {split}")
        count = row_count(path)
        if count != expected_count:
            raise ValueError(f"row-count mismatch for {split}: {count}")
        splits[split] = {"path": path.relative_to(root).as_posix(), "sha256": digest, "rows": count}

    return {
        "schema_version": 1,
        "state": "FIXED_FILE_ROW_COUNTS_REPRODUCED",
        "interpretation_limit": "Counts describe these hash-pinned local split files only; they do not establish paper protocol equivalence, model performance, or generalization.",
        "manifest": {"path": manifest_path.relative_to(root).as_posix(), "sha256": sha256(manifest_path)},
        "splits": splits,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--data-dir", type=Path, default=DEFAULT_DATA)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    root = args.repo_root.resolve(strict=True)
    output = (root / args.output).resolve(strict=False) if not args.output.is_absolute() else args.output.resolve(strict=False)
    try:
        output.relative_to(root)
    except ValueError as exc:
        raise ValueError("output must remain inside repository root") from exc
    if output.exists():
        raise FileExistsError(f"refusing to overwrite output: {output}")
    output.parent.mkdir(parents=True, exist_ok=True)
    report = build_report(root, args.data_dir, args.manifest)
    fd, temp_name = tempfile.mkstemp(prefix=".split-counts-", suffix=".json", dir=output.parent)
    temp = Path(temp_name)
    try:
        with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as stream:
            stream.write(json.dumps(report, indent=2, sort_keys=True) + "\n")
            stream.flush()
            os.fsync(stream.fileno())
        temp.rename(output)
    except Exception:
        temp.unlink(missing_ok=True)
        raise
    print(json.dumps(report, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
