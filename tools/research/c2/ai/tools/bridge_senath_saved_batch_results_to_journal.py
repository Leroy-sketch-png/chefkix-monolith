#!/usr/bin/env python3
"""Reconcile a complete saved Batch result pair into the existing C2 WAL.

Offline-only adapter: no SDK, credentials, network, provider call, selection,
retry, or dispatch authorization. It consumes only already-saved result files.
"""

from __future__ import annotations

import argparse
import hashlib
import importlib.util
import json
import os
import re
import tempfile
from pathlib import Path
from typing import Any

T253_SOURCE_SHA256 = "24091fe506bb91c4225de4c3d9052e2e00e8518b5810666d78afeb0f151742ac"
T219_V1_SOURCE_SHA256 = "d2a0a1c06f3a0f12c74c0bf3a766367ee93a2d0e5b3d26b4f35c2d2b1a71c36b"
T249_V2_SOURCE_SHA256 = "5f894e1b2d50120781ef7aaad9bcfeb0fa70e976787586bca00367f83686b2ae"
PARSER_SOURCE_SHA256 = "342dfc1f547bebb539267648c6b7dca6fd2f749d1f4e18e0b57e089d73f34656"
EXPECTED_ENDPOINT = "/v1/responses"
EXPECTED_MODEL = "gpt-4o-2024-08-06"
TOKEN_RE = re.compile(r"c2_[A-Za-z0-9_-]{43}\Z")
SHA256_RE = re.compile(r"[0-9a-f]{64}\Z")


class BridgeError(ValueError):
    """Sanitized, value-free validation failure."""


