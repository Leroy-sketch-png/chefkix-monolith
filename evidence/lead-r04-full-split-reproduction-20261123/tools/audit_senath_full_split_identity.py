#!/usr/bin/env python3
"""Reconstruct full Senath validation/test case joins and pair-identity strata.

This creates local-only case/evaluation artifacts. Gold labels are written to
separate files and are never included in the source-only case frames. The
released GISMo identity contract and Senath-normalized sensitivity are both
reported; neither is silently substituted for the other.
"""

from __future__ import annotations

import argparse
import ast
import csv
import hashlib
import json
import os
import platform
import shutil
import uuid
from collections import Counter
from pathlib import Path
from typing import Any

import torch

from build_senath_candidate_pools import ASSET_HASHES, SOURCE_SHA256, load_hash_pinned_helpers, normalize, sha256


EXPECTED_CLEANROOM = {
    "train_comments_subs_with_titles.csv": "65d143c3bacd21555e5f16abdddd961585fce7dcbfdabfcbad0c5ccf8d60557c",
    "val_comments_subs_with_titles.csv": "002588cb1d7db2d1644cb16079542df641c0ace4807490be380e150973796a0b",
    "test_comments_subs_with_titles.csv": "7d239b405ef71a4dea3513afe2a22b11b9e0e15256428b40896a31cae0ddd2a6",
}
GISMO_TEST_PICKLE_SHA256 = "0758382cae59697958dd2a5e509c15f8d0417ea1a9e079fd75792779f1ce6174"
COUNTS = {
    "train": 49_044,
    "val": 10_729,
    "test": 10_747,
}
EXPECTED_STRATA = {
    "val": {"GISMo_graph_pair_identity": {"seen": 6_827, "unseen": 3_902},
            "Senath_normalized_pair_sensitivity": {"seen": 6_771, "unseen": 3_958}},
    "test": {"GISMo_graph_pair_identity": {"seen": 6_932, "unseen": 3_815},
             "Senath_normalized_pair_sensitivity": {"seen": 6_866, "unseen": 3_881}},
}


def read_csv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8-sig", newline="") as handle:
        return list(csv.DictReader(handle))


def parse_pair(value: str) -> tuple[str, str]:
    parsed = ast.literal_eval(value)
    if not isinstance(parsed, tuple) or len(parsed) != 2:
        raise ValueError("substitution field must be a two-item tuple")
    return str(parsed[0]), str(parsed[1])


def context(value: str) -> list[list[str]]:
    parsed = ast.literal_eval(value)
    if not isinstance(parsed, list) or any(not isinstance(group, list) for group in parsed):
        raise ValueError("ingredient context must be a list of lists")
    return [[str(item) for item in group] for group in parsed]


def row_digest(row: dict[str, Any]) -> str:
    payload = json.dumps(row, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def write_jsonl(path: Path, rows: list[dict[str, Any]]) -> None:
    with path.open("x", encoding="utf-8", newline="\n") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False, sort_keys=True) + "\n")
        handle.flush()
        os.fsync(handle.fileno())


def pair_node_id(raw: str, vocab: Any, names: dict[str, int]) -> int | None:
    vocab_index = vocab.word2idx.get(str(raw))
    if vocab_index is None:
        return None
    canonical = vocab.idx2word[vocab_index]
    if isinstance(canonical, list):
        canonical = canonical[0]
    graph_id = names.get(canonical)
    return int(graph_id) if graph_id is not None else None


