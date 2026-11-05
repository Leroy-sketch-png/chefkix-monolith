"""Create a local-only OpenAI Batch JSONL candidate from pinned C2 request bodies.

This tool never uploads or contacts a provider. The only provider-format file
contains Batch request envelopes. The opaque-ID map is separately marked
local-only and must never be uploaded or published.
"""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import os
import re
import secrets
import uuid
from pathlib import Path
from typing import Any, Callable

T208_DIR = Path("ai/out/T208/package-v3")
T208_MANIFEST_SHA256 = "82973520e41a41160eb938ab9921fff64046bfe5cc133b6e05456dae7a8030d1"
T208_BODIES_SHA256 = "7c54ea1b0995263a3e67ad5210454d1c4363604a469b1f238c4a4f471e1e3175"
T208_DISPOSITIONS_SHA256 = "d533439d247e43af696c8bc3ab6963e5fcd095557282d9c1179b2b625c552d9a"
PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
DENOMINATOR = 10_747
READY = 10_499
ERRORS = 248
ENDPOINT = "/v1/responses"
METHOD = "POST"
DOCS = {
    "batch_guide": "https://developers.openai.com/api/docs/guides/batch",
    "batch_create": "https://developers.openai.com/api/reference/resources/batches/methods/create",
    "file_create": "https://developers.openai.com/api/reference/resources/files/methods/create",
    "accessed": "2026-10-23",
}
TOKEN_RE = re.compile(r"^c2_[A-Za-z0-9_-]{43}$")


class CandidateError(ValueError):
    """Sanitized, value-free validation failure."""


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def canonical_json(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def jsonl(records: list[dict[str, Any]]) -> bytes:
    return b"".join(canonical_json(record) + b"\n" for record in records)


def read_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError):
        raise CandidateError("pinned JSON file is unreadable or invalid") from None
    if not isinstance(value, dict):
        raise CandidateError("pinned JSON root is not an object")
    return value


def read_jsonl(path: Path) -> tuple[list[dict[str, Any]], list[bytes]]:
    records: list[dict[str, Any]] = []
    raw_rows: list[bytes] = []
    try:
        with path.open("rb") as stream:
            for raw in stream:
                line = raw[:-1] if raw.endswith(b"\n") else raw
                if not line:
                    raise CandidateError("JSONL contains an empty record")
                value = json.loads(line.decode("utf-8"))
                if not isinstance(value, dict):
                    raise CandidateError("JSONL record is not an object")
                records.append(value)
                raw_rows.append(line)
    except (OSError, UnicodeError, json.JSONDecodeError):
        raise CandidateError("pinned JSONL file is unreadable or invalid") from None
    return records, raw_rows


def opaque_custom_id() -> str:
    token = base64.urlsafe_b64encode(secrets.token_bytes(32)).decode("ascii").rstrip("=")
    return "c2_" + token


def contains_any_reference(text: str, references: list[str]) -> bool:
    """Linear-time multi-pattern scan over provider metadata only (never bodies)."""
    transitions: list[dict[str, int]] = [{}]
    failure = [0]
    terminal = [False]
    for reference in references:
        if not isinstance(reference, str) or not reference:
            raise CandidateError("local reference pattern is invalid")
        state = 0
        for char in reference:
            next_state = transitions[state].get(char)
            if next_state is None:
                next_state = len(transitions)
                transitions[state][char] = next_state
                transitions.append({})
                failure.append(0)
                terminal.append(False)
            state = next_state
        terminal[state] = True

    queue = list(transitions[0].values())
    cursor = 0
    while cursor < len(queue):
        state = queue[cursor]
        cursor += 1
        for char, child in transitions[state].items():
            fallback = failure[state]
            while fallback and char not in transitions[fallback]:
                fallback = failure[fallback]
            failure[child] = transitions[fallback].get(char, 0)
            terminal[child] = terminal[child] or terminal[failure[child]]
            queue.append(child)

    state = 0
    for char in text:
        while state and char not in transitions[state]:
            state = failure[state]
        state = transitions[state].get(char, 0)
        if terminal[state]:
            return True
    return False


def validate_provider_metadata(lines: list[dict[str, Any]], references: list[str]) -> None:
    values: list[str] = []
    seen: set[str] = set()
    for line in lines:
        if set(line) != {"custom_id", "method", "url", "body"}:
            raise CandidateError("Batch line schema differs from documented envelope")
        token = line.get("custom_id")
        if not isinstance(token, str) or not TOKEN_RE.fullmatch(token) or token in seen:
            raise CandidateError("Batch correlation ID is invalid or duplicated")
        if line.get("method") != METHOD or line.get("url") != ENDPOINT:
            raise CandidateError("Batch endpoint metadata differs from frozen contract")
        seen.add(token)
        values.extend((token, METHOD, ENDPOINT))
    metadata_text = "\n".join(values)
    if contains_any_reference(metadata_text, references):
        raise CandidateError("local case identity leaked into provider-facing envelope metadata")


