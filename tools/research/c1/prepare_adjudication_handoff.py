"""Prepare a private, context-bearing L36 adjudication packet from issued C1 returns.

This tool preserves both raw returns, exposes neither rater identity nor arm/rank/gold,
and writes a separate opaque-ID mapping for the coordinator. It does not adjudicate.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
from typing import Any

LABELS = {"A", "C", "N", "U"}


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def opaque_item(case_id: str, identity_id: str) -> str:
    return hashlib.sha256((case_id + "\0" + identity_id).encode("utf-8")).hexdigest()[:24]


def pack_binding(pack: dict[str, Any]) -> str:
    raw = json.dumps(pack, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(raw).hexdigest()


def validate_context(view: dict[str, Any]) -> tuple[str, tuple[str, ...], str]:
    title, target = view.get("recipe_title"), view.get("target")
    ingredients = view.get("ingredients")
    if not isinstance(title, str) or not title.strip() or not isinstance(target, str) or not target.strip():
        raise ValueError("missing recipe context")
    if (not isinstance(ingredients, list) or not ingredients or
            any(not isinstance(x, str) or not x.strip() for x in ingredients)):
        raise ValueError("invalid ingredient context")
    return title, tuple(ingredients), target


def prepare(key_path: Path, r1_pack_path: Path, r2_pack_path: Path,
            r1_return_path: Path, r2_return_path: Path, queue_path: Path) -> tuple[dict, dict, dict]:
    paths = {"private_key": key_path, "r1_pack": r1_pack_path, "r2_pack": r2_pack_path,
             "r1_return": r1_return_path, "r2_return": r2_return_path, "queue": queue_path}
    key, packs = load(key_path), {"R1": load(r1_pack_path), "R2": load(r2_pack_path)}
    returns = {"R1": load(r1_return_path), "R2": load(r2_return_path)}
    queue = [json.loads(line) for line in queue_path.read_text(encoding="utf-8").splitlines() if line.strip()]
    if key.get("format") != "c1-private-key-v1" or not isinstance(key.get("entries"), list):
        raise ValueError("invalid private key")
    if len(queue) != 150 or len({(x.get("case_id"), x.get("identity_id")) for x in queue}) != 150:
        raise ValueError("expected exactly 150 unique disagreement units")

    key_index: dict[tuple[str, str, str], dict] = {}
    for row in key["entries"]:
        ix = (row.get("reviewer"), row.get("case_id"), row.get("identity_id"))
        if ix in key_index or ix[0] not in {"R1", "R2"}:
            raise ValueError("duplicate or invalid private-key identity")
        key_index[ix] = row
    pack_views = {r: {v["view_id"]: v for v in packs[r]["views"]} for r in ("R1", "R2")}
    if any(len(pack_views[r]) != 150 for r in pack_views):
        raise ValueError("issued pack does not contain 150 cases")
    answer_indexes: dict[tuple[str, str, str], str] = {}
    for reviewer in ("R1", "R2"):
        ret = returns[reviewer]
        pack = packs[reviewer]
        expected_indexes = {(reviewer, entry["view_id"], entry["blind_candidate"])
                            for entry in key["entries"] if entry.get("reviewer") == reviewer}
        if (pack.get("reviewer") != reviewer or ret.get("reviewer") != reviewer or
                ret.get("pack_id") != pack.get("pack_id") or ret.get("pack_binding_sha256") != pack_binding(pack) or
                ret.get("complete") is not True or ret.get("expected_judgments") != 867 or
                len(expected_indexes) != 867):
            raise ValueError("return is not bound to its issued pack")
        if len(ret.get("judgments", [])) != 867:
            raise ValueError("fixed judgment inventory mismatch")
        for row in ret["judgments"]:
            idx = (reviewer, row.get("view_id"), row.get("blind_candidate"))
            if (set(row) != {"record_id", "view_id", "blind_candidate", "label"} or
                    row.get("record_id") != f"{reviewer}:{row.get('view_id')}:{row.get('blind_candidate')}" or
                    row.get("label") not in LABELS or idx in answer_indexes or idx not in expected_indexes):
                raise ValueError("invalid or duplicate return row")
            answer_indexes[idx] = row["label"]
        actual_indexes = {idx for idx in answer_indexes if idx[0] == reviewer}
        if actual_indexes != expected_indexes:
            raise ValueError("return does not exactly cover the issued pack/key")

    visible_items, sealed_items = [], []
    seen_ids: set[str] = set()
    for item in sorted(queue, key=lambda x: opaque_item(x["case_id"], x["identity_id"])):
        case_id, identity_id = item["case_id"], item["identity_id"]
        keys = {r: key_index.get((r, case_id, identity_id)) for r in ("R1", "R2")}
        if any(v is None for v in keys.values()):
            raise ValueError("disagreement identity is absent from one issued pack")
        labels, views, candidate_names = {}, {}, {}
        for reviewer, entry in keys.items():
            view = pack_views[reviewer].get(entry.get("view_id"))
            if view is None or entry.get("blind_candidate") not in {c.get("blind_candidate") for c in view.get("candidates", [])}:
                raise ValueError("private identity does not join to issued blind view")
            candidate = next(c for c in view["candidates"] if c["blind_candidate"] == entry["blind_candidate"])
            if candidate.get("display_names") != entry.get("display_names"):
                raise ValueError("candidate naming differs between pack and sealed key")
            labels[reviewer] = answer_indexes.get((reviewer, entry["view_id"], entry["blind_candidate"]))
            views[reviewer] = view
            candidate_names[reviewer] = candidate["display_names"]
        if labels["R1"] not in LABELS or labels["R2"] not in LABELS or labels["R1"] == labels["R2"]:
            raise ValueError("queued unit is not a valid disagreement")
        context = {r: validate_context(views[r]) for r in ("R1", "R2")}
        if context["R1"] != context["R2"] or candidate_names["R1"] != candidate_names["R2"]:
            raise ValueError("case context or candidate identity differs across issued packs")
        item_id = opaque_item(case_id, identity_id)
        if item_id in seen_ids:
            raise ValueError("opaque item ID collision")
        seen_ids.add(item_id)
        title, ingredients, target = context["R1"]
        visible_items.append({
            "item_id": item_id,
            "recipe_title": title,
            "ingredients": list(ingredients),
            "target": target,
            "candidate_names": candidate_names["R1"],
            "unordered_raw_labels": sorted([labels["R1"], labels["R2"]]),
            "final_label": None,
            "rationale": None,
        })
        sealed_items.append({"item_id": item_id, "case_id": case_id, "identity_id": identity_id,
                             "raw_labels_by_rater": labels,
                             "raw_record_refs": {r: {"view_id": keys[r]["view_id"],
                                                     "blind_candidate": keys[r]["blind_candidate"]}
                                                 for r in ("R1", "R2")}})
    packet = {"format": "c1-l36-adjudication-packet-v1", "specification_id": "iron-chef-l36-case-rule-v1",
              "items": visible_items}
    sealed = {"format": "c1-l36-adjudication-key-v1", "items": sealed_items}
    receipt = {"format": "c1-l36-adjudication-preparation-receipt-v1",
               "source_sha256": {name: digest(path) for name, path in paths.items()},
               "item_count": len(visible_items), "adjudicated_values_created": 0,
               "raw_labels_changed": False, "network_or_external_transfer": False,
               "blinding": ["rater identity", "arm", "rank", "recorded-gold identity"],
               "method_decision_status": "HUMAN_METHOD_CONFIRMATION_REQUIRED"}
    return packet, sealed, receipt


def create(path: Path, value: dict) -> str:
    raw = (json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode("utf-8")
    with path.open("xb") as stream:
        stream.write(raw)
        stream.flush()
        os.fsync(stream.fileno())
    return hashlib.sha256(raw).hexdigest()


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    for name in ("private-key", "r1-pack", "r2-pack", "r1-return", "r2-return", "queue", "output-dir"):
        p.add_argument("--" + name, type=Path, required=True)
    args = p.parse_args()
    out = args.output_dir
    out.mkdir(parents=False, exist_ok=False)
    packet, sealed, receipt = prepare(args.private_key, args.r1_pack, args.r2_pack,
                                     args.r1_return, args.r2_return, args.queue)
    receipt["packet_sha256"] = create(out / "adjudication-packet.json", packet)
    receipt["sealed-map_sha256"] = create(out / "sealed-map.json", sealed)
    receipt["receipt_sha256"] = hashlib.sha256(json.dumps(receipt, sort_keys=True).encode("utf-8")).hexdigest()
    create(out / "preparation-receipt.json", receipt)
    print(json.dumps({"state": "PREPARED_PRIVATE_ONLY", "items": len(packet["items"]),
                      "packet_sha256": receipt["packet_sha256"], "sealed_map_sha256": receipt["sealed-map_sha256"],
                      "receipt_sha256": digest(out / "preparation-receipt.json")}, sort_keys=True))


if __name__ == "__main__":
    main()
