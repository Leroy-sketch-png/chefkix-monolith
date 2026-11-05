#!/usr/bin/env python3
"""Prepare a local-only Responses input-token-count package from T208 bodies.

This tool performs no network, provider, account, upload, or dispatch action.
It makes a minimal local projection of the frozen generation body's
model/input/structured-output fields for input-token-count preparation. Those
fields are not an exhaustive statement of the endpoint's accepted schema, and
the projection is not byte-identical replay of a generation request. The
case-reference map stays separate from the request JSONL. Output includes
source recipe text and must remain local until transfer, account, cost, and
dispatch gates are independently cleared.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any, Iterable

try:
    from . import build_senath_c2_offline_request_package as _request_package
except ImportError:  # direct script execution from ai/tools/
    import build_senath_c2_offline_request_package as _request_package

# Reuse T208's hash-verified frozen serializer; never bind this consumer to
# the mutable working-tree serializer path.
serializer = _request_package.serializer


PROTOCOL_SHA256 = "b295259909c72a5c3473a117b4711af829b84949a60004ce21c963721b6d5c12"
T208_PACKAGE_SHA256 = "82973520e41a41160eb938ab9921fff64046bfe5cc133b6e05456dae7a8030d1"
T208_REQUEST_BODIES_SHA256 = "7c54ea1b0995263a3e67ad5210454d1c4363604a469b1f238c4a4f471e1e3175"
T208_DISPOSITIONS_SHA256 = "d533439d247e43af696c8bc3ab6963e5fcd095557282d9c1179b2b625c552d9a"
SERIALIZER_SHA256 = "4c5694f378d5b3ae6b1bda6e0733a25926dffaaa235e6ff2292f1b3d9385d829"
EXPECTED_DENOMINATOR = 10_747
EXPECTED_READY = 10_499
EXPECTED_ERRORS = 248
EXPECTED_ERROR_REASONS = {"TARGET_SLOT_MATCH_MULTIPLE": EXPECTED_ERRORS}
COUNT_ENDPOINT = "https://api.openai.com/v1/responses/input_tokens"
GENERATION_FIELDS = {"model", "input", "temperature", "max_output_tokens", "store", "text"}
COUNT_FIELDS = {"model", "input", "text"}
PAYLOAD_FIELDS = {"recipe_title", "ingredient_list", "ingredient_to_replace"}


class PackageError(ValueError):
    """Raised when pinned package structure or projection invariants fail."""


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _unique_object(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise PackageError(f"duplicate JSON key: {key}")
        result[key] = value
    return result


def parse_json_object(line: str, label: str) -> dict[str, Any]:
    try:
        value = json.loads(line, object_pairs_hook=_unique_object)
    except (json.JSONDecodeError, PackageError) as exc:
        raise PackageError(f"{label} is invalid JSON") from exc
    if not isinstance(value, dict):
        raise PackageError(f"{label} must be a JSON object")
    return value


def canonical_json(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, separators=(",", ":"), sort_keys=True).encode("utf-8")


def _validate_frozen_payload(serialized: str, line_no: int) -> None:
    payload = parse_json_object(serialized, f"request line {line_no} user content")
    if set(payload) != PAYLOAD_FIELDS:
        raise PackageError(f"request line {line_no} user payload differs from the three-field allowlist")
    title = payload["recipe_title"]
    target = payload["ingredient_to_replace"]
    slots = payload["ingredient_list"]
    if not isinstance(title, str) or not title.strip() or not isinstance(target, str) or not target.strip():
        raise PackageError(f"request line {line_no} has an empty source field")
    if not isinstance(slots, list) or not slots:
        raise PackageError(f"request line {line_no} ingredient slots are invalid")
    if any(not isinstance(slot, list) or not slot or any(not isinstance(alias, str) or not alias.strip() for alias in slot) for slot in slots):
        raise PackageError(f"request line {line_no} alias-slot structure is invalid")
    if sum(any(alias == target for alias in slot) for slot in slots) != 1:
        raise PackageError(f"request line {line_no} exact target must identify one alias slot")


def project_generation_body(body: dict[str, Any], line_no: int = 1) -> dict[str, Any]:
    """Return a minimal token-count preparation projection, preserving inputs."""
    if set(body) != GENERATION_FIELDS:
        raise PackageError(f"request line {line_no} generation envelope differs from frozen serializer fields")
    if body.get("model") != serializer.MODEL:
        raise PackageError(f"request line {line_no} model differs from the frozen protocol")
    if body.get("temperature") != 0 or isinstance(body.get("temperature"), bool):
        raise PackageError(f"request line {line_no} temperature differs from frozen generation bytes")
    if body.get("max_output_tokens") != 256 or isinstance(body.get("max_output_tokens"), bool):
        raise PackageError(f"request line {line_no} output cap differs from frozen generation bytes")
    if body.get("store") is not False:
        raise PackageError(f"request line {line_no} storage setting differs from frozen generation bytes")

    messages = body.get("input")
    if not isinstance(messages, list) or len(messages) != 2:
        raise PackageError(f"request line {line_no} must contain the frozen system/user message pair")
    if any(not isinstance(item, dict) or set(item) != {"role", "content"} or not isinstance(item["content"], str) for item in messages):
        raise PackageError(f"request line {line_no} input message schema is invalid")
    if [item["role"] for item in messages] != ["system", "user"]:
        raise PackageError(f"request line {line_no} message order/roles changed")
    _validate_frozen_payload(messages[1]["content"], line_no)

    expected_text = {"format": serializer.response_schema()}
    if body.get("text") != expected_text:
        raise PackageError(f"request line {line_no} structured-output schema differs from the frozen serializer")

    projected = {"model": body["model"], "input": body["input"], "text": body["text"]}
    if set(projected) != COUNT_FIELDS:
        raise PackageError(f"request line {line_no} count-endpoint field projection is invalid")
    return projected


def _read_jsonl(path: Path, label: str) -> list[dict[str, Any]]:
    try:
        text = path.read_text(encoding="utf-8")
    except (OSError, UnicodeDecodeError) as exc:
        raise PackageError(f"cannot read {label} as UTF-8") from exc
    lines = text.splitlines()
    if not lines or any(not line.strip() for line in lines):
        raise PackageError(f"{label} is empty or contains blank records")
    return [parse_json_object(line, f"{label} line {index}") for index, line in enumerate(lines, 1)]


def validate_dispositions(
    dispositions: list[dict[str, Any]],
    *,
    expected_ready: int = EXPECTED_READY,
    expected_denominator: int = EXPECTED_DENOMINATOR,
    expected_error_reasons: dict[str, int] | None = None,
) -> tuple[list[dict[str, Any]], dict[str, int]]:
    expected_error_reasons = expected_error_reasons if expected_error_reasons is not None else EXPECTED_ERROR_REASONS
    local_map: list[dict[str, Any]] = []
    ready_ordinals: list[int] = []
    error_reasons: dict[str, int] = {}
    seen_refs: set[str] = set()
    for line_no, disposition in enumerate(dispositions, 1):
        case_ref = disposition.get("case_ref")
        status = disposition.get("status")
        ordinal = disposition.get("request_ordinal")
        if not isinstance(case_ref, str) or not case_ref or case_ref in seen_refs:
            raise PackageError(f"disposition line {line_no} has an empty or duplicate local identity")
        seen_refs.add(case_ref)
        if status == "READY":
            if set(disposition) != {"case_ref", "request_ordinal", "status"}:
                raise PackageError(f"READY disposition line {line_no} has unexpected fields")
            if isinstance(ordinal, bool) or not isinstance(ordinal, int) or ordinal < 0:
                raise PackageError(f"READY disposition line {line_no} lacks a valid request ordinal")
            ready_ordinals.append(ordinal)
            local_map.append({"case_ref": case_ref, "request_ordinal": ordinal})
        elif status == "PREPARATION_ERROR":
            if set(disposition) != {"case_ref", "reason", "status"}:
                raise PackageError(f"preparation-error disposition line {line_no} has unexpected fields")
            reason = disposition.get("reason")
            if not isinstance(reason, str) or not reason.strip():
                raise PackageError(f"preparation-error disposition line {line_no} lacks a reason")
            error_reasons[reason] = error_reasons.get(reason, 0) + 1
            if ordinal is not None:
                raise PackageError(f"preparation-error line {line_no} unexpectedly references a request")
        else:
            raise PackageError(f"disposition line {line_no} has an unsupported status")
    if (
        len(local_map) != expected_ready
        or len(seen_refs) != expected_denominator
        or sorted(ready_ordinals) != list(range(expected_ready))
        or error_reasons != expected_error_reasons
    ):
        raise PackageError("full denominator, READY/error split, or contiguous request ordinals are invalid")
    return local_map, error_reasons


def validate_parent(manifest_path: Path, bodies_path: Path, dispositions_path: Path) -> dict[str, Any]:
    if sha256_file(manifest_path) != T208_PACKAGE_SHA256:
        raise PackageError("T208 package manifest SHA-256 mismatch")
    if sha256_file(bodies_path) != T208_REQUEST_BODIES_SHA256:
        raise PackageError("T208 request-body SHA-256 mismatch")
    if sha256_file(dispositions_path) != T208_DISPOSITIONS_SHA256:
        raise PackageError("T208 disposition SHA-256 mismatch")
    if sha256_file(Path(serializer.__file__).resolve()) != SERIALIZER_SHA256:
        raise PackageError("canonical T208 serializer SHA-256 mismatch")

    manifest = parse_json_object(manifest_path.read_text(encoding="utf-8"), "T208 package manifest")
    counts = manifest.get("counts")
    inventory = manifest.get("output_inventory")
    request_inventory = inventory.get("request_bodies.jsonl") if isinstance(inventory, dict) else None
    disposition_inventory = inventory.get("dispositions.jsonl") if isinstance(inventory, dict) else None
    if (
        manifest.get("protocol_sha256") != PROTOCOL_SHA256
        or manifest.get("serializer_source_sha256") != SERIALIZER_SHA256
        or manifest.get("status") != "LOCAL_ONLY_NOT_SELECTED_NOT_AUTHORIZED_FOR_DISPATCH"
        or manifest.get("provider_called") is not False
        or manifest.get("dispatch_authorized") is not False
        or manifest.get("cohort_selected") is not False
        or manifest.get("model_input_tokens") is not None
        or manifest.get("model_input_token_cost_usd") is not None
        or not isinstance(counts, dict)
        or counts.get("denominator_cases") != EXPECTED_DENOMINATOR
        or counts.get("ready_cases") != EXPECTED_READY
        or counts.get("preparation_error_cases") != EXPECTED_ERRORS
        or not isinstance(inventory, dict)
        or set(inventory) != {"request_bodies.jsonl", "dispositions.jsonl"}
        or not isinstance(request_inventory, dict)
        or request_inventory.get("sha256") != T208_REQUEST_BODIES_SHA256
        or request_inventory.get("bytes") != bodies_path.stat().st_size
        or not isinstance(disposition_inventory, dict)
        or disposition_inventory.get("sha256") != T208_DISPOSITIONS_SHA256
        or disposition_inventory.get("bytes") != dispositions_path.stat().st_size
    ):
        raise PackageError("T208 manifest state/counts do not match the frozen local-only parent package")
    return manifest


def build_package(manifest_path: Path, bodies_path: Path, dispositions_path: Path, output_dir: Path) -> dict[str, Any]:
    repo_root = Path(__file__).resolve().parents[2]
    allowed_root = (repo_root / "ai" / "out" / "T419").resolve()
    resolved_output = output_dir.resolve()
    if allowed_root not in resolved_output.parents or resolved_output.exists():
        raise PackageError("output must be a new dedicated child directory under ai/out/T419")
    if sha256_file(Path(serializer.__file__).resolve()) != SERIALIZER_SHA256:
        raise PackageError("canonical serializer is not the exact frozen T208 source")

    validate_parent(manifest_path, bodies_path, dispositions_path)
    bodies = _read_jsonl(bodies_path, "T208 request bodies")
    dispositions = _read_jsonl(dispositions_path, "T208 dispositions")
    if len(bodies) != EXPECTED_READY or len(dispositions) != EXPECTED_DENOMINATOR:
        raise PackageError("T208 source record counts differ from frozen 10,499/10,747 contract")

    local_map, error_reasons = validate_dispositions(dispositions)

    projected_lines: list[bytes] = []
    body_hashes: list[str] = []
    for index, body in enumerate(bodies):
        projected = project_generation_body(body, index + 1)
        serialized = canonical_json(projected)
        projected_lines.append(serialized)
        body_hashes.append(sha256_bytes(serialized))
    map_by_ordinal = {row["request_ordinal"]: row for row in local_map}
    if set(map_by_ordinal) != set(range(EXPECTED_READY)):
        raise PackageError("local case map does not cover every request ordinal exactly once")
    for ordinal, row in map_by_ordinal.items():
        row["count_request_sha256"] = body_hashes[ordinal]

    request_bytes = b"\n".join(projected_lines) + b"\n"
    map_bytes = b"\n".join(canonical_json(row) for row in sorted(local_map, key=lambda item: item["request_ordinal"])) + b"\n"
    resolved_output.mkdir(parents=True)
    request_path = resolved_output / "input_token_count_requests.jsonl"
    map_path = resolved_output / "DO_NOT_UPLOAD_local_case_map.jsonl"
    request_path.write_bytes(request_bytes)
    map_path.write_bytes(map_bytes)
    receipt = {
        "schema_version": 1,
        "status": "LOCAL_ONLY_COUNT_REQUESTS_PREPARED_NOT_AUTHORIZED",
        "protocol_sha256": PROTOCOL_SHA256,
        "parent_t208_manifest_sha256": T208_PACKAGE_SHA256,
        "parent_request_bodies_sha256": T208_REQUEST_BODIES_SHA256,
        "parent_dispositions_sha256": T208_DISPOSITIONS_SHA256,
        "canonical_serializer_sha256": SERIALIZER_SHA256,
        "builder_source_sha256": sha256_file(Path(__file__).resolve()),
        "endpoint": COUNT_ENDPOINT,
        "projected_count_request_fields": ["model", "input", "text"],
        "omitted_generation_fields": ["max_output_tokens", "store", "temperature"],
        "denominator_cases": EXPECTED_DENOMINATOR,
        "ready_cases": EXPECTED_READY,
        "preparation_error_cases": EXPECTED_ERRORS,
        "preparation_error_reasons": error_reasons,
        "count_request_cases": len(projected_lines),
        "request_body_sha256_by_ordinal_sha256": sha256_bytes(("\n".join(body_hashes) + "\n").encode("ascii")),
        "outputs": {
            request_path.name: {"bytes": len(request_bytes), "sha256": sha256_bytes(request_bytes)},
            map_path.name: {"bytes": len(map_bytes), "sha256": sha256_bytes(map_bytes)},
        },
        "provider_call_performed": False,
        "upload_performed": False,
        "dispatch_authorized": False,
        "rights_and_transfer_status": "UNRESOLVED_KEEP_LOCAL",
        "billing_and_call_cap_treatment": "UNKNOWN_NO_COUNT_REQUEST_MADE",
        "account_key_and_capacity": "HUMAN_REPORTED_FREE_USD_0_NO_MISSION_KEY",
        "response_output_token_cost": "NOT_INCLUDED",
        "case_identity_location": map_path.name + " (local-only; never send)",
    }
    receipt_bytes = (json.dumps(receipt, ensure_ascii=False, indent=2, sort_keys=True) + "\n").encode("utf-8")
    (resolved_output / "count_package_receipt.json").write_bytes(receipt_bytes)
    return receipt


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", required=True, type=Path)
    parser.add_argument("--request-bodies", required=True, type=Path)
    parser.add_argument("--dispositions", required=True, type=Path)
    parser.add_argument("--output-dir", required=True, type=Path)
    args = parser.parse_args()
    try:
        result = build_package(args.manifest, args.request_bodies, args.dispositions, args.output_dir)
    except (OSError, UnicodeDecodeError, PackageError, json.JSONDecodeError) as exc:
        print(json.dumps({"status": "BLOCKED", "dispatch_authorized": False, "error": str(exc)}), file=sys.stderr)
        return 2
    print(json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