def validate_dispositions(dispositions: list[dict[str, Any]], expected_denominator: int = DENOMINATOR,
                          expected_ready: int = READY, expected_errors: int = ERRORS) -> list[dict[str, Any]]:
    if len(dispositions) != expected_denominator:
        raise CandidateError("disposition denominator differs from frozen count")
    seen_refs: set[str] = set()
    by_ordinal: dict[int, dict[str, Any]] = {}
    ready_count = error_count = 0
    for row in dispositions:
        if not isinstance(row, dict):
            raise CandidateError("disposition row is malformed")
        case_ref = row.get("case_ref")
        status = row.get("status")
        if not isinstance(case_ref, str) or not case_ref or case_ref in seen_refs:
            raise CandidateError("local disposition identity is invalid or duplicated")
        seen_refs.add(case_ref)
        if status == "READY":
            if set(row) != {"case_ref", "status", "request_ordinal"}:
                raise CandidateError("READY disposition has unexpected fields")
            ordinal = row.get("request_ordinal")
            if isinstance(ordinal, bool) or not isinstance(ordinal, int) or ordinal < 0 or ordinal in by_ordinal:
                raise CandidateError("READY request ordinal is invalid or duplicated")
            by_ordinal[ordinal] = row
            ready_count += 1
        elif status == "PREPARATION_ERROR":
            if set(row) != {"case_ref", "status", "reason"} or row.get("reason") != "TARGET_SLOT_MATCH_MULTIPLE":
                raise CandidateError("preparation-error disposition differs from frozen allowlist")
            error_count += 1
        else:
            raise CandidateError("disposition status is not allowlisted")
    if ready_count != expected_ready or error_count != expected_errors or set(by_ordinal) != set(range(expected_ready)):
        raise CandidateError("fixed READY/error totals or ordinal coverage mismatch")
    return [by_ordinal[index] for index in range(expected_ready)]


def serialize_batch(body_records: list[dict[str, Any]], body_raw: list[bytes],
                    dispositions: list[dict[str, Any]],
                    token_factory: Callable[[], str] = opaque_custom_id,
                    *, expected_denominator: int = DENOMINATOR,
                    expected_ready: int = READY, expected_errors: int = ERRORS) -> tuple[bytes, bytes]:
    ready_rows = validate_dispositions(dispositions, expected_denominator, expected_ready, expected_errors)
    if len(body_records) != expected_ready or len(body_raw) != expected_ready:
        raise CandidateError("request-body count differs from frozen READY total")
    batch_lines: list[dict[str, Any]] = []
    mapping_lines: list[dict[str, Any]] = []
    tokens: set[str] = set()
    refs = [row["case_ref"] for row in dispositions]
    model_names: set[str] = set()
    for index, (body, raw, disposition) in enumerate(zip(body_records, body_raw, ready_rows)):
        if canonical_json(body) != raw:
            raise CandidateError("frozen request body bytes are not canonical")
        model = body.get("model")
        if not isinstance(model, str) or not model:
            raise CandidateError("frozen body is missing its model")
        model_names.add(model)
        token = token_factory()
        if not isinstance(token, str) or not TOKEN_RE.fullmatch(token) or token in tokens or token in refs:
            raise CandidateError("generated Batch correlation ID is invalid, non-unique, or non-opaque")
        tokens.add(token)
        batch_lines.append({"custom_id": token, "method": METHOD, "url": ENDPOINT, "body": body})
        mapping_lines.append({"custom_id": token, "case_ref": disposition["case_ref"]})
    if len(model_names) != 1:
        raise CandidateError("Batch file must contain requests for exactly one model")
    provider_bytes = jsonl(batch_lines)
    mapping_bytes = jsonl(mapping_lines)
    validate_provider_metadata(batch_lines, refs)
    return provider_bytes, mapping_bytes