def join_split(
    split: str,
    csv_rows: list[dict[str, str]],
    pickle_rows: list[dict[str, Any]],
    train_graph_pairs: set[tuple[int, int]],
    train_normalized_pairs: set[tuple[str, str]],
    vocab: Any,
    names: dict[str, int],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]], list[dict[str, Any]], dict[str, dict[str, int]]]:
    if len(csv_rows) != len(pickle_rows) or len(csv_rows) != COUNTS[split]:
        raise ValueError(f"{split} CSV/pickle row-count mismatch")

    case_rows: list[dict[str, Any]] = []
    label_rows: list[dict[str, Any]] = []
    identity_rows: list[dict[str, Any]] = []
    graph_counts: Counter[str] = Counter()
    senath_counts: Counter[str] = Counter()
    missing_graph_pair = 0

    for index, (csv_row, graph_row) in enumerate(zip(csv_rows, pickle_rows)):
        csv_pair = parse_pair(csv_row["subs"])
        graph_pair = tuple(str(item) for item in graph_row["subs"])
        csv_context = context(csv_row["ingredients"])
        graph_context = [[str(item) for item in group] for group in graph_row["ingredients"]]
        if (
            len(graph_pair) != 2
            or str(csv_row["id"]) != str(graph_row["id"])
            or tuple(normalize(item) for item in csv_pair) != tuple(normalize(item) for item in graph_pair)
            or [[normalize(item) for item in group] for group in csv_context]
            != [[normalize(item) for item in group] for group in graph_context]
        ):
            raise ValueError(f"{split} source/pickle join mismatch at row {index}")

        source_raw, target_raw = csv_pair
        source_id = pair_node_id(graph_pair[0], vocab, names)
        target_id = pair_node_id(graph_pair[1], vocab, names)
        if source_id is None or target_id is None:
            missing_graph_pair += 1
        graph_status = "seen" if source_id is not None and target_id is not None and (source_id, target_id) in train_graph_pairs else "unseen"
        normalized_pair = (normalize(source_raw), normalize(target_raw))
        senath_status = "seen" if normalized_pair in train_normalized_pairs else "unseen"
        graph_counts[graph_status] += 1
        senath_counts[senath_status] += 1

        source_row = {
            "case_id": f"{split}:{index}:{csv_row['id']}",
            "split": split,
            "source_dataset": "Recipe1MSubs",
            "license_tag": "UNKNOWN_NOT_REVIEWED",
            "row_index": index,
            "recipe_id": str(csv_row["id"]),
            "recipe_title": str(csv_row["Recipe Title"]),
            "ingredients_raw": csv_context,
            "source_ingredient_raw": source_raw,
            # This digest is safe to include in the source-only case frame: it
            # binds only fields that the inference runner may receive.
            "source_input_sha256": row_digest({
                "recipe_id": str(csv_row["id"]), "title": str(csv_row["Recipe Title"]),
                "ingredients": csv_context, "source_ingredient": source_raw,
            }),
        }
        case_rows.append(source_row)
        label_rows.append({
            "case_id": source_row["case_id"], "split": split, "row_index": index,
            "recipe_id": str(csv_row["id"]), "target_ingredient_raw": target_raw,
            "target_ingredient_senath_normalized": normalized_pair[1],
            "target_graph_id": target_id,
            "source_target_pair_sha256": row_digest({"pair": list(csv_pair)}),
        })
        identity_rows.append({
            "case_id": source_row["case_id"], "split": split, "row_index": index,
            "recipe_id": str(csv_row["id"]), "source_graph_id": source_id,
            "target_graph_id": target_id, "GISMo_graph_pair_identity": graph_status,
            "Senath_normalized_pair_sensitivity": senath_status,
            "evidence_coverage": "UNKNOWN_NOT_COMPUTED",
            "license_tag": "UNKNOWN_NOT_REVIEWED",
        })

    if missing_graph_pair:
        raise ValueError(f"{split} has {missing_graph_pair} unresolved GISMo graph pair identities")
    actual = {
        "GISMo_graph_pair_identity": {"seen": graph_counts["seen"], "unseen": graph_counts["unseen"]},
        "Senath_normalized_pair_sensitivity": {"seen": senath_counts["seen"], "unseen": senath_counts["unseen"]},
    }
    if actual != EXPECTED_STRATA[split]:
        raise ValueError(f"{split} strata differ from the current pinned identity contract: {actual}")
    return case_rows, label_rows, identity_rows, actual


