"""Private, protocol-bound C1 raw-rater agreement analysis.

The bind command hashes inputs only. The analyze command verifies that receipt
before parsing returns. Outputs are private aggregate JSON plus a fresh-blind
adjudication queue; no raw labels or case-level metrics are printed.
"""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import math
import random
from pathlib import Path
from typing import Any

PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
LABELS = ("A", "C", "N", "U")
SEED = 20261021
BOOTSTRAPS = 2000


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def canonical_pack_binding(pack: dict[str, Any]) -> str:
    raw = json.dumps(pack, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()


def cohens_kappa(pairs: list[tuple[str, str]]) -> float | None:
    if not pairs:
        return None
    counts = {label: [0, 0] for label in LABELS}
    observed = 0
    for left, right in pairs:
        if left not in counts or right not in counts:
            raise ValueError("label outside frozen A/C/N/U vocabulary")
        counts[left][0] += 1
        counts[right][1] += 1
        observed += left == right
    n = len(pairs)
    po = observed / n
    pe = sum(a * b for a, b in counts.values()) / (n * n)
    if math.isclose(pe, 1.0):
        return 1.0 if math.isclose(po, 1.0) else None
    return (po - pe) / (1.0 - pe)


def wilson(successes: int, n: int, z: float = 1.959963984540054) -> list[float] | None:
    if n <= 0:
        return None
    p = successes / n
    z2 = z * z
    denom = 1.0 + z2 / n
    center = (p + z2 / (2.0 * n)) / denom
    margin = z * math.sqrt(p * (1.0 - p) / n + z2 / (4.0 * n * n)) / denom
    return [max(0.0, center - margin), min(1.0, center + margin)]


def linear_quantile(sorted_values: list[float], probability: float) -> float:
    """Type-7/NumPy-linear quantile from already sorted values, for sensitivity only."""
    if not sorted_values or not 0.0 <= probability <= 1.0:
        raise ValueError("quantile needs nonempty values and probability in [0,1]")
    position = (len(sorted_values) - 1) * probability
    lower = math.floor(position)
    upper = math.ceil(position)
    fraction = position - lower
    return sorted_values[lower] + fraction * (sorted_values[upper] - sorted_values[lower])


def cluster_bootstrap(case_pairs: dict[str, list[tuple[str, str]]]) -> dict[str, Any]:
    cases = sorted(case_pairs)
    rng = random.Random(SEED)
    values: list[float] = []
    for _ in range(BOOTSTRAPS):
        sampled = rng.choices(cases, k=len(cases))
        pairs = [pair for case_id in sampled for pair in case_pairs[case_id]]
        value = cohens_kappa(pairs)
        if value is not None and math.isfinite(value):
            values.append(value)
    if len(values) < 20:
        return {"valid_draws": len(values), "requested_draws": BOOTSTRAPS, "interval_95": None,
                "unavailable_reason": "fewer_than_20_valid_draws"}
    values.sort()
    lo = int(0.025 * len(values))
    hi = int(0.975 * len(values)) - 1
    return {"valid_draws": len(values), "requested_draws": BOOTSTRAPS,
            "interval_95": [values[lo], values[hi]],
            "interval_95_linear_interpolation_sensitivity": [linear_quantile(values, 0.025),
                                                               linear_quantile(values, 0.975)],
            "unavailable_reason": None}


def common_paths(args: argparse.Namespace) -> dict[str, Path]:
    return {
        "protocol": args.protocol.resolve(), "sample": args.sample.resolve(),
        "sample_manifest": args.sample_manifest.resolve(), "candidate_input": args.candidate_input.resolve(),
        "candidate_manifest": args.candidate_manifest.resolve(), "private_key": args.private_key.resolve(),
        "r1_pack": args.r1_pack.resolve(), "r2_pack": args.r2_pack.resolve(),
        "r1_return": args.r1_return.resolve(), "r2_return": args.r2_return.resolve(),
        "intake_receipt": args.intake_receipt.resolve(),
        "finalization_receipt": args.finalization_receipt.resolve(),
        "join_integrity_receipt": args.join_integrity_receipt.resolve(),
    }


def bind(args: argparse.Namespace) -> None:
    paths = common_paths(args)
    if digest(paths["protocol"]) != PROTOCOL_SHA256:
        raise ValueError("frozen protocol hash mismatch")
    if args.output.exists():
        raise FileExistsError("refusing to overwrite pre-access receipt")
    finalization = load_json(paths["finalization_receipt"])
    intake = load_json(paths["intake_receipt"])
    joins = load_json(paths["join_integrity_receipt"])
    if finalization.get("format") != "c1-preaccess-binding-v1" or finalization.get("protocol_sha256") != PROTOCOL_SHA256:
        raise ValueError("frozen reviewer-pack pre-access receipt mismatch")
    if intake.get("status") != "COMPLETE_BOUND_RETURNS" or intake.get("raw_files_unmodified") is not True or intake.get("adjudication_performed") is not False:
        raise ValueError("private return intake receipt is not in the expected state")
    if joins.get("format") != "c1-private-pack-join-integrity-v1" or not all(joins.get("checks", {}).values()):
        raise ValueError("private pack/join integrity receipt is not a complete pass")
    expected = {
        "protocol": PROTOCOL_SHA256,
        "sample": joins["pins"]["raw_sample_jsonl_sha256"],
        "sample_manifest": joins["pins"]["sample_manifest_sha256"],
        "candidate_input": joins["pins"]["candidate_input_sha256"],
        "private_key": joins["pins"]["private_key_sha256"],
        "r1_pack": joins["pins"]["reviewer_r1_json_sha256"],
        "r2_pack": joins["pins"]["reviewer_r2_json_sha256"],
        "r1_return": next(x["sha256"] for x in intake["returns"] if x["reviewer"] == "R1"),
        "r2_return": next(x["sha256"] for x in intake["returns"] if x["reviewer"] == "R2"),
        "finalization_receipt": joins["pins"]["finalization_receipt_sha256"],
    }
    observed = {name: digest(path) for name, path in paths.items()}
    for name, expected_hash in expected.items():
        if observed[name] != expected_hash:
            raise ValueError(f"pre-access pin mismatch: {name}")
    payload = {
        "format": "c1-rater-agreement-preaccess-v1",
        "task_id": "C1-Freeze-A-private-raw-agreement-20261107",
        "timestamp_utc": dt.datetime.now(dt.timezone.utc).isoformat().replace("+00:00", "Z"),
        "human_reported_ethics_consent_coverage": True,
        "ethics_evidence_basis": "HUMAN confirmation in chat; no institutional document inspected",
        "scope": "Private raw four-category agreement; no primary H1 estimate; no labels overwritten",
        "method": {"cohens_kappa": "unweighted nominal A/C/N/U", "cluster": "case_id; resample all candidate pairs within sampled cases",
                   "bootstrap_draws": BOOTSTRAPS, "seed": SEED,
                   "interval": "primary empirical order-statistic percentile 95%; linear-interpolation quantile is sensitivity only",
                   "missing_draw_rule": "interval unavailable if fewer than 20 valid draws"},
        "paths": {k: str(v) for k, v in paths.items()},
        "sha256": observed,
        "analyzer_sha256": digest(Path(__file__).resolve()),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps({"state": "PREACCESS_HASHES_BOUND", "input_count": len(paths),
                      "analyzer_sha256": payload["analyzer_sha256"], "receipt_sha256": digest(args.output)}, sort_keys=True))


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def analyze(args: argparse.Namespace) -> None:
    receipt = load_json(args.preaccess_receipt)
    if receipt.get("format") != "c1-rater-agreement-preaccess-v1" or not receipt.get("human_reported_ethics_consent_coverage"):
        raise ValueError("missing frozen pre-access receipt or HUMAN-reported authorization")
    paths = {k: Path(v) for k, v in receipt["paths"].items()}
    if digest(Path(__file__).resolve()) != receipt["analyzer_sha256"]:
        raise ValueError("analyzer changed after pre-access binding")
    for name, path in paths.items():
        if digest(path) != receipt["sha256"][name]:
            raise ValueError(f"pre-access input hash mismatch: {name}")
    if receipt["sha256"]["protocol"] != PROTOCOL_SHA256:
        raise ValueError("protocol hash in receipt is not frozen Protocol v1")
    key = load_json(paths["private_key"])
    packs = {"R1": load_json(paths["r1_pack"]), "R2": load_json(paths["r2_pack"])}
    returns = {"R1": load_json(paths["r1_return"]), "R2": load_json(paths["r2_return"])}
    if key.get("format") != "c1-private-key-v1" or len(key.get("entries", [])) != 1734:
        raise ValueError("private key inventory mismatch")
    key_by_token: dict[tuple[str, str, str], dict[str, Any]] = {}
    for entry in key["entries"]:
        index = (entry["reviewer"], entry["view_id"], entry["blind_candidate"])
        if index in key_by_token or entry.get("recorded_gold") not in (True, False):
            raise ValueError("private-key duplicate or invalid gold flag")
        key_by_token[index] = entry
    pairs_by_case: dict[str, list[tuple[str, str]]] = {}
    identity_labels: dict[str, dict[str, dict[str, str]]] = {"R1": {}, "R2": {}}
    counts: dict[str, dict[str, int]] = {r: {label: 0 for label in LABELS} for r in ("R1", "R2")}
    seen_indexes: set[tuple[str, str, str]] = set()
    queue: list[dict[str, Any]] = []
    gold_n_both: set[str] = set()
    sample_rows = [json.loads(line) for line in paths["sample"].read_text(encoding="utf-8").splitlines() if line.strip()]
    if len(sample_rows) != 150 or len({row["case_id"] for row in sample_rows}) != 150:
        raise ValueError("sample does not contain the frozen 150 unique cases")
    sample_case_ids = {row["case_id"] for row in sample_rows}
    for reviewer in ("R1", "R2"):
        pack, returned = packs[reviewer], returns[reviewer]
        binding = canonical_pack_binding(pack)
        if pack.get("reviewer") != reviewer or returned.get("reviewer") != reviewer or returned.get("pack_id") != pack.get("pack_id") or returned.get("pack_binding_sha256") != binding:
            raise ValueError(f"issued pack binding mismatch: {reviewer}")
        if returned.get("format") != "c1-reviewer-return-v1" or returned.get("complete") is not True:
            raise ValueError(f"return is incomplete or wrong format: {reviewer}")
        view_by_id = {v["view_id"]: v for v in pack["views"]}
        rows = returned.get("judgments")
        if len(pack.get("views", [])) != 150 or len(rows) != 867 or returned.get("expected_judgments") != 867:
            raise ValueError(f"fixed coverage mismatch: {reviewer}")
        local_seen: set[tuple[str, str, str]] = set()
        for row in rows:
            index = (reviewer, row["view_id"], row["blind_candidate"])
            if set(row) != {"record_id", "view_id", "blind_candidate", "label"} or row["label"] not in LABELS:
                raise ValueError(f"invalid return row schema/label: {reviewer}")
            if row["record_id"] != f"{reviewer}:{row['view_id']}:{row['blind_candidate']}" or index in local_seen or index not in key_by_token:
                raise ValueError(f"duplicate or foreign return row: {reviewer}")
            local_seen.add(index)
            seen_indexes.add(index)
            entry = key_by_token[index]
            view = view_by_id.get(row["view_id"])
            if view is None:
                raise ValueError(f"return token absent from issued view: {reviewer}")
            case_id, identity = entry["case_id"], entry["identity_id"]
            view_candidate = next((c for c in view["candidates"] if c["blind_candidate"] == row["blind_candidate"]), None)
            # The issued blind view intentionally omits case and identity IDs.
            # The prevalidated private view→key join is the identity boundary.
            if (entry["reviewer"] != reviewer or entry["view_id"] != row["view_id"] or
                    entry["blind_candidate"] != row["blind_candidate"] or view_candidate is None or
                    view_candidate.get("display_names") != entry.get("display_names") or
                    identity in identity_labels[reviewer].setdefault(case_id, {})):
                raise ValueError(f"duplicate identity or private pack/key binding mismatch: {reviewer}")
            identity_labels[reviewer][case_id][identity] = row["label"]
            counts[reviewer][row["label"]] += 1
        if local_seen != {k for k in key_by_token if k[0] == reviewer}:
            raise ValueError(f"return does not exactly cover private key: {reviewer}")
    if seen_indexes != set(key_by_token):
        raise ValueError("rater returns do not exactly cover the private key")
    if set(identity_labels["R1"]) != set(identity_labels["R2"]) or set(identity_labels["R1"]) != sample_case_ids:
        raise ValueError("rater case sets differ from the frozen 150-case roster")
    r1_counts = {"A": 0, "C": 0, "N": 0, "U": 0}
    r2_counts = {"A": 0, "C": 0, "N": 0, "U": 0}
    for case_id in sorted(identity_labels["R1"]):
        l1, l2 = identity_labels["R1"][case_id], identity_labels["R2"][case_id]
        if set(l1) != set(l2) or not 5 <= len(l1) <= 6:
            raise ValueError("candidate identity roster differs across raters")
        case_pairs = [(l1[identity], l2[identity]) for identity in sorted(l1)]
        pairs_by_case[case_id] = case_pairs
        for identity in sorted(l1):
            left, right = l1[identity], l2[identity]
            r1_counts[left] += 1
            r2_counts[right] += 1
            if left != right:
                queue.append({"queue_item_id": hashlib.sha256((case_id + "\0" + identity).encode()).hexdigest()[:24],
                              "case_id": case_id, "identity_id": identity, "rater_labels_differ": True})
            if any(e["recorded_gold"] for e in key["entries"] if e["case_id"] == case_id and e["identity_id"] == identity) and left == right == "N":
                gold_n_both.add(case_id)
    pairs = [pair for case in pairs_by_case.values() for pair in case]
    matches = sum(left == right for left, right in pairs)
    kappa = cohens_kappa(pairs)
    ci = cluster_bootstrap(pairs_by_case)
    summary = {
        "format": "c1-private-raw-agreement-summary-v1", "state": "PRIVATE_DESCRIPTIVE_RAW_AGREEMENT",
        "protocol_sha256": PROTOCOL_SHA256, "sample_raw_sha256": receipt["sha256"]["sample"],
        "sample_manifest_sha256": receipt["sha256"]["sample_manifest"],
        "candidate_input_sha256": receipt["sha256"]["candidate_input"],
        "rater_return_sha256": {r: receipt["sha256"][f"{r.lower()}_return"] for r in ("R1", "R2")},
        "denominators": {"cases": 150, "candidate_judgments_per_rater": len(pairs), "paired_judgments": len(pairs)},
        "rater_label_counts": {"R1": r1_counts, "R2": r2_counts},
        "u_rate": {r: {"count": counts[r]["U"], "n": len(pairs), "proportion": counts[r]["U"] / len(pairs), "wilson_95": wilson(counts[r]["U"], len(pairs))} for r in ("R1", "R2")},
        "exact_four_category_agreement": {"matches": matches, "n": len(pairs), "proportion": matches / len(pairs), "wilson_95": wilson(matches, len(pairs))},
        "cohens_kappa_unweighted_nominal_A_C_N_U": {"point": kappa, **ci,
          "protocol_proceed_threshold_0_5_met": kappa is not None and kappa >= 0.5,
          "reliable_agreement_lower_bound_0_6_met": ci["interval_95"] is not None and ci["interval_95"][0] >= 0.6},
        "both_raters_N_on_recorded_gold_case_count": len(gold_n_both),
        "disagreement_candidate_count": len(queue), "adjudicated_values_created": 0,
        "interpretation_limit": "Exposed fixed-sample human audit description; not primary H1 rescue, human-validity acceptance, or generalization.",
        "input_receipt_sha256": digest(args.preaccess_receipt),
    }
    for path in (args.summary_output, args.queue_output):
        if path.exists():
            raise FileExistsError(f"refusing to overwrite output: {path}")
    args.summary_output.parent.mkdir(parents=True, exist_ok=True)
    create_only_text(args.summary_output, json.dumps(summary, indent=2, sort_keys=True) + "\n")
    create_only_text(args.queue_output, "".join(json.dumps(row, sort_keys=True) + "\n" for row in queue))
    print(json.dumps({"state": summary["state"], "cases": 150, "paired_judgments": len(pairs),
                      "agreement_count": matches, "disagreement_candidates": len(queue),
                      "kappa": kappa, "bootstrap_valid_draws": ci["valid_draws"],
                      "summary_sha256": digest(args.summary_output), "queue_sha256": digest(args.queue_output)}, sort_keys=True))


def create_only_text(path: Path, text: str) -> None:
    """Create a durable UTF-8 artifact without overwriting prior evidence."""
    with path.open("x", encoding="utf-8", newline="\n") as f:
        f.write(text)
        f.flush()
        import os
        os.fsync(f.fileno())


def add_paths(parser: argparse.ArgumentParser) -> None:
    for name in ("protocol", "sample", "sample-manifest", "candidate-input", "candidate-manifest", "private-key",
                 "r1-pack", "r2-pack", "r1-return", "r2-return", "intake-receipt", "finalization-receipt", "join-integrity-receipt"):
        parser.add_argument("--" + name, type=Path, required=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    bind_parser = sub.add_parser("bind")
    add_paths(bind_parser)
    bind_parser.add_argument("--output", type=Path, required=True)
    analyze_parser = sub.add_parser("analyze")
    analyze_parser.add_argument("--preaccess-receipt", type=Path, required=True)
    analyze_parser.add_argument("--summary-output", type=Path, required=True)
    analyze_parser.add_argument("--queue-output", type=Path, required=True)
    args = parser.parse_args()
    if args.command == "bind":
        bind(args)
    else:
        analyze(args)


if __name__ == "__main__":
    main()