def verify_pinned_inputs(root: Path) -> tuple[dict[str, Any], list[dict[str, Any]], list[bytes], list[dict[str, Any]], list[bytes]]:
    root = root.resolve()
    manifest_path = root / T208_DIR / "manifest.json"
    bodies_path = root / T208_DIR / "request_bodies.jsonl"
    dispositions_path = root / T208_DIR / "dispositions.jsonl"
    if sha256_file(manifest_path) != T208_MANIFEST_SHA256:
        raise CandidateError("T208 manifest pin mismatch")
    if sha256_file(bodies_path) != T208_BODIES_SHA256 or sha256_file(dispositions_path) != T208_DISPOSITIONS_SHA256:
        raise CandidateError("T208 package output pin mismatch")
    manifest = read_json(manifest_path)
    if (manifest.get("schema") != "C2_OFFLINE_REQUEST_BOUNDARY_PACKAGE_V1"
            or manifest.get("protocol_sha256") != PROTOCOL_SHA256
            or manifest.get("counts") != {
                "denominator_cases": DENOMINATOR,
                "ready_cases": READY,
                "preparation_error_cases": ERRORS,
                "preparation_error_reasons": {"TARGET_SLOT_MATCH_MULTIPLE": ERRORS},
                "case_refs_sha256": "8a45f07e855cb5bca06e0a7a10bf1c42ccfd09fed8abadd693f1978229fe7a0d",
            }
            or manifest.get("dispatch_authorized") is not False
            or manifest.get("provider_called") is not False
            or manifest.get("cohort_selected") is not False):
        raise CandidateError("T208 manifest state/count contract mismatch")
    input_inventory = manifest.get("output_inventory", {})
    if set(input_inventory) != {"request_bodies.jsonl", "dispositions.jsonl"}:
        raise CandidateError("T208 output inventory is not exact")
    for name, path in (("request_bodies.jsonl", bodies_path), ("dispositions.jsonl", dispositions_path)):
        entry = input_inventory[name]
        if entry.get("sha256") != sha256_file(path) or entry.get("bytes") != path.stat().st_size:
            raise CandidateError("T208 manifest output inventory mismatch")
    bodies, body_raw = read_jsonl(bodies_path)
    dispositions, disposition_raw = read_jsonl(dispositions_path)
    if jsonl(dispositions) != b"".join(line + b"\n" for line in disposition_raw):
        raise CandidateError("T208 dispositions are not canonical JSONL")
    validate_dispositions(dispositions)
    return manifest, bodies, body_raw, dispositions, disposition_raw


def package_manifest(batch_bytes: bytes, mapping_bytes: bytes, *, builder_sha256: str) -> dict[str, Any]:
    return {
        "schema": "C2_BATCH_TRANSPORT_CANDIDATE_V1",
        "status": "LOCAL_ONLY_NOT_SELECTED_NOT_AUTHORIZED_FOR_UPLOAD",
        "protocol_sha256": PROTOCOL_SHA256,
        "parent_t208_manifest_sha256": T208_MANIFEST_SHA256,
        "parent_request_bodies_sha256": T208_BODIES_SHA256,
        "parent_dispositions_sha256": T208_DISPOSITIONS_SHA256,
        "official_documentation": DOCS,
        "batch_contract": {
            "method": METHOD,
            "url": ENDPOINT,
            "line_keys": ["body", "custom_id", "method", "url"],
            "custom_id": "fresh_random_256_bit_opaque_correlation_token",
            "provider_input_filename": "batch_requests.jsonl",
            "local_mapping_uploadable": False,
        },
        "counts": {"denominator_cases": DENOMINATOR, "ready_requests": READY,
                   "retained_preparation_errors": ERRORS},
        "builder_sha256": builder_sha256,
        "output_inventory": {
            "batch_requests.jsonl": {"bytes": len(batch_bytes), "sha256": sha256_bytes(batch_bytes),
                                      "role": "provider-format-candidate_not_authorized_for_upload"},
            "local_only_mapping.jsonl": {"bytes": len(mapping_bytes), "sha256": sha256_bytes(mapping_bytes),
                                          "role": "private_local_correlation_map_never_upload"},
        },
        "cohort_selected": False,
        "upload_authorized": False,
        "dispatch_authorized": False,
        "provider_called": False,
        "model_input_tokens": None,
        "model_input_token_cost_usd": None,
    }


def write_durable(path: Path, data: bytes) -> None:
    with path.open("xb") as stream:
        stream.write(data)
        stream.flush()
        os.fsync(stream.fileno())


def build(root: Path, output_dir: Path) -> dict[str, Any]:
    if output_dir.exists():
        raise CandidateError("refusing to overwrite existing package output")
    _, bodies, raw, dispositions, _ = verify_pinned_inputs(root)
    batch_bytes, mapping_bytes = serialize_batch(bodies, raw, dispositions)
    builder_hash = sha256_file(Path(__file__).resolve())
    manifest = package_manifest(batch_bytes, mapping_bytes, builder_sha256=builder_hash)
    output_dir.parent.mkdir(parents=True, exist_ok=True)
    temp_dir = output_dir.parent / f".{output_dir.name}.pending-{uuid.uuid4().hex}"
    temp_dir.mkdir()
    write_durable(temp_dir / "batch_requests.jsonl", batch_bytes)
    write_durable(temp_dir / "local_only_mapping.jsonl", mapping_bytes)
    write_durable(temp_dir / "manifest.json", json.dumps(manifest, sort_keys=True, indent=2).encode("utf-8") + b"\n")
    os.rename(temp_dir, output_dir)
    return validate_saved_package(root, output_dir)