def build(args: argparse.Namespace) -> dict[str, Any]:
    assets = args.gismo_assets.resolve()
    cleanroom = args.cleanroom_dir.resolve()
    source = args.gismo_source.resolve()
    output = args.output.resolve()
    if output.exists():
        raise FileExistsError(f"refusing to overwrite existing output: {output}")

    actual_assets = {name: sha256(assets / name) for name in ASSET_HASHES}
    if actual_assets != ASSET_HASHES:
        raise ValueError("GISMo asset hash mismatch against current pinned inventory")
    extra_hashes = {"test_comments_subs.pkl": GISMO_TEST_PICKLE_SHA256}
    if {name: sha256(assets / name) for name in extra_hashes} != extra_hashes:
        raise ValueError("GISMo test pickle hash mismatch")
    input_hashes = {name: sha256(cleanroom / name) for name in EXPECTED_CLEANROOM}
    if input_hashes != EXPECTED_CLEANROOM:
        raise ValueError("clean-room input hashes differ from current frozen pins")
    if sha256(source) != SOURCE_SHA256:
        raise ValueError("GISMo reproduction source hash mismatch")

    helpers = load_hash_pinned_helpers(source)
    vocab = helpers["load_vocab"](assets / "vocab_ingrs.pkl")
    pickle_splits = {
        "train": helpers["load_plain"](assets / "train_comments_subs.pkl"),
        "val": helpers["load_plain"](assets / "val_comments_subs.pkl"),
        "test": helpers["load_plain"](assets / "test_comments_subs.pkl"),
    }
    if {split: len(rows) for split, rows in pickle_splits.items()} != COUNTS:
        raise ValueError("GISMo pickle split row counts differ from frozen counts")

    names, _, _, _, _, _ = helpers["load_graph"](
        {"nodes_191120.csv": assets / "nodes_191120.csv", "edges_191120.csv": assets / "edges_191120.csv"},
        torch.device("cpu"),
    )
    train_graph_pairs: set[tuple[int, int]] = set()
    train_normalized_pairs: set[tuple[str, str]] = set()
    for record in pickle_splits["train"]:
        raw_pair = tuple(str(item) for item in record["subs"])
        source_id, target_id = (pair_node_id(raw, vocab, names) for raw in raw_pair)
        if source_id is None or target_id is None:
            raise ValueError("training split contains an unresolved GISMo graph pair")
        train_graph_pairs.add((source_id, target_id))
        train_normalized_pairs.add((normalize(raw_pair[0]), normalize(raw_pair[1])))

    train_csv = read_csv(cleanroom / "train_comments_subs_with_titles.csv")
    if len(train_csv) != COUNTS["train"]:
        raise ValueError("clean-room training CSV row count mismatch")
    # The training CSV is the case-frame parent; verify it joins to GISMo's pair
    # reference before it is used to define either seen/unseen contract.
    for index, (row, graph_row) in enumerate(zip(train_csv, pickle_splits["train"])):
        csv_pair = parse_pair(row["subs"])
        graph_pair = tuple(str(item) for item in graph_row["subs"])
        csv_context = context(row["ingredients"])
        if (
            str(row["id"]) != str(graph_row["id"])
            or tuple(normalize(item) for item in csv_pair) != tuple(normalize(item) for item in graph_pair)
            or [[normalize(item) for item in group] for group in csv_context]
            != [[normalize(str(item)) for item in group] for group in graph_row["ingredients"]]
        ):
            raise ValueError(f"train source/pickle join mismatch at row {index}")

    csv_splits = {split: read_csv(cleanroom / f"{split}_comments_subs_with_titles.csv") for split in ("val", "test")}
    results: dict[str, Any] = {}
    staging = output.with_name(f".{output.name}.staging-{uuid.uuid4().hex}")
    staging.mkdir(parents=True)
    try:
        evaluation_dir = staging / "local-only-evaluation"
        evaluation_dir.mkdir()
        case_hashes: dict[str, str] = {}
        label_hashes: dict[str, str] = {}
        identity_rows_all: list[dict[str, Any]] = []
        for split in ("val", "test"):
            case_rows, labels, identities, strata = join_split(
                split, csv_splits[split], pickle_splits[split], train_graph_pairs,
                train_normalized_pairs, vocab, names,
            )
            case_path = staging / f"cases_{split}.jsonl"
            label_path = evaluation_dir / f"labels_{split}.jsonl"
            write_jsonl(case_path, case_rows)
            write_jsonl(label_path, labels)
            case_hashes[case_path.name] = sha256(case_path)
            label_hashes[label_path.relative_to(staging).as_posix()] = sha256(label_path)
            identity_rows_all.extend(identities)
            results[split] = {"row_count": len(case_rows), "case_sha256": case_hashes[case_path.name],
                              "label_sha256": label_hashes[label_path.relative_to(staging).as_posix()],
                              "strata": strata}

        identity_path = evaluation_dir / "identity_map.csv"
        with identity_path.open("x", encoding="utf-8", newline="") as handle:
            fields = list(identity_rows_all[0])
            writer = csv.DictWriter(handle, fieldnames=fields, lineterminator="\n")
            writer.writeheader()
            writer.writerows(identity_rows_all)
            handle.flush()
            os.fsync(handle.fileno())
        counts_path = evaluation_dir / "strata_counts.csv"
        with counts_path.open("x", encoding="utf-8", newline="") as handle:
            writer = csv.writer(handle, lineterminator="\n")
            writer.writerow(["split", "identity_contract", "stratum", "count"])
            for split in ("val", "test"):
                for contract, strata in results[split]["strata"].items():
                    for stratum in ("seen", "unseen"):
                        writer.writerow([split, contract, stratum, strata[stratum]])
            handle.flush()
            os.fsync(handle.fileno())

        outputs = [staging / "cases_val.jsonl", staging / "cases_test.jsonl", identity_path,
                   counts_path, evaluation_dir / "labels_val.jsonl", evaluation_dir / "labels_test.jsonl"]
        manifest = {
            "schema_version": 1,
            "state": "LOCAL_FULL_SPLIT_JOIN_AND_IDENTITY_AUDIT_PASS",
            "scope": "Local-only deterministic case/identity frames; no inference, ranking, human labels, or quality result.",
            "identity_contracts": {
                "GISMo_graph_pair_identity": "Resolve each endpoint through the hash-pinned released GISMo vocabulary into graph-node IDs; pair is seen iff that exact ordered graph-ID pair occurs in train. Do not restrict pair identity to the 6,653 candidate-scoring catalog.",
                "Senath_normalized_pair_sensitivity": "Normalize raw source/target strings with the pinned existing normalization function; pair is seen iff exact normalized ordered pair occurs in train.",
            },
            "inputs": {"cleanroom": input_hashes, "gismo_assets": actual_assets | extra_hashes,
                       "gismo_source_sha256": SOURCE_SHA256},
            "builder_script_sha256": sha256(Path(__file__).resolve()),
            "runtime": {"python": platform.python_version(), "torch": torch.__version__},
            "train_reference": {"row_count": COUNTS["train"], "csv_pickle_join": "PASS",
                                "unique_graph_pairs": len(train_graph_pairs),
                                "unique_senath_normalized_pairs": len(train_normalized_pairs)},
            "splits": results,
            "strata_counts_sha256": sha256(counts_path),
            "identity_map_sha256": sha256(identity_path),
            "case_file_hashes": case_hashes,
            "local_only_evaluation_hashes": {
                path.relative_to(staging).as_posix(): sha256(path)
                for path in [identity_path, counts_path, evaluation_dir / "labels_val.jsonl", evaluation_dir / "labels_test.jsonl"]
            },
            "case_schema_notes": {"license_tag": "UNKNOWN_NOT_REVIEWED", "evidence_coverage": "UNKNOWN_NOT_COMPUTED",
                                  "endpoint_frequency": "NOT_COMPUTED; PR01/PR03/PR04 resolution required before use"},
            "outputs_sha256": {path.relative_to(staging).as_posix(): sha256(path) for path in outputs},
            "limits": ["No FooDB evidence-coverage adjudication", "No blinded rater fixture or human labels",
                       "No arm outputs, model inference, metrics, quality, or release-rights claim"],
        }
        manifest_path = staging / "manifest.json"
        with manifest_path.open("x", encoding="utf-8", newline="\n") as handle:
            handle.write(json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n")
            handle.flush()
            os.fsync(handle.fileno())
        if output.exists():
            raise FileExistsError(f"refusing to overwrite existing output: {output}")
        staging.replace(output)
    except Exception:
        shutil.rmtree(staging, ignore_errors=True)
        raise

    return {"state": manifest["state"], "output": str(output), "splits": results,
            "strata_counts_sha256": manifest["strata_counts_sha256"],
            "identity_map_sha256": manifest["identity_map_sha256"],
            "manifest_sha256": sha256(output / "manifest.json"),
            "train_reference": manifest["train_reference"], "outputs_sha256": manifest["outputs_sha256"]}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--gismo-assets", required=True, type=Path)
    parser.add_argument("--cleanroom-dir", required=True, type=Path)
    parser.add_argument("--gismo-source", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    print(json.dumps(build(args), indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