def sha256_bytes(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def _json_no_duplicates(raw: bytes) -> Any:
    def pairs(rows: list[tuple[str, Any]]) -> dict[str, Any]:
        result: dict[str, Any] = {}
        for key, value in rows:
            if key in result:
                raise BridgeError("duplicate JSON property")
            result[key] = value
        return result

    def reject_constant(_: str) -> None:
        raise BridgeError("non-standard JSON constant")

    try:
        return json.loads(raw.decode("utf-8", errors="strict"), object_pairs_hook=pairs,
                          parse_constant=reject_constant)
    except (UnicodeError, json.JSONDecodeError, BridgeError):
        raise BridgeError("malformed UTF-8 JSON record") from None


def _read_jsonl(raw: bytes, *, allow_empty: bool) -> list[dict[str, Any]]:
    if not raw:
        if allow_empty:
            return []
        raise BridgeError("required JSONL file is empty")
    if not raw.endswith(b"\n") or b"\r" in raw:
        raise BridgeError("JSONL must be complete LF-terminated records")
    records: list[dict[str, Any]] = []
    for line in raw[:-1].split(b"\n"):
        if not line:
            raise BridgeError("JSONL contains a blank record")
        value = _json_no_duplicates(line)
        if not isinstance(value, dict):
            raise BridgeError("JSONL record must be an object")
        records.append(value)
    return records


def _canonical(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True,
                      separators=(",", ":"), allow_nan=False).encode("utf-8", errors="strict")


def _load_pinned(name: str, path: Path, expected_hash: str):
    try:
        raw = path.read_bytes()
    except OSError:
        raise BridgeError(f"pinned {name} source unavailable") from None
    if sha256_bytes(raw) != expected_hash:
        raise BridgeError(f"pinned {name} source digest mismatch")
    spec = importlib.util.spec_from_file_location(f"t314_{name}", path)
    if spec is None or spec.loader is None:
        raise BridgeError(f"pinned {name} source could not be loaded")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def _validate_receipt(receipt: dict[str, Any], *, expected_batch_id: str,
                      expected_input_sha256: str, output_sha256: str,
                      error_sha256: str, output_count: int, error_count: int,
                      total_intents: int, model: str) -> None:
    required = {
        "schema", "status", "batch_id", "input_file_id", "input_file_sha256",
        "endpoint", "model", "output_file_sha256", "error_file_sha256", "request_counts",
    }
    if set(receipt) != required:
        raise BridgeError("saved Batch receipt has an unexpected schema")
    if (receipt.get("schema") != "C2_SAVED_BATCH_RECEIPT_V1"
            or receipt.get("status") != "completed"
            or not isinstance(receipt.get("batch_id"), str)
            or receipt["batch_id"] != expected_batch_id
            or not isinstance(receipt.get("input_file_id"), str)
            or not receipt["input_file_id"]
            or receipt.get("endpoint") != EXPECTED_ENDPOINT
            or receipt.get("model") != model or model != EXPECTED_MODEL
            or receipt.get("input_file_sha256") != expected_input_sha256
            or receipt.get("output_file_sha256") != output_sha256
            or receipt.get("error_file_sha256") != error_sha256):
        raise BridgeError("saved Batch receipt does not bind the expected job/package/files")
    counts = receipt.get("request_counts")
    if (not isinstance(counts, dict) or set(counts) != {"total", "completed", "failed"}
            or any(isinstance(counts[key], bool) or not isinstance(counts[key], int) or counts[key] < 0
                   for key in counts)
            or counts != {"total": total_intents, "completed": output_count, "failed": error_count}
            or output_count + error_count != total_intents):
        raise BridgeError("saved Batch receipt counts do not cover the frozen intent set")


def _map_indexes(mapping: list[dict[str, Any]], cases: list[dict[str, Any]]) -> tuple[dict[str, str], dict[str, dict[str, Any]]]:
    ready = {row["case_id"]: row for row in cases if row.get("disposition") == "READY"}
    if len(ready) != sum(row.get("disposition") == "READY" for row in cases):
        raise BridgeError("frozen case inventory contains duplicate identities")
    token_to_case: dict[str, str] = {}
    mapped_cases: set[str] = set()
    for row in mapping:
        if set(row) != {"custom_id", "case_ref"}:
            raise BridgeError("local custom-ID map has an unexpected schema")
        token, case_ref = row.get("custom_id"), row.get("case_ref")
        if (not isinstance(token, str) or TOKEN_RE.fullmatch(token) is None or token in token_to_case
                or not isinstance(case_ref, str) or case_ref not in ready or case_ref in mapped_cases):
            raise BridgeError("local custom-ID map is invalid or ambiguous")
        token_to_case[token] = case_ref
        mapped_cases.add(case_ref)
    if mapped_cases != set(ready):
        raise BridgeError("local custom-ID map does not cover the exact READY inventory")
    return token_to_case, ready


def _parse_result_rows(output_rows: list[dict[str, Any]], error_rows: list[dict[str, Any]],
                       token_to_case: dict[str, str], ready: dict[str, dict[str, Any]],
                       intents: set[str], parser: Any) -> list[tuple[str, str, dict[str, Any] | None]]:
    results: list[tuple[str, str, dict[str, Any] | None]] = []
    seen_tokens: set[str] = set()
    seen_result_ids: set[str] = set()
    for row, is_error in [*((row, False) for row in output_rows), *((row, True) for row in error_rows)]:
        expected_fields = {"id", "custom_id", "error"} if is_error else {"id", "custom_id", "response"}
        if set(row) != expected_fields or not isinstance(row.get("id"), str) or not row["id"]:
            raise BridgeError("saved Batch result line has an unexpected envelope")
        token = row.get("custom_id")
        if (not isinstance(token, str) or token in seen_tokens or token not in token_to_case):
            raise BridgeError("saved Batch result contains duplicate or unknown custom_id")
        if row["id"] in seen_result_ids:
            raise BridgeError("saved Batch result contains a duplicate provider result ID")
        seen_result_ids.add(row["id"])
        seen_tokens.add(token)
        case_id = token_to_case[token]
        if case_id not in intents:
            raise BridgeError("saved Batch result has no unique prior uncertain intent")
        case = ready[case_id]
        if is_error:
            error = row.get("error")
            if not isinstance(error, dict) or not error:
                raise BridgeError("saved Batch error result is malformed")
            results.append((case_id, "batch_error", None))
            continue
        response = row.get("response")
        if (not isinstance(response, dict) or set(response) != {"status_code", "request_id", "body"}
                or isinstance(response.get("status_code"), bool)
                or not isinstance(response.get("status_code"), int)
                or response["status_code"] < 100 or response["status_code"] > 599
                or not isinstance(response.get("request_id"), str) or not response["request_id"]
                or not isinstance(response.get("body"), dict)):
            raise BridgeError("saved Batch response envelope is malformed")
        body_bytes = _canonical(response["body"])
        if response["status_code"] == 200:
            record = parser.validate_response(
                body_bytes, case_ref=case_id,
                request_body_sha256=case["request_body_sha256"],
            )
        else:
            digest = sha256_bytes(body_bytes)
            record = {
                "case_id": case_id,
                "disposition": "TERMINAL_FAILURE",
                "failure_code": "provider_error",
                "request_body_sha256": case["request_body_sha256"],
                "response_sha256": digest,
            }
        results.append((case_id, "response", record))
    if seen_tokens != {token for token, case_id in token_to_case.items() if case_id in intents}:
        raise BridgeError("saved Batch result files are partial or omit an intent")
    return results


INGESTION_RECEIPT_FIELDS = {
    "schema", "state", "batch_id", "bundle_sha256", "package_manifest_sha256",
    "case_inventory_sha256", "batch_input_sha256", "provider_receipt_sha256",
    "output_sha256", "error_sha256", "result_count", "base_journal_sha256",
    "final_journal_sha256", "appended_event_count", "reconciled_existing_count",
}


def _receipt_bytes(record: dict[str, Any]) -> bytes:
    return json.dumps(record, ensure_ascii=False, sort_keys=True,
                      separators=(",", ":"), allow_nan=False).encode("utf-8") + b"\n"


def _atomic_create_receipt(path: Path, content: bytes, v1: Any) -> bytes:
    try:
        v1.atomic_write_no_overwrite(path, content)
    except Exception as exc:
        # A concurrently-created receipt is reconciled by the caller; do not
        # expose filesystem details or overwrite any existing evidence.
        if path.exists():
            return path.read_bytes()
        raise BridgeError("ingestion receipt could not be created atomically") from None
    return content


def _atomic_replace_receipt(path: Path, expected_old: bytes, content: bytes) -> None:
    temporary: Path | None = None
    try:
        if path.read_bytes() != expected_old:
            raise BridgeError("ingestion receipt changed concurrently; stop")
        descriptor, temporary_name = tempfile.mkstemp(prefix=f".{path.name}.pending-", dir=path.parent)
        temporary = Path(temporary_name)
        with os.fdopen(descriptor, "wb") as stream:
            stream.write(content)
            stream.flush()
            os.fsync(stream.fileno())
        if path.read_bytes() != expected_old:
            raise BridgeError("ingestion receipt changed concurrently; stop")
        os.replace(temporary, path)
        temporary = None
    except BridgeError:
        raise
    except OSError:
        raise BridgeError("ingestion receipt update is ambiguous; stop and reconcile") from None
    finally:
        if temporary is not None:
            try:
                temporary.unlink()
            except OSError:
                pass


def _read_ingestion_receipt(path: Path) -> tuple[dict[str, Any], bytes] | None:
    if not path.exists():
        return None
    try:
        raw = path.read_bytes()
    except OSError:
        raise BridgeError("ingestion receipt is unreadable; stop") from None
    record = _json_no_duplicates(raw)
    if (not isinstance(record, dict) or raw != _receipt_bytes(record)
            or set(record) != INGESTION_RECEIPT_FIELDS
            or record.get("schema") != "C2_SAVED_BATCH_INGESTION_V1"
            or record.get("state") not in {"STARTED", "COMPLETE"}
            or any(not isinstance(record.get(key), str) or SHA256_RE.fullmatch(record[key]) is None
                   for key in ("bundle_sha256", "package_manifest_sha256", "case_inventory_sha256",
                               "batch_input_sha256", "provider_receipt_sha256", "output_sha256",
                               "error_sha256", "base_journal_sha256"))
            or not isinstance(record.get("batch_id"), str) or not record["batch_id"]
            or any(isinstance(record.get(key), bool) or not isinstance(record.get(key), int)
                   or record[key] < 0 for key in ("result_count", "appended_event_count", "reconciled_existing_count"))
            or record["appended_event_count"] + record["reconciled_existing_count"] > record["result_count"]
            or (record["state"] == "STARTED" and record["final_journal_sha256"] is not None)
            or (record["state"] == "COMPLETE" and (not isinstance(record["final_journal_sha256"], str)
                or SHA256_RE.fullmatch(record["final_journal_sha256"]) is None
                or record["appended_event_count"] + record["reconciled_existing_count"] != record["result_count"]))):
        raise BridgeError("ingestion receipt is malformed or noncanonical; stop")
    return record, raw


def _validate_distinct_artifact_paths(*, package_dir: Path, cases_path: Path,
                                      events_path: Path, receipt_path: Path,
                                      output_path: Path, error_path: Path,
                                      ingestion_receipt_path: Path,
                                      dependency_paths: dict[str, Path]) -> None:
    """Reject path aliases before reading or mutating any local artifact."""
    paths = {
        "package manifest": package_dir / "manifest.json",
        "package request": package_dir / "batch_requests.jsonl",
        "package mapping": package_dir / "local_only_mapping.jsonl",
        "cases": cases_path,
        "journal": events_path,
        "provider receipt": receipt_path,
        "output": output_path,
        "error": error_path,
        "ingestion receipt": ingestion_receipt_path,
        "journal lock": events_path.with_name(events_path.name + ".lock"),
        "ingestion receipt lock": ingestion_receipt_path.with_name(
            ingestion_receipt_path.name + ".lock"
        ),
    }
    paths.update({f"pinned {name} source": path for name, path in dependency_paths.items()})
    entries = list(paths.items())
    try:
        resolved = {name: path.resolve() for name, path in entries}
        for index, (left_name, left_path) in enumerate(entries):
            for right_name, right_path in entries[index + 1:]:
                if resolved[left_name] == resolved[right_name]:
                    raise BridgeError("local artifact and lock paths must be distinct; preserve and stop")
                if left_path.exists() and right_path.exists() and os.path.samefile(left_path, right_path):
                    raise BridgeError("local artifact and lock paths must be distinct; preserve and stop")
    except BridgeError:
        raise
    except OSError:
        raise BridgeError("local artifact path identity is unavailable; preserve and stop") from None


def ingest_saved_batch(*, package_dir: Path, cases_path: Path, events_path: Path,
                       receipt_path: Path, output_path: Path, error_path: Path,
                       ingestion_receipt_path: Path,
                       expected_batch_id: str, expected_case_inventory_sha256: str,
                       expected_journal_sha256: str, root: Path,
                       dependency_paths: dict[str, Path]) -> dict[str, Any]:
    """Validate and reconcile one complete, digest-bound saved Batch bundle.

    The ingestion receipt is atomically created before the first WAL append.
    Re-entry requires identical bundle hashes, reconciles existing events to
    the exact parsed rows, and appends only missing terminal events. A legacy
    partial append without this binding fails closed for manual reconciliation.
    This remains offline-only and never authorizes a request retry.
    """
    _validate_distinct_artifact_paths(
        package_dir=package_dir, cases_path=cases_path, events_path=events_path,
        receipt_path=receipt_path, output_path=output_path, error_path=error_path,
        ingestion_receipt_path=ingestion_receipt_path, dependency_paths=dependency_paths,
    )
    t253 = _load_pinned("t253", dependency_paths["t253"], T253_SOURCE_SHA256)
    v1 = _load_pinned("t219", dependency_paths["t219"], T219_V1_SOURCE_SHA256)
    v2 = _load_pinned("t249", dependency_paths["t249"], T249_V2_SOURCE_SHA256)
    parser = _load_pinned("parser", dependency_paths["parser"], PARSER_SOURCE_SHA256)
    try:
        package_receipt = t253.validate_saved_package(root, package_dir)
        package_manifest = t253.read_json(package_dir / "manifest.json")
        mapping_raw = (package_dir / "local_only_mapping.jsonl").read_bytes()
        cases_raw = cases_path.read_bytes()
        receipt_raw = receipt_path.read_bytes()
        output_raw = output_path.read_bytes()
        error_raw = error_path.read_bytes()
        events_raw = events_path.read_bytes() if events_path.exists() else b""
    except (OSError, t253.CandidateError):
        raise BridgeError("pinned local package or saved result artifact is unavailable/invalid") from None
    if (not isinstance(expected_batch_id, str) or not expected_batch_id.strip()
            or SHA256_RE.fullmatch(expected_case_inventory_sha256) is None
            or SHA256_RE.fullmatch(expected_journal_sha256) is None
            or ingestion_receipt_path.resolve() in {events_path.resolve(), cases_path.resolve()}):
        raise BridgeError("expected batch or journal pins are malformed")
    if sha256_bytes(cases_raw) != expected_case_inventory_sha256:
        raise BridgeError("frozen case inventory digest mismatch")
    cases = _read_jsonl(cases_raw, allow_empty=False)
    if cases_raw != v1.jsonl_bytes(cases):
        raise BridgeError("frozen case inventory is not canonical JSONL")
    events, parsed_events = v1.read_jsonl(events_path) if events_path.exists() else ([], b"")
    if parsed_events != events_raw or (events and events_raw != v1.jsonl_bytes(events)):
        raise BridgeError("attempt journal is partial or noncanonical")
    if sha256_bytes(events_raw) != expected_journal_sha256:
        raise BridgeError("attempt journal digest changed; refresh and stop")
    package_manifest_sha256 = package_manifest.get("parent_t208_manifest_sha256")
    if not isinstance(package_manifest_sha256, str) or SHA256_RE.fullmatch(package_manifest_sha256) is None:
        raise BridgeError("T253 parent package binding is invalid")
    state = v2.reconcile_attempts_v2(
        cases, events, package_manifest_sha256,
        expected_case_inventory_sha256=expected_case_inventory_sha256,
        event_ledger_sha256=expected_journal_sha256, v1_path=dependency_paths["t219"],
    )
    states = {row["case_id"]: row["state"] for row in state["cases"]}
    mapping = _read_jsonl(mapping_raw, allow_empty=False)
    token_to_case, ready = _map_indexes(mapping, cases)
    request_rows, _ = t253.read_jsonl(package_dir / "batch_requests.jsonl")
    models = {row.get("body", {}).get("model") for row in request_rows
              if isinstance(row.get("body"), dict)}
    if models != {EXPECTED_MODEL}:
        raise BridgeError("pinned Batch package model differs from the frozen model")
    output_rows = _read_jsonl(output_raw, allow_empty=True)
    error_rows = _read_jsonl(error_raw, allow_empty=True)
    receipt = _json_no_duplicates(receipt_raw)
    if not isinstance(receipt, dict):
        raise BridgeError("saved Batch receipt is not an object")
    attempted_cases = {case_id for case_id, case_state in states.items()
                       if case_state in {"UNCERTAIN_NO_RETRY", "RESPONSE_RECEIVED", "TERMINAL_FAILURE"}}
    if attempted_cases != set(ready):
        raise BridgeError("saved Batch result denominator differs from the full frozen READY intent set")
    _validate_receipt(
        receipt, expected_batch_id=expected_batch_id,
        expected_input_sha256=package_receipt["batch_requests_sha256"],
        output_sha256=sha256_bytes(output_raw), error_sha256=sha256_bytes(error_raw),
        output_count=len(output_rows), error_count=len(error_rows),
        total_intents=len(attempted_cases), model=next(iter(models)),
    )
    results = _parse_result_rows(output_rows, error_rows, token_to_case, ready, attempted_cases, parser)
    if len(results) != len(attempted_cases):
        raise BridgeError("saved Batch result count differs from authorized-intent denominator")

    expected_events: list[tuple[str, dict[str, Any], tuple[str, str, dict[str, Any] | None]]] = []
    for index, (case_id, result_kind, parser_record) in enumerate(results, start=1):
        event_id = f"batch:{expected_batch_id}:{index:05d}:{sha256_bytes(case_id.encode('utf-8'))[:16]}"
        expected_event: dict[str, Any] = {
            "event_id": event_id, "case_id": case_id,
            "package_manifest_sha256": package_manifest_sha256,
            "request_body_sha256": ready[case_id]["request_body_sha256"],
        }
        if result_kind == "batch_error":
            expected_event.update({"event_type": "TERMINAL_FAILURE", "failure_code": "provider_error"})
        else:
            assert parser_record is not None
            if parser_record["disposition"] == "RESPONSE_VALID":
                expected_event.update({"event_type": "RESPONSE_RECEIVED",
                                       "response_sha256": parser_record["response_sha256"]})
            else:
                expected_event.update({"event_type": "TERMINAL_FAILURE",
                                       "failure_code": parser_record["failure_code"],
                                       "response_sha256": parser_record["response_sha256"],
                                       "event_schema": v2.V2_FAILURE_MARKER})
        expected_events.append((case_id, expected_event, (case_id, result_kind, parser_record)))

    expected_batch_event_ids = {event["event_id"] for _, event, _ in expected_events}
    existing_by_id = {event["event_id"]: event for event in events}
    batch_prefix = f"batch:{expected_batch_id}:"
    existing_batch_events = {event["event_id"] for event in events
                             if isinstance(event.get("event_id"), str)
                             and event["event_id"].startswith(batch_prefix)}
    if not existing_batch_events <= expected_batch_event_ids:
        raise BridgeError("journal contains an unexpected event for this Batch ID")

    matched_existing: list[tuple[str, dict[str, Any], tuple[str, str, dict[str, Any] | None]]] = []
    to_append: list[tuple[str, dict[str, Any], tuple[str, str, dict[str, Any] | None]]] = []
    for case_id, expected_event, result in expected_events:
        prior = existing_by_id.get(expected_event["event_id"])
        current_state = states[case_id]
        if prior is not None:
            prior_without_sequence = {key: value for key, value in prior.items() if key != "sequence"}
            if prior_without_sequence != expected_event or current_state not in {"RESPONSE_RECEIVED", "TERMINAL_FAILURE"}:
                raise BridgeError("existing terminal event differs from this exact saved Batch result")
            matched_existing.append((case_id, expected_event, result))
        elif current_state == "UNCERTAIN_NO_RETRY":
            to_append.append((case_id, expected_event, result))
        else:
            raise BridgeError("consumed attempt has no exact matching Batch result event")

    bundle_parts = {
        "batch_id": expected_batch_id,
        "package_manifest_sha256": package_manifest_sha256,
        "case_inventory_sha256": expected_case_inventory_sha256,
        "batch_input_sha256": package_receipt["batch_requests_sha256"],
        "provider_receipt_sha256": sha256_bytes(receipt_raw),
        "output_sha256": sha256_bytes(output_raw),
        "error_sha256": sha256_bytes(error_raw),
        "result_count": len(results),
    }
    bundle_sha256 = sha256_bytes(_canonical(bundle_parts))
    receipt_lock = ingestion_receipt_path.with_name(ingestion_receipt_path.name + ".lock")
    with v1._cooperative_file_lock(receipt_lock):
        loaded_ingestion_receipt = _read_ingestion_receipt(ingestion_receipt_path)
        if loaded_ingestion_receipt is None:
            if existing_batch_events:
                raise BridgeError("legacy partial Batch events lack a prior bundle binding; preserve and stop")
            started = {
                "schema": "C2_SAVED_BATCH_INGESTION_V1", "state": "STARTED",
                "batch_id": expected_batch_id, "bundle_sha256": bundle_sha256,
                "package_manifest_sha256": package_manifest_sha256,
                "case_inventory_sha256": expected_case_inventory_sha256,
                "batch_input_sha256": package_receipt["batch_requests_sha256"],
                "provider_receipt_sha256": sha256_bytes(receipt_raw),
                "output_sha256": sha256_bytes(output_raw), "error_sha256": sha256_bytes(error_raw),
                "result_count": len(results), "base_journal_sha256": expected_journal_sha256,
                "final_journal_sha256": None, "appended_event_count": 0,
                "reconciled_existing_count": 0,
            }
            ingestion_raw = _atomic_create_receipt(ingestion_receipt_path, _receipt_bytes(started), v1)
            loaded_ingestion_receipt = _read_ingestion_receipt(ingestion_receipt_path)
            if loaded_ingestion_receipt is None or ingestion_raw != loaded_ingestion_receipt[1]:
                raise BridgeError("ingestion receipt creation did not verify")
        ingestion_receipt, old_ingestion_raw = loaded_ingestion_receipt
        if any(ingestion_receipt.get(key) != value for key, value in {
            "batch_id": expected_batch_id, "bundle_sha256": bundle_sha256,
            "package_manifest_sha256": package_manifest_sha256,
            "case_inventory_sha256": expected_case_inventory_sha256,
            "batch_input_sha256": package_receipt["batch_requests_sha256"],
            "provider_receipt_sha256": sha256_bytes(receipt_raw),
            "output_sha256": sha256_bytes(output_raw), "error_sha256": sha256_bytes(error_raw),
            "result_count": len(results),
        }.items()):
            raise BridgeError("saved result bundle differs from its durable ingestion binding")
        if (ingestion_receipt["state"] == "COMPLETE"
                and ingestion_receipt["appended_event_count"]
                + ingestion_receipt["reconciled_existing_count"] != len(results)):
            raise BridgeError("completed ingestion receipt does not match result coverage")
        if ingestion_receipt["state"] == "COMPLETE":
            if expected_journal_sha256 != ingestion_receipt["final_journal_sha256"]:
                raise BridgeError("completed ingestion journal digest changed; preserve and stop")
            if to_append or len(matched_existing) != len(results):
                raise BridgeError("completed ingestion is not an exact journal no-op; preserve and stop")

        current_digest = expected_journal_sha256
        appended_count = 0
        for case_id, expected_event, (_, result_kind, parser_record) in to_append:
            if result_kind == "batch_error":
                appended = v2.append_event_v2(
                    cases_path, events_path, package_manifest_sha256,
                    expected_case_inventory_sha256, expected_journal_sha256=current_digest,
                    case_id=case_id, event_id=expected_event["event_id"], event_type="TERMINAL_FAILURE",
                    failure_code="provider_error", v1_path=dependency_paths["t219"],
                )
            else:
                assert parser_record is not None
                appended = v2.append_validated_response(
                    cases_path, events_path, package_manifest_sha256,
                    expected_case_inventory_sha256, expected_journal_sha256=current_digest,
                    parser_record=parser_record, event_id=expected_event["event_id"],
                    v1_path=dependency_paths["t219"],
                )
            current_digest = appended["event_ledger_sha256"]
            appended_count += 1

        final_events, final_raw = v1.read_jsonl(events_path)
        if sha256_bytes(final_raw) != current_digest:
            raise BridgeError("journal digest differs after saved-result appends; stop")
        final_by_id = {event["event_id"]: event for event in final_events}
        for _, expected_event, _ in expected_events:
            prior = final_by_id.get(expected_event["event_id"])
            if (prior is None or {key: value for key, value in prior.items() if key != "sequence"}
                    != expected_event):
                raise BridgeError("final journal does not contain exact results for the complete bundle")

        complete = dict(ingestion_receipt)
        complete.update({
            "state": "COMPLETE", "final_journal_sha256": current_digest,
            "appended_event_count": appended_count,
            "reconciled_existing_count": len(matched_existing),
        })
        complete_raw = _receipt_bytes(complete)
        if ingestion_receipt["state"] == "STARTED":
            _atomic_replace_receipt(ingestion_receipt_path, old_ingestion_raw, complete_raw)
        elif ingestion_receipt["state"] == "COMPLETE":
            complete_raw = old_ingestion_raw
        return {
            "status": "LOCAL_SAVED_RESULT_RECONCILIATION_ONLY",
            "ingestion_state": "NO_OP_VERIFIED" if ingestion_receipt["state"] == "COMPLETE" else "COMPLETE",
            "bundle_sha256": bundle_sha256,
            "package_manifest_sha256": package_manifest_sha256,
            "case_inventory_sha256": expected_case_inventory_sha256,
            "result_count": len(results), "batch_error_count": len(error_rows),
            "response_count": len(output_rows), "event_count": appended_count,
            "appended_event_count": appended_count,
            "reconciled_existing_count": len(matched_existing),
            "ingestion_receipt_sha256": sha256_bytes(complete_raw),
            "event_ledger_sha256": current_digest,
            "dispatch_authorized": False, "provider_called": False, "cohort_selected": False,
        }
    return {
        "status": "LOCAL_SAVED_RESULT_RECONCILIATION_ONLY",
        "package_manifest_sha256": package_manifest_sha256,
        "case_inventory_sha256": expected_case_inventory_sha256,
        "prior_journal_sha256": expected_journal_sha256,
        "result_count": len(pending),
        "batch_error_count": len(error_rows),
        "response_count": len(output_rows),
        "event_count": len(pending),
        "event_ledger_sha256": current_digest,
        "dispatch_authorized": False,
        "provider_called": False,
        "cohort_selected": False,
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path.cwd())
    parser.add_argument("--package-dir", type=Path, required=True)
    parser.add_argument("--cases", type=Path, required=True)
    parser.add_argument("--events", type=Path, required=True)
    parser.add_argument("--receipt", type=Path, required=True)
    parser.add_argument("--output-jsonl", type=Path, required=True)
    parser.add_argument("--error-jsonl", type=Path, required=True)
    parser.add_argument("--ingestion-receipt", type=Path, required=True,
                        help="new local bundle-binding receipt; reused only for an exact same-bundle resume")
    parser.add_argument("--expected-batch-id", required=True)
    parser.add_argument("--case-inventory-sha256", required=True)
    parser.add_argument("--journal-sha256", required=True)
    parser.add_argument("--t253-source", type=Path, default=Path("ai/tools/build_senath_c2_batch_transport_candidate.py"))
    parser.add_argument("--t219-source", type=Path, default=Path("ai/tools/c2_attempt_resume.py"))
    parser.add_argument("--t249-source", type=Path, default=Path("ai/tools/c2_attempt_journal_v2.py"))
    parser.add_argument("--parser-source", type=Path, default=Path("ai/tools/validate_c2_openai_response.py"))
    args = parser.parse_args()
    deps = {"t253": args.t253_source, "t219": args.t219_source,
            "t249": args.t249_source, "parser": args.parser_source}
    try:
        result = ingest_saved_batch(
            package_dir=args.package_dir, cases_path=args.cases, events_path=args.events,
            receipt_path=args.receipt, output_path=args.output_jsonl, error_path=args.error_jsonl,
            ingestion_receipt_path=args.ingestion_receipt,
            expected_batch_id=args.expected_batch_id,
            expected_case_inventory_sha256=args.case_inventory_sha256,
            expected_journal_sha256=args.journal_sha256, root=args.root, dependency_paths=deps,
        )
    except (BridgeError, OSError, ValueError) as exc:
        print(json.dumps({"status": "BLOCKED", "reason": str(exc)}, sort_keys=True))
        return 2
    print(json.dumps(result, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
