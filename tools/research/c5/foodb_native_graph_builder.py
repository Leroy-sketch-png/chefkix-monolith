"""Build a private, deterministic FooDB-native Food->Compound graph snapshot.

The output is a local research artifact, not a publication bundle or an accepted
ingredient/allergen graph. Only source-native IDs, typed relation support, and
the existing export-eligibility predicate are emitted. Source names and unused
JSON values are structurally skipped rather than decoded by the row parser.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import defaultdict
from pathlib import Path
from typing import Any, Iterable

PINNED_ARCHIVE_SHA256 = "3a37305b2a46d7fbd41969bdc5c3f9233bd837cde7c6f46e34316d08879a999c"
PINNED_PROFILE_SHA256 = "8490cd6adadc5bad0e59f5f4edd8d05020b941b987ca47e38ac0cc80d85a04ac"
PINNED_SIDECAR_SHA256 = "5117b791bd825b5c7c87f4f816342ff4bc65100b74502a8652157a989ed9d208"
PINNED_MEMBER_SHA256 = {
    "foodb_2020_04_07_json/Food.json": "c8a0bcd193ea37b11f7a2e5ade6306fe27309a4bda2e390924548483ec7c881e",
    "foodb_2020_04_07_json/Compound.json": "c0ae95bfb179336299fb2d3e54b49336abb86cb01af4aafc65495f6f926de072",
    "foodb_2020_04_07_json/Content.json": "d0b9b151d54ec5491b53a1b2166d2a3e2d93c111953c82e828778a8c496968ed",
}
EXPECTED = {
    "profile_compound_ids": 206,
    "food_rows": 992,
    "compound_rows": 70477,
    "unique_pairs": 125992,
    "export_eligible_pairs": 107496,
    "not_export_eligible_pairs": 18496,
    "profile_food_names": 971,
}
MEMBERS = {
    "foodb_2020_04_07_json/Food.json": {"id", "public_id", "export_to_foodb"},
    "foodb_2020_04_07_json/Compound.json": {"id"},
    "foodb_2020_04_07_json/Content.json": {"food_id", "source_type", "source_id", "export"},
}


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _skip_string(text: str, start: int) -> int:
    if start >= len(text) or text[start] != '"':
        raise ValueError("expected JSON string")
    i = start + 1
    valid_escapes = {'"', "\\", "/", "b", "f", "n", "r", "t"}
    while i < len(text):
        char = text[i]
        if char == '"':
            return i + 1
        if ord(char) < 0x20:
            raise ValueError("unescaped control in JSON string")
        if char == "\\":
            i += 1
            if i >= len(text):
                break
            if text[i] == "u":
                if i + 4 >= len(text) or not re.fullmatch(r"[0-9a-fA-F]{4}", text[i + 1 : i + 5]):
                    raise ValueError("invalid JSON unicode escape")
                i += 5
                continue
            if text[i] not in valid_escapes:
                raise ValueError("invalid JSON escape")
        i += 1
    raise ValueError("unterminated JSON string")


def _skip_value(text: str, start: int) -> int:
    """Validate and skip one JSON value without decoding nested values."""
    def ws(index: int) -> int:
        while index < len(text) and text[index] in " \t\r\n":
            index += 1
        return index

    def value(index: int) -> int:
        index = ws(index)
        if index >= len(text):
            raise ValueError("missing JSON value")
        char = text[index]
        if char == '"':
            return _skip_string(text, index)
        if char == "{":
            index = ws(index + 1)
            if index < len(text) and text[index] == "}":
                return index + 1
            seen: set[str] = set()
            decoder = json.JSONDecoder()
            while True:
                index = ws(index)
                key, after = decoder.raw_decode(text, index)
                if not isinstance(key, str) or key in seen:
                    raise ValueError("invalid or duplicate JSON key")
                seen.add(key)
                index = ws(after)
                if index >= len(text) or text[index] != ":":
                    raise ValueError("missing JSON object colon")
                index = value(index + 1)
                index = ws(index)
                if index < len(text) and text[index] == "}":
                    return index + 1
                if index >= len(text) or text[index] != ",":
                    raise ValueError("invalid JSON object separator")
                index += 1
        if char == "[":
            index = ws(index + 1)
            if index < len(text) and text[index] == "]":
                return index + 1
            while True:
                index = value(index)
                index = ws(index)
                if index < len(text) and text[index] == "]":
                    return index + 1
                if index >= len(text) or text[index] != ",":
                    raise ValueError("invalid JSON array separator")
                index += 1
        for literal in ("true", "false", "null"):
            if text.startswith(literal, index):
                end = index + len(literal)
                if end == len(text) or text[end] in " \t\r\n,]}":
                    return end
        number = re.match(r"-?(?:0|[1-9][0-9]*)(?:\.[0-9]+)?(?:[eE][+-]?[0-9]+)?", text[index:])
        if number:
            return index + len(number.group(0))
        raise ValueError("invalid JSON token")

    return value(start)


def _reject_non_json_constant(_token: str) -> None:
    raise ValueError("invalid non-JSON constant")


def project_object(text: str, allowed: set[str]) -> dict[str, Any]:
    decoder = json.JSONDecoder(parse_constant=_reject_non_json_constant)
    i = 0
    while i < len(text) and text[i] in " \t\r\n":
        i += 1
    if i >= len(text) or text[i] != "{":
        raise ValueError("expected JSON object")
    i += 1
    result: dict[str, Any] = {}
    seen: set[str] = set()
    while True:
        while i < len(text) and text[i] in " \t\r\n":
            i += 1
        if i >= len(text):
            raise ValueError("unterminated JSON object")
        if text[i] == "}":
            i += 1
            break
        key, after = decoder.raw_decode(text, i)
        if not isinstance(key, str) or key in seen:
            raise ValueError("invalid or duplicate JSON object key")
        seen.add(key)
        i = after
        while i < len(text) and text[i] in " \t\r\n":
            i += 1
        if i >= len(text) or text[i] != ":":
            raise ValueError("missing JSON object colon")
        i += 1
        while i < len(text) and text[i] in " \t\r\n":
            i += 1
        if key in allowed:
            value, i = decoder.raw_decode(text, i)
            result[key] = value
        else:
            i = _skip_value(text, i)
        while i < len(text) and text[i] in " \t\r\n":
            i += 1
        if i < len(text) and text[i] == "}":
            i += 1
            break
        if i >= len(text) or text[i] != ",":
            raise ValueError("invalid JSON object separator")
        i += 1
    if any(char not in " \t\r\n" for char in text[i:]):
        raise ValueError("trailing JSON content")
    return result


def extract_profile_scope(
    profile_path: Path,
    expected_profile_names: int | None = EXPECTED["profile_food_names"],
    expected_compound_ids: int | None = EXPECTED["profile_compound_ids"],
    expected_pairs: int | None = EXPECTED["unique_pairs"],
) -> tuple[set[int], set[tuple[int, str]], int]:
    """Read compound/public IDs; use profile names only to detect duplicate keys."""
    text = profile_path.read_text(encoding="utf-8")
    decoder = json.JSONDecoder()
    i = 0
    def ws(index: int) -> int:
        while index < len(text) and text[index] in " \t\r\n":
            index += 1
        return index

    i = ws(i)
    if i >= len(text) or text[i] != "{":
        raise ValueError("profile artifact must be an object")
    i = ws(i + 1)
    ids: set[int] = set()
    pairs: set[tuple[int, str]] = set()
    seen_profile_keys: set[str] = set()
    if i < len(text) and text[i] == "}":
        raise ValueError("profile artifact is empty")
    while True:
        i = ws(i)
        after_key = _skip_string(text, i)
        key = decoder.decode(text[i:after_key])
        if key in seen_profile_keys:
            raise ValueError("duplicate top-level profile key")
        seen_profile_keys.add(key)
        i = ws(after_key)
        if i >= len(text) or text[i] != ":":
            raise ValueError("invalid profile object")
        i = ws(i + 1)
        if i >= len(text) or text[i] != "[":
            raise ValueError("profile value must be a list")
        i = ws(i + 1)
        if i < len(text) and text[i] != "]":
            while True:
                i = ws(i)
                end = _skip_value(text, i)
                row = project_object(text[i:end], {"compound_id", "food_public_ids"})
                compound_id = row.get("compound_id")
                if type(compound_id) is int and compound_id > 0:
                    ids.add(compound_id)
                    public_ids = row.get("food_public_ids")
                    if not isinstance(public_ids, list) or any(not isinstance(value, str) or not value for value in public_ids):
                        raise ValueError("invalid profile food public IDs")
                    pairs.update((compound_id, public_id) for public_id in public_ids)
                elif compound_id is not None:
                    raise ValueError("invalid profile compound ID")
                i = ws(end)
                if i < len(text) and text[i] == "]":
                    break
                if i >= len(text) or text[i] != ",":
                    raise ValueError("invalid profile list separator")
                i += 1
        i = ws(i + 1)
        if i < len(text) and text[i] == "}":
            i = ws(i + 1)
            break
        if i >= len(text) or text[i] != ",":
            raise ValueError("invalid profile object separator")
        i += 1
    if i != len(text):
        raise ValueError("trailing profile JSON")
    profile_names = len(seen_profile_keys)
    if (expected_profile_names is not None and profile_names != expected_profile_names) or (
        expected_compound_ids is not None and len(ids) != expected_compound_ids
    ):
        raise ValueError("profile inventory differs from pinned scope")
    if expected_pairs is not None and len(pairs) != expected_pairs:
        raise ValueError("profile Food-public-ID pair inventory differs from pinned scope")
    return ids, pairs, profile_names


def _iter_member(member_path: Path, member_label: str, allowed: set[str], digest: Any) -> Iterable[dict[str, Any]]:
    with member_path.open("rb") as stream:
        for number, raw in enumerate(stream, 1):
            digest.update(raw)
            if not raw.strip():
                continue
            try:
                yield project_object(raw.decode("utf-8"), allowed)
            except (UnicodeDecodeError, json.JSONDecodeError, ValueError) as exc:
                raise ValueError(f"invalid projected row at {member_label} line {number}") from exc


def _require_int(value: Any, field: str) -> int:
    if type(value) is not int or value <= 0:
        raise ValueError(f"invalid {field}")
    return value


def require_profile_ids_in_compounds(profile_ids: set[int], compound_ids: set[int]) -> None:
    if not profile_ids.issubset(compound_ids):
        raise ValueError("profile compound IDs do not resolve to source Compound.id")


def aggregate_rows(
    food_rows: Iterable[dict[str, Any]],
    compound_rows: Iterable[dict[str, Any]],
    content_rows: Iterable[dict[str, Any]],
    profile_ids: set[int],
    profile_public_pairs: set[tuple[int, str]],
) -> tuple[dict[int, bool], set[int], dict[tuple[int, int], list[int]], dict[str, int]]:
    """Form exact typed pairs and support counts; no normalized names are accepted."""
    foods: dict[int, bool] = {}
    wanted_public_ids = {public_id for _compound_id, public_id in profile_public_pairs}
    food_public_ids: dict[str, int] = {}
    for row in food_rows:
        food_id = _require_int(row.get("id"), "Food.id")
        public_id = row.get("public_id")
        if food_id in foods or type(row.get("export_to_foodb")) is not bool:
            raise ValueError("duplicate Food ID or invalid public-export flag")
        if isinstance(public_id, str) and public_id in wanted_public_ids:
            if public_id in food_public_ids:
                raise ValueError("duplicate selected Food.public_id")
            food_public_ids[public_id] = food_id
        foods[food_id] = row["export_to_foodb"]
    compounds: set[int] = set()
    for row in compound_rows:
        compound_id = _require_int(row.get("id"), "Compound.id")
        if compound_id in compounds:
            raise ValueError("duplicate Compound.id")
        compounds.add(compound_id)
    if not profile_ids.issubset(compounds):
        raise ValueError("profile compound IDs do not resolve to source Compound.id")
    expected_pairs: set[tuple[int, int]] = set()
    for compound_id, public_id in profile_public_pairs:
        if compound_id not in profile_ids or public_id not in food_public_ids:
            raise ValueError("profile public-ID pair does not resolve to source Food.public_id and Compound.id")
        expected_pairs.add((food_public_ids[public_id], compound_id))
    if len(expected_pairs) != len(profile_public_pairs):
        raise ValueError("profile public-ID pairs do not resolve uniquely")
    edges: dict[tuple[int, int], list[int]] = {}
    selected_content_rows = 0
    for row in content_rows:
        if row.get("source_type") != "Compound":
            continue
        source_id = row.get("source_id")
        if type(source_id) is not int or source_id not in profile_ids:
            continue
        food_id = _require_int(row.get("food_id"), "Content.food_id")
        if food_id not in foods:
            # A compound can have unrelated Food rows outside the frozen profile scope.
            continue
        if (food_id, source_id) not in expected_pairs:
            continue
        support = edges.setdefault((food_id, source_id), [0, 0])
        support[0] += 1
        selected_content_rows += 1
        if type(row.get("export")) is int and row["export"] == 1 and foods[food_id]:
            support[1] += 1
    if set(edges) != expected_pairs:
        raise ValueError("one or more frozen profile pairs lack typed Content support")
    return foods, compounds, edges, {
        "food_rows": len(foods),
        "compound_rows": len(compounds),
        "selected_content_rows": selected_content_rows,
    }


def _write_jsonl(path: Path, rows: Iterable[dict[str, Any]]) -> str:
    digest = hashlib.sha256()
    with path.open("xb") as stream:
        for row in rows:
            raw = (json.dumps(row, sort_keys=True, separators=(",", ":"), ensure_ascii=True) + "\n").encode("utf-8")
            stream.write(raw)
            digest.update(raw)
    return digest.hexdigest()


def _seal_manifest(manifest: dict[str, Any]) -> tuple[dict[str, Any], bytes]:
    """Bind the persisted manifest to its deterministic payload (excluding the digest field)."""
    payload = (json.dumps(manifest, sort_keys=True, indent=2, ensure_ascii=True) + "\n").encode("utf-8")
    sealed = dict(manifest)
    sealed["manifest_payload_sha256"] = hashlib.sha256(payload).hexdigest()
    encoded = (json.dumps(sealed, sort_keys=True, indent=2, ensure_ascii=True) + "\n").encode("utf-8")
    return sealed, encoded


def build_graph(archive_path: Path, profile_path: Path, sidecar_path: Path, output_dir: Path) -> dict[str, Any]:
    if not output_dir.is_dir() or any(output_dir.iterdir()):
        raise ValueError("output directory must exist and be empty")
    inputs = {
        "archive_sha256": sha256_file(archive_path),
        "profile_sha256": sha256_file(profile_path),
        "sidecar_sha256": sha256_file(sidecar_path),
    }
    if inputs != {
        "archive_sha256": PINNED_ARCHIVE_SHA256,
        "profile_sha256": PINNED_PROFILE_SHA256,
        "sidecar_sha256": PINNED_SIDECAR_SHA256,
    }:
        raise ValueError("pinned source/profile/sidecar hash mismatch")
    provenance = json.loads(sidecar_path.read_text(encoding="utf-8"))
    if provenance.get("archive_sha256") != inputs["archive_sha256"] or provenance.get("data_sha256") != inputs["profile_sha256"]:
        raise ValueError("provenance sidecar does not bind the pinned inputs")
    profile_ids, profile_public_pairs, profile_names = extract_profile_scope(profile_path)
    counts: dict[str, int] = defaultdict(int)
    member_hashes: dict[str, str] = {}
    source_dir = archive_path.with_suffix("")
    member_paths = {name: source_dir / Path(name).name for name in MEMBERS}
    if not source_dir.is_dir() or any(not path.is_file() for path in member_paths.values()):
        raise ValueError("hash-pinned extracted FooDB member directory is incomplete")
    digest = hashlib.sha256()
    food_rows = list(_iter_member(member_paths["foodb_2020_04_07_json/Food.json"], "Food.json", MEMBERS["foodb_2020_04_07_json/Food.json"], digest))
    counts["food_rows"] = len(food_rows)
    member_hashes["Food.json"] = digest.hexdigest()
    if member_hashes["Food.json"] != PINNED_MEMBER_SHA256["foodb_2020_04_07_json/Food.json"]:
        raise ValueError("Food.json digest mismatch")

    digest = hashlib.sha256()
    compound_rows = list(_iter_member(member_paths["foodb_2020_04_07_json/Compound.json"], "Compound.json", MEMBERS["foodb_2020_04_07_json/Compound.json"], digest))
    counts["compound_rows"] = len(compound_rows)
    member_hashes["Compound.json"] = digest.hexdigest()
    if member_hashes["Compound.json"] != PINNED_MEMBER_SHA256["foodb_2020_04_07_json/Compound.json"]:
        raise ValueError("Compound.json digest mismatch")
    compound_id_rows = [_require_int(row.get("id"), "Compound.id") for row in compound_rows]
    if len(compound_id_rows) != len(set(compound_id_rows)):
        raise ValueError("duplicate Compound.id")
    compound_ids = set(compound_id_rows)
    require_profile_ids_in_compounds(profile_ids, compound_ids)

    digest = hashlib.sha256()
    content_rows = _iter_member(member_paths["foodb_2020_04_07_json/Content.json"], "Content.json", MEMBERS["foodb_2020_04_07_json/Content.json"], digest)
    foods, compounds, edges, aggregate_counts = aggregate_rows(food_rows, compound_rows, content_rows, profile_ids, profile_public_pairs)
    counts.update(aggregate_counts)
    # Parse the exact extracted member once while binding it to the pinned archive sidecar.
    member_hashes["Content.json"] = digest.hexdigest()
    if member_hashes["Content.json"] != PINNED_MEMBER_SHA256["foodb_2020_04_07_json/Content.json"]:
        raise ValueError("Content.json digest mismatch")

    if counts["food_rows"] != EXPECTED["food_rows"] or counts["compound_rows"] != EXPECTED["compound_rows"]:
        raise ValueError("source table row count differs from pinned provenance")
    if profile_names != EXPECTED["profile_food_names"]:
        raise ValueError("profile identity count differs from pinned provenance")
    if len(edges) != EXPECTED["unique_pairs"]:
        raise ValueError("native pair count differs from pinned source evidence")
    eligible = sum(1 for support in edges.values() if support[1] > 0)
    noneligible = len(edges) - eligible
    if (eligible, noneligible) != (EXPECTED["export_eligible_pairs"], EXPECTED["not_export_eligible_pairs"]):
        raise ValueError("export-eligibility partition differs from pinned source evidence")
    if len(profile_ids) != EXPECTED["profile_compound_ids"]:
        raise ValueError("selected compound node count differs from pinned profile")

    food_nodes = {food_id for food_id, _compound_id in edges}
    nodes = [
        {"node_id": f"foodb:1.0:food-id:{food_id}", "node_type": "Food", "export_to_foodb": foods[food_id]}
        for food_id in sorted(food_nodes)
    ] + [
        {"node_id": f"foodb:1.0:compound-id:{compound_id}", "node_type": "Compound"}
        for compound_id in sorted(profile_ids)
    ]
    edge_rows = [
        {
            "source": f"foodb:1.0:food-id:{food_id}",
            "predicate": "has_compound",
            "target": f"foodb:1.0:compound-id:{compound_id}",
            "presence": "PRESENT",
            "content_row_support": support[0],
            "export_eligible_support": support[1],
            "export_filter_state": "EXPORT_ELIGIBLE" if support[1] > 0 else "PRESENT_NOT_EXPORT_ELIGIBLE",
        }
        for (food_id, compound_id), support in sorted(edges.items())
    ]
    nodes_hash = _write_jsonl(output_dir / "nodes.jsonl", nodes)
    edges_hash = _write_jsonl(output_dir / "edges.jsonl", edge_rows)
    manifest = {
        "schema": "chefkix.graph_product_v4.foodb_native_snapshot.v1",
        "release": "FooDB 1.0 JSON 2020-04-07",
        "license_declared_by_source_sidecar": "CC BY-NC 4.0",
        "usage_boundary": "private, local, noncommercial; no publicity or redistribution",
        "destination_graph": "graph_product_v4",
        "source_relation": "profile.food_public_ids->Food.public_id->Food.id; profile.compound_id->Compound.id; Content.source_type=Compound; Content.food_id->Food.id; Content.source_id->Compound.id",
        "selection_basis": "the exact distinct (profile compound_id, food_public_id) pairs in the hash-pinned profile artifact, joined through Food.public_id and validated against typed Content; its sidecar reports 255 top Content source IDs and 49 dangling IDs; rank selection is not recomputed here",
        "semantics": "presence only; no concentration, equivalence, flavor, nutrient, allergen, or safety inference",
        "absence_semantics": "not represented; missing relations remain UNKNOWN",
        "inputs": {**inputs, "archive_members_sha256": dict(sorted(member_hashes.items()))},
        "outputs": {"nodes.jsonl": nodes_hash, "edges.jsonl": edges_hash},
        "counts": {
            "food_source_rows": counts["food_rows"],
            "compound_source_rows": counts["compound_rows"],
            "profile_identity_count": profile_names,
            "selected_resolved_compound_nodes": len(profile_ids),
            "food_nodes_with_selected_edges": len(food_nodes),
            "unique_native_food_compound_pairs": len(edges),
            "export_eligible_pairs": eligible,
            "present_not_export_eligible_pairs": noneligible,
            "selected_content_row_support": counts["selected_content_rows"],
        },
        "builder_sha256": sha256_file(Path(__file__)),
    }
    manifest, manifest_bytes = _seal_manifest(manifest)
    (output_dir / "manifest.json").write_bytes(manifest_bytes)
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--archive", type=Path, required=True)
    parser.add_argument("--profile", type=Path, required=True)
    parser.add_argument("--sidecar", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    result = build_graph(args.archive, args.profile, args.sidecar, args.output_dir)
    print(json.dumps({"state": "PRIVATE_NATIVE_GRAPH_BUILT", "manifest_payload_sha256": result["manifest_payload_sha256"], "counts": result["counts"]}, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