def validate_saved_package(root: Path, output_dir: Path) -> dict[str, Any]:
    saved = read_json(output_dir / "manifest.json")
    expected_names = {"batch_requests.jsonl", "local_only_mapping.jsonl"}
    inventory = saved.get("output_inventory")
    if not isinstance(inventory, dict) or set(inventory) != expected_names:
        raise CandidateError("saved output inventory differs from exact package contract")
    for name in expected_names:
        path = output_dir / name
        item = inventory[name]
        if not path.is_file() or item.get("bytes") != path.stat().st_size or item.get("sha256") != sha256_file(path):
            raise CandidateError("saved package output hash/size mismatch")
    _, bodies, raw, dispositions, _ = verify_pinned_inputs(root)
    batch_bytes, mapping_bytes = serialize_batch_from_saved_map(bodies, raw, dispositions,
                                                               output_dir / "local_only_mapping.jsonl")
    batch_path = output_dir / "batch_requests.jsonl"
    if batch_path.read_bytes() != batch_bytes or (output_dir / "local_only_mapping.jsonl").read_bytes() != mapping_bytes:
        raise CandidateError("saved Batch envelope or local map differs from pinned body/order contract")
    expected = package_manifest(batch_bytes, mapping_bytes, builder_sha256=sha256_file(Path(__file__).resolve()))
    if saved != expected:
        raise CandidateError("saved manifest does not match frozen inputs, outputs, docs, and flags")
    return {
        "status": "LOCAL_BATCH_CANDIDATE_VALIDATED_NO_UPLOAD",
        "denominator_cases": DENOMINATOR,
        "ready_requests": READY,
        "retained_preparation_errors": ERRORS,
        "batch_requests_sha256": inventory["batch_requests.jsonl"]["sha256"],
        "local_mapping_sha256": inventory["local_only_mapping.jsonl"]["sha256"],
        "manifest_sha256": sha256_file(output_dir / "manifest.json"),
        "upload_authorized": False,
        "dispatch_authorized": False,
        "provider_called": False,
    }


def serialize_batch_from_saved_map(body_records: list[dict[str, Any]], body_raw: list[bytes],
                                   dispositions: list[dict[str, Any]], mapping_path: Path) -> tuple[bytes, bytes]:
    mapping, mapping_raw = read_jsonl(mapping_path)
    if len(mapping) != READY:
        raise CandidateError("local correlation map count differs from READY total")
    ready = validate_dispositions(dispositions)
    if len(body_records) != READY or len(body_raw) != READY:
        raise CandidateError("pinned request bodies differ from READY total")
    expected_refs = [row["case_ref"] for row in ready]
    all_refs = [row["case_ref"] for row in dispositions]
    token_to_ref: dict[str, str] = {}
    for item in mapping:
        if set(item) != {"custom_id", "case_ref"}:
            raise CandidateError("local map has unexpected fields")
        token, case_ref = item.get("custom_id"), item.get("case_ref")
        if not isinstance(token, str) or not TOKEN_RE.fullmatch(token) or token in token_to_ref:
            raise CandidateError("local map token is invalid or duplicated")
        token_to_ref[token] = case_ref
    if [token_to_ref.get(item.get("custom_id")) for item in mapping] != expected_refs:
        raise CandidateError("local map does not preserve exact frozen request order/identity")
    lines = []
    for index, (body, raw, map_row) in enumerate(zip(body_records, body_raw, mapping)):
        if canonical_json(body) != raw or not isinstance(body.get("model"), str):
            raise CandidateError("pinned body is not canonical or lacks model")
        lines.append({"custom_id": map_row["custom_id"], "method": METHOD, "url": ENDPOINT, "body": body})
    validate_provider_metadata(lines, all_refs)
    batch_bytes = jsonl(lines)
    return batch_bytes, jsonl(mapping)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--build", action="store_true")
    mode.add_argument("--validate", action="store_true")
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()
    try:
        result = build(args.root, args.output_dir) if args.build else validate_saved_package(args.root, args.output_dir)
    except (OSError, CandidateError, json.JSONDecodeError) as exc:
        parser.exit(1, f"T253 stopped: {exc}\n")
    print(json.dumps(result, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
